# 第十八章：其他框架集成

AgentScope Java 不仅支持 Spring Boot，还提供了与 Micronaut 和 Quarkus 两大现代云原生框架的无缝集成。本章将详细介绍这两个框架的集成方式，并提供完整的快速启动案例。

---

## 18.1 Micronaut 集成

### 18.1.1 Micronaut 扩展概述

Micronaut 是一款现代化的 Java 框架，以其快速的启动时间、低内存占用和强大的依赖注入能力著称。AgentScope 提供了官方的 Micronaut 扩展，支持通过 `application.yml` 进行零配置集成。

### 18.1.2 添加依赖

在 `pom.xml` 中添加 Micronaut 扩展依赖：

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-micronaut-extension</artifactId>
    <version>${agentscope.version}</version>
</dependency>
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-core</artifactId>
</dependency>
```

### 18.1.3 配置 application.yml

Micronaut 使用 YAML 格式的配置文件，支持层级化的配置结构：

```yaml
micronaut:
  application:
    name: agentscope-demo
  server:
    port: 8080

# AgentScope 配置
agentscope:
  model:
    provider: dashscope  # 可选: dashscope, openai, gemini, anthropic

  # DashScope 配置（默认）
  dashscope:
    enabled: true
    api-key: ${DASHSCOPE_API_KEY:your-api-key}
    model-name: qwen-plus
    stream: false
    enable-thinking: false

  # OpenAI 配置（可选）
  openai:
    enabled: false
    api-key: ${OPENAI_API_KEY:}
    model-name: gpt-4-mini
    stream: false

  # Gemini 配置（可选）
  gemini:
    enabled: false
    api-key: ${GEMINI_API_KEY:}
    model-name: gemini-2.0-flash
    stream: false

  # Anthropic 配置（可选）
  anthropic:
    enabled: false
    api-key: ${ANTHROPIC_API_KEY:}
    model-name: claude-sonnet-4-20250514
    stream: false

  # Agent 配置
  agent:
    name: "MicronautAssistant"
    sys-prompt: |
      你是一个由 AgentScope 驱动的智能助手，
      可以在 Micronaut 环境中运行。
    max-iters: 10
```

### 18.1.4 使用 Bean

通过 Micronaut 的依赖注入获取 Agent 实例：

```java
package io.agentscope.tutorial.chapter18;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.micronaut.context.ApplicationContext;

/**
 * Micronaut 集成示例 - 展示如何在 Micronaut 中使用 AgentScope。
 *
 * <p>本示例演示：
 * <ul>
 *   <li>通过 ApplicationContext 获取 AgentScope Bean</li>
 *   <li>配置文件的自动加载</li>
 *   <li>简单的对话调用</li>
 * </ul>
 */
public class MicronautApplication {

    public static void main(String[] args) {
        System.out.println("\n========== AgentScope Micronaut 集成示例 ==========\n");

        // 启动 Micronaut ApplicationContext
        try (ApplicationContext context = ApplicationContext.run()) {
            // 从 Micronaut 容器获取 ReActAgent（通过 application.yml 自动配置）
            ReActAgent agent = context.getBean(ReActAgent.class);

            System.out.println("✓ Agent 通过 Micronaut 依赖注入创建成功");
            System.out.println("✓ 配置已从 application.yml 加载\n");

            // 演示多轮对话
            String[] questions = {
                "你好，请介绍一下你自己",
                "你支持哪些框架集成？",
                "谢谢，再见"
            };

            for (String question : questions) {
                System.out.println("用户: " + question);

                // 构建用户消息
                Msg userMsg = Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(question).build())
                        .build();

                // 调用 Agent 并获取响应
                Msg response = agent.call(userMsg).block();
                System.out.println("Agent: " + (response != null ? response.getContent() : "无响应"));
                System.out.println();
            }

            System.out.println("========== 示例完成 ==========\n");

        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
```

### 18.1.5 Micronaut 扩展核心类

Micronaut 扩展的核心架构包括：

| 类名 | 功能描述 |
|------|----------|
| `AgentscopeFactory` | Bean 工厂，负责创建 ReActAgent 实例 |
| `AgentscopeProperties` | 配置属性类，映射 `agentscope.*` 配置 |
| `ModelProviderType` | 模型提供者枚举（DashScope、OpenAI、Gemini、Anthropic） |
| `AgentProperties` | Agent 配置属性 |
| `DashScopeProperties` / `OpenAIProperties` / 等 | 各模型配置属性 |

---

## 18.2 Quarkus 集成

### 18.2.1 Quarkus 扩展概述

Quarkus 是专为云原生场景设计的 Java 框架，以 GraalVM 原生编译和超快速启动著称。AgentScope 提供了完整的 Quarkus 扩展，支持构建时优化和原生镜像（Native Image）构建。

### 18.2.2 添加依赖

```xml
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-quarkus-extension</artifactId>
    <version>${agentscope.version}</version>
</dependency>
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-core</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

### 18.2.3 配置 application.properties

Quarkus 使用 `.properties` 格式的配置文件：

```properties
# AgentScope 配置
agentscope.model.provider=dashscope
agentscope.dashscope.api-key=${DASHSCOPE_API_KEY:your-api-key}
agentscope.dashscope.model-name=qwen-plus
agentscope.dashscope.stream=false

# Agent 配置
agentscope.agent.name=QuarkusAssistant
agentscope.agent.sys-prompt=你是一个运行在 Quarkus 上的智能助手。
agentscope.agent.max-iters=10

# Quarkus HTTP 配置
quarkus.http.port=8080
quarkus.http.host=0.0.0.0

# 日志配置
quarkus.log.level=INFO
quarkus.log.category."io.agentscope".level=DEBUG
```

### 18.2.4 REST API 实现

通过 CDI 注入使用 Agent：

```java
package io.agentscope.tutorial.chapter18;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Quarkus REST 资源 - 展示 AgentScope 与 Quarkus REST 的集成。
 *
 * <p>提供两个端点：
 * <ul>
 *   <li>POST /agent/chat - 聊天接口</li>
 *   <li>GET /agent/health - 健康检查</li>
 * </ul>
 *
 * <p>调用示例：
 * <pre>
 * curl -X POST http://localhost:8080/agent/chat \
 *   -H "Content-Type: application/json" \
 *   -d '{"message":"你好"}
 * </pre>
 */
@Path("/agent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuarkusAgentResource {

    /**
     * 注入的 ReActAgent - 从 application.properties 自动配置。
     */
    @Inject
    ReActAgent agent;

    /**
     * 聊天端点 - 接收用户消息并返回 Agent 响应。
     *
     * @param request 包含用户消息的请求
     * @return Agent 的响应
     */
    @POST
    @Path("/chat")
    public Response chat(ChatRequest request) {
        // 参数验证
        if (request.message() == null || request.message().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("消息不能为空"))
                    .build();
        }

        try {
            // 构建用户消息
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(request.message()).build())
                    .build();

            // 调用 Agent
            Msg response = agent.call(userMsg).block();

            // 提取文本内容
            String responseText = response != null ? response.getTextContent() : "无响应";

            return Response.ok(new ChatResponse(responseText)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("处理请求时发生错误: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * 健康检查端点 - 返回 Agent 状态信息。
     *
     * @return 健康状态
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(
                new HealthResponse("AgentScope Agent 已就绪", agent.getName())
        ).build();
    }

    // ==================== 内部类：请求/响应 DTO ====================

    /** 聊天请求 */
    public record ChatRequest(String message) {}

    /** 聊天响应 */
    public record ChatResponse(String response) {}

    /** 错误响应 */
    public record ErrorResponse(String error) {}

    /** 健康检查响应 */
    public record HealthResponse(String status, String agentName) {}
}
```

### 18.2.5 运行 Quarkus 应用

```bash
# 开发模式（热重载）
export DASHSCOPE_API_KEY=your-api-key
mvn quarkus:dev

# 生产环境打包
mvn package
java -jar target/quarkus-app/quarkus-run.jar

# 构建原生镜像（需要 GraalVM）
mvn package -Pnative
./target/quarkus-example-*-runner
```

### 18.2.6 测试用例

```java
package io.agentscope.tutorial.chapter18;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import java.util.Map;

/**
 * AgentResource 集成测试。
 *
 * <p>验证 REST 端点的正确性，包括：
 * <ul>
 *   <li>成功请求的响应格式</li>
 *   <li>错误情况的处理</li>
 *   <li>边界条件的容错</li>
 * </ul>
 */
@QuarkusTest
@TestProfile(QuarkusAgentResourceTest.TestProfile.class)
class QuarkusAgentResourceTest {

    /**
     * 测试健康检查端点。
     */
    @Test
    void testHealthEndpoint() {
        given().when()
                .get("/agent/health")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("status", is("AgentScope Agent 已就绪"))
                .body("agentName", notNullValue());
    }

    /**
     * 测试有效消息的聊天请求。
     */
    @Test
    void testChatWithValidMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"你好\"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(200)
                .contentType(MediaType.APPLICATION_JSON)
                .body("response", notNullValue());
    }

    /**
     * 测试空消息被拒绝。
     */
    @Test
    void testEmptyMessageRejected() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"\"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(400)
                .body("error", is("消息不能为空"));
    }

    /**
     * 测试空白字符消息被拒绝。
     */
    @Test
    void testWhitespaceMessageRejected() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"   \"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(400)
                .body("error", is("消息不能为空"));
    }

    /**
     * 测试缺失消息字段的请求。
     */
    @Test
    void testMissingMessageField() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(400);
    }

    /**
     * 测试长消息的处理。
     */
    @Test
    void testLongMessage() {
        String longMessage = "测试消息 ".repeat(100);
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"" + longMessage + "\"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(200)
                .body("response", notNullValue());
    }

    /**
     * 测试特殊字符的处理。
     */
    @Test
    void testSpecialCharacters() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"Hello! @#$%^&*() 中文测试 🚀\"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(200)
                .body("response", notNullValue());
    }

    /**
     * 测试多行消息的处理。
     */
    @Test
    void testMultilineMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"第一行\\n第二行\\n第三行\"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(200)
                .body("response", notNullValue());
    }

    /**
     * 测试配置文件。
     */
    public static class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agentscope.model.provider", "dashscope",
                    "agentscope.dashscope.api-key", "test-api-key",
                    "agentscope.dashscope.model-name", "qwen-plus",
                    "agentscope.agent.name", "TestAgent",
                    "agentscope.agent.sys-prompt", "你是一个测试助手，请保持回答简洁。"
            );
        }
    }
}
```

### 18.2.7 Quarkus 扩展核心类

| 类名 | 功能描述 |
|------|----------|
| `AgentScopeConfig` | 使用 `@ConfigMapping` 映射配置到强类型对象 |
| `AgentScopeProducer` | CDI Producer，生成 Model 和 ReActAgent Bean |
| `AgentScopeRecorder` | Quarkus 构建时 recorder，用于可观测性和热部署 |

---

## 18.3 【案例】各框架快速集成示例

### 案例概述

本案例展示如何在 Spring Boot、Micronaut 和 Quarkus 三个主流框架中快速集成 AgentScope。我们将创建一个智能助手，支持基本的问答功能，并对比三个框架的配置方式。

### 18.3.1 项目结构

```
chapter18-examples/
├── pom.xml                          # 父 POM
├── spring-boot-example/             # Spring Boot 示例
│   ├── pom.xml
│   └── src/main/java/.../SpringBootAgentDemo.java
├── micronaut-example/               # Micronaut 示例
│   ├── pom.xml
│   ├── src/main/java/.../MicronautAgentDemo.java
│   └── src/main/resources/application.yml
└── quarkus-example/                  # Quarkus 示例
    ├── pom.xml
    ├── src/main/java/.../QuarkusAgentDemo.java
    ├── src/main/resources/application.properties
    └── src/test/java/.../QuarkusAgentDemoTest.java
```

### 18.3.2 父 POM 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter18-examples</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <name>AgentScope Java - 第十八章示例</name>
    <description>演示 Spring Boot、Micronaut、Quarkus 框架集成</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <agentscope.version>1.0.0</agentscope.version>
        <micronaut.version>4.5.0</micronaut.version>
        <quarkus.version>3.30.6</quarkus.version>
        <spring-boot.version>3.5.0</spring-boot.version>
    </properties>

    <modules>
        <module>spring-boot-example</module>
        <module>micronaut-example</module>
        <module>quarkus-example</module>
    </modules>
</project>
```

### 18.3.3 Spring Boot 示例

#### POM 配置

```xml
<project>
    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>spring-boot-example</artifactId>

    <properties>
        <spring-boot.version>3.5.0</spring-boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-spring-boot-starter</artifactId>
            <version>${agentscope.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
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

#### 配置 application.yml

```yaml
spring:
  application:
    name: agentscope-spring-boot

agentscope:
  model:
    provider: dashscope
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
    model-name: qwen-plus
  agent:
    name: SpringBootAssistant
    sys-prompt: 你是一个运行在 Spring Boot 上的智能助手。
    max-iters: 10
```

#### 代码实现

```java
package io.agentscope.tutorial.chapter18;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot AgentScope 集成示例。
 *
 * <p>演示在 Spring Boot 中使用 AgentScope 的完整流程。
 */
@SpringBootApplication
public class SpringBootAgentDemo implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootAgentDemo.class, args);
    }

    /**
     * 注入 AgentScope 自动配置的 ReActAgent。
     */
    @Bean
    ReActAgent agent() {
        // Agent 由 agentscope-spring-boot-starter 自动配置
        // 此处仅演示手动创建方式供参考
        return null; // 使用注入的 agent
    }

    @Override
    public void run(String... args) {
        // 获取自动配置的 Agent
        ReActAgent agent = applicationContext.getBean(ReActAgent.class);

        System.out.println("\n========== Spring Boot AgentScope 集成示例 ==========\n");
        System.out.println("✓ Agent 已通过 Spring Boot 自动配置创建");
        System.out.println("Agent 名称: " + agent.getName() + "\n");

        // 执行问答
        String[] questions = {
            "你好，请介绍一下你自己",
            "你运行在哪个框架上？",
            "再见"
        };

        for (String question : questions) {
            System.out.println("用户: " + question);

            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(question).build())
                    .build();

            Msg response = agent.call(userMsg).block();
            System.out.println("Agent: " + (response != null ? response.getContent() : "无响应"));
            System.out.println();
        }

        System.out.println("========== 示例完成 ==========\n");
        System.exit(0);
    }

    @javax.inject.Inject
    org.springframework.context.ApplicationContext applicationContext;
}
```

### 18.3.4 Micronaut 示例

#### POM 配置

```xml
<project>
    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>micronaut-example</artifactId>

    <properties>
        <exec.mainClass>io.agentscope.tutorial.chapter18.MicronautAgentDemo</exec.mainClass>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.micronaut.platform</groupId>
                <artifactId>micronaut-platform</artifactId>
                <version>${micronaut.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-micronaut-extension</artifactId>
            <version>${agentscope.version}</version>
        </dependency>
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.6.3</version>
                <configuration>
                    <mainClass>${exec.mainClass}</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 配置 application.yml

```yaml
micronaut:
  application:
    name: agentscope-micronaut
  server:
    port: 8080

agentscope:
  model:
    provider: dashscope
  dashscope:
    enabled: true
    api-key: ${DASHSCOPE_API_KEY:your-api-key}
    model-name: qwen-plus
    stream: false
  agent:
    enabled: true
    name: MicronautAssistant
    sys-prompt: |
      你是一个运行在 Micronaut 框架上的智能助手。
      Micronaut 以其快速的启动和低内存占用著称。
    max-iters: 10

logger:
  levels:
    io.agentscope: INFO
```

#### 代码实现

```java
package io.agentscope.tutorial.chapter18;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.micronaut.context.ApplicationContext;

/**
 * Micronaut AgentScope 集成示例。
 *
 * <p>演示在 Micronaut 中通过 ApplicationContext 获取 AgentScope Bean。
 *
 * <p>运行方式：
 * <pre>
 * DASHSCOPE_API_KEY=your-key mvn compile exec:java
 * </pre>
 */
public class MicronautAgentDemo {

    public static void main(String[] args) {
        System.out.println("\n========== Micronaut AgentScope 集成示例 ==========\n");

        // 启动 Micronaut 容器
        try (ApplicationContext context = ApplicationContext.run()) {
            // 从容器获取自动配置的 ReActAgent
            ReActAgent agent = context.getBean(ReActAgent.class);

            System.out.println("✓ Agent 通过 Micronaut 依赖注入创建成功");
            System.out.println("✓ 配置已从 application.yml 加载");
            System.out.println("Agent 名称: " + agent.getName() + "\n");

            // 问答演示
            String[] questions = {
                "你好，请介绍一下你自己",
                "你运行在哪个框架上？",
                "Micronaut 有哪些优势？",
                "再见"
            };

            for (String question : questions) {
                System.out.println("用户: " + question);

                Msg userMsg = Msg.builder()
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(question).build())
                        .build();

                Msg response = agent.call(userMsg).block();
                System.out.println("Agent: " + (response != null ? response.getContent() : "无响应"));
                System.out.println();
            }

            System.out.println("========== 示例完成 ==========\n");

        } catch (Exception e) {
            System.err.println("执行错误: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
```

### 18.3.5 Quarkus 示例

#### POM 配置

```xml
<project>
    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>quarkus-example</artifactId>

    <properties>
        <quarkus.version>3.30.6</quarkus.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-bom</artifactId>
                <version>${quarkus.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-quarkus-extension</artifactId>
            <version>${agentscope.version}</version>
        </dependency>
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${agentscope.version}</version>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-rest-jackson</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-junit5</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>io.quarkus</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <version>${quarkus.version}</version>
            </plugin>
        </plugins>
    </build>

    <profiles>
        <profile>
            <id>native</id>
            <properties>
                <quarkus.native.enabled>true</quarkus.native.enabled>
            </properties>
        </profile>
    </profiles>
</project>
```

#### 配置 application.properties

```properties
# AgentScope 配置
agentscope.model.provider=dashscope
agentscope.dashscope.api-key=${DASHSCOPE_API_KEY:your-api-key}
agentscope.dashscope.model-name=qwen-plus
agentscope.dashscope.stream=false

# Agent 配置
agentscope.agent.name=QuarkusAssistant
agentscope.agent.sys-prompt=你是一个运行在 Quarkus 框架上的智能助手。Quarkus 专为云原生设计。
agentscope.agent.max-iters=10

# Quarkus HTTP
quarkus.http.port=8080
quarkus.http.host=0.0.0.0

# 日志
quarkus.log.level=INFO
quarkus.log.category."io.agentscope".level=DEBUG
```

#### REST 端点实现

```java
package io.agentscope.tutorial.chapter18;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Quarkus AgentScope REST 端点示例。
 *
 * <p>提供基于 HTTP 的 Agent 对话接口。
 *
 * <p>端点：
 * <ul>
 *   <li>POST /agent/chat - 聊天接口</li>
 *   <li>GET /agent/health - 健康检查</li>
 * </ul>
 *
 * <p>运行方式：
 * <pre>
 * DASHSCOPE_API_KEY=your-key mvn quarkus:dev
 * </pre>
 */
@Path("/agent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuarkusAgentDemo {

    @Inject
    ReActAgent agent;

    /**
     * 聊天端点。
     */
    @POST
    @Path("/chat")
    public Response chat(ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("消息不能为空"))
                    .build();
        }

        try {
            Msg userMsg = Msg.builder()
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(request.message()).build())
                    .build();

            Msg response = agent.call(userMsg).block();
            String text = response != null ? response.getTextContent() : "无响应";

            return Response.ok(new ChatResponse(text)).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("处理请求时发生错误"))
                    .build();
        }
    }

    /**
     * 健康检查端点。
     */
    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(
                new HealthResponse("AgentScope Agent 已就绪", agent.getName())
        ).build();
    }

    // DTO 记录类
    public record ChatRequest(String message) {}
    public record ChatResponse(String response) {}
    public record ErrorResponse(String error) {}
    public record HealthResponse(String status, String agentName) {}
}
```

#### 测试类

```java
package io.agentscope.tutorial.chapter18;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;
import java.util.Map;

/**
 * QuarkusAgentDemo 集成测试。
 */
@QuarkusTest
@TestProfile(QuarkusAgentDemoTest.TestProfile.class)
class QuarkusAgentDemoTest {

    @Test
    void testHealthEndpoint() {
        given().when()
                .get("/agent/health")
                .then()
                .statusCode(200)
                .body("status", is("AgentScope Agent 已就绪"))
                .body("agentName", notNullValue());
    }

    @Test
    void testChatWithValidMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"你好\"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(200)
                .body("response", notNullValue());
    }

    @Test
    void testEmptyMessage() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"\"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(400)
                .body("error", is("消息不能为空"));
    }

    @Test
    void testSpecialCharacters() {
        given().contentType(MediaType.APPLICATION_JSON)
                .body("{\"message\":\"测试表情 🚀 和特殊字符 @#$%\"}")
                .when()
                .post("/agent/chat")
                .then()
                .statusCode(200)
                .body("response", notNullValue());
    }

    public static class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "agentscope.model.provider", "dashscope",
                    "agentscope.dashscope.api-key", "test-key",
                    "agentscope.dashscope.model-name", "qwen-plus",
                    "agentscope.agent.name", "TestAgent",
                    "agentscope.agent.sys-prompt", "你是一个测试助手。"
            );
        }
    }
}
```

### 18.3.6 框架对比总结

| 特性 | Spring Boot | Micronaut | Quarkus |
|------|-------------|----------|---------|
| **依赖注入** | @Autowired / 构造器注入 | 构造器注入（自动） | @Inject（CDI） |
| **配置格式** | `application.yml` | `application.yml` | `application.properties` |
| **启动速度** | 较慢 | 快速 | 最快（支持 Native） |
| **内存占用** | 较高 | 低 | 极低（Native） |
| **自动配置** | Spring Boot Starter | Micronaut 扩展 | Quarkus 扩展 |
| **原生编译** | 不支持 | 不支持 | 原生支持（GraalVM） |
| **适合场景** | 企业应用 | 微服务 | Serverless / 云原生 |
| **API 风格** | Spring MVC | Netty (非阻塞) | REST / Reactive |
| **配置层级** | 嵌套 YAML | 嵌套 YAML | 扁平 Properties |
| **测试支持** | Spring Test | Micronaut Test | Quarkus Test |

### 18.3.7 运行示例

```bash
# 设置 API Key
export DASHSCOPE_API_KEY=your-api-key

# Spring Boot
cd spring-boot-example && mvn spring-boot:run

# Micronaut
cd micronaut-example && mvn compile exec:java

# Quarkus (开发模式)
cd quarkus-example && mvn quarkus:dev

# Quarkus (打包运行)
cd quarkus-example && mvn package && java -jar target/quarkus-app/quarkus-run.jar

# Quarkus (原生镜像)
cd quarkus-example && mvn package -Pnative
./target/quarkus-example-*-runner
```

### 18.3.8 快速选择建议

- **企业级应用**：选择 **Spring Boot**，生态最完善，社区活跃
- **微服务 / Lambda**：选择 **Quarkus**，启动最快，原生支持好
- **轻量级服务 / 移动端**：选择 **Micronaut**，内存占用低，启动快速

---

## 本章小结

本章介绍了 AgentScope Java 与 Micronaut 和 Quarkus 两大现代框架的集成方式：

1. **Micronaut 集成**：通过 `agentscope-micronaut-extension` 扩展，支持 YAML 配置和构造器注入
2. **Quarkus 集成**：通过 `agentscope-quarkus-extension` 扩展，支持属性配置和 CDI 注入，提供原生镜像支持
3. **框架对比**：从启动速度、内存占用、适用场景等维度对比了三个框架的差异

三个框架的集成方式高度统一，核心都是通过各自的扩展模块实现 AgentScope 的自动配置，开发者可以根据项目需求灵活选择。