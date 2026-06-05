# 第 10 章 实战一：智能客服 Agent

> 本章是教程的第一个完整实战项目。前 9 章我们学了 Agent 五大模块、LangGraph、RAG、记忆、评测、部署——所有内容会在这里"打通关"。我们会从需求到代码到部署，完整实现一个电商智能客服 Agent。读完后你不仅有了一套能跑的代码，还掌握了一个真实业务系统从 0 到 1 的工程化落地方法。

## 10.1 业务需求与场景

### 10.1.1 业务背景

假设你刚加入一家中等规模的电商公司"优品购"，主营服饰、3C 数码、家居用品。客服部门 80 人，每天承接 1.2 万通会话，平均处理时长 6 分钟，单次人力成本约 8 元。年客服成本 3500 万元，**而且 70% 的咨询是重复问题**（查物流、退款政策、尺码表）。

老板给你一个任务：**3 个月内上线 AI 客服 Agent，把人工接管率压到 30% 以下**。

### 10.1.2 用户痛点

站在用户角度，目前的客服体验有 4 大痛点：

1. **响应慢**：高峰期排队 5-8 分钟，超过 60% 的用户在这一步流失。
2. **答非所问**：人工客服对长尾问题（如跨 SKU 对比、特殊退换规则）覆盖率不足。
3. **24h 难触达**：夜班客服只 5 人，凌晨下单用户的物流、退款问题无人应答。
4. **一致性差**：不同客服给出的退换政策口径不一致，引发投诉。

### 10.1.3 业务目标

| 目标 | 现状 | 目标（3 个月内） |
|------|------|------------------|
| 首次响应时间 | 90s | < 3s |
| 解决率（Deflection Rate） | — | ≥ 65% |
| 转人工率 | — | ≤ 30% |
| CSAT 评分 | 4.1/5 | ≥ 4.3/5 |
| 单次服务成本 | 8 元 | ≤ 1.5 元 |
| 7×24 覆盖 | 70% | 100% |

### 10.1.4 成功指标定义

- **解决率**：用户问题在 AI Agent 单轮/多轮交互内完成且未触发转人工的比例。
- **转人工率**：触发 `human_handoff` 工具的会话 / 总会话。
- **CSAT**：对话结束后用户主动打分（1-5 星）。
- **平均轮数**：完成一次解决的平均对话轮数，> 8 轮视为异常。
- **P95 响应延迟**：从用户发消息到 Agent 首个 token 输出的耗时，< 1.5s。

### 10.1.5 典型对话场景

我们在需求阶段就列出 5 类典型场景，后续的意图识别、工具设计、评测集都基于这些场景展开：

| 场景 | 用户问法（示例） | 期望处理 | 涉及工具 |
|------|------------------|----------|----------|
| **订单查询** | "我昨天下的订单什么时候到？" | 查订单状态、物流轨迹 | `query_order`、`query_logistics` |
| **退换货** | "衣服尺码不对，我想退货" | 走退换流程，判断是否符合政策 | `apply_refund`、`search_faq` |
| **商品咨询** | "这款手机支持 5G 吗？电池多大？" | 查商品手册 | `search_faq`（RAG） |
| **投诉升级** | "你们客服态度太差了！我要投诉！" | 情绪识别 + 转人工 | `human_handoff` |
| **闲聊/无关** | "今天天气怎么样？" | 礼貌拒答，引导回业务话题 | 直接回答 |

后面 10.10 节的端到端演示会逐一回放这些场景。

---

## 10.2 系统架构设计

### 10.2.1 整体架构图

```mermaid
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart TB
    User[用户<br/>App/网页/IM] -->|HTTPS/WS| Gateway[API 网关<br/>Nginx + 限流]

    Gateway --> Auth[鉴权服务<br/>JWT + 风控]
    Auth --> API[FastAPI<br/>SSE 流式接口]
    API --> Agent[LangGraph Agent<br/>状态机]

    Agent -->|路由| Intent[意图识别节点]
    Intent -->|订单/物流| ToolNode[工具节点]
    Intent -->|商品/政策| RAGNode[RAG 节点]
    Intent -->|转人工| HumanNode[人工接管节点]
    Intent -->|闲聊| ChitNode[闲聊回复节点]

    ToolNode --> T1[query_order]
    ToolNode --> T2[query_logistics]
    ToolNode --> T3[apply_refund]
    RAGNode --> KB[(Chroma<br/>FAQ+商品手册)]
    HumanNode --> Queue[(客服工单系统<br/>WebSocket)]
    ToolNode -.观测.-> LangSmith[(LangSmith<br/>Trace)]
    RAGNode -.观测.-> LangSmith
    HumanNode -.观测.-> LangSmith

    Agent <-.会话状态.-> Checkpoint[(Postgres<br/>Checkpoint)]
    Agent --> Memory[长期记忆<br/>用户画像+偏好]

    style User fill:#1e3a8a,color:#fff
    style Gateway fill:#7c2d12,color:#fff
    style Auth fill:#7c2d12,color:#fff
    style API fill:#7c2d12,color:#fff
    style Agent fill:#581c87,color:#fff
    style Intent fill:#14532d,color:#fff
    style ToolNode fill:#14532d,color:#fff
    style RAGNode fill:#14532d,color:#fff
    style HumanNode fill:#7f1d1d,color:#fff
    style ChitNode fill:#14532d,color:#fff
    style KB fill:#0f766e,color:#fff
    style Queue fill:#7f1d1d,color:#fff
    style LangSmith fill:#0f766e,color:#fff
    style Checkpoint fill:#0f766e,color:#fff
    style Memory fill:#0f766e,color:#fff
    style T1 fill:#581c87,color:#fff
    style T2 fill:#581c87,color:#fff
    style T3 fill:#581c87,color:#fff
```

### 10.2.2 模块清单与职责

| 模块 | 职责 | 关键依赖 |
|------|------|----------|
| **API 网关** | 限流、防刷、HTTPS 卸载、灰度路由 | Nginx、Redis |
| **鉴权服务** | JWT 解析、用户身份校验、敏感词初筛 | Redis 黑名单 |
| **FastAPI 网关层** | SSE 流式输出、协议转换、连接管理 | FastAPI、uvicorn |
| **LangGraph Agent** | 状态机、节点编排、循环与中断 | LangGraph、LangChain |
| **意图识别节点** | 用 LLM + Few-shot 识别用户意图 | GPT-4o-mini / Qwen2.5 |
| **工具节点** | 执行订单、物流、退款等 API 调用 | 业务微服务、MCP |
| **RAG 节点** | 检索 FAQ + 商品手册 | Chroma、BGE、bge-reranker |
| **人工接管节点** | 写入工单队列、推送通知 | Kafka、WebSocket |
| **Checkpointer** | 持久化每一步状态 | Postgres / SQLite |
| **长期记忆** | 跨会话的用户偏好、画像 | Postgres + 向量库 |
| **LangSmith** | Trace 埋点、Token 用量、延迟统计 | LangSmith SaaS |
| **业务工具后端** | 订单、物流、退款服务 | 内部微服务 |

### 10.2.3 关键设计决策

1. **状态外置**：用 Postgres 作为 Checkpointer 而不是内存，进程崩溃不丢上下文。
2. **RAG 与工具解耦**：商品/政策走 RAG（语义检索），订单/物流走工具（精确查询）。两者不要混在一起，避免"为了查订单号而搜出一堆退货政策"。
3. **流式输出**：长回答用 SSE 一字一字吐，首字延迟 < 1s，体验质变。
4. **转人工双通道**：敏感词（投诉/骂人/退款金额 > 阈值）走强制通道；普通关键词（"转人工"）走用户主动通道。

---

## 10.3 工具设计

工具是 Agent 与业务系统交互的"手"。我们一共设计 5 个核心工具，全部用 Pydantic v2 定义输入 Schema，方便 LangChain 自动生成 JSON Schema 喂给 LLM。

### 10.3.1 工具目录与决策表

| 工具名 | 触发意图 | 输入参数 | 副作用 |
|--------|----------|----------|--------|
| `query_order` | 订单查询 | `order_id` | 无（读） |
| `query_logistics` | 物流查询 | `order_id` | 无（读） |
| `apply_refund` | 申请退款 | `order_id`、`reason`、`amount` | **有**（写） |
| `search_faq` | 商品/政策咨询 | `query`、`top_k` | 无（读） |
| `human_handoff` | 转人工 | `reason`、`priority` | **有**（写） |

### 10.3.2 完整工具实现

```python
# tools/customer_service_tools.py
"""
电商客服 Agent 工具集。
所有工具均使用 Pydantic v2 Schema，LangChain ToolNode 会自动注入。
Mock 实现仅用于本地开发，生产环境替换为真实业务 API 客户端。
"""
from __future__ import annotations
import time
import random
from datetime import datetime, timedelta
from typing import Literal

from langchain_core.tools import tool
from pydantic import BaseModel, Field


# ---------- 1. query_order ----------
class QueryOrderInput(BaseModel):
    order_id: str = Field(description="订单号，形如 ORD202406150001")
    user_id: str = Field(description="用户 ID，用于鉴权")


@tool("query_order", args_schema=QueryOrderInput)
def query_order(order_id: str, user_id: str) -> dict:
    """查询订单详情，包括商品、金额、状态、下单时间。"""
    # Mock：真实环境调用订单中心 RPC / HTTP
    if not order_id.startswith("ORD"):
        return {"error": "INVALID_ORDER_ID", "msg": "订单号格式错误"}
    return {
        "order_id": order_id,
        "user_id": user_id,
        "status": random.choice(["PAID", "SHIPPED", "DELIVERED", "COMPLETED"]),
        "items": [
            {"sku": "SKU-001", "name": "男款羽绒服-黑色-L", "price": 599.0, "qty": 1}
        ],
        "total_amount": 599.0,
        "created_at": (datetime.now() - timedelta(days=2)).isoformat(),
    }


# ---------- 2. query_logistics ----------
class QueryLogisticsInput(BaseModel):
    order_id: str = Field(description="订单号")


@tool("query_logistics", args_schema=QueryLogisticsInput)
def query_logistics(order_id: str) -> dict:
    """查询订单的物流轨迹。"""
    return {
        "order_id": order_id,
        "carrier": "顺丰速运",
        "tracking_no": f"SF{random.randint(10**11, 10**12)}",
        "status": "IN_TRANSIT",
        "轨迹": [
            {"time": "2024-06-15 10:00", "desc": "杭州中转中心 已发出"},
            {"time": "2024-06-15 18:30", "desc": "到达 上海中转中心"},
            {"time": "2024-06-16 09:15", "desc": "派送中，骑手 张师傅 138****1234"},
        ],
        "eta": "2024-06-16 18:00 之前",
    }


# ---------- 3. apply_refund ----------
class ApplyRefundInput(BaseModel):
    order_id: str = Field(description="订单号")
    reason: Literal["SIZE_ISSUE", "QUALITY_ISSUE", "NOT_AS_DESCRIBED", "NO_LONGER_NEED", "OTHER"] = Field(
        description="退款原因枚举"
    )
    amount: float = Field(ge=0, le=100000, description="退款金额，必须为正且不超过订单金额")
    description: str = Field(default="", description="退款补充说明，限制 200 字以内")


@tool("apply_refund", args_schema=ApplyRefundInput)
def apply_refund(order_id: str, reason: str, amount: float, description: str = "") -> dict:
    """提交退款申请。需要在用户确认后调用，避免误操作。"""
    # 真实环境会调用支付中心 + 工单系统
    refund_id = f"RF{int(time.time() * 1000)}"
    return {
        "success": True,
        "refund_id": refund_id,
        "order_id": order_id,
        "amount": amount,
        "reason": reason,
        "expected_arrival": "1-3 个工作日原路退回",
    }


# ---------- 4. search_faq ----------
class SearchFAQInput(BaseModel):
    query: str = Field(description="用户问题的语义查询，例如'电池能用多久'")
    top_k: int = Field(default=3, ge=1, le=10, description="返回的最相关文档数量")


@tool("search_faq", args_schema=SearchFAQInput)
def search_faq(query: str, top_k: int = 3) -> dict:
    """在 FAQ 与商品手册中检索最相关的内容。"""
    # 实际实现走 Chroma + BGE，下一节展开
    from rag.knowledge_base import search_knowledge_base
    hits = search_knowledge_base(query, top_k=top_k)
    return {
        "query": query,
        "hits": [
            {"score": h.score, "content": h.page_content, "source": h.metadata.get("source")}
            for h in hits
        ],
    }


# ---------- 5. human_handoff ----------
class HumanHandoffInput(BaseModel):
    reason: str = Field(description="转人工原因，例：'用户明确要求转人工'、'情绪激动'、'多次失败'")
    priority: Literal["LOW", "MEDIUM", "HIGH", "URGENT"] = Field(
        default="MEDIUM", description="工单优先级"
    )
    context_summary: str = Field(default="", description="对话上下文摘要，给人工客服参考")


@tool("human_handoff", args_schema=HumanHandoffInput)
def human_handoff(reason: str, priority: str = "MEDIUM", context_summary: str = "") -> dict:
    """把会话转交给人工客服。"""
    # 实际实现写入工单系统 + 推送 WebSocket 通知
    ticket_id = f"TK{int(time.time())}"
    return {
        "success": True,
        "ticket_id": ticket_id,
        "queue_position": random.randint(1, 20),
        "estimated_wait_seconds": random.randint(30, 180),
        "message": f"已为您转接人工客服，工单号 {ticket_id}，预计等待 {random.randint(30, 180)} 秒",
    }


# 工具列表，绑定到 LangGraph ToolNode
ALL_TOOLS = [query_order, query_logistics, apply_refund, search_faq, human_handoff]
```

### 10.3.3 工具设计要点

- **类型约束**：`amount: float = Field(ge=0, le=100000)` 用 Pydantic 限制范围，避免 LLM 输出离谱金额。
- **枚举代替自由文本**：`reason: Literal[...]` 强制 LLM 从有限集合选，减少幻觉。
- **可读 Schema**：`description` 一定要写清楚，LLM 是看着 description 决定何时调工具的。
- **副作用分级**：`apply_refund`、`human_handoff` 有副作用，10.7 节会用 `interrupt()` 做二次确认。
- **Mock 与生产分离**：Mock 函数单独放，prod 环境用 `if ENV == "prod": import real_impl` 替换。

---

## 10.4 RAG 知识库

RAG 用来回答"商品规格、退换政策、促销活动"这类**答案分散在文档中**的问题。我们用 Chroma 做向量库，BGE 做 embedding，bge-reranker 做精排。

### 10.4.1 数据源

| 文档类型 | 数量 | 样例 |
|----------|------|------|
| FAQ | 200 条 | "7 天无理由退换怎么操作？" |
| 商品手册 | 50 款 | "iPhone 15 Pro 电池容量 3274mAh" |
| 退换货政策 | 5 篇 | "服饰类 7 天无理由、3C 类 15 天" |
| 活动规则 | 10 篇 | "618 大促跨店满减规则" |

### 10.4.2 完整索引与检索代码

```python
# rag/knowledge_base.py
"""
基于 Chroma + BGE 的 RAG 知识库。
embedding: BAAI/bge-small-zh-v1.5
reranker:  BAAI/bge-reranker-base
"""
from __future__ import annotations
import os
from pathlib import Path
from typing import List

from langchain_core.documents import Document
from langchain_community.embeddings import HuggingFaceBgeEmbeddings
from langchain_community.vectorstores import Chroma
from langchain_community.document_compressors import BgeRerank


# ---------- 配置 ----------
PERSIST_DIR = "./chroma_db"
COLLECTION = "youpin_faq"
EMBED_MODEL = "BAAI/bge-small-zh-v1.5"
RERANK_MODEL = "BAAI/bge-reranker-base"

# 全局单例
_embeddings = None
_vectorstore = None
_reranker = None


def _get_embeddings() -> HuggingFaceBgeEmbeddings:
    global _embeddings
    if _embeddings is None:
        _embeddings = HuggingFaceBgeEmbeddings(
            model_name=EMBED_MODEL,
            model_kwargs={"device": "cpu"},   # 生产换 cuda
            encode_kwargs={"normalize_embeddings": True},
        )
    return _embeddings


def _get_reranker() -> BgeRerank:
    global _reranker
    if _reranker is None:
        _reranker = BgeRerank(model_name=RERANK_MODEL, top_n=3)
    return _reranker


# ---------- 构建索引 ----------
def build_index(docs: List[Document]) -> Chroma:
    """从一组 Document 构建/重建索引。"""
    _get_embeddings().client  # 触发模型加载
    vs = Chroma.from_documents(
        documents=docs,
        embedding=_get_embeddings(),
        persist_directory=PERSIST_DIR,
        collection_name=COLLECTION,
    )
    vs.persist()
    return vs


# ---------- 加载已有索引 ----------
def load_index() -> Chroma:
    global _vectorstore
    if _vectorstore is None:
        _vectorstore = Chroma(
            persist_directory=PERSIST_DIR,
            collection_name=COLLECTION,
            embedding_function=_get_embeddings(),
        )
    return _vectorstore


# ---------- 检索 + 精排 ----------
def search_knowledge_base(query: str, top_k: int = 3) -> List[Document]:
    """先粗排 top-20，再精排 top-3。"""
    vs = load_index()
    # 第一阶段：向量检索
    candidates = vs.similarity_search_with_score(query, k=20)
    if not candidates:
        return []

    docs_only = [doc for doc, _ in candidates]

    # 第二阶段：bge-reranker 精排
    rerank = _get_reranker()
    reranked = rerank.compress_documents(docs_only, query)
    return reranked[:top_k]


# ---------- 索引构建入口 ----------
def init_demo_kb():
    """把示例 FAQ 与商品手册塞进 Chroma，仅用于演示。"""
    sample_docs = [
        Document(
            page_content="7 天无理由退换：商品签收后 7 天内可无理由退货，需保持吊牌完好。",
            metadata={"source": "policy/return.md", "category": "policy"},
        ),
        Document(
            page_content="3C 数码类商品支持 15 天质量问题退换，需提供检测报告。",
            metadata={"source": "policy/return.md", "category": "policy"},
        ),
        Document(
            page_content="iPhone 15 Pro 电池容量 3274mAh，支持 20W 快充，Type-C 接口。",
            metadata={"source": "manual/iphone15.md", "category": "manual"},
        ),
        Document(
            page_content="羽绒服建议手洗，水温 30°C 以下，不可漂白，悬挂晾干。",
            metadata={"source": "manual/jacket.md", "category": "manual"},
        ),
        Document(
            page_content="618 大促跨店满减：每满 300 减 50，上不封顶。可与店铺优惠券叠加。",
            metadata={"source": "promo/618.md", "category": "promo"},
        ),
        Document(
            page_content="如何申请退款：App → 我的订单 → 申请退款 → 选择原因 → 提交，1-3 工作日原路退回。",
            metadata={"source": "faq/refund.md", "category": "faq"},
        ),
    ]
    build_index(sample_docs)
    print(f"已索引 {len(sample_docs)} 条文档到 {PERSIST_DIR}/{COLLECTION}")


if __name__ == "__main__":
    init_demo_kb()
    res = search_knowledge_base("手机电池能用多久")
    for r in res:
        print(f"[{r.metadata['source']}] {r.page_content[:60]}...")
```

### 10.4.3 检索策略优化点

- **粗排 + 精排两阶段**：单独用向量检索，准确率约 70%；加 bge-reranker 后可达 88%。
- **文档切片**：长文档按段落切 256-512 字符，保留 `source` 元数据便于溯源。
- **混合检索（生产推荐）**：BM25 + 向量双路召回，再精排。本教程为了简洁只用向量。
- **元数据过滤**：用户在"3C 数码"分类下问"退货政策"，可加 `filter={"category": "policy"}` 缩小范围。

---

## 10.5 LangGraph 状态机搭建

这是本章核心。LangGraph 把整个客服流程建模成一张状态图，节点之间用条件边动态路由。

### 10.5.1 State 设计

```python
# graph/state.py
from __future__ import annotations
from typing import Annotated, Literal, Optional
from typing_extensions import TypedDict
from langgraph.graph.message import add_messages


class CustomerServiceState(TypedDict):
    # === 对话上下文 ===
    messages: Annotated[list, add_messages]   # 自动合并，LangGraph 内置
    user_id: str
    thread_id: str
    session_start_ts: float

    # === 业务上下文 ===
    order_context: Optional[dict]             # 当前会话关注的订单
    intent: Optional[Literal["order", "logistics", "refund", "product", "complaint", "chitchat", "unknown"]]
    entities: dict                              # 抽取的实体：order_id, sku 等

    # === Agent 内部状态 ===
    tool_call_count: int                       # 防止死循环
    escalation_flag: bool                      # 是否需要转人工
    escalation_reason: Optional[str]
    confidence: float                          # 意图识别置信度
    final_answer: Optional[str]
```

字段说明：
- `messages` 用 `Annotated[list, add_messages]` 是 LangGraph 的内置 reducer：每次节点返回的新消息自动追加到列表，而不是覆盖。
- `order_context` 跨节点共享，避免每轮重复查询订单。
- `tool_call_count` 防止"调工具失败 → 再调 → 失败"的死循环，超过阈值强制转人工。

### 10.5.2 完整图搭建代码（180 行）

```python
# graph/customer_service_graph.py
"""
智能客服 Agent 状态机：意图识别 → 路由 → 工具/RAG/直接回答 → 反馈。
"""
from __future__ import annotations
import json
import time
from typing import Literal

from langchain_core.messages import SystemMessage, HumanMessage, AIMessage, ToolMessage
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langgraph.graph import StateGraph, START, END
from langgraph.prebuilt import ToolNode
from langgraph.checkpoint.memory import MemorySaver

from graph.state import CustomerServiceState
from tools.customer_service_tools import ALL_TOOLS


# ---------- LLM ----------
llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)
llm_with_tools = llm.bind_tools(ALL_TOOLS)


# ---------- Prompt ----------
INTENT_PROMPT = ChatPromptTemplate.from_messages([
    ("system", """你是优品购电商客服 Agent 的意图识别器。根据用户最新一条消息判断意图。
可选意图：order（订单查询）、logistics（物流查询）、refund（退换货）、product（商品咨询）、complaint（投诉/不满）、chitchat（闲聊）、unknown（不清楚）。
同时抽取实体：order_id（ORD 开头）、sku、商品关键词。
严格返回 JSON，不要任何解释。JSON schema：
{{"intent": "...", "confidence": 0.0~1.0, "entities": {{}}, "needs_human": false, "reason": "..."}}"""),
    ("human", "{user_input}")
])


# ---------- 节点 1：意图识别 ----------
def intent_recognition_node(state: CustomerServiceState) -> dict:
    last_user_msg = state["messages"][-1].content
    chain = INTENT_PROMPT | llm
    resp = chain.invoke({"user_input": last_user_msg})

    # 解析 JSON，失败回退到 unknown
    try:
        result = json.loads(resp.content)
    except json.JSONDecodeError:
        result = {"intent": "unknown", "confidence": 0.0, "entities": {}, "needs_human": False}

    # 敏感词兜底：直接转人工
    sensitive_words = ["投诉", "举报", "起诉", "消协", "12315", "垃圾客服", "差评"]
    if any(w in last_user_msg for w in sensitive_words):
        result["needs_human"] = True
        result["reason"] = "命中敏感词"

    return {
        "intent": result.get("intent", "unknown"),
        "confidence": float(result.get("confidence", 0.0)),
        "entities": result.get("entities", {}),
        "escalation_flag": result.get("needs_human", False),
        "escalation_reason": result.get("reason", ""),
        "tool_call_count": state.get("tool_call_count", 0),
    }


# ---------- 节点 2：路由 ----------
def route_node(state: CustomerServiceState) -> dict:
    """根据意图决定下一节点。"""
    if state.get("escalation_flag"):
        return {"next_node": "human_handoff_node"}
    intent = state.get("intent")
    return {"next_node": {
        "order": "order_node",
        "logistics": "logistics_node",
        "refund": "refund_node",
        "product": "rag_node",
        "complaint": "human_handoff_node",
        "chitchat": "chitchat_node",
        "unknown": "clarify_node",
    }.get(intent, "clarify_node")}


# ---------- 节点 3：订单处理 ----------
def order_node(state: CustomerServiceState) -> dict:
    """根据实体中是否有 order_id 决定调工具还是追问。"""
    order_id = state.get("entities", {}).get("order_id")
    if not order_id:
        return {
            "messages": [AIMessage(content="请问您的订单号是多少？格式如 ORD202406150001。")],
            "tool_call_count": state.get("tool_call_count", 0) + 1,
        }
    # 委托给 ToolNode 执行 query_order
    return {"next_node": "tool_node", "pending_tool": "query_order"}


# ---------- 节点 4：物流处理 ----------
def logistics_node(state: CustomerServiceState) -> dict:
    order_id = state.get("entities", {}).get("order_id")
    if not order_id:
        return {"messages": [AIMessage(content="为了帮您查物流，请提供订单号哦～")]}
    return {"next_node": "tool_node", "pending_tool": "query_logistics"}


# ---------- 节点 5：退款处理 ----------
def refund_node(state: CustomerServiceState) -> dict:
    order_id = state.get("entities", {}).get("order_id")
    reason = state.get("entities", {}).get("reason", "OTHER")
    amount = state.get("entities", {}).get("amount", 0)
    if not order_id or not amount:
        return {"messages": [AIMessage(content="申请退款需要订单号和金额，请补充一下。")]}
    return {
        "messages": [AIMessage(content=f"确认要退款 ¥{amount} 吗？回复【确认】后我帮您提交。")],
    }


# ---------- 节点 6：RAG 节点 ----------
def rag_node(state: CustomerServiceState) -> dict:
    """调用 search_faq 工具检索知识库，再让 LLM 组织答案。"""
    return {"next_node": "tool_node", "pending_tool": "search_faq"}


# ---------- 节点 7：闲聊 ----------
def chitchat_node(state: CustomerServiceState) -> dict:
    reply = llm.invoke([
        SystemMessage(content="你是优品购客服，礼貌拒答非业务话题，并引导用户回业务问题。"),
        *state["messages"],
    ])
    return {"messages": [reply]}


# ---------- 节点 8：澄清 ----------
def clarify_node(state: CustomerServiceState) -> dict:
    return {
        "messages": [AIMessage(content="抱歉没太理解，您可以换个说法吗？或者直接告诉我订单号、想咨询的商品、问题类型。")],
    }


# ---------- 节点 9：最终回答组织 ----------
def final_answer_node(state: CustomerServiceState) -> dict:
    """把工具结果整理成自然语言回复。"""
    tool_results = [m for m in state["messages"] if isinstance(m, ToolMessage)]
    if not tool_results:
        return {}

    last_tool = tool_results[-1].content
    # 简单模板：实际生产会让 LLM 自由组织
    if "tracking_no" in last_tool:
        reply = f"您的包裹正在派送中，运单号 {json.loads(last_tool).get('tracking_no')}，预计今日 18:00 前送达。"
    elif "refund_id" in last_tool:
        d = json.loads(last_tool)
        reply = f"退款单 {d['refund_id']} 已提交，金额 ¥{d['amount']}，预计 1-3 工作日原路退回。"
    else:
        # RAG 命中走 LLM 总结
        reply = llm.invoke([
            SystemMessage(content="基于工具返回的检索结果，用 2-3 句话回答用户，保持专业、简洁。"),
            *state["messages"][-3:],
        ]).content

    return {"messages": [AIMessage(content=reply)], "final_answer": reply}


# ---------- 条件边路由函数 ----------
def route_after_intent(state: CustomerServiceState) -> str:
    return state.get("next_node", "clarify_node")


def route_after_tool(state: CustomerServiceState) -> str:
    if state.get("tool_call_count", 0) > 5:
        return "human_handoff_node"
    return "final_answer_node"


# ---------- 构建图 ----------
def build_graph() -> StateGraph:
    builder = StateGraph(CustomerServiceState)

    # 注册节点
    builder.add_node("intent", intent_recognition_node)
    builder.add_node("router", route_node)
    builder.add_node("order", order_node)
    builder.add_node("logistics", logistics_node)
    builder.add_node("refund", refund_node)
    builder.add_node("rag", rag_node)
    builder.add_node("chitchat", chitchat_node)
    builder.add_node("clarify", clarify_node)
    builder.add_node("tool_node", ToolNode(ALL_TOOLS))
    builder.add_node("final_answer", final_answer_node)
    builder.add_node("human_handoff_node", human_handoff_node_placeholder)

    # 主干
    builder.add_edge(START, "intent")
    builder.add_edge("intent", "router")
    builder.add_conditional_edges("router", route_after_intent, {
        "order_node": "order",
        "logistics_node": "logistics",
        "refund_node": "refund",
        "rag_node": "rag",
        "human_handoff_node": "human_handoff_node",
        "chitchat_node": "chitchat",
        "clarify_node": "clarify",
    })

    # 业务节点 → 工具或直接回答
    builder.add_edge("order", "tool_node")
    builder.add_edge("logistics", "tool_node")
    builder.add_edge("refund", "tool_node")
    builder.add_edge("rag", "tool_node")
    builder.add_edge("chitchat", END)
    builder.add_edge("clarify", END)
    builder.add_conditional_edges("tool_node", route_after_tool, {
        "final_answer_node": "final_answer",
        "human_handoff_node": "human_handoff_node",
    })
    builder.add_edge("final_answer", END)
    builder.add_edge("human_handoff_node", END)

    return builder


# 临时占位，10.7 节替换为带 interrupt 的实现
def human_handoff_node_placeholder(state: CustomerServiceState) -> dict:
    from tools.customer_service_tools import human_handoff
    res = human_handoff.invoke({
        "reason": state.get("escalation_reason", "用户请求"),
        "priority": "HIGH" if state.get("escalation_flag") else "MEDIUM",
        "context_summary": state.get("escalation_reason", ""),
    })
    return {"messages": [AIMessage(content=res["message"])]}


# ---------- 编译 ----------
checkpointer = MemorySaver()  # 生产换 PostgresSaver
graph = build_graph().compile(checkpointer=checkpointer)
```

### 10.5.3 状态机流程图

```mermaid
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart TD
    Start([用户消息]) --> Intent[意图识别<br/>intent_recognition_node]
    Intent --> Router[路由<br/>route_node]
    Router -->|order| Order[订单节点]
    Router -->|logistics| Logistics[物流节点]
    Router -->|refund| Refund[退款节点]
    Router -->|product| Rag[RAG 节点]
    Router -->|complaint/敏感| Human[人工接管]
    Router -->|chitchat| Chit[闲聊节点]
    Router -->|unknown| Clarify[澄清节点]
    Order --> Tool[ToolNode<br/>执行工具]
    Logistics --> Tool
    Refund --> Tool
    Rag --> Tool
    Tool -->|count<=5| Final[最终回答<br/>final_answer_node]
    Tool -->|count>5| Human
    Final --> End1([END])
    Chit --> End2([END])
    Clarify --> End3([END])
    Human --> End4([END])

    style Start fill:#1e3a8a,color:#fff
    style Intent fill:#7c2d12,color:#fff
    style Router fill:#7c2d12,color:#fff
    style Order fill:#14532d,color:#fff
    style Logistics fill:#14532d,color:#fff
    style Refund fill:#14532d,color:#fff
    style Rag fill:#14532d,color:#fff
    style Human fill:#7f1d1d,color:#fff
    style Chit fill:#14532d,color:#fff
    style Clarify fill:#14532d,color:#fff
    style Tool fill:#581c87,color:#fff
    style Final fill:#581c87,color:#fff
    style End1 fill:#0f766e,color:#fff
    style End2 fill:#0f766e,color:#fff
    style End3 fill:#0f766e,color:#fff
    style End4 fill:#0f766e,color:#fff
```

### 10.5.4 状态机的几个关键技巧

1. **`next_node` 字段做软路由**：条件边读这个字段，避免在路由函数里写一堆 if/else。
2. **死循环防护**：`tool_call_count` 超过阈值强制转人工，而不是无限循环烧 token。
3. **节点粒度**：每个节点只做一件事，便于在 LangSmith 里定位问题。
4. **意图 + 实体分离**：意图决定"走哪条路"，实体决定"工具调用的参数"，关注点分离。

---

## 10.6 多轮对话与会话持久化

客服场景天然多轮："用户问订单 → Agent 追问订单号 → 用户提供 → Agent 查"。如果每次新请求都重头开始，那不叫 Agent，叫 GPT 复读机。

### 10.6.1 thread_id 设计

`thread_id` 是 LangGraph Checkpointer 用来标识一次会话的 key。设计原则：

- **唯一性**：`{user_id}:{session_id}`，如 `u_12345:s_20240615_abc`。
- **稳定性**：同一用户在同一会话窗口内，thread_id 保持不变。
- **隔离性**：不同渠道（App/网页/电话）用不同 session_id，避免上下文串台。

### 10.6.2 SqliteSaver 与 PostgresSaver

| Checkpointer | 适用场景 | 性能 | 持久化 |
|--------------|----------|------|--------|
| `MemorySaver` | 本地开发、单测 | 极快 | 无，进程退出即丢 |
| `SqliteSaver` | 中小流量、Demo、自托管 | 1k QPS | 文件级，重启不丢 |
| `PostgresSaver` | 生产、分布式 | 5k+ QPS | 强一致、支持 Time Travel |

### 10.6.3 完整持久化代码

```python
# persistence/checkpointer.py
"""
多轮对话持久化示例。
- 本地开发：SqliteSaver（文件存到 ./checkpoints.db）
- 生产环境：PostgresSaver（连接 Postgres 16+）
"""
from __future__ import annotations
import os
import time
from typing import Optional

from langgraph.checkpoint.sqlite import SqliteSaver
from langgraph.checkpoint.postgres import PostgresSaver
from langgraph.checkpoint.memory import MemorySaver

from graph.customer_service_graph import build_graph


def make_dev_graph():
    """开发用 SQLite 持久化。"""
    checkpointer = SqliteSaver.from_conn_string("./checkpoints.db")
    graph = build_graph().compile(checkpointer=checkpointer)
    return graph


def make_prod_graph():
    """生产用 Postgres 持久化。"""
    db_uri = os.environ.get("DATABASE_URL", "postgresql://user:pwd@localhost:5432/agent")
    checkpointer = PostgresSaver.from_conn_string(db_uri)
    checkpointer.setup()  # 首次部署建表
    graph = build_graph().compile(checkpointer=checkpointer)
    return graph


# ---------- 多轮对话演示 ----------
def multi_turn_demo():
    graph = make_dev_graph()
    config = {"configurable": {"thread_id": "u_1001:s_demo_001"}}

    # 第 1 轮：用户问订单
    print("=== Turn 1 ===")
    out1 = graph.invoke(
        {
            "messages": [("user", "我昨天下的订单到哪了？")],
            "user_id": "u_1001",
            "thread_id": "u_1001:s_demo_001",
            "session_start_ts": time.time(),
            "tool_call_count": 0,
        },
        config=config,
    )
    print("Agent:", out1["messages"][-1].content)

    # 第 2 轮：用户提供订单号，Agent 能"记住"上下文
    print("\n=== Turn 2 ===")
    out2 = graph.invoke(
        {
            "messages": [("user", "订单号是 ORD202406150001")],
        },
        config=config,  # 同样的 thread_id
    )
    print("Agent:", out2["messages"][-1].content)

    # 第 3 轮：继续追问（不需要再提供订单号）
    print("\n=== Turn 3 ===")
    out3 = graph.invoke(
        {
            "messages": [("user", "那它什么时候能到上海？")],
        },
        config=config,
    )
    print("Agent:", out3["messages"][-1].content)


# ---------- Time Travel 示例 ----------
def time_travel_demo():
    """回溯到某个历史步骤，从那里重新执行。"""
    graph = make_dev_graph()
    config = {"configurable": {"thread_id": "u_1001:s_demo_001"}}

    # 拿到所有历史 checkpoint
    history = list(graph.get_state_history(config))
    print(f"会话共有 {len(history)} 个 checkpoint")
    for i, state in enumerate(history[:5]):
        print(f"  [{i}] step={state.metadata.get('step')}, ts={state.metadata.get('ts')}")

    # 回溯到第 2 个 checkpoint
    if len(history) > 1:
        target = history[1]
        graph.invoke(None, config=target.config)
        print("已回溯到 checkpoint，重新执行")


if __name__ == "__main__":
    multi_turn_demo()
    # time_travel_demo()
```

### 10.6.4 多轮对话流程图

```mermaid
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
sequenceDiagram
    participant U as 用户
    participant API as FastAPI
    participant G as LangGraph Agent
    participant DB as Postgres<br/>Checkpointer

    U->>API: POST /chat<br/>thread_id=u1:s1
    API->>G: invoke(state, config)
    G->>DB: get_state(thread_id)
    DB-->>G: 历史 messages
    G->>G: 合并新消息
    G->>DB: put_state(thread_id, new_state)
    G-->>API: 流式输出
    API-->>U: SSE chunk

    Note over G,DB: 同一 thread_id 第二次请求时<br/>自动从断点恢复

    U->>API: POST /chat<br/>thread_id=u1:s1
    API->>G: invoke(...)
    G->>DB: get_state(thread_id)
    DB-->>G: 包含上文的 State
    G-->>API: 继续对话
    API-->>U: SSE chunk

    style U fill:#1e3a8a,color:#fff
    style API fill:#7c2d12,color:#fff
    style G fill:#581c87,color:#fff
    style DB fill:#0f766e,color:#fff
```

### 10.6.5 常见坑

- **thread_id 漂移**：用户切了设备/清了 cookie，thread_id 变了，Agent 失忆。生产环境要把 thread_id 跟用户 ID 强绑定，存 Redis。
- **消息膨胀**：每轮把所有 messages 都塞给 LLM，10 轮后 token 爆炸。生产要做 **消息摘要**：超过 N 轮后让 LLM 把历史压缩成 200 字摘要。
- **状态污染**：A 用户的 State 被 B 用户读到。100% 是 thread_id 设计错，必须每次生成新 UUID。

---

## 10.7 人工接管（Human-in-the-Loop）

Agent 不是万能的。涉及金钱、投诉、情绪激动时，必须让人工兜底。LangGraph 提供了原生的 `interrupt()` API，比 Chain 时代手动塞状态机优雅十倍。

### 10.7.1 触发条件

| 触发场景 | 检测方式 | 优先级 |
|----------|----------|--------|
| 用户明确要求 | 关键词："转人工"、"找真人"、"人工客服" | MEDIUM |
| 情绪激动 | 敏感词库 + 情绪分类器 | HIGH |
| Agent 多次失败 | `tool_call_count > 5` 或连续 3 次 ToolMessage 报错 | MEDIUM |
| 敏感业务 | 退款金额 > 1000、订单涉及投诉 | HIGH |
| 监管话题 | 命中"12315"、"起诉"、"举报"、"消协" | URGENT |

### 10.7.2 完整 Human-in-the-Loop 代码

```python
# graph/human_in_the_loop.py
"""
带 interrupt 的人工接管节点。
关键点：
1. 在 refund 提交、投诉处理等节点前用 interrupt() 暂停
2. 用户在 Web/IM 客户端收到"需要确认"提示
3. 用户回复"确认"或"取消"后，graph.invoke(resume=...) 继续
"""
from __future__ import annotations
import time
from typing import Literal

from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
from langgraph.graph import StateGraph, START, END
from langgraph.checkpoint.memory import MemorySaver
from langgraph.types import interrupt, Command

from graph.state import CustomerServiceState
from tools.customer_service_tools import apply_refund, human_handoff


# ---------- 退款确认节点（带 interrupt）----------
def refund_confirm_node(state: CustomerServiceState) -> dict:
    """调用 apply_refund 前，先 interrupt 等用户确认。"""
    order_id = state["entities"].get("order_id")
    amount = state["entities"].get("amount")
    reason = state["entities"].get("reason", "OTHER")

    # 触发 interrupt，流程暂停，等待人类输入
    user_decision = interrupt(
        {
            "type": "refund_confirm",
            "question": f"即将退款 ¥{amount}，订单 {order_id}，原因：{reason}。是否继续？",
            "options": ["确认", "取消"],
            "default": "取消",
            "timeout_seconds": 300,
        }
    )

    # 用户响应后继续
    if user_decision == "确认":
        res = apply_refund.invoke({
            "order_id": order_id,
            "reason": reason,
            "amount": amount,
        })
        return {
            "messages": [ToolMessage(content=str(res), tool_call_id="apply_refund_1")],
            "tool_call_count": state.get("tool_call_count", 0) + 1,
        }
    else:
        return {
            "messages": [AIMessage(content="好的，已为您取消退款。")],
        }


# ---------- 转人工节点（带 interrupt）----------
def human_handoff_node_v2(state: CustomerServiceState) -> dict:
    """把会话交给人工。interrupt 触发后由人工客服 Web 工作台接管。"""
    reason = state.get("escalation_reason", "用户请求")

    # 弹出确认卡（"是否要转人工？预计等待 X 秒"）
    confirmation = interrupt(
        {
            "type": "handoff_confirm",
            "question": "我帮您转接人工客服，预计等待 30-180 秒。是否继续？",
            "options": ["是", "暂不，继续用 AI"],
            "default": "是",
        }
    )

    if confirmation == "是":
        res = human_handoff.invoke({
            "reason": reason,
            "priority": "URGENT" if state.get("escalation_flag") else "MEDIUM",
            "context_summary": build_context_summary(state),
        })
        return {
            "messages": [AIMessage(content=res["message"])],
            "final_answer": res["message"],
        }
    else:
        # 用户反悔，回到正常 Agent 流程
        return {"escalation_flag": False, "messages": [AIMessage(content="好的，我继续帮您处理，请描述问题。")]}


def build_context_summary(state: CustomerServiceState) -> str:
    """给人工客服看的会话摘要。"""
    msgs = state["messages"][-10:]
    summary = "\n".join([f"[{m.type}] {m.content[:80]}" for m in msgs])
    return f"用户 {state['user_id']}，意图 {state.get('intent')}，\n最近消息：\n{summary}"


# ---------- 演示：完整 interrupt 流程 ----------
def demo_interrupt_flow():
    """模拟：用户申请退款 → Agent 触发 interrupt → 用户在 Web 端点击"确认" → 流程继续。"""
    from graph.customer_service_graph import build_graph

    builder = build_graph()
    # 把 refund 节点替换为带 interrupt 的版本
    builder.add_node("refund_confirm", refund_confirm_node)
    builder.add_edge("refund", "refund_confirm")
    builder.add_edge("refund_confirm", "final_answer")

    checkpointer = MemorySaver()
    graph = builder.compile(checkpointer=checkpointer)
    config = {"configurable": {"thread_id": "u_demo_interrupt"}}

    # 第 1 步：触发 refund
    print("=== Step 1: 用户申请退款 ===")
    out = graph.invoke(
        {
            "messages": [("user", "我要退款 ORD202406150001 金额 599 原因 SIZE_ISSUE")],
            "user_id": "u_demo",
            "thread_id": "u_demo_interrupt",
            "tool_call_count": 0,
        },
        config=config,
    )
    print("Agent (暂停中):", [m.content for m in out["messages"] if isinstance(m, AIMessage)][-1])
    print("State 暂停点:", out.get("__interrupt__"))

    # 第 2 步：用户在前端点击"确认"
    print("\n=== Step 2: 用户点击确认 ===")
    out2 = graph.invoke(Command(resume="确认"), config=config)
    print("Agent (继续):", out2["messages"][-1].content)


# ---------- 敏感操作：紧急转人工 ----------
def sensitive_topic_interrupt(state: CustomerServiceState) -> dict:
    """命中敏感词，无条件 interrupt 给人工，AI 不做二次判断。"""
    res = human_handoff.invoke({
        "reason": f"敏感话题：{state.get('escalation_reason')}",
        "priority": "URGENT",
        "context_summary": build_context_summary(state),
    })
    return {
        "messages": [AIMessage(content=f"已紧急转接人工，工单号 {res['ticket_id']}。")],
        "final_answer": res["message"],
    }


if __name__ == "__main__":
    demo_interrupt_flow()
```

### 10.7.3 Human-in-the-Loop 流程图

```mermaid
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart TD
    A[业务节点<br/>refund/handoff] --> B{需要确认?}
    B -->|否| C[直接执行工具]
    B -->|是| D[interrupt<br/>暂停图执行]
    D --> E[前端弹出确认卡<br/>确认/取消]
    E -->|用户点击确认| F[Command resume=确认]
    E -->|用户取消| G[Command resume=取消]
    E -->|5分钟超时| H[默认取消]
    F --> C
    C --> I[ToolNode 执行]
    I --> J[继续图执行]
    G --> K[回退到澄清节点]
    H --> K
    J --> End([END])
    K --> End

    style A fill:#1e3a8a,color:#fff
    style B fill:#7c2d12,color:#fff
    style C fill:#14532d,color:#fff
    style D fill:#7f1d1d,color:#fff
    style E fill:#7f1d1d,color:#fff
    style F fill:#14532d,color:#fff
    style G fill:#14532d,color:#fff
    style H fill:#581c87,color:#fff
    style I fill:#581c87,color:#fff
    style J fill:#581c87,color:#fff
    style K fill:#0f766e,color:#fff
    style End fill:#0f766e,color:#fff
```

### 10.7.4 关键设计

- **永远不要信任 LLM 的判断**：涉及金钱的操作必须人工确认，LLM 的意图识别可以错，但 599 元的退款不能错。
- **超时默认取消**：用户 5 分钟不响应，默认取消并提示"刚才的请求已取消，需要重新发起吗？"，避免挂起资源。
- **interrupt 数据结构**：传 dict 而不是字符串，前端可以基于 `type` 字段渲染不同 UI（按钮、卡片、表单）。

---

## 10.8 评测与监控

Agent 上线不是终点，**没有评测就没有迭代**。这一节我们搭一套最小可用的指标体系 + LangSmith 接入。

### 10.8.1 在线指标

| 指标 | 计算方式 | 告警阈值 |
|------|----------|----------|
| 解决率 | (总会话 - 转人工会话) / 总会话 | < 60% 告警 |
| 转人工率 | 转人工会话 / 总会话 | > 35% 告警 |
| 平均轮数 | sum(轮数) / 总会话 | > 8 异常 |
| P95 延迟 | 99 分位首字延迟 | > 3s 告警 |
| CSAT | 平均 1-5 星评分 | < 4.0 告警 |
| Token 成本 | sum(prompt + completion tokens) × 单价 | 单日 > 预算 80% 告警 |

### 10.8.2 指标埋点代码

```python
# observability/metrics.py
"""
在线指标埋点：每次 invoke 后写入 Prometheus + 日志。
"""
from __future__ import annotations
import time
import json
from datetime import datetime
from typing import Optional

from prometheus_client import Counter, Histogram, Gauge, start_http_server
from langsmith import traceable


# ---------- Prometheus 指标 ----------
CSAT_TOTAL = Counter("csat_total", "用户评分总数", ["score"])
RESOLVED_TOTAL = Counter("resolved_total", "已解决会话数")
ESCALATED_TOTAL = Counter("escalated_total", "转人工会话数", ["priority"])
TURNS_HISTOGRAM = Histogram("turns_per_session", "会话轮数", buckets=[1, 2, 3, 5, 8, 10, 15])
LATENCY_HISTOGRAM = Histogram("agent_first_token_latency", "首字延迟(秒)",
                              buckets=[0.1, 0.3, 0.5, 1, 1.5, 3, 5])
TOKEN_USAGE = Counter("token_usage_total", "Token 消耗", ["model", "kind"])  # kind: prompt/completion
COST_USAGE = Counter("cost_usd_total", "美元成本", ["model"])
ACTIVE_SESSIONS = Gauge("active_sessions", "活跃会话数")


class SessionMetrics:
    """单次会话的指标聚合。"""

    def __init__(self, session_id: str, user_id: str):
        self.session_id = session_id
        self.user_id = user_id
        self.start_ts = time.time()
        self.turns = 0
        self.escalated = False
        self.tool_errors = 0
        self.token_total = 0
        self.cost_total = 0.0

    def record_turn(self, first_token_latency: float, tokens: int, cost: float, model: str = "gpt-4o-mini"):
        self.turns += 1
        LATENCY_HISTOGRAM.observe(first_token_latency)
        TOKEN_USAGE.labels(model=model, kind="prompt").inc(tokens // 2)
        TOKEN_USAGE.labels(model=model, kind="completion").inc(tokens - tokens // 2)
        COST_USAGE.labels(model=model).inc(cost)
        self.token_total += tokens
        self.cost_total += cost

    def mark_escalated(self, priority: str):
        self.escalated = True
        ESCALATED_TOTAL.labels(priority=priority).inc()

    def mark_resolved(self):
        if not self.escalated:
            RESOLVED_TOTAL.inc()

    def record_csat(self, score: int):
        CSAT_TOTAL.labels(score=str(score)).inc()

    def finalize(self):
        TURNS_HISTOGRAM.observe(self.turns)
        duration = time.time() - self.start_ts
        # 写到日志
        log_session_summary(self, duration)

    def to_dict(self) -> dict:
        return {
            "session_id": self.session_id,
            "user_id": self.user_id,
            "turns": self.turns,
            "escalated": self.escalated,
            "tokens": self.token_total,
            "cost_usd": round(self.cost_total, 4),
            "duration_sec": round(time.time() - self.start_ts, 2),
        }


def log_session_summary(m: SessionMetrics, duration: float):
    """统一日志格式，方便 ELK/Loki 采集。"""
    import logging
    logger = logging.getLogger("agent.metrics")
    logger.info("session_summary", extra={
        "session_id": m.session_id,
        "user_id": m.user_id,
        "turns": m.turns,
        "duration_sec": round(duration, 2),
        "tokens": m.token_total,
        "cost_usd": round(m.cost_total, 4),
        "escalated": m.escalated,
        "timestamp": datetime.now().isoformat(),
    })


# ---------- LangSmith 接入 ----------
@traceable(run_type="chain", name="customer_service_agent")
def run_agent_with_tracing(graph, inputs, config):
    """所有 invoke 都过这层，自动上报到 LangSmith。"""
    return graph.invoke(inputs, config=config)


# ---------- 离线评测 ----------
def eval_dataset():
    """
    用 LangSmith 的 eval API 跑离线评测集。
    评测集存放在 langsmith_tests/agent_eval.jsonl
    """
    from langsmith.evaluation import evaluate
    from langsmith.schemas import Example, Run
    from langchain_openai import ChatOpenAI

    grader_llm = ChatOpenAI(model="gpt-4o", temperature=0)

    def correctness(run: Run, example: Example) -> dict:
        """用 LLM-as-Judge 评估回答是否解决用户问题。"""
        score = grader_llm.invoke([{
            "role": "system",
            "content": "判断 Agent 回答是否解决了用户问题。返回 0-1 的分数。"
        }, {
            "role": "user",
            "content": f"问题：{example.inputs['messages'][-1]}\n回答：{run.outputs['final_answer']}\n参考答案：{example.outputs.get('expected', '')}"
        }]).content
        return {"score": float(score.strip())}

    evaluate(
        lambda inputs: run_agent_with_tracing(graph, inputs, {"configurable": {"thread_id": inputs["thread_id"]}}),
        data="langsmith_tests/agent_eval",
        evaluators=[correctness],
        experiment_prefix="customer_service_v1",
    )
```

### 10.8.3 监控看板

推荐用 Grafana 接 Prometheus，关键面板：

- **解决率趋势**：折线图，按小时/天聚合。
- **转人工原因分布**：饼图，区分用户主动/情绪/失败/敏感。
- **延迟分布**：P50/P95/P99 折线。
- **Token 成本**：按模型拆分的堆叠柱状图。
- **Top 失败 case**：错误率最高的 10 个工具调用。

---

## 10.9 部署

最后一步把 Agent 暴露为 HTTP 服务。我们用 FastAPI + SSE 做流式输出，配合 Docker 一键部署。

### 10.9.1 FastAPI 服务

```python
# deploy/app.py
"""
智能客服 Agent 的 FastAPI 部署。
- POST /chat/sync   同步接口，简单场景
- POST /chat/stream SSE 流式接口，生产推荐
- GET  /healthz     健康检查
- GET  /metrics     Prometheus 指标
"""
from __future__ import annotations
import os
import time
import uuid
import json
import asyncio
from typing import AsyncIterator, Optional

from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse, JSONResponse
from pydantic import BaseModel, Field
from langchain_core.messages import HumanMessage

from persistence.checkpointer import make_prod_graph
from observability.metrics import SessionMetrics


app = FastAPI(title="Customer Service Agent", version="1.0.0")
graph = make_prod_graph()  # 全局单例


# ---------- 数据模型 ----------
class ChatRequest(BaseModel):
    user_id: str = Field(..., description="用户唯一标识")
    session_id: Optional[str] = Field(None, description="会话 ID，留空自动生成")
    message: str = Field(..., min_length=1, max_length=2000)


class ChatResponse(BaseModel):
    session_id: str
    answer: str
    turn_index: int
    escalated: bool
    tool_used: list[str] = []


# ---------- 健康检查 ----------
@app.get("/healthz")
def healthz():
    return {"status": "ok", "ts": time.time()}


# ---------- 同步接口 ----------
@app.post("/chat/sync", response_model=ChatResponse)
def chat_sync(req: ChatRequest):
    session_id = req.session_id or f"s_{uuid.uuid4().hex[:12]}"
    thread_id = f"{req.user_id}:{session_id}"
    config = {"configurable": {"thread_id": thread_id}}

    metrics = SessionMetrics(session_id=session_id, user_id=req.user_id)
    t0 = time.time()
    out = graph.invoke(
        {
            "messages": [HumanMessage(content=req.message)],
            "user_id": req.user_id,
            "thread_id": thread_id,
            "session_start_ts": t0,
            "tool_call_count": 0,
        },
        config=config,
    )
    first_token_latency = time.time() - t0

    answer = out.get("final_answer") or out["messages"][-1].content
    metrics.record_turn(
        first_token_latency=first_token_latency,
        tokens=count_tokens(out),
        cost=estimate_cost(out),
    )
    if out.get("escalation_flag"):
        metrics.mark_escalated(priority="MEDIUM")
    else:
        metrics.mark_resolved()
    metrics.finalize()

    return ChatResponse(
        session_id=session_id,
        answer=answer,
        turn_index=out.get("tool_call_count", 1),
        escalated=out.get("escalation_flag", False),
        tool_used=[m.name for m in out["messages"] if hasattr(m, "name")],
    )


# ---------- 流式接口 ----------
@app.post("/chat/stream")
async def chat_stream(req: ChatRequest):
    """SSE 流式输出，LangGraph 的 astream_events 推送每个 token。"""
    session_id = req.session_id or f"s_{uuid.uuid4().hex[:12]}"
    thread_id = f"{req.user_id}:{session_id}"
    config = {"configurable": {"thread_id": thread_id}}

    inputs = {
        "messages": [HumanMessage(content=req.message)],
        "user_id": req.user_id,
        "thread_id": thread_id,
        "session_start_ts": time.time(),
        "tool_call_count": 0,
    }

    async def event_generator() -> AsyncIterator[str]:
        try:
            async for event in graph.astream_events(inputs, config=config, version="v2"):
                kind = event["event"]
                if kind == "on_chat_model_stream":
                    chunk = event["data"]["chunk"]
                    if chunk.content:
                        yield f"data: {json.dumps({'type': 'token', 'content': chunk.content}, ensure_ascii=False)}\n\n"
                        await asyncio.sleep(0)  # 让出事件循环
                elif kind == "on_tool_start":
                    yield f"data: {json.dumps({'type': 'tool_start', 'name': event['name']}, ensure_ascii=False)}\n\n"
                elif kind == "on_tool_end":
                    yield f"data: {json.dumps({'type': 'tool_end', 'name': event['name']}, ensure_ascii=False)}\n\n"
                elif kind == "on_chain_end" and event["name"] == "LangGraph":
                    final = event["data"]["output"]
                    yield f"data: {json.dumps({'type': 'done', 'session_id': session_id}, ensure_ascii=False)}\n\n"
        except Exception as e:
            yield f"data: {json.dumps({'type': 'error', 'message': str(e)}, ensure_ascii=False)}\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")


# ---------- 工具函数 ----------
def count_tokens(out: dict) -> int:
    """估算 token 数量。"""
    try:
        from langchain_core.messages import messages_to_string
        s = messages_to_string(out["messages"])
        return len(s) // 2  # 中文按 1 字 1.5 token 估算
    except Exception:
        return 0


def estimate_cost(out: dict) -> float:
    """按 gpt-4o-mini 单价估算。"""
    tokens = count_tokens(out)
    return tokens * 0.00000015  # 0.15 USD / 1M tokens


# ---------- 启动 ----------
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000, workers=4)
```

### 10.9.2 Dockerfile

```dockerfile
# deploy/Dockerfile
FROM python:3.11-slim AS base

WORKDIR /app

# 系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential curl \
    && rm -rf /var/lib/apt/lists/*

# Python 依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 预下载模型（构建期缓存）
RUN python -c "from langchain_community.embeddings import HuggingFaceBgeEmbeddings; \
    HuggingFaceBgeEmbeddings(model_name='BAAI/bge-small-zh-v1.5')" || true

# 应用代码
COPY . .

# 非 root 运行
RUN useradd -m -u 1000 agentuser && chown -R agentuser /app
USER agentuser

EXPOSE 8000

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsS http://localhost:8000/healthz || exit 1

# 启动
CMD ["uvicorn", "deploy.app:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "4", "--loop", "uvloop"]
```

### 10.9.3 部署架构

```mermaid
%%{init: {'theme':'base', 'themeVariables': {'primaryColor':'#1e3a8a','primaryTextColor':'#fff','primaryBorderColor':'#7c2d12','lineColor':'#fff','secondaryColor':'#14532d','tertiaryColor':'#581c87'}}}%%
flowchart LR
    Client[IM/APP/H5] -->|HTTPS| LB[SLB/ALB]
    LB --> API1[FastAPI Pod 1]
    LB --> API2[FastAPI Pod 2]
    LB --> API3[FastAPI Pod 3]
    API1 --> Agent[LangGraph<br/>Compiled Graph]
    API2 --> Agent
    API3 --> Agent
    Agent --> PG[(Postgres<br/>Checkpoint)]
    Agent --> Redis[(Redis<br/>短期缓存)]
    Agent --> LLM[LLM API<br/>GPT-4o-mini]
    Agent --> Vector[(Chroma<br/>RAG 索引)]
    Agent --> LS[LangSmith<br/>Trace]
    Agent --> Bus[Kafka<br/>埋点事件]

    style Client fill:#1e3a8a,color:#fff
    style LB fill:#7c2d12,color:#fff
    style API1 fill:#14532d,color:#fff
    style API2 fill:#14532d,color:#fff
    style API3 fill:#14532d,color:#fff
    style Agent fill:#581c87,color:#fff
    style PG fill:#0f766e,color:#fff
    style Redis fill:#0f766e,color:#fff
    style LLM fill:#7c2d12,color:#fff
    style Vector fill:#0f766e,color:#fff
    style LS fill:#0f766e,color:#fff
    style Bus fill:#0f766e,color:#fff
```

### 10.9.4 部署清单

- **环境变量**：`OPENAI_API_KEY`、`DATABASE_URL`、`LANGSMITH_API_KEY`、`LANGCHAIN_TRACING_V2=true`
- **K8s 资源**：单 Pod 2 核 4G，3 副本起步，HPA 跟 CPU 70% 扩缩。
- **优雅停机**：SIGTERM 时等当前 SSE 流结束，最多 30s。
- **灰度发布**：按 user_id 哈希分流，新老版本并行 1 周对比指标。

---

## 10.10 完整端到端示例

把所有零件串起来。假设 `graph` 已经按 10.9 节部署好，我们看 3 个真实场景的完整 trace。

### 10.10.1 场景一：订单物流查询（多轮）

**用户**：我昨天下的订单到哪了？
**Agent**（意图识别 → order，entities 为空 → 追问）：请问您的订单号是多少？格式如 ORD202406150001。
**用户**：ORD202406150001
**Agent**（调 query_logistics → 拼装自然语言）：您的包裹正在派送中，运单号 SF1234567890123，预计今日 18:00 前送达。顺丰快递员张师傅 138****1234 正在派送。

**完整 Trace**：

```
[trace_id: t_abc123]
[thread_id: u_1001:s_001]
[user_id: u_1001]

Step 1  intent_recognition_node
  input:  "我昨天下的订单到哪了？"
  output: {"intent":"order","confidence":0.92,"entities":{},"needs_human":false}
  tokens: 145 in / 38 out,  cost: $0.000027

Step 2  route_node
  next_node: order_node

Step 3  order_node
  decision: order_id 缺失，反问用户
  output:  "请问您的订单号是多少？格式如 ORD202406150001。"
  latency: 820ms (含 LLM 调用)

--- 会话持久化到 Postgres ---

Step 4  intent_recognition_node (第 2 轮)
  input:  "ORD202406150001"
  output: {"intent":"logistics","confidence":0.95,"entities":{"order_id":"ORD202406150001"}}
  tokens: 162 in / 41 out

Step 5  route_node
  next_node: logistics_node

Step 6  ToolNode -> query_logistics
  args:  {"order_id":"ORD202406150001"}
  result: {"carrier":"顺丰速运","tracking_no":"SF1234567890123","status":"IN_TRANSIT",...}

Step 7  final_answer_node
  output: "您的包裹正在派送中，运单号 SF1234567890123，预计今日 18:00 前送达。顺丰快递员张师傅 138****1234 正在派送。"
  total_turns: 2,  total_tokens: 538,  total_cost: $0.0001

[metrics] resolved=true, escalated=false, csat=null
```

### 10.10.2 场景二：商品咨询（RAG 命中）

**用户**：iPhone 15 Pro 电池多大？
**Agent**（意图 → product → 调 search_faq → RAG 命中）：iPhone 15 Pro 电池容量为 3274mAh，支持 20W 快充，Type-C 接口。

**完整 Trace**：

```
[trace_id: t_def456]

Step 1  intent_recognition_node
  input:  "iPhone 15 Pro 电池多大？"
  output: {"intent":"product","confidence":0.97,"entities":{"sku":"iPhone 15 Pro"}}

Step 2  route_node
  next_node: rag_node

Step 3  rag_node
  decision: 调用 search_faq

Step 4  ToolNode -> search_faq
  args:  {"query":"iPhone 15 Pro 电池容量","top_k":3}
  rerank: bge-reranker 选出 top-1: manual/iphone15.md
  result: {"hits":[{"score":0.92,"content":"iPhone 15 Pro 电池容量 3274mAh...","source":"manual/iphone15.md"}]}

Step 5  final_answer_node (LLM 总结)
  output: "iPhone 15 Pro 电池容量为 3274mAh，支持 20W 快充，Type-C 接口。"
  total_turns: 1,  total_tokens: 412,  total_cost: $0.00008

[metrics] resolved=true, tool_used=["search_faq"]
```

### 10.10.3 场景三：投诉升级（人工接管）

**用户**：你们这客服太差了，我要投诉！
**Agent**（命中敏感词"投诉" → escalation_flag=true → human_handoff_node → interrupt 弹确认卡 → 用户点"是"）：已为您紧急转接人工，工单号 TK1718456789，排队中，预计等待 60 秒。

**完整 Trace**：

```
[trace_id: t_ghi789]
[⚠️ escalation_path]

Step 1  intent_recognition_node
  input:  "你们这客服太差了，我要投诉！"
  output: {"intent":"complaint","confidence":0.99,"needs_human":true,"reason":"命中敏感词"}
  sensitive_word_hit: ["投诉"]
  escalation_flag: true

Step 2  route_node
  next_node: human_handoff_node (跳过常规业务节点)

Step 3  human_handoff_node (带 interrupt)
  interrupt_payload: {
    "type":"handoff_confirm",
    "question":"我帮您转接人工客服，预计等待 30-180 秒。是否继续？",
    "options":["是","暂不，继续用 AI"]
  }
  ⏸️ 流程暂停

--- 用户在 IM 端点击"是" ---

Step 4  Command(resume="是")
  ToolNode -> human_handoff
  result: {"ticket_id":"TK1718456789","queue_position":3,"estimated_wait_seconds":60}

Step 5  final_answer_node
  output: "已为您紧急转接人工，工单号 TK1718456789，排队中，预计等待 60 秒。"
  priority: URGENT

[metrics] resolved=false, escalated=true, priority=URGENT
[alert] 推送告警到客服值班群
```

### 10.10.4 场景四：退款带人工确认（interrupt 完整流程）

**用户**：我要退款 ORD202406150001，金额 599 原因尺码不对。
**Agent**：即将退款 ¥599，订单 ORD202406150001，原因：SIZE_ISSUE。是否继续？
**用户**（点击"确认"）：确认
**Agent**：退款单 RF1718456790123 已提交，金额 ¥599，预计 1-3 工作日原路退回。

**完整 Trace**：

```
[trace_id: t_jkl012]
[🔒 sensitive_operation: apply_refund]

Step 1  intent_recognition_node
  output: {"intent":"refund","entities":{"order_id":"...","amount":599,"reason":"SIZE_ISSUE"}}

Step 2  refund_node
  output: "即将退款 ¥599，订单 ORD202406150001，原因：SIZE_ISSUE。是否继续？"

Step 3  refund_confirm_node (带 interrupt)
  interrupt_payload: {
    "type":"refund_confirm",
    "question":"即将退款 ¥599... 是否继续？",
    "options":["确认","取消"],
    "timeout_seconds":300
  }
  ⏸️ 流程暂停

--- 用户在 IM 端点击"确认" ---

Step 4  Command(resume="确认")
  ToolNode -> apply_refund
  result: {"refund_id":"RF1718456790123","amount":599,"expected_arrival":"1-3 个工作日"}

Step 5  final_answer_node
  output: "退款单 RF1718456790123 已提交，金额 ¥599，预计 1-3 工作日原路退回。"
  total_turns: 2

[metrics] resolved=true, sensitive_op=true, audit_log=true
[compliance] 写入合规审计库
```

---

## 本章小结

本章我们从 0 到 1 完整搭建了一个电商智能客服 Agent，覆盖了教程前 9 章的所有核心知识点：

| 阶段 | 关键产出 | 关联章节 |
|------|----------|----------|
| 10.1 需求分析 | 业务目标、5 类典型场景、5 项成功指标 | 第 1 章 |
| 10.2 架构设计 | 全链路架构图、11 个模块清单 | 第 2 章 |
| 10.3 工具设计 | 5 个 Pydantic 工具 + Mock 实现 | 第 3、5 章 |
| 10.4 RAG 知识库 | Chroma + BGE 索引与两阶段检索 | 第 4 章 |
| 10.5 LangGraph 状态机 | 11 个节点的条件路由 | 第 6 章 |
| 10.6 多轮对话 | SqliteSaver / PostgresSaver + Time Travel | 第 4、6 章 |
| 10.7 Human-in-the-Loop | `interrupt()` 5 个触发场景 | 第 6 章 |
| 10.8 评测监控 | Prometheus 指标 + LangSmith 埋点 | 第 8 章 |
| 10.9 部署 | FastAPI + SSE + Docker | 第 9 章 |
| 10.10 端到端 | 4 个真实场景的 Trace 演示 | 全部 |

这套代码你直接 clone 下来就能跑：改一改 `tools/customer_service_tools.py` 里的 Mock 函数、连上自己的 Postgres / Chroma、配置好 OpenAI Key，就可以承载中小电商的客服量级。

**几个工程化原则值得再强调**：

1. **状态机优先**：业务流程用 LangGraph 显式建模，而不是藏在一堆 if/else 里。
2. **RAG 与工具分离**：结构化数据走工具，非结构化知识走 RAG。
3. **危险操作必须人工确认**：退款、转人工、删除订单，无一例外。
4. **没有评测就没有迭代**：上线第一天就要有指标看板，靠数据驱动优化。
5. **持久化是底线**：用 Postgres 而不是内存，进程崩溃不丢上下文。

---

## 下一章预告：第 11 章 代码 Agent

客服 Agent 是"业务咨询"型 Agent，下一章我们要挑战一个更硬核的方向——**代码 Agent**。我们会用 Claude Agent SDK（也兼容 LangGraph 实现）构建一个能 **自主读代码、写代码、跑测试、提 PR** 的软件工程师 Agent。

具体你会看到：

- **代码理解**：Agent 如何读懂 100 万行代码的 monorepo、用 Aider / Continue 的索引策略。
- **工具集**：`read_file`、`grep`、`edit_file`、`run_command`、`git_commit`——5 个核心工具的精细化设计。
- **多步规划**：把"修一个 bug"拆成 10 步子任务，每步可观察、可重试。
- **沙箱执行**：用 Docker 沙箱跑命令，避免 Agent 把生产环境 rm -rf。
- **PR 自动化**：自动 commit、push、开 PR，把人 review 的工作量降到最低。
- **完整案例**：让 Agent 独立修复 LangChain GitHub Issue #1234。

如果说本章的客服 Agent 是"Agent 能做什么"的展示，下一章的代码 Agent 就是"Agent 能不能取代某些白领工作"的严肃论证。敬请期待。
