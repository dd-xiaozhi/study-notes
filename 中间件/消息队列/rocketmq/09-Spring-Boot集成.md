# Chapter 9: Spring Boot 集成 RocketMQ

## 9.1 环境配置

### Maven 依赖

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

### application.yml 配置

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: my-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
    retry-times-when-send-async-failed: 2
```

### 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| rocketmq.name-server | NameServer 地址，支持多个地址用分号分隔 | - |
| rocketmq.producer.group | 生产者组名称 | - |
| rocketmq.producer.send-message-timeout | 发送消息超时时间(毫秒) | 3000 |
| rocketmq.producer.retry-times-when-send-failed | 同步发送失败重试次数 | 2 |

---

## 9.2 消息发送

### RocketMQTemplate 使用

RocketMQTemplate 是 Spring Boot 集成中最重要的消息发送组件，它封装了 RocketMQ 的发送逻辑。

### 同步发送

同步发送是最常见的发送方式，发送后会阻塞等待结果。

```java
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void createOrder(Order order) {
        // 同步发送消息
        // 参数1: 主题:标签
        // 参数2: 消息体
        SendResult result = rocketMQTemplate.syncSend("order-topic:create", order);

        System.out.println("发送状态: " + result.getSendStatus());
        System.out.println("消息ID: " + result.getMsgId());
        System.out.println("队列: " + result.getMessageQueue());
    }
}
```

### 异步发送

异步发送不会阻塞线程，适合对响应时间敏感的场景。

```java
public void sendAsyncMessage(Order order) {
    rocketMQTemplate.asyncSend("order-topic:async", order, new SendCallback() {
        @Override
        public void onSuccess(SendResult sendResult) {
            System.out.println("异步发送成功: " + sendResult.getMsgId());
        }

        @Override
        public void onException(Throwable e) {
            System.err.println("异步发送失败: " + e.getMessage());
        }
    });
    // 不需要等待，继续执行其他业务
    System.out.println("消息已提交发送");
}
```

### 单向发送

单向发送不等待服务器响应，性能最高但可靠性最低。

```java
public void sendOneWayMessage(String message) {
    rocketMQTemplate.sendOneWay("log-topic:info", message);
    System.out.println("单向消息已发送");
}
```

### 带 key 和 tag 的发送

```java
public void sendMessageWithKeyAndTag(Order order) {
    org.springframework.messaging.Message<Order> message = org.springframework.messaging.support.MessageBuilder
        .withPayload(order)
        .setHeader("KEYS", order.getOrderId())
        .build();

    // 发送消息并指定 tag
    SendResult result = rocketMQTemplate.syncSend("order-topic:pay", message);

    System.out.println("消息发送成功，Key: " + order.getOrderId());
}
```

---

## 9.3 消息消费

### @RocketMQMessageListener 注解

这是 Spring Boot 集成中最常用的消息监听方式，使用注解配置消费行为。

```java
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "order-topic",
    consumerGroup = "order-consumer-group",
    selectorExpression = "create",
    messageModel = MessageModel.CLUSTERING
)
public class OrderConsumer implements RocketMQListener<Order> {

    @Override
    public void onMessage(Order order) {
        System.out.println("收到订单消息: " + order);
        // 处理订单业务逻辑
    }
}
```

### 注解参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| topic | 消息主题 | - |
| consumerGroup | 消费者组 | - |
| selectorExpression | 标签筛选表达式 | "*" |
| messageModel | 消费模式(CLUSTERING/BROADCASTING) | CLUSTERING |
| consumeThreadMax | 最大消费线程数 | 64 |

### MessageListener 实现

如果需要更灵活的消费控制，可以实现 RocketMQListener 接口。

```java
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
public class AdvancedOrderConsumer implements RocketMQListener<Order> {

    @Override
    public void onMessage(Order order) {
        try {
            // 业务处理
            processOrder(order);

            // 默认自动 ACK，抛出异常会自动重试
        } catch (Exception e) {
            // 记录异常日志
            throw new RuntimeException("订单处理失败", e);
        }
    }

    private void processOrder(Order order) {
        System.out.println("处理订单: " + order.getOrderId());
        // 具体业务逻辑
    }
}
```

### 手动确认消费

在某些场景下需要手动控制消息确认。

```java
import org.apache.rocketmq.spring.extension.annotation.ConsumeMode;
import org.apache.rocketmq.spring.extension.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.extension.listener.ConsumeRequest;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(
    topic = "payment-topic",
    consumerGroup = "payment-consumer-group",
    consumeMode = ConsumeMode.MANUAL
)
public class PaymentConsumer implements RocketMQListener<ConsumeRequest> {

    @Override
    public void onMessage(ConsumeRequest request) {
        PaymentMessage message = (PaymentMessage) request.getMessage().getPayload();

        try {
            processPayment(message);
            // 手动确认消息
            request.getAckCallback().ack();
        } catch (Exception e) {
            // 处理失败，消息会被重试
            throw e;
        }
    }
}
```

---

## 9.4 事务消息集成

### TransactionListener 配置

事务消息用于保证本地事务和消息发送的原子性。

```java
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.extension.TransactionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class AccountService implements TransactionListener {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private AccountMapper accountMapper;

    /**
     * 执行本地事务
     */
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            // 解密消息获取转账信息
            TransferDTO transfer = decryptMessage(msg);

            // 执行本地数据库事务
            accountMapper.debit(transfer.getFromAccount(), transfer.getAmount());
            accountMapper.credit(transfer.getToAccount(), transfer.getAmount());

            // 本地事务成功，提交事务消息
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            // 本地事务失败，回滚事务消息
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }

    /**
     * 回查本地事务状态
     * 当 RocketMQ 服务端未收到提交/回滚确认时，会调用此方法
     */
    @Override
    public LocalTransactionState checkLocalTransaction(Message msg) {
        try {
            TransferDTO transfer = decryptMessage(msg);

            // 查询本地事务是否已成功
            TransactionStatus status = accountMapper.getTransactionStatus(transfer.getTransactionId());

            if (status == TransactionStatus.COMPLETED) {
                return LocalTransactionState.COMMIT_MESSAGE;
            } else if (status == TransactionStatus.FAILED) {
                return LocalTransactionState.ROLLBACK_MESSAGE;
            } else {
                return LocalTransactionState.UNKNOWN;
            }
        } catch (Exception e) {
            return LocalTransactionState.UNKNOWN;
        }
    }
}
```

### 使用 RocketMQTemplate 发送事务消息

```java
@Service
public class TransferService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    public void transfer(TransferDTO transfer) {
        // 构建消息
        org.springframework.messaging.Message<TransferDTO> message =
            org.springframework.messaging.support.MessageBuilder
                .withPayload(transfer)
                .setHeader("TRANSACTION_ID", transfer.getTransactionId())
                .build();

        // 发送事务消息
        // 第三个参数用于传递给 executeLocalTransaction 的 arg
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
            "transfer-topic:transaction",
            message,
            transfer
        );

        System.out.println("事务消息发送状态: " + result.getSendStatus());
    }
}
```

### 事务消息完整示例

```java
// 转账服务完整实现
@Service
public class TransferService {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private AccountService accountService;

    public void transfer(TransferDTO transfer) {
        // 构建消息
        org.springframework.messaging.Message<TransferDTO> message =
            org.springframework.messaging.support.MessageBuilder
                .withPayload(transfer)
                .setHeader("TRANSACTION_ID", transfer.getTransactionId())
                .build();

        // 发送事务消息
        // 执行顺序:
        // 1. 执行本地事务 (executeLocalTransaction)
        // 2. 根据本地事务结果提交或回滚消息
        // 3. 如果未收到确认，回查本地事务状态 (checkLocalTransaction)
        TransactionSendResult result = rocketMQTemplate.sendMessageInTransaction(
            "transfer-topic:transaction",
            message,
            transfer
        );

        // 打印发送结果
        System.out.println("事务消息发送状态: " + result.getSendStatus());

        // 如果是 UNKNOWN 状态，需要等待回查
        if (result.getSendStatus() == SendStatus.UNKNOWN) {
            System.out.println("事务状态未知，等待回查...");
        }
    }
}

// 转账消费者
@Component
@RocketMQMessageListener(
    topic = "transfer-topic",
    consumerGroup = "transfer-consumer-group",
    selectorExpression = "transaction"
)
public class TransferConsumer implements RocketMQListener<TransferDTO> {

    @Override
    public void onMessage(TransferDTO transfer) {
        // 只有当本地事务提交后，这里才会收到消息
        System.out.println("收到转账确认消息: " + transfer.getTransactionId());
        // 更新转账状态等后续处理
    }
}
```

---

## 9.5 最佳实践

### 1. 消息发送最佳实践

- 合理设置超时时间，避免线程阻塞
- 异步发送用于对延迟不敏感的场景
- 重要消息使用同步发送并处理失败重试

### 2. 消息消费最佳实践

- 消费逻辑要幂等，避免重复消费导致的问题
- 使用 try-catch 包裹业务逻辑，异常时抛出以便重试
- 合理配置消费线程数，避免资源浪费

### 3. 事务消息最佳实践

- 本地事务执行要快速，避免占用 RocketMQ 事务资源
- 实现回查逻辑时注意处理 UNKNOWN 状态
- 事务消息的 topic 要和普通消息分开，避免互相影响

### 4. 配置建议

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: my-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 3
    max-message-size: 4194304  # 4MB
  consumer:
    max-reconsume-times: 3
    suspend-time-millis: 1000
```
