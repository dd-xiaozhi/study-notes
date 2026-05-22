# Redis集群(cluster)

## 1、概述

>   维基百科
>
>   **计算机集群**（英语：computer cluster）是一组松散或紧密连接在一起工作的[计算机](https://zh.wikipedia.org/wiki/電子計算機)。由于这些计算机协同工作，在许多方面它们可以被视为单个系统。与[网格计算机](https://zh.wikipedia.org/wiki/网格计算)不同，计算机集群将每个[节点](https://zh.wikipedia.org/w/index.php?title=节点_(计算机科学)&action=edit&redlink=1)设置为执行相同的任务，由软件控制和调度。



简单点来说就是多个服务器组成一个集群来提供服务，每个服务器分摊服务的压力，比如存储服务就是每个服务器都存一部分的数据，这样协同工作形成一个整体，水平扩展的能力可以使系统拥有很好的扩展性。



**是什么**

由于数据量过大，单个Master复制集难以承担，因此需要对多个复制集来过程一个集群提供服务， 其作用就是提供在多个Redis节点共享数据的程序集。每个Redis节点存有不同的数据，这样协同工作来形成一个大的集群系统。

![image-20230602162748314](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602162748314.png) 

上图是官网的集群架构图，分别是三台Master和三台Slave，由它们组成三组主从复制的结构。



**能干嘛**

Redis集群支持多个Master，每个Master又可以挂载多个slave

-   读写分离
-   支持数据的高可用
-   支持海量数据的读写存储操作

由于Cluster自带Sentinel的故障转移机制，内置了高可用的支持，无需再去使用哨兵功能客户端与Redis的节点连接，不再需要连接集群中所有的节点，只需要任意连接集群中的一个可用节点即可槽位slot负责分配到各个物理服务节点，由对应的集群来负责维护节点、插槽和数据之间的关系

Redis集群不保证强一致性，在特定的情况下它可能会丢失一些被系统接收到的写入请求命令，也就是说Redis集群是AP而不是CP





## 2、集群算法

### 2.1 概述

>   **官网原话**：
>
>   **Key distribution model** 密钥分发模型
>
>   集群的密钥空间分为 16384 个插槽，有效地为 16384 个主节点的集群大小设置了上限（但是，建议的最大节点大小约为 ~ 1000 个节点）。
>
>   集群中的每个主节点处理 16384 哈希槽的子集。当没有正在进行的集群重新配置时（即哈希槽从一个节点移动到另一个节点），集群是稳定的。当集群稳定时，单个节点将提供单个哈希槽（但是，服务节点可以有一个或多个副本，这些副本将在网络拆分或故障的情况下替换它，并且可用于扩展读取操作，其中读取过时数据是可以接受的）。
>
>   用于将密钥映射到哈希槽的基本算法如下（有关此规则的哈希标记例外，请阅读下一段）：
>
>   ```sh
>   HASH_SLOT = CRC16(key) mod 16384
>   ```

**Redis集群的槽位slot**

Redis集群没有使用一致性Hash，而是引入了**哈希槽**的方式

Redis集群有 16348（2^14）个哈希槽，每个key通过 `CRC6` 校验后对 16384 取模来决定放置哪个槽位，集群的每个节点负责部分的hash槽

例如：当前集群有三个节点，那么槽位分配分别是 0-5460、5361-10922、10923-16383，通过计算 key 来获取它要放入的槽位置，接着由负责对应槽位的Redis节点来提供服务。

![image-20230602171820473](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602171820473.png) 

**Redis集群的分片**

使用Redis集群时我们会将存储的数据分散到多台redis机器上，这称为分片。简言之，集群中的每个Redis实例都被认为是整个数据的一个分片。

为了找到给定key的分片，我们对key进行CRC16(key)算法处理并通过对总分片数量取模。然后，使用确定性哈希函数，这意味着给定的key将多次始终映射到同一个分片，我们可以推断将来读取特定key的位置。

**分片和槽位的组合优势**

方便扩容收缩和数据分派查找。这种方式可以让我们很容易的添加节点，只需要将其他节点的槽位分配一点给新增的节点即可，删除也是一样的，将对应的槽位还给对应节点。无论是添加节点还是删除节点都能保证集群可用。



### 2.2 slot槽位映射方案

#### 2.2.1 哈希取余分区

![image-20230602172905836](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602172905836.png) 

通过计算key的哈希值摸上服务节点的数量来决定由那个节点来提供服务

**优点**：简单粗暴，只需要预估好数据规划好节点即可，

**缺点**：进行扩容和缩容比较麻烦，每次节点变动都要进行重新计算，也就是说如果由一台挂掉了就会导致节点数量发生变化，此时就需要冲洗洗牌。



#### 2.2.2 一致性哈希算法分区

**一致性Hash算法背景**

一致性哈希算法在1997年由麻省理工学院中提出的，设计目标是为了解决分布式缓存数据变动和映射问题，某个机器宕机了，分母数量改变了，自然取余数不OK了。它可以减少影响客户端到服务器端的映射关系

**三大步骤**

1.  算法构建一致性哈希环

    一致性哈希算法必然有个hash函数并按照算法产生hash值，这个算法的所有可能哈希值会构成一个全量集，这个集合可以成为一个hash空间[0,2^32-1]，这个是一个线性空间，但是在算法中，我们通过适当的逻辑控制将它首尾相连(0 = 2^32),这样让它逻辑上形成了一个环形空间。**实际上是一个数组，逻辑上是一个环形**

    ![image-20230602173538278](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602173538278.png) 

2.  redis服务器IP节点映射

    将集群中各个IP节点映射到环上的某一个位置。将各个服务器使用Hash进行一个哈希，具体可以选择服务器的IP或主机名作为关键字进行哈希，这样每台机器就能确定其在哈希环上的位置。假如4个节点NodeA、B、C、D，经过IP地址的哈希函数计算(hash(ip))，使用IP地址哈希后在环空间的位置如下：

    ![image-20230602173637615](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602173637615.png) 

3.  key落到服务器的落建规则

    计算出key的hash值，确定key在环上的位置，接着按顺序针走，遇到的第一台Redis就是提供服务的节点。

    ![image-20230602174009045](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602174009045.png)

**优点**：

-   一致性哈希算法的容错性

    假设Node C宕机，可以看到此时对象A、B、D不会受到影响。一般的，在一致性Hash算法中，如果一台服务器不可用，则受影响的数据仅仅是此服务器到其环空间中前一台服务器（即沿着逆时针方向行走遇到的第一台服务器）之间数据，其它不会受到影响。简单说，就是C挂了，受到影响的只是B、C之间的数据且这些数据会转移到D进行存储。

    ![image-20230602174308093](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602174308093.png) 

-   一致性哈希算法的扩展性

    数据量增加了，需要增加一台节点NodeX，X的位置在A和B之间，那收到影响的也就是A到X之间的数据，重新把A到X的数据录入到X上即可，不会导致hash取余全部数据重新洗牌。

    ![image-20230602174327449](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602174327449.png) 



**缺点**：一致性哈希算法的数据倾斜问题

一致性Hash算法在服务**节点太少时**，容易因为节点分布不均匀而造成**数据倾斜**（被缓存的对象大部分集中缓存在某一台服务器上）问题，例如系统中只有两台服务器：

![image-20230602174412082](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602174412082.png) 

可以看到A节点明显服务压力比B的要更大



#### 2.2.3 哈希槽分区(Redis使用)

解决均匀分配的问题，在数据和节点之间又加入了一层，把这层称为哈希槽（slot），用于管理数据和节点之间的关系，现在就相当于节点上放的是槽，槽里放的是数据。

![image-20230602174512983](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602174512983.png) 

槽解决的是粒度问题，相当于把粒度变大了，这样便于数据移动。哈希解决的是映射问题，使用key的哈希值来计算所在的槽，便于数据分配

**多少个hash槽**

一个集群只能有16384个槽，编号0-16383（0-2^14-1）。这些槽会分配给集群中的所有主节点，分配策略没有要求。

集群会记录节点和槽的对应关系，解决了节点和槽的关系后，接下来就需要对key求哈希值，然后对16384取模，余数是几key就落入对应的槽里。HASH_SLOT = CRC16(key) mod 16384。以槽为单位移动数据，因为槽的数目是固定的，处理起来比较容易，这样数据移动问题就解决了。



### 2.3 面试题

**为什么Redis集群的最大槽数是 16384 个？**

Redis集群并没有使用一致性hash而是引入了哈希槽的概念。Redis 集群有16384个哈希槽，每个key通过CRC16校验后对16384取模来决定放置哪个槽，集群的每个节点负责一部分hash槽。但为什么哈希槽的数量是16384（2^14）个呢？

CRC16算法产生的hash值有16bit，该算法可以产生2^16=65536个值。换句话说值是分布在0~65535之间，有更大的65536不用为什么只用16384就够？作者在做mod运算的时候，为什么不mod65536，而选择mod16384？ HASH_SLOT = CRC16(key) mod 65536为什么没启用

作者回答：https://github.com/redis/redis/issues/2576

 ![image-20230602174822053](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602174822053.png)

>   **作者回答**：
>
>   正常的心跳数据包带有节点的完整配置，可以用幂等方式用旧的节点替换旧节点，以便更新旧的配置。这意味着它们包含原始节点的插槽配置，该节点使用2k的空间和16k的插槽，但是会使用8k的空间（使用65k的插槽）。
>
>   同时，由于其他设计折衷，Redis集群不太可能扩展到1000个以上的主节点。
>
>   因此16k处于正确的范围内，以确保每个主机具有足够的插槽，最多可容纳1000个矩阵，但数量足够少，可以轻松地将插槽配置作为原始位图传播。请注意，在小型群集中，位图将难以压缩，因为当N较小时，位图将设置的slot / N位占设置位的很大百分比。

**说明**：

**(1)如果槽位为65536，发送心跳信息的消息头达8k，发送的心跳包过于庞大。**

在消息头中最占空间的是myslots[CLUSTER_SLOTS/8]。 当槽位为65536时，这块的大小是: 65536÷8÷1024=8kb 

在消息头中最占空间的是myslots[CLUSTER_SLOTS/8]。 当槽位为16384时，这块的大小是: 16384÷8÷1024=2kb 

因为每秒钟，redis节点需要发送一定数量的ping消息作为心跳包，如果槽位为65536，这个ping消息的消息头太大了，浪费带宽。

**(2)redis的集群主节点数量基本不可能超过1000个。**

集群节点越多，心跳包的消息体内携带的数据越多。如果节点过1000个，也会导致网络拥堵。因此redis作者不建议redis cluster节点数量超过1000个。 那么，对于节点数在1000以内的redis cluster集群，16384个槽位够用了。没有必要拓展到65536个。

**(3)槽位越小，节点少的情况下，压缩比高，容易传输**

Redis主节点的配置信息中它所负责的哈希槽是通过一张bitmap的形式来保存的，在传输过程中会对bitmap进行压缩，但是如果bitmap的填充率slots / N很高的话(N表示节点数)，bitmap的压缩率就很低。 如果节点数很少，而哈希槽数量很多的话，bitmap的压缩率就很低。 

**总的来说**：Redis是一个需要低延迟快熟反应的数据库，所以传输越快就越好







### 2.4 CRC16算法分析











## 3、集群搭建

![image-20230602231922464](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602231922464.png) 

**架构说明**：

资源有限，利用三台机器来做这个实验，一台机器上运行两个实例，一共是六台，对应是三组一主一从，集群它会自己进行分配，不需要我们去指定，所以不用指定谁是 Master 谁是 Slave





### 3.1 集群搭建

#### 3.1.1 集群配置

创建一个新目录放置我们集群的文件

```sh
mkdir -p /myredis/cluster
```

三台机器对应的IP和启动的端口号

| 192.168.10.10 | 192.168.10.11 | 192.168.10.12 |
| ------------- | ------------- | ------------- |
| 6381          | 6383          | 6385          |
| 6382          | 6384          | 6386          |

在对应主机的 `/myredis/cluster` 目录下添加对应端口号的配置文件

```sh
bind 0.0.0.0
daemonize yes
protected-mode no
port 6381
logfile "/myredis/cluster/cluster6381.log"
pidfile /myredis/cluster6381.pid
dir /myredis/cluster
dbfilename dump6381.rdb
appendonly yes
appendfilename "appendonly6381.aof"
requirepass 111111
masterauth 111111
 
# 开启集群
cluster-enabled yes
# 集群的配置文件
cluster-config-file nodes-6381.conf
# 集群连接的超时时间
cluster-node-timeout 5000
```

==其他端口号的机器要修改对应的端口号！！！==



#### 3.1.2 启动集群

首先启动我们配置好的 6 台实例

```sh
redis-server redisCluster<port>.conf
```

通过 redis-cli命令为6台机器构建集群关系

```sh
# 注意这里要写真实地址，不能写 127.0.0.1
# ----cluster-replicas <num> 表示为每个Master创建 num 个slave节点
redis-cli -a 111111 --cluster create --cluster-replicas 1 192.168.10.10:6381 192.168.10.10:6382 192.168.10.11:6383 192.168.10.11:6384 192.168.10.12:6385 192.168.10.12:6386
```

![image-20230602234913048](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230602234913048.png) 

创建成功~~~



#### 3.1.3 查看集群状态

| 命令             | 作用呢           |
| ---------------- | ---------------- |
| info replication | 查看当前节点信息 |
| cluster info     |                  |
| cluster nodes    |                  |

连接进入6381 作为切入点，查看并检验集群状态

```sh
[root@xdz cluster]# redis-cli -a 111111 -p 6381
```

**查看集群信息**

```sh
127.0.0.1:6381> cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
# 集群运行的节点数
cluster_known_nodes:6
# Master的数量为3台
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:1
```

**查看节点信息**

```sh
127.0.0.1:6381> cluster nodes
da1ccf965889a4874605ce89f489c89ceab3ef4f 192.168.10.11:6383@16383 master - 0 1685721310557 3 connected 5461-10922
6b4b0bcec3a687b29273d3f79abb834d4386f2cf 192.168.10.12:6386@16386 slave da1ccf965889a4874605ce89f489c89ceab3ef4f 0 1685721311600 3 connected
a6719abe0d0639d27dbdb48e267a6c0e0c15e691 192.168.10.10:6382@16382 slave e426860fd72c155c6ea02878b3ad4162b7200a8e 0 1685721311000 5 connected
e426860fd72c155c6ea02878b3ad4162b7200a8e 192.168.10.12:6385@16385 master - 0 1685721311000 5 connected 10923-16383
7a74adf8649cc0e0cb3255f8575cf199ec3660d1 192.168.10.11:6384@16384 slave 2c390e90f71a8085915fb47f64e7daca5d4bcae6 0 1685721311600 1 connected
2c390e90f71a8085915fb47f64e7daca5d4bcae6 192.168.10.10:6381@16381 myself,master - 0 1685721311000 1 connected 0-5460
```

我们可以通过节点信息知道Master节点和Slave节点分别是哪几个

-   master前面的就是我们的Master节点
-   slave 前面的是 从节点，后面的是它对应的主节点
-   myself 表示是当前的客户端
-   5461-10922、10923-16383、0-5460表示的是分配给对应节点的槽位

那么可以得到的信息如下表

| Master | Slave | 槽位        |
| ------ | ----- | ----------- |
| 6383   | 6386  | 5461-10922  |
| 6385   | 6382  | 10923-16383 |
| 6381   | 6384  | 0-5460      |



### 3.2 集群读写

**添加数据**

```sh
127.0.0.1:6381> set k1 v1
(error) MOVED 12706 192.168.10.12:6385
127.0.0.1:6381> set k2 v2
OK
```

设置 k2 没有报错，设置 k1 的时候报错了，这是为啥？

我们此时是在 6381 客户端下的，而 k1 计算出来的值不在 6381 节点映射的槽位，所以会出现这个错误

```sh
# 通过 CLUSTER KEYSLOT <key> 命令可以查看key对应的槽位值
127.0.0.1:6381> CLUSTER KEYSLOT k1
(integer) 12706
```

**如何解决？**

在连接客户端之前加`-c` 参数，它会帮我们重定向到对应的节点

```sh
redis-cli -a 111111 -p 6381 -c
```

此时再次添加数据

```sh
127.0.0.1:6381> set k1 v1                                                    
-> Redirected to slot [12706] located at 192.168.10.12:6385                  
OK
```

完美~~~



### 3.3 主从容错切换迁移

#### 3.3.1 Master挂掉

此时的集群信息

| Master | Slave | 槽位        |
| ------ | ----- | ----------- |
| 6383   | 6386  | 5461-10922  |
| 6385   | 6382  | 10923-16383 |
| 6381   | 6384  | 0-5460      |

我们将 `6383` 端口号关闭，看一下 `6386` 会不会上位

```sh
127.0.0.1:6383> SHUTDOWN
not connected>
```

接着查看集群信息

![image-20230604115141255](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604115141255.png)

`6383`成功上位，让我们恭喜这位候补选手



#### 3.3.2 Master回归

那么 Master 回归它还是 Master 吗？

![image-20230604115430956](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604115430956.png)

On No~~~，该死，回不去了，Master是以 Slave 的方式回归，老大哥变小弟了哈哈



#### 3.3.3 维持原有关系

能不能 Master 回归还是 Master 呢？是可以的，可以在对应机器上执行下面的命令来指定 Master

```sh
CLUSTER FAILOVER
```

恢复关系，之前我们的Master节点是 `6383`，所以登录到 `6383` 机器

```sh
127.0.0.1:6383> CLUSTER FAILOVER
OK
```

查看节点信息

![image-20230604115946371](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604115946371.png)

我胡汉天又回来了哈哈~~~





### 3.4 主从扩容

#### 3.4.1 加入集群



**新增节点**

在 192.168.10.12 机器上添加两个节点 分别是 6387、6388

```sh
# 启动两台机器
[root@localhost cluster]# redis-server redisCluster6387.conf                              
[root@localhost cluster]# redis-server redisCluster6388.conf
```



**加入集群**

将 6387 加入到集群中，通过 6381 机器加入到集群中，6381 相当于是 6387 的领路人，带它加入集群大家庭中

```sh
[root@localhost cluster]# redis-cli -a 111111 --cluster add-node 192.168.10.12:6387 192.168.10.10:6381
Warning: Using a password with '-a' or '-u' option on the command line interface may not be safe.
>>> Adding node 192.168.10.12:6387 to cluster 192.168.10.10:6381
>>> Performing Cluster Check (using node 192.168.10.10:6381)
M: 2c390e90f71a8085915fb47f64e7daca5d4bcae6 192.168.10.10:6381
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
S: da1ccf965889a4874605ce89f489c89ceab3ef4f 192.168.10.11:6383
   slots: (0 slots) slave
   replicates 6b4b0bcec3a687b29273d3f79abb834d4386f2cf
M: 6b4b0bcec3a687b29273d3f79abb834d4386f2cf 192.168.10.12:6386
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
S: a6719abe0d0639d27dbdb48e267a6c0e0c15e691 192.168.10.10:6382
   slots: (0 slots) slave
   replicates e426860fd72c155c6ea02878b3ad4162b7200a8e
M: e426860fd72c155c6ea02878b3ad4162b7200a8e 192.168.10.12:6385
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: 7a74adf8649cc0e0cb3255f8575cf199ec3660d1 192.168.10.11:6384
   slots: (0 slots) slave
   replicates 2c390e90f71a8085915fb47f64e7daca5d4bcae6
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
>>> Getting functions from cluster
>>> Send FUNCTION LIST to 192.168.10.12:6387 to verify there is no functions in it
>>> Send FUNCTION RESTORE to 192.168.10.12:6387
>>> Send CLUSTER MEET to node 192.168.10.12:6387 to make it join the cluster.
[OK] New node added correctly.	# 表示加入成功
```



**检查集群情况**

```sh
[root@xdz cluster]# redis-cli -a 111111 --cluster check 192.168.10.10:6381
```

![image-20230604130620661](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604130620661.png)

此时新加入的加点是还没有分配槽位的



#### 3.4.2 分派槽位

各个节点给新加入的加点分派部分槽位

```sh
[root@xdz cluster]# redis-cli -a 111111 --cluster reshard 192.168.10.10:6381
```

![image-20230604131304696](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604131304696.png)

**检查集群情况**

![image-20230604131425822](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604131425822.png)

可以看到新加入的节点也拥有自己的槽位了，它被分成了三部分，是集群之前的三台节点都分配一点所造成的，每台机器都分派出一部分。



#### 3.4.3 分配从节点

命令

```sh
redis-cli -a "密码" --cluster add-node 新节点ID:端口号 领路节点ID:端口号 --cluster-slave --cluster-master-id masterID 
```

为新增的 6387 节点添加 从节点

```sh
[root@xdz cluster]# redis-cli -a 111111 --cluster add-node 192.168.10.12:6388 192.168.10.1
2:6387 --cluster-slave --cluster-master-id 83c7f82b66f1ad1dca1647b3150d46437df0caa4   
```

![image-20230604132057078](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604132057078.png) 

添加成功，再次检查集群状态

![image-20230604132201476](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604132201476.png)  

成功~~~~





### 3.5 主从缩容

让  6387 和 6388 节点下线

首先需要获取下线节点的ID

![image-20230604173226887](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604173226887.png)



**从集群中将 6388 节点删除**

```sh
[root@xdz cluster]# redis-cli -a 111111 --cluster  del-node 192.168.10.12:6388 d9261712bf10d40c127e2b8a573f658d315cba66
```

![image-20230604173434110](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604173434110.png)

检查集群信息

![image-20230604173631993](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604173631993.png) 

可以发现集群中还剩余 7 个节点，6388 成功被移除



**将 6387 的槽位全部转给 6381**

![image-20230604174218808](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604174218808.png)

此时检查集群状态可以发现 6381 的槽位数是另外两个节点的两倍

![image-20230604174038569](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604174038569.png)



**将 6387 从集群中移除**

```sh
[root@xdz cluster]# redis-cli -a 111111 --cluster del-node 192.168.10.12:6387 83c7f82b66f1ad1dca1647b3150d46437df0caa4
```

![image-20230604174455222](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604174455222.png)

检查集群状态

![image-20230604174614578](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604174614578.png)

自此，6387 和 6388 就成功被移除了~~~~





## 4、常用配置和命令

### 4.1 配置

![image-20230604175001804](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604175001804.png)

```sh
cluster-require-full-coverage yes
```

默认是yes，集群是又多个节点组成，多个节点之间分摊服务的压力，如果其中的节点挂掉，就会导致提供的服务不完整，yes表示不完整的情况下Reids不提供服务，no表示继续提供服务，不过数据不完整，对于数据完整性的应用建议是yes，相反可以设置为no



### 4.2 Reids 命令

**查看槽位是否被占用**

```sh
 CLUSTER COUNTKEYSINSLOT slot
 # 返回0表示未占用，1表示已占用
```

```sh
127.0.0.1:6381> CLUSTER COUNTKEYSINSLOT 123
(integer) 0
```



**查看 key 对应的槽位**

```sh
CLUSTER KEYSLOT key
# 返回对应的槽位
```

```sh
127.0.0.1:6381> CLUSTER KEYSLOT k1
(integer) 12706
```



**节点成为 Master**

```sh
CLUSTER FAILOVER [FORCE|TAKEOVER]
# FIRCE 强制
# TAKEOVER 接管
```

```sh
127.0.0.1:6382> CLUSTER FAILOVER
OK
127.0.0.1:6382> info replication
# Replication
role:master
connected_slaves:1
slave0:ip=192.168.10.12,port=6385,state=online,offset=37391,lag=0
......
```



**查看节点信息**

```
CLUSTER INFO
```

```sh
127.0.0.1:6381> CLUSTER INFO
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:12
cluster_my_epoch:11
cluster_stats_messages_ping_sent:57205
cluster_stats_messages_pong_sent:83070
cluster_stats_messages_fail_sent:5
cluster_stats_messages_auth-ack_sent:4
cluster_stats_messages_update_sent:2
cluster_stats_messages_sent:140286
cluster_stats_messages_ping_received:58488
cluster_stats_messages_pong_received:61302
cluster_stats_messages_meet_received:6
cluster_stats_messages_fail_received:2
cluster_stats_messages_auth-req_received:4
cluster_stats_messages_update_received:1
cluster_stats_messages_received:119803
total_cluster_links_buffer_limit_exceeded:0
```



**查看集群信息**

```
CLUSTER NODES
```

```sh
127.0.0.1:6381> CLUSTER NODES
da1ccf965889a4874605ce89f489c89ceab3ef4f 192.168.10.11:6383@16383 slave 6b4b0bcec3a687b29273d3f79abb834d4386f2cf 0 1685873121716 9 connected
6b4b0bcec3a687b29273d3f79abb834d4386f2cf 192.168.10.12:6386@16386 master - 0 1685873121611 9 connected 6827-10922
a6719abe0d0639d27dbdb48e267a6c0e0c15e691 192.168.10.10:6382@16382 master - 0 1685873120694 12 connected 12288-16383
e426860fd72c155c6ea02878b3ad4162b7200a8e 192.168.10.12:6385@16385 slave a6719abe0d0639d27dbdb48e267a6c0e0c15e691 0 1685873121000 12 connected
7a74adf8649cc0e0cb3255f8575cf199ec3660d1 192.168.10.11:6384@16384 slave 2c390e90f71a8085915fb47f64e7daca5d4bcae6 0 1685873121508 11 connected
2c390e90f71a8085915fb47f64e7daca5d4bcae6 192.168.10.10:6381@16381 myself,master - 0 1685873120000 11 connected 0-6826 10923-12287
```





### 4.3 redis-cli 命令

| 功能                      | 命令                                                         |
| ------------------------- | ------------------------------------------------------------ |
| 客户端以集群方式访问      | redis-cli -a 密码 -p 端口号 **-c**                           |
| 添加节点到集群            | redis-cli  -a 密码 --cluster **add-node** 新节点IP:prot 引路人IP:prot |
| 从集群中移除节点          | redis-cli  -a 密码 --cluster **del-node** 新节点IP:prot 引路人IP:prot |
| 给 Master 添加 slave 节点 | redis-cli  -a 密码 --cluster **add-node** 新节点IP:prot 引路人IP:Port  **--cluster -slave --cluster-master-id master节点ID** |
| 重新分配节点              | redis-cli -a 密码 --cluster **reshard** IP:prot              |
| 检查集群状态              | redis-cli -a 密码 **check** 节点IP:port                      |





# Reids的Java客户端

![image-20220430172828283](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430172828283.png)





## 1、Jedis

Jedis官方地址：https://github.com/redis/jedis

Jedis的所有方法都是和命令一致的



### 1.1 基本使用

**代码实现**

创建maven工程，并引入依赖

```xml
<dependency>
    <groupId>redis.clients</groupId>
    <artifactId>jedis</artifactId>
    <version>4.2.0</version>
</dependency>
<!--单元测试-->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.7.0</version>
</dependency>
```



**测试**

```java
public class JedisTest {
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        // 1 建立连接
        jedis = new Jedis("------", 6379);
        // 2 设置密码
        jedis.auth("------");
        // 3 选择库
        jedis.select(0);
    }

    @Test
    public void testString(){
        String result = jedis.set("name", "小智");
        System.out.println(result);
        String name = jedis.get("name");
        System.out.println(name);
    }

    @Test
    public void testHash(){
        jedis.hset("user:1", "name", "Jack");
        jedis.hset("user:1", "age", "11");

        // 获取值
        Map<String, String> map = jedis.hgetAll("user:1");
        System.out.println(map);
    }

    @AfterEach
    void tearDown() {
        if (jedis != null) {
            // 释放连接
            jedis.close();
        }
    }
}
```



### 1.2 Jedis连接池

创建JedisPool

```java
public class JedisConnectionFactory {

    private static final JedisPool jedisPool;

    static {
        // 配置连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMaxWaitMillis(1000);
        // 创建连接池对象
        new JedisPool(poolConfig, "-------",
                6379, 1000, "-------");
    }

    public static Jedis getJedis() {
        return jedisPool.getResource();
    }
}
```

然后通过jedisPool获取连接

```java
@BeforeEach
void setUp() {
    jedis = JedisConnectionFactory.getJedis();
}
```





## 2、Spring Data Reids

### 2.1 简介

SpringData是Spring中数据操作的模块，包含对各种数据库的集成，其中对Redis的集成模块就叫做SpringDataRedis，官网地址：https://spring.io/projects/spring-data-redis

-   提供了对不同Redis客户端的整合（Lettuce和Jedis）
-   提供了RedisTemplate统一API来操作Redis
-   支持Redis的发布订阅模型
-   支持Redis哨兵和Redis集群
-   支持基于Lettuce的响应式编程
-   支持基于JDK、JSON、字符串、Spring对象的数据序列化及反序列化
-   支持基于Redis的JDKCollection实现

![image-20220430202129662](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430202129662.png)



### 2.2 连接单机使用

#### 2.2.1 配置使用

引入依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!--连接池-->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

yml配置

```yml
spring:
  application:
    name: redis_cluster

  redis:
    host: ip
    port: 6379
    password: 111111
    lettuce:
      pool:
          max-active: 8
          max-wait: 1ms
          max-idle: 8
          min-idle: 0
```



**测试**

```java
@Autowired
private RedisTemplate redisTemplate;

@Test
void contextLoads() {
    // 写入一条String数据
    redisTemplate.opsForValue().set("name", "haha");
    // 获取数据
    String name = (String) redisTemplate.opsForValue().get("name");
    System.out.println(name);
}
```





#### 2.2.2 的序列化方式

发现问题：我们对对应key的值进行覆盖的时候，会出现不是覆盖内容的情况

写入的内容

![image-20220430210441299](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430210441299.png)

存储到redis中的内容

![image-20220430210526626](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430210526626.png)

这是由于SpringDataRedis使用是JDK 的序列化机制，JDK在底层调用ObjectOutputStream将Object对象转成字节，所以显示出来的就是转换过的

![image-20230604212515739](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230604212515739.png)



**修改序列化方式**

创建一个RedisTemplate替换默认的

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        // 创建RedisTemplate对象
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 设置连接工厂
        template.setConnectionFactory(connectionFactory);
        // 创建JSON序列化工具
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        // 设置key的序列化
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        // 创建value的序列化
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        return template;
    }
}
```

最后测试结果

![image-20220430212708546](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430212708546.png)



#### 2.2.3 序列化一个对象

```java
@Test
public void test(){
    // 写入一个对象
    redisTemplate.opsForValue().set("user:1000", new User("小智", 19));
    // 获取对象
    User user = (User) redisTemplate.opsForValue().get("name:1000");
    System.out.println("user = " + user);
}
```

![image-20220430213127351](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430213127351.png)





### 2.3 连接集群

#### 2.3.1 配置连接

**修改 yml文件**

```yml
spring:
  application:
    name: redis_cluster

  ############## Redis集群 ###############
  redis:
    password: 111111
    # 添加集群信息
    cluster:
      max-redirects: 3
      nodes: 192.168.10.10:6381,192.168.10.10:6382,192.168.10.11:6383,192.168.10.11:6384,192.168.10.12:6385,192.168.10.12:6386 
    lettuce:
      pool:
          max-active: 8
          max-wait: 1ms
          max-idle: 8
          min-idle: 0
```

**测试**

```java
@Test
public void testCluster(){
    redisTemplate.opsForValue().set("k1", "v1");
    String k1 = (String) redisTemplate.opsForValue().get("k1");
    System.out.println(k1);
}
```



#### 2.3.2 动态感应刷新

我们尝试让一台Redis节点down掉，接着 slave 会上位成为Master，此时我们尝试访问Redis集群会发生什么？

首先我们让 6381 节点 down 掉，它的 slave 会上位成为  Master，接着我们使用 Java 客户端访问集群











