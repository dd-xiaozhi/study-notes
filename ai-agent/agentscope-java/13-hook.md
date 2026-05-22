# 第十三章：Hook 钩子系统

AgentScope Java 的 Hook 钩子系统是实现 Agent 运行时可观测性、可干预性和业务扩展的核心机制。通过 Hook，开发者可以在 Agent 执行的关键阶段插入自定义逻辑，实现日志记录、性能监控、安全检查、人工介入等功能，而无需修改核心 Agent 代码。

## 13.1 生命周期钩子概念

### 13.1.1 什么是 Hook

Hook（钩子）是一种事件监听机制。AgentScope Java 将 Agent 的执行过程拆解为一系列生命周期事件，每个事件触发时，所有注册的 Hook 都会被调用。Hook 可以读取事件上下文、修改事件数据、甚至中止 Agent 的执行流程。

**核心设计原则：**

- **统一事件模型**：所有 Hook 事件都通过 `onEvent(T event)` 方法处理，使用 Java 17+ 的 pattern matching 进行类型分发
- **响应式编程**：Hook 返回 `Mono<T>`（Project Reactor），支持异步处理和链式调用
- **优先级机制**：Hook 可设置优先级（0-1000），数值越小优先级越高，优先执行
- **可修改事件**：部分事件（如 `PreReasoningEvent`、`PostActingEvent`）提供了 setter 方法，允许 Hook 修改后续处理的数据

### 13.1.2 事件类型体系

AgentScope Java 定义了完整的生命周期事件类型，存放在 `io.agentscope.core.hook` 包中：

```
HookEvent (sealed abstract class)
├── PreCallEvent          - Agent 调用开始前（可修改输入）
├── PostCallEvent         - Agent 调用完成后（可修改最终输出）
├── ReasoningEvent (sealed)
│   ├── PreReasoningEvent    - LLM 推理前（可修改输入消息）
│   ├── PostReasoningEvent   - LLM 推理后（可修改推理结果，可中止）
│   └── ReasoningChunkEvent  - 推理流式输出（只读）
├── ActingEvent (sealed)
│   ├── PreActingEvent       - 工具执行前（可修改工具参数）
│   ├── PostActingEvent      - 工具执行后（可修改结果，可中止）
│   └── ActingChunkEvent     - 工具流式输出（只读）
├── SummaryEvent (sealed)
│   ├── PreSummaryEvent      - 摘要生成前
│   ├── SummaryChunkEvent    - 摘要流式输出
│   └── PostSummaryEvent     - 摘要生成后
└── ErrorEvent             - 执行出错（只读）
```

### 13.1.3 事件流向图

```
┌─────────────────────────────────────────────────────────────────┐
│                        Agent.call()                             │
├─────────────────────────────────────────────────────────────────┤
│  1. PreCallEvent ──► [Hook] ──► Hook ──► ...                   │
│         ↓                                                        │
│  2. PreReasoningEvent ──► [Hook] ──► Hook ──► ...               │
│         ↓                                                        │
│  3. LLM.stream() ──► ReasoningChunkEvent ──► ...                │
│         ↓                                                        │
│  4. PostReasoningEvent ──► [Hook] ──► Hook ──► ...               │
│         ↓                                                        │
│  ┌─────────────────────────────────────────┐                   │
│  │  循环：ToolUseBlock 存在？               │                   │
│  │    ├─► PreActingEvent ──► [Hook]        │                   │
│  │    │         ↓                          │                   │
│  │    │   Tool.execute() ──► ActingChunk   │                   │
│  │    │         ↓                          │                   │
│  │    └─► PostActingEvent ──► [Hook]        │                   │
│  │         ↓                                │                   │
│  └─────► 回到 PreReasoningEvent             │                   │
│         ↓                                                        │
│  5. PreSummaryEvent ──► [Hook]                                   │
│         ↓                                                        │
│  6. SummaryChunkEvent ──► ...                                    │
│         ↓                                                        │
│  7. PostSummaryEvent ──► ...                                     │
│         ↓                                                        │
│  8. PostCallEvent ──► [Hook] ──► 返回最终消息                    │
└─────────────────────────────────────────────────────────────────┘
        ↓
   ErrorEvent （任意阶段出错时触发）
```

### 13.1.4 Hook 接口详解

```java
public interface Hook {

    /**
     * 处理钩子事件
     *
     * @param event 钩子事件
     * @param <T> 事件具体类型
     * @return 包含潜在修改后事件的无状态 Mono
     */
    <T extends HookEvent> Mono<T> onEvent(T event);

    /**
     * 可选：随 Hook 一起安装的工具列表
     * 在 Agent 构建时，这些工具会自动注册到 Agent 的 Toolkit 中
     *
     * @return 工具实例列表（默认返回空列表）
     */
    default List<Object> tools() {
        return Collections.emptyList();
    }

    /**
     * 钩子优先级（数值越小越先执行）
     *
     * 常见优先级范围：
     * - 0-50:   系统关键钩子（认证、安全）
     * - 51-100: 高优先级钩子（验证、预处理）
     * - 101-500: 普通业务钩子
     * - 501-1000: 低优先级钩子（日志、监控）
     *
     * @return 优先级数值（默认 100）
     */
    default int priority() {
        return 100;
    }
}
```

### 13.1.5 事件可修改性

| 事件类型 | 可修改 | 允许的操作 |
|---------|--------|-----------|
| PreCallEvent | 是 | `setInputMessages(List<Msg>)` |
| PreReasoningEvent | 是 | `setInputMessages(List<Msg>)`, `setGenerateOptions(GenerateOptions)` |
| PostReasoningEvent | 是 | `setReasoningMessage(Msg)`, `stopAgent()`, `gotoReasoning()` |
| PreActingEvent | 是 | 工具参数修改（通过修改 ToolUseBlock） |
| PostActingEvent | 是 | `setToolResult(ToolResultBlock)`, `stopAgent()` |
| PostCallEvent | 是 | `setFinalMessage(Msg)` |
| ReasoningChunkEvent | 否 | 只读，流式通知 |
| ActingChunkEvent | 否 | 只读，流式通知 |
| ErrorEvent | 否 | 只读，错误通知 |

## 13.2 内置钩子实现

AgentScope Java 提供了一系列开箱即用的内置 Hook，满足常见的监控、日志、追踪需求。

### 13.2.1 JsonlTraceExporter - JSONL 追踪导出器

**位置**：`io.agentscope.core.hook.recorder.JsonlTraceExporter`

**功能**：将所有 Hook 事件以 JSON Lines 格式写入文件，适用于本地调试、离线排查和 GitHub Issue 附图。

**特性**：

- 异步写入，单线程队列保证顺序一致性
- 支持 OpenTelemetry trace_id 和 span_id 注入
- 可配置的事件过滤（默认记录主要事件）
- best-effort 模式：IO 错误不中断 Agent 执行

**使用示例**：

```java
// 创建追踪导出器
JsonlTraceExporter tracer = JsonlTraceExporter.builder(
        Path.of("logs", "agent-trace.jsonl"))
    .includeReasoningChunks(true)   // 包含推理流式输出
    .includeActingChunks(true)      // 包含工具执行流式输出
    .includeSummary(true)          // 包含摘要事件
    .priority(900)                 // 低优先级（日志类钩子）
    .build();

// 注册到 Agent
ReActAgent agent = ReActAgent.builder()
    .name("TracedAgent")
    .hooks(List.of(tracer))         // 可同时注册多个 Hook
    // ... 其他配置
    .build();

// 使用 try-with-resources 确保正确关闭
try (tracer) {
    Msg response = agent.call(userInput);
}
```

### 13.2.2 LoggingHook - 通用日志钩子（自定义实现）

以下是一个生产级的日志 Hook 示例，整合了 Spring Boot 的日志框架：

```java
package io.agentscope.tutorial.chapter13;

import io.agentscope.core.hook.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 生产级日志钩子：记录所有关键生命周期事件
 * 使用 SLF4J/Logback 与 Spring Boot 集成
 */
public class LoggingHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(LoggingHook.class);

    // 可配置：是否记录流式 chunks（可能产生大量日志）
    private final boolean logStreamingChunks;

    public LoggingHook() {
        this(false);
    }

    public LoggingHook(boolean logStreamingChunks) {
        this.logStreamingChunks = logStreamingChunks;
    }

    @Override
    public int priority() {
        return 950; // 低优先级，确保在业务钩子之后执行
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return switch (event) {
            case PreCallEvent e -> handlePreCall(e);
            case PostCallEvent e -> handlePostCall(e);
            case PreReasoningEvent e -> handlePreReasoning(e);
            case PostReasoningEvent e -> handlePostReasoning(e);
            case ReasoningChunkEvent e when logStreamingChunks -> handleReasoningChunk(e);
            case PreActingEvent e -> handlePreActing(e);
            case PostActingEvent e -> handlePostActing(e);
            case ActingChunkEvent e when logStreamingChunks -> handleActingChunk(e);
            case ErrorEvent e -> handleError(e);
            default -> Mono.just(event);
        };
    }

    private <T extends HookEvent> Mono<T> handlePreCall(PreCallEvent e) {
        log.info("[{}] Agent 调用开始 - 输入消息数: {}",
                e.getAgent().getName(),
                e.getInputMessages().size());
        return Mono.just((T) event);
    }

    private <T extends HookEvent> Mono<T> handlePostCall(PostCallEvent e) {
        Msg finalMsg = e.getFinalMessage();
        log.info("[{}] Agent 调用完成 - 响应内容长度: {}",
                e.getAgent().getName(),
                finalMsg != null ? finalMsg.getTextContent().length() : 0);
        return Mono.just((T) event);
    }

    private <T extends HookEvent> Mono<T> handlePreReasoning(PreReasoningEvent e) {
        log.debug("[{}] 准备推理 - 模型: {}, 输入消息数: {}",
                e.getAgent().getName(),
                e.getModelName(),
                e.getInputMessages().size());
        return Mono.just((T) event);
    }

    private <T extends HookEvent> Mono<T> handlePostReasoning(PostReasoningEvent e) {
        Msg reasoningMsg = e.getReasoningMessage();
        if (reasoningMsg != null) {
            List<ToolUseBlock> toolCalls = reasoningMsg.getContentBlocks(ToolUseBlock.class);
            log.info("[{}] 推理完成 - ToolCalls: {}",
                    e.getAgent().getName(),
                    toolCalls.size());
        } else {
            log.info("[{}] 推理完成 - 无 ToolCall",
                    e.getAgent().getName());
        }
        return Mono.just((T) event);
    }

    private <T extends HookEvent> Mono<T> handleReasoningChunk(ReasoningChunkEvent e) {
        Msg chunk = e.getIncrementalChunk();
        String text = chunk != null ? chunk.getTextContent() : "";
        log.trace("[{}] 推理 Chunk: {}",
                e.getAgent().getName(),
                text.length() > 100 ? text.substring(0, 100) + "..." : text);
        return Mono.just((T) event);
    }

    private <T extends HookEvent> Mono<T> handlePreActing(PreActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();
        log.info("[{}] 执行工具: {} - 输入: {}",
                e.getAgent().getName(),
                toolUse.getName(),
                toolUse.getInput());
        return Mono.just((T) event);
    }

    private <T extends HookEvent> Mono<T> handlePostActing(PostActingEvent e) {
        ToolResultBlock result = e.getToolResult();
        String output = result != null && !result.getOutput().isEmpty()
                ? result.getOutput().get(0).toString()
                : "(empty)";
        log.info("[{}] 工具执行完成: {} - 结果: {}",
                e.getAgent().getName(),
                e.getToolUse().getName(),
                output.length() > 200 ? output.substring(0, 200) + "..." : output);
        return Mono.just((T) event);
    }

    private <T extends HookEvent> Mono<T> handleActingChunk(ActingChunkEvent e) {
        ToolResultBlock chunk = e.getChunk();
        log.trace("[{}] 工具 Chunk: {}",
                e.getAgent().getName(),
                e.getToolUse().getName());
        return Mono.just((T) event);
    }

    private <T extends HookEvent> Mono<T> handleError(ErrorEvent e) {
        log.error("[{}] 执行出错: {} - {}",
                e.getAgent().getName(),
                e.getError().getClass().getSimpleName(),
                e.getError().getMessage(),
                e.getError());
        return Mono.just((T) event);
    }
}
```

### 13.2.3 其他内置 Hook 示例

| Hook 类 | 位置 | 功能 |
|--------|------|------|
| TTSHook | `io.agentscope.core.hook.TTSHook` | 文本转语音流式输出 |
| SkillHook | `io.agentscope.core.skill.SkillHook` | 技能动态注册 |
| GenericRAGHook | `io.agentscope.core.rag.GenericRAGHook` | RAG 检索增强 |
| AutoContextHook | `agentscope-extensions-autocontext-memory` | 自动上下文记忆 |
| StudioMessageHook | `agentscope-extensions-studio` | Studio 消息同步 |

## 13.3 自定义钩子开发

### 13.3.1 基本开发模式

自定义 Hook 需要实现 `Hook` 接口，核心是重写 `onEvent` 方法。使用 Java 17+ 的 pattern matching 进行事件类型匹配：

```java
public class MyCustomHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        // 使用 pattern matching 处理不同事件
        return switch (event) {
            case PreCallEvent e -> {
                // 处理 Agent 开始事件
                yield Mono.just(event);
            }
            case PostCallEvent e -> {
                // 处理 Agent 完成事件
                yield Mono.just(event);
            }
            default -> Mono.just(event);  // 其他事件保持不变
        };
    }
}
```

### 13.3.2 拦截与修改示例

**示例 1：注入系统提示词**

在推理前向消息列表中添加额外的系统指令：

```java
public class HintInjectionHook implements Hook {

    private final String hintText;

    public HintInjectionHook(String hintText) {
        this.hintText = hintText;
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent e) {
            // 复制一份消息列表（不可变）
            List<Msg> modifiedMessages = new ArrayList<>(e.getInputMessages());

            // 在开头插入提示词
            modifiedMessages.add(0, Msg.builder()
                .role(MsgRole.SYSTEM)
                .name("hint")
                .content(TextBlock.of(hintText))
                .build());

            // 应用修改
            e.setInputMessages(modifiedMessages);
        }
        return Mono.just(event);
    }

    @Override
    public int priority() {
        return 80; // 高优先级，在推理前尽早注入
    }
}
```

**示例 2：工具调用安全检查**

在工具执行前检查并阻止危险操作：

```java
import io.agentscope.core.hook.*;
import io.agentscope.core.message.ToolUseBlock;
import reactor.core.publisher.Mono;

import java.util.Set;

public class SecurityCheckHook implements Hook {

    private final Set<String> blockedTools;

    public SecurityCheckHook(Set<String> blockedTools) {
        this.blockedTools = Set.copyOf(blockedTools);
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreActingEvent e) {
            ToolUseBlock toolUse = e.getToolUse();
            if (blockedTools.contains(toolUse.getName())) {
                // 阻止危险工具执行
                System.out.println("[SECURITY] 阻止执行危险工具: " + toolUse.getName());
                // 通过设置空结果来模拟阻止
                e.setToolUse(ToolUseBlock.builder()
                    .name(toolUse.getName())
                    .input(toolUse.getInput())
                    .id(toolUse.getId())
                    .build());
                // 注意：实际阻止需要更复杂的机制
            }
        }
        return Mono.just(event);
    }

    @Override
    public int priority() {
        return 10; // 最高优先级，最先执行
    }
}
```

**示例 3：人工介入钩子（Human-in-the-Loop）**

在危险工具执行前暂停，等待人工确认：

```java
import io.agentscope.core.hook.*;
import reactor.core.publisher.Mono;

import java.util.Scanner;
import java.util.Set;

public class HumanConfirmationHook implements Hook {

    private final Set<String> sensitiveTools;

    public HumanConfirmationHook(Set<String> sensitiveTools) {
        this.sensitiveTools = Set.copyOf(sensitiveTools);
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PostReasoningEvent e) {
            // 检查是否有敏感工具被调用
            var toolCalls = e.getReasoningMessage().getContentBlocks(ToolUseBlock.class);
            boolean hasSensitiveTool = toolCalls.stream()
                .anyMatch(t -> sensitiveTools.contains(t.getName()));

            if (hasSensitiveTool) {
                System.out.println("\n[确认] 检测到敏感工具调用，需要人工确认 (y/n): ");
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine().trim().toLowerCase();

                if (!input.equals("y") && !input.equals("yes")) {
                    // 用户拒绝，停止 Agent 执行
                    e.stopAgent();
                    System.out.println("[确认] 用户取消操作");
                } else {
                    System.out.println("[确认] 用户批准继续");
                }
            }
        }
        return Mono.just(event);
    }

    @Override
    public int priority() {
        return 50; // 高优先级，尽早拦截
    }
}
```

### 13.3.3 钩子优先级设计

合理的优先级设计确保钩子链按预期顺序执行：

```java
// 优先级常量定义
public class HookPriorities {
    public static final int SYSTEM_CRITICAL = 10;   // 系统关键（认证、安全）
    public static final int SECURITY_CHECK = 30;   // 安全检查
    public static final int VALIDATION = 60;       // 数据验证
    public static final int PREPROCESSING = 80;    // 预处理
    public static final int NORMAL = 100;          // 默认业务逻辑
    public static final int POSTPROCESSING = 200;  // 后处理
    public static final int LOGGING = 900;         // 日志记录
    public static final int METRICS = 950;         // 指标收集
}

// 优先级组配示例
List<Hook> hooks = List.of(
    new SecurityCheckHook(blockedTools),     // 10
    new HumanConfirmationHook(sensitiveTools), // 50
    new HintInjectionHook("Think step by step"), // 80
    new BusinessLogicHook(),                 // 100
    new PostProcessingHook(),               // 200
    new LoggingHook(),                      // 900
    new MetricsHook()                       // 950
);
```

### 13.3.4 钩子链执行顺序

```
Agent 执行流程:

[PreCall]     --> SecurityHook(10) --> ValidationHook(60) --> LoggingHook(900)
                  ↓
[PreReasoning] --> HintInjectionHook(80) --> CustomHook(100)
                  ↓
[LLM Call]    --> (ReasoningChunk events)
                  ↓
[PostReasoning] --> PostProcessHook(200) --> LoggingHook(900)
                  ↓
[Loop: Acting] --> SecurityCheck(30) --> BusinessLogic(100) --> Logging(900)
                  ↓
[PreSummary]  --> ...
                  ↓
[PostSummary] --> ...
                  ↓
[PostCall]    --> FinalProcessingHook(300) --> Logging(900)
                  ↓
               --> Agent 返回最终消息
```

## 13.4 【案例】日志与监控钩子实战

### 13.4.1 项目概述

本案例实现一个完整的 Spring Boot 3 + Java 21 应用，演示如何：

1. 配置内置 `JsonlTraceExporter` 实现调用追踪
2. 自定义 `LoggingHook` 记录结构化日志
3. 自定义 `MetricsHook` 收集性能指标
4. 自定义 `ToolAuditHook` 审计工具调用
5. 配置钩子优先级链

### 13.4.2 项目结构

```
src/main/java/io/agentscope/tutorial/chapter13/
├── Chapter13Application.java          # Spring Boot 启动类
├── config/
│   └── AgentConfig.java               # Agent 配置类
├── hook/
│   ├── LoggingHook.java              # 自定义日志钩子
│   ├── MetricsHook.java             # 自定义监控钩子
│   └── ToolAuditHook.java           # 工具审计钩子
├── service/
│   └── AgentService.java            # Agent 服务层
└── controller/
    └── AgentController.java         # REST 控制器
```

### 13.4.3 完整代码实现

**pom.xml 依赖配置：**

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
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter13-hook</artifactId>
    <version>1.0.0</version>
    <name>Chapter 13 - Hook System</name>
    <description>AgentScope Java Hook System Tutorial</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- AgentScope DashScope Model (for demo) -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-model-dashscope</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- Lombok (optional, for reducing boilerplate) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Test -->
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
            </plugin>
        </plugins>
    </build>
</project>
```

**application.yml 配置：**

```yaml
spring:
  application:
    name: agentscope-chapter13

# AgentScope 配置
agentscope:
  api-key: ${DASHSCOPE_API_KEY:your-api-key-here}
  model:
    default: qwen-plus
    thinking:
      enabled: true

# 日志配置
logging:
  level:
    io.agentscope: DEBUG
    io.agentscope.tutorial: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/agentscope.log

server:
  port: 8080
```

**Chapter13Application.java - Spring Boot 启动类：**

```java
package io.agentscope.tutorial.chapter13;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Hook 系统教程 - Spring Boot 启动类
 *
 * 本案例演示如何通过 Spring Boot 3 + Java 21 集成 AgentScope Java 的 Hook 机制，
 * 实现日志记录、性能监控和工具调用审计。
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter13Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter13Application.class, args);
    }
}
```

**config/AgentConfig.java - Agent 配置与 Bean 定义：**

```java
package io.agentscope.tutorial.chapter13.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.recorder.JsonlTraceExporter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter13.hook.LoggingHook;
import io.agentscope.tutorial.chapter13.hook.MetricsHook;
import io.agentscope.tutorial.chapter13.hook.ToolAuditHook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Agent 配置类 - 集中管理 Agent 实例的创建和钩子配置
 *
 * 通过 Spring 的依赖注入机制，确保：
 * 1. 所有钩子只创建一次（单例）
 * 2. Agent 及其钩子链在应用启动时完成初始化
 * 3. 便于测试时替换模拟实现
 */
@Configuration
public class AgentConfig {

    @Value("${agentscope.api-key}")
    private String apiKey;

    @Value("${agentscope.model.default:qwen-plus}")
    private String defaultModel;

    /**
     * 配置日志钩子 - 记录所有关键生命周期事件
     * 优先级 900：低优先级，确保在业务逻辑之后执行
     */
    @Bean
    public LoggingHook loggingHook() {
        return new LoggingHook(false); // 不记录流式 chunks 以减少日志量
    }

    /**
     * 配置监控钩子 - 收集性能指标
     * 优先级 950：最低优先级，专注于指标收集
     */
    @Bean
    public MetricsHook metricsHook() {
        return new MetricsHook();
    }

    /**
     * 配置工具审计钩子 - 记录工具调用的详细信息
     * 优先级 60：高优先级，尽早拦截工具调用
     */
    @Bean
    public ToolAuditHook toolAuditHook() {
        // 审计所有工具调用
        return new ToolAuditHook(Set.of());
    }

    /**
     * 配置 JSONL 追踪导出器 - 将所有事件写入文件
     * 优先级 900：与日志钩子同级别
     */
    @Bean
    public JsonlTraceExporter traceExporter() {
        return JsonlTraceExporter.builder(Path.of("logs", "agent-trace.jsonl"))
                .includeReasoningChunks(true)
                .includeActingChunks(true)
                .includeSummary(true)
                .priority(900)
                .build();
    }

    /**
     * 配置钩子列表 - 按优先级排序
     *
     * 执行顺序：
     * 1. ToolAuditHook(60)   - 工具审计
     * 2. MetricsHook(950)    - 指标收集
     * 3. LoggingHook(900)   - 日志记录
     * 4. TraceExporter(900)  - 追踪导出
     */
    @Bean
    public List<Hook> hooks(LoggingHook loggingHook,
                            MetricsHook metricsHook,
                            ToolAuditHook toolAuditHook,
                            JsonlTraceExporter traceExporter) {
        return List.of(
                toolAuditHook,   // 优先级 60
                metricsHook,     // 优先级 950
                loggingHook,     // 优先级 900
                traceExporter    // 优先级 900
        );
    }

    /**
     * 创建示例工具集
     */
    @Bean
    public Toolkit agentToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new DemoTools());
        return toolkit;
    }

    /**
     * 创建 ReActAgent 实例
     *
     * 配置说明：
     * - name: Agent 标识名称
     * - sysPrompt: 系统提示词，定义 Agent 行为
     * - model: 使用 DashScope 阿里云模型
     * - toolkit: 注册的工具集合
     * - memory: 内存管理（InMemoryMemory 仅用于演示，生产环境建议使用持久化方案）
     * - hooks: 钩子链配置
     */
    @Bean
    public ReActAgent tutorialAgent(List<Hook> hooks, Toolkit agentToolkit) {
        return ReActAgent.builder()
                .name("TutorialAgent")
                .sysPrompt("""
                    You are a helpful assistant with access to several tools.

                    Available tools:
                    - calculate: Perform mathematical calculations
                    - get_weather: Get weather information for a city
                    - search_info: Search for information on the internet

                    Use tools when needed to answer user questions accurately.
                    Always explain your reasoning process.
                    """)
                .model(DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(defaultModel)
                        .stream(true)
                        .enableThinking(true)
                        .formatter(new DashScopeChatFormatter())
                        .build())
                .toolkit(agentToolkit)
                .memory(new InMemoryMemory())
                .hooks(hooks)
                .build();
    }

    /**
     * 示例工具类 - 演示如何在 Hook 中观察工具调用
     */
    public static class DemoTools {

        @Tool(name = "calculate", description = "Perform a mathematical calculation")
        public String calculate(
                @ToolParam(name = "expression", description = "Mathematical expression to evaluate")
                String expression) {
            try {
                // 安全评估：仅支持基本数学运算
                double result = evaluateExpression(expression);
                return String.format("%.2f", result);
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }

        @Tool(name = "get_weather", description = "Get weather for a city")
        public String getWeather(
                @ToolParam(name = "city", description = "City name")
                String city) {
            // 模拟天气查询
            return String.format("Weather in %s: Sunny, 25°C", city);
        }

        @Tool(name = "search_info", description = "Search information online")
        public String searchInfo(
                @ToolParam(name = "query", description = "Search query")
                String query) {
            // 模拟搜索
            return String.format("Search results for '%s': Found 100 results", query);
        }

        /**
         * 安全评估数学表达式（仅用于演示，生产环境请使用专业表达式求值库）
         */
        private double evaluateExpression(String expr) {
            // 移除空格
            expr = expr.replaceAll("\\s+", "");
            // 使用简单的正则表达式验证安全性
            if (!expr.matches("^[0-9+\\-*/().]+$")) {
                throw new IllegalArgumentException("Invalid expression");
            }
            // 使用 ScriptEngine 进行计算
            try {
                return new javax.script.ScriptEngineManager()
                        .getEngineByName("JavaScript")
                        .eval(expr)
                        .toString()
                        .matches("-?\\d+(\\.\\d+)?")
                        ? Double.parseDouble(new javax.script.ScriptEngineManager()
                                .getEngineByName("JavaScript")
                                .eval(expr).toString())
                        : 0;
            } catch (Exception e) {
                throw new RuntimeException("Calculation error: " + e.getMessage());
            }
        }
    }
}
```

**hook/LoggingHook.java - 自定义日志钩子：**

```java
package io.agentscope.tutorial.chapter13.hook;

import io.agentscope.core.hook.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 自定义日志钩子 - 记录 Agent 执行的关键生命周期事件
 *
 * 功能特点：
 * 1. 实现完整的生命周期事件监听
 * 2. 使用 SLF4J 与 Spring Boot 日志框架集成
 * 3. 可配置是否记录流式 chunks
 * 4. 结构化日志输出，便于日志分析
 *
 * @author AgentScope Tutorial
 */
public class LoggingHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(LoggingHook.class);

    /** 是否记录流式 chunks（可能产生大量日志） */
    private final boolean logStreamingChunks;

    public LoggingHook() {
        this(false);
    }

    public LoggingHook(boolean logStreamingChunks) {
        this.logStreamingChunks = logStreamingChunks;
    }

    /**
     * 钩子优先级：900（低优先级）
     * 确保在业务逻辑之后执行，专注于日志记录
     */
    @Override
    public int priority() {
        return 900;
    }

    /**
     * 统一事件处理入口
     * 使用 pattern matching 分发到具体处理器
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return switch (event) {
            case PreCallEvent e -> handlePreCall(e);
            case PostCallEvent e -> handlePostCall(e);
            case PreReasoningEvent e -> handlePreReasoning(e);
            case PostReasoningEvent e -> handlePostReasoning(e);
            case ReasoningChunkEvent e when logStreamingChunks -> handleReasoningChunk(e);
            case PreActingEvent e -> handlePreActing(e);
            case PostActingEvent e -> handlePostActing(e);
            case ActingChunkEvent e when logStreamingChunks -> handleActingChunk(e);
            case PreSummaryEvent e -> handlePreSummary(e);
            case PostSummaryEvent e -> handlePostSummary(e);
            case ErrorEvent e -> handleError(e);
            default -> Mono.just(event);
        };
    }

    /**
     * 处理 Agent 调用开始事件
     */
    private <T extends HookEvent> Mono<T> handlePreCall(PreCallEvent e) {
        List<Msg> messages = e.getInputMessages();
        log.info("[{}] Agent 调用开始 | 输入消息数: {}",
                e.getAgent().getName(),
                messages.size());

        // 记录前 3 条消息的摘要
        for (int i = 0; i < Math.min(3, messages.size()); i++) {
            Msg msg = messages.get(i);
            String preview = truncate(msg.getTextContent(), 50);
            log.debug("  消息[{}]: {} - {}",
                    i, msg.getRole(), preview);
        }

        return Mono.just(event);
    }

    /**
     * 处理 Agent 调用完成事件
     */
    private <T extends HookEvent> Mono<T> handlePostCall(PostCallEvent e) {
        Msg finalMsg = e.getFinalMessage();
        String content = finalMsg != null ? finalMsg.getTextContent() : "";
        log.info("[{}] Agent 调用完成 | 响应长度: {} 字符",
                e.getAgent().getName(),
                content.length());

        return Mono.just(event);
    }

    /**
     * 处理推理开始事件
     */
    private <T extends HookEvent> Mono<T> handlePreReasoning(PreReasoningEvent e) {
        log.debug("[{}] 推理开始 | 模型: {} | 输入消息数: {}",
                e.getAgent().getName(),
                e.getModelName(),
                e.getInputMessages().size());

        return Mono.just(event);
    }

    /**
     * 处理推理完成事件
     */
    private <T extends HookEvent> Mono<T> handlePostReasoning(PostReasoningEvent e) {
        Msg reasoningMsg = e.getReasoningMessage();

        if (reasoningMsg != null) {
            List<ToolUseBlock> toolCalls = reasoningMsg.getContentBlocks(ToolUseBlock.class);
            String reasoningPreview = truncate(reasoningMsg.getTextContent(), 100);

            log.info("[{}] 推理完成 | 推理内容预览: {}... | ToolCalls: {}",
                    e.getAgent().getName(),
                    reasoningPreview,
                    toolCalls.size());

            // 记录每个 ToolCall 的详情
            for (ToolUseBlock tool : toolCalls) {
                log.debug("  ToolCall: name={}, input={}",
                        tool.getName(),
                        truncate(tool.getInput().toString(), 80));
            }

            // 检查是否请求停止
            if (e.isStopRequested()) {
                log.info("[{}] 推理阶段请求停止 Agent", e.getAgent().getName());
            }

            // 检查是否请求返回推理
            if (e.isGotoReasoningRequested()) {
                log.info("[{}] 推理阶段请求返回推理", e.getAgent().getName());
            }
        } else {
            log.info("[{}] 推理完成 | 无推理消息（直接返回）",
                    e.getAgent().getName());
        }

        return Mono.just(event);
    }

    /**
     * 处理推理流式输出事件
     */
    private <T extends HookEvent> Mono<T> handleReasoningChunk(ReasoningChunkEvent e) {
        Msg chunk = e.getIncrementalChunk();
        String text = chunk != null ? chunk.getTextContent() : "";

        log.trace("[{}] 推理 Chunk | 内容: {}",
                e.getAgent().getName(),
                truncate(text, 200));

        return Mono.just(event);
    }

    /**
     * 处理工具执行开始事件
     */
    private <T extends HookEvent> Mono<T> handlePreActing(PreActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();

        log.info("[{}] 工具执行开始 | 工具: {} | 输入: {}",
                e.getAgent().getName(),
                toolUse.getName(),
                truncate(toolUse.getInput().toString(), 100));

        return Mono.just(event);
    }

    /**
     * 处理工具执行完成事件
     */
    private <T extends HookEvent> Mono<T> handlePostActing(PostActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();
        ToolResultBlock result = e.getToolResult();

        String output = extractResultText(result);
        log.info("[{}] 工具执行完成 | 工具: {} | 结果: {}",
                e.getAgent().getName(),
                toolUse.getName(),
                truncate(output, 150));

        // 检查是否请求停止
        if (e.isStopRequested()) {
            log.info("[{}] 工具执行后请求停止 Agent", e.getAgent().getName());
        }

        return Mono.just(event);
    }

    /**
     * 处理工具流式输出事件
     */
    private <T extends HookEvent> Mono<T> handleActingChunk(ActingChunkEvent e) {
        ToolResultBlock chunk = e.getChunk();
        String text = extractResultText(chunk);

        log.trace("[{}] 工具 Chunk | 工具: {} | 内容: {}",
                e.getAgent().getName(),
                e.getToolUse().getName(),
                truncate(text, 100));

        return Mono.just(event);
    }

    /**
     * 处理摘要生成开始事件
     */
    private <T extends HookEvent> Mono<T> handlePreSummary(PreSummaryEvent e) {
        log.info("[{}] 摘要生成开始 | 迭代次数: {}/{} | 模型: {}",
                e.getAgent().getName(),
                e.getCurrentIteration(),
                e.getMaxIterations(),
                e.getModelName());

        return Mono.just(event);
    }

    /**
     * 处理摘要生成完成事件
     */
    private <T extends HookEvent> Mono<T> handlePostSummary(PostSummaryEvent e) {
        Msg summaryMsg = e.getSummaryMessage();
        String content = summaryMsg != null ? summaryMsg.getTextContent() : "";

        log.info("[{}] 摘要生成完成 | 摘要长度: {} 字符",
                e.getAgent().getName(),
                content.length());

        return Mono.just(event);
    }

    /**
     * 处理错误事件
     */
    private <T extends HookEvent> Mono<T> handleError(ErrorEvent e) {
        Throwable error = e.getError();

        log.error("[{}] 执行出错 | 类型: {} | 消息: {}",
                e.getAgent().getName(),
                error.getClass().getSimpleName(),
                error.getMessage());

        // 记录堆栈跟踪的前几行
        StackTraceElement[] stackTrace = error.getStackTrace();
        if (stackTrace.length > 0) {
            log.error("  发生在: {}:{}",
                    stackTrace[0].getFileName(),
                    stackTrace[0].getLineNumber());
        }

        return Mono.just(event);
    }

    /**
     * 从 ToolResultBlock 提取文本结果
     */
    private String extractResultText(ToolResultBlock result) {
        if (result == null || result.getOutput().isEmpty()) {
            return "(empty)";
        }
        return result.getOutput().get(0).toString();
    }

    /**
     * 截断过长文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "(null)";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
```

**hook/MetricsHook.java - 自定义监控钩子：**

```java
package io.agentscope.tutorial.chapter13.hook;

import io.agentscope.core.hook.*;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义监控钩子 - 收集 Agent 执行性能指标
 *
 * 收集的指标：
 * 1. 调用次数和总耗时
 * 2. 推理次数和耗时
 * 3. 工具调用统计（按工具名称分类）
 * 4. 错误统计
 * 5. 推理/摘要切换次数
 *
 * @author AgentScope Tutorial
 */
public class MetricsHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(MetricsHook.class);

    /** 调用计数 */
    private final AtomicInteger callCount = new AtomicInteger(0);

    /** 总调用耗时（毫秒） */
    private final AtomicLong totalCallTimeMs = new AtomicLong(0);

    /** 推理次数 */
    private final AtomicInteger reasoningCount = new AtomicInteger(0);

    /** 工具调用统计 */
    private final ConcurrentMap<String, AtomicInteger> toolCallCounts = new ConcurrentHashMap<>();

    /** 错误计数 */
    private final AtomicInteger errorCount = new AtomicInteger(0);

    /** 当前调用的开始时间 */
    private Instant currentCallStart;

    /** 当前调用的推理开始时间 */
    private Instant currentReasoningStart;

    /** 当前调用内的迭代计数 */
    private int currentIterationCount = 0;

    public MetricsHook() {
        log.info("MetricsHook initialized");
    }

    /**
     * 钩子优先级：950（最低优先级）
     * 确保在所有业务逻辑之后执行，最后进行指标收集
     */
    @Override
    public int priority() {
        return 950;
    }

    /**
     * 统一事件处理入口
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return switch (event) {
            case PreCallEvent e -> handlePreCall(e);
            case PostCallEvent e -> handlePostCall(e);
            case PreReasoningEvent e -> handlePreReasoning(e);
            case PostReasoningEvent e -> handlePostReasoning(e);
            case PreActingEvent e -> handlePreActing(e);
            case ErrorEvent e -> handleError(e);
            default -> Mono.just(event);
        };
    }

    /**
     * 处理 Agent 调用开始
     */
    private <T extends HookEvent> Mono<T> handlePreCall(PreCallEvent e) {
        currentCallStart = Instant.now();
        currentIterationCount = 0;
        callCount.incrementAndGet();

        log.debug("[{}] 指标收集 - Agent 调用开始 #{}",
                e.getAgent().getName(),
                callCount.get());

        return Mono.just(event);
    }

    /**
     * 处理 Agent 调用完成
     */
    private <T extends HookEvent> Mono<T> handlePostCall(PostCallEvent e) {
        if (currentCallStart != null) {
            long durationMs = Duration.between(currentCallStart, Instant.now()).toMillis();
            totalCallTimeMs.addAndGet(durationMs);

            log.info("=== Agent 调用指标 #{} ===", callCount.get());
            log.info("  总耗时: {} ms", durationMs);
            log.info("  迭代次数: {}", currentIterationCount);
            log.info("  推理次数: {}", reasoningCount.get());
            log.info("  错误数: {}", errorCount.get());

            // 输出工具调用统计
            if (!toolCallCounts.isEmpty()) {
                log.info("  工具调用统计:");
                toolCallCounts.forEach((tool, count) ->
                    log.info("    - {}: {} 次", tool, count.get()));
            }

            log.info("  平均耗时: {} ms", getAverageCallTimeMs());
            log.info("===================================");

            // 重置状态
            currentCallStart = null;
            reasoningCount.set(0);
            toolCallCounts.clear();
            errorCount.set(0);
        }

        return Mono.just(event);
    }

    /**
     * 处理推理开始
     */
    private <T extends HookEvent> Mono<T> handlePreReasoning(PreReasoningEvent e) {
        currentReasoningStart = Instant.now();
        currentIterationCount++;
        reasoningCount.incrementAndGet();

        log.debug("[{}] 指标收集 - 推理开始 #{} (迭代 #{})",
                e.getAgent().getName(),
                reasoningCount.get(),
                currentIterationCount);

        return Mono.just(event);
    }

    /**
     * 处理推理完成
     */
    private <T extends HookEvent> Mono<T> handlePostReasoning(PostReasoningEvent e) {
        if (currentReasoningStart != null) {
            long durationMs = Duration.between(currentReasoningStart, Instant.now()).toMillis();

            // 统计 ToolCalls
            if (e.getReasoningMessage() != null) {
                List<ToolUseBlock> toolCalls =
                    e.getReasoningMessage().getContentBlocks(ToolUseBlock.class);
                for (ToolUseBlock tool : toolCalls) {
                    toolCallCounts.computeIfAbsent(tool.getName(), k -> new AtomicInteger())
                            .incrementAndGet();
                }
            }

            log.debug("[{}] 指标收集 - 推理完成 #{}, 耗时: {} ms",
                    e.getAgent().getName(),
                    reasoningCount.get(),
                    durationMs);

            currentReasoningStart = null;
        }

        return Mono.just(event);
    }

    /**
     * 处理工具执行开始
     */
    private <T extends HookEvent> Mono<T> handlePreActing(PreActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();

        log.debug("[{}] 指标收集 - 工具执行: {}",
                e.getAgent().getName(),
                toolUse.getName());

        return Mono.just(event);
    }

    /**
     * 处理错误
     */
    private <T extends HookEvent> Mono<T> handleError(ErrorEvent e) {
        errorCount.incrementAndGet();

        log.warn("[{}] 指标收集 - 执行错误 #{}: {}",
                e.getAgent().getName(),
                errorCount.get(),
                e.getError().getMessage());

        return Mono.just(event);
    }

    /**
     * 获取平均调用耗时（毫秒）
     */
    public long getAverageCallTimeMs() {
        int count = callCount.get();
        if (count == 0) return 0;
        return totalCallTimeMs.get() / count;
    }

    /**
     * 获取总调用次数
     */
    public int getTotalCallCount() {
        return callCount.get();
    }

    /**
     * 获取总调用耗时（毫秒）
     */
    public long getTotalCallTimeMs() {
        return totalCallTimeMs.get();
    }

    /**
     * 获取错误总数
     */
    public int getTotalErrorCount() {
        return errorCount.get();
    }

    /**
     * 获取工具调用统计
     */
    public Map<String, Integer> getToolCallStats() {
        return Map.copyOf(toolCallCounts.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                )));
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        callCount.set(0);
        totalCallTimeMs.set(0);
        reasoningCount.set(0);
        toolCallCounts.clear();
        errorCount.set(0);
        currentCallStart = null;
        currentReasoningStart = null;
        currentIterationCount = 0;

        log.info("指标已重置");
    }

    @Override
    public String toString() {
        return String.format(
                "MetricsHook{totalCalls=%d, avgTime=%dms, errors=%d, tools=%s}",
                callCount.get(),
                getAverageCallTimeMs(),
                errorCount.get(),
                toolCallCounts);
    }
}
```

**hook/ToolAuditHook.java - 工具审计钩子：**

```java
package io.agentscope.tutorial.chapter13.hook;

import io.agentscope.core.hook.*;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 工具审计钩子 - 记录所有工具调用的详细信息
 *
 * 功能特点：
 * 1. 高优先级执行，确保尽早拦截工具调用
 * 2. 记录完整的工具调用上下文（参数、结果、时间）
 * 3. 支持指定敏感工具进行额外关注
 * 4. 可配置阻止特定工具的执行
 * 5. 提供审计历史查询接口
 *
 * @author AgentScope Tutorial
 */
public class ToolAuditHook implements Hook {

    private static final Logger auditLog = LoggerFactory.getLogger("AuditLog");

    /** 需要特别关注的敏感工具集合 */
    private final Set<String> sensitiveTools;

    /** 工具调用审计记录（线程安全） */
    private final List<ToolAuditRecord> auditRecords = new CopyOnWriteArrayList<>();

    /** 当前调用的审计记录列表 */
    private final ThreadLocal<List<ToolAuditRecord>> currentCallRecords = ThreadLocal.withInitial(ArrayList::new);

    /** 是否启用严格模式（阻止未授权工具） */
    private final boolean strictMode;

    /** 允许的工具集合（空表示全部允许） */
    private final Set<String> allowedTools;

    /** 日期时间格式化器 */
    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * 内部类：工具审计记录
     */
    public record ToolAuditRecord(
            String agentName,
            String toolName,
            String input,
            String output,
            long startTime,
            long durationMs,
            boolean sensitive,
            boolean blocked
    ) {
        public String getStartTimeFormatted() {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(startTime),
                    java.time.ZoneId.systemDefault()
            ).format(DT_FORMATTER);
        }
    }

    /**
     * 默认构造函数 - 记录所有工具调用
     */
    public ToolAuditHook() {
        this(Set.of(), false, Set.of());
    }

    /**
     * 指定敏感工具的构造函数
     *
     * @param sensitiveTools 需要特别关注的工具集合
     */
    public ToolAuditHook(Set<String> sensitiveTools) {
        this(sensitiveTools, false, Set.of());
    }

    /**
     * 完整配置构造函数
     *
     * @param sensitiveTools 需要特别关注的工具集合
     * @param strictMode 是否启用严格模式
     * @param allowedTools 允许的工具集合（空表示全部允许）
     */
    public ToolAuditHook(Set<String> sensitiveTools, boolean strictMode, Set<String> allowedTools) {
        this.sensitiveTools = Set.copyOf(sensitiveTools);
        this.strictMode = strictMode;
        this.allowedTools = Set.copyOf(allowedTools);
    }

    /**
     * 钩子优先级：60（高优先级）
     * 确保在工具执行前尽早进行审计检查
     */
    @Override
    public int priority() {
        return 60;
    }

    /**
     * 统一事件处理入口
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return switch (event) {
            case PreCallEvent e -> handlePreCall(e);
            case PreActingEvent e -> handlePreActing(e);
            case PostActingEvent e -> handlePostActing(e);
            case PostCallEvent e -> handlePostCall(e);
            default -> Mono.just(event);
        };
    }

    /**
     * 处理 Agent 调用开始
     */
    private <T extends HookEvent> Mono<T> handlePreCall(PreCallEvent e) {
        // 清空当前线程的记录列表
        currentCallRecords.get().clear();

        auditLog.info("[AUDIT] Agent '{}' 调用开始", e.getAgent().getName());

        return Mono.just(event);
    }

    /**
     * 处理工具执行开始
     * 进行安全检查和权限验证
     */
    private <T extends HookEvent> Mono<T> handlePreActing(PreActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();
        String toolName = toolUse.getName();
        boolean isSensitive = sensitiveTools.contains(toolName);

        // 权限检查
        boolean isAllowed = allowedTools.isEmpty() || allowedTools.contains(toolName);

        if (!isAllowed) {
            auditLog.warn("[AUDIT] 工具 '{}' 未授权 - Agent: '{}'",
                    toolName, e.getAgent().getName());

            if (strictMode) {
                auditLog.error("[AUDIT] 严格模式：阻止未授权工具 '{}'", toolName);
                // 注意：实际阻止需要更复杂的机制，这里仅记录
            }
        }

        // 敏感工具标记
        if (isSensitive) {
            auditLog.warn("[AUDIT] 敏感工具 '{}' 即将执行 - Agent: '{}', 输入: {}",
                    toolName,
                    e.getAgent().getName(),
                    truncate(toolUse.getInput().toString(), 200));
        } else {
            auditLog.info("[AUDIT] 工具 '{}' 即将执行 - Agent: '{}'",
                    toolName, e.getAgent().getName());
        }

        // 创建审计记录
        ToolAuditRecord record = new ToolAuditRecord(
                e.getAgent().getName(),
                toolName,
                toolUse.getInput().toString(),
                null,  // 结果尚未生成
                System.currentTimeMillis(),
                0,     // 耗时尚未计算
                isSensitive,
                !isAllowed
        );
        currentCallRecords.get().add(record);

        return Mono.just(event);
    }

    /**
     * 处理工具执行完成
     * 记录完整的执行结果
     */
    private <T extends HookEvent> Mono<T> handlePostActing(PostActingEvent e) {
        ToolUseBlock toolUse = e.getToolUse();
        ToolResultBlock result = e.getToolResult();
        String output = extractResultText(result);

        // 更新审计记录
        List<ToolAuditRecord> records = currentCallRecords.get();
        if (!records.isEmpty()) {
            // 找到对应的记录（按工具名称匹配最后一个）
            ToolAuditRecord existingRecord = null;
            for (int i = records.size() - 1; i >= 0; i--) {
                if (records.get(i).toolName().equals(toolUse.getName())) {
                    existingRecord = records.get(i);
                    break;
                }
            }

            if (existingRecord != null) {
                long duration = System.currentTimeMillis() - existingRecord.startTime();

                // 创建更新后的记录
                ToolAuditRecord updatedRecord = new ToolAuditRecord(
                        existingRecord.agentName(),
                        existingRecord.toolName(),
                        existingRecord.input(),
                        output,
                        existingRecord.startTime(),
                        duration,
                        existingRecord.sensitive(),
                        existingRecord.blocked()
                );

                // 移除旧记录，添加新记录
                records.remove(existingRecord);
                records.add(updatedRecord);

                // 输出审计日志
                if (existingRecord.sensitive()) {
                    auditLog.warn("[AUDIT] 敏感工具 '{}' 执行完成 | 耗时: {}ms | 结果: {}",
                            toolUse.getName(),
                            duration,
                            truncate(output, 200));
                } else {
                    auditLog.info("[AUDIT] 工具 '{}' 执行完成 | 耗时: {}ms",
                            toolUse.getName(),
                            duration);
                }
            }
        }

        return Mono.just(event);
    }

    /**
     * 处理 Agent 调用完成
     * 保存本次调用的所有审计记录
     */
    private <T extends HookEvent> Mono<T> handlePostCall(PostCallEvent e) {
        List<ToolAuditRecord> records = currentCallRecords.get();

        if (!records.isEmpty()) {
            auditLog.info("[AUDIT] Agent '{}' 调用完成 | 工具调用数: {}",
                    e.getAgent().getName(),
                    records.size());

            // 复制记录到永久存储
            auditRecords.addAll(records);

            // 打印本次调用的工具统计
            long totalDuration = records.stream()
                    .mapToLong(ToolAuditRecord::durationMs)
                    .sum();
            auditLog.info("  总工具调用耗时: {}ms", totalDuration);

            // 检查是否有被阻止的工具
            long blockedCount = records.stream()
                    .filter(ToolAuditRecord::blocked)
                    .count();
            if (blockedCount > 0) {
                auditLog.warn("  被阻止的工具调用数: {}", blockedCount);
            }
        }

        // 清空当前线程记录
        currentCallRecords.remove();

        return Mono.just(event);
    }

    /**
     * 从 ToolResultBlock 提取文本结果
     */
    private String extractResultText(ToolResultBlock result) {
        if (result == null || result.getOutput().isEmpty()) {
            return "(empty)";
        }
        return result.getOutput().get(0).toString();
    }

    /**
     * 截断过长文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "(null)";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * 获取所有审计记录
     */
    public List<ToolAuditRecord> getAuditRecords() {
        return List.copyOf(auditRecords);
    }

    /**
     * 获取指定 Agent 的审计记录
     */
    public List<ToolAuditRecord> getAuditRecordsByAgent(String agentName) {
        return auditRecords.stream()
                .filter(r -> r.agentName().equals(agentName))
                .toList();
    }

    /**
     * 获取指定工具的审计记录
     */
    public List<ToolAuditRecord> getAuditRecordsByTool(String toolName) {
        return auditRecords.stream()
                .filter(r -> r.toolName().equals(toolName))
                .toList();
    }

    /**
     * 获取敏感工具的审计记录
     */
    public List<ToolAuditRecord> getSensitiveToolRecords() {
        return auditRecords.stream()
                .filter(ToolAuditRecord::sensitive)
                .toList();
    }

    /**
     * 清空审计记录
     */
    public void clearRecords() {
        auditRecords.clear();
        auditLog.info("[AUDIT] 审计记录已清空");
    }

    /**
     * 添加敏感工具
     */
    public void addSensitiveTool(String toolName) {
        ((java.util.HashSet<String>) sensitiveTools).add(toolName);
    }

    /**
     * 移除敏感工具
     */
    public void removeSensitiveTool(String toolName) {
        ((java.util.HashSet<String>) sensitiveTools).remove(toolName);
    }
}
```

**service/AgentService.java - Agent 服务层：**

```java
package io.agentscope.tutorial.chapter13.service;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.recorder.JsonlTraceExporter;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.tutorial.chapter13.hook.LoggingHook;
import io.agentscope.tutorial.chapter13.hook.MetricsHook;
import io.agentscope.tutorial.chapter13.hook.ToolAuditHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent 服务层 - 封装 Agent 的调用逻辑
 *
 * 提供：
 * 1. 同步调用接口
 * 2. 异步调用接口（使用 Project Reactor）
 * 3. Agent 状态查询
 * 4. 钩子链管理
 *
 * @author AgentScope Tutorial
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    /** 注入的 Agent 实例 */
    private final ReActAgent agent;

    /** 注入的日志钩子 */
    private final LoggingHook loggingHook;

    /** 注入的监控钩子 */
    private final MetricsHook metricsHook;

    /** 注入的工具审计钩子 */
    private final ToolAuditHook toolAuditHook;

    /** 注入的追踪导出器 */
    private final JsonlTraceExporter traceExporter;

    public AgentService(
            @Qualifier("tutorialAgent") ReActAgent agent,
            LoggingHook loggingHook,
            MetricsHook metricsHook,
            ToolAuditHook toolAuditHook,
            JsonlTraceExporter traceExporter) {
        this.agent = agent;
        this.loggingHook = loggingHook;
        this.metricsHook = metricsHook;
        this.toolAuditHook = toolAuditHook;
        this.traceExporter = traceExporter;
    }

    /**
     * 同步调用 Agent
     *
     * @param userInput 用户输入
     * @return Agent 响应消息
     */
    public Msg call(String userInput) {
        log.info("调用 Agent，输入: {}", truncate(userInput, 100));

        try {
            Msg response = agent.call(
                    Msg.user().content(userInput).build()
            );

            log.info("Agent 响应: {}", truncate(response.getTextContent(), 200));
            return response;

        } catch (Exception e) {
            log.error("Agent 调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("Agent 调用失败", e);
        }
    }

    /**
     * 异步调用 Agent（返回 Mono）
     *
     * @param userInput 用户输入
     * @return 包含响应消息的 Mono
     */
    public reactor.core.publisher.Mono<Msg> callAsync(String userInput) {
        log.info("异步调用 Agent，输入: {}", truncate(userInput, 100));

        return reactor.core.publisher.Mono.fromCallable(() -> {
                    Msg response = agent.call(
                            Msg.user().content(userInput).build()
                    );
                    log.info("异步调用完成: {}", truncate(response.getTextContent(), 200));
                    return response;
                })
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .doOnError(e -> log.error("异步 Agent 调用失败: {}", e.getMessage(), e));
    }

    /**
     * 获取性能指标
     */
    public MetricsHook getMetrics() {
        return metricsHook;
    }

    /**
     * 获取工具审计记录
     */
    public List<ToolAuditHook.ToolAuditRecord> getAuditRecords() {
        return toolAuditHook.getAuditRecords();
    }

    /**
     * 获取工具审计记录（按工具过滤）
     */
    public List<ToolAuditHook.ToolAuditRecord> getAuditRecordsByTool(String toolName) {
        return toolAuditHook.getAuditRecordsByTool(toolName);
    }

    /**
     * 重置性能指标
     */
    public void resetMetrics() {
        metricsHook.reset();
        log.info("性能指标已重置");
    }

    /**
     * 清空审计记录
     */
    public void clearAuditRecords() {
        toolAuditHook.clearRecords();
    }

    /**
     * 获取 Agent 名称
     */
    public String getAgentName() {
        return agent.getName();
    }

    /**
     * 检查 Agent 是否健康
     */
    public boolean isHealthy() {
        try {
            // 简单的健康检查：执行一个空调用
            agent.call(Msg.user().content("health check").build());
            return true;
        } catch (Exception e) {
            log.warn("Agent 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取已注册的钩子列表信息
     */
    public String getHookInfo() {
        return String.format("""
                Hook 链信息:
                - LoggingHook: priority=%d
                - MetricsHook: priority=%d, totalCalls=%d, avgTime=%dms
                - ToolAuditHook: priority=%d, records=%d
                - JsonlTraceExporter: priority=%d
                """,
                loggingHook.priority(),
                metricsHook.priority(),
                metricsHook.getTotalCallCount(),
                metricsHook.getAverageCallTimeMs(),
                toolAuditHook.priority(),
                toolAuditHook.getAuditRecords().size(),
                traceExporter.priority());
    }

    /**
     * 截断过长文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "(null)";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
```

**controller/AgentController.java - REST 控制器：**

```java
package io.agentscope.tutorial.chapter13.controller;

import io.agentscope.core.message.Msg;
import io.agentscope.tutorial.chapter13.hook.MetricsHook;
import io.agentscope.tutorial.chapter13.hook.ToolAuditHook;
import io.agentscope.tutorial.chapter13.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent REST 控制器
 *
 * 提供以下 API 端点：
 * - POST /api/agent/call - 调用 Agent
 * - POST /api/agent/call-async - 异步调用 Agent
 * - GET /api/agent/metrics - 获取性能指标
 * - GET /api/agent/audit - 获取审计记录
 * - GET /api/agent/info - 获取 Agent 信息
 * - POST /api/agent/reset - 重置指标
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 同步调用 Agent
     *
     * @param request 请求体，包含 "input" 字段
     * @return Agent 响应
     */
    @PostMapping("/call")
    public ResponseEntity<Map<String, Object>> callAgent(@RequestBody Map<String, String> request) {
        String input = request.get("input");
        if (input == null || input.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "input is required"));
        }

        try {
            long startTime = System.currentTimeMillis();
            Msg response = agentService.call(input);
            long duration = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "response", response.getTextContent(),
                    "agent", agentService.getAgentName(),
                    "durationMs", duration
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * 异步调用 Agent
     *
     * @param request 请求体，包含 "input" 字段
     * @return 202 Accepted，异步处理中
     */
    @PostMapping("/call-async")
    public ResponseEntity<Map<String, Object>> callAgentAsync(@RequestBody Map<String, String> request) {
        String input = request.get("input");
        if (input == null || input.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "input is required"));
        }

        return ResponseEntity.accepted()
                .body(Map.of(
                        "message", "异步调用已接受",
                        "input", truncate(input, 100)
                ));
    }

    /**
     * 获取性能指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        MetricsHook metrics = agentService.getMetrics();

        return ResponseEntity.ok(Map.of(
                "totalCalls", metrics.getTotalCallCount(),
                "totalTimeMs", metrics.getTotalCallTimeMs(),
                "averageTimeMs", metrics.getAverageCallTimeMs(),
                "totalErrors", metrics.getTotalErrorCount(),
                "toolStats", metrics.getToolCallStats()
        ));
    }

    /**
     * 获取工具审计记录
     */
    @GetMapping("/audit")
    public ResponseEntity<Map<String, Object>> getAuditRecords(
            @RequestParam(required = false) String tool) {

        List<ToolAuditHook.ToolAuditRecord> records;
        if (tool != null && !tool.isBlank()) {
            records = agentService.getAuditRecordsByTool(tool);
        } else {
            records = agentService.getAuditRecords();
        }

        return ResponseEntity.ok(Map.of(
                "count", records.size(),
                "records", records.stream()
                        .map(r -> Map.of(
                                "agent", r.agentName(),
                                "tool", r.toolName(),
                                "sensitive", r.sensitive(),
                                "blocked", r.blocked(),
                                "durationMs", r.durationMs(),
                                "startTime", r.getStartTimeFormatted()
                        ))
                        .toList()
        ));
    }

    /**
     * 获取 Agent 信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getAgentInfo() {
        return ResponseEntity.ok(Map.of(
                "name", agentService.getAgentName(),
                "healthy", agentService.isHealthy(),
                "hookInfo", agentService.getHookInfo()
        ));
    }

    /**
     * 重置性能指标
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetMetrics() {
        agentService.resetMetrics();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "指标已重置"
        ));
    }

    /**
     * 清空审计记录
     */
    @DeleteMapping("/audit")
    public ResponseEntity<Map<String, Object>> clearAuditRecords() {
        agentService.clearAuditRecords();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "审计记录已清空"
        ));
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean healthy = agentService.isHealthy();
        return ResponseEntity.ok(Map.of(
                "status", healthy ? "UP" : "DOWN",
                "agent", agentService.getAgentName()
        ));
    }

    /**
     * 截断过长文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
```

### 13.4.4 运行测试

**启动应用：**

```bash
# 设置 API Key
export DASHSCOPE_API_KEY=your-api-key

# 启动 Spring Boot 应用
cd chapter13-hook
mvn spring-boot:run
```

**调用示例：**

```bash
# 调用 Agent
curl -X POST http://localhost:8080/api/agent/call \
  -H "Content-Type: application/json" \
  -d '{"input": "Calculate 123 + 456"}'

# 查看性能指标
curl http://localhost:8080/api/agent/metrics

# 查看工具审计记录
curl http://localhost:8080/api/agent/audit

# 查看 Agent 信息
curl http://localhost:8080/api/agent/info

# 健康检查
curl http://localhost:8080/api/agent/health

# 重置指标
curl -X POST http://localhost:8080/api/agent/reset
```

**预期输出（日志）：**

```
2026-05-17 10:30:00.001 [http-nio-8080-exec-1] INFO  AgentService - 调用 Agent，输入: Calculate 123 + 456

2026-05-17 10:30:00.050 [boundedElastic-1] INFO  LoggingHook - [TutorialAgent] Agent 调用开始 | 输入消息数: 1

2026-05-17 10:30:00.100 [boundedElastic-1] INFO  AuditLog - [AUDIT] Agent 'TutorialAgent' 调用开始

2026-05-17 10:30:00.150 [boundedElastic-1] INFO  AuditLog - [AUDIT] 工具 'calculate' 即将执行 - Agent: 'TutorialAgent'

2026-05-17 10:30:00.200 [boundedElastic-1] INFO  LoggingHook - [TutorialAgent] 工具执行开始 | 工具: calculate | 输入: {expression=123 + 456}

2026-05-17 10:30:00.250 [boundedElastic-1] INFO  LoggingHook - [TutorialAgent] 工具执行完成 | 工具: calculate | 结果: 579.00

2026-05-17 10:30:00.300 [boundedElastic-1] INFO  AuditLog - [AUDIT] 工具 'calculate' 执行完成 | 耗时: 50ms

2026-05-17 10:30:00.350 [boundedElastic-1] INFO  LoggingHook - [TutorialAgent] Agent 调用完成 | 响应长度: 85 字符

2026-05-17 10:30:00.400 [boundedElastic-1] INFO  MetricsHook - === Agent 调用指标 #1 ===
2026-05-17 10:30:00.400 [boundedElastic-1] INFO  MetricsHook -   总耗时: 350 ms
2026-05-17 10:30:00.400 [boundedElastic-1] INFO  MetricsHook -   迭代次数: 2
2026-05-17 10:30:00.400 [boundedElastic-1] INFO  MetricsHook -   工具调用统计:
2026-05-17 10:30:00.400 [boundedElastic-1] INFO  MetricsHook -     - calculate: 1 次
2026-05-17 10:30:00.400 [boundedElastic-1] INFO  MetricsHook -   平均耗时: 350 ms
2026-05-17 10:30:00.400 [boundedElastic-1] INFO  MetricsHook - ==================================

2026-05-17 10:30:00.450 [boundedElastic-1] INFO  AgentService - Agent 响应: 计算结果为 579.00
```

## 13.5 本章小结

### 核心要点

1. **Hook 是 AgentScope Java 的核心扩展机制**
   - 通过统一的事件模型拦截 Agent 执行的全生命周期
   - 支持同步修改（PreCall、PreReasoning、PreActing 等）和中止控制（PostReasoning.stopAgent()）

2. **事件类型体系清晰**
   - `HookEvent` 是所有事件的基类，采用 sealed class 设计
   - 12 种事件类型覆盖推理、工具执行、摘要、错误等场景
   - 部分事件可修改（带 setter），部分仅通知（只读）

3. **优先级决定执行顺序**
   - 数值越小优先级越高，范围 0-1000
   - 建议分层设计：系统关键(0-50) → 业务逻辑(51-500) → 监控日志(501-1000)

4. **内置 Hook 开箱即用**
   - `JsonlTraceExporter` 提供完整的调用追踪
   - 复杂场景可组合多个内置 Hook

5. **自定义 Hook 简单灵活**
   - 实现 `Hook` 接口，重写 `onEvent` 方法
   - 使用 pattern matching 分发事件处理
   - 返回 `Mono.just(event)` 保持事件链继续

### 实践建议

- **生产环境**：组合使用 LoggingHook + MetricsHook + JsonlTraceExporter
- **安全敏感场景**：使用 ToolAuditHook 进行权限控制和审计
- **人工介入场景**：在 PostReasoningEvent 或 PostActingEvent 中调用 `stopAgent()`
- **性能优化**：避免在 Hook 中执行耗时操作，使用异步队列处理日志和指标

### 后续章节预告

- **第十四章：Memory 记忆系统** - 深入学习 AgentScope Java 的记忆管理机制
- **第十五章：Tool 工具开发** - 掌握自定义工具的开发模式和最佳实践
- **第十六章：Multi-Agent 编排** - 学习多 Agent 协作的通信与协调机制