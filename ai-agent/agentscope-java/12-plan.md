# 第十二章：计划与任务分解

在复杂任务的处理过程中，Agent 需要将一个笼统的目标分解为可执行的子任务，并按照计划逐步推进。AgentScope Java 提供了完整的 **计划系统（Plan System）**，通过 `PlanNotebook` 实现任务的分解、执行与状态追踪。本章将深入解析这一核心功能。

## 12.1 计划系统架构

### 12.1.1 核心组件

AgentScope Java 的计划系统由以下几个核心组件构成：

```
┌─────────────────────────────────────────────────────────────────┐
│                        PlanNotebook                             │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │    Plan     │  │  SubTask[]  │  │ PlanStorage │            │
│  │  (当前计划)  │  │  (子任务列表) │  │  (存储后端)   │            │
│  └─────────────┘  └─────────────┘  └─────────────┘            │
│         │                 │                 │                   │
│         ▼                 ▼                 ▼                   │
│  ┌─────────────────────────────────────────────────┐          │
│  │              PlanToHint (提示生成)                │          │
│  └─────────────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

- **PlanNotebook**：计划笔记本，负责管理当前计划、子任务状态、存储和提示生成
- **Plan**：计划实体，包含名称、描述、子任务列表和执行状态
- **SubTask**：子任务实体，包含名称、描述、预期结果、实际结果和状态
- **PlanStorage**：计划存储接口，支持内存、数据库等多种存储后端
- **PlanToHint**：计划到提示的转换器，根据计划状态生成上下文引导

### 12.1.2 计划状态模型

```
PlanState (计划状态):
├── TODO         - 计划已创建，未开始执行
├── IN_PROGRESS  - 计划正在执行中
├── DONE         - 计划已完成
└── ABANDONED    - 计划被放弃

SubTaskState (子任务状态):
├── TODO         - 子任务待执行
├── IN_PROGRESS  - 子任务执行中
├── DONE         - 子任务已完成
└── ABANDONED    - 子任务被放弃
```

### 12.1.3 工具函数体系

PlanNotebook 提供了 10 个内置工具函数，Agent 可通过调用这些工具来管理计划：

| 工具函数 | 功能描述 |
|---------|---------|
| `create_plan` | 创建新计划 |
| `update_plan_info` | 更新计划信息（名称、描述、预期结果） |
| `revise_current_plan` | 修订计划（增/删/改子任务） |
| `update_subtask_state` | 更新子任务状态 |
| `finish_subtask` | 完成子任务并记录结果 |
| `view_subtasks` | 查看子任务详情 |
| `get_subtask_count` | 获取子任务数量统计 |
| `finish_plan` | 完成或放弃计划 |
| `view_historical_plans` | 查看历史计划 |
| `recover_historical_plan` | 恢复历史计划 |

### 12.1.4 自动提示注入机制

PlanNotebook 通过 Hook 机制在每次推理前自动注入上下文提示，引导 Agent 正确执行计划。提示内容根据计划状态动态生成：

```java
// 配置 PlanNotebook，启用自动提示注入
PlanNotebook planNotebook = PlanNotebook.builder()
        .planToHint(new DefaultPlanToHint())  // 默认提示生成器
        .storage(new InMemoryPlanStorage())   // 内存存储
        .needUserConfirm(true)                 // 需要用户确认
        .maxSubtasks(10)                       // 最大子任务数
        .build();

// 将 PlanNotebook 关联到 Agent
ReActAgent agent = ReActAgent.builder()
        .name("Assistant")
        .model(model)
        .toolkit(toolkit)
        .planNotebook(planNotebook)
        .build();
```

## 12.2 任务分解策略

### 12.2.1 任务分解原则

有效的任务分解应遵循以下原则：

1. **原子性**：每个子任务应是一个不可分割的最小执行单元
2. **可验证性**：子任务应有明确的预期结果，便于验证完成状态
3. **顺序性**：子任务之间应存在逻辑上的先后依赖关系
4. **独立性**：子任务应尽量减少相互依赖，降低执行复杂度

### 12.2.2 创建计划的最佳实践

在创建计划时，应提供清晰的信息：

```java
// 创建计划的示例
String planName = "构建电商网站";
String description = "使用 Spring Boot + Vue 构建完整的电商平台，包含用户认证、商品管理、订单处理、支付集成等功能，目标用户为中小型商家";
String expectedOutcome = "可部署上线的电商网站，支持商品浏览、购物车、订单生成、支付跳转";

List<Map<String, Object>> subtasks = List.of(
        Map.of(
                "name", "项目初始化",
                "description", "创建 Spring Boot 项目结构，配置 Maven 依赖、数据库连接、JPA 实体",
                "expected_outcome", "项目可运行，数据库连接成功"
        ),
        Map.of(
                "name", "用户认证模块",
                "description", "实现用户注册、登录、JWT Token 认证、权限控制",
                "expected_outcome", "用户可完成注册登录，获取有效 Token"
        ),
        Map.of(
                "name", "商品管理模块",
                "description", "实现商品 CRUD、分类管理、搜索过滤、库存管理",
                "expected_outcome", "支持商品的增删改查，搜索响应时间 < 500ms"
        )
);
```

### 12.2.3 子任务状态转换规则

系统对子任务状态转换有严格的约束：

```
TODO ──────► IN_PROGRESS ──────► DONE
   │                │
   │                ▼
   └──────────► ABANDONED
```

**状态转换约束**：
- 只有当前子任务（index 最小的非完成子任务）才能标记为 `IN_PROGRESS`
- 标记子任务为完成时，系统会自动激活下一个待执行子任务
- 只能将状态更新为 `TODO`、`IN_PROGRESS` 或 `ABANDONED`，`DONE` 状态必须通过 `finish_subtask` 设置

### 12.2.4 计划修订策略

在计划执行过程中，可能需要修订计划：

```java
// 添加子任务
planNotebook.reviseCurrentPlan(2, "add",
        Map.of("name", "新子任务", "description", "描述", "expected_outcome", "结果"));

// 修改子任务
planNotebook.reviseCurrentPlan(1, "revise",
        Map.of("name", "修改后的名称", "description", "新描述", "expected_outcome", "新结果"));

// 删除子任务
planNotebook.reviseCurrentPlan(1, "delete", null);
```

## 12.3 计划执行与监控

### 12.3.1 执行流程

计划执行的标准流程如下：

```
1. 创建计划 (create_plan)
         │
         ▼
2. 用户确认后开始执行
         │
         ▼
3. 标记第一个子任务为 IN_PROGRESS (update_subtask_state)
         │
         ▼
4. 执行子任务 → 调用工具完成实际操作
         │
         ▼
5. 完成子任务 (finish_subtask) → 记录具体结果
         │
         ▼
6. 系统自动激活下一个子任务
         │
         ▼
7. 重复 4-6 直至所有子任务完成
         │
         ▼
8. 完成计划 (finish_plan) → 总结整个过程
```

### 12.3.2 实时状态监控

通过 Hook 机制可以实时监控计划状态变化：

```java
// 注册计划变更监听器
planNotebook.addChangeHook("monitor", (notebook, plan) -> {
    if (plan != null) {
        System.out.println("计划变更: " + plan.getName());
        System.out.println("子任务数: " + plan.getSubtasks().size());
        // 触发前端更新、发送通知等操作
    }
});
```

### 12.3.3 历史计划管理

完成或放弃的计划会自动保存到历史记录：

```java
// 查看历史计划
planNotebook.viewHistoricalPlans()
        .subscribe(plans -> {
            for (Plan p : plans) {
                System.out.println(p.getName() + " - " + p.getState());
            }
        });

// 恢复历史计划
planNotebook.recoverHistoricalPlan(planId);
```

### 12.3.4 计划与存储

PlanNotebook 支持多种存储后端实现：

```java
// 内存存储（默认）
PlanStorage memoryStorage = new InMemoryPlanStorage();

// 自定义数据库存储
PlanStorage dbStorage = new JdbcPlanStorage(dataSource);
```

**实现自定义存储示例**：

```java
/**
 * H2 数据库计划存储实现
 */
public class JdbcPlanStorage implements PlanStorage {
    private final DataSource dataSource;

    @Override
    public Mono<Void> addPlan(Plan plan) {
        return Mono.fromRunnable(() -> {
            String sql = "INSERT INTO plans (id, name, data, created_at) VALUES (?, ?, ?, ?)";
            try (var conn = dataSource.getConnection();
                 var stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, plan.getId());
                stmt.setString(2, plan.getName());
                stmt.setString(3, new ObjectMapper().writeValueAsString(plan));
                stmt.setString(4, plan.getCreatedAt());
                stmt.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    // ... 其他方法实现
}
```

## 12.4 【案例】自动化任务规划系统

本案例实现一个完整的任务规划系统，包含：
- Spring Boot 3 + Java 21 Web 服务
- H2 内存数据库存储计划数据
- SSE 实时推送计划状态
- 交互式计划管理与执行

### 12.4.1 项目结构

```
src/main/java/io/agentscope/tutorial/chapter12/
├── Chapter12Application.java           # Spring Boot 启动类
├── config/
│   └── AppConfig.java                 # 应用配置
├── controller/
│   ├── ChatController.java            # 对话控制器
│   └── PlanController.java            # 计划管理控制器
├── dto/
│   ├── ChatRequest.java               # 聊天请求 DTO
│   ├── PlanRequest.java               # 计划请求 DTO
│   └── SubTaskRequest.java            # 子任务请求 DTO
├── entity/
│   └── PlanEntity.java                # JPA 实体（持久化计划）
├── repository/
│   └── PlanRepository.java            # JPA 仓库
├── service/
│   ├── PlanDbService.java             # 计划数据库服务
│   └── AgentService.java              # Agent 服务
└── hook/
    └── PlanChangeHook.java           # 计划变更监听 Hook
```

### 12.4.2 Maven 依赖 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.agentscope</groupId>
        <artifactId>agentscope-parent</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>chapter12-plan-tutorial</artifactId>
    <packaging>jar</packaging>
    <name>Chapter 12 - Plan and Task Decomposition Tutorial</name>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA + H2 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- AgentScope Core -->
        <dependency>
            <groupId>io.agentscope</groupId>
            <artifactId>agentscope-core</artifactId>
            <version>${revision}</version>
        </dependency>

        <!-- Reactor for reactive streams -->
        <dependency>
            <groupId>reactor-core</groupId>
            <artifactId>reactor-core</artifactId>
        </dependency>

        <!-- Jackson for JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
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

### 12.4.3 应用配置文件 (application.yml)

```yaml
# 第十二章：计划与任务分解 - 配置文件

server:
  port: 8080

spring:
  application:
    name: chapter12-plan-tutorial

  # H2 数据库配置
  datasource:
    url: jdbc:h2:mem:plan_db           # 内存数据库
    driver-class-name: org.h2.Driver
    username: sa
    password:

  # JPA 配置
  jpa:
    hibernate:
      ddl-auto: update                  # 自动创建/更新表结构
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

  # H2 Console（开发时可选）
  h2:
    console:
      enabled: true
      path: /h2-console

# AgentScope 配置
agentscope:
  model:
    api-key: ${DASHSCOPE_API_KEY:your_api_key_here}
    model-name: qwen-plus

logging:
  level:
    io.agentscope: DEBUG
    io.agentscope.tutorial.chapter12: INFO
```

### 12.4.4 Spring Boot 启动类

```java
package io.agentscope.tutorial.chapter12;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第十二章：计划与任务分解 - Spring Boot 启动类
 *
 * 本应用展示如何使用 AgentScope Java 实现自动化任务规划系统。
 *
 * <p>主要功能：
 * <ul>
 *   <li>与 Agent 进行多轮对话，支持复杂任务的计划创建与执行</li>
 *   <li>实时监控计划状态变化，通过 SSE 推送给前端</li>
 *   <li>使用 H2 数据库持久化存储计划和历史记录</li>
 *   <li>提供 REST API 管理计划（创建、修订、完成）</li>
 * </ul>
 *
 * <p>访问地址：
 * <ul>
 *   <li>主页: http://localhost:8080</li>
 *   <li>H2 Console: http://localhost:8080/h2-console</li>
 *   <li>API 文档: http://localhost:8080/swagger-ui.html (需添加 springdoc 依赖)</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter12Application {

    public static void main(String[] args) {
        // 打印启动Banner
        printBanner();
        SpringApplication.run(Chapter12Application.class, args);
        printUsage();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║     AgentScope Java - 第十二章：计划与任务分解           ║");
        System.out.println("║     Chapter 12: Plan and Task Decomposition              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    private static void printUsage() {
        System.out.println("┌──────────────────────────────────────────────────────────┐");
        System.out.println("│                    服务已启动成功                         │");
        System.out.println("└──────────────────────────────────────────────────────────┘");
        System.out.println();
        System.out.println("  访问地址:");
        System.out.println("    • 首页:     http://localhost:8080");
        System.out.println("    • H2 Console: http://localhost:8080/h2-console");
        System.out.println();
        System.out.println("  API 接口:");
        System.out.println("    • POST /api/chat          - 与 Agent 对话 (SSE 流式)");
        System.out.println("    • GET  /api/plan          - 获取当前计划");
        System.out.println("    • POST /api/plan          - 创建新计划");
        System.out.println("    • PUT  /api/plan          - 更新计划信息");
        System.out.println("    • DELETE /api/plan/subtasks/{idx} - 删除子任务");
        System.out.println("    • POST /api/plan/finish  - 完成计划");
        System.out.println();
        System.out.println("  环境变量:");
        System.out.println("    • DASHSCOPE_API_KEY - 阿里云百炼 API Key");
        System.out.println();
    }
}
```

### 12.4.5 JPA 实体类 (PlanEntity.java)

```java
package io.agentscope.tutorial.chapter12.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 计划实体类 - 对应数据库中的 plans 表
 *
 * <p>用于将 Plan 对象持久化到 H2 数据库中，支持历史计划的查询和恢复。
 *
 * <p>数据库表结构：
 * <ul>
 *   <li>id - UUID 主键</li>
 *   <li>name - 计划名称</li>
 *   <li>description - 计划描述</li>
 *   <li>expected_outcome - 预期结果</li>
 *   <li>actual_outcome - 实际完成结果</li>
 *   <li>state - 计划状态 (TODO/IN_PROGRESS/DONE/ABANDONED)</li>
 *   <li>subtasks_json - 子任务列表的 JSON 序列化</li>
 *   <li>created_at - 创建时间</li>
 *   <li>finished_at - 完成时间</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Entity
@Table(name = "plans")
public class PlanEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "expected_outcome", length = 1000)
    private String expectedOutcome;

    @Column(name = "actual_outcome", length = 2000)
    private String actualOutcome;

    @Column(name = "state", length = 20)
    private String state;

    @Column(name = "subtasks_json", length = 10000)
    private String subtasksJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // 构造函数
    public PlanEntity() {
        this.createdAt = LocalDateTime.now();
    }

    public PlanEntity(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    // Getter 和 Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public String getActualOutcome() {
        return actualOutcome;
    }

    public void setActualOutcome(String actualOutcome) {
        this.actualOutcome = actualOutcome;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getSubtasksJson() {
        return subtasksJson;
    }

    public void setSubtasksJson(String subtasksJson) {
        this.subtasksJson = subtasksJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    @Override
    public String toString() {
        return "PlanEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
```

### 12.4.6 JPA 仓库接口 (PlanRepository.java)

```java
package io.agentscope.tutorial.chapter12.repository;

import io.agentscope.tutorial.chapter12.entity.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 计划数据仓库 - 提供计划的持久化操作
 *
 * <p>继承 Spring Data JPA 的 JpaRepository，提供常用的 CRUD 操作。
 *
 * @author AgentScope Tutorial
 */
@Repository
public interface PlanRepository extends JpaRepository<PlanEntity, String> {

    /**
     * 根据状态查询计划列表
     *
     * @param state 计划状态
     * @return 符合条件的计划列表
     */
    List<PlanEntity> findByState(String state);

    /**
     * 根据名称模糊查询计划
     *
     * @param namePattern 名称匹配模式
     * @return 符合条件的计划列表
     */
    List<PlanEntity> findByNameContaining(String namePattern);

    /**
     * 查询所有计划，按创建时间倒序排列
     *
     * @return 所有计划列表
     */
    List<PlanEntity> findAllByOrderByCreatedAtDesc();

    /**
     * 删除指定时间之前创建的计划（清理历史）
     *
     * @param beforeTime 删除此时间之前的计划
     * @return 删除的计划数量
     */
    long deleteByCreatedAtBefore(java.time.LocalDateTime beforeTime);
}
```

### 12.4.7 计划数据库服务 (PlanDbService.java)

```java
package io.agentscope.tutorial.chapter12.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.plan.model.Plan;
import io.agentscope.core.plan.model.PlanState;
import io.agentscope.core.plan.model.SubTask;
import io.agentscope.core.plan.model.SubTaskState;
import io.agentscope.core.plan.storage.PlanStorage;
import io.agentscope.tutorial.chapter12.entity.PlanEntity;
import io.agentscope.tutorial.chapter12.repository.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 计划数据库服务 - 实现基于 H2 数据库的计划存储
 *
 * <p>将 Plan 对象与 PlanEntity 互相转换，并提供持久化操作。
 * 实现了 PlanStorage 接口，可直接用于 PlanNotebook。
 *
 * @author AgentScope Tutorial
 */
@Service
public class PlanDbService implements PlanStorage {

    private static final Logger log = LoggerFactory.getLogger(PlanDbService.class);

    private final PlanRepository planRepository;
    private final ObjectMapper objectMapper;

    public PlanDbService(PlanRepository planRepository, ObjectMapper objectMapper) {
        this.planRepository = planRepository;
        this.objectMapper = objectMapper;
        log.info("PlanDbService initialized with H2 database storage");
    }

    /**
     * 添加计划到数据库
     *
     * @param plan 要保存的计划
     * @return 保存完成的通知
     */
    @Override
    public Mono<Void> addPlan(Plan plan) {
        return Mono.fromRunnable(() -> {
            try {
                PlanEntity entity = toEntity(plan);
                planRepository.save(entity);
                log.info("Plan saved to database: {} (id={})", plan.getName(), plan.getId());
            } catch (Exception e) {
                log.error("Failed to save plan to database", e);
                throw new RuntimeException("Failed to save plan", e);
            }
        });
    }

    /**
     * 根据 ID 获取计划
     *
     * @param planId 计划 ID
     * @return 计划对象（如果不存在则返回空）
     */
    @Override
    public Mono<Plan> getPlan(String planId) {
        return Mono.fromCallable(() -> {
            Optional<PlanEntity> entity = planRepository.findById(planId);
            return entity.map(this::toPlan).orElse(null);
        });
    }

    /**
     * 获取所有历史计划
     *
     * @return 计划列表
     */
    @Override
    public Mono<List<Plan>> getPlans() {
        return Mono.fromCallable(() -> {
            List<PlanEntity> entities = planRepository.findAllByOrderByCreatedAtDesc();
            List<Plan> plans = new ArrayList<>();
            for (PlanEntity entity : entities) {
                plans.add(toPlan(entity));
            }
            return plans;
        });
    }

    // ==================== 实体转换方法 ====================

    /**
     * 将 Plan 实体转换为 PlanEntity
     */
    private PlanEntity toEntity(Plan plan) {
        PlanEntity entity = new PlanEntity();
        entity.setId(plan.getId());
        entity.setName(plan.getName());
        entity.setDescription(plan.getDescription());
        entity.setExpectedOutcome(plan.getExpectedOutcome());
        entity.setActualOutcome(plan.getOutcome());
        entity.setState(plan.getState().getValue());
        entity.setCreatedAt(LocalDateTime.now());

        // 序列化子任务为 JSON
        if (plan.getSubtasks() != null) {
            try {
                List<SubTaskDto> dtos = new ArrayList<>();
                for (SubTask subtask : plan.getSubtasks()) {
                    SubTaskDto dto = new SubTaskDto();
                    dto.name = subtask.getName();
                    dto.description = subtask.getDescription();
                    dto.expectedOutcome = subtask.getExpectedOutcome();
                    dto.outcome = subtask.getOutcome();
                    dto.state = subtask.getState().getValue();
                    dto.createdAt = subtask.getCreatedAt();
                    dto.finishedAt = subtask.getFinishedAt();
                    dtos.add(dto);
                }
                entity.setSubtasksJson(objectMapper.writeValueAsString(dtos));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize subtasks", e);
            }
        }

        // 设置完成时间
        if (plan.getState() == PlanState.DONE || plan.getState() == PlanState.ABANDONED) {
            entity.setFinishedAt(LocalDateTime.now());
        }

        return entity;
    }

    /**
     * 将 PlanEntity 转换为 Plan
     */
    private Plan toPlan(PlanEntity entity) {
        Plan plan = new Plan();
        plan.setId(entity.getId());
        plan.setName(entity.getName());
        plan.setDescription(entity.getDescription());
        plan.setExpectedOutcome(entity.getExpectedOutcome());
        plan.setOutcome(entity.getActualOutcome());

        // 解析状态
        try {
            plan.setState(PlanState.valueOf(entity.getState().toUpperCase()));
        } catch (IllegalArgumentException e) {
            plan.setState(PlanState.TODO);
        }

        // 解析子任务 JSON
        if (entity.getSubtasksJson() != null && !entity.getSubtasksJson().isEmpty()) {
            try {
                List<SubTaskDto> dtos = objectMapper.readValue(
                        entity.getSubtasksJson(),
                        new TypeReference<List<SubTaskDto>>() {}
                );
                List<SubTask> subtasks = new ArrayList<>();
                for (SubTaskDto dto : dtos) {
                    SubTask subtask = new SubTask(
                            dto.name != null ? dto.name : "Unnamed",
                            dto.description != null ? dto.description : "",
                            dto.expectedOutcome != null ? dto.expectedOutcome : ""
                    );
                    if (dto.outcome != null) {
                        subtask.setOutcome(dto.outcome);
                    }
                    if (dto.state != null) {
                        try {
                            subtask.setState(SubTaskState.valueOf(dto.state.toUpperCase()));
                        } catch (IllegalArgumentException ignored) {
                            subtask.setState(SubTaskState.TODO);
                        }
                    }
                    subtask.setCreatedAt(dto.createdAt);
                    subtask.setFinishedAt(dto.finishedAt);
                    subtasks.add(subtask);
                }
                plan.setSubtasks(subtasks);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse subtasks JSON", e);
            }
        }

        return plan;
    }

    // 内部 DTO 类用于 JSON 序列化
    private static class SubTaskDto {
        public String name;
        public String description;
        public String expectedOutcome;
        public String outcome;
        public String state;
        public String createdAt;
        public String finishedAt;
    }

    // ==================== 额外业务方法 ====================

    /**
     * 保存当前计划（用于状态同步）
     */
    public Mono<Void> saveCurrentPlan(Plan plan) {
        return addPlan(plan);
    }

    /**
     * 获取最近的 N 条历史计划
     */
    public Mono<List<Plan>> getRecentPlans(int limit) {
        return Mono.fromCallable(() -> {
            List<PlanEntity> entities = planRepository.findAllByOrderByCreatedAtDesc();
            List<Plan> plans = new ArrayList<>();
            int count = 0;
            for (PlanEntity entity : entities) {
                if (count++ >= limit) break;
                plans.add(toPlan(entity));
            }
            return plans;
        });
    }

    /**
     * 获取计划统计信息
     */
    public Mono<PlanStatistics> getStatistics() {
        return Mono.fromCallable(() -> {
            long total = planRepository.count();
            long done = planRepository.findByState(PlanState.DONE.getValue()).size();
            long abandoned = planRepository.findByState(PlanState.ABANDONED.getValue()).size();
            long inProgress = planRepository.findByState(PlanState.IN_PROGRESS.getValue()).size();
            long todo = planRepository.findByState(PlanState.TODO.getValue()).size();
            return new PlanStatistics(total, done, abandoned, inProgress, todo);
        });
    }

    /**
     * 计划统计信息
     */
    public record PlanStatistics(
            long total,
            long done,
            long abandoned,
            long inProgress,
            long todo
    ) {}
}
```

### 12.4.8 Agent 服务类 (AgentService.java)

```java
package io.agentscope.tutorial.chapter12.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.agent.StreamOptions;
import io.agentscope.core.formatter.dashscope.DashScopeChatFormatter;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.*;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import io.agentscope.core.plan.model.SubTask;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent 服务 - 管理 ReActAgent 和计划执行
 *
 * <p>核心职责：
 * <ul>
 *   <li>初始化和配置 ReActAgent</li>
 *   <li>提供对话和流式响应能力</li>
 *   <li>管理计划状态和变更通知</li>
 *   <li>处理用户交互（暂停/恢复/停止）</li>
 * </ul>
 *
 * <p>通过 Hook 机制实现：
 * <ul>
 *   <li>计划变更监听 - 实时更新前端</li>
 *   <li>工具调用拦截 - 捕获工具输入输出</li>
 *   <li>推理上下文捕获 - 调试和问题排查</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Service
public class AgentService implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    /** SSE 上下文消息最大字符数 */
    private static final int CTX_FLAT_MAX_CHARS = 48_000;

    /** SSE 上下文字段最大字符数 */
    private static final int CTX_FIELD_MAX_CHARS = 8_000;

    /** 计划工具名称集合（用于暂停检测） */
    private static final Set<String> PLAN_TOOL_NAMES = Set.of(
            "create_plan", "update_plan_info", "revise_current_plan",
            "update_subtask_state", "finish_subtask", "get_subtask_count",
            "finish_plan", "view_subtasks", "view_historical_plans", "recover_historical_plan"
    );

    @Value("${agentscope.model.api-key}")
    private String apiKey;

    @Value("${agentscope.model.model-name:qwen-plus}")
    private String modelName;

    private final PlanDbService planDbService;
    private final ObjectMapper objectMapper;

    private String dashscopeApiKey;
    private ReActAgent agent;
    private PlanNotebook planNotebook;
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    /** 待处理的工具输入（用于 SSE 展示） */
    private final ConcurrentLinkedQueue<Map<String, Object>> pendingToolInputs = new ConcurrentLinkedQueue<>();

    /** 待处理的上下文行（用于 SSE 调试） */
    private final ConcurrentLinkedQueue<String> pendingContextLines = new ConcurrentLinkedQueue<>();
    private final AtomicInteger contextSeq = new AtomicInteger(0);

    /** 计划变更通知器 */
    private final List<Runnable> planChangeListeners = new ArrayList<>();

    public AgentService(PlanDbService planDbService, ObjectMapper objectMapper) {
        this.planDbService = planDbService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 从环境变量获取 API Key（优先级高于配置）
        dashscopeApiKey = System.getenv("DASHSCOPE_API_KEY");
        if (dashscopeApiKey == null || dashscopeApiKey.isEmpty()) {
            dashscopeApiKey = apiKey;
        }

        if (dashscopeApiKey == null || dashscopeApiKey.isEmpty() ||
            dashscopeApiKey.equals("your_api_key_here")) {
            log.warn("DASHSCOPE_API_KEY not configured, agent will use placeholder");
        }

        initializeAgent();
        log.info("AgentService initialized successfully");
    }

    /**
     * 初始化 Agent
     */
    private void initializeAgent() {
        // 清空待处理状态
        pendingToolInputs.clear();
        pendingContextLines.clear();
        contextSeq.set(0);

        // 创建内存记忆
        var memory = new InMemoryMemory();

        // 创建工具集
        var toolkit = new Toolkit();
        toolkit.registerTool(new FileOperationTool());

        // 创建计划笔记本（使用数据库存储）
        planNotebook = PlanNotebook.builder()
                .storage(planDbService)
                .needUserConfirm(true)
                .maxSubtasks(15)
                .build();

        // 注册计划变更监听器
        planNotebook.addChangeHook("dbService", (notebook, plan) -> {
            notifyPlanChange(plan);
        });

        // 创建计划工具 Hook（用于暂停和状态监控）
        Hook planToolHook = new Hook() {
            @Override
            public <T extends HookEvent> Mono<T> onEvent(T event) {
                if (event instanceof PostActingEvent postActing) {
                    ToolUseBlock toolUse = postActing.getToolUse();
                    if (toolUse != null && toolUse.getInput() != null) {
                        pendingToolInputs.offer(new LinkedHashMap<>(toolUse.getInput()));
                    }
                    if (toolUse != null && PLAN_TOOL_NAMES.contains(toolUse.getName())) {
                        if (stopRequested.compareAndSet(true, false)) {
                            log.info("Plan tool executed, pausing for user review: {}", toolUse.getName());
                            isPaused.set(true);
                            postActing.stopAgent();
                        }
                    }
                }
                return Mono.just(event);
            }
        };

        // 创建上下文捕获 Hook（用于调试）
        Hook promptCaptureHook = new Hook() {
            @Override
            public int priority() {
                return 1000;
            }

            @Override
            public <T extends HookEvent> Mono<T> onEvent(T event) {
                if (event instanceof PreReasoningEvent pre) {
                    offerContextLine("reasoning", pre.getModelName(), pre.getInputMessages());
                }
                return Mono.just(event);
            }
        };

        // 构建 Agent
        agent = ReActAgent.builder()
                .name("PlanAssistant")
                .sysPrompt(buildSystemPrompt())
                .model(DashScopeChatModel.builder()
                        .apiKey(dashscopeApiKey)
                        .modelName(modelName)
                        .stream(true)
                        .enableThinking(true)
                        .defaultOptions(GenerateOptions.builder()
                                .thinkingBudget(8192)
                                .build())
                        .formatter(new DashScopeChatFormatter())
                        .build())
                .memory(memory)
                .toolkit(toolkit)
                .maxIters(50)
                .hook(planToolHook)
                .hook(promptCaptureHook)
                .planNotebook(planNotebook)
                .build();

        log.info("Agent initialized with model: {}", modelName);
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
                你是一个系统性助手，通过结构化计划帮助用户完成复杂任务。

                核心能力：
                1. 复杂任务规划：当用户提出复杂请求时（如编程、调研、设计），主动创建计划
                2. 任务分解：将大任务分解为可执行的小步骤
                3. 进度追踪：实时更新任务状态，确保按计划执行
                4. 结果验证：完成每个子任务后，验证是否达成预期结果

                工作流程：
                1. 理解用户需求
                2. 如需要，创建详细计划（使用 create_plan 工具）
                3. 逐步执行子任务（使用 update_subtask_state 标记进度）
                4. 完成后记录结果（使用 finish_subtask）
                5. 计划全部完成后总结（使用 finish_plan）

                注意事项：
                - 每个子任务完成后必须调用 finish_subtask 记录具体结果
                - 结果应该是具体的数据、信息或文件路径，而非"已完成"
                - 用户可能随时修改计划，需要灵活应对
                - 如果用户表示不需要继续执行计划，使用 finish_plan 放弃
                """;
    }

    /**
     * 发送消息并获取流式响应
     */
    public Flux<String> chat(String sessionId, String message) {
        // 重置暂停状态
        isPaused.set(false);
        pendingToolInputs.clear();
        pendingContextLines.clear();
        contextSeq.set(0);

        // 构建用户消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(message).build())
                .build();

        // 执行对话并附加上下文
        return attachPendingContext(
                agent.stream(userMsg, createStreamOptions())
                        .subscribeOn(Schedulers.boundedElastic())
        );
    }

    /**
     * 恢复暂停中的 Agent
     */
    public Flux<String> resume(String sessionId) {
        if (isPaused.compareAndSet(true, false)) {
            log.info("Resuming agent execution");
            pendingToolInputs.clear();
            pendingContextLines.clear();
            contextSeq.set(0);

            return attachPendingContext(
                    agent.stream(createStreamOptions())
                            .subscribeOn(Schedulers.boundedElastic())
            );
        } else {
            log.warn("Agent is not paused");
            return Flux.just("Agent is not paused or already resuming.");
        }
    }

    /**
     * 请求停止 Agent（在下一个计划工具执行后暂停）
     */
    public void requestStop() {
        log.info("Stop requested - will pause after next plan tool");
        stopRequested.set(true);
    }

    /**
     * 检查 Agent 是否暂停
     */
    public boolean isPaused() {
        return isPaused.get();
    }

    /**
     * 重置 Agent
     */
    public void reset() {
        log.info("Resetting agent");
        isPaused.set(false);
        stopRequested.set(false);
        initializeAgent();
        notifyPlanChange(null);
    }

    /**
     * 获取当前计划
     */
    public Plan getCurrentPlan() {
        return planNotebook != null ? planNotebook.getCurrentPlan() : null;
    }

    // ==================== 私有辅助方法 ====================

    private StreamOptions createStreamOptions() {
        return StreamOptions.builder()
                .eventTypes(EventType.REASONING, EventType.TOOL_RESULT, EventType.AGENT_RESULT)
                .incremental(true)
                .includeActingChunk(false)
                .build();
    }

    private Flux<String> attachPendingContext(Flux<Event> events) {
        return events.concatMap(event -> {
            List<String> prefix = drainPendingContextLines();
            String mapped = mapEventToString(event);
            boolean hasPrefix = !prefix.isEmpty();
            boolean hasBody = mapped != null && !mapped.isEmpty();
            if (!hasPrefix && !hasBody) {
                return Flux.empty();
            }
            Flux<String> head = Flux.fromIterable(prefix);
            return hasBody ? head.concatWith(Flux.just(mapped)) : head;
        });
    }

    private List<String> drainPendingContextLines() {
        List<String> out = new ArrayList<>();
        String line;
        while ((line = pendingContextLines.poll()) != null) {
            out.add(line);
        }
        return out;
    }

    private void offerContextLine(String phase, String modelName, List<Msg> messages) {
        try {
            int seq = contextSeq.incrementAndGet();
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("t", "ctx");
            root.put("phase", phase);
            root.put("seq", seq);
            root.put("model", modelName != null ? modelName : "");
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Msg m : messages) {
                rows.add(msgToDebugRow(m));
            }
            root.put("messages", rows);
            String flat = truncateIfNeeded(buildFlatTranscript(messages), CTX_FLAT_MAX_CHARS);
            root.put("flat", flat);
            pendingContextLines.offer(objectMapper.writeValueAsString(root));
        } catch (Exception e) {
            log.warn("Failed to serialize context for SSE: {}", e.getMessage());
        }
    }

    private Map<String, Object> msgToDebugRow(Msg m) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("role", m.getRole().name());
        if (m.getName() != null) {
            row.put("name", m.getName());
        }
        List<Object> parts = new ArrayList<>();
        for (ContentBlock b : m.getContent()) {
            parts.add(contentBlockToDebugMap(b));
        }
        row.put("content", parts);
        return row;
    }

    private Object contentBlockToDebugMap(ContentBlock b) {
        if (b instanceof TextBlock tb) {
            return Map.of("type", "text", "text", truncateIfNeeded(tb.getText(), CTX_FIELD_MAX_CHARS));
        }
        if (b instanceof ThinkingBlock th) {
            return Map.of("type", "thinking", "thinking", truncateIfNeeded(th.getThinking(), CTX_FIELD_MAX_CHARS));
        }
        if (b instanceof ToolUseBlock tu) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "tool_use");
            m.put("id", tu.getId());
            m.put("name", tu.getName());
            m.put("input", tu.getInput() != null ? tu.getInput() : Map.of());
            return m;
        }
        if (b instanceof ToolResultBlock tr) {
            return Map.of("type", "tool_result", "name", tr.getName(), "output",
                    truncateIfNeeded(flattenToolOutput(tr), CTX_FIELD_MAX_CHARS));
        }
        return Map.of("type", "other");
    }

    private String buildFlatTranscript(List<Msg> messages) {
        StringBuilder sb = new StringBuilder();
        for (Msg m : messages) {
            sb.append("--- ").append(m.getRole().name()).append(" ---\n");
            for (ContentBlock b : m.getContent()) {
                appendContentBlockForFlat(sb, b);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private void appendContentBlockForFlat(StringBuilder sb, ContentBlock o) {
        if (o instanceof TextBlock tb) {
            sb.append(tb.getText() != null ? tb.getText() : "");
        } else if (o instanceof ThinkingBlock th) {
            sb.append(th.getThinking() != null ? th.getThinking() : "");
        } else if (o instanceof ToolUseBlock tu) {
            sb.append("[tool:").append(tu.getName()).append("] ");
            sb.append(tu.getInput() != null ? tu.getInput().toString() : "{}");
        } else if (o instanceof ToolResultBlock tr) {
            sb.append("[result:").append(tr.getName()).append("] ");
            sb.append(flattenToolOutput(tr));
        }
    }

    private String mapEventToString(Event event) {
        try {
            // 处理暂停状态
            if (event.getType() == EventType.AGENT_RESULT) {
                Msg msg = event.getMessage();
                if (msg != null && msg.getGenerateReason() == GenerateReason.ACTING_STOP_REQUESTED) {
                    isPaused.set(true);
                    return objectMapper.writeValueAsString(Map.of("t", "paused"));
                }
                return "";
            }

            // 处理工具结果
            if (event.getType() == EventType.TOOL_RESULT) {
                Msg msg = event.getMessage();
                if (msg == null) return "";
                List<ToolResultBlock> blocks = msg.getContentBlocks(ToolResultBlock.class);
                if (blocks.isEmpty()) return "";
                ToolResultBlock tr = blocks.get(0);
                Map<String, Object> payload = new LinkedHashMap<>(4);
                payload.put("t", "tool");
                payload.put("n", tr.getName() != null ? tr.getName() : "");
                payload.put("d", flattenToolOutput(tr));
                Map<String, Object> inputSnapshot = pendingToolInputs.poll();
                if (inputSnapshot != null && !inputSnapshot.isEmpty()) {
                    payload.put("i", inputSnapshot);
                }
                return objectMapper.writeValueAsString(payload);
            }

            if (event.isLast()) return "";

            Msg msg = event.getMessage();
            if (msg == null) return "";

            // 处理思维内容
            List<ThinkingBlock> thinkingBlocks = msg.getContentBlocks(ThinkingBlock.class);
            if (!thinkingBlocks.isEmpty()) {
                String delta = thinkingBlocks.get(0).getThinking();
                if (delta != null && !delta.isEmpty()) {
                    return objectMapper.writeValueAsString(Map.of("t", "think", "d", delta));
                }
            }

            // 处理文本内容
            List<TextBlock> textBlocks = msg.getContentBlocks(TextBlock.class);
            if (!textBlocks.isEmpty()) {
                String delta = textBlocks.get(0).getText();
                if (delta != null && !delta.isEmpty()) {
                    return objectMapper.writeValueAsString(Map.of("t", "text", "d", delta));
                }
            }
            return "";
        } catch (Exception e) {
            log.warn("Failed to encode SSE chunk: {}", e.getMessage());
            return "";
        }
    }

    private String flattenToolOutput(ToolResultBlock block) {
        StringBuilder sb = new StringBuilder();
        for (ContentBlock o : block.getOutput()) {
            if (o instanceof TextBlock tb) {
                sb.append(tb.getText());
            }
        }
        return sb.toString();
    }

    private String truncateIfNeeded(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "\n...(truncated)";
    }

    /**
     * 通知计划变更
     */
    private void notifyPlanChange(Plan plan) {
        synchronized (planChangeListeners) {
            for (Runnable listener : planChangeListeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    log.error("Error in plan change listener", e);
                }
            }
        }
    }

    /**
     * 注册计划变更监听器
     */
    public void addPlanChangeListener(Runnable listener) {
        synchronized (planChangeListeners) {
            planChangeListeners.add(listener);
        }
    }

    // ==================== 内置工具 ====================

    /**
     * 文件操作工具 - 用于演示计划执行
     */
    public static class FileOperationTool {

        @Tool(name = "write_file", description = "将内容写入文件")
        public Mono<String> writeFile(
                @ToolParam(name = "filename", description = "文件名") String filename,
                @ToolParam(name = "content", description = "文件内容") String content) {
            log.info("[write_file] filename={}, content_length={}", filename, content.length());
            return Mono.just("File written successfully: " + filename + " (" + content.length() + " chars)");
        }

        @Tool(name = "read_file", description = "读取文件内容")
        public Mono<String> readFile(
                @ToolParam(name = "filename", description = "文件名") String filename) {
            log.info("[read_file] filename={}", filename);
            return Mono.just("File content for: " + filename);
        }

        @Tool(name = "calculate", description = "执行数学计算")
        public Mono<String> calculate(
                @ToolParam(name = "expression", description = "数学表达式") String expression) {
            log.info("[calculate] {}", expression);
            try {
                double result = evaluateSimpleExpression(expression);
                return Mono.just(expression + " = " + result);
            } catch (Exception e) {
                return Mono.just("Error: " + e.getMessage());
            }
        }

        private double evaluateSimpleExpression(String expr) {
            // 简化版表达式计算
            expr = expr.replaceAll("\\s+", "");
            String[] tokens = expr.split("(?=[+\\-*/])|(?<=[+\\-*/])");
            double result = 0;
            String op = "+";
            for (String token : tokens) {
                if (token.matches("[+\\-*/]")) {
                    op = token;
                } else if (!token.isEmpty()) {
                    double value = Double.parseDouble(token);
                    result = switch (op) {
                        case "+" -> result + value;
                        case "-" -> result - value;
                        case "*" -> result * value;
                        case "/" -> result / value;
                        default -> result;
                    };
                }
            }
            return result;
        }
    }
}
```

### 12.4.9 计划变更 Hook (PlanChangeHook.java)

```java
package io.agentscope.tutorial.chapter12.hook;

import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 计划变更监听 Hook
 *
 * <p>监听计划状态变化，执行必要的业务逻辑：
 * <ul>
 *   <li>记录计划变更日志</li>
 *   <li>更新缓存</li>
 *   <li>触发下游系统更新</li>
 * </ul>
 *
 * <p>通过在 PlanNotebook 中注册，可以捕获以下事件：
 * <ul>
 *   <li>创建新计划</li>
 *   <li>添加/修改/删除子任务</li>
 *   <li>子任务状态变更</li>
 *   <li>计划完成或放弃</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@Component
public class PlanChangeHook {

    private static final Logger log = LoggerFactory.getLogger(PlanChangeHook.class);

    /**
     * 注册到 PlanNotebook 的变更回调
     *
     * @param planNotebook 计划笔记本实例
     */
    public void register(PlanNotebook planNotebook) {
        planNotebook.addChangeHook("loggingHook", this::onPlanChanged);
    }

    /**
     * 计划变更处理方法
     *
     * @param notebook 计划笔记本实例
     * @param plan 当前计划（可能为 null 表示计划已清除）
     */
    private void onPlanChanged(PlanNotebook notebook, Plan plan) {
        if (plan == null) {
            log.info("Plan cleared");
            return;
        }

        log.info("Plan changed: {} (state={}, subtasks={})",
                plan.getName(),
                plan.getState(),
                plan.getSubtasks() != null ? plan.getSubtasks().size() : 0);

        // 打印子任务状态
        if (plan.getSubtasks() != null) {
            int idx = 0;
            for (var subtask : plan.getSubtasks()) {
                log.debug("  Subtask[{}] {}: {}", idx++, subtask.getName(), subtask.getState());
            }
        }
    }

    /**
     * 创建变更日志摘要
     *
     * @param plan 计划对象
     * @return 格式化的日志字符串
     */
    public static String createLogSummary(Plan plan) {
        if (plan == null) {
            return "No active plan";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Plan: ").append(plan.getName()).append("\n");
        sb.append("State: ").append(plan.getState()).append("\n");
        sb.append("Subtasks:\n");

        if (plan.getSubtasks() != null) {
            int done = 0, inProgress = 0, todo = 0;
            for (var subtask : plan.getSubtasks()) {
                String marker = switch (subtask.getState()) {
                    case DONE -> "[x]";
                    case IN_PROGRESS -> "[>]";
                    case ABANDONED -> "[-]";
                    default -> "[ ]";
                };
                sb.append("  ").append(marker).append(" ").append(subtask.getName()).append("\n");

                switch (subtask.getState()) {
                    case DONE -> done++;
                    case IN_PROGRESS -> inProgress++;
                    default -> todo++;
                }
            }
            sb.append("Progress: ").append(done).append(" done, ")
                    .append(inProgress).append(" in progress, ")
                    .append(todo).append(" todo");
        }

        return sb.toString();
    }
}
```

### 12.4.10 计划管理控制器 (PlanController.java)

```java
package io.agentscope.tutorial.chapter12.controller;

import io.agentscope.core.plan.PlanNotebook;
import io.agentscope.core.plan.model.Plan;
import io.agentscope.core.plan.model.SubTask;
import io.agentscope.tutorial.chapter12.dto.PlanRequest;
import io.agentscope.tutorial.chapter12.dto.SubTaskRequest;
import io.agentscope.tutorial.chapter12.service.AgentService;
import io.agentscope.tutorial.chapter12.service.PlanDbService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 计划管理控制器 - 提供计划的 CRUD 操作接口
 *
 * <p>REST API 端点：
 * <ul>
 *   <li>GET  /api/plan          - 获取当前计划</li>
 *   <li>GET  /api/plan/stream   - SSE 流式推送计划状态</li>
 *   <li>POST /api/plan          - 创建新计划</li>
 *   <li>PUT  /api/plan          - 更新计划基本信息</li>
 *   <li>DELETE /api/plan/subtasks/{idx} - 删除子任务</li>
 *   <li>PATCH /api/plan/subtasks/{idx}/state - 更新子任务状态</li>
 *   <li>POST /api/plan/subtasks/{idx}/finish - 完成子任务</li>
 *   <li>POST /api/plan/finish   - 完成计划</li>
 *   <li>GET  /api/plan/history   - 获取历史计划列表</li>
 *   <li>GET  /api/plan/stats     - 获取计划统计</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final AgentService agentService;
    private final PlanDbService planDbService;

    public PlanController(AgentService agentService, PlanDbService planDbService) {
        this.agentService = agentService;
        this.planDbService = planDbService;
    }

    /**
     * 获取当前计划详情
     */
    @GetMapping
    public ResponseEntity<PlanDto> getCurrentPlan() {
        Plan plan = agentService.getCurrentPlan();
        if (plan == null) {
            return ResponseEntity.ok(new PlanDto());
        }
        return ResponseEntity.ok(toPlanDto(plan));
    }

    /**
     * SSE 流式推送计划状态变化
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<PlanDto> getPlanStream() {
        AtomicReference<Plan> lastPlan = new AtomicReference<>();

        // 注册监听器监听计划变化
        agentService.addPlanChangeListener(() -> {
            lastPlan.set(agentService.getCurrentPlan());
        });

        // 返回计划状态流
        return Flux.concat(
                Mono.fromCallable(() -> {
                    Plan p = agentService.getCurrentPlan();
                    return p != null ? toPlanDto(p) : new PlanDto();
                }),
                Flux.interval(java.time.Duration.ofMillis(500))
                        .map(tick -> {
                            Plan p = agentService.getCurrentPlan();
                            return p != null ? toPlanDto(p) : new PlanDto();
                        })
                        .distinctUntilChanged(dto -> dto.id)
        );
    }

    /**
     * 创建新计划
     */
    @PostMapping
    public Mono<Map<String, Object>> createPlan(@RequestBody PlanRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Plan name is required"));
        }

        Plan plan = agentService.getCurrentPlan();
        if (plan != null) {
            // 存在进行中的计划时，先完成再创建新的
            return planDbService.addPlan(plan)
                    .then(Mono.defer(() -> createNewPlan(request)));
        }

        return createNewPlan(request);
    }

    private Mono<Map<String, Object>> createNewPlan(PlanRequest request) {
        List<Map<String, Object>> subtasks = new ArrayList<>();
        if (request.getSubtasks() != null) {
            for (SubTaskRequest st : request.getSubtasks()) {
                subtasks.add(Map.of(
                        "name", st.getName() != null ? st.getName() : "Unnamed",
                        "description", st.getDescription() != null ? st.getDescription() : "",
                        "expected_outcome", st.getExpectedOutcome() != null ? st.getExpectedOutcome() : ""
                ));
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("status", "ready");
        result.put("message", "Please use the chat interface to start the plan");

        return Mono.just(result);
    }

    /**
     * 更新计划基本信息
     */
    @PutMapping
    public Mono<Map<String, Object>> updatePlanInfo(@RequestBody PlanRequest request) {
        Plan plan = agentService.getCurrentPlan();
        if (plan == null) {
            return Mono.just(Map.of("error", "No active plan"));
        }

        Map<String, Object> result = new HashMap<>();
        if (request.getName() != null && !request.getName().isEmpty()) {
            result.put("name", request.getName());
        }
        if (request.getDescription() != null) {
            result.put("description", request.getDescription());
        }
        if (request.getExpectedOutcome() != null) {
            result.put("expectedOutcome", request.getExpectedOutcome());
        }

        result.put("status", "updated");
        return Mono.just(result);
    }

    /**
     * 删除子任务
     */
    @DeleteMapping("/subtasks/{index}")
    public Mono<Map<String, Object>> deleteSubtask(@PathVariable int index) {
        Plan plan = agentService.getCurrentPlan();
        if (plan == null) {
            return Mono.just(Map.of("error", "No active plan"));
        }

        if (index < 0 || index >= plan.getSubtasks().size()) {
            return Mono.just(Map.of("error", "Invalid index"));
        }

        SubTask removed = plan.getSubtasks().remove(index);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "deleted");
        result.put("removedTask", removed.getName());

        return Mono.just(result);
    }

    /**
     * 更新子任务状态
     */
    @PatchMapping("/subtasks/{index}/state")
    public Mono<Map<String, Object>> updateSubtaskState(
            @PathVariable int index,
            @RequestBody Map<String, String> body) {
        Plan plan = agentService.getCurrentPlan();
        if (plan == null) {
            return Mono.just(Map.of("error", "No active plan"));
        }

        String state = body.get("state");
        Map<String, Object> result = new HashMap<>();
        result.put("index", index);
        result.put("state", state);
        result.put("status", "updated");

        return Mono.just(result);
    }

    /**
     * 完成子任务
     */
    @PostMapping("/subtasks/{index}/finish")
    public Mono<Map<String, Object>> finishSubtask(
            @PathVariable int index,
            @RequestBody Map<String, String> body) {
        String outcome = body.get("outcome");
        Map<String, Object> result = new HashMap<>();
        result.put("index", index);
        result.put("outcome", outcome != null ? outcome : "");
        result.put("status", "finished");

        return Mono.just(result);
    }

    /**
     * 完成计划
     */
    @PostMapping("/finish")
    public Mono<Map<String, Object>> finishPlan(@RequestBody Map<String, String> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "finished");
        result.put("outcome", body.get("outcome"));
        return Mono.just(result);
    }

    /**
     * 获取历史计划列表
     */
    @GetMapping("/history")
    public Mono<List<PlanDto>> getHistory() {
        return planDbService.getPlans()
                .map(plans -> {
                    List<PlanDto> dtos = new ArrayList<>();
                    for (Plan p : plans) {
                        dtos.add(toPlanDto(p));
                    }
                    return dtos;
                });
    }

    /**
     * 获取计划统计信息
     */
    @GetMapping("/stats")
    public Mono<Map<String, Object>> getStatistics() {
        return planDbService.getStatistics()
                .map(stats -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("total", stats.total());
                    result.put("done", stats.done());
                    result.put("abandoned", stats.abandoned());
                    result.put("inProgress", stats.inProgress());
                    result.put("todo", stats.todo());
                    return result;
                });
    }

    // ==================== DTO 转换方法 ====================

    private PlanDto toPlanDto(Plan plan) {
        PlanDto dto = new PlanDto();
        dto.id = plan.getId();
        dto.name = plan.getName();
        dto.description = plan.getDescription();
        dto.expectedOutcome = plan.getExpectedOutcome();
        dto.state = plan.getState().getValue();
        dto.createdAt = plan.getCreatedAt();
        dto.finishedAt = plan.getFinishedAt();
        dto.outcome = plan.getOutcome();

        if (plan.getSubtasks() != null) {
            dto.subtasks = new ArrayList<>();
            for (int i = 0; i < plan.getSubtasks().size(); i++) {
                SubTask st = plan.getSubtasks().get(i);
                SubTaskDto std = new SubTaskDto();
                std.index = i;
                std.name = st.getName();
                std.description = st.getDescription();
                std.expectedOutcome = st.getExpectedOutcome();
                std.outcome = st.getOutcome();
                std.state = st.getState().getValue();
                std.createdAt = st.getCreatedAt();
                std.finishedAt = st.getFinishedAt();
                dto.subtasks.add(std);
            }
        }

        return dto;
    }

    // ==================== DTO 内部类 ====================

    public static class PlanDto {
        public String id;
        public String name;
        public String description;
        public String expectedOutcome;
        public String state;
        public String createdAt;
        public String finishedAt;
        public String outcome;
        public List<SubTaskDto> subtasks;

        public PlanDto() {
            this.subtasks = new ArrayList<>();
            this.state = "none";
        }
    }

    public static class SubTaskDto {
        public int index;
        public String name;
        public String description;
        public String expectedOutcome;
        public String outcome;
        public String state;
        public String createdAt;
        public String finishedAt;
    }
}
```

### 12.4.11 对话控制器 (ChatController.java)

```java
package io.agentscope.tutorial.chapter12.controller;

import io.agentscope.tutorial.chapter12.dto.ChatRequest;
import io.agentscope.tutorial.chapter12.service.AgentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 对话控制器 - 处理与 Agent 的交互
 *
 * <p>提供对话接口，支持流式响应（SSE）。
 *
 * <p>端点：
 * <ul>
 *   <li>POST /api/chat - 发送消息，获取流式响应</li>
 *   <li>POST /api/chat/resume - 恢复暂停中的 Agent</li>
 *   <li>POST /api/chat/stop - 请求停止 Agent</li>
 *   <li>GET  /api/chat/status - 获取 Agent 状态</li>
 *   <li>POST /api/reset - 重置 Agent 和计划</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final AgentService agentService;

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 发送消息并获取流式响应
     *
     * <p>返回 Server-Sent Events (SSE) 流，每个事件是一个 JSON 对象：
     * <ul>
     *   <li>{"t":"think","d":"..."} - 思维内容</li>
     *   <li>{"t":"text","d":"..."} - 文本响应</li>
     *   <li>{"t":"tool","n":"...","d":"..."} - 工具结果</li>
     *   <li>{"t":"paused"} - Agent 暂停等待确认</li>
     *   <li>{"t":"ctx",...} - 调试上下文信息</li>
     * </ul>
     */
    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request, @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "default";
        }

        log.info("Chat request [session={}]: {}", sessionId, truncate(request.getMessage(), 100));

        return agentService.chat(sessionId, request.getMessage());
    }

    /**
     * 恢复暂停中的 Agent
     */
    @PostMapping(path = "/chat/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> resume(@RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId) {
        log.info("Resume request [session={}]", sessionId);
        return agentService.resume(sessionId);
    }

    /**
     * 请求停止 Agent（在下一个计划工具后暂停）
     */
    @PostMapping("/chat/stop")
    public Map<String, Object> stop() {
        log.info("Stop requested");
        agentService.requestStop();
        return Map.of("status", "stop_requested", "message", "Agent will pause after next plan tool");
    }

    /**
     * 获取 Agent 状态
     */
    @GetMapping("/chat/status")
    public Map<String, Object> getStatus() {
        boolean paused = agentService.isPaused();
        var plan = agentService.getCurrentPlan();

        return Map.of(
                "paused", paused,
                "hasPlan", plan != null,
                "planName", plan != null ? plan.getName() : null,
                "subtaskCount", plan != null && plan.getSubtasks() != null ? plan.getSubtasks().size() : 0
        );
    }

    /**
     * 重置 Agent 和计划
     */
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        log.info("Reset requested");
        agentService.reset();
        return Map.of("status", "reset", "message", "Agent and plan have been reset");
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "chapter12-plan-tutorial",
                "version", "1.0.0"
        );
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}
```

### 12.4.12 请求 DTO 类

```java
package io.agentscope.tutorial.chapter12.dto;

import java.util.List;

/**
 * 聊天请求 DTO
 */
class ChatRequest {
    private String message;

    public ChatRequest() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

/**
 * 计划请求 DTO
 */
class PlanRequest {
    private String name;
    private String description;
    private String expectedOutcome;
    private List<SubTaskRequest> subtasks;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }

    public List<SubTaskRequest> getSubtasks() {
        return subtasks;
    }

    public void setSubtasks(List<SubTaskRequest> subtasks) {
        this.subtasks = subtasks;
    }
}

/**
 * 子任务请求 DTO
 */
class SubTaskRequest {
    private String name;
    private String description;
    private String expectedOutcome;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExpectedOutcome() {
        return expectedOutcome;
    }

    public void setExpectedOutcome(String expectedOutcome) {
        this.expectedOutcome = expectedOutcome;
    }
}
```

### 12.4.13 运行与测试

**1. 启动应用**

```bash
# 设置环境变量（阿里云百炼 API Key）
export DASHSCOPE_API_KEY=your_api_key_here

# 进入项目目录
cd agentscope-examples/chapter12-plan-tutorial

# 启动应用
mvn spring-boot:run
```

**2. 创建计划并执行**

通过 curl 或 Postman 测试：

```bash
# 1. 发起对话（会自动创建计划）
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test1" \
  -d '{"message": "帮我计算 100+200，然后写入 result.txt，再读取验证"}'

# 2. 查看当前计划
curl http://localhost:8080/api/plan

# 3. 获取历史计划
curl http://localhost:8080/api/plan/history

# 4. 获取计划统计
curl http://localhost:8080/api/plan/stats

# 5. 检查 Agent 状态
curl http://localhost:8080/api/chat/status

# 6. 重置 Agent
curl -X POST http://localhost:8080/api/reset
```

**3. 预期输出示例**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "计算并保存结果",
  "description": "执行三个步骤：计算 100+200，将结果写入文件，验证文件内容",
  "state": "in_progress",
  "subtasks": [
    {
      "index": 0,
      "name": "计算表达式",
      "description": "使用 calculate 工具计算 100+200",
      "expectedOutcome": "结果为 300",
      "state": "done",
      "outcome": "100+200 = 300.0"
    },
    {
      "index": 1,
      "name": "保存结果到文件",
      "description": "使用 write_file 工具写入 result.txt",
      "expectedOutcome": "文件包含 '300'",
      "state": "done",
      "outcome": "File written: result.txt"
    },
    {
      "index": 2,
      "name": "验证文件内容",
      "description": "使用 read_file 读取并确认内容",
      "expectedOutcome": "内容为 '300'",
      "state": "in_progress",
      "outcome": null
    }
  ]
}
```

**4. H2 数据库查看**

访问 http://localhost:8080/h2-console，使用以下配置：
- JDBC URL: `jdbc:h2:mem:plan_db`
- 用户名: `sa`
- 密码: （空）

## 12.5 本章小结

本章详细介绍了 AgentScope Java 的计划与任务分解系统：

**核心要点**：

1. **PlanNotebook** 是计划系统的核心，提供创建、修订、完成计划的能力
2. **自动提示注入** 通过 Hook 机制在每次推理前提供上下文引导
3. **严格的状态转换** 保证计划执行的有序性和可预测性
4. **灵活的存储后端** 支持内存、数据库等多种持久化方案
5. **实时状态监控** 通过 SSE 和监听器实现前端同步更新

**最佳实践**：

- 计划创建时应提供清晰、具体、可测量的目标和预期结果
- 子任务应该是原子性的，能够独立验证完成状态
- 使用数据库存储实现计划的历史追溯和恢复能力
- 通过 Hook 机制实现计划变更的实时通知和业务联动

**下一步**：第十三章将介绍 Hook 钩子系统，深入讲解如何通过自定义钩子扩展 Agent 的行为和控制流程。