工作队列(又称任务队列)的主要思想是避免立即执行资源密集型任务，而不得不等待它完成。相反我们安排任务在之后执行。我们把任务封装为消息并将其发送到队列。在后台运行的工作进程将弹出任务并最终执行作业。当有多个工作线程时，这些工作线程将一起处理这些任务。所谓的工作线程可以理解为是多个消费者，多个消费者去消费队列中的消息。工作队列就会以轮询的方式将消息分发给消费者进行消费





# 1、轮询分发消息
轮询就是一个到一个轮着来，一轮回来就接着下一个，它会均匀的将消息分发给消费者。



接下来我们要做的实验就是产生九条消息，由三个线程去进行消费，我们看一下它是否是轮询的方式去分发消息



## 1.1 Product


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



## 1.2 Consumer
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



## 1.3 测试


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

<!-- 这是一张图片，ocr 内容为：TACK X COM.XIAOZHI.CLIENT.PRODUCT 当前线程:线程二一MSG:0 当前线程:线程二-MSG:1 当前线程:线程二-MSG:2 当前线程:线程二一MSG:3 当前线程:线程二一MSG:4 当前线程:线程二一)MSG: : 5 当前线程:线程二一MSG:6 当前线程:线程二一 MSG:7 当前线程:线程二-MSG:8 -->
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696727489-b60cebf0-700d-49f3-8837-421cbfaac6b9.png)

下面我们再测试一下
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696770836-85150335-9a07-4b92-9107-8f9fdfd8be19.png)

线程消费对应的消息如下表

| 线程名 | 消费消息 |
| --- | --- |
| 线程一 | 1,4,7 |
| 线程二 | 0,3,6 |
| 线程三 | 2,5,8 |


可以看到消息确实是被轮询消费了





# 2、消息应答
## 2.1 概述
消费者完成一个任务可能需要较长一段时间，如果它在完成的过程中出现异常或者宕机了，假设RabbitMQ是传递消息之后就立刻删除消息的机制，那么这个消息就丢失了。

为了保证消息在发送的过程中不丢失，RabbitMQ引入了消息应答机制，消费者在接收到消息并且处理该消息之后，需要告诉RabbitMQ已经处理了，RabbitMQ就可以把消息删除了。就相当



**自动应答**

消息发送后立即被认为已经传送成功，这种模式需要在**高吞吐量和数据传输安全性方面做权衡**,因为这种模式如果消息在接收到之前，消费者那边出现连接或者 channel 关闭，那么消息就丢失了,当然另一方面这种模式消费者那边可以传递过载的消息，**没有对传递的消息数量进行限制**，当然这样有可能使得消费者这边由于接收太多还来不及处理的消息，导致这些消息的积压，最终使得内存耗尽，最终这些消费者线程被操作系统杀死，**所以这种模式仅适用在消费者可以高效并以某种速率能够处理这些消息的情况下使用**

实际的开发中很少使用自动应答



**手动应答**

消息发送后需要手动去发ack给RabbitMQ告诉它进行删除。



## 2.2 消息应答的 method
+  Channel.basicAck(用于肯定确认)  
RabbitMQ 已知道该消息并且成功的处理消息，可以将其丢弃了 
+  Channel.basicNack(用于否定确认) 
+  Channel.basicReject(用于否定确认)  
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

<!-- 这是一张图片，ocr 内容为：当前TAG CHANNEL MULTIPLE-TRUE 5 9 此时5,6,7,8都会被确认应答 当前TAG CHANNEL MULTIPLEFALSE 5 6 7 此时5,6,7不会被确认应答,只有8被确认应答 -->
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696849334-53d8057a-db4a-4267-87ad-6937c921ba9f.png)



## 2.3 消息自动重新入队
消费者由于某些原因失去连接，导致未发送ACK确认消息，RabbitMQ会将对应未被处理的消息重新入队，将消息交给可以处理的消费者。如此，某个消费者宕机了或者出现异常时，也能保证消息不回丢失

<!-- 这是一张图片，ocr 内容为：消息1 消息1 消息3 消息3 失去连接 未ACK QUEUE QUEUE 消息 消息 消息 消息 消息2 消息2 消息重新入队 消息3 消息1 消息3 QUEUE QUEUE P 消息 消息 消息 消息1 消息 C2消费者可以处理消息 -->
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696858467-11f14045-1b54-4ac5-aa1f-a3b0661c87d7.png)



## 2.4 消息手动应答实现
### 1.生产者
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

### 2.消费者
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



### 3.测试
模拟实现：首先启动三个程序，然后再生产者中生产消息，接着将 其中一个消费者 手动关闭，查看变化
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696879276-a26aafab-af1f-4d96-b3da-2ae8d9edfe76.png)

发送两条消息应该是会被两个消费者分别消费，由于 work02 的消费时间较长 ，消息还未被 ack
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696890568-93b815f9-7b60-42e1-b609-d98dd0c29038.png)

可以看到关闭后未消费的消息被 Work01 消费了~~~





# 3、不公平分发消息
在上面的教程中我们说了轮询分发消息，它可以保证消息被多个消费者轮流消费。问题就是处理能力快的消费者大部分时间处于空闲状态，而处理能力慢的消费者就是一直在干活，导致资源利用率下降。所以这种情况下应该是让能力强的消费者多去消费消息，帮处理能力弱的消费者分摊处理压力。



## 3.1 预取值
RabbitMQ的信道上肯定不止只有一个消息，因此这里就存在一个未确认的消息缓冲区，因此希望开发人员能限制此缓冲区的大小，以避免缓冲区里面无限制的未确认消息问题。可以通过使用 `Channel#basicQos` 方法设置“预取计数”值来完成的。该值定义通道上允许的未确认消息的最大数量。一旦数量达到配置的数量，RabbitMQ 将停止在通道上传递更多消息，除非至少有一个未处理的消息被确认

:::color1
例子，假设在通道上有未确认的消息 5、6、7，8，并且通道的预取计数设置为 4，此时 RabbitMQ 将不会在该通道上再传递任何消息，除非至少有一个未应答的消息被 ack。比方说 tag=6 这个消息刚刚被确认 ACK，RabbitMQ 将会感知这个情况到并再发送一条消息。消息应答和 QoS 预取值对用户吞吐量有重大影响。通常，增加预取将提高向消费者传递消息的速度。**虽然自动应答传输消息速率是最佳的，但是，在这种情况下已传递但尚未处理的消息的数量也会增加，从而增加了消费者的** **RAM** **消耗**(随机存取存储器)应该小心使用具有无限预处理的自动确认模式或手动确认模式，消费者消费了大量的消息如果没有确认的话，会导致消费者连接节点的内存消耗变大，所以找到合适的预取值是一个反复试验的过程，不同的负载该值取值也不同 100 到 300 范围内的值通常可提供最佳的吞吐量，并且不会给消费者带来太大的风险。预取值为 1 是最保守的。当然这将使吞吐量变得很低，特别是消费者连接延迟很严重的情况下，特别是在消费者连接等待时间较长的环境中。对于大多数应用来说，稍微高一点的值将是最佳的。

:::

可以理解为是流水线，流水线不能太快，太快员工跟不上，所以要给员工一点缓冲，让他不至于手上的还没处理完就又处理下一个。
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696976717-63a3db1f-f6fc-4215-8623-47b6606ae7c6.png)



## 3.2 代码实现
我们这边有两个消费者分别是 work01 和 work02，他们的预取值分别是2和5

修改 Work01 和 Work02，添加预取值

```java
// work01
channel.basicQos(2);

// work02
channel.basicQos(5);
```

启动程序进行测试...
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696989615-dff576f8-d085-4d58-b59b-a2ae3e5118a4.png)

输入 10 条数据，work01已经消费了五条，由于 work02 延时了 30秒，所以有五条还在缓存区未被消费确认，和我们的预期一样。





# 4、RabbitMQ持久化
持久化就是将消息存储到硬盘中，RabbitMQ宕机或者出现异常都不会影响到磁盘中的消息，确保消息不会丢失。

确保消息保丢失我们需要将队列和消息都进行持久化



## 4.1 队列持久化
在上面的案例中我们的队列都是非持久化的，RabbitMQ重启队列就会被删除，如果队列要进行持久化就需要在创建的时候指明。

```java
// durable参数表示队列是否持久化
channel.queueDeclare(WORK_QUEUE_NAME, true, false, false, null);
```

**注意**：如果之前声明的队列不是持久化的，需要把原先队列先删除，或者重新创建一个持久化的队列，不然就会出现错误

持久化和非持久化队列的对比
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690697011368-fd76b1eb-cb83-44e1-8c50-084e63d1e614.png)



### 4.2 消息持久化
在发布的时候指定持久化消息，添加 `MessageProperties.PERSISTENT_TEXT_PLAIN` 参数

```java
channel.basicPublish("", WORK_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
```

将消息标记为持久化并不能完全保证不会丢失消息。因为在保存的过程中它可能会发生宕机，那么这个时候消息还是会丢失。消息确认策略才能强有力的保证数据不丢失。





# 5、发布确认策略
由于可能在持久化的过程中 RabbitMQ 宕机了，所以为了保证消息的可靠性，RabbitMQ引入了发布确认策略，生产者生产消息发给RabbitMQ Broker，接着Broker持久化完成之后会告诉生产者它已经持久化这个消息了，那么生产者就可以进行确认操作，如果持久化失败，那么生产者可以再次发送消息给 Broker，以此来保证数据不丢失。

**开启发布确认的方法** -> `Channel#confirmSelect`



## 5.1 单个确认发布
它是一种同步确认发布的方式，每次发布一个消息之后只有消息被确认发布了它才会继续发布下一个消息，`waitForConfirmsOrDie(long)`这个方法只有在消息被确认的时候才返回，**如果在指定时间范围内这个消息没有被确认那么它将抛出异常**

**缺点**：发布速度特别慢，它要等前面的确认返回才会发布下一条消息，这种方式最多提供每秒不超过数百条发布消息的吞吐量

****

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



## 5.2 批量确认发布
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



## 5.3 异步确认发布
异步确认虽然编程逻辑比上两个要复杂，但是性价比最高，无论是可靠性还是效率都没得说，他是利用回调函数来达到消息可靠性传递的，这个中间件也是通过函数回调来保证是否投递成功，下面就让我们来详细讲解异步确认是怎么实现的。

<!-- 这是一张图片，ocr 内容为：未确认收到回调NACKCALLBACK 确认收到回调ACKCALLBACK MAP KEY:消息序号 BROKER VALUE:消息内容 队列ACK_QUEUE 飞机 快递门市 寄快件的人 消息生产者 交换机 -->
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690697059605-9b749b54-ef7a-452c-869a-c7cb14deb29b.png)

这种方式就是 生产者 一直发布消息，然后异步处理确认收到和未收到的消息



### 5.3.1 代码实现
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



### 5.3.2 如何处理异步未确认消息
1. 首先保存发送的消息到Map中
2. 将确认收到的消息移除Map
3. 将剩余的消息重洗再发送一遍

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



## 5.4 三者比较
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690697092622-6e285785-21e2-47ad-8214-fcb0bfdd27ba.png)
+  单独发布消息  
同步等待确认，简单，但吞吐量非常有限。 
+  批量发布消息  
批量同步等待确认，简单，合理的吞吐量，一旦出现问题但很难推断出是那条消息出现了问题 
+  异步处理  
最佳性能和资源使用，在出现错误的情况下可以很好地控制，但是实现起来稍微难些 







# 
