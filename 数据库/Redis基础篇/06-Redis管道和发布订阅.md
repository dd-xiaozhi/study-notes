# Redis管道和发布订阅

## 管道

### 1、为什么需要管道？

Redis是一种基于客户端-服务端模型以及请求/响应协议的TCP服务。一个请求会遵循以下步骤：

1 客户端向服务端发送命令分四步(发送命令→命令排队→命令执行→返回结果)，并监听Socket返回，通常以阻塞模式等待服务端响应。

2 服务端处理命令，并将结果返回给客户端。

**上述两步称为：Round Trip Time(简称RTT,数据包往返于两端的时间)**

如果同时需要执行大量的命令，那么就要等待上一条命令应答后再执行，这中间不仅仅多了RTT（Round Time Trip），而且还频繁调用系统IO，发送网络请求，同时需要redis调用多次read()和write()系统方法，系统方法会将数据从用户态转移到内核态，这样就会对进程上下文有比较大的影响了，性能不太好，o(╥﹏╥)o

如果我们需要批量的执行多个命令，那么我们的RTT就会有多次，原生的 `mset`命令也可以批量进行设置，但是只能批量处理同一种类型，而管道时可以处理多种命令类型，批处理多个命令时得到优化。

**使用**

```sh
redis-cli --pie
```





### 2、案例

创建一个`.txt`文件，写入下面的内容

```txt
set k100 v100
set k200 v200
hset k300 name xiaohei
hset k300 age 20
hset k300 gender nan
```

批量执行命令

```sh
cat test.txt | redis-cli --pipe
All data transferred. Waiting for the last reply...
Last reply received from server.
errors: 0, replies: 5
```

查看命令是否已经执行

```sh
127.0.0.1:6379> keys *
1) "k300"
2) "k200"
5) "k100"
# nice~~~
```



### 3、注意事项

-   pipeline缓冲的指令只是会依次执行，不保证原子性，如果执行命令中发生异常，它不会停下，会继续执行后面的命令
-   使用 pipeline 组装的命令个数太多，不然数量过大客户端阻塞的时间可能过久，同时服务端此时也被迫回复一个队列的答复，占用很多的内存





## 发布订阅（了解）

### 1、介绍

它是一种消息通信模式，发送者(PUBLISH)发送消息，订阅者(SUBSCRIBE)接收消息，可以实现进程间的消息传递

它的工作模式就类似于我定于了微信的公众号，当公众号发布新的内容时就会推送内容给订阅了公众号的用户



### 2、常用命令

| 命令                                                         | 描述                               |
| :----------------------------------------------------------- | :--------------------------------- |
| [PSUBSCRIBE](https://redis.com.cn/commands/psubscribe.html)  | 订阅一个或多个符合给定模式的频道。 |
| [PUBSUB](https://redis.com.cn/commands/pubsub.html)          | 查看订阅与发布系统状态。           |
| [PUBLISH](https://redis.com.cn/commands/publish.html)        | 将信息发送到指定的频道。           |
| [PUNSUBSCRIBE](https://redis.com.cn/commands/punsubscribe.html) | 退订所有给定模式的频道。           |
| [SUBSCRIBE](https://redis.com.cn/commands/subscribe.html)    | 订阅给定的一个或多个频道的信息。   |
| [UNSUBSCRIBE](https://redis.com.cn/commands/unsubscribe.html) | 指退订给定的频道。                 |







