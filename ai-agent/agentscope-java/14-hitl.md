# 第十四章：人类在环（HITL）

Human-in-the-Loop（人类在环，简称 HITL）是一种将人类判断嵌入 AI 自动化流程的设计模式。AgentScope Java 通过 Hook 系统和工具挂起机制，提供了完整的人类介入能力。本章将详细讲解 HITL 的核心概念、人工介入机制、审批工作流设计，并手把手实现一个带审批工作流的完整 Spring Boot 案例。

## 14.1 HITL 概念与场景

### 14.1.1 什么是 HITL

HITL（Human-in-the-Loop）指在 AI 代理的自动化执行过程中，允许人类在关键节点介入决策、审批或补充信息的模式。它不是让 AI 完全自主地执行一切操作，而是在以下情况下引入人工判断：

- **高风险操作**：代理执行的某些操作可能产生不可逆的后果（如删除数据、发送邮件、执行交易）
- **信息缺失**：代理遇到模糊或不完整的输入，无法独立做出决策
- **合规要求**：监管或业务规范要求某些决策必须经过人工审批
- **质量把控**：在 AI 输出正式生效前，需人类审核内容准确性

### 14.1.2 典型应用场景

| 场景 | 说明 |
|------|------|
| 敏感工具确认 | 执行删除文件、发送邮件、转账等危险操作前，需人工确认 |
| 信息收集交互 | AI 在缺少必要参数时，通过 UI 向用户提问获取信息 |
| 内容审核审批 | AI 生成的内容（报告、文案、代码）需人工审核后发布 |
| 异常处理介入 | AI 遇到无法处理的情况时，暂停并请求人工指导 |
| 业务流程审批 | 合同审批、采购审批等需要多级审批的企业流程 |

### 14.1.3 AgentScope HITL 架构

AgentScope Java 中，HITL 的核心机制建立在以下组件之上：

```
┌─────────────────────────────────────────────────────────────────┐
│                        ReAct Agent                               │
│  ┌──────────┐    ┌──────────────┐    ┌───────────────┐          │
│  │ 推理阶段  │ -> │ 工具调用决策  │ -> │ 行动阶段      │          │
│  │(Reasoning)│    │ ToolCall    │    │ (Acting)     │          │
│  └──────────┘    └──────────────┘    └───────────────┘          │
│                        ▲                   ▲                    │
│                        │    Hook 系统      │                    │
│                        │ ┌───────────────┐│                    │
│                        │ │ PostReasoning ││ ──▶ 暂停代理        │
│                        │ │ PreActing     ││ ──▶ 审批确认        │
│                        │ │ PostActing    ││ ──▶ 结果注入        │
│                        └──┴───────────────┴┘                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │  ToolSuspendException
                    │  (工具挂起机制)     │
                    └─────────┬─────────┘
                              │
                    ┌─────────▼──────────┐
                    │  暂停 / 等待人类响应   │
                    │  注入 ToolResult    │
                    │  恢复代理执行        │
                    └────────────────────┘
```

关键组件：

- **Hook 系统**（第十三章）：在代理执行的生命周期各阶段拦截事件，触发暂停逻辑
- **ToolSuspendException**：工具抛出此异常时，代理暂停等待外部结果
- **PostReasoningEvent**：推理完成后触发，可在此决定是否需要人工确认
- **GenerateReason.TOOL_SUSPENDED**：指示当前消息因等待人类响应而暂停

## 14.2 人工介入机制

### 14.2.1 暂停代理的两种方式

AgentScope Java 支持两种人工介入模式：

**模式一：工具挂起（Tool Suspend）**

工具在执行过程中抛出 `ToolSuspendException`，代理立即暂停，等待外部注入 `ToolResultBlock` 后恢复执行。

```java
// 工具定义中抛出挂起异常
@Tool(name = "ask_user", description = "向用户请求信息")
public String askUser(
        @ToolParam(name = "question") String question,
        @ToolParam(name = "ui_type") String uiType) {
    // 代理在此暂停，等待用户响应
    throw new ToolSuspendException("等待用户输入: " + question);
}
```

**模式二：Hook 拦截暂停（基于 PostReasoningEvent）**

在推理完成后，通过 Hook 的 `stopAgent()` 方法暂停代理，适合需要批量确认多个工具调用的场景。

```java
public class ApprovalHook implements Hook {
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PostReasoningEvent post) {
            Msg reasoning = post.getReasoningMessage();
            // 检查是否有需要审批的工具调用
            if (hasPendingApprovals(reasoning)) {
                post.stopAgent();  // 暂停代理，等待人工审批
            }
        }
        return Mono.just(event);
    }
}
```

### 14.2.2 恢复代理执行

代理暂停后，外部系统（前端、API 调用方）需要注入 `ToolResultBlock` 来恢复执行。注入方式有两种：

**方式一：通过消息注入结果**

```java
// 人工审批通过后，注入工具结果继续执行
ToolResultBlock result = ToolResultBlock.of(
    toolId,
    "delete_file",
    TextBlock.builder().text("文件已删除").build()
);
Msg responseMsg = Msg.builder()
    .role(MsgRole.TOOL)
    .content(result)
    .build();
// 调用 stream() 时传入响应消息，代理自动恢复执行
agent.stream(responseMsg);
```

**方式二：拒绝工具执行（注入失败结果）**

```java
// 人工审批拒绝，注入取消结果
ToolResultBlock cancelledResult = ToolResultBlock.of(
    toolId,
    "delete_file",
    TextBlock.builder().text("用户取消: 审批被拒绝").build()
);
Msg cancelMsg = Msg.builder()
    .role(MsgRole.TOOL)
    .content(cancelledResult)
    .build();
agent.stream(cancelMsg);
```

### 14.2.3 中断运行中的代理

除了暂停等待输入，还可以随时中断正在运行的代理：

```java
// 在另一个线程/端点中调用 interrupt()
agent.interrupt();
```

中断后，代理会优雅地停止当前执行，保留内存状态，可后续恢复或销毁。

## 14.3 审批工作流

### 14.3.1 审批工作流设计要素

一个完整的审批工作流需要考虑以下要素：

- **审批节点**：定义哪些操作需要人工审批
- **审批级别**：单级审批、多级审批、条件分支审批
- **超时处理**：审批超时后的默认行为（自动拒绝、重新提醒）
- **状态持久化**：将待审批项持久化到数据库，确保不丢失
- **结果回传**：审批完成后通知代理继续执行

### 14.3.2 状态模型

典型的审批状态流转：

```
PENDING（待审批）
    │
    ├── 通过 ──▶ APPROVED ──▶ 执行工具，继续代理
    │
    ├── 拒绝 ──▶ REJECTED ──▶ 注入失败结果，继续代理
    │
    └── 超时 ──▶ TIMEOUT ──▶ 取决于业务规则
```

### 14.3.3 审批工作流与 Agent 的交互

```
┌──────────┐       推理完成        ┌───────────┐
│   LLM    │ ────────────────────▶│  Approval │
│          │  检测到危险工具       │   Hook    │
└──────────┘                      └─────┬─────┘
                                        │ stopAgent()
                                        ▼
                               ┌────────────────┐
                               │  审批服务       │
                               │  保存审批请求    │
                               │  到数据库       │
                               └────────┬───────┘
                                        │ 持久化 PENDING
                                        ▼
                               ┌────────────────┐
                               │  前端/管理后台   │  ◀── 人工审批
                               │  显示待审批列表   │
                               │  用户点击审批    │
                               └────────┬───────┘
                                        │ 审批结果
                                        ▼
                               ┌────────────────┐
                               │  注入结果       │  ◀── APPROVED/REJECTED
                               │  恢复代理       │
                               └────────────────┘
```

## 14.4 【案例】审批工作流实现

本案例实现一个企业级的 HITL 审批工作流系统：代理在执行敏感操作（如发送邮件、删除数据、审批合同）前，需要人工审批确认。审批请求持久化到 H2 数据库，支持多级审批、审批超时和结果回传。

### 14.4.1 项目结构

```
src/main/java/io/agentscope/tutorial/chapter14/
├── Chapter14Application.java          # Spring Boot 启动类
├── config/
│   └── AppConfig.java                  # Agent 和 Model 配置
├── entity/
│   └── ApprovalRequest.java           # 审批请求实体
├── repository/
│   └── ApprovalRequestRepository.java  # JPA 仓库
├── service/
│   ├── AgentService.java              # 代理服务（封装 ReActAgent）
│   └── ApprovalWorkflow.java          # 审批工作流核心
├── hook/
│   └── ToolApprovalHook.java          # 工具审批 Hook
├── controller/
│   ├── ChatController.java            # 聊天接口（SSE 流式响应）
│   └── ApprovalController.java        # 审批管理接口
├── dto/
│   ├── ChatRequest.java
│   ├── ApprovalDecision.java
│   └── ChatEvent.java
└── tool/
    ├── SendEmailTool.java             # 发送邮件工具（需审批）
    └── DeleteDataTool.java            # 删除数据工具（需审批）
```

### 14.4.2 完整项目代码

#### 14.4.2.1 pom.xml 依赖配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.agentscope.tutorial</groupId>
    <artifactId>chapter14-hitl</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>AgentScope Java - Tutorial - Chapter14 HITL</name>
    <description>人类在环（HITL）审批工作流完整案例</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.boot.version>3.2.5</spring.boot.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${revision}</version>
        </dependency>

        <!-- Spring Boot WebFlux（响应式 Web，支持 SSE） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Spring Data JPA + H2 数据库 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
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

        <!-- Jackson JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
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

#### 14.4.2.2 application.yml 配置

```yaml
# src/main/resources/application.yml
spring:
  application:
    name: chapter14-hitl

  datasource:
    url: jdbc:h2:mem:hitl_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
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
    database-platform: org.hibernate.dialect.H2Dialect

  jackson:
    serialization:
      write-dates-as-timestamps: false

server:
  port: 8080

# DashScope 配置（替换为你的 API Key）
dashscope:
  api-key: ${DASHSCOPE_API_KEY:your_api_key_here}

logging:
  level:
    io.agentscope: INFO
    io.agentscope.tutorial.chapter14: DEBUG
```

#### 14.4.2.3 审批请求实体

```java
package io.agentscope.tutorial.chapter14.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 审批请求实体.
 * 记录需要人工审批的工具调用信息，包括工具名、参数、状态和审批结果.
 */
@Entity
@Table(name = "approval_requests")
public class ApprovalRequest {

    /**
     * 审批状态枚举.
     */
    public enum Status {
        /** 待审批 */
        PENDING,
        /** 已批准，工具将执行 */
        APPROVED,
        /** 已拒绝，工具结果将被标记为失败 */
        REJECTED,
        /** 审批超时 */
        TIMEOUT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话 ID，用于关联到具体的代理会话 */
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    /** 工具调用 ID（来自 ToolUseBlock） */
    @Column(name = "tool_call_id", nullable = false)
    private String toolCallId;

    /** 工具名称 */
    @Column(name = "tool_name", nullable = false)
    private String toolName;

    /** 工具输入参数（JSON 字符串存储） */
    @Column(name = "tool_input", columnDefinition = "TEXT")
    private String toolInput;

    /** 审批状态 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    /** 审批人（可为空，直到有人审批） */
    @Column(name = "approver")
    private String approver;

    /** 审批意见 */
    @Column(name = "comment")
    private String comment;

    /** 审批时间 */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 创建时间 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 超时时间（秒），默认 3600 秒（1 小时） */
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 3600;

    // ==================== 构造函数 ====================

    public ApprovalRequest() {
        this.createdAt = LocalDateTime.now();
    }

    public ApprovalRequest(String sessionId, String toolCallId, String toolName, String toolInput) {
        this();
        this.sessionId = sessionId;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    // ==================== 业务方法 ====================

    /**
     * 检查审批是否已超时.
     *
     * @return true 如果已超时
     */
    public boolean isTimeout() {
        return LocalDateTime.now().isAfter(createdAt.plusSeconds(timeoutSeconds));
    }

    /**
     * 执行审批操作.
     *
     * @param approved  true 表示批准，false 表示拒绝
     * @param approver  审批人
     * @param comment   审批意见
     */
    public void approve(boolean approved, String approver, String comment) {
        this.status = approved ? Status.APPROVED : Status.REJECTED;
        this.approver = approver;
        this.comment = comment;
        this.approvedAt = LocalDateTime.now();
    }

    // ==================== Getter & Setter ====================

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

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolInput() {
        return toolInput;
    }

    public void setToolInput(String toolInput) {
        this.toolInput = toolInput;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
```

#### 14.4.2.4 JPA 仓库

```java
package io.agentscope.tutorial.chapter14.repository;

import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.entity.ApprovalRequest.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审批请求 JPA 仓库.
 * 提供审批请求的 CRUD 操作和复杂查询.
 */
@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    /**
     * 根据会话 ID 和工具调用 ID 查找审批请求.
     * 用于验证某个特定的工具调用是否已有待审批请求.
     */
    Optional<ApprovalRequest> findBySessionIdAndToolCallId(String sessionId, String toolCallId);

    /**
     * 查找指定会话的所有待审批请求.
     */
    List<ApprovalRequest> findBySessionIdAndStatus(String sessionId, Status status);

    /**
     * 查找所有待审批请求（全局）.
     */
    List<ApprovalRequest> findByStatus(Status status);

    /**
     * 查找超时但仍为待审批状态的请求.
     * 这些请求需要被标记为 TIMEOUT.
     */
    @Query("SELECT a FROM ApprovalRequest a WHERE a.status = :status AND a.createdAt < :deadline")
    List<ApprovalRequest> findTimeoutPendingRequests(
            @Param("status") Status status,
            @Param("deadline") LocalDateTime deadline);

    /**
     * 批量更新超时请求的状态.
     */
    @Modifying
    @Query("UPDATE ApprovalRequest a SET a.status = :newStatus WHERE a.status = :oldStatus AND a.createdAt < :deadline")
    int updateTimeoutRequests(
            @Param("oldStatus") Status oldStatus,
            @Param("newStatus") Status newStatus,
            @Param("deadline") LocalDateTime deadline);

    /**
     * 根据会话 ID 删除所有审批请求.
     */
    void deleteBySessionId(String sessionId);
}
```

#### 14.4.2.5 工具类：发送邮件（需审批）

```java
package io.agentscope.tutorial.chapter14.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.ToolResultMessageBuilder;

/**
 * 发送邮件工具（需要审批）.
 * 此工具涉及敏感操作，在执行前必须经过人工审批.
 * 审批通过后，工具将输出邮件发送成功的模拟信息.
 */
public class SendEmailTool {

    /** 工具名称，需要在 ToolApprovalHook 中配置为需要审批的工具 */
    public static final String TOOL_NAME = "send_email";

    /**
     * 发送电子邮件.
     * 注意：此方法在审批通过前不会执行，实际项目中应集成真实的邮件发送服务.
     *
     * @param to      收件人邮箱地址
     * @param subject 邮件主题
     * @param body    邮件正文（支持多行文本）
     * @param cc      抄送人（可选，多个邮箱用逗号分隔）
     * @return 邮件发送结果
     */
    @Tool(
            name = TOOL_NAME,
            description =
                    "发送电子邮件. 此操作需要审批通过后才能执行.\n"
                        + "适用于：发送通知、报告、提醒等正式邮件.\n"
                        + "注意：仅支持公司域名的收件人邮箱（@company.com 结尾）。")
    public String sendEmail(
            @ToolParam(
                            name = "to",
                            description = "收件人邮箱地址，例如：user@company.com",
                            required = true)
                    String to,
            @ToolParam(
                            name = "subject",
                            description = "邮件主题，最大 200 字符",
                            required = true)
                    String subject,
            @ToolParam(
                            name = "body",
                            description = "邮件正文内容，支持多行文本",
                            required = true)
                    String body,
            @ToolParam(
                            name = "cc",
                            description = "抄送人邮箱地址，多个用逗号分隔（可选）",
                            required = false)
                    String cc) {

        // 模拟邮件发送逻辑
        StringBuilder result = new StringBuilder();
        result.append("邮件已发送成功！\n");
        result.append("━━━━━━━━━━━━━━━\n");
        result.append("收件人: ").append(to).append("\n");
        if (cc != null && !cc.isBlank()) {
            result.append("抄送: ").append(cc).append("\n");
        }
        result.append("主题: ").append(subject).append("\n");
        result.append("━━━━━━━━━━━━━━━\n");
        result.append("正文:\n").append(body);

        return result.toString();
    }
}
```

#### 14.4.2.6 工具类：删除数据（需审批）

```java
package io.agentscope.tutorial.chapter14.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 删除数据工具（需要审批）.
 * 此工具涉及数据删除的不可逆操作，必须经过人工审批才能执行.
 */
public class DeleteDataTool {

    /** 工具名称，需要在 ToolApprovalHook 中配置为需要审批的工具 */
    public static final String TOOL_NAME = "delete_data";

    /**
     * 删除指定数据记录.
     * 注意：此操作不可逆，必须经过审批确认.
     *
     * @param table     数据表名
     * @param recordId  要删除的记录 ID
     * @param reason    删除原因
     * @param confirm   必须为 true 才能执行删除（防止误操作）
     * @return 删除操作的结果
     */
    @Tool(
            name = TOOL_NAME,
            description =
                    "删除指定的数据记录。此操作不可逆且需要审批才能执行。\n"
                        + "适用场景：清理过期数据、删除测试记录、移除无效数据等。\n"
                        + "警告：删除后数据无法恢复，请确保已备份重要数据！")
    public String deleteData(
            @ToolParam(
                            name = "table",
                            description = "数据表名，例如：users、orders、logs",
                            required = true)
                    String table,
            @ToolParam(
                            name = "record_id",
                            description = "要删除的记录 ID",
                            required = true)
                    String recordId,
            @ToolParam(
                            name = "reason",
                            description = "删除原因说明",
                            required = true)
                    String reason,
            @ToolParam(
                            name = "confirm",
                            description = "确认删除操作，必须为 true",
                            required = true)
                    Boolean confirm) {

        if (!Boolean.TRUE.equals(confirm)) {
            return "错误：删除操作未确认。请将 confirm 参数设为 true 以确认删除。";
        }

        // 模拟删除操作（实际项目中应调用数据库或存储服务）
        return String.format(
                "数据删除成功！\n"
                        + "━━━━━━━━━━━━━━━\n"
                        + "表: %s\n"
                        + "记录ID: %s\n"
                        + "原因: %s\n"
                        + "━━━━━━━━━━━━━━━\n"
                        + "⚠️ 注意：此操作不可逆，数据已永久删除。",
                table, recordId, reason);
    }
}
```

#### 14.4.2.7 工具类：查询数据（辅助工具，不需要审批）

```java
package io.agentscope.tutorial.chapter14.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

/**
 * 查询数据工具（不需要审批）.
 * 用于代理获取必要信息，辅助决策，不涉及敏感操作.
 */
public class QueryDataTool {

    public static final String TOOL_NAME = "query_data";

    /**
     * 查询数据记录.
     * 这是一个只读操作，不需要人工审批.
     *
     * @param table    数据表名
     * @param recordId 记录 ID
     * @return 记录内容
     */
    @Tool(
            name = TOOL_NAME,
            description =
                    "查询指定的数据记录（只读操作）。\n"
                        + "此工具用于获取数据以辅助决策，不会修改或删除任何数据。\n"
                        + "适用于：查看订单详情、用户信息、统计数据等。")
    public String queryData(
            @ToolParam(name = "table", description = "数据表名", required = true) String table,
            @ToolParam(name = "record_id", description = "记录ID", required = true) String recordId) {
        // 模拟查询结果
        return String.format(
                "查询结果（表: %s, ID: %s）:\n"
                        + "━━━━━━━━━━━━━━━\n"
                        + "id: %s\n"
                        + "status: active\n"
                        + "created_at: 2024-01-15 10:30:00\n"
                        + "updated_at: 2024-03-20 14:22:00\n"
                        + "data: {...}",
                table, recordId, recordId);
    }
}
```

#### 14.4.2.8 审批 Hook

```java
package io.agentscope.tutorial.chapter14.hook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.repository.ApprovalRequestRepository;
import io.agentscope.tutorial.chapter14.service.ApprovalWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工具审批 Hook.
 *
 * <p>此 Hook 在推理阶段完成后检查是否有需要审批的工具调用。
 * 如果有，它会停止代理执行，创建审批请求记录到数据库，
 * 并等待外部审批后恢复执行。
 *
 * <p>需要审批的工具通过构造函数注入（在 AgentService 中配置）。
 *
 * <p>工作流程：
 * <ol>
 *   <li>推理完成 → PostReasoningEvent</li>
 *   <li>检查 ToolUseBlock 中是否有需要审批的工具</li>
 *   <li>有 → 调用 ApprovalWorkflow 保存审批请求，stopAgent()</li>
 *   <li>无 → 正常继续执行</li>
 * </ol>
 *
 * @see ApprovalWorkflow
 * @see PostReasoningEvent
 */
public class ToolApprovalHook implements Hook {

    private static final Logger log = LoggerFactory.getLogger(ToolApprovalHook.class);

    /** 需要审批的工具名称集合 */
    private final Set<String> toolsRequiringApproval;

    /** 审批工作流服务 */
    private final ApprovalWorkflow approvalWorkflow;

    /** JSON 序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 构造函数，注入需要审批的工具集合.
     *
     * @param toolsRequiringApproval 需要审批的工具名称集合
     * @param approvalWorkflow      审批工作流服务
     */
    public ToolApprovalHook(
            Set<String> toolsRequiringApproval, ApprovalWorkflow approvalWorkflow) {
        this.toolsRequiringApproval = new HashSet<>(toolsRequiringApproval);
        this.approvalWorkflow = approvalWorkflow;
        this.objectMapper = new ObjectMapper();
        log.info("ToolApprovalHook 初始化，需要审批的工具: {}", toolsRequiringApproval);
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (!(event instanceof PostReasoningEvent post)) {
            return Mono.just(event);
        }

        Msg reasoning = post.getReasoningMessage();
        if (reasoning == null) {
            return Mono.just(event);
        }

        // 获取推理结果中的所有工具调用
        List<ToolUseBlock> toolCalls = reasoning.getContentBlocks(ToolUseBlock.class);
        if (toolCalls.isEmpty()) {
            return Mono.just(event);
        }

        // 检查是否有需要审批的工具
        List<ToolUseBlock> pendingApprovals =
                toolCalls.stream()
                        .filter(t -> toolsRequiringApproval.contains(t.getName()))
                        .toList();

        if (!pendingApprovals.isEmpty()) {
            log.info(
                    "检测到 {} 个需要审批的工具调用，停止代理执行",
                    pendingApprovals.size());

            // 为每个需要审批的工具创建审批请求
            String sessionId = post.getSessionId();
            for (ToolUseBlock tool : pendingApprovals) {
                try {
                    String toolInputJson = objectMapper.writeValueAsString(tool.getInput());
                    ApprovalRequest request =
                            new ApprovalRequest(
                                    sessionId, tool.getId(), tool.getName(), toolInputJson);
                    approvalWorkflow.createApprovalRequest(request);
                    log.info(
                            "创建审批请求: tool={}, toolCallId={}",
                            tool.getName(),
                            tool.getId());
                } catch (JsonProcessingException e) {
                    log.error("序列化工具参数失败: {}", e.getMessage());
                }
            }

            // 停止代理执行，等待审批
            post.stopAgent();
        }

        return Mono.just(event);
    }

    /**
     * 添加需要审批的工具.
     *
     * @param toolName 工具名称
     */
    public void addTool(String toolName) {
        this.toolsRequiringApproval.add(toolName);
    }

    /**
     * 移除需要审批的工具.
     *
     * @param toolName 工具名称
     */
    public void removeTool(String toolName) {
        this.toolsRequiringApproval.remove(toolName);
    }
}
```

#### 14.4.2.9 审批工作流服务

```java
package io.agentscope.tutorial.chapter14.service;

import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.entity.ApprovalRequest.Status;
import io.agentscope.tutorial.chapter14.repository.ApprovalRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 审批工作流核心服务.
 * 负责审批请求的创建、查询、审批决策和超时处理.
 */
@Service
public class ApprovalWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ApprovalWorkflow.class);

    private final ApprovalRequestRepository repository;

    public ApprovalWorkflow(ApprovalRequestRepository repository) {
        this.repository = repository;
    }

    /**
     * 创建新的审批请求.
     *
     * @param request 审批请求实体
     * @return 保存后的审批请求（含 ID）
     */
    @Transactional
    public ApprovalRequest createApprovalRequest(ApprovalRequest request) {
        ApprovalRequest saved = repository.save(request);
        log.info(
                "审批请求已创建: id={}, session={}, tool={}, toolCallId={}",
                saved.getId(),
                saved.getSessionId(),
                saved.getToolName(),
                saved.getToolCallId());
        return saved;
    }

    /**
     * 根据 ID 获取审批请求.
     */
    public Optional<ApprovalRequest> findById(Long id) {
        return repository.findById(id);
    }

    /**
     * 根据会话 ID 和工具调用 ID 查找审批请求.
     * 用于恢复代理执行时验证审批状态.
     */
    public Optional<ApprovalRequest> findBySessionAndToolCallId(
            String sessionId, String toolCallId) {
        return repository.findBySessionIdAndToolCallId(sessionId, toolCallId);
    }

    /**
     * 获取指定会话的所有待审批请求.
     */
    public List<ApprovalRequest> getPendingRequests(String sessionId) {
        return repository.findBySessionIdAndStatus(sessionId, Status.PENDING);
    }

    /**
     * 获取所有待审批请求（全局）.
     */
    public List<ApprovalRequest> getAllPendingRequests() {
        return repository.findByStatus(Status.PENDING);
    }

    /**
     * 执行审批决策.
     *
     * @param approvalId 审批请求 ID
     * @param approved   true = 批准，false = 拒绝
     * @param approver   审批人名称
     * @param comment    审批意见（可选）
     * @return 审批后的请求，如果不存在则返回空
     */
    @Transactional
    public Optional<ApprovalRequest> approve(
            Long approvalId, boolean approved, String approver, String comment) {
        Optional<ApprovalRequest> opt = repository.findById(approvalId);
        if (opt.isEmpty()) {
            log.warn("审批请求不存在: id={}", approvalId);
            return Optional.empty();
        }

        ApprovalRequest request = opt.get();
        if (request.getStatus() != Status.PENDING) {
            log.warn(
                    "审批请求状态不是 PENDING，无法审批: id={}, status={}",
                    approvalId,
                    request.getStatus());
            return Optional.empty();
        }

        request.approve(approved, approver, comment);
        ApprovalRequest saved = repository.save(request);

        log.info(
                "审批完成: id={}, tool={}, toolCallId={}, decision={}, approver={}",
                saved.getId(),
                saved.getToolName(),
                saved.getToolCallId(),
                saved.getStatus(),
                saved.getApprover());

        return Optional.of(saved);
    }

    /**
     * 根据会话 ID 执行批量审批决策.
     * 通常用于会话结束时清理所有待审批项.
     *
     * @param sessionId 会话 ID
     * @param approved  true = 全部批准，false = 全部拒绝
     * @param approver  审批人
     * @param comment   审批意见
     * @return 被处理的请求数量
     */
    @Transactional
    public int batchApproveBySession(
            String sessionId, boolean approved, String approver, String comment) {
        List<ApprovalRequest> pending = repository.findBySessionIdAndStatus(sessionId, Status.PENDING);
        for (ApprovalRequest request : pending) {
            request.approve(approved, approver, comment + " [会话结束自动处理]");
            repository.save(request);
        }
        log.info(
                "批量审批完成: session={}, count={}, decision={}",
                sessionId,
                pending.size(),
                approved ? "APPROVED" : "REJECTED");
        return pending.size();
    }

    /**
     * 定时任务：处理审批超时.
     * 每分钟执行一次，将超时的待审批请求标记为 TIMEOUT.
     * 默认超时时间为 3600 秒（1 小时），可在实体中自定义.
     */
    @Scheduled(fixedRate = 60000) // 每 60 秒执行一次
    @Transactional
    public void handleTimeoutRequests() {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(60); // 至少 60 秒未处理
        int updated = repository.updateTimeoutRequests(Status.PENDING, Status.TIMEOUT, deadline);
        if (updated > 0) {
            log.warn("自动处理了 {} 个超时审批请求", updated);
        }
    }

    /**
     * 删除会话的所有审批记录.
     */
    @Transactional
    public void deleteBySessionId(String sessionId) {
        repository.deleteBySessionId(sessionId);
        log.info("已删除会话的所有审批记录: session={}", sessionId);
    }
}
```

#### 14.4.2.10 代理服务

```java
package io.agentscope.tutorial.chapter14.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.hook.ObservationHook;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.session.InMemorySession;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.hook.ToolApprovalHook;
import io.agentscope.tutorial.chapter14.tool.DeleteDataTool;
import io.agentscope.tutorial.chapter14.tool.QueryDataTool;
import io.agentscope.tutorial.chapter14.tool.SendEmailTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 代理服务.
 * 封装 ReActAgent 的创建和管理，处理 HITL 场景下的人类介入逻辑.
 */
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 需要审批的工具集合 */
    private static final Set<String> TOOLS_REQUIRING_APPROVAL =
            Set.of(SendEmailTool.TOOL_NAME, DeleteDataTool.TOOL_NAME);

    /** 系统提示词 */
    private static final String SYS_PROMPT = """
            你是一个企业助手，可以帮助用户完成日常办公任务。

            你可以使用以下工具：
            - query_data: 查询数据记录（不需要审批）
            - send_email: 发送电子邮件（需要审批）
            - delete_data: 删除数据记录（需要审批）

            重要规则：
            - 发送邮件前，必须确认收件人邮箱地址格式正确（@company.com 结尾）
            - 删除数据前，必须说明删除原因和影响范围
            - 如果用户没有提供足够的信息，不要随意猜测，主动向用户询问
            - 涉及敏感操作（邮件、删除）时，系统会自动暂停等待审批，无需额外提醒用户
            """;

    /** DashScope API Key（从环境变量或配置中获取） */
    private final String apiKey;

    /** 审批工作流服务 */
    private final ApprovalWorkflow approvalWorkflow;

    /** 内存会话存储 */
    private final Session session = new InMemorySession();

    /** 运行中的代理映射 */
    private final ConcurrentHashMap<String, ReActAgent> runningAgents = new ConcurrentHashMap<>();

    /** DashScope 聊天模型 */
    private final DashScopeChatModel model;

    /** 工具包 */
    private final Toolkit toolkit;

    /** 工具审批 Hook */
    private final ToolApprovalHook approvalHook;

    public AgentService(String apiKey, ApprovalWorkflow approvalWorkflow) {
        this.apiKey = apiKey;
        this.approvalWorkflow = approvalWorkflow;

        // 初始化工具包
        this.toolkit = new Toolkit();
        this.toolkit.registerTool(new SendEmailTool());
        this.toolkit.registerTool(new DeleteDataTool());
        this.toolkit.registerTool(new QueryDataTool());

        // 初始化审批 Hook
        this.approvalHook = new ToolApprovalHook(TOOLS_REQUIRING_APPROVAL, approvalWorkflow);

        // 初始化模型
        this.model =
                DashScopeChatModel.builder()
                        .apiKey(apiKey)
                        .modelName("qwen-max")
                        .stream(true)
                        .enableThinking(false)
                        .formatter(new DashScopeChatFormatter())
                        .build();

        log.info("AgentService 初始化完成，API Key 长度: {}", apiKey.length());
    }

    /**
     * 发送聊天消息并获取流式响应.
     *
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @return SSE 事件流
     */
    public Flux<Map<String, Object>> chat(String sessionId, String message) {
        ReActAgent agent = getOrCreateAgent(sessionId);

        Msg userMsg =
                Msg.builder()
                        .name("User")
                        .role(MsgRole.USER)
                        .content(TextBlock.builder().text(message).build())
                        .build();

        Flux<Map<String, Object>> events = agent.stream(userMsg).flatMap(this::convertEvent);
        return wrapWithCompletion(events, sessionId, agent);
    }

    /**
     * 处理用户的审批决策，恢复代理执行.
     *
     * @param sessionId 会话 ID
     * @param toolCallId 工具调用 ID
     * @param approved   审批决策
     * @param approver   审批人
     * @param comment    审批意见
     * @return SSE 事件流
     */
    public Flux<Map<String, Object>> handleApproval(
            String sessionId,
            String toolCallId,
            boolean approved,
            String approver,
            String comment) {

        // 查找对应的审批请求
        Optional<ApprovalRequest> optRequest =
                approvalWorkflow.findBySessionAndToolCallId(sessionId, toolCallId);

        if (optRequest.isEmpty()) {
            log.warn("未找到审批请求: session={}, toolCallId={}", sessionId, toolCallId);
            return Flux.just(Map.of("type", "ERROR", "error", "审批请求不存在"));
        }

        ApprovalRequest request = optRequest.get();

        // 执行审批决策
        Optional<ApprovalRequest> approvedRequest =
                approvalWorkflow.approve(request.getId(), approved, approver, comment);

        if (approvedRequest.isEmpty()) {
            return Flux.just(Map.of("type", "ERROR", "error", "审批失败"));
        }

        // 根据审批结果注入工具结果
        ReActAgent agent = getOrCreateAgent(sessionId);
        ToolResultBlock result;

        if (approved) {
            // 批准：注入成功结果，工具将被执行
            result =
                    ToolResultBlock.of(
                            toolCallId,
                            request.getToolName(),
                            TextBlock.builder()
                                    .text("审批通过，工具将执行")
                                    .build());
        } else {
            // 拒绝：注入失败结果，代理将收到"工具执行失败"的消息
            result =
                    ToolResultBlock.of(
                            toolCallId,
                            request.getToolName(),
                            TextBlock.builder()
                                    .text("审批被拒绝: " + (comment != null ? comment : "未提供原因"))
                                    .build());
        }

        Msg responseMsg =
                Msg.builder().role(MsgRole.TOOL).content(result).build();

        Flux<Map<String, Object>> events = agent.stream(responseMsg).flatMap(this::convertEvent);
        return wrapWithCompletion(events, sessionId, agent);
    }

    /**
     * 获取或创建代理实例.
     */
    private ReActAgent getOrCreateAgent(String sessionId) {
        ReActAgent existing = runningAgents.get(sessionId);
        if (existing != null) {
            return existing;
        }

        ReActAgent agent =
                ReActAgent.builder()
                        .name("EnterpriseAssistant")
                        .sysPrompt(SYS_PROMPT)
                        .model(model)
                        .toolkit(toolkit)
                        .memory(new InMemoryMemory())
                        .hook(approvalHook)
                        .hook(new ObservationHook())
                        .build();

        // 从持久化存储中恢复会话状态
        agent.loadIfExists(session, sessionId);
        runningAgents.put(sessionId, agent);

        log.info("创建新代理实例: session={}", sessionId);
        return agent;
    }

    /**
     * 将事件流包装为包含完成标记的流.
     */
    private Flux<Map<String, Object>> wrapWithCompletion(
            Flux<Map<String, Object>> events, String sessionId, ReActAgent agent) {
        return Flux.concat(
                        events,
                        Flux.just(Map.of("type", "COMPLETE")))
                .doOnError(
                        error ->
                                log.error(
                                        "代理执行出错: session={}, error={}",
                                        sessionId,
                                        error.getMessage()))
                .doFinally(
                        signal -> {
                            runningAgents.remove(sessionId);
                            agent.saveTo(session, sessionId);
                            log.info("会话结束: session={}", sessionId);
                        });
    }

    /**
     * 将 Agent Event 转换为 SSE 友好的 Map.
     */
    private Flux<Map<String, Object>> convertEvent(Event event) {
        List<Map<String, Object>> events = new ArrayList<>();
        Msg msg = event.getMessage();

        switch (event.getType()) {
            case REASONING -> {
                if (event.isLast() && msg.hasContentBlocks(ToolUseBlock.class)) {
                    // 推理结束且有工具调用
                    List<ToolUseBlock> toolCalls = msg.getContentBlocks(ToolUseBlock.class);
                    for (ToolUseBlock tool : toolCalls) {
                        if (TOOLS_REQUIRING_APPROVAL.contains(tool.getName())) {
                            // 需要审批的工具调用
                            events.add(
                                    Map.of(
                                            "type", "PENDING_APPROVAL",
                                            "toolCallId", tool.getId(),
                                            "toolName", tool.getName(),
                                            "toolInput", convertInput(tool.getInput()),
                                            "sessionId", event.getSessionId() != null
                                                    ? event.getSessionId() : "unknown"));
                        } else {
                            // 普通工具调用
                            events.add(
                                    Map.of(
                                            "type", "TOOL_USE",
                                            "toolId", tool.getId(),
                                            "toolName", tool.getName(),
                                            "toolInput", convertInput(tool.getInput())));
                        }
                    }
                } else {
                    // 推理过程（流式文本）
                    String text = extractText(msg);
                    if (text != null && !text.isBlank()) {
                        events.add(
                                Map.of(
                                        "type", "REASONING",
                                        "content", text,
                                        "incremental", !event.isLast()));
                    }
                }
            }
            case TOOL_RESULT -> {
                for (ToolResultBlock result : msg.getContentBlocks(ToolResultBlock.class)) {
                    events.add(
                            Map.of(
                                    "type", "TOOL_RESULT",
                                    "toolId", result.getId(),
                                    "toolName", result.getName(),
                                    "toolResult", extractToolResultText(result)));
                }
            }
            case AGENT_RESULT -> {
                String text = extractText(msg);
                if (text != null && !text.isBlank()) {
                    events.add(Map.of("type", "FINAL", "content", text));
                }
            }
            default -> {
                // HINT, SUMMARY 等其他事件类型暂不处理
            }
        }

        return Flux.fromIterable(events);
    }

    /** 提取文本内容 */
    private String extractText(Msg msg) {
        List<TextBlock> blocks = msg.getContentBlocks(TextBlock.class);
        if (blocks.isEmpty()) {
            return null;
        }
        return blocks.stream().map(TextBlock::getText).collect(Collectors.joining());
    }

    /** 提取工具执行结果文本 */
    private String extractToolResultText(ToolResultBlock result) {
        List<io.agentscope.core.message.ContentBlock> outputs = result.getOutput();
        if (outputs == null || outputs.isEmpty()) {
            return "(empty)";
        }
        return outputs.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .collect(Collectors.joining("\n"));
    }

    /** 转换工具输入参数 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertInput(Object input) {
        if (input == null) {
            return Map.of();
        }
        if (input instanceof Map) {
            return (Map<String, Object>) input;
        }
        try {
            return OBJECT_MAPPER.convertValue(input, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of("value", input.toString());
        }
    }

    /**
     * 中断指定会话的代理执行.
     */
    public void interrupt(String sessionId) {
        ReActAgent agent = runningAgents.get(sessionId);
        if (agent != null) {
            agent.interrupt();
            log.info("代理已中断: session={}", sessionId);
        }
    }

    /**
     * 清空会话状态.
     */
    public void clearSession(String sessionId) {
        runningAgents.remove(sessionId);
        session.delete(SimpleSessionKey.of(sessionId));
        approvalWorkflow.deleteBySessionId(sessionId);
        log.info("会话已清空: session={}", sessionId);
    }
}
```

#### 14.4.2.11 聊天控制器

```java
package io.agentscope.tutorial.chapter14.controller;

import io.agentscope.tutorial.chapter14.service.AgentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 聊天控制器.
 * 提供聊天接口，支持 SSE 流式响应和审批决策回传.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 发送聊天消息.
     * 返回 SSE 流式响应，实时推送代理推理和执行过程.
     *
     * @param request 包含 sessionId 和 message 的请求体
     * @return SSE 事件流
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> chat(@RequestBody Map<String, String> request) {
        String sessionId = request.getOrDefault("sessionId", "default");
        String message = request.get("message");

        if (message == null || message.isBlank()) {
            return Flux.just(
                    ServerSentEvent.<Map<String, Object>>builder()
                            .data(Map.of("type", "ERROR", "error", "消息不能为空"))
                            .build());
        }

        Flux<Map<String, Object>> events = agentService.chat(sessionId, message);
        return events.map(data -> ServerSentEvent.<Map<String, Object>>builder().data(data).build());
    }

    /**
     * 处理审批决策.
     * 当前端用户点击"批准"或"拒绝"按钮时调用此接口，恢复代理执行.
     *
     * @param request 包含审批决策的请求体
     * @return SSE 事件流（恢复执行后的响应）
     */
    @PostMapping(value = "/api/approve", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, Object>>> approve(@RequestBody Map<String, Object> request) {
        String sessionId = (String) request.getOrDefault("sessionId", "default");
        String toolCallId = (String) request.get("toolCallId");
        boolean approved = Boolean.TRUE.equals(request.get("approved"));
        String approver = (String) request.getOrDefault("approver", "admin");
        String comment = (String) request.get("comment");

        if (toolCallId == null || toolCallId.isBlank()) {
            return Flux.just(
                    ServerSentEvent.<Map<String, Object>>builder()
                            .data(Map.of("type", "ERROR", "error", "toolCallId 不能为空"))
                            .build());
        }

        Flux<Map<String, Object>> events =
                agentService.handleApproval(sessionId, toolCallId, approved, approver, comment);
        return events.map(data -> ServerSentEvent.<Map<String, Object>>builder().data(data).build());
    }

    /**
     * 中断正在执行的代理.
     */
    @PostMapping("/api/interrupt/{sessionId}")
    public ResponseEntity<Map<String, Object>> interrupt(@PathVariable String sessionId) {
        agentService.interrupt(sessionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "代理已中断"));
    }

    /**
     * 清空会话.
     */
    @DeleteMapping("/api/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearSession(@PathVariable String sessionId) {
        agentService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "会话已清空"));
    }
}
```

#### 14.4.2.12 审批管理控制器

```java
package io.agentscope.tutorial.chapter14.controller;

import io.agentscope.tutorial.chapter14.entity.ApprovalRequest;
import io.agentscope.tutorial.chapter14.service.ApprovalWorkflow;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 审批管理控制器.
 * 提供审批请求的查询和手动审批接口.
 */
@RestController
@RequestMapping("/api/approval")
public class ApprovalController {

    private final ApprovalWorkflow approvalWorkflow;

    public ApprovalController(ApprovalWorkflow approvalWorkflow) {
        this.approvalWorkflow = approvalWorkflow;
    }

    /**
     * 获取所有待审批请求（全局）.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalRequest>> getAllPending() {
        List<ApprovalRequest> pending = approvalWorkflow.getAllPendingRequests();
        return ResponseEntity.ok(pending);
    }

    /**
     * 获取指定会话的待审批请求.
     */
    @GetMapping("/pending/{sessionId}")
    public ResponseEntity<List<ApprovalRequest>> getPendingBySession(@PathVariable String sessionId) {
        List<ApprovalRequest> pending = approvalWorkflow.getPendingRequests(sessionId);
        return ResponseEntity.ok(pending);
    }

    /**
     * 获取审批请求详情.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequest> getById(@PathVariable Long id) {
        Optional<ApprovalRequest> opt = approvalWorkflow.findById(id);
        return opt.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 执行审批（手动审批接口）.
     * 可用于管理后台或审批流程系统调用.
     *
     * @param id       审批请求 ID
     * @param approved 审批决策
     * @param approver 审批人
     * @param comment  审批意见
     */
    @PostMapping("/{id}")
    public ResponseEntity<Map<String, Object>> approve(
            @PathVariable Long id,
            @RequestParam boolean approved,
            @RequestParam(defaultValue = "admin") String approver,
            @RequestParam(required = false) String comment) {

        Optional<ApprovalRequest> result =
                approvalWorkflow.approve(id, approved, approver, comment);

        if (result.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "审批请求不存在或状态不正确"));
        }

        ApprovalRequest request = result.get();
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "approvalId", request.getId(),
                        "status", request.getStatus().name(),
                        "approver", request.getApprover()));
    }

    /**
     * 批量审批：拒绝指定会话的所有待审批请求.
     * 通常用于会话超时或用户主动取消时调用.
     */
    @PostMapping("/reject-all/{sessionId}")
    public ResponseEntity<Map<String, Object>> rejectAll(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "admin") String approver,
            @RequestParam(required = false) String reason) {

        int count = approvalWorkflow.batchApproveBySession(
                sessionId, false, approver, reason != null ? reason : "批量拒绝");
        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "rejectedCount", count,
                        "sessionId", sessionId));
    }
}
```

#### 14.4.2.13 Spring Boot 启动类

```java
package io.agentscope.tutorial.chapter14;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 第十四章案例：人类在环（HITL）审批工作流 Spring Boot 启动类.
 *
 * <p>本案例演示了如何在 AgentScope Java 中实现完整的人类在环审批流程：
 * <ul>
 *   <li>敏感工具执行前自动暂停，等待人工审批</li>
 *   <li>审批请求持久化到 H2 数据库，支持管理后台查看</li>
 *   <li>审批结果通过 API 回传，代理自动恢复执行</li>
 *   <li>定时任务自动处理审批超时</li>
 * </ul>
 *
 * <h2>启动方式</h2>
 * <pre>
 * # 设置 DashScope API Key
 * export DASHSCOPE_API_KEY=your_api_key
 *
 * # 启动应用
 * mvn spring-boot:run
 * </pre>
 *
 * <h2>API 接口</h2>
 * <pre>
 * POST /api/chat                    # 发送聊天消息（SSE 流式）
 * POST /api/approve                 # 提交审批决策（SSE 流式恢复）
 * GET  /api/approval/pending         # 查看所有待审批请求
 * POST /api/approval/{id}           # 手动审批某个请求
 * DELETE /api/session/{sessionId}   # 清空会话
 * </pre>
 *
 * <h2>H2 控制台</h2>
 * <pre>
 * 访问 http://localhost:8080/h2-console
 * JDBC URL: jdbc:h2:mem:hitl_db
 * </pre>
 */
@SpringBootApplication
@EnableScheduling  // 启用定时任务（用于处理审批超时）
public class Chapter14Application {

    public static void main(String[] args) {
        // 检查必要的环境变量
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your_api_key_here")) {
            System.err.println();
            System.err.println("=".repeat(60));
            System.err.println("  错误：请设置 DASHSCOPE_API_KEY 环境变量");
            System.err.println("  示例：export DASHSCOPE_API_KEY=sk-xxxxxxx");
            System.err.println("=".repeat(60));
            System.err.println();
            System.err.println("提示：如果是 Windows 系统，请使用：");
            System.err.println("  set DASHSCOPE_API_KEY=sk-xxxxxxx");
            System.err.println("  mvn spring-boot:run");
            System.err.println();
            System.exit(1);
        }

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("  AgentScope Java - Chapter 14: 人类在环（HITL）");
        System.out.println("  审批工作流案例");
        System.out.println("=".repeat(60));
        System.out.println("  服务地址: http://localhost:8080");
        System.out.println("  H2 控制台: http://localhost:8080/h2-console");
        System.out.println("  API 文档: http://localhost:8080/api/approval/pending");
        System.out.println("=".repeat(60));
        System.out.println();

        SpringApplication.run(Chapter14Application.class, args);
    }
}
```

#### 14.4.2.14 Agent 配置 Bean

```java
package io.agentscope.tutorial.chapter14.config;

import io.agentscope.tutorial.chapter14.service.ApprovalWorkflow;
import io.agentscope.tutorial.chapter14.service.AgentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置类.
 * 配置 AgentService Bean，注入必要的依赖.
 */
@Configuration
public class AppConfig {

    @Value("${dashscope.api-key}")
    private String apiKey;

    /**
     * 配置 AgentService.
     *
     * @param approvalWorkflow 审批工作流服务（由 Spring 自动注入）
     * @return AgentService 实例
     */
    @Bean
    public AgentService agentService(ApprovalWorkflow approvalWorkflow) {
        return new AgentService(apiKey, approvalWorkflow);
    }
}
```

### 14.4.3 运行与测试

#### 启动应用

```bash
# 设置环境变量（Linux/macOS）
export DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxx

# 或 Windows PowerShell
$env:DASHSCOPE_API_KEY="sk-xxxxxxxxxxxxxxxx"

# 启动
mvn spring-boot:run
```

#### 测试场景

**场景 1：正常工具调用（不需要审批）**

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test-001","message":"帮我查询 ID 为 12345 的订单"}'
```

预期响应：
- 直接返回查询结果，不需要审批

**场景 2：发送邮件（需要审批）**

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test-002","message":"给 user@company.com 发送一封邮件，主题是会议通知，内容是明天下午3点有周会"}'
```

预期响应（SSE 事件流）：
```
type: REASONING, content: 正在处理... incremental: true
type: REASONING, content: 我需要使用 send_email 工具... incremental: false
type: PENDING_APPROVAL, toolCallId: "xxx", toolName: "send_email", toolInput: {...}
```

随后可以通过以下方式审批：

```bash
# 查看待审批请求
curl http://localhost:8080/api/approval/pending

# 批准邮件发送
curl -X POST http://localhost:8080/api/approval/1?approved=true&approver=admin

# 或者通过 approve 接口恢复代理
curl -X POST http://localhost:8080/api/approve \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test-002","toolCallId":"xxx","approved":true,"approver":"admin","comment":"邮件内容检查通过"}'
```

**场景 3：删除数据（需要审批）**

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"sessionId":"test-003","message":"删除 ID 为 999 的过期日志记录，原因：数据已归档"}'
```

### 14.4.4 数据库状态

通过 H2 控制台（http://localhost:8080/h2-console）可以查看审批请求表：

```sql
-- 查看所有审批请求
SELECT * FROM approval_requests ORDER BY created_at DESC;

-- 查看待审批请求
SELECT * FROM approval_requests WHERE status = 'PENDING';

-- 查看某个会话的审批历史
SELECT * FROM approval_requests WHERE session_id = 'test-002' ORDER BY created_at;
```

## 14.5 本章小结

本章介绍了 AgentScope Java 中人类在环（HITL）的核心机制：

- **ToolSuspendException**：工具层面主动挂起，等待外部注入结果
- **Hook stopAgent()**：基于 PostReasoningEvent 在推理完成后拦截，决定是否需要人工介入
- **审批工作流**：将审批请求持久化到数据库，支持异步审批、超时处理、批量审批
- **SSE 流式响应**：通过 Server-Sent Events 实时推送事件，前端可动态渲染审批界面

通过组合使用这些机制，可以构建出符合企业合规要求的 AI Agent 系统，在自动化效率和人工监督之间找到最佳平衡点。