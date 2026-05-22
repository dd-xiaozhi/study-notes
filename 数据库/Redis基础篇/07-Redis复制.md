# Redis复制(replica)

## 1、概述

Redis支持主从复制的高可用架构和故障转移，当主节点down掉的时候，从节点可以补上，所以这个模式提高了高可用性，不至于在主机down掉了之后就整个服务就不可用了。主从复制的模式还可以是主负责写入，从节点负责读操作，这样的话就可以分担服务的压力。

![image-20230519164225451](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230519164225451.png)

可以看到主节点会同步数据给从节点，从而达到复制的效果

**它可以**：

-   读写分离
-   容灾恢复
-   数据备份
-   水平扩容支撑高并发



## 2、使用

这里我们用 master 表示主节点，v表示从节点



### 2.1 配置

要使用主从复制只需要配置从库，主库是不用动的，配置从库来实现主从复制

```sh
# 指定master的IP和端口号
replicaof 主库IP 端口号

# 权限校验 
# master如果配置了密码登录，那么就需要从节点设置校验密码。否则master会拒绝 slave的 访问 
masterauth xxxxxx
```



### 2.2 基本操作命令

| 命令                    | 作用                                                        |
| ----------------------- | ----------------------------------------------------------- |
| info replication        | 查看复制节点的主从关系和配置信息                            |
| replicaof 主库IP 端口号 | 指定主库的ip和端口号（一般在配置文件中写入）                |
| slaveof 主库IP 端口号   | 每次与master 断开之后需要重新链接，除非配置进redis.conf文件 |
| slaveof no one          | 当前从库转为主库，自立山头                                  |





## 3、实战演练

**配置说明**：6379为主机，6380和6381为从机

![image-20230531171103999](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531171103999.png)





### 3.1 一主二从

![image-20230531171103999](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531171103999.png)



#### 3.1.1 配置文件指定

①复制一个新的redis.conf到`/myredis`目录下

②修改redis.conf的配置

```sh
# 开启daemonize yes 后台运行
# 注释掉bind 127.0.0.1. 
# 设置protected-mode为no
# 指定端口
# 指定当前工作目录，dir
dir /myredis
# log文件名字,logfile
logfile /myredis/redis.log
# requirepass
requirepass 111111
```

③复制配置文件到从机上并进行对应修改

```sh
# 修改端口号 -> 对应不同的redis定义对应不同的端口号
# # 指定master的IP和端口号
replicaof 192.168.10.10 6379

# master如果配置了密码登录，那么就需要从节点设置校验密码。否则master会拒绝 slave的 访问 
masterauth 111111
```

④测试 -> 先启动主机，再启动从机

⑤查看主从节点信息

-   日志文件

    -   主节点

        ![image-20230531165156839](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531165156839.png)

    -   从节点

        ![image-20230531170055793](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531170055793.png)

-   命令查看 -> 使用 `INFO replication` 进行查看

    -   主节点

        ![image-20230531164535854](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531164535854.png)

    -   从节点

        ![image-20230531164608610](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531164608610.png)

能看到对应的消息就表示成功开启了主从复制~~~~

⑥测试

在主节点上添加数据，查看从节点是否已经同步对应数据

```sh
# master节点
127.0.0.1:6379> mset k1 v1 k2 v2
OK
127.0.0.1:6379> keys *
1) "k2"
2) "k1"

# slave节点
127.0.0.1:6380> keys *
1) "k1"
2) "k2"

127.0.0.1:6381> keys *
1) "k1"
2) "k2"
```

没有问题，完美~~~



#### 3.1.2 主从节点问题

**从节点是否可以执行命令？**是不可以的

![image-20230531165441624](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531165441624.png)



**从机切入点问题**

从节点down掉了，主机继续添加数据，那么从节点再次上线之后是否会同步数据呢？

是会的，首先会全量加载数据，接着就是增量同步数据

```sh
# 首先关闭 slave2
127.0.0.1:6381> SHUTDOWN

# master执行命令
127.0.0.1:6379> mset k4 v4 k5 v5
OK

# 再次启动slave2
127.0.0.1:6381> get k4
"v4"
127.0.0.1:6381> get k5
"v5"
```

结果发现，即使掉队了重新上线之后数据也是会同步过来的



**主机down掉之后从机会上位吗？**

```sh
# 关闭master
127.0.0.1:6379> SHUTDOWN

# 查看从节点信息
127.0.0.1:6380> info replication
role:slave
master_host:192.168.10.10
master_port:6379
master_link_status:down
......
```

可以发现从节点还是slave，而master显示已经down掉，所以从节点默认是不会在master节点down掉上位的



**主机shutdown后，重启后主从关系还在吗? 从机还能否顺利复制?** 

刚才关闭了master，此时再次启动查看状态信息

```sh
127.0.0.1:6380> info replication
role:slave
master_host:192.168.10.10
master_port:6379
master_link_status:up

# master添加数据
127.0.0.1:6379> set k6 v6
OK

# slave1查看数据
127.0.0.1:6380> get k6
"v6"
```

可以看到主从节点重新恢复了，从机也能顺利复制



**某台从机down后，master继续，从机重启后它能跟上大部队吗?**

是可以的



#### 3.1.3 命令手动指定

首先将从节点 slave1 和 slave2 设置的配置去掉

![image-20230531215637294](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531215637294.png)

master密码得留着，进行注册的时候需要这个配置

接着再次启动查看节点信息

```sh
127.0.0.1:6380> info replication
role:master
connected_slaves:0
......
```

可以发现两个从节点都变成了master，因为我们没有进行配置。

接下来我们使用命令来让它们成为从节点

```sh
127.0.0.1:6380> SLAVEOF 192.168.10.10 6379
OK
127.0.0.1:6380> info replication
role:slave
master_host:192.168.10.10
master_port:6379
master_link_status:up
......
```

可以看到身份发生了变化，其他的从节点也是这样使用命令设置即可

**注意**：从机重启之后关系就不在了，需要再次命令建立关系





### 3.2 薪火相传

由于从节点的数量会给master同步数据的压力，所以我们需要由 slave 来同步给其他的 slave，从而减轻master同步数据的压力，这就是传说中的传纸条~~~

![image-20230531221108454](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531221108454.png)

我们需要实现上图的关系连接，使用`slaveof 主机号 端口号` 来让 `slave2`改变门户，拜`slave1`为老大

```sh
# 此时我们是一主二从
127.0.0.1:6379> info replication
role:master
connected_slaves:2	# 可以看到有两个从节点

# 让slave2拜slave1为老大
127.0.0.1:6381> SLAVEOF 192.168.10.11 6380
OK
# 查看slave2信息
127.0.0.1:6381> INFO replication
role:slave
master_host:192.168.10.11	# 拜码头成功
master_port:6380


# 查看 slave1 的节点信息
127.0.0.1:6380> info replication
role:slave
master_host:192.168.10.10
master_port:6379
master_link_status:up
......
connected_slaves:1
slave0:ip=192.168.10.12,port=6381,state=online,offset=29568,lag=0
......
```

 可以看到 `slave1` 中又一个从节点，接着我们测试一下数据是否有进行同步

```sh
# master设置
127.0.0.1:6379> set test 11
OK

# slave1查看
127.0.0.1:6380> get test
"11"

# slave2查看
127.0.0.1:6381> get test
"11"
```

可以看到数据是已经进行同步了的，成功~~~





### 3.3 反客为主

让从节点重新变成主节点，执行下面命令或者删除复制的配置

```sh
# 方式一：注释或删除配置
# replicaof 192.168.10.10 6379

# 方式二：使用命令 -> SLAVEOF no one
127.0.0.1:6380> SLAVEOF no one
OK
127.0.0.1:6380> info replication
role:master
connected_slaves:1
......
```





## 4、工作原理和流程

1.  slave启动，同步初始

    slave启动成功连接master后会发送一个sync命令

2.  首次连接，全量复制

    主节点收到SYNC命令后，开始执行BGSAVE命令生成RDB文件并使用缓冲区记录所有接收到的写命令，接着从节点收到RDB文件后将它加载进内存，完成复制初始化

3.  心跳持续，持续通信

    ```sh
    # master发出PING包的周期，默认是10秒
    repl-ping-replica-period 10
    ```

4.  进入平稳期，增量复制

    主节点继续将接受到的命令同步给从节点

5.  从机下线，重连续传

    如果主节点重新启动，从节点将向主节点发送PSYNC命令，主节点将检查从节点的复制偏移量，并决定是使用部分重同步还是完全重同步的方式向从节点同步数据，类似于断点续传





## 5、复制的缺点

**复制延迟，信号衰减**

由于所有的写操作都是先在Master上操作，然后同步更新到Slave上，所以从Master同步到Slave机器有一定的延迟，当系统很繁忙的时候，延迟问题会更加严重，Slave机器数量的增加也会使这个问题更加严重。

![image-20230531224952579](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230531224952579.png)



**master挂掉需要人工干预**

master挂掉之后从节点是不会自己上位的，所以当master挂掉之后就需要人工手动的将 slave 节点设置为 master，这个时间段整个服务是不可用的，人工手动修改也是比较麻烦的事情，所以redis有哨兵和集群两种方式来提高高可用性





