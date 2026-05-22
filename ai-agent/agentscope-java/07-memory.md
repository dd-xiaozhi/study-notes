# 第七章：记忆系统

记忆系统是 AgentScope Java 框架的核心组件之一，它负责管理和持久化 Agent 与用户之间的对话历史。本章将详细介绍记忆的类型与实现、消息累加器机制、对话历史管理，以及记忆压缩与摘要功能。

## 7.1 记忆类型与实现

AgentScope Java 提供了两种主要的记忆类型：会话级记忆（Memory）和长期记忆（LongTermMemory）。

### 7.1.1 会话级记忆（Memory）

会话级记忆用于存储单个会话内的对话历史，是 Agent 最基础的记忆组件。

**Memory 接口定义：**

```java
public interface Memory extends StateModule {
    /**
     * 添加消息到记忆
     */
    void addMessage(Msg message);

    /**
     * 获取所有存储的消息
     */
    List<Msg> getMessages();

    /**
     * 删除指定索引的消息
     */
    void deleteMessage(int index);

    /**
     * 清空所有消息
     */
    void clear();
}
```

**InMemoryMemory 实现：**

`InMemoryMemory` 是 Memory 接口的内存实现，采用线程安全的 `CopyOnWriteArrayList` 存储消息，并支持通过 Session 进行状态持久化。

```java
public class InMemoryMemory implements Memory {
    private final List<Msg> messages = new CopyOnWriteArrayList<>();
    private static final String KEY_PREFIX = "memory";

    @Override
    public void addMessage(Msg message) {
        messages.add(message);
    }

    @Override
    public List<Msg> getMessages() {
        return messages.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public void saveTo(Session session, SessionKey sessionKey) {
        session.save(sessionKey, KEY_PREFIX + "_messages", new ArrayList<>(messages));
    }

    @Override
    public void loadFrom(Session session, SessionKey sessionKey) {
        List<Msg> loaded = session.getList(sessionKey, KEY_PREFIX + "_messages", Msg.class);
        messages.clear();
        messages.addAll(loaded);
    }
}
```

### 7.1.2 长期记忆（LongTermMemory）

长期记忆允许 Agent 跨会话记忆用户偏好、重要事实等信息，适用于多会话场景下的个性化服务。

**LongTermMemory 接口：**

```java
public interface LongTermMemory {
    /**
     * 记录消息到长期记忆
     */
    Mono<Void> record(List<Msg> msgs);

    /**
     * 根据输入检索相关记忆
     */
    Mono<String> retrieve(Msg msg);
}
```

### 7.1.3 记忆模式配置

`LongTermMemoryMode` 枚举控制长期记忆的集成方式：

| 模式 | 说明 |
|------|------|
| `AGENT_CONTROL` | Agent 通过工具调用主动管理记忆 |
| `STATIC_CONTROL` | 框架自动管理记忆，无需 Agent 介入 |
| `BOTH` | 结合两种模式（推荐默认值） |

**配置示例：**

```java
ReActAgent agent = ReActAgent.builder()
    .name("Assistant")
    .model(model)
    .memory(new InMemoryMemory())  // 会话级记忆
    .longTermMemory(longTermMemory)  // 长期记忆
    .longTermMemoryMode(LongTermMemoryMode.BOTH)  // 推荐模式
    .build();
```

## 7.2 消息累加器（Accumulator）

消息累加器用于处理流式响应中的内容块，将分散的块聚合为完整的消息。

### 7.2.1 ContentAccumulator 接口

```java
public interface ContentAccumulator<T extends ContentBlock> {
    /**
     * 添加内容块到累加器
     */
    void add(T block);

    /**
     * 检查是否有累积内容
     */
    boolean hasContent();

    /**
     * 构建聚合后的内容块
     */
    ContentBlock buildAggregated();

    /**
     * 重置累加器状态
     */
    void reset();
}
```

### 7.2.2 累加器类型

| 类型 | 说明 |
|------|------|
| `TextAccumulator` | 累积文本内容块 |
| `ThinkingAccumulator` | 累积思考过程块 |
| `ToolCallsAccumulator` | 累积工具调用块 |

### 7.2.3 ReasoningContext

`ReasoningContext` 是 ReAct Agent 核心的上下文类，负责在推理过程中聚合流式响应：

```java
public class ReasoningContext {
    private final String agentName;
    private final TextAccumulator textAccumulator;
    private final ThinkingAccumulator thinkingAccumulator;
    private final ToolCallsAccumulator toolCallsAccumulator;
    private ChatUsage chatUsage;

    /**
     * 处理流式块，返回增量消息
     */
    public List<Msg> processChunk(Event event) {
        // 处理不同类型的 ContentBlock
    }

    /**
     * 构建最终消息
     */
    public Msg buildFinalMessage() {
        // 聚合所有累积内容生成完整消息
    }
}
```

## 7.3 对话历史管理

### 7.3.1 Session 接口

Session 接口提供了状态持久化的核心能力：

```java
public interface Session {
    /**
     * 保存单个状态值
     */
    void save(SessionKey sessionKey, String key, State value);

    /**
     * 保存列表状态值
     */
    void save(SessionKey sessionKey, String key, List<? extends State> values);

    /**
     * 获取单个状态值
     */
    <T extends State> Optional<T> get(SessionKey sessionKey, String key, Class<T> type);

    /**
     * 获取列表状态值
     */
    <T extends State> List<T> getList(SessionKey sessionKey, String key, Class<T> itemType);

    /**
     * 检查会话是否存在
     */
    boolean exists(SessionKey sessionKey);

    /**
     * 删除会话
     */
    void delete(SessionKey sessionKey);
}
```

### 7.3.2 状态持久化流程

```
┌─────────────┐     saveTo()      ┌─────────────┐
│   Agent     │ ────────────────> │   Session   │
│             │                   │             │
│  Memory ───────── saveTo() ────>│  (JSON/H2)  │
└─────────────┘                   └─────────────┘
       ^                               │
       │                               │
       │      loadFrom()               │
       └───────────────────────────────┘
```

### 7.3.3 内存会话实现

```java
public class InMemorySession implements Session {
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(SessionKey sessionKey, String key, State value) {
        String sessionKeyStr = serializeSessionKey(sessionKey);
        SessionData data = sessions.computeIfAbsent(sessionKeyStr, k -> new SessionData());
        data.setSingleState(key, value);
    }
}
```

## 7.4 记忆压缩与摘要

当 Agent 达到最大迭代次数或遇到异常时，框架会自动触发摘要机制，将冗长的对话历史压缩为简洁的摘要。

### 7.4.1 摘要相关事件

| 事件类型 | 说明 |
|----------|------|
| `PreSummaryEvent` | 摘要生成前触发，可修改输入消息和生成选项 |
| `PostSummaryEvent` | 摘要生成后触发，可访问生成的摘要消息 |
| `SummaryChunkEvent` | 摘要流式输出时的增量事件 |

### 7.4.2 摘要生成流程

```java
// ReActAgent 中的摘要逻辑
protected Mono<Msg> summarizing() {
    List<Msg> messageList = prepareSummaryMessages();
    GenerateOptions generateOptions = buildGenerateOptions();

    return notifyPreSummaryHook(messageList, generateOptions)
        .flatMap(preSummaryEvent -> {
            // 流式生成摘要
            return streamAndAccumulateSummary(
                preSummaryEvent.getInputMessages(),
                preSummaryEvent.getEffectiveGenerateOptions()
            );
        })
        .flatMap(msg -> notifyPostSummaryHook(msg, generateOptions))
        .map(postEvent -> {
            Msg finalMsg = postEvent.getSummaryMessage();
            memory.addMessage(finalMsg);  // 将摘要添加到记忆
            return finalMsg;
        });
}
```

### 7.4.3 自定义摘要 Hook

```java
public class CustomSummaryHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreSummaryEvent e) {
            // 在摘要生成前添加额外的上下文
            String enhancedPrompt = "请用简洁的语言总结以下对话的核心要点：";
            // 修改输入消息...
        }
        return Mono.just(event);
    }
}

// 使用自定义 Hook
ReActAgent agent = ReActAgent.builder()
    .name("Assistant")
    .model(model)
    .hook(new CustomSummaryHook())
    .build();
```

## 7.5 【案例】实现持久化对话记忆

本案例展示如何使用 Spring Boot 3 + Java 21 + H2 数据库实现持久化对话记忆系统。

### 7.5.1 项目结构

```
chapter07-memory/
├── pom.xml
├── src/main/java/io/agentscope/tutorial/chapter07/
│   ├── Chapter07Application.java
│   ├── config/
│   │   └── AgentConfig.java
│   ├── memory/
│   │   ├── H2DatabaseMemory.java
│   │   ├── DatabaseSession.java
│   │   └── MemoryService.java
│   ├── entity/
│   │   └── ConversationMessage.java
│   ├── repository/
│   │   └── MessageRepository.java
│   └── controller/
│       └── ChatController.java
└── src/main/resources/
    ├── application.yml
    └── data.sql
```

### 7.5.2 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter07-memory</artifactId>
    <version>1.0.0</version>
    <name>chapter07-memory</name>
    <description>AgentScope Java 教程 - 第七章：记忆系统</description>

    <properties>
        <java.version>21</java.version>
        <agentscope.version>1.0.0</agentscope.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- H2 数据库 -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 7.5.3 application.yml

```yaml
spring:
  application:
    name: chapter07-memory
  datasource:
    url: jdbc:h2:file:./data/agentscope
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  sql:
    init:
      mode: always

server:
  port: 8080

# AgentScope 配置
agentscope:
  model:
    type: dashscope
    api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
    model-name: qwen-plus
```

### 7.5.4 实体类：ConversationMessage

```java
package io.agentscope.tutorial.chapter07.entity;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.state.State;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 对话消息实体类
 *
 * 存储对话历史到 H2 数据库，支持跨会话查询和管理。
 */
@Entity
@Table(name = "conversation_messages",
       indexes = {
           @Index(name = "idx_session_id", columnList = "sessionId"),
           @Index(name = "idx_created_at", columnList = "createdAt")
       })
public class ConversationMessage implements State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 会话标识符
     */
    @Column(name = "session_id", nullable = false, length = 255)
    private String sessionId;

    /**
     * 消息角色：user, assistant, system
     */
    @Column(name = "role", nullable = false, length = 50)
    private String role;

    /**
     * 消息内容（JSON 格式存储多模态内容）
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 消息发送者名称
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * 消息创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 消息索引（用于维护对话顺序）
     */
    @Column(name = "message_index")
    private Integer messageIndex;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // 构造函数
    public ConversationMessage() {
    }

    public ConversationMessage(String sessionId, String role, String content, String name) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.name = name;
    }

    /**
     * 从 Msg 转换为实体对象
     */
    public static ConversationMessage fromMsg(Msg msg, String sessionId, int index) {
        ConversationMessage entity = new ConversationMessage();
        entity.setSessionId(sessionId);
        entity.setRole(msg.getRole().name().toLowerCase());
        entity.setContent(msg.getContentAsString());
        entity.setName(msg.getName());
        entity.setMessageIndex(index);
        return entity;
    }

    /**
     * 转换为 Msg 对象
     */
    public Msg toMsg() {
        return Msg.builder()
                .name(name)
                .role(MsgRole.valueOf(role.toUpperCase()))
                .content(io.agentscope.core.message.TextBlock.builder()
                        .text(content)
                        .build())
                .build();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }
}
```

### 7.5.5 仓库接口：MessageRepository

```java
package io.agentscope.tutorial.chapter07.repository;

import io.agentscope.tutorial.chapter07.entity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 消息数据访问接口
 *
 * 提供对话消息的数据库查询和管理功能。
 */
@Repository
public interface MessageRepository extends JpaRepository<ConversationMessage, Long> {

    /**
     * 根据会话ID查询所有消息，按索引排序
     */
    List<ConversationMessage> findBySessionIdOrderByMessageIndex(String sessionId);

    /**
     * 根据会话ID和消息角色查询
     */
    List<ConversationMessage> findBySessionIdAndRoleOrderByMessageIndex(
            String sessionId, String role);

    /**
     * 统计会话中的消息数量
     */
    long countBySessionId(String sessionId);

    /**
     * 根据会话ID删除所有消息
     */
    void deleteBySessionId(String sessionId);

    /**
     * 查询最近的N条消息
     */
    @Query(value = "SELECT * FROM conversation_messages " +
                   "WHERE session_id = :sessionId " +
                   "ORDER BY message_index DESC " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<ConversationMessage> findRecentMessages(
            @Param("sessionId") String sessionId,
            @Param("limit") int limit);

    /**
     * 查询时间范围内的消息
     */
    @Query("SELECT m FROM ConversationMessage m " +
           "WHERE m.sessionId = :sessionId " +
           "AND m.createdAt >= :startTime " +
           "AND m.createdAt <= :endTime " +
           "ORDER BY m.messageIndex")
    List<ConversationMessage> findMessagesInTimeRange(
            @Param("sessionId") String sessionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 搜索消息内容
     */
    @Query("SELECT m FROM ConversationMessage m " +
           "WHERE m.sessionId = :sessionId " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY m.messageIndex")
    List<ConversationMessage> searchMessages(
            @Param("sessionId") String sessionId,
            @Param("keyword") String keyword);
}
```

### 7.5.6 数据库会话实现：DatabaseSession

```java
package io.agentscope.tutorial.chapter07.memory;

import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.State;
import io.agentscope.tutorial.chapter07.entity.ConversationMessage;
import io.agentscope.tutorial.chapter07.repository.MessageRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于数据库的 Session 实现
 *
 * 使用 JPA Repository 存储和检索状态，支持持久化记忆。
 */
@Component
public class DatabaseSession implements Session {

    private final MessageRepository messageRepository;

    /**
     * 内存缓存，用于存储未持久化的状态
     */
    private final Map<String, Map<String, State>> memoryCache = new ConcurrentHashMap<>();

    public DatabaseSession(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void save(SessionKey sessionKey, String key, State value) {
        String sk = serializeSessionKey(sessionKey);
        memoryCache.computeIfAbsent(sk, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    @Override
    public void save(SessionKey sessionKey, String key, List<? extends State> values) {
        String sk = serializeSessionKey(sessionKey);

        // 如果是消息列表，持久化到数据库
        if (values != null && !values.isEmpty() && values.get(0) instanceof ConversationMessage) {
            @SuppressWarnings("unchecked")
            List<ConversationMessage> messages = (List<ConversationMessage>) (List<?>) values;

            // 获取现有消息数量以确定起始索引
            long existingCount = messageRepository.countBySessionId(sk);
            int startIndex = (int) existingCount;

            // 设置索引并保存
            for (int i = 0; i < messages.size(); i++) {
                ConversationMessage msg = messages.get(i);
                if (msg.getMessageIndex() == null) {
                    msg.setMessageIndex(startIndex + i);
                }
            }

            messageRepository.saveAll(messages);
        }

        // 同时缓存到内存
        memoryCache.computeIfAbsent(sk, k -> new ConcurrentHashMap<>()).put(key, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends State> Optional<T> get(SessionKey sessionKey, String key, Class<T> type) {
        String sk = serializeSessionKey(sessionKey);
        Map<String, State> sessionData = memoryCache.get(sk);

        if (sessionData == null) {
            return Optional.empty();
        }

        State state = sessionData.get(key);
        if (state == null) {
            return Optional.empty();
        }

        return Optional.of(type.cast(state));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends State> List<T> getList(SessionKey sessionKey, String key, Class<T> itemType) {
        String sk = serializeSessionKey(sessionKey);

        // 如果是消息类型，从数据库加载
        if (ConversationMessage.class.isAssignableFrom(itemType)) {
            List<ConversationMessage> messages =
                    messageRepository.findBySessionIdOrderByMessageIndex(sk);
            return (List<T>) messages;
        }

        // 其他类型从缓存获取
        Map<String, State> sessionData = memoryCache.get(sk);
        if (sessionData == null) {
            return List.of();
        }

        State state = sessionData.get(key);
        if (state instanceof List) {
            return (List<T>) state;
        }

        return List.of();
    }

    @Override
    public boolean exists(SessionKey sessionKey) {
        String sk = serializeSessionKey(sessionKey);
        return memoryCache.containsKey(sk) ||
               messageRepository.countBySessionId(sk) > 0;
    }

    @Override
    public void delete(SessionKey sessionKey) {
        String sk = serializeSessionKey(sessionKey);
        memoryCache.remove(sk);
        messageRepository.deleteBySessionId(sk);
    }

    @Override
    public void delete(SessionKey sessionKey, String key) {
        String sk = serializeSessionKey(sessionKey);
        Map<String, State> sessionData = memoryCache.get(sk);
        if (sessionData != null) {
            sessionData.remove(key);
        }
    }

    @Override
    public Set<SessionKey> listSessionKeys() {
        Set<SessionKey> keys = new HashSet<>();

        // 从缓存获取会话键
        memoryCache.keySet().forEach(key -> keys.add(new SimpleSessionKey(key)));

        // 从数据库获取会话键
        messageRepository.findAll().stream()
                .map(ConversationMessage::getSessionId)
                .distinct()
                .forEach(sessionId -> keys.add(new SimpleSessionKey(sessionId)));

        return keys;
    }

    @Override
    public void close() {
        memoryCache.clear();
    }

    private String serializeSessionKey(SessionKey sessionKey) {
        if (sessionKey instanceof SimpleSessionKey simple) {
            return simple.sessionId();
        }
        return sessionKey.toString();
    }

    /**
     * 简单的 SessionKey 实现
     */
    public record SimpleSessionKey(String sessionId) implements SessionKey {
        @Override
        public String toString() {
            return sessionId;
        }
    }
}
```

### 7.5.7 H2 数据库记忆实现：H2DatabaseMemory

```java
package io.agentscope.tutorial.chapter07.memory;

import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.State;
import io.agentscope.tutorial.chapter07.entity.ConversationMessage;
import io.agentscope.tutorial.chapter07.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 基于 H2 数据库的记忆实现
 *
 * 提供持久化的对话历史存储，支持跨会话查询和记忆管理。
 * 消息存储在数据库中，同时在内存中保留缓存以提高读取性能。
 */
@Component
public class H2DatabaseMemory implements Memory {

    private static final Logger log = LoggerFactory.getLogger(H2DatabaseMemory.class);

    private static final String KEY_PREFIX = "memory";

    private final MessageRepository messageRepository;
    private final DatabaseSession databaseSession;

    /**
     * 内存缓存，用于快速访问
     */
    private final List<Msg> messagesCache = new CopyOnWriteArrayList<>();

    /**
     * 当前会话ID
     */
    private String currentSessionId;

    public H2DatabaseMemory(MessageRepository messageRepository, DatabaseSession databaseSession) {
        this.messageRepository = messageRepository;
        this.databaseSession = databaseSession;
    }

    /**
     * 设置当前会话ID
     */
    public void setSessionId(String sessionId) {
        this.currentSessionId = sessionId;

        // 加载该会话的历史消息到缓存
        if (sessionId != null) {
            reloadFromDatabase();
        }
    }

    /**
     * 从数据库重新加载消息到缓存
     */
    private void reloadFromDatabase() {
        if (currentSessionId == null) {
            return;
        }

        List<ConversationMessage> entities =
                messageRepository.findBySessionIdOrderByMessageIndex(currentSessionId);

        messagesCache.clear();
        for (ConversationMessage entity : entities) {
            messagesCache.add(entity.toMsg());
        }

        log.debug("Loaded {} messages from database for session: {}",
                messagesCache.size(), currentSessionId);
    }

    @Override
    public void addMessage(Msg message) {
        messagesCache.add(message);

        // 如果有会话ID，立即持久化到数据库
        if (currentSessionId != null) {
            ConversationMessage entity = ConversationMessage.fromMsg(
                    message, currentSessionId, messagesCache.size() - 1);
            messageRepository.save(entity);
        }
    }

    @Override
    public List<Msg> getMessages() {
        return messagesCache.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMessage(int index) {
        if (index >= 0 && index < messagesCache.size()) {
            messagesCache.remove(index);
            // 注意：这里不会从数据库删除，只是从缓存移除
            // 实际应用中可能需要同步删除数据库记录
        }
    }

    @Override
    public void clear() {
        messagesCache.clear();

        if (currentSessionId != null) {
            messageRepository.deleteBySessionId(currentSessionId);
        }
    }

    @Override
    public void saveTo(Session session, SessionKey sessionKey) {
        // 保存到 Session（用于状态持久化）
        session.save(sessionKey, KEY_PREFIX + "_messages",
                new ArrayList<>(messagesCache));
    }

    @Override
    public void loadFrom(Session session, SessionKey sessionKey) {
        // 从 Session 加载
        List<ConversationMessage> messages =
                session.getList(sessionKey, KEY_PREFIX + "_messages",
                        ConversationMessage.class);

        messagesCache.clear();
        for (ConversationMessage entity : messages) {
            messagesCache.add(entity.toMsg());
        }
    }

    /**
     * 搜索包含特定关键词的消息
     */
    public List<Msg> searchMessages(String keyword) {
        if (currentSessionId == null || keyword == null || keyword.isEmpty()) {
            return List.of();
        }

        List<ConversationMessage> results =
                messageRepository.searchMessages(currentSessionId, keyword);

        return results.stream()
                .map(ConversationMessage::toMsg)
                .collect(Collectors.toList());
    }

    /**
     * 获取最近的消息
     */
    public List<Msg> getRecentMessages(int limit) {
        return messageRepository.findRecentMessages(currentSessionId, limit)
                .stream()
                .map(ConversationMessage::toMsg)
                .collect(Collectors.toList());
    }

    /**
     * 获取消息统计信息
     */
    public MemoryStats getStats() {
        if (currentSessionId == null) {
            return new MemoryStats(0, 0, 0, 0);
        }

        long totalCount = messageRepository.countBySessionId(currentSessionId);
        long userCount = messageRepository
                .findBySessionIdAndRoleOrderByMessageIndex(currentSessionId, "user")
                .size();
        long assistantCount = messageRepository
                .findBySessionIdAndRoleOrderByMessageIndex(currentSessionId, "assistant")
                .size();

        return new MemoryStats(totalCount, userCount, assistantCount,
                messagesCache.size());
    }

    /**
     * 记忆统计信息
     */
    public record MemoryStats(
            long totalInDatabase,
            long userMessageCount,
            long assistantMessageCount,
            int cachedCount
    ) {}
}
```

### 7.5.8 记忆服务：MemoryService

```java
package io.agentscope.tutorial.chapter07.memory;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.state.StatePersistence;
import io.agentscope.tutorial.chapter07.entity.ConversationMessage;
import io.agentscope.tutorial.chapter07.repository.MessageRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记忆管理服务
 *
 * 提供统一的记忆管理接口，支持多会话管理和记忆持久化。
 */
@Service
public class MemoryService {

    private static final Logger log = LoggerFactory.getLogger(MemoryService.class);

    private final MessageRepository messageRepository;
    private final DatabaseSession databaseSession;

    /**
     * 存储每个会话的 Agent 实例
     */
    private final Map<String, ReActAgent> sessionAgents = new ConcurrentHashMap<>();

    /**
     * 存储每个会话的记忆实例
     */
    private final Map<String, H2DatabaseMemory> sessionMemories = new ConcurrentHashMap<>();

    public MemoryService(MessageRepository messageRepository, DatabaseSession databaseSession) {
        this.messageRepository = messageRepository;
        this.databaseSession = databaseSession;
    }

    /**
     * 为指定会话创建或获取 Agent
     */
    public ReActAgent getOrCreateAgent(String sessionId, ReActAgent baseAgent) {
        return sessionAgents.computeIfAbsent(sessionId, id -> {
            // 创建新的 Agent 实例，使用该会话的记忆
            H2DatabaseMemory memory = new H2DatabaseMemory(messageRepository, databaseSession);
            memory.setSessionId(id);

            ReActAgent agent = ReActAgent.builder()
                    .name(baseAgent.getName())
                    .sysPrompt(baseAgent.getSysPrompt())
                    .model(baseAgent.getModel())
                    .toolkit(baseAgent.getMemory() != null ? createToolkitFromMemory(memory) : null)
                    .memory(memory)
                    .maxIters(baseAgent.getMaxIters())
                    .statePersistence(StatePersistence.builder()
                            .memoryManaged(true)
                            .build())
                    .build();

            sessionMemories.put(id, memory);

            log.info("Created new agent for session: {}", id);
            return agent;
        });
    }

    /**
     * 创建工具包（从记忆中提取）
     */
    private io.agentscope.core.tool.Toolkit createToolkitFromMemory(H2DatabaseMemory memory) {
        io.agentscope.core.tool.Toolkit toolkit = new io.agentscope.core.tool.Toolkit();

        // 注册记忆管理工具
        toolkit.registerTool(new MemoryManagementTool(memory));

        return toolkit;
    }

    /**
     * 获取指定会话的消息历史
     */
    public List<ConversationMessage> getConversationHistory(String sessionId) {
        return messageRepository.findBySessionIdOrderByMessageIndex(sessionId);
    }

    /**
     * 清除指定会话的记忆
     */
    public void clearSessionMemory(String sessionId) {
        H2DatabaseMemory memory = sessionMemories.get(sessionId);
        if (memory != null) {
            memory.clear();
        }
        sessionAgents.remove(sessionId);
        sessionMemories.remove(sessionId);

        log.info("Cleared memory for session: {}", sessionId);
    }

    /**
     * 获取记忆统计信息
     */
    public H2DatabaseMemory.MemoryStats getMemoryStats(String sessionId) {
        H2DatabaseMemory memory = sessionMemories.get(sessionId);
        if (memory != null) {
            return memory.getStats();
        }
        return new H2DatabaseMemory.MemoryStats(0, 0, 0, 0);
    }

    /**
     * 搜索会话中的消息
     */
    public List<io.agentscope.core.message.Msg> searchMessages(String sessionId, String keyword) {
        H2DatabaseMemory memory = sessionMemories.get(sessionId);
        if (memory != null) {
            return memory.searchMessages(keyword);
        }
        return List.of();
    }

    /**
     * 记忆管理工具（供 Agent 调用）
     */
    public static class MemoryManagementTool {

        private final H2DatabaseMemory memory;

        public MemoryManagementTool(H2DatabaseMemory memory) {
            this.memory = memory;
        }

        /**
         * 记住重要信息
         */
        @io.agentscope.core.tool.Tool(
                description = "记录重要信息到记忆系统，用于跨会话记住用户偏好和关键事实"
        )
        public String remember(
                @io.agentscope.core.tool.ToolParam(name = "content", description = "要记住的内容") String content) {
            io.agentscope.core.message.Msg msg = io.agentscope.core.message.Msg.builder()
                    .name("system")
                    .role(io.agentscope.core.message.MsgRole.SYSTEM)
                    .content(io.agentscope.core.message.TextBlock.builder()
                            .text("[记忆] " + content)
                            .build())
                    .build();
            memory.addMessage(msg);
            return "已记住: " + content;
        }

        /**
         * 搜索记忆
         */
        @io.agentscope.core.tool.Tool(
                description = "从记忆系统中搜索相关信息"
        )
        public String recall(
                @io.agentscope.core.tool.ToolParam(name = "keyword", description = "搜索关键词") String keyword) {
            List<io.agentscope.core.message.Msg> results = memory.searchMessages(keyword);
            if (results.isEmpty()) {
                return "没有找到与 '" + keyword + "' 相关的记忆";
            }
            return "找到 " + results.size() + " 条相关记忆:\n" +
                    results.stream()
                            .map(m -> "- " + m.getContentAsString())
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("");
        }

        /**
         * 获取最近记忆
         */
        @io.agentscope.core.tool.Tool(
                description = "获取最近的 N 条记忆"
        )
        public String getRecent(
                @io.agentscope.core.tool.ToolParam(name = "count", description = "记忆数量") int count) {
            List<io.agentscope.core.message.Msg> results = memory.getRecentMessages(count);
            if (results.isEmpty()) {
                return "没有最近的记忆";
            }
            return "最近 " + results.size() + " 条记忆:\n" +
                    results.stream()
                            .map(m -> "- " + m.getContentAsString())
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("");
        }
    }
}
```

### 7.5.9 Agent 配置：AgentConfig

```java
package io.agentscope.tutorial.chapter07.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置类
 *
 * 配置 Agent 实例的默认参数和基础组件。
 */
@Configuration
public class AgentConfig {

    @Value("${agentscope.model.api-key}")
    private String apiKey;

    @Value("${agentscope.model.model-name:qwen-plus}")
    private String modelName;

    /**
     * 创建基础 Agent 配置
     *
     * 这个 Agent 配置作为模板，实际使用时会被 MemoryService 包装
     */
    @Bean
    public ReActAgent baseAgent(Model model) {
        String systemPrompt = """
                你是一个智能助手，具备强大的对话能力和记忆功能。
                你可以：
                - 回答各种问题
                - 记住用户的重要偏好和信息
                - 在需要时搜索历史记忆
                - 使用工具辅助完成任务

                请友好、耐心地与用户交流。
                """;

        return ReActAgent.builder()
                .name("Assistant")
                .sysPrompt(systemPrompt)
                .model(model)
                .memory(new InMemoryMemory())
                .maxIters(10)
                .build();
    }
}
```

### 7.5.10 控制器：ChatController

```java
package io.agentscope.tutorial.chapter07.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter07.memory.H2DatabaseMemory;
import io.agentscope.tutorial.chapter07.memory.MemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天控制器
 *
 * 提供 REST API 进行对话和记忆管理。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final MemoryService memoryService;
    private final ReActAgent baseAgent;

    public ChatController(MemoryService memoryService, ReActAgent baseAgent) {
        this.memoryService = memoryService;
        this.baseAgent = baseAgent;
    }

    /**
     * 发送消息并获取回复
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestBody Map<String, String> request) {

        String userMessage = request.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "消息不能为空"));
        }

        try {
            // 获取或创建该会话的 Agent
            ReActAgent agent = memoryService.getOrCreateAgent(sessionId, baseAgent);

            // 构建用户消息
            Msg userMsg = Msg.builder()
                    .name("user")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(userMessage).build())
                    .build();

            // 调用 Agent
            Msg response = agent.call(userMsg).block();

            // 构建响应
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", sessionId);
            result.put("userMessage", userMessage);
            result.put("response", response != null ? response.getContentAsString() : "无响应");
            result.put("memoryStats", memoryService.getMemoryStats(sessionId));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "处理消息时出错: " + e.getMessage()));
        }
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam(required = false) Integer limit) {

        List<?> history;
        if (limit != null && limit > 0) {
            history = memoryService.getConversationHistory(sessionId)
                    .stream()
                    .skip(Math.max(0, memoryService.getConversationHistory(sessionId).size() - limit))
                    .collect(Collectors.toList());
        } else {
            history = memoryService.getConversationHistory(sessionId);
        }

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "count", history.size(),
                "history", history
        ));
    }

    /**
     * 清除会话记忆
     */
    @DeleteMapping("/memory")
    public ResponseEntity<Map<String, Object>> clearMemory(
            @RequestParam(defaultValue = "default") String sessionId) {

        memoryService.clearSessionMemory(sessionId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "message", "记忆已清除"
        ));
    }

    /**
     * 搜索记忆
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMemory(
            @RequestParam(defaultValue = "default") String sessionId,
            @RequestParam String keyword) {

        List<Msg> results = memoryService.searchMessages(sessionId, keyword);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "keyword", keyword,
                "count", results.size(),
                "results", results.stream()
                        .map(Msg::getContentAsString)
                        .collect(Collectors.toList())
        ));
    }

    /**
     * 获取记忆统计
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(defaultValue = "default") String sessionId) {

        H2DatabaseMemory.MemoryStats stats = memoryService.getMemoryStats(sessionId);

        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "totalMessages", stats.totalInDatabase(),
                "userMessages", stats.userMessageCount(),
                "assistantMessages", stats.assistantMessageCount(),
                "cachedMessages", stats.cachedCount()
        ));
    }

    /**
     * 根路径响应
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> index() {
        return ResponseEntity.ok(Map.of(
                "service", "AgentScope Java 第七章：记忆系统",
                "version", "1.0.0",
                "endpoints", "/api/chat/send, /api/chat/history, /api/chat/search, /api/chat/stats"
        ));
    }
}
```

### 7.5.11 应用主类：Chapter07Application

```java
package io.agentscope.tutorial.chapter07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java 教程 - 第七章：记忆系统
 *
 * 演示如何使用 H2 数据库实现持久化对话记忆。
 *
 * 访问 http://localhost:8080/h2-console 连接数据库控制台
 * JDBC URL: jdbc:h2:file:./data/agentscope
 */
@SpringBootApplication
public class Chapter07Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter07Application.class, args);
    }
}
```

### 7.5.12 使用示例

**1. 启动应用：**

```bash
cd chapter07-memory
mvn spring-boot:run
```

**2. 发送对话请求：**

```bash
# 发送消息
curl -X POST "http://localhost:8080/api/chat/send?sessionId=user001" \
  -H "Content-Type: application/json" \
  -d '{"message": "我叫张三，住在杭州"}'

# 后续对话，Agent 会记住用户信息
curl -X POST "http://localhost:8080/api/chat/send?sessionId=user001" \
  -H "Content-Type: application/json" \
  -d '{"message": "我叫什么名字？"}'
```

**3. 查询记忆：**

```bash
# 获取记忆统计
curl "http://localhost:8080/api/chat/stats?sessionId=user001"

# 搜索记忆
curl "http://localhost:8080/api/chat/search?sessionId=user001&keyword=杭州"

# 获取对话历史
curl "http://localhost:8080/api/chat/history?sessionId=user001"
```

**4. 响应示例：**

```json
// GET /api/chat/stats?sessionId=user001
{
  "sessionId": "user001",
  "totalMessages": 4,
  "userMessages": 2,
  "assistantMessages": 2,
  "cachedMessages": 4
}

// POST /api/chat/send?sessionId=user001
{
  "sessionId": "user001",
  "userMessage": "我叫什么名字？",
  "response": "你刚才告诉我你叫张三，住在杭州。我已经记住了这些信息。",
  "memoryStats": {
    "totalInDatabase": 4,
    "userMessageCount": 2,
    "assistantMessageCount": 2,
    "cachedCount": 4
  }
}
```

### 7.5.13 数据库表结构

应用启动后会自动创建以下表：

```sql
-- 对话消息表
CREATE TABLE conversation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT,
    name VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    message_index INTEGER,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
);
```

## 7.6 本章小结

本章介绍了 AgentScope Java 框架的记忆系统，主要内容包括：

| 概念 | 说明 |
|------|------|
| Memory 接口 | 会话级记忆，存储当前会话的对话历史 |
| InMemoryMemory | 内存实现的会话级记忆 |
| LongTermMemory | 跨会话的长期记忆接口 |
| LongTermMemoryMode | 长期记忆的三种控制模式 |
| ContentAccumulator | 流式响应的内容累加器 |
| Session 接口 | 状态持久化接口 |
| DatabaseSession | 基于数据库的 Session 实现 |
| H2DatabaseMemory | 基于 H2 数据库的记忆实现 |

通过本章的学习，你应该能够：

1. 理解 AgentScope 的记忆架构
2. 使用不同类型的记忆实现
3. 实现持久化的对话记忆系统
4. 开发和注册自定义的记忆组件

下一章将介绍多代理架构模式，敬请期待。