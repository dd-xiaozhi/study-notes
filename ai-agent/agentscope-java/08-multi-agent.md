# 第八章：多代理架构模式

> 本章深入讲解 AgentScope Java 框架下的多代理协作模式，涵盖 7 种核心架构模式，并通过完整案例展示如何用 Java 21 + Spring Boot 3 构建复杂的多代理系统。

---

## 8.1 多代理协作概述

### 8.1.1 为什么需要多代理？

单代理（Single Agent）受限于其设计目标：每个 Agent 只有一套系统提示词、一组工具和有限的上下文窗口。当业务场景变得复杂时，单代理往往会出现"能力稀释"问题——试图让一个 Agent 兼顾所有职责，结果反而每件事都做不好。

多代理架构通过将职责分离，让专业 Agent 做专业的事：

- **解耦复杂任务**：将问题分解为多个子问题，每个子问题由专门的 Agent 处理
- **提升模型效率**：不同 Agent 可以使用不同规模/成本的模型
- **增强可维护性**：每个 Agent 的职责单一，调试和优化更清晰
- **支持并行执行**：相互独立的子任务可以并发处理

### 8.1.2 AgentScope Java 的多代理能力

AgentScope Java 与 Spring AI Alibaba 深度集成，提供了完整的多代理编排能力：

```
┌─────────────────────────────────────────────────────────────┐
│                    AgentScope Java                          │
├─────────────────────────────────────────────────────────────┤
│  核心抽象层                                                 │
│  ├── ReActAgent          单代理执行引擎                     │
│  ├── AgentScopeAgent     代理器（适配 Spring AI Alibaba）   │
│  ├── StateGraph          状态图编排                         │
│  └── Pipeline Agent      流水线编排（顺序/并行/循环）       │
├─────────────────────────────────────────────────────────────┤
│  编排模式                                                   │
│  ├── Handoffs            交接模式（控制权转移）             │
│  ├── Pipeline            流水线模式（顺序/并行/循环）       │
│  ├── Routing             路由模式（条件分发）               │
│  ├── Supervisor          监督者模式（层级管理）             │
│  ├── SubAgent            子代理模式（动态创建）             │
│  └── Workflow            工作流模式（复杂编排）             │
└─────────────────────────────────────────────────────────────┘
```

### 8.1.3 核心概念速查

| 概念 | 说明 | 关键类 |
|------|------|--------|
| **ReActAgent** | AgentScope 的单代理执行引擎，基于 ReAct 范式 | `io.agentscope.core.ReActAgent` |
| **AgentScopeAgent** | 将 ReActAgent 适配为 Spring AI Alibaba Agent 接口 | `com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent` |
| **Toolkit** | 工具注册与管理容器 | `io.agentscope.core.tool.Toolkit` |
| **StateGraph** | 状态图，用于复杂流程编排 | `com.alibaba.cloud.ai.graph.StateGraph` |
| **SequentialAgent** | 顺序执行多个子代理 | `com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent` |
| **ParallelAgent** | 并行执行多个子代理 | `com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent` |
| **LoopAgent** | 循环执行直到满足条件 | `com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent` |
| **RoutingAgent** | 路由代理，根据条件分发任务 | `com.alibaba.cloud.ai.agent.agentscope.flow.AgentScopeRoutingAgent` |

### 8.1.4 快速一览：七种模式对比

| 模式 | 适用场景 | 数据流 | 复杂度 |
|------|---------|--------|--------|
| **Handoffs** | 需要来回切换的对话场景（如客服销售/技术支持切换） | 有向图，节点间转移 | 中等 |
| **Pipeline** | 有固定顺序的处理步骤（如：解析 → 处理 → 输出） | 线性/并行/循环 | 低~高 |
| **Routing** | 分类后分发给不同专家（如：查询 → GitHub/Notion/Slack） | 树状分发 | 中等 |
| **Supervisor** | 需要监督者把关结果（如：主管审批） | 层级调用 | 中等 |
| **SubAgent** | 动态拆分并行任务（如：处理列表中的每个元素） | 动态生成 | 高 |
| **Workflow** | 任意复杂流程编排（如：客服+CRM+工单系统联动） | DAG | 高 |

---

## 8.2 交接（Handoffs）模式

### 8.2.1 模式定义

交接（Handoffs）模式是指在一个 Agent 完成当前任务后，将控制权主动转移给另一个 Agent 的架构模式。两个 Agent 之间不再是简单的线性调用，而是根据对话内容动态决定是否交接，以及交接给谁。

**核心特征**：
- Agent 主动决定何时交接（通过调用 handoff 工具）
- 交接后，原 Agent 终止，新 Agent 接管上下文
- 状态图中维护 `active_agent` 字段作为路由依据
- 支持多轮交接（如：销售 → 支持 → 销售 → 结束）

### 8.2.2 架构原理

```
┌──────────────┐     transfer_to_support      ┌──────────────┐
│ Sales Agent  │ ──────────────────────────►  │ Support Agent│
│ (销售代理)   │   更新 active_agent=         │ (技术支持)  │
│              │   "support_agent"            │              │
└──────────────┘                               └──────────────┘
         ▲                                            │
         │        transfer_to_sales                   │
         │ ◄─────────────────────────────────────────┘
```

状态图的条件边根据 `active_agent` 字段决定下一个节点：

```java
// 初始路由：默认进入销售代理
graph.addConditionalEdges(START,
    new RouteInitialAction(),
    Map.of(SALES_AGENT, SALES_AGENT, SUPPORT_AGENT, SUPPORT_AGENT));

// 销售代理后的路由：交接给支持或结束
graph.addConditionalEdges(SALES_AGENT,
    new RouteAfterSalesAction(),
    Map.of(SUPPORT_AGENT, SUPPORT_AGENT, "__end__", END));
```

### 8.2.3 交接工具实现

交接通过工具实现。AgentScope 的工具通过 `@Tool` 注解注册，自动注入 `ToolContext`，可以访问和更新状态图的状态：

```java
public final class TransferToSupportTool {

    public static final String TOOL_NAME = "transfer_to_support";

    @Tool(
        name = TOOL_NAME,
        description = "转移对话到技术支持代理。当客户询问技术问题、故障排除或账户问题时使用。")
    public String transferToSupport(ToolContext toolContext) {
        // 通过 ToolContextHelper 更新图状态
        ToolContextHelper.getStateForUpdate(toolContext)
            .ifPresent(update -> update.put(ACTIVE_AGENT, SUPPORT_AGENT));
        return "已转接至技术支持代理。";
    }

    public static TransferToSupportTool create() {
        return new TransferToSupportTool();
    }
}
```

### 8.2.4 完整配置示例

```java
@Configuration
public class AgentScopeHandoffsConfig {

    @Bean
    public AgentScopeAgent salesAgent(@Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        Model scopeModel = DashScopeChatModel.builder()
            .apiKey(apiKey)
            .modelName("qwen-plus")
            .build();

        // 注册交接工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(TransferToSupportTool.create());

        ReActAgent.Builder scopeReActBuilder = ReActAgent.builder()
            .name("sales_agent")
            .description("销售代理：处理定价、产品可用性和销售咨询")
            .sysPrompt("你是销售代理。帮助处理销售咨询、定价和产品可用性问题。\n" +
                       "如果客户询问技术问题、故障排除或账户问题，使用 transfer_to_support 交接给支持代理。")
            .model(scopeModel)
            .toolkit(toolkit)
            .memory(new InMemoryMemory());

        return AgentScopeAgent.fromBuilder(scopeReActBuilder)
            .name("sales_agent")
            .description("销售代理：处理定价、产品可用性和销售咨询")
            .instruction("请协助客户的销售咨询：{input}")
            .includeContents(true)
            .returnReasoningContents(true)
            .build();
    }

    @Bean
    public AgentScopeAgent supportAgent(@Value("${spring.ai.dashscope.api-key:}") String apiKey) {
        // 支持代理类似配置，注册 TransferToSalesTool
        // ...
        return agent;
    }

    @Bean
    public CompiledGraph agentScopeHandoffsGraph(
            AgentScopeAgent salesAgent, AgentScopeAgent supportAgent) {

        StateGraph graph = new StateGraph("handoffs", () -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("messages", new AppendStrategy(false));
            strategies.put("active_agent", new ReplaceStrategy());
            return strategies;
        });

        graph.addNode(SALES_AGENT, salesAgent.asNode());
        graph.addNode(SUPPORT_AGENT, supportAgent.asNode());

        graph.addConditionalEdges(START, new RouteInitialAction(),
            Map.of(SALES_AGENT, SALES_AGENT, SUPPORT_AGENT, SUPPORT_AGENT));

        graph.addConditionalEdges(SALES_AGENT, new RouteAfterSalesAction(),
            Map.of(SUPPORT_AGENT, SUPPORT_AGENT, "__end__", END));

        graph.addConditionalEdges(SUPPORT_AGENT, new RouteAfterSupportAction(),
            Map.of(SALES_AGENT, SALES_AGENT, "__end__", END));

        return graph.compile();
    }
}
```

**使用方式**：

```java
@Autowired
private AgentScopeHandoffsService service;

public void handleCustomerQuery(String query) {
    AgentScopeHandoffsResult result = service.run(query);
    result.messages().forEach(msg -> System.out.println(msg.getText()));
}
```

### 8.2.5 使用场景

- **客服系统**：销售问题 → 技术支持 → 再次销售
- **医疗分诊**：全科初诊 → 专科转诊 → 回访
- **企业审批**：员工申请 → 经理审批 → HR 复核

---

## 8.3 流水线（Pipeline）模式

### 8.3.1 模式定义

流水线（Pipeline）模式将一个复杂任务分解为多个固定步骤，按顺序或并行执行。Spring AI Alibaba 提供了三种流水线 Agent：

| 类型 | 类 | 描述 |
|------|---|------|
| **顺序执行** | `SequentialAgent` | 子代理按定义顺序依次执行，上一步输出作为下一步输入 |
| **并行执行** | `ParallelAgent` | 子代理同时执行，最后合并结果 |
| **循环执行** | `LoopAgent` | 循环执行直到满足终止条件（如评分达标、最大迭代次数） |

### 8.3.2 顺序流水线（Sequential Pipeline）

**场景**：自然语言 → SQL 生成 → SQL 评分

```
用户输入 → SQL生成代理 → SQL评分代理 → 结果
     "最近30天订单"    "SELECT..."    0.85
```

**配置示例**：

```java
@Configuration
public class SequentialPipelineConfig {

    @Bean("sequentialSqlAgent")
    public SequentialAgent sequentialSqlAgent(Model dashScopeChatModel) {
        // 步骤1：SQL 生成代理
        ReActAgent.Builder sqlGenBuilder = ReActAgent.builder()
            .name("sql_generator")
            .model(dashScopeChatModel)
            .description("将自然语言转换为 MySQL SQL")
            .sysPrompt("你是 MySQL 数据库专家。根据用户的自然语言请求，输出对应的 SQL 语句。只输出有效的 MySQL SQL，不包含解释。")
            .memory(new InMemoryMemory());

        AgentScopeAgent sqlGenerateAgent = AgentScopeAgent.fromBuilder(sqlGenBuilder)
            .name("sql_generator")
            .description("将自然语言转换为 MySQL SQL")
            .instruction("{input}")
            .includeContents(false)
            .outputKey("sql")  // 输出存入 state["sql"]
            .build();

        // 步骤2：SQL 评分代理
        ReActAgent.Builder sqlRaterBuilder = ReActAgent.builder()
            .name("sql_rater")
            .model(dashScopeChatModel)
            .description("对 SQL 质量评分")
            .sysPrompt("你是 SQL 质量审查员。根据用户的自然语言请求和生成的 SQL，输出 0 到 1 之间的单个浮点分数。分数表示 SQL 与用户意图的匹配程度。只输出数字，示例：0.85")
            .memory(new InMemoryMemory());

        AgentScopeAgent sqlRatingAgent = AgentScopeAgent.fromBuilder(sqlRaterBuilder)
            .name("sql_rater")
            .description("对 SQL 质量评分")
            .instruction("生成的 SQL：\n{sql}。\n\n原始用户请求：\n{input}。")
            .includeContents(false)
            .outputKey("score")  // 输出存入 state["score"]
            .build();

        return SequentialAgent.builder()
            .name("sequential_sql_agent")
            .description("自然语言到 SQL 流水线：生成 SQL 并评估其质量")
            .subAgents(List.of(sqlGenerateAgent, sqlRatingAgent))
            .build();
    }
}
```

**调用服务**：

```java
public class PipelineService {

    public SequentialResult runSequential(String userInput) throws GraphRunnerException {
        Optional<OverAllState> resultOpt = sequentialSqlAgent.invoke(userInput);
        OverAllState state = resultOpt.get();
        String sql = extractText(state.value("sql"));
        String score = extractText(state.value("score"));
        return new SequentialResult(userInput, sql, score);
    }

    public record SequentialResult(String input, String sql, String score) {}
}
```

### 8.3.3 并行流水线（Parallel Pipeline）

**场景**：主题研究，从三个角度同时分析并合并结果

```
输入: "大语言模型现状"
         │
    ┌────┴────┬────────────┐
    ▼         ▼            ▼
 技术研究员  金融研究员   市场研究员
 (并行)     (并行)       (并行)
    │         │            │
    └────┬────┴────────────┘
         ▼
    合并输出 → 研究报告
```

**配置示例**：

```java
@Configuration
public class ParallelPipelineConfig {

    @Bean("parallelResearchAgent")
    public ParallelAgent parallelResearchAgent(Model dashScopeChatModel) {
        // 技术研究员
        ReActAgent.Builder techBuilder = ReActAgent.builder()
            .name("tech_researcher")
            .model(dashScopeChatModel)
            .description("从技术角度研究")
            .sysPrompt("你是技术分析师。从技术角度研究给定主题。提供简洁的 2-3 段分析，涵盖：关键技术、趋势和创新。只关注技术方面。")
            .memory(new InMemoryMemory());

        AgentScopeAgent techResearcher = AgentScopeAgent.fromBuilder(techBuilder)
            .name("tech_researcher")
            .description("从技术角度研究")
            .instruction("研究以下主题：{input}")
            .includeContents(false)
            .outputKey("tech_analysis")
            .build();

        // 金融研究员
        AgentScopeAgent financeResearcher = /* 类似配置 */ ...;

        // 市场研究员
        AgentScopeAgent marketResearcher = /* 类似配置 */ ...;

        return ParallelAgent.builder()
            .name("parallel_research_agent")
            .description("多角度研究：从技术、金融和市场角度并行分析主题")
            .subAgents(List.of(techResearcher, financeResearcher, marketResearcher))
            .mergeStrategy(new ParallelAgent.DefaultMergeStrategy())
            .mergeOutputKey("research_report")
            .maxConcurrency(3)  // 最大并发数
            .build();
    }
}
```

### 8.3.4 循环流水线（Loop Pipeline）

**场景**：迭代改进 SQL，直到评分达到阈值

```
┌─────────────────────────────────────────┐
│           LoopAgent (循环)               │
│  ┌───────────────────────────────────┐  │
│  │  SequentialAgent (顺序)            │  │
│  │  SQL生成 → SQL评分 → 评分>0.5?     │  │
│  │     是 → 结束                      │  │
│  │     否 → 继续生成                   │  │
│  └───────────────────────────────────┘  │
│         最大迭代次数: 5                  │
└─────────────────────────────────────────┘
```

**配置示例**：

```java
@Bean("loopSqlRefinementAgent")
public LoopAgent loopSqlRefinementAgent(Model dashScopeChatModel) {
    // 使用已配置的 sequentialSqlAgent
    SequentialAgent innerAgent = sequentialSqlAgent(dashScopeChatModel);

    return LoopAgent.builder()
        .name("loop_sql_refinement_agent")
        .description("迭代 SQL 改进流水线：生成 SQL 并评分，直到质量分数 > 0.5")
        .maxIterations(5)  // 最大迭代 5 次
        .loopCondition(state -> {
            // 检查分数是否 > 0.5
            Optional<Object> scoreOpt = state.value("score");
            if (scoreOpt.isPresent()) {
                try {
                    double score = Double.parseDouble(scoreOpt.get().toString());
                    return score <= 0.5;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;  // 无分数时继续循环
        })
        .loopBody(innerAgent)
        .build();
}
```

### 8.3.5 使用场景

- **顺序处理**：数据清洗 → 转换 → 入库；翻译 → 校对 → 润色
- **并行调研**：产品评估从技术/财务/市场多角度分析；新闻从多个来源汇总
- **迭代优化**：代码生成 → 测试 → 评分 → 不合格则重生成；文案生成 → A/B 测试评分 → 不达标则重写

---

## 8.4 路由（Routing）模式

### 8.4.1 模式定义

路由（Routing）模式根据用户输入的内容特征，将任务分发给最合适的专业 Agent。不同于固定顺序的流水线，路由是**动态选择**目标 Agent，可能是单个，也可能是多个并行。

**核心组件**：
- **Router Agent**：负责任务分类的 LLM Agent，内置路由决策能力
- **Specialist Agents**：各个专业领域的子代理（GitHub、Notion、Slack 等）
- **可选的合并步骤**：将多个子代理的结果合成最终答案

### 8.4.2 架构原理

```
┌─────────────────────────────────────────────────────────────┐
│                     用户查询                                  │
│                  "如何在 GitHub 上    ←──────────────────────┤
│                   创建 PR 并通知    "                       │
│                   Slack 频道？"                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                   ┌──────────────────┐
                   │  Router Agent    │  (AgentScopeRoutingAgent)
                   │  ┌────────────┐  │
                   │  │ 分类模型   │  │  决策：GitHub + Slack
                   │  └────────────┘  │
                   └──────────────────┘
                         │       │
              ┌──────────┘       └──────────┐
              ▼                             ▼
      ┌──────────────┐              ┌──────────────┐
      │ GitHub Agent  │              │ Slack Agent  │
      │ (并行执行)    │              │ (并行执行)   │
      └──────────────┘              └──────────────┘
              │                             │
              └──────────────┬──────────────┘
                             ▼
                   ┌──────────────────┐
                   │   合并结果       │  (AgentScope Model 合成)
                   │   (可选)         │
                   └──────────────────┘
                             │
                             ▼
                   ┌──────────────────┐
                   │    最终答案      │
                   └──────────────────┘
```

### 8.4.3 路由配置实现

```java
@Configuration
public class RoutingConfig {

    @Bean
    public Model dashScopeChatModel() {
        String key = System.getenv("AI_DASHSCOPE_API_KEY");
        return DashScopeChatModel.builder()
            .apiKey(key)
            .modelName("qwen-plus")
            .build();
    }

    // GitHub 专业代理
    @Bean
    public AgentScopeAgent githubAgent(Model dashScopeChatModel, GitHubStubTools tools) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(tools);

        ReActAgent.Builder builder = ReActAgent.builder()
            .name("github")
            .description("GitHub 专家：代码、issues 和 PRs")
            .sysPrompt("你是 GitHub 专家。通过搜索仓库、issues 和 pull requests 回答关于代码、API 引用和实现细节的问题。")
            .model(dashScopeChatModel)
            .toolkit(toolkit)
            .memory(new InMemoryMemory());

        return AgentScopeAgent.fromBuilder(builder)
            .name("github")
            .description("GitHub 专家：代码、issues 和 PRs")
            .instruction("请响应以下请求：{github_input}")
            .outputKey("github_key")
            .build();
    }

    // Notion 专业代理（类似配置）
    @Bean
    public AgentScopeAgent notionAgent(...) { ... }

    // Slack 专业代理（类似配置）
    @Bean
    public AgentScopeAgent slackAgent(...) { ... }

    // 路由代理：将查询分类并分发
    @Bean
    public AgentScopeRoutingAgent routerAgent(
            Model dashScopeChatModel,
            AgentScopeAgent githubAgent,
            AgentScopeAgent notionAgent,
            AgentScopeAgent slackAgent) {
        return AgentScopeRoutingAgent.builder()
            .name("router")
            .model(dashScopeChatModel)
            .description("根据相关性将查询路由到 GitHub、Notion 和/或 Slack 专家")
            .subAgents(List.of(githubAgent, notionAgent, slackAgent))
            .build();
    }
}
```

### 8.4.4 路由服务实现

```java
public class RouterService {

    private final Model model;
    private final AgentScopeRoutingAgent routerAgent;

    public RouterService(Model model, AgentScopeRoutingAgent routerAgent) {
        this.model = model;
        this.routerAgent = routerAgent;
    }

    /**
     * 执行完整路由流程：分类 + 并行子代理 + 合并结果
     */
    public RouterResult run(String query) throws GraphRunnerException {
        // 1. 路由决策
        Optional<OverAllState> resultOpt = routerAgent.invoke(query);
        OverAllState state = resultOpt.get();

        // 2. 收集分类结果
        List<Classification> classifications = collectClassifications(state);

        // 3. 收集子代理输出
        List<AgentOutput> results = collectAgentOutputs(state);

        // 4. 合并结果（使用 AgentScope Model 进行合成）
        String finalAnswer = synthesize(query, results);

        return new RouterResult(query, classifications, results, finalAnswer);
    }

    /**
     * 使用 AgentScope Model 合成多源结果
     */
    public String synthesize(String query, List<AgentOutput> results) {
        String formatted = results.stream()
            .map(r -> "**来自 " + capitalize(r.source()) + "：**\n" + r.result())
            .reduce((a, b) -> a + "\n\n" + b)
            .orElse("");

        String systemPrompt = "综合这些搜索结果来回答原始问题：\n" +
            "1. 合并多源信息，避免重复\n" +
            "2. 突出最相关和可操作的信息\n" +
            "3. 标注来源间的差异\n" +
            "4. 保持回答简洁有序";

        List<Msg> messages = List.of(
            Msg.of(MsgRole.SYSTEM, systemPrompt.formatted(query)),
            Msg.of(MsgRole.USER, formatted)
        );

        Flux<ChatResponse> stream = model.stream(messages, null, null);
        ChatResponse last = stream.blockLast();
        return extractText(last.getContent());
    }

    public record RouterResult(
            String query,
            List<Classification> classifications,
            List<AgentOutput> results,
            String finalAnswer) {}
}
```

### 8.4.5 使用场景

- **企业知识库**：问题分类后路由到 GitHub/Notion/Slack/Confluence 等知识源
- **客服分流**：技术问题 → 技术支持，产品咨询 → 销售，问题投诉 → 客服经理
- **多语言处理**：根据语言路由到对应语言的专业代理
- **权限控制**：根据用户角色路由到不同功能域的代理

---

## 8.5 监督者（Supervisor）模式

### 8.5.1 模式定义

监督者（Supervisor）模式是指一个 Agent（监督者）负责监督其他 Agent（执行者）的工作，在执行者完成后进行审核、汇总或决策。监督者模式常用于：

- 结果审核：执行者输出需要监督者审核后才能返回
- 汇总决策：多个执行者的结果由监督者综合决策
- 异常处理：监督者判断执行者是否需要重试或升级

### 8.5.2 与 Pipeline 模式的区别

| 特征 | Pipeline | Supervisor |
|------|----------|------------|
| 关系 | 平级串联/并联 | 主从层级 |
| 调用方式 | 自动按顺序/并行 | 监督者显式调用执行者 |
| 决策权 | 固定流程 | 监督者动态决策 |
| 典型场景 | 固定处理流程 | 需要审核把关的场景 |

### 8.5.3 架构示例

```
┌─────────────────────────────────────────┐
│           Supervisor Agent              │
│  (监督者 - 决策何时调用哪个执行者)        │
│  1. 调用执行者完成任务                   │
│  2. 审核输出                             │
│  3. 决定是否需要重试或返回               │
└─────────────────────────────────────────┘
           │           │
    ┌──────┘           └──────┐
    ▼                         ▼
┌────────┐              ┌────────┐
│执行者A │              │执行者B │
└────────┘              └────────┘
```

### 8.5.4 实现方式

监督者模式可以通过 StateGraph 实现，将监督者作为一个 Agent 节点，执行者作为另一个节点：

```java
// 监督者 Agent 节点
graph.addNode("supervisor", supervisorAgent.asNode());

// 执行者 Agent 节点
graph.addNode("worker_a", workerAgentA.asNode());
graph.addNode("worker_b", workerAgentB.asNode());

// 监督者到执行者的条件边
graph.addConditionalEdges("supervisor",
    new SupervisorRoutingAction(),  // 监督者决定调用哪个执行者
    Map.of("worker_a", "worker_a", "worker_b", "worker_b", "__end__", END));

// 执行者完成后回到监督者审核
graph.addEdge("worker_a", "supervisor");
graph.addEdge("worker_b", "supervisor");
```

---

## 8.6 子代理（SubAgent）模式

### 8.6.1 模式定义

子代理（SubAgent）模式是指一个 Agent 在执行过程中动态创建其他 Agent 来处理子任务。与 Pipeline/Routing 的预定义子代理不同，SubAgent 模式是**动态生成**的，常用于处理列表型任务或需要为每个元素分配专门处理逻辑的场景。

### 8.6.2 与 Pipeline 模式的区别

| 特征 | Pipeline | SubAgent |
|------|----------|----------|
| 子代理数量 | 编译时确定 | 运行时动态确定 |
| 输入 | 单一输入 | 列表输入（每个元素一个子代理） |
| 适用场景 | 固定步骤流程 | 批量并行处理 |
| 实现复杂度 | 低 | 高 |

### 8.6.3 架构示例

```
输入: ["订单1", "订单2", "订单3"]
           │
    ┌──────┴──────┬────────────┐
    ▼             ▼            ▼
 子代理1      子代理2      子代理3
 处理订单1   处理订单2   处理订单3
    │             │            │
    └──────┬──────┴────────────┘
           ▼
      合并结果 → 输出
```

### 8.6.4 实现方式

AgentScope Agent 可以通过 `.subAgents()` 方法指定动态子代理：

```java
// 在配置中定义动态子代理工厂
@Bean
public AgentScopeAgent dynamicSubAgentExample(Model model) {
    // 动态创建处理每个元素的子代理
    return AgentScopeAgent.builder()
        .name("dynamic_sub_agent")
        .model(model)
        .subAgents(createPerItemAgents(items, model))
        .build();
}
```

> **注意**：SubAgent 模式的完整实现需要结合 Harness 执行环境，详见第十五章（Harness 安全执行环境）。

---

## 8.7 工作流（Workflow）模式

### 8.7.1 模式定义

工作流（Workflow）模式是多代理架构的终极形态，通过有向无环图（DAG）编排任意复杂的业务流程。状态图（StateGraph）是实现工作流的核心抽象，支持：

- **任意数量的节点**：Agent 节点、功能节点、数据处理节点
- **条件路由**：根据状态动态决定下一个节点
- **状态管理**：跨节点共享数据（messages、状态变量）
- **循环结构**：支持 while/for 循环（通过 LoopAgent 或条件边）

### 8.7.2 StateGraph 核心概念

```
StateGraph 的核心组成：
┌──────────────────────────────────────────┐
│  StateGraph                              │
│  ├── addNode(name, node)                │  添加节点
│  ├── addEdge(source, target)           │  添加固定边
│  ├── addConditionalEdges(...)          │  添加条件边
│  ├── compile() → CompiledGraph          │  编译为可执行图
└──────────────────────────────────────────┘

节点类型：
┌──────────────────────────────────────────┐
│  AgentScopeAgent.asNode()               │  Agent 节点
│  FunctionNode.of(name, function)        │  函数节点
│  ToolNode.of(name, tool)                │  工具节点
└──────────────────────────────────────────┘

状态策略：
┌──────────────────────────────────────────┐
│  AppendStrategy(allowDup)                │  追加策略（用于 messages）
│  ReplaceStrategy()                      │  替换策略（用于 active_agent）
│  ReduceStrategy( reducer)               │  归约策略（用于计数器等）
└──────────────────────────────────────────┘
```

### 8.7.3 完整工作流示例

```java
@Configuration
public class WorkflowConfig {

    @Bean
    public CompiledGraph customerServiceGraph(
            AgentScopeAgent classifier,
            AgentScopeAgent salesAgent,
            AgentScopeAgent supportAgent,
            AgentScopeAgent summarizer) {

        StateGraph graph = new StateGraph(() -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("messages", new AppendStrategy(false));
            strategies.put("category", new ReplaceStrategy());
            strategies.put("final_answer", new ReplaceStrategy());
            return strategies;
        });

        // 分类节点
        graph.addNode("classifier", classifier.asNode());

        // 销售节点
        graph.addNode("sales", salesAgent.asNode());

        // 支持节点
        graph.addNode("support", supportAgent.asNode());

        // 汇总节点
        graph.addNode("summarizer", summarizer.asNode());

        // START → 分类
        graph.addEdge(START, "classifier");

        // 分类后根据类别路由
        graph.addConditionalEdges("classifier",
            (state, config) -> {
                String category = state.value("category")
                    .map(Object::toString)
                    .orElse("unknown");
                return switch (category) {
                    case "sales" -> new Command("sales");
                    case "support" -> new Command("support");
                    default -> new Command(END);
                };
            },
            Map.of("sales", "sales", "support", "support", "__end__", END));

        // 执行完成后汇总
        graph.addEdge("sales", "summarizer");
        graph.addEdge("support", "summarizer");

        // 汇总后结束
        graph.addEdge("summarizer", END);

        return graph.compile();
    }
}
```

### 8.7.4 使用场景

- **复杂业务流程**：客服 + CRM + 工单系统联动
- **多阶段审批**：申请 → 初审 → 复审 → 批准/拒绝
- **研究工作流**：数据收集 → 分析 → 报告生成 → 审阅

---

## 8.8 【案例】多代理协作完成复杂任务：智能客户服务中心

### 8.8.1 案例概述

本案例构建一个**智能客户服务中心**，整合 Pipeline 和 Routing 两种核心模式，实现：

1. **路由层**（Routing）：接收用户查询，智能分类分发到对应专业代理
2. **处理层**（Pipeline）：每个专业代理使用顺序流水线完成处理
3. **汇总层**：最终由监督者代理汇总输出

**系统架构图**：

```
┌─────────────────────────────────────────────────────────────────┐
│                      智能客户服务中心                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   用户: "查询订单状态并更新收货地址"                              │
│          │                                                      │
│          ▼                                                      │
│   ┌─────────────────┐                                          │
│   │  Router Agent   │  ← 路由层：分类 + 并行分发                 │
│   │  (路由决策)     │                                          │
│   └────────┬────────┘                                          │
│            │                                                    │
│     ┌──────┴──────┬──────────────────┐                         │
│     ▼             ▼                  ▼                          │
│ ┌────────┐  ┌────────┐  ┌────────────────┐                     │
│ │订单查询│  │地址更新│  │综合查询        │                     │
│ │ Pipeline│ │ Pipeline│ │  Pipeline      │                     │
│ └────┬───┘  └────┬───┘  └───────┬────────┘                     │
│      │           │               │                              │
│      └──────┬────┴───────┬──────┘                              │
│             ▼            ▼                                      │
│   ┌─────────────────┐                                          │
│   │ Summarizer Agent│  ← 汇总层：综合多源输出                    │
│   └────────┬────────┘                                          │
│            ▼                                                    │
│      最终回复                                                  │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 8.8.2 项目结构

```
D:/Work/ai/agents/agentscope-java/tutorial/
└── chapter08-multi-agent/                    ← 完整可运行的项目
    ├── pom.xml
    └── src/main/
        ├── java/io/agentscope/tutorial/chapter08/
        │   ├── Chapter08Application.java       # Spring Boot 启动类
        │   ├── config/
        │   │   ├── ModelConfig.java            # 模型配置
        │   │   ├── RoutingPipelineConfig.java  # 路由+流水线配置
        │   │   └── WorkflowConfig.java         # 工作流配置
        │   ├── service/
        │   │   ├── CustomerServiceCenter.java  # 服务中心服务类
        │   │   └── SynthesisService.java        # 结果合成服务
        │   ├── agents/
        │   │   ├── OrderQueryPipeline.java     # 订单查询流水线
        │   │   ├── AddressUpdatePipeline.java  # 地址更新流水线
        │   │   └── GeneralQueryPipeline.java   # 综合查询流水线
        │   ├── tools/
        │   │   ├── OrderQueryTool.java         # 订单查询工具（stub）
        │   │   ├── AddressUpdateTool.java      # 地址更新工具（stub）
        │   │   └── GeneralSearchTool.java      # 通用搜索工具（stub）
        │   └── model/
        │       ├── QueryClassification.java    # 查询分类结果
        │       └── ServiceResult.java           # 服务返回结果
        └── resources/
            └── application.yml                 # 应用配置
```

### 8.8.3 完整代码

#### 8.8.3.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ 第八章：多代理架构模式 - 智能客户服务中心案例
  ~
  ~ 本项目演示 Pipeline + Routing 模式的多代理协作
  ~
  ~ 环境要求：
  ~   - JDK 21+
  ~   - Maven 3.8+
  ~   - DashScope API Key
  -->
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.agentscope</groupId>
    <artifactId>chapter08-multi-agent</artifactId>
    <version>1.0.0</version>
    <name>Chapter 08 - Multi-Agent Architecture Patterns</name>
    <description>智能客户服务中心 - Pipeline + Routing 多代理协作案例</description>

    <properties>
        <java.version>21</java.version>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.boot.version>3.3.0</spring.boot.version>
        <spring-ai-alibaba.version>1.1.2.2</spring-ai-alibaba.version>
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
            <dependency>
                <groupId>com.alibaba.cloud.ai</groupId>
                <artifactId>spring-ai-alibaba-bom</artifactId>
                <version>${spring-ai-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot 基础 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>

        <!-- Spring AI Alibaba AgentScope 集成 -->
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-starter-agentscope</artifactId>
        </dependency>

        <!-- Web 支持（可选，用于暴露 REST API） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 测试支持 -->
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
            </plugin>
        </plugins>
    </build>

    <repositories>
        <repository>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
            <id>spring-milestones</id>
            <name>Spring Milestones</name>
            <url>https://repo.spring.io/milestone</url>
        </repository>
        <repository>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
            <id>sonatype-snapshots</id>
            <name>Sonatype Snapshot Repository</name>
            <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        </repository>
    </repositories>
</project>
```

#### 8.8.3.2 application.yml

```yaml
# 第八章案例：智能客户服务中心配置
spring:
  application:
    name: chapter08-multi-agent

  # DashScope 模型配置
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY:your-api-key-here}
      model: qwen-plus

# 服务端口
server:
  port: 8088

# 是否在启动时运行演示（设置为 true 方便测试）
customer-service:
  demo:
    enabled: true

# 日志级别
logging:
  level:
    io.agentscope: DEBUG
    com.alibaba.cloud.ai: DEBUG
```

#### 8.8.3.3 启动类 Chapter08Application.java

```java
package io.agentscope.tutorial.chapter08;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第八章：多代理架构模式 - Spring Boot 启动类
 *
 * 本案例演示智能客户服务中心，整合 Pipeline 和 Routing 模式：
 * - 路由层：接收查询，智能分类分发到专业代理
 * - 处理层：每个专业代理使用顺序流水线完成处理
 * - 汇总层：监督者代理汇总多源输出
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter08Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter08Application.class, args);
    }
}
```

#### 8.8.3.4 模型配置 ModelConfig.java

```java
package io.agentscope.tutorial.chapter08.config;

import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 模型配置
 *
 * 配置 AgentScope DashScope 模型，所有 Agent 共享同一个模型实例。
 * 在生产环境中，可以为不同 Agent 配置不同的模型（如 qwen-max 用于复杂任务，
 * qwen-turbo 用于简单查询）以优化成本和性能。
 */
@Configuration
public class ModelConfig {

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    /**
     * 主模型 Bean：用于所有 Agent
     * 使用 qwen-plus 模型，平衡能力与成本
     */
    @Bean
    public Model primaryModel() {
        String key = StringUtils.hasText(apiKey)
            ? apiKey
            : System.getenv("AI_DASHSCOPE_API_KEY");

        return DashScopeChatModel.builder()
            .apiKey(key)
            .modelName("qwen-plus")
            .build();
    }
}
```

#### 8.8.3.5 查询分类模型 QueryClassification.java

```java
package io.agentscope.tutorial.chapter08.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 查询分类结果
 *
 * 记录路由代理的分类决策：哪个类型的查询应该由哪个专业代理处理
 */
public record QueryClassification(
    @JsonProperty("category") String category,
    @JsonProperty("reason") String reason
) {

    @JsonCreator
    public QueryClassification {}

    /** 查询类型常量 */
    public static final String ORDER_QUERY = "order_query";
    public static final String ADDRESS_UPDATE = "address_update";
    public static final String GENERAL_QUERY = "general_query";
    public static final String UNKNOWN = "unknown";

    /** 查询类型对应的 Pipeline 节点名称 */
    public String toPipelineNode() {
        return switch (category) {
            case ORDER_QUERY -> "order_query_pipeline";
            case ADDRESS_UPDATE -> "address_update_pipeline";
            case GENERAL_QUERY -> "general_query_pipeline";
            default -> "unknown";
        };
    }
}
```

#### 8.8.3.6 服务结果模型 ServiceResult.java

```java
package io.agentscope.tutorial.chapter08.model;

import java.util.List;
import java.util.Map;

/**
 * 服务中心返回结果
 *
 * 包含完整的处理结果：原始查询、分类决策、各 Pipeline 输出和最终汇总
 */
public record ServiceResult(
    String originalQuery,           // 原始查询
    QueryClassification classify,   // 分类结果
    Map<String, String> pipelineOutputs,  // 各 Pipeline 的输出
    String finalAnswer,            // 最终汇总答案
    List<String> agentsInvoked    // 被调用的 Agent 列表
) {

    /**
     * 便捷构造函数
     */
    public static ServiceResult of(
            String query,
            QueryClassification classify,
            String finalAnswer) {
        return new ServiceResult(
            query,
            classify,
            Map.of(),
            finalAnswer,
            List.of(classify.category())
        );
    }

    /**
     * 添加 Pipeline 输出
     */
    public ServiceResult withPipelineOutput(String pipeline, String output) {
        return new ServiceResult(
            originalQuery,
            classify,
            Map.of(pipeline, output),
            finalAnswer,
            agentsInvoked
        );
    }
}
```

#### 8.8.3.7 工具类：订单查询工具 OrderQueryTool.java

```java
package io.agentscope.tutorial.chapter08.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 订单查询工具（模拟实现）
 *
 * 在实际生产环境中，此工具会调用真实的订单服务 API。
 * 这里使用模拟数据演示工具调用的工作原理。
 */
public class OrderQueryTool {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryTool.class);

    private OrderQueryTool() {}

    /**
     * 根据订单号查询订单状态
     *
     * @param orderId 订单号
     * @return 订单状态信息（模拟数据）
     */
    @Tool(
        name = "query_order_status",
        description = "根据订单号查询订单状态和物流信息。" +
                      "适用于用户询问"我的订单到哪了"或"订单什么时候发货"等问题。"
    )
    public static String queryOrderStatus(
            @ToolParam(name = "order_id", description = "订单号，例如：ORD20240101001")
            String orderId) {

        log.info("查询订单状态，订单号：{}", orderId);

        // 模拟返回订单状态
        return """
            订单信息：
            - 订单号：%s
            - 订单状态：运输中
            - 下单时间：2024-01-15 10:30:00
            - 预计送达：2024-01-20 18:00:00
            - 物流公司：顺丰速运
            - 物流单号：SF1234567890
            - 当前物流状态：
              1. [2024-01-15 10:30] 订单已发货
              2. [2024-01-16 08:00] 快件到达【杭州中转场】
              3. [2024-01-16 14:00] 快件离开【杭州中转场】，发往【上海中转场】
              4. [2024-01-17 09:00] 快件到达【上海中转场】
            """.formatted(orderId);
    }

    /**
     * 根据时间范围查询订单列表
     *
     * @param startDate 开始日期（YYYY-MM-DD）
     * @param endDate 结束日期（YYYY-MM-DD）
     * @return 订单列表摘要
     */
    @Tool(
        name = "query_order_list",
        description = "根据时间范围查询用户的订单列表。" +
                      "适用于用户询问"最近有哪些订单"或"这个月买了什么"等问题。"
    )
    public static String queryOrderList(
            @ToolParam(name = "start_date", description = "开始日期，格式：YYYY-MM-DD")
            String startDate,
            @ToolParam(name = "end_date", description = "结束日期，格式：YYYY-MM-DD")
            String endDate) {

        log.info("查询订单列表，时间范围：{} 至 {}", startDate, endDate);

        // 模拟返回订单列表
        return """
            订单列表（%s 至 %s）：
            1. ORD20240115001 - ¥299.00 - 运输中 - 2024-01-15
            2. ORD20240110002 - ¥59.00 - 已完成 - 2024-01-10
            3. ORD20240105003 - ¥1299.00 - 已完成 - 2024-01-05
            4. ORD20240101004 - ¥89.00 - 已完成 - 2024-01-01
            合计：4 个订单，总金额：¥1746.00
            """.formatted(startDate, endDate);
    }
}
```

#### 8.8.3.8 工具类：地址更新工具 AddressUpdateTool.java

```java
package io.agentscope.tutorial.chapter08.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 地址更新工具（模拟实现）
 *
 * 在实际生产环境中，此工具会调用真实的用户地址管理 API。
 */
public class AddressUpdateTool {

    private static final Logger log = LoggerFactory.getLogger(AddressUpdateTool.class);

    private AddressUpdateTool() {}

    /**
     * 查询当前收货地址
     *
     * @param userId 用户 ID
     * @return 当前收货地址信息
     */
    @Tool(
        name = "get_shipping_address",
        description = "获取用户当前的默认收货地址。" +
                      "在需要更新地址前，应先查询当前地址信息。"
    )
    public static String getShippingAddress(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询用户收货地址，用户ID：{}", userId);

        return """
            当前收货地址：
            收货人：张三
            手机号：138****5678
            省市区：浙江省杭州市西湖区
            详细地址：文三路 123 号 XX 小区 1 栋 1001 室
            邮编：310000
            """;
    }

    /**
     * 更新收货地址
     *
     * @param userId 用户 ID
     * @param province 省份
     * @param city 城市
     * @param district 区/县
     * @param detailAddress 详细地址
     * @param recipient 收货人姓名
     * @param phone 手机号
     * @return 更新结果
     */
    @Tool(
        name = "update_shipping_address",
        description = "更新用户的收货地址。" +
                      "需要提供完整的地址信息，包括省市区和详细地址。"
    )
    public static String updateShippingAddress(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId,
            @ToolParam(name = "province", description = "省份，如：浙江省")
            String province,
            @ToolParam(name = "city", description = "城市，如：杭州市")
            String city,
            @ToolParam(name = "district", description = "区/县，如：西湖区")
            String district,
            @ToolParam(name = "detail_address", description = "详细地址，包含街道、小区、门牌号")
            String detailAddress,
            @ToolParam(name = "recipient", description = "收货人姓名")
            String recipient,
            @ToolParam(name = "phone", description = "手机号")
            String phone) {

        log.info("更新用户收货地址，用户ID：{}，新地址：{} {} {} {}",
            userId, province, city, district, detailAddress);

        return """
            地址更新成功！
            新收货地址：
            收货人：%s
            手机号：%s
            省市区：%s %s %s
            详细地址：%s
            更新时间：2024-01-18 15:30:00
            """.formatted(recipient, phone, province, city, district, detailAddress);
    }
}
```

#### 8.8.3.9 工具类：通用搜索工具 GeneralSearchTool.java

```java
package io.agentscope.tutorial.chapter08.tools;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通用搜索工具（模拟实现）
 *
 * 提供产品查询、优惠查询等通用功能
 */
public class GeneralSearchTool {

    private static final Logger log = LoggerFactory.getLogger(GeneralSearchTool.class);

    private GeneralSearchTool() {}

    /**
     * 搜索产品
     *
     * @param keyword 搜索关键词
     * @return 产品列表
     */
    @Tool(
        name = "search_products",
        description = "搜索商品或产品信息。" +
                      "适用于用户询问"有什么推荐的产品"或"XX产品多少钱"等问题。"
    )
    public static String searchProducts(
            @ToolParam(name = "keyword", description = "搜索关键词")
            String keyword) {

        log.info("搜索产品，关键词：{}", keyword);

        return """
            搜索结果（关键词：%s）：
            1. 【热销】智能蓝牙耳机 Pro - ¥299.00 - 好评率 98%%
            2. 【新品】无线充电器 15W - ¥89.00 - 好评率 95%%
            3. 【特惠】手机保护套 适用 iPhone 15 - ¥39.00 - 好评率 92%%
            """.formatted(keyword);
    }

    /**
     * 查询优惠政策
     *
     * @param userId 用户 ID
     * @return 可用的优惠政策
     */
    @Tool(
        name = "query_promotions",
        description = "查询当前可用的优惠活动。" +
                      "适用于用户询问"有什么优惠"或"能打折吗"等问题。"
    )
    public static String queryPromotions(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询优惠活动，用户ID：{}", userId);

        return """
            当前可用的优惠活动：
            1. 新用户首单立减 ¥50（限未下单新用户）
            2. 满 ¥200 减 ¥30（有效期至 2024-01-31）
            3. 会员日 9 折券（每月 18 日会员日）
            4. 积分兑换抵现（100 积分 = ¥1）
            5. 【限时】冬季保暖用品专区 8 折起
            """;
    }

    /**
     * 账户信息查询
     *
     * @param userId 用户 ID
     * @return 账户相关信息
     */
    @Tool(
        name = "query_account_info",
        description = "查询用户的账户信息，包括积分、优惠券、会员等级等。" +
                      "适用于用户询问"我的积分是多少"或"我有多少优惠券"等问题。"
    )
    public static String queryAccountInfo(
            @ToolParam(name = "user_id", description = "用户 ID")
            String userId) {

        log.info("查询账户信息，用户ID：{}", userId);

        return """
            账户信息：
            会员等级：黄金会员
            当前积分：2,580 积分
            可用优惠券：3 张
              - ¥20 优惠券（满 ¥200 可用，有效期至 2024-01-31）
              - ¥10 优惠券（满 ¥100 可用，有效期至 2024-02-15）
              - 9 折会员日券（每月 18 日可用）
            """;
    }
}
```

#### 8.8.3.10 订单查询流水线 OrderQueryPipeline.java

```java
package io.agentscope.tutorial.chapter08.agents;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter08.tools.OrderQueryTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单查询流水线（Pipeline 模式）
 *
 * 包含两个顺序执行的子代理：
 * 1. 订单信息提取器：从用户查询中提取订单号或时间范围
 * 2. 订单状态查询器：使用工具查询订单详情并格式化输出
 *
 * 场景示例：
 * - 用户："我的订单 ORD20240115001 到哪里了？"
 * - 第一步：提取订单号 "ORD20240115001"
 * - 第二步：查询并格式化订单状态
 */
@Component
public class OrderQueryPipeline {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryPipeline.class);

    private final SequentialAgent agent;

    public OrderQueryPipeline(Model primaryModel) {
        this.agent = createPipeline(primaryModel);
        log.info("订单查询流水线初始化完成");
    }

    private SequentialAgent createPipeline(Model model) {
        // === 子代理 1：订单信息提取器 ===
        ReActAgent.Builder extractorBuilder = ReActAgent.builder()
            .name("order_extractor")
            .model(model)
            .description("从用户查询中提取订单相关信息")
            .sysPrompt("""
                你是一个订单信息提取专家。
                从用户的自然语言查询中，提取以下信息之一：
                1. 订单号（格式如 ORD20240115001）
                2. 时间范围（下单时间，如最近一周、1月份）

                如果用户没有提供具体信息，你需要礼貌地询问用户提供：
                - 如果询问订单状态，请用户提供订单号
                - 如果查询历史订单，请用户提供大概的时间范围

                输出格式：
                - 提取到信息时：{"type": "order_id", "value": "具体订单号"}
                - 提取时间范围时：{"type": "date_range", "start": "开始日期", "end": "结束日期"}
                - 需要询问时：{"type": "need_input", "question": "需要询问的问题"}
                """)
            .memory(new InMemoryMemory());

        AgentScopeAgent extractor = AgentScopeAgent.fromBuilder(extractorBuilder)
            .name("order_extractor")
            .description("从用户查询中提取订单信息")
            .instruction("{input}")
            .includeContents(false)
            .outputKey("extracted_info")
            .build();

        // === 子代理 2：订单状态查询器 ===
        Toolkit queryToolkit = new Toolkit();
        queryToolkit.registerTool(OrderQueryTool::queryOrderStatus);
        queryToolkit.registerTool(OrderQueryTool::queryOrderList);

        ReActAgent.Builder queryBuilder = ReActAgent.builder()
            .name("order_query_executor")
            .model(model)
            .description("查询订单状态并格式化输出")
            .sysPrompt("""
                你是一个订单状态查询专家。
                根据上一步提取的订单信息，使用工具查询订单详情并格式化输出。

                查询规则：
                1. 如果提取到订单号，使用 query_order_status 查询该订单的状态
                2. 如果提取到时间范围，使用 query_order_list 查询该时间段的订单列表
                3. 查询结果需要用友好的方式呈现给用户

                注意：
                - 如果工具调用失败，需要告知用户并给出建议
                - 对于正在运输中的订单，需要提醒预计送达时间
                """)
            .model(model)
            .toolkit(queryToolkit)
            .memory(new InMemoryMemory());

        AgentScopeAgent queryExecutor = AgentScopeAgent.fromBuilder(queryBuilder)
            .name("order_query_executor")
            .description("查询订单状态并格式化输出")
            .instruction("根据以下信息查询订单：\n{extracted_info}\n\n用户原始查询：\n{input}")
            .includeContents(false)
            .outputKey("order_result")
            .build();

        // === 构建顺序流水线 ===
        return SequentialAgent.builder()
            .name("order_query_pipeline")
            .description("订单查询流水线：提取订单信息 → 查询订单状态")
            .subAgents(List.of(extractor, queryExecutor))
            .build();
    }

    /**
     * 执行订单查询流水线
     *
     * @param userQuery 用户查询
     * @return 流水线输出结果
     */
    public String invoke(String userQuery) {
        log.info("执行订单查询流水线，输入：{}", userQuery);
        try {
            var result = agent.invoke(userQuery);
            if (result.isPresent()) {
                var state = result.get();
                var orderResult = state.value("order_result");
                log.info("订单查询流水线执行完成");
                return orderResult.map(Object::toString).orElse("未获取到订单信息");
            }
        } catch (Exception e) {
            log.error("订单查询流水线执行失败", e);
        }
        return "抱歉，订单查询服务暂时不可用，请稍后重试。";
    }

    /**
     * 获取流水线 Agent（用于 StateGraph 节点）
     */
    public SequentialAgent getAgent() {
        return agent;
    }
}
```

#### 8.8.3.11 地址更新流水线 AddressUpdatePipeline.java

```java
package io.agentscope.tutorial.chapter08.agents;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter08.tools.AddressUpdateTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 地址更新流水线（Pipeline 模式）
 *
 * 包含两个顺序执行的子代理：
 * 1. 地址信息提取器：从用户查询中提取新地址信息
 * 2. 地址更新执行器：使用工具更新地址并确认
 *
 * 场景示例：
 * - 用户："帮我把收货地址改成上海市浦东新区 XX 路 XX 号"
 * - 第一步：提取地址信息（上海市、浦东新区、XX路XX号）
 * - 第二步：调用工具更新地址并返回确认
 */
@Component
public class AddressUpdatePipeline {

    private static final Logger log = LoggerFactory.getLogger(AddressUpdatePipeline.class);

    private final SequentialAgent agent;

    public AddressUpdatePipeline(Model primaryModel) {
        this.agent = createPipeline(primaryModel);
        log.info("地址更新流水线初始化完成");
    }

    private SequentialAgent createPipeline(Model model) {
        // === 子代理 1：地址信息提取器 ===
        ReActAgent.Builder extractorBuilder = ReActAgent.builder()
            .name("address_extractor")
            .model(model)
            .description("从用户查询中提取地址信息")
            .sysPrompt("""
                你是一个地址信息提取专家。
                从用户的自然语言中提取完整的收货地址信息。

                需要提取的字段：
                - 省份：如 浙江省、北京市
                - 城市：如 杭州市、上海市
                - 区/县：如 西湖区、浦东新区
                - 详细地址：街道、小区、门牌号等
                - 收货人姓名
                - 手机号

                处理规则：
                1. 如果用户提供不完整地址，需要礼貌地询问缺失信息
                2. 手机号需要完整提供（包括区号）
                3. 详细地址需要包含足够的定位信息

                输出格式：
                - 信息完整：{"status": "complete", "address": {...}}
                - 需要补充：{"status": "incomplete", "missing": ["需要补充的字段"]}
                """)
            .memory(new InMemoryMemory());

        AgentScopeAgent extractor = AgentScopeAgent.fromBuilder(extractorBuilder)
            .name("address_extractor")
            .description("从用户查询中提取地址信息")
            .instruction("{input}")
            .includeContents(false)
            .outputKey("extracted_address")
            .build();

        // === 子代理 2：地址更新执行器 ===
        Toolkit addressToolkit = new Toolkit();
        addressToolkit.registerTool(AddressUpdateTool::getShippingAddress);
        addressToolkit.registerTool(AddressUpdateTool::updateShippingAddress);

        ReActAgent.Builder updaterBuilder = ReActAgent.builder()
            .name("address_updater")
            .model(model)
            .description("更新收货地址")
            .sysPrompt("""
                你是一个地址更新执行专家。
                根据上一步提取的地址信息，使用工具更新用户的收货地址。

                更新规则：
                1. 首先调用 get_shipping_address 获取当前地址（用户ID使用固定值 "user_001"）
                2. 然后调用 update_shipping_address 更新为新地址
                3. 更新完成后，用友好的方式告知用户更新结果

                用户ID固定为：user_001

                注意：
                - 更新操作需要谨慎，确认地址信息完整后再更新
                - 返回给用户的确认信息需要包含完整的地址信息
                """)
            .model(model)
            .toolkit(addressToolkit)
            .memory(new InMemoryMemory());

        AgentScopeAgent updater = AgentScopeAgent.fromBuilder(updaterBuilder)
            .name("address_updater")
            .description("更新收货地址")
            .instruction("根据以下提取的地址信息更新收货地址：\n{extracted_address}\n\n用户原始请求：\n{input}")
            .includeContents(false)
            .outputKey("update_result")
            .build();

        // === 构建顺序流水线 ===
        return SequentialAgent.builder()
            .name("address_update_pipeline")
            .description("地址更新流水线：提取地址信息 → 更新收货地址")
            .subAgents(List.of(extractor, updater))
            .build();
    }

    /**
     * 执行地址更新流水线
     */
    public String invoke(String userQuery) {
        log.info("执行地址更新流水线，输入：{}", userQuery);
        try {
            var result = agent.invoke(userQuery);
            if (result.isPresent()) {
                var state = result.get();
                var updateResult = state.value("update_result");
                log.info("地址更新流水线执行完成");
                return updateResult.map(Object::toString).orElse("地址更新服务暂时不可用");
            }
        } catch (Exception e) {
            log.error("地址更新流水线执行失败", e);
        }
        return "抱歉，地址更新服务暂时不可用，请稍后重试。";
    }

    /**
     * 获取流水线 Agent
     */
    public SequentialAgent getAgent() {
        return agent;
    }
}
```

#### 8.8.3.12 综合查询流水线 GeneralQueryPipeline.java

```java
package io.agentscope.tutorial.chapter08.agents;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter08.tools.GeneralSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 综合查询流水线（Pipeline 模式）
 *
 * 包含两个顺序执行的子代理：
 * 1. 综合信息提取器：确定用户查询属于哪类（产品/优惠/账户）
 * 2. 综合信息查询器：调用对应工具获取信息
 *
 * 场景示例：
 * - 用户："我有多少积分？能用来抵现吗？"
 * - 第一步：分类为"账户信息查询"
 * - 第二步：查询积分和优惠政策
 */
@Component
public class GeneralQueryPipeline {

    private static final Logger log = LoggerFactory.getLogger(GeneralQueryPipeline.class);

    private final SequentialAgent agent;

    public GeneralQueryPipeline(Model primaryModel) {
        this.agent = createPipeline(primaryModel);
        log.info("综合查询流水线初始化完成");
    }

    private SequentialAgent createPipeline(Model model) {
        // === 子代理 1：查询类型分类器 ===
        ReActAgent.Builder classifierBuilder = ReActAgent.builder()
            .name("query_classifier")
            .model(model)
            .description("将用户查询分类到对应的服务")
            .sysPrompt("""
                你是一个查询分类专家。
                分析用户的自然语言查询，确定需要查询哪类信息：

                分类类别：
                - product_search：产品搜索，如"有什么耳机推荐"、"XX 多少钱"
                - promotion_query：优惠查询，如"有什么优惠"、"能打折吗"
                - account_info：账户信息，如"我的积分"、"我的优惠券"、"会员等级"

                输出格式：
                {"category": "分类类别", "keyword": "搜索关键词（如有）"}
                """)
            .memory(new InMemoryMemory());

        AgentScopeAgent classifier = AgentScopeAgent.fromBuilder(classifierBuilder)
            .name("query_classifier")
            .description("将用户查询分类")
            .instruction("{input}")
            .includeContents(false)
            .outputKey("query_category")
            .build();

        // === 子代理 2：综合信息查询器 ===
        Toolkit searchToolkit = new Toolkit();
        searchToolkit.registerTool(GeneralSearchTool::searchProducts);
        searchToolkit.registerTool(GeneralSearchTool::queryPromotions);
        searchToolkit.registerTool(GeneralSearchTool::queryAccountInfo);

        ReActAgent.Builder searchBuilder = ReActAgent.builder()
            .name("general_search_executor")
            .model(model)
            .description("执行综合信息查询")
            .sysPrompt("""
                你是一个综合信息查询专家。
                根据上一步的分类结果，使用相应工具查询信息并格式化输出。

                查询规则：
                1. 如果分类为 product_search：使用 search_products 搜索产品
                2. 如果分类为 promotion_query：使用 query_promotions 查询优惠（用户ID用 "user_001"）
                3. 如果分类为 account_info：使用 query_account_info 查询账户信息（用户ID用 "user_001"）

                用户ID固定为：user_001

                输出格式：
                用友好的方式呈现查询结果，包括：
                - 明确的标题
                - 详细的信息列表
                - 相关的操作建议（如有）
                """)
            .model(model)
            .toolkit(searchToolkit)
            .memory(new InMemoryMemory());

        AgentScopeAgent searchExecutor = AgentScopeAgent.fromBuilder(searchBuilder)
            .name("general_search_executor")
            .description("执行综合信息查询")
            .instruction("查询类别：{query_category}\n\n用户原始查询：\n{input}")
            .includeContents(false)
            .outputKey("search_result")
            .build();

        // === 构建顺序流水线 ===
        return SequentialAgent.builder()
            .name("general_query_pipeline")
            .description("综合查询流水线：分类查询类型 → 执行查询")
            .subAgents(List.of(classifier, searchExecutor))
            .build();
    }

    /**
     * 执行综合查询流水线
     */
    public String invoke(String userQuery) {
        log.info("执行综合查询流水线，输入：{}", userQuery);
        try {
            var result = agent.invoke(userQuery);
            if (result.isPresent()) {
                var state = result.get();
                var searchResult = state.value("search_result");
                log.info("综合查询流水线执行完成");
                return searchResult.map(Object::toString).orElse("未获取到查询结果");
            }
        } catch (Exception e) {
            log.error("综合查询流水线执行失败", e);
        }
        return "抱歉，综合查询服务暂时不可用，请稍后重试。";
    }

    /**
     * 获取流水线 Agent
     */
    public SequentialAgent getAgent() {
        return agent;
    }
}
```

#### 8.8.3.13 结果合成服务 SynthesisService.java

```java
package io.agentscope.tutorial.chapter08.service;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 结果合成服务
 *
 * 当多个 Pipeline 并行执行后，需要将结果合成为最终答案。
 * 本服务使用 AgentScope Model 作为 LLM 进行结果合成。
 */
@Service
public class SynthesisService {

    private static final Logger log = LoggerFactory.getLogger(SynthesisService.class);

    private final Model model;

    public SynthesisService(Model model) {
        this.model = model;
    }

    /**
     * 合成单个 Pipeline 的输出
     * （简化场景：只有一个 Pipeline 被调用）
     */
    public String synthesizeSingle(String originalQuery, String pipelineName, String output) {
        String prompt = """
            作为智能客户服务中心，请将以下处理结果以友好的方式呈现给用户。

            用户原始查询：%s

            处理服务：%s

            处理结果：
            %s

            请用简洁、有条理的方式回复用户，突出关键信息，并询问是否需要其他帮助。
            """.formatted(originalQuery, getServiceName(pipelineName), output);

        return callModel(prompt);
    }

    /**
     * 合成多个 Pipeline 的输出
     * （场景：用户查询涉及多个方面的服务）
     */
    public String synthesizeMultiple(String originalQuery, Map<String, String> outputs) {
        StringBuilder outputSummary = new StringBuilder();
        outputs.forEach((name, output) -> {
            outputSummary.append("【").append(getServiceName(name)).append("】\n")
                         .append(output).append("\n\n");
        });

        String prompt = """
            作为智能客户服务中心，请将以下多个服务的处理结果整合后呈现给用户。

            用户原始查询：%s

            各服务处理结果：
            %s

            请：
            1. 整合相关信息，避免重复
            2. 按逻辑顺序组织内容
            3. 突出最重要和用户最关注的信息
            4. 询问用户是否需要进一步帮助
            """.formatted(originalQuery, outputSummary.toString());

        return callModel(prompt);
    }

    /**
     * 生成最终回复
     * 用于监督者模式下的最终汇总
     */
    public String generateFinalResponse(String query, String response, String agentName) {
        String prompt = """
            作为智能客户服务中心，请评估以下回复质量，并生成最终输出。

            用户查询：%s

            原始回复：%s

            处理代理：%s

            如果回复质量良好，直接返回该回复（不加额外说明）。
            如果回复需要补充或优化，请补充后返回。
            """.formatted(query, response, agentName);

        return callModel(prompt);
    }

    /**
     * 调用 AgentScope Model
     */
    private String callModel(String prompt) {
        List<Msg> messages = List.of(
            Msg.of(MsgRole.USER, prompt)
        );

        try {
            Flux<ChatResponse> stream = model.stream(messages, null, null);
            ChatResponse last = stream.blockLast();
            if (last != null && last.getContent() != null) {
                StringBuilder text = new StringBuilder();
                for (var block : last.getContent()) {
                    if (block instanceof TextBlock tb) {
                        text.append(tb.getText());
                    }
                }
                return text.toString();
            }
        } catch (Exception e) {
            log.error("调用模型失败", e);
        }
        return "服务暂时不可用，请稍后重试。";
    }

    /**
     * 获取服务中文名称
     */
    private String getServiceName(String pipelineName) {
        return switch (pipelineName) {
            case "order_query_pipeline" -> "订单查询服务";
            case "address_update_pipeline" -> "地址更新服务";
            case "general_query_pipeline" -> "综合查询服务";
            default -> pipelineName;
        };
    }
}
```

#### 8.8.3.14 路由配置 RoutingPipelineConfig.java

```java
package io.agentscope.tutorial.chapter08.config;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.agent.agentscope.flow.AgentScopeRoutingAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter08.agents.AddressUpdatePipeline;
import io.agentscope.tutorial.chapter08.agents.GeneralQueryPipeline;
import io.agentscope.tutorial.chapter08.agents.OrderQueryPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 路由 + 流水线配置
 *
 * 本配置展示了 Routing + Pipeline 模式的完整结合：
 * 1. RoutingAgent 负责任务分类
 * 2. 根据分类结果，调用对应的 Pipeline
 * 3. 多个 Pipeline 可以并行执行
 *
 * 流程示意：
 * 用户查询 → RouterAgent（分类）→ [OrderQueryPipeline | AddressUpdatePipeline | GeneralQueryPipeline]
 *                                          ↓
 *                                    并行执行（可选）
 *                                          ↓
 *                                    汇总结果
 */
@Configuration
public class RoutingPipelineConfig {

    private static final Logger log = LoggerFactory.getLogger(RoutingPipelineConfig.class);

    // ==================== 路由 Agent ====================

    /**
     * 路由器 Agent
     *
     * 负责任务分类，决定将查询分发到哪个 Pipeline
     */
    @Bean
    public AgentScopeRoutingAgent routerAgent(
            Model primaryModel,
            // 注入各个 Pipeline 的 Agent
            SequentialAgent orderQueryPipeline,
            SequentialAgent addressUpdatePipeline,
            SequentialAgent generalQueryPipeline) {

        log.info("初始化路由 Agent");

        return AgentScopeRoutingAgent.builder()
            .name("customer_service_router")
            .model(primaryModel)
            .description("智能客户服务中心路由：分类用户查询并分发到对应专业流水线")
            .subAgents(List.of(
                AgentScopeAgent.fromBuilder(
                        createPipelineAgentBuilder(
                            "order_query", primaryModel, orderQueryPipeline))
                    .name("order_query")
                    .description("订单查询流水线：查询订单状态和物流信息")
                    .outputKey("order_query_key")
                    .build(),

                AgentScopeAgent.fromBuilder(
                        createPipelineAgentBuilder(
                            "address_update", primaryModel, addressUpdatePipeline))
                    .name("address_update")
                    .description("地址更新流水线：更新用户收货地址")
                    .outputKey("address_update_key")
                    .build(),

                AgentScopeAgent.fromBuilder(
                        createPipelineAgentBuilder(
                            "general_query", primaryModel, generalQueryPipeline))
                    .name("general_query")
                    .description("综合查询流水线：产品搜索、优惠查询、账户信息")
                    .outputKey("general_query_key")
                    .build()
            ))
            .build();
    }

    /**
     * 创建 Pipeline Agent 的 Builder
     *
     * 这是一个包装方法，将 SequentialAgent 包装为 ReActAgent.Builder
     */
    private ReActAgent.Builder createPipelineAgentBuilder(
            String name, Model model, SequentialAgent pipeline) {

        String instruction = switch (name) {
            case "order_query" -> "请处理以下订单查询请求：{order_query_input}";
            case "address_update" -> "请处理以下地址更新请求：{address_update_input}";
            case "general_query" -> "请处理以下综合查询请求：{general_query_input}";
            default -> "请处理以下请求：{input}";
        };

        return ReActAgent.builder()
            .name(name)
            .model(model)
            .description("Pipeline 包装代理")
            .sysPrompt("""
                你是一个专业服务执行代理。
                接收用户的服务请求，调用相应的处理流水线完成任务。
                执行完成后，用简洁的语言总结处理结果。
                """)
            .instruction(instruction)
            .memory(new InMemoryMemory());
    }

    // ==================== Pipeline 节点 Bean ====================

    @Bean("orderQueryPipelineAgent")
    public SequentialAgent orderQueryPipelineAgent(Model primaryModel) {
        OrderQueryPipeline pipeline = new OrderQueryPipeline(primaryModel);
        return pipeline.getAgent();
    }

    @Bean("addressUpdatePipelineAgent")
    public SequentialAgent addressUpdatePipelineAgent(Model primaryModel) {
        AddressUpdatePipeline pipeline = new AddressUpdatePipeline(primaryModel);
        return pipeline.getAgent();
    }

    @Bean("generalQueryPipelineAgent")
    public SequentialAgent generalQueryPipelineAgent(Model primaryModel) {
        GeneralQueryPipeline pipeline = new GeneralQueryPipeline(primaryModel);
        return pipeline.getAgent();
    }
}
```

#### 8.8.3.15 服务中心 CustomerServiceCenter.java

```java
package io.agentscope.tutorial.chapter08.service;

import com.alibaba.cloud.ai.agent.agentscope.flow.AgentScopeRoutingAgent;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.node.RoutingMergeNode;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import io.agentscope.tutorial.chapter08.model.QueryClassification;
import io.agentscope.tutorial.chapter08.model.ServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 智能客户服务中心
 *
 * 整合 Routing + Pipeline 模式的多代理服务：
 * 1. 接收用户查询
 * 2. 使用路由 Agent 分类查询
 * 3. 调用对应的 Pipeline 处理
 * 4. 汇总结果返回
 *
 * 这是一个简化的实现，实际生产环境可以扩展为：
 * - 并行执行多个匹配的 Pipeline
 * - 添加监督者 Agent 审核结果
 * - 支持多轮对话和上下文管理
 */
@Service
public class CustomerServiceCenter {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceCenter.class);

    private static final String[] PIPELINE_KEYS = {
        "order_query_key",
        "address_update_key",
        "general_query_key"
    };

    private static final String[] PIPELINE_NAMES = {
        "order_query_pipeline",
        "address_update_pipeline",
        "general_query_pipeline"
    };

    private final AgentScopeRoutingAgent routerAgent;
    private final SynthesisService synthesisService;

    public CustomerServiceCenter(
            AgentScopeRoutingAgent routerAgent,
            SynthesisService synthesisService) {
        this.routerAgent = routerAgent;
        this.synthesisService = synthesisService;
        log.info("智能客户服务中心初始化完成");
    }

    /**
     * 处理用户查询
     *
     * @param userQuery 用户查询
     * @return 服务结果
     */
    public ServiceResult process(String userQuery) {
        log.info("开始处理用户查询：{}", userQuery);

        try {
            // 1. 路由决策
            Optional<OverAllState> resultOpt = routerAgent.invoke(userQuery);
            if (resultOpt.isEmpty()) {
                log.warn("路由代理未返回结果");
                return ServiceResult.of(userQuery,
                    new QueryClassification(QueryClassification.UNKNOWN, "路由失败"),
                    "抱歉，服务暂时不可用，请稍后重试。");
            }

            OverAllState state = resultOpt.get();

            // 2. 收集路由决策
            QueryClassification classification = analyzeRouting(state, userQuery);
            log.info("查询分类结果：{}", classification.category());

            // 3. 收集 Pipeline 输出
            List<String> invokedAgents = new ArrayList<>();
            String finalAnswer;

            // 检查是否有合并后的输出
            Optional<Object> mergedOpt = state.value(RoutingMergeNode.DEFAULT_MERGED_OUTPUT_KEY);
            if (mergedOpt.isPresent()) {
                // 有合并输出，直接使用
                finalAnswer = extractText(mergedOpt.get());
            } else {
                // 没有合并输出，收集各 Pipeline 的输出
                String pipelineOutput = collectPipelineOutputs(state, invokedAgents);
                finalAnswer = synthesisService.synthesizeSingle(
                    userQuery,
                    classification.toPipelineNode(),
                    pipelineOutput
                );
            }

            return new ServiceResult(
                userQuery,
                classification,
                state.value("pipeline_outputs")
                    .map(o -> (java.util.Map<String, String>) o)
                    .orElse(java.util.Map.of()),
                finalAnswer,
                invokedAgents
            );

        } catch (GraphRunnerException e) {
            log.error("执行图运行器异常", e);
            return ServiceResult.of(userQuery,
                new QueryClassification(QueryClassification.UNKNOWN, "执行异常"),
                "抱歉，服务执行过程中出现错误，请稍后重试。");
        }
    }

    /**
     * 分析路由决策
     */
    private QueryClassification analyzeRouting(OverAllState state, String query) {
        for (int i = 0; i < PIPELINE_KEYS.length; i++) {
            Optional<Object> outputOpt = state.value(PIPELINE_KEYS[i]);
            if (outputOpt.isPresent()) {
                String pipelineName = PIPELINE_NAMES[i];
                String reason = getReasonForPipeline(pipelineName);
                return new QueryClassification(pipelineName, reason);
            }
        }
        return new QueryClassification(QueryClassification.UNKNOWN, "未能匹配到对应服务");
    }

    /**
     * 收集各 Pipeline 的输出
     */
    private String collectPipelineOutputs(OverAllState state, List<String> invokedAgents) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < PIPELINE_KEYS.length; i++) {
            Optional<Object> outputOpt = state.value(PIPELINE_KEYS[i]);
            if (outputOpt.isPresent()) {
                String text = extractText(outputOpt.get());
                String pipelineName = PIPELINE_NAMES[i];
                invokedAgents.add(pipelineName);

                if (output.length() > 0) {
                    output.append("\n\n");
                }
                output.append("【").append(getPipelineDisplayName(pipelineName)).append("】\n")
                      .append(text);
            }
        }

        return output.length() > 0 ? output.toString() : "未获取到处理结果";
    }

    /**
     * 提取文本内容
     */
    private String extractText(Object output) {
        if (output instanceof Message message) {
            return message.getText();
        }
        return output != null ? output.toString() : "";
    }

    /**
     * 获取 Pipeline 的中文名称
     */
    private String getPipelineDisplayName(String name) {
        return switch (name) {
            case "order_query_pipeline" -> "订单查询服务";
            case "address_update_pipeline" -> "地址更新服务";
            case "general_query_pipeline" -> "综合查询服务";
            default -> name;
        };
    }

    /**
     * 获取选择该 Pipeline 的原因
     */
    private String getReasonForPipeline(String name) {
        return switch (name) {
            case "order_query_pipeline" -> "查询涉及订单状态或物流信息";
            case "address_update_pipeline" -> "查询涉及收货地址更新";
            case "general_query_pipeline" -> "查询涉及产品、优惠或账户信息";
            default -> "根据查询内容匹配";
        };
    }
}
```

#### 8.8.3.16 工作流配置（可选扩展）WorkflowConfig.java

```java
package io.agentscope.tutorial.chapter08.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.Command;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter08.tools.GeneralSearchTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流配置（Workflow 模式）
 *
 * 本配置展示了如何使用 StateGraph 实现更复杂的工作流编排。
 * 在 RouterAgent 之外，提供了一个可选的 StateGraph 版本，
 * 支持更细粒度的流程控制和监督者模式。
 *
 * 工作流节点：
 * 1. classifier - 分类节点（使用 ReActAgent）
 * 2. order_query - 订单查询节点
 * 3. address_update - 地址更新节点
 * 4. general_query - 综合查询节点
 * 5. summarizer - 汇总节点（监督者）
 *
 * 工作流边：
 * START → classifier
 * classifier → [order_query | address_update | general_query]（条件边）
 * 各处理节点 → summarizer
 * summarizer → END
 */
@Configuration
public class WorkflowConfig {

    private static final Logger log = LoggerFactory.getLogger(WorkflowConfig.class);

    /**
     * 创建完整的工作流图
     *
     * 这是一个可选的高级配置，展示了如何使用 StateGraph 实现监督者模式。
     * 与 RouterAgent 的区别：
     * - StateGraph 可以添加任意的功能节点（如 summarizer）
     * - 支持更细粒度的条件路由逻辑
     * - 可以实现循环、并行等复杂流程
     */
    @Bean
    public CompiledGraph customerServiceWorkflow(Model primaryModel) {

        StateGraph graph = new StateGraph(() -> {
            Map<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("messages", new AppendStrategy(false));
            strategies.put("category", new ReplaceStrategy());
            strategies.put("final_result", new ReplaceStrategy());
            return strategies;
        });

        // === 添加分类节点 ===
        AgentScopeAgent classifierAgent = createClassifierAgent(primaryModel);
        graph.addNode("classifier", classifierAgent.asNode());

        // === 添加处理节点 ===
        // 这里简化处理，实际应该注入 Pipeline
        AgentScopeAgent orderAgent = createStubAgent(primaryModel, "order_handler",
            "你是订单处理专家，处理订单查询请求。");
        graph.addNode("order_query", orderAgent.asNode());

        AgentScopeAgent addressAgent = createStubAgent(primaryModel, "address_handler",
            "你是地址处理专家，处理地址更新请求。");
        graph.addNode("address_update", addressAgent.asNode());

        AgentScopeAgent generalAgent = createStubAgent(primaryModel, "general_handler",
            "你是综合服务专家，处理产品搜索、优惠查询和账户信息请求。");
        graph.addNode("general_query", generalAgent.asNode());

        // === 添加汇总节点（监督者）===
        AgentScopeAgent summarizerAgent = createSummarizerAgent(primaryModel);
        graph.addNode("summarizer", summarizerAgent.asNode());

        // === 添加边 ===
        // START → classifier
        graph.addEdge(StateGraph.START, "classifier");

        // classifier → 处理节点（条件边）
        graph.addConditionalEdges("classifier",
            new AsyncCommandAction() {
                @Override
                public java.util.concurrent.CompletableFuture<Command> apply(
                        com.alibaba.cloud.ai.graph.OverAllState state,
                        com.alibaba.cloud.ai.graph.RunnableConfig config) {
                    String category = state.value("category")
                        .map(Object::toString)
                        .orElse("unknown");
                    return java.util.concurrent.CompletableFuture.completedFuture(
                        new Command(category));
                }
            },
            Map.of(
                "order_query", "order_query",
                "address_update", "address_update",
                "general_query", "general_query",
                "unknown", "summarizer"
            )
        );

        // 处理节点 → summarizer
        graph.addEdge("order_query", "summarizer");
        graph.addEdge("address_update", "summarizer");
        graph.addEdge("general_query", "summarizer");

        // summarizer → END
        graph.addEdge("summarizer", StateGraph.END);

        try {
            CompiledGraph compiled = graph.compile();
            log.info("客户服务中心工作流图编译完成");
            return compiled;
        } catch (Exception e) {
            log.error("工作流图编译失败", e);
            throw new RuntimeException("工作流图编译失败", e);
        }
    }

    /**
     * 创建分类 Agent
     */
    private AgentScopeAgent createClassifierAgent(Model model) {
        ReActAgent.Builder builder = ReActAgent.builder()
            .name("workflow_classifier")
            .model(model)
            .description("工作流分类器：分析用户查询，确定服务类型")
            .sysPrompt("""
                你是一个客户服务分类专家。
                分析用户的查询内容，将其分类为以下类别之一：
                - order_query：订单查询，如"我的订单到哪了"、"查询订单列表"
                - address_update：地址更新，如"修改收货地址"、"更新地址"
                - general_query：综合查询，如"有什么优惠"、"我的积分是多少"
                - unknown：无法确定类别

                输出格式：
                只输出分类结果，如：order_query
                不要输出其他文字。
                """)
            .memory(new InMemoryMemory());

        return AgentScopeAgent.fromBuilder(builder)
            .name("workflow_classifier")
            .description("工作流分类器")
            .instruction("请分类以下用户查询：{input}")
            .includeContents(false)
            .outputKey("category")
            .build();
    }

    /**
     * 创建 Stub Agent（用于演示）
     */
    private AgentScopeAgent createStubAgent(Model model, String name, String prompt) {
        ReActAgent.Builder builder = ReActAgent.builder()
            .name(name)
            .model(model)
            .description(name)
            .sysPrompt(prompt)
            .memory(new InMemoryMemory());

        return AgentScopeAgent.fromBuilder(builder)
            .name(name)
            .description(name)
            .instruction("{input}")
            .includeContents(false)
            .outputKey("result")
            .build();
    }

    /**
     * 创建汇总 Agent（监督者）
     */
    private AgentScopeAgent createSummarizerAgent(Model model) {
        ReActAgent.Builder builder = ReActAgent.builder()
            .name("summarizer")
            .model(model)
            .description("汇总监督者：整合各处理节点的结果")
            .sysPrompt("""
                你是一个客户服务汇总专家。
                汇总各服务节点的处理结果，用简洁友好的方式呈现给用户。

                规则：
                1. 整合各节点输出的关键信息
                2. 避免重复，突出重点
                3. 询问用户是否需要其他帮助
                4. 如果有任何节点处理失败，需要在回复中说明

                输出格式：
                用段落形式回复，不要使用 JSON 或列表格式。
                """)
            .memory(new InMemoryMemory());

        return AgentScopeAgent.fromBuilder(builder)
            .name("summarizer")
            .description("结果汇总监督者")
            .instruction("请汇总以下处理结果：{result}")
            .includeContents(false)
            .outputKey("final_result")
            .build();
    }
}
```

#### 8.8.3.17 演示运行类 Chapter08Demo.java（可选）

```java
package io.agentscope.tutorial.chapter08;

import io.agentscope.tutorial.chapter08.model.ServiceResult;
import io.agentscope.tutorial.chapter08.service.CustomerServiceCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 演示运行器
 *
 * 当配置文件中设置 customer-service.demo.enabled=true 时，
 * 应用启动后自动运行演示查询。
 */
@Component
@ConditionalOnProperty(name = "customer-service.demo.enabled", havingValue = "true")
public class Chapter08Demo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Chapter08Demo.class);

    private final CustomerServiceCenter serviceCenter;

    public Chapter08Demo(CustomerServiceCenter serviceCenter) {
        this.serviceCenter = serviceCenter;
    }

    @Override
    public void run(String... args) {
        log.info("===========================================");
        log.info("第八章案例演示：智能客户服务中心");
        log.info("===========================================");

        // 演示查询
        demonstrateOrderQuery();
        demonstrateAddressUpdate();
        demonstrateGeneralQuery();

        log.info("===========================================");
        log.info("演示完成！");
        log.info("===========================================");
    }

    private void demonstrateOrderQuery() {
        log.info("\n--- 演示1：订单查询 ---");
        String query = "帮我查一下订单 ORD20240115001 到哪里了？";

        log.info("用户查询：{}", query);
        ServiceResult result = serviceCenter.process(query);

        log.info("分类结果：{}", result.classify().category());
        log.info("最终回复：\n{}", result.finalAnswer());
    }

    private void demonstrateAddressUpdate() {
        log.info("\n--- 演示2：地址更新 ---");
        String query = "帮我把收货地址改成上海市浦东新区世纪大道100号，收货人李四，手机号13900001111";

        log.info("用户查询：{}", query);
        ServiceResult result = serviceCenter.process(query);

        log.info("分类结果：{}", result.classify().category());
        log.info("最终回复：\n{}", result.finalAnswer());
    }

    private void demonstrateGeneralQuery() {
        log.info("\n--- 演示3：综合查询 ---");
        String query = "我有多少积分？有什么优惠可以用？";

        log.info("用户查询：{}", query);
        ServiceResult result = serviceCenter.process(query);

        log.info("分类结果：{}", result.classify().category());
        log.info("最终回复：\n{}", result.finalAnswer());
    }
}
```

### 8.8.4 运行方式

#### 环境准备

```bash
# 1. 设置 DashScope API Key
export AI_DASHSCOPE_API_KEY=your-api-key-here

# 2. 进入项目目录
cd D:/Work/ai/agents/agentscope-java/tutorial/chapter08-multi-agent

# 3. 编译项目
../mvnw clean package -DskipTests
```

#### 启动应用

```bash
# 方式1：启用演示模式（启动后自动运行测试查询）
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--customer-service.demo.enabled=true"

# 方式2：正常启动（通过 API 调用）
./mvnw spring-boot:run
```

#### API 调用示例

启动后可以通过 REST API 调用：

```bash
# 查询订单
curl -X POST http://localhost:8088/api/customer-service/query \
  -H "Content-Type: application/json" \
  -d '{"query": "帮我查一下订单 ORD20240115001 到哪里了？"}'

# 更新地址
curl -X POST http://localhost:8088/api/customer-service/query \
  -H "Content-Type: application/json" \
  -d '{"query": "帮我把收货地址改成上海市浦东新区世纪大道100号，收货人李四，手机号13900001111"}'

# 综合查询
curl -X POST http://localhost:8088/api/customer-service/query \
  -H "Content-Type: application/json" \
  -d '{"query": "我有多少积分？有什么优惠可以用？"}'
```

### 8.8.5 案例总结

本案例展示了 AgentScope Java 中两种最常用的多代理模式：

| 模式 | 在案例中的应用 | 关键价值 |
|------|---------------|---------|
| **Pipeline** | 三个独立流水线：订单查询、地址更新、综合查询 | 每个流水线内部顺序执行，职责清晰，便于调试 |
| **Routing** | RouterAgent 统一入口，分类后分发 | 动态决策，无需预定义流程，支持多对一映射 |

**架构亮点**：

1. **职责分离**：每个 Pipeline 只负责一类任务，Agent 角色清晰
2. **可扩展性**：新增服务类型只需添加新的 Pipeline，无需修改路由逻辑
3. **工具抽象**：工具类（如 OrderQueryTool）与 Agent 逻辑分离，便于单独测试
4. **合成服务**：统一的结果合成层，确保输出格式一致

---

## 本章小结

本章介绍了 AgentScope Java 框架下的 7 种多代理架构模式：

| 模式 | 核心场景 | AgentScope 组件 |
|------|---------|----------------|
| Handoffs | 客服来回切换 | StateGraph + 交接工具 |
| Pipeline | 顺序/并行/循环处理 | SequentialAgent / ParallelAgent / LoopAgent |
| Routing | 分类后分发 | AgentScopeRoutingAgent |
| Supervisor | 结果审核把关 | StateGraph + 监督者节点 |
| SubAgent | 动态创建子任务 | AgentScopeAgent.subAgents() |
| Workflow | 复杂业务流程编排 | StateGraph (DAG) |

**实战建议**：
- 从简单的 **Pipeline** 开始，积累经验后再尝试 **Routing**
- **Handoffs** 适合客服等需要来回切换的场景
- **Supervisor** 和 **Workflow** 用于复杂企业流程，需要更多设计投入
- **SubAgent** 模式需要结合 Harness（第十五章），适合大规模并行处理场景

---

> **下一篇预告**：第九章将深入讲解**代理间通信协议（A2A）**，包括 AgentScope 的 A2A 实现、RocketMQ 集成，以及多代理通信的实战案例。