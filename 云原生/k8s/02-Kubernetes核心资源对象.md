# Kubernetes 教程第二章：核心资源对象

## 目录

1. [概述](#概述)
2. [Pod - 最小调度单元](#pod---最小调度单元)
3. [Label、Selector 与 Annotation](#label、selector-与-annotation)
4. [ReplicaSet - 副本集](#replicaset---副本集)
5. [Deployment - 部署](#deployment---部署)
6. [StatefulSet - 有状态副本集](#statefulset---有状态副本集)
7. [DaemonSet - 守护进程集](#daemonset---守护进程集)
8. [Job 与 CronJob - 批处理任务](#job-与-cronjob---批处理任务)
9. [Service - 服务发现](#service---服务发现)
10. [Ingress - HTTP/HTTPS 路由](#ingress---httphttps-路由)
11. [ConfigMap 与 Secret - 配置管理](#configmap-与-secret---配置管理)
12. [Volume 与 PersistentVolume - 存储](#volume-与-persistentvolume---存储)

---

## 概述

Kubernetes（简称 K8s）是一个容器编排平台，其核心功能是自动化管理容器化应用程序的部署、扩缩容和网络通信。在 Kubernetes 中，**所有资源对象都通过 RESTful API 进行管理，并且通常以 YAML 格式声明式地定义期望状态**。

Kubernetes 的核心设计理念是：**声明式 API + 控制器模式**。用户声明期望状态，Kubernetes 控制器负责将实际状态调谐到期望状态。

### 核心资源对象分类

| 类别 | 资源对象 | 作用 |
|------|----------|------|
| **工作负载** | Pod、ReplicaSet、Deployment、StatefulSet、DaemonSet、Job、CronJob | 管理容器运行 |
| **服务发现** | Service、Ingress | 网络通信与路由 |
| **配置与存储** | ConfigMap、Secret、Volume、PersistentVolume、PersistentVolumeClaim | 配置与数据管理 |
| **元数据** | Label、Selector、Annotation | 资源分组与元数据管理 |

---

## Pod - 最小调度单元

### 什么是 Pod

**Pod 是 Kubernetes 中的最小调度单元**，它封装了一个或多个容器、存储资源、网络标识和配置选项。Pod 是 Kubernetes 原子性操作的单位，这意味着整个 Pod 会被调度到同一个节点上。

**关键特性：**
- Pod 中的容器共享相同的网络命名空间（相同的 IP 和端口空间）
- 容器之间可以通过 `localhost` 相互通信
- 容器之间共享存储卷
- Pod 是临时资源，调度后不会迁移（除非主动删除重建）

### Pod 的两种类型

#### 1. 单容器 Pod（最常见）
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-pod
  labels:
    app: nginx
    environment: production
spec:
  containers:
  - name: nginx
    image: nginx:1.24-alpine
    ports:
    - containerPort: 80
    resources:
      limits:
        memory: "128Mi"
        cpu: "500m"
      requests:
        memory: "64Mi"
        cpu: "250m"
```

#### 2. 多容器 Pod（Sidecar 模式）
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: webapp-with-logger
  labels:
    app: webapp
spec:
  containers:
  - name: webapp
    image: mywebapp:1.0
    ports:
    - containerPort: 8080

  - name: logger
    image: busybox:1.36
    command: ["sh", "-c", "while true; do sleep 10; done"]
    volumeMounts:
    - name: shared-logs
      mountPath: /var/log
      readOnly: true

  volumes:
  - name: shared-logs
    emptyDir: {}
```

### Pod 生命周期

Pod 的生命周期经历以下阶段：

```
Pending → Running → Succeeded/Failed
              ↓
           Unknown
```

| 阶段 | 描述 |
|------|------|
| **Pending** | Pod 已被 Kubernetes 系统接受，但容器镜像尚未创建或正在拉取 |
| **Running** | Pod 已绑定到节点，所有容器已创建，至少有一个容器正在运行 |
| **Succeeded** | Pod 中的所有容器都已成功终止，不会重启 |
| **Failed** | Pod 中的所有容器都已终止，至少有一个容器以失败终止 |
| **Unknown** | 无法获取 Pod 状态，通常是节点通信问题 |

### 健康检查（Probes）

Kubernetes 提供三种健康检查机制：

#### 1. 存活探针（Liveness Probe）
检测容器是否存活，如果失败则重启容器。

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-with-liveness
spec:
  containers:
  - name: nginx
    image: nginx:1.24-alpine
    livenessProbe:
      httpGet:
        path: /healthz
        port: 80
      initialDelaySeconds: 10      # 容器启动后等待 10 秒开始探测
      periodSeconds: 5              # 每 5 秒探测一次
      timeoutSeconds: 3             # 探测超时时间
      failureThreshold: 3           # 连续 3 次失败后重启容器
```

#### 2. 就绪探针（Readiness Probe）
检测容器是否准备好接收流量，如果失败则从 Service 负载均衡中移除。

```yaml
spec:
  containers:
  - name: nginx
    image: nginx:1.24-alpine
    readinessProbe:
      httpGet:
        path: /ready
        port: 80
      initialDelaySeconds: 5
      periodSeconds: 3
      successThreshold: 1          # 成功次数阈值
      failureThreshold: 3
```

#### 3. 启动探针（Startup Probe）
用于慢启动容器，在容器启动完成前禁用其他探针。

```yaml
spec:
  containers:
  - name: nginx
    image: nginx:1.24-alpine
    startupProbe:
      httpGet:
        path: /
        port: 80
      failureThreshold: 30          # 最多重试 30 次
      periodSeconds: 10             # 每 10 秒探测一次
```

### 资源限制

#### 资源类型
- **CPU**：单位可以是核心数（`1`、`2`）或毫核（`100m`、`500m`）
- **Memory**：单位可以是 `Ki`、`Mi`、`Gi`（二进制）或 `k`、`M`、`G`（十进制）

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    resources:
      limits:
        memory: "512Mi"
        cpu: "1"
      requests:
        memory: "256Mi"
        cpu: "500m"
```

**最佳实践**：
- `requests` 定义容器最低保证资源
- `limits` 定义容器最大可用资源
- CPU 超过 limits 会被限制（throttling）
- Memory 超过 limits 会导致 OOM 被杀

### 环境变量与 ConfigMap/Secret 引用

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    env:
    - name: DB_HOST
      value: "mysql.database.svc.cluster.local"
    - name: DB_PORT
      value: "3306"
    - name: LOG_LEVEL
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: log_level
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: db-secrets
          key: password
```

### 常用 kubectl 命令

```bash
# 创建 Pod
kubectl apply -f pod.yaml

# 查看 Pod 列表
kubectl get pods
kubectl get pods -o wide                      # 详细信息
kubectl get pods -l app=nginx                 # 标签筛选
kubectl get pods --watch                      # 实时监控

# 查看 Pod 详情
kubectl describe pod nginx-pod

# 查看日志
kubectl logs nginx-pod                        # 查看日志
kubectl logs nginx-pod -c container-name      # 多容器指定容器
kubectl logs nginx-pod --tail=100             # 查看最后 100 行
kubectl logs nginx-pod -f                     # 实时跟踪日志

# 进入容器调试
kubectl exec -it nginx-pod -- /bin/sh

# 删除 Pod
kubectl delete pod nginx-pod

# 扩缩容（通过修改副本数，实际创建的是 ReplicaSet）
kubectl scale pod nginx-pod --replicas=3

# 端口转发（用于本地调试）
kubectl port-forward nginx-pod 8080:80

# 获取 Pod YAML
kubectl get pod nginx-pod -o yaml

# 查看 Pod 资源使用
kubectl top pod
kubectl top pod nginx-pod
```

---

## Label、Selector 与 Annotation

### Label（标签）

Label 是附着在 Kubernetes 对象上的键值对，用于组织和选择一组资源。

**重要特性**：
- 每个资源可以有多个 Label
- 同一资源上 Label 键必须唯一
- Label 键由前缀（可选）和名称组成：`prefix/name`
- 名称段最多 63 个字符

```yaml
metadata:
  labels:
    app: frontend                    # 应用名称
    tier: web                       # 层级
    environment: production         # 环境
    version: v2.0.0                 # 版本
    release: stable                 # 发布类型
```

**有效字符**：
- 名称：以字母或数字开头，可包含字母、数字、连字符、下划线、点
- 前缀：符合 DNS 子域名规范

### Selector（选择器）

Label Selector 用于根据 Label 筛选资源。

#### 等值选择器（Equality-based）

```yaml
# Service 选择带有 app=frontend 和 tier=web 的 Pod
selector:
  app: frontend
  tier: web
```

```bash
# kubectl 命令行等效
kubectl get pods -l app=frontend,tier=web
kubectl get pods -l 'app=frontend'
kubectl get pods -l 'app!=frontend'
```

#### 集合选择器（Set-based）

```yaml
selector:
  matchExpressions:
  - key: app
    operator: In
    values:
    - frontend
    - backend
  - key: tier
    operator: NotIn
    values:
    - dev
  - key: environment
    operator: Exists              # 存在此标签
  - key: environment
    operator: DoesNotExist       # 不存在此标签
```

```bash
# kubectl 命令行等效
kubectl get pods -l 'app in (frontend, backend)'
kubectl get pods -l 'app notin (dev)'
kubectl get pods -l 'environment'
kubectl get pods -l '!environment'
```

### Annotation（注解）

Annotation 与 Label 相似，但用于存储非标识性元数据。

**用途**：
- 添加说明性信息（构建信息、团队联系方式）
- 记录配置变更信息
- 存储第三方工具使用的数据
- 管理工具状态标记

```yaml
metadata:
  annotations:
    description: "前端服务，负责用户界面"
    maintainer: "team-frontend@example.com"
    lastModified: "2024-01-15"
    kubernetes.io/change-cause: "升级到 v2.0 版本"
    prometheus.io/scrape: "true"
    prometheus.io/port: "9090"
```

### 常用 kubectl 命令

```bash
# 查看资源标签
kubectl get pods --show-labels

# 添加标签
kubectl label pods nginx-pod environment=production

# 更新标签
kubectl label pods nginx-pod environment=staging --overwrite

# 删除标签
kubectl label pods nginx-pod environment-

# 添加注解
kubectl annotate pods nginx-pod description="测试注释"

# 查看注解
kubectl describe pod nginx-pod | grep Annotations

# 按标签筛选
kubectl get all -l app=frontend
```

---

## ReplicaSet - 副本集

### 什么是 ReplicaSet

ReplicaSet 是用于确保指定数量的 Pod 副本始终运行的资源对象。它是 Kubernetes 自我修复机制的基础。

**核心功能**：
- 维持指定的 Pod 副本数量
- 当 Pod 数量低于期望时自动创建
- 当 Pod 数量高于期望时自动删除多余 Pod
- 支持标签选择器

### ReplicaSet 与 Deployment 的关系

**重要提示**：在生产环境中，通常不直接创建 ReplicaSet，而是通过 Deployment 来管理。Deployment 会自动创建和管理 ReplicaSet，实现滚动更新和回滚功能。

```
Deployment → ReplicaSet → Pod
```

### ReplicaSet 定义

```yaml
apiVersion: apps/v1
kind: ReplicaSet
metadata:
  name: nginx-replicaset
  labels:
    app: nginx
spec:
  replicas: 3                        # 期望副本数
  selector:                         # 选择器（必须匹配 Pod 标签）
    matchLabels:
      app: nginx
  template:                         # Pod 模板
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.24-alpine
        ports:
        - containerPort: 80
        resources:
          limits:
            memory: "128Mi"
            cpu: "500m"
          requests:
            memory: "64Mi"
            cpu: "250m"
```

### 常用 kubectl 命令

```bash
# 创建 ReplicaSet
kubectl apply -f replicaset.yaml

# 查看 ReplicaSet
kubectl get replicaset
kubectl get rs

# 查看 ReplicaSet 详情
kubectl describe replicaset nginx-replicaset

# 扩缩容
kubectl scale replicaset nginx-replicaset --replicas=5

# 删除 ReplicaSet（会同时删除管理的 Pod）
kubectl delete replicaset nginx-replicaset

# 只删除 ReplicaSet，保留 Pod
kubectl delete replicaset nginx-replicaset --cascade=orphan
```

---

## Deployment - 部署

### 什么是 Deployment

Deployment 是 Kubernetes 最常用的 workload 资源，用于管理无状态应用的部署。它提供了声明式的方式管理 Pod 和 ReplicaSet，支持滚动更新、回滚、扩缩容等操作。

### Deployment 定义

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nginx-deployment
  labels:
    app: nginx
spec:
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  # 滚动更新策略
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1              # 最多可以超出期望副本数
      maxUnavailable: 0        # 滚动过程中最少保持的可用副本数
  # Pod 模板
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:1.24-alpine
        ports:
        - containerPort: 80
        resources:
          limits:
            memory: "128Mi"
            cpu: "500m"
          requests:
            memory: "64Mi"
            cpu: "250m"
        # 滚动更新探针
        readinessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 5
          periodSeconds: 3
```

### 部署策略

#### 1. RollingUpdate（滚动更新，默认）

```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1                # 默认 25%
      maxUnavailable: 0          # 默认 25%
```

**maxSurge**：滚动更新过程中，期望副本数之外可以创建的额外 Pod 数量
- 数值：1 或百分比 25%
- `maxSurge: 1` + `replicas: 3` → 最多同时有 4 个 Pod

**maxUnavailable**：滚动更新过程中，可以不可用的 Pod 数量
- 数值：0 或百分比 25%
- `maxUnavailable: 0` + `replicas: 3` → 始终保持 3 个可用 Pod

#### 2. Recreate（重建）

```yaml
spec:
  strategy:
    type: Recreate
```

删除所有现有 Pod 后再创建新的 Pod，适用于不能同时运行两个版本的应用。

### 滚动更新机制

当更新 Deployment 中的容器镜像时：

```
原有状态: [Pod v1.0] [Pod v1.0] [Pod v1.0]

更新中:   [Pod v1.0] [Pod v1.0] [Pod v1.0] [Pod v2.0 (创建中)]

更新完成: [Pod v1.0] [Pod v2.0] [Pod v2.0] → 等待旧 Pod 终止
```

### 回滚

```yaml
# revision history 示例，保存历史版本
spec:
  revisionHistoryLimit: 10        # 保留的历史版本数量，默认 10
```

### 常用 kubectl 命令

```bash
# 创建 Deployment
kubectl apply -f deployment.yaml

# 查看 Deployment
kubectl get deployment
kubectl get deploy

# 查看 Deployment 详情
kubectl describe deployment nginx-deployment

# 查看滚动历史
kubectl rollout history deployment/nginx-deployment

# 查看特定版本详情
kubectl rollout history deployment/nginx-deployment --revision=2

# 滚动更新（修改镜像）
kubectl set image deployment/nginx-deployment nginx=nginx:1.25-alpine

# 滚动回滚
kubectl rollout undo deployment/nginx-deployment           # 回滚到上一个版本
kubectl rollout undo deployment/nginx-deployment --to-revision=2  # 回滚到指定版本

# 暂停/恢复滚动
kubectl rollout pause deployment/nginx-deployment
kubectl rollout resume deployment/nginx-deployment

# 扩缩容
kubectl scale deployment nginx-deployment --replicas=5

# 自动扩缩容（HPA - HorizontalPodAutoscaler）
kubectl autoscale deployment nginx-deployment --min=2 --max=10 --cpu-percent=80

# 查看 ReplicaSet
kubectl get replicasets
kubectl get rs

# 删除 Deployment
kubectl delete deployment nginx-deployment

# 金丝雀部署示例（通过 label 控制部分流量）
kubectl patch deployment nginx-deployment -p "spec:\n  replicas: 4"
kubectl label pods -l app=nginx version=v2 --overwrite
# 手动验证后，再更新全部
```

### 最佳实践

1. **始终设置 `strategy` 和 `resources`**
2. **使用 `readinessProbe` 确保滚动更新安全**
3. **合理设置 `revisionHistoryLimit`** 保存足够的历史版本
4. **镜像标签不要使用 `latest`**，使用具体版本号
5. **在生产环境使用 `maxUnavailable: 0` 和 `maxSurge: 1`** 实现最保守的滚动策略

---

## StatefulSet - 有状态副本集

### 什么是 StatefulSet

StatefulSet 是用于管理有状态应用的资源对象。与 Deployment 不同，StatefulSet 为每个 Pod 提供稳定的唯一标识和网络标识。

### StatefulSet 与 Deployment 的区别

| 特性 | Deployment | StatefulSet |
|------|------------|-------------|
| Pod 标识 | 随机，不稳定 | 稳定，持久 |
| 启动顺序 | 并行启动 | 顺序启动（0 到 N-1） |
| 终止顺序 | 并行终止 | 逆序终止（N-1 到 0） |
| 网络标识 | 共享 Service IP | 稳定的域名（pod-name.service-name.namespace.svc.cluster.local） |
| 存储卷 | 共享或随机 | 每个 Pod 有独立的 PVC |
| 扩缩容 | 随意扩缩 | 需考虑存储 |

### StatefulSet 定义

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql-statefulset
  labels:
    app: mysql
spec:
  serviceName: mysql-headless     # 必须与 Headless Service 名称一致
  replicas: 3
  selector:
    matchLabels:
      app: mysql
  # Pod 管理策略
  podManagementPolicy: OrderedReady   # OrderedReady 或 Parallel
  # 更新策略
  updateStrategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
  # 服务保障
  serviceName: mysql-headless
  # 持久化存储卷
  volumeClaimTemplates:            # 类似 Pod 模板，但用于 PVC
  - metadata:
      name: data
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi
      storageClassName: standard
  template:
    metadata:
      labels:
        app: mysql
    spec:
      terminationGracePeriodSeconds: 30
      containers:
      - name: mysql
        image: mysql:8.0
        ports:
        - containerPort: 3306
          name: mysql
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secrets
              key: root-password
        volumeMounts:
        - name: data
          mountPath: /var/lib/mysql
        resources:
          limits:
            memory: "1Gi"
            cpu: "500m"
          requests:
            memory: "512Mi"
            cpu: "250m"
        # 存活探针
        livenessProbe:
          exec:
            command: ["mysqladmin", "ping", "-h", "localhost"]
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          exec:
            command: ["mysql", "-h", "localhost", "-u", "root", "-p$MYSQL_ROOT_PASSWORD", "-e", "SELECT 1"]
          initialDelaySeconds: 20
          periodSeconds: 5
```

### 关联的 Headless Service

StatefulSet 需要一个 Headless Service 来管理 Pod 的 DNS 记录：

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-headless
  labels:
    app: mysql
spec:
  clusterIP: None                  # Headless Service 必须设为 None
  selector:
    app: mysql
  ports:
  - port: 3306
    targetPort: 3306
    name: mysql
```

### Pod 标识规则

对于 `mysql-statefulset` 有 3 个副本，Pod 名称和 DNS 名称为：

| Pod 序号 | Pod 名称 | DNS 名称 |
|----------|----------|----------|
| 0 | mysql-statefulset-0 | mysql-statefulset-0.mysql-headless.default.svc.cluster.local |
| 1 | mysql-statefulset-1 | mysql-statefulset-1.mysql-headless.default.svc.cluster.local |
| 2 | mysql-statefulset-2 | mysql-statefulset-2.mysql-headless.default.svc.cluster.local |

### 扩缩容行为

**扩缩容规则**：
- **扩容**：按顺序创建 Pod（0 → 1 → 2 → ...）
- **缩容**：逆序删除 Pod（... → 2 → 1 → 0）
- **数据安全**：缩容前会等待新 Pod 就绪后才删除旧 Pod

### 常用 kubectl 命令

```bash
# 创建 StatefulSet
kubectl apply -f statefulset.yaml

# 查看 StatefulSet
kubectl get statefulset
kubectl get sts

# 查看 StatefulSet 详情
kubectl describe statefulset mysql-statefulset

# 扩缩容（缩容会逆序删除）
kubectl scale statefulset mysql-statefulset --replicas=5

# 删除 StatefulSet（不会删除 PVC，需手动删除）
kubectl delete statefulset mysql-statefulset

# 查看 Pod 的稳定唯一标识
kubectl get pods -l app=mysql

# 删除 PVC（谨慎操作，会删除数据）
kubectl delete pvc data-mysql-statefulset-0
```

### 典型使用场景

- **数据库**：MySQL、PostgreSQL、MongoDB
- **分布式缓存**：Redis Cluster
- **消息队列**：Kafka、RabbitMQ 集群
- **有状态微服务**：需要持久化客户端连接的应用

---

## DaemonSet - 守护进程集

### 什么是 DaemonSet

DaemonSet 确保集群中每个节点（或满足选择条件的节点）上都运行一个 Pod 副本。当新节点加入集群时，会自动在该节点上创建 Pod；当节点从集群移除时，Pod 会被垃圾回收。

### 典型使用场景

- **日志收集**：Fluentd、Logstash
- **监控代理**：Prometheus Node Exporter、Datadog Agent
- **网络插件**：Calico、Cilium 的节点组件
- **存储守护进程**：GlusterFS、Ceph 的客户端
- **集群 DNS**：CoreDNS（在某些配置下）

### DaemonSet 定义

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd-logging
  labels:
    app: fluentd
spec:
  selector:
    matchLabels:
      app: fluentd
  # 更新策略
  updateStrategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
  # 节点选择
  nodeSelector:                    # 只调度到带有此标签的节点
    node-type: logging
  tolerations:                     # 容忍污点
  - key: node-role.kubernetes.io/master
    operator: Exists
    effect: NoSchedule
  template:
    metadata:
      labels:
        app: fluentd
    spec:
      containers:
      - name: fluentd
        image: fluent/fluentd:v1.16-1
        resources:
          limits:
            memory: "256Mi"
            cpu: "250m"
          requests:
            memory: "128Mi"
            cpu: "100m"
        volumeMounts:
        - name: varlog
          mountPath: /var/log
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
      # 调度亲和性（可选，更精细的控制）
      affinity:
        nodeAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
            nodeSelectorTerms:
            - matchExpressions:
              - key: kubernetes.io/os
                operator: NotIn
                values:
                - windows
```

### DaemonSet 与 Deployment 的区别

| 特性 | DaemonSet | Deployment |
|------|-----------|------------|
| 调度目标 | 每个节点一个 Pod | 任意节点，按需调度 |
| 调度保证 | 确保每个节点一个 | 不保证节点分布 |
| 缩容行为 | 节点移除时 Pod 自动删除 | Pod 调度到其他节点 |
| 滚动更新 | 支持 | 支持 |

### 常用 kubectl 命令

```bash
# 创建 DaemonSet
kubectl apply -f daemonset.yaml

# 查看 DaemonSet
kubectl get daemonset
kubectl get ds

# 查看 DaemonSet 详情
kubectl describe daemonset fluentd-logging

# 查看 DaemonSet Pod 分布
kubectl get pods -l app=fluentd -o wide

# 滚动更新
kubectl set image daemonset/fluentd-logging fluentd=flud/fluentd:v2

# 滚动回滚
kubectl rollout undo daemonset/fluentd-logging

# 删除 DaemonSet
kubectl delete daemonset fluentd-logging
```

---

## Job 与 CronJob - 批处理任务

### Job

Job 用于创建一次性任务，确保一个或多个 Pod 成功完成指定工作。

#### Job 定义

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: pi-job
  labels:
    app: pi
spec:
  # 并行执行数
  parallelism: 2                   # 同时运行的 Pod 数量
  # 成功完成次数（Completions）
  completions: 5                  # 成功完成的 Pod 总数
  # 超时时间
  activeDeadlineSeconds: 300      # Job 最大运行时间
  # 失败重试次数
  backoffLimit: 3                 # 失败重试次数
  # 保留成功 Job 的时间
  ttlSecondsAfterFinished: 100    # 完成后 100 秒自动删除
  template:
    metadata:
      labels:
        app: pi
    spec:
      restartPolicy: OnFailure     # 注意：Job 必须设置为 OnFailure 或 Never
      containers:
      - name: pi
        image: perl:5.34
        command: ["perl", "-Mbignum=bpi", "-wle", "print bpi(2000)"]
        resources:
          limits:
            memory: "512Mi"
            cpu: "500m"
          requests:
            memory: "256Mi"
            cpu: "100m"
```

#### Job 并行执行模式

| 模式 | completions | parallelism | 说明 |
|------|-------------|-------------|------|
| 非并行 | 1 | 1（可选） | 一个 Pod 完成任务即结束 |
| 固定完成数 | N | 可选 | N 个 Pod 成功完成 |
| 工作队列 | 未设置 | > 1 | 任意 Pod 完成所有任务即结束 |

```yaml
# 工作队列模式示例
spec:
  parallelism: 4                  # 4 个 Pod 并行处理
  completions: null               # 不设置，无固定完成数
  template:
    spec:
      containers:
      - name: worker
        image: myworker:1.0
        command: ["python", "/worker.py"]
      restartPolicy: OnFailure
```

### CronJob

CronJob 是基于时间调度的 Job，用于创建周期性任务。

#### CronJob 定义

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: database-backup
  labels:
    app: backup
spec:
  # Cron 表达式
  schedule: "0 2 * * *"           # 每天凌晨 2 点执行
  # 时区（Kubernetes 1.27+）
  timeZone: "Asia/Shanghai"
  # 并发策略
  concurrencyPolicy: Forbid       # Forbid | Allow | Replace
  # 跳过错过时间（集群不可用时）
  successfulJobsHistoryLimit: 3   # 保留成功 Job 历史数
  failedJobsHistoryLimit: 1        # 保留失败 Job 历史数
  # 暂停 CronJob
  suspend: false
  # Job 超时时间
  startingDeadlineSeconds: 200    # Job 开始的最晚时间
  jobTemplate:
    metadata:
      labels:
        app: backup
    spec:
      ttlSecondsAfterFinished: 3600
      template:
        metadata:
          labels:
            app: backup
        spec:
          restartPolicy: OnFailure
          containers:
          - name: backup
            image: postgres:15-alpine
            command:
            - /bin/sh
            - -c
            - |
              pg_dump -h postgres.database.svc.cluster.local \
                      -U postgres \
                      -d mydb \
                      -F custom \
                      -f /backups/mydb-$(date +%Y%m%d%H%M%S).dump
            env:
            - name: PGPASSWORD
              valueFrom:
                secretKeyRef:
                  name: db-secrets
                  key: password
            volumeMounts:
            - name: backup-storage
              mountPath: /backups
          volumes:
          - name: backup-storage
            persistentVolumeClaim:
              claimName: backup-pvc
          # 节点亲和性（可选）
          affinity:
            nodeAffinity:
              preferredDuringSchedulingIgnoredDuringExecution:
              - weight: 1
                preference:
                  matchExpressions:
                  - key: node-type
                    operator: In
                    values:
                    - backup-node
```

#### Cron 表达式格式

```
┌───────────── 分钟 (0-59)
│ ┌─────────── 小时 (0-23)
│ │ ┌───────── 日 (1-31)
│ │ │ ┌─────── 月 (1-12)
│ │ │ │ ┌───── 星期 (0-6, 0 是星期日)
│ │ │ │ │
* * * * *
```

**常用示例**：
- `0 * * * *` - 每小时
- `0 0 * * *` - 每天午夜
- `0 2 * * *` - 每天凌晨 2 点
- `0 */4 * * *` - 每 4 小时
- `0 0 * * 0` - 每周日午夜
- `0 0 1 * *` - 每月 1 日午夜
- `*/5 * * * *` - 每 5 分钟

#### 并发策略

| 策略 | 说明 |
|------|------|
| Allow（默认） | 允许并发运行多个 Job |
| Forbid | 如果上一个 Job 还在运行，跳过本次执行 |
| Replace | 取消正在运行的 Job，启动新的 |

### 常用 kubectl 命令

```bash
# 创建 Job
kubectl apply -f job.yaml

# 查看 Job
kubectl get job
kubectl get jobs

# 查看 Job 详情
kubectl describe job pi-job

# 查看 Job 的 Pod
kubectl get pods -l job-name=pi-job

# 查看 Job 日志
kubectl logs -l job-name=pi-job

# 删除 Job
kubectl delete job pi-job

# 创建 CronJob
kubectl apply -f cronjob.yaml

# 查看 CronJob
kubectl get cronjob
kubectl get cj

# 暂停 CronJob
kubectl patch cronjob database-backup -p '{"spec":{"suspend":true}}'

# 恢复 CronJob
kubectl patch cronjob database-backup -p '{"spec":{"suspend":false}}'

# 手动触发 CronJob（立即执行一次）
kubectl create job backup-now --from=cronjob/database-backup

# 删除 CronJob
kubectl delete cronjob database-backup
```

### 最佳实践

1. **Job**：
   - 设置合理的 `activeDeadlineSeconds` 防止挂起
   - 使用 `ttlSecondsAfterFinished` 自动清理完成 Job
   - 设置 `restartPolicy` 为 `OnFailure` 或 `Never`

2. **CronJob**：
   - 生产环境使用 `concurrencyPolicy: Forbid` 或 `Replace`
   - 设置合理的 `successfulJobsHistoryLimit` 和 `failedJobsHistoryLimit`
   - 考虑使用时区设置
   - Job 应设计为幂等操作

---

## Service - 服务发现

### 什么是 Service

Service 是 Kubernetes 提供的一种抽象，它定义了一组 Pod 的逻辑集合和访问它们的策略。Service 为这组 Pod 提供稳定的 IP 地址和 DNS 名称，实现负载均衡和服务发现。

### Service 类型

#### 1. ClusterIP（默认）

在集群内部提供访问，分配一个集群内部 IP。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-clusterip
  labels:
    app: nginx
spec:
  type: ClusterIP
  selector:
    app: nginx
  ports:
  - name: http
    port: 80                # Service 端口
    targetPort: 80         # Pod 端口
    protocol: TCP
  - name: https
    port: 443
    targetPort: 443
    protocol: TCP
```

**访问方式**：
- 同命名空间：`nginx-clusterip`
- 不同命名空间：`nginx-clusterip.default.svc.cluster.local`
- 集群内任意位置：`nginx-clusterip.default.svc.cluster.local`

#### 2. NodePort

在每个节点的 IP 上开放一个静态端口访问服务。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-nodeport
  labels:
    app: nginx
spec:
  type: NodePort
  selector:
    app: nginx
  ports:
  - name: http
    port: 80              # Service 端口
    targetPort: 80        # Pod 端口
    nodePort: 30080       # 节点端口（30000-32767）
    protocol: TCP
  - name: https
    port: 443
    targetPort: 443
    nodePort: 30443
    protocol: TCP
```

**访问方式**：
- 节点 IP + 节点端口：`http://<node-ip>:30080`
- 所有节点 IP 都可访问

#### 3. LoadBalancer

使用云提供商的负载均衡器暴露服务。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: nginx-loadbalancer
  labels:
    app: nginx
spec:
  type: LoadBalancer
  selector:
    app: nginx
  ports:
  - name: http
    port: 80
    targetPort: 80
    protocol: TCP
  - name: https
    port: 443
    targetPort: 443
    protocol: TCP
  # AWS NLB 配置示例
  externalTrafficPolicy: Cluster    # Cluster 或 Local
  loadBalancerIP: 1.2.3.4           # 指定 IP（需要云提供商支持）
  loadBalancerSourceRanges:         # 限制 IP 范围
  - 10.0.0.0/8
```

#### 4. ExternalName

将服务映射到 DNS 名称。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-external
  labels:
    app: mysql
spec:
  type: ExternalName
  externalName: mysql.database.example.com   # 外部 DNS 名称
```

### Headless Service

不分配 ClusterIP，用于 StatefulSet 或自定义服务发现。

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mysql-headless
  labels:
    app: mysql
spec:
  clusterIP: None              # 必须为 None
  selector:
    app: mysql
  ports:
  - port: 3306
    targetPort: 3306
```

### Service 端点（Endpoints）

Service 自动创建 Endpoints 对象，列出所有匹配的 Pod IP 和端口。

```yaml
# 查看 Service 对应的 Endpoints
kubectl get endpoints nginx-service

# Endpoints 示例
apiVersion: v1
kind: Endpoints
metadata:
  name: nginx-service
subsets:
- addresses:
  - ip: 10.244.1.15
    targetRef:
      kind: Pod
      name: nginx-deployment-7fb96c846b-2h4xz
      namespace: default
  - ip: 10.244.1.16
    targetRef:
      kind: Pod
      name: nginx-deployment-7fb96c846b-5tr4s
  ports:
  - port: 80
    protocol: TCP
```

### 多端口 Service

```yaml
apiVersion: v1
kind: Service
metadata:
  name: multiport-service
  labels:
    app: myapp
spec:
  type: ClusterIP
  selector:
    app: myapp
  ports:
  - name: http
    port: 80
    targetPort: 8080
    protocol: TCP
  - name: grpc
    port: 50051
    targetPort: 50051
    protocol: TCP
  - name: admin
    port: 9090
    targetPort: 9090
    protocol: TCP
```

### 常用 kubectl 命令

```bash
# 创建 Service
kubectl apply -f service.yaml

# 查看 Service
kubectl get service
kubectl get svc

# 查看 Service 详情
kubectl describe service nginx-service

# 查看 Service 的 Endpoints
kubectl get endpoints nginx-service

# 端口转发（本地调试）
kubectl port-forward svc/nginx-service 8080:80

# 暴露 Deployment 为 Service
kubectl expose deployment nginx-deployment --type=ClusterIP --port=80 --target-port=80

# 暴露 Deployment 为 NodePort
kubectl expose deployment nginx-deployment --type=NodePort --port=80 --target-port=80 --node-port=30080

# 删除 Service
kubectl delete service nginx-service

# 查看 Service 详细信息
kubectl get svc nginx-service -o yaml
```

### 服务发现

在 Kubernetes 集群内部，可以通过以下方式发现服务：

1. **环境变量**（默认启用）
   ```
   {SVCNAME}_SERVICE_HOST
   {SVCNAME}_SERVICE_PORT
   ```

2. **DNS**（推荐）
   - A 记录：`nginx.default.svc.cluster.local`
   - SRV 记录：`_http._tcp.nginx.default.svc.cluster.local`

### 最佳实践

1. **始终为 Service 设置 `name` 端口**，便于调试和文档
2. **使用 `selector` 精确匹配**，避免意外的 Pod 被包含
3. **为 StatefulSet 使用 Headless Service**
4. **生产环境优先使用 LoadBalancer 或 Ingress**
5. **使用 `sessionAffinity: ClientIP`** 实现会话保持（如需要）

---

## Ingress - HTTP/HTTPS 路由

### 什么是 Ingress

Ingress 是 Kubernetes 资源对象，用于 HTTP/HTTPS 外部访问到集群内部服务的路由规则。它提供 URL 访问、SSL/TLS 终止、虚拟主机和基于路径的路由等功能。

### Ingress 资源定义

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: myapp-ingress
  labels:
    app: myapp
  annotations:
    # Nginx Ingress Controller 注解
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/proxy-connect-timeout: "30"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "1800"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "1800"
spec:
  ingressClassName: nginx          # 指定 Ingress Class（K8s 1.18+）
  # TLS 配置
  tls:
  - hosts:
    - myapp.example.com
    secretName: myapp-tls
  rules:
  - host: myapp.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: myapp-service
            port:
              number: 80
      - path: /api
        pathType: Prefix
        backend:
          service:
            name: api-service
            port:
              number: 8080
  # 默认后端（当没有匹配规则时）
  defaultBackend:
    service:
      name: default-backend
      port:
        number: 80
```

### 基于路径的路由

```yaml
spec:
  rules:
  - host: app.example.com
    http:
      paths:
      # 前端应用
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
      # API 服务
      - path: /api/v1
        pathType: Prefix
        backend:
          service:
            name: api-v1-service
            port:
              number: 8080
      # 静态资源
      - path: /static
        pathType: Prefix
        backend:
          service:
            name: static-service
            port:
              number: 80
```

### 基于主机名的路由

```yaml
spec:
  rules:
  # 第一个域名
  - host: frontend.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: frontend-service
            port:
              number: 80
  # 第二个域名
  - host: backend.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: backend-service
            port:
              number: 8080
```

### TLS 证书 Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: myapp-tls
type: kubernetes.io/tls
data:
  # Base64 编码的证书
  tls.crt: LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0t...
  # Base64 编码的私钥
  tls.key: LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0t...
```

### Ingress Class

```yaml
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: nginx
  annotations:
    ingressclass.kubernetes.io/is-default-class: "true"
spec:
  controller: k8s.io/ingress-nginx
  parameters:
    apiGroup: k8s.example.com
    kind: IngressParameters
    name: external-lb
```

### 常用 kubectl 命令

```bash
# 创建 Ingress
kubectl apply -f ingress.yaml

# 查看 Ingress
kubectl get ingress
kubectl get ing

# 查看 Ingress 详情
kubectl describe ingress myapp-ingress

# 获取 Ingress YAML
kubectl get ingress myapp-ingress -o yaml

# 端口转发（需要 Ingress Controller 支持）
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80

# 删除 Ingress
kubectl delete ingress myapp-ingress
```

### 常见 Ingress Controller

| Controller | 注解前缀 |
|------------|----------|
| **Nginx Ingress Controller** | `nginx.ingress.kubernetes.io/` |
| **Traefik** | `traefik.ingress.kubernetes.io/` |
| **HAProxy Ingress** | `haproxy-ingress.github.io/` |
| **AWS ALB Ingress Controller** | `alb.ingress.kubernetes.io/` |
| **GKE Ingress** | `kubernetes.io/ingress.class: gce` |

### 最佳实践

1. **始终使用 TLS（HTTPS）**
2. **使用 `ingressClassName` 而不是注解**（K8s 1.18+ 推荐）
3. **设置 `pathType: ImplementationSpecific`** 用于后端配置复杂路由
4. **使用默认后端处理 404 场景**
5. **在 TLS Secret 中使用真实证书，不要使用自签名证书生产环境**
6. **使用注解配置重写规则、速率限制、超时等**

---

## ConfigMap 与 Secret - 配置管理

### ConfigMap

ConfigMap 用于存储非敏感的配置数据，如配置文件、环境变量、命令行参数等。

#### 创建 ConfigMap

**方式一：通过 YAML 创建**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  labels:
    app: myapp
data:
  # 键值对格式
  DATABASE_HOST: "postgres.database.svc.cluster.local"
  DATABASE_PORT: "5432"
  LOG_LEVEL: "info"

  # 配置文件格式（使用 multi-line）
  app.conf: |
    server {
      port = 8080
      host = "0.0.0.0"
      timeout = 30
    }

  redis.conf: |
    maxmemory 256mb
    maxmemory-policy allkeys-lru
    appendonly yes
```

**方式二：通过文件创建**

```bash
# 从文件创建 ConfigMap
kubectl create configmap app-config --from-file=app.conf=./app.conf

# 从目录创建（目录下所有文件都会成为 ConfigMap 的键值对）
kubectl create configmap app-config --from-file=./config/

# 从环境变量文件创建
kubectl create configmap db-config --from-env-file=./db.env

# 直接从命令行创建
kubectl create configmap my-config --from-literal=key1=value1 --from-literal=key2=value2
```

### Secret

Secret 用于存储敏感数据，如密码、OAuth 令牌、SSH 密钥等。Secret 的数据以 Base64 编码存储。

#### Secret 类型

| 类型 | 用途 |
|------|------|
| `Opaque` | 通用 Secret（默认） |
| `kubernetes.io/tls` | TLS 证书和私钥 |
| `kubernetes.io/dockerconfigjson` | Docker 仓库认证 |
| `kubernetes.io/basic-auth` | Basic 认证凭证 |
| `kubernetes.io/ssh-auth` | SSH 凭证 |

#### Opaque Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  labels:
    app: myapp
type: Opaque
data:
  # 密码（echo -n "secretpass" | base64）
  DB_PASSWORD: c2VjcmV0cGFzcw==
  # API 密钥
  API_KEY: YXBpLWtleS12YWx1ZQ==
  # Base64 编码的文件内容
  certificate.pem: LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0t...
stringData:
  # 可以直接写明文（会自动 Base64 编码存储）
  plain-text-secret: "this-will-be-encoded"
```

#### TLS Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: tls-secret
  labels:
    app: myapp
type: kubernetes.io/tls
data:
  # Base64 编码的证书
  tls.crt: LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0t...
  # Base64 编码的私钥
  tls.key: LS0tLS1CRUdJTiBQUklWQVRFIEtFWS0tLS0t...
```

#### Docker Registry Secret

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: docker-registry-secret
type: kubernetes.io/dockerconfigjson
data:
  .dockerconfigjson: eyJhdXRocyI6eyJxdWF5LmNvbSI6eyJ1c2VybmFtZSI6InRlc3QiLCJwYXNzd29yZCI6InRlc3QiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20ifX0=
```

创建命令：
```bash
kubectl create secret docker-registry my-registry-secret \
  --docker-server=quay.io \
  --docker-username=myuser \
  --docker-password=mypassword \
  --docker-email=myemail@example.com
```

### Pod 中使用 ConfigMap 和 Secret

#### 作为环境变量

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    env:
    # 简单值
    - name: DATABASE_HOST
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: DATABASE_HOST
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: app-secrets
          key: DB_PASSWORD
    # 引用所有键值对
    envFrom:
    - configMapRef:
        name: app-config
    - secretRef:
        name: app-secrets
```

#### 作为命令行参数

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    command:
    - /app
    - --config
    - /etc/config/app.conf
    - --log-level
    - $(LOG_LEVEL)
    env:
    - name: LOG_LEVEL
      valueFrom:
        configMapKeyRef:
          name: app-config
          key: LOG_LEVEL
```

#### 作为文件挂载

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    # 配置文件
    - name: config-volume
      mountPath: /etc/config
      readOnly: true
    # 敏感文件
    - name: secrets-volume
      mountPath: /etc/secrets
      readOnly: true
    # TLS 证书
    - name: tls-volume
      mountPath: /etc/tls
      readOnly: true
  volumes:
  - name: config-volume
    configMap:
      name: app-config
      items:
      - key: app.conf
        path: app.conf
      - key: redis.conf
        path: redis.conf
  - name: secrets-volume
    secret:
      secretName: app-secrets
      items:
      - key: DB_PASSWORD
        path: db_password
  - name: tls-volume
    secret:
      secretName: tls-secret
```

### 常用 kubectl 命令

```bash
# 创建 ConfigMap
kubectl create configmap app-config --from-file=app.conf=./app.conf
kubectl apply -f configmap.yaml

# 查看 ConfigMap
kubectl get configmap
kubectl get cm
kubectl describe configmap app-config

# 获取 ConfigMap 内容
kubectl get configmap app-config -o yaml

# 创建 Secret
kubectl create secret generic app-secrets --from-literal=DB_PASSWORD=secretpass
kubectl create secret tls tls-secret --cert=./tls.crt --key=./tls.key
kubectl apply -f secret.yaml

# 查看 Secret（默认不显示值）
kubectl get secret app-secrets

# 解码 Secret 值
kubectl get secret app-secrets -o jsonpath='{.data.DB_PASSWORD}' | base64 --decode

# 删除 ConfigMap/Secret
kubectl delete configmap app-config
kubectl delete secret app-secrets
```

### 最佳实践

1. **区分 ConfigMap 和 Secret**：敏感数据必须使用 Secret
2. **不要在 YAML 中直接写 Base64 值**，使用 `stringData` 或创建时自动编码
3. **使用 `items` 精确控制挂载的文件名**
4. **ConfigMap 和 Secret 有大小限制**（Etcd 默认限制 1MB）
5. **大配置文件考虑使用挂载方式**，小配置使用环境变量
6. **Pod 引用不存在的 ConfigMap/Secret 会导致 Pod 启动失败**
7. **使用 `readOnly: true`** 保护挂载的配置文件

---

## Volume 与 PersistentVolume - 存储

### Volume

Volume 是 Pod 的一部分，与 Pod 生命周期相同。

#### 常用 Volume 类型

##### 1. emptyDir

临时目录，同 Pod 生命周期，用于共享存储或临时缓存。

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: cache
      mountPath: /tmp/cache
  volumes:
  - name: cache
    emptyDir:
      sizeLimit: 100Mi
      medium: Memory    # 存储在内存中（tmpfs）
```

##### 2. hostPath

将节点文件系统挂载到 Pod 中。

```yaml
volumes:
- name: log-volume
  hostPath:
    path: /var/log/pods
    type: Directory
```

**type 选项**：
- `Directory`：目录必须存在
- `DirectoryOrCreate`：目录不存在时自动创建
- `File`：文件必须存在
- `FileOrCreate`：文件不存在时自动创建

**注意**：hostPath 存在安全隐患，生产环境慎用。

##### 3. ConfigMap/Secret 作为 Volume

```yaml
volumes:
- name: config
  configMap:
    name: app-config
- name: secrets
  secret:
    secretName: app-secrets
```

##### 4. NFS（网络文件系统）

```yaml
volumes:
- name: nfs-storage
  nfs:
    server: nfs-server.example.com
    path: /exported/path
```

### PersistentVolume（PV）与 PersistentVolumeClaim（PVC）

PV 是集群级别的存储资源，PVC 是用户对存储的请求。

#### PersistentVolume 定义

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: my-pv
  labels:
    type: local
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce          # 单节点读写
    # - ReadOnlyMany          # 多节点只读
    # - ReadWriteMany         # 多节点读写
  persistentVolumeReclaimPolicy: Retain   # Retain | Recycle | Delete
  storageClassName: standard
  mountOptions:
    - hard
    - nfsvers=4
  nfs:
    server: nfs-server.example.com
    path: /data/my-pv
```

#### PersistentVolumeClaim 定义

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc
  labels:
    app: myapp
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
  storageClassName: standard
  # 可选：指定 PV 名称（静态绑定）
  volumeName: my-pv
  # 可选：标签选择器
  selector:
    matchLabels:
      type: fast
```

#### Pod 使用 PVC

```yaml
spec:
  containers:
  - name: app
    image: myapp:1.0
    volumeMounts:
    - name: data
      mountPath: /var/lib/myapp
  volumes:
  - name: data
    persistentVolumeClaim:
      claimName: my-pvc
```

### StorageClass

StorageClass 动态提供 PV，用户无需预先创建 PV。

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: fast-storage
provisioner: kubernetes.io/gce-pd     # 供应器
parameters:
  type: pd-ssd                        # GCP PD SSD
  replication-type: regional-pd
reclaimPolicy: Delete
allowVolumeExpansion: true
volumeBindingMode: WaitForFirstConsumer   # 延迟绑定
```

#### 常用 provisioner

| 云提供商 | Provisioner |
|----------|-------------|
| AWS | `kubernetes.io/aws-ebs` |
| GCP | `kubernetes.io/gce-pd` |
| Azure | `kubernetes.io/azure-disk` |
| NFS | `nfs-subdir-external-provisioner` |
| 本地 | `kubernetes.io/no-provisioner` |

### 访问模式

| 模式 | 缩写 | 说明 |
|------|------|------|
| ReadWriteOnce | RWO | 单节点读写 |
| ReadOnlyMany | ROX | 多节点只读 |
| ReadWriteMany | RWX | 多节点读写 |
| ReadWriteOncePod | RWOP | 单 Pod 独占（K8s 1.22+） |

### 回收策略

| 策略 | 说明 |
|------|------|
| Retain | 保留数据，需要手动清理 |
| Delete | 自动删除存储资源 |
| Recycle | 删除数据，保留 PV（已废弃） |

### 常用 kubectl 命令

```bash
# 查看 PV
kubectl get persistentvolume
kubectl get pv

# 查看 PVC
kubectl get persistentvolumeclaim
kubectl get pvc

# 查看 StorageClass
kubectl get storageclass
kubectl get sc

# 创建 PVC
kubectl apply -f pvc.yaml

# 绑定状态
kubectl get pvc

# 删除 PVC（会触发 PV 回收）
kubectl delete pvc my-pvc

# 查看 PV 详情
kubectl describe pv my-pv

# 扩容 PVC（需要 StorageClass 支持）
kubectl patch pvc my-pvc -p '{"spec":{"resources":{"requests":{"storage":"15Gi"}}}}'
```

### 最佳实践

1. **使用 StorageClass 动态提供存储**
2. **生产环境使用 `ReadWriteOnce` 或 `ReadWriteOncePod`**
3. **设置 `volumeBindingMode: WaitForFirstConsumer`** 延迟绑定到最优节点
4. **重要数据设置 `reclaimPolicy: Retain`**，防止意外删除
5. **监控存储使用量**，避免存储耗尽
6. **为 StatefulSet 使用 PVC Template**，确保每个 Pod 有独立存储
7. **NFS/共享存储注意并发访问控制**

---

## 总结

本章详细介绍了 Kubernetes 的核心资源对象：

### 工作负载层
- **Pod**：最小调度单元，理解生命周期、健康检查、资源限制
- **ReplicaSet**：副本控制，通常由 Deployment 管理
- **Deployment**：无状态应用部署，支持滚动更新和回滚
- **StatefulSet**：有状态应用部署，提供稳定标识和独立存储
- **DaemonSet**：节点级别守护进程，每个节点运行一个 Pod
- **Job/CronJob**：批处理任务，一次性和周期性任务

### 服务发现层
- **Service**：集群内部负载均衡和服务发现
- **Ingress**：HTTP/HTTPS 外部访问路由

### 配置与存储层
- **ConfigMap**：非敏感配置存储
- **Secret**：敏感数据存储
- **Volume**：临时和持久存储卷
- **PersistentVolume**：集群级存储资源

### 元数据
- **Label/Selector**：资源组织和筛选
- **Annotation**：非标识性元数据

理解这些核心资源对象是掌握 Kubernetes 的基础。在实际应用中，需要根据业务场景选择合适的资源类型，并遵循最佳实践进行配置和管理。

---

> 上一章：[第一章：Kubernetes 概述与集群架构](./chapter01-kubernetes-overview.md)
>
> 下一章：[第三章：Kubernetes 网络与通信](./chapter03-kubernetes-networking.md)（待续）
