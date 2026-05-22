# 第四章：ReAct Agent 详解

ReAct（Reasoning and Acting）是一种将推理与行动相结合的 Agent 设计范式。在 AgentScope Java 框架中，ReActAgent 是核心实现，它通过迭代式的 Thought-Action-Observation 循环，使 AI Agent 能够像人类一样先思考再行动。本章将深入解析 ReAct 模式的原理、ReActAgent 的配置与生命周期、状态管理机制，以及工具调用系统。

---

## 4.1 ReAct 模式原理与执行流程

### 4.1.1 什么是 ReAct

ReAct 模式由 Google Research 在 2022 年提出，其核心思想是将大型语言模型（LLM）的推理能力与外部工具调用能力结合起来。在传统的 LLM 调用中，模型只能基于训练知识生成文本；而 ReAct 模式让模型能够：

1. **推理（Reasoning）**：分析当前状态，决定下一步行动
2. **行动（Acting）**：调用外部工具获取信息或执行操作
3. **观察（Observation）**：获取行动结果，纳入后续推理

### 4.1.2 Thought-Action-Observation 循环

ReAct Agent 的执行遵循以下循环流程：

```
┌─────────────────────────────────────────────────────────────────┐
│                        ReAct 执行流程                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐     ┌─────────┐ │
│  │  用户输入  │ ──▶ │ Reasoning │ ──▶ │  Acting  │ ──▶ │观察结果 │ │
│  └──────────┘     └──────────┘     └──────────┘     └────┬────┘ │
│                                                            │     │
│                     ◀─────── 循环继续 ◀─────────────── ◀───┘     │
│                                                                  │
│  当达到最大迭代次数 或 模型决定不调用工具 时循环结束              │
└─────────────────────────────────────────────────────────────────┘
```

### 4.1.3 各阶段详细说明

**Reasoning 阶段（推理阶段）**
- Agent 向 LLM 发送包含对话历史的请求
- LLM 分析上下文，决定是否需要调用工具
- 如果需要调用工具，生成 `ToolUseBlock` 描述工具调用请求
- 如果不需要调用工具，返回最终回答文本

**Acting 阶段（行动阶段）**
- 从推理结果中提取工具调用请求
- 在 Toolkit 中查找对应的工具实现
- 执行工具并获取结果，生成 `ToolResultBlock`
- 将结果反馈给 LLM 进行下一轮推理

**Observation 阶段（观察阶段）**
- 工具执行结果作为新的上下文
- Agent 进入下一轮推理循环
- 持续迭代直到任务完成或达到最大迭代次数

### 4.1.4 ReAct 循环的关键特性

```java
// ReActAgent 核心循环伪代码
public Mono<Msg> executeReActLoop() {
    int iteration = 0;
    while (iteration < maxIters) {
        // 1. Reasoning: 让模型推理并决定是否调用工具
        Msg reasoningResult = model.reasoning(memory.getMessages());
        
        // 2. 检查是否完成（没有工具调用）
        if (!reasoningResult.hasToolCalls()) {
            return Mono.just(reasoningResult); // 任务完成
        }
        
        // 3. Acting: 执行工具调用
        for (ToolUse toolCall : reasoningResult.getToolCalls()) {
            ToolResult result = toolkit.execute(toolCall);
            memory.addMessage(result);
        }
        
        // 4. Observation: 继续下一轮循环
        iteration++;
    }
    
    // 达到最大迭代次数，生成摘要
    return summarize();
}
```

### 4.1.5 与传统方法的对比

| 特性 | 纯推理方法 | ReAct 方法 |
|------|-----------|-----------|
| 知识时效性 | 受限于训练数据 | 可实时获取最新信息 |
| 外部交互 | 无 | 通过工具调用实现 |
| 执行确定性 | 高 | 中等（依赖模型决策） |
| 适用场景 | 通用对话、创作 | 工具调用、信息检索 |
| 错误恢复 | 无 | 工具返回错误供模型分析 |

---

## 4.2 ReActAgent 配置与生命周期

### 4.2.1 ReActAgent 核心配置

ReActAgent 通过 Builder 模式进行配置，主要配置项如下：

```java
// ReActAgent 核心配置示例
ReActAgent agent = ReActAgent.builder()
    .name("助手")                                    // Agent 名称
    .description("一个有帮助的 AI 助手")             // Agent 描述
    .sysPrompt("你是一个乐于助人的 AI 助手。")       // 系统提示词
    .model(dashScopeModel)                           // LLM 模型（必需）
    .toolkit(toolkit)                                // 工具包
    .memory(new InMemoryMemory())                    // 记忆存储
    .maxIters(10)                                    // 最大迭代次数
    .build();
```

### 4.2.2 配置参数详解

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `name` | String | - | Agent 名称，用于标识和日志 |
| `description` | String | null | Agent 描述信息 |
| `sysPrompt` | String | null | 系统提示词，定义 Agent 行为 |
| `model` | Model | - | LLM 模型（必需参数） |
| `toolkit` | Toolkit | new Toolkit() | 工具包，包含可用工具 |
| `memory` | Memory | InMemoryMemory | 对话记忆存储 |
| `maxIters` | int | 10 | 最大推理-行动循环次数 |
| `hooks` | List\<Hook\> | empty | 生命周期钩子列表 |
| `generateOptions` | GenerateOptions | null | 模型生成选项 |

### 4.2.3 Agent 生命周期

ReActAgent 的完整生命周期如下：

```
┌────────────────────────────────────────────────────────────────────────┐
│                         ReActAgent 生命周期                            │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────┐                                                           │
│  │  创建   │  ReActAgent.builder().build()                              │
│  └────┬────┘                                                           │
│       ▼                                                                │
│  ┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐          │
│  │  空闲   │ ──▶ │  运行中  │ ──▶ │  等待中  │ ──▶ │ 已终止   │          │
│  │ (idle)  │     │(running)│     │(waiting)│     │(terminated)│       │
│  └────┬────┘     └────┬────┘     └────┬────┘     └─────────┘          │
│       │               │               │                                │
│       │               ▼               │                                │
│       │         ┌───────────┐         │                                │
│       │         │ 检查中断   │ ◀───────┘                                │
│       │         └───────────┘                                          │
│       │               │                                                │
│       ▼               ▼                                                │
│  ┌─────────┐     ┌─────────┐                                          │
│  │  响应   │     │ 返回中断 │                                          │
│  │  请求   │     │  响应    │                                          │
│  └─────────┘     └─────────┘                                          │
└────────────────────────────────────────────────────────────────────────┘
```

**各状态说明：**

- **idle（空闲）**：Agent 创建后的初始状态，可以接受新的请求
- **running（运行中）**：正在执行推理-行动循环，处理请求
- **waiting（等待中）**：在工具执行期间等待结果，或等待用户输入（Human-in-the-Loop）
- **terminated（已终止）**：Agent 执行完成或被中断

### 4.2.4 生命周期状态码

```java
// 状态常量定义
public class AgentConstants {
    // Agent 运行状态
    public enum AgentState {
        IDLE,        // 空闲状态，等待请求
        RUNNING,    // 运行中，正在处理请求
        WAITING,    // 等待状态（工具执行中或等待用户输入）
        TERMINATED  // 已终止，执行完成或被中断
    }
    
    // 生成原因
    public enum GenerateReason {
        NORMAL,              // 正常完成
        MAX_ITERATIONS,      // 达到最大迭代次数
        TOOL_SUSPENDED,      // 工具挂起（等待人工介入）
        REASONING_STOP_REQUESTED,  // 推理阶段被停止
        ACTING_STOP_REQUESTED     // 行动阶段被停止
    }
}
```

---

## 4.3 Agent 状态管理

### 4.3.1 状态查看与监控

ReActAgent 提供了多种状态查看方法：

```java
// 状态监控示例
public void monitorAgentState(ReActAgent agent) {
    // 获取 Agent 基本信息
    System.out.println("Agent ID: " + agent.getAgentId());
    System.out.println("Agent 名称: " + agent.getName());
    System.out.println("Agent 描述: " + agent.getDescription());
    
    // 获取配置信息
    System.out.println("最大迭代次数: " + agent.getMaxIters());
    System.out.println("系统提示词: " + agent.getSysPrompt());
    
    // 获取记忆内容
    Memory memory = agent.getMemory();
    System.out.println("记忆消息数: " + memory.getMessages().size());
}
```

### 4.3.2 中断机制

ReActAgent 支持 cooperative interrupt（协作式中断），允许外部代码在合适的时机终止 Agent 执行：

```java
// 中断机制示例
public class InterruptExample {
    
    private final ReActAgent agent;
    
    // 外部线程可以请求中断
    public void requestInterrupt() {
        System.out.println("请求中断 Agent 执行...");
        agent.interrupt();  // 设置中断标志
    }
    
    // 带用户消息的中断
    public void requestInterruptWithMessage(Msg userMessage) {
        System.out.println("请求中断并传递用户消息...");
        agent.interrupt(userMessage);
    }
    
    // 在执行前设置中断源
    public void requestSystemInterrupt() {
        agent.interrupt(InterruptSource.SYSTEM);
    }
}
```

### 4.3.3 通过 Hook 监听状态变化

使用 Hook 系统可以监听 Agent 的各种执行事件：

```java
// Agent 状态监听 Hook 示例
public class AgentStateListener implements Hook {
    
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return switch (event) {
            case PreCallEvent e -> {
                System.out.println("[状态变化] Agent 即将开始执行");
                System.out.println("输入消息数: " + e.getInputMessages().size());
                yield Mono.just(e);
            }
            
            case PreReasoningEvent e -> {
                System.out.println("[状态变化] 进入推理阶段");
                System.out.println("使用的模型: " + e.getModelName());
                yield Mono.just(e);
            }
            
            case PostReasoningEvent e -> {
                System.out.println("[状态变化] 推理阶段完成");
                Msg msg = e.getReasoningMessage();
                if (msg != null) {
                    System.out.println("推理结果: " + msg.getContent());
                }
                yield Mono.just(e);
            }
            
            case PreActingEvent e -> {
                System.out.println("[状态变化] 进入行动阶段");
                System.out.println("准备执行工具: " + e.getToolUse().getName());
                yield Mono.just(e);
            }
            
            case PostActingEvent e -> {
                System.out.println("[状态变化] 行动阶段完成");
                System.out.println("工具执行结果: " + e.getToolResult().getOutput());
                yield Mono.just(e);
            }
            
            case PostCallEvent e -> {
                System.out.println("[状态变化] Agent 执行完成");
                System.out.println("最终响应: " + e.getFinalMessage().getContent());
                yield Mono.just(e);
            }
            
            case ErrorEvent e -> {
                System.out.println("[错误] Agent 执行出错: " + e.getError().getMessage());
                yield Mono.just(e);
            }
            
            default -> Mono.just(event);
        };
    }
    
    @Override
    public int priority() {
        return 50;  // 高优先级，确保早期执行
    }
}
```

### 4.3.4 状态持久化

ReActAgent 支持状态持久化，可以在会话间保存和恢复 Agent 状态：

```java
// 状态持久化配置
public class StatePersistenceExample {
    
    public void configureStatePersistence() {
        // 默认配置：保存所有状态
        ReActAgent agent1 = ReActAgent.builder()
            .name("默认配置")
            .model(model)
            .build();
        
        // 自定义配置：只保存内存和工具包状态
        ReActAgent agent2 = ReActAgent.builder()
            .name("自定义配置")
            .model(model)
            .statePersistence(StatePersistence.builder()
                .planNotebookManaged(false)  // 不保存计划笔记本状态
                .build())
            .build();
    }
    
    // 保存状态到会话
    public void saveAgentState(ReActAgent agent, Session session, String sessionKey) {
        agent.saveTo(session, new SessionKey(sessionKey));
        System.out.println("Agent 状态已保存到会话: " + sessionKey);
    }
    
    // 从会话恢复状态
    public void loadAgentState(ReActAgent agent, Session session, String sessionKey) {
        boolean loaded = agent.loadIfExists(session, new SessionKey(sessionKey));
        if (loaded) {
            System.out.println("Agent 状态已从会话恢复: " + sessionKey);
        } else {
            System.out.println("未找到保存的会话状态");
        }
    }
}
```

---

## 4.4 工具调用机制

### 4.4.1 工具系统架构

AgentScope Java 的工具系统由以下几个核心组件构成：

```
┌──────────────────────────────────────────────────────────────────────┐
│                          工具系统架构                                  │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐        │
│  │    LLM       │ ───▶ │  ToolUseBlock │ ───▶ │   Toolkit    │        │
│  │  (模型推理)   │      │  (工具调用请求) │      │  (工具管理)   │        │
│  └──────────────┘      └──────────────┘      └──────┬───────┘        │
│                                                      │               │
│                              ┌───────────────────────┘               │
│                              ▼                                       │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐       │
│  │ToolResultBlock│ ◀─── │    Tool      │ ◀─── │ ToolCallParam│       │
│  │  (执行结果)   │      │  (工具实现)   │      │  (调用参数)   │       │
│  └──────────────┘      └──────────────┘      └──────────────┘       │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.4.2 定义工具方法

使用 `@Tool` 注解可以轻松定义工具：

```java
// 工具定义示例
public class CalculatorTools {
    
    /**
     * 计算器工具：执行基本数学运算
     */
    @Tool(
        name = "calculate",
        description = "执行数学计算，支持加、减、乘、除和幂运算。" +
                      "当你需要进行数值计算时使用此工具。"
    )
    public String calculate(
        @ToolParam(name = "expression", description = "数学表达式，例如: 2+3*5 或 10/2")
        String expression) {
        
        try {
            // 使用 JavaScript 引擎执行表达式
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("js");
            Object result = engine.eval(expression);
            return "计算结果: " + result;
        } catch (ScriptException e) {
            return "计算错误: 无效的表达式 '" + expression + "'";
        }
    }
    
    /**
     * 单位转换工具
     */
    @Tool(
        name = "convert_unit",
        description = "将数值从一个单位转换到另一个单位。"
    )
    public String convertUnit(
        @ToolParam(name = "value", description = "要转换的数值")
        double value,
        @ToolParam(name = "from_unit", description = "源单位，可选值: km, m, cm, mm")
        String fromUnit,
        @ToolParam(name = "to_unit", description = "目标单位，可选值: km, m, cm, mm")
        String toUnit) {
        
        // 单位转换系数
        Map<String, Double> toMeter = Map.of(
            "km", 1000.0,
            "m", 1.0,
            "cm", 0.01,
            "mm", 0.001
        );
        
        if (!toMeter.containsKey(fromUnit) || !toMeter.containsKey(toUnit)) {
            return "错误: 无效的单位";
        }
        
        double result = value * toMeter.get(fromUnit) / toMeter.get(toUnit);
        return String.format("转换结果: %.4f %s = %.4f %s", value, fromUnit, result, toUnit);
    }
}
```

### 4.4.3 异步工具实现

对于需要长时间运行的操作，应使用异步实现：

```java
// 异步工具示例
public class AsyncTools {
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    @Tool(
        name = "fetch_web_content",
        description = "从 URL 获取网页内容。适用于需要获取网页信息的场景。"
    )
    public Mono<String> fetchWebContent(
        @ToolParam(name = "url", description = "目标网页的 URL 地址")
        String url) {
        
        return Mono.fromCallable(() -> {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "AgentScope Java/1.0")
                .GET()
                .build();
            
            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
            
            // 限制返回内容长度
            String body = response.body();
            if (body.length() > 8000) {
                body = body.substring(0, 8000) + "\n...[内容已截断，超出最大长度限制]";
            }
            
            return body;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Tool(
        name = "search_database",
        description = "从数据库搜索产品信息"
    )
    public Mono<String> searchDatabase(
        @ToolParam(name = "query", description = "搜索关键词")
        String query,
        @ToolParam(name = "limit", description = "返回结果数量上限")
        int limit) {
        
        return Mono.fromCallable(() -> {
            // 模拟数据库查询
            List<Product> results = productRepository.search(query, limit);
            return JsonUtils.toJson(results);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
}
```

### 4.4.4 工具包管理

Toolkit 负责管理 Agent 可用的所有工具：

```java
// 工具包配置示例
public class ToolkitConfiguration {
    
    /**
     * 创建包含多个工具的 Toolkit
     */
    public Toolkit createToolkit() {
        Toolkit toolkit = new Toolkit();
        
        // 注册工具类（自动发现所有 @Tool 注解方法）
        toolkit.registerObject(new CalculatorTools());
        toolkit.registerObject(new AsyncTools());
        toolkit.registerObject(new FileTools());
        
        // 设置活跃的工具组
        toolkit.setActiveGroups(Set.of("calculator", "async"));
        
        // 配置工具执行选项
        toolkit.setDefaultExecutionConfig(
            ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(30))
                .maxRetries(2)
                .build()
        );
        
        return toolkit;
    }
    
    /**
     * 动态添加工具
     */
    public void addToolDynamically(Toolkit toolkit) {
        // 运行时添加新工具
        toolkit.registerObject(new DynamicTool());
    }
    
    /**
     * 按组管理工具
     */
    public void manageToolGroups() {
        Toolkit toolkit = new Toolkit();
        
        // 注册工具并指定组
        toolkit.registerObject(new FileTools(), "file_operations");
        toolkit.registerObject(new NetworkTools(), "network");
        toolkit.registerObject(new DatabaseTools(), "database");
        
        // 激活特定组的工具
        toolkit.setActiveGroups(Set.of("file_operations", "network"));
        
        // 禁用某些组
        toolkit.disableGroups(Set.of("dangerous_operations"));
    }
}
```

### 4.4.5 工具执行上下文

工具执行时可以访问丰富的上下文信息：

```java
// 使用工具执行上下文
public class ContextAwareTools {
    
    @Tool(name = "get_context_info", description = "获取当前执行上下文信息")
    public String getContextInfo(ToolCallParam param) {
        // 获取调用者信息
        String agentId = param.getAgentId();
        String agentName = param.getAgentName();
        
        // 获取执行上下文
        ToolExecutionContext context = param.getContext();
        String userId = context.getUserId();
        String sessionId = context.getSessionId();
        Map<String, Object> metadata = context.getMetadata();
        
        return String.format(
            "Agent ID: %s, Agent Name: %s, User ID: %s, Session ID: %s",
            agentId, agentName, userId, sessionId
        );
    }
}
```

---

## 4.5 【案例】带工具调用的 ReAct Agent 完整示例

本案例实现一个完整的 Spring Boot 应用，展示如何配置和使用带工具调用的 ReActAgent。

### 4.5.1 项目结构

```
src/main/java/io/agentscope/tutorial/chapter04/
├── Chapter04Application.java          # Spring Boot 启动类
├── config/
│   └── AgentConfig.java               # Agent 配置类
├── tools/
│   ├── WeatherTools.java             # 天气查询工具
│   └── KnowledgeTools.java           # 知识查询工具
├── hooks/
│   └── LoggingHook.java              # 日志记录钩子
├── controller/
│   └── AgentController.java          # REST API 控制器
└── service/
    └── AgentService.java              # Agent 服务层
```

### 4.5.2 项目依赖（pom.xml）

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

    <groupId>io.agentscope</groupId>
    <artifactId>tutorial-chapter04</artifactId>
    <version>1.0.0</version>
    <name>tutorial-chapter04</name>
    <description>AgentScope Java 教程 - 第四章 ReAct Agent 示例</description>

    <properties>
        <java.version>21</java.version>
        <agentscope.version>1.0.0</agentscope.version>
    </properties>

    <dependencies>
        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
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

### 4.5.3 Spring Boot 启动类

```java
package io.agentscope.tutorial.chapter04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 第四章教程示例应用启动类
 *
 * 本示例展示如何在 Spring Boot 环境中配置和使用 ReActAgent，
 * 包括工具定义、生命周期钩子、状态管理等核心功能。
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
@EnableAsync  // 启用异步支持，用于 Agent 的响应式执行
public class Chapter04Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter04Application.class, args);
        System.out.println("===============================================");
        System.out.println("AgentScope Java 教程 - 第四章示例已启动");
        System.out.println("API 文档: http://localhost:8080/swagger-ui.html");
        System.out.println("===============================================");
    }
}
```

### 4.5.4 天气查询工具

```java
package io.agentscope.tutorial.chapter04.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

/**
 * 天气查询工具集
 *
 * 本工具类提供模拟的天气查询功能，包括：
 * - 获取当前天气
 * - 获取天气预报
 * - 获取天气指数（如穿衣指数、运动指数等）
 *
 * @author AgentScope Tutorial
 */
public class WeatherTools {

    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取指定城市的当前天气
     *
     * @param city 城市名称（中文或英文均可）
     * @return 当前天气信息，包括温度、天气状况、湿度等
     */
    @Tool(
        name = "get_current_weather",
        description = "获取指定城市的当前天气信息。适用于询问某个地方的实时天气状况。"
    )
    public Mono<String> getCurrentWeather(
        @ToolParam(name = "city", description = "城市名称，例如：北京、上海、Beijing")
        String city) {

        return Mono.fromCallable(() -> {
            // 模拟网络延迟
            simulateDelay(500, 1000);

            // 生成模拟天气数据
            String[] conditions = {"晴", "多云", "阴", "小雨", "雷阵雨"};
            String condition = conditions[RANDOM.nextInt(conditions.length)];
            int temperature = RANDOM.nextInt(35) + 5;  // 5-40°C
            int humidity = RANDOM.nextInt(60) + 40;    // 40-100%
            int windSpeed = RANDOM.nextInt(20) + 1;   // 1-21 m/s

            return String.format(
                "【%s 当前天气】\n" +
                "🌡️ 温度: %d°C\n" +
                "☁️ 天气: %s\n" +
                "💧 湿度: %d%%\n" +
                "🌬️ 风速: %d m/s\n" +
                "⏰ 更新时间: %s",
                city, temperature, condition, humidity, windSpeed,
                LocalDateTime.now().format(DATE_FORMAT)
            );
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取指定城市未来几天的天气预报
     *
     * @param city 城市名称
     * @param days 预报天数（1-7天）
     * @return 天气预报信息
     */
    @Tool(
        name = "get_weather_forecast",
        description = "获取指定城市未来几天的天气预报。适用于询问接下来几天的天气变化。"
    )
    public Mono<String> getWeatherForecast(
        @ToolParam(name = "city", description = "城市名称")
        String city,
        @ToolParam(name = "days", description = "预报天数，最大7天")
        int days) {

        return Mono.fromCallable(() -> {
            simulateDelay(800, 1500);

            // 限制天数范围
            days = Math.max(1, Math.min(days, 7));

            StringBuilder forecast = new StringBuilder();
            forecast.append(String.format("【%s 天气预报（未来%d天）】\n\n", city, days));

            String[] conditions = {"晴", "多云", "阴", "小雨", "中雨", "雷阵雨"};
            LocalDateTime date = LocalDateTime.now();

            for (int i = 0; i < days; i++) {
                date = date.plusDays(1);
                String condition = conditions[RANDOM.nextInt(conditions.length)];
                int highTemp = RANDOM.nextInt(15) + 20;  // 20-35°C
                int lowTemp = highTemp - RANDOM.nextInt(10) - 5;  // 比高温低5-15度

                forecast.append(String.format(
                    "📅 %s\n" +
                    "   天气: %s\n" +
                    "   温度: %d°C ~ %d°C\n\n",
                    date.format(DateTimeFormatter.ofPattern("MM月dd日 E")),
                    condition, lowTemp, highTemp
                ));
            }

            return forecast.toString();
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取天气相关的生活指数
     *
     * @param city 城市名称
     * @param indexType 指数类型（可选值：穿衣、运动、旅游、感冒、紫外线）
     * @return 生活指数信息
     */
    @Tool(
        name = "get_weather_index",
        description = "获取天气相关的生活指数，帮助安排日常活动。"
    )
    public Mono<String> getWeatherIndex(
        @ToolParam(name = "city", description = "城市名称")
        String city,
        @ToolParam(name = "index_type",
                   description = "指数类型，可选值：穿衣、运动、旅游、感冒、紫外线")
        String indexType) {

        return Mono.fromCallable(() -> {
            simulateDelay(600, 1200);

            // 根据指数类型生成建议
            return switch (indexType) {
                case "穿衣" -> String.format(
                    "【%s 穿衣指数】\n" +
                    "👔 建议指数: %s\n" +
                    "💡 建议: 今日气温适中，建议穿着薄外套或长袖T恤，\n" +
                    "   早晚温差较大，可携带薄款外套备用。",
                    city, getRandomLevel()
                );
                case "运动" -> String.format(
                    "【%s 运动指数】\n" +
                    "🏃 建议指数: %s\n" +
                    "💡 建议: %s",
                    city, getRandomLevel(),
                    RANDOM.nextBoolean() ? "适合户外运动" : "建议室内运动"
                );
                case "旅游" -> String.format(
                    "【%s 旅游指数】\n" +
                    "🎒 建议指数: %s\n" +
                    "💡 建议: %s",
                    city, getRandomLevel(),
                    RANDOM.nextBoolean() ? "非常适合旅游" : "建议延期出行"
                );
                case "感冒" -> String.format(
                    "【%s 感冒指数】\n" +
                    "🤧 风险指数: %s\n" +
                    "💡 建议: %s",
                    city, getRandomLevel(),
                    RANDOM.nextBoolean() ? "不易感冒" : "注意保暖，预防感冒"
                );
                case "紫外线" -> String.format(
                    "【%s 紫外线指数】\n" +
                    "☀️ 强度指数: %s\n" +
                    "💡 建议: %s",
                    city, getRandomLevel(),
                    RANDOM.nextBoolean() ? "请注意防晒" : "紫外线较弱，可不防晒"
                );
                default -> "错误: 不支持的指数类型 '" + indexType + "'";
            };
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取支持的指数类型列表
     *
     * @return 支持的所有指数类型
     */
    @Tool(
        name = "get_supported_index_types",
        description = "获取支持的天气指数类型列表，便于后续查询。"
    )
    public String getSupportedIndexTypes() {
        return "支持的天气指数类型：\n" +
               "  - 穿衣: 穿衣指数\n" +
               "  - 运动: 运动指数\n" +
               "  - 旅游: 旅游指数\n" +
               "  - 感冒: 感冒指数\n" +
               "  - 紫外线: 紫外线指数";
    }

    // ========== 私有辅助方法 ==========

    private String getRandomLevel() {
        String[] levels = {"一级（极适宜）", "二级（适宜）", "三级（较适宜）",
                          "四级（较不适宜）", "五级（不适宜）"};
        return levels[RANDOM.nextInt(levels.length)];
    }

    private void simulateDelay(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + RANDOM.nextInt(maxMs - minMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 4.5.5 知识查询工具

```java
package io.agentscope.tutorial.chapter04.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 知识查询工具集
 *
 * 提供各种知识查询功能，包括：
 * - 单位换算
 * - 数学计算
 * - 常识问答
 * - 百科查询
 *
 * @author AgentScope Tutorial
 */
public class KnowledgeTools {

    private static final Random RANDOM = new Random();

    /**
     * 单位换算工具
     *
     * @param value 要转换的数值
     * @param fromUnit 源单位
     * @param toUnit 目标单位
     * @return 转换结果
     */
    @Tool(
        name = "convert_unit",
        description = "进行单位换算。支持长度、重量、温度、面积、体积等单位转换。"
    )
    public Mono<String> convertUnit(
        @ToolParam(name = "value", description = "要转换的数值")
        double value,
        @ToolParam(name = "from_unit", description = "源单位")
        String fromUnit,
        @ToolParam(name = "to_unit", description = "目标单位")
        String toUnit) {

        return Mono.fromCallable(() -> {
            simulateDelay(200, 500);

            // 长度单位转换（转换为米的系数）
            Map<String, Double> lengthToMeter = Map.ofEntries(
                Map.entry("km", 1000.0),
                Map.entry("m", 1.0),
                Map.entry("cm", 0.01),
                Map.entry("mm", 0.001),
                Map.entry("mi", 1609.344),
                Map.entry("ft", 0.3048),
                Map.entry("in", 0.0254),
                Map.entry("yd", 0.9144)
            );

            // 重量单位转换（转换为千克的系数）
            Map<String, Double> weightToKg = Map.ofEntries(
                Map.entry("kg", 1.0),
                Map.entry("g", 0.001),
                Map.entry("mg", 0.000001),
                Map.entry("t", 1000.0),
                Map.entry("lb", 0.453592),
                Map.entry("oz", 0.0283495)
            );

            // 温度转换
            if (fromUnit.equalsIgnoreCase(toUnit)) {
                return String.format("%.4f %s = %.4f %s", value, fromUnit, value, toUnit);
            }

            // 尝试长度转换
            if (lengthToMeter.containsKey(fromUnit.toLowerCase())
                && lengthToMeter.containsKey(toUnit.toLowerCase())) {
                double result = value * lengthToMeter.get(fromUnit.toLowerCase())
                    / lengthToMeter.get(toUnit.toLowerCase());
                return String.format("%.6f %s = %.6f %s", value, fromUnit, result, toUnit);
            }

            // 尝试重量转换
            if (weightToKg.containsKey(fromUnit.toLowerCase())
                && weightToKg.containsKey(toUnit.toLowerCase())) {
                double result = value * weightToKg.get(fromUnit.toLowerCase())
                    / weightToKg.get(toUnit.toLowerCase());
                return String.format("%.6f %s = %.6f %s", value, fromUnit, result, toUnit);
            }

            // 温度转换
            double celsius = toCelsius(value, fromUnit);
            if (celsius != Double.MIN_VALUE) {
                double result = fromCelsius(celsius, toUnit);
                return String.format("%.4f %s = %.4f %s", value, fromUnit, result, toUnit);
            }

            return String.format("错误: 不支持的单位转换 '%s' -> '%s'", fromUnit, toUnit);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 数学计算工具
     *
     * @param expression 数学表达式（如：2+3*5, sqrt(16), pow(2,3)）
     * @return 计算结果
     */
    @Tool(
        name = "calculate",
        description = "执行数学计算。支持的运算：加(+)、减(-)、乘(*)、除(/)、幂(^或pow)、" +
                      "平方根(sqrt)、绝对值(abs)、三角函数(sin/cos/tan)等。"
    )
    public Mono<String> calculate(
        @ToolParam(name = "expression", description = "数学表达式")
        String expression) {

        return Mono.fromCallable(() -> {
            simulateDelay(100, 300);

            try {
                // 使用简单的表达式解析器
                double result = evaluateExpression(expression);
                return String.format("计算结果: %s = %.6f", expression, result);
            } catch (Exception e) {
                return String.format("计算错误: %s", e.getMessage());
            }
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取词条解释
     *
     * @param term 要查询的词条
     * @return 词条的解释信息
     */
    @Tool(
        name = "get_term_explanation",
        description = "查询某个词条或概念的解释。适用于询问专业术语或概念的含义。"
    )
    public Mono<String> getTermExplanation(
        @ToolParam(name = "term", description = "要查询的词条或概念")
        String term) {

        return Mono.fromCallable(() -> {
            simulateDelay(500, 1000);

            // 模拟知识库
            Map<String, String> knowledgeBase = Map.ofEntries(
                Map.entry("人工智能",
                    "人工智能（AI）是研究、开发用于模拟、延伸和扩展人的智能的理论、方法、" +
                    "技术及应用系统的一门新的技术科学。"),
                Map.entry("机器学习",
                    "机器学习是人工智能的一个分支，专门研究计算机怎样模拟或实现人类的学习行为，" +
                    "以获取新的知识或技能，重新组织已有的知识结构使之不断改善自身的性能。"),
                Map.entry("深度学习",
                    "深度学习是机器学习的一个分支，它模仿在人脑中的神经元的工作方式，" +
                    "研究如何让计算机具有类似人脑的学习能力。"),
                Map.entry("大语言模型",
                    "大语言模型（LLM）是一类基于深度学习的自然语言处理模型，" +
                    "能够理解和生成人类语言，典型代表如 GPT、BERT 等。")
            );

            // 模糊匹配
            String lowerTerm = term.toLowerCase();
            for (Map.Entry<String, String> entry : knowledgeBase.entrySet()) {
                if (lowerTerm.contains(entry.getKey().toLowerCase())
                    || entry.getKey().toLowerCase().contains(lowerTerm)) {
                    return String.format("【%s】\n%s", entry.getKey(), entry.getValue());
                }
            }

            return String.format("抱歉，暂未找到关于 '%s' 的解释信息。", term);
        })
        .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 获取当前时间信息
     *
     * @param format 时间格式（可选，默认返回完整信息）
     * @return 当前时间信息
     */
    @Tool(
        name = "get_current_time",
        description = "获取当前的日期和时间信息。适用于需要获取当前时间戳的场景。"
    )
    public String getCurrentTime(
        @ToolParam(name = "format", description = "时间格式，如：yyyy-MM-dd HH:mm:ss")
        String format) {

        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        if (format == null || format.isBlank()) {
            return String.format(
                "【当前时间信息】\n" +
                "📅 日期: %s\n" +
                "⏰ 时间: %s\n" +
                "🗓️ 星期: %s\n" +
                "📆 年份: %d年第%d天",
                now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                now.format(java.time.format.DateTimeFormatter.ofPattern("HH时mm分ss秒")),
                now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL,
                    java.util.Locale.CHINA),
                now.getYear(),
                now.getDayOfYear()
            );
        }

        try {
            return now.format(java.time.format.DateTimeFormatter.ofPattern(format));
        } catch (Exception e) {
            return String.format("错误: 无效的时间格式 '%s'", format);
        }
    }

    // ========== 私有辅助方法 ==========

    private double toCelsius(double value, String unit) {
        return switch (unit.toLowerCase()) {
            case "c", "celsius" -> value;
            case "f", "fahrenheit" -> (value - 32) * 5 / 9;
            case "k", "kelvin" -> value - 273.15;
            default -> Double.MIN_VALUE;
        };
    }

    private double fromCelsius(double celsius, String unit) {
        return switch (unit.toLowerCase()) {
            case "c", "celsius" -> celsius;
            case "f", "fahrenheit" -> celsius * 9 / 5 + 32;
            case "k", "kelvin" -> celsius + 273.15;
            default -> Double.MIN_VALUE;
        };
    }

    private double evaluateExpression(String expr) {
        // 简单的表达式解析（实际应用中建议使用表达式引擎库）
        String expression = expr.replaceAll("\\s+", "").toLowerCase();

        // 处理基本运算
        try {
            // 尝试直接解析为数字
            if (expression.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(expression);
            }

            // 简单计算
            if (expression.contains("+")) {
                String[] parts = expression.split("\\+");
                return evaluateExpression(parts[0]) + evaluateExpression(parts[1]);
            }
            if (expression.contains("-") && expression.indexOf("-") > 0) {
                String[] parts = expression.split("-", 2);
                return evaluateExpression(parts[0]) - evaluateExpression(parts[1]);
            }
            if (expression.contains("*")) {
                String[] parts = expression.split("\\*");
                double result = 1;
                for (String part : parts) {
                    result *= evaluateExpression(part);
                }
                return result;
            }
            if (expression.contains("/")) {
                String[] parts = expression.split("/");
                double result = evaluateExpression(parts[0]);
                for (int i = 1; i < parts.length; i++) {
                    result /= evaluateExpression(parts[i]);
                }
                return result;
            }

            // 处理简单函数
            if (expression.startsWith("sqrt(") && expression.endsWith(")")) {
                double val = evaluateExpression(expression.substring(5, expression.length() - 1));
                return Math.sqrt(val);
            }
            if (expression.startsWith("abs(") && expression.endsWith(")")) {
                double val = evaluateExpression(expression.substring(4, expression.length() - 1));
                return Math.abs(val);
            }

        } catch (Exception e) {
            throw new RuntimeException("无法解析表达式: " + expr);
        }

        throw new RuntimeException("无法解析表达式: " + expr);
    }

    private void simulateDelay(int minMs, int maxMs) {
        try {
            Thread.sleep(minMs + RANDOM.nextInt(maxMs - minMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 4.5.6 日志记录钩子

```java
package io.agentscope.tutorial.chapter04.hooks;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PostCallEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.hook.ReasoningChunkEvent;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志记录钩子
 *
 * 本钩子负责记录 Agent 执行过程中的关键事件，包括：
 * - 推理阶段开始/结束
 * - 工具调用开始/结束
 * - 最终响应完成
 * - 执行时间统计
 *
 * @author AgentScope Tutorial
 */
public class LoggingHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(LoggingHook.class);

    private final Instant callStartTime = Instant.now();
    private final List<String> reasoningSteps = new ArrayList<>();
    private final List<String> toolExecutions = new ArrayList<>();

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        return Mono.fromCallable(() -> {
            if (event instanceof PreCallEvent e) {
                handlePreCall(e);
            } else if (event instanceof PreReasoningEvent e) {
                handlePreReasoning(e);
            } else if (event instanceof PostReasoningEvent e) {
                handlePostReasoning(e);
            } else if (event instanceof PreActingEvent e) {
                handlePreActing(e);
            } else if (event instanceof PostActingEvent e) {
                handlePostActing(e);
            } else if (event instanceof ReasoningChunkEvent e) {
                handleReasoningChunk(e);
            } else if (event instanceof PostCallEvent e) {
                handlePostCall(e);
            }
            return event;
        });
    }

    private void handlePreCall(PreCallEvent event) {
        callStartTime = Instant.now();
        reasoningSteps.clear();
        toolExecutions.clear();

        log.info("=".repeat(60));
        log.info("Agent 执行开始");
        log.info("输入消息数: {}", event.getInputMessages().size());
        if (event.getSystemMessage() != null) {
            log.info("系统提示词: {}",
                truncate(event.getSystemMessage().getContentAsString(), 100));
        }
        log.info("-".repeat(60));
    }

    private void handlePreReasoning(PreReasoningEvent event) {
        log.info("[推理阶段] 开始 - 模型: {}", event.getModelName());
    }

    private void handlePostReasoning(PostReasoningEvent event) {
        var msg = event.getReasoningMessage();
        if (msg == null) {
            log.info("[推理阶段] 结束 - 无推理输出");
            return;
        }

        // 记录思考内容
        var thinkingBlocks = msg.getContentBlocks(ThinkingBlock.class);
        if (!thinkingBlocks.isEmpty()) {
            String thinking = thinkingBlocks.get(0).getThinking();
            reasoningSteps.add(truncate(thinking, 150));
            log.info("[推理阶段] 结束 - 思考长度: {} 字符", thinking.length());
        }

        // 检查是否有工具调用
        var toolCalls = msg.getContentBlocks(ToolUseBlock.class);
        if (!toolCalls.isEmpty()) {
            log.info("[推理阶段] 决定调用 {} 个工具:", toolCalls.size());
            for (var tool : toolCalls) {
                log.info("  └─ {} ({})", tool.getName(), tool.getId());
            }
        } else {
            log.info("[推理阶段] 结束 - 无工具调用，将返回最终响应");
        }
    }

    private void handlePreActing(PreActingEvent event) {
        var toolUse = event.getToolUse();
        log.info("[工具执行] 开始 - 工具: {} ({})", toolUse.getName(), toolUse.getId());
    }

    private void handlePostActing(PostActingEvent event) {
        var result = event.getToolResult();
        var toolUse = event.getToolUse();

        String output = result.getOutputAsString();
        toolExecutions.add(String.format("%s: %s",
            toolUse.getName(),
            result.isError() ? "错误" : "成功"));

        if (result.isError()) {
            log.warn("[工具执行] 完成 - {}: 错误 - {}",
                toolUse.getName(), truncate(output, 100));
        } else {
            log.info("[工具执行] 完成 - {}: 成功",
                toolUse.getName());
        }
    }

    private void handleReasoningChunk(ReasoningChunkEvent event) {
        var chunk = event.getIncrementalChunk();
        var content = chunk.getFirstContentBlock();

        if (content instanceof TextBlock textBlock) {
            // 流式文本输出
            System.out.print(textBlock.getText());
        } else if (content instanceof ThinkingBlock thinkingBlock) {
            // 思考内容（不打印，只记录）
        } else if (content instanceof ToolUseBlock toolBlock) {
            System.out.println();  // 换行
            log.debug("[流式输出] 工具调用块: {}", toolBlock.getName());
        }
    }

    private void handlePostCall(PostCallEvent event) {
        Duration duration = Duration.between(callStartTime, Instant.now());

        log.info("-".repeat(60));
        log.info("Agent 执行完成");
        log.info("执行时长: {} ms", duration.toMillis());

        if (!reasoningSteps.isEmpty()) {
            log.info("推理步骤数: {}", reasoningSteps.size());
        }

        if (!toolExecutions.isEmpty()) {
            log.info("工具执行:");
            for (String exec : toolExecutions) {
                log.info("  └─ {}", exec);
            }
        }

        var response = event.getFinalMessage();
        log.info("最终响应长度: {} 字符", response.getContentAsString().length());
        log.info("=".repeat(60));
    }

    @Override
    public int priority() {
        return 500;  // 低优先级，在其他钩子之后执行
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
```

### 4.5.7 Agent 配置类

```java
package io.agentscope.tutorial.chapter04.config;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter04.hooks.LoggingHook;
import io.agentscope.tutorial.chapter04.tools.KnowledgeTools;
import io.agentscope.tutorial.chapter04.tools.WeatherTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * Agent 配置类
 *
 * 配置 ReActAgent 实例，包括：
 * - 模型配置
 * - 工具包配置
 * - 钩子配置
 * - 记忆配置
 *
 * @author AgentScope Tutorial
 */
@Configuration
public class AgentConfig {

    @Value("${agentscope.model.api-key:EMPTY}")
    private String apiKey;

    @Value("${agentscope.model.model-name:qwen-plus}")
    private String modelName;

    @Value("${agentscope.model.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    /**
     * 创建工具包
     *
     * 配置 Agent 可用的所有工具
     */
    @Bean
    public Toolkit toolkit() {
        Toolkit toolkit = new Toolkit();

        // 注册工具类（自动发现所有 @Tool 注解方法）
        toolkit.registerObject(new WeatherTools());
        toolkit.registerObject(new KnowledgeTools());

        // 配置工具执行超时时间
        toolkit.setDefaultExecutionConfig(
            ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(60))
                .maxRetries(2)
                .build()
        );

        return toolkit;
    }

    /**
     * 创建日志钩子
     */
    @Bean
    public Hook loggingHook() {
        return new LoggingHook();
    }

    /**
     * 创建 ReActAgent
     *
     * 配置完整的 ReActAgent 实例，包括：
     * - 名称和描述
     * - 系统提示词
     * - 模型
     * - 工具包
     * - 记忆存储
     * - 最大迭代次数
     * - 钩子列表
     */
    @Bean
    public ReActAgent reactAgent(Model model, Toolkit toolkit, Hook loggingHook) {
        String systemPrompt = """
            你是一个智能助手，名为小探。你有以下能力：

            1. 天气查询：你可以查询城市当前的天气、天气预报以及各种生活指数。
               可用的天气工具包括：
               - get_current_weather: 获取当前天气
               - get_weather_forecast: 获取天气预报
               - get_weather_index: 获取生活指数
               - get_supported_index_types: 获取支持的指数类型

            2. 单位换算：你可以进行各种单位之间的转换。
               - 长度单位：km, m, cm, mm, mi, ft, in, yd
               - 重量单位：kg, g, mg, t, lb, oz
               - 温度单位：c/f/k

            3. 数学计算：你可以执行数学表达式的计算。

            4. 知识查询：你可以查询一些常见术语和概念的解释。

            5. 时间查询：你可以获取当前时间信息。

            请根据用户的问题，选择合适的工具来回答。
            如果用户的问题需要调用工具才能回答，请仔细分析并选择正确的工具。
            每次只调用一个工具，等待结果后再决定下一步。
            """;

        return ReActAgent.builder()
            .name("小探")
            .description("一个智能助手，可以查询天气、单位换算、数学计算等")
            .sysPrompt(systemPrompt)
            .model(model)
            .toolkit(toolkit)
            .memory(new InMemoryMemory())
            .maxIters(15)  // 最多15次推理-行动循环
            .hooks(List.of(loggingHook))
            .build();
    }
}
```

### 4.5.8 REST API 控制器

```java
package io.agentscope.tutorial.chapter04.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Agent REST API 控制器
 *
 * 提供以下 API 端点：
 * - POST /api/agent/chat - 发送消息并获取响应
 * - GET /api/agent/chat/stream - 流式响应
 * - GET /api/agent/history - 获取对话历史
 * - DELETE /api/agent/history - 清除对话历史
 * - GET /api/agent/status - 获取 Agent 状态
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final ReActAgent agent;

    public AgentController(ReActAgent agent) {
        this.agent = agent;
    }

    /**
     * 发送消息并获取响应（同步）
     *
     * @param request 聊天请求，包含消息内容
     * @return Agent 的响应
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求: {}", truncate(request.message(), 50));

        // 构建用户消息
        Msg userMessage = Msg.builder()
            .name("user")
            .role(MsgRole.USER)
            .content(TextBlock.builder().text(request.message()).build())
            .build();

        // 调用 Agent（同步方式）
        Msg response = agent.call(userMessage).block();

        // 构建响应
        Map<String, Object> result = Map.of(
            "response", response != null ? response.getContentAsString() : "无响应",
            "agentName", agent.getName(),
            "messageCount", agent.getMemory().getMessages().size()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 发送消息并获取流式响应
     *
     * @param request 聊天请求
     * @return SSE 流
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> chatStream(@RequestBody ChatRequest request) {
        log.info("收到流式聊天请求: {}", truncate(request.message(), 50));

        Msg userMessage = Msg.builder()
            .name("user")
            .role(MsgRole.USER)
            .content(TextBlock.builder().text(request.message()).build())
            .build();

        StreamOptions options = StreamOptions.builder()
            .stream(EventType.REASONING)
            .stream(EventType.TOOL_RESULT)
            .build();

        return agent.stream(userMessage, options)
            .map(event -> Map.of(
                "type", event.getType().name(),
                "content", formatEventContent(event),
                "timestamp", java.time.Instant.now().toString()
            ))
            .doOnComplete(() -> log.info("流式响应完成"))
            .doOnError(e -> log.error("流式响应出错", e));
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getHistory() {
        List<Msg> messages = agent.getMemory().getMessages();

        List<Map<String, Object>> messageList = messages.stream()
            .map(msg -> Map.of(
                "role", msg.getRole().name(),
                "content", msg.getContentAsString(),
                "name", msg.getName() != null ? msg.getName() : ""
            ))
            .toList();

        return ResponseEntity.ok(Map.of(
            "messages", messageList,
            "count", messages.size()
        ));
    }

    /**
     * 清除对话历史
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, Object>> clearHistory() {
        int count = agent.getMemory().getMessages().size();
        // 重新创建记忆存储（简单实现）
        agent.getMemory().clear();  // 如果 InMemoryMemory 支持 clear 方法

        return ResponseEntity.ok(Map.of(
            "message", "对话历史已清除",
            "clearedCount", count
        ));
    }

    /**
     * 获取 Agent 状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "agentId", agent.getAgentId(),
            "name", agent.getName(),
            "description", agent.getDescription(),
            "maxIterations", agent.getMaxIters(),
            "messageCount", agent.getMemory().getMessages().size(),
            "sysPrompt", truncate(agent.getSysPrompt(), 200)
        ));
    }

    /**
     * 中断 Agent 执行
     */
    @PostMapping("/interrupt")
    public ResponseEntity<Map<String, String>> interrupt() {
        log.info("收到中断请求");
        agent.interrupt();
        return ResponseEntity.ok(Map.of(
            "message", "中断请求已发送"
        ));
    }

    // ========== 私有辅助方法 ==========

    private String formatEventContent(Event event) {
        var msg = event.getMessage();
        if (msg == null) return "";

        return switch (event.getType()) {
            case REASONING -> msg.getContentAsString();
            case TOOL_RESULT -> {
                var toolResults = msg.getContentBlocks(
                    io.agentscope.core.message.ToolResultBlock.class);
                if (!toolResults.isEmpty()) {
                    yield "工具 [" + toolResults.get(0).getName() + "] 执行完成";
                }
                yield "";
            }
            case HINT -> msg.getContentAsString();
            case AGENT_RESULT -> msg.getContentAsString();
            case SUMMARY -> msg.getContentAsString();
            default -> "";
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    // ========== 请求/响应 DTO ==========

    public record ChatRequest(
        @NotBlank(message = "消息不能为空")
        @Size(max = 5000, message = "消息长度不能超过5000字符")
        String message
    ) {}
}
```

### 4.5.9 应用配置文件

```yaml
# application.yml
spring:
  application:
    name: agentscope-chapter04-tutorial

  # HTTP 服务器配置
  server:
    port: 8080

# AgentScope 配置
agentscope:
  model:
    api-key: ${DASHSCOPE_API_KEY:EMPTY}
    model-name: ${MODEL_NAME:qwen-plus}
    base-url: ${BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}

# 日志配置
logging:
  level:
    root: INFO
    io.agentscope: DEBUG
    io.agentscope.tutorial: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 4.5.10 API 使用示例

**1. 同步聊天**
```bash
# 发送普通对话
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "北京现在的天气怎么样？"}'

# 响应示例
{
  "response": "根据查询结果，北京当前天气晴朗，气温25°C，湿度45%，风速3m/s。...",
  "agentName": "小探",
  "messageCount": 4
}
```

**2. 流式聊天**
```bash
# 获取流式响应
curl -X POST http://localhost:8080/api/agent/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "请帮我计算 25*4+10 的结果"}' \
  --no-buffer

# 响应示例（SSE格式）
event: {"type":"REASONING","content":"用户想让我计算数学表达式25*4+10=110...","timestamp":"..."}
event: {"type":"TOOL_RESULT","content":"工具 [calculate] 执行完成","timestamp":"..."}
event: {"type":"AGENT_RESULT","content":"25*4+10的计算结果是110...","timestamp":"..."}
```

**3. 查看状态**
```bash
# 获取 Agent 状态
curl http://localhost:8080/api/agent/status

# 响应示例
{
  "agentId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "小探",
  "description": "一个智能助手，可以查询天气、单位换算、数学计算等",
  "maxIterations": 15,
  "messageCount": 6,
  "sysPrompt": "你是一个智能助手，名为小探..."
}
```

---

## 4.6 本章小结

本章深入介绍了 AgentScope Java 框架中 ReAct Agent 的核心概念和实践应用：

### 核心要点

1. **ReAct 模式原理**：通过 Thought-Action-Observation 循环，结合 LLM 的推理能力和工具调用能力，实现复杂任务的自动化处理

2. **ReActAgent 配置**：使用 Builder 模式灵活配置 Agent 的名称、模型、工具包、记忆存储等组件

3. **生命周期管理**：ReActAgent 具有 idle、running、waiting、terminated 等状态，通过 Hook 系统实现状态变化的监控和响应

4. **工具调用机制**：基于 `@Tool` 注解定义工具方法，Toolkit 负责工具的注册、发现和执行

5. **状态持久化**：支持将 Agent 状态保存到会话，实现跨会话的状态恢复

### 最佳实践

- **合理设置 maxIters**：根据任务复杂度设置合适的最大迭代次数，避免无限循环
- **使用异步工具**：耗时操作应使用 `Mono<T>` 返回类型，避免阻塞执行线程
- **完善工具描述**：为每个工具提供清晰、准确的描述，帮助模型做出正确的工具选择
- **善用 Hook 系统**：通过 Hook 实现日志记录、性能监控、错误处理等横切关注点
- **实现状态持久化**：对于需要长期运行的 Agent，务必配置状态持久化机制

### 下一章预告

第五章我们将深入探讨 **工具系统**，包括：
- 内置工具的使用
- 自定义工具开发规范
- 工具参数验证
- MCP（Model Context Protocol）工具集成

---

**相关资源**
- 框架源码：`io.agentscope.core.ReActAgent`
- 工具定义：`io.agentscope.core.tool.Tool`
- 钩子系统：`io.agentscope.core.hook.Hook`
- 消息系统：`io.agentscope.core.message.Msg`