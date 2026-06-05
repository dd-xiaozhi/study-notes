# AI Agent 系统设计教程

> 全栈深度版 · Python 实现 · 主推 LangGraph · 2026 版
> 从理论原理到生产落地，13 章 + 4 个完整实战案例

## 教程定位

- **目标读者**：有 Python 基础、了解 LLM API 调用、想系统掌握 AI Agent 工程化的开发者/架构师
- **学习目标**：能够独立设计、实现、评测、部署一个生产级 AI Agent 系统
- **配套代码**：每章包含可运行代码，第 10-13 章为完整端到端项目

## 教程结构

```
ai-agent系统设计/
├── README.md                              # 本文件（教程总览与导航）
├── 01-AI-Agent概述与发展历程.md           # 第一部分：基础理论
├── 02-核心架构-五大模块全景.md
├── 03-规划模块-Planning深度解析.md         # 第二部分：核心模块
├── 04-记忆模块-Memory与RAG.md
├── 05-工具调用-ToolUse与MCP.md
├── 06-LangGraph深度实战.md                # 第三部分：工程框架
├── 07-多Agent协作模式与编排.md
├── 08-评测与可观测性.md                    # 第四部分：生产就绪
├── 09-生产部署-安全与成本控制.md
├── 10-实战一-智能客服Agent.md              # 第五部分：完整实战
├── 11-实战二-代码Agent.md
├── 12-实战三-数据分析Agent.md
└── 13-实战四-多Agent协作研究助手.md
```

## 章节速览

### 第一部分：基础理论（建立心智模型）

| 章节 | 主题 | 核心内容 |
|------|------|---------|
| 第 1 章 | [AI Agent 概述](./01-AI-Agent概述与发展历程.md) | 定义、与传统软件/LLM 对比、发展历程、典型场景 |
| 第 2 章 | [核心架构](./02-核心架构-五大模块全景.md) | 感知-规划-记忆-工具-行动五大模块、反思闭环 |

### 第二部分：核心模块深入（吃透每个组件）

| 章节 | 主题 | 核心内容 |
|------|------|---------|
| 第 3 章 | [规划模块](./03-规划模块-Planning深度解析.md) | ReAct / CoT / ToT / Plan-and-Execute / Reflexion |
| 第 4 章 | [记忆模块](./04-记忆模块-Memory与RAG.md) | 短期/长期/语义记忆、RAG、上下文工程 |
| 第 5 章 | [工具调用](./05-工具调用-ToolUse与MCP.md) | Function Calling、MCP 协议、工具设计、沙箱 |

### 第三部分：工程框架（上手主流框架）

| 章节 | 主题 | 核心内容 |
|------|------|---------|
| 第 6 章 | [LangGraph 深度实战](./06-LangGraph深度实战.md) | 状态机、节点/边、Checkpointer、Human-in-the-Loop |
| 第 7 章 | [多 Agent 协作](./07-多Agent协作模式与编排.md) | Supervisor / Hierarchical / Swarm / Network |

### 第四部分：生产就绪（让 Agent 真正可用）

| 章节 | 主题 | 核心内容 |
|------|------|---------|
| 第 8 章 | [评测与可观测性](./08-评测与可观测性.md) | 评测维度、LangSmith、LangFuse、Trace |
| 第 9 章 | [生产部署](./09-生产部署-安全与成本控制.md) | 部署架构、Prompt 注入防护、限流、降级、成本 |

### 第五部分：完整实战（端到端落地）

| 章节 | 主题 | 核心内容 |
|------|------|---------|
| 第 10 章 | [智能客服 Agent](./10-实战一-智能客服Agent.md) | RAG + 工具调用 + 多轮对话 + 人工接管 |
| 第 11 章 | [代码 Agent](./11-实战二-代码Agent.md) | 文件读写 + 命令执行 + 任务规划（类 Claude Code） |
| 第 12 章 | [数据分析 Agent](./12-实战三-数据分析Agent.md) | NL2SQL + 图表生成 + 报告输出 |
| 第 13 章 | [多 Agent 研究助手](./13-实战四-多Agent协作研究助手.md) | Planner + Researcher + Writer + Reviewer |

## 学习路径建议

```mermaid
flowchart LR
    A[第1-2章<br/>建立心智] --> B[第3-5章<br/>吃透模块]
    B --> C[第6章<br/>LangGraph上手]
    C --> D[第7章<br/>多Agent编排]
    D --> E[第8-9章<br/>生产准备]
    E --> F[第10-13章<br/>选1-2个实战]

    style A fill:#1e3a8a,color:#fff,stroke:#60a5fa
    style B fill:#7c2d12,color:#fff,stroke:#fb923c
    style C fill:#14532d,color:#fff,stroke:#4ade80
    style D fill:#14532d,color:#fff,stroke:#4ade80
    style E fill:#581c87,color:#fff,stroke:#c084fc
    style F fill:#7f1d1d,color:#fff,stroke:#f87171
```

**推荐路径**：
- **快速入门**（1 周）：第 1、2、3、6 章 → 跑通第 10 章
- **系统学习**（4 周）：按顺序通读 1-9 章 → 选 2 个实战
- **架构师视角**（重点）：第 2、7、8、9 章 + 全部实战

## 环境准备

```bash
# Python 3.10+
python --version

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate    # Windows

# 核心依赖
pip install langgraph langchain langchain-openai langchain-anthropic
pip install langchain-community chromadb tavily-python
pip install langsmith langfuse fastapi uvicorn
```

## 配套资源

- LangGraph 官方文档：https://langchain-ai.github.io/langgraph/
- LangChain 官方文档：https://python.langchain.com/
- Anthropic Agent 最佳实践：https://www.anthropic.com/research/building-effective-agents
- MCP 协议规范：https://modelcontextprotocol.io/

## 版本与维护

- 最后更新：2026-06-05
- LangGraph 版本：>= 0.2.x
- Python 版本：>= 3.10
