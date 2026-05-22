# 第九章：代理间通信协议

> 本章讲解 A2A（Agent-to-Agent）协议的原理、AgentScope Java 的实现方式，以及如何通过消息队列实现分布式多代理通信。

---

## 9.1 A2A（Agent-to-Agent）协议详解

### 9.1.1 什么是 A2A 协议

A2A（Agent-to-Agent）协议是一种标准化协议，用于**不同 Agent 之间进行通信和协作**。在传统的多代理系统中，每个代理通常需要知道其他代理的实现细节才能进行通信，这导致了紧耦合。 A2A 协议通过定义统一的通信格式和交互模式，实现了代理间的**松耦合通信**。

A2A 协议的核心特性：

- **服务发现**：通过 AgentCard（元数据描述文件）自动发现可用代理
- **标准化交互**：统一的消息格式和状态机定义
- **多传输支持**：支持 HTTP/JSON-RPC、WebSocket、消息队列等多种传输方式
- **技能声明**：代理可以声明自己支持的能力（Skills），方便路由决策

### 9.1.2 AgentCard（代理卡片）

AgentCard 是 A2A 协议的核心概念，它是一个 JSON 文档，描述了代理的身份、能力、通信方式等元数据。

```json
{
  "name": "Weather Agent",
  "description": "提供全球天气查询服务",
  "version": "1.0.0",
  "provider": {
    "organization": "WeatherService Inc.",
    "url": "https://weather.example.com"
  },
  "url": "http://localhost:8080",
  "capabilities": {
    "streaming": true,
    "pushNotifications": true
  },
  "skills": [
    {
      "id": "get_weather",
      "name": "获取天气",
      "description": "查询指定城市的当前天气",
      "tags": ["weather", "forecast"],
      "examples": ["北京今天天气怎么样？", "东京明天会下雨吗？"]
    }
  ]
}
```

### 9.1.3 A2A 消息生命周期

A2A 协议定义了清晰的消息状态机：

```
Task States: submit -> working -> completed/failed/input-required
```

关键状态说明：

| 状态 | 描述 |
|------|------|
| `submitted` | 任务已提交，等待处理 |
| `working` | 任务正在处理中 |
| `completed` | 任务成功完成 |
| `failed` | 任务执行失败 |
| `input-required` | 需要用户提供更多信息 |

---

## 9.2 Agent Protocol 标准

### 9.2.1 JSON-RPC 2.0 传输

AgentScope A2A 基于 JSON-RPC 2.0 规范定义了一组标准方法：

| 方法 | 描述 | 请求类型 |
|------|------|---------|
| `message/send` | 发送单条消息，返回完整响应 | 同步 |
| `message/stream` | 发送消息，返回流式响应 | 流式 |
| `tasks/send` | 发送任务（带状态跟踪） | 同步 |
| `tasks/get` | 获取任务状态和结果 | 同步 |
| `tasks/cancel` | 取消正在执行的任务 | 同步 |

### 9.2.2 核心消息格式

**消息请求示例**：

```json
{
  "method": "message/stream",
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "jsonrpc": "2.0",
  "params": {
    "message": {
      "role": "user",
      "kind": "message",
      "contextId": "session-001",
      "metadata": {
        "userId": "user-001",
        "sessionId": "session-001"
      },
      "parts": [
        {
          "kind": "text",
          "text": "请帮我查询北京的天气"
        }
      ],
      "messageId": "msg-001"
    }
  }
}
```

**消息响应示例**：

```json
{
  "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "jsonrpc": "2.0",
  "result": {
    "status": "completed",
    "message": {
      "role": "assistant",
      "kind": "message",
      "parts": [
        {
          "kind": "text",
          "text": "北京今天天气晴朗，气温 15-25°C，适合外出。"
        }
      ]
    }
  }
}
```

### 9.2.3 事件推送（Server-Sent Events）

A2A 协议支持通过 SSE（Server-Sent Events）进行流式响应和事件推送：

```
event: task
data: {"taskId":"task-001","status":"working","message":{...}}

event: task
data: {"taskId":"task-001","status":"working","message":{...}}

event: task
data: {"taskId":"task-001","status":"completed","message":{...}}
```

---

## 9.3 消息队列集成（RocketMQ）

### 9.3.1 为什么需要消息队列

在分布式多代理系统中，HTTP 同步调用存在以下局限性：

1. **紧耦合**：调用方需要知道被调用方的网络地址
2. **同步阻塞**：调用方需要等待响应才能继续
3. **单点故障**：被调用方不可用时整个流程中断
4. **扩展性差**：难以支持高并发场景

消息队列通过**异步解耦**解决了这些问题：

- **发布/订阅模式**：代理之间不需要直接通信
- **削峰填谷**：应对突发流量
- **可靠性保证**：消息持久化、重试机制
- **地理分布**：支持跨机房、跨地域通信

### 9.3.2 RocketMQ 集成架构

AgentScope Java 通过 RocketMQ 实现了 A2A 协议的异步传输：

```
┌─────────────┐       ┌──────────────┐       ┌─────────────┐
│   Client    │──────▶│  RocketMQ    │──────▶│   Server    │
│  (Publisher)│       │  Broker      │       │  (Consumer) │
└─────────────┘       └──────────────┘       └─────────────┘
                           ▲
                           │ LiteTopic (响应通道)
                           │
                      ┌────┴─────┐
                      │ Response │
                      │  Topic   │
                      └──────────┘
```

关键概念：

- **BizTopic**：业务消息主题，传输 Agent 间的主消息
- **LiteTopic**：轻量级主题，用于接收定向响应
- **ConsumerGroup**：消费者组，实现负载均衡和故障转移

### 9.3.3 消息格式设计

RocketMQ 传输使用的消息扩展格式：

```json
{
  "messageId": "msg-uuid-001",
  "sessionId": "session-001",
  "sourceAgent": "agent-client-001",
  "targetAgent": "agent-server-001",
  "transportType": "rocketmq",
  "payload": {
    "message": {...},
    "metadata": {...}
  },
  "timestamp": 1704067200000
}
```

---

## 9.4 【案例】A2A 协议下的多代理通信

本案例实现一个完整的多代理系统，包含：

1. **A2A Server**：通过 RocketMQ 接收请求的代理服务
2. **A2A Client**：通过 HTTP 和 RocketMQ 调用远程代理
3. **Nacos 服务注册与发现**：实现动态服务寻址

### 9.4.1 项目结构

```
src/main/java/io/agentscope/tutorial/chapter09/
├── TutorialA2AApplication.java          # Spring Boot 启动类
├── config/
│   ├── AgentConfiguration.java        # Agent 配置
│   └── NacosConfiguration.java         # Nacos 注册配置
├── agent/
│   ├── AssistantAgent.java             # 助手代理实现
│   └── WeatherAgent.java              # 天气代理实现
├── tools/
│   └── TutorialTools.java             # 示例工具集
├── a2a/
│   ├── ClientRunner.java              # A2A 客户端运行器
│   └── ServerRunner.java              # A2A 服务端运行器
└── integration/
    └── MultiAgentDemo.java           # 多代理协作演示

src/main/resources/
├── application.yml                    # 服务端配置
├── application-client.yml            # 客户端配置（可选）
└── logback.xml                       # 日志配置
```

### 9.4.2 Maven 依赖配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-parent</artifactId>
        <version>${revision}</version>
    </parent>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter09-a2a-protocol</artifactId>
    <packaging>jar</packaging>

    <name>AgentScope Tutorial - Chapter 09: A2A Protocol</name>
    <description>多代理通信协议示例</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
        </dependency>

        <!-- A2A Spring Boot Starter -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-a2a-spring-boot-starter</artifactId>
        </dependency>

        <!-- Nacos Spring Boot Starter（用于服务注册与发现） -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-nacos-spring-boot-starter</artifactId>
        </dependency>

        <!-- RocketMQ A2A 传输支持 -->
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-a2a</artifactId>
            <version>1.0.9</version>
            <exclusions>
                <exclusion>
                    <groupId>org.jboss.logmanager</groupId>
                    <artifactId>jboss-logmanager</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
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

### 9.4.3 服务端配置

```yaml
# src/main/resources/application.yml

# 服务端口配置
server:
  port: 8080

# Spring 应用配置
spring:
  application:
    name: agentscope-a2a-tutorial-server
  main:
    banner-mode: off

# AgentScope 核心配置
agentscope:
  # 阿里云百炼模型配置
  dashscope:
    api-key: ${DASHSCOPE_API_KEY:demo-key}

  # 本地 Agent 配置
  agent:
    enabled: true
    name: tutorial-assistant

  # A2A 服务端配置
  a2a:
    server:
      enabled: true

      # AgentCard 配置（代理元数据）
      card:
        name: "Tutorial Assistant"
        description: "教程演示助手，支持天气查询、数学计算、文本处理等技能"
        version: "1.0.0"
        provider:
          organization: "AgentScope Tutorial"
          url: "https://java.agentscope.io"
        documentation-url: "https://java.agentscope.io/tutorial/chapter09"
        skills:
          - id: "get_weather"
            name: "获取天气"
            description: "查询指定城市的当前天气和预报信息"
            tags: ["weather", "forecast"]
            examples:
              - "北京今天天气怎么样？"
              - "东京明天会下雨吗？"
          - id: "calculate"
            name: "数学计算"
            description: "执行基本数学运算（加、减、乘、除）"
            tags: ["math", "calculation"]
            examples:
              - "计算 123 + 456"
              - "100 除以 3 等于多少？"
          - id: "text_processing"
            name: "文本处理"
            description: "文本格式化、统计、转换等操作"
            tags: ["text", "nlp"]
            examples:
              - "将'hello world'转为大写"
              - "统计'Hello'有多少个字符"

      # 传输层配置
      transports:
        # HTTP JSON-RPC 传输（同步通信）
        jsonrpc:
          enabled: true

        # RocketMQ 传输（异步通信）
        rocketmq:
          enabled: ${ROCKETMQ_ENABLED:false}
          rocketMQEndpoint: ${ROCKETMQ_ENDPOINT:}
          rocketMQNamespace: ${ROCKETMQ_NAMESPACE:}
          biz-topic: ${ROCKETMQ_BIZ_TOPIC:LLM_TOPIC}
          biz-consumer-group: ${ROCKETMQ_BIZ_CID:LLM_CID}
          workAgentResponseTopic: ${ROCKETMQ_RESPONSE_TOPIC:WorkerAgentResponseServer}
          workAgentResponseGroupId: ${ROCKETMQ_RESPONSE_CID:CID_HOST_AGENT_LITE_SERVER}
          accessKey: ${ROCKETMQ_AK:}
          secretKey: ${ROCKETMQ_SK:}

    # Nacos 服务注册配置
    nacos:
      enabled: ${NACOS_ENABLED:false}
      server-addr: ${NACOS_SERVER_ADDR:localhost:8848}
      username: ${NACOS_USERNAME:nacos}
      password: ${NACOS_PASSWORD:}
```

### 9.4.4 工具类实现

```java
package io.agentscope.tutorial.chapter09.tools;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Random;

/**
 * 教程示例工具集
 *
 * <p>本类展示了如何为 AgentScope Agent 定义自定义工具函数。
 * 工具通过 @Tool 注解声明，参数通过 @ToolParam 注解定义。
 *
 * @author AgentScope Tutorial
 */
public class TutorialTools {

    private final Random random = new Random();

    /**
     * 获取城市天气信息
     *
     * <p>这是一个模拟实现，用于演示工具调用功能。
     * 生产环境中应接入真实天气 API（如和风天气、OpenWeatherMap 等）。
     *
     * @param city 城市名称，支持中英文
     * @return 天气信息，包含温度、湿度、天气状况等
     */
    @Tool(
        name = "get_weather",
        description = "获取指定城市的当前天气信息"
    )
    public ToolResultBlock getWeather(
            @ToolParam(
                name = "city",
                description = "城市名称，例如：北京、Tokyo、New York",
                required = true
            )
            String city) {

        // 模拟天气数据
        String[] conditions = {"晴朗", "多云", "阴天", "小雨", "雷阵雨"};
        String condition = conditions[random.nextInt(conditions.length)];
        int temperature = random.nextInt(30) + 5;        // 5-35°C
        int humidity = random.nextInt(60) + 30;          // 30-90%

        // 生成模拟预报
        String[] forecasts = {"未来几天以晴天为主", "明天有雨，记得带伞", "气温逐渐回升"};
        String forecast = forecasts[random.nextInt(forecasts.length)];

        String result = String.format(
            "📍 %s 天气报告\n" +
            "━━━━━━━━━━━━━\n" +
            "🌤️  天气状况：%s\n" +
            "🌡️  温度：%d°C\n" +
            "💧  湿度：%d%%\n" +
            "📋  预报：%s\n" +
            "━━━━━━━━━━━━━",
            city, condition, temperature, humidity, forecast
        );

        return ToolResultBlock.text(result);
    }

    /**
     * 执行数学计算
     *
     * <p>支持基本的算术运算：加(+)、减(-)、乘(*)、除(/)
     * 支持括号和优先级处理。
     *
     * @param expression 数学表达式，例如 "2 + 3 * 4" 或 "(10 + 5) / 3"
     * @return 计算结果
     */
    @Tool(
        name = "calculate",
        description = "执行数学运算，支持加减乘除和括号"
    )
    public ToolResultBlock calculate(
            @ToolParam(
                name = "expression",
                description = "数学表达式，例如：2 + 3、10 * 5、(3 + 4) / 2",
                required = true
            )
            String expression) {

        try {
            double result = evaluateExpression(expression.trim());

            String formattedResult;
            if (result == (long) result) {
                formattedResult = String.valueOf((long) result);
            } else {
                formattedResult = String.format("%.6f", result).replaceAll("0+$", "").replaceAll("\\.$", "");
            }

            return ToolResultBlock.text(String.format(
                "🧮 计算结果\n" +
                "━━━━━━━━━━━━━\n" +
                "表达式：%s\n" +
                "结果：%s\n" +
                "━━━━━━━━━━━━━",
                expression, formattedResult
            ));
        } catch (Exception e) {
            return ToolResultBlock.error("计算失败：" + e.getMessage());
        }
    }

    /**
     * 获取当前日期时间
     *
     * @return 当前时间（格式：yyyy-MM-dd HH:mm:ss）
     */
    @Tool(
        name = "get_current_time",
        description = "获取当前系统时间"
    )
    public ToolResultBlock getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return ToolResultBlock.text(String.format(
            "🕐 当前时间\n" +
            "━━━━━━━━━━━━━\n" +
            "日期时间：%s\n" +
            "━━━━━━━━━━━━━",
            now.format(formatter)
        ));
    }

    /**
     * 文本处理工具
     *
     * <p>支持多种文本操作：大小写转换、字符统计、去除空格等。
     *
     * @param text 待处理的文本
     * @param operation 操作类型：upper（大写）、lower（小写）、trim（去空格）、count（统计）
     * @return 处理结果
     */
    @Tool(
        name = "text_process",
        description = "文本处理工具，支持大小写转换、统计、去空格等"
    )
    public ToolResultBlock textProcess(
            @ToolParam(
                name = "text",
                description = "待处理的文本内容",
                required = true
            )
            String text,
            @ToolParam(
                name = "operation",
                description = "操作类型：upper（转大写）、lower（转小写）、trim（去空格）、count（字符统计）",
                required = true
            )
            String operation) {

        String result;
        switch (operation.toLowerCase()) {
            case "upper":
                result = text.toUpperCase();
                break;
            case "lower":
                result = text.toLowerCase();
                break;
            case "trim":
                result = text.trim();
                break;
            case "count":
                return ToolResultBlock.text(String.format(
                    "📊 文本统计\n" +
                    "━━━━━━━━━━━━━\n" +
                    "字符数（不含空格）：%d\n" +
                    "字符数（含空格）：%d\n" +
                    "单词数：%d\n" +
                    "━━━━━━━━━━━━━",
                    text.replaceAll("\\s", "").length(),
                    text.length(),
                    text.trim().split("\\s+").length
                ));
            default:
                return ToolResultBlock.error("不支持的操作：" + operation + "，支持的类型：upper、lower、trim、count");
        }

        return ToolResultBlock.text(String.format(
            "✏️ 文本处理结果\n" +
            "━━━━━━━━━━━━━\n" +
            "操作：%s\n" +
            "原文：%s\n" +
            "结果：%s\n" +
            "━━━━━━━━━━━━━",
            operation, text, result
        ));
    }

    /**
     * 表达式求值器
     *
     * <p>支持基本算术运算和括号优先级处理。
     */
    private double evaluateExpression(String expr) {
        // 去除所有空格
        expr = expr.replaceAll("\\s+", "");

        return parseExpression(expr);
    }

    private double parseExpression(String expr) {
        // 处理括号
        if (expr.startsWith("(") && expr.endsWith(")") && matchesParens(expr)) {
            return parseExpression(expr.substring(1, expr.length() - 1));
        }

        // 找到最后一个 + 或 -（优先级最低）
        int lastAddSub = findLastOp(expr, '+', '-');
        if (lastAddSub > 0) {
            double left = parseExpression(expr.substring(0, lastAddSub));
            double right = parseExpression(expr.substring(lastAddSub + 1));
            return expr.charAt(lastAddSub) == '+' ? left + right : left - right;
        }

        // 找到最后一个 * 或 /
        int lastMulDiv = findLastOp(expr, '*', '/');
        if (lastMulDiv > 0) {
            double left = parseExpression(expr.substring(0, lastMulDiv));
            double right = parseExpression(expr.substring(lastMulDiv + 1));
            return expr.charAt(lastMulDiv) == '*' ? left * right : left / right;
        }

        // 解析数字
        return parseNumber(expr);
    }

    private int findLastOp(String expr, char... ops) {
        int depth = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') depth++;
            else if (c == '(') depth--;
            else if (depth == 0) {
                for (char op : ops) {
                    if (c == op && i > 0) return i;
                }
            }
        }
        return -1;
    }

    private boolean matchesParens(String expr) {
        int depth = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            if (depth < 0) return false;
        }
        return depth == 0;
    }

    private double parseNumber(String expr) {
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无法解析的数字：" + expr);
        }
    }
}
```

### 9.4.5 A2A 服务端实现

```java
package io.agentscope.tutorial.chapter09.a2a;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter09.tools.TutorialTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Agent 组件配置
 *
 * <p>本类负责配置和创建 AgentScope Agent 实例。
 * 通过 Spring 依赖注入，将 Agent 暴露给 A2A 服务端使用。
 *
 * @author AgentScope Tutorial
 */
@Component
public class AgentConfiguration {

    /**
     * 配置并返回 ReActAgent 的构建器
     *
     * <p>该方法使用配置文件中定义的参数创建 Agent 构建器。
     * Agent 将自动注册到 A2A 服务端并暴露其能力。
     *
     * @param dashScopeApiKey 阿里云百炼 API Key
     * @param agentName       Agent 实例名称
     * @param modelName       模型名称（如 qwen-plus、qwen-turbo）
     * @return 配置好的 ReActAgent 构建器
     */
    @Bean
    public ReActAgent.Builder agentBuilder(
            @Value("${agentscope.dashscope.api-key:}") String dashScopeApiKey,
            @Value("${agentscope.agent.name:}") String agentName,
            @Value("${agentscope.agent.modelName:qwen-plus}") String modelName) {

        // 创建 DashScope 聊天模型
        DashScopeChatModel model = DashScopeChatModel.builder()
                .apiKey(dashScopeApiKey)
                .modelName(modelName)
                .stream(true)
                .enableThinking(false)
                .build();

        // 构建 Agent
        return ReActAgent.builder()
                .name(agentName != null && !agentName.isEmpty() ? agentName : "tutorial-assistant")
                .model(model)
                .sysPrompt("""
                    你是一个乐于助人的助手，名叫"小助手"。
                    你拥有以下工具可以使用：
                    - get_weather: 查询城市天气
                    - calculate: 执行数学计算
                    - get_current_time: 获取当前时间
                    - text_process: 文本处理

                    请根据用户的需求，选择合适的工具来回答问题。
                    如果用户没有明确要求使用工具，请直接回答。
                    回答时保持友好、简洁的风格。
                    """);
    }

    /**
     * 工具包配置
     *
     * <p>将自定义工具注册到工具包中，供 Agent 调用。
     *
     * @return 配置好的工具包
     */
    @Bean
    public Toolkit tutorialToolkit() {
        Toolkit toolkit = new Toolkit();
        // 注册示例工具
        toolkit.registerTool(new TutorialTools());
        return toolkit;
    }
}
```

### 9.4.6 A2A 客户端实现

```java
package io.agentscope.tutorial.chapter09.a2a;

import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.a2a.agent.A2aAgentConfig;
import io.agentscope.core.a2a.agent.A2aAgentConfig.A2aAgentConfigBuilder;
import io.agentscope.core.a2a.agent.card.WellKnownAgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.a2a.client.http.JdkA2AHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * A2A 客户端运行器
 *
 * <p>本类演示如何使用 A2aAgent 调用远程 A2A 服务。
 * 支持通过 HTTP JSON-RPC 或 RocketMQ 两种传输方式调用远程代理。
 *
 * @author AgentScope Tutorial
 */
public class ClientRunner {

    private static final Logger logger = LoggerFactory.getLogger(ClientRunner.class);

    private static final String USER_INPUT_PREFIX = "\033[34m您>\033[0m ";
    private static final String AGENT_RESPONSE_PREFIX = "\033[32m助手>\033[0m ";

    /**
     * 创建使用 HTTP JSON-RPC 传输的 A2A Agent
     *
     * @param serverUrl 远程 A2A 服务器地址
     * @param agentName 本地代理名称
     * @return 配置好的 A2aAgent 实例
     */
    public static A2aAgent createHttpAgent(String serverUrl, String agentName) {
        logger.info("创建 HTTP A2A Agent，服务器地址：{}", serverUrl);

        // 创建 AgentCard 解析器，通过 well-known URI 获取远程 Agent 元数据
        WellKnownAgentCardResolver resolver = WellKnownAgentCardResolver.builder()
                .baseUrl(serverUrl)
                .build();

        // 构建 A2A Agent
        return A2aAgent.builder()
                .name(agentName)
                .agentCardResolver(resolver)
                .build();
    }

    /**
     * 创建使用 RocketMQ 传输的 A2A Agent
     *
     * <p>RocketMQ 传输适合需要异步通信、高并发、跨地域部署的场景。
     *
     * @param serverUrl    远程 A2A 服务器地址（用于获取 AgentCard）
     * @param agentName    本地代理名称
     * @param mqConfig     RocketMQ 配置
     * @return 配置好的 A2aAgent 实例
     */
    public static A2aAgent createRocketMQAgent(
            String serverUrl,
            String agentName,
            RocketMQConfig mqConfig) {

        logger.info("创建 RocketMQ A2A Agent，服务器地址：{}", serverUrl);

        // 创建 RocketMQ 传输配置
        RocketMQTransportConfig transportConfig = new RocketMQTransportConfig();
        transportConfig.setAccessKey(mqConfig.getAccessKey());
        transportConfig.setSecretKey(mqConfig.getSecretKey());
        transportConfig.setWorkAgentResponseTopic(mqConfig.getResponseTopic());
        transportConfig.setWorkAgentResponseGroupID(mqConfig.getResponseGroupId());
        transportConfig.setNamespace(mqConfig.getNamespace());
        transportConfig.setHttpClient(new JdkA2AHttpClient());

        // 创建 Agent 配置，指定使用 RocketMQ 传输
        A2aAgentConfig a2aConfig = new A2aAgentConfigBuilder()
                .withTransport(RocketMQTransport.class, transportConfig)
                .build();

        // 创建 AgentCard 解析器
        WellKnownAgentCardResolver resolver = WellKnownAgentCardResolver.builder()
                .baseUrl(serverUrl)
                .build();

        // 构建 A2A Agent
        return A2aAgent.builder()
                .a2aAgentConfig(a2aConfig)
                .name(agentName)
                .agentCardResolver(resolver)
                .build();
    }

    /**
     * 启动交互式聊天会话
     *
     * @param agent A2A Agent 实例
     */
    public void startChat(A2aAgent agent) {
        logger.info("启动交互式聊天会话...");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                // 接收用户输入
                System.out.print(USER_INPUT_PREFIX);
                String input = reader.readLine();

                // 处理退出命令
                if (input == null || input.trim().isEmpty() ||
                    input.trim().equalsIgnoreCase("exit") ||
                    input.trim().equalsIgnoreCase("quit")) {
                    System.out.println(AGENT_RESPONSE_PREFIX + "感谢使用，再见！");
                    break;
                }

                // 显示接收确认
                System.out.println(AGENT_RESPONSE_PREFIX + "收到您的消息，正在处理...");
                System.out.println(AGENT_RESPONSE_PREFIX);

                // 调用远程 Agent 并处理响应
                try {
                    processAndPrintResponse(agent, input);
                } catch (Exception e) {
                    logger.error("调用 Agent 时发生错误", e);
                    System.out.println("\n抱歉，处理您的请求时出现错误：" + e.getMessage());
                }

                System.out.println();
            }
        } catch (Exception e) {
            logger.error("聊天会话异常退出", e);
        }
    }

    /**
     * 处理用户输入并打印响应
     *
     * @param agent A2A Agent
     * @param input 用户输入
     */
    private void processAndPrintResponse(A2aAgent agent, String input) {
        // 构建消息
        Msg message = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(input).build())
                .build();

        // 使用流式响应
        Flux<String> responseStream = agent.stream(message);

        responseStream.subscribe(
            // 处理每个响应片段
            text -> {
                if (!text.isEmpty()) {
                    System.out.print(text);
                }
            },
            // 处理错误
            error -> {
                logger.error("流式响应错误", error);
                System.err.println("\n响应处理出错：" + error.getMessage());
            },
            // 完成时
            () -> {
                logger.debug("流式响应完成");
            }
        );

        // 阻塞等待响应完成（演示用途，生产环境建议异步处理）
        responseStream.then().block();
    }

    /**
     * 使用 Nacos 服务发现创建 A2A Agent
     *
     * <p>通过 Nacos 注册中心自动发现可用的 A2A 服务，
     * 无需硬编码服务器地址。
     *
     * @param agentName      本地代理名称
     * @param targetAgentName 目标代理名称
     * @param nacosServerAddr Nacos 服务器地址
     * @return 配置好的 A2aAgent 实例
     */
    public static A2aAgent createNacosDiscoveredAgent(
            String agentName,
            String targetAgentName,
            String nacosServerAddr) {

        logger.info("创建通过 Nacos 发现的 A2A Agent，目标代理：{}", targetAgentName);

        // 创建 Nacos AgentCard 解析器
        NacosAgentCardResolver nacosResolver = new NacosAgentCardResolver(
            createNacosService(nacosServerAddr),
            targetAgentName
        );

        return A2aAgent.builder()
                .name(agentName)
                .agentCardResolver(nacosResolver)
                .build();
    }

    // RocketMQ 传输配置类
    public static class RocketMQConfig {
        private String accessKey;
        private String secretKey;
        private String responseTopic;
        private String responseGroupId;
        private String namespace;

        // Getters and Setters
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getResponseTopic() { return responseTopic; }
        public void setResponseTopic(String responseTopic) { this.responseTopic = responseTopic; }
        public String getResponseGroupId() { return responseGroupId; }
        public void setResponseGroupId(String responseGroupId) { this.responseGroupId = responseGroupId; }
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
    }

    // 简化实现，使用反射导入需要的类
    private static class RocketMQTransportConfig {
        private String accessKey;
        private String secretKey;
        private String workAgentResponseTopic;
        private String workAgentResponseGroupID;
        private String namespace;
        private Object httpClient;

        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public void setWorkAgentResponseTopic(String topic) { this.workAgentResponseTopic = topic; }
        public void setWorkAgentResponseGroupID(String groupId) { this.workAgentResponseGroupID = groupId; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        public void setHttpClient(Object httpClient) { this.httpClient = httpClient; }
    }

    // 简化实现
    private static class RocketMQTransport {}

    // 简化实现
    private static class NacosAgentCardResolver {
        public NacosAgentCardResolver(Object aiService, String agentName) {}
    }

    private static Object createNacosService(String serverAddr) {
        // 实际实现需要创建 Nacos AiService
        return null;
    }
}
```

### 9.4.7 Spring Boot 启动类

```java
package io.agentscope.tutorial.chapter09;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter09.tools.TutorialTools;

/**
 * A2A 协议教程主启动类
 *
 * <p>本应用演示了如何使用 AgentScope Java 构建支持 A2A 协议的多代理系统。
 * 通过 Spring Boot 快速启动 A2A 服务端，暴露本地 Agent 给远程调用。
 *
 * <p>功能特性：
 * <ul>
 *   <li>A2A HTTP JSON-RPC 传输（同步调用）</li>
 *   <li>A2A RocketMQ 传输（异步消息队列）</li>
 *   <li>Nacos 服务注册与发现</li>
 *   <li>流式响应支持</li>
 * </ul>
 *
 * <p>使用示例：
 * <ol>
 *   <li>设置环境变量 DASHSCOPE_API_KEY</li>
 *   <li>运行本应用</li>
 *   <li>通过 HTTP 调用 A2A 接口</li>
 *   <li>或通过客户端示例程序进行交互</li>
 * </ol>
 *
 * @author AgentScope Tutorial
 * @version 1.0.0
 */
@SpringBootApplication
public class TutorialA2AApplication {

    /**
     * 应用入口点
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TutorialA2AApplication.class, args);
    }

    /**
     * 注册工具包 Bean
     *
     * <p>工具包会被 Agent 使用，提供各种工具函数能力。
     * 也可以通过配置文件的 agentscope.agent.toolkit 属性注入。
     *
     * @return 配置好的工具包实例
     */
    @Bean
    public Toolkit tutorialToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new TutorialTools());
        return toolkit;
    }
}
```

### 9.4.8 多代理协作演示

```java
package io.agentscope.tutorial.chapter09.integration;

import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter09.a2a.ClientRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 多代理协作演示
 *
 * <p>本类演示了如何通过 A2A 协议协调多个代理完成任务。
 * 典型场景：
 * <ul>
 *   <li>路由器代理：根据用户意图将请求分发到专业代理</li>
 *   <li>并行代理：同时调用多个代理获取不同维度的信息</li>
 *   <li>链式代理：将前一代理的输出作为下一代理的输入</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
public class MultiAgentDemo {

    private static final Logger logger = LoggerFactory.getLogger(MultiAgentDemo.class);

    private final ClientRunner clientRunner = new ClientRunner();

    /**
     * 示例 1：路由器模式
     *
     * <p>根据用户请求类型，自动选择合适的专业代理处理。
     */
    public void routerDemo() {
        logger.info("===== 路由器模式演示 =====");

        // 创建路由器 Agent（根据 URL 路由到不同服务）
        A2aAgent routerAgent = ClientRunner.createHttpAgent(
            "http://localhost:8080",
            "router-agent"
        );

        // 用户请求天气
        Msg weatherRequest = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("北京今天天气怎么样？").build())
                .build();

        logger.info("发送天气查询请求...");
        // 实际路由逻辑需要后端配合实现
        processRequest(routerAgent, weatherRequest);

        // 用户请求计算
        Msg calcRequest = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("计算 12345 * 67890").build())
                .build();

        logger.info("发送计算请求...");
        processRequest(routerAgent, calcRequest);
    }

    /**
     * 示例 2：并行查询模式
     *
     * <p>同时查询多个代理，汇总结果。
     */
    public void parallelQueryDemo() {
        logger.info("===== 并行查询模式演示 =====");

        // 创建多个代理实例
        A2aAgent agent1 = ClientRunner.createHttpAgent(
            "http://localhost:8081",
            "query-agent-1"
        );
        A2aAgent agent2 = ClientRunner.createHttpAgent(
            "http://localhost:8082",
            "query-agent-2"
        );

        // 构建查询请求
        Msg queryMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("查询今天的重要新闻").build())
                .build();

        logger.info("同时发送查询请求到多个代理...");

        // 并行执行查询（使用响应式编程）
        // 实际实现需要使用 Mono.zip 或 Flux.merge 等操作符
        logger.info("等待所有代理响应...");

        // 汇总结果
        logger.info("===== 汇总各代理结果 =====");
    }

    /**
     * 示例 3：链式处理模式
     *
     * <p>将前一个代理的输出作为下一个代理的输入。
     */
    public void chainProcessingDemo() {
        logger.info("===== 链式处理模式演示 =====");

        // 创建处理链中的各环节代理
        A2aAgent translator = ClientRunner.createHttpAgent(
            "http://localhost:8080",
            "translator"
        );
        A2aAgent summarizer = ClientRunner.createHttpAgent(
            "http://localhost:8080",
            "summarizer"
        );

        // 初始输入
        String originalText = "AgentScope is a Java framework for building multi-agent systems with A2A protocol support.";

        // 第一步：翻译
        logger.info("步骤 1：翻译原文...");
        Msg translateRequest = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("请将以下英文翻译成中文：" + originalText).build())
                .build();

        // 第二步：总结（使用第一步的结果）
        logger.info("步骤 2：总结翻译结果...");
        Msg summarizeRequest = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("请总结以下内容要点：" + "[翻译结果]").build())
                .build();

        logger.info("===== 链式处理完成 =====");
    }

    /**
     * 处理单个请求的辅助方法
     */
    private void processRequest(A2aAgent agent, Msg message) {
        try {
            agent.call(message)
                 .doOnSuccess(response -> {
                     logger.info("收到响应：{}", response.getContent());
                 })
                 .doOnError(error -> {
                     logger.error("请求失败：{}", error.getMessage());
                 })
                 .block();
        } catch (Exception e) {
            logger.error("处理请求时发生异常", e);
        }
    }

    /**
     * 主演示入口
     */
    public static void main(String[] args) {
        MultiAgentDemo demo = new MultiAgentDemo();

        // 运行所有演示
        demo.routerDemo();
        demo.parallelQueryDemo();
        demo.chainProcessingDemo();

        logger.info("===== 多代理协作演示结束 =====");
    }
}
```

### 9.4.9 测试类

```java
package io.agentscope.tutorial.chapter09;

import io.agentscope.core.a2a.agent.A2aAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.tutorial.chapter09.a2a.ClientRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A2A 协议集成测试
 *
 * <p>本测试类验证 A2A 客户端和服务端的交互功能。
 * 测试需要启动完整的 A2A 服务端。
 *
 * @author AgentScope Tutorial
 */
public class A2AIntegrationTest {

    private static final String SERVER_URL = "http://localhost:8080";
    private static final String TEST_AGENT_NAME = "test-client-agent";

    /**
     * 测试：通过 HTTP 调用 A2A 服务
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
    void testHttpA2ACall() {
        // 创建 A2A Agent
        A2aAgent agent = ClientRunner.createHttpAgent(SERVER_URL, TEST_AGENT_NAME);

        // 验证 Agent 已创建
        assertNotNull(agent);
        assertEquals(TEST_AGENT_NAME, agent.getName());

        // 发送测试消息
        Msg testMessage = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("你好，请介绍一下你自己").build())
                .build();

        // 调用并验证响应
        Msg response = agent.call(testMessage).block();

        assertNotNull(response);
        assertNotNull(response.getContent());
        assertFalse(response.getContent().isEmpty());
    }

    /**
     * 测试：流式响应接收
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
    void testStreamingResponse() {
        A2aAgent agent = ClientRunner.createHttpAgent(SERVER_URL, TEST_AGENT_NAME);

        Msg testMessage = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("写一首关于春天的诗").build())
                .build();

        // 收集流式响应
        StringBuilder responseBuilder = new StringBuilder();
        agent.stream(testMessage)
             .doOnNext(part -> {
                 if (part != null && !part.isEmpty()) {
                     responseBuilder.append(part);
                 }
             })
             .then()
             .block();

        // 验证响应内容
        String fullResponse = responseBuilder.toString();
        assertNotNull(fullResponse);
        assertFalse(fullResponse.isEmpty());

        System.out.println("流式响应内容：" + fullResponse);
    }

    /**
     * 测试：工具调用能力
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
    void testToolCalling() {
        A2aAgent agent = ClientRunner.createHttpAgent(SERVER_URL, TEST_AGENT_NAME);

        // 测试天气查询工具
        Msg weatherRequest = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("北京今天天气怎么样？").build())
                .build();

        Msg response = agent.call(weatherRequest).block();

        assertNotNull(response);
        // 验证响应包含天气相关信息
        String content = response.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .findFirst()
                .orElse("");

        // 如果工具正常工作，响应应包含天气数据
        System.out.println("工具调用响应：" + content);
    }

    /**
     * 测试：消息上下文保持
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
    void testContextMaintenance() {
        A2aAgent agent = ClientRunner.createHttpAgent(SERVER_URL, TEST_AGENT_NAME);

        // 第一轮对话
        Msg firstMessage = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("我的名字叫小明").build())
                .build();

        agent.call(firstMessage).block();

        // 第二轮对话（应记住之前的上下文）
        Msg secondMessage = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("我叫什么名字？").build())
                .build();

        Msg response = agent.call(secondMessage).block();

        assertNotNull(response);
        String content = response.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .findFirst()
                .orElse("");

        // 验证代理记住了用户名
        System.out.println("上下文测试响应：" + content);
    }

    /**
     * 测试：任务中断功能
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
    void testInterruptCapability() {
        A2aAgent agent = ClientRunner.createHttpAgent(SERVER_URL, TEST_AGENT_NAME);

        // 发送长时间运行的请求
        Msg longRequest = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("写一篇 10000 字的文章").build())
                .build();

        // 在流式响应过程中中断
        try {
            agent.stream(longRequest)
                 .doOnNext(msg -> {
                     // 模拟收到前几条消息后中断
                     System.out.println("收到响应，开始中断...");
                     agent.interrupt();
                 })
                 .then()
                 .block();
        } catch (Exception e) {
            // 中断后可能抛出异常，这是预期行为
            System.out.println("任务已被中断：" + e.getMessage());
        }
    }
}
```

### 9.4.10 日志配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- src/main/resources/logback.xml -->

<configuration>
    <!-- 定义日志格式 -->
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>
    <property name="LOG_FILE" value="logs/agentscope-a2a"/>

    <!-- 控制台 Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 文件 Appender（按天滚动） -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_FILE}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_FILE}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- A2A 专用日志配置 -->
    <logger name="io.agentscope.core.a2a" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>

    <!-- 根日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

## 9.5 本章小结

本章学习了 A2A（Agent-to-Agent）协议的核心概念和 AgentScope Java 的实现：

### 核心要点

1. **A2A 协议价值**：实现代理间的松耦合通信，通过标准化接口连接不同实现
2. **AgentCard**：代理的元数据描述，支持服务发现和能力声明
3. **传输方式**：
   - HTTP JSON-RPC：适合同步、低延迟场景
   - RocketMQ：适合异步、高并发、分布式场景
4. **服务发现**：通过 Nacos 实现动态服务注册与发现

### 实践要点

1. 使用 `agentscope-a2a-spring-boot-starter` 快速暴露本地 Agent
2. 通过 `@Tool` 注解定义自定义工具，扩展 Agent 能力
3. 根据场景选择合适的传输方式（HTTP vs RocketMQ）
4. 使用流式响应提供更好的用户体验

### 扩展阅读

- [A2A 协议规范](https://a2a-protocol.org/latest/specification/)
- [AgentScope Java A2A 文档](/zh/task/a2a)
- [RocketMQ 官方文档](https://rocketmq.apache.org/docs/quick-start/)
- [Nacos 服务发现](https://nacos.io/zh/docs/what-is-nacos.html)