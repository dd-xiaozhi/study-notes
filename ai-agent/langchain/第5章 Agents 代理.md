# 第5章：LangChain Agent（智能代理）

## 5.1 Agent 的概念与原理

### 什么是 Agent？

Agent（智能代理）是 LangChain 框架中最核心的概念之一。简单来说，Agent 是一种能够**自主决策**、**调用工具**、**与环境交互**的智能系统。与传统的固定流程程序不同，Agent 能够根据输入和当前状态动态决定下一步行动。

### Agent 与传统程序的区别

| 特性 | 传统程序 | Agent |
|------|---------|-------|
| 决策方式 | 硬编码的 if-else | 动态推理 |
| 工具调用 | 固定调用顺序 | 按需调用 |
| 状态管理 | 预设状态机 | 自主维护 |
| 错误处理 | 预定义处理 | 自主发现和恢复 |

### Agent 的核心组件

```
┌─────────────────────────────────────────────────────────┐
│                        Agent                             │
├─────────────────────────────────────────────────────────┤
│  ┌─────────┐    ┌─────────┐    ┌─────────┐              │
│  │  LLM    │    │  Tools  │    │ Memory  │              │
│  │ (大脑)  │    │ (四肢)  │    │ (记忆)  │              │
│  └────┬────┘    └────┬────┘    └────┬────┘              │
│       │              │              │                    │
│       └──────────────┼──────────────┘                    │
│                      │                                   │
│              ┌───────┴───────┐                           │
│              │  AgentExecutor │                          │
│              │  (执行引擎)    │                          │
│              └───────────────┘                           │
└─────────────────────────────────────────────────────────┘
```

### Agent 工作原理

Agent 的核心工作流程可以概括为 **思考-行动-观察** 的循环：

1. **思考 (Think)**：分析当前情况，决定下一步行动
2. **行动 (Act)**：调用工具或生成响应
3. **观察 (Observe)**：获取行动结果，更新状态
4. **重复**：直到任务完成或达到最大迭代次数

---

## 5.2 ReAct 模式（Reasoning + Acting）

### ReAct 概述

ReAct 是一种将**推理 (Reasoning)** 和**行动 (Acting)** 相结合的 Agent 架构模式。它由清华大学和 Google Research 在 2022 年提出，旨在让 Agent 能够显式地进行逻辑推理，同时能够与外部环境交互。

### ReAct 的核心思想

传统的 LLM 只是基于 prompt 生成文本，而 ReAct 模式让 LLM 能够：

1. **显式推理**：在采取行动前，先进行逻辑推理
2. **交错执行**：将推理步骤和行动步骤交错进行
3. **自我纠错**：根据行动结果调整推理

### ReAct 提示词模板

```python
from langchain import PromptTemplate

react_prompt = PromptTemplate.from_template("""
你是一个智能助手，能够使用工具来完成任务。

你可以使用以下工具：
- search: 搜索信息
- calculator: 进行数学计算
- wikipedia: 查询维基百科

对于每个问题，你必须按照以下格式回答：

问题：{question}

思考：{your_thought_process}
行动：{action_to_take}
观察：{result_of_the_action}

... (这个循环可能重复多次)

最终答案：{final_answer}

请开始回答问题。
""")
```

### ReAct 执行示例

```python
# 完整的 ReAct 执行流程示例
from langchain.agents import initialize_agent, Tool
from langchain.llms import OpenAI
from langchain.utilities import SerpAPIWrapper

# 定义工具
tools = [
    Tool(
        name="搜索",
        func=SerpAPIWrapper().run,
        description="用于搜索最新信息或不确定的事实"
    ),
    Tool(
        name="计算器",
        func=lambda x: eval(x),
        description="用于数学计算，输入为数学表达式"
    )
]

# 初始化 Agent
llm = OpenAI(temperature=0)
agent = initialize_agent(
    tools, 
    llm, 
    agent="zero-shot-react-description",  # ReAct 模式
    verbose=True
)

# 执行查询
result = agent.run("北京的面积是多少平方公里？除以深圳的面积是多少？")
```

### ReAct 的优势与局限

**优势**：
- 推理过程透明可追溯
- 能够处理复杂的多步骤任务
- 自我纠错能力强

**局限**：
- 迭代次数可能较多
- 对 LLM 的推理能力依赖较高
- 可能陷入重复循环

---

## 5.3 Tool 的概念与定义

### 什么是 Tool？

Tool（工具）是 Agent 与外部世界交互的桥梁。在 LangChain 中，Tool 是一个封装了特定功能的类，Agent 可以调用这些工具来获取信息、执行操作或处理数据。

### Tool 的结构

```python
from langchain.tools import Tool
from pydantic import BaseModel

# 简单的 Tool 定义
tool = Tool(
    name="工具名称",
    func=执行函数,
    description="工具的描述，说明何时使用"
)
```

### 完整的 Tool 定义示例

```python
from langchain.tools import Tool
from langchain.pydantic_v1 import BaseModel, Field
from typing import Optional

# 定义工具输入模式
class SearchInput(BaseModel):
    query: str = Field(description="搜索查询关键词")

# 定义工具函数
def search_function(query: str) -> str:
    """执行搜索并返回结果"""
    # 这里可以是 API 调用、数据库查询等
    return f"搜索 '{query}' 的结果：..."

# 创建带输入模式的工具
search_tool = Tool(
    name="网络搜索",
    description="当需要查找最新信息或不确定的事实时使用",
    func=search_function,
    args_schema=SearchInput
)
```

### 内置工具

LangChain 提供了丰富的内置工具：

```python
from langchain.agents import load_tools

# 加载预定义工具
tools = load_tools(
    ["serpapi", "python_repl", "ddg-search"],  # 工具名称列表
    llm=llm  # 部分工具需要 LLM
)
```

常用内置工具：
- `serpapi`: 使用 SerpAPI 进行网络搜索
- `ddg-search`: 使用 DuckDuckGo 进行搜索
- `python_repl`: 执行 Python 代码
- `wikipedia`: 查询维基百科
- `requests`: 发送 HTTP 请求

### 自定义工具

```python
from langchain.tools import Tool
from datetime import datetime

def get_current_time(format: str = "%Y-%m-%d %H:%M:%S") -> str:
    """获取当前时间"""
    return datetime.now().strftime(format)

def convert_currency(amount: float, from_currency: str, to_currency: str) -> dict:
    """货币转换"""
    # 简化的转换逻辑
    rates = {"USD_TO_CNY": 7.2, "CNY_TO_USD": 0.14}
    key = f"{from_currency}_TO_{to_currency}"
    rate = rates.get(key, 1.0)
    return {"amount": amount, "result": amount * rate, "currency": to_currency}

# 创建工具
time_tool = Tool(
    name="获取当前时间",
    description="获取当前的日期和时间",
    func=get_current_time
)

currency_tool = Tool(
    name="货币转换",
    description="将一种货币转换为另一种货币",
    func=convert_currency
)
```

---

## 5.4 Agent 类型详解

### 5.4.1 ConversationalAgent

#### 概述

ConversationalAgent 是专为对话场景设计的 Agent，它能够：
- 保持对话上下文
- 适当地使用工具回答问题
- 生成自然、连贯的对话响应

#### 使用示例

```python
from langchain.agents import initialize_agent, ConversationalAgent
from langchain.llms import OpenAI
from langchain.tools import Tool

# 定义工具
tools = [
    Tool(
        name="搜索",
        func=lambda x: f"搜索结果：关于 '{x}' 的信息",
        description="搜索信息"
    ),
    Tool(
        name="计算器",
        func=lambda x: str(eval(x)),
        description="数学计算"
    )
]

# 初始化对话 Agent
llm = OpenAI(temperature=0.7)
agent = ConversationalAgent.from_llm_and_tools(
    llm=llm,
    tools=tools,
    verbose=True
)

# 创建 AgentExecutor
from langchain.agents import AgentExecutor
agent_executor = AgentExecutor.from_agent_and_tools(
    agent=agent,
    tools=tools,
    verbose=True,
    max_iterations=10
)

# 对话交互
response = agent_executor.run("你好！请问深圳的人口是多少？")
print(response)

# 继续对话
response = agent_executor.run("谢谢你！能再告诉我它的面积吗？")
print(response)
```

#### 特点

- **对话友好**：生成自然语言响应
- **记忆上下文**：能够记住之前的对话内容
- **选择性工具使用**：不会每个问题都调用工具

---

### 5.4.2 ReActAgent

#### 概述

ReActAgent 是实现 ReAct 推理模式的 Agent，它通过显式的思考-行动-观察循环来解决问题。

#### 使用示例

```python
from langchain.agents import initialize_agent, AgentType
from langchain.llms import OpenAI
from langchain.tools import Tool

# 定义工具
tools = [
    Tool(
        name="搜索",
        func=lambda x: f"关于 '{x}' 的搜索结果...",
        description="搜索最新信息"
    ),
    Tool(
        name="百科",
        func=lambda x: f"'{x}' 的百科条目内容...",
        description="查询百科知识"
    ),
    Tool(
        name="计算器",
        func=lambda x: str(eval(x)),
        description="数学计算，接收数学表达式"
    )
]

# 初始化 ReAct Agent
llm = OpenAI(temperature=0)
agent = initialize_agent(
    tools,
    llm,
    agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION,
    verbose=True,
    max_iterations=10,
    max_execution_time=60
)

# 执行复杂任务
question = """
问题：一个电商网站的日均访问量是 10000 次，转化率是 2.5%，
平均客单价是 150 元。计算该网站每日的预估收入是多少？
"""

result = agent.run(question)
print(result)
```

#### 输出解析

ReActAgent 的 verbose 输出会显示完整的思考过程：

```
> Entering new AgentExecutor chain...
 Thought: 我需要计算每日收入。
         每日收入 = 日访问量 × 转化率 × 客单价
         = 10000 × 0.025 × 150
 Action: 计算器
 Action Input: 10000 * 0.025 * 150
 Observation: 37500.0
 Thought: 计算完成，每日预估收入是 37500 元。
 Final Answer: 该电商网站每日的预估收入约为 37,500 元人民币。
```

---

### 5.4.3 OpenAIFunctionsAgent

#### 概述

OpenAIFunctionsAgent 是专为 OpenAI 的 function calling 功能设计的 Agent。它利用 OpenAI API 的函数调用能力，能够更可靠地选择和执行工具。

#### 使用示例

```python
from langchain.agents import initialize_agent, AgentType
from langchain.llms import OpenAI
from langchain.chat_models import ChatOpenAI
from langchain.tools import Tool
from langchain.schema import FunctionDefinition

# 定义 OpenAI 函数格式的工具
functions = [
    {
        "name": "get_weather",
        "description": "获取指定城市的天气信息",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {
                    "type": "string",
                    "description": "城市名称，如：北京、上海"
                },
                "unit": {
                    "type": "string",
                    "enum": ["celsius", "fahrenheit"],
                    "description": "温度单位"
                }
            },
            "required": ["city"]
        }
    },
    {
        "name": "search_flights",
        "description": "搜索航班信息",
        "parameters": {
            "type": "object",
            "properties": {
                "origin": {"type": "string", "description": "出发城市"},
                "destination": {"type": "string", "description": "目的城市"},
                "date": {"type": "string", "description": "出发日期 YYYY-MM-DD"}
            },
            "required": ["origin", "destination"]
        }
    }
]

# 模拟的工具函数
def get_weather(city: str, unit: str = "celsius") -> str:
    weather_data = {"北京": "晴 25°C", "上海": "多云 28°C", "深圳": "雷阵雨 30°C"}
    return weather_data.get(city, "未知城市")

def search_flights(origin: str, destination: str, date: str) -> str:
    return f"从{origin}到{destination}的航班：\n1. CA1234 10:00-12:30\n2. MU5678 14:00-16:30"

# 创建工具
tools = [
    Tool(
        name="get_weather",
        func=get_weather,
        description="获取天气信息"
    ),
    Tool(
        name="search_flights",
        func=search_flights,
        description="搜索航班"
    )
]

# 使用 ChatOpenAI
chat_llm = ChatOpenAI(
    temperature=0,
    model="gpt-4",  # 或 gpt-3.5-turbo-0613
    functions=functions
)

# 初始化 Agent
agent = initialize_agent(
    tools,
    chat_llm,
    agent=AgentType.OPENAI_FUNCTIONS,
    verbose=True
)

# 执行查询
result = agent.run("北京今天的天气怎么样？")
print(result)

# 多工具调用
result = agent.run("帮我搜索从北京到上海的航班，日期是 2024年6月1日")
print(result)
```

#### 函数调用格式的优势

```
传统 ReAct 模式：
  Action: 搜索
  Action Input: "北京天气"

OpenAI Functions 模式：
  {
    "name": "get_weather",
    "arguments": {
      "city": "北京",
      "unit": "celsius"
    }
  }
```

优势：
- **结构化输出**：参数类型和格式更明确
- **更少错误**：LLM 直接输出结构化 JSON
- **类型安全**：参数类型在定义时指定

---

## 5.5 AgentExecutor 的工作流程

### 工作流程概述

AgentExecutor 是 LangChain 中负责执行 Agent 决策的核心引擎。它管理着 Agent 的整个生命周期。

### 详细工作流程

```mermaid
%%{
init: {
    theme: 'base',
    themeVariables: {
        primaryColor: '#E3F2FD',
        primaryTextColor: '#0D47A1',
        primaryBorderColor: '#1976D2',
        lineColor: '#424242',
        secondaryColor: '#F3E5F5',
        tertiaryColor: '#E8F5E9',
        backgroundColor: '#FFFFFF',
        mainBkg: '#FAFAFA',
        nodeBorder: '#1976D2',
        clusterBkg: '#FAFAFA',
        clusterBorder: '#90CAF9',
        titleColor: '#0D47A1',
        edgeLabelBackground: '#FFFFFF'
    }
}%%
classDef model fill:#E3F2FD,stroke:#1976D2,color:#0D47A1

flowchart TD
    A["开始: 输入问题"] --> B["创建 AgentExecutor"]
    B --> C{"检查迭代次数\n< max_iterations?"}
    C -->|"是"| D["Agent 生成思考和行动"]
    D --> E["提取工具和参数"]
    E --> F["执行工具调用"]
    F --> G["获取工具返回结果"]
    G --> H{"检查结果\n是最终答案?"}
    H -->|"否"| I["更新中间步骤到记忆"]
    I --> C
    H -->|"是"| J["返回最终答案"]
    C -->|"否"| J
```

### AgentExecutor 源码解析

```python
# AgentExecutor 的核心逻辑（简化版）
class AgentExecutor:
    def __init__(self, agent, tools, max_iterations=10):
        self.agent = agent
        self.tools = {tool.name: tool for tool in tools}
        self.max_iterations = max_iterations

    def _call(self, inputs):
        # 获取输入
        question = inputs["input"]

        # 初始化
        steps = []
        iterations = 0

        # 主循环
        while iterations < self.max_iterations:
            # 1. Agent 思考
            action = self.agent.plan(steps, question)

            # 2. 检查是否是最终答案
            if action.is_final:
                return action.output

            # 3. 执行工具
            if action.tool in self.tools:
                observation = self.tools[action.tool].run(action.tool_input)
            else:
                observation = f"错误：未知工具 {action.tool}"

            # 4. 记录步骤
            steps.append((action, observation))
            iterations += 1

        # 达到最大迭代次数
        return "达到最大迭代次数，任务未能完成"
```

### AgentExecutor 初始化参数

```python
from langchain.agents import AgentExecutor

executor = AgentExecutor.from_agent_and_tools(
    agent=agent,
    tools=tools,
    verbose=True,              # 是否打印详细输出
    max_iterations=10,         # 最大迭代次数
    max_execution_time=None,   # 最大执行时间（秒）
    return_intermediate_steps=True,  # 是否返回中间步骤
    handle_parsing_errors=True,     # 如何处理解析错误
    early_stopping_method="generate" # 提前停止方法
)
```

### 中间步骤访问

```python
# 获取完整的执行过程
result = agent_executor(
    "深圳的天气怎么样？",
    return_intermediate_steps=True
)

print("=== 最终答案 ===")
print(result["output"])

print("\n=== 中间步骤 ===")
for step in result["intermediate_steps"]:
    action, observation = step
    print(f"行动: {action.tool}")
    print(f"参数: {action.tool_input}")
    print(f"观察: {observation}")
    print("---")
```

---

## 5.6 Agent 决策流程详解

### 完整决策流程图

```mermaid
%%{
init: {
    theme: 'base',
    themeVariables: {
        primaryColor: '#E3F2FD',
        primaryTextColor: '#0D47A1',
        primaryBorderColor: '#1976D2',
        lineColor: '#424242',
        secondaryColor: '#F3E5F5',
        tertiaryColor: '#E8F5E9',
        backgroundColor: '#FFFFFF',
        mainBkg: '#FAFAFA',
        nodeBorder: '#1976D2',
        clusterBkg: '#FAFAFA',
        clusterBorder: '#90CAF9',
        titleColor: '#0D47A1',
        edgeLabelBackground: '#FFFFFF'
    }
}%%
classDef model fill:#E3F2FD,stroke:#1976D2,color:#0D47A1

flowchart TD
    subgraph "初始化阶段"
        A["用户输入问题"] --> B["加载 Agent 和 Tools"]
        B --> C["创建 AgentExecutor"]
    end

    subgraph "执行阶段"
        C --> D["Agent 分析问题"]
        D --> E["生成思考 Thought"]
        E --> F{"需要调用工具?"}
        F -->|"是"| G["选择工具 Select Tool"]
        G --> H["执行工具 Execute Tool"]
        H --> I["获取结果 Observation"]
        I --> J["更新记忆 Update Memory"]
        J --> D
        F -->|"否"| K["生成最终响应"]
    end

    subgraph "结束条件"
        K --> L["返回结果"]
        D --> M{"达到终止条件?"}
        M -->|"是"| K
        M -->|"否"| D
    end

    subgraph "终止条件"
        N["返回最终答案"] --> O["任务完成"]
        M -->|"max_iterations"| N
        M -->|"early_stopping"| N
        M -->|"tool_not_found"| N
    end
```

### 思考-行动循环详解

```mermaid
%%{
init: {
    theme: 'base',
    themeVariables: {
        primaryColor: '#E3F2FD',
        primaryTextColor: '#0D47A1',
        primaryBorderColor: '#1976D2',
        lineColor: '#424242',
        secondaryColor: '#F3E5F5',
        tertiaryColor: '#E8F5E9',
        backgroundColor: '#FFFFFF',
        mainBkg: '#FAFAFA',
        nodeBorder: '#1976D2',
        clusterBkg: '#FAFAFA',
        clusterBorder: '#90CAF9',
        titleColor: '#0D47A1',
        edgeLabelBackground: '#FFFFFF'
    }
}%%
classDef model fill:#E3F2FD,stroke:#1976D2,color:#0D47A1

sequenceDiagram
    participant User as "用户"
    participant Agent as "Agent (LLM)"
    participant Executor as "AgentExecutor"
    participant Tool as "Tools"
    participant Memory as "Memory"

    User->>Executor: "输入问题"
    Executor->>Agent: "发送问题"
    Agent->>Agent: "思考分析"

    loop "思考-行动循环"
        Agent->>Executor: "返回 Action (思考+工具+参数)"
        Executor->>Tool: "调用工具"
        Tool->>Tool: "执行工具逻辑"
        Tool-->>Executor: "返回结果"
        Executor->>Memory: "保存中间步骤"
        Executor->>Agent: "发送执行结果"
        Agent->>Agent: "分析结果，决定下一步"

        alt "任务完成"
            Agent->>Executor: "Final Answer"
        else "需要继续"
            Agent->>Executor: "Next Action"
        end
    end

    Executor-->>User: "返回最终答案"
```

### 工具选择决策过程

```mermaid
%%{
init: {
    theme: 'base',
    themeVariables: {
        primaryColor: '#E3F2FD',
        primaryTextColor: '#0D47A1',
        primaryBorderColor: '#1976D2',
        lineColor: '#424242',
        secondaryColor: '#F3E5F5',
        tertiaryColor: '#E8F5E9',
        backgroundColor: '#FFFFFF',
        mainBkg: '#FAFAFA',
        nodeBorder: '#1976D2',
        clusterBkg: '#FAFAFA',
        clusterBorder: '#90CAF9',
        titleColor: '#0D47A1',
        edgeLabelBackground: '#FFFFFF'
    }
}%%
classDef model fill:#E3F2FD,stroke:#1976D2,color:#0D47A1

flowchart TD
    A["问题分析"] --> B{"问题类型判断"}

    B -->|"事实查询"| C["搜索工具"]
    B -->|"数学计算"| D["计算器工具"]
    B -->|"代码执行"| E["Python解释器"]
    B -->|"知识问答"| F["百科工具"]
    B -->|"对话响应"| G["无需工具"]

    C --> H["执行搜索"]
    D --> I["执行计算"]
    E --> J["执行代码"]
    F --> K["查询百科"]

    H --> L["返回结果"]
    I --> L
    J --> L
    K --> L
    G --> L["直接生成回答"]
    L --> M["更新记忆"]
```

---

## 5.7 完整代码示例

### 示例一：基础问答 Agent

```python
"""
LangChain Agent 基础示例：智能问答系统
"""

from langchain.agents import initialize_agent, Tool
from langchain.llms import OpenAI
from langchain.memory import ConversationBufferMemory

# 1. 定义工具
def search_tool(query: str) -> str:
    """模拟搜索工具"""
    knowledge_base = {
        "python": "Python 是一种高级编程语言，由 Guido van Rossum 于 1991 年创建。",
        "java": "Java 是一种面向对象的编程语言，由 Sun Microsystems 于 1995 年发布。",
        "javascript": "JavaScript 是一种脚本语言，主要用于网页前端开发。",
    }
    return knowledge_base.get(query.lower(), f"未找到关于 '{query}' 的信息")

def calculator_tool(expression: str) -> str:
    """数学计算工具"""
    try:
        result = eval(expression)
        return f"计算结果：{result}"
    except Exception as e:
        return f"计算错误：{str(e)}"

# 创建工具列表
tools = [
    Tool(
        name="搜索",
        func=search_tool,
        description="当用户询问编程语言相关知识时使用，如 Python、Java、JavaScript 等"
    ),
    Tool(
        name="计算器",
        func=calculator_tool,
        description="当用户需要进行数学计算时使用"
    )
]

# 2. 初始化 LLM
llm = OpenAI(
    temperature=0.7,  # 创造性参数
    model_name="gpt-3.5-turbo-instruct"
)

# 3. 创建 Agent
agent = initialize_agent(
    tools=tools,
    llm=llm,
    agent="conversational-react-description",
    verbose=True,
    max_iterations=10,
    memory=ConversationBufferMemory(memory_key="chat_history", return_messages=True)
)

# 4. 运行 Agent
if __name__ == "__main__":
    print("=== 智能问答系统 ===")
    print("你可以问我关于编程语言的问题，或者进行数学计算。\n")

    # 交互式问答
    questions = [
        "Python 是什么？",
        "你能帮我计算 (15 + 25) * 3 吗？",
        "Java 和 Python 有什么区别？"
    ]

    for q in questions:
        print(f"\n问：{q}")
        print("答：", end="")
        result = agent.run(q)
        print(result)
```

### 示例二：多工具协作 Agent

```python
"""
LangChain Agent 进阶示例：旅行助手
"""

from langchain.agents import initialize_agent, Tool, AgentType
from langchain.llms import OpenAI
from langchain.pydantic_v1 import BaseModel, Field
from typing import Union, List
from datetime import datetime, timedelta

# ============ 工具定义 ============

class WeatherInput(BaseModel):
    city: str = Field(description="城市名称")

class FlightInput(BaseModel):
    origin: str = Field(description="出发城市")
    destination: str = Field(description="目的城市")
    date: str = Field(description="出发日期 YYYY-MM-DD")

def get_weather(city: str) -> str:
    """获取天气信息"""
    weather_db = {
        "北京": {"weather": "晴", "temp": "25°C", "humidity": "40%"},
        "上海": {"weather": "多云", "temp": "28°C", "humidity": "60%"},
        "深圳": {"weather": "雷阵雨", "temp": "30°C", "humidity": "75%"},
        "广州": {"weather": "晴", "temp": "32°C", "humidity": "55%"},
    }
    if city in weather_db:
        w = weather_db[city]
        return f"{city}天气：{w['weather']}，温度：{w['temp']}，湿度：{w['humidity']}"
    return f"抱歉，暂未收录 {city} 的天气信息"

def search_flights(origin: str, destination: str, date: str) -> str:
    """搜索航班"""
    flights = [
        {"airline": "国航 CA1234", "time": "10:00-12:30", "price": "¥580"},
        {"airline": "东航 MU5678", "time": "14:00-16:30", "price": "¥620"},
        {"airline": "南航 CZ9012", "time": "18:30-21:00", "price": "¥550"},
    ]
    result = f"从{origin}到{destination}的航班（{date}）：\n"
    for f in flights:
        result += f"- {f['airline']}，{f['time']}，价格：{f['price']}\n"
    return result

def hotel_recommendation(city: str, nights: int) -> str:
    """推荐酒店"""
    hotels = {
        "北京": ["北京饭店 ¥500/晚", "王府井酒店 ¥350/晚"],
        "上海": ["外滩酒店 ¥600/晚", "陆家嘴酒店 ¥450/晚"],
        "深圳": ["华侨城酒店 ¥400/晚", "南山酒店 ¥320/晚"],
    }
    result = f"{city}推荐酒店：\n"
    for h in hotels.get(city, ["暂无推荐"]):
        result += f"- {h}\n"
    result += f"建议住宿：{nights}晚，总计约 {nights * 400} 元"
    return result

# 创建工具
tools = [
    Tool(
        name="查询天气",
        func=get_weather,
        description="查询城市的天气情况",
        args_schema=WeatherInput
    ),
    Tool(
        name="搜索航班",
        func=search_flights,
        description="搜索两城市间的航班",
        args_schema=FlightInput
    ),
    Tool(
        name="推荐酒店",
        func=hotel_recommendation,
        description="推荐城市的酒店"
    )
]

# ============ 初始化 Agent ============

llm = OpenAI(temperature=0)
agent = initialize_agent(
    tools=tools,
    llm=llm,
    agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION,
    verbose=True,
    max_iterations=15
)

# ============ 执行任务 ============

if __name__ == "__main__":
    print("=== 旅行助手 ===\n")

    # 复杂多步骤任务
    tasks = [
        "帮我规划下周从北京去深圳的行程，需要：\n"
        "1. 查询北京和深圳的天气\n"
        "2. 搜索航班\n"
        "3. 推荐酒店",

        "我想去上海出差3天，帮我看看上海天气和推荐酒店"
    ]

    for task in tasks:
        print(f"\n{'='*50}")
        print(f"任务：{task}")
        print("="*50)
        result = agent.run(task)
        print(f"\n最终结果：{result}")
```

### 示例三：使用 OpenAI Functions Agent

```python
"""
OpenAI Functions Agent 示例：智能客服
"""

from langchain.agents import initialize_agent, AgentType
from langchain.chat_models import ChatOpenAI
from langchain.tools import Tool

# ============ 工具函数 ============

def create_order(product: str, quantity: int, address: str) -> dict:
    """创建订单"""
    order_id = f"ORD{datetime.now().strftime('%Y%m%d%H%M%S')}"
    return {
        "order_id": order_id,
        "product": product,
        "quantity": quantity,
        "address": address,
        "status": "created",
        "estimated_delivery": "3-5个工作日"
    }

def query_order(order_id: str) -> dict:
    """查询订单状态"""
    statuses = {
        "ORD001": {"status": "配送中", "location": "深圳分拣中心"},
        "ORD002": {"status": "已送达", "location": "客户手中"},
    }
    if order_id in statuses:
        return {"order_id": order_id, **statuses[order_id]}
    return {"order_id": order_id, "status": "未找到订单", "message": "请检查订单号是否正确"}

def get_product_info(product: str) -> dict:
    """获取产品信息"""
    products = {
        "手机": {"price": "¥3999", "stock": "有货", "description": "最新款智能手机"},
        "电脑": {"price": "¥6999", "stock": "有货", "description": "轻薄笔记本电脑"},
        "耳机": {"price": "¥599", "stock": "缺货", "description": "无线降噪耳机"},
    }
    return products.get(product, {"message": f"未找到产品：{product}"})

# ============ 创建工具 ============

tools = [
    Tool(
        name="create_order",
        func=lambda args: create_order(**args) if isinstance(args, dict) else create_order(**eval(args)),
        description="创建新订单，包含商品、数量和地址"
    ),
    Tool(
        name="query_order",
        func=query_order,
        description="根据订单号查询订单状态"
    ),
    Tool(
        name="get_product_info",
        func=get_product_info,
        description="查询产品信息、价格和库存"
    )
]

# ============ 初始化 ChatOpenAI ============

from datetime import datetime

llm = ChatOpenAI(
    temperature=0,
    model="gpt-4",
    # OpenAI Functions 支持的模型
)

# ============ 创建 Agent ============

agent = initialize_agent(
    tools=tools,
    llm=llm,
    agent=AgentType.OPENAI_FUNCTIONS,
    verbose=True
)

# ============ 执行查询 ============

if __name__ == "__main__":
    print("=== 智能客服系统 ===\n")

    queries = [
        "我想买一部手机，价格是多少？有货吗？",
        "帮我查一下订单 ORD001 的状态",
        "我要订购2台电脑，送到深圳市南山区科技园",
    ]

    for query in queries:
        print(f"\n客户：{query}")
        print("-" * 40)
        result = agent.run(query)
        print(f"客服：{result}")
```

---

## 5.8 本章小结

### 核心要点

1. **Agent 是智能代理**：能够自主决策、调用工具、与环境交互
2. **ReAct 模式**：通过思考-行动-观察循环实现复杂推理
3. **Tool 是桥梁**：连接 Agent 与外部世界
4. **多种 Agent 类型**：ConversationalAgent、ReActAgent、OpenAIFunctionsAgent
5. **AgentExecutor 管理执行**：处理循环、错误和终止条件

### 关键对比

| 特性 | ConversationalAgent | ReActAgent | OpenAIFunctionsAgent |
|------|---------------------|------------|---------------------|
| 最佳场景 | 对话系统 | 复杂推理任务 | 结构化工具调用 |
| 工具使用 | 按需使用 | 显式推理后使用 | 函数调用格式 |
| 输出格式 | 自然语言 | 思考+行动+观察 | 结构化 JSON |
| 记忆支持 | 内置 | 可选 | 可选 |

### 下一步学习

- 第6章：Memory（记忆系统）
- 第7章：Callback（回调机制）
- 第8章：Chain（链式调用）

---

## 参考资源

- [LangChain Agents 官方文档](https://python.langchain.com/docs/modules/agents/)
- [ReAct 论文](https://arxiv.org/abs/2210.03629)
- [OpenAI Function Calling](https://platform.openai.com/docs/guides/gpt-currently-supports)
