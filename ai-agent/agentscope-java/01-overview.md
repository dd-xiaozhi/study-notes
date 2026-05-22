# 第一章：AgentScope Java 概述



## 1.1 什么是 AgentScope Java

AgentScope Java 是一个面向智能体的编程框架，用于构建基于大语言模型（LLM）的应用。它脱胎于 Python 版 AgentScope，并针对 Java 生态进行了深度优化。与传统的硬编码工作流不同，AgentScope Java 强调**智能体（Agent）**的核心地位——由 LLM 驱动、自主演知、自适应执行。

**核心定位**：用 Java 构建生产级 AI 智能体，让企业级 Java 项目能够无缝集成 LLM 驱动的智能体能力。

**典型应用场景**：

- **自动化助手**：邮件处理、文档生成、数据分析
- **智能客服**：多轮对话、意图识别、工具调用
- **代码助手**：代码审查、Bug 修复、代码生成
- **多智能体协作**：复杂任务分解与多角色协同

**与 Python 版的核心差异**：

| 特性 | AgentScope Python | AgentScope Java |
|------|-------------------|-----------------|
| 启动性能 | 解释执行，较慢 | 编译执行，毫秒级冷启动 |
| 生态集成 | 科学计算生态 | Spring Boot、企业级中间件 |
| 部署方式 | Python 环境依赖 | 纯 JVM，原生镜像编译 |
| 线程模型 | asyncio 单线程 | Project Reactor 响应式非阻塞 |

## 1.2 核心概念：ReAct 推理-行动范式

### 1.2.1 ReAct 是什么

ReAct（Reasoning and Acting）是一种让大语言模型在执行任务时**交替进行推理（Reasoning）和行动（Acting）**的设计范式。其核心思想是：模型不仅生成最终答案，而是先"思考"，然后根据思考结果决定是否调用外部工具，最后综合工具返回的信息继续迭代，直到完成任务。

这一思想最早由论文 *ReAct: Synergizing Reasoning and Acting in Language Models*（Yao et al., 2022）提出。相比于直接让模型生成答案，ReAct 有几个关键优势：

- **可解释性**：每一步推理都显式可见，便于追踪决策过程
- **可控性**：工具调用受框架约束，不会无限循环或执行危险操作
- **准确性**：通过外部工具获取实时、精确的信息，避免模型幻觉

### 1.2.2 ReAct 执行流程

AgentScope Java 中，ReAct 智能体的执行遵循以下循环：

```
┌──────────────────────────────────────────────────────┐
│                     开始执行                          │
└─────────────────────┬────────────────────────────────┘
                      ▼
┌──────────────────────────────────────────────────────┐
│  阶段一：推理（Reasoning）                             │
│  - 模型分析当前状态、用户输入、工具结果                │
│  - 决定下一步行动（回答 or 调用工具）                  │
└─────────────────────┬────────────────────────────────┘
                      ▼
              ┌───────────────┐
              │  是否调用工具？ │──否──►  返回最终回复  ◄──┐
              └───────┬───────┘                         │
                      │ 是                             │
                      ▼                                 │
┌──────────────────────────────────────────────────────┐
│  阶段二：行动（Acting）                               │
│  - 根据推理结果选择并调用对应工具                     │
│  - 收集工具返回结果                                   │
└─────────────────────┬────────────────────────────────┘
                      │                                 │
                      ▼                                 │
              ┌───────────────┐                         │
              │ 是否达到最大   │──是──► 返回当前结果     │ │
              │ 迭代次数？     │                         │ │
              └───────┬───────┘                         │ │
                      │ 否（继续循环）                   │ │
                      └──────────────────────────────────┘
```

在 AgentScope Java 中，这三个阶段通过事件钩子（Hook）暴露：

- `PreReasoningEvent` / `PostReasoningEvent`：推理前后插入自定义逻辑
- `PreActingEvent` / `PostActingEvent`：工具调用前后监控执行
- `PreSummaryEvent` / `PostSummaryEvent`：生成最终回复前调整内容

### 1.2.3 消息与会话

AgentScope Java 使用 `Msg` 对象作为所有通信的基本单元。每个消息包含：

- **角色（Role）**：`USER`（用户）、`ASSISTANT`（助手）、`SYSTEM`（系统）、`TOOL`（工具）
- **内容块（Content Blocks）**：支持文本、图片、音频、视频、思维链、工具调用等多种类型
- **元数据（Metadata）**：时间戳、生成原因等附加信息

会话（Session）管理多个消息的上下文，自动维护对话历史，支持多轮交互。

### 1.2.4 工具与工具箱

工具（Tool）是智能体与外部世界交互的桥梁。AgentScope Java 的工具系统基于方法级注解，任意带有 `@Tool` 注解的公共方法都会自动注册为可用工具。多个工具组织为工具箱（Toolkit），方便批量管理和隔离。

```java
// 工具示例
@Tool(name = "get_weather", description = "查询城市天气")
public String queryWeather(@ToolParam(name = "city") String city) {
    // 调用天气 API
    return "晴，25°C";
}
```

## 1.3 项目架构与模块划分

AgentScope Java 采用多模块 Maven 项目结构，主 pom 文件定义了以下模块：

```
agentscope-parent/
├── agentscope-core/          # 核心框架
├── agentscope-harness/       # 安全沙箱执行环境
├── agentscope-extensions/     # 扩展功能（Pipeline、RAG 等）
├── agentscope-examples/       # 示例代码
├── agentscope-dependencies-bom/ # 统一依赖版本管理
└── agentscope-distribution/   # 发布包
```

### 1.3.1 agentscope-core（核心框架）

这是框架的主体，包含了构建智能体应用所需的全部核心接口和实现：

| 包路径 | 说明 |
|--------|------|
| `io.agentscope.core.agent` | 智能体核心接口：Agent、ReActAgent、CallableAgent 等 |
| `io.agentscope.core.message` | 消息模型：Msg、TextBlock、ImageBlock、ToolUseBlock 等 |
| `io.agentscope.core.model` | 模型客户端：DashScope、OpenAI、Ollama、Gemini 等 |
| `io.agentscope.core.memory` | 记忆系统：InMemoryMemory、LongTermMemory |
| `io.agentscope.core.tool` | 工具系统：Toolkit、ToolResultMessageBuilder |
| `io.agentscope.core.hook` | 生命周期钩子：PreReasoningEvent、PostActingEvent 等 |
| `io.agentscope.core.session` | 会话管理 |
| `io.agentscope.core.plan` | 任务计划：PlanNotebook |
| `io.agentscope.core.interruption` | 安全中断机制 |
| `io.agentscope.core.pipeline` | 多代理流水线 |
| `io.agentscope.core.rag` | RAG 检索增强生成 |
| `io.agentscope.core.skill` | 技能系统 |
| `io.agentscope.core.tracing` | 分布式追踪（OpenTelemetry） |

### 1.3.2 agentscope-harness（安全沙箱）

Harness 为不可信的第三方工具代码提供隔离执行环境，防止恶意代码访问系统资源。主要特性：

- **文件系统隔离**：工具只能访问指定工作目录，无法读写系统敏感路径
- **GUI 自动化沙箱**：内置 GUI 操作的安全预置环境
- **资源限制**：CPU、内存、执行时间均有上限

### 1.3.3 agentscope-extensions（扩展功能）

包含非核心但常用的扩展能力：

- **A2A（Agent-to-Agent）协议**：多智能体之间通过标准协议通信
- **MCP（Model Context Protocol）**：集成 MCP 兼容的外部工具和服务
- **Pipeline**：多代理流水线编排

### 1.3.4 agentscope-dependencies-bom（依赖管理）

统一的 Maven BOM（Bill of Materials），管理所有第三方依赖的版本，避免版本冲突。

## 1.4 环境准备

### 1.4.1 JDK 21 安装

AgentScope Java 要求 JDK 17 以上，推荐使用 **JDK 21** 以充分利用虚拟线程（Virtual Threads）等新特性。

**下载安装**：

- Oracle JDK 21：https://www.oracle.com/java/technologies/downloads/#java21
- OpenJDK 21：https://adoptium.net/temurin/releases/?version=21

**验证安装**：

```bash
java -version
# 应显示 21.x.x

# 设置 JAVA_HOME（Windows PowerShell）
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x.x"
```

> 注意：AgentScope Java 已在主 pom.xml 中将 Java 版本配置为 17 以上的最小兼容版本。如果你的项目需要使用更高级的 JVM 特性，可在项目 pom.xml 中覆盖 `<java.version>` 属性。

### 1.4.2 Maven 配置

**版本要求**：Maven 3.8+（推荐 3.9+）

**验证安装**：

```bash
mvn -version
# Apache Maven 3.9.x
# Java version: 21.x.x
```

**Maven 镜像配置**（适用于国内用户，编辑 `~/.m2/settings.xml`）：

```xml
<mirrors>
    <mirror>
        <id>aliyun-maven</id>
        <mirrorOf>central</mirrorOf>
        <name>Aliyun Maven Mirror</name>
        <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
</mirrors>
```

### 1.4.3 Docker 配置（可选）

Harness 沙箱功能依赖 Docker 进行隔离执行。如果你的项目不需要 Harness，可以跳过此步骤。

**验证安装**：

```bash
docker --version
docker run --rm hello-world
```

**Docker Desktop**（Windows）：从 https://www.docker.com/products/docker-desktop/ 下载安装，安装后确保 WSL 2 后端正常运行。

## 1.5 【案例】环境验证项目

本节创建一个完整的 Spring Boot 3 + Java 21 项目，验证 AgentScope Java 环境是否正确配置。项目使用 H2 嵌入式数据库，无需 Docker。

### 1.5.1 项目结构

```
chapter01-verification/
├── pom.xml
└── src/main/
    ├── java/io/agentscope/tutorial/chapter01/
    │   ├── Chapter01Application.java
    │   ├── config/
    │   │   └── AgentScopeConfig.java
    │   ├── agent/
    │   │   └── SimpleAssistantAgent.java
    │   └── service/
    │       └── AgentService.java
    └── resources/
        └── application.yml
```

### 1.5.2 pom.xml

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
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter01-verification</artifactId>
    <version>1.0.0</version>
    <name>chapter01-verification</name>
    <description>AgentScope Java 第一章环境验证项目</description>

    <properties>
        <java.version>21</java.version>
        <agentscope.version>1.0.12</agentscope.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot 数据源（使用 H2 嵌入式数据库） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- AgentScope Java 核心依赖 -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- Lombok（简化代码，非必须） -->
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

### 1.5.3 应用配置文件

`src/main/resources/application.yml`

```yaml
spring:
  application:
    name: agentscope-chapter01

  # H2 嵌入式数据库配置
  datasource:
    url: jdbc:h2:mem:agentscope_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:

  # H2 Web 控制台（开发调试用，生产环境请关闭）
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
    database-platform: org.hibernate.dialect.H2Dialect

# AgentScope 配置
agentscope:
  # 模型配置（此处使用占位符，通过环境变量或配置文件注入）
  model:
    api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
    model-name: qwen-plus
```

### 1.5.4 框架配置类

`src/main/java/io/agentscope/tutorial/chapter01/config/AgentScopeConfig.java`

```java
package io.agentscope.tutorial.chapter01.config;

import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentScope 框架配置类。
 *
 * <p>本配置类负责初始化 AgentScope 的核心组件：
 * <ul>
 *   <li>Model：模型客户端配置（此处以阿里云 DashScope 为例）</li>
 *   <li>Memory：对话记忆存储器</li>
 * </ul>
 *
 * <p>在实际项目中，可以将配置抽取到 application.yml 中，通过 @ConfigurationProperties 绑定。
 */
@Configuration
public class AgentScopeConfig {

    /**
     * 从配置文件中读取 DashScope API Key。
     * 建议在生产环境中使用密钥管理服务（如阿里云 KMS）存储敏感信息。
     */
    @Value("${agentscope.model.api-key}")
    private String apiKey;

    /**
     * 模型名称配置，默认为 qwen-plus。
     * 支持的模型请参考阿里云百炼文档。
     */
    @Value("${agentscope.model.model-name:qwen-plus}")
    private String modelName;

    /**
     * 创建 DashScope 模型客户端 Bean。
     *
     * <p>DashScopeChatModel 是 AgentScope 提供的阿里云百炼模型客户端实现，
     * 支持 qwen 系列等多种模型。这里采用 Builder 模式进行配置。
     *
     * @return 配置好的模型客户端实例
     */
    @Bean
    public Model chatModel() {
        return DashScopeChatModel.builder()
                // 从环境变量或配置文件读取 API Key
                .apiKey(apiKey)
                // 指定要使用的模型名称
                .modelName(modelName)
                // 可选：设置请求超时时间（单位：秒）
                .timeout(60)
                .build();
    }

    /**
     * 创建内存记忆存储器 Bean。
     *
     * <p>InMemoryMemory 是 AgentScope 提供的基础记忆实现，
     * 将对话历史存储在内存中。适合单实例短期运行的场景。
     *
     * <p>对于需要跨会话持久化的场景，可以使用 LongTermMemory
     * 配合数据库或向量数据库存储。
     *
     * @return 内存记忆实例
     */
    @Bean
    public Memory memory() {
        return new InMemoryMemory();
    }
}
```

### 1.5.5 智能体服务类

`src/main/java/io/agentscope/tutorial/chapter01/service/AgentService.java`

```java
package io.agentscope.tutorial.chapter01.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 * 智能体服务类：负责创建和管理 Agent 实例。
 *
 * <p>本类演示了如何在 Spring Boot 环境中集成 AgentScope 智能体。
 * 在实际应用中，通常建议将 Agent 实例作为单例 Bean 管理，
 * 避免重复创建带来的开销。
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final Model model;
    private final Memory memory;

    /** Agent 实例，由 Spring 容器管理其生命周期 */
    private Agent agent;

    public AgentService(Model model, Memory memory) {
        this.model = model;
        this.memory = memory;
    }

    /**
     * Spring 初始化后构建 Agent 实例。
     *
     * <p>使用 ReActAgent.builder() 构建一个支持 ReAct 范式的智能体。
     * ReActAgent 是 AgentScope 最核心的智能体实现，完整支持推理-行动循环。
     */
    @PostConstruct
    public void init() {
        this.agent = ReActAgent.builder()
                // 智能体名称，用于日志和调试
                .name("TutorialAssistant")
                // 智能体描述
                .description("AgentScope Java 第一章示例智能体")
                // 系统提示词：定义智能体的角色和行为
                .sysPrompt("""
                        你是一个乐于助人的 AI 助手。
                        你的名字叫 Tutorial Assistant，
                        由 AgentScope Java 框架驱动。
                        请用简洁友好的语言回答用户的问题。
                        """)
                // 注入模型客户端（由配置类注入 DashScope 模型）
                .model(model)
                // 注入记忆系统（由配置类注入内存记忆）
                .memory(memory)
                // 最大迭代次数，防止无限循环
                .maxIters(10)
                .build();

        log.info("Agent '{}' 初始化完成，当前记忆容量: {} 条消息",
                agent.getName(), memory.messages().size());
    }

    /**
     * 向智能体发送消息并获取回复。
     *
     * <p>这是一个同步调用方法，实际项目中可能需要改为异步或响应式方式。
     * agent.call() 返回的是 Project Reactor 的 Mono<Msg>，
     * 这里使用 .block() 转换为同步调用（仅用于演示）。
     *
     * @param userInput 用户输入的文本内容
     * @return 智能体的回复消息
     */
    public String chat(String userInput) {
        // 构建用户消息
        Msg userMsg = Msg.builder()
                .name("user")
                .role(MsgRole.USER)
                .content(TextBlock.of(userInput))
                .build();

        // 调用智能体，获取回复
        Msg response = agent.call(userMsg).block();

        // 提取回复文本内容
        String reply = response != null ? response.getTextContent() : "（未收到回复）";

        log.info("用户输入: {}", userInput);
        log.info("智能体回复: {}", reply);

        return reply;
    }

    /**
     * 获取当前智能体状态信息，用于监控和调试。
     */
    public String getAgentInfo() {
        return String.format(
                "Agent[名称=%s, ID=%s, 描述=%s]",
                agent.getName(),
                agent.getAgentId(),
                agent.getDescription()
        );
    }
}
```

### 1.5.6 REST 控制器

`src/main/java/io/agentscope/tutorial/chapter01/controller/ChatController.java`

```java
package io.agentscope.tutorial.chapter01.controller;

import io.agentscope.tutorial.chapter01.service.AgentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 聊天控制器：暴露 REST API 供前端或外部调用。
 *
 * <p>提供两个端点：
 * <ul>
 *   <li>POST /api/chat - 发送消息给智能体</li>
 *   <li>GET  /api/agent/info - 查询智能体状态</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 聊天接口。
     *
     * @param request 请求体，包含用户输入
     * @return 智能体回复
     */
    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String userInput = request.get("message");
        if (userInput == null || userInput.isBlank()) {
            return Map.of("error", "message 不能为空");
        }

        String reply = agentService.chat(userInput);
        return Map.of("reply", reply);
    }

    /**
     * 查询智能体状态信息。
     */
    @GetMapping("/agent/info")
    public Map<String, String> agentInfo() {
        return Map.of("info", agentService.getAgentInfo());
    }
}
```

### 1.5.7 Spring Boot 启动类

`src/main/java/io/agentscope/tutorial/chapter01/Chapter01Application.java`

```java
package io.agentscope.tutorial.chapter01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java 第一章示例应用启动类。
 *
 * <p>本应用演示了如何在 Spring Boot 3 + Java 21 环境下
 * 集成 AgentScope Java 框架，运行一个简单的智能体。
 *
 * <p>启动方式：
 * <ul>
 *   <li>Maven: mvn spring-boot:run</li>
 *   <li>IDE: 直接运行 main 方法</li>
 *   <li>JAR: java -jar target/chapter01-verification-1.0.0.jar</li>
 * </ul>
 *
 * <p>启动后访问：
 * <ul>
 *   <li>聊天 API: POST http://localhost:8080/api/chat</li>
 *   <li>Agent 信息: GET http://localhost:8080/api/agent/info</li>
 *   <li>H2 控制台: http://localhost:8080/h2-console（调试用）</li>
 * </ul>
 */
@SpringBootApplication
public class Chapter01Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter01Application.class, args);
    }
}
```

### 1.5.8 运行项目

**第一步：配置 API Key**

在使用阿里云 DashScope 模型前，需要设置 API Key 环境变量：

```bash
# Windows PowerShell
$env:DASHSCOPE_API_KEY = "your-actual-api-key"

# 或在 application.yml 中直接配置（不推荐用于生产环境）
# agentscope.model.api-key: sk-xxxxxxxxxxxxxxxx
```

**第二步：编译并启动**

```bash
cd chapter01-verification
mvn clean compile spring-boot:run
```

**第三步：测试调用**

```bash
# 查询 Agent 信息
curl http://localhost:8080/api/agent/info

# 发送聊天请求
curl -X POST http://localhost:8080/api/chat \
     -H "Content-Type: application/json" \
     -d "{\"message\": \"你好，请介绍一下你自己\"}"
```

**预期输出示例**：

```json
{
  "reply": "你好！我叫 Tutorial Assistant，由 AgentScope Java 框架驱动，是一个乐于助人的 AI 助手。有什么我可以帮助你的吗？"
}
```

### 1.5.9 完整测试代码（单元测试）

`src/test/java/io/agentscope/tutorial/chapter01/AgentServiceTest.java`

```java
package io.agentscope.tutorial.chapter01;

import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础功能测试：在不依赖外部 API 的情况下验证 AgentScope 核心组件的初始化。
 *
 * <p>完整的端到端测试需要配置有效的 API Key，
 * 建议在集成测试阶段使用真实模型进行。
 */
class AgentServiceTest {

    /** 验证框架依赖是否正确加载 */
    @Test
    void testDependencyLoading() {
        // 验证 Msg 类可以正常构造
        Msg msg = Msg.builder()
                .name("test")
                .role(MsgRole.USER)
                .content(TextBlock.of("Hello"))
                .build();

        assertNotNull(msg);
        assertEquals("test", msg.getName());
        assertEquals(MsgRole.USER, msg.getRole());
        assertEquals("Hello", msg.getTextContent());
    }

    /** 验证记忆系统基本功能 */
    @Test
    void testMemoryBasicOperations() {
        var memory = new InMemoryMemory();

        // 添加用户消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.of("你好"))
                .build();
        memory.add(userMsg);

        // 添加助手回复
        Msg assistantMsg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .content(TextBlock.of("你好！有什么可以帮助你的吗？"))
                .build();
        memory.add(assistantMsg);

        // 验证记忆已保存
        assertEquals(2, memory.messages().size());
    }

    /** 验证模型配置（不发起真实请求） */
    @Test
    void testModelConfiguration() {
        // 仅验证 Builder 模式配置是否正常
        var model = DashScopeChatModel.builder()
                .apiKey("test-key")
                .modelName("qwen-plus")
                .timeout(30)
                .build();

        assertNotNull(model);
        assertEquals("qwen-plus", model.modelName());
    }

    /** 验证 ReActAgent 可以正常构建（不执行调用） */
    @Test
    void testAgentBuilder() {
        var model = DashScopeChatModel.builder()
                .apiKey("test-key")
                .modelName("qwen-plus")
                .build();

        var agent = io.agentscope.core.ReActAgent.builder()
                .name("TestAgent")
                .sysPrompt("你是一个测试助手。")
                .model(model)
                .memory(new InMemoryMemory())
                .maxIters(3)
                .build();

        assertNotNull(agent);
        assertEquals("TestAgent", agent.getName());
        assertEquals(3, agent.getMaxIters());
    }
}
```

## 1.6 本章小结

本章介绍了 AgentScope Java 框架的核心概念与项目架构：

- **AgentScope Java** 是一个生产级的 Java 智能体编程框架，支持 ReAct 推理-行动范式、工具调用、记忆管理、多智能体协作等核心能力
- **ReAct 范式**通过交替进行"推理"和"行动"两个阶段，使智能体能够自主规划并调用外部工具完成复杂任务
- **项目分为三大核心模块**：agentscope-core（核心框架）、agentscope-harness（安全沙箱）、agentscope-extensions（扩展功能）
- **环境要求**：JDK 17+（推荐 21）、Maven 3.8+、Docker（可选，仅 Harness 场景需要）

下一章我们将创建第一个真正能调用 LLM 的 Agent，体验完整的 ReAct 执行流程。