# Study Notes

个人学习笔记仓库，涵盖 Java 后端、系统设计、算法、云计算等核心技术领域的知识整理。

## 目录结构

```
study-notes/
├── Java/                          # Java 技术栈
│   ├── Java基础/                   # Java 基础语法与 Web 开发
│   ├── java高级/                   # 并发编程、JVM
│   ├── sass-system/               # SaaS 多租户系统
│   ├── 框架/                       # Spring、MyBatis、Dubbo 等框架
│   ├── 源码/                       # 框架源码分析
│   └── 项目/                       # Java 项目实践
│
├── ai-agent/                      # AI Agent
│   ├── agentscope-java/           # AgentScope Java 开发指南
│   ├── langchain/                 # LangChain 学习笔记
│   ├── langgraph/                 # LangGraph 状态图、Checkpoint、多 Agent
│   └── rag/                       # RAG 技术详解
│
├── 系统设计/                       # 系统设计
│   ├── 分布式系统设计/             # 分布式理论与实践
│   ├── 微服务系统设计/             # 微服务架构
│   ├── 架构设计/                   # 软件架构设计原则
│   ├── 广告投放系统设计/           # 程序化广告、RTB、召回排序、计费出价
│   ├── 短视频平台/                 # 上传转码、CDN、推荐系统、Feed流
│   ├── sass平台系统设计/           # 多租户隔离、租户路由、订阅计费、Entitlements
│   ├── 商城平台系统设计/           # 商品、订单、库存、秒杀、支付、售后
│   ├── 供应链系统设计/             # SRM、采购、OMS、WMS、TMS、控制塔
│   ├── ai-agent系统设计/           # 规划、记忆、工具调用、多 Agent 编排
│   └── DDD领域驱动设计/            # 限界上下文、聚合、领域事件、CQRS
│
├── 计算机基础/                     # 计算机基础
│   ├── 核心/                      # 操作系统、计算机网络、网络协议
│   ├── 算法/                      # 数据结构与算法
│   └── linux/                     # Linux 系统
│
├── 数据库/                        # 数据库技术
│   ├── MySql基础.md               # MySQL 基础
│   ├── Mysql高级.md                # MySQL 高级特性
│   ├── Redis基础篇/               # Redis 基础
│   ├── Redis高级篇/               # Redis 高级
│   ├── Redis应用实战/             # Redis 实战
│   └── mongodb/                   # MongoDB
│
├── 云原生/                        # 云原生技术
│   ├── Docker.md                  # Docker 容器化
│   ├── k8s/                      # Kubernetes
│   ├── jenkins/                  # Jenkins CI/CD
│   └── cicd-study/               # CI/CD 实战：Gitea → Jenkins → ArgoCD → K8s
│
├── 中间件/                        # 中间件技术
│   ├── 消息队列/                  # 消息队列
│   └── 负载均衡/                  # 负载均衡
│
├── 其他语言/                      # 其他编程语言
│   ├── go/                       # Go 语言
│   ├── python/                  # Python
│   └── C语言/                    # C 语言
│
└── 前端/                         # 前端技术
    ├── 前端基础/                  # HTML/CSS/JavaScript
    ├── 框架/                     # 前端框架
    ├── UI框架/                   # UI 组件库
    └── 微信小程序/                # 微信小程序
```

## 核心内容

### Java 技术栈
- **基础**: Java 语法、面向对象、集合框架
- **高级**: 并发编程、JVM 原理与调优
- **框架**: Spring、Spring Boot、Spring Cloud、Dubbo、MyBatis
- **生态**: SaaS 多租户系统、分布式事务

### 系统设计
- 分布式系统核心概念（一致性、CAP、PACELC）
- 共识算法（Raft）、分布式事务（Saga、TCC）
- 微服务架构设计原则
- 高可用、高性能、可扩展架构
- 广告投放系统（程序化广告、RTB、召回粗排精排、计费出价、反作弊）
- 短视频平台（上传转码、CDN 分发、推荐系统、Feed 流、直播）
- SaaS 平台（多租户隔离模型、租户标识路由、订阅计费、RBAC/ABAC 权限、Entitlements、租户级监控）
- 商城平台（商品中心、多级缓存、订单、库存、营销计价、秒杀、支付、售后、API 网关）
- 供应链系统（SRM、采购、OMS、WMS、IMS、TMS、末端配送、需求预测、控制塔、跨境供应链）
- AI Agent 系统（规划、记忆与 RAG、Tool Use 与 MCP、多 Agent 协作、评测与可观测性、生产部署）
- DDD 领域驱动设计（限界上下文、实体/值对象/聚合、领域事件、事件风暴、分层架构、CQRS/ES）

### AI Agent
- AgentScope Java 开发
- LangChain 与 LLM 集成
- LangGraph（状态管理、条件路由、Checkpoint、人机交互、多 Agent 系统）
- RAG（检索增强生成）技术

### 计算机基础
- 操作系统原理
- 计算机网络与协议
- 数据结构与算法

### 云原生
- Docker 容器化
- Kubernetes 容器编排
- CI/CD 持续集成与部署
- CI/CD 实战（Gitea + Jenkins + SonarQube + 镜像仓库 + Helm + Argo CD GitOps，含多环境发布与回滚）

## 学习路线建议

```
1. Java 基础 → Java 高级（JVM、并发）→ 主流框架 → 分布式技术
2. 计算机基础 → 系统设计 → 架构实践
3. 传统开发 → 云原生 → DevOps
```

## 使用说明

- 笔记采用 Markdown 格式，推荐使用 VS Code + Markdown All in One 阅读
- 部分笔记包含代码示例，建议配合实际编码练习
- 笔记会持续更新和完善

## 统计

<p align="center">
  <img src="https://img.shields.io/github/languages/count/dd-xiaozhi/study-notes" alt="languages">
  <img src="https://img.shields.io/github/repo-size/dd-xiaozhi/study-notes" alt="size">
  <img src="https://img.shields.io/github/last-commit/dd-xiaozhi/study-notes/main" alt="last commit">
</p>

## 许可证

本仓库内容仅供个人学习交流使用。

