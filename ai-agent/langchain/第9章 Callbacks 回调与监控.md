# 第9章：LangChain Callbacks 回调系统

Callbacks（回调）是 LangChain 中一个非常重要的模块，它允许你在 LangChain 运行过程中的各个阶段插入自定义逻辑。本章将详细介绍 Callbacks 的概念、用途以及如何自定义和使用它们。

## 9.1 Callbacks 的概念与用途

### 什么是 Callbacks？

Callbacks 是一种设计模式，允许你将函数（处理器）传递给另一个函数，在特定的"事件"发生时自动调用这些函数。在 LangChain 中，Callbacks 用于：

- **监听事件**：在 Chain、LLM、Tool 等组件执行的不同阶段接收通知
- **日志记录**：记录请求、响应、执行时间等信息
- **监控追踪**：追踪 Token 使用量、调用成本等
- **自定义处理**：在特定事件发生时执行自定义逻辑

### Callbacks 的用途场景

```python
# 常见的 Callbacks 使用场景

# 1. 调试和日志
#    - 打印 LLM 输入输出
#    - 查看 Chain 执行流程

# 2. 成本追踪
#    - 统计 Token 使用量
#    - 计算 API 调用成本

# 3. 性能监控
#    - 测量执行时间
#    - 分析性能瓶颈

# 4. 自定义处理
#    - 将结果保存到数据库
#    - 发送通知到 Slack/邮件
#    - 记录执行轨迹用于分析
```

## 9.2 事件处理器（Callback Handler）

### Callback Handler 的结构

LangChain 的 `CallbackHandler` 是一个定义了多个方法的接口，每个方法对应一种事件类型：

| 事件类型 | 方法名 | 触发时机 |
|---------|--------|---------|
| LLM 开始 | `on_llm_start` | LLM 开始处理请求 |
| LLM 结束 | `on_llm_end` | LLM 完成处理 |
| Chain 开始 | `on_chain_start` | Chain 开始执行 |
| Chain 结束 | `on_chain_end` | Chain 完成执行 |
| Tool 开始 | `on_tool_start` | Tool 开始执行 |
| Tool 结束 | `on_tool_end` | Tool 完成执行 |
| 文本生成 | `on_text` | 生成文本内容 |
| 错误 | `on_error` | 发生错误时 |

### CallbackHandler 接口

```python
from langchain.callbacks.base import BaseCallbackHandler
from typing import Any, Optional, List, Dict, Union, Callable

class BaseCallbackHandler:
    """Callback Handler 的基类"""

    def on_llm_start(
        self,
        serialized: Dict[str, Any],
        prompts: List[str],
        **kwargs: Any
    ) -> Any:
        """LLM 开始处理时调用"""
        pass

    def on_llm_end(
        self,
        response: Any,
        **kwargs: Any
    ) -> Any:
        """LLM 完成处理时调用"""
        pass

    def on_chain_start(
        self,
        serialized: Dict[str, Any],
        inputs: Dict[str, Any],
        **kwargs: Any
    ) -> Any:
        """Chain 开始执行时调用"""
        pass

    def on_chain_end(
        self,
        outputs: Dict[str, Any],
        **kwargs: Any
    ) -> Any:
        """Chain 完成执行时调用"""
        pass

    def on_tool_start(
        self,
        serialized: Dict[str, Any],
        input_str: str,
        **kwargs: Any
    ) -> Any:
        """Tool 开始执行时调用"""
        pass

    def on_tool_end(
        self,
        output: str,
        **kwargs: Any
    ) -> Any:
        """Tool 完成执行时调用"""
        pass

    def on_text(
        self,
        text: str,
        **kwargs: Any
    ) -> Any:
        """生成文本时调用"""
        pass
```

## 9.3 LangChain 内置 Callbacks

LangChain 提供了多个内置的 Callback Handlers，可以直接使用。

### 9.3.1 ConsoleCallbackHandler

`ConsoleCallbackHandler` 将事件信息打印到控制台，适合调试使用。

```python
from langchain.callbacks import ConsoleCallbackHandler
from langchain_openai import OpenAI

# 创建 LLM
llm = OpenAI(temperature=0)

# 方式1：在调用时传递 ConsoleCallbackHandler
response = llm.invoke(
    "解释什么是人工智能",
    config={"callbacks": [ConsoleCallbackHandler()]}
)
print(response)
```

### 9.3.2 FileCallbackHandler

`FileCallbackHandler` 将事件日志写入文件，适合持久化记录。

```python
from langchain.callbacks import FileCallbackHandler
from langchain_openai import OpenAI

# 创建 FileCallbackHandler，输出到指定文件
file_handler = FileCallbackHandler("callback_log.txt")

llm = OpenAI(temperature=0)

# 使用 FileCallbackHandler
response = llm.invoke(
    "写一首关于春天的诗",
    config={"callbacks": [file_handler]}
)

# 查看日志文件内容
with open("callback_log.txt", "r", encoding="utf-8") as f:
    print(f.read())
```

### 9.3.3 StdOutCallbackHandler

`StdOutCallbackHandler` 与 `ConsoleCallbackHandler` 类似，输出到标准输出。

```python
from langchain.callbacks import StdOutCallbackHandler
from langchain.chains import LLMChain
from langchain_openai import OpenAI
from langchain.prompts import PromptTemplate

# 创建组件
llm = OpenAI(temperature=0)
prompt = PromptTemplate.from_template("把 {topic} 翻译成英文")

# 创建 Chain
chain = LLMChain(llm=llm, prompt=prompt)

# 使用 StdOutCallbackHandler
result = chain.invoke(
    {"topic": "人工智能"},
    config={"callbacks": [StdOutCallbackHandler()]}
)
print("Chain 结果:", result)
```

### 9.3.4 更多内置 Callbacks

LangChain 还提供了其他内置 Callbacks：

```python
from langchain.callbacks import (
    ConsoleCallbackHandler,
    FileCallbackHandler,
    StdOutCallbackHandler,
    StreamingStdOutCallbackHandler,  # 支持流式输出
)

# StreamingStdOutCallbackHandler 用于流式 LLM
from langchain_openai import OpenAI

llm = OpenAI(streaming=True, callbacks=[StreamingStdOutCallbackHandler()])
```

## 9.4 自定义 Callback Handler

通过继承 `BaseCallbackHandler` 或实现 `CallbackHandler` 接口，你可以创建自定义的 Callback Handler。

### 9.4.1 基础自定义 Callback

```python
from langchain.callbacks.base import BaseCallbackHandler
from langchain_openai import OpenAI
from langchain.schema import LLMResult

class MyCallbackHandler(BaseCallbackHandler):
    """自定义 Callback Handler 示例"""

    def __init__(self):
        self.llm_call_count = 0
        self.total_tokens = 0

    def on_llm_start(
        self,
        serialized: dict,
        prompts: list,
        **kwargs
    ) -> None:
        """LLM 开始时调用"""
        self.llm_call_count += 1
        print(f"[Callback] LLM 调用 #{self.llm_call_count} 开始")
        print(f"[Callback] 输入提示: {prompts[0][:50]}...")

    def on_llm_end(
        self,
        response: LLMResult,
        **kwargs
    ) -> None:
        """LLM 结束时调用"""
        # 获取 Token 使用统计
        if response.llm_output and "token_usage" in response.llm_output:
            usage = response.llm_output["token_usage"]
            self.total_tokens += usage.get("total_tokens", 0)
            print(f"[Callback] Token 使用: {usage}")
        print(f"[Callback] LLM 调用 #{self.llm_call_count} 结束")

    def on_chain_start(
        self,
        serialized: dict,
        inputs: dict,
        **kwargs
    ) -> None:
        """Chain 开始时调用"""
        print(f"[Callback] Chain 开始执行")

    def on_chain_end(
        self,
        outputs: dict,
        **kwargs
    ) -> None:
        """Chain 结束时调用"""
        print(f"[Callback] Chain 执行完成")
        print(f"[Callback] 输出: {outputs}")

    def on_tool_start(
        self,
        serialized: dict,
        input_str: str,
        **kwargs
    ) -> None:
        """Tool 开始时调用"""
        tool_name = serialized.get("name", "Unknown")
        print(f"[Callback] Tool '{tool_name}' 开始执行")
        print(f"[Callback] 输入: {input_str[:50]}...")

    def on_tool_end(
        self,
        output: str,
        **kwargs
    ) -> None:
        """Tool 结束时调用"""
        print(f"[Callback] Tool 执行完成")
        print(f"[Callback] 输出: {output[:50]}...")

    def on_text(self, text: str, **kwargs) -> None:
        """文本生成时调用"""
        print(f"[Callback] 生成文本: {text[:30]}...")

# 使用自定义 Callback
llm = OpenAI(temperature=0)
custom_callback = MyCallbackHandler()

response = llm.invoke(
    "解释量子计算的基本原理",
    config={"callbacks": [custom_callback]}
)

print(f"\n总 LLM 调用次数: {custom_callback.llm_call_count}")
print(f"总 Token 使用量: {custom_callback.total_tokens}")
```

### 9.4.2 带状态追踪的自定义 Callback

```python
import json
from datetime import datetime
from langchain.callbacks.base import BaseCallbackHandler
from langchain.schema import AgentAction, AgentFinish

class AgentCallbackHandler(BaseCallbackHandler):
    """专门用于追踪 Agent 执行的 Callback"""

    def __init__(self, trace_file: str = "agent_trace.json"):
        self.trace_file = trace_file
        self.trace = {
            "start_time": datetime.now().isoformat(),
            "events": [],
            "actions": [],
            "final_output": None
        }

    def on_chain_start(self, serialized: dict, inputs: dict, **kwargs) -> None:
        event = {
            "type": "chain_start",
            "time": datetime.now().isoformat(),
            "data": {"inputs": inputs}
        }
        self.trace["events"].append(event)
        print(f"[Agent] 开始处理输入: {inputs.get('input', '')[:50]}...")

    def on_llm_start(self, serialized: dict, prompts: list, **kwargs) -> None:
        event = {
            "type": "llm_start",
            "time": datetime.now().isoformat(),
            "data": {"prompts": prompts}
        }
        self.trace["events"].append(event)

    def on_llm_end(self, response, **kwargs) -> None:
        event = {
            "type": "llm_end",
            "time": datetime.now().isoformat(),
            "data": {"has_response": response is not None}
        }
        self.trace["events"].append(event)

    def on_tool_start(self, serialized: dict, input_str: str, **kwargs) -> None:
        tool_name = serialized.get("name", "unknown")
        event = {
            "type": "tool_start",
            "time": datetime.now().isoformat(),
            "data": {"tool": tool_name, "input": input_str}
        }
        self.trace["events"].append(event)
        self.trace["actions"].append({"tool": tool_name, "action": "start"})
        print(f"[Agent] 调用工具: {tool_name}")

    def on_tool_end(self, output: str, **kwargs) -> None:
        event = {
            "type": "tool_end",
            "time": datetime.now().isoformat(),
            "data": {"output": output}
        }
        self.trace["events"].append(event)
        self.trace["actions"].append({"tool": "result", "action": "end"})
        print(f"[Agent] 工具返回: {output[:50]}...")

    def on_chain_end(self, outputs: dict, **kwargs) -> None:
        self.trace["end_time"] = datetime.now().isoformat()
        self.trace["final_output"] = outputs
        event = {
            "type": "chain_end",
            "time": datetime.now().isoformat(),
            "data": {"outputs": outputs}
        }
        self.trace["events"].append(event)
        print(f"[Agent] 完成处理")

    def save_trace(self) -> None:
        """将追踪结果保存到文件"""
        with open(self.trace_file, "w", encoding="utf-8") as f:
            json.dump(self.trace, f, ensure_ascii=False, indent=2)
        print(f"[Agent] 追踪记录已保存到 {self.trace_file}")


# 使用示例
from langchain.agents import AgentExecutor, create_react_agent
from langchain_openai import OpenAI
from langchain.tools import Tool
from langchain import hub

# 定义一个简单的工具
def get_weather(city: str) -> str:
    """获取城市天气"""
    weather_data = {
        "北京": "晴，25°C",
        "上海": "多云，28°C",
        "东京": "雨，22°C"
    }
    return weather_data.get(city, "未知城市")

tools = [
    Tool(
        name="天气查询",
        func=get_weather,
        description="查询城市的天气信息，输入应该是城市名称"
    )
]

# 创建 Agent
llm = OpenAI(temperature=0)
prompt = hub.pull("hwchase17/react")
agent = create_react_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

# 使用自定义 Callback
callback_handler = AgentCallbackHandler("weather_agent_trace.json")

# 执行 Agent
result = agent_executor.invoke(
    {"input": "北京的天气怎么样？"},
    config={"callbacks": [callback_handler]}
)

# 保存追踪记录
callback_handler.save_trace()

print(f"\n最终输出: {result['output']}")
```

## 9.5 同步与异步 Callbacks

LangChain 支持同步和异步两种 Callback Handler。

### 9.5.1 同步 Callbacks

大多数情况下使用同步 Callbacks：

```python
from langchain.callbacks.base import BaseCallbackHandler
from langchain_openai import OpenAI

class SyncCallbackHandler(BaseCallbackHandler):
    """同步 Callback Handler"""

    def on_llm_start(self, serialized, prompts, **kwargs):
        print("同步: LLM 开始处理")

    def on_llm_end(self, response, **kwargs):
        print("同步: LLM 处理完成")

    def on_chain_start(self, serialized, inputs, **kwargs):
        print("同步: Chain 开始执行")

    def on_chain_end(self, outputs, **kwargs):
        print("同步: Chain 执行完成")

# 使用同步 Callback
llm = OpenAI(temperature=0)
result = llm.invoke(
    "你好",
    config={"callbacks": [SyncCallbackHandler()]}
)
```

### 9.5.2 异步 Callbacks

对于异步操作，需要使用异步 Callback Handler：

```python
import asyncio
from langchain.callbacks.base import BaseCallbackHandler
from langchain_openai import ChatOpenAI
from langchain.schema import LLMResult

class AsyncCallbackHandler(BaseCallbackHandler):
    """异步 Callback Handler"""

    async def on_llm_start(
        self,
        serialized: dict,
        prompts: list,
        **kwargs
    ) -> None:
        """异步 LLM 开始处理"""
        print("异步: LLM 开始处理")
        await asyncio.sleep(0.1)  # 模拟异步操作

    async def on_llm_end(
        self,
        response: LLMResult,
        **kwargs
    ) -> None:
        """异步 LLM 处理完成"""
        print("异步: LLM 处理完成")
        await asyncio.sleep(0.1)

    async def on_chain_start(
        self,
        serialized: dict,
        inputs: dict,
        **kwargs
    ) -> None:
        """异步 Chain 开始执行"""
        print("异步: Chain 开始执行")
        await asyncio.sleep(0.1)

    async def on_chain_end(
        self,
        outputs: dict,
        **kwargs
    ) -> None:
        """异步 Chain 执行完成"""
        print("异步: Chain 执行完成")
        await asyncio.sleep(0.1)

# 异步使用示例
async def main():
    chat = ChatOpenAI(model="gpt-3.5-turbo", temperature=0)
    callback = AsyncCallbackHandler()

    # 调用异步 LLM
    response = await chat.ainvoke(
        "用一句话解释机器学习",
        config={"callbacks": [callback]}
    )
    print(f"响应: {response.content}")

# 运行异步代码
asyncio.run(main())
```

### 9.5.3 混合使用同步和异步

```python
from langchain.callbacks.base import BaseCallbackHandler
from langchain_openai import OpenAI
from langchain.chains import LLMChain
from langchain.prompts import PromptTemplate

class HybridCallbackHandler(BaseCallbackHandler):
    """混合同步和异步的 Callback Handler"""

    def on_chain_start(self, serialized, inputs, **kwargs):
        """同步方法"""
        print("Chain 开始 (同步)")

    def on_chain_end(self, outputs, **kwargs):
        """同步方法"""
        print("Chain 结束 (同步)")

    async def on_llm_start(self, serialized, prompts, **kwargs):
        """异步方法"""
        print("LLM 开始 (异步)")
        await asyncio.sleep(0)

    async def on_llm_end(self, response, **kwargs):
        """异步方法"""
        print("LLM 结束 (异步)")
        await asyncio.sleep(0)

# 使用
async def run():
    llm = OpenAI(temperature=0)
    prompt = PromptTemplate.from_template("把 {text} 翻译成英文")
    chain = LLMChain(llm=llm, prompt=prompt)

    result = await chain.ainvoke(
        {"text": "你好世界"},
        config={"callbacks": [HybridCallbackHandler()]}
    )
    return result

asyncio.run(run())
```

## 9.6 在 Chain、Agent、Model 中使用 Callbacks

### 9.6.1 在 LLM 中使用 Callbacks

```python
from langchain_openai import OpenAI, ChatOpenAI
from langchain.callbacks import ConsoleCallbackHandler

# 同步 LLM
llm = OpenAI(callbacks=[ConsoleCallbackHandler()])
response = llm.invoke("解释什么是回调函数")
print(response)

# 异步 Chat LLM
chat = ChatOpenAI(
    model="gpt-3.5-turbo",
    callbacks=[ConsoleCallbackHandler()]
)

import asyncio
async def main():
    response = await chat.ainvoke("你好")
    print(response.content)

asyncio.run(main())
```

### 9.6.2 在 Chain 中使用 Callbacks

```python
from langchain.chains import LLMChain
from langchain_openai import OpenAI
from langchain.prompts import PromptTemplate
from langchain.callbacks import ConsoleCallbackHandler

# 创建 Chain
llm = OpenAI(temperature=0)
prompt = PromptTemplate.from_template(
    "请为 {product} 写一句广告语，要求不超过 20 字"
)
chain = LLMChain(llm=llm, prompt=prompt)

# 在 invoke 时传递 Callbacks
result = chain.invoke(
    {"product": "智能手表"},
    config={"callbacks": [ConsoleCallbackHandler()]}
)

print("广告语:", result["text"])
```

### 9.6.3 在 Agent 中使用 Callbacks

```python
from langchain.agents import AgentExecutor, create_react_agent
from langchain_openai import OpenAI
from langchain.tools import Tool
from langchain import hub
from langchain.callbacks import ConsoleCallbackHandler

# 定义工具
def calculator(expression: str) -> str:
    """计算数学表达式"""
    try:
        result = eval(expression)
        return f"结果: {result}"
    except Exception as e:
        return f"计算错误: {e}"

tools = [
    Tool(
        name="计算器",
        func=calculator,
        description="用于数学计算，输入是数学表达式如 '2+3*5'"
    )
]

# 创建 Agent
llm = OpenAI(temperature=0)
prompt = hub.pull("hwchase17/react")
agent = create_react_agent(llm, tools, prompt)
agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    verbose=True,
    callbacks=[ConsoleCallbackHandler()]  # 添加 Callback
)

# 执行 Agent
result = agent_executor.invoke(
    {"input": "计算 (25 + 15) * 2 的值"}
)

print("\n最终结果:", result["output"])
```

### 9.6.4 通过配置传递 Callbacks

```python
from langchain_openai import OpenAI
from langchain.callbacks import ConsoleCallbackHandler

# 方式1：直接在构造函数中传递
llm = OpenAI(callbacks=[ConsoleCallbackHandler()])

# 方式2：通过 config 字典传递
llm = OpenAI()
response = llm.invoke(
    "你好",
    config={"callbacks": [ConsoleCallbackHandler()]}
)

# 方式3：通过 run_config 传递
llm = OpenAI()
response = llm.invoke(
    "你好",
    config={"callbacks": [ConsoleCallbackHandler()]}
)

# 方式4：在 Chain 中传递
from langchain.chains import LLMChain
from langchain.prompts import PromptTemplate

chain = LLMChain(
    llm=OpenAI(),
    prompt=PromptTemplate.from_template("{question}?"),
    callbacks=[ConsoleCallbackHandler()]
)
```

## 9.7 Callback 事件流

### 9.7.1 Callback 事件流程图

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

classDef startNode fill:#E3F2FD,stroke:#1976D2,color:#0D47A1
classDef processNode fill:#BBDEFB,stroke:#1976D2,color:#0D47A1
classDef eventNode fill:#E8F5E9,stroke:#1976D2,color:#0D47A1
classDef decisionNode fill:#F3E5F5,stroke:#1976D2,color:#0D47A1
classDef endNode fill:#FFCCBC,stroke:#1976D2,color:#0D47A1

graph TD
    A["开始请求"] --> B{"选择组件类型"}
    B -->|"LLM"| C["on_llm_start"]
    B -->|"Chain"| D["on_chain_start"]
    B -->|"Tool"| E["on_tool_start"]

    C --> F["执行 LLM"]
    D --> G["执行 Chain"]
    E --> H["执行 Tool"]

    F --> I{"执行结果"}
    G --> I
    H --> I

    I -->|"LLM"| J["on_llm_end"]
    I -->|"Chain"| K["on_chain_end"]
    I -->|"Tool"| L["on_tool_end"]

    J --> M["on_text"]
    K --> M
    L --> M

    M --> N{"还有其他事件?"}
    N -->|"是"| B
    N -->|"否"| O["完成"]

    class A startNode
    class C,D,E processNode
    class F,G,H processNode
    class J,K,L eventNode
    class M eventNode
    class B,N decisionNode
    class O endNode
```

### 9.7.2 Agent 执行流程中的 Callback 事件

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

classDef participant fill:#E3F2FD,stroke:#1976D2,color:#0D47A1
classDef callbackEvent fill:#E8F5E9,stroke:#1976D2,color:#0D47A1
classDef note fill:#F3E5F5,stroke:#90CAF9,color:#0D47A1

sequenceDiagram
    participant User as "用户"
    participant Agent as "Agent"
    participant LLM as "LLM"
    participant Tool as "Tool"
    participant CB as "Callback Handler"

    User->>Agent: "执行 Agent，输入问题"
    Agent->>CB: "on_chain_start"
    Agent->>LLM: "发送提示词"
    LLM->>CB: "on_llm_start"

    Note over LLM: "LLM 思考中..."

    LLM->>CB: "on_llm_end"
    LLM-->>Agent: "返回 Action"

    alt "需要调用 Tool"
        Agent->>Tool: "执行 Tool"
        Tool->>CB: "on_tool_start"
        Note over Tool: "执行工具..."

        Tool->>CB: "on_tool_end"
        Tool-->>Agent: "返回结果"
        Agent->>LLM: "发送结果给 LLM"
        LLM->>CB: "on_llm_start"

        LLM->>CB: "on_llm_end"
        LLM-->>Agent: "返回最终响应"
    end

    Agent->>CB: "on_chain_end"
    Agent-->>User: "返回结果"

    class User,Agent,LLM,Tool participant
    class CB callbackEvent
```

### 9.7.3 Chain 执行流程

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

classDef inputNode fill:#E3F2FD,stroke:#1976D2,color:#0D47A1
classDef processNode fill:#BBDEFB,stroke:#1976D2,color:#0D47A1
classDef decisionNode fill:#F3E5F5,stroke:#1976D2,color:#0D47A1
classDef toolNode fill:#E8F5E9,stroke:#1976D2,color:#0D47A1
classDef outputNode fill:#FFCCBC,stroke:#1976D2,color:#0D47A1

graph LR
    A["输入"] --> B["Prompt Template"]
    B --> C["LLM Chain"]
    C --> D{"需要 Tool?"}
    D -->|"是"| E["Agent Executor"]
    D -->|"否"| F["直接返回"]

    E --> G["ReAct LLM"]
    G --> H["选择 Tool"]
    H --> I["执行 Tool"]
    I --> J["获取结果"]
    J --> G

    G --> F
    F --> K["Output"]
    K --> L["on_chain_end"]

    class A inputNode
    class B,C,G processNode
    class D decisionNode
    class E decisionNode
    class I toolNode
    class F,K outputNode
    class L outputNode
```

## 9.8 完整可运行的代码示例

### 9.8.1 综合示例：构建日志追踪系统

```python
"""
LangChain Callbacks 综合示例：构建日志追踪系统
这个示例展示如何创建一个完整的日志追踪系统，记录所有 LLM 和 Chain 的执行情况
"""

import json
from datetime import datetime
from typing import Any, Dict, List
from langchain.callbacks.base import BaseCallbackHandler
from langchain_openai import OpenAI
from langchain.chains import LLMChain
from langchain.prompts import PromptTemplate
from langchain.agents import AgentExecutor, create_react_agent
from langchain import hub
from langchain.tools import Tool


class StructuredLoggingCallback(BaseCallbackHandler):
    """
    结构化日志 Callback
    将所有事件记录为结构化数据，支持后续分析
    """

    def __init__(self, log_file: str = "execution_log.jsonl"):
        self.log_file = log_file
        self.current_chain = None
        self.events: List[Dict] = []

    def _log_event(self, event_type: str, data: Dict[str, Any]):
        """记录事件到日志"""
        event = {
            "timestamp": datetime.now().isoformat(),
            "type": event_type,
            "data": data
        }
        self.events.append(event)

        # 同时写入文件
        with open(self.log_file, "a", encoding="utf-8") as f:
            f.write(json.dumps(event, ensure_ascii=False) + "\n")

    def on_llm_start(self, serialized: Dict, prompts: List[str], **kwargs):
        self._log_event("llm_start", {
            "model": serialized.get("name", "unknown"),
            "prompt_preview": prompts[0][:100] if prompts else ""
        })

    def on_llm_end(self, response: Any, **kwargs):
        if hasattr(response, "llm_output") and response.llm_output:
            token_usage = response.llm_output.get("token_usage", {})
            self._log_event("llm_end", {
                "total_tokens": token_usage.get("total_tokens", 0),
                "prompt_tokens": token_usage.get("prompt_tokens", 0),
                "completion_tokens": token_usage.get("completion_tokens", 0)
            })
        else:
            self._log_event("llm_end", {"status": "completed"})

    def on_chain_start(self, serialized: Dict, inputs: Dict, **kwargs):
        chain_name = serialized.get("name", "unknown")
        self.current_chain = chain_name
        self._log_event("chain_start", {
            "chain": chain_name,
            "inputs": {k: str(v)[:50] for k, v in inputs.items()}
        })

    def on_chain_end(self, outputs: Dict, **kwargs):
        self._log_event("chain_end", {
            "chain": self.current_chain,
            "outputs": {k: str(v)[:50] for k, v in outputs.items()}
        })
        self.current_chain = None

    def on_tool_start(self, serialized: Dict, input_str: str, **kwargs):
        self._log_event("tool_start", {
            "tool": serialized.get("name", "unknown"),
            "input": input_str[:100]
        })

    def on_tool_end(self, output: str, **kwargs):
        self._log_event("tool_end", {
            "output": output[:100]
        })

    def on_text(self, text: str, **kwargs):
        # 避免重复记录 LLM 输出
        if self.current_chain:
            self._log_event("text", {"content": text[:100]})

    def get_summary(self) -> Dict:
        """获取执行摘要"""
        return {
            "total_events": len(self.events),
            "llm_calls": sum(1 for e in self.events if e["type"] == "llm_end"),
            "chain_calls": sum(1 for e in self.events if e["type"] == "chain_end"),
            "tool_calls": sum(1 for e in self.events if e["type"] == "tool_end")
        }


def create_sample_tools():
    """创建示例工具"""
    def get_current_time() -> str:
        """获取当前时间"""
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    def search_wiki(query: str) -> str:
        """模拟 Wikipedia 搜索"""
        wiki_data = {
            "python": "Python 是一种高级编程语言，由 Guido van Rossum 创建",
            "人工智能": "人工智能是计算机科学的一个分支，致力于创建智能机器",
            "机器学习": "机器学习是人工智能的一个子领域，使用数据来改进性能"
        }
        return wiki_data.get(query, f"未找到关于 '{query}' 的信息")

    return [
        Tool(
            name="当前时间",
            func=get_current_time,
            description="获取当前的日期和时间"
        ),
        Tool(
            name="维基百科搜索",
            func=search_wiki,
            description="搜索维基百科，输入应该是搜索关键词"
        )
    ]


def run_demo():
    """运行演示"""
    print("=" * 60)
    print("LangChain Callbacks 综合示例")
    print("=" * 60)

    # 创建日志 Callback
    log_callback = StructuredLoggingCallback("demo_log.jsonl")

    # 初始化 OpenAI LLM
    llm = OpenAI(temperature=0)

    # 示例 1: 简单 LLM 调用
    print("\n[示例 1] 简单 LLM 调用")
    print("-" * 40)
    response = llm.invoke(
        "用一句话解释什么是 LangChain",
        config={"callbacks": [log_callback]}
    )
    print(f"响应: {response}\n")

    # 示例 2: LLM Chain
    print("\n[示例 2] LLM Chain")
    print("-" * 40)
    prompt = PromptTemplate.from_template(
        "请为以下产品生成 3 个营销要点:\n产品: {product}\n要求: 每个要点不超过 20 字"
    )
    chain = LLMChain(llm=llm, prompt=prompt)

    result = chain.invoke(
        {"product": "智能音箱"},
        config={"callbacks": [log_callback]}
    )
    print(f"营销要点:\n{result['text']}\n")

    # 示例 3: Agent with Tools
    print("\n[示例 3] Agent with Tools")
    print("-" * 40)
    tools = create_sample_tools()
    prompt = hub.pull("hwchase17/react")
    agent = create_react_agent(llm, tools, prompt)
    agent_executor = AgentExecutor(
        agent=agent,
        tools=tools,
        verbose=False,
        callbacks=[log_callback]
    )

    agent_result = agent_executor.invoke(
        {"input": "请搜索 '人工智能' 的相关信息，并告诉我当前时间"}
    )
    print(f"Agent 输出: {agent_result['output']}\n")

    # 打印执行摘要
    print("\n" + "=" * 60)
    print("执行摘要")
    print("=" * 60)
    summary = log_callback.get_summary()
    for key, value in summary.items():
        print(f"  {key}: {value}")

    print(f"\n详细日志已保存到: {log_callback.log_file}")


if __name__ == "__main__":
    run_demo()
```

### 9.8.2 示例：成本追踪系统

```python
"""
成本追踪 Callback 示例
用于追踪 LLM 调用的 Token 使用量和估算成本
"""

from dataclasses import dataclass
from datetime import datetime
from typing import Optional
from langchain.callbacks.base import BaseCallbackHandler
from langchain_openai import OpenAI
from langchain.schema import LLMResult


@dataclass
class CostRecord:
    """成本记录"""
    timestamp: str
    model: str
    prompt_tokens: int
    completion_tokens: int
    total_tokens: int
    estimated_cost: float  # 美元


class CostTrackingCallback(BaseCallbackHandler):
    """成本追踪 Callback"""

    # OpenAI GPT-3.5-turbo 价格（每 1K tokens）
    PRICING = {
        "gpt-3.5-turbo": {"prompt": 0.0015, "completion": 0.002},
        "gpt-4": {"prompt": 0.03, "completion": 0.06},
        "gpt-4-turbo": {"prompt": 0.01, "completion": 0.03},
    }

    def __init__(self, model_name: str = "gpt-3.5-turbo"):
        self.model_name = model_name
        self.records: list[CostRecord] = []
        self.total_cost = 0.0
        self.total_tokens = 0

    def on_llm_end(self, response: LLMResult, **kwargs) -> None:
        """计算并记录成本"""
        if not response.llm_output:
            return

        usage = response.llm_output.get("token_usage", {})
        prompt_tokens = usage.get("prompt_tokens", 0)
        completion_tokens = usage.get("completion_tokens", 0)
        total_tokens = usage.get("total_tokens", 0)

        # 计算成本
        pricing = self.PRICING.get(self.model_name, self.PRICING["gpt-3.5-turbo"])
        cost = (
            prompt_tokens / 1000 * pricing["prompt"] +
            completion_tokens / 1000 * pricing["completion"]
        )

        # 记录
        record = CostRecord(
            timestamp=datetime.now().isoformat(),
            model=self.model_name,
            prompt_tokens=prompt_tokens,
            completion_tokens=completion_tokens,
            total_tokens=total_tokens,
            estimated_cost=cost
        )
        self.records.append(record)
        self.total_cost += cost
        self.total_tokens += total_tokens

        print(f"[成本追踪] Token: {total_tokens}, 成本: ${cost:.6f}")

    def get_report(self) -> dict:
        """生成成本报告"""
        return {
            "total_calls": len(self.records),
            "total_tokens": self.total_tokens,
            "total_cost": self.total_cost,
            "average_cost_per_call": self.total_cost / len(self.records) if self.records else 0,
            "records": [
                {
                    "timestamp": r.timestamp,
                    "tokens": r.total_tokens,
                    "cost": r.estimated_cost
                }
                for r in self.records
            ]
        }


def main():
    # 创建成本追踪 Callback
    cost_callback = CostTrackingCallback("gpt-3.5-turbo")

    # 创建 LLM
    llm = OpenAI(temperature=0)

    # 执行多个请求
    queries = [
        "什么是人工智能？",
        "解释机器学习的基本原理",
        "详细介绍深度学习"
    ]

    print("执行多个 LLM 请求并追踪成本\n")
    print("-" * 50)

    for i, query in enumerate(queries, 1):
        print(f"\n请求 {i}: {query}")
        response = llm.invoke(
            query,
            config={"callbacks": [cost_callback]}
        )

    # 打印成本报告
    print("\n" + "=" * 50)
    print("成本追踪报告")
    print("=" * 50)

    report = cost_callback.get_report()
    print(f"总调用次数: {report['total_calls']}")
    print(f"总 Token 数: {report['total_tokens']}")
    print(f"总成本: ${report['total_cost']:.6f}")
    print(f"平均每次调用成本: ${report['average_cost_per_call']:.6f}")


if __name__ == "__main__":
    main()
```

### 9.8.3 示例：流式输出 Callback

```python
"""
流式输出 Callback 示例
展示如何在流式 LLM 调用中使用 Callbacks
"""

from langchain.callbacks.base import BaseCallbackHandler
from langchain_openai import OpenAI
import sys


class StreamingCallbackHandler(BaseCallbackHandler):
    """流式输出 Callback Handler"""

    def __init__(self):
        self.text_buffer = ""

    def on_llm_start(self, serialized, prompts, **kwargs):
        print("开始生成... ", end="", flush=True)

    def on_llm_new_token(self, token: str, **kwargs):
        """每个新 token 产生时调用"""
        print(token, end="", flush=True)
        self.text_buffer += token

    def on_llm_end(self, response, **kwargs):
        print("\n生成完成!")

    def on_llm_error(self, error, **kwargs):
        print(f"\n生成出错: {error}")


def streaming_demo():
    """流式输出演示"""
    print("=" * 60)
    print("流式输出 Callback 示例")
    print("=" * 60)

    # 创建流式 Callback
    stream_callback = StreamingCallbackHandler()

    # 创建流式 LLM
    llm = OpenAI(
        temperature=0,
        streaming=True,
        callbacks=[stream_callback]
    )

    print("\n问题: 什么是 Python 编程语言？")
    print("-" * 60)

    response = llm.invoke("什么是 Python 编程语言？请简要介绍。")

    print("-" * 60)
    print(f"\n完整响应长度: {len(stream_callback.text_buffer)} 字符")


if __name__ == "__main__":
    streaming_demo()
```

## 9.9 最佳实践与注意事项

### 9.9.1 使用建议

1. **按需选择 Callback 类型**：不是所有场景都需要所有事件类型的 Callback

2. **注意性能影响**：过多的 Callback 或复杂的日志逻辑可能影响性能

3. **异步处理**：对于 I/O 操作（如写文件、发送网络请求），使用异步 Callback

4. **资源清理**：如果 Callback 中使用了资源，确保适当的清理机制

### 9.9.2 常见问题

```python
# Q: 如何在多个组件中共享同一个 Callback？
# A: 创建 Callback 实例并传递给多个组件

callback = MyCallbackHandler()
chain1 = LLMChain(llm=llm1, prompt=prompt1, callbacks=[callback])
chain2 = LLMChain(llm=llm2, prompt=prompt2, callbacks=[callback])

# Q: 如何禁用某个 Callback？
# A: 不传递该 Callback 或传递空列表

llm.invoke("query", config={"callbacks": []})  # 不使用任何 Callback

# Q: Callback 中发生异常会怎样？
# A: 通常异常会被捕获并记录，但不会中断主流程
```

### 9.9.3 性能优化建议

```python
# 1. 使用批量处理而不是实时处理
class BatchedCallback(BaseCallbackHandler):
    def __init__(self):
        self.events_batch = []
        self.batch_size = 100

    def on_llm_end(self, response, **kwargs):
        self.events_batch.append(response)
        if len(self.events_batch) >= self.batch_size:
            self._flush_batch()

    def _flush_batch(self):
        # 批量处理事件
        pass

# 2. 使用异步 I/O
import asyncio

class AsyncFileCallback(BaseCallbackHandler):
    async def on_llm_end(self, response, **kwargs):
        await self._async_write_to_file(response)

    async def _async_write_to_file(self, data):
        # 异步写入文件
        await asyncio.sleep(0)
```

## 9.10 总结

本章介绍了 LangChain Callbacks 系统的核心概念和使用方法：

1. **Callbacks 基础**：Callbacks 是一种事件监听机制，允许在 LangChain 执行过程中插入自定义逻辑

2. **内置 Callbacks**：LangChain 提供了 ConsoleCallbackHandler、FileCallbackHandler、StdOutCallbackHandler 等内置实现

3. **自定义 Callback**：通过继承 BaseCallbackHandler，可以创建满足特定需求的 Callback Handler

4. **同步与异步**：根据场景选择同步或异步 Callback，异步适用于 I/O 密集型操作

5. **使用场景**：
   - 调试和日志记录
   - 成本和用量追踪
   - 性能监控
   - 自定义业务逻辑

Callbacks 是 LangChain 中非常强大和灵活的功能，掌握它将帮助你更好地监控、控制和优化 LangChain 应用。

---

**下一章预告**：第10章将介绍 LangChain 的记忆（Memory）系统，学习如何让 AI 应用记住对话历史。
