# 第十九章：综合实战案例

本章通过三个完整的实战案例，展示 AgentScope Java 在不同场景下的应用。每个案例都包含完整的架构说明、核心代码实现、关键配置和运行说明。所有案例均基于 Java 21、Spring Boot 3 和 H2 嵌入式数据库，可以直接运行。

---

## 19.1 【案例】奶茶店多代理系统

### 19.1.1 系统概述

奶茶店多代理系统（boba-tea-shop）是一个完整的多代理协作系统，模拟奶茶店从点单、制作到配送的全流程。系统采用 supervisor-agent（监督者代理）作为核心调度节点，负责协调多个子代理（业务代理、咨询代理）完成复杂任务。

**系统架构图：**

```
┌─────────────────────────────────────────────────────────────────┐
│                     客户端（Web/移动端）                          │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP/SSE
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│              Supervisor Agent（监督者代理）                       │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  核心调度：任务分发 / 结果聚合 / 异常处理                  │   │
│  │  A2A协议：与子代理通信                                    │   │
│  └─────────────────────────────────────────────────────────┘   │
└───────────┬─────────────────────────────────────┬───────────────┘
            │ A2A 协议                             │ A2A 协议
            ▼                                     ▼
┌─────────────────────┐             ┌─────────────────────────────┐
│  Business Agent     │             │   Consult Agent             │
│  (业务代理)          │             │   (咨询代理)                 │
│  - 订单管理          │             │   - 产品推荐                │
│  - 库存检查          │             │   - FAQ 问答               │
│  - 投诉处理          │             │   - 知识库检索               │
└─────────┬───────────┘             └──────────────┬──────────────┘
          │ MCP 协议                             │ MCP 协议
          ▼                                      ▼
┌─────────────────────────────────────────────────────────────────┐
│              Business MCP Server（业务服务）                      │
│  ┌──────────┬──────────┬──────────┬──────────┐                 │
│  │ 订单服务  │ 产品服务  │ 用户服务  │ 反馈服务  │                 │
│  └──────────┴──────────┴──────────┴──────────┘                 │
│                     H2 嵌入式数据库                             │
└─────────────────────────────────────────────────────────────────┘
```

### 19.1.2 项目结构

```
boba-tea-shop/
├── supervisor-agent/          # 监督者代理主服务
│   └── src/main/java/
│       └── io/agentscope/examples/bobatea/supervisor/
│           ├── SupervisorAgentApplication.java
│           ├── agent/SupervisorAgent.java
│           ├── config/
│           │   ├── SupervisorAgentConfig.java
│           │   ├── SupervisorAgentPromptConfig.java
│           │   └── A2aAgentConfiguration.java
│           ├── controller/SupervisorAgentController.java
│           └── tools/
│               ├── A2aAgentTools.java
│               └── ScheduleAgentTools.java
├── business-sub-agent/         # 业务子代理
├── consult-sub-agent/         # 咨询子代理
└── business-mcp-server/       # 业务MCP服务
    └── src/main/java/
        └── io/agentscope/examples/bobatea/business/
            ├── service/
            │   ├── OrderService.java
            │   └── FeedbackService.java
            └── controller/OrderController.java
```

### 19.1.3 核心实现

#### 1. SupervisorAgent 主代理

SupervisorAgent 是整个系统的核心，负责接收用户请求并协调各子代理完成任务。

```java
// 文件：SupervisorAgent.java
package io.agentscope.tutorial.chapter19.bobatea;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.mysql.MysqlSession;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter19.bobatea.tools.A2aAgentTools;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

/**
 * 奶茶店监督者代理
 *
 * 职责：
 * 1. 接收用户自然语言输入
 * 2. 调用 A2A 工具与业务代理、咨询代理通信
 * 3. 聚合子代理返回的结果
 * 4. 将最终回答返回给用户
 */
public class SupervisorAgent {

    private static final Logger logger = LoggerFactory.getLogger(SupervisorAgent.class);

    private final Model model;
    private final A2aAgentTools tools;
    private final String sysPrompt;
    private final Path sessionPath;

    @Autowired
    private DataSource dataSource;

    public SupervisorAgent(Model model, A2aAgentTools tools, String sysPrompt) {
        this.model = model;
        this.tools = tools;
        this.sysPrompt = sysPrompt;
        // 会话持久化路径
        this.sessionPath = Paths.get(
                System.getProperty("java.io.tmpdir"),
                ".agentscope", "examples", "sessions");
        logger.info("SupervisorAgent 初始化，会话路径: {}", sessionPath);
    }

    /**
     * 处理用户消息
     *
     * @param msg 用户消息
     * @param sessionId 会话ID，用于会话持久化
     * @param userId 用户ID
     * @return 事件流，包含代理的思考和回答过程
     */
    public Flux<Event> stream(Msg msg, String sessionId, String userId) {
        // 创建工具集
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(tools);

        // 自动上下文压缩配置：当 token 占比超过 40% 时压缩，保留最近 10 条消息
        AutoContextConfig autoContextConfig = AutoContextConfig.builder()
                .tokenRatio(0.4)
                .lastKeep(10)
                .build();

        // 使用自动上下文内存，支持长对话
        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);

        // MySQL 会话持久化
        MysqlSession mysqlSession = new MysqlSession(
                dataSource,
                System.getenv("DB_NAME"),
                null,
                true);

        // 为每个请求创建新的代理实例，保证请求隔离
        ReActAgent agent = createAgent(toolkit, memory);
        agent.loadIfExists(mysqlSession, sessionId);

        return agent.stream(msg)
                .doFinally(signalType -> {
                    logger.info("流式结束，信号: {}，保存会话: {}", signalType, sessionId);
                    agent.saveTo(mysqlSession, sessionId);
                });
    }

    /**
     * 创建新的 ReActAgent 实例
     */
    private ReActAgent createAgent(Toolkit toolkit, Memory memory) {
        return ReActAgent.builder()
                .name("supervisor_agent")
                .sysPrompt(sysPrompt)
                .toolkit(toolkit)
                .hook(new MonitoringHook())  // 监控钩子
                .model(model)
                .memory(memory)
                .build();
    }
}
```

#### 2. A2A 代理通信工具

A2A（Agent-to-Agent）协议允许代理之间相互通信，协调完成复杂任务。

```java
// 文件：A2aAgentTools.java
package io.agentscope.tutorial.chapter19.bobatea.tools;

import io.agentscope.core.tool.ToolResult;
import io.agentscope.core.tool.ToolSpec;
import java.util.List;

/**
 * A2A 代理通信工具类
 *
 * 提供与业务代理、咨询代理通信的能力
 */
public class A2aAgentTools {

    // 子代理的服务URL
    private static final String BUSINESS_AGENT_URL = "http://localhost:8081";
    private static final String CONSULT_AGENT_URL = "http://localhost:8082";

    /**
     * 获取工具规格列表
     */
    public static List<ToolSpec> getToolSpecs() {
        return List.of(
                // 调用业务代理处理订单相关请求
                ToolSpec.builder()
                        .name("call_business_agent")
                        .description("调用业务代理处理订单、库存、投诉等业务请求")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "task": {
                                            "type": "string",
                                            "description": "业务任务描述"
                                        },
                                        "parameters": {
                                            "type": "object",
                                            "description": "任务参数（如订单ID、产品ID等）"
                                        }
                                    },
                                    "required": ["task"]
                                }
                                """)
                        .build(),

                // 调用咨询代理处理产品咨询
                ToolSpec.builder()
                        .name("call_consult_agent")
                        .description("调用咨询代理处理产品推荐、FAQ等咨询请求")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "query": {
                                            "type": "string",
                                            "description": "用户咨询内容"
                                        },
                                        "context": {
                                            "type": "object",
                                            "description": "上下文信息（如用户偏好、历史订单等）"
                                        }
                                    },
                                    "required": ["query"]
                                }
                                """)
                        .build()
        );
    }

    /**
     * 调用业务代理
     *
     * @param task 业务任务描述
     * @param parameters 任务参数
     * @return 业务代理的处理结果
     */
    public ToolResult callBusinessAgent(String task, Object parameters) {
        try {
            // 通过 HTTP 调用业务代理的 A2A 接口
            String url = BUSINESS_AGENT_URL + "/a2a/call";
            // 实际实现中会使用 HTTP 客户端发送请求
            String response = sendA2ARequest(url, task, parameters);

            return ToolResult.success("业务代理处理完成", response);
        } catch (Exception e) {
            return ToolResult.failure("调用业务代理失败: " + e.getMessage());
        }
    }

    /**
     * 调用咨询代理
     *
     * @param query 咨询内容
     * @param context 上下文信息
     * @return 咨询代理的处理结果
     */
    public ToolResult callConsultAgent(String query, Object context) {
        try {
            String url = CONSULT_AGENT_URL + "/a2a/call";
            String response = sendA2ARequest(url, query, context);

            return ToolResult.success("咨询代理处理完成", response);
        } catch (Exception e) {
            return ToolResult.failure("调用咨询代理失败: " + e.getMessage());
        }
    }

    // 模拟 A2A 请求发送
    private String sendA2ARequest(String url, String task, Object params) {
        // 实际实现中需要使用 HTTP 客户端（如 RestTemplate、WebClient）
        // 此处为简化示例
        return "{\"status\":\"success\",\"result\":\"处理完成\"}";
    }
}
```

#### 3. 订单服务实现

业务 MCP Server 中的核心服务，处理订单相关业务。

```java
// 文件：OrderService.java
package io.agentscope.tutorial.chapter19.bobatea.service;

import io.agentscope.tutorial.chapter19.bobatea.entity.Order;
import io.agentscope.tutorial.chapter19.bobatea.mapper.OrderMapper;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 订单服务
 *
 * 核心功能：
 * - 创建订单
 * - 查询订单
 * - 修改订单备注
 * - 处理订单投诉
 */
@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 创建新订单
     *
     * @param userId 用户ID
     * @param productId 产品ID
     * @param productName 产品名称
     * @param sweetness 甜度（1-5）
     * @param iceLevel 冰度（1-5）
     * @param quantity 数量
     * @param remark 备注
     * @return 创建的订单
     */
    public Order createOrder(Long userId, Long productId, String productName,
                            Integer sweetness, Integer iceLevel,
                            Integer quantity, String remark) {
        // 生成唯一订单号
        String orderId = generateOrderId();

        // 计算价格（假设单价从产品服务获取，此处简化）
        BigDecimal unitPrice = new BigDecimal("18.00");
        BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(quantity));

        // 构建订单对象
        Order order = new Order(
                orderId, userId, productId, productName,
                sweetness, iceLevel, quantity,
                unitPrice, totalPrice, remark);

        order.onCreate();

        // 插入数据库
        orderMapper.insert(order);

        return order;
    }

    /**
     * 根据用户ID查询订单列表
     */
    public List<Order> getOrdersByUserId(Long userId) {
        return orderMapper.findByUserId(userId);
    }

    /**
     * 根据订单号查询订单
     */
    public Optional<Order> getOrderByOrderId(String orderId) {
        Order order = orderMapper.findByOrderId(orderId);
        return Optional.ofNullable(order);
    }

    /**
     * 更新订单备注
     */
    public boolean updateRemark(String orderId, String newRemark) {
        Optional<Order> orderOpt = getOrderByOrderId(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        order.setRemark(newRemark);
        order.onUpdate();

        return orderMapper.update(order) > 0;
    }

    /**
     * 取消订单
     */
    public boolean cancelOrder(String orderId) {
        Optional<Order> orderOpt = getOrderByOrderId(orderId);
        if (orderOpt.isEmpty()) {
            return false;
        }

        Order order = orderOpt.get();
        // 只有未制作的订单可以取消
        if ("PENDING".equals(order.getStatus())) {
            order.setStatus("CANCELLED");
            order.onUpdate();
            return orderMapper.update(order) > 0;
        }

        return false;
    }

    /**
     * 生成唯一订单号
     */
    private String generateOrderId() {
        return "BOBA" + System.currentTimeMillis()
                + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }
}
```

### 19.1.4 关键配置文件

```yaml
# application.yml - supervisor-agent 配置
server:
  port: 8080

spring:
  application:
    name: supervisor-agent

  datasource:
    url: jdbc:h2:mem:bobatea;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:

# AgentScope 配置
agentscope:
  a2a:
    enabled: true
    port: 8080

# 代理提示词配置
agent:
  prompts:
    supervisor-agent-instruction: |
      你是一位奶茶店智能助手，负责协调多个子代理完成用户请求。
      可用工具：
      - call_business_agent: 处理订单、库存等业务请求
      - call_consult_agent: 处理产品咨询、推荐等咨询请求

      工作流程：
      1. 理解用户意图
      2. 判断需要调用的子代理
      3. 调用相应子代理获取结果
      4. 汇总结果返回给用户

      如果用户请求涉及多个领域，可以并行调用多个子代理。
```

### 19.1.5 运行说明

**1. 环境准备**

```bash
# 确保已安装 Java 21 和 Maven
java -version  # 应该显示 21.x
mvn -version

# 设置必要的环境变量
export DASHSCOPE_API_KEY=your_api_key_here
export DB_NAME=bobatea
```

**2. 启动服务**

```bash
# 启动业务 MCP Server
cd business-mcp-server
mvn spring-boot:run

# 启动业务子代理（新终端）
cd business-sub-agent
mvn spring-boot:run

# 启动咨询子代理（新终端）
cd consult-sub-agent
mvn spring-boot:run

# 启动监督者代理（新终端）
cd supervisor-agent
mvn spring-boot:run
```

**3. 测试接口**

```bash
# 测试聊天接口
curl -X GET "http://localhost:8080/api/assistant/chat?chat_id=test001&user_query=我想点一杯珍珠奶茶&user_id=user001"

# 查询报告列表
curl -X GET "http://localhost:8080/api/assistant/reports"
```

---

## 19.2 【案例】狼人杀游戏 Agent

### 19.2.1 系统概述

狼人杀游戏是一个经典的社交推理游戏。在本案例中，我们使用 AgentScope Java 构建了一个完整的多 Agent 狼人杀游戏系统。每个玩家角色都由一个独立的 ReActAgent 控制，Agent 之间通过 MsgHub 进行讨论和投票。

**游戏角色配置：**

| 角色 | 数量 | 阵营 | 特殊能力 |
|------|------|------|----------|
| 村民 | 3 | 好人 | 无 |
| 狼人 | 3 | 狼人 | 每晚可击杀一名玩家 |
| 预言家 | 1 | 好人 | 每晚可查验一名玩家身份 |
| 女巫 | 1 | 好人 | 拥有一瓶解药和一瓶毒药 |
| 猎人 | 1 | 好人 | 死亡时可带走一名玩家 |

**游戏流程图：**

```
┌─────────────────────────────────────────────────────────────────┐
│                        游戏主流程                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────┐   夜晚阶段      ┌─────────┐   白天阶段     ┌───────┐│
│  │ 狼人讨论 │──▶击杀目标 ──▶│ 女巫救人 │──▶查验目标 ──▶│ 预言家 ││
│  └─────────┘               └─────────┘                └───────┘│
│       │                         │                           │     │
│       ▼                         ▼                           ▼     │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                     结算夜晚结果                             ││
│  └─────────────────────────────────────────────────────────────┘│
│                              │                                   │
│                              ▼                                   │
│  ┌────────────┐    ┌──────────────┐    ┌─────────────────────┐  │
│  │  猎人开枪   │───▶│  自由讨论    │───▶│   投票放逐           │  │
│  └────────────┘    └──────────────┘    └─────────────────────┘  │
│                              │                                   │
│                              ▼                                   │
│                    ┌─────────────────┐                          │
│                    │   检查胜负条件   │                          │
│                    └─────────────────┘                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 19.2.2 项目结构

```
werewolf/
├── src/main/java/io/agentscope/examples/werewolf/
│   ├── WerewolfWebApplication.java     # Spring Boot 入口
│   ├── WerewolfGameConfig.java         # 游戏配置常量
│   ├── WerewolfUtils.java              # 工具类
│   ├── entity/
│   │   ├── GameState.java              # 游戏状态管理
│   │   ├── Player.java                 # 玩家实体
│   │   └── Role.java                   # 角色枚举
│   ├── model/
│   │   ├── VoteModel.java              # 投票数据模型
│   │   ├── SeerCheckModel.java         # 预言家查验模型
│   │   ├── WitchHealModel.java         # 女巫救人模型
│   │   ├── WitchPoisonModel.java       # 女巫毒人模型
│   │   └── HunterShootModel.java       # 猎人开枪模型
│   ├── localization/
│   │   ├── GameMessages.java            # 游戏消息接口
│   │   ├── LocalizationBundle.java     # 本地化包
│   │   └── LanguageConfig.java         # 语言配置
│   └── web/
│       ├── WerewolfWebController.java  # Web 控制器
│       ├── WerewolfWebGame.java         # Web 游戏实现
│       └── GameEventEmitter.java        # 事件发射器
└── src/main/resources/
    └── templates/                       # 前端模板
```

### 19.2.3 核心实现

#### 1. 游戏状态管理

```java
// 文件：GameState.java
package io.agentscope.tutorial.chapter19.werewolf.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 游戏状态管理类
 *
 * 负责：
 * - 维护所有玩家状态
 * - 管理夜晚行动结果
 * - 判断游戏胜负
 */
public class GameState {

    private final List<Player> allPlayers;  // 所有玩家
    private final Player seer;              // 预言家
    private final Player witch;             // 女巫
    private final Player hunter;            // 猎人

    private int currentRound;               // 当前回合
    private Player lastNightVictim;         // 昨夜被击杀玩家
    private Player lastPoisonedVictim;      // 被毒杀玩家
    private boolean lastVictimResurrected;  // 是否被救活

    public GameState(List<Player> allPlayers) {
        this.allPlayers = new ArrayList<>(allPlayers);
        this.currentRound = 0;

        // 初始化特殊角色
        this.seer = findPlayerByRole(Role.SEER);
        this.witch = findPlayerByRole(Role.WITCH);
        this.hunter = findPlayerByRole(Role.HUNTER);
    }

    /**
     * 获取存活的玩家列表
     */
    public List<Player> getAlivePlayers() {
        return allPlayers.stream()
                .filter(Player::isAlive)
                .collect(Collectors.toList());
    }

    /**
     * 获取存活的狼人列表
     */
    public List<Player> getAliveWerewolves() {
        return getAlivePlayers().stream()
                .filter(p -> p.getRole() == Role.WEREWOLF)
                .collect(Collectors.toList());
    }

    /**
     * 获取存活的村民阵营玩家
     */
    public List<Player> getAliveVillagers() {
        return getAlivePlayers().stream()
                .filter(p -> p.getRole().isVillagerCamp())
                .collect(Collectors.toList());
    }

    /**
     * 检查狼人是否胜利
     * 条件：狼人数量 >= 村民数量
     */
    public boolean checkWerewolvesWin() {
        int aliveWerewolves = getAliveWerewolves().size();
        int aliveVillagers = getAliveVillagers().size();
        return aliveWerewolves > 0 && aliveWerewolves >= aliveVillagers;
    }

    /**
     * 检查村民是否胜利
     * 条件：狼人全部死亡
     */
    public boolean checkVillagersWin() {
        return getAliveWerewolves().isEmpty();
    }

    /**
     * 根据玩家名称查找玩家
     */
    public Player findPlayerByName(String name) {
        return allPlayers.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    // Getter 和 Setter 方法
    public Player getSeer() { return seer; }
    public Player getWitch() { return witch; }
    public Player getHunter() { return hunter; }
    public int getCurrentRound() { return currentRound; }
    public Player getLastNightVictim() { return lastNightVictim; }
    public Player getLastPoisonedVictim() { return lastPoisonedVictim; }
    public boolean isLastVictimResurrected() { return lastVictimResurrected; }

    public void nextRound() { this.currentRound++; }
    public void setLastNightVictim(Player victim) { this.lastNightVictim = victim; }
    public void setLastPoisonedVictim(Player victim) { this.lastPoisonedVictim = victim; }
    public void setLastVictimResurrected(boolean resurrected) { this.lastVictimResurrected = resurrected; }
    public void clearNightResults() {
        this.lastNightVictim = null;
        this.lastPoisonedVictim = null;
        this.lastVictimResurrected = false;
    }

    private Player findPlayerByRole(Role role) {
        return allPlayers.stream().filter(p -> p.getRole() == role).findFirst().orElse(null);
    }
}
```

#### 2. 玩家实体

```java
// 文件：Player.java
package io.agentscope.tutorial.chapter19.werewolf.entity;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;

/**
 * 玩家实体
 *
 * 每个玩家都关联一个 ReActAgent，负责与 AI 模型交互做出决策
 */
public class Player {

    private String name;                    // 玩家名称
    private Role role;                     // 角色
    private boolean alive;                  // 是否存活
    private boolean witchHasHealPotion;     // 女巫是否有解药
    private boolean witchHasPoisonPotion;   // 女巫是否有毒药
    private boolean voted;                  // 是否已投票
    private AgentBase agent;                // 关联的 Agent

    private Player(Builder builder) {
        this.name = builder.name;
        this.role = builder.role;
        this.alive = true;
        this.witchHasHealPotion = (builder.role == Role.WITCH);
        this.witchHasPoisonPotion = (builder.role == Role.WITCH);
        this.agent = builder.agent;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 玩家死亡
     */
    public void kill() {
        this.alive = false;
    }

    /**
     * 玩家复活（如女巫救人）
     */
    public void resurrect() {
        this.alive = true;
    }

    /**
     * 女巫使用解药
     */
    public void useHealPotion() {
        this.witchHasHealPotion = false;
    }

    /**
     * 女巫使用毒药
     */
    public void usePoisonPotion() {
        this.witchHasPoisonPotion = false;
    }

    // Getter 方法
    public String getName() { return name; }
    public Role getRole() { return role; }
    public boolean isAlive() { return alive; }
    public boolean isWitchHasHealPotion() { return witchHasHealPotion; }
    public boolean isWitchHasPoisonPotion() { return witchHasPoisonPotion; }
    public AgentBase getAgent() { return agent; }

    public static class Builder {
        private String name;
        private Role role;
        private AgentBase agent;

        public Builder name(String name) { this.name = name; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder agent(AgentBase agent) { this.agent = agent; return this; }
        public Player build() { return new Player(this); }
    }
}
```

#### 3. Web 游戏控制器

```java
// 文件：WerewolfWebController.java
package io.agentscope.tutorial.chapter19.werewolf.web;

import io.agentscope.examples.werewolf.localization.LocalizationBundle;
import io.agentscope.examples.werewolf.localization.LocalizationFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 狼人杀游戏 Web 控制器
 *
 * 提供 SSE（Server-Sent Events）接口，支持实时游戏进程推送
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class WerewolfWebController {

    private final LocalizationFactory localizationFactory;

    public WerewolfWebController(LocalizationFactory localizationFactory) {
        this.localizationFactory = localizationFactory;
    }

    /**
     * 开始新游戏
     *
     * @param lang 语言配置（默认中文）
     * @return SSE 事件流，包含游戏进程
     */
    @PostMapping(value = "/start", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<GameEvent>> startGame(
            @RequestParam(name = "lang", defaultValue = "zh-CN") String lang) {

        // 创建本地化消息包
        LocalizationBundle bundle = localizationFactory.createBundle(lang);
        GameEventEmitter emitter = new GameEventEmitter();

        // 创建 Web 游戏实例
        WerewolfWebGame game = new WerewolfWebGame(emitter, bundle);

        // 异步执行游戏逻辑
        Mono.fromRunnable(() -> {
            try {
                game.start();
            } catch (Exception e) {
                emitter.emitError("游戏错误: " + e.getMessage());
            } finally {
                emitter.complete();
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe();

        // 返回 SSE 事件流
        return emitter.getEventStream()
                .map(event -> ServerSentEvent.<GameEvent>builder()
                        .event(event.getType().name().toLowerCase())
                        .data(event)
                        .build());
    }
}
```

#### 4. 游戏事件发射器

```java
// 文件：GameEventEmitter.java
package io.agentscope.tutorial.chapter19.werewolf.web;

import reactor.core.publisher.Sinks;

/**
 * 游戏事件发射器
 *
 * 负责将游戏中的各种事件通过 SSE 推送给前端
 */
public class GameEventEmitter {

    private final Sinks.Many<GameEvent> sink;

    public GameEventEmitter() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
    }

    /**
     * 获取事件流
     */
    public Flux<GameEvent> getEventStream() {
        return sink.asFlux();
    }

    /**
     * 发射游戏初始化事件
     */
    public void emitGameInit(java.util.List<java.util.Map<String, Object>> playersInfo) {
        GameEvent event = new GameEvent(GameEventType.GAME_INIT);
        event.setPlayersInfo(playersInfo);
        sink.tryEmitNext(event);
    }

    /**
     * 发射阶段变化事件
     */
    public void emitPhaseChange(int round, String phase) {
        GameEvent event = new GameEvent(GameEventType.PHASE_CHANGE);
        event.setRound(round);
        event.setPhase(phase);
        sink.tryEmitNext(event);
    }

    /**
     * 发射玩家发言事件
     */
    public void emitPlayerSpeak(String playerName, String content, String type) {
        GameEvent event = new GameEvent(GameEventType.PLAYER_SPEAK);
        event.setPlayerName(playerName);
        event.setContent(content);
        event.setSpeakType(type);
        sink.tryEmitNext(event);
    }

    /**
     * 发射玩家投票事件
     */
    public void emitPlayerVote(String playerName, String targetPlayer, String reason) {
        GameEvent event = new GameEvent(GameEventType.PLAYER_VOTE);
        event.setPlayerName(playerName);
        event.setTargetPlayer(targetPlayer);
        event.setContent(reason);
        sink.tryEmitNext(event);
    }

    /**
     * 发射玩家死亡事件
     */
    public void emitPlayerEliminated(String playerName, String roleDisplay, String cause) {
        GameEvent event = new GameEvent(GameEventType.PLAYER_ELIMINATED);
        event.setPlayerName(playerName);
        event.setRoleDisplay(roleDisplay);
        event.setEliminateCause(cause);
        sink.tryEmitNext(event);
    }

    /**
     * 发射玩家复活事件
     */
    public void emitPlayerResurrected(String playerName) {
        GameEvent event = new GameEvent(GameEventType.PLAYER_RESURRECTED);
        event.setPlayerName(playerName);
        sink.tryEmitNext(event);
    }

    /**
     * 发射系统消息
     */
    public void emitSystemMessage(String message) {
        GameEvent event = new GameEvent(GameEventType.SYSTEM_MESSAGE);
        event.setContent(message);
        sink.tryEmitNext(event);
    }

    /**
     * 发射玩家动作事件
     */
    public void emitPlayerAction(String playerName, String roleDisplay,
                                 String action, String target, String result) {
        GameEvent event = new GameEvent(GameEventType.PLAYER_ACTION);
        event.setPlayerName(playerName);
        event.setRoleDisplay(roleDisplay);
        event.setAction(action);
        event.setTargetPlayer(target);
        event.setContent(result);
        sink.tryEmitNext(event);
    }

    /**
     * 发射统计数据更新
     */
    public void emitStatsUpdate(int aliveCount, int werewolfCount, int villagerCount) {
        GameEvent event = new GameEvent(GameEventType.STATS_UPDATE);
        event.setAliveCount(aliveCount);
        event.setWerewolfCount(werewolfCount);
        event.setVillagerCount(villagerCount);
        sink.tryEmitNext(event);
    }

    /**
     * 发射游戏结束事件
     */
    public void emitGameEnd(String winner, String explanation) {
        GameEvent event = new GameEvent(GameEventType.GAME_END);
        event.setWinner(winner);
        event.setContent(explanation);
        sink.tryEmitNext(event);
    }

    /**
     * 发射错误事件
     */
    public void emitError(String error) {
        GameEvent event = new GameEvent(GameEventType.ERROR);
        event.setContent(error);
        sink.tryEmitNext(event);
    }

    /**
     * 完成事件流
     */
    public void complete() {
        sink.tryEmitComplete();
    }
}
```

#### 5. Web 游戏主逻辑

```java
// 文件：WerewolfWebGame.java
package io.agentscope.tutorial.chapter19.werewolf.web;

import static io.agentscope.examples.werewolf.WerewolfGameConfig.*;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.formatter.dashscope.DashScopeMultiAgentFormatter;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.pipeline.FanoutPipeline;
import io.agentscope.core.pipeline.MsgHub;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter19.werewolf.entity.GameState;
import io.agentscope.tutorial.chapter19.werewolf.entity.Player;
import io.agentscope.tutorial.chapter19.werewolf.entity.Role;
import io.agentscope.tutorial.chapter19.werewolf.localization.PromptProvider;
import io.agentscope.tutorial.chapter19.werewolf.model.HunterShootModel;
import io.agentscope.tutorial.chapter19.werewolf.model.SeerCheckModel;
import io.agentscope.tutorial.chapter19.werewolf.model.VoteModel;
import io.agentscope.tutorial.chapter19.werewolf.model.WitchHealModel;
import io.agentscope.tutorial.chapter19.werewolf.model.WitchPoisonModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 狼人杀 Web 游戏实现
 *
 * 核心流程：
 * 1. 初始化游戏，创建 9 个玩家 Agent
 * 2. 夜晚阶段：狼人讨论投票、女巫救人/毒人、预言家查验
 * 3. 白天阶段：公布死讯、猎人开枪、自由讨论、投票放逐
 * 4. 检查胜负条件，循环直到游戏结束
 */
public class WerewolfWebGame {

    private final GameEventEmitter emitter;
    private final PromptProvider prompts;
    private final WerewolfUtils utils;

    private DashScopeChatModel model;
    private GameState gameState;

    public WerewolfWebGame(GameEventEmitter emitter, LocalizationBundle bundle) {
        this.emitter = emitter;
        this.prompts = bundle.prompts();
        this.utils = new WerewolfUtils(bundle.messages());
    }

    /**
     * 开始游戏
     */
    public void start() throws Exception {
        emitter.emitSystemMessage("正在初始化游戏...");

        // 初始化 AI 模型
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        model = DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(DEFAULT_MODEL)
                .formatter(new DashScopeMultiAgentFormatter())
                .stream(false)
                .build();

        // 初始化游戏状态
        gameState = initializeGame();
        emitStatsUpdate();

        // 主游戏循环
        for (int round = 1; round <= MAX_ROUNDS; round++) {
            gameState.nextRound();

            // 夜晚阶段
            emitter.emitPhaseChange(round, "night");
            nightPhase();
            if (checkGameEnd()) break;

            // 白天阶段
            emitter.emitPhaseChange(round, "day");
            dayPhase();
            if (checkGameEnd()) break;
        }

        // 宣布结果
        announceWinner();
    }

    /**
     * 初始化游戏，创建所有玩家
     */
    private GameState initializeGame() {
        // 创建角色列表
        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < VILLAGER_COUNT; i++) roles.add(Role.VILLAGER);
        for (int i = 0; i < WEREWOLF_COUNT; i++) roles.add(Role.WEREWOLF);
        for (int i = 0; i < SEER_COUNT; i++) roles.add(Role.SEER);
        for (int i = 0; i < WITCH_COUNT; i++) roles.add(Role.WITCH);
        for (int i = 0; i < HUNTER_COUNT; i++) roles.add(Role.HUNTER);
        Collections.shuffle(roles);

        // 创建玩家和 Agent
        List<Player> players = new ArrayList<>();
        List<String> playerNames = langConfig.getPlayerNames();

        for (int i = 0; i < roles.size(); i++) {
            String name = playerNames.get(i);
            Role role = roles.get(i);

            // 为每个玩家创建 ReActAgent
            ReActAgent agent = ReActAgent.builder()
                    .name(name)
                    .sysPrompt(prompts.getSystemPrompt(role, name))
                    .model(model)
                    .memory(new InMemoryMemory())
                    .toolkit(new Toolkit())
                    .build();

            players.add(Player.builder()
                    .name(name)
                    .role(role)
                    .agent(agent)
                    .build());
        }

        // 发射初始化事件
        emitter.emitGameInit(playersInfo);

        return new GameState(players);
    }

    /**
     * 夜晚阶段
     */
    private void nightPhase() {
        emitter.emitSystemMessage("夜幕降临，请所有玩家闭眼...");

        // 1. 狼人讨论和击杀
        Player victim = werewolvesKill();
        if (victim != null) {
            gameState.setLastNightVictim(victim);
            victim.kill();
            emitter.emitPlayerEliminated(victim.getName(),
                    messages.getRoleDisplayName(victim.getRole()), "killed");
        }

        // 2. 女巫行动
        if (gameState.getWitch() != null && gameState.getWitch().isAlive()) {
            witchActions();
        }

        // 3. 预言家查验
        if (gameState.getSeer() != null && gameState.getSeer().isAlive()) {
            seerCheck();
        }

        // 处理夜晚真正的死亡（未被救活的情况）
        Player nightVictim = gameState.getLastNightVictim();
        if (nightVictim != null && !gameState.isLastVictimResurrected()) {
            emitter.emitPlayerEliminated(nightVictim.getName(),
                    messages.getRoleDisplayName(nightVictim.getRole()), "killed");
        }

        emitStatsUpdate();
        emitter.emitSystemMessage("天亮了，请所有玩家睁眼。");
    }

    /**
     * 狼人击杀阶段
     */
    private Player werewolvesKill() {
        List<Player> werewolves = gameState.getAliveWerewolves();
        if (werewolves.isEmpty()) return null;

        emitter.emitSystemMessage("狼人请睁眼，请讨论并决定今晚击杀目标。");

        // 创建狼人讨论组
        try (MsgHub werewolfHub = MsgHub.builder()
                .name("WerewolfDiscussion")
                .participants(werewolves.stream()
                        .map(Player::getAgent)
                        .toArray(ReActAgent[]::new))
                .announcement(prompts.createWerewolfDiscussionPrompt(gameState))
                .enableAutoBroadcast(true)
                .build()) {

            werewolfHub.enter().block();

            // 狼人讨论 2 轮
            for (int i = 0; i < 2; i++) {
                for (Player werewolf : werewolves) {
                    Msg response = werewolf.getAgent().call().block();
                    String content = utils.extractTextContent(response);
                    emitter.emitPlayerSpeak(werewolf.getName(), content, "werewolf_discussion");
                }
            }

            // 并行投票
            FanoutPipeline votingPipeline = FanoutPipeline.builder()
                    .addAgents(werewolves.stream()
                            .map(p -> (AgentBase) p.getAgent()).toList())
                    .concurrent()
                    .build();

            List<Msg> votes = votingPipeline.execute(
                    prompts.createWerewolfVotingPrompt(gameState),
                    VoteModel.class).block();

            // 统计票数
            return utils.countVotes(votes, gameState);
        }
    }

    /**
     * 女巫行动阶段
     */
    private void witchActions() {
        Player witch = gameState.getWitch();
        Player victim = gameState.getLastNightVictim();

        // 解药使用
        if (witch.isWitchHasHealPotion() && victim != null) {
            emitter.emitSystemMessage(
                    messages.getSystemWitchSeesVictim(victim.getName()));

            Msg healDecision = witch.getAgent()
                    .call(prompts.createWitchHealPrompt(victim), WitchHealModel.class)
                    .block();

            WitchHealModel healModel = healDecision.getStructuredData(WitchHealModel.class);
            if (Boolean.TRUE.equals(healModel.useHealPotion)) {
                victim.resurrect();
                witch.useHealPotion();
                gameState.setLastVictimResurrected(true);
                emitter.emitPlayerAction(
                        witch.getName(),
                        messages.getRoleDisplayName(Role.WITCH),
                        "使用解药",
                        victim.getName(),
                        "救活了 " + victim.getName());
            }
        }

        // 毒药使用
        if (witch.isWitchHasPoisonPotion()) {
            Msg poisonDecision = witch.getAgent()
                    .call(prompts.createWitchPoisonPrompt(gameState, false),
                            WitchPoisonModel.class)
                    .block();

            WitchPoisonModel poisonModel =
                    poisonDecision.getStructuredData(WitchPoisonModel.class);

            if (Boolean.TRUE.equals(poisonModel.usePoisonPotion)
                    && poisonModel.targetPlayer != null) {
                Player target = gameState.findPlayerByName(poisonModel.targetPlayer);
                if (target != null && target.isAlive()) {
                    target.kill();
                    witch.usePoisonPotion();
                    emitter.emitPlayerEliminated(
                            target.getName(),
                            messages.getRoleDisplayName(target.getRole()),
                            "poisoned");
                }
            }
        }
    }

    /**
     * 预言家查验阶段
     */
    private void seerCheck() {
        Player seer = gameState.getSeer();

        emitter.emitSystemMessage("预言家请睁眼，请选择今晚要查验的玩家。");

        Msg checkDecision = seer.getAgent()
                .call(prompts.createSeerCheckPrompt(gameState), SeerCheckModel.class)
                .block();

        SeerCheckModel checkModel = checkDecision.getStructuredData(SeerCheckModel.class);

        if (checkModel.targetPlayer != null) {
            Player target = gameState.findPlayerByName(checkModel.targetPlayer);
            if (target != null && target.isAlive()) {
                String identity = (target.getRole() == Role.WEREWOLF)
                        ? "是狼人" : "不是狼人";
                emitter.emitPlayerAction(
                        seer.getName(),
                        messages.getRoleDisplayName(Role.SEER),
                        "查验",
                        target.getName(),
                        target.getName() + " " + identity);

                // 通知预言家查验结果
                seer.getAgent().call(prompts.createSeerResultPrompt(target)).block();
            }
        }
    }

    /**
     * 白天阶段
     */
    private void dayPhase() {
        emitter.emitSystemMessage("现在是白天，请所有玩家自由讨论。");

        // 猎人开枪
        Player hunter = gameState.getHunter();
        if (hunter != null && !hunter.isAlive()) {
            if (hunter.equals(gameState.getLastNightVictim())
                    || hunter.equals(gameState.getLastPoisonedVictim())) {
                hunterShoot(hunter);
                if (checkGameEnd()) return;
            }
        }

        // 讨论阶段
        discussionPhase();

        // 投票阶段
        Player votedOut = votingPhase();

        if (votedOut != null) {
            votedOut.kill();
            emitter.emitPlayerEliminated(
                    votedOut.getName(),
                    messages.getRoleDisplayName(votedOut.getRole()),
                    "voted");

            // 猎人技能
            if (votedOut.getRole() == Role.HUNTER) {
                hunterShoot(votedOut);
            }
        }

        emitStatsUpdate();
    }

    /**
     * 讨论阶段
     */
    private void discussionPhase() {
        List<Player> alivePlayers = gameState.getAlivePlayers();
        if (alivePlayers.size() <= 2) return;

        try (MsgHub discussionHub = MsgHub.builder()
                .name("DayDiscussion")
                .participants(alivePlayers.stream()
                        .map(Player::getAgent)
                        .toArray(ReActAgent[]::new))
                .enableAutoBroadcast(true)
                .build()) {

            discussionHub.enter().block();

            // 讨论 3 轮
            for (int round = 1; round <= MAX_DISCUSSION_ROUNDS; round++) {
                emitter.emitSystemMessage("讨论第 " + round + " 轮");

                for (Player player : alivePlayers) {
                    Msg response = player.getAgent().call().block();
                    String content = utils.extractTextContent(response);
                    emitter.emitPlayerSpeak(player.getName(), content, "day_discussion");
                }
            }
        }
    }

    /**
     * 投票阶段
     */
    private Player votingPhase() {
        List<Player> alivePlayers = gameState.getAlivePlayers();
        if (alivePlayers.size() <= 1) return null;

        emitter.emitSystemMessage("请所有玩家投票，选择要放逐的玩家。");

        // 并行投票
        FanoutPipeline votingPipeline = FanoutPipeline.builder()
                .addAgents(alivePlayers.stream()
                        .map(p -> (AgentBase) p.getAgent()).toList())
                .concurrent()
                .build();

        List<Msg> votes = votingPipeline.execute(
                prompts.createVotingPrompt(gameState),
                VoteModel.class).block();

        // 显示投票
        for (Msg vote : votes) {
            try {
                VoteModel voteData = vote.getStructuredData(VoteModel.class);
                emitter.emitPlayerVote(
                        vote.getName(),
                        voteData.targetPlayer,
                        voteData.reason);
            } catch (Exception e) {
                emitter.emitSystemMessage(
                        messages.getVoteParsingError(vote.getName()));
            }
        }

        // 统计票数
        return utils.countVotes(votes, gameState);
    }

    /**
     * 猎人开枪
     */
    private void hunterShoot(Player hunter) {
        emitter.emitSystemMessage(
                messages.getRoleDisplayName(Role.HUNTER) + " "
                        + hunter.getName() + " 请发动技能。");

        Msg shootDecision = hunter.getAgent()
                .call(prompts.createHunterShootPrompt(gameState, hunter),
                        HunterShootModel.class)
                .block();

        HunterShootModel shootModel =
                shootDecision.getStructuredData(HunterShootModel.class);

        if (Boolean.TRUE.equals(shootModel.willShoot)
                && shootModel.targetPlayer != null) {
            Player target = gameState.findPlayerByName(shootModel.targetPlayer);
            if (target != null && target.isAlive()) {
                target.kill();
                emitter.emitPlayerEliminated(
                        target.getName(),
                        messages.getRoleDisplayName(target.getRole()),
                        "shot");
            }
        }
    }

    /**
     * 检查游戏是否结束
     */
    private boolean checkGameEnd() {
        return gameState.checkVillagersWin() || gameState.checkWerewolvesWin();
    }

    /**
     * 宣布游戏结果
     */
    private void announceWinner() {
        if (gameState.checkVillagersWin()) {
            emitter.emitGameEnd("villagers",
                    "好人阵营胜利！所有狼人已被放逐。");
        } else if (gameState.checkWerewolvesWin()) {
            emitter.emitGameEnd("werewolves",
                    "狼人阵营胜利！狼人数量已超过好人。");
        } else {
            emitter.emitGameEnd("none", "游戏达到最大回合数，平局。");
        }
    }

    private void emitStatsUpdate() {
        emitter.emitStatsUpdate(
                gameState.getAlivePlayers().size(),
                gameState.getAliveWerewolves().size(),
                gameState.getAliveVillagers().size());
    }
}
```

### 19.2.4 投票数据模型

```java
// 文件：VoteModel.java
package io.agentscope.tutorial.chapter19.werewolf.model;

import io.agentscope.core.annotation.StructuredData;

/**
 * 投票数据模型
 *
 * 用于结构化 LLM 输出的投票决策
 */
@StructuredData
public class VoteModel {

    /**
     * 投票目标玩家名称
     */
    public String targetPlayer;

    /**
     * 投票理由（可选）
     */
    public String reason;

    public VoteModel() {}

    public VoteModel(String targetPlayer, String reason) {
        this.targetPlayer = targetPlayer;
        this.reason = reason;
    }
}
```

### 19.2.5 配置文件

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: werewolf-game

# 游戏配置
werewolf:
  model: qwen3-max
  max-rounds: 30
  max-discussion-rounds: 3

# 角色配置
roles:
  villager-count: 3
  werewolf-count: 3
  seer-count: 1
  witch-count: 1
  hunter-count: 1
```

### 19.2.6 运行说明

**1. 环境准备**

```bash
# 设置 API Key
export DASHSCOPE_API_KEY=your_api_key_here

# 验证 Java 环境
java -version  # 需要 21.x
```

**2. 启动游戏**

```bash
# 编译项目
cd werewolf
mvn clean package -DskipTests

# 启动应用
mvn spring-boot:run
```

**3. 测试游戏**

使用浏览器或 curl 测试：

```bash
# 启动中文游戏（默认）
curl -X POST "http://localhost:8080/api/game/start?lang=zh-CN"

# 启动英文游戏
curl -X POST "http://localhost:8080/api/game/start?lang=en-US"
```

**4. 前端集成**

前端通过 SSE 接收游戏事件：

```javascript
// 前端 SSE 连接示例
const eventSource = new EventSource('http://localhost:8080/api/game/start?lang=zh-CN');

eventSource.addEventListener('game_init', (e) => {
    const players = JSON.parse(e.data).playersInfo;
    console.log('游戏初始化，玩家:', players);
});

eventSource.addEventListener('phase_change', (e) => {
    const phase = JSON.parse(e.data);
    console.log(`第 ${phase.round} 回合 - ${phase.phase}`);
});

eventSource.addEventListener('player_speak', (e) => {
    const speak = JSON.parse(e.data);
    console.log(`${speak.playerName}: ${speak.content}`);
});

eventSource.addEventListener('player_vote', (e) => {
    const vote = JSON.parse(e.data);
    console.log(`${vote.playerName} 投票给 ${vote.targetPlayer}`);
});

eventSource.addEventListener('player_eliminated', (e) => {
    const death = JSON.parse(e.data);
    console.log(`${death.playerName}（${death.roleDisplay}）被${death.cause}`);
});

eventSource.addEventListener('game_end', (e) => {
    const result = JSON.parse(e.data);
    console.log(`游戏结束 - ${result.winner} 获胜: ${result.content}`);
});
```

---

## 19.3 【案例】企业智能助手

### 19.3.1 系统概述

企业智能助手是一个面向企业场景的多功能 AI 助手系统，集成了知识库问答、日程管理、邮件处理、报表生成等多种能力。系统采用多代理架构，主代理负责意图识别和任务分发，各专业子代理负责具体业务处理。

**系统架构图：**

```
┌─────────────────────────────────────────────────────────────────┐
│                    企业用户（多终端）                            │
│    ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│    │  Web   │  │ 钉钉/企微 │  │ 钉钉/企微 │  │   API   │      │
│    └──────────┘  └──────────┘  └──────────┘  └──────────┘      │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     企业智能助手网关                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  意图识别 → 能力路由 → 会话管理 → 安全审计                │  │
│  └──────────────────────────────────────────────────────────┘  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              │                 │                 │
              ▼                 ▼                 ▼
    ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
    │  知识库问答代理   │ │   日程管理代理   │ │   报表生成代理   │
    │                  │ │                  │ │                  │
    │  - FAQ检索       │ │  - 创建日程      │ │  - 数据查询      │
    │  - 文档问答      │ │  - 查询日程      │ │  - 图表生成      │
    │  - RAG 增强      │ │  - 发送邀请      │ │  - 定时推送      │
    └────────┬─────────┘ └────────┬─────────┘ └────────┬─────────┘
             │                    │                    │
             └────────────────────┼────────────────────┘
                                  │
                                  ▼
                    ┌─────────────────────────┐
                    │     H2 嵌入式数据库      │
                    │  - 知识库              │
                    │  - 会话历史            │
                    │  - 日程数据            │
                    │  - 报表模板            │
                    └─────────────────────────┘
```

### 19.3.2 项目结构

```
enterprise-assistant/
├── src/main/java/io/agentscope/tutorial/chapter19/enterprise/
│   ├── EnterpriseAssistantApplication.java    # 应用入口
│   ├── agent/
│   │   ├── EnterpriseAssistantAgent.java       # 主助手代理
│   │   ├── KnowledgeAgent.java                 # 知识库问答代理
│   │   ├── ScheduleAgent.java                  # 日程管理代理
│   │   └── ReportAgent.java                    # 报表生成代理
│   ├── config/
│   │   ├── AgentConfig.java                    # 代理配置
│   │   ├── PromptConfig.java                   # 提示词配置
│   │   └── RAGConfig.java                      # RAG 配置
│   ├── controller/
│   │   └── AssistantController.java            # Web 控制器
│   ├── service/
│   │   ├── KnowledgeService.java               # 知识库服务
│   │   ├── ScheduleService.java                # 日程服务
│   │   └── ReportService.java                  # 报表服务
│   ├── entity/
│   │   ├── KnowledgeArticle.java               # 知识文章
│   │   ├── Schedule.java                       # 日程
│   │   └── Report.java                         # 报表
│   └── tools/
│       └── AssistantTools.java                # 工具类
└── src/main/resources/
    ├── application.yml
    └── db/
        └── schema.sql                          # 数据库 Schema
```

### 19.3.3 核心实现

#### 1. 企业助手主代理

```java
// 文件：EnterpriseAssistantAgent.java
package io.agentscope.tutorial.chapter19.enterprise.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Event;
import io.agentscope.core.memory.Memory;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.mysql.MysqlSession;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter19.enterprise.tools.AssistantTools;
import java.nio.file.Paths;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;

/**
 * 企业智能助手主代理
 *
 * 负责：
 * 1. 理解用户意图
 * 2. 调用相应的子代理处理任务
 * 3. 聚合结果返回给用户
 */
public class EnterpriseAssistantAgent {

    private static final Logger logger =
            LoggerFactory.getLogger(EnterpriseAssistantAgent.class);

    private final Model model;
    private final AssistantTools tools;
    private final String sysPrompt;

    @Autowired
    private DataSource dataSource;

    public EnterpriseAssistantAgent(
            Model model,
            AssistantTools tools,
            String sysPrompt) {
        this.model = model;
        this.tools = tools;
        this.sysPrompt = sysPrompt;
    }

    /**
     * 处理用户请求
     *
     * @param msg 用户消息
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 事件流
     */
    public Flux<Event> stream(Msg msg, String sessionId, String userId) {
        logger.info("处理用户请求 - userId: {}, sessionId: {}", userId, sessionId);

        // 创建工具集
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(tools);

        // 配置上下文压缩：当 token 占比超过 50% 时压缩
        AutoContextConfig autoContextConfig = AutoContextConfig.builder()
                .tokenRatio(0.5)
                .lastKeep(15)
                .build();

        // 使用自动上下文内存
        AutoContextMemory memory = new AutoContextMemory(autoContextConfig, model);

        // MySQL 会话持久化
        MysqlSession mysqlSession = new MysqlSession(
                dataSource,
                "enterprise_assistant",
                null,
                true);

        // 创建 ReActAgent
        ReActAgent agent = ReActAgent.builder()
                .name("enterprise_assistant")
                .sysPrompt(sysPrompt)
                .toolkit(toolkit)
                .model(model)
                .memory(memory)
                .build();

        // 加载历史会话
        agent.loadIfExists(mysqlSession, sessionId);

        return agent.stream(msg)
                .doFinally(signalType -> {
                    logger.info("请求处理完成，保存会话: {}", sessionId);
                    agent.saveTo(mysqlSession, sessionId);
                });
    }
}
```

#### 2. 知识库问答代理

```java
// 文件：KnowledgeAgent.java
package io.agentscope.tutorial.chapter19.enterprise.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter19.enterprise.service.KnowledgeService;
import io.agentscope.tutorial.chapter19.enterprise.entity.KnowledgeArticle;
import java.util.List;

/**
 * 知识库问答代理
 *
 * 负责：
 * - FAQ 问答
 * - 文档检索
 * - RAG 增强回答
 */
public class KnowledgeAgent {

    private final Model model;
    private final KnowledgeService knowledgeService;
    private final String sysPrompt;

    public KnowledgeAgent(
            Model model,
            KnowledgeService knowledgeService,
            String sysPrompt) {
        this.model = model;
        this.knowledgeService = knowledgeService;
        this.sysPrompt = sysPrompt;
    }

    /**
     * 回答知识库相关问题
     *
     * @param query 用户问题
     * @return 回答内容
     */
    public String answer(String query) {
        // 检索相关知识
        List<KnowledgeArticle> articles = knowledgeService.search(query, 5);

        if (articles.isEmpty()) {
            return "抱歉，我没有找到相关的知识内容。";
        }

        // 构建增强提示词
        StringBuilder context = new StringBuilder();
        context.append("参考以下知识内容回答用户问题：\n\n");

        for (int i = 0; i < articles.size(); i++) {
            KnowledgeArticle article = articles.get(i);
            context.append(String.format("【文档 %d】%s\n%s\n\n",
                    i + 1,
                    article.getTitle(),
                    article.getContent()));
        }

        context.append("用户问题：").append(query);

        // 创建 Agent 回答
        ReActAgent agent = ReActAgent.builder()
                .name("knowledge_agent")
                .sysPrompt(buildSysPrompt())
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(new Toolkit())
                .build();

        Msg response = agent.call(Msg.builder()
                .content(TextBlock.builder().text(context.toString()).build())
                .build()).block();

        return extractText(response);
    }

    /**
     * RAG 增强检索
     */
    public String answerWithRAG(String query) {
        // 向量检索
        List<KnowledgeArticle> articles = knowledgeService.vectorSearch(query, 5);

        // 构建上下文
        StringBuilder context = new StringBuilder();
        context.append("请根据以下检索到的知识内容，准确回答用户问题。\n\n");

        for (KnowledgeArticle article : articles) {
            context.append(String.format("标题：%s\n内容：%s\n相似度：%.2f\n\n",
                    article.getTitle(),
                    article.getContent(),
                    article.getScore()));
        }

        context.append("用户问题：").append(query);

        ReActAgent agent = ReActAgent.builder()
                .name("knowledge_agent_rag")
                .sysPrompt(buildRagPrompt())
                .model(model)
                .memory(new InMemoryMemory())
                .toolkit(new Toolkit())
                .build();

        Msg response = agent.call(Msg.builder()
                .content(TextBlock.builder().text(context.toString()).build())
                .build()).block();

        return extractText(response);
    }

    private String buildSysPrompt() {
        return """
                你是一个企业知识库助手，负责根据检索到的知识内容回答用户问题。

                回答要求：
                1. 基于提供的知识内容进行回答，不要编造信息
                2. 如果知识内容不足以回答，说明无法回答
                3. 回答要清晰、准确、易懂
                4. 可以引用具体的文档内容来支持回答

                如果没有找到相关知识，明确告知用户。
                """;
    }

    private String buildRagPrompt() {
        return """
                你是一个企业知识库助手，基于 RAG 检索结果回答用户问题。

                回答要求：
                1. 充分利用检索到的文档内容
                2. 标注信息来源（文档标题）
                3. 如果检索结果不够，诚实说明
                4. 保持回答的专业性和准确性
                """;
    }

    private String extractText(Msg msg) {
        return msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .findFirst()
                .orElse("");
    }
}
```

#### 3. 日程管理代理

```java
// 文件：ScheduleAgent.java
package io.agentscope.tutorial.chapter19.enterprise.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter19.enterprise.entity.Schedule;
import io.agentscope.tutorial.chapter19.enterprise.service.ScheduleService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日程管理代理
 *
 * 负责：
 * - 创建日程
 * - 查询日程
 * - 修改日程
 * - 发送日程邀请
 */
public class ScheduleAgent {

    private final Model model;
    private final ScheduleService scheduleService;
    private final String sysPrompt;

    public ScheduleAgent(
            Model model,
            ScheduleService scheduleService,
            String sysPrompt) {
        this.model = model;
        this.scheduleService = scheduleService;
        this.sysPrompt = sysPrompt;
    }

    /**
     * 创建日程
     *
     * @param params 日程参数
     * @return 创建结果
     */
    public String createSchedule(Map<String, Object> params) {
        try {
            String title = (String) params.get("title");
            String description = (String) params.get("description");
            String startTime = (String) params.get("startTime");
            String endTime = (String) params.get("endTime");
            List<String> attendees = (List<String>) params.get("attendees");

            Schedule schedule = scheduleService.createSchedule(
                    title, description,
                    LocalDateTime.parse(startTime),
                    LocalDateTime.parse(endTime),
                    attendees);

            StringBuilder result = new StringBuilder();
            result.append("日程创建成功！\n");
            result.append(String.format("标题：%s\n", schedule.getTitle()));
            result.append(String.format("时间：%s - %s\n",
                    schedule.getStartTime(), schedule.getEndTime()));
            result.append(String.format("ID：%s", schedule.getId()));

            return result.toString();

        } catch (Exception e) {
            return "创建日程失败：" + e.getMessage();
        }
    }

    /**
     * 查询用户的日程
     *
     * @param userId 用户ID
     * @param date 日期（可选）
     * @return 日程列表
     */
    public String querySchedules(String userId, String date) {
        List<Schedule> schedules;

        if (date != null && !date.isEmpty()) {
            schedules = scheduleService.getSchedulesByDate(userId, date);
        } else {
            schedules = scheduleService.getUpcomingSchedules(userId);
        }

        if (schedules.isEmpty()) {
            return "暂无日程安排";
        }

        StringBuilder result = new StringBuilder("您的日程安排：\n\n");

        for (int i = 0; i < schedules.size(); i++) {
            Schedule s = schedules.get(i);
            result.append(String.format("%d. %s\n", i + 1, s.getTitle()));
            result.append(String.format("   时间：%s - %s\n",
                    s.getStartTime(), s.getEndTime()));
            if (s.getDescription() != null) {
                result.append(String.format("   描述：%s\n", s.getDescription()));
            }
            result.append("\n");
        }

        return result.toString();
    }

    /**
     * 更新日程
     *
     * @param scheduleId 日程ID
     * @param params 更新参数
     * @return 更新结果
     */
    public String updateSchedule(String scheduleId, Map<String, Object> params) {
        try {
            boolean success = scheduleService.updateSchedule(scheduleId, params);

            if (success) {
                return "日程更新成功！";
            } else {
                return "日程更新失败，日程可能不存在。";
            }
        } catch (Exception e) {
            return "更新日程失败：" + e.getMessage();
        }
    }

    /**
     * 发送日程邀请
     */
    public String sendInvitation(String scheduleId) {
        try {
            scheduleService.sendInvitations(scheduleId);
            return "日程邀请已发送！";
        } catch (Exception e) {
            return "发送邀请失败：" + e.getMessage();
        }
    }

    /**
     * 删除日程
     */
    public String deleteSchedule(String scheduleId) {
        try {
            boolean success = scheduleService.deleteSchedule(scheduleId);
            return success ? "日程已删除" : "删除失败，日程可能不存在";
        } catch (Exception e) {
            return "删除日程失败：" + e.getMessage();
        }
    }
}
```

#### 4. 报表生成代理

```java
// 文件：ReportAgent.java
package io.agentscope.tutorial.chapter19.enterprise.agent;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter19.enterprise.entity.Report;
import io.agentscope.tutorial.chapter19.enterprise.service.ReportService;
import java.util.Map;

/**
 * 报表生成代理
 *
 * 负责：
 * - 数据查询和分析
 * - 图表生成
 * - 报表模板应用
 * - 定时推送
 */
public class ReportAgent {

    private final Model model;
    private final ReportService reportService;
    private final String sysPrompt;

    public ReportAgent(
            Model model,
            ReportService reportService,
            String sysPrompt) {
        this.model = model;
        this.reportService = reportService;
        this.sysPrompt = sysPrompt;
    }

    /**
     * 生成数据报表
     *
     * @param params 报表参数
     * @return 报表内容
     */
    public String generateReport(Map<String, Object> params) {
        try {
            String reportType = (String) params.get("type");
            String dataRange = (String) params.get("dataRange");
            String format = (String) params.getOrDefault("format", "text");

            // 查询数据
            Map<String, Object> data = reportService.queryData(reportType, dataRange);

            // 生成报表
            Report report = reportService.generateReport(
                    reportType, dataRange, data, format);

            return formatReport(report);

        } catch (Exception e) {
            return "生成报表失败：" + e.getMessage();
        }
    }

    /**
     * 生成图表
     */
    public String generateChart(Map<String, Object> params) {
        try {
            String chartType = (String) params.get("chartType");
            String dataSource = (String) params.get("dataSource");
            Map<String, Object> options = (Map<String, Object>) params.get("options");

            // 查询图表数据
            Map<String, Object> chartData = reportService.queryChartData(
                    dataSource, chartType);

            // 生成图表配置
            String chartConfig = reportService.generateChartConfig(
                    chartType, chartData, options);

            return chartConfig;

        } catch (Exception e) {
            return "生成图表失败：" + e.getMessage();
        }
    }

    /**
     * 设置定时推送
     */
    public String setupScheduleReport(Map<String, Object> params) {
        try {
            String reportType = (String) params.get("type");
            String cron = (String) params.get("cron");
            String recipients = (String) params.get("recipients");

            reportService.setupScheduledReport(reportType, cron, recipients);

            return String.format("定时报表设置成功！\n" +
                    "类型：%s\n" +
                    "执行时间：%s\n" +
                    "接收人：%s", reportType, cron, recipients);

        } catch (Exception e) {
            return "设置定时报表失败：" + e.getMessage();
        }
    }

    /**
     * 查询历史报表
     */
    public String queryHistoryReports(String userId, String type, int limit) {
        var reports = reportService.getHistoryReports(userId, type, limit);

        if (reports.isEmpty()) {
            return "暂无历史报表";
        }

        StringBuilder result = new StringBuilder("历史报表：\n\n");

        for (Report report : reports) {
            result.append(String.format("- %s (%s)\n",
                    report.getTitle(), report.getCreatedAt()));
        }

        return result.toString();
    }

    /**
     * 格式化报表输出
     */
    private String formatReport(Report report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(50)).append("\n");
        sb.append(report.getTitle()).append("\n");
        sb.append("=".repeat(50)).append("\n\n");

        sb.append(report.getContent()).append("\n\n");

        sb.append("-".repeat(50)).append("\n");
        sb.append(String.format("生成时间：%s\n", report.getCreatedAt()));
        sb.append(String.format("数据范围：%s", report.getDataRange()));

        return sb.toString();
    }
}
```

#### 5. 知识库服务

```java
// 文件：KnowledgeService.java
package io.agentscope.tutorial.chapter19.enterprise.service;

import io.agentscope.tutorial.chapter19.enterprise.entity.KnowledgeArticle;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 知识库服务
 *
 * 负责知识文章的存储、检索和管理
 */
@Service
public class KnowledgeService {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 搜索知识文章（关键词匹配）
     */
    public List<KnowledgeArticle> search(String query, int limit) {
        String sql = """
                SELECT * FROM knowledge_articles
                WHERE match(title, content) AGAINST(? IN NATURAL LANGUAGE MODE)
                   OR title LIKE CONCAT('%', ?, '%')
                   OR content LIKE CONCAT('%', ?, '%')
                ORDER BY score DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql,
                new KnowledgeArticleRowMapper(),
                query, query, query, limit);
    }

    /**
     * 向量检索（RAG 增强）
     */
    public List<KnowledgeArticle> vectorSearch(String query, int limit) {
        // H2 不支持向量检索，这里使用简化的相似度匹配
        // 生产环境应使用 PostgreSQL + pgvector 或其他向量数据库
        String sql = """
                SELECT *,
                       (CASE
                            WHEN title LIKE CONCAT('%', ?, '%') THEN 2.0
                            ELSE 0.0
                        END +
                        CASE
                            WHEN content LIKE CONCAT('%', ?, '%') THEN 1.0
                            ELSE 0.0
                        END) as score
                FROM knowledge_articles
                WHERE title LIKE CONCAT('%', ?, '%')
                   OR content LIKE CONCAT('%', ?, '%')
                ORDER BY score DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql,
                new KnowledgeArticleRowMapper(),
                query, query, query, query, limit);
    }

    /**
     * 保存知识文章
     */
    public KnowledgeArticle save(KnowledgeArticle article) {
        if (article.getId() == null) {
            String sql = """
                    INSERT INTO knowledge_articles
                    (title, content, category, tags, created_at, updated_at)
                    VALUES (?, ?, ?, ?, NOW(), NOW())
                    """;
            jdbcTemplate.update(sql,
                    article.getTitle(),
                    article.getContent(),
                    article.getCategory(),
                    String.join(",", article.getTags()));
        } else {
            String sql = """
                    UPDATE knowledge_articles
                    SET title = ?, content = ?, category = ?, tags = ?,
                        updated_at = NOW()
                    WHERE id = ?
                    """;
            jdbcTemplate.update(sql,
                    article.getTitle(),
                    article.getContent(),
                    article.getCategory(),
                    String.join(",", article.getTags()),
                    article.getId());
        }

        return article;
    }

    /**
     * 根据分类查询
     */
    public List<KnowledgeArticle> findByCategory(String category) {
        String sql = "SELECT * FROM knowledge_articles WHERE category = ?";
        return jdbcTemplate.query(sql,
                new KnowledgeArticleRowMapper(),
                category);
    }

    /**
     * 删除文章
     */
    public void delete(Long id) {
        String sql = "DELETE FROM knowledge_articles WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    private static class KnowledgeArticleRowMapper
            implements RowMapper<KnowledgeArticle> {

        @Override
        public KnowledgeArticle mapRow(ResultSet rs, int rowNum)
                throws SQLException {

            KnowledgeArticle article = new KnowledgeArticle();
            article.setId(rs.getLong("id"));
            article.setTitle(rs.getString("title"));
            article.setContent(rs.getString("content"));
            article.setCategory(rs.getString("category"));

            String tags = rs.getString("tags");
            if (tags != null && !tags.isEmpty()) {
                article.setTags(List.of(tags.split(",")));
            }

            article.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            article.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

            // 计算相似度分数（如果有）
            try {
                article.setScore(rs.getDouble("score"));
            } catch (SQLException e) {
                article.setScore(1.0);
            }

            return article;
        }
    }
}
```

### 19.3.4 工具类

```java
// 文件：AssistantTools.java
package io.agentscope.tutorial.chapter19.enterprise.tools;

import io.agentscope.core.tool.ToolResult;
import io.agentscope.core.tool.ToolSpec;
import io.agentscope.tutorial.chapter19.enterprise.agent.KnowledgeAgent;
import io.agentscope.tutorial.chapter19.enterprise.agent.ScheduleAgent;
import io.agentscope.tutorial.chapter19.enterprise.agent.ReportAgent;
import java.util.List;
import java.util.Map;

/**
 * 企业助手工具类
 *
 * 提供与各子代理交互的工具
 */
public class AssistantTools {

    private final KnowledgeAgent knowledgeAgent;
    private final ScheduleAgent scheduleAgent;
    private final ReportAgent reportAgent;

    public AssistantTools(
            KnowledgeAgent knowledgeAgent,
            ScheduleAgent scheduleAgent,
            ReportAgent reportAgent) {
        this.knowledgeAgent = knowledgeAgent;
        this.scheduleAgent = scheduleAgent;
        this.reportAgent = reportAgent;
    }

    /**
     * 获取工具规格列表
     */
    public static List<ToolSpec> getToolSpecs() {
        return List.of(
                // 知识库问答
                ToolSpec.builder()
                        .name("search_knowledge")
                        .description("搜索企业知识库，回答用户关于公司制度、业务流程等问题")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "query": {
                                            "type": "string",
                                            "description": "用户问题或关键词"
                                        }
                                    },
                                    "required": ["query"]
                                }
                                """)
                        .build(),

                // 创建日程
                ToolSpec.builder()
                        .name("create_schedule")
                        .description("创建新的日程安排")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "title": {"type": "string", "description": "日程标题"},
                                        "description": {"type": "string", "description": "日程描述"},
                                        "startTime": {"type": "string", "description": "开始时间 ISO 格式"},
                                        "endTime": {"type": "string", "description": "结束时间 ISO 格式"},
                                        "attendees": {
                                            "type": "array",
                                            "items": {"type": "string"},
                                            "description": "参与人邮箱列表"
                                        }
                                    },
                                    "required": ["title", "startTime", "endTime"]
                                }
                                """)
                        .build(),

                // 查询日程
                ToolSpec.builder()
                        .name("query_schedules")
                        .description("查询用户的日程安排")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "date": {
                                            "type": "string",
                                            "description": "查询日期（可选，如 2024-01-15）"
                                        }
                                    }
                                }
                                """)
                        .build(),

                // 生成报表
                ToolSpec.builder()
                        .name("generate_report")
                        .description("生成数据报表")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "type": {
                                            "type": "string",
                                            "description": "报表类型（如 sales, user, performance）"
                                        },
                                        "dataRange": {
                                            "type": "string",
                                            "description": "数据范围（如 2024-01, last_week）"
                                        },
                                        "format": {
                                            "type": "string",
                                            "description": "输出格式（text, markdown, json）"
                                        }
                                    },
                                    "required": ["type", "dataRange"]
                                }
                                """)
                        .build(),

                // 生成图表
                ToolSpec.builder()
                        .name("generate_chart")
                        .description("生成数据图表")
                        .paramSpec("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "chartType": {
                                            "type": "string",
                                            "description": "图表类型（bar, line, pie, table）"
                                        },
                                        "dataSource": {
                                            "type": "string",
                                            "description": "数据源标识"
                                        },
                                        "options": {
                                            "type": "object",
                                            "description": "图表配置选项"
                                        }
                                    },
                                    "required": ["chartType", "dataSource"]
                                }
                                """)
                        .build()
        );
    }

    // 工具执行方法
    public ToolResult searchKnowledge(String query) {
        try {
            String answer = knowledgeAgent.answer(query);
            return ToolResult.success("知识检索完成", answer);
        } catch (Exception e) {
            return ToolResult.failure("检索失败：" + e.getMessage());
        }
    }

    public ToolResult createSchedule(Map<String, Object> params) {
        try {
            String result = scheduleAgent.createSchedule(params);
            return ToolResult.success("日程创建完成", result);
        } catch (Exception e) {
            return ToolResult.failure("创建日程失败：" + e.getMessage());
        }
    }

    public ToolResult querySchedules(String date) {
        try {
            // 从当前上下文获取 userId
            String userId = "current_user";
            String result = scheduleAgent.querySchedules(userId, date);
            return ToolResult.success("日程查询完成", result);
        } catch (Exception e) {
            return ToolResult.failure("查询日程失败：" + e.getMessage());
        }
    }

    public ToolResult generateReport(Map<String, Object> params) {
        try {
            String result = reportAgent.generateReport(params);
            return ToolResult.success("报表生成完成", result);
        } catch (Exception e) {
            return ToolResult.failure("生成报表失败：" + e.getMessage());
        }
    }

    public ToolResult generateChart(Map<String, Object> params) {
        try {
            String result = reportAgent.generateChart(params);
            return ToolResult.success("图表生成完成", result);
        } catch (Exception e) {
            return ToolResult.failure("生成图表失败：" + e.getMessage());
        }
    }
}
```

### 19.3.5 数据库 Schema

```sql
-- 企业智能助手数据库 Schema
-- H2 嵌入式数据库

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(100),
    tags VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FULLTEXT (title, content)
);

-- 日程表
CREATE TABLE IF NOT EXISTS schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    attendees VARCHAR(1000),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 报表表
CREATE TABLE IF NOT EXISTS reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    content TEXT,
    data_range VARCHAR(100),
    format VARCHAR(20) DEFAULT 'text',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 会话历史表
CREATE TABLE IF NOT EXISTS session_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    messages TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 定时任务表
CREATE TABLE IF NOT EXISTS scheduled_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    cron_expression VARCHAR(100),
    params TEXT,
    last_run TIMESTAMP,
    next_run TIMESTAMP,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 初始化知识库数据
INSERT INTO knowledge_articles (title, content, category, tags) VALUES
('请假制度', '员工请假需提前在系统中提交申请，审批流程：员工 -> 直属上级 -> HR', '人力资源', '请假,考勤,制度'),
('报销流程', '员工报销需在每月 5 日前提交上月票据，审批周期 3 个工作日', '财务', '报销,财务,流程'),
('IT 支持', 'IT 问题请联系 helpdesk@company.com 或拨打 8001', 'IT服务', 'IT,技术支持'),
('会议室预约', '会议室通过 OA 系统预约，提前 2 小时可预约使用', '行政', '会议,预约,办公');

-- 初始化示例日程
INSERT INTO schedules (user_id, title, description, start_time, end_time) VALUES
('admin', '周例会', '部门周例会', DATEADD('DAY', 1, CURRENT_TIMESTAMP), DATEADD('HOUR', 1, DATEADD('DAY', 1, CURRENT_TIMESTAMP)));

-- 初始化示例报表
INSERT INTO reports (user_id, title, type, content, data_range) VALUES
('admin', '销售月报', 'sales', '本月销售额 100 万，环比增长 15%', 'last_month');
```

### 19.3.6 应用配置

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: enterprise-assistant

  datasource:
    url: jdbc:h2:mem:enterprise;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
    username: sa
    password:

  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql

# AgentScope 配置
agentscope:
  model:
    provider: dashscope
    api-key: ${DASHSCOPE_API_KEY:demo}
    model-name: qwen3-max

# 代理提示词配置
enterprise:
  prompts:
    assistant-instruction: |
      你是一个企业智能助手，帮助用户处理日常工作。

      你可以：
      1. 回答企业知识库相关问题
      2. 创建和管理日程安排
      3. 生成数据报表和图表
      4. 设置定时任务和推送

      请根据用户需求，调用相应的工具完成任务。
      回答要专业、简洁、准确。

# RAG 配置
rag:
  embedding:
    model: text-embedding-v3
    dimension: 1536
  retrieval:
    top-k: 5
    min-score: 0.7
```

### 19.3.7 实体类

```java
// 文件：KnowledgeArticle.java
package io.agentscope.tutorial.chapter19.enterprise.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文章实体
 */
public class KnowledgeArticle {

    private Long id;
    private String title;
    private String content;
    private String category;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private double score;  // 检索相似度分数

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
```

```java
// 文件：Schedule.java
package io.agentscope.tutorial.chapter19.enterprise.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日程实体
 */
public class Schedule {

    private Long id;
    private String userId;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> attendees;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public List<String> getAttendees() { return attendees; }
    public void setAttendees(List<String> attendees) { this.attendees = attendees; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

```java
// 文件：Report.java
package io.agentscope.tutorial.chapter19.enterprise.entity;

import java.time.LocalDateTime;

/**
 * 报表实体
 */
public class Report {

    private Long id;
    private String userId;
    private String title;
    private String type;
    private String content;
    private String dataRange;
    private String format;
    private LocalDateTime createdAt;

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDataRange() { return dataRange; }
    public void setDataRange(String dataRange) { this.dataRange = dataRange; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

### 19.3.8 运行说明

**1. 环境准备**

```bash
# 确保已安装 Java 21
java -version

# 设置 API Key
export DASHSCOPE_API_KEY=your_api_key_here
```

**2. 启动应用**

```bash
cd enterprise-assistant
mvn spring-boot:run
```

**3. 测试接口**

```bash
# 聊天接口
curl -X GET "http://localhost:8080/api/assistant/chat?chat_id=test&user_query=我想查一下请假制度&user_id=user001"

# 查询知识库
curl -X GET "http://localhost:8080/api/assistant/knowledge?query=报销流程"

# 创建日程
curl -X POST "http://localhost:8080/api/assistant/schedule" \
  -H "Content-Type: application/json" \
  -d '{"title":"团队会议","startTime":"2024-01-15T10:00","endTime":"2024-01-15T11:00"}'

# 生成报表
curl -X GET "http://localhost:8080/api/assistant/report?type=sales&dataRange=last_month"
```

---

## 19.4 本章小结

本章通过三个完整的实战案例，展示了 AgentScope Java 在不同场景下的应用能力：

| 案例 | 场景 | 核心特性 | 关键技术 |
|------|------|----------|----------|
| 奶茶店多代理系统 | 电商/客服 | 多代理协作、A2A 通信、MCP 协议 | Supervisor Agent、MCP Server、MySQL Session |
| 狼人杀游戏 | 游戏/社交 | 多 Agent 博弈、实时 SSE、MsgHub 讨论 | FanoutPipeline、MsgHub、ReAct Agent |
| 企业智能助手 | 企业服务 | RAG 检索、日程管理、报表生成 | RAG、AutoContextMemory、多子代理协作 |

**共同的设计模式：**

1. **代理分工**：主代理负责协调，子代理负责具体业务
2. **会话持久化**：使用 MysqlSession 保存对话历史
3. **工具集成**：通过 Toolkit 注册工具，实现能力扩展
4. **流式响应**：使用 Flux 实现 SSE 流式输出

这些案例可以作为开发复杂多代理系统的参考模板。