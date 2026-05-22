# 一、概述和安装

## 1、MQ相关概念

### 1.1 什么是MQ

MQ(message queue)，从字面意思上看，本质是个队列，FIFO 先入先出，只不过队列中存放的内容是 message 而已，还是一种跨进程的通信机制，用于上下游传递消息。在互联网架构中，MQ 是一种非常常见的上下游“逻辑解耦+物理解耦”的消息通信服务。使用了 MQ 之后，消息发送上游只需要依赖 MQ，不用依赖其他服务。

MQ是一个中间人，只负责转发消息，MQ可以理解为是快递站，负责接收快递和分发快递，但是不负责处理快递。





### 1.2 为什么用MQ

#### 1. 流量消峰

服务器的访问量太大会导致服务器宕机，服务器宕机就不能对外提供服务，这对消费者来说是不友好的，我们可以使用降级熔断的方式来保护服务器，也可以使用MQ的方式来进行处理，可以将访问服务的用户放入到队列中，排队进行处理，这样的话服务器就能根据自身的处理能力来处理。

举个例子，双十一的时候客户疯狂下单，本身服务器的QPS是 1w/s，但是在高峰期假设有两万次的下单操作，那么服务器是处理不了的，这个时候就可以使用MQ来缓冲，处理完前面的再处理后面的，这样就能达到保护服务器的作用。



#### 2.应用解耦

以电商系统为例，我们执行一个下单操作如下

![image-20230719134506271](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230719134506271.png) 

应用中有订单系统、支付系统、库存系统、物流系统，如果订单系统藕合这三个系统，那么有任何一个系统出现了异常都会导致整个订单失败，如果使用消息队列来进行处理之后就会减少系统调用的问题，比如物流系统发生故障，需要一点时间来进行修复，那么由于消息是存储在队列中的，物流消息还没有被消费，那么等到物流系统修复之后就可以去消费未被消费的物流消息，这个过程用户是感知不到的，提高了系统的可用性。

![image-20230719135152810](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230719135152810.png) 



#### 3. 异步处理

应用中的一些操作的实时性不高，我们使用异步处理，比如聊天的消息，它不一定发送就马上收到，可能会延迟1秒收到，那么这个时候我们就可以使用消息队列来做这个消息转发。比如API的调用，A调用B，B处理的时间有点长，等B处理完就将消息发送给消息队列，这个时候A系统再去消费消息。





### 1.3 MQ的分类

#### 1.ActiveMQ

优点：单机吞吐量万级，时效性 ms 级，可用性高，基于主从架构实现高可用性，消息可靠性较低的概率丢失数据

缺点:官方社区现在对 ActiveMQ 5.x **维护越来越少，高吞吐量场景较少使用**。



#### 2.Kafka

大数据的杀手锏，谈到大数据领域内的消息传输，则绕不开 Kafka，这款为**大数据而生**的消息中间件，以其**百万级** **TPS** 的吞吐量名声大噪，迅速成为大数据领域的宠儿，在数据采集、传输、存储的过程中发挥着举足轻重的作用。目前已经被 LinkedIn，Uber, Twitter, Netflix 等大公司所采纳。

**优点**: 性能卓越，单机写入 TPS 约在百万条/秒，最大的优点，就是吞**吐量高**。时效性 ms 级可用性非常高，kafka 是分布式的，一个数据多个副本，少数机器宕机，不会丢失数据，不会导致不可用,消费者采用 Pull 方式获取消息, 消息有序, 通过控制能够保证所有消息被消费且仅被消费一次;有优秀的第三方Kafka Web 管理界面 Kafka-Manager；在日志领域比较成熟，被多家公司和多个开源项目使用；功能支持：功能较为简单，主要支持简单的 MQ 功能，在大数据领域的实时计算以及**日志采集**被大规模使用

**缺点**：Kafka 单机超过 64 个队列/分区，Load 会发生明显的飙高现象，队列越多，load 越高，发送消息响应时间变长，使用短轮询方式，实时性取决于轮询间隔时间，消费失败不支持重试；支持消息顺序，但是一台代理宕机后，就会产生消息乱序，**社区更新较慢**；



#### 3.RocketMQ

RocketMQ 出自阿里巴巴的开源产品，用 Java 语言实现，在设计时参考了 Kafka，并做出了自己的一些改进。被阿里巴巴广泛应用在订单，交易，充值，流计算，消息推送，日志流式处理，binglog 分发等场景。

优点:**单机吞吐量十万级**,可用性非常高，分布式架构,**消息可以做到 0 丢失**,**MQ 功能较为完善，还是分布式的，扩展性好,**支持 **10** **亿级别的消息堆积**，不会因为堆积导致性能下降,源码是 java 我们可以自己阅读源码，定制自己公司的 MQ

缺点：**支持的客户端语言不多**，目前是 java 及 c++，其中 c++不成熟；社区活跃度一般,没有在 MQ核心中去实现 JMS 等接口,有些系统要迁移需要修改大量代码



#### 4.RabbitMQ

2007 年发布，是一个在 AMQP(高级消息队列协议)基础上完成的，可复用的企业消息系统，是**当前最主流的消息中间件之一**。

**优点**:由于 erlang 语言的**高并发特性**，性能较好；**吞吐量到万级**，MQ 功能比较完备,健壮、稳定、易用、跨平台、**支持多种语言** 如：Python、Ruby、.NET、Java、JMS、C、PHP、ActionScript、XMPP、STOMP等，支持 AJAX 文档齐全；开源提供的管理界面非常棒，用起来很好用,**社区活跃度高**；更新频率相当高

https://www.rabbitmq.com/news.html

**缺点**：商业版需要收费,学习成本较高



### 1.4 MQ的选择

#### 1.Kafka

Kafka 主要特点是基于 Pull 的模式来处理消息消费，追求高吞吐量，一开始的目的就是用于日志收集和传输，适合产生**大量数据**的互联网服务的数据收集业务。**大型公司**建议可以选用，如果有**日志采集**功能，肯定是首选 kafka 了。



#### 2.RocketMQ

天生为**金融互联网**领域而生，对于可靠性要求很高的场景，尤其是电商里面的订单扣款，以及业务削峰，在大量交易涌入时，后端可能无法及时处理的情况。RoketMQ 在稳定性上可能更值得信赖，这些业务场景在阿里双 11 已经经历了多次考验，如果你的业务有上述并发场景，建议可以选择 RocketMQ。



#### 3.RabbitMQ

结合 erlang 语言本身的并发优势，性能好**时效性微秒级**，**社区活跃度也比较高**，管理界面用起来十分方便，如果你的**数据量没有那么大**，中小型公司优先选择功能比较完备的 RabbitMQ。







## 2、RabbitMQ概述

### 2.1 概念

RabbitMQ 是一个消息中间件：它接受并转发消息。你可以把它当做一个快递站点，当你要发送一个包裹时，你把你的包裹放到快递站，快递员最终会把你的快递送到收件人那里，按照这种逻辑 RabbitMQ 是一个快递站，一个快递员帮你传递快件。RabbitMQ 与快递站的主要区别在于，它不处理快件而是接收，存储和转发消息数据。 

![image-20230719140606466](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230719140606466.png) 





### 2.2 四大核心概念

-   **Product（生产者）**：生产消息数据的应用

-   **Consumer(消费者)**：

    消费者消费与接收具有相似的含义。消费者大多时候是一个等待接收消息的程序。请注意生产者，消费者和消息中间件很多时候并不在同一机器上。**同一个应用程序既可以是生产者又是可以是消费者**。

-   **交换机(Exchange)**：

    交换机是 RabbitMQ 非常重要的一个部件，一方面它接收来自生产者的消息，另一方面它将消息推送到队列中。交换机必须确切知道如何处理它接收到的消息，是将这些消息推送到特定队列还是推送到多个队列，亦或者是把消息丢弃，这个得有交换机类型决定。

    **交换机的四大类型**：Direct exchange（直连交换机）、Fanout exchange（扇出交换机）、Topic exchange（主题交换机）、Headers exchange（头交换机）

    交换机可以理解为规则路由器，根据不同的规则类型来处理消息，举例子可以为银行的交易窗口，有多种交易方式，

-   **队列（Queue）**：

    队列是 RabbitMQ 内部使用的一种数据结构，尽管消息流经 RabbitMQ 和应用程序，但它们只能存储在队列中。队列仅受主机的内存和磁盘限制的约束，**本质上是一个大的消息缓冲区**。许多生产者可以将消息发送到一个队列，许多消费者可以尝试从一个队列接收数据。这就是我们使用队列的方式





### 2.3 Rabbit核心部分

![image-20230719142421981](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230719142421981.png)





### 2.4 名词介绍

![image-20230719143213295](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230719143213295.png) 

**Broker**：接收和分发消息的应用，RabbitMQ Server 就是 Message Broker，每一个RabbitMQ实例就是一个Broker

**Virtual host**：出于多租户和安全因素设计的，把 AMQP 的基本组件划分到一个虚拟的分组中，类似于网络中的 namespace 概念。当多个不同的用户使用同一个 RabbitMQ server 提供的服务时，可以划分出多个 vhost，每个用户在自己的 vhost 创建 exchange／queue 等

**Connection**：publisher／consumer 和 broker 之间的 TCP 连接

**Channel**：如果每一次访问 RabbitMQ 都建立一个 Connection，在消息量大的时候建立 TCP Connection 的开销将是巨大的，效率也较低。Channel 是在 connection 内部建立的逻辑连接，如果应用程序支持多线程，通常每个 thread 创建单独的 channel 进行通讯，AMQP method 包含了 channel id 帮助客户端和 message broker 识别 channel，所以 channel 之间是完全隔离的。**Channel 作为轻量级的Connection 极大减少了操作系统建立 TCP connection的开销** 

**Exchange**：message 到达 broker 的第一站，根据分发规则，匹配查询表中的 routing key，分发消息到 queue 中去。常用的类型有：direct (point-to-point), topic (publish-subscribe) and fanout (multicast)

**Queue**：消息最终被送到这里等待 consumer 取走

**Binding**：exchange 和 queue 之间的虚拟连接，binding 中可以包含 routing key，Binding 信息被保存到 exchange 中的查询表中，用于 message 的分发依据





## 3、安装RabbitMQ

安装教程地址：https://www.rabbitmq.com/download.html

这里我们使用资源包的方式进行安装

服务器：Centos7

首先将对应的资源包上传到服务器

![image-20230719150333549](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230719150333549.png) 



### 3.1 安装Erlang

RabbitMQ是用Erlang语言开发的，所以需要先安装Erlang

```sh
rpm -ivh erlang-21.3-1.el7.x86_64.rpm
```



### 3.2 安装RabbitMQ服务端

```sh
yum install socat -y
rpm -ivh rabbitmq-server-3.8.8-1.el7.noarch.rpm
```



### 3.3 卸载

```sh
# 卸载erlang
yum list | grep erlang
yum -y remove erlang-*
rm -rf /usr/lib64/erlang

# 卸载RabbitMQ
yum list | grep rabbitmq
yum -y remove rabbitmq-server.noarch

# 查看是否删除干净
rpm -qa | grep rabbitmq
rpm -qa | grep erlang

# 如果 rpm -qa | grep erlang 显示不为空
sudo yum remove erlang
sudo yum remove erlang-erts
sudo yum remove erlang-asn1
sudo yum remove erlang-os_mon
sudo yum remove erlang-xmerl
```



### 3.4 常用命令

```sh
# 启动服务
service rabbitmq-server start

# 查看服务状态
service rabbitmq-server status

# 停止服务
service rabbitmq-server stop

# 重启服务
service rabbitmq-server restart

# 开启 web 管理插件
rabbitmq-plugins enable rabbitmq_management

# 启动应用
rabbitmqctl start_app
# 停止应用
rabbitmqctl stop_app
# 清除命令
rabbitmqctl reset
```



### 3.5 guest用户远程访问

默认是有一个 guest 用户，密码也是 guest，不支持远程访问，只能本地访问，需要配置才能远程访问

```sh
sudo vi /etc/rabbitmq/rabbitmq.conf
```

```sh
# 添加或修改loopback_users配置项
[
  {rabbit, [
    {loopback_users, []}
  ]}
]
```

```sh
# 重启服务
sudo systemctl restart rabbitmq-server

###### 再次访问没有问题
```



### 3.6 添加用户

```sh
# 创建账号
rabbitmqctl add_user admin 123

# 设置用户角色
rabbitmqctl set_user_tags admin administrator

# 设置用户权限
# 语法： set_permissions [-p <vhostpath>] <user> <conf> <write> <read>
rabbitmqctl set_permissions -p "/" admin ".*" ".*" ".*"
### 用户 user_admin 具有/vhost1 这个 virtual host 中所有资源的配置、写、读权限

# 当前用户和角色
rabbitmqctl list_users
```







# 二、Hello Word

在本教程的这一部分中，我们将用 Java 编写两个程序；发送单个消息的生产者和接收消息并将其打印出来的消费者。

在下图中，“P”是我们的生产者，“C”是我们的消费者。中间的框是一个队列 - RabbitMQ 代表消费者保留的消息缓冲区。

![(P) -> [|||] -> (C)](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/python-one-16897536929733.png) 





## 1、 引入依赖

```xml
<dependencies>
    <!--rabbitmq 依赖客户端-->
    <dependency>
        <groupId>com.rabbitmq</groupId>
        <artifactId>amqp-client</artifactId>
        <version>5.8.0</version>
    </dependency>
    <!--操作文件流的一个依赖-->
    <dependency>
        <groupId>commons-io</groupId>
        <artifactId>commons-io</artifactId>
        <version>2.6</version>
    </dependency>
</dependencies>
```





## 2、Product

```java
/**
 * @author xiaozhi
 *
 * 消息生产者
 */
public class Product {

    public static final String QUEUE_NAME = "hello";

    public static void main(String[] args) throws IOException, TimeoutException {
        // 1 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        // 设置连接信息
        factory.setHost("192.168.10.10");
        factory.setUsername("guest");
        factory.setPassword("guest");

        // 2 创建连接
        Connection connection = factory.newConnection();
        // 3 创建 channel
        Channel channel = connection.createChannel();

        /**
         * 生成一个队列 -> Channel#queueDeclare，参数如下：
         *      1.队列名称
         *      2.消息是否持久化
         *      3.消息是否共享，true可以多个消费者进行消费，false只能一个消费者进行消费
         *      4.队列是否自动删除，最后一个消费者消费完就进行删除
         *      5.其他参数
         */
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);

        /*
            发送一个消息 -> Channel#basicPublish，参数如下：
                1.发送到那个交换机
                2.路由的key是哪个
                3.其他的参数信息
                4.发送消息的消息体
         */
        String message = "hello word";
        channel.basicPublish("", QUEUE_NAME, null,message.getBytes(StandardCharsets.UTF_8));
        System.out.println("消息发送完毕");
    }
}
```

运行程序在web控制台上可以看到未消费的消息增加了

![image-20230719162532263](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230719162532263.png)



## 3、Consumer

```java
/**
 * @author xiaozhi
 *
 * 消息消费者
 */
public class Consumer {

    public static final String QUEUE_NAME = "hello";

    public static void main(String[] args) throws IOException, TimeoutException {
        // 1 创建一个连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        // 设置连接信息
        factory.setHost("192.168.10.10");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        System.out.println("等待接收消息...");

        // 接收消息成功的回调
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            System.out.println(consumerTag);
            System.out.println(new String(delivery.getBody(), StandardCharsets.UTF_8));
        };

        // 取消消费的一个回调接口 如在消费的时候队列被删除掉了
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("消息中断");
        };

        /*
            消费者消费消息 -> channel#basicConsume
                1.消费的队列名
                2.消费完成是否自动应答，ture表示自动应答，false表示手动应答
                3.消息消费成功回调
                4.消费消费失败回调
         */
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, cancelCallback);
    }
}
```





## 4、工具类抽取

```java
/**
 * @author xiaozhi
 * 
 * 连接 RabbitMQ 工具类
 */
public class RabbitMQUtil {
    
    public static Channel getChannel() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        // 设置连接信息
        factory.setHost("192.168.10.10");
        factory.setUsername("guest");
        factory.setPassword("guest");
        Connection connection = factory.newConnection();
        return connection.createChannel();
    }
}
```



# 三、Work Queues

工作队列(又称任务队列)的主要思想是避免立即执行资源密集型任务，而不得不等待它完成。相反我们安排任务在之后执行。我们把任务封装为消息并将其发送到队列。在后台运行的工作进程将弹出任务并最终执行作业。当有多个工作线程时，这些工作线程将一起处理这些任务。所谓的工作线程可以理解为是多个消费者，多个消费者去消费队列中的消息。工作队列就会以轮询的方式将消息分发给消费者进行消费



## 1、轮询分发消息

轮询就是一个到一个轮着来，一轮回来就接着下一个，它会均匀的将消息分发给消费者。

接下来我们要做的实验就是产生九条消息，由三个线程去进行消费，我们看一下它是否是轮询的方式去分发消息



### 1.1 Product

```java
/**
 * @author xiaozhi
 *
 * 消息生产者，生产九条消息
 */
public class Product {

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        channel.queueDeclare(RabbitMQUtil.QUEUE_NAME, false, false, false, null);
        for (int i = 0; i < 9; i++) {
            String msg = String.valueOf(i);
            System.out.println("消息发送完毕：" + i);
            channel.basicPublish("", RabbitMQUtil.QUEUE_NAME, null, msg.getBytes(StandardCharsets.UTF_8));
        }
    }
}
```



### 1.2 Consumer

```java
/**
 * @author xiaozhi
 *
 * 线程消费者
 */
public class ThreadConsumer implements Runnable{

    @Override
    public void run(){
        String name = Thread.currentThread().getName();
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("当前线程：" + name + "-> msg：" + msg);
        };
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("消息中断：" + consumerTag);
        };
        try {
            Connection con = RabbitMQUtil.getCon();
            Channel channel = con.createChannel();
            channel.basicConsume(RabbitMQUtil.QUEUE_NAME, true, deliverCallback, cancelCallback);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```



### 1.3 测试

```java
/**
 * @author xiaozhi
 */
public class Tack {

    public static void main(String[] args) {
        ThreadConsumer threadConsumer = new ThreadConsumer();
        new Thread(threadConsumer, "线程一").start();
        new Thread(threadConsumer, "线程二").start();
        new Thread(threadConsumer, "线程三").start();
    }
}
```

注意：先启动生产者的话可能会被一个线程消费，因为由于网络的原因其余的线程还未和RabbitMQ建立连接，那么先建立连接的线程就会将我们的消息全部消费了，所以我们要先启动消费者建立连接之后再启动生产者

![image-20230720100219072](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720100219072.png) 

下面我们再测试一下

![image-20230720100252399](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720100252399.png) 

线程消费对应的消息如下表

| 线程名 | 消费消息 |
| ------ | -------- |
| 线程一 | 1,4,7    |
| 线程二 | 0,3,6    |
| 线程三 | 2,5,8    |

可以看到消息确实是被轮询消费了





## 2、消息应答

### 2.1 概述

消费者完成一个任务可能需要较长一段时间，如果它在完成的过程中出现异常或者宕机了，假设RabbitMQ是传递消息之后就立刻删除消息的机制，那么这个消息就丢失了。

为了保证消息在发送的过程中不丢失，RabbitMQ引入了消息应答机制，消费者在接收到消息并且处理该消息之后，需要告诉RabbitMQ已经处理了，RabbitMQ就可以把消息删除了。就相当



**自动应答**

消息发送后立即被认为已经传送成功，这种模式需要在**高吞吐量和数据传输安全性方面做权衡**,因为这种模式如果消息在接收到之前，消费者那边出现连接或者 channel 关闭，那么消息就丢失了,当然另一方面这种模式消费者那边可以传递过载的消息，**没有对传递的消息数量进行限制**，当然这样有可能使得消费者这边由于接收太多还来不及处理的消息，导致这些消息的积压，最终使得内存耗尽，最终这些消费者线程被操作系统杀死，**所以这种模式仅适用在消费者可以高效并以某种速率能够处理这些消息的情况下使用**

实际的开发中很少使用自动应答



**手动应答**

消息发送后需要手动去发ack给RabbitMQ告诉它进行删除。





### 2.2 消息应答的 method

-   Channel.basicAck(用于肯定确认)

    RabbitMQ 已知道该消息并且成功的处理消息，可以将其丢弃了

-   Channel.basicNack(用于否定确认)

-   Channel.basicReject(用于否定确认)

    与 Channel.basicNack 相比少一个参数，不处理该消息了直接拒绝，可以将其丢弃了



**Multiple解释**

手动应答的好处是可以批量应答并且减少网络拥堵

```java
void basicAck(long deliveryTag, boolean multiple)
/*
	deliveryTag：消息标识
	multiple：
		true 表示批量应答，有弊端，它是以最后一个处理的消息为准，处理完就全部进行提交，如果前面的消息还未被消费完成，也会							提交
		false 表示单个应答，只会应答对应 deliveryTag 的消息
*/
```

![image-20230720152612712](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720152612712.png) 





### 2.3 消息自动重新入队

消费者由于某些原因失去连接，导致未发送ACK确认消息，RabbitMQ会将对应未被处理的消息重新入队，将消息交给可以处理的消费者。如此，某个消费者宕机了或者出现异常时，也能保证消息不会丢失

![image-20230720152855827](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720152855827.png) 





### 2.4 消息手动应答实现

#### 1.生产者

```java
public class Task2 {

    public static final String WORK_QUEUE_NAME = "work_queue";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        // 创建队列
        channel.queueDeclare(WORK_QUEUE_NAME, false, false, false, null);
        Scanner sc = new Scanner(System.in);
        while (true) {
            String message = sc.next();
            System.out.println("发送消息：" + message);
            channel.basicPublish("", WORK_QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
        }
    }
}
```



#### 2.消费者

这里使用两个消费者来进行模拟多个消费者的情况

```java
public class Work01 {

    public static final String WORK_QUEUE_NAME = "work_queue";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        System.out.println("c1应用等待消息...");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            SleepUtil.sleep(1);
            System.out.println("接收消息： " + msg);
            /*
                手动应答 -> Channel#basicAck
                    1.消息标记tag
                    2.批量应答 false：单个应答 ture：批量应答
             */
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        CancelCallback cancelCallback = (consumerTag) -> {
            System.out.println("消息中断：" + consumerTag);
        };
        channel.basicConsume(WORK_QUEUE_NAME, false, deliverCallback, cancelCallback);
    }
}
```

第二个消费者修改一下以下信息

```java
System.out.println("c2应用等待消息...");
// 让它等待三十秒
SleepUtil.sleep(30);
```



#### 3.测试

模拟实现：首先启动三个程序，然后再生产者中生产消息，接着将 其中一个消费者 手动关闭，查看变化

![image-20230720153426240](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720153426240.png) 

发送两条消息应该是会被两个消费者分别消费，由于 work02 的消费时间较长 ，消息还未被 ack

![image-20230720153557145](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720153557145.png) 

可以看到关闭后未消费的消息被 Work01 消费了~~~





## 3、不公平分发消息

在上面的教程中我们说了轮询分发消息，它可以保证消息被多个消费者轮流消费。问题就是处理能力快的消费者大部分时间处于空闲状态，而处理能力慢的消费者就是一直在干活，导致资源利用率下降。所以这种情况下应该是让能力强的消费者多去消费消息，帮处理能力弱的消费者分摊处理压力。



### 3.1 预取值

RabbitMQ的信道上肯定不止只有一个消息，因此这里就存在一个未确认的消息缓冲区，因此希望开发人员能限制此缓冲区的大小，以避免缓冲区里面无限制的未确认消息问题。可以通过使用 `Channel#basicQos` 方法设置“预取计数”值来完成的。该值定义通道上允许的未确认消息的最大数量。一旦数量达到配置的数量，RabbitMQ 将停止在通道上传递更多消息，除非至少有一个未处理的消息被确认

例如，假设在通道上有未确认的消息 5、6、7，8，并且通道的预取计数设置为 4，此时 RabbitMQ 将不会在该通道上再传递任何消息，除非至少有一个未应答的消息被 ack。比方说 tag=6 这个消息刚刚被确认 ACK，RabbitMQ 将会感知这个情况到并再发送一条消息。消息应答和 QoS 预取值对用户吞吐量有重大影响。通常，增加预取将提高向消费者传递消息的速度。**虽然自动应答传输消息速率是最佳的，但是，在这种情况下已传递但尚未处理****的消息的数量也会增加，从而增加了消费者的** **RAM** **消耗**(随机存取存储器)应该小心使用具有无限预处理的自动确认模式或手动确认模式，消费者消费了大量的消息如果没有确认的话，会导致消费者连接节点的内存消耗变大，所以找到合适的预取值是一个反复试验的过程，不同的负载该值取值也不同 100 到 300 范围内的值通常可提供最佳的吞吐量，并且不会给消费者带来太大的风险。预取值为 1 是最保守的。当然这将使吞吐量变得很低，特别是消费者连接延迟很严重的情况下，特别是在消费者连接等待时间较长的环境中。对于大多数应用来说，稍微高一点的值将是最佳的。

可以理解为是流水线，流水线不能太快，太快员工跟不上，所以要给员工一点缓冲，让他不至于手上的还没处理完就又处理下一个。

![image-20230720195109860](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720195109860.png)



### 3.2 代码实现

我们这边有两个消费者分别是 work01 和 work02，他们的预取值分别是2和5

修改 Work01 和 Work02，添加预取值

```java
// work01
channel.basicQos(2);

// work02
channel.basicQos(5);
```

启动程序进行测试...

![image-20230720200415930](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720200415930.png) 

输入 10 条数据，work01已经消费了五条，由于 work02 延时了 30秒，所以有五条还在缓存区未被消费确认，和我们的预期一样。





## 4、RabbitMQ持久化

持久化就是将消息存储到硬盘中，RabbitMQ宕机或者出现异常都不会影响到磁盘中的消息，确保消息不会丢失。

确保消息保丢失我们需要将队列和消息都进行持久化



### 4.1 队列持久化

在上面的案例中我们的队列都是非持久化的，RabbitMQ重启队列就会被删除，如果队列要进行持久化就需要在创建的时候指明。

```java
// durable参数表示队列是否持久化
channel.queueDeclare(WORK_QUEUE_NAME, true, false, false, null);
```

**注意**：如果之前声明的队列不是持久化的，需要把原先队列先删除，或者重新创建一个持久化的队列，不然就会出现错误

持久化和非持久化队列的对比

![image-20230720180418193](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720180418193.png) 



### 4.2 消息持久化

在发布的时候指定持久化消息，添加 `MessageProperties.PERSISTENT_TEXT_PLAIN` 参数

```java
channel.basicPublish("", WORK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
```

将消息标记为持久化并不能完全保证不会丢失消息。因为在保存的过程中它可能会发生宕机，那么这个时候消息还是会丢失。消息确认策略才能强有力的保证数据不丢失。





## 5、发布确认策略

由于可能在持久化的过程中 RabbitMQ 宕机了，所以为了保证消息的可靠性，RabbitMQ引入了发布确认策略，生产者生产消息发给RabbitMQ Broker，接着Broker持久化完成之后会告诉生产者它已经持久化这个消息了，那么生产者就可以进行确认操作，如果持久化失败，那么生产者可以再次发送消息给 Broker，以此来保证数据不丢失。

**开启发布确认的方法** -> `Channel#confirmSelect`



### 5.1 单个确认发布

它是一种同步确认发布的方式，每次发布一个消息之后只有消息被确认发布了它才会继续发布下一个消息，`waitForConfirmsOrDie(long)`这个方法只有在消息被确认的时候才返回，**如果在指定时间范围内这个消息没有被确认那么它将抛出异常**

**缺点**：发布速度特别慢，它要等前面的确认返回才会发布下一条消息，这种方式最多提供每秒不超过数百条发布消息的吞吐量

**代码实现**

```java
public static void individualConfirmation() throws Exception {
    Channel channel = RabbitMQUtil.getChannel();
    String queueName = String.valueOf(UUID.randomUUID());
    // 创建队列
    channel.queueDeclare(queueName, false, false, false, null);
    // 开启确定发布
    channel.confirmSelect();
    // 统计时间
    long start = System.currentTimeMillis();
    for (int i = 0; i < COUNT; i++) {
        channel.basicPublish("", queueName,
                MessageProperties.PERSISTENT_TEXT_PLAIN, String.valueOf(i).getBytes());
        // 确认发布
        boolean flag = channel.waitForConfirms();
    }
    long end = System.currentTimeMillis();
    System.out.println("单个确认耗时：：" + (end - start) + "ms");
}
```



### 5.2 批量确认发布

上面那种方式非常慢，与单个等待确认消息相比，先发布一批消息然后一起确认可以极大地提高吞吐量，当然这种方式的缺点就是:当发生故障导致发布出现问题时，不知道是哪个消息出现问题了，我们必须将整个批处理保存在内存中，以记录重要的信息而后重新发布消息。当然这种方案仍然是同步的，也一样阻塞消息的发布。

```java
public static void batchConfirmation() throws Exception {
    Channel channel = RabbitMQUtil.getChannel();
    String queueName = String.valueOf(UUID.randomUUID());
    // 创建队列
    channel.queueDeclare(queueName, true, false, false, null);
    // 开启确定发布
    channel.confirmSelect();
    // 批量确认个数
    int batchSize = 100;
    // 未确认个数
    int outstandingMessageCount = 0;
    // 统计时间
    long start = System.currentTimeMillis();
    for (int i = 0; i < COUNT; i++) {
        channel.basicPublish("", queueName,
                MessageProperties.PERSISTENT_TEXT_PLAIN, String.valueOf(i).getBytes());
        outstandingMessageCount++;
        if (outstandingMessageCount % batchSize == 0) {
            channel.waitForConfirms();
            outstandingMessageCount = 0;
        }
    }
    long end = System.currentTimeMillis();
    System.out.println("批量确认耗时：" + (end - start) + "ms");
    // 生产消息的总时长为：934ms
}
```



### 5.3 异步确认发布

异步确认虽然编程逻辑比上两个要复杂，但是性价比最高，无论是可靠性还是效率都没得说，他是利用回调函数来达到消息可靠性传递的，这个中间件也是通过函数回调来保证是否投递成功，下面就让我们来详细讲解异步确认是怎么实现的。

![image-20230720210931330](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720210931330.png) 

这种方式就是 生产者 一直发布消息，然后异步处理确认收到和未收到的消息



#### 5.3.1 代码实现

```java
public static void asyncConfirmation() throws Exception {
    Channel channel = RabbitMQUtil.getChannel();
    String queueName = UUID.randomUUID().toString();
    channel.queueDeclare(queueName, true, false, false, null);
    channel.confirmSelect();
    ConfirmCallback confirmCallback = (deliveryTag, multiple) -> {

    };
    ConfirmCallback nackCallback = (sequenceNumber, multiple) -> {

    };
    /*
        添加一个异步确认的监听器
            1.确认收到消息的回调
            2.未收到消息的回调
     */
    channel.addConfirmListener(confirmCallback, nackCallback);
    long begin = System.currentTimeMillis();
    for (int i = 0; i < COUNT; i++) {
        channel.basicPublish("", queueName,
                MessageProperties.PERSISTENT_TEXT_PLAIN, String.valueOf(i).getBytes());
    }
    long end = System.currentTimeMillis();
    System.out.println("异步确认耗时：" + (end - begin) + "ms");
}
```



#### 5.3.2 如何处理异步未确认消息

1.  首先保存发送的消息到Map中
2.  将确认收到的消息移除Map
3.  将剩余的消息重洗再发送一遍

```java
    public static void asyncConfirmation() throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        String queueName = UUID.randomUUID().toString();
        channel.queueDeclare(queueName, true, false, false, null);
        channel.confirmSelect();
        ConcurrentSkipListMap<Long, String> outstandingConfirms = new ConcurrentSkipListMap<>();
        ConfirmCallback confirmCallback = (deliveryTag, multiple) -> {
            // 2.将确认收到的消息移除 Map
            if (multiple) {
                // 批处理
                // ConcurrentSkipListMap#headMap(toKey)：返回此映射的部分视图，其键值严格小于 toKey
                ConcurrentNavigableMap<Long, String> map = outstandingConfirms.headMap(deliveryTag);
                // 清空
                map.clear();
            } else {
                // 删除对应 deliveryTag 的消息
                outstandingConfirms.remove(deliveryTag);
            }
        };
        ConfirmCallback nackCallback = (sequenceNumber, multiple) -> {
            // 3.将发送失败的消息重新发送
            String message = outstandingConfirms.get(sequenceNumber);
            // 重新发送
            try {
                channel.waitForConfirms();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        /*
            添加一个异步确认的监听器
                1.确认收到消息的回调
                2.未收到消息的回调
         */
        channel.addConfirmListener(confirmCallback, nackCallback);
        long begin = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) {
            // 1.将发送的消息保存到 Map 中
            String message = String.valueOf(i);
            outstandingConfirms.put(channel.getNextPublishSeqNo(), message);
            channel.basicPublish("", queueName,
                    MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        }
        long end = System.currentTimeMillis();
        System.out.println("异步确认耗时：" + (end - begin) + "ms");
    }

```



### 5.4 三者比较

![image-20230720210625568](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230720210625568.png) 

-   单独发布消息

    同步等待确认，简单，但吞吐量非常有限。

-   批量发布消息

    批量同步等待确认，简单，合理的吞吐量，一旦出现问题但很难推断出是那条消息出现了问题

-   异步处理

    最佳性能和资源使用，在出现错误的情况下可以很好地控制，但是实现起来稍微难些





# 四、交换机

## 1、Exchanges 

### 1.1 概述

RabbitMQ的 Broker 接收到消息后交给对应的交换机，由交换机将消息交给对应的队列进行处理，不同的交换机类型它的功能也不一样。

**交换机类型**：Direct exchange（直接交换机）、Fanout exchange（扇出交换机）、Topic exchange（主题交换机）、Headers exchange（请求头交换机）



### 1.2 绑定(Bindings)

![image-20230721173326883](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721173326883.png) 

==注意：首先 消费者要创建队列和 交换机进行绑定，接着才能发送消息，不然消息是会丢失的==

![image-20230721141352541](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721141352541.png) 

上图是我们的运行架构，生产者叫消息交给交换机，交换机发送给对应绑定的队列，接着消费者再进行消费。

那么这个`绑定(binding)`就是交换机和队列进行的绑定关系，通过 `RoutingKey` 来进行关系映射

==**注意**：要使用指定类型的交换机必须进行绑定==



### 1.3 临时队列

临时队列就如同它的名字一样是临时的，RabbitMQ会给我们生成一个随机名字的队列，当我们断开消费者的连接，那么队列就会被自动删除

**创建临时队列**

```java
String queueName = channel.queueDeclare().getQueue();
```

web控制台显示如下

![image-20230721142539420](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721142539420.png)



### 1.4 无名 Exchange

在之前的教程中我们并没有使用交换机，而是用空串来代替

![image-20230721170252436](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721170252436.png) 

空串表示是默认或无名交换机

![image-20230721170533910](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721170533910.png) 

默认交换机它是根据队列名进行分发的





## 2、Direct exchange

### 2.1 概述

直接交换机就是将消息只发送给对应 RoutingKey 的队列进行消息处理

![image-20230721152251540](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721152251540.png) 

如上图，图中有两个不同的 `RoutingKey`绑定不同的两个队列，那么对应 RoutingKey 的消息只能发送给对应的队列。





### 2.2 模拟日志发送

需求：产生四条日志消息给消费者消费

![image-20230721171118538](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721171118538.png) 

上图使我们的整体架构

| RoutingKey    | 队列名  |
| ------------- | ------- |
| info、warning | console |
| error         | disk    |

最终的输出结果就是，RoutingKey 是info、waraing的消息就会被 consloe队列消费，error 被 disk队列消费

![image-20230721174614991](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721174614991.png)



### 2.3 代码实现

**生产者**

```java
public class EmitLogDirect {

    public static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        // 创建多个 bindingKey
        Map<String, String> bindingKeyMap = new HashMap<String, String>();
        bindingKeyMap.put("info", "普通 info 消息");
        bindingKeyMap.put("warning","警告 warning 信息");
        bindingKeyMap.put("debug", "调试 bug 消息");
        bindingKeyMap.put("error", "错误 error 消息");
        bindingKeyMap.forEach((k, v) -> {
            try {
                channel.basicPublish(EXCHANGE_NAME, k, null, v.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("生产者发出消息：" + v);
        });
    }
}
```

**两个消费者**

```java
public class ReceiveLogsDirect01 {

    // 交换机名称
    public static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        // 创建交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        // 创建队列
        String queueName = "console";
        channel.queueDeclare(queueName, false, false, false, null);
        // 绑定交换机
        channel.queueBind(queueName, EXCHANGE_NAME, "info");
        channel.queueBind(queueName, EXCHANGE_NAME, "warning");
        System.out.println("等待接收消息");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("绑定建：" + delivery.getEnvelope().getRoutingKey() + "消息：" + msg);
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
```

```java
public class ReceiveLogsDirect02 {

    // 交换机名称
    public static final String EXCHANGE_NAME = "direct_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        // 创建交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        // 创建队列
        String queueName = "disk";
        channel.queueDeclare(queueName, false, false, false, null);
        // 绑定交换机
        channel.queueBind(queueName, EXCHANGE_NAME, "error");
        System.out.println("等待接收消息");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String msg = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("绑定建：" + delivery.getEnvelope().getRoutingKey() + "消息：" + msg);
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
```



**输出结果**

![image-20230721165608294](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721165608294.png)

完美，和我们设想的一样~~~





## 3、Fanout exchange

扇出交换机，和广播是一样的原理的。有消息过来那么和它绑定的所有队列都会被消费。

![image-20230721172139503](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721172139503.png) 

这里的队列使用的是临时队列，因为队列名不是很需要，方式绑定的队列都会收到消息



**代码实现**

```java
public class EmitLog {

    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        Scanner sc = new Scanner(System.in);
        while (sc.hasNext()) {
            String msg = sc.next();
            channel.basicPublish(EXCHANGE_NAME, "", null, msg.getBytes(StandardCharsets.UTF_8));
        }
    }
}
```

```java
public class ReceiveLogs01 {

    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        // 生成一个随机队列，当消费者断开和该队列的连接时就会进行删除
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        System.out.println("c1 等待消息...");
        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println("消息：" + new String(message.getBody(), StandardCharsets.UTF_8));
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
```

```java
public class ReceiveLogs02 {

    private static final String EXCHANGE_NAME = "logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        // 生成一个随机队列，当消费者断开和该队列的连接时就会进行删除
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        System.out.println("c2 等待消息...");
        DeliverCallback deliverCallback = (consumerTag, message) -> {
            System.out.println("消息：" + new String(message.getBody(), StandardCharsets.UTF_8));
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
```



**启动程序测试**

![image-20230721174214643](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230721174214643.png)

可以看到所有的绑定了 `Fanout Exchange` 的队列都收到了消息





## 4、Topic Exchange

### 4.1 概述

`Topic Exchange`它是通过 `RoutingKey` 的规则进行匹配，举个例子

`RoutingKey` 为 `*.rabbitmq.*` 的消息可以是 a.rabbitmq.b、c.rabbitmq.d，只要中间的是 rabbitmq 皆可以进行匹配

通过这样的匹配的方式我们可以定制化的发送消息，选择那些群发，那些单个发，结合了 `Direct Exchange` 和 `Fanout Exchange`的特点

**匹配规则说明**

-   `*`表示一个单词

    a.* 可以匹配 a.b、a.c，不可以匹配 a.b.c

-   `#`表示一个或者多个单词

    a.# 可以匹配 a.b、a.b.c 等等



### 4.2 案例说明

![image-20230722030023995](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230722030023995.png) 

上图绑定关系如下

| 队列 | RoutingKey说明                                               |
| ---- | ------------------------------------------------------------ |
| Q1   | 中间是 orange 的消息（`*.orange.*`）                         |
| Q2   | 第三个是 rabbit 的消息（`*.*.rabbit`）<br />第一个是 lazy 的消息（`lazy.#`） |



### 4.3 代码实现

```java
public class EmitLogTopic {

    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        Map<String, String> bindingMap = new HashMap<>();
        bindingMap.put("quick.orange.rabbit","被队列 Q1Q2 接收到");
        bindingMap.put("lazy.orange.elephant","被队列 Q1Q2 接收到");
        bindingMap.put("quick.orange.fox","被队列 Q1 接收到");
        bindingMap.put("lazy.brown.fox","被队列 Q2 接收到");
        bindingMap.put("lazy.pink.rabbit","虽然满足两个绑定但只被队列 Q2 接收一次");
        bindingMap.put("quick.brown.fox","不匹配任何绑定不会被任何队列接收到会被丢弃");
        bindingMap.put("quick.orange.male.rabbit","是四个单词不匹配任何绑定会被丢弃");
        bindingMap.put("lazy.orange.male.rabbit","是四个单词但匹配 Q2");
        bindingMap.forEach((k, v) -> {
            try {
                channel.basicPublish(EXCHANGE_NAME, k, null, v.getBytes(StandardCharsets.UTF_8));
                System.out.println("发送消息：" + v);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
```

```java
public class ReceiveLogsTopic01 {

    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        String queueName = "Q1";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "*.orange.*");
        System.out.println("c1（RoutingKey=*.orange.*）等待接收消息...");
        DeliverCallback deliverCallback  = ((consumerTag, message) -> {
            String msg = new String(message.getBody(), StandardCharsets.UTF_8);
            String routingKey = message.getEnvelope().getRoutingKey();
            System.out.println("绑定键：" + routingKey + "\t接收到消息：" + msg);
        });
        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {});
    }
}
```

```java
public class ReceiveLogsTopic02 {

    private static final String EXCHANGE_NAME = "topic_logs";

    public static void main(String[] args) throws Exception {
        Channel channel = RabbitMQUtil.getChannel();
        String queueName = "Q2";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        channel.queueDeclare(queueName, false, false, false, null);
        channel.queueBind(queueName, EXCHANGE_NAME, "*.*.rabbit");
        channel.queueBind(queueName, EXCHANGE_NAME, "lazy.#");
        System.out.println("c2（RoutingKey=*.*.rabbit & lazy.#）等待接收消息...");
        DeliverCallback deliverCallback  = ((consumerTag, message) -> {
            String msg = new String(message.getBody(), StandardCharsets.UTF_8);
            String routingKey = message.getEnvelope().getRoutingKey();
            System.out.println("绑定键：" + routingKey + "\t接收到消息：" + msg);
        });
        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {});
    }
}
```

**测试结果显示**

![image-20230722025258817](https://xiaozhi-blog.oss-cn-guangzhou.aliyuncs.com/img01/img01/image-20230722025258817.png) 





## 5、Headers exchange

它和上面三种的交换机不同，它是通过请求头的键值来进行匹配







# 五、死信队列

## 1、概述







2、









# 六、延迟队列







