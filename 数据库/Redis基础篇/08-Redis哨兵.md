# Redis哨兵(sentinel)

## 1、概述

主从复制在Master挂掉之后从节点只能在原地等候Master节点的回归，损失了系统的高可用，所以我们需要哨兵在Master挂掉了之后从新选一个新的Master来继续提供服务。

哨兵的作用就是无人值守，监控Master节点的运行，当Master出现问题的时候就需要哨兵来选出从节点成为新的Master

![image-20230601161419718](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601161419718.png) 

**提供的功能**：

-   主从监控：监控Master是否正常运行
-   消息通知：哨兵可以将故障转移的结果发送给客户端
-   故障转移：Master异常，选举新的Master来提供服务
-   配置中心：客户端可以通过哨兵来获取当前Master的主机节点地址和端口号





## 2、使用

### 2.1 配置文件详解

sentinel和redis的配置文件是分开的

![image-20230601161634210](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601161634210.png)



sentinel中和redis配置文件同款的配置项

```sh
# 保护模式
protected-mode no
# 端口号
port 26379
# 后台启动
daemonize no
# pid文件位置
pidfile /var/run/redis-sentinel.pid
# 日志文件
logfile ""
# 工作目录
dir /tmp
```



**监听Master的配置项**

```sh
sentinel monitor <master-name> <ip> <redis-port> <quorum>
# sentinel monitor mymaster 127.0.0.1 6379 2		默认配置
```

**`quorum`**配置项：表示最少有几个哨兵认可客观下线同意故障迁移的法定票数。由于网络原因会导致Master和其中某个哨兵的通信中断，这个时候这个哨兵就会认为Master已经挂掉了，但事实上Master并没有挂掉，所以就需要另外的哨兵来进行判断，看一下其他的哨兵是否还能接收到来自Master的心跳，如果超过指定的票数就会进行故障迁移，也就是选取新的Master上线。

**配置Master的连接密码**

```sh
sentinel auth-pass <master-name> <password>
```

**其他配置项**

```sh
sentinel down-after-milliseconds <master-name> <milliseconds>：
# 指定多少毫秒之后，主节点没有应答哨兵，此时哨兵主观上认为主节点下线

sentinel parallel-syncs <master-name> <nums>：
# 表示允许并行同步的slave个数，当Master挂了后，哨兵会选出新的Master，此时，剩余的slave会向新的master发起同步数据

sentinel failover-timeout <master-name> <milliseconds>：
# 故障转移的超时时间，进行故障转移时，如果超过设置的毫秒，表示故障转移失败

sentinel notification-script <master-name> <script-path> ：
# 配置当某一事件发生时所需要执行的脚本

sentinel client-reconfig-script <master-name> <script-path>：
# 客户端重新配置主节点参数脚本
```





### 2.2 实践案例

![image-20230601164016530](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601164016530.png) 

**架构说明**：有三台redis形成主从复制的架构，由于资源有限，Sentinel集群放在同一台机器上

| 机器                                                     |
| -------------------------------------------------------- |
| 192.168.10.10 -> Master  sentinel1、sentinel2、sentinel3 |
| 192.168.10.11 -> slave                                   |
| 192.168.10.12 -> slave                                   |



#### 2.2.1 启动三台sentinel

**编写配置文件**

将配置文件复制到`/myredis`下，将下面的配置复制三份，对应的端口号要进行修改，分别是 26379、26380、26381

```sh
bind 0.0.0.0
daemonize yes
protected-mode no
port 26379
logfile "/myredis/sentinel26379.log"
pidfile /var/run/redis-sentinel26379.pid
dir /myredis
sentinel monitor mymaster 192.168.10.10 6379 2
sentinel auth-pass mymaster 111111
```

![image-20230601164910635](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601164910635.png) 



**启动三台sentinel**

==首先启动三台redis==，再启动sentinel

![image-20230601221644324](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601221644324.png)  

```sh
[root@xdz myredis]# redis-sentinel sentinel26379.conf --sentinel
[root@xdz myredis]# redis-sentinel sentinel26380.conf --sentinel
[root@xdz myredis]# redis-sentinel sentinel26381.conf --sentinel
```

![image-20230601165310892](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601165310892.png) 

启动成功~~~



#### 2.2.2 鸠占鹊巢

将 master 关掉，接着查看两台从机的变化

 ![image-20230601223308880](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601223308880.png)

![image-20230601223344891](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601223344891.png) 

可以看到两台 slave 中的一台变成了master，完成了上位



#### 2.2.3 老master回归

我们将老master重新上线，看一下它会是什么情况

![image-20230601224439163](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601224439163.png) 

可以看到老家伙回归之后只能乖乖的给新老大当小弟了哈哈



#### 2.2.4 查看配置文件变化

sentinel 会动态的修改配置文件，以前是master现在是从节点它的配置文件会发生变化吗？

**老master**

![image-20230601224733104](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601224733104.png) 

发现文件的末尾添加上了这几行配置，也就是说老master再也不能成为master了，除非新的master挂掉了它可以竞争上位

**新master**

![image-20230601225001687](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601225001687.png) 

reids的配置文件的末尾都被sentinel动态添加了配置



**sentinel配置文件**

![image-20230601225412952](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601225412952.png) 

上面是 sentinel 动态添加到它的配置文件中的配置项，其中的 myid 是用来表示 sentinel 节点的唯一id



### 2.3 使用建议

-   哨兵节点的数量应为多个，哨兵本身应该集群，保证高可用
-   哨兵节点的数量应该是奇数
-   各个哨兵节点的配置应一致
-   如果哨兵节点部署在Docker等容器里面，尤其要注意端口的正确映射
-   哨兵集群+主从复制，并不能保证数据零丢失

所以大型企业大多数是**采用集群的方式**来保证数据安全性和高可用







## 3、故障切换和选举原理

### 3.1 SDown主观下线(Subjectively Down)

所谓主观下线（Subjectively Down， 简称 SDOWN）指的是单个Sentinel实例对服务器做出的下线判断，即单个sentinel认为某个服务下线（有可能是接收不到订阅，之间的网络不通等等原因）。主观下线就是说如果服务器在[sentinel down-after-milliseconds]给定的毫秒数之内没有回应PING命令或者返回一个错误消息， 那么这个Sentinel会主观的(单方面的)认为这个master不可以用了，o(╥﹏╥)o

![image-20230601230023619](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601230023619.png) 

```sh
sentinel down-after-milliseconds <masterName> <timeout>
```

 表示master被当前sentinel实例认定为失效的间隔时间，这个配置其实就是进行主观下线的一个依据

master在多长时间内一直没有给Sentine返回有效信息，则认定该master主观下线。也就是说如果多久没联系上redis-servevr，认为这个redis-server进入到失效（SDOWN）状态。



### 3.2 ODwn客观下线(Objectively Down)

客观下线就是在主观下线之后发生的，当主管下线触发后就会进行商议，如果票数满足设置的值那么就会进行新的master选举

![image-20230601230134030](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601230134030.png)

**masterName**是对某个master+slave组合的一个区分标识(一套sentinel可以监听多组master+slave这样的组合)

**quorum这个参数是进行客观下线的一个依据**，法定人数/法定票数

意思是至少有quorum个sentinel认为这个master有故障才会对这个master进行下线以及故障转移。因为有的时候，某个sentinel节点可能因为自身网络原因导致无法连接master，而此时master并没有出现故障，所以这就需要多个sentinel都一致认为该master有问题，才可以进行下一步操作，这就保证了公平性和高可用。



### 3.3 哨兵选取leader

当 master 被判断主观下线后，各个哨兵就开始商议要选举出一个leader，由这个leader来failover（故障迁移）

![image-20230601230513764](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601230513764.png) 



#### 3.3.1 查看 sentinel 日志文件进行分析

-   26379

    ![image-20230601231329438](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601231329438.png) 

-   26380

    ![image-20230601231546371](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601231546371.png)

-   26381

    和80的一样都是给 79 投了一票



#### 3.3.2 哨兵 leader 如何选举出来呢？

**Raft算法 **

![image-20230601230632408](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601230632408.png)

监视该主节点的所有哨兵都有可能被选为领导者，选举使用的算法是Raft算法；Raft算法的基本思路**是先到先得**：谁网络好谁就是有机会

即在一轮选举中，哨兵A向B发送成为领导者的申请，如果B没有同意过其他哨兵，则会同意A成为领导者



### 3.4 选取新master

选举出新的 leader 之后由它来选取新的master



#### 3.4.1 新主登基

某个 slave 被选举成为了 master，接着其他的 slave 要拜这个为老大

**选举当选的规则**

![image-20230601231936341](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601231936341.png) 

1.  优先级 `replica-priority`最高的从节点（数字越小优先级越高）

    ![image-20230601232210537](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230601232210537.png) 

2.  复制偏移量位置 offset 最大的从节点

    可以理解为同步数据最多的那个从节点，因为由于网络的原因会导致多个从节点之间的数据量浮动的情况。

3.  最小的 Run ID 的从节点

    字典顺序，ASCLL码

选举出来的从节点会执行 `slaveif no one`命令成为新的master



#### 3.4.2 群臣俯首

当选出 master 节点之后，剩下的 slave 就要拜这个新的 master 为老大

哨兵 leader 发消息给其他的 slave 让它们拜新 master 为老大



#### 3.4.3 旧主拜服

当老的master从新上线，也要乖乖的认新的master为老大

哨兵 leader 会让原来的 master 降级为 slave 并恢复正常工作





