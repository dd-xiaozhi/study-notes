# 第二章：快速开始

本章将带你从零开始，通过一个完整的案例，手把手教你搭建并运行第一个 AgentScope Java 项目。

---

## 2.1 Maven 依赖配置

AgentScope Java 通过 Maven 进行依赖管理。核心模块为 `agentscope-core`，提供了 Agent、模型、工具、记忆等所有基础能力。

### 引入 BOM（Bill of Materials）

推荐先引入父 BOM，统一管理版本号，避免依赖冲突：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter02-quickstart</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>AgentScope Java - Tutorial - Chapter 02 Quickstart</name>

    <!-- 引入父 BOM，统一管理所有 AgentScope 依赖版本 -->
    <parent>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-parent</artifactId>
        <version>${revision}</version>
        <relativePath/>
    </parent>

    <properties>
        <revision>1.1.0-SNAPSHOT</revision>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- 核心模块：包含 Agent、模型、工具、记忆等所有基础能力 -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
        </dependency>

        <!-- 如需 Spring Boot 集成 -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-spring-boot-starter</artifactId>
        </dependency>

        <!-- 如使用阿里云百炼模型（可选） -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>dashscope-sdk-java</artifactId>
        </dependency>
    </dependencies>
</project>
```

> **提示**：AgentScope Java 要求 JDK 17 及以上，推荐使用 JDK 21 以获得最佳性能。

---

## 2.2 创建第一个 Agent

AgentScope Java 的核心是 `ReActAgent`，它基于 ReAct（Reasoning + Acting）范式，让 Agent 能够思考、推理并调用工具完成任务。

### 基本创建步骤

```java
package io.agentscope.tutorial.chapter02;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;

public class FirstAgent {

    public static void main(String[] args) {
        // 1. 创建内存（保存对话历史）
        var memory = new InMemoryMemory();

        // 2. 创建工具包（Agent 可调用的工具集合）
        var toolkit = new Toolkit();

        // 3. 构建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("MyFirstAgent")           // Agent 名称
                .sysPrompt("你是一个有帮助的AI助手")  // 系统提示词
                .model(dashScopeModel())         // 模型客户端（见下一节）
                .memory(memory)                  // 记忆组件
                .toolkit(toolkit)                // 工具包
                .build();

        // 4. 执行对话
        System.out.println("Agent 构建成功！");
    }

    private static Object dashScopeModel() {
        // 见 2.3 节
        return null;
    }
}
```

### Agent 核心配置项说明

| 配置项 | 类型 | 必需 | 说明 |
|--------|------|------|------|
| `name` | String | 是 | Agent 唯一标识名称 |
| `sysPrompt` | String | 是 | 系统提示词，定义 Agent 的角色和行为 |
| `model` | `Model` | 是 | 模型客户端，负责与 LLM 交互 |
| `memory` | `Memory` | 是 | 记忆组件，保存对话历史 |
| `toolkit` | `Toolkit` | 否 | 工具包，包含 Agent 可调用的工具 |
| `maxIterations` | int | 否 | 最大迭代次数，默认 10，防止无限循环 |
| `maxToolCallCount` | int | 否 | 单次执行最大工具调用次数 |

---

## 2.3 配置模型客户端

模型客户端是 Agent 的大脑，负责与 LLM 进行通信。AgentScope Java 内置支持多种模型：

- **DashScope**（阿里云百炼）
- **OpenAI 兼容接口**
- **Ollama 本地模型**
- **Google Gemini**
- **Anthropic Claude**

### 以 DashScope 为例

DashScope 是阿里云提供的 LLM 服务，支持通义千问系列模型。

```java
package io.agentscope.tutorial.chapter02;

import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;

// 配置 DashScope 模型客户端
public class ModelConfig {

    public static DashScopeChatModel createDashScopeModel() {
        // ⚠️ 请替换为你的实际 API Key
        // 可通过环境变量 DASHSCOPE_API_KEY 设置
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "your-api-key-here"; // 占位符，请替换
        }

        return DashScopeChatModel.builder()
                .apiKey(apiKey)                              // API Key
                .modelName("qwen-plus")                      // 模型名称：qwen-plus / qwen-turbo / qwen-max 等
                .stream(true)                                // 启用流式输出
                .enableThinking(true)                         // 启用思考模式（部分模型支持）
                .formatter(new DashScopeChatFormatter())     // 消息格式化器
                .defaultOptions(
                        GenerateOptions.builder()
                                .thinkingBudget(1024)       // 思考 token 上限
                                .temperature(0.7)           // 温度参数
                                .build())
                .build();
    }
}
```

### 模型配置参数详解

| 参数 | 类型 | 说明 |
|------|------|------|
| `apiKey` | String | 模型服务的 API Key |
| `modelName` | String | 模型标识符，如 `qwen-plus`、`gpt-4o` |
| `stream` | boolean | 是否启用流式输出（默认 true） |
| `enableThinking` | boolean | 是否启用思考模式（部分模型支持） |
| `formatter` | `ChatFormatter` | 消息格式化器，用于将 AgentScope 消息转换为模型格式 |
| `defaultOptions` | `GenerateOptions` | 默认生成参数（temperature、topP、thinkingBudget 等） |

---

## 2.4 Agent 执行流程

理解 Agent 的执行流程，有助于更好地使用 AgentScope Java。

### ReAct 执行循环

```
┌─────────────────────────────────────────────────────────┐
│                    ReAct 执行流程                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────┐    1. 思考 (Reasoning)                     │
│  │  开始    │──────────────────────────┐                 │
│  └──────────┘                         ▼                 │
│                               ┌──────────────┐          │
│                               │  思考当前状态 │          │
│                               │  决定下一步   │          │
│                               └──────────────┘          │
│                                       │                 │
│                          2. 行动 (Acting)               │
│                                       ▼                 │
│                               ┌──────────────┐          │
│                               │  调用工具     │          │
│                               │  或生成回复   │          │
│                               └──────────────┘          │
│                                       │                 │
│                    3. 观察 (Observation)                 │
│                                       ▼                 │
│                               ┌──────────────┐          │
│                               │  获取工具结果 │          │
│                               │  或用户反馈   │          │
│                               └──────────────┘          │
│                                       │                 │
│                               ┌───────┴───────┐         │
│                               │  达到终止条件? │         │
│                               └───────────────┘         │
│                                    是 │ 否              │
│                                      ▼                  │
│                               回到 步骤1                 │
│                                                         │
│  终止条件：                                              │
│  - 生成了最终回复                                        │
│  - 达到最大迭代次数                                      │
│  - 用户中断                                             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 核心方法

| 方法 | 说明 |
|------|------|
| `agent.call(msg)` | 非流式调用，阻塞等待完整响应，返回 `Mono<Msg>` |
| `agent.stream(msg, options)` | 流式调用，实时返回响应事件，返回 `Flux<Event>` |
| `agent.run(task)` | 运行任务描述，自动构建消息并执行 |

---

## 2.5 运行你的第一个 Agent

下面通过一个完整的案例，演示如何运行一个简单的问答 Agent。

### 2.5.1 项目结构

```
chapter02-quickstart/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── io/
                └── agentscope/
                    └── tutorial/
                        └── chapter02/
                            ├── App.java                    # 运行入口
                            ├── agent/
                            │   └── QAAgent.java            # Agent 配置类
                            └── config/
                                └── ModelConfig.java         # 模型配置
```

---

### 2.5.2 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright 2024-2026 the original author or authors.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~     http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
-->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter02-quickstart</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>AgentScope Java - Tutorial - Chapter 02 Quickstart</name>
    <description>第二章快速开始示例：创建你的第一个 Agent</description>

    <!-- 继承 AgentScope 父 BOM，统一管理版本 -->
    <parent>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-parent</artifactId>
        <version>${revision}</version>
        <relativePath/>
    </parent>

    <properties>
        <!-- 请确保使用 JDK 21 或更高版本 -->
        <revision>1.1.0-SNAPSHOT</revision>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- 核心模块：包含 Agent、模型、工具、记忆等所有基础能力 -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
        </dependency>

        <!-- 引入 DashScope SDK（用于模型调用） -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>dashscope-sdk-java</artifactId>
        </dependency>

        <!-- SLF4J 日志门面（AgentScope 使用 slf4j 记录日志） -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
        </dependency>
    </dependencies>

    <!-- 配置 Maven 插件 -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>io.agentscope.tutorial.chapter02.App</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### 2.5.3 模型配置 - config/ModelConfig.java

```java
/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter02.config;

import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;

/**
 * 模型配置类。
 *
 * <p>本类负责创建和配置模型客户端，支持 DashScope（阿里云百炼）模型服务。
 *
 * <p>API Key 获取方式：
 * <ul>
 *   <li>环境变量：设置 DASHSCOPE_API_KEY</li>
 *   <li>阿里云控制台：https://dashscope.console.aliyun.com/apiKey</li>
 * </ul>
 *
 * @author AgentScope Team
 * @since 1.0
 */
public class ModelConfig {

    /**
     * 创建 DashScope 模型客户端。
     *
     * <p>配置说明：
     * <ul>
     *   <li>modelName: 支持 qwen-plus（推荐）、qwen-turbo（快速）、qwen-max（最强）等</li>
     *   <li>stream: 启用流式输出，实时显示思考过程和回复</li>
     *   <li>enableThinking: 启用思考模式，模型会先思考再回答</li>
     * </ul>
     *
     * @return 配置好的 DashScopeChatModel 实例
     */
    public static DashScopeChatModel createDashScopeModel() {
        // 从环境变量获取 API Key，如果未设置则使用占位符
        // ⚠️ 生产环境请确保通过环境变量或安全配置获取 API Key
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("[警告] 未设置 DASHSCOPE_API_KEY 环境变量，使用占位符演示");
            apiKey = "your-api-key-here";
        }

        return DashScopeChatModel.builder()
                // API Key：从环境变量或配置中心获取
                .apiKey(apiKey)
                // 模型名称：qwen-plus（通义千问增强版）
                // 其他可选：qwen-turbo（快速版）、qwen-max（最强版）、qwen-vl-plus（视觉版）
                .modelName("qwen-plus")
                // 启用流式输出：实时显示模型的思考和回复过程
                .stream(true)
                // 启用思考模式：模型会先进行推理思考，再给出最终答案
                .enableThinking(true)
                // 消息格式化器：将 AgentScope 消息格式转换为 DashScope API 格式
                .formatter(new DashScopeChatFormatter())
                // 默认生成参数
                .defaultOptions(
                        GenerateOptions.builder()
                                // 思考 token 预算：允许模型使用最多 1024 个 token 进行思考
                                .thinkingBudget(1024)
                                // 温度参数：控制输出的随机性（0=确定性强，1=创造性高）
                                .temperature(0.7)
                                // 最大 token 数：限制单次回复的最大长度
                                .maxTokens(2048)
                                .build())
                .build();
    }

    /**
     * 创建带自定义参数的模型客户端。
     *
     * @param modelName   模型名称
     * @param temperature 温度参数
     * @param thinkingBudget 思考 token 预算
     * @return 配置好的模型客户端
     */
    public static DashScopeChatModel createCustomModel(
            String modelName, double temperature, int thinkingBudget) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = "your-api-key-here";
        }

        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .stream(true)
                .enableThinking(true)
                .formatter(new DashScopeChatFormatter())
                .defaultOptions(
                        GenerateOptions.builder()
                                .thinkingBudget(thinkingBudget)
                                .temperature(temperature)
                                .maxTokens(2048)
                                .build())
                .build();
    }
}
```

---

### 2.5.4 Agent 配置类 - agent/QAAgent.java

```java
/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter02.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter02.config.ModelConfig;

/**
 * 问答 Agent 配置类。
 *
 * <p>本类负责构建一个简单的问答 Agent，具备以下能力：
 * <ul>
 *   <li>基于通义千问大模型进行对话</li>
 *   <li>保存对话历史，实现多轮对话</li>
 *   <li>支持思考模式，展示推理过程</li>
 * </ul>
 *
 * <p>Agent 构建器模式：
 * <pre>{@code
 * ReActAgent agent = QAAgent.create();
 * }</pre>
 *
 * @author AgentScope Team
 * @since 1.0
 */
public class QAAgent {

    /** Agent 默认名称 */
    private static final String DEFAULT_NAME = "QAAgent";

    /** 默认系统提示词 */
    private static final String DEFAULT_SYS_PROMPT =
            "你是一个专业、耐心的AI助手。你的任务是：\n"
                    + "1. 准确理解用户的问题\n"
                    + "2. 提供清晰、有条理的回答\n"
                    + "3. 如果不确定，诚实告知用户\n"
                    + "4. 适当使用思考过程来分析问题";

    /** 最大迭代次数：防止 Agent 陷入无限循环 */
    private static final int DEFAULT_MAX_ITERATIONS = 10;

    /**
     * 创建默认配置的问答 Agent。
     *
     * <p>使用配置：
     * <ul>
     *   <li>模型：qwen-plus（通义千问增强版）</li>
     *   <li>记忆：内存记忆（对话关闭后丢失）</li>
     *   <li>工具：无（纯问答场景）</li>
     * </ul>
     *
     * @return 配置好的 ReActAgent 实例
     */
    public static ReActAgent create() {
        return create(DEFAULT_NAME, DEFAULT_SYS_PROMPT);
    }

    /**
     * 创建自定义名称和提示词的问答 Agent。
     *
     * @param name      Agent 名称
     * @param sysPrompt 系统提示词
     * @return 配置好的 ReActAgent 实例
     */
    public static ReActAgent create(String name, String sysPrompt) {
        // 1. 创建内存组件：用于保存对话历史
        // InMemoryMemory：基于内存的记忆实现，重启后丢失
        // 如需持久化，可使用 SessionMemory 或其他实现
        var memory = new InMemoryMemory();

        // 2. 创建工具包：定义 Agent 可调用的工具集合
        // 当前为空（new Toolkit()），表示这是一个纯问答 Agent
        // 如需添加工具，可通过 toolkit.register(...) 注册
        var toolkit = new Toolkit();

        // 3. 构建 ReActAgent
        return ReActAgent.builder()
                // Agent 名称：唯一标识，用于日志和调试
                .name(name)
                // 系统提示词：定义 Agent 的角色、能力边界和行为规范
                .sysPrompt(sysPrompt)
                // 模型客户端：从配置类获取
                .model(ModelConfig.createDashScopeModel())
                // 记忆组件：保存对话历史
                .memory(memory)
                // 工具包：Agent 可调用的工具集合
                .toolkit(toolkit)
                // 最大迭代次数：防止无限循环
                .maxIterations(DEFAULT_MAX_ITERATIONS)
                // 构建 Agent 实例
                .build();
    }

    /**
     * 创建带工具的问答 Agent。
     *
     * <p>示例：创建一个能够搜索网络的问答 Agent
     *
     * @param toolkit 工具包
     * @return 配置好的 ReActAgent 实例
     */
    public static ReActAgent createWithTools(Toolkit toolkit) {
        return ReActAgent.builder()
                .name(DEFAULT_NAME)
                .sysPrompt(DEFAULT_SYS_PROMPT)
                .model(ModelConfig.createDashScopeModel())
                .memory(new InMemoryMemory())
                .toolkit(toolkit)
                .maxIterations(DEFAULT_MAX_ITERATIONS)
                .build();
    }
}
```

---

### 2.5.5 运行入口 - App.java

```java
/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter02;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ThinkingBlock;
import io.agentscope.tutorial.chapter02.agent.QAAgent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 第二章快速开始：运行你的第一个 Agent。
 *
 * <p>本示例演示：
 * <ul>
 *   <li>如何创建一个简单的问答 Agent</li>
 *   <li>如何使用流式输出实时显示思考和回复</li>
 *   <li>如何实现多轮对话</li>
 * </ul>
 *
 * <p>运行方式：
 * <pre>{@code
 * # 设置 API Key
 * export DASHSCOPE_API_KEY=your-api-key
 *
 * # 编译运行
 * mvn compile exec:java -Dexec.mainClass="io.agentscope.tutorial.chapter02.App"
 * }</pre>
 *
 * @author AgentScope Team
 * @since 1.0
 */
public class App {

    /** 标准输入读取器 */
    private static final BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

    /**
     * 程序入口。
     *
     * @param args 命令行参数（当前未使用）
     */
    public static void main(String[] args) {
        // 打印欢迎信息
        printWelcome();

        // 创建 Agent
        ReActAgent agent = QAAgent.create();
        System.out.println("[INFO] Agent 创建成功！\n");

        // 进入交互式对话循环
        runChatLoop(agent);

        // 退出提示
        System.out.println("[INFO] 程序结束");
    }

    /**
     * 打印欢迎信息。
     */
    private static void printWelcome() {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║        AgentScope Java - 第二章：快速开始                  ║");
        System.out.println("║        你的第一个 AI Agent                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();

        // 检查 API Key
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("[警告] 未设置 DASHSCOPE_API_KEY 环境变量");
            System.out.println("       请先设置：export DASHSCOPE_API_KEY=your-key");
            System.out.println("       或访问：https://dashscope.console.aliyun.com/apiKey");
            System.out.println();
        } else {
            System.out.println("[INFO] API Key 已配置（已脱敏显示）");
            System.out.println("       " + maskApiKey(apiKey));
            System.out.println();
        }
    }

    /**
     * 运行交互式对话循环。
     *
     * @param agent 要对话的 Agent 实例
     */
    private static void runChatLoop(Agent agent) {
        System.out.println("=== 对话开始 ===");
        System.out.println("输入 'exit' 或 'quit' 退出对话\n");

        while (true) {
            // 读取用户输入
            System.out.print("你> ");
            String input;
            try {
                input = reader.readLine();
            } catch (IOException e) {
                System.err.println("[错误] 读取输入失败：" + e.getMessage());
                break;
            }

            // 处理退出命令
            if (input == null || "exit".equalsIgnoreCase(input.trim())
                    || "quit".equalsIgnoreCase(input.trim())) {
                System.out.println("再见！");
                break;
            }

            // 跳过空输入
            if (input.trim().isEmpty()) {
                continue;
            }

            // 发送消息并显示响应
            try {
                sendMessage(agent, input);
            } catch (Exception e) {
                System.err.println("\n[错误] " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println();
        }
    }

    /**
     * 发送消息并显示响应（流式输出）。
     *
     * @param agent Agent 实例
     * @param input 用户输入
     */
    private static void sendMessage(Agent agent, String input) {
        // 构建用户消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(input).build())
                .build();

        System.out.print("Agent> ");

        // 用于跟踪输出状态
        AtomicBoolean hasPrintedThinkingHeader = new AtomicBoolean(false);
        AtomicBoolean hasPrintedTextHeader = new AtomicBoolean(false);
        AtomicBoolean hasPrintedTextSeparator = new AtomicBoolean(false);
        AtomicReference<String> lastThinkingContent = new AtomicReference<>("");
        AtomicReference<String> lastTextContent = new AtomicReference<>("");

        // 配置流式输出选项
        StreamOptions streamOptions = StreamOptions.builder()
                // 监听推理过程和工具结果事件
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT)
                // 增量模式：只显示新增内容
                .incremental(true)
                // 不包含最终的推理结果文本（已在思考中显示）
                .includeReasoningResult(false)
                .build();

        try {
            // 使用流式调用，实时显示响应
            agent.stream(userMsg, streamOptions)
                    .doOnNext(event -> {
                        Msg msg = event.getMessage();
                        for (ContentBlock block : msg.getContent()) {
                            if (block instanceof ThinkingBlock) {
                                // 显示思考过程
                                printStreamContent(
                                        ((ThinkingBlock) block).getThinking(),
                                        lastThinkingContent,
                                        hasPrintedThinkingHeader,
                                        "> 思考: ",
                                        null);
                            } else if (block instanceof TextBlock) {
                                // 显示回复文本
                                printStreamContent(
                                        ((TextBlock) block).getText(),
                                        lastTextContent,
                                        hasPrintedTextHeader,
                                        "回答: ",
                                        () -> {
                                            // 在思考和回答之间添加分隔
                                            if (hasPrintedThinkingHeader.get()
                                                    && !hasPrintedTextSeparator.get()) {
                                                System.out.print("\n\n");
                                                hasPrintedTextSeparator.set(true);
                                            }
                                        });
                            }
                        }
                    })
                    .blockLast(); // 等待流式输出完成

        } catch (Exception e) {
            // 流式调用失败时的降级处理
            System.err.println("\n[警告] 流式输出失败，尝试普通调用...");
            System.err.println("       原因：" + e.getMessage());

            try {
                // 降级为普通调用
                Msg response = agent.call(userMsg).block();
                if (response != null) {
                    printNonStreamResponse(response);
                }
            } catch (Exception ex) {
                System.err.println("[错误] 调用失败：" + ex.getMessage());
            }
        }
    }

    /**
     * 打印非流式响应。
     *
     * @param response 响应消息
     */
    private static void printNonStreamResponse(Msg response) {
        // 提取思考内容
        String thinking = response.getContent().stream()
                .filter(block -> block instanceof ThinkingBlock)
                .map(block -> ((ThinkingBlock) block).getThinking())
                .collect(Collectors.joining("\n"));

        // 提取文本内容
        String text = response.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .collect(Collectors.joining("\n"));

        boolean hasContent = false;
        if (!thinking.isEmpty()) {
            System.out.print("> 思考: " + thinking);
            hasContent = true;
        }
        if (!text.isEmpty()) {
            if (hasContent) {
                System.out.print("\n\n");
            }
            System.out.print("回答: " + text);
        }
        if (!hasContent) {
            System.out.print("[无响应]");
        }
    }

    /**
     * 打印流式内容（增量模式）。
     *
     * @param content          当前内容
     * @param lastContentRef   上次内容引用
     * @param hasPrintedHeaderRef 是否已打印标题引用
     * @param header           标题前缀
     * @param prePrintAction   打印前的操作
     */
    private static void printStreamContent(
            String content,
            AtomicReference<String> lastContentRef,
            AtomicBoolean hasPrintedHeaderRef,
            String header,
            Runnable prePrintAction) {
        String lastContent = lastContentRef.get();
        String toPrint;

        // 检测是增量模式还是累积模式
        if (content.startsWith(lastContent)) {
            // 累积模式：只打印新增部分
            toPrint = content.substring(lastContent.length());
            lastContentRef.set(content);
        } else {
            // 增量模式：直接打印
            toPrint = content;
            lastContentRef.set(lastContent + content);
        }

        if (!toPrint.isEmpty()) {
            // 执行打印前操作（如添加分隔）
            if (prePrintAction != null) {
                prePrintAction.run();
            }

            // 打印标题（仅第一次）
            if (!hasPrintedHeaderRef.get()) {
                System.out.print(header);
                hasPrintedHeaderRef.set(true);
            }

            // 打印内容并刷新输出缓冲
            System.out.print(toPrint);
            System.out.flush();
        }
    }

    /**
     * 脱敏显示 API Key（显示首尾各 4 位，中间用 *** 替代）。
     *
     * @param apiKey 原始 API Key
     * @return 脱敏后的字符串
     */
    private static String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
```

---

### 2.5.6 运行方式

**1. 编译项目**

```bash
cd chapter02-quickstart
mvn clean compile
```

**2. 设置 API Key**

```bash
# Linux / macOS
export DASHSCOPE_API_KEY=your-actual-api-key

# Windows PowerShell
$env:DASHSCOPE_API_KEY="your-actual-api-key"
```

**3. 运行程序**

```bash
mvn exec:java -Dexec.mainClass="io.agentscope.tutorial.chapter02.App"
```

**4. 预期输出**

```
╔════════════════════════════════════════════════════════════╗
║        AgentScope Java - 第二章：快速开始                  ║
║        你的第一个 AI Agent                                 ║
╚════════════════════════════════════════════════════════════╝

[INFO] API Key 已配置（已脱敏显示）
       sk-****abcd

[INFO] Agent 创建成功！

=== 对话开始 ===
输入 'exit' 或 'quit' 退出对话

你> 你好，请介绍一下你自己
Agent> > 思考: 这个问题要求我介绍自己，我需要说明我是基于通义千问的AI助手...
回答: 你好！我是基于通义千问大模型的AI助手...

你> exit
再见！
```

---

## 2.6 本章小结

本章学习了以下内容：

| 知识点 | 说明 |
|--------|------|
| Maven 依赖 | 通过 `agentscope-core` 引入框架核心能力 |
| ReActAgent | 基于 ReAct 范式的 Agent 实现 |
| 模型配置 | 以 DashScope 为例，配置模型客户端 |
| 执行方法 | `call()`（同步）和 `stream()`（流式）两种调用方式 |
| 完整案例 | 包含配置类、Agent 类和运行入口的完整项目 |

### 常见问题

**Q: 为什么我的 Agent 没有响应？**
- 检查 API Key 是否正确配置
- 确认网络可以访问阿里云百炼服务
- 查看日志是否有错误信息

**Q: 如何实现流式输出？**
- 使用 `agent.stream()` 方法，配合 `StreamOptions` 配置
- 需要模型服务端支持流式输出

**Q: 如何保存对话历史？**
- 当前使用 `InMemoryMemory`，程序结束后会丢失
- 如需持久化，可参考第三章「消息与会话管理」

---

## 下一步

- 第三章：消息与会话管理 - 学习消息格式、会话管理和消息格式化器
- 第四章：ReAct Agent 详解 - 深入了解 Agent 的配置和生命周期
- 第五章：工具系统 - 为 Agent 添加工具调用能力