在本教程的这一部分中，我们将用 Java 编写两个程序；发送单个消息的生产者和接收消息并将其打印出来的消费者。

在下图中，“P”是我们的生产者，“C”是我们的消费者。中间的框是一个队列 - RabbitMQ 代表消费者保留的消息缓冲区。

<!-- 这是一张图片，ocr 内容为：C -->
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696593255-38b5f8bb-562e-4283-adf5-6764e462bb91.png)





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
![](https://cdn.nlark.com/yuque/0/2023/png/12747499/1690696674844-968a4743-9840-4b5b-b6df-2bfbec79dfe3.png)



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

