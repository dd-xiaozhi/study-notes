# reids数据结构

## 1、Redis 数据结构介绍 

十大数据结构

![image-20230509170057512](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230509170057512.png)



## 2、通用命令 

通用指令是部分数据类型的，都可以使用的指令，常见的有：

-   KEYS：查看符合模板的所有key，例如keys * 是查看所有的key
-   DEL：删除一个指定的key
-   unlink key：非阻塞删除（异步删除）
-   EXISTS：判断key是否存在
-   EXPIRE：给一个key设置有效期，有效期到期时该key会被自动删除
-   TTL：查看一个KEY的剩余有效期
-   help：可以查看对应命令的使用
-   Type：查看对应值的存储类型
-   select db：选择数据库，类似于mysql的`use db`
-   move key db：移动对应key到指定数据库
-   dbsize：查看当前数据库key的数量
-   flushdb：清空当前库
-   flushall：通杀全部库

通过help [command] 可以查看一个命令的具体用法，例如：

![image-20220430144429579](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/\image-20220430144429579.png)

==**注意**：命令是不区分大小写的，但是KEY区分大小写==



## String(字符串)

### ①介绍

String类型，也就是字符串类型，是Redis中最简单的存储类型。
其value是字符串，不过根据字符串的格式不同，又可以分为3类：

-   string：普通字符串
-   int：整数类型，可以做自增、自减操作
-   float：浮点类型，可以做自增、自减操作

**注意**：不管是哪种格式，底层都是字节数组形式存储，只不过是编码方式不同。字符串类型的<span style="color: red">最大空间不能超过512m</span>.

![image-20220430144715775](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/\image-20220430144715775.png)



### ②String常见命令

-   SET：添加或者修改已经存在的一个String类型的键值对
-   GET：根据key获取String类型的value
-   MSET：批量添加多个String类型的键值对
-   MGET：根据多个key获取多个String类型的value
-   INCR：让一个整型的key自增1
-   INCRBY:让一个整型的key自增并指定步长，例如：incrby num 2 让num值自增2
-   INCRBYFLOAT：让一个浮点类型的数字自增并指定步长
-   SETNX：添加一个String类型的键值对，前提是这个key不存在，否则不执行
-   SETEX：添加一个String类型的键值对，并且指定有效期





### ③区分不同的key

![image-20220430155948287](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430155948287.png)









## Hash (哈希表)

![image-20220430160456429](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430160456429.png)

**Hash的常见命令有**：

-   HSET key field value：添加或者修改hash类型key的field的值
-   HGET key field：获取一个hash类型key的field的值
-   HMSET：批量添加多个hash类型key的field的值
-   HMGET：批量获取多个hash类型key的field的值
-   HGETALL：获取一个hash类型的key中的所有的field和value
-   HKEYS：获取一个hash类型的key中的所有的field
-   HVALS：获取一个hash类型的key中的所有的value
-   HINCRBY:让一个hash类型key的字段值自增并指定步长
-   HSETNX：添加一个hash类型的key的field值，前提是这个field不存在，否则不执行

![image-20220430161255404](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/image-20220430161255404.png)





## List (列表)

Redis中的List类型与Java中的LinkedList类似，可以看做是一个双向链表结构。既可以支持正向检索和也可以支持反向检索。
特征也与LinkedList类似：

-   有序
-   元素可以重复
-   插入和删除快
-   查询速度一般

常用来存储一个有序数据，例如：朋友圈点赞列表，评论列表等。



**List的常见命令有**：

-   LPUSH key  element ... ：向列表左侧插入一个或多个元素
-   LPOP key：移除并返回列表左侧的第一个元素，没有则返回nil
-   RPUSH key  element ... ：向列表右侧插入一个或多个元素
-   RPOP key：移除并返回列表右侧的第一个元素
-   LRANGE key star end：返回一段角标范围内的所有元素
-   BLPOP和BRPOP：与LPOP和RPOP类似，只不过在没有元素时等待指定时间，而不是直接返回nil（阻塞模式）



**思考**：

-   如何利用List结构模拟一个栈?

    入口和出口在同一边

-   如何利用List结构模拟一个队列?

    入口和出口在不同边

-   如何利用List结构模拟一个阻塞队列?

    -   入口和出口在不同边
    -   出队时采用BLPOP或BRPOP





## Set (集合)

Redis的Set结构与Java中的HashSet类似，可以看做是一个value为null的HashMap。因为也是一个hash表，因此具备与HashSet类似的特征：

-   无序
-   元素不可重复
-   查找快
-   支持交集、并集、差集等功能



**Set常见命令**

Set的常见命令有：

-   SADD key member ... ：向set中添加一个或多个元素
-   SREM key member ... : 移除set中的指定元素
-   SCARD key： 返回set中元素的个数
-   SISMEMBER key member：判断一个元素是否存在于set中
-   SMEMBERS：获取set中的所有元素
-   SINTER key1 key2 ... ：求key1与key2的交集
-   SDIFF key1 key2 ... ：求key1与key2的差集
-   SUNION key1 key2 ..：求key1和key2的并集



**练习**

将下列数据用Redis的Set集合来存储：

```sh
# 张三的好友有：李四、王五、赵六
127.0.0.1:6379> Sadd zhangsan lisi wangwu zhaoliu
# 李四的好友有：王五、麻子、二狗
127.0.0.1:6379> Sadd lisi wangwu mazi ergou
```

利用Set的命令实现下列功能：

```sh
#  计算张三的好友有几人
127.0.0.1:6379> Scard zhangsan
#  计算张三和李四有哪些共同好友
127.0.0.1:6379> Sinter zhangsan lisi
#   查询哪些人是张三的好友却不是李四的好友
127.0.0.1:6379> SDIFF zhangsan lisi
1) "lisi"
2) "zhaoliu"
#   查询张三和李四的好友总共有哪些人
127.0.0.1:6379> Sunion zhangsan lisi
1) "zhaoliu"
2) "wangwu"
3) "lisi"
4) "mazi"
5) "ergou"
#   判断李四是否是张三的好友
127.0.0.1:6379> SISMEMBER lisi "zhangsan"
(integer) 0
#   判断张三是否是李四的好友
127.0.0.1:6379> SISMEMBER zhangsan "lisi"
(integer) 1
#   将李四从张三的好友列表中移除
127.0.0.1:6379> SREM zhangsan lisi
```





## ZSet(有序集合)

Redis的SortedSet是一个可排序的set集合，与Java中的TreeSet有些类似，但底层数据结构却差别很大。SortedSet中的每一个元素都带有一个score属性，可以基于score属性对元素排序，底层的实现是一个跳表（SkipList）加 hash表。
SortedSet具备下列特性：

-   可排序
-   元素不重复
-   查询速度快
-   因为SortedSet的可排序特性，经常被用来实现排行榜这样的功能



**SortedSet类型的常见命令**

1.  ZADD key score member：添加一个或多个元素到sorted set ，如果已经存在则更新其score值
2.  ZREM key member：删除sorted set中的一个指定元素
3.  ZSCORE key member : 获取sorted set中的指定元素的score值
4.  ZRANK key member：获取sorted set 中的指定元素的排名
5.  ZCARD key：获取sorted set中的元素个数
6.  ZCOUNT key min max：统计score值在给定范围内的所有元素的个数
7.  ZINCRBY key increment member：让sorted set中的指定元素自增，步长为指定的increment值
8.  ZRANGE key min max：按照score排序后，获取指定排名范围内的元素
9.  ZRANGEBYSCORE key min max：按照score排序后，获取指定score范围内的元素
10.  ZDIFF、ZINTER、ZUNION：求差集、交集、并集

**注意**：所有的排名默认都是升序，如果要降序则在命令的Z后面添加REV即可



**练习**：

将班级的下列学生得分存入Redis的SortedSet中：
Jack 85, Lucy 89, Rose 82, Tom 95, Jerry 78, Amy 92, Miles 76

```sh
127.0.0.1:6379> ZADD score 85 "Jack" 89 "Lucy" 82 "Rose" 95 "Tom" 78 "Jerry" 92 "Amy" 76 "Milles"
```

并实现下列功能：

```sh
# 删除Tom同学
127.0.0.1:6379> ZREM score "Tom"
# 获取Amy同学的分数
127.0.0.1:6379> ZSCORE score "Amy"
# 获取Rose同学的排名，从0开始的
127.0.0.1:6379> ZREVRANK score "Rose"
# 查询80分以下有几个学生
127.0.0.1:6379> ZCOUNT score 0 80
# 给Amy同学加2分
127.0.0.1:6379> ZINCRBY score 2 "Amy"
# 查出成绩前3名的同学
127.0.0.1:6379> ZREVRANGE score 0 2
# 查出成绩80分以下的所有同学
127.0.0.1:6379> ZREVRANGEBYSCORE score 80 0
```





## GEO(地理空间)

### 8.1 介绍

移动互联网时代LBS应用越来越多，交友软件中附近的小姐姐、外卖软件中附近的美食店铺、高德地图附近的核酸检查点等等，那这种附近各种形形色色的XXX地址位置选择是如何实现的？

地球上的地理位置是使用二维的经纬度表示，经度范围 (-180, 180]，纬度范围 (-90, 90]，只要我们确定一个点的经纬度就可以名取得他在地球的位置。

例如滴滴打车，最直观的操作就是实时记录更新各个车的位置，然后当我们要找车时，在数据库中查找距离我们(坐标x0,y0)附近r公里范围内部的车辆

**使用关系型数据库解决？**

```sql
select taxi from position where x0-r < x < x0 + r and y0-r < y < y0+r
```

但是这样会有什么问题呢？

1.查询性能问题，如果并发高，数据量大这种查询是要搞垮数据库的

2.这个查询的是一个矩形访问，而不是以我为中心r公里为半径的圆形访问。

3.精准度的问题，我们知道地球不是平面坐标系，而是一个圆球，这种矩形计算在长距离计算时会有很大误差



### 8.2 常用命令

-   GEOADD key longitude latitude member [longitude latitude member ...\]添加一个或多个地理空间位置到sorted set

-   GEOHASH key member [member ...\] 返回一个标准的地理空间的Geohash字符串

    GEOHASH它会将地理空间使用base32进行编码，这样我们就可以通过这个字符串作为唯一标识

-   GEOPOS key member [member ...\] 返回地理空间的经纬度

-   GEODIST key member1 member2 [unit\] 返回两个地理空间之间的距离

-   GEORADIUS key longitude latitude radius m|km|ft|mi [WITHCOORD\] [WITHDIST] [WITHHASH] [COUNT count] 查询指定半径内所有的地理空间元素的集合。

    -   WITHDIST: 在返回位置元素的同时， 将位置元素与中心之间的距离也一并返回。 距离的单位和用户给定的范围单位保持一致。
    -   WITHCOORD: 将位置元素的经度和维度也一并返回。
    -   WITHHASH: 以 52 位有符号整数的形式， 返回位置元素经过原始 geohash 编码的有序集合分值。 这个选项主要用于底层应用或者调试， 实际中的作用并不大
    -   COUNT 限定返回的记录数。
    -   ASC | DESC 正序或者反序

-   GEORADIUSBYMEMBER key member radius m|km|ft|mi [WITHCOORD\] [WITHDIST] [WITHHASH] [COUNT count]查询指定半径内匹配到的最大距离的一个地理空间元素。



### 8.3 实现案例

-   计算两点之间的距离

    ```sh
    127.0.0.1:6379> GEOADD city 116.403963 39.915119 "天安门" 116.403414 39.924091 "故宫" 116.024067 40.362639 "长城"
    (integer) 3
    127.0.0.1:6379> GEOPOS city 天安门 故宫 长城
    1) 1) "116.40396326780319214"
       2) "39.91511970338637383"
    2) 1) "116.40341609716415405"
       2) "39.92409008156928252"
    3) 1) "116.02406591176986694"
       2) "40.36263993239462167"
    # 最后面的是单位表示
    127.0.0.1:6379> GEODIST city 天安门 故宫 km
    "0.9988"
    127.0.0.1:6379> GEODIST city 天安门 故宫 m
    "998.8332"
    ```

-   查看10KM内的地点

    默认客户端中文会乱码，需要指定一下编码

    ```sh
    127.0.0.1:6379> GEORADIUS city 116.418017 39.914402 10 km withdist withcoord count 10 withhash desc
    1) 1) "\xe6\x95\x85\xe5\xae\xab"
       2) "1.6470"
       3) (integer) 4069885568908290
       4) 1) "116.40341609716415405"
          2) "39.92409008156928252"
    2) 1) "\xe5\xa4\xa9\xe5\xae\x89\xe9\x97\xa8"
       2) "1.2016"
       3) (integer) 4069885555089531
       4) 1) "116.40396326780319214"
          2) "39.91511970338637383"
    # 解决中文乱码
    [root@localhost ~]# redis-cli --raw
    
    # 查询北京王府井10km内的地点，
    127.0.0.1:6379> GEORADIUS city 116.418017 39.914402 10 km withdist withcoord count 10 withhash desc
    故宫
    1.6470
    4069885568908290
    116.40341609716415405
    39.92409008156928252
    天安门
    1.2016
    4069885555089531
    116.40396326780319214
    39.91511970338637383
    
    # 指定成员为中心查询
    127.0.0.1:6379> GEORADIUSBYMEMBER city 天安门 10 km withdist withcoord count 10 withhash asc
    天安门
    0.0000
    4069885555089531
    116.40396326780319214
    39.91511970338637383
    故宫
    0.9988
    4069885568908290
    116.40341609716415405
    39.92409008156928252
    ```

    





## HyperLogLog(基数统计)

### 9.1 介绍

基数：是一种数据集，去重后的真实个数

![image-20230504172715475](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230504172715475.png)

**可以做？**

-   统计某个网站的UV
-   统计某个文章的UV
-   ......



### 9.2 常用命令

-   PFADD key element [element...]：添加指定元素，元素是一个字符串
-   PFCOUNT key [key...]：返回给定的基数估算值，这个值会有0.8%的误差
-   PFMERGE deslkey sourcekey [sourcekey ...]：将多个HyperLogLog合并成一个HyperLogLog



### 9.3 案例实现

-   统计网站的访问人数

    ```sh
    127.0.0.1:6379> PFADD vis:ip 10.10.10.10 10.10.10.11 10.10.10.12 10.10.10.10
    (integer) 1
    127.0.0.1:6379> PFCOUNT vis:ip
    (integer) 3
    ```

-   统计单个用户的多篇文章的访问量

    ```sh
    127.0.0.1:6379> PFADD blog:2349523 10.10.10.10 10.10.10.11 10.10.10.12 10.10.10.10
    (integer) 1
    127.0.0.1:6379> PFADD blog:2349524 10.10.10.10 10.10.10.11 10.10.10.12 10.10.10.10 10.23.23.10 10.10.10.10
    (integer) 1
    127.0.0.1:6379> PFMERGE temp blog:2349523 blog:2349524
    OK
    127.0.0.1:6379> PFCOUNT temp
    (integer) 4
    127.0.0.1:6379>
    ```

    





## BitMap(位图)

### 10.1 介绍

由0和状态表示的二进制位的bit数组

![image-20230504165600764](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230504165600764.png)

说明：用String类型作为底层数据结构实现的一种统计二值状态的数据类型

位图本质是数组，它是基于String数据类型的按位的操作。该数组由多个二进制位组成，每个二进制位都对应一个偏移量(我们称之为一个索引)。

 Bitmap支持的最大位数是2^32位，它可以极大的节约存储空间，使用512M内存就可以存储多达<span style="color: red;">42.9亿的字节信息(2^32 = 4294967296)</span>



**能干嘛？**

可以用于状态统计，比如：

-   用户是否登录过
-   电影、广告是否被点击播放过
-   钉钉打卡上班，签到统计
-   ......



### 10.2 常用命令

-   setbit key offset value：根据偏移量设置值
-   getbit key offset：获取key对应偏移量的值
-   strlen：不是字符串长度而是占据几个**字节**，超过8位后自己按照8位一组一byte再扩容
-   bitcount：统计键里面含有1的有多少
-   bitop：对一个或多个保存二进制位的字符串 key 进行位元操作，并将结果保存到 destkey 上。
    -   `BITOP AND destkey srckey1 srckey2 srckey3 ... srckeyN` ，对一个或多个 key 求逻辑并，并将结果保存到 destkey 。
    -   `BITOP OR destkey srckey1 srckey2 srckey3 ... srckeyN`，对一个或多个 key 求逻辑或，并将结果保存到 destkey 。
    -   `BITOP XOR destkey srckey1 srckey2 srckey3 ... srckeyN`，对一个或多个 key 求逻辑异或，并将结果保存到 destkey 。
    -   `BITOP NOT destkey srckey`，对给定 key 求逻辑非，并将结果保存到 destkey 。



### 10.3 案例实现

查询两天内签到的学生

```sh
127.0.0.1:6379> SETBIT 20220503 0 1
(integer) 0
127.0.0.1:6379> SETBIT 20220503 1 1
(integer) 0
127.0.0.1:6379> SETBIT 20220503 2 1
(integer) 0
127.0.0.1:6379> SETBIT 20220503 3 1
(integer) 0
127.0.0.1:6379> SETBIT 20220503 4 1
(integer) 0
127.0.0.1:6379> SETBIT 20220504 1 1
(integer) 0
127.0.0.1:6379> SETBIT 20220504 2 1
(integer) 0
127.0.0.1:6379> BITCOUNT 20220503
(integer) 5
127.0.0.1:6379> BITCOUNT 20220504
(integer) 2
127.0.0.1:6379> BITOP and res 20220503 20220504
(integer) 1
127.0.0.1:6379> BITCOUNT res
(integer) 2


```



查看某个学生一年的登录天数

```sh
127.0.0.1:6379> SETBIT 202006150060 1 1
(integer) 0
127.0.0.1:6379> SETBIT 202006150060 2 1
(integer) 0
127.0.0.1:6379> SETBIT 202006150060 345 1
(integer) 0
127.0.0.1:6379> BITCOUNT 202006150060
(integer) 3
```

还可以通过get命令获取它的ASCLL码值，通过这个来得到该学生的签到日历





## redis流(stream)

### 介绍

Redis Stream 是 Redis 5.0 版本新增加的数据结构。是Redis版的MQ

Redis Stream 主要用于消息队列（MQ，Message Queue），Redis 本身是有一个 Redis 发布订阅 (pub/sub) 来实现消息队列的功能，但它有个缺点就是消息无法持久化，如果出现网络断开、Redis 宕机等，消息就会被丢弃。

简单来说发布订阅 (pub/sub) 可以分发消息，但无法记录历史消息。而Redis Stream可以记录消息，这样我们就可以知道消息是已读还是未读。

而 Redis Stream 提供了消息的持久化和主备复制功能，可以让任何客户端访问任何时刻的数据，并且能记住每一个客户端的访问位置，还能保证消息不丢失。

Redis Stream 的结构如下所示，它有一个消息链表，将所有加入的消息都串起来，每个消息都有一个唯一的 ID 和对应的内容：

![img](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\stream1.png)

每个 Stream 都有唯一的名称，它就是 Redis 的 key，在我们首次使用 xadd 指令追加消息时自动创建。

上图解析：

-   Consumer Group ：消费组，使用 XGROUP CREATE 命令创建，一个消费组有多个消费者(Consumer)。
-   last*delivered*id ：游标，每个消费组会有个游标 last*delivered*id，任意一个消费者读取了消息都会使游标 last*delivered*id 往前移动。==也就是说每条消息只能被组中的某一个消费者消费==
-   pending*ids ：消费者(Consumer)的状态变量，作用是维护消费者的未确认的 id。 pending*ids 记录了当前已经被客户端读取的消息，但是还没有 ack (Acknowledge character：确认字符）。



### 常用指令

消息队列相关命令：

-   XADD - 添加消息到末尾
-   XTRIM - 对流进行修剪，限制长度
-   XDEL - 删除消息
-   XLEN - 获取流包含的元素数量，即消息长度
-   XRANGE - 获取消息列表，会自动过滤已经删除的消息
-   XREVRANGE - 反向获取消息列表，ID 从大到小
-   XREAD - 以阻塞或非阻塞方式获取消息列表

消费者组相关命令：

-   XGROUP CREATE - 创建消费者组
-   XREADGROUP GROUP - 读取消费者组中的消息
-   XACK - 将消息标记为"已处理"
-   XGROUP SETID - 为消费者组设置新的最后递送消息ID
-   XGROUP DELCONSUMER - 删除消费者
-   XGROUP DESTROY - 删除消费者组
-   XPENDING - 显示待处理消息的相关信息
-   XCLAIM - 转移消息的归属权
-   XINFO - 查看流和消费者组的相关信息；
-   XINFO GROUPS - 打印消费者组的信息；
-   XINFO STREAM - 打印流信息



### 命令详解

#### XADD

使用 [XADD](https://redis.com.cn/commands/xadd.html) 向队列添加消息，如果指定的队列不存在，则创建一个队列，XADD 语法格式：

```
XADD key ID field value [field value ...]
```

-   key ：队列名称，如果不存在就创建
-   ID ：消息 id，我们使用 * 表示由 redis 生成，可以自定义，但是要自己保证递增性(不可重复)。
-   field value ： 记录。



**XADD实例**

```sh
redis> XADD mystream * name Sara surname OConnor
"1601372323627-0"
redis> XADD mystream * field1 value1 field2 value2 field3 value3
"1601372323627-1"
redis> XLEN mystream
(integer) 2
redis> XRANGE mystream - +
1) 1) "1601372323627-0"
  2) 1) "name"
   2) "Sara"
   3) "surname"
   4) "OConnor"
2) 1) "1601372323627-1"
  2) 1) "field1"
   2) "value1"
   3) "field2"
   4) "value2"
   5) "field3"
   6) "value3"
redis>
```



#### XTRIM

使用 [XTRIM](https://redis.com.cn/commands/xtrim.html) 对流进行修剪，限制长度， 语法格式：

```sh
XTRIM key MAXLEN|MINID [=|~] threshold [LIMIT count]
```

-   key ：队列名称
-   MAXLEN ：长度
-   MINID：最小ID
-   count ：数量



**XTRIM实例**

```sh
127.0.0.1:6379> XADD mystream * field1 A field2 B field3 C field4 D
"1601372434568-0"
127.0.0.1:6379> XTRIM mystream MAXLEN 2
(integer) 0
127.0.0.1:6379> XRANGE mystream - +
1) 1) "1601372434568-0"
  2) 1) "field1"
   2) "A"
   3) "field2"
   4) "B"
   5) "field3"
   6) "C"
   7) "field4"
   8) "D"
127.0.0.1:6379>

redis>
```



#### XDEL

使用 [XDEL](https://redis.com.cn/commands/xdel.html) 删除消息，语法格式：

```sh
XDEL key ID [ID ...]
```

-   key：队列名称
-   ID ：消息 ID



**XDEL实例**

```sh
127.0.0.1:6379> XADD mystream * a 1
1538561698944-0
127.0.0.1:6379> XADD mystream * b 2
1538561700640-0
127.0.0.1:6379> XADD mystream * c 3
1538561701744-0
127.0.0.1:6379> XDEL mystream 1538561700640-0
(integer) 1
127.0.0.1:6379> XRANGE mystream - +
1) 1) 1538561698944-0
   2) 1) "a"
      2) "1"
2) 1) 1538561701744-0
   2) 1) "c"
      2) "3"
```



#### XLEN

使用 [XLEN](https://redis.com.cn/commands/xlen.html) 获取流包含的元素数量，即消息长度，语法格式：

```sh
XLEN key
```

-   key：队列名称



**XLEN实例**

```sh
redis> XADD mystream * item 1
"1601372563177-0"
redis> XADD mystream * item 2
"1601372563178-0"
redis> XADD mystream * item 3
"1601372563178-1"
redis> XLEN mystream
(integer) 3
redis>
```



#### XRANGE

使用 XRANGE 获取消息列表，会自动过滤已经删除的消息 ，语法格式：

```sh
XRANGE key start end [COUNT count]
```

-   key ：队列名
-   start ：开始值， - 表示最小值
-   end ：结束值， + 表示最大值
-   count ：数量



**实例**

```sh
redis> XADD writers * name Virginia surname Woolf
"1601372577811-0"
redis> XADD writers * name Jane surname Austen
"1601372577811-1"
redis> XADD writers * name Toni surname Morrison
"1601372577811-2"
redis> XADD writers * name Agatha surname Christie
"1601372577812-0"
redis> XADD writers * name Ngozi surname Adichie
"1601372577812-1"
redis> XLEN writers
(integer) 5
redis> XRANGE writers - + COUNT 2
1) 1) "1601372577811-0"
   2) 1) "name"
   2) "Virginia"
   3) "surname"
   4) "Woolf"
2) 1) "1601372577811-1"
   2) 1) "name"
   2) "Jane"
   3) "surname"
   4) "Austen"
redis>
```



#### XREVRANGE

使用 [XREVRANGE](https://redis.com.cn/commands/xrevrange.html) 获取消息列表，会自动过滤已经删除的消息 ，语法格式：

```sh
XREVRANGE key end start [COUNT count]
```

-   key ：队列名
-   end ：结束值， + 表示最大值
-   start ：开始值， - 表示最小值
-   count ：数量



**XREVRANGE实例**

```sh
redis> XADD writers * name Virginia surname Woolf
"1601372731458-0"
redis> XADD writers * name Jane surname Austen
"1601372731459-0"
redis> XADD writers * name Toni surname Morrison
"1601372731459-1"
redis> XADD writers * name Agatha surname Christie
"1601372731459-2"
redis> XADD writers * name Ngozi surname Adichie
"1601372731459-3"
redis> XLEN writers
(integer) 5
redis> XREVRANGE writers + - COUNT 1
1) 1) "1601372731459-3"
   2) 1) "name"
   2) "Ngozi"
   3) "surname"
   4) "Adichie"
redis>
```



#### XREAD

使用 XREAD 以阻塞或非阻塞方式获取消息列表 ，语法格式：

```sh
XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] id [id ...]
```

-   count ：数量
-   milliseconds ：可选，阻塞毫秒数，没有设置就是非阻塞模式
-   key ：队列名
-   id ：消息ID
    -   $ : $代表特殊ID，表示以当前Stream已经存储的最大的ID作为最后一个ID
    -   0-0：代表最小的ID开始获取队列中的消息，000/0/0-0都可以

阻塞它会等待新的消息添加进来直接读取返回，会等待到指定的毫秒数

非阻塞直接获取消息返回



**实例**

*# 从 Stream 头部读取两条消息*

```sh
####################   非阻塞  #######################
redis> XREAD COUNT 2 STREAMS mystream 0-0
1) 1) "mystream"
   2) 1) 1) "1683705561947-0"
         2) 1) "k1"
            2) "v1"
            3) "k2"
            4) "v2"
      2) 1) "1683705575519-0"
         2) 1) "k3"
            2) "v3"
            3) "k6"
            4) "v6"
            
127.0.0.1:6379> XREAD count 2 streams mystream $
# 此时队列中不存在最大ID ，所以返回的为空
(nil)

####################   阻塞  #######################
# 1 设置阻塞获取 - 会等待新消息进来
127.0.0.1:6379> XREAD count 2 block 20000 streams mystream $

# 2 打开第二个客户端添加消息
127.0.0.1:6379> XADD mystream *  k1 v2
"1683708163453-0"

# 3 此时获取到了添加到的消息
127.0.0.1:6379> XREAD count 2 block 20000 streams mystream $
1) 1) "mystream"
   2) 1) 1) "1683708163453-0"
         2) 1) "k1"
            2) "v2"
(3.73s
```



#### XGROUP CREATE

基于 Stream 实现的消息队列，如何保证消费者在发生故障或宕机再次重启后，仍然可以读取未处理完的消息？

-   Streams 会自动使用内部队列（也称为 PENDING List）留存消费组里每个消费者读取的消息保底措施，直到消费者使用 XACK 命令通知 Streams“消息已经处理完成”。
-   消费确认增加了消息的可靠性，一般在业务处理完成之后，需要执行 XACK 命令确认消息已经被消费完成

![image-20230510165831928](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img/img01\image-20230510165831928.png)

使用 XGROUP CREATE 创建消费者组，语法格式：

```sh
XGROUP CREATE key groupname id|$ [MKSTREAM] [ENTRIESREAD entries_read]
```

-   key ：队列名称，如果不存在就创建
-   groupname ：组名。
-   $ ： 表示从尾部开始消费，只接受新消息，当前 Stream 消息会全部忽略。

从头开始消费:

```shell
XGROUP CREATE mystream consumer-group-name 0-0  
```

从尾部开始消费:

```sh
XGROUP CREATE mystream consumer-group-name $
```



**案例**

```sh
127.0.0.1:6379> XGROUP CREATE mystream groupA $
OK
127.0.0.1:6379> XGROUP CREATE mystream groupB 0
OK
```



#### XREADGROUP GROUP

使用 XREADGROUP GROUP 读取消费组中的消息，语法格式：

```sh
XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [NOACK] STREAMS key [key ...] ID [ID ...]
```

-   group ：消费组名
-   consumer ：消费者名。
-   count ： 读取数量。
-   milliseconds ： 阻塞毫秒数。
-   key ： 队列名。
-   ID ： 消息 ID。

```sh
XREADGROUP GROUP consumer-group-name consumer-name COUNT 1 STREAMS mystream >
```



**案例**

```sh
127.0.0.1:6379> XREADGROUP group groupA consumer1 streams mystream >
1) 1) "mystream"
   2) 1) 1) "1683705561947-0"
         2) 1) "k1"
            2) "v1"
            3) "k2"
            4) "v2"
      2) 1) "1683705575519-0"
         2) 1) "k3"
            2) "v3"
            3) "k6"
            4) "v6"
	 ......
# 每个组只能消费一次，第二次消费就为nil了
127.0.0.1:6379> XREADGROUP group groupA consumer1 streams mystream >
(nil)
# 同一个的消费者也是不能消费同一条消息
127.0.0.1:6379> XREADGROUP group groupA consumer2 streams mystream >
(nil)

# 换一个组就又可以消费了
127.0.0.1:6379> XREADGROUP group groupB consumer1 streams mystream >
1) 1) "mystream"
   2) 1) 1) "1683705561947-0"
         2) 1) "k1"
            2) "v1"
            3) "k2"
            4) "v2"
   ......
```

**消费者的目的**：让组内的多个消费者共同分担读取消息，所以，我们通常会让每个消费者读取部分消息，从而实现消息读取负载在多个消费者间是均衡分布的



#### XGROUP DESTROY

删除消费组或组中的消费者

```sh
XGROUP DELCONSUMER key groupname consumername
```



#### XPENDING

使用 XPENDING 查看组中的 已读取但未确认 的消息，语法格式：

```sh
XPENDING key group [[IDLE min-idle-time] start end count [consumer]]
```

-   key ：队列名
-   group ：组名。
-   count ： 读取数量。
-   milliseconds ： 阻塞毫秒数。
-   key ： 。
-   ID ： 消息 ID。

```sh
XREADGROUP GROUP consumer-group-name consumer-name COUNT 1 STREAMS mystream >
```



**案例**

```sh
#==================   查看消费组消费的消息  ===================== 
127.0.0.1:6379> XPENDING mystream groupA
1) (integer) 8			# 消费条数
2) "1683705561947-0"	# 最小ID
3) "1683708163453-0"	# 最大ID
4) 1) 1) "consumer1"	# 那个组消费的
      2) "8"
127.0.0.1:6379> XPENDING mystream groupC
1) (integer) 6
2) "1683705561947-0"
3) "1683707565771-0"
4) 1) 1) "consumer1"
      2) "2"
   2) 1) "consumer2"
      2) "2"
   3) 1) "consumer3"
      2) "2"
      
#==================   查看消费组中某一个消费者消费的消息  =====================      
127.0.0.1:6379> XPENDING mystream groupC - + 10 consumer2
1) 1) "1683705641820-0"
   2) "consumer2"
   3) (integer) 268096
   4) (integer) 1
2) 1) "1683705646742-0"
   2) "consumer2"
   3) (integer) 268096
   4) (integer) 1
```





#### XACK

使用 XACK 向 已读取但未确认 的消息发送确认，语法格式：

```sh
XACK key group id [id ...]
```



**案例**

```sh
# 查看已读未确定的消息
127.0.0.1:6379> XPENDING mystream groupC - + 10 consumer1
1) 1) "1683705561947-0"
   2) "consumer1"
   3) (integer) 616938
   4) (integer) 1
2) 1) "1683705575519-0"
   2) "consumer1"
   3) (integer) 616938
   4) (integer) 1
   
# ACK签收消息
127.0.0.1:6379> XACK mystream groupC 1683705561947-0
(integer) 1

# 再次查看发现没有了签收的那条消息
127.0.0.1:6379> XPENDING mystream groupC - + 10 consumer1
1) 1) "1683705575519-0"
   2) "consumer1"
   3) (integer) 642287
   4) (integer) 1
```



#### XINFO

用户打印 Stream / Consumer / Group 的详细消息，格式如下：

```sh
XINFO stream mystream [FULL [COUNT count]]
XINFO CONSUMERS key groupname
XINFO GROUPS key
```





## bitfield(位域)（了解）

[BITFIELD](https://www.redis.com.cn/commands/bitfield.html) 命令可以将一个 Redis 字符串看作是一个由二进制位组成的数组， 并对这个数组中任意偏移进行访问 。 可以使用该命令对一个有符号的 5 位整型数的第 1234 位设置指定值，也可以对一个 31 位无符号整型数的第 4567 位进行取值。类似地，本命令可以对指定的整数进行自增和自减操作，可配置的上溢和下溢处理操作。

`BITFIELD`命令可以在一次调用中同时对多个位范围进行操作：它接受一系列待执行的操作作为参数，并返回一个数组，数组中的每个元素就是对应操作的执行结果。

例如，对位于 5 位有符号整数的偏移量 100 执行自增操作，并获取位于偏移量 0 上的 4 位长无符号整数：

```
> BITFIELD mykey INCRBY i5 100 1 GET u4 0
1) (integer) 1
2) (integer) 0
```

注意:

1.  使用 [GET](https://www.redis.com.cn/commands/get.html) 子命令对超出字符串当前范围的二进制位进行访问（包括键不存在的情况），超出部分的二进制位的值将被当做是 0。
2.  使用 [SET](https://www.redis.com.cn/commands/set.html) 或 [INCRBY](https://www.redis.com.cn/commands/incrby.html) 子命令对超出字符串当前范围的二进制位进行访问将导致字符串被扩大，被扩大的部分会使用值为 0 的二进制位进行填充。在对字符串进行扩展时，命令会根据字符串目前已有的最远端二进制位，计算出执行操作所需的最小长度。



### 语法

redis [BITFIELD](https://www.redis.com.cn/commands/bitfield.html) 命令基本语法如下：

```sh
redis 127.0.0.1:6379> BITFIELD key [GET type offset] [SET type offset value] [INCRBY type offset increment] [OVERFLOW WRAP|SAT|FAIL]
```



### 支持的子命令以及整数类型

支持的子命令：

-   **GET** `<type>` `<offset>` -- 返回指定的二进制位范围。
-   **SET** `<type>` `<offset>` `<value>` -- 对指定的二进制位范围进行设置，并返回它的旧值。
-   **INCRBY** `<type>` `<offset>` `<increment>` -- 对指定的二进制位范围执行加法操作，并返回它的旧值。可以通过向 increment 参数传入负值来实现相应的减法操作。

除了以上三个子命令之外，还有一个子命令，它可以改变之后执行的 [INCRBY](https://www.redis.com.cn/commands/incrby.html) 子命令在发生溢出情况时的行为:

-   **OVERFLOW** `[WRAP|SAT|FAIL]`

当被设置的二进制位范围值为整数时，用户可以在类型参数的前面添加 `i` 来表示有符号整数， `u` 来表示无符号整数。 比如说，我们可以使用 `u8` 来表示 8 位长的无符号整数，也可以使用 `i16` 来表示 16 位长的有符号整数。

命令最大支持 64 位长的有符号整数以及 63 位长的无符号整数，其中无符号整数的 63 位长度限制是由于 Redis 协议目前还无法返回 64 位长的无符号整数而导致的。



### 二进制位和位置偏移量

在二进制位范围命令中，用户有两种方法来设置偏移量：如果用户给定的是一个没有任何前缀的数字，那么这个数字指示的就是字符串以零为开始（zero-base）的偏移量。

如果用户给定的是一个带有 `#` 前缀的偏移量，那么命令将使用这个偏移量与被设置的数字类型的位长度相乘，从而计算出真正的偏移量。例如：There are two ways in order to specify offsets in the bitfield command.

```
BITFIELD mystring SET i8 #0 100 SET i8 #1 200
```

命令会把 mystring 里面，第一个 i8 长度的二进制位的值设置为100 并把第二个 i8 长度的二进制位的值设置为 200 。当我们把一个字符串键当成数组来使用，并且数组中储存的都是同等长度的整数时，使用 `#` 前缀可以让我们免去手动计算被设置二进制位所在位置的麻烦。



### 溢出控制

用户可以通过 `OVERFLOW` 以下三个参数来控制BITFIELD 命令在执行自增或者自减操作时上溢出和下溢出：

-   **WRAP**: 使用回绕（wrap around）方法处理有符号整数和无符号整数的溢出情况。对于无符号整数来说，回绕就像使用数值本身与能够被储存的最大无符号整数执行取模计算，这也是 C 语言的标准行为。对于有符号整数来说，上溢将导致数字重新从最小的负数开始计算，而下溢将导致数字重新从最大的正数开始计算。比如说，如果我们对一个值为 127 的 i8 整数执行加一操作，那么将得到结果 -128。
-   **SAT**: 使用饱和计算（saturation arithmetic）方法处理溢出，也即是说，下溢计算的结果为最小的整数值，而上溢计算的结果为最大的整数值。举个例子，如果我们对一个值为 120 的 i8 整数执行加 10 计算，那么命令的结果将为 i8 类型所能储存的最大整数值 127 。与此相反，如果一个针对 i8 值的计算造成了下溢，那么这个 i8 值将被设置为 -127 。
-   **FAIL**: 在这一模式下，命令将拒绝执行那些会导致上溢或者下溢情况出现的计算，并向用户返回空值表示计算未被执行。

需要注意的是， `OVERFLOW` 做作用于该命令之后，下一个 `OVERFLOW` 子命令之前的 [INCRBY](https://www.redis.com.cn/commands/incrby.html) 命令。

默认情况下, [INCRBY](https://www.redis.com.cn/commands/incrby.html) 使用 **WRAP** 参数。

```
> BITFIELD mykey incrby u2 100 1 OVERFLOW SAT incrby u2 102 1
1) (integer) 1
2) (integer) 1
> BITFIELD mykey incrby u2 100 1 OVERFLOW SAT incrby u2 102 1
1) (integer) 2
2) (integer) 2
> BITFIELD mykey incrby u2 100 1 OVERFLOW SAT incrby u2 102 1
1) (integer) 3
2) (integer) 3
> BITFIELD mykey incrby u2 100 1 OVERFLOW SAT incrby u2 102 1
1) (integer) 0
2) (integer) 3
```



### 返回值

命令的返回值是一个数组，数组中的每个元素对应一个被执行的子命令。需要注意的是，`OVERFLOW` 子命令本身并不产生任何回复。

例如下面的 `OVERFLOW FAIL` 返回 NULL 。

```
> BITFIELD mykey OVERFLOW FAIL incrby u2 102 1
1) (nil)
```



### 用途

BITFIELD 命令的作用在于它能够将很多小的整数储存到一个长度较大的位图中，又或者将一个非常庞大的键分割为多个较小的键来进行储存，从而非常高效地使用内存，使得 Redis 能够得到更多不同的应用 ——特别是在实时分析领域：BITFIELD 能够以指定的方式对计算溢出进行控制的能力，使得它可以被应用于这一领域。



### 性能考虑

[BITFIELD](https://www.redis.com.cn/commands/bitfield.html) 在一般情况下都是一个快速的命令，需要注意的是，访问一个长度较短的字符串的远端不存在的二进制位将引发一次内存分配操作，这一操作花费的时间可能会比命令访问已有的字符串花费的时间要长。



### 二进制位的顺序

[BITFIELD](https://www.redis.com.cn/commands/bitfield.html) 把位图第一个字节偏移量 0 上的二进制位看作是 most significant 位，以此类推。举个例子，如果我们对一个已经预先被全部设置为 0 的位图进行设置，将它在偏移量 7 的值设置为 5 位无符号整数值 23 （二进制位为 10111 ），那么命令将生产出以下这个位图表示：

```
+--------+--------+
|00000001|01110000|
+--------+--------+
```

当偏移量和整数长度与字节边界进行对齐时，BITFIELD 表示二进制位的方式跟大端表示法（big endian）一致，但是在没有对齐的情况下，理解这些二进制位是如何进行排列也是非常重要的。







