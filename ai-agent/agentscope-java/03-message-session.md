# 第三章：消息与会话管理

> 本章讲解 AgentScope Java 中消息与会话的核心概念与实战用法。

## 3.1 消息类型与格式

### 3.1.1 核心类概述

AgentScope Java 的消息系统以 `Msg` 类为核心，配合 `ContentBlock` 密封层次结构来表示各种类型的内容。

```
Msg（消息）
├── id: String           -- 唯一标识
├── name: String         -- 可选名称
├── role: MsgRole        -- 角色（USER/ASSISTANT/SYSTEM/TOOL）
├── content: List<ContentBlock>  -- 内容块列表
├── metadata: Map        -- 元数据
└── timestamp: String    -- 时间戳

ContentBlock（密封类）
├── TextBlock            -- 纯文本
├── ThinkingBlock        -- 推理思考内容
├── ImageBlock           -- 图片（URL/Base64）
├── AudioBlock           -- 音频（URL/Base64）
├── VideoBlock           -- 视频（URL/Base64）
├── ToolUseBlock         -- 工具调用请求
└── ToolResultBlock      -- 工具执行结果
```

### 3.1.2 Msg 角色（MsgRole）

| 角色 | 说明 | 使用场景 |
|------|------|----------|
| `USER` | 用户消息 | 人类输入、外部系统数据 |
| `ASSISTANT` | 助手消息 | AI 生成的内容 |
| `SYSTEM` | 系统消息 | 系统指令、上下文配置 |
| `TOOL` | 工具消息 | 工具执行返回结果 |

### 3.1.3 创建各种消息

```java
package io.agentscope.tutorial.chapter03;

import io.agentscope.core.message.*;
import org.junit.jupiter.api.Test;

/**
 * 消息类型示例：展示各种消息的创建方式
 */
public class MessageTypeDemo {

    @Test
    void createAllMessageTypes() {
        // 1. 纯文本消息（最常用）
        Msg textMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好，请介绍一下 AgentScope Java")
                .build();
        System.out.println("文本消息: " + textMsg.getTextContent());

        // 2. 带名称的用户消息
        Msg namedUserMsg = Msg.builder()
                .name("张三")
                .role(MsgRole.USER)
                .textContent("我想了解多轮对话实现")
                .build();
        System.out.println("命名消息名称: " + namedUserMsg.getName());

        // 3. 助手回复消息
        Msg assistantMsg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("AgentScope Java 是一个多代理框架...")
                .build();

        // 4. 系统消息（通常放在对话开头）
        Msg systemMsg = Msg.builder()
                .role(MsgRole.SYSTEM)
                .textContent("你是一个专业的 Java 技术顾问。")
                .build();

        // 5. 多内容块消息
        Msg multiBlockMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(
                        TextBlock.builder().text("请分析以下代码：").build(),
                        TextBlock.builder().text("public class Hello {}").build()
                )
                .build();
        System.out.println("多内容块数: " + multiBlockMsg.getContent().size());

        // 6. 带图片的消息
        Msg imageMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(ImageBlock.builder()
                        .source(new URLSource("https://example.com/diagram.png"))
                        .mediaType("image/png")
                        .build())
                .build();
        System.out.println("图片消息包含媒体: " + imageMsg.getContent().get(0) instanceof ImageBlock);

        // 7. 工具调用消息
        Msg toolMsg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .content(ToolUseBlock.builder()
                        .id("call_001")
                        .name("search_code")
                        .input(java.util.Map.of("query", "Spring Boot", "limit", 10))
                        .build())
                .build();
        System.out.println("工具调用名称: " + toolMsg.getContentBlocks(ToolUseBlock.class).get(0).getName());

        // 8. 工具结果消息
        Msg toolResultMsg = Msg.builder()
                .role(MsgRole.TOOL)
                .content(ToolResultBlock.builder()
                        .toolCallId("call_001")
                        .output(List.of(TextBlock.builder()
                                .text("[{...搜索结果...}]")
                                .build()))
                        .build())
                .build();

        // 9. 带元数据的消息
        Msg metaMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("结构化输入数据")
                .metadata(java.util.Map.of("structured_input", java.util.Map.of(
                        "type", "object",
                        "properties", java.util.Map.of("name", "string")
                )))
                .build();
        System.out.println("包含结构化数据: " + metaMsg.hasStructuredData());
    }
}
```

### 3.1.4 消息内容块详解

```java
package io.agentscope.tutorial.chapter03;

import io.agentscope.core.message.*;
import org.junit.jupiter.api.Test;

/**
 * 内容块详解：展示各种 ContentBlock 的创建与使用
 */
public class ContentBlockDemo {

    @Test
    void demonstrateContentBlocks() {
        // ========== TextBlock ==========
        TextBlock textBlock = TextBlock.builder()
                .text("这是普通文本内容")
                .build();
        System.out.println("文本块: " + textBlock.getText());

        // ========== ImageBlock ==========
        // 方式1：使用 URL 来源
        ImageBlock urlImage = ImageBlock.builder()
                .source(new URLSource("https://example.com/photo.jpg"))
                .mediaType("image/jpeg")
                .build();

        // 方式2：使用 Base64 来源
        ImageBlock base64Image = ImageBlock.builder()
                .source(new Base64Source("image/png", "iVBORw0KGgoAAAANS..."))
                .build();

        // ========== AudioBlock ==========
        AudioBlock audioBlock = AudioBlock.builder()
                .source(new URLSource("https://example.com/audio.mp3"))
                .mediaType("audio/mpeg")
                .build();

        // ========== VideoBlock ==========
        VideoBlock videoBlock = VideoBlock.builder()
                .source(new URLSource("https://example.com/video.mp4"))
                .mediaType("video/mp4")
                .build();

        // ========== ThinkingBlock ==========
        // 思考内容不会被发送到 LLM API，仅用于内部推理追踪
        ThinkingBlock thinkingBlock = ThinkingBlock.builder()
                .thinking("让我分析这个问题...")
                .build();

        // ========== ToolUseBlock ==========
        ToolUseBlock toolUse = ToolUseBlock.builder()
                .id("call_123")
                .name("web_search")
                .input(java.util.Map.of(
                        "query", "AgentScope Java",
                        "max_results", 5
                ))
                .build();
        System.out.println("工具名: " + toolUse.getName());
        System.out.println("工具输入: " + toolUse.getInput());

        // ========== ToolResultBlock ==========
        ToolResultBlock toolResult = ToolResultBlock.builder()
                .toolCallId("call_123")
                .output(List.of(
                        TextBlock.builder()
                                .text("搜索结果: AgentScope 是一个多代理框架...")
                                .build()
                ))
                .isError(false)
                .build();
        System.out.println("工具结果输出数: " + toolResult.getOutput().size());
    }

    @Test
    void messageContentAccess() {
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .content(
                        TextBlock.builder().text("第一段").build(),
                        TextBlock.builder().text("第二段").build()
                )
                .build();

        // 获取所有文本内容
        String fullText = msg.getTextContent();
        System.out.println("完整文本: " + fullText);

        // 获取特定类型的内容块
        var textBlocks = msg.getContentBlocks(TextBlock.class);
        System.out.println("文本块数量: " + textBlocks.size());

        // 获取第一个内容块
        ContentBlock first = msg.getFirstContentBlock();
        System.out.println("第一个内容块: " + first);

        // 检查是否包含特定类型
        boolean hasImage = msg.hasContentBlocks(ImageBlock.class);
        System.out.println("包含图片: " + hasImage);
    }
}
```

---

## 3.2 会话（Session）管理基础

### 3.2.1 Session 接口概述

`Session` 接口是 AgentScope 的状态持久化抽象，支持以下操作：

| 方法 | 说明 |
|------|------|
| `save(key, value)` | 保存单个状态对象 |
| `save(key, list)` | 保存状态列表（增量追加） |
| `get(key, type)` | 获取单个状态 |
| `getList(key, type)` | 获取状态列表 |
| `exists(key)` | 检查会话是否存在 |
| `delete(key)` | 删除会话 |
| `listSessionKeys()` | 列出所有会话 |

### 3.2.2 Session 实现对比

| 实现类 | 存储方式 | 适用场景 |
|--------|----------|----------|
| `InMemorySession` | 内存 Map | 开发测试、短期会话 |
| `JsonSession` | 文件系统 JSON | 生产环境、需要持久化 |
| 自定义实现 | 数据库等 | 需要数据库存储时 |

### 3.2.3 SessionManager 流畅 API

```java
package io.agentscope.tutorial.chapter03;

import io.agentscope.core.session.*;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.state.StateModule;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * 会话管理示例：展示 Session 和 SessionManager 的用法
 */
public class SessionManagementDemo {

    @Test
    void inMemorySessionDemo() {
        // 创建内存会话
        Session session = new InMemorySession();
        SessionKey key = SimpleSessionKey.of("session_001");

        // 检查会话不存在
        System.out.println("会话存在: " + session.exists(key));

        // 保存单个状态
        session.save(key, "user_name", new StringState("张三"));
        session.save(key, "user_age", new IntState(30));

        // 保存列表状态
        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.USER).textContent("你好").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("你好！").build()
        );
        session.save(key, "chat_history", messages);

        // 加载单个状态
        Optional<StringState> name = session.get(key, "user_name", StringState.class);
        System.out.println("用户名: " + name.map(StringState::value).orElse("未找到"));

        // 加载列表状态
        List<Msg> history = session.getList(key, "chat_history", Msg.class);
        System.out.println("历史消息数: " + history.size());

        // 检查会话存在
        System.out.println("会话存在: " + session.exists(key));

        // 清理
        session.delete(key);
        System.out.println("删除后会话存在: " + session.exists(key));
    }

    @Test
    void jsonSessionDemo() {
        // 创建 JSON 文件会话（会话存储在 ./sessions 目录）
        Session session = new JsonSession(Path.of("./sessions"));
        SessionKey key = SimpleSessionKey.of("user_123_session");

        // 保存会话数据
        session.save(key, "config", new StringState("some_config_data"));

        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.USER).textContent("问题1").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("回答1").build()
        );
        session.save(key, "messages", messages);

        System.out.println("会话已保存到文件系统");

        // 重新加载
        boolean exists = session.exists(key);
        System.out.println("会话存在: " + exists);

        if (exists) {
            List<Msg> loaded = session.getList(key, "messages", Msg.class);
            System.out.println("加载消息数: " + loaded.size());
        }

        session.close();
    }

    @Test
    void sessionManagerDemo() {
        // 使用 SessionManager 流畅 API 管理会话
        Session session = new InMemorySession();

        // 创建要管理的组件
        var agentState = new StringState("AgentState");
        var memoryState = new StringState("MemoryState");

        // 流畅链式调用
        SessionManager.forSessionId("user_456")
                .withSession(session)
                .addComponent(new SimpleStateModule("agent", agentState))
                .addComponent(new SimpleStateModule("memory", memoryState))
                .saveSession();

        System.out.println("会话已保存");

        // 加载已存在的会话
        boolean exists = SessionManager.forSessionId("user_456")
                .withSession(session)
                .sessionExists();
        System.out.println("会话存在: " + exists);

        // 删除会话
        boolean deleted = SessionManager.forSessionId("user_456")
                .withSession(session)
                .deleteIfExists();
        System.out.println("删除成功: " + deleted);
    }

    @Test
    void multiSessionDemo() {
        Session session = new InMemorySession();

        // 管理多个用户会话
        String[] userIds = {"user_a", "user_b", "user_c"};

        for (String userId : userIds) {
            SessionManager.forSessionId(userId)
                    .withSession(session)
                    .addComponent(new SimpleStateModule("state",
                            new StringState("data_for_" + userId)))
                    .saveSession();
        }

        // 列出所有会话
        var allKeys = session.listSessionKeys();
        System.out.println("会话总数: " + allKeys.size());

        // 遍历所有会话
        for (SessionKey key : allKeys) {
            var state = session.get(key, "state", StringState.class);
            System.out.println("会话 " + key + " 的状态: " + state.map(StringState::value).orElse("无"));
        }
    }
}
```

### 3.2.4 自定义 State 类

```java
package io.agentscope.tutorial.chapter03;

import io.agentscope.core.state.State;

/**
 * 简单字符串状态实现
 */
public class StringState implements State {
    private final String value;

    public StringState(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "StringState{value='" + value + "'}";
    }
}

/**
 * 简单整数状态实现
 */
class IntState implements State {
    private final int value;

    public IntState(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public String toString() {
        return "IntState{value=" + value + "}";
    }
}

/**
 * 简单的 StateModule 实现，用于演示
 */
class SimpleStateModule implements io.agentscope.core.state.StateModule {
    private final String key;
    private final State state;

    public SimpleStateModule(String key, State state) {
        this.key = key;
        this.state = state;
    }

    @Override
    public void saveTo(Session session, SessionKey sessionKey) {
        session.save(sessionKey, key, state);
    }

    @Override
    public void loadFrom(Session session, SessionKey sessionKey) {
        // 加载逻辑
    }
}
```

---

## 3.3 消息格式器（Formatter）配置

### 3.3.1 Formatter 接口概述

Formatter 负责将 AgentScope 的 `Msg` 转换为各模型提供商的请求格式。

```
AgentScope Msg 列表
        ↓
    Formatter.format()
        ↓
提供商特定请求格式
        ↓
    Model API
        ↓
提供商响应
        ↓
    Formatter.parseResponse()
        ↓
ChatResponse
```

### 3.3.2 内置 Formatter

| 提供商 | Formatter 类 |
|--------|-------------|
| Anthropic | `AnthropicChatFormatter` |
| 阿里云百炼 | `DashScopeChatFormatter` |
| OpenAI | `OpenAIChatFormatter` |
| Ollama | `OllamaChatFormatter` |
| Gemini | `GeminiChatFormatter` |

### 3.3.3 自定义 Formatter

```java
package io.agentscope.tutorial.chapter03;

import io.agentscope.core.formatter.AbstractBaseFormatter;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ToolSchema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 自定义 Formatter 示例
 * 展示如何实现自己的消息格式器
 */
public class CustomFormatterDemo {

    /**
     * 自定义 JSON 格式化器示例
     * 将消息转换为简单的 JSON 格式
     */
    public static class JsonMessageFormatter
            extends AbstractBaseFormatter<Map<String, Object>, Map<String, Object>, Map<String, Object>> {

        @Override
        protected List<Map<String, Object>> doFormat(List<Msg> msgs) {
            return msgs.stream().map(this::msgToMap).toList();
        }

        private Map<String, Object> msgToMap(Msg msg) {
            return Map.of(
                    "id", msg.getId(),
                    "role", msg.getRole().name(),
                    "content", msg.getTextContent(),
                    "timestamp", msg.getTimestamp() != null ? msg.getTimestamp() : ""
            );
        }

        @Override
        public ChatResponse parseResponse(Map<String, Object> response, Instant startTime) {
            String content = (String) response.get("content");
            return ChatResponse.builder()
                    .message(Msg.builder()
                            .role(MsgRole.ASSISTANT)
                            .textContent(content != null ? content : "")
                            .build())
                    .build();
        }

        @Override
        public void applyOptions(Map<String, Object> paramsBuilder,
                                  GenerateOptions options,
                                  GenerateOptions defaultOptions) {
            // 应用 generation options
        }

        @Override
        public void applyTools(Map<String, Object> paramsBuilder, List<ToolSchema> tools) {
            // 应用工具定义
        }
    }

    /**
     * 格式化消息示例
     */
    public void formatMessages() {
        JsonMessageFormatter formatter = new JsonMessageFormatter();

        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.SYSTEM).textContent("你是一个助手").build(),
                Msg.builder().role(MsgRole.USER).textContent("你好").build()
        );

        List<Map<String, Object>> formatted = formatter.format(messages);
        System.out.println("格式化消息数: " + formatted.size());
        System.out.println("第一条消息: " + formatted.get(0));
    }
}
```

### 3.3.4 Formatter 配置与使用

```java
package io.agentscope.tutorial.chapter03;

import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatModel;
import io.agentscope.core.model.ModelConfig;
import io.agentscope.core.model.dashscope.DashScopeChatModel;
import org.junit.jupiter.api.Test;
import java.util.List;

/**
 * Formatter 配置示例
 */
public class FormatterConfigDemo {

    @Test
    void dashscopeFormatterConfig() {
        // 配置阿里云百炼模型（自动使用 DashScopeChatFormatter）
        ModelConfig config = ModelConfig.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .model("qwen-plus")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();

        ChatModel model = DashScopeChatModel.builder()
                .config(config)
                .build();

        System.out.println("DashScope 模型已配置，使用 DashScopeChatFormatter");
    }

    @Test
    void openaiFormatterConfig() {
        // 配置 OpenAI 模型（自动使用 OpenAIChatFormatter）
        ModelConfig config = ModelConfig.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .model("gpt-4o")
                .baseUrl("https://api.openai.com/v1")
                .build();

        // OpenAIChatFormatter 会自动处理消息格式转换
        System.out.println("OpenAI 模型已配置，使用 OpenAIChatFormatter");
    }

    @Test
    void messageConversionDemo() {
        // 演示消息如何在不同 Formatter 间转换
        OpenAIChatFormatter formatter = new OpenAIChatFormatter();

        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.SYSTEM).textContent("你是一个有帮助的助手").build(),
                Msg.builder().role(MsgRole.USER).textContent("解释什么是 RAG").build()
        );

        // format() 会将 AgentScope Msg 转换为 OpenAI 格式
        var openAIMessages = formatter.format(messages);
        System.out.println("转换为 OpenAI 格式的消息数: " + openAIMessages.size());
    }
}
```

---

## 3.4 【案例】多轮对话 Agent

### 3.4.1 项目概述

本案例实现一个完整的多轮对话 Agent 系统，包含：

- 消息历史管理
- Session 持久化（H2 嵌入式数据库）
- 自定义消息格式器
- 多轮对话支持

### 3.4.2 项目结构

```
src/main/java/io/agentscope/tutorial/chapter03/
├── Chapter03Application.java          -- Spring Boot 启动类
├── config/
│   └── AppConfig.java                 -- 应用配置
├── controller/
│   └── ChatController.java            -- REST API 控制器
├── service/
│   ├── ChatService.java               -- 对话服务
│   └── SessionService.java            -- 会话管理服务
├── agent/
│   └── MultiTurnAgent.java            -- 多轮对话 Agent
├── formatter/
│   └── JsonLogFormatter.java          -- 自定义日志格式器
├── entity/
│   └── SessionEntity.java             -- 会话实体（JPA）
├── repository/
│   └── SessionRepository.java        -- 会话仓库
└── dto/
    ├── ChatRequest.java              -- 请求 DTO
    └── ChatResponse.java             -- 响应 DTO
```

### 3.4.3 完整项目代码

#### 1. pom.xml 依赖配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter03-chat-demo</artifactId>
    <version>1.0.0</version>
    <name>AgentScope Java Chapter 03 Demo</name>
    <description>消息与会话管理实战案例</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- H2 嵌入式数据库 -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>1.0.0</version>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Spring Boot Test -->
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
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2. application.yml 配置

```yaml
server:
  port: 8080

spring:
  application:
    name: agentscope-chapter03-demo

  datasource:
    url: jdbc:h2:file:./data/chatsession;AUTO_RECONNECT=TRUE
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
    show-sql: false
    properties:
      hibernate:
        format_sql: true

# AgentScope 配置
agentscope:
  model:
    api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
    model: qwen-plus
```

#### 3. Spring Boot 启动类

```java
package io.agentscope.tutorial.chapter03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java 第三章演示应用
 *
 * 本应用展示消息与会话管理的完整实现：
 * - 多轮对话 Agent
 * - H2 数据库会话持久化
 * - 自定义消息格式器
 */
@SpringBootApplication
public class Chapter03Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter03Application.class, args);
    }
}
```

#### 4. 会话实体（JPA）

```java
package io.agentscope.tutorial.chapter03.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 会话实体 - 存储在 H2 数据库中
 */
@Entity
@Table(name = "chat_sessions")
public class SessionEntity {

    @Id
    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "message_count")
    private Integer messageCount = 0;

    // 构造函数
    public SessionEntity() {}

    public SessionEntity(String sessionId, String userId, String title) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.messageCount = 0;
    }

    // Getter 和 Setter
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

#### 5. 消息实体

```java
package io.agentscope.tutorial.chapter03.entity;

import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.state.State;
import jakarta.persistence.*;
import java.util.List;

/**
 * 消息实体 - 存储对话历史
 */
@Entity
@Table(name = "chat_messages",
       indexes = @Index(name = "idx_session_created", columnList = "session_id, created_at"))
public class MessageEntity implements State {

    @Id
    @Column(name = "message_id", length = 64)
    private String messageId;

    @Column(name = "session_id", length = 64, nullable = false)
    private String sessionId;

    @Column(name = "role", length = 20, nullable = false)
    private String role;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "sequence")
    private Integer sequence;

    // 构造函数
    public MessageEntity() {}

    /**
     * 从 AgentScope Msg 创建实体
     */
    public static MessageEntity fromMsg(Msg msg, String sessionId, int sequence) {
        MessageEntity entity = new MessageEntity();
        entity.setMessageId(msg.getId());
        entity.setSessionId(sessionId);
        entity.setRole(msg.getRole().name());
        entity.setContent(extractTextContent(msg));
        entity.setName(msg.getName());
        entity.setCreatedAt(msg.getTimestamp());
        entity.setSequence(sequence);
        return entity;
    }

    /**
     * 转换为 AgentScope Msg
     */
    public Msg toMsg() {
        return Msg.builder()
                .id(messageId)
                .name(name)
                .role(MsgRole.valueOf(role))
                .textContent(content != null ? content : "")
                .timestamp(createdAt)
                .build();
    }

    private static String extractTextContent(Msg msg) {
        if (msg.getContent() == null || msg.getContent().isEmpty()) {
            return "";
        }
        return msg.getContent().stream()
                .filter(block -> block instanceof io.agentscope.core.message.TextBlock)
                .map(block -> ((io.agentscope.core.message.TextBlock) block).getText())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    // Getter 和 Setter
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
}
```

#### 6. 会话仓库

```java
package io.agentscope.tutorial.chapter03.repository;

import io.agentscope.tutorial.chapter03.entity.MessageEntity;
import io.agentscope.tutorial.chapter03.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 会话仓库 - JPA 数据访问
 */
@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, String> {

    /**
     * 根据用户 ID 查找所有会话
     */
    List<SessionEntity> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * 根据会话 ID 查找消息历史
     */
    @Query("SELECT m FROM MessageEntity m WHERE m.sessionId = :sessionId ORDER BY m.sequence ASC")
    List<MessageEntity> findMessagesBySessionId(String sessionId);

    /**
     * 删除会话的所有消息
     */
    void deleteBySessionId(String sessionId);
}
```

#### 7. DTO 类

```java
package io.agentscope.tutorial.chapter03.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 聊天请求 DTO
 */
public class ChatRequest {

    @NotBlank(message = "会话 ID 不能为空")
    private String sessionId;

    @NotBlank(message = "用户消息不能为空")
    private String message;

    // 可选：系统提示词
    private String systemPrompt;

    // 可选：额外参数
    private Map<String, Object> options;

    // Getter 和 Setter
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) { this.options = options; }
}

/**
 * 聊天响应 DTO
 */
public class ChatResponse {

    private String sessionId;
    private String messageId;
    private String content;
    private String role;
    private long tokens;
    private long durationMs;
    private List<MessageInfo> history;

    // 内部类：消息信息
    public static class MessageInfo {
        private String id;
        private String role;
        private String content;
        private String timestamp;

        public MessageInfo() {}

        public MessageInfo(String id, String role, String content, String timestamp) {
            this.id = id;
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }

        // Getter 和 Setter
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    // Builder 模式
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ChatResponse response = new ChatResponse();

        public Builder sessionId(String sessionId) {
            response.sessionId = sessionId;
            return this;
        }

        public Builder messageId(String messageId) {
            response.messageId = messageId;
            return this;
        }

        public Builder content(String content) {
            response.content = content;
            return this;
        }

        public Builder role(String role) {
            response.role = role;
            return this;
        }

        public Builder tokens(long tokens) {
            response.tokens = tokens;
            return this;
        }

        public Builder durationMs(long durationMs) {
            response.durationMs = durationMs;
            return this;
        }

        public Builder history(List<MessageInfo> history) {
            response.history = history;
            return this;
        }

        public ChatResponse build() { return response; }
    }

    // Getter
    public String getSessionId() { return sessionId; }
    public String getMessageId() { return messageId; }
    public String getContent() { return content; }
    public String getRole() { return role; }
    public long getTokens() { return tokens; }
    public long getDurationMs() { return durationMs; }
    public List<MessageInfo> getHistory() { return history; }
}
```

#### 8. 会话服务

```java
package io.agentscope.tutorial.chapter03.service;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.tutorial.chapter03.entity.MessageEntity;
import io.agentscope.tutorial.chapter03.entity.SessionEntity;
import io.agentscope.tutorial.chapter03.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话服务 - 管理会话的创建、加载和持久化
 */
@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final Session agentscopeSession;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
        // 使用 JsonSession 作为 AgentScope 的底层存储
        this.agentscopeSession = new io.agentscope.core.session.JsonSession(
                Path.of("./data/agentscope_sessions")
        );
    }

    @PostConstruct
    public void init() throws Exception {
        // 确保数据目录存在
        Files.createDirectories(Path.of("./data"));
    }

    /**
     * 创建新会话
     */
    @Transactional
    public SessionEntity createSession(String userId, String title) {
        String sessionId = UUID.randomUUID().toString();
        SessionEntity entity = new SessionEntity(sessionId, userId, title != null ? title : "新会话");
        return sessionRepository.save(entity);
    }

    /**
     * 获取或创建会话
     */
    @Transactional
    public SessionEntity getOrCreateSession(String sessionId, String userId) {
        Optional<SessionEntity> existing = sessionRepository.findById(sessionId);
        if (existing.isPresent()) {
            return existing.get();
        }
        return createSession(userId, null);
    }

    /**
     * 加载会话消息历史
     */
    public List<Msg> loadMessages(String sessionId) {
        List<MessageEntity> entities = sessionRepository.findMessagesBySessionId(sessionId);
        List<Msg> messages = new ArrayList<>();
        for (MessageEntity entity : entities) {
            messages.add(entity.toMsg());
        }
        return messages;
    }

    /**
     * 保存消息到会话
     */
    @Transactional
    public void saveMessage(String sessionId, Msg message) {
        // 查找当前会话的最大序列号
        List<MessageEntity> existing = sessionRepository.findMessagesBySessionId(sessionId);
        int nextSequence = existing.size();

        // 创建消息实体
        MessageEntity entity = MessageEntity.fromMsg(message, sessionId, nextSequence);
        sessionRepository.save(entity);

        // 更新会话的消息计数
        SessionEntity session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setMessageCount(nextSequence + 1);
            sessionRepository.save(session);
        }

        // 同时保存到 AgentScope Session（用于高级功能）
        SessionKey key = SimpleSessionKey.of(sessionId);
        List<Msg> messages = loadMessages(sessionId);
        agentscopeSession.save(key, "messages", messages);
    }

    /**
     * 保存消息列表
     */
    @Transactional
    public void saveMessages(String sessionId, List<Msg> messages) {
        int sequence = 0;
        for (Msg message : messages) {
            MessageEntity entity = MessageEntity.fromMsg(message, sessionId, sequence++);
            sessionRepository.save(entity);
        }

        // 更新会话消息计数
        SessionEntity session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setMessageCount(messages.size());
            sessionRepository.save(session);
        }

        // 保存到 AgentScope Session
        SessionKey key = SimpleSessionKey.of(sessionId);
        agentscopeSession.save(key, "messages", messages);
    }

    /**
     * 清空会话消息
     */
    @Transactional
    public void clearMessages(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
        SessionEntity session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setMessageCount(0);
            sessionRepository.save(session);
        }
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
        // 清理 AgentScope Session
        SessionKey key = SimpleSessionKey.of(sessionId);
        agentscopeSession.delete(key);
    }

    /**
     * 获取用户的所有会话
     */
    public List<SessionEntity> getUserSessions(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }
}
```

#### 9. 自定义消息格式器

```java
package io.agentscope.tutorial.chapter03.formatter;

import io.agentscope.core.formatter.AbstractBaseFormatter;
import io.agentscope.core.message.*;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.ToolSchema;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自定义 JSON 消息格式器
 *
 * 将 AgentScope Msg 转换为友好可读的 JSON 格式
 * 用于日志记录和调试
 */
public class JsonLogFormatter
        extends AbstractBaseFormatter<Map<String, Object>, Map<String, Object>, Map<String, Object>> {

    /** 是否包含完整元数据 */
    private final boolean includeMetadata;

    /** 是否美化输出 */
    private final boolean prettyPrint;

    public JsonLogFormatter() {
        this(false, false);
    }

    public JsonLogFormatter(boolean includeMetadata, boolean prettyPrint) {
        this.includeMetadata = includeMetadata;
        this.prettyPrint = prettyPrint;
    }

    @Override
    protected List<Map<String, Object>> doFormat(List<Msg> msgs) {
        return msgs.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());
    }

    /**
     * 将 Msg 转换为 Map（用于日志和存储）
     */
    public Map<String, Object> convertToMap(Msg msg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", msg.getId());
        map.put("role", msg.getRole().name());
        map.put("name", msg.getName());
        map.put("timestamp", msg.getTimestamp());

        // 转换内容块
        List<Map<String, Object>> contentList = new ArrayList<>();
        for (ContentBlock block : msg.getContent()) {
            contentList.add(convertBlockToMap(block));
        }
        map.put("content", contentList);

        // 可选：包含元数据
        if (includeMetadata && msg.getMetadata() != null && !msg.getMetadata().isEmpty()) {
            map.put("metadata", msg.getMetadata());
        }

        return map;
    }

    private Map<String, Object> convertBlockToMap(ContentBlock block) {
        Map<String, Object> blockMap = new LinkedHashMap<>();

        if (block instanceof TextBlock textBlock) {
            blockMap.put("type", "text");
            blockMap.put("text", textBlock.getText());
        } else if (block instanceof ImageBlock imageBlock) {
            blockMap.put("type", "image");
            blockMap.put("mediaType", imageBlock.getMediaType());
            Source source = imageBlock.getSource();
            if (source instanceof URLSource urlSource) {
                blockMap.put("url", urlSource.getUrl());
            } else if (source instanceof Base64Source base64Source) {
                blockMap.put("data", "[Base64 data, length=" + base64Source.getData().length() + "]");
            }
        } else if (block instanceof ToolUseBlock toolBlock) {
            blockMap.put("type", "tool_use");
            blockMap.put("id", toolBlock.getId());
            blockMap.put("name", toolBlock.getName());
            blockMap.put("input", toolBlock.getInput());
        } else if (block instanceof ToolResultBlock resultBlock) {
            blockMap.put("type", "tool_result");
            blockMap.put("toolCallId", resultBlock.getToolCallId());
            blockMap.put("isError", resultBlock.isError());
            blockMap.put("output", convertBlockListToText(resultBlock.getOutput()));
        } else if (block instanceof ThinkingBlock thinkingBlock) {
            blockMap.put("type", "thinking");
            blockMap.put("thinking", "[Thinking content hidden from log]");
        } else {
            blockMap.put("type", block.getClass().getSimpleName());
        }

        return blockMap;
    }

    private List<String> convertBlockListToText(List<ContentBlock> blocks) {
        if (blocks == null) return List.of();
        return blocks.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.toList());
    }

    @Override
    public ChatResponse parseResponse(Map<String, Object> response, Instant startTime) {
        String content = (String) response.get("content");
        return ChatResponse.builder()
                .message(Msg.builder()
                        .role(MsgRole.ASSISTANT)
                        .textContent(content != null ? content : "")
                        .build())
                .build();
    }

    @Override
    public void applyOptions(Map<String, Object> paramsBuilder,
                              GenerateOptions options,
                              GenerateOptions defaultOptions) {
        // 应用生成选项到参数构建器
        if (options != null) {
            if (options.getTemperature() != null) {
                paramsBuilder.put("temperature", options.getTemperature());
            }
            if (options.getMaxTokens() != null) {
                paramsBuilder.put("max_tokens", options.getMaxTokens());
            }
        }
    }

    @Override
    public void applyTools(Map<String, Object> paramsBuilder, List<ToolSchema> tools) {
        if (tools == null || tools.isEmpty()) {
            paramsBuilder.put("tools", List.of());
            return;
        }
        List<Map<String, Object>> toolList = tools.stream()
                .map(this::convertToolSchemaToMap)
                .collect(Collectors.toList());
        paramsBuilder.put("tools", toolList);
    }

    private Map<String, Object> convertToolSchemaToMap(ToolSchema schema) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "function");
        map.put("function", Map.of(
                "name", schema.getName(),
                "description", schema.getDescription() != null ? schema.getDescription() : "",
                "parameters", schema.getParameters() != null ? schema.getParameters() : Map.of()
        ));
        return map;
    }

    /**
     * 将消息列表格式化为可读字符串
     */
    public String formatMessagesToString(List<Msg> messages) {
        List<Map<String, Object>> maps = doFormat(messages);
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < maps.size(); i++) {
            sb.append("  ").append(formatMapToString(maps.get(i)));
            if (i < maps.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private String formatMapToString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof String s) {
                sb.append("\"").append(s.replace("\"", "\\\"")).append("\"");
            } else if (value instanceof List<?> list) {
                sb.append("[").append(list.size()).append(" items]");
            } else if (value instanceof Map<?, ?> m) {
                sb.append("{").append(m.size()).append(" fields}");
            } else {
                sb.append(String.valueOf(value));
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
```

#### 10. 多轮对话 Agent

```java
package io.agentscope.tutorial.chapter03.agent;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatModel;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ModelConfig;
import io.agentscope.core.model.dashscope.DashScopeChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多轮对话 Agent
 *
 * 负责任务：
 * - 管理对话历史
 * - 维护系统提示词
 * - 生成回复
 */
public class MultiTurnAgent {

    private static final Logger log = LoggerFactory.getLogger(MultiTurnAgent.class);

    /** 默认系统提示词 */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个有帮助的 AI 助手。请用简洁专业的语言回答用户问题。";

    /** 模型客户端 */
    private final ChatModel model;

    /** 系统提示词 */
    private final String systemPrompt;

    /** 对话历史 */
    private final List<Msg> conversationHistory;

    /** 最大历史消息数 */
    private final int maxHistorySize;

    /**
     * 创建多轮对话 Agent
     */
    public MultiTurnAgent(String apiKey, String modelName) {
        this(apiKey, modelName, DEFAULT_SYSTEM_PROMPT, 50);
    }

    /**
     * 创建多轮对话 Agent（完整参数）
     */
    public MultiTurnAgent(String apiKey, String modelName,
                          String systemPrompt, int maxHistorySize) {
        this.systemPrompt = systemPrompt;
        this.maxHistorySize = maxHistorySize;
        this.conversationHistory = new ArrayList<>();

        // 初始化系统消息
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            conversationHistory.add(Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .textContent(systemPrompt)
                    .build());
        }

        // 创建模型客户端
        ModelConfig config = ModelConfig.builder()
                .apiKey(apiKey)
                .model(modelName)
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();

        this.model = DashScopeChatModel.builder()
                .config(config)
                .build();

        log.info("MultiTurnAgent 初始化完成，模型: {}", modelName);
    }

    /**
     * 发送消息并获取回复
     */
    public Msg chat(String userMessage) {
        // 1. 添加用户消息到历史
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(userMessage)
                .build();
        conversationHistory.add(userMsg);

        // 2. 调用模型生成回复
        try {
            ChatResponse response = model.chat(conversationHistory)
                    .block(60, TimeUnit.SECONDS);

            if (response != null && response.getMessage() != null) {
                Msg assistantMsg = response.getMessage();
                // 3. 添加助手回复到历史
                conversationHistory.add(assistantMsg);

                // 4. 修剪过长的历史
                trimHistory();

                log.info("对话轮次完成，当前历史长度: {}", conversationHistory.size());
                return assistantMsg;
            } else {
                log.warn("模型返回空响应");
                return createErrorMessage("模型返回空响应");
            }
        } catch (Exception e) {
            log.error("调用模型失败: {}", e.getMessage());
            // 移除失败的用户消息
            conversationHistory.remove(userMsg);
            return createErrorMessage("调用失败: " + e.getMessage());
        }
    }

    /**
     * 异步发送消息
     */
    public java.util.concurrent.CompletableFuture<Msg> chatAsync(String userMessage) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> chat(userMessage));
    }

    /**
     * 获取当前对话历史
     */
    public List<Msg> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * 清空对话历史（保留系统消息）
     */
    public void clearHistory() {
        conversationHistory.clear();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            conversationHistory.add(Msg.builder()
                    .role(MsgRole.SYSTEM)
                    .textContent(systemPrompt)
                    .build());
        }
        log.info("对话历史已清空");
    }

    /**
     * 加载外部消息历史
     */
    public void loadHistory(List<Msg> messages) {
        conversationHistory.clear();
        if (messages != null) {
            conversationHistory.addAll(messages);
        }
        log.info("已加载 {} 条消息到对话历史", messages != null ? messages.size() : 0);
    }

    /**
     * 导出对话历史（用于持久化）
     */
    public List<Msg> exportHistory() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * 修整过长的对话历史
     */
    private void trimHistory() {
        if (conversationHistory.size() > maxHistorySize) {
            // 保留系统消息和最近的消息
            int systemMessages = 0;
            for (Msg msg : conversationHistory) {
                if (msg.getRole() == MsgRole.SYSTEM) {
                    systemMessages++;
                } else {
                    break;
                }
            }

            int keepCount = systemMessages + maxHistorySize;
            while (conversationHistory.size() > keepCount) {
                // 移除最早的 非系统消息
                for (int i = systemMessages; i < conversationHistory.size(); i++) {
                    if (conversationHistory.get(i).getRole() != MsgRole.SYSTEM) {
                        conversationHistory.remove(i);
                        break;
                    }
                }
            }
            log.info("对话历史已修整，当前长度: {}", conversationHistory.size());
        }
    }

    /**
     * 创建错误消息
     */
    private Msg createErrorMessage(String errorText) {
        return Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("抱歉，发生了错误: " + errorText)
                .build();
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        return String.format(
                "MultiTurnAgent[历史消息数=%d, 最大容量=%d, 系统提示=%s]",
                conversationHistory.size(),
                maxHistorySize,
                systemPrompt != null ? "已设置" : "未设置"
        );
    }

    /**
     * 演示流式对话
     */
    public void demonstrateStreaming(String userMessage) {
        log.info("开始流式对话...");
        userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent(userMessage)
                .build();
        conversationHistory.add(userMsg);

        StringBuilder fullResponse = new StringBuilder();

        try {
            model.streamChat(conversationHistory)
                    .subscribe(
                            // onNext: 处理每个 token
                            token -> {
                                fullResponse.append(token);
                                // 可以这里实时输出
                            },
                            // onError: 处理错误
                            error -> {
                                log.error("流式调用失败: {}", error.getMessage());
                            },
                            // onComplete: 完成
                            () -> {
                                Msg assistantMsg = Msg.builder()
                                        .role(MsgRole.ASSISTANT)
                                        .textContent(fullResponse.toString())
                                        .build();
                                conversationHistory.add(assistantMsg);
                                trimHistory();
                                log.info("流式对话完成，响应长度: {}", fullResponse.length());
                            }
                    );
        } catch (Exception e) {
            log.error("流式对话异常: {}", e.getMessage());
        }
    }
}
```

#### 11. 对话服务

```java
package io.agentscope.tutorial.chapter03.service;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter03.agent.MultiTurnAgent;
import io.agentscope.tutorial.chapter03.dto.ChatRequest;
import io.agentscope.tutorial.chapter03.dto.ChatResponse;
import io.agentscope.tutorial.chapter03.entity.MessageEntity;
import io.agentscope.tutorial.chapter03.entity.SessionEntity;
import io.agentscope.tutorial.chapter03.formatter.JsonLogFormatter;
import io.agentscope.tutorial.chapter03.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对话服务 - 整合 Session 和 Agent 的核心服务
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final SessionService sessionService;
    private final SessionRepository sessionRepository;
    private final JsonLogFormatter logFormatter;

    @Value("${agentscope.model.api-key}")
    private String apiKey;

    @Value("${agentscope.model.model}")
    private String modelName;

    /** 活跃的 Agent 实例（会话级） */
    private final java.util.concurrent.ConcurrentHashMap<String, MultiTurnAgent> activeAgents =
            new java.util.concurrent.ConcurrentHashMap<>();

    public ChatService(SessionService sessionService,
                       SessionRepository sessionRepository) {
        this.sessionService = sessionService;
        this.sessionRepository = sessionRepository;
        this.logFormatter = new JsonLogFormatter(false, true);
    }

    @PostConstruct
    public void init() {
        log.info("ChatService 初始化完成");
        log.info("使用模型: {}", modelName);
    }

    /**
     * 处理聊天请求
     */
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. 获取或创建会话
            SessionEntity session = sessionService.getOrCreateSession(
                    request.getSessionId(), "default_user");

            // 2. 获取 Agent 实例
            MultiTurnAgent agent = getOrCreateAgent(request.getSessionId(), session);

            // 3. 检查是否需要更新系统提示词
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
                // 创建新的 Agent 实例
                activeAgents.remove(request.getSessionId());
                agent = createAgent(request.getSessionId(), request.getSystemPrompt());
            }

            // 4. 加载会话历史到 Agent
            List<Msg> history = sessionService.loadMessages(request.getSessionId());
            if (!history.isEmpty()) {
                agent.loadHistory(history);
            }

            // 5. 发送消息并获取回复
            log.info("用户消息: {}", request.getMessage());
            Msg response = agent.chat(request.getMessage());

            // 6. 保存消息到会话
            // 保存用户消息
            Msg userMsg = Msg.builder()
                    .role(io.agentscope.core.message.MsgRole.USER)
                    .textContent(request.getMessage())
                    .build();
            sessionService.saveMessage(request.getSessionId(), userMsg);

            // 保存助手回复
            sessionService.saveMessage(request.getSessionId(), response);

            // 7. 构建响应
            long duration = System.currentTimeMillis() - startTime;
            return buildResponse(session, response, agent.getConversationHistory(), duration);

        } catch (Exception e) {
            log.error("处理聊天请求失败: {}", e.getMessage(), e);
            return ChatResponse.builder()
                    .sessionId(request.getSessionId())
                    .content("处理请求时发生错误: " + e.getMessage())
                    .role("assistant")
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 获取或创建 Agent 实例
     */
    private MultiTurnAgent getOrCreateAgent(String sessionId, SessionEntity session) {
        return activeAgents.computeIfAbsent(sessionId,
                k -> createAgent(sessionId, session.getTitle()));
    }

    /**
     * 创建新的 Agent 实例
     */
    private MultiTurnAgent createAgent(String sessionId, String systemPrompt) {
        String effectivePrompt = systemPrompt != null && !systemPrompt.isEmpty()
                ? systemPrompt
                : "你是一个有帮助的 AI 助手。";
        return new MultiTurnAgent(apiKey, modelName, effectivePrompt, 50);
    }

    /**
     * 构建响应对象
     */
    private ChatResponse buildResponse(SessionEntity session, Msg response,
                                        List<Msg> history, long durationMs) {
        List<ChatResponse.MessageInfo> historyInfos = history.stream()
                .filter(m -> m.getRole() != io.agentscope.core.message.MsgRole.SYSTEM)
                .map(m -> new ChatResponse.MessageInfo(
                        m.getId(),
                        m.getRole().name(),
                        m.getTextContent(),
                        m.getTimestamp()
                ))
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .sessionId(session.getSessionId())
                .messageId(response.getId())
                .content(response.getTextContent())
                .role(response.getRole().name())
                .durationMs(durationMs)
                .history(historyInfos)
                .build();
    }

    /**
     * 获取会话历史
     */
    public List<Msg> getSessionHistory(String sessionId) {
        return sessionService.loadMessages(sessionId);
    }

    /**
     * 清空会话
     */
    public void clearSession(String sessionId) {
        sessionService.clearMessages(sessionId);
        activeAgents.remove(sessionId);
        log.info("会话已清空: {}", sessionId);
    }

    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        sessionService.deleteSession(sessionId);
        activeAgents.remove(sessionId);
        log.info("会话已删除: {}", sessionId);
    }

    /**
     * 创建新会话
     */
    public SessionEntity createNewSession(String userId, String title) {
        return sessionService.createSession(userId, title);
    }

    /**
     * 导出对话日志（用于调试）
     */
    public String exportConversationLog(String sessionId) {
        List<Msg> history = sessionService.loadMessages(sessionId);
        return logFormatter.formatMessagesToString(history);
    }
}
```

#### 12. REST 控制器

```java
package io.agentscope.tutorial.chapter03.controller;

import io.agentscope.tutorial.chapter03.dto.ChatRequest;
import io.agentscope.tutorial.chapter03.dto.ChatResponse;
import io.agentscope.tutorial.chapter03.entity.SessionEntity;
import io.agentscope.tutorial.chapter03.service.ChatService;
import io.agentscope.tutorial.chapter03.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 聊天 REST 控制器
 *
 * 提供以下 API：
 * - POST /api/chat - 发送消息
 * - GET /api/sessions - 获取会话列表
 * - POST /api/sessions - 创建新会话
 * - GET /api/sessions/{id}/history - 获取会话历史
 * - DELETE /api/sessions/{id} - 删除会话
 * - DELETE /api/sessions/{id}/messages - 清空会话消息
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final SessionService sessionService;

    public ChatController(ChatService chatService, SessionService sessionService) {
        this.chatService = chatService;
        this.sessionService = sessionService;
    }

    /**
     * 发送消息
     * POST /api/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 创建新会话
     * POST /api/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionEntity> createSession(@RequestBody Map<String, String> body) {
        String userId = body.getOrDefault("userId", "default_user");
        String title = body.get("title");
        SessionEntity session = chatService.createNewSession(userId, title);
        return ResponseEntity.ok(session);
    }

    /**
     * 获取会话列表
     * GET /api/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionEntity>> listSessions(
            @RequestParam(defaultValue = "default_user") String userId) {
        List<SessionEntity> sessions = sessionService.getUserSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * 获取会话历史
     * GET /api/sessions/{sessionId}/history
     */
    @GetMapping("/sessions/{sessionId}/history")
    public ResponseEntity<List<ChatResponse.MessageInfo>> getSessionHistory(
            @PathVariable String sessionId) {
        var history = chatService.getSessionHistory(sessionId);
        var infos = history.stream()
                .filter(m -> m.getRole() != io.agentscope.core.message.MsgRole.SYSTEM)
                .map(m -> new ChatResponse.MessageInfo(
                        m.getId(),
                        m.getRole().name(),
                        m.getTextContent(),
                        m.getTimestamp()
                ))
                .toList();
        return ResponseEntity.ok(infos);
    }

    /**
     * 清空会话消息
     * DELETE /api/sessions/{sessionId}/messages
     */
    @DeleteMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        chatService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 删除会话
     * DELETE /api/sessions/{sessionId}
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 导出对话日志（调试用）
     * GET /api/sessions/{sessionId}/export
     */
    @GetMapping("/sessions/{sessionId}/export")
    public ResponseEntity<String> exportConversation(@PathVariable String sessionId) {
        String log = chatService.exportConversationLog(sessionId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/json; charset=utf-8")
                .body(log);
    }
}
```

### 3.4.4 API 使用示例

```bash
# 1. 创建新会话
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"userId": "user1", "title": "技术问答"}'

# 响应: {"sessionId": "xxx-xxx-xxx", "userId": "user1", "title": "技术问答", ...}

# 2. 发送消息
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "xxx-xxx-xxx",
    "message": "什么是 RAG？"
  }'

# 响应:
# {
#   "sessionId": "xxx-xxx-xxx",
#   "content": "RAG（检索增强生成）是一种...",
#   "role": "ASSISTANT",
#   "history": [
#     {"role": "USER", "content": "什么是 RAG？"},
#     {"role": "ASSISTANT", "content": "RAG（检索增强生成）..."}
#   ]
# }

# 3. 继续多轮对话
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": "xxx-xxx-xxx",
    "message": "能举个具体例子吗？"
  }'

# 4. 获取历史记录
curl http://localhost:8080/api/sessions/xxx-xxx-xxx/history

# 5. 导出对话日志（JSON 格式）
curl http://localhost:8080/api/sessions/xxx-xxx-xxx/export

# 6. 删除会话
curl -X DELETE http://localhost:8080/api/sessions/xxx-xxx-xxx
```

### 3.4.5 测试类

```java
package io.agentscope.tutorial.chapter03;

import io.agentscope.tutorial.chapter03.agent.MultiTurnAgent;
import io.agentscope.tutorial.chapter03.entity.MessageEntity;
import io.agentscope.tutorial.chapter03.formatter.JsonLogFormatter;
import io.agentscope.tutorial.chapter03.service.SessionService;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.session.InMemorySession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 第三章综合测试
 */
@SpringBootTest
class Chapter03Test {

    @Autowired
    private SessionService sessionService;

    @Test
    void messageCreationTest() {
        // 测试消息创建
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("测试消息")
                .build();

        assertNotNull(msg.getId());
        assertEquals(MsgRole.USER, msg.getRole());
        assertEquals("测试消息", msg.getTextContent());
        assertNotNull(msg.getTimestamp());
    }

    @Test
    void multiContentBlockTest() {
        // 测试多内容块消息
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .content(
                        TextBlock.builder().text("第一段").build(),
                        TextBlock.builder().text("第二段").build()
                )
                .build();

        assertEquals(2, msg.getContent().size());
        assertEquals("第一段\n第二段", msg.getTextContent());
    }

    @Test
    void sessionManagementTest() {
        // 测试会话管理
        String sessionId = "test-session-001";

        // 创建会话
        var session = sessionService.createSession("test_user", "测试会话");
        assertNotNull(session.getSessionId());

        // 保存消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好")
                .build();
        sessionService.saveMessage(session.getSessionId(), userMsg);

        Msg assistantMsg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("你好！有什么可以帮你的？")
                .build();
        sessionService.saveMessage(session.getSessionId(), assistantMsg);

        // 加载消息
        List<Msg> history = sessionService.loadMessages(session.getSessionId());
        assertEquals(2, history.size());
        assertEquals("你好", history.get(0).getTextContent());
        assertEquals("你好！有什么可以帮你的？", history.get(1).getTextContent());

        // 清空消息
        sessionService.clearMessages(session.getSessionId());
        history = sessionService.loadMessages(session.getSessionId());
        assertTrue(history.isEmpty());

        // 清理
        sessionService.deleteSession(session.getSessionId());
    }

    @Test
    void formatterTest() {
        // 测试自定义格式器
        JsonLogFormatter formatter = new JsonLogFormatter(true, true);

        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.SYSTEM).textContent("你是助手").build(),
                Msg.builder().role(MsgRole.USER).textContent("问题").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("回答").build()
        );

        String formatted = formatter.formatMessagesToString(messages);
        assertNotNull(formatted);
        assertTrue(formatted.contains("SYSTEM"));
        assertTrue(formatted.contains("USER"));
        assertTrue(formatted.contains("ASSISTANT"));

        System.out.println("格式化输出:\n" + formatted);
    }

    @Test
    void inMemorySessionTest() {
        // 测试内存会话
        var session = new InMemorySession();
        var key = io.agentscope.core.state.SimpleSessionKey.of("mem_test");

        session.save(key, "data", new io.agentscope.core.state.State() {});

        assertTrue(session.exists(key));
        session.delete(key);
        assertFalse(session.exists(key));
    }

    @Test
    void messageMetadataTest() {
        // 测试消息元数据
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("带元数据的消息")
                .metadata(java.util.Map.of("key1", "value1", "key2", 123))
                .build();

        assertNotNull(msg.getMetadata());
        assertEquals("value1", msg.getMetadata().get("key1"));
        assertEquals(123, msg.getMetadata().get("key2"));
    }
}
```

---

## 3.5 本章小结

本章介绍了 AgentScope Java 中消息与会话管理的核心概念：

| 概念 | 关键类 | 说明 |
|------|--------|------|
| **消息** | `Msg` | 框架的核心通信单元，包含角色、内容块、元数据 |
| **内容块** | `ContentBlock` | 密封类，包括 Text、Image、Audio、Video、Tool 等类型 |
| **会话** | `Session` | 状态持久化接口，支持 InMemorySession、JsonSession 实现 |
| **会话管理** | `SessionManager` | 流畅 API，简化会话的加载与保存 |
| **格式器** | `Formatter` | 将 Msg 转换为各模型提供商的请求格式 |

**实战要点**：
1. 使用 `Msg.builder()` 创建各种消息
2. 通过 `Session` 接口实现消息历史持久化
3. 利用 `SessionManager` 简化会话状态管理
4. 自定义 `Formatter` 可以灵活处理消息格式转换

---

## 3.6 扩展阅读

- 第四章：ReAct Agent 详解 - 了解 Agent 如何使用消息进行推理
- 第七章：记忆系统 - 深入了解对话历史管理
- 第十六章：Spring Boot 集成 - 完整的企业级集成方案