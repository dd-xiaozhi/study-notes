# Jenkins 学习笔记

Jenkins 自动化服务器完整学习笔记，涵盖从安装配置到高级最佳实践的完整知识体系。

## 目录结构

| 章节 | 内容 |
|------|------|
| [第1章：简介与核心概念](./第1章.%20introduction.md) | Jenkins 概述、发展历史、Master/Agent 架构、Job/Build/Pipeline 核心概念 |
| [第2章：安装](./第2章.%20installation.md) | 各平台安装、Docker 部署、Kubernetes 部署、升级与迁移 |
| [第3章：基础配置](./第3章.%20basic-configuration.md) | 系统配置、插件管理、用户认证、邮件通知 |
| [第4章：任务配置](./第4章.%20job-configuration.md) | Freestyle Job、参数化构建、构建触发器、构建步骤 |
| [第5章：插件管理](./第5章.%20plugin-management.md) | 插件安装、插件推荐、插件开发 |
| [第6章：流水线](./第6章.%20pipeline.md) | Declarative Pipeline、Scripted Pipeline、共享库 |
| [第7章：自动化构建](./第7章.%20automation-build.md) | 多语言构建、Docker 构建、多平台构建 |
| [第8章：自动化测试](./第8章.%20automation-testing.md) | 单元测试、集成测试、UI 测试、性能测试 |
| [第9章：自动化部署](./第9章.%20automation-deployment.md) | 蓝绿部署、金丝雀发布、回滚策略 |
| [第10章：安全与权限](./第10章.%20security-and-permissions.md) | 认证授权、角色管理、安全配置 |
| [第11章：分布式构建与节点管理](./第11章.%20distributed-build-and-node-management.md) | Master-Agent 架构、节点配置、分布式执行 |
| [第12章：监控、日志与故障排除](./第12章.%20monitoring-logging-troubleshooting.md) | 监控配置、日志分析、常见问题解决 |
| [第13章：最佳实践](./第13章.%20best-practices.md) | Pipeline 编写规范、安全加固、性能优化 |

## 核心概念速览

```
┌─────────────────────────────────────────────────────────────┐
│                        Jenkins Master                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Job 配置   │  │  调度引擎  │  │   Web Server        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
      ┌─────────┐       ┌─────────┐       ┌─────────┐
      │ Agent 1 │       │ Agent 2 │       │ Agent 3 │
      │ (Linux) │       │ (Docker)│       │ (Win)   │
      └─────────┘       └─────────┘       └─────────┘
```

## 快速开始

### 基础 Pipeline 示例

```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing...'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying...'
            }
        }
    }
}
```

## 学习路径建议

1. **入门**：第1章 → 第2章 → 第3章
2. **实践**：第4章 → 第5章 → 第6章
3. **进阶**：第7章 → 第8章 → 第9章
4. **专家**：第10章 → 第11章 → 第12章 → 第13章

## 适用场景

| 场景 | 适用性 |
|------|--------|
| 中小型团队快速 CI | 中等 |
| 大型企业复杂 CI/CD | 强 |
| 多平台构建（Windows/Linux/macOS） | 强 |
| Kubernetes 部署 | 强 |
| 传统企业（J2EE/.NET） | 强 |
| 纯移动应用（iOS/Android） | 中等 |

---

> 本笔记基于 Jenkins 2.426+ 版本编写