# Redis持久化

## 1、概述

为什么需要持久化？

Redis的数据是存储在内存中的，内存断点即失，那么如果在没有持久化的情况下，再次重启Redis就会出现缓存为空的情况，那么这个时候缓存时失效的，所有的访问压力就会在MySQL数据库上就会导致MySQL宕掉，为了不让这种情况的发生，我们可以持久化Redis中的数据，宕机之后再次启动我们就可以直接将持久化的数据直接加载到Redis中，省去了重新写入的步骤，让缓存可以立刻生效，但是持久化还是会带来不同的问题？Redis中有两种方式来进行持久化，分别是RDB和AOF

-   RDB：快照的形式保存，恢复的时候读取快照文件进行恢复
-   AOF：每次操作都进行记录，恢复的时候重新执行命令进行恢复

![image-20230515144728613](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515144728613.png)





## 2、RDB(Redis Database)

### 2.1 介绍

RDB（Redis Database）：实现类似照片记录效果的方式，就是把某一时刻的数据和状态以文件的形式写到磁盘上，也就是快照。这样一来即使故障宕机，快照文件也不会丢失，数据的可靠性也就得到了保证。这个快照文件就称为RDB文件(dump.rdb)，其中，RDB就是Redis DataBase的缩写。



### 2.2 基本使用

#### 2.2.1 自动触发

##### ①介绍

要使用RDB需要在配置中进行持久化配置，在Redis配置文件 `` 配置项中进行修改。配置的是自动触发，也就是在某段时间内保存了多少次数据就进行保存



##### ②配置

==要注意不同版本它们的配置文件可能不同，本次使用的版本是7.0.11，可以设置多个规则==

```sh
# 表示的是 在多少秒内修改多少次就保存一次
save <seconds> <changes> [ <seconds> <changes> ...]
```

-   是Reids6.2以下版本的配置文件

    ![image-20230515151627656](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515151627656.png)

-   Reids6.2到Redis7.0的配置文件，可以配置多个规则

    ![image-20230515150305412](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515150305412.png)

-   指定文件名

    ```sh
    dbfilename dump.rdb
    ```

-   指定文件保存的位置

    ```sh
    dir /opt/redis/redis-7.0.11/saveRDB
    ```



##### ③案例

**测试RDB策略是否成功**

-   设置规则

    ```sh
    # 设置保存频率，5秒钟内修改2次就保存一次
    save 5 2
    # 存放文件名
    dbfilename dump.rdb
    # 设置存放位置
    dir /opt/redis/redis-7.0.11/saveRDB/
    ```

-   重新启动Redis

-   进入Redis客户端，五秒内添加或修改两条数据

    ```sh
    127.0.0.1:6379> set k1 v1
    OK
    127.0.0.1:6379> set k2 v2
    OK
    127.0.0.1:6379> set k3 v3
    OK
    127.0.0.1:6379> set k4 v4
    OK
    127.0.0.1:6379> keys *
    1) "k1"
    2) "k4"
    3) "k3"
    4) "k2"
    ```

-   查看是否有生成文件

    ![image-20230515181151330](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515181151330.png)

-   再次5秒内添加2条数据，查看文件大小的变化

    ![image-20230515181330329](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515181330329.png)

    可以看到文件是有变化的，证明它执行了保存动作，nice~~~



**数据恢复测试**

将Redis停止，接着再次启动查看是否有数据，如果有那么证明数据恢复成功，因为RDB会自动找到我们保存的文件，然后将它读入到内存中完成恢复

```sh
[root@localhost saveRDB]# systemctl stop redis.service
[root@localhost saveRDB]# systemctl start redis.service
[root@localhost saveRDB]# redis-cli 
127.0.0.1:6379> keys *
1) "k2"
2) "k3"
3) "k4"
4) "k1"
127.0.0.1:6379>
```

恢复没问题~~~。



#### 2.2.2 手动触发

##### ① 介绍

有时候一些重要的数据我们不想等它执行到对应次数的时候才进行备份，想让它立刻进行备份，这个时候就需要我们手动进行备份



##### ②命令

-   Save：在主程序中执行会阻塞当前Redis服务器，直到它持久化完成，意味着所有的请求Redis此刻都不能进行处理，这在线上环境是非常危险的操作，所以线上是禁用的

-   BGSAVE（默认）：Redis会在后台异步进行持久化从中，不阻塞当前服务器，快照同时还可以响应客户端请求，该触发方式会fork一个进程由子进程复制持久化过程
    ![image-20230515182759845](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515182759845.png)

    fork：在Linux程序中，fork()会产生一个和父进程完全相同的子进程，但子进程在此后多会exec系统调用，出于效率考虑，尽量避免膨胀。

-   LASTSAVE：获取最后一次执行快照的时间



##### ③案例

```sh
127.0.0.1:6379> set k5 v5
OK
127.0.0.1:6379> BGSAVE
Background saving started
```

![image-20230515182953251](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515182953251.png)

完美哈哈~~





### 2.3 优势和劣势

**优势**

![image-20230515184606584](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515184606584.png)

小总结：

-   适合大规模的数据恢复
-   按照业务进行定时备份
-   对数据完整性和一致性要求不高
-   RDB文件在内存中的加载速度要比AOF快得多



**劣势**

![image-20230515184744157](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230515184744157.png)

小总结：

-   在一定间隔时间做一次备份，所以如果Redis意外down掉的话，就会丢失从当前至最近一次快照期间的数据，快照之间的数据丢失

-   内存数据的全量同步，如果数据量太大就会导致I/O严重影响服务器性能

-   RDB依赖于主进程的fork，在更大的数据集中，这可能会导致服务请求的瞬间延迟

    frok的时候内存中的数据被克隆一份，大致2倍的膨胀性，需要考虑



### 2.4如何检查修复rdb文件

```sh
[root@localhost bin]# pwd
/usr/local/bin
[root@localhost bin]# ll
total 21852
-rw-r--r--. 1 root root       92 Nov 11  2022 dump.rdb
-rwxr-xr-x. 1 root root      248 Nov 27 07:47 normalizer
-rwxr-xr-x. 1 root root     2363 Oct 17  2022 pcre-config
-rwxr-xr-x. 1 root root    97816 Oct 17  2022 pcregrep
-rwxr-xr-x. 1 root root   195008 Oct 17  2022 pcretest
-rwxr-xr-x. 1 root root  5205176 May 10 01:23 redis-benchmark
# 检查aof文件
lrwxrwxrwx. 1 root root       12 May 10 01:23 redis-check-aof -> redis-server
# 检查rdb文件
lrwxrwxrwx. 1 root root       12 May 10 01:23 redis-check-rdb -> redis-server
-rwxr-xr-x. 1 root root  5422592 May 10 01:23 redis-cli
lrwxrwxrwx. 1 root root       12 May 10 01:23 redis-sentinel -> redis-server
-rwxr-xr-x. 1 root root 11436968 May 10 01:23 redis-server
# 检验文件
[root@localhost bin]# redis-check-rdb /opt/redis/redis-7.0.11/saveRDB/dump.rdb 
[offset 0] Checking RDB file /opt/redis/redis-7.0.11/saveRDB/dump.rdb
[offset 27] AUX FIELD redis-ver = '7.0.11'
[offset 41] AUX FIELD redis-bits = '64'
[offset 53] AUX FIELD ctime = '1684146533'
[offset 68] AUX FIELD used-mem = '956016'
[offset 80] AUX FIELD aof-base = '0'
[offset 82] Selecting DB ID 0
[offset 129] Checksum OK
[offset 129] \o/ RDB looks OK! \o/
[info] 5 keys read
[info] 0 expires
[info] 0 already expired
```

如果出现了错误不能修复，只能找另外的方式进行修改了！！！



### 2.5 快照配置详解

```sh
################################ SNAPSHOTTING  ################################
# 配置保存的规则
save <seconds> <changes> [ <seconds> <changes> ...]
# 文件名
dbfilename dump.rdb
# 文件保存位置
dir /
# 默认是yes
# 如果配置成no，表示你不在乎数据一致性或者有其他的手段发现和控制这种不一致，那么快照写入失败时，也能确保redis继续接收新的请求
stop-writes-on-bgsave-error yes
# 默认yes
# 对于存储到磁盘中的快照，可以设置是否进行压缩存储。如果是的话，redis会采用LZF算法进行压缩。
# 如果你不想消耗CPU来进行压缩的话，可以设置为关闭此功能
rdbcompression yes
# 默认yes
# 在存储快照后，还可以让redis使用CRC64算法来进行数据校验，但是这样做会增加大约10%的性能消耗，
# 如果希望获取到最大的性能提升，可以关闭此功能，建议开启
rdbchecksum yes
# 在没有持久性的情况下删除复制中使用的RDB文件启用。默认情况下no，此选项是禁用的。
rdb-del-sync-files no
```



### 2.6 触发和禁用RDB快照

什么情况下会触发快照？

-   配置文件中默认的快照配置
-   手动执行 save / bgsave 命令
-   执行 flushdb / flushall 命令也会产生 dump.rdb 文件，但是里面是空的，毫无意义
-   执行shutdown 且没有设置开启AOF持久化
-   主从复制时，主节点自动触发



禁用RDB快照

-   命令方式：

    ```sh
    redis-cli config set save ""
    ```

-   配置方式：

    ```sh
    save ""
    ```

    ![image-20230517082445957](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517082445957.png)













## 3、AOF

### 3.1 概述

AOF（Append Only file）是以日志的形式**记录每个写的操作**，只可以追加文件但不可以修改文件，Redis在重启的时候会将日志中的命令重新执行一遍以达到恢复数据的目的。

在默认情况下，redis是没有开启AOF的，开启AOF需要在配置文件中进行配置。

**AOF的持久化工作流程**

![image-20230517162938864](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517162938864.png)

1.  客户端接收命令的输入
2.  Redis Server不会直接写入到AOF文件中，而是先将命令放入到AOF缓存区中进行保存，这是为了不频繁的去进行IO操作，到达一定量以后再写入磁盘
3.  AOF缓冲会根据AOF缓冲同步文件的三种策略将命令写入磁盘上的AOF文件
4.  由于AOF只需追加所以多次修改的内容会使文件膨胀，所以AOF到达一定的量就会进行命令的合并（又称AOF重写），从而起到减少恢复加载的时间和效率。
5.  Redis服务器重启的时候会从AOF文件中载入数据用来恢复

**写回策略**

![image-20230517165203381](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517165203381.png)

| 配置项           | 功能                                                         |
| ---------------- | ------------------------------------------------------------ |
| Always           | 同步写回，每个命令执行完成立刻同步地将日志写回磁盘           |
| Everysec（默认） | 每秒写回，每个写命令执行完，只是先把日志写到AOF文件的内存缓冲区，每隔1秒把缓冲区中的内容写入磁盘 |
| No               | 操作系统控制的写回，每个命令执行完成，只是先把日志写到AOF文件的内容缓冲区，由操作系统决定何时将缓冲区内容写回磁盘 |

![image-20230517164413096](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517164413096.png)



### 3.2 配置项

这里列举常用的配置项

```sh
# 开启AOF，默认是关闭
appendonly no

# 保存的aof的文件名
appendfilename "appendonly.aof"

# 在快照配置的dir配置项所在的文件下再创建一个文件夹，redis7和6的区别，因为reids7它的aof文件变成了三个（重大变化）
# appendonly.aof.1.base.rdb		base：基本文件，重写的时候会重写到这个文件中
# appendonly.aof.1.incr.aof		incr：追加文件，记录所有的写命令
# appendonly.aof.manifest	    manifest：清单文件
appenddirname "appendonlydir"

# 三种文件同步方式
appendfsync always
appendfsync everysec
appendfsync no

# aof重写期间是否同步，默认为no
no-appendfsync-on-rewrite no

# 重写触发配置、文件重写策略
# 当前的AOF文件的大小相较于上次文件的大小增长了100%
auto-aof-rewrite-percentage 100
# 当文件达到最小64mb时将文件进行合并重写
auto-aof-rewrite-min-size 64mb
```



### 3.3 案例

#### 3.3.1 正常恢复

1.  启动aof，修改配置文件

2.  新生成了dump.rdb和aof文件夹

    ```sh
    [root@localhost saveRDB]# ll
    total 4
    drwxr-xr-x. 2 root root 103 May 17 01:57 appendonlydir
    -rw-r--r--. 1 root root 108 May 17 02:29 dump.rdb
    [root@localhost appendonlydir]# cd appendonlydir
    [root@localhost appendonlydir]# ll
    total 12
    -rw-r--r--. 1 root root  89 May 17 01:57 appendonly.aof.1.base.rdb
    -rw-r--r--. 1 root root 0 May 17 02:29 appendonly.aof.1.incr.aof
    -rw-r--r--. 1 root root  88 May 17 01:57 appendonly.aof.manifest
    ```

    可以看到 incr文件刚创建是为0的，因为此时还没有数据执行命令

3.  启动redis，输入命令添加几条数据，再次查看aof文件夹变化

    ```sh
    [root@localhost appendonlydir]# ll                                                        
    total 12                                                                                  
    -rw-r--r--. 1 root root  89 May 17 01:57 appendonly.aof.1.base.rdb      
    # 可以看到文件大小变化了
    -rw-r--r--. 1 root root 110 May 17 02:29 appendonly.aof.1.incr.aof                        
    -rw-r--r--. 1 root root  88 May 17 01:57 appendonly.aof.manifest   
    ```

4.  查看 `incr`文件内容是怎样的结构

    ```sh
    [root@localhost appendonlydir]# cat appendonly.aof.1.incr.aof 
    *2
    $6
    SELECT
    $1
    0
    *3
    $3
    set
    $2
    k1
    $2
    v1
    *3
    $3
    set
    $2
    k2
    $2
    v2
    *3
    $3
    set
    $2
    k3
    $2
    v3
    ```

    可以看到命令被记录下来了

5.  停止redis并将对应文件夹下的 ` .rdb`文件删除，然后再次启动查看数据是否重新写入

    ```sh
    [root@localhost saveRDB]# redis-cli                                                       
    127.0.0.1:6379> keys *                                                                    
    1) "k2"                                                                                   
    2) "k3"                                                                                   
    3) "k1"  
    # 没问题，很nice~~~
    ```

    

#### 3.3.2 异常恢复

**问题**：redis执行持久化操作的时候意外down机了，此时数据出现不完整，该如何解决呢？

**解决**：使用 `/usr/local/bin`下的 `redis-check-aof --fix` 命令来修复

![image-20230517174058570](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517174058570.png)

**模拟情景**

1.  停止redis服务器，将 `.incr` 文件的数据进行修改

    ![image-20230517174308912](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517174308912.png)

2.  再次启动redis发现报错

    ![image-20230517174433684](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517174433684.png)

3.  修复文件

    ```sh
    [root@localhost bin]# redis-check-aof --fix /opt/redis/redis-7.0.11/saveRDB/appendonlydir/appendonly.aof.1.incr.aof
    Start checking Old-Style AOF
    0x              6a: Expected \r\n, got: 3432
    AOF analyzed: filename=/opt/redis/redis-7.0.11/saveRDB/appendonlydir/appendonly.aof.1.incr.aof, size=136, ok_up_to=81, ok_up_to_line=26, diff=55
    This will shrink the AOF /opt/redis/redis-7.0.11/saveRDB/appendonlydir/appendonly.aof.1.incr.aof from 136 bytes, with 55 bytes, to 81 bytes
    Continue? [y/N]: y
    Successfully truncated AOF /opt/redis/redis-7.0.11/saveRDB/appendonlydir/appendonly.aof.1.incr.aof
    ```

    查看文件情况

    ![image-20230517174711947](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517174711947.png)

    发现k3的值被删除了

4.  再次启动就启动成功了~~~

此案例中我们发现我们可以手动修改`.incr`文件，也就是说当我们错误使用`flushall`等删库命令的时候只需要将`.incr`文件中的命令删除一下即可

```sh
127.0.0.1:6379> keys *
1) "k2"
2) "k1"
127.0.0.1:6379> FLUSHDB
OK
```

停止redis服务器，接着找到`.incr`中的flushdb命令进行删除，再次重启就可以将数据进行恢复了



### 3.4 优势和劣势

优势：更好的保护数据不丢失，性能高，可做紧急恢复

劣势：

-   同样的数据量，占用空间比rdb的要大，恢复速度比rdb的要慢
-   aof运行效率要满于rdb，每秒同步策略效率较好，不同步频率和rdb相同



### 3.5 AOF重写机制

#### 3.5.1 概述

AOF的运行原理是将命令追加到 `.incr` 文件中，它不会进行替换，那么我们多次设置同一个值就会占有多次修改的空间，这是不必要的，所以AOF有一个重写机制来将这些重复修改的命令进行合并，我们称这个操作为重写机制

**重写机制原理**

1.  在重写开始前，redis会创建一个“重写子进程”，这个子进程会读取现有的AOF文件，并将其包含的指令进行分析压缩并写入到一个临时文件中。
2.  与此同时，主进程会将新接收到的写指令一边累积到内存缓冲区中，一边继续写入到原有的AOF文件中，这样做是保证原有的AOF文件的可用性，避免在重写过程中出现意外。
3.  当“重写子进程”完成重写工作后，它会给父进程发一个信号，父进程收到信号后就会将内存中缓存的写指令追加到新AOF文件中
4.  当追加结束后，redis就会用新AOF文件来代替旧AOF文件，之后再有新的写指令，就都会追加到新的AOF文件中
5.  重写aof文件的操作，并没有读取旧的aof文件，而是将整个内存中的数据库内容用命令的方式重写了一个新的aof文件，这点和快照有点类似



#### 3.5.2 触发机制

**自动触发**

需要进行对应的配置，在redis中的默认配置

![image-20230517183701000](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517183701000.png)

表示的是在 当文件大小比上次重写的文件大 百分值一百 倍后且文件大于 64M时就进行重写



**手动触发**

客户端向服务器发送 `bgrewriteaof` 命令来手动触发重写机制



#### 3.5.3 案例

需求说明：将配置项规则改为

```sh
# 关闭混合，设置为no
aof-use-rdb-preamble no

# 设置重写规则
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 1k
```

接着多次对一个值进行修改，当 `.incr` 文件达到对应大小是，看是否有触发重写机制

![image-20230517185209101](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517185209101.png)

此时的`.incr`文件大小

![image-20230517184547722](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517184547722.png)

接着疯狂添加数据

![image-20230517185949491](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517185949491.png)

观察发现，当超过1k的数据就会进行文件的重写，因为只对一个数据进行修改，所以数据量时一样的

![image-20230517185827316](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230517185827316.png)

完美~~~~





## 4、RDB和AOF混合持久化

### 4.1 两种模式同时开启

**问题**：RDB和AOF是否可以共存，如果可以那么先后顺序是怎样的？

**回答**：是可以共存的，在两者都开启的情况下，重启时只会加载aof文件，不会加载rdb文件

![image-20230518145814549](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230518145814549.png)

同时开启两种持久化模式，Redis会优先加载AOF文件来做恢复，AOF的数据完整性是比RDB的要好的。既然AOF的数据完整性更好，为什么还要开启RDB呢，因为AOF文件在不断的变化，RDB是不变的更适合做备份。



### 4.2 混合模式（推荐）

混合模式就是RDB+AOF，RDB负责全量数据保存，AOF负责增量数据保存，当**重写策略被触发或者手动触发重写策略**的时候首先会将所有的数据进行全量保存，于此同时还会接收新的命令保存到AOF文件中，重写完成之后就会删除原有的AOF文件，生成新的AOF文件，这个时候的AOF文件包含两部分，**一部分是RDB格式，一部分是AOF格式**。

![image-20230518161554640](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230518161554640.png)

这样做的好处就是在RDB被触发的区间内也能进行数据持久化，down机之后也能保证数据的完整性，由于前面的数据都是使用RDB进行存储的，所以恢复的时间也有所优化。后面的数据是AOF进行保存的，所以也可以保持数据的完整性。推荐使用~~~

**开启混合模式**

```sh
aof-use-rdb-preamble yes
```







## 5、纯缓存模式Only

开启持久化模式会消耗一部分的性能，为了使 Redis 性能达到极致我们可以使用纯缓存模式，需要持久化的时候可以使用命令的方式进行持久化

-   关闭RDB

    ```sh
    save ""
    ```

-   关闭AOF

    ```sh
    appendonly no
    ```







