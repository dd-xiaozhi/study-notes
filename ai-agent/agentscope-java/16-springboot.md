# 第十六章：Spring Boot 集成

> 本章讲解如何将 AgentScope Java 与 Spring Boot 3 深度集成，通过 starters 实现零配置自动装配，并构建符合 OpenAI Chat Completions 标准的 HTTP API 服务。

---

## 16.1 Spring Boot Starter 配置

AgentScope Java 提供了一组 Spring Boot Starters，封装了核心组件的自动装配逻辑，让你无需手动创建 Model、Memory、Toolkit、ReActAgent 等 Bean，开箱即用。

### 16.1.1 Starter 模块总览

| Starter | 描述 | 核心功能 |
|---------|------|---------|
| `agentscope-spring-boot-starter` | 核心 Starter | 自动装配 Model、Memory、Toolkit、ReActAgent |
| `agentscope-chat-completions-web-starter` | Chat Completions API | 暴露 OpenAI 兼容的 `/v1/chat/completions` 端点 |
| `agentscope-agui-spring-boot-starter` | AG-UI 协议支持 | 支持 AG-UI 协议的 Agent 运行接口 |
| `agentscope-a2a-spring-boot-starter` | A2A 协议支持 | Agent-to-Agent 通信的 HTTP 接口 |
| `agentscope-nacos-spring-boot-starter` | Nacos 集成 | 配置中心与服务注册发现 |

### 16.1.2 添加 Maven 依赖

在 `pom.xml` 中引入核心 Starter 和 Chat Completions Web Starter：

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
    </parent>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter16-agent-api-service</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Chapter 16 - Agent API Service</name>
    <description>基于 Spring Boot 3 的 AgentScope API 服务</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Boot Web 依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot 配置处理器（生成配置元数据） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- AgentScope 核心 Starter（自动装配 Model、Agent 等组件） -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-spring-boot-starter</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- AgentScope Chat Completions Web Starter（HTTP API） -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-chat-completions-web-starter</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- Spring Boot JDBC（用于 H2 会话管理） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

        <!-- H2 数据库（内存会话存储） -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok（简化代码） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.agentscope</groupId>
                <artifactId>agentscope-bom</artifactId>
                <version>${agentscope.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

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

---

## 16.2 自动装配机制

AgentScope 的自动装配基于 Spring Boot 的 `@AutoConfiguration` 和 `@ConfigurationProperties` 机制实现，遵循"约定优于配置"原则。

### 16.2.1 核心自动配置类

`AgentscopeAutoConfiguration` 是核心配置类，它在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中声明，Spring Boot 启动时自动加载。

关键设计：

**1. 原型作用域的组件 Bean**

由于 ReActAgent、Memory、Toolkit 都是**有状态且非线程安全**的，它们被注册为 `prototype` 作用域 Bean，确保每个请求获得独立实例：

```java
/**
 * Memory Bean - 原型作用域，每次注入创建新实例
 * 有状态，非线程安全，每个会话独立使用
 */
@Bean
@ConditionalOnProperty(prefix = "agentscope.agent", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public Memory agentscopeMemory() {
    return new InMemoryMemory();
}

/**
 * ReActAgent Bean - 原型作用域
 * 在 Controller/Service 中通过 ObjectProvider<ReActAgent> 按需获取
 */
@Bean
@ConditionalOnMissingBean
@ConditionalOnProperty(prefix = "agentscope.agent", name = "enabled", havingValue = "true")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public ReActAgent agentscopeReActAgent(
        Model model, Memory memory, Toolkit toolkit, AgentscopeProperties properties) {
    AgentProperties config = properties.getAgent();
    return ReActAgent.builder()
            .name(config.getName())
            .sysPrompt(config.getSysPrompt())
            .model(model)
            .memory(memory)
            .toolkit(toolkit)
            .maxIters(config.getMaxIters())
            .build();
}
```

**2. 多模型提供商支持**

通过 `ModelProviderType` 枚举根据配置自动选择模型客户端：

```java
public enum ModelProviderType {
    DASHSCOPE,  // 阿里云百炼
    OPENAI,     // OpenAI 及兼容 API
    GEMINI,     // Google Gemini
    ANTHROPIC;  // Anthropic Claude

    public static ModelProviderType fromProperties(AgentscopeProperties properties) {
        ModelProperties modelConfig = properties.getModel();
        String provider = modelConfig.getProvider();
        if (provider != null) {
            try {
                return ModelProviderType.valueOf(provider.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 默认使用 DashScope
            }
        }
        return DASHSCOPE;
    }
}
```

### 16.2.2 配置属性绑定

所有配置通过 `application.yml` 中的 `agentscope.*` 前缀统一管理：

```yaml
agentscope:
  # 模型提供商配置
  model:
    provider: dashscope  # dashscope | openai | gemini | anthropic

  # 阿里云百炼配置（默认）
  dashscope:
    enabled: true
    api-key: ${DASHSCOPE_API_KEY}
    model-name: qwen-plus
    stream: true
    enable-thinking: true

  # OpenAI 配置（备选）
  # openai:
  #   enabled: true
  #   api-key: ${OPENAI_API_KEY}
  #   model-name: gpt-4.1-mini
  #   base-url: https://api.openai.com/v1

  # 默认 Agent 配置
  agent:
    enabled: true
    name: "Assistant"
    sys-prompt: "你是一个有帮助的AI助手。"
    max-iters: 10
```

配置属性类采用分层结构，`AgentscopeProperties` 作为根节点，包含各子模块的配置类（`AgentProperties`、`DashscopeProperties`、`OpenAIProperties` 等）。

### 16.2.3 条件装配逻辑

自动配置使用多个条件注解实现灵活装配：

| 条件注解 | 作用 |
|---------|------|
| `@ConditionalOnClass` | 仅当类路径存在相应类时才加载配置 |
| `@ConditionalOnProperty` | 仅当配置属性满足条件时才生效 |
| `@ConditionalOnMissingBean` | 仅当同类型 Bean 不存在时才创建 |

这确保了：
- 当模型 SDK 在类路径中时自动配置对应模型
- 用户可以通过自定义 Bean 覆盖默认配置
- 各 Starter 之间可以无缝协作

---

## 16.3 Web API 暴露

AgentScope 提供了多种 Web API 暴露方式，可以将 Agent 能力以 HTTP 接口形式对外提供服务。

### 16.3.1 REST API 端点设计

典型的 Agent HTTP API 设计：

```
POST /api/v1/agent/chat        - 发送对话请求
GET  /api/v1/agent/sessions    - 获取会话列表
GET  /api/v1/agent/sessions/{id} - 获取特定会话历史
DELETE /api/v1/agent/sessions/{id} - 删除会话
POST /api/v1/agent/sessions/{id}/reset - 重置会话
GET  /api/v1/agent/tools       - 获取可用工具列表
```

### 16.3.2 会话管理设计

使用 H2 数据库管理会话状态：

```sql
-- 会话表
CREATE TABLE agent_sessions (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata    TEXT
);

-- 消息历史表
CREATE TABLE session_messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(36) NOT NULL,
    role        VARCHAR(20) NOT NULL,  -- system | user | assistant | tool
    content     TEXT NOT NULL,
    tool_call_id VARCHAR(255),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES agent_sessions(id) ON DELETE CASCADE
);

-- 索引
CREATE INDEX idx_messages_session ON session_messages(session_id);
CREATE INDEX idx_messages_created ON session_messages(created_at);
```

---

## 16.4 Chat Completions Web 接口

`agentscope-chat-completions-web-starter` 自动暴露了一个与 OpenAI Chat Completions API 完全兼容的 HTTP 端点。

### 16.4.1 接口规范

**端点**: `POST /v1/chat/completions`

**请求格式**（与 OpenAI Chat Completions API 兼容）:

```json
{
  "model": "qwen-plus",
  "messages": [
    {"role": "system", "content": "你是一个有帮助的助手。"},
    {"role": "user", "content": "你好，请介绍一下你自己。"}
  ],
  "temperature": 0.7,
  "max_tokens": 1000,
  "stream": false,
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_weather",
        "description": "获取指定城市的天气信息",
        "parameters": {
          "type": "object",
          "properties": {
            "city": {"type": "string", "description": "城市名称"}
          },
          "required": ["city"]
        }
      }
    }
  ]
}
```

**响应格式**（非流式）:

```json
{
  "id": "chatcmpl-xxxx",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "qwen-plus",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "你好！我是一个AI助手..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 50,
    "completion_tokens": 120,
    "total_tokens": 170
  }
}
```

**SSE 流式响应**:

当 `stream: true` 或请求头 `Accept: text/event-stream` 时，返回 Server-Sent Events 流：

```
event: chat.completion.chunk
data: {"id":"chatcmpl-xxxx","object":"chat.completion.chunk","created":1234567890,"model":"qwen-plus","choices":[{"index":0,"delta":{"content":"你"},"finish_reason":null}]}

event: chat.completion.chunk
data: {"id":"chatcmpl-xxxx","object":"chat.completion.chunk","created":1234567890,"model":"qwen-plus","choices":[{"index":0,"delta":{"content":"好"},"finish_reason":null}]}

event: chat.completion
data: [DONE]
```

### 16.4.2 自动配置原理

`ChatCompletionsWebAutoConfiguration` 负责创建所有相关组件：

```java
@AutoConfiguration
@EnableConfigurationProperties(ChatCompletionsProperties.class)
@ConditionalOnProperty(
    prefix = "agentscope.chat-completions",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ConditionalOnClass(ReActAgent.class)
public class ChatCompletionsWebAutoConfiguration {

    // 消息转换器：将 HTTP DTO 转为框架消息
    @Bean
    @ConditionalOnMissingBean
    public ChatMessageConverter chatMessageConverter() { ... }

    // 响应构建器：构建 OpenAI 格式响应
    @Bean
    @ConditionalOnMissingBean
    public ChatCompletionsResponseBuilder chatCompletionsResponseBuilder() { ... }

    // 流式服务：将 agent 响应转为 SSE
    @Bean
    @ConditionalOnMissingBean
    public ChatCompletionsStreamingService chatCompletionsStreamingService(...) { ... }

    // HTTP 控制器
    @Bean
    @ConditionalOnMissingBean
    public ChatCompletionsController chatCompletionsController(
            ObjectProvider<ReActAgent> agentProvider, ...) { ... }
}
```

---

## 16.5 【案例】构建 Agent API 服务

本案例实现一个完整的 Spring Boot 3 + Java 21 Agent API 服务，包含：
- Agent 配置与工具注册
- REST API 端点
- Chat Completions Web 接口
- H2 数据库会话管理

### 16.5.1 项目结构

```
src/main/java/io/agentscope/tutorial/chapter16/
├── Application.java                          # Spring Boot 启动类
├── config/
│   └── AgentConfig.java                      # Agent 配置类
├── controller/
│   ├── AgentController.java                 # REST API 控制器
│   └── SessionController.java                # 会话管理控制器
├── service/
│   ├── SessionService.java                   # 会话服务
│   └── H2SessionManager.java                 # H2 会话管理器
├── model/
│   ├── dto/
│   │   ├── ChatRequest.java                  # 聊天请求 DTO
│   │   ├── ChatResponse.java                 # 聊天响应 DTO
│   │   └── Session.java                      # 会话 DTO
│   └── entity/
│       └── SessionMessage.java               # 消息实体
├── repository/
│   └── SessionRepository.java                # 会话数据访问
└── tool/
    └── WeatherTool.java                      # 示例工具

src/main/resources/
├── application.yml                           # 应用配置
└── schema.sql                                # H2 数据库初始化脚本

src/test/java/io/agentscope/tutorial/chapter16/
├── AgentApiApplicationTests.java            # 集成测试
└── SessionServiceTest.java                   # 单元测试
```

### 16.5.2 应用配置

**src/main/resources/application.yml**:

```yaml
# =============================================================================
# Spring Boot 应用配置
# =============================================================================
spring:
  application:
    name: agentscope-agent-api

  # H2 数据库配置（用于会话管理）
  datasource:
    url: jdbc:h2:mem:agent_sessions;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

  # H2 Web 控制台（开发时使用，生产建议禁用）
  h2:
    console:
      enabled: true
      path: /h2-console

  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: none  # 使用 schema.sql 初始化
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
        format_sql: true

  # SQL 初始化
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

# =============================================================================
# 服务器配置
# =============================================================================
server:
  port: 8080

# =============================================================================
# AgentScope 配置
# =============================================================================
agentscope:
  # 模型提供商配置
  model:
    provider: dashscope  # 可选: dashscope | openai | gemini | anthropic

  # 阿里云百炼（默认模型）
  dashscope:
    enabled: true
    api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
    model-name: qwen-plus
    stream: true
    enable-thinking: true

  # OpenAI 配置（取消注释以使用）
  # openai:
  #   enabled: true
  #   api-key: ${OPENAI_API_KEY}
  #   model-name: gpt-4.1-mini
  #   base-url: https://api.openai.com/v1

  # Google Gemini 配置（取消注释以使用）
  # gemini:
  #   enabled: true
  #   api-key: ${GEMINI_API_KEY}
  #   model-name: gemini-2.0-flash
  #   project: your-gcp-project
  #   location: us-central1
  #   vertex-ai: false

  # 默认 Agent 配置
  agent:
    enabled: true
    name: "TutorialAgent"
    sys-prompt: |
      你是一个专业的AI助手，代号为 TutorialAgent。
      你具备以下能力：
      - 回答各类问题
      - 使用工具获取实时信息（如查询天气）
      - 进行多轮对话
    max-iters: 15

  # Chat Completions API 配置
  chat-completions:
    enabled: true
    base-path: /v1/chat/completions

# =============================================================================
# 日志配置
# =============================================================================
logging:
  level:
    root: INFO
    io.agentscope: DEBUG
    io.agentscope.tutorial: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

**src/main/resources/schema.sql**:

```sql
-- =============================================================================
-- AgentScope Session Schema for H2 Database
-- =============================================================================

-- 会话表
CREATE TABLE IF NOT EXISTS agent_sessions (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(255),
    description VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata    TEXT
);

-- 消息历史表
CREATE TABLE IF NOT EXISTS session_messages (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(36) NOT NULL,
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    tool_call_id VARCHAR(255),
    tool_name   VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES agent_sessions(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_messages_session ON session_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_messages_created ON session_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_sessions_updated ON agent_sessions(updated_at);
```

### 16.5.3 Spring Boot 启动类

**io/agentscope/tutorial/chapter16/Application.java**:

```java
package io.agentscope.tutorial.chapter16;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * AgentScope Agent API Service - Spring Boot 启动类
 *
 * <p>本服务提供以下功能：
 * <ul>
 *   <li>REST API 端点：POST /api/v1/agent/chat</li>
 *   <li>会话管理：GET/DELETE /api/v1/agent/sessions/{id}</li>
 *   <li>Chat Completions API：POST /v1/chat/completions（OpenAI 兼容）</li>
 * </ul>
 *
 * <p>启动方式：
 * <pre>{@code
 * # 方式1: 命令行
 * mvn spring-boot:run
 *
 * # 方式2: 设置环境变量后运行
 * export DASHSCOPE_API_KEY=your-api-key
 * java -jar target/chapter16-agent-api-service-1.0.0.jar
 *
 * # 方式3: IDE 中运行
 * 直接运行 main 方法
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @version 1.0.0
 */
@SpringBootApplication
@ConfigurationPropertiesScan  // 启用配置属性扫描
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 16.5.4 Agent 配置类

**io/agentscope/tutorial/chapter16/config/AgentConfig.java**:

```java
package io.agentscope.tutorial.chapter16.config;

import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter16.tool.WeatherTool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 配置类
 *
 * <p>用于注册自定义工具和额外的 Agent 配置。
 * Memory、Model、ReActAgent 由 agentscope-spring-boot-starter 自动装配。
 * 这里我们额外注册工具集。
 *
 * @see WeatherTool
 */
@Configuration
public class AgentConfig {

    /**
     * 注册自定义工具集
     *
     * <p>Toolkit 是原型作用域，这里注册的工具会被添加到每个新创建的 Agent 中。
     * 由于是原型作用域，不同的 Agent 实例会有独立的 Toolkit 副本。
     *
     * @return 配置好的工具集
     */
    @Bean
    public Toolkit agentscopeToolkit() {
        Toolkit toolkit = new Toolkit();
        // 注册天气查询工具
        toolkit.register(new WeatherTool());
        return toolkit;
    }

    /**
     * 注册更多工具的方法示例
     *
     * <p>如果需要注册多个工具，可以使用链式调用：
     * <pre>{@code
     * toolkit.register(new WeatherTool())
     *        .register(new SearchTool())
     *        .register(new CalculatorTool());
     * }</pre>
     *
     * <p>也可以注册动态工具（通过工具 Schema）：
     * <pre>{@code
     * toolkit.registerSchema(ToolSchema.builder()
     *     .name("custom_tool")
     *     .description("自定义工具")
     *     .parameters("{\"type\":\"object\",\"properties\":{...}}")
     *     .executor((params) -> { /* 执行逻辑 */ })
     *     .build());
     * }</pre>
     */
}
```

### 16.5.5 天气工具

**io/agentscope/tutorial/chapter16/tool/WeatherTool.java**:

```java
package io.agentscope.tutorial.chapter16.tool;

import io.agentscope.core.tool.builtin.CodeInterpreterTool;
import io.agentscope.core.tool.param.ParamSchema;
import io.agentscope.core.tool.param.ParamType;
import io.agentscope.core.tool.param.ValidationType;
import io.agentscope.core.tool.result.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 天气查询工具
 *
 * <p>演示如何创建自定义工具并注册到 Agent。
 * 实际应用中应调用真实天气 API（如和风天气、心知天气等）。
 *
 * <p>使用方式：
 * <pre>{@code
 * Agent: "北京今天天气怎么样？"
 * -> 调用 get_weather(city="北京")
 * <- 返回天气信息
 * }</pre>
 *
 * @see CodeInterpreterTool 内置代码解释器工具
 */
public class WeatherTool extends CodeInterpreterTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    public WeatherTool() {
        super(
            "get_weather",                    // 工具名称
            "获取指定城市的当前天气信息，包括温度、湿度、风力等",  // 工具描述
            createParamSchema()              // 参数定义
        );
    }

    /**
     * 创建参数 Schema
     */
    private static ParamSchema createParamSchema() {
        return ParamSchema.builder()
            .param("city", ParamType.STRING, true)
            .description("城市名称（中文），如：北京、上海、纽约")
            .validation(ValidationType.NOT_BLANK, "城市名称不能为空")
            .validation(ValidationType.MAX_LENGTH, 20, "城市名称不能超过20个字符")
            .build();
    }

    /**
     * 工具执行逻辑
     *
     * <p>这里使用模拟数据，实际项目中应调用真实天气 API。
     * 返回结果需要符合 ToolResult 格式。
     *
     * @param params 解析后的参数（由框架自动从 JSON 解析）
     * @return 工具执行结果
     */
    @Override
    public ToolResult execute(Map<String, Object> params) {
        // 获取城市参数
        String city = (String) params.get("city");

        log.info("查询天气: city={}", city);

        // 模拟天气数据（实际应用中应调用外部 API）
        String weatherData = getMockWeatherData(city);

        return ToolResult.success(weatherData);
    }

    /**
     * 获取模拟天气数据
     *
     * <p>在实际应用中，替换为真实天气 API 调用。
     * 推荐使用：心知天气 API、和风天气 API、中国天气网 API 等。
     */
    private String getMockWeatherData(String city) {
        // 简单的模拟数据
        return String.format("""
            {
              "city": "%s",
              "weather": "晴",
              "temperature": "25°C",
              "humidity": "45%%",
              "wind": "东南风 3级",
              "aqi": "良",
              "update_time": "2026-05-17 10:30:00",
              "tips": "今日天气良好，适合外出活动"
            }
            """, city);
    }

    /**
     * 执行异常处理
     *
     * <p>当工具执行出错时，框架会自动调用此方法。
     * 可以在这里记录日志或进行其他错误处理。
     */
    @Override
    protected void onError(Exception e, Map<String, Object> params) {
        log.error("天气查询工具执行失败: {}", e.getMessage(), e);
    }
}
```

### 16.5.6 REST API 控制器

**io/agentscope/tutorial/chapter16/controller/AgentController.java**:

```java
package io.agentscope.tutorial.chapter16.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Role;
import io.agentscope.core.message.TextMsg;
import io.agentscope.tutorial.chapter16.model.dto.ChatRequest;
import io.agentscope.tutorial.chapter16.model.dto.ChatResponse;
import io.agentscope.tutorial.chapter16.service.SessionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agent REST API 控制器
 *
 * <p>提供对话相关的 HTTP 端点，包括：
 * <ul>
 *   <li>POST /api/v1/agent/chat - 发送对话请求（带会话管理）</li>
 *   <li>GET /api/v1/agent/tools - 获取可用工具列表</li>
 *   <li>GET /api/v1/agent/health - 健康检查</li>
 * </ul>
 *
 * <p>与 /v1/chat/completions 的区别：
 * <ul>
 *   <li>/api/v1/agent/chat - 支持会话管理，自动维护上下文</li>
 *   <li>/v1/chat/completions - 无状态 API，客户端维护上下文</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    /** Agent 实例提供器（原型作用域，每次获取新实例） */
    private final ObjectProvider<ReActAgent> agentProvider;

    /** 会话服务 */
    private final SessionService sessionService;

    public AgentController(
            ObjectProvider<ReActAgent> agentProvider,
            SessionService sessionService) {
        this.agentProvider = agentProvider;
        this.sessionService = sessionService;
    }

    /**
     * 发送对话请求（带会话管理）
     *
     * <p>此端点自动维护会话历史，新消息会追加到现有会话中。
     * 适用于需要长期上下文的应用场景。
     *
     * @param sessionId 会话 ID（可选，为空则创建新会话）
     * @param request   聊天请求
     * @return 聊天响应（包含会话 ID 和 Agent 回复）
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestParam(required = false) String sessionId,
            @Valid @RequestBody ChatRequest request) {

        log.info("收到聊天请求: sessionId={}, messageCount={}",
                sessionId, request.getMessages().size());

        // 获取或创建会话
        String currentSessionId = sessionId;
        if (currentSessionId == null || currentSessionId.isBlank()) {
            currentSessionId = sessionService.createSession("默认会话");
            log.info("创建新会话: sessionId={}", currentSessionId);
        }

        // 保存用户消息到会话
        request.getMessages().forEach(msg -> {
            sessionService.addMessage(currentSessionId, msg.getRole(), msg.getContent());
        });

        // 获取 Agent 实例（原型作用域）
        ReActAgent agent = agentProvider.getObject();

        // 构建消息列表（从会话历史中获取）
        List<TextMsg> historyMessages = sessionService.getMessages(currentSessionId);
        List<TextMsg> agentMessages = new ArrayList<>(historyMessages);

        // 如果请求中有额外消息，追加到列表
        request.getMessages().forEach(msg -> {
            Role role = switch (msg.getRole().toLowerCase()) {
                case "system" -> Role.SYSTEM;
                case "user" -> Role.USER;
                case "assistant" -> Role.ASSISTANT;
                case "tool" -> Role.TOOL;
                default -> Role.USER;
            };
            agentMessages.add(new TextMsg(role, msg.getContent()));
        });

        // 调用 Agent
        long startTime = System.currentTimeMillis();
        String reply = agent.call(agentMessages).block();
        long duration = System.currentTimeMillis() - startTime;

        log.info("Agent 响应完成: sessionId={}, duration={}ms, replyLength={}",
                currentSessionId, duration, reply != null ? reply.length() : 0);

        // 保存 Agent 回复到会话
        if (reply != null) {
            sessionService.addMessage(currentSessionId, "assistant", reply);
        }

        // 更新会话时间
        sessionService.updateSessionTime(currentSessionId);

        // 构建响应
        ChatResponse response = ChatResponse.builder()
                .sessionId(currentSessionId)
                .message(reply != null ? reply : "")
                .tokenCount(estimateTokenCount(reply))
                .duration(duration)
                .finishReason("stop")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * 获取可用工具列表
     *
     * @return 工具信息列表
     */
    @GetMapping("/tools")
    public ResponseEntity<List<Map<String, Object>>> getTools() {
        ReActAgent agent = agentProvider.getObject();

        List<Map<String, Object>> tools = agent.getToolkit().getTools().stream()
                .map(tool -> Map.<String, Object>of(
                        "name", tool.getName(),
                        "description", tool.getDescription(),
                        "parameters", tool.getParamSchema() != null ?
                                tool.getParamSchema().toMap() : Map.of()
                ))
                .toList();

        return ResponseEntity.ok(tools);
    }

    /**
     * 健康检查
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "agentscope-agent-api",
                "version", "1.0.0"
        ));
    }

    /**
     * 估算 token 数量（简化估算：中文 ~2字符/token，英文 ~4字符/token）
     */
    private int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // 简单估算：总字符数 / 2.5
        return (int) (text.length() / 2.5);
    }
}
```

### 16.5.7 聊天请求/响应 DTO

**io/agentscope/tutorial/chapter16/model/dto/ChatRequest.java**:

```java
package io.agentscope.tutorial.chapter16.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天请求 DTO
 *
 * <p>用于 /api/v1/agent/chat 端点。
 * 支持传入消息历史以进行多轮对话。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /** 消息列表（包含角色和内容） */
    @NotEmpty(message = "消息列表不能为空")
    private List<Message> messages;

    /** 模型名称（可选，用于覆盖默认配置） */
    private String model;

    /** 温度参数（控制随机性，0-2） */
    private Double temperature;

    /** 最大生成 token 数 */
    private Integer maxTokens;

    /**
     * 消息结构
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /** 角色：system | user | assistant | tool */
        private String role;

        /** 消息内容 */
        private String content;

        /** 工具调用 ID（仅 tool 角色使用） */
        private String toolCallId;

        /** 工具名称（仅 tool 角色使用） */
        private String toolName;
    }
}
```

**io/agentscope/tutorial/chapter16/model/dto/ChatResponse.java**:

```java
package io.agentscope.tutorial.chapter16.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 聊天响应 DTO
 *
 * <p>包含 Agent 回复及元数据信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    /** 会话 ID */
    private String sessionId;

    /** Agent 回复内容 */
    private String message;

    /** 使用的模型名称 */
    private String model;

    /** 估算的 token 数量 */
    private Integer tokenCount;

    /** 请求耗时（毫秒） */
    private Long duration;

    /** 完成原因：stop | length | content_filter | tool_calls */
    private String finishReason;

    /** 响应时间戳 */
    private Instant timestamp;

    /** 工具调用（如果有） */
    private Object toolCalls;

    /** 追加到响应的静态工厂方法 */
    public static ChatResponseBuilder builder() {
        return new ChatResponseBuilder().timestamp(Instant.now());
    }
}
```

### 16.5.8 会话服务

**io/agentscope/tutorial/chapter16/service/SessionService.java**:

```java
package io.agentscope.tutorial.chapter16.service;

import io.agentscope.core.message.Role;
import io.agentscope.core.message.TextMsg;
import io.agentscope.tutorial.chapter16.model.entity.SessionMessage;
import io.agentscope.tutorial.chapter16.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话服务
 *
 * <p>负责会话的创建、查询、管理和消息的持久化。
 * 使用 H2 数据库存储会话数据和消息历史。
 *
 * @see H2SessionManager H2 会话管理器
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final H2SessionManager h2SessionManager;

    public SessionService(SessionRepository sessionRepository, H2SessionManager h2SessionManager) {
        this.sessionRepository = sessionRepository;
        this.h2SessionManager = h2SessionManager;
    }

    /**
     * 创建新会话
     *
     * @param name 会话名称
     * @return 会话 ID
     */
    @Transactional
    public String createSession(String name) {
        String sessionId = UUID.randomUUID().toString();
        h2SessionManager.createSession(sessionId, name);
        log.info("创建会话: id={}, name={}", sessionId, name);
        return sessionId;
    }

    /**
     * 获取会话名称
     *
     * @param sessionId 会话 ID
     * @return 会话名称（如果不存在返回空）
     */
    public Optional<String> getSessionName(String sessionId) {
        return sessionRepository.findNameById(sessionId);
    }

    /**
     * 获取所有会话
     *
     * @return 会话列表（按更新时间倒序）
     */
    public List<SessionInfo> getAllSessions() {
        return sessionRepository.findAllOrderByUpdatedAt().stream()
                .map(row -> new SessionInfo(
                        (String) row[0],  // id
                        (String) row[1],  // name
                        (java.sql.Timestamp) row[2],  // created_at
                        (java.sql.Timestamp) row[3]  // updated_at
                ))
                .toList();
    }

    /**
     * 获取会话消息历史
     *
     * <p>返回的消息按时间正序排列，可直接用于 Agent 调用。
     *
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    public List<TextMsg> getMessages(String sessionId) {
        return sessionRepository.findMessagesBySessionId(sessionId).stream()
                .map(row -> {
                    String roleStr = (String) row[1];
                    String content = (String) row[2];
                    Role role = parseRole(roleStr);
                    return new TextMsg(role, content);
                })
                .toList();
    }

    /**
     * 添加消息到会话
     *
     * @param sessionId 会话 ID
     * @param role      角色（system | user | assistant | tool）
     * @param content   消息内容
     * @return 自动生成的消息 ID
     */
    @Transactional
    public long addMessage(String sessionId, String role, String content) {
        long messageId = sessionRepository.insertMessage(
                sessionId, role, content, null, null);
        log.debug("添加消息: sessionId={}, role={}, contentLength={}",
                sessionId, role, content != null ? content.length() : 0);
        return messageId;
    }

    /**
     * 更新会话时间戳
     *
     * @param sessionId 会话 ID
     */
    @Transactional
    public void updateSessionTime(String sessionId) {
        sessionRepository.updateSessionTime(sessionId, Instant.now());
    }

    /**
     * 删除会话
     *
     * <p>会话删除后，所有关联消息也会被删除（级联删除）。
     *
     * @param sessionId 会话 ID
     * @return 是否成功删除
     */
    @Transactional
    public boolean deleteSession(String sessionId) {
        int deleted = sessionRepository.deleteById(sessionId);
        log.info("删除会话: sessionId={}, deleted={}", sessionId, deleted > 0);
        return deleted > 0;
    }

    /**
     * 重置会话（清除消息历史）
     *
     * @param sessionId 会话 ID
     */
    @Transactional
    public void resetSession(String sessionId) {
        sessionRepository.deleteMessagesBySessionId(sessionId);
        updateSessionTime(sessionId);
        log.info("重置会话: sessionId={}", sessionId);
    }

    /**
     * 获取会话消息数量
     *
     * @param sessionId 会话 ID
     * @return 消息数量
     */
    public int getMessageCount(String sessionId) {
        return sessionRepository.countMessagesBySessionId(sessionId);
    }

    /**
     * 解析角色字符串为 Role 枚举
     */
    private Role parseRole(String roleStr) {
        return switch (roleStr.toLowerCase()) {
            case "system" -> Role.SYSTEM;
            case "assistant" -> Role.ASSISTANT;
            case "tool" -> Role.TOOL;
            default -> Role.USER;
        };
    }

    /**
     * 会话信息记录
     *
     * @param id        会话 ID
     * @param name      会话名称
     * @param createdAt 创建时间
     * @param updatedAt 更新时间
     */
    public record SessionInfo(
            String id,
            String name,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
```

### 16.5.9 H2 会话管理器

**io/agentscope/tutorial/chapter16/service/H2SessionManager.java**:

```java
package io.agentscope.tutorial.chapter16.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * H2 会话管理器
 *
 * <p>直接操作 H2 数据库，执行会话相关的 SQL 操作。
 * 封装了基础的 CRUD 操作，供 SessionService 调用。
 *
 * @see SessionService 会话服务（更高层抽象）
 */
@Component
public class H2SessionManager {

    private static final Logger log = LoggerFactory.getLogger(H2SessionManager.class);

    private final JdbcTemplate jdbcTemplate;

    public H2SessionManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建会话记录
     *
     * @param id   会话 ID
     * @param name 会话名称
     */
    public void createSession(String id, String name) {
        jdbcTemplate.update(
            "INSERT INTO agent_sessions (id, name, created_at, updated_at) VALUES (?, ?, ?, ?)",
            id, name, Instant.now(), Instant.now()
        );
        log.debug("创建会话记录: id={}, name={}", id, name);
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话 ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM agent_sessions WHERE id = ?",
            Integer.class,
            sessionId
        );
        return count != null && count > 0;
    }

    /**
     * 获取会话更新时间
     *
     * @param sessionId 会话 ID
     * @return 更新时间（如果不存在返回 null）
     */
    public Instant getSessionUpdatedAt(String sessionId) {
        return jdbcTemplate.queryForObject(
            "SELECT updated_at FROM agent_sessions WHERE id = ?",
            (rs, rowNum) -> rs.getTimestamp("updated_at").toInstant(),
            sessionId
        );
    }

    /**
     * 更新会话的更新时间
     *
     * @param sessionId 会话 ID
     * @param time      新的时间戳
     */
    public void updateSessionUpdatedAt(String sessionId, Instant time) {
        jdbcTemplate.update(
            "UPDATE agent_sessions SET updated_at = ? WHERE id = ?",
            time, sessionId
        );
    }

    /**
     * 删除会话及其所有消息
     *
     * @param sessionId 会话 ID
     */
    public void deleteSession(String sessionId) {
        // 由于外键约束，会自动级联删除消息
        int deleted = jdbcTemplate.update(
            "DELETE FROM agent_sessions WHERE id = ?",
            sessionId
        );
        log.debug("删除会话: id={}, rowsAffected={}", sessionId, deleted);
    }
}
```

### 16.5.10 会话数据访问

**io/agentscope/tutorial/chapter16/repository/SessionRepository.java**:

```java
package io.agentscope.tutorial.chapter16.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 会话数据访问层
 *
 * <p>使用 Spring JDBC 访问 H2 数据库。
 * 相比 JPA/JDBC 模板，JDBC Template 更轻量且性能更好。
 */
@Repository
public class SessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public SessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ==================== 会话操作 ====================

    /**
     * 查询会话名称
     */
    public Optional<String> findNameById(String sessionId) {
        String name = jdbcTemplate.queryForObject(
            "SELECT name FROM agent_sessions WHERE id = ?",
            String.class,
            sessionId
        );
        return Optional.ofNullable(name);
    }

    /**
     * 查询所有会话（按更新时间倒序）
     */
    public List<Object[]> findAllOrderByUpdatedAt() {
        return jdbcTemplate.query(
            "SELECT id, name, created_at, updated_at FROM agent_sessions ORDER BY updated_at DESC",
            (rs, rowNum) -> new Object[]{
                rs.getString("id"),
                rs.getString("name"),
                rs.getTimestamp("created_at"),
                rs.getTimestamp("updated_at")
            }
        );
    }

    /**
     * 更新会话时间
     */
    public void updateSessionTime(String sessionId, Instant time) {
        jdbcTemplate.update(
            "UPDATE agent_sessions SET updated_at = ? WHERE id = ?",
            Timestamp.from(time), sessionId
        );
    }

    /**
     * 删除会话
     */
    public int deleteById(String sessionId) {
        return jdbcTemplate.update(
            "DELETE FROM agent_sessions WHERE id = ?",
            sessionId
        );
    }

    // ==================== 消息操作 ====================

    /**
     * 查询会话的所有消息（按时间正序）
     */
    public List<Object[]> findMessagesBySessionId(String sessionId) {
        return jdbcTemplate.query(
            """
            SELECT id, role, content, tool_call_id, tool_name, created_at
            FROM session_messages
            WHERE session_id = ?
            ORDER BY created_at ASC
            """,
            (rs, rowNum) -> new Object[]{
                rs.getLong("id"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("tool_call_id"),
                rs.getString("tool_name"),
                rs.getTimestamp("created_at")
            },
            sessionId
        );
    }

    /**
     * 插入消息
     *
     * @return 插入行的 ID
     */
    public long insertMessage(String sessionId, String role, String content,
                              String toolCallId, String toolName) {
        return jdbcTemplate.queryForObject(
            """
            INSERT INTO session_messages (session_id, role, content, tool_call_id, tool_name, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            (rs, meta) -> rs.getLong(1),
            sessionId, role, content, toolCallId, toolName, Timestamp.from(Instant.now())
        );
    }

    /**
     * 统计会话消息数量
     */
    public int countMessagesBySessionId(String sessionId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM session_messages WHERE session_id = ?",
            Integer.class,
            sessionId
        );
        return count != null ? count : 0;
    }

    /**
     * 删除会话的所有消息
     */
    public int deleteMessagesBySessionId(String sessionId) {
        return jdbcTemplate.update(
            "DELETE FROM session_messages WHERE session_id = ?",
            sessionId
        );
    }

    /**
     * 查询会话最新的一条消息
     */
    public Optional<Object[]> findLatestMessage(String sessionId) {
        List<Object[]> results = jdbcTemplate.query(
            """
            SELECT id, role, content, tool_call_id, tool_name, created_at
            FROM session_messages
            WHERE session_id = ?
            ORDER BY created_at DESC
            LIMIT 1
            """,
            (rs, rowNum) -> new Object[]{
                rs.getLong("id"),
                rs.getString("role"),
                rs.getString("content"),
                rs.getString("tool_call_id"),
                rs.getString("tool_name"),
                rs.getTimestamp("created_at")
            },
            sessionId
        );
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
```

### 16.5.11 会话管理控制器

**io/agentscope/tutorial/chapter16/controller/SessionController.java**:

```java
package io.agentscope.tutorial.chapter16.controller;

import io.agentscope.core.message.TextMsg;
import io.agentscope.tutorial.chapter16.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器
 *
 * <p>提供会话的 CRUD 操作：
 * <ul>
 *   <li>GET /api/v1/agent/sessions - 获取所有会话</li>
 *   <li>GET /api/v1/agent/sessions/{id} - 获取会话详情</li>
 *   <li>DELETE /api/v1/agent/sessions/{id} - 删除会话</li>
 *   <li>POST /api/v1/agent/sessions/{id}/reset - 重置会话</li>
 *   <li>GET /api/v1/agent/sessions/{id}/messages - 获取会话消息历史</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/agent/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 获取所有会话列表
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllSessions() {
        List<Map<String, Object>> sessions = sessionService.getAllSessions().stream()
                .map(info -> Map.<String, Object>of(
                        "id", info.id(),
                        "name", info.name(),
                        "createdAt", info.createdAt().toString(),
                        "updatedAt", info.updatedAt().toString()
                ))
                .toList();
        return ResponseEntity.ok(sessions);
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        return sessionService.getSessionName(sessionId)
                .map(name -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", sessionId,
                        "name", name,
                        "messageCount", sessionService.getMessageCount(sessionId)
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        boolean deleted = sessionService.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "deleted", deleted
        ));
    }

    /**
     * 重置会话（清除消息历史）
     */
    @PostMapping("/{sessionId}/reset")
    public ResponseEntity<Map<String, Object>> resetSession(@PathVariable String sessionId) {
        sessionService.resetSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "sessionId", sessionId,
                "messageCount", 0
        ));
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(@PathVariable String sessionId) {
        List<TextMsg> messages = sessionService.getMessages(sessionId);
        List<Map<String, Object>> result = messages.stream()
                .map(msg -> Map.<String, Object>of(
                        "role", msg.getRole().name().toLowerCase(),
                        "content", msg.getContent()
                ))
                .toList();
        return ResponseEntity.ok(result);
    }
}
```

### 16.5.12 消息实体

**io/agentscope/tutorial/chapter16/model/entity/SessionMessage.java**:

```java
package io.agentscope.tutorial.chapter16.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 会话消息实体
 *
 * <p>与数据库表 session_messages 对应。
 * 注意：这里不使用 JPA 实体，而是通过 JDBCTemplate 手动操作 SQL，
 * 因此此类仅作为数据传输对象（DTO）。
 *
 * @see io.agentscope.tutorial.chapter16.repository.SessionRepository
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionMessage {

    /** 消息 ID（自增） */
    private Long id;

    /** 所属会话 ID */
    private String sessionId;

    /** 消息角色：system | user | assistant | tool */
    private String role;

    /** 消息内容 */
    private String content;

    /** 工具调用 ID（仅 tool 角色） */
    private String toolCallId;

    /** 工具名称（仅 tool 角色） */
    private String toolName;

    /** 创建时间 */
    private Instant createdAt;

    /**
     * 判断是否为用户消息
     */
    public boolean isUserMessage() {
        return "user".equalsIgnoreCase(role);
    }

    /**
     * 判断是否为助手消息
     */
    public boolean isAssistantMessage() {
        return "assistant".equalsIgnoreCase(role);
    }

    /**
     * 判断是否为工具调用消息
     */
    public boolean isToolMessage() {
        return "tool".equalsIgnoreCase(role);
    }
}
```

### 16.5.13 集成测试

**io/agentscope/tutorial/chapter16/AgentApiApplicationTests.java**:

```java
package io.agentscope.tutorial.chapter16;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用集成测试
 *
 * <p>测试 Spring Boot 应用能否正常启动，以及核心组件是否正确装配。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")  // 使用测试配置
class AgentApiApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文能否成功加载
        // 如果自动配置失败，这里会抛出异常
    }
}
```

**src/test/resources/application-test.yml**:

```yaml
# 测试环境配置
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:

  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

# 禁用真实 API 调用（测试时使用模拟）
agentscope:
  dashscope:
    api-key: test-api-key-for-testing-only
  agent:
    enabled: true
```

### 16.5.14 测试用例

**io/agentscope/tutorial/chapter16/SessionServiceTest.java**:

```java
package io.agentscope.tutorial.chapter16;

import io.agentscope.tutorial.chapter16.service.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会话服务单元测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SessionServiceTest {

    @Autowired
    private SessionService sessionService;

    private String testSessionId;

    @BeforeEach
    void setUp() {
        // 每个测试前创建新会话
        testSessionId = sessionService.createSession("测试会话");
    }

    @Test
    void testCreateSession() {
        assertNotNull(testSessionId);
        assertFalse(testSessionId.isBlank());
    }

    @Test
    void testAddMessage() {
        // 添加用户消息
        long msgId = sessionService.addMessage(testSessionId, "user", "你好");
        assertTrue(msgId > 0);

        // 验证消息数量
        int count = sessionService.getMessageCount(testSessionId);
        assertEquals(1, count);
    }

    @Test
    void testGetMessages() {
        // 添加测试消息
        sessionService.addMessage(testSessionId, "user", "你好");
        sessionService.addMessage(testSessionId, "assistant", "你好！有什么可以帮助你的吗？");

        // 获取消息
        List<io.agentscope.core.message.TextMsg> messages = sessionService.getMessages(testSessionId);
        assertEquals(2, messages.size());
        assertEquals("user", messages.get(0).getRole().name().toLowerCase());
        assertEquals("你好", messages.get(0).getContent());
        assertEquals("assistant", messages.get(1).getRole().name().toLowerCase());
    }

    @Test
    void testResetSession() {
        // 添加消息
        sessionService.addMessage(testSessionId, "user", "测试");
        assertEquals(1, sessionService.getMessageCount(testSessionId));

        // 重置会话
        sessionService.resetSession(testSessionId);
        assertEquals(0, sessionService.getMessageCount(testSessionId));
    }

    @Test
    void testDeleteSession() {
        boolean deleted = sessionService.deleteSession(testSessionId);
        assertTrue(deleted);
        assertEquals(0, sessionService.getMessageCount(testSessionId));
    }
}
```

---

## 16.6 运行与测试

### 16.6.1 启动服务

```bash
# 方式一：Maven 运行
cd tutorial/chapter16
mvn spring-boot:run

# 方式二：设置 API Key 后运行
export DASHSCOPE_API_KEY=your-actual-api-key
mvn spring-boot:run

# 方式三：打包后运行
mvn clean package -DskipTests
java -jar target/chapter16-agent-api-service-1.0.0.jar
```

### 16.6.2 API 测试

**健康检查**:

```bash
curl http://localhost:8080/api/v1/agent/health
```

**发送对话请求**:

```bash
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "messages": [
      {"role": "user", "content": "你好，请介绍一下自己"}
    ]
  }'
```

**使用 Chat Completions API（OpenAI 兼容）**:

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen-plus",
    "messages": [
      {"role": "user", "content": "北京今天天气怎么样？"}
    ]
  }'
```

**流式响应**:

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "model": "qwen-plus",
    "messages": [
      {"role": "user", "content": "请讲一个笑话"}
    ],
    "stream": true
  }'
```

**会话管理**:

```bash
# 获取所有会话
curl http://localhost:8080/api/v1/agent/sessions

# 获取特定会话消息
curl http://localhost:8080/api/v1/agent/sessions/{sessionId}/messages

# 删除会话
curl -X DELETE http://localhost:8080/api/v1/agent/sessions/{sessionId}

# 重置会话
curl -X POST http://localhost:8080/api/v1/agent/sessions/{sessionId}/reset
```

**查询天气（使用工具）**:

```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen-plus",
    "messages": [
      {"role": "user", "content": "北京今天天气怎么样？"}
    ],
    "tools": [
      {
        "type": "function",
        "function": {
          "name": "get_weather",
          "description": "获取指定城市的天气信息",
          "parameters": {
            "type": "object",
            "properties": {
              "city": {"type": "string", "description": "城市名称"}
            },
            "required": ["city"]
          }
        }
      }
    ]
  }'
```

### 16.6.3 H2 控制台

开发环境下可以访问 H2 Web 控制台查看会话数据：

```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:agent_sessions
User: sa
Password: (空)
```

---

## 16.7 小结

本章介绍了 AgentScope Java 与 Spring Boot 3 的深度集成：

| 知识点 | 内容 |
|-------|------|
| **Starter 模块** | agentscope-spring-boot-starter 提供核心自动装配 |
| **自动装配机制** | 原型作用域 Bean、条件注解、多模型支持 |
| **配置属性** | agentscope.* 前缀的层次化配置 |
| **REST API** | /api/v1/agent/* 端点，支持会话管理 |
| **Chat Completions** | /v1/chat/completions 端点，OpenAI 兼容 |
| **会话持久化** | H2 数据库存储会话和消息历史 |
| **工具注册** | 自定义工具通过 AgentConfig 注册到 Toolkit |

通过 starters 的自动装配，你可以用最少的配置快速搭建 Agent 服务，同时通过自定义配置覆盖默认行为，实现灵活的业务扩展。