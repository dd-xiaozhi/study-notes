# 第 6 章 LangGraph 深度实战

> 本章是教程从"理论"转向"工程"的关键章节。我们将全面讲透 LangGraph——目前最主流的 Agent 编排框架。后续所有实战项目都会基于 LangGraph 构建。

---

## 6.1 为什么选 LangGraph

### 6.1.1 从 LangChain 到 LangGraph

如果你用过 LangChain，一定熟悉 `Chain` 这个概念。Chain 是"链式调用"：把 Prompt、LLM、Parser、Tool 等组件像链条一样串起来，一节一节往下走。

```python
# 传统 LangChain Chain 的写法
chain = prompt | llm | output_parser
result = chain.invoke({"input": "你好"})
```

这种 DAG（有向无环图）结构在处理线性流程时非常优雅，但当我们想构建一个 **真正的 Agent** 时，立刻就会遇到三个问题：

1. **没有循环**：Agent 需要"思考 → 行动 → 观察 → 再思考"，这天然是一个环。
2. **状态难管**：多轮对话、工具结果、中间变量需要在多个节点间共享。
3. **人在回路**：危险操作前要人工审批，Chain 写法很难优雅地实现。

LangGraph 就是为了解决这些问题而诞生的。它把整个 Agent 流程建模为 **状态机（State Machine）**，每个节点是一个函数，边是状态转移条件，**循环是天然支持的一等公民**。

### 6.1.2 LangChain vs LangGraph 对比

| 维度 | LangChain Chain | LangGraph |
| --- | --- | --- |
| 拓扑结构 | DAG（有向无环图） | 支持循环的状态机 |
| 状态管理 | 局部变量、闭包 | 全局 `State`，自动在节点间流转 |
| 循环 | 不支持 | 一等公民（`add_edge` 回到上游即可） |
| 人机协同 | 需要 hack | 原生 `interrupt()` API |
| 持久化 | 手动实现 | 内置 `Checkpointer` |
| 适用场景 | 简单流水线、RAG | 复杂 Agent、多步推理 |
| 调试 | LangSmith 跟踪 | LangSmith + 自动 mermaid 图 |

### 6.1.3 与其他 Agent 框架对比

| 框架 | 核心理念 | 优点 | 缺点 |
| --- | --- | --- | --- |
| **LangGraph** | 显式状态机 + 图 | 灵活、可控、生产级 | 学习曲线较陡 |
| **AutoGen**（微软） | 多 Agent 对话 | 适合角色扮演、辩论 | 状态管理隐式，复杂场景易失控 |
| **CrewAI** | Crew / Role / Task 抽象 | 上手快、声明式 | 灵活度低、循环支持弱 |
| **Claude Agent SDK** | Claude 原生工具调用 | Claude 模型效果最佳 | 绑定单一模型供应商 |

### 6.1.4 谁在用 LangGraph

LangGraph 在生产环境被广泛采用：

- **Replit**：用 LangGraph 构建 Agent，自动生成完整 Web 应用
- **Uber**：客服 Agent，处理多轮对话 + 工具调用
- **Klarna**：AI 客服，处理退款、物流查询等业务
- **LinkedIn**：求职 Agent，搜索职位 + 匹配 + 投递
- **阿里、字节、京东**：内部 RAG 与 Agent 平台

选 LangGraph，本质上是选 **"显式状态 + 图编排"** 的工程范式——这在生产环境最可控。

---

## 6.2 核心概念

LangGraph 的概念不多，但每个都很关键。我们先把它们一次性讲透。

### 6.2.1 七个核心概念

| 概念 | 含义 | 类比 |
| --- | --- | --- |
| **State（状态）** | 图中流转的数据 | 流水线上传递的"工单" |
| **Node（节点）** | 状态转换函数 | 流水线上的"工位" |
| **Edge（边）** | 节点间的连接 | 流水线上的"传送带" |
| **Graph（图）** | 节点和边的集合 | 整条流水线 |
| **Compile（编译）** | 把 Graph 编译成可执行对象 | 把图纸变成可运行的机器 |
| **Checkpointer（检查点）** | 状态持久化器 | 给流水线装个"存档点" |
| **Send / Command** | 动态控制流 | 临时加塞的"调度员" |

### 6.2.2 用一张图把它们串起来

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart LR
    A[用户输入] --> B[State<br/>TypedDict 状态]
    B --> C[Node 1<br/>状态转换]
    C --> D[Node 2<br/>调用 LLM]
    D --> E{条件路由<br/>Conditional Edge}
    E -->|是| F[Node 3<br/>调用工具]
    E -->|否| G[END 结束]
    F --> D
    D -.->|Checkpointer| H[(SQLite<br/>持久化)]
    H -.->|Time Travel| D
    classDef stateNode fill:#1e3a8a,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef llmNode fill:#7c2d12,stroke:#1e3a8a,stroke-width:2px,color:#fff
    classDef condNode fill:#14532d,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef endNode fill:#581c87,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef dbNode fill:#7f1d1d,stroke:#fff,stroke-width:2px,color:#fff
    class B stateNode
    class D llmNode
    class E condNode
    class G endNode
    class H dbNode
```

### 6.2.3 State（状态）

**State 是图中流转的数据**，通常用 Python 的 `TypedDict` 定义。每个节点读取 State、处理后返回部分更新，LangGraph 会自动合并到全局 State。

```python
from typing import TypedDict

class AgentState(TypedDict):
    messages: list[str]      # 消息历史
    user_id: str              # 用户标识
    step_count: int           # 步骤计数
    final_answer: str | None  # 最终答案
```

### 6.2.4 Node（节点）

**Node 是一个函数**，签名为 `(state) -> dict`。它接收 State 的一部分（也可以读全部），返回要更新的字段。

```python
def my_node(state: AgentState) -> dict:
    return {"step_count": state["step_count"] + 1}
```

### 6.2.5 Edge（边）

边定义节点间的流转。LangGraph 支持两种：

- **Normal Edge（普通边）**：`add_edge("A", "B")`，A 完了必走 B。
- **Conditional Edge（条件边）**：`add_conditional_edges("A", router_fn, {...})`，根据路由函数返回值决定下一节点。

```python
# 普通边
builder.add_edge("node_a", "node_b")

# 条件边
def router(state):
    return "tool" if needs_tool(state) else "end"

builder.add_conditional_edges("llm", router, {
    "tool": "tool_node",
    "end": END
})
```

### 6.2.6 Graph 与 Compile

`StateGraph` 是构建器（Builder），`compile()` 后才得到可执行的 `CompiledStateGraph`。

```python
builder = StateGraph(AgentState)
builder.add_node("a", node_a)
builder.add_node("b", node_b)
builder.add_edge(START, "a")
builder.add_edge("a", "b")
builder.add_edge("b", END)

graph = builder.compile()  # 编译
result = graph.invoke({"messages": []})  # 执行
```

### 6.2.7 Checkpointer（检查点）

**Checkpointer 把每一步的 State 存到外部存储**，实现：
- **持久化**：进程崩溃后能恢复
- **Time Travel**：回到任意历史状态
- **Human-in-the-Loop**：在某个节点暂停，等人工介入

```python
from langgraph.checkpoint.memory import MemorySaver
memory = MemorySaver()
graph = builder.compile(checkpointer=memory)
```

---

## 6.3 Hello World：5 分钟搭一个 Agent

我们从最简的"问候 Agent"开始，体验完整流程。

### 6.3.1 完整代码

```python
# hello_langgraph.py
from typing import TypedDict
from langgraph.graph import StateGraph, START, END

# 1. 定义 State
class State(TypedDict):
    name: str
    greeting: str

# 2. 定义节点
def greet_node(state: State) -> dict:
    name = state["name"]
    return {"greeting": f"你好，{name}！欢迎来到 LangGraph 的世界。"}

# 3. 构建图
builder = StateGraph(State)
builder.add_node("greet", greet_node)
builder.add_edge(START, "greet")
builder.add_edge("greet", END)

# 4. 编译
graph = builder.compile()

# 5. 调用
result = graph.invoke({"name": "小明", "greeting": ""})
print(result)
```

### 6.3.2 逐行讲解

| 行 | 作用 |
| --- | --- |
| `class State(TypedDict)` | 定义状态结构，greeting 字段会由节点填充 |
| `def greet_node(state)` | 节点函数：读 `state["name"]`，返回 `greeting` |
| `builder.add_node("greet", greet_node)` | 注册节点，名字叫 "greet" |
| `builder.add_edge(START, "greet")` | 入口：流程从 START 进入 greet |
| `builder.add_edge("greet", END)` | 出口：greet 完了结束 |
| `builder.compile()` | 编译成可执行对象（校验图结构） |
| `graph.invoke(...)` | 同步执行，返回最终 State |

### 6.3.3 运行输出（模拟）

```
$ python hello_langgraph.py

{'name': '小明', 'greeting': '你好，小明！欢迎来到 LangGraph 的世界。'}
```

**进阶版（用 LLM）**：

```python
# hello_llm.py
import os
from typing import TypedDict
from langchain_openai import ChatOpenAI
from langgraph.graph import StateGraph, START, END

os.environ["OPENAI_API_KEY"] = "sk-xxx"

class State(TypedDict):
    topic: str
    poem: str

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.7)

def write_poem(state: State) -> dict:
    msg = llm.invoke(f"请围绕「{state['topic']}」写一首四句古诗。")
    return {"poem": msg.content}

builder = StateGraph(State)
builder.add_node("write_poem", write_poem)
builder.add_edge(START, "write_poem")
builder.add_edge("write_poem", END)
graph = builder.compile()

print(graph.invoke({"topic": "秋叶", "poem": ""}))
```

**输出**：

```
{'topic': '秋叶', 'poem': '金风瑟瑟染层林，落叶纷飞逐故心。\n独倚栏杆望远岫，一轮明月照疏襟。'}
```

### 6.3.4 流程图

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart LR
    A([START]) --> B[greet 节点<br/>生成问候语]
    B --> C([END])
    classDef startEnd fill:#581c87,stroke:#7f1d1d,stroke-width:2px,color:#fff
    classDef node fill:#1e3a8a,stroke:#7c2d12,stroke-width:2px,color:#fff
    class A,C startEnd
    class B node
```

这就是 LangGraph 的最小骨架：`State → Node → Edge → Compile → Invoke`。下面我们逐个深挖。

---

## 6.4 State 的设计

State 是 LangGraph 的灵魂。设计 State 时要回答三个问题：

1. 用什么类型定义？（TypedDict / Pydantic / dataclass）
2. 状态如何合并？（Reducer）
3. 默认值怎么给？（可选）

### 6.4.1 三种定义方式

```python
# state_definitions.py
from typing import TypedDict, Annotated, List
from pydantic import BaseModel, Field
from dataclasses import dataclass, field
from langgraph.graph.message import add_messages
from langchain_core.messages import BaseMessage

# ========== 方式 1: TypedDict（最常用）==========
class AgentState(TypedDict):
    messages: Annotated[List[BaseMessage], add_messages]
    user_id: str
    step: int

# ========== 方式 2: Pydantic（带类型校验）==========
class AgentStatePydantic(BaseModel):
    messages: List[BaseMessage] = Field(default_factory=list)
    user_id: str
    step: int = 0

# ========== 方式 3: dataclass（纯 Python）==========
@dataclass
class AgentStateDC:
    messages: List[BaseMessage] = field(default_factory=list)
    user_id: str = ""
    step: int = 0
```

### 6.4.2 对比

| 特性 | TypedDict | Pydantic | dataclass |
| --- | --- | --- | --- |
| 类型校验 | 静态（IDE） | 运行时 | 静态 |
| 默认值 | 不支持 | 支持 | 支持 |
| LangGraph 推荐 | 是 | 实验性 | 支持 |
| 性能 | 最快 | 慢（有校验） | 快 |
| 适用场景 | 90% 场景 | 需要严格校验 | 简单场景 |

**官方推荐 TypedDict**——性能好、与 LangGraph 兼容性最佳。

### 6.4.3 Reducer：状态如何合并

当节点返回 `{"messages": [new_msg]}` 时，LangGraph 默认会 **覆盖** `messages` 字段。如果你想 **追加**（消息列表场景），就需要 Reducer。

```python
# 默认行为：覆盖
def node_a(state):
    return {"messages": ["hello"]}  # 整个 messages 被替换成 ["hello"]

# 追加行为：使用 Reducer
from typing import Annotated
from langgraph.graph.message import add_messages

class State(TypedDict):
    messages: Annotated[list, add_messages]  # 用 add_messages 累加
```

### 6.4.4 内置 Reducer：`add_messages`

`add_messages` 是 LangGraph 专为消息列表设计的 Reducer，行为类似 `append`，但会自动处理消息 ID 和去重。

```python
# add_messages_demo.py
from typing import TypedDict, Annotated
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langchain_core.messages import HumanMessage, AIMessage

class State(TypedDict):
    messages: Annotated[list, add_messages]

def node_a(state: State) -> dict:
    return {"messages": [AIMessage(content="我是 AI")]}  # 追加

def node_b(state: State) -> dict:
    return {"messages": [AIMessage(content="继续说")]}

builder = StateGraph(State)
builder.add_node("a", node_a)
builder.add_node("b", node_b)
builder.add_edge(START, "a")
builder.add_edge("a", "b")
builder.add_edge("b", END)
graph = builder.compile()

print(graph.invoke({"messages": [HumanMessage(content="你好")]}))
# messages: [HumanMessage('你好'), AIMessage('我是 AI'), AIMessage('继续说')]
```

### 6.4.5 自定义 Reducer

```python
# custom_reducer.py
from typing import TypedDict, Annotated
from langgraph.graph import StateGraph, START, END
import operator

# 累加 Reducer（类似 sum）
def add_dict(left: dict, right: dict) -> dict:
    """合并两个 dict，相同 key 累加"""
    result = left.copy()
    for k, v in right.items():
        result[k] = result.get(k, 0) + v
    return result

class State(TypedDict):
    counters: Annotated[dict, add_dict]

def node_a(state: State) -> dict:
    return {"counters": {"a": 1, "b": 2}}

def node_b(state: State) -> dict:
    return {"counters": {"a": 10, "c": 5}}

builder = StateGraph(State)
builder.add_node("a", node_a)
builder.add_node("b", node_b)
builder.add_edge(START, "a")
builder.add_edge("a", "b")
builder.add_edge("b", END)
graph = builder.compile()

print(graph.invoke({"counters": {}}))
# {'counters': {'a': 11, 'b': 2, 'c': 5}}  # a=1+10, b=2, c=5
```

### 6.4.6 完整代码：三种 State 定义方式演示

```python
# state_three_ways.py
from typing import TypedDict, Annotated, List
from pydantic import BaseModel
from dataclasses import dataclass, field
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langchain_core.messages import HumanMessage

# ---- TypedDict 写法 ----
class StateTD(TypedDict):
    messages: Annotated[List, add_messages]
    score: int

def node_td(state: StateTD) -> dict:
    return {"score": state.get("score", 0) + 1,
            "messages": ["hi"]}

b1 = StateGraph(StateTD)
b1.add_node("n", node_td)
b1.add_edge(START, "n")
b1.add_edge("n", END)
g1 = b1.compile()
print("TypedDict:", g1.invoke({"messages": [], "score": 0}))

# ---- Pydantic 写法 ----
class StatePD(BaseModel):
    messages: List = []
    score: int = 0

def node_pd(state: StatePD) -> dict:
    return {"score": state.score + 1, "messages": ["hi"]}

b2 = StateGraph(StatePD)
b2.add_node("n", node_pd)
b2.add_edge(START, "n")
b2.add_edge("n", END)
g2 = b2.compile()
print("Pydantic:  ", g2.invoke({"messages": [], "score": 0}))

# ---- dataclass 写法 ----
@dataclass
class StateDC:
    messages: List = field(default_factory=list)
    score: int = 0

def node_dc(state: StateDC) -> dict:
    return {"score": state.score + 1, "messages": ["hi"]}

b3 = StateGraph(StateDC)
b3.add_node("n", node_dc)
b3.add_edge(START, "n")
b3.add_edge("n", END)
g3 = b3.compile()
print("dataclass:", g3.invoke(StateDC(messages=[], score=0)))
```

**设计建议**：

- 90% 场景用 TypedDict + `Annotated[list, add_messages]`
- 当你希望运行时校验输入（如外部 API）用 Pydantic
- State 字段不要超过 15 个，否则会变得难维护

---

## 6.5 Node 的实现

Node 是"动作"，State 是"数据"。Node 的本质是 **一个纯函数**。

### 6.5.1 Node 函数签名

```python
def my_node(state: State) -> dict | State:
    # 1. 读 state
    # 2. 执行业务逻辑（LLM/工具/子图）
    # 3. 返回要更新的字段（partial update）
    return {"field": "new_value"}
```

返回 **部分字段**，LangGraph 会用 Reducer 合并到全局 State。

### 6.5.2 同步 vs 异步 Node

```python
import asyncio
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(model="gpt-4o-mini")

# 同步 Node
def sync_node(state: State) -> dict:
    result = llm.invoke(state["prompt"])
    return {"answer": result.content}

# 异步 Node（高并发推荐）
async def async_node(state: State) -> dict:
    result = await llm.ainvoke(state["prompt"])
    return {"answer": result.content}

# 注册：两者语法一样，LangGraph 会按调用方式选 sync/async
builder.add_node("sync", sync_node)
builder.add_node("async", async_node)

# 同步调用 → 跑 sync_node
graph.invoke(input)

# 异步调用 → 跑 async_node
await graph.ainvoke(input)
```

### 6.5.3 Node 中调用 LLM、工具、子图

```python
from langchain_core.tools import tool
from langgraph.graph import StateGraph

# ---- 工具 ----
@tool
def search(query: str) -> str:
    """模拟搜索引擎"""
    return f"搜索结果：关于「{query}」找到 3 条信息"

tools = [search]
llm_with_tools = llm.bind_tools(tools)

# ---- LLM Node ----
def call_llm(state: State) -> dict:
    response = llm_with_tools.invoke(state["messages"])
    return {"messages": [response]}

# ---- 工具 Node ----
from langgraph.prebuilt import ToolNode
tool_node = ToolNode(tools)

# ---- 子图 Node（见 6.10）----
# def call_subgraph(state): return subgraph.invoke(state)
```

### 6.5.4 错误处理与重试

```python
import logging
from tenacity import retry, stop_after_attempt, wait_exponential

logger = logging.getLogger(__name__)

@retry(stop=stop_after_attempt(3), wait=wait_exponential(min=1, max=10))
def robust_llm_node(state: State) -> dict:
    """带指数退避重试的 LLM 节点"""
    try:
        response = llm.invoke(state["messages"])
        return {"messages": [response], "error": None}
    except Exception as e:
        logger.error(f"LLM 调用失败: {e}")
        if "rate_limit" in str(e).lower():
            raise  # 触发重试
        return {"error": str(e), "messages": []}

# 或者用 LangGraph 自带的 RetryPolicy
from langgraph.types import RetryPolicy
builder.add_node(
    "robust_llm",
    robust_llm_node,
    retry_policy=RetryPolicy(max_attempts=3)
)
```

### 6.5.5 完整 Node 示例（含 LLM、工具、错误处理）

```python
# node_full_demo.py
import os, logging
from typing import TypedDict, Annotated, List
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode
from langgraph.types import RetryPolicy

os.environ["OPENAI_API_KEY"] = "sk-xxx"
logging.basicConfig(level=logging.INFO)

class State(TypedDict):
    messages: Annotated[List, add_messages]
    retry_count: int

@tool
def get_weather(city: str) -> str:
    """查询城市天气"""
    return f"{city}：晴，25°C"

llm = ChatOpenAI(model="gpt-4o-mini").bind_tools([get_weather])

# Node 1：调用 LLM
def llm_node(state: State) -> dict:
    resp = llm.invoke(state["messages"])
    return {"messages": [resp]}

# Node 2：执行工具
tool_node = ToolNode([get_weather])

# 路由：LLM 是否要调用工具？
def should_continue(state: State) -> str:
    last = state["messages"][-1]
    return "tools" if last.tool_calls else "end"

builder = StateGraph(State)
builder.add_node("llm", llm_node, retry_policy=RetryPolicy(max_attempts=3))
builder.add_node("tools", tool_node)
builder.add_edge(START, "llm")
builder.add_conditional_edges("llm", should_continue,
                              {"tools": "tools", "end": END})
builder.add_edge("tools", "llm")
graph = builder.compile()

result = graph.invoke({"messages": [{"role": "user", "content": "北京天气？"}]})
for m in result["messages"]:
    print(f"{type(m).__name__}: {m.content[:60]}")
```

**输出**：

```
HumanMessage: 北京天气？
AIMessage: （调用 get_weather 工具）
ToolMessage: 北京：晴，25°C
AIMessage: 北京今天天气晴朗，气温 25°C，适合出行。
```

---

## 6.6 Edge 与控制流

Edge 是图的血脉。LangGraph 支持四种控制流：

| 类型 | API | 用途 |
| --- | --- | --- |
| 普通边 | `add_edge(A, B)` | 固定流转 |
| 条件边 | `add_conditional_edges(A, fn, map)` | 分支判断 |
| 入口 | `add_edge(START, A)` | 起点 |
| 终点 | `add_edge(A, END)` | 终点 |

### 6.6.1 普通边

```python
builder.add_edge("A", "B")   # A 完了必走 B
builder.add_edge("B", END)    # B 完了结束
```

### 6.6.2 条件边

```python
def router(state: State) -> str:
    """路由函数：返回字符串，对应 map 的 key"""
    if state["score"] > 80:
        return "pass"
    elif state["score"] > 60:
        return "review"
    else:
        return "reject"

builder.add_conditional_edges(
    "examine",          # 源节点
    router,             # 路由函数
    {                   # 映射：返回值 -> 目标节点
        "pass": "celebrate",
        "review": "human_review",
        "reject": "retry",
    }
)
```

### 6.6.3 循环（环）

循环 = 把边指回上游节点。这是 Agent"思考-行动-观察"循环的基础。

```python
# 工具调用完后回到 LLM，让 LLM 决定下一步
builder.add_edge("tools", "llm")

# 防止无限循环：用条件边判断
def should_continue(state):
    if state["step"] >= 5:
        return "end"      # 兜底退出
    last = state["messages"][-1]
    return "tools" if last.tool_calls else "end"

builder.add_conditional_edges("llm", should_continue,
                              {"tools": "tools", "end": END})
builder.add_edge("tools", "llm")
```

### 6.6.4 复杂控制流：分支 + 循环 + 合并

```python
# complex_flow.py
from typing import TypedDict, Literal
from langgraph.graph import StateGraph, START, END

class State(TypedDict):
    query: str
    classification: str
    answer: str
    refine_count: int

def classify(state: State) -> dict:
    q = state["query"]
    kind = "math" if any(c in q for c in "+-*/") else "general"
    return {"classification": kind}

def answer_math(state: State) -> dict:
    return {"answer": f"数学结果：{eval(state['query'])}"}

def answer_general(state: State) -> dict:
    return {"answer": f"通用回答：{state['query']}"}

def check_quality(state: State) -> Literal["refine", "done"]:
    return "refine" if state["refine_count"] < 2 else "done"

def refine(state: State) -> dict:
    return {"refine_count": state["refine_count"] + 1,
            "answer": state["answer"] + " [已润色]"}

builder = StateGraph(State)
builder.add_node("classify", classify)
builder.add_node("math", answer_math)
builder.add_node("general", answer_general)
builder.add_node("refine", refine)

builder.add_edge(START, "classify")
builder.add_conditional_edges("classify", lambda s: s["classification"],
                              {"math": "math", "general": "general"})
builder.add_edge("math", "refine")
builder.add_edge("general", "refine")
builder.add_conditional_edges("refine", check_quality,
                              {"refine": "refine", "done": END})

graph = builder.compile()
print(graph.invoke({"query": "3+5*2", "classification": "",
                    "answer": "", "refine_count": 0}))
# {'query': '3+5*2', 'classification': 'math',
#  'answer': '数学结果：13 [已润色] [已润色]', 'refine_count': 2}
```

### 6.6.5 复杂流程图

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart TD
    A([START]) --> B[classify 分类]
    B -->|math| C[answer_math]
    B -->|general| D[answer_general]
    C --> E[refine 润色]
    D --> E
    E -->|count<2| E
    E -->|count>=2| F([END])
    classDef startEnd fill:#581c87,stroke:#7f1d1d,stroke-width:2px,color:#fff
    classDef branch fill:#1e3a8a,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef loop fill:#7c2d12,stroke:#1e3a8a,stroke-width:2px,color:#fff
    classDef cond fill:#14532d,stroke:#7c2d12,stroke-width:2px,color:#fff
    class A,F startEnd
    class B,C,D branch
    class E loop
```

**控制流设计口诀**：

1. **入口**：用 `START`
2. **出口**：用 `END`（实际是特殊节点）
3. **循环**：边回指上游 + 条件边退出
4. **分支**：条件边 + 路由函数
5. **合并**：多个节点的边指向同一目标

---

## 6.7 构建一个完整的 ReAct Agent

**ReAct**（Reasoning + Acting）是 Agent 的经典范式：LLM 推理 → 决定行动 → 观察结果 → 再推理。我们用 LangGraph 完整实现。

### 6.7.1 ReAct 思路

```
用户: 北京今天多少度？穿什么合适？
↓
LLM 思考: 我需要先查天气 → 调用 get_weather("北京")
↓
工具: 返回 "北京：晴，25°C"
↓
LLM 思考: 25°C 适合穿 T 恤 + 薄外套
↓
最终回答: 北京今天 25°C 晴朗，建议穿 T 恤加薄外套。
```

### 6.7.2 完整代码（80+ 行）

```python
# react_agent.py
"""
完整 ReAct Agent：
- LLM 推理节点
- 工具执行节点
- 条件路由
- 自动循环直到 LLM 不再调用工具
"""
import os
from typing import TypedDict, Annotated, List
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langchain_core.messages import BaseMessage, HumanMessage
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.prebuilt import ToolNode

os.environ["OPENAI_API_KEY"] = "sk-xxx"

# ========== 1. 定义工具 ==========
@tool
def get_weather(city: str) -> str:
    """查询指定城市的天气"""
    weather_db = {
        "北京": "晴，25°C，东南风 3 级",
        "上海": "多云，28°C",
        "深圳": "雷阵雨，31°C",
    }
    return weather_db.get(city, f"{city}：暂无数据")

@tool
def calculator(expression: str) -> str:
    """计算数学表达式，例如 '3*5+2'"""
    try:
        # 实际生产环境应使用更安全的表达式求值器
        return f"{expression} = {eval(expression)}"
    except Exception as e:
        return f"计算错误: {e}"

tools = [get_weather, calculator]

# ========== 2. State 定义 ==========
class AgentState(TypedDict):
    messages: Annotated[List[BaseMessage], add_messages]
    step: int

# ========== 3. 模型绑定工具 ==========
llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
llm_with_tools = llm.bind_tools(tools)

# ========== 4. 节点定义 ==========
def agent_node(state: AgentState) -> dict:
    """LLM 推理节点：决定是否调用工具"""
    print(f"--- Step {state['step']}: 思考 ---")
    response = llm_with_tools.invoke(state["messages"])
    return {"messages": [response], "step": state["step"] + 1}

tool_node = ToolNode(tools)

# ========== 5. 路由函数 ==========
def should_continue(state: AgentState) -> str:
    """决定流程走向"""
    # 兜底：超过 5 步强制结束
    if state["step"] >= 5:
        print("--- 达到步数上限，强制结束 ---")
        return "end"
    last_msg = state["messages"][-1]
    # 如果 LLM 调用了工具，去执行
    if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
        print(f"--- 决定调用工具: {[tc['name'] for tc in last_msg.tool_calls]} ---")
        return "tools"
    # 否则结束
    print("--- 给出最终答案 ---")
    return "end"

# ========== 6. 构建图 ==========
builder = StateGraph(AgentState)
builder.add_node("agent", agent_node)
builder.add_node("tools", tool_node)

builder.add_edge(START, "agent")
builder.add_conditional_edges(
    "agent", should_continue,
    {"tools": "tools", "end": END}
)
builder.add_edge("tools", "agent")  # 工具执行完回到 agent

graph = builder.compile()

# ========== 7. 运行 ==========
if __name__ == "__main__":
    query = "北京今天天气如何？另外帮我算一下 (25-5)*3 是多少？"
    print(f"\n>>> 用户: {query}\n")

    result = graph.invoke({
        "messages": [HumanMessage(content=query)],
        "step": 0
    })

    print(f"\n>>> 最终回答:\n{result['messages'][-1].content}")
    print(f"\n>>> 总步数: {result['step']}")
    print(f">>> 消息条数: {len(result['messages'])}")
```

### 6.7.3 运行示例

```
$ python react_agent.py

>>> 用户: 北京今天天气如何？另外帮我算一下 (25-5)*3 是多少？

--- Step 0: 思考 ---
--- 决定调用工具: ['get_weather', 'calculator'] ---
--- Step 1: 思考 ---
--- 给出最终答案 ---

>>> 最终回答:
北京今天天气晴朗，气温 25°C，东南风 3 级，适合户外活动。
(25-5)*3 = 60。

>>> 总步数: 1
>>> 消息条数: 4
```

### 6.7.4 流程图

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart LR
    A([START]) --> B[agent<br/>LLM 思考]
    B -->|tool_calls| C[tools<br/>执行工具]
    B -->|无 tool_calls| D([END])
    C --> B
    classDef startEnd fill:#581c87,stroke:#7f1d1d,stroke-width:2px,color:#fff
    classDef llmNode fill:#1e3a8a,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef toolNode fill:#14532d,stroke:#7c2d12,stroke-width:2px,color:#fff
    class A,D startEnd
    class B llmNode
    class C toolNode
```

这就是 ReAct 的本质：**agent 节点（思考）+ tools 节点（行动）+ 条件路由（决定）**。后续所有 Agent 都是这个模板的变体。

---

## 6.8 Checkpointer 与持久化

Checkpointer 是 LangGraph 的"记忆系统"，负责把每一步的 State 序列化存储。

### 6.8.1 为什么需要持久化

1. **崩溃恢复**：进程挂了能从最近的 checkpoint 继续
2. **Time Travel**：回到任意历史状态重新执行（调试神器）
3. **多轮对话**：用户第二天回来，对话历史不丢
4. **Human-in-the-Loop**：在某个节点暂停，等人工审批后再继续
5. **Fork**：基于历史状态开新分支

### 6.8.2 三种内置 Checkpointer

| Checkpointer | 存储 | 适用场景 |
| --- | --- | --- |
| `MemorySaver` | 内存 | 单元测试、演示 |
| `SqliteSaver` | 本地 SQLite | 单机开发、原型 |
| `PostgresSaver` | PostgreSQL | 生产环境、多实例 |

```python
from langgraph.checkpoint.memory import MemorySaver
from langgraph.checkpoint.sqlite import SqliteSaver
from langgraph.checkpoint.postgres import PostgresSaver

# 内存
memory = MemorySaver()
graph = builder.compile(checkpointer=memory)

# SQLite（推荐本地开发）
with SqliteSaver.from_conn_string("./checkpoints.db") as sqlite:
    graph = builder.compile(checkpointer=sqlite)

# PostgreSQL（生产）
with PostgresSaver.from_conn_string("postgresql://user:pwd@host/db") as pg:
    graph = builder.compile(checkpointer=pg)
```

### 6.8.3 thread_id 与 checkpoint_id

- **thread_id**：会话 ID，区分不同用户/对话
- **checkpoint_id**：某次执行的具体快照 ID

```python
config = {"configurable": {"thread_id": "user_001"}}
result = graph.invoke(input, config)

# 同 thread 继续 → 历史累积
result2 = graph.invoke(new_input, config)
```

### 6.8.4 完整代码：可恢复的多轮对话

```python
# checkpoint_chat.py
import os
from typing import TypedDict, Annotated, List
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.checkpoint.sqlite import SqliteSaver

os.environ["OPENAI_API_KEY"] = "sk-xxx"

class State(TypedDict):
    messages: Annotated[List, add_messages]

llm = ChatOpenAI(model="gpt-4o-mini")

def chat(state: State) -> dict:
    return {"messages": [llm.invoke(state["messages"])]}

builder = StateGraph(State)
builder.add_node("chat", chat)
builder.add_edge(START, "chat")
builder.add_edge("chat", END)

# 用 SQLite 持久化
with SqliteSaver.from_conn_string("./chat.db") as memory:
    graph = builder.compile(checkpointer=memory)

    # 第 1 轮
    cfg = {"configurable": {"thread_id": "alice"}}
    r1 = graph.invoke(
        {"messages": [HumanMessage(content="我叫 Alice，喜欢猫")]},
        cfg
    )
    print("R1:", r1["messages"][-1].content)

    # 第 2 轮：模拟"第二天回来"
    r2 = graph.invoke(
        {"messages": [HumanMessage(content="我叫什么？喜欢什么？")]},
        cfg
    )
    print("R2:", r2["messages"][-1].content)
    # 输出: 你叫 Alice，喜欢猫（说明历史被保留）
```

### 6.8.5 Time Travel：回到历史状态

```python
# time_travel.py
from langgraph.checkpoint.sqlite import SqliteSaver

with SqliteSaver.from_conn_string("./chat.db") as memory:
    graph = builder.compile(checkpointer=memory)
    cfg = {"configurable": {"thread_id": "alice"}}

    # 列出所有历史 checkpoint
    history = list(graph.get_state_history(cfg))
    for h in history[:5]:
        print(f"Step {h.metadata['step']}: {h.next}")

    # 选择一个历史 checkpoint
    past_state = history[2]  # 回到第 3 步
    past_cfg = past_state.config

    # 从历史状态开新分支
    new_cfg = {
        "configurable": {
            "thread_id": "alice_fork",          # 新线程（不污染原线程）
            "thread_ts": past_state.config["configurable"]["thread_ts"]
        }
    }
    new_result = graph.invoke(
        {"messages": [HumanMessage(content="换个角度回答")]},
        new_cfg
    )
```

**应用场景**：

- 调试：复现某次失败，重新执行
- 探索：基于旧状态尝试不同 prompt
- 纠错：发现中间结果错了，从出错点重跑

### 6.8.6 Checkpointer 流程图

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart LR
    A[节点 A 执行] -->|step 1| B[(Checkpointer<br/>SQLite)]
    B -->|step 2| C[节点 B 执行]
    C -->|step 3| B
    B -->|读取| D[Time Travel<br/>回到 step 1]
    D --> A
    B -.->|thread_id| E[多用户隔离]
    classDef node fill:#1e3a8a,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef db fill:#7f1d1d,stroke:#fff,stroke-width:2px,color:#fff
    classDef feature fill:#14532d,stroke:#7c2d12,stroke-width:2px,color:#fff
    class A,C node
    class B db
    class D,E feature
```

---

## 6.9 Human-in-the-Loop（人在回路）

HIL 是生产 Agent 的必备能力。LangGraph 用 `interrupt()` 原生支持。

### 6.9.1 三种 HITL 模式

| 模式 | 场景 | API |
| --- | --- | --- |
| **Approval** | 危险操作前审批（发邮件、转账、删数据） | `interrupt()` + `Command(resume=...)` |
| **Editing** | 让人工修改 Agent 中间结果 | `interrupt()` + `Command(resume={"data": ...})` |
| **Input** | 节点需要人工输入（如必填字段） | 同上 |

### 6.9.2 `interrupt()` 工作机制

```python
from langgraph.types import interrupt, Command

def dangerous_node(state):
    # 暂停执行，等待人工输入
    approval = interrupt("是否批准发送邮件给老板？")
    if approval == "yes":
        return {"sent": True}
    return {"sent": False}

# 主线程
config = {"configurable": {"thread_id": "1"}}
graph.invoke(input, config)  # 跑到 dangerous_node 时暂停

# 人工审核
graph.invoke(Command(resume="yes"), config)  # 继续执行
```

### 6.9.3 完整代码：邮件发送 Agent 需人工批准

```python
# hitl_email.py
"""
场景：Agent 写完邮件后，必须人工批准才能发送。
"""
import os
from typing import TypedDict, Annotated, List, Literal
from langchain_openai import ChatOpenAI
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.checkpoint.memory import MemorySaver
from langgraph.types import interrupt, Command

os.environ["OPENAI_API_KEY"] = "sk-xxx"

class State(TypedDict):
    messages: Annotated[List, add_messages]
    email_draft: str
    approved: bool
    sent: bool

llm = ChatOpenAI(model="gpt-4o-mini")

# ---- Node 1: 写邮件草稿 ----
def write_email(state: State) -> dict:
    print(">>> [draft] 正在写邮件...")
    user_request = state["messages"][-1].content
    prompt = f"用户需求: {user_request}\n请写一封简洁的邮件（中文，3 句话以内）。"
    draft = llm.invoke(prompt).content
    return {"email_draft": draft, "messages": [f"草稿: {draft}"]}

# ---- Node 2: 人工审核 ----
def human_review(state: State) -> dict:
    print(f"\n>>> [review] 待审核邮件:\n{state['email_draft']}\n")
    # 暂停，等待人工输入
    decision = interrupt({
        "type": "approval",
        "draft": state["email_draft"]
    })
    # decision 来自人工：可以是 "approve" / "reject" / "edit:新内容"
    if decision == "approve":
        return {"approved": True}
    elif decision == "reject":
        return {"approved": False}
    elif decision.startswith("edit:"):
        new_content = decision[5:]
        return {"email_draft": new_content, "approved": True}
    return {"approved": False}

# ---- Node 3: 发送 ----
def send_email(state: State) -> dict:
    if not state["approved"]:
        print(">>> [send] 未批准，跳过发送")
        return {"sent": False}
    print(f">>> [send] 已发送: {state['email_draft']}")
    return {"sent": True}

# ---- 路由 ----
def after_review(state: State) -> Literal["send", END]:
    return "send" if state["approved"] else END

# ---- 构建图 ----
builder = StateGraph(State)
builder.add_node("write", write_email)
builder.add_node("review", human_review)
builder.add_node("send", send_email)
builder.add_edge(START, "write")
builder.add_edge("write", "review")
builder.add_conditional_edges("review", after_review,
                              {"send": "send", END: END})
builder.add_edge("send", END)

memory = MemorySaver()
graph = builder.compile(checkpointer=memory)

# ---- 运行 ----
if __name__ == "__main__":
    cfg = {"configurable": {"thread_id": "user_1"}}

    # 第一次调用：跑到 review 时暂停
    result = graph.invoke({
        "messages": ["写一封给老板的请假邮件，明天请假一天"],
        "email_draft": "",
        "approved": False,
        "sent": False
    }, cfg)
    print(f"\n[暂停] 当前状态: {result.get('__interrupt__')}")

    # 场景 1: 批准
    print("\n--- 场景 1: 人工批准 ---")
    result = graph.invoke(Command(resume="approve"), cfg)
    print(f"sent = {result['sent']}")

    # 场景 2: 编辑后批准
    print("\n--- 场景 2: 人工编辑 ---")
    cfg2 = {"configurable": {"thread_id": "user_2"}}
    graph.invoke({
        "messages": ["写一封给老板的请假邮件"],
        "email_draft": "",
        "approved": False,
        "sent": False
    }, cfg2)
    result = graph.invoke(
        Command(resume="edit:老板您好，因家中有事，明天请假一天，请批准。"),
        cfg2
    )
    print(f"final draft: {result['email_draft']}")
    print(f"sent = {result['sent']}")
```

### 6.9.4 HITL 流程图

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
sequenceDiagram
    participant U as 用户
    participant G as LangGraph
    participant H as 人工
    U->>G: 触发 Agent
    G->>G: write 节点写草稿
    G->>G: review 节点
    G->>H: interrupt() 暂停
    Note over G,H: 等待人工输入
    H->>G: Command(resume="approve")
    G->>G: send 节点发送
    G->>U: 返回结果
    classDef actor fill:#1e3a8a,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef human fill:#7c2d12,stroke:#1e3a8a,stroke-width:2px,color:#fff
```

### 6.9.5 在 Web 端集成 HITL

```python
# FastAPI 示例
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import asyncio
import json

app = FastAPI()

@app.post("/chat")
async def chat(thread_id: str, message: str):
    """发起对话，遇到 interrupt 时返回待审批状态"""
    cfg = {"configurable": {"thread_id": thread_id}}
    result = graph.invoke({"messages": [message], ...}, cfg)

    # 检查是否中断
    if result.get("__interrupt__"):
        interrupt_data = result["__interrupt__"][0].value
        return {"status": "pending", "draft": interrupt_data["draft"]}
    return {"status": "done", "answer": result["messages"][-1]}

@app.post("/approve")
async def approve(thread_id: str, decision: str):
    """人工审核接口"""
    cfg = {"configurable": {"thread_id": thread_id}}
    result = graph.invoke(Command(resume=decision), cfg)
    return {"status": "done", "sent": result["sent"]}
```

---

## 6.10 Subgraph 与组合

当 Agent 变得复杂时，把所有节点塞在一个图里会难以维护。**Subgraph（子图）** 允许我们把一个子流程封装成独立模块。

### 6.10.1 Subgraph 的两种使用方式

**方式 1：作为节点**

```python
subgraph = builder.compile()

# 把整个 subgraph 当成节点
parent_builder.add_node("research", subgraph)
parent_builder.add_edge("research", "write")
```

**方式 2：手动调用**

```python
def call_subgraph(state):
    result = subgraph.invoke({"query": state["query"]})
    return {"research_result": result["summary"]}

parent_builder.add_node("research", call_subgraph)
```

### 6.10.2 State 共享与隔离

- **方式 1（作为节点）**：Subgraph 接收父图的 State，运行后写回父图 State
- **方式 2（手动调用）**：State 完全隔离，需要手动传参和回填

### 6.10.3 完整代码："研究 + 写作"二阶段 Agent

```python
# subgraph_research_write.py
"""
父图：研究 → 写作
  ├─ 子图 1：研究子图（多轮搜索）
  └─ 子图 2：写作子图（大纲 → 段落 → 校对）
"""
import os
from typing import TypedDict, Annotated, List
from langchain_openai import ChatOpenAI
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages

os.environ["OPENAI_API_KEY"] = "sk-xxx"
llm = ChatOpenAI(model="gpt-4o-mini")

# ========== 子图 1: 研究 ==========
class ResearchState(TypedDict):
    topic: str
    sources: Annotated[List[str], lambda a, b: a + b]  # 累加
    summary: str

def search_node(state: ResearchState) -> dict:
    topic = state["topic"]
    # 模拟搜索（生产环境调真实搜索 API）
    sources = [
        f"关于「{topic}」的来源 A：核心观点是...",
        f"关于「{topic}」的来源 B：数据显示...",
    ]
    return {"sources": sources}

def summarize_node(state: ResearchState) -> dict:
    joined = "\n".join(state["sources"])
    summary = llm.invoke(f"总结以下资料:\n{joined}").content
    return {"summary": summary}

research_builder = StateGraph(ResearchState)
research_builder.add_node("search", search_node)
research_builder.add_node("summarize", summarize_node)
research_builder.add_edge(START, "search")
research_builder.add_edge("search", "summarize")
research_builder.add_edge("summarize", END)
research_subgraph = research_builder.compile()

# ========== 子图 2: 写作 ==========
class WriteState(TypedDict):
    topic: str
    outline: str
    content: str
    polished: str

def outline_node(state: WriteState) -> dict:
    out = llm.invoke(f"为「{state['topic']}」写一个 3 段大纲").content
    return {"outline": out}

def write_node(state: WriteState) -> dict:
    content = llm.invoke(
        f"按大纲写正文:\n{state['outline']}"
    ).content
    return {"content": content}

def polish_node(state: WriteState) -> dict:
    polished = llm.invoke(
        f"润色以下文本，保持原意:\n{state['content']}"
    ).content
    return {"polished": polished}

write_builder = StateGraph(WriteState)
write_builder.add_node("outline", outline_node)
write_builder.add_node("write", write_node)
write_builder.add_node("polish", polish_node)
write_builder.add_edge(START, "outline")
write_builder.add_edge("outline", "write")
write_builder.add_edge("write", "polish")
write_builder.add_edge("polish", END)
write_subgraph = write_builder.compile()

# ========== 父图 ==========
class ParentState(TypedDict):
    topic: str
    research_result: str       # 来自 research 子图
    final_article: str         # 来自 write 子图

def run_research(state: ParentState) -> dict:
    """调用 research 子图"""
    result = research_subgraph.invoke({
        "topic": state["topic"],
        "sources": [],
        "summary": ""
    })
    return {"research_result": result["summary"]}

def run_write(state: ParentState) -> dict:
    """调用 write 子图"""
    result = write_subgraph.invoke({
        "topic": state["topic"],
        "outline": "",
        "content": "",
        "polished": ""
    })
    return {"final_article": result["polished"]}

parent_builder = StateGraph(ParentState)
parent_builder.add_node("research", run_research)
parent_builder.add_node("write", run_write)
parent_builder.add_edge(START, "research")
parent_builder.add_edge("research", "write")
parent_builder.add_edge("write", END)
parent_graph = parent_builder.compile()

# ========== 运行 ==========
if __name__ == "__main__":
    result = parent_graph.invoke({
        "topic": "LangGraph 持久化机制",
        "research_result": "",
        "final_article": ""
    })
    print("=" * 60)
    print("最终文章:")
    print("=" * 60)
    print(result["final_article"][:500])
```

### 6.10.4 Subgraph 结构图

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart TB
    subgraph Parent[父图]
        A([START]) --> B[research 节点]
        B --> C[write 节点]
        C --> D([END])
    end
    subgraph Research[研究子图]
        R1[search] --> R2[summarize]
    end
    subgraph Write[写作子图]
        W1[outline] --> W2[write] --> W3[polish]
    end
    B -.->|invoke| Research
    C -.->|invoke| Write
    classDef parent fill:#1e3a8a,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef child fill:#14532d,stroke:#7c2d12,stroke-width:2px,color:#fff
    class A,B,C,D parent
    class R1,R2,W1,W2,W3 child
```

**Subgraph 设计原则**：

1. **高内聚**：一个 Subgraph 只做一件事（研究、写作、审核）
2. **低耦合**：Subgraph 之间通过父图 State 通信
3. **可复用**：相同 Subgraph 可以在不同父图中使用
4. **独立测试**：每个 Subgraph 可以单独测试

---

## 6.11 Streaming 与 Token 流式输出

生产环境的 LLM 应用必须支持流式输出——用户看到一个字一个字"打字"出来，体验远比等 3 秒后整体呈现要好。

### 6.11.1 三种流式模式

| 模式 | 输出 | 适用场景 |
| --- | --- | --- |
| `values` | 每步执行后的完整 State | 调试、状态可视化 |
| `updates` | 每步的 **delta**（只返回变化字段） | 增量更新 UI |
| `messages` | LLM 输出的 token 流 | 实时打字机效果 |
| `events` | 细粒度事件（含工具调用、错误等） | 高级监控 |

### 6.11.2 完整代码：三种 streaming

```python
# streaming_demo.py
import os
import asyncio
from typing import TypedDict, Annotated, List
from langchain_openai import ChatOpenAI
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages

os.environ["OPENAI_API_KEY"] = "sk-xxx"
llm = ChatOpenAI(model="gpt-4o-mini", streaming=True)

class State(TypedDict):
    messages: Annotated[List, add_messages]
    counter: int

def node_a(state: State) -> dict:
    return {"messages": [llm.invoke(f"用一句话介绍 LangGraph")], "counter": 1}

def node_b(state: State) -> dict:
    return {"messages": [llm.invoke(f"用一句话介绍 ReAct")], "counter": 2}

builder = StateGraph(State)
builder.add_node("a", node_a)
builder.add_node("b", node_b)
builder.add_edge(START, "a")
builder.add_edge("a", "b")
builder.add_edge("b", END)
graph = builder.compile()

# ---- 模式 1: values（完整状态）----
print("=" * 40, "\n模式 1: stream_mode='values'")
for chunk in graph.stream({"messages": [], "counter": 0}, stream_mode="values"):
    print(f"  chunk: counter={chunk.get('counter')}, msgs={len(chunk.get('messages', []))}")

# ---- 模式 2: updates（增量）----
print("\n", "=" * 40, "\n模式 2: stream_mode='updates'")
for chunk in graph.stream({"messages": [], "counter": 0}, stream_mode="updates"):
    print(f"  delta: {list(chunk.keys())} → {chunk}")

# ---- 模式 3: messages（token 级）----
print("\n", "=" * 40, "\n模式 3: stream_mode='messages'")
for msg, metadata in graph.stream(
    {"messages": [], "counter": 0},
    stream_mode="messages"
):
    if msg.content:
        print(msg.content, end="", flush=True)
print()
```

### 6.11.3 异步流式 + 打字机效果

```python
# async_stream.py
import asyncio
from langchain_core.messages import HumanMessage

async def typewriter():
    cfg = {"configurable": {"thread_id": "user_1"}}
    print("AI: ", end="", flush=True)
    async for event in graph.astream_events(
        {"messages": [HumanMessage(content="写一首诗")]},
        config=cfg,
        version="v2"
    ):
        kind = event["event"]
        if kind == "on_chat_model_stream":
            chunk = event["data"]["chunk"]
            if chunk.content:
                print(chunk.content, end="", flush=True)
                await asyncio.sleep(0.02)  # 模拟打字速度
    print()

asyncio.run(typewriter())
```

**输出（实时滚动）**：

```
AI: 千
    千 锤
    千 锤 万
    千 锤 万 凿
    ...（最终完整输出）
```

### 6.11.4 集成到 Web（FastAPI + SSE）

```python
# sse_endpoint.py
from fastapi import FastAPI
from fastapi.responses import StreamingResponse

app = FastAPI()

@app.post("/chat/stream")
async def chat_stream(message: str, thread_id: str = "default"):
    async def event_generator():
        cfg = {"configurable": {"thread_id": thread_id}}
        async for msg, meta in graph.astream(
            {"messages": [HumanMessage(content=message)]},
            config=cfg,
            stream_mode="messages"
        ):
            if msg.content:
                # SSE 格式：data: <内容>\n\n
                yield f"data: {msg.content}\n\n"
        yield "data: [DONE]\n\n"
    return StreamingResponse(event_generator(), media_type="text/event-stream")
```

前端用 `EventSource` 接收：

```javascript
const source = new EventSource('/chat/stream?message=你好');
source.onmessage = (e) => {
  if (e.data === '[DONE]') source.close();
  else document.getElementById('output').innerText += e.data;
};
```

---

## 6.12 调试与可视化

### 6.12.1 自动生成 Mermaid 图

LangGraph 有一个非常贴心的能力——**自动把图渲染成 mermaid 代码**：

```python
# visualize.py
mermaid_code = graph.get_graph().draw_mermaid()
print(mermaid_code)

# 输出示例：
# flowchart TD
#     START --> agent
#     agent --> tools
#     agent --> END
#     tools --> agent

# 还可以直接保存为图片
png_bytes = graph.get_graph().draw_mermaid_png()
with open("graph.png", "wb") as f:
    f.write(png_bytes)
```

### 6.12.2 LangSmith 集成（一行代码）

```python
import os
os.environ["LANGSMITH_TRACING"] = "true"
os.environ["LANGSMITH_API_KEY"] = "lsv2_xxx"
os.environ["LANGSMITH_PROJECT"] = "my-agent"

# 这样写一行，所有 graph 调用都会自动上报到 LangSmith
graph = builder.compile()
```

LangSmith 提供：

- 每次 `invoke` 的完整 trace
- 每个节点的输入/输出
- LLM 调用的 token 数、耗时
- 错误堆栈

### 6.12.3 常见错误与排查

| 错误现象 | 原因 | 解决 |
| --- | --- | --- |
| `GraphRecursionError` | 节点循环没有退出条件 | 在条件边加 `step` 上限 |
| `KeyError: 'xxx'` | 节点读 state 不存在的字段 | 用 `state.get("xxx", default)` |
| `InvalidUpdateError` | 节点返回了未在 State 定义的字段 | 检查 State TypedDict |
| 状态不累加 | 忘了加 `Annotated[list, add_messages]` | State 字段加 Reducer |
| Checkpoint 不生效 | 编译时没传 `checkpointer=` | `compile(checkpointer=...)` |
| `interrupt` 不暂停 | 用了 `invoke` 而非 stream | HITL 必须用 stream 配合 |

### 6.12.4 调试技巧

```python
# 1. 加 print 看状态流转
def my_node(state: State) -> dict:
    print(f"[my_node] enter: {state}")
    result = do_something(state)
    print(f"[my_node] exit: {result}")
    return result

# 2. 用 stream 看中间过程
for chunk in graph.stream(input, stream_mode="updates"):
    print(chunk)

# 3. 看 State 历史
history = list(graph.get_state_history(cfg))
for h in history:
    print(h.next, h.values)

# 4. 单独测试节点
from langgraph.graph import StateGraph
test_graph = StateGraph(State)
test_graph.add_node("a", my_node)
test_graph.add_edge(START, "a")
test_graph.add_edge("a", END)
result = test_graph.compile().invoke(test_input)
```

---

## 6.13 工程实战：构建一个生产级研究 Agent

我们将所有概念综合到一个 **生产级研究 Agent** 中。

### 6.13.1 需求

> 用户提问 → 拆解为子问题 → 多轮搜索 → 综合分析 → 写初稿报告 → **人工审核** → 终稿或修改

### 6.13.2 完整代码（200+ 行）

```python
# research_agent.py
"""
生产级研究 Agent：综合应用 State/Node/Edge/Checkpointer/HITL/Streaming/Subgraph
"""
import os
import json
from typing import TypedDict, Annotated, List, Literal
from langchain_openai import ChatOpenAI
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage
from langgraph.graph import StateGraph, START, END
from langgraph.graph.message import add_messages
from langgraph.checkpoint.sqlite import SqliteSaver
from langgraph.types import interrupt, Command

os.environ["OPENAI_API_KEY"] = "sk-xxx"
os.environ["LANGSMITH_TRACING"] = "true"
os.environ["LANGSMITH_PROJECT"] = "research-agent"

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.3)

# ========== 1. State 定义 ==========
class ResearchState(TypedDict):
    # 用户输入
    user_question: str

    # 拆解后的子问题
    sub_questions: Annotated[List[str], lambda a, b: a + b]

    # 多轮搜索结果
    search_results: Annotated[List[dict], lambda a, b: a + b]

    # 报告草稿
    draft_report: str

    # 人工审核状态
    human_approved: bool
    human_feedback: str

    # 终稿
    final_report: str

    # 步骤计数（防无限循环）
    step: int

    # 消息日志
    messages: Annotated[List[BaseMessage], add_messages]

# ========== 2. 工具（模拟） ==========
def mock_search(query: str) -> str:
    """模拟搜索引擎（生产环境替换为 Tavily / SerpAPI）"""
    return f"[搜索结果] 关于「{query}」找到 3 篇相关文档：..."

# ========== 3. 节点定义 ==========
def decompose_question(state: ResearchState) -> dict:
    """把用户问题拆成 2-3 个子问题"""
    print(f"\n[Step {state['step']}] 拆解问题")
    prompt = f"""把以下问题拆成 2-3 个可独立研究的子问题，每行一个：
问题: {state['user_question']}

子问题:"""
    resp = llm.invoke(prompt)
    subs = [s.strip() for s in resp.content.strip().split("\n") if s.strip()]
    return {
        "sub_questions": subs,
        "step": state["step"] + 1,
        "messages": [AIMessage(content=f"拆解为 {len(subs)} 个子问题")]
    }

def research_node(state: ResearchState) -> dict:
    """对每个子问题执行搜索"""
    print(f"[Step {state['step']}] 研究子问题")
    new_results = []
    subs_to_process = state["sub_questions"][:3]   # 限制最多 3 个

    for sub in subs_to_process:
        result = mock_search(sub)
        new_results.append({"question": sub, "content": result})

    return {
        "search_results": new_results,
        "step": state["step"] + 1,
        "messages": [AIMessage(content=f"完成 {len(new_results)} 项搜索")]
    }

def analyze_node(state: ResearchState) -> dict:
    """综合分析搜索结果"""
    print(f"[Step {state['step']}] 综合分析")
    joined = "\n\n".join(
        f"Q: {r['question']}\nA: {r['content']}"
        for r in state["search_results"]
    )
    prompt = f"""基于以下研究资料，撰写一份结构化的分析报告（Markdown 格式）:
资料:
{joined}

原问题: {state['user_question']}

报告:"""
    report = llm.invoke(prompt).content
    return {
        "draft_report": report,
        "step": state["step"] + 1,
        "messages": [AIMessage(content="完成分析报告")]
    }

def human_review_node(state: ResearchState) -> dict:
    """人工审核：批准 / 修改 / 拒绝"""
    print(f"[Step {state['step']}] 等待人工审核")
    decision = interrupt({
        "type": "review",
        "draft": state["draft_report"]
    })
    # decision: "approve" | "reject" | "edit:<new_content>"
    if decision == "approve":
        return {
            "human_approved": True,
            "human_feedback": "",
            "step": state["step"] + 1
        }
    elif decision.startswith("edit:"):
        new_content = decision[5:]
        return {
            "draft_report": new_content,
            "human_approved": True,
            "human_feedback": "已编辑",
            "step": state["step"] + 1
        }
    else:  # reject
        return {
            "human_approved": False,
            "human_feedback": decision,
            "step": state["step"] + 1
        }

def finalize_node(state: ResearchState) -> dict:
    """生成终稿"""
    print(f"[Step {state['step']}] 生成终稿")
    final = llm.invoke(
        f"将以下报告整理为最终版（修辞润色、格式统一）:\n{state['draft_report']}"
    ).content
    return {
        "final_report": final,
        "step": state["step"] + 1,
        "messages": [AIMessage(content="报告已完成")]
    }

# ========== 4. 路由函数 ==========
def after_review(state: ResearchState) -> Literal["finalize", "analyze"]:
    """审核后：批准就定稿，不批准就重新分析"""
    if state["human_approved"]:
        return "finalize"
    else:
        return "analyze"

def should_decompose_more(state: ResearchState) -> Literal["research", END]:
    """拆解后是否需要进入研究？"""
    return "research" if state["sub_questions"] else END

# ========== 5. 构建图 ==========
builder = StateGraph(ResearchState)

builder.add_node("decompose", decompose_question)
builder.add_node("research", research_node)
builder.add_node("analyze", analyze_node)
builder.add_node("review", human_review_node)
builder.add_node("finalize", finalize_node)

# 边
builder.add_edge(START, "decompose")
builder.add_conditional_edges("decompose", should_decompose_more,
                              {"research": "research", END: END})
builder.add_edge("research", "analyze")
builder.add_edge("analyze", "review")
builder.add_conditional_edges("review", after_review,
                              {"finalize": "finalize", "analyze": "analyze"})
builder.add_edge("finalize", END)

# ========== 6. 编译（带 SQLite 持久化）==========
DB_PATH = "./research_agent.db"

def make_graph():
    with SqliteSaver.from_conn_string(DB_PATH) as memory:
        return builder.compile(checkpointer=memory)

graph = make_graph()

# ========== 7. 运行 ==========
def run_research(question: str, thread_id: str = "user_1"):
    """完整运行流程（含 HITL）"""
    cfg = {"configurable": {"thread_id": thread_id}}
    initial = {
        "user_question": question,
        "sub_questions": [],
        "search_results": [],
        "draft_report": "",
        "human_approved": False,
        "human_feedback": "",
        "final_report": "",
        "step": 0,
        "messages": [HumanMessage(content=question)]
    }

    # 第一阶段：跑到 review 时暂停
    print(f"\n{'='*60}\n问题: {question}\n{'='*60}")
    for chunk in graph.stream(initial, config=cfg, stream_mode="updates"):
        print(f"  [更新] {list(chunk.keys())}")

    # 检查是否中断
    state = graph.get_state(cfg)
    if state.next and "review" in state.next:
        print(f"\n{'='*60}\n>>> 报告草稿已生成，等待审核...\n{'='*60}")
        print(state.values["draft_report"][:400] + "...")

        # 模拟人工审核（实际场景：用户在前端输入）
        user_decision = input("\n请输入决策 (approve / reject / edit:新内容): ").strip()
        if not user_decision:
            user_decision = "approve"

        # 第二阶段：恢复执行
        for chunk in graph.stream(Command(resume=user_decision),
                                  config=cfg, stream_mode="updates"):
            print(f"  [更新] {list(chunk.keys())}")

    # 输出最终结果
    final = graph.get_state(cfg)
    print(f"\n{'='*60}\n>>> 最终报告:\n{'='*60}")
    print(final.values["final_report"][:600] + "...")

if __name__ == "__main__":
    run_research("LangGraph 的 Checkpointer 机制是如何实现的？")
```

### 6.13.3 完整架构图

```mermaid
%%{init: {'theme':'dark', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart TD
    A([START]) --> B[decompose<br/>拆解问题]
    B --> C[research<br/>多轮搜索]
    C --> D[analyze<br/>综合分析]
    D --> E{{review<br/>人工审核}}
    E -->|approve| F[finalize<br/>生成终稿]
    E -->|reject| D
    F --> G([END])
    H[(SQLite<br/>Checkpointer)] -.-> B
    H -.-> C
    H -.-> D
    H -.-> E
    I[LangSmith<br/>全链路追踪] -.-> B
    classDef startEnd fill:#581c87,stroke:#7f1d1d,stroke-width:2px,color:#fff
    classDef process fill:#1e3a8a,stroke:#7c2d12,stroke-width:2px,color:#fff
    classDef human fill:#7c2d12,stroke:#1e3a8a,stroke-width:2px,color:#fff
    classDef infra fill:#14532d,stroke:#7c2d12,stroke-width:2px,color:#fff
    class A,G startEnd
    class B,C,D,F process
    class E human
    class H,I infra
```

### 6.13.4 运行示例

```
$ python research_agent.py

============================================================
问题: LangGraph 的 Checkpointer 机制是如何实现的？
============================================================
  [更新] ['decompose']
  [更新] ['research']
  [更新] ['analyze']
  [更新] ['review']

============================================================
>>> 报告草稿已生成，等待审核...
============================================================
# LangGraph Checkpointer 机制分析

## 1. 核心概念
Checkpointer 是 LangGraph 的状态持久化层...

请输入决策 (approve / reject / edit:新内容): approve
  [更新] ['finalize']

============================================================
>>> 最终报告:
============================================================
# LangGraph Checkpointer 机制深度分析

## 一、什么是 Checkpointer
Checkpointer 是 LangGraph 内置的状态持久化组件...
```

### 6.13.5 生产化清单

把这个研究 Agent 推到生产还需要：

| 项目 | 方案 |
| --- | --- |
| 真实搜索 | 集成 Tavily / SerpAPI / Brave Search |
| 真实 LLM | 替换为生产模型（GPT-4o / Claude 3.5） |
| 持久化 | PostgreSQL Checkpointer |
| 监控 | LangSmith 完整 trace |
| 前端 | Next.js + 流式 SSE |
| 异步队列 | Celery 处理长任务 |
| 限流 | API 网关层 rate limit |
| 鉴权 | JWT + 用户级 thread_id |
| 成本控制 | Token 用量监控 + 报警 |
| 测试 | 单元测试 + E2E + 评估集 |

---

## 本章小结

本章我们系统学习了 LangGraph 的全部核心概念：

1. **6.1**：理解 LangGraph 相对 LangChain Chain 的优势，以及在生产环境的广泛采用
2. **6.2**：掌握 State、Node、Edge、Graph、Compile、Checkpointer 七个核心概念
3. **6.3**：从 Hello World 跑通完整流程
4. **6.4**：State 的三种定义方式、Reducer 机制、`add_messages` 内置 Reducer
5. **6.5**：Node 的同步/异步实现、错误处理与重试
6. **6.6**：普通边、条件边、循环、复杂控制流
7. **6.7**：完整 ReAct Agent（80+ 行代码）
8. **6.8**：Checkpointer 三种实现、thread_id、Time Travel
9. **6.9**：HITL 三种模式（Approval / Editing / Input）
10. **6.10**：Subgraph 封装与组合
11. **6.11**：三种流式模式 + 异步 token 流
12. **6.12**：自动 mermaid 可视化、LangSmith 集成、常见错误排查
13. **6.13**：生产级研究 Agent（200+ 行综合实战）

**关键收获**：

- LangGraph 把 Agent 抽象为 **状态机**，每个节点是纯函数
- **循环 + Checkpointer + HITL** 是生产 Agent 的三大支柱
- 优先用 **TypedDict + Reducer** 设计 State
- Subgraph 让你能 **像搭积木一样** 构建复杂 Agent
- Streaming 是用户体验的关键，**必须掌握**

## 下一章预告：第 7 章 多 Agent 协作

单 Agent 能力有上限。第 7 章我们将探索 **多 Agent 协作**：

- **Supervisor 模式**：一个调度 Agent 管理多个 Worker
- **Swarm 模式**：Agent 之间动态交接
- **Hierarchical 模式**：分层 Agent 树
- **CrewAI / AutoGen 横向对比**
- **实战项目**：构建一个多 Agent 团队完成"市场调研 → 数据分析 → 报告撰写"全流程

我们下一章见！
