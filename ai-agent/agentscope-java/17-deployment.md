# 第十七章：部署与生产环境

本章介绍如何将 AgentScope Java 应用部署到生产环境。内容包括 Docker 容器化、Kubernetes 部署、监控与追踪、优雅关闭机制以及完整的生产环境配置案例。通过本章的学习，你将掌握将多智能体系统稳定、安全地运行在生产环境中的核心技能。

---

## 17.1 Docker 容器化

Docker 是现代应用部署的基础设施。本节介绍如何为 AgentScope Java 应用构建生产级别的 Docker 镜像。

### 1.1 多阶段构建 Dockerfile

生产环境的 Docker 镜像应该尽可能精简，只包含运行时必需的组件。以下是一个标准的 Java 21 多阶段构建 Dockerfile：

```dockerfile
# Copyright 2024-2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# =============================================================================
# Stage 1: Build Stage - 编译 Java 源码
# =============================================================================
FROM eclipse-temurin:21-jdk AS builder

# 设置工作目录
WORKDIR /build

# 复制 Maven 依赖配置文件
COPY pom.xml .
COPY src ./src

# 使用 Maven 构建项目
# 通过 .mvn 目录配置 Maven 参数（可配置镜像仓库）
RUN --mount=type=cache,target=/root/.m2/repository \
    ./mvnw clean package -DskipTests -B

# =============================================================================
# Stage 2: Runtime Stage - 生产环境镜像
# =============================================================================
FROM eclipse-temurin:21-jre

# 维护者信息和版本标签
LABEL maintainer="AgentScope Team"
LABEL description="AgentScope Java Agent - Production Image"
LABEL version="1.0.0"

# 创建非 root 用户运行应用（安全最佳实践）
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# 设置工作目录
WORKDIR /app

# 复制构建产物
COPY --from=builder /build/target/*.jar app.jar

# 复制配置文件（生产环境建议通过 volume 挂载）
# COPY config/application.yml /app/config/

# 修改文件所有权
RUN chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

# 暴露应用端口（根据实际情况修改）
EXPOSE 8080

# 定义环境变量（支持外部覆盖）
ENV SERVER_PORT=8080 \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC" \
    JAVA_TOOL_OPTIONS="-javaagent:/app/jmx_exporter.jar=9404:/app/config/jmx.yml" \
    SPRING_PROFILES_ACTIVE=production

# 健康检查：检查 Spring Boot Actuator 健康端点
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

# 启动命令：使用 exec 格式确保信号正确传递
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

### 1.2 前端集成构建

如果你的应用包含前端模块（如 React/Vue），可以使用多阶段构建同时处理前端和后端：

```dockerfile
# =============================================================================
# Stage 1: Frontend Build - 构建前端资源
# =============================================================================
FROM node:20-alpine AS frontend-builder

WORKDIR /frontend

# 复制前端依赖配置
COPY frontend/package*.json ./

# 配置 npm 镜像（加速国内构建）
RUN npm config set registry https://registry.npmmirror.com && \
    npm install --legacy-peer-deps

# 复制前端源码
COPY frontend/ .

# 构建前端（输出到 dist/）
RUN npm run build-only

# =============================================================================
# Stage 2: Backend Build - 构建 Java 后端
# =============================================================================
FROM eclipse-temurin:21-jdk AS backend-builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN ./mvnw clean package -DskipTests -B

# =============================================================================
# Stage 3: Runtime Stage - 最终镜像
# =============================================================================
FROM eclipse-temurin:21-jre

LABEL maintainer="AgentScope Team"
LABEL description="AgentScope with Frontend - Production Image"
LABEL version="1.0.0"

RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# 复制后端 JAR
COPY --from=backend-builder /build/target/*.jar app.jar

# 复制前端静态文件
RUN mkdir -p /app/static
COPY --from=frontend-builder /frontend/dist /app/static

# 修改文件所有权
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENV SERVER_PORT=8080 \
    JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC" \
    SPRING_WEB_RESOURCES_STATIC_LOCATIONS="file:/app/static/"

HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

### 1.3 .dockerignore 配置

排除不必要的文件，减小构建上下文体积：

```
# Git
.git
.gitignore

# Build artifacts
target/
build/
*.class

# IDE
.idea/
.vscode/
*.iml

# Documentation
*.md
docs/

# Docker files (避免递归复制)
Dockerfile
docker-compose*.yml
.dockerignore

# Logs
*.log
logs/

# Temporary files
*.tmp
*.temp
```

### 1.4 镜像构建脚本

创建一个自动化构建脚本，支持多平台构建和推送：

```bash
#!/bin/bash
# =============================================================================
# AgentScope Docker Build Script
# 构建脚本：支持多平台、多模块构建
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 默认配置
VERSION="${VERSION:-latest}"
REGISTRY="${REGISTRY:-registry.cn-hangzhou.aliyuncs.com/agentscope}"
PLATFORM="${PLATFORM:-linux/amd64}"
PUSH_IMAGE=false
SKIP_DOCKER=false

# 模块列表
MODULES=("supervisor-agent" "business-sub-agent" "consult-sub-agent" "business-mcp-server")

# 打印函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助
show_help() {
    cat << EOF
用法: $0 [选项]

选项:
    -v, --version <版本>      Docker 镜像版本标签 (默认: latest)
    -r, --registry <仓库>    Docker 镜像仓库地址 (默认: registry.cn-hangzhou.aliyuncs.com/agentscope)
    -p, --platform <平台>     目标平台，多个用逗号分隔 (默认: linux/amd64)
    -m, --modules <模块>      指定要构建的模块，逗号分隔 (默认: 全部)
    --push                    构建后推送镜像到仓库
    --skip-docker             跳过 Docker 镜像构建
    -h, --help                显示帮助信息

示例:
    $0 -v 1.0.0 --push
    $0 -m supervisor-agent,business-sub-agent -v 1.0.0
EOF
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--version)
            VERSION="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -p|--platform)
            PLATFORM="$2"
            shift 2
            ;;
        -m|--modules)
            IFS=',' read -ra MODULES <<< "$2"
            shift 2
            ;;
        --push)
            PUSH_IMAGE=true
            shift
            ;;
        --skip-docker)
            SKIP_DOCKER=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "未知参数: $1"
            show_help
            exit 1
            ;;
    esac
done

log_info "========== 构建配置 =========="
log_info "版本: ${VERSION}"
log_info "仓库: ${REGISTRY}"
log_info "平台: ${PLATFORM}"
log_info "模块: ${MODULES[*]}"
log_info "推送: ${PUSH_IMAGE}"
log_info "=============================="

# 构建函数
build_module() {
    local module=$1

    log_info "开始构建模块: ${module}"

    # 进入模块目录
    cd ${module} || { log_error "目录不存在: ${module}"; return 1; }

    # 如果没有构建产物，先构建
    if [ ! -f "target/${module}.jar" ] && [ ! -f "target/*.jar" ]; then
        log_info "构建 Maven 项目..."
        ./mvnw clean package -DskipTests -B || { log_error "Maven 构建失败"; cd ..; return 1; }
    fi

    # 构建 Docker 镜像
    if [ "${SKIP_DOCKER}" = false ]; then
        log_info "构建 Docker 镜像..."
        docker build \
            --platform ${PLATFORM} \
            --tag ${REGISTRY}/${module}:${VERSION} \
            --tag ${REGISTRY}/${module}:latest \
            .

        # 推送镜像
        if [ "${PUSH_IMAGE}" = true ]; then
            log_info "推送镜像到仓库..."
            docker push ${REGISTRY}/${module}:${VERSION}
            docker push ${REGISTRY}/${module}:latest
        fi
    fi

    # 返回上级目录
    cd ..

    log_info "模块 ${module} 构建完成"
}

# 批量构建模块
for module in "${MODULES[@]}"; do
    build_module ${module}
done

log_info "========== 全部构建完成 =========="
```

---

## 17.2 Kubernetes 部署

Kubernetes 是生产环境容器编排的首选平台。本节介绍如何在 K8s 中部署 AgentScope Java 应用。

### 2.1 基本 Deployment 配置

以下是生产级别的 Kubernetes Deployment 配置文件：

```yaml
# Copyright 2024-2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

---
# =============================================================================
# Supervisor Agent Deployment
# =============================================================================
apiVersion: apps/v1
kind: Deployment
metadata:
  name: supervisor-agent
  namespace: agentscope
  labels:
    app: supervisor-agent
    version: v1
    component: agent
spec:
  # 生产环境建议设置副本数 > 1
  replicas: 2

  # 保留历史版本用于回滚
  revisionHistoryLimit: 10

  # 更新超时时间
  progressDeadlineSeconds: 600

  selector:
    matchLabels:
      app: supervisor-agent

  # 滚动更新策略
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%          # 最多超出 25% 的 Pod
      maxUnavailable: 25%    # 最多不可用 25% 的 Pod

  template:
    metadata:
      labels:
        app: supervisor-agent
        version: v1
        component: agent

    spec:
      # 初始化容器：等待 Nacos 就绪
      initContainers:
        - name: wait-for-nacos
          image: busybox:1.36
          imagePullPolicy: IfNotPresent
          command:
            - sh
            - -c
            - |
              echo "等待 Nacos 服务就绪..."
              NACOS_HOST=$(echo $NACOS_SERVER_ADDR | cut -d: -f1)
              NACOS_PORT=$(echo $NACOS_SERVER_ADDR | cut -d: -f2)
              until nc -z $NACOS_HOST $NACOS_PORT; do
                echo "Nacos 未就绪，等待 2 秒..."
                sleep 2
              done
              echo "Nacos 已就绪"

      # 应用容器
      containers:
        - name: supervisor-agent
          # 镜像地址（根据实际情况修改）
          image: registry.cn-hangzhou.aliyuncs.com/agentscope/supervisor-agent:1.0.0
          imagePullPolicy: Always

          ports:
            - name: http
              containerPort: 10008
              protocol: TCP

          # 环境变量配置
          env:
            - name: SERVER_PORT
              value: "10008"
            - name: JAVA_OPTS
              value: "-Xms1024m -Xmx2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
            - name: MODEL_PROVIDER
              value: "dashscope"
            - name: MODEL_API_KEY
              valueFrom:
                secretKeyRef:
                  name: agentscope-secrets
                  key: model-api-key
            - name: MODEL_NAME
              value: "qwen-max"
            - name: DB_HOST
              value: "mysql"
            - name: DB_PORT
              value: "3306"
            - name: DB_NAME
              value: "multi_agent_demo"
            - name: DB_USERNAME
              value: "multi_agent_demo"
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: agentscope-secrets
                  key: db-password
            - name: NACOS_SERVER_ADDR
              value: "nacos-server:8848"
            - name: NACOS_NAMESPACE
              value: "public"
            - name: NACOS_REGISTER_ENABLED
              value: "true"
            # OpenTelemetry 配置
            - name: OTEL_EXPORTER_OTLP_ENDPOINT
              value: "http://otel-collector:4317"
            - name: OTEL_SERVICE_NAME
              value: "supervisor-agent"

          # 资源配置（生产环境必须设置）
          resources:
            requests:
              cpu: "1"
              memory: 2Gi
            limits:
              cpu: "2"
              memory: 4Gi

          # 健康检查
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: http
            initialDelaySeconds: 60
            periodSeconds: 30
            timeoutSeconds: 10
            failureThreshold: 3

          readinessProbe:
            httpGet:
              path: /actuator/health
              port: http
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3

          # 生命周期钩子：优雅关闭
          lifecycle:
            preStop:
              exec:
                command:
                  - sh
                  - -c
                  - sleep 10

          # 终止消息
          terminationMessagePath: /dev/termination-log
          terminationMessagePolicy: File

      # DNS 策略
      dnsPolicy: ClusterFirst

      # 重启策略
      restartPolicy: Always

      # 调度器
      schedulerName: default-scheduler

      # 优雅关闭超时时间
      terminationGracePeriodSeconds: 60

      # 服务账号（用于 RBAC）
      serviceAccountName: agentscope-sa

---
# =============================================================================
# Supervisor Agent Service
# =============================================================================
apiVersion: v1
kind: Service
metadata:
  name: supervisor-agent
  namespace: agentscope
  labels:
    app: supervisor-agent
spec:
  type: ClusterIP

  selector:
    app: supervisor-agent

  ports:
    - name: http
      port: 80
      targetPort: 10008
      protocol: TCP

  # 保持会话亲和性（可选）
  sessionAffinity: None

---
# =============================================================================
# ServiceAccount 配置
# =============================================================================
apiVersion: v1
kind: ServiceAccount
metadata:
  name: agentscope-sa
  namespace: agentscope

---
# =============================================================================
# RBAC Role 配置
# =============================================================================
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: agentscope-role
  namespace: agentscope
rules:
  # 如果需要访问 K8s API（如 Harness Kubernetes 模式）
  - apiGroups: [""]
    resources: ["pods", "services", "configmaps"]
    verbs: ["get", "list", "watch"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: agentscope-role-binding
  namespace: agentscope
subjects:
  - kind: ServiceAccount
    name: agentscope-sa
    namespace: agentscope
roleRef:
  kind: Role
  name: agentscope-role
  apiGroup: rbac.authorization.k8s.io
```

### 2.2 HPA 自动扩缩容配置

根据负载自动调整 Pod 副本数：

```yaml
# =============================================================================
# Horizontal Pod Autoscaler
# 基于 CPU 和内存自动扩缩容
# =============================================================================
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: supervisor-agent-hpa
  namespace: agentscope
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: supervisor-agent

  # 副本数范围
  minReplicas: 2
  maxReplicas: 10

  # 扩缩容指标
  metrics:
    # CPU 使用率
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70

    # 内存使用率
    - type: Resource
      resource:
        name: memory
        target:
          type: Utilization
          averageUtilization: 80

  # 扩缩容行为策略
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 冷却 5 分钟
      policies:
        - type: Percent
          value: 10
          periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0    # 立即扩容
      policies:
        - type: Percent
          value: 100
          periodSeconds: 15
        - type: Pods
          value: 4
          periodSeconds: 15
```

### 2.3 ConfigMap 配置

集中管理配置：

```yaml
# =============================================================================
# ConfigMap - 应用配置
# =============================================================================
apiVersion: v1
kind: ConfigMap
metadata:
  name: supervisor-agent-config
  namespace: agentscope
data:
  application.yml: |
    server:
      port: 10008
      shutdown: graceful

    spring:
      application:
        name: supervisor-agent
      lifecycle:
        timeout-per-shutdown-phase: 30s

    agentscope:
      model:
        provider: ${MODEL_PROVIDER:dashscope}

    management:
      endpoints:
        web:
          exposure:
            include: health,info,metrics,prometheus
      metrics:
        tags:
          application: ${spring.application.name}

  logback.xml: |
    <?xml version="1.0" encoding="UTF-8"?>
    <configuration>
        <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

        <springProperty scope="context" name="appName" source="spring.application.name"/>

        <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>

        <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>/app/logs/${appName}.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>/app/logs/${appName}.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
            </encoder>
        </appender>

        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
    </configuration>

---
# =============================================================================
# Secret - 敏感信息配置
# =============================================================================
apiVersion: v1
kind: Secret
metadata:
  name: agentscope-secrets
  namespace: agentscope
type: Opaque
stringData:
  model-api-key: "your-model-api-key-here"
  db-password: "your-db-password-here"
  nacos-password: "your-nacos-password-here"
```

### 2.4 Helm Chart 打包

使用 Helm 管理 Kubernetes 部署：

```yaml
# Chart.yaml
# =============================================================================
apiVersion: v2
name: agentscope
description: AgentScope Java Multi-Agent System Helm Chart
type: application
version: 1.0.0
appVersion: "1.0.0"
keywords:
  - agentscope
  - multi-agent
  - ai
maintainers:
  - name: AgentScope Team
    email: agentscope@example.com
```

```yaml
# values.yaml
# =============================================================================
global:
  namespace: agentscope

image:
  registry: registry.cn-hangzhou.aliyuncs.com/agentscope
  pullPolicy: Always
  tag: "1.0.0"

# MySQL 配置
mysql:
  deployEnabled: true
  host: mysql
  dbname: multi_agent_demo
  username: multi_agent_demo
  password: multi_agent_demo@321

# Nacos 配置
nacos:
  deployEnabled: true
  serverAddr: nacos-server:8848
  namespace: public
  registerEnabled: true

# 模型配置
model:
  provider: dashscope
  apiKey: {API_KEY}
  modelName: qwen-max

# 服务副本数
replicas: 2

# 资源配置
resources:
  requests:
    cpu: "1"
    memory: 2Gi
  limits:
    cpu: "2"
    memory: 4Gi

# HPA 配置
hpa:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  cpuTargetUtilization: 70
  memoryTargetUtilization: 80

# 服务启用开关
services:
  supervisorAgent:
    enabled: true
  businessSubAgent:
    enabled: true
  consultSubAgent:
    enabled: true
  businessMcpServer:
    enabled: true
```

---

## 17.3 监控与追踪（OpenTelemetry）

生产环境必须具备完善的监控和追踪能力。本节介绍如何使用 OpenTelemetry 实现可观测性。

### 3.1 OpenTelemetry 简介

OpenTelemetry 是云原生计算基金会（CNCF）的可观测性框架，提供统一的指标、日志、追踪采集标准。

```
┌─────────────────────────────────────────────────────────────────┐
│                     OpenTelemetry 架构                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────┐   ┌─────────┐   ┌─────────┐                      │
│  │  App 1  │   │  App 2  │   │  App N  │   应用层（OTel SDK）  │
│  └────┬────┘   └────┬────┘   └────┬────┘                      │
│       │            │            │                             │
│  ┌────▼────────────▼────────────▼────┐                       │
│  │         OTLP Exporter            │   数据导出              │
│  └──────────────┬───────────────────┘                        │
│                 │                                            │
│  ┌──────────────▼───────────────────┐                       │
│  │       OTel Collector             │   收集器               │
│  │  - 接收 OTLP                      │                       │
│  │  - 批处理                         │                       │
│  │  - 过滤/转换                      │                       │
│  └──────┬────────┬────────┬─────────┘                       │
│         │        │        │                                 │
│    ┌────▼┐  ┌────▼┐  ┌────▼┐                                │
│    │Trace│  │Metric│ │ Log │    后端存储                   │
│    └────┘  └────┘  └────┘                                  │
│    Tempo   Prometheus  Loki                                │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Spring Boot 集成配置

在 `application.yml` 中配置 OpenTelemetry：

```yaml
# =============================================================================
# OpenTelemetry + Micrometer 配置
# =============================================================================

spring:
  application:
    name: supervisor-agent

  # 优雅关闭配置
  lifecycle:
    timeout-per-shutdown-phase: 30s

# 服务端口
server:
  port: 10008
  shutdown: graceful

# Actuator + Micrometer 配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env
      base-path: /actuator

  endpoint:
    health:
      show-details: always
      probes:
        enabled: true

    metrics:
      enabled: true

  metrics:
    tags:
      # 添加应用标识，便于在 Prometheus 中过滤
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true

  # 健康检查配置
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

# OpenTelemetry 配置（通过环境变量或配置注入）
# OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4317
# OTEL_EXPORTER_OTLP_PROTOCOL=grpc
# OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=http://otel-collector:4317
# OTEL_EXPORTER_OTLP_METRICS_ENDPOINT=http://otel-collector:4318
# OTEL_SERVICE_NAME=${spring.application.name}
# OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production

# JMX Exporter 配置（用于 Prometheus 抓取 JVM 指标）
# 启用方式：javaagent:jmx_exporter.jar=9404:config/jmx.yml

# 日志配置
logging:
  level:
    root: INFO
    io.agentscope: DEBUG
    io.opentelemetry: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### 3.3 JMX Exporter 配置

JMX Exporter 用于暴露 JVM 指标给 Prometheus：

```yaml
# jmx.yml
# JMX Exporter 配置文件
---
jmxUrl: service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi
host: 0.0.0.0
port: 9404
ssl: false
lowercaseOutputName: true
lowercaseOutputLabelNames: true

# JVM 指标
whitelistObjectNames:
  - "java.lang:type=Memory:*"
  - "java.lang:type=GarbageCollector,*"
  - "java.lang:type=Runtime:*"
  - "java.lang:type=Threading:*"
  - "java.lang:type=ClassLoading:*"

# Tomcat 指标（如果使用嵌入式 Tomcat）
# - "Catalina:type=GlobalRequestProcessor,*"
# - "Catalina:type=ThreadPool,*"

# 自定义指标规则
rules:
  # 内存使用情况
  - pattern: 'java.lang<type=Memory><HeapMemoryUsage>(\w+)'
    name: jvm_memory_heap_$1
    type: GAUGE
    help: "JVM Heap Memory Usage"

  - pattern: 'java.lang<type=Memory><NonHeapMemoryUsage>(\w+)'
    name: jvm_memory_nonheap_$1
    type: GAUGE
    help: "JVM Non-Heap Memory Usage"

  # GC 指标
  - pattern: 'java.lang<type=GarbageCollector, name=(\w+)><>(\w+)'
    name: jvm_gc_$1_$2
    type: GAUGE
    help: "JVM Garbage Collector Metrics"

  # 线程指标
  - pattern: 'java.lang<type=Threading><>(\w+)'
    name: jvm_threads_$1
    type: GAUGE
    help: "JVM Thread Metrics"

  # 类加载指标
  - pattern: 'java.lang<type=ClassLoading><>(\w+)'
    name: jvm_classloading_$1
    type: GAUGE
    help: "JVM Class Loading Metrics"

  # JVM 运行时指标
  - pattern: 'java.lang<type=Runtime><>(\w+)'
    name: jvm_runtime_$1
    type: GAUGE
    help: "JVM Runtime Metrics"

  # 默认规则：转换所有 JMX MBean
  - pattern: '.*'
    name: jmx_$1
    type: GAUGE
```

### 3.4 OTel Collector 配置

部署 OTel Collector 作为数据收集中间件：

```yaml
# otel-collector-config.yaml
# =============================================================================
receivers:
  # OTLP 接收器（gRPC）
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

  # Prometheus 接收器（抓取 Pod 指标）
  prometheus:
    config:
      scrape_configs:
        - job_name: 'agentscope-agents'
          kubernetes_sd_configs:
            - role: pod
          relabel_configs:
            - source_labels: [__meta_kubernetes_pod_label_app]
              regex: '.*-agent'
              action: keep
            - source_labels: [__meta_kubernetes_namespace]
              target_label: namespace
            - source_labels: [__meta_kubernetes_pod_name]
              target_label: pod

processors:
  # 批处理
  batch:
    timeout: 10s
    send_batch_size: 1024

  # 内存限制
  memory_limiter:
    check_interval: 1s
    limit_mib: 512
    spike_limit_mib: 128

  # 属性过滤
  filter:
    metrics:
      exclude:
        match_type: strict
        metric_names:
          - 'otel.exporter'

  # 资源属性添加
  resource:
    attributes:
      - action: upsert
        key: deployment.environment
        value: production

exporters:
  # Jaeger 追踪导出
  jaeger:
    endpoint: jaeger:14250
    tls:
      insecure: true

  # Prometheus 指标导出（供下游 Prometheus 抓取）
  prometheus:
    endpoint: "0.0.0.0:8889"
    namespace: agentscope
    const_labels:
      environment: production

  # Loki 日志导出
  loki:
    endpoint: http://loki:3100/loki/api/v1/push

  # DEBUG exporter（开发调试用）
  logging:
    verbosity: basic

service:
  pipelines:
    # Traces pipeline
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch, resource]
      exporters: [jaeger, logging]

    # Metrics pipeline
    metrics:
      receivers: [otlp, prometheus]
      processors: [memory_limiter, filter, batch, resource]
      exporters: [prometheus, logging]

    # Logs pipeline
    logs:
      receivers: [otlp]
      processors: [memory_limiter, batch, resource]
      exporters: [loki, logging]
```

### 3.5 Prometheus 配置

Prometheus 配置用于抓取应用指标：

```yaml
# prometheus-config.yaml
# =============================================================================
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # OTel Collector 指标
  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8889']
        labels:
          component: otel-collector

  # AgentScope 应用（JMX Exporter）
  - job_name: 'agentscope-agents'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      # 过滤 agent 应用
      - source_labels: [__meta_kubernetes_pod_label_app]
        regex: '.*-agent'
        action: keep
      # 过滤 metrics 端口
      - source_labels: [__meta_kubernetes_pod_container_port_number]
        regex: '9404'
        action: keep
      # 重命名标签
      - source_labels: [__meta_kubernetes_namespace]
        target_label: namespace
      - source_labels: [__meta_kubernetes_pod_name]
        target_label: pod
      - source_labels: [__meta_kubernetes_pod_label_app]
        target_label: app
    metric_relabel_configs:
      # 添加环境标签
      - target_label: environment
        replacement: production

  # Spring Boot Actuator (Prometheus 端点)
  - job_name: 'spring-boot-actuator'
    kubernetes_sd_configs:
      - role: service
    relabel_configs:
      - source_labels: [__meta_kubernetes_service_label_app]
        regex: '.*-agent'
        action: keep
      - source_labels: [__meta_kubernetes_service_name]
        target_label: service
      - source_labels: [__meta_kubernetes_namespace]
        target_label: namespace
```

### 3.6 Grafana Dashboard 配置

Grafana Dashboard 用于可视化展示监控数据：

```json
{
  "annotations": {
    "list": []
  },
  "editable": true,
  "fiscalYearStartMonth": 0,
  "graphTooltip": 1,
  "id": null,
  "links": [],
  "liveNow": false,
  "panels": [
    {
      "datasource": {
        "type": "prometheus",
        "uid": "prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "percent"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 0
      },
      "id": 1,
      "options": {
        "legend": {
          "calcs": ["mean", "lastNotNull"],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "jvm_memory_used_bytes{application=\"$application\", area=\"heap\"} / jvm_memory_max_bytes{application=\"$application\", area=\"heap\"}",
          "legendFormat": "Heap Usage",
          "refId": "A"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "system_cpu_usage{application=\"$application\"}",
          "legendFormat": "System CPU",
          "refId": "B"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "process_cpu_usage{application=\"$application\"}",
          "legendFormat": "Process CPU",
          "refId": "C"
        }
      ],
      "title": "JVM 和系统资源使用率",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              },
              {
                "color": "yellow",
                "value": 70
              },
              {
                "color": "red",
                "value": 90
              }
            ]
          },
          "unit": "percent"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 4,
        "w": 6,
        "x": 12,
        "y": 0
      },
      "id": 2,
      "options": {
        "colorMode": "value",
        "graphMode": "area",
        "justifyMode": "auto",
        "orientation": "auto",
        "reduceOptions": {
          "calcs": ["lastNotNull"],
          "fields": "",
          "values": false
        },
        "textMode": "auto"
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "jvm_memory_used_bytes{application=\"$application\", area=\"heap\"} / jvm_memory_max_bytes{application=\"$application\", area=\"heap\"} * 100",
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "堆内存使用率",
      "type": "stat"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "thresholds"
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "short"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 4,
        "w": 6,
        "x": 18,
        "y": 0
      },
      "id": 3,
      "options": {
        "colorMode": "value",
        "graphMode": "none",
        "justifyMode": "auto",
        "orientation": "auto",
        "reduceOptions": {
          "calcs": ["lastNotNull"],
          "fields": "",
          "values": false
        },
        "textMode": "auto"
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "jvm_threads_live_threads{application=\"$application\"}",
          "legendFormat": "",
          "refId": "A"
        }
      ],
      "title": "活跃线程数",
      "type": "stat"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "reqps"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 4
      },
      "id": 4,
      "options": {
        "legend": {
          "calcs": ["mean", "max"],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "rate(http_server_requests_seconds_count{application=\"$application\", status=~\"2..\"}[1m])",
          "legendFormat": "2xx",
          "refId": "A"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "rate(http_server_requests_seconds_count{application=\"$application\", status=~\"4..\"}[1m])",
          "legendFormat": "4xx",
          "refId": "B"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "rate(http_server_requests_seconds_count{application=\"$application\", status=~\"5..\"}[1m])",
          "legendFormat": "5xx",
          "refId": "C"
        }
      ],
      "title": "HTTP 请求速率",
      "type": "timeseries"
    },
    {
      "datasource": {
        "type": "prometheus",
        "uid": "prometheus"
      },
      "fieldConfig": {
        "defaults": {
          "color": {
            "mode": "palette-classic"
          },
          "custom": {
            "axisCenteredZero": false,
            "axisColorMode": "text",
            "axisLabel": "",
            "axisPlacement": "auto",
            "barAlignment": 0,
            "drawStyle": "line",
            "fillOpacity": 10,
            "gradientMode": "none",
            "hideFrom": {
              "legend": false,
              "tooltip": false,
              "viz": false
            },
            "lineInterpolation": "linear",
            "lineWidth": 1,
            "pointSize": 5,
            "scaleDistribution": {
              "type": "linear"
            },
            "showPoints": "never",
            "spanNulls": false,
            "stacking": {
              "group": "A",
              "mode": "none"
            },
            "thresholdsStyle": {
              "mode": "off"
            }
          },
          "mappings": [],
          "thresholds": {
            "mode": "absolute",
            "steps": [
              {
                "color": "green",
                "value": null
              }
            ]
          },
          "unit": "ms"
        },
        "overrides": []
      },
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 8
      },
      "id": 5,
      "options": {
        "legend": {
          "calcs": ["mean", "p95"],
          "displayMode": "table",
          "placement": "bottom",
          "showLegend": true
        },
        "tooltip": {
          "mode": "single",
          "sort": "none"
        }
      },
      "targets": [
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "histogram_quantile(0.5, rate(http_server_requests_seconds_bucket{application=\"$application\"}[5m])) * 1000",
          "legendFormat": "p50",
          "refId": "A"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application=\"$application\"}[5m])) * 1000",
          "legendFormat": "p95",
          "refId": "B"
        },
        {
          "datasource": {
            "type": "prometheus",
            "uid": "prometheus"
          },
          "expr": "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{application=\"$application\"}[5m])) * 1000",
          "legendFormat": "p99",
          "refId": "C"
        }
      ],
      "title": "HTTP 响应延迟",
      "type": "timeseries"
    }
  ],
  "refresh": "10s",
  "schemaVersion": 38,
  "style": "dark",
  "tags": ["agentscope", "jvm", "spring-boot"],
  "templating": {
    "list": [
      {
        "current": {
          "selected": false,
          "text": "supervisor-agent",
          "value": "supervisor-agent"
        },
        "datasource": {
          "type": "prometheus",
          "uid": "prometheus"
        },
        "definition": "label_values(jvm_memory_used_bytes, application)",
        "hide": 0,
        "includeAll": false,
        "label": "应用",
        "multi": false,
        "name": "application",
        "options": [],
        "query": {
          "query": "label_values(jvm_memory_used_bytes, application)",
          "refId": "StandardVariableQuery"
        },
        "refresh": 1,
        "regex": "",
        "skipUrlSync": false,
        "sort": 0,
        "type": "query"
      }
    ]
  },
  "time": {
    "from": "now-1h",
    "to": "now"
  },
  "timepicker": {},
  "timezone": "browser",
  "title": "AgentScope JVM 监控面板",
  "uid": "agentscope-jvm",
  "version": 1,
  "weekStart": ""
}
```

---

## 17.4 优雅关闭

生产环境中，服务的关闭必须优雅，以确保正在处理的请求能够正常完成，避免数据丢失和服务中断。

### 4.1 优雅关闭原理

```
┌────────────────────────────────────────────────────────────────────┐
│                     优雅关闭流程                                    │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  1. 收到 SIGTERM 信号                                              │
│         │                                                         │
│         ▼                                                         │
│  2. 停止接收新请求（Liveness Probe 失败）                          │
│         │                                                         │
│         ▼                                                         │
│  3. 等待正在处理的请求完成                                          │
│     ┌────────────────────────────────────────────────────┐        │
│     │  Spring Lifecycle: timeout-per-shutdown-phase      │        │
│     │  AgentScope: GracefulShutdownManager               │        │
│     └────────────────────────────────────────────────────┘        │
│         │                                                         │
│         ▼                                                         │
│  4. 关闭数据库连接池                                               │
│         │                                                         │
│         ▼                                                         │
│  5. 关闭线程池                                                    │
│         │                                                         │
│         ▼                                                         │
│  6. 进程退出                                                      │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 4.2 Spring Boot 优雅关闭配置

```yaml
# application.yml
# =============================================================================
server:
  port: 8080
  # 启用优雅关闭
  shutdown: graceful

# 关闭阶段超时时间（生产环境建议设置较长时间）
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

# Actuator 健康检查配置
management:
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true

  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true

# AgentScope 优雅关闭配置
agent:
  # Agent 执行超时时间（-1 表示不超时）
  shutdown-timeout-seconds: -1

  # 部分推理结果保存策略
  # 可选值：
  #   - save: 保存部分推理结果
  #   - discard: 丢弃部分推理结果
  shutdown-partial-reasoning-policy: save
```

### 4.3 GracefulShutdownManager 使用

AgentScope 提供了 `GracefulShutdownManager` 用于管理 Agent 的优雅关闭：

```java
/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter17.shutdown;

import io.agentscope.core.shutdown.GracefulShutdownManager;
import io.agentscope.core.shutdown.ShutdownState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 优雅关闭控制器
 *
 * <p>提供以下功能：
 * <ul>
 *   <li>获取当前关闭状态</li>
 *   <li>获取活跃请求数</li>
 *   <li>手动触发优雅关闭</li>
 *   <li>就绪检查（用于 Kubernetes readinessProbe）</li>
 * </ul>
 *
 * @author AgentScope Team
 */
@RestController
@RequestMapping("/api/shutdown")
public class ShutdownController {

    /**
     * 单例获取关闭管理器
     */
    private final GracefulShutdownManager shutdownManager = GracefulShutdownManager.getInstance();

    /**
     * 获取当前关闭状态
     *
     * @return 包含状态和活跃请求数的响应
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("state", shutdownManager.getState().name());
        result.put("activeRequests", shutdownManager.getActiveRequestCount());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 就绪检查端点
     *
     * <p>当服务处于 RUNNING 状态时返回 200 OK，
     * 其他状态返回 503 Service Unavailable
     *
     * @return 就绪状态信息
     */
    @GetMapping("/readiness")
    public Map<String, Object> readiness() {
        Map<String, Object> result = new LinkedHashMap<>();
        ShutdownState state = shutdownManager.getState();
        result.put("state", state.name());
        result.put("activeRequests", shutdownManager.getActiveRequestCount());

        // 非 RUNNING 状态返回 503
        if (state != ShutdownState.RUNNING) {
            result.put("ready", false);
            // 注意：Spring 会自动返回 503 状态码
        } else {
            result.put("ready", true);
        }

        return result;
    }

    /**
     * 手动触发优雅关闭
     *
     * <p>通常由 Kubernetes preStop hook 或运维平台触发
     *
     * @return 关闭操作结果
     */
    @PostMapping("/trigger")
    public Map<String, Object> triggerShutdown() {
        Map<String, Object> result = new LinkedHashMap<>();

        ShutdownState currentState = shutdownManager.getState();
        result.put("previousState", currentState.name());
        result.put("activeRequests", shutdownManager.getActiveRequestCount());

        if (currentState == ShutdownState.RUNNING) {
            shutdownManager.performGracefulShutdown();
            result.put("message", "优雅关闭已触发");
            result.put("newState", shutdownManager.getState().name());
        } else {
            result.put("message", "服务已处于关闭流程中");
        }

        return result;
    }
}
```

### 4.4 自定义关闭钩子

如果需要执行自定义的清理逻辑，可以注册 Spring 关闭钩子：

```java
/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter17.shutdown;

import io.agentscope.core.workflow.agent.AgentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * 应用关闭事件监听器
 *
 * <p>在应用关闭时执行必要的清理工作，
 * 如注销 Agent、刷新缓冲、关闭连接等
 */
@Component
public class ApplicationShutdownListener implements ApplicationListener<ContextClosedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownListener.class);

    private final AgentRegistry agentRegistry;

    public ApplicationShutdownListener(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        logger.info("收到关闭事件，开始清理资源...");

        try {
            // 1. 停止接收新请求
            logger.info("步骤 1: 停止 Agent 接收新请求");
            agentRegistry.pauseAll();

            // 2. 等待正在处理的请求完成
            logger.info("步骤 2: 等待活跃请求完成...");
            if (!agentRegistry.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warn("活跃请求未在 30 秒内完成，继续关闭...");
            }

            // 3. 注销所有 Agent
            logger.info("步骤 3: 注销所有 Agent");
            agentRegistry.shutdown();

            // 4. 刷新缓冲数据
            logger.info("步骤 4: 刷新缓冲数据");
            // flushBuffers();

            logger.info("资源清理完成");
        } catch (Exception e) {
            logger.error("关闭过程中发生错误", e);
        }
    }
}
```

### 4.5 Kubernetes 关闭优化

在 Kubernetes 环境中，还需要配置合理的终止宽限期和 preStop hook：

```yaml
# Kubernetes Deployment 中的 lifecycle 配置
spec:
  template:
    spec:
      containers:
        - name: supervisor-agent
          lifecycle:
            # 容器停止前执行的命令
            preStop:
              exec:
                command:
                  - sh
                  - -c
                  - |
                    echo "开始优雅关闭..."
                    # 等待信号传递到应用
                    sleep 5

      # 终止宽限期（与 shutdown-timeout-per-shutdown-phase 配合）
      terminationGracePeriodSeconds: 60
```

---

## 17.5 【案例】生产环境部署

本案例展示一个完整的生产环境部署配置，包含 Docker Compose 本地开发和 Kubernetes 生产部署两个版本。

### 5.1 项目结构

```
agentscope-production/
├── docker/
│   ├── Dockerfile
│   ├── .dockerignore
│   └── build.sh
├── kubernetes/
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── configmap.yaml
│   │   └── secret.yaml
│   ├── overlays/
│   │   ├── dev/
│   │   └── prod/
│   └── kustomization.yaml
├── monitoring/
│   ├── otel-collector.yaml
│   ├── prometheus.yaml
│   └── grafana-dashboard.yaml
├── docker-compose.yml
└── README.md
```

### 5.2 Docker Compose 配置

```yaml
# docker-compose.yml
# =============================================================================
# AgentScope 生产环境 Docker Compose 配置
# 适用于本地开发和测试环境
# 生产环境请使用 Kubernetes 部署
# =============================================================================

services:
  # ============================================================================
  # 基础设施服务
  # ============================================================================

  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: agentscope-mysql
    restart: unless-stopped
    ports:
      - "${MYSQL_PORT:-3306}:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD:-multi_agent_demo@321}
      MYSQL_DATABASE: ${DB_NAME:-multi_agent_demo}
      MYSQL_USER: ${DB_USERNAME:-multi_agent_demo}
      MYSQL_PASSWORD: ${DB_PASSWORD:-multi_agent_demo@321}
      TZ: Asia/Shanghai
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d
    command:
      - --default-authentication-plugin=mysql_native_password
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --max_connections=500
      - --innodb_buffer_pool_size=1G
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-p${DB_PASSWORD:-multi_agent_demo@321}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    networks:
      - agentscope-network

  # Nacos 服务注册中心
  nacos-server:
    image: nacos/nacos-server:v2.4.3
    container_name: agentscope-nacos
    restart: unless-stopped
    ports:
      - "${NACOS_PORT:-8848}:8848"
      - "${NACOS_GRPC_PORT:-9848}:9848"
    environment:
      PREFER_HOST_MODE: hostname
      MODE: standalone
      NACOS_AUTH_ENABLE: false
      TZ: Asia/Shanghai
      JVM_XMS: 512m
      JVM_XMX: 1024m
    volumes:
      - nacos-data:/home/nacos/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8848/nacos/"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 60s
    networks:
      - agentscope-network

  # Redis 会话存储（可选）
  redis:
    image: redis:7-alpine
    container_name: agentscope-redis
    restart: unless-stopped
    ports:
      - "${REDIS_PORT:-6379}:6379"
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3
    networks:
      - agentscope-network

  # ============================================================================
  # 监控服务
  # ============================================================================

  # Prometheus 监控
  prometheus:
    image: prom/prometheus:v2.52.0
    container_name: agentscope-prometheus
    restart: unless-stopped
    ports:
      - "${PROMETHEUS_PORT:-9090}:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'
      - '--web.console.libraries=/usr/share/prometheus/console_libraries'
      - '--web.console.templates=/usr/share/prometheus/consoles'
    networks:
      - agentscope-network

  # Grafana 可视化
  grafana:
    image: grafana/grafana:10.4.0
    container_name: agentscope-grafana
    restart: unless-stopped
    ports:
      - "${GRAFANA_PORT:-3000}:3000"
    environment:
      GF_SECURITY_ADMIN_USER: admin
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin123}
      GF_USERS_ALLOW_SIGN_UP: false
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - grafana-data:/var/lib/grafana
      - ./monitoring/dashboards:/etc/grafana/provisioning/dashboards
      - ./monitoring/datasources.yml:/etc/grafana/provisioning/datasources/datasources.yml
    networks:
      - agentscope-network

  # OTel Collector
  otel-collector:
    image: otel/opentelemetry-collector-contrib:0.100.0
    container_name: agentscope-otel-collector
    restart: unless-stopped
    ports:
      - "4317:4317"   # OTLP gRPC
      - "4318:4318"   # OTLP HTTP
      - "8889:8889"   # Prometheus metrics
    volumes:
      - ./monitoring/otel-collector-config.yml:/etc/otelcol/config.yml
    command: ["--config=/etc/otelcol/config.yml"]
    networks:
      - agentscope-network

  # ============================================================================
  # 业务服务
  # ============================================================================

  # Supervisor Agent（主管理 Agent）
  supervisor-agent:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    container_name: agentscope-supervisor-agent
    restart: unless-stopped
    ports:
      - "${SUPERVISOR_PORT:-10008}:10008"
    environment:
      SERVER_PORT: 10008
      # 模型配置
      MODEL_PROVIDER: ${MODEL_PROVIDER:-dashscope}
      MODEL_API_KEY: ${MODEL_API_KEY}
      MODEL_NAME: ${MODEL_NAME:-qwen-max}
      MODEL_BASE_URL: ${MODEL_BASE_URL:-}
      # 数据库配置
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: ${DB_NAME:-multi_agent_demo}
      DB_USERNAME: ${DB_USERNAME:-multi_agent_demo}
      DB_PASSWORD: ${DB_PASSWORD:-multi_agent_demo@321}
      # Nacos 配置
      NACOS_SERVER_ADDR: nacos-server:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-public}
      NACOS_REGISTER_ENABLED: "true"
      # Redis 配置（会话共享）
      REDIS_HOST: redis
      REDIS_PORT: 6379
      # OpenTelemetry 配置
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
      OTEL_SERVICE_NAME: supervisor-agent
      # JVM 参数
      JAVA_OPTS: "-Xms1024m -Xmx2048m -XX:+UseG1GC"
      # 关闭配置
      SPRING_PROFILES_ACTIVE: production
    depends_on:
      mysql:
        condition: service_healthy
      nacos-server:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:10008/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - agentscope-network

  # Business Sub Agent
  business-sub-agent:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    container_name: agentscope-business-sub-agent
    restart: unless-stopped
    ports:
      - "${BUSINESS_AGENT_PORT:-10006}:10006"
    environment:
      SERVER_PORT: 10006
      MODEL_PROVIDER: ${MODEL_PROVIDER:-dashscope}
      MODEL_API_KEY: ${MODEL_API_KEY}
      MODEL_NAME: ${MODEL_NAME:-qwen-max}
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: ${DB_NAME:-multi_agent_demo}
      DB_USERNAME: ${DB_USERNAME:-multi_agent_demo}
      DB_PASSWORD: ${DB_PASSWORD:-multi_agent_demo@321}
      NACOS_SERVER_ADDR: nacos-server:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-public}
      NACOS_REGISTER_ENABLED: "true"
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
      OTEL_SERVICE_NAME: business-sub-agent
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
      SPRING_PROFILES_ACTIVE: production
    depends_on:
      mysql:
        condition: service_healthy
      nacos-server:
        condition: service_healthy
    networks:
      - agentscope-network

  # Consult Sub Agent
  consult-sub-agent:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    container_name: agentscope-consult-sub-agent
    restart: unless-stopped
    ports:
      - "${CONSULT_AGENT_PORT:-10005}:10005"
    environment:
      SERVER_PORT: 10005
      MODEL_PROVIDER: ${MODEL_PROVIDER:-dashscope}
      MODEL_API_KEY: ${MODEL_API_KEY}
      MODEL_NAME: ${MODEL_NAME:-qwen-max}
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: ${DB_NAME:-multi_agent_demo}
      DB_USERNAME: ${DB_USERNAME:-multi_agent_demo}
      DB_PASSWORD: ${DB_PASSWORD:-multi_agent_demo@321}
      NACOS_SERVER_ADDR: nacos-server:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-public}
      NACOS_REGISTER_ENABLED: "true"
      OTEL_EXPORTER_OTLP_ENDPOINT: http://otel-collector:4317
      OTEL_SERVICE_NAME: consult-sub-agent
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC"
      SPRING_PROFILES_ACTIVE: production
    depends_on:
      mysql:
        condition: service_healthy
      nacos-server:
        condition: service_healthy
    networks:
      - agentscope-network

# ============================================================================
# 网络配置
# ============================================================================
networks:
  agentscope-network:
    driver: bridge
    name: agentscope-network

# ============================================================================
# 存储卷配置
# ============================================================================
volumes:
  mysql-data:
    name: agentscope-mysql-data
  nacos-data:
    name: agentscope-nacos-data
  redis-data:
    name: agentscope-redis-data
  prometheus-data:
    name: agentscope-prometheus-data
  grafana-data:
    name: agentscope-grafana-data
```

### 5.3 环境变量配置文件

创建 `.env` 文件（不要提交到版本控制）：

```bash
# .env.example - 环境变量配置示例

# ==================== 数据库配置 ====================
DB_NAME=multi_agent_demo
DB_USERNAME=multi_agent_demo
DB_PASSWORD=your_secure_password_here

# ==================== 服务端口 ====================
MYSQL_PORT=3306
NACOS_PORT=8848
NACOS_GRPC_PORT=9848
REDIS_PORT=6379
SUPERVISOR_PORT=10008
BUSINESS_AGENT_PORT=10006
CONSULT_AGENT_PORT=10005
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000

# ==================== Nacos 配置 ====================
NACOS_NAMESPACE=public

# ==================== 模型配置 ====================
# 可选值: dashscope, openai, ollama, gemini
MODEL_PROVIDER=dashscope
MODEL_API_KEY=your_model_api_key
MODEL_NAME=qwen-max

# ==================== 监控配置 ====================
GRAFANA_PASSWORD=your_grafana_password

# ==================== 镜像仓库 ====================
IMAGE_REGISTRY=registry.cn-hangzhou.aliyuncs.com/agentscope
IMAGE_TAG=1.0.0
```

### 5.4 Prometheus 配置文件

```yaml
# monitoring/prometheus.yml
# =============================================================================
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets: []

rule_files: []

scrape_configs:
  # Prometheus 自身指标
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # OTel Collector 指标
  - job_name: 'otel-collector'
    static_configs:
      - targets: ['otel-collector:8889']

  # Spring Boot Actuator 指标
  - job_name: 'spring-boot-actuator'
    metrics_path: /actuator/prometheus
    static_configs:
      # Supervisor Agent
      - targets: ['supervisor-agent:10008']
        labels:
          application: supervisor-agent
          environment: production
      # Business Sub Agent
      - targets: ['business-sub-agent:10006']
        labels:
          application: business-sub-agent
          environment: production
      # Consult Sub Agent
      - targets: ['consult-sub-agent:10005']
        labels:
          application: consult-sub-agent
          environment: production

  # JMX Exporter 指标
  - job_name: 'jmx-exporter'
    static_configs:
      - targets: ['supervisor-agent:9404']
        labels:
          application: supervisor-agent
      - targets: ['business-sub-agent:9404']
        labels:
          application: business-sub-agent
      - targets: ['consult-sub-agent:9404']
        labels:
          application: consult-sub-agent
```

### 5.5 OTel Collector 配置文件

```yaml
# monitoring/otel-collector-config.yml
# =============================================================================
receivers:
  # OTLP 接收器
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

  # Prometheus 接收器
  prometheus:
    config:
      global:
        scrape_interval: 15s
      scrape_configs:
        - job_name: 'spring-boot-apps'
          static_configs:
            - targets: ['supervisor-agent:10008', 'business-sub-agent:10006', 'consult-sub-agent:10005']
              labels:
                service: agentscope

processors:
  # 批处理
  batch:
    timeout: 10s
    send_batch_size: 1024

  # 内存限制
  memory_limiter:
    check_interval: 1s
    limit_mib: 512
    spike_limit_mib: 128

  # 属性过滤
  filter:
    metrics:
      exclude:
        match_type: strict
        metric_names:
          - 'otel.*'
          - 'process.*'

  # 资源属性
  resource:
    attributes:
      - action: upsert
        key: deployment.environment
        value: production
      - action: upsert
        key: service.namespace
        value: agentscope

exporters:
  # Prometheus 导出（供下游 Prometheus 抓取）
  prometheus:
    endpoint: "0.0.0.0:8889"
    namespace: agentscope
    const_labels:
      environment: production

  # 日志导出（开发调试）
  logging:
    verbosity: basic
    sampling_initial: 5
    sampling_thereafter: 200

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [memory_limiter, batch, resource]
      exporters: [logging]

    metrics:
      receivers: [otlp, prometheus]
      processors: [memory_limiter, batch, resource]
      exporters: [prometheus, logging]

    logs:
      receivers: [otlp]
      processors: [memory_limiter, batch, resource]
      exporters: [logging]
```

### 5.6 Grafana 数据源配置

```yaml
# monitoring/datasources.yml
# =============================================================================
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    uid: prometheus

  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    editable: true
    uid: loki
```

### 5.7 启动脚本

```bash
#!/bin/bash
# =============================================================================
# AgentScope 生产环境启动脚本
# =============================================================================

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查环境变量
check_env() {
    log_info "检查环境变量..."

    if [ -z "$MODEL_API_KEY" ]; then
        log_error "请设置 MODEL_API_KEY 环境变量"
        exit 1
    fi

    log_info "环境变量检查通过"
}

# 构建镜像
build_images() {
    log_info "构建 Docker 镜像..."

    if [ -d "../supervisor-agent" ]; then
        log_info "构建 Supervisor Agent 镜像..."
        cd ../supervisor-agent
        ./mvnw clean package -DskipTests -B
        docker build -t agentscope/supervisor-agent:latest -f ../docker/Dockerfile ..
    fi

    cd - > /dev/null
}

# 启动服务
start_services() {
    log_info "启动服务..."
    docker-compose up -d

    log_info "等待服务就绪..."
    sleep 10

    # 检查健康状态
    check_health
}

# 检查健康状态
check_health() {
    log_info "检查服务健康状态..."

    local services=("mysql" "nacos-server" "redis" "supervisor-agent")

    for service in "${services[@]}"; do
        local max_attempts=30
        local attempt=0

        while [ $attempt -lt $max_attempts ]; do
            if docker ps --format '{{.Names}}' | grep -q "${service}"; then
                log_info "${service} 已启动"
                break
            fi
            attempt=$((attempt + 1))
            sleep 2
        done

        if [ $attempt -eq $max_attempts ]; then
            log_error "${service} 启动超时"
        fi
    done
}

# 显示服务状态
show_status() {
    echo ""
    log_info "========== 服务状态 =========="
    docker-compose ps
    echo ""
    log_info "========== 访问地址 =========="
    echo "Supervisor Agent: http://localhost:10008"
    echo "Nacos:           http://localhost:8848/nacos"
    echo "Prometheus:      http://localhost:9090"
    echo "Grafana:         http://localhost:3000"
    echo ""
    log_info "默认账号: admin / admin123"
    echo "==============================="
}

# 主函数
main() {
    log_info "========== AgentScope 生产环境启动 =========="

    check_env

    if [ "$1" == "--build" ]; then
        build_images
    fi

    start_services
    show_status

    log_info "启动完成!"
}

# 执行主函数
main "$@"
```

### 5.8 生产环境检查清单

在部署到生产环境前，请确保完成以下检查：

```markdown
## 生产环境部署检查清单

### 镜像构建
- [ ] 使用生产版本 tag（非 latest）
- [ ] 镜像已推送到私有仓库
- [ ] 镜像签名验证（如启用）

### 配置验证
- [ ] 所有敏感信息使用 Secret 管理
- [ ] 数据库密码已修改为强密码
- [ ] API Keys 已配置（从 Secret 或 Vault 读取）
- [ ] 域名和端口配置正确

### Kubernetes 配置
- [ ] Resources requests/limits 已设置
- [ ] Liveness/Readiness Probe 已配置
- [ ] 滚动更新策略已设置
- [ ] 终止宽限期合理（建议 60s）
- [ ] HPA 配置已验证

### 监控告警
- [ ] Prometheus 配置已验证
- [ ] Grafana Dashboard 已导入
- [ ] 告警规则已配置
- [ ] 通知渠道已设置（邮件/钉钉/Slack）

### 备份恢复
- [ ] 数据库备份策略已配置
- [ ] 配置文件备份已设置
- [ ] 恢复流程已测试

### 安全
- [ ] 网络策略已配置
- [ ] RBAC 权限已设置
- [ ] 容器使用非 root 用户运行
- [ ] TLS/SSL 证书已配置
```

---

## 本章小结

本章详细介绍了 AgentScope Java 应用的生产环境部署方案，涵盖以下核心内容：

| 主题 | 关键要点 |
|------|----------|
| **Docker 容器化** | 多阶段构建、安全配置（非 root 用户）、健康检查、资源限制 |
| **Kubernetes 部署** | Deployment 配置、HPA 自动扩缩容、ConfigMap/Secret 管理、Helm Chart |
| **监控与追踪** | OpenTelemetry 集成、Prometheus 指标抓取、Grafana 可视化、JMX Exporter |
| **优雅关闭** | Spring Boot 关闭配置、GracefulShutdownManager、生命周期钩子、K8s preStop |
| **生产环境案例** | Docker Compose 开发环境、完整 Kubernetes 配置、监控栈集成 |

**最佳实践建议**：

1. **镜像优化**：使用多阶段构建、减小镜像体积、避免使用 latest tag
2. **资源配置**：生产环境必须设置 CPU/内存 limits，避免资源耗尽
3. **健康检查**：配置 Liveness 和 Readiness Probe，确保 K8s 正确管理 Pod 生命周期
4. **监控覆盖**：JVM 指标、业务指标、追踪全覆盖，提前发现和定位问题
5. **优雅关闭**：合理设置 terminationGracePeriodSeconds，确保正在处理的请求能够完成

---

## 参考资料

- [Docker 官方文档](https://docs.docker.com/)
- [Kubernetes 官方文档](https://kubernetes.io/zh/docs/)
- [OpenTelemetry 官方文档](https://opentelemetry.io/docs/)
- [Spring Boot 生产特性](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus 官方文档](https://prometheus.io/docs/introduction/overview/)
- [Grafana 官方文档](https://grafana.com/docs/)