# Kubernetes 教程第六章：Kubernetes 调度与资源管理

---

## 1. kube-scheduler 调度流程概述

### 1.1 调度器简介

`kube-scheduler` 是 Kubernetes 集群的默认调度器，负责为新创建的 Pod 选择最合适的节点运行。

### 1.2 调度流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                     Pod 创建请求                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    1. 资源请求解析                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    2. 预选阶段 (Predicates)                       │
│              (过滤掉不满足条件的节点)                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    3. 优选阶段 (Priorities)                      │
│              (对可用节点进行打分排序)                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    4. 选择阶段 (Select)                          │
│              (选择得分最高的节点)                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    5. 绑定执行                                    │
│              (将 Pod 绑定到目标节点)                               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 资源请求与限制

### 2.1 基本概念

- **Requests（请求）**：容器需要的最小资源量，调度器根据此值决定将 Pod 调度到哪个节点
- **Limits（限制）**：容器可以使用的最大资源量，超过此值会被限制或终止

### 2.2 配置示例

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: resource-demo
spec:
  containers:
  - name: app
    image: nginx:1.21
    resources:
      requests:
        memory: "128Mi"
        cpu: "250m"
      limits:
        memory: "256Mi"
        cpu: "500m"
```

---

## 3. 调度器工作原理

### 3.1 预选阶段 (Predicates)

| 策略名称 | 功能 |
|---------|------|
| `PodFitsResources` | 检查节点有足够的资源 |
| `PodFitsHostPorts` | 检查所需的端口未被占用 |
| `HostName` | 检查 Pod 是否指定了特定节点 |
| `MatchNodeSelector` | 检查节点标签匹配 |
| `PodFitsTaints` | 检查 Pod 是否容忍节点的污点 |

### 3.2 优选阶段 (Priorities)

| 策略名称 | 功能 |
|---------|------|
| `SelectorSpreadPriority` | 同一 Service 的 Pod 分散到不同节点 |
| `InterPodAffinityPriority` | Pod 亲和性规则 |
| `LeastRequestedPriority` | 优先选择资源使用率低的节点 |
| `ImageLocalityPriority` | 优先选择有所需镜像的节点 |

---

## 4. 节点亲和性与反亲和性

### 4.1 nodeSelector

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-pod
spec:
  nodeSelector:
    disktype: ssd
    region: east-china-1
  containers:
  - name: nginx
    image: nginx:1.21
```

### 4.2 节点亲和性

**硬亲和性（必须满足）：**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: required-affinity-pod
spec:
  affinity:
    nodeAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
        nodeSelectorTerms:
        - matchExpressions:
          - key: topology.kubernetes.io/zone
            operator: In
            values:
            - zone-a
```

**软亲和性（尽量满足）：**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: preferred-affinity-pod
spec:
  affinity:
    nodeAffinity:
      preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 1
        preference:
          matchExpressions:
          - key: disktype
            operator: In
            values:
            - ssd
      - weight: 2
        preference:
          matchExpressions:
          - key: gpu
            operator: Exists
```

### 4.3 操作符说明

| 操作符 | 说明 |
|--------|------|
| `In` | 标签值在列表中 |
| `NotIn` | 标签值不在列表中 |
| `Exists` | 标签存在 |
| `DoesNotExist` | 标签不存在 |
| `Gt` | 标签值大于 |
| `Lt` | 标签值小于 |

---

## 5. Pod 亲和性与反亲和性

### 5.1 Pod 硬亲和性

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: web-server
spec:
  affinity:
    podAffinity:
      requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
          - key: app
            operator: In
            values:
            - database
        topologyKey: topology.kubernetes.io/zone
  containers:
  - name: web-server
    image: nginx:1.21
```

### 5.2 Pod 反亲和性（多副本分散）

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web-cluster
spec:
  replicas: 3
  selector:
    matchLabels:
      app: web
  template:
    spec:
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - web
            topologyKey: topology.kubernetes.io/zone
      containers:
      - name: nginx
        image: nginx:1.21
```

---

## 6. 污点与容忍

### 6.1 基本概念

**污点 (Taints)** 是节点的属性，用于拒绝 Pod 调度到该节点，除非 Pod 具有相应的**容忍 (Tolerations)**。

### 6.2 污点效果

| 效果 | 说明 |
|------|------|
| `NoSchedule` | 不会调度新 Pod |
| `PreferNoSchedule` | 尽量不调度 |
| `NoExecute` | 不会调度，且会驱逐现有 Pod |

### 6.3 为节点添加污点

```bash
# 添加污点
kubectl taint nodes node-gpu-1 nvidia.com/gpu=present:NoSchedule

# 添加维护污点
kubectl taint nodes node-1 node.kubernetes.io/unschedulable:NoSchedule:NoExecute

# 删除污点
kubectl taint nodes node-1 nvidia.com/gpu-
```

### 6.4 Pod 容忍配置

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: gpu-pod
spec:
  containers:
  - name: cuda-app
    image: nvidia/cuda:11.0
  tolerations:
  - key: "nvidia.com/gpu"
    operator: "Exists"
    effect: "NoSchedule"
```

**NoExecute 容忍时间：**

```yaml
tolerations:
- key: "node.kubernetes.io/unreachable"
  operator: "Exists"
  effect: "NoExecute"
  tolerationSeconds: 60  # 容忍 60 秒后才会被驱逐
```

---

## 7. 优先级和抢占式调度

### 7.1 创建 PriorityClass

```yaml
apiVersion: scheduling.k8s.io/v1
kind: PriorityClass
metadata:
  name: high-priority
value: 10000
globalDefault: false
description: "生产环境高优先级服务"
```

### 7.2 Pod 使用优先级

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: critical-app
spec:
  priorityClassName: high-priority
  containers:
  - name: app
    image: nginx:1.21
```

### 7.3 抢占机制

当高优先级 Pod 无法调度时：
1. 调度器尝试找到可调度节点
2. 如果没有合适节点，触发抢占逻辑
3. 选择一个已调度的低优先级 Pod 驱逐
4. 将高优先级 Pod 调度到该节点

### 7.4 非抢占式 Pod

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: non-preemptible-pod
spec:
  priorityClassName: high-priority
  preemptionPolicy: Never
```

---

## 8. 资源配额与限制范围

### 8.1 ResourceQuota

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute-resources
  namespace: production
spec:
  hard:
    requests.cpu: "20"
    requests.memory: 100Gi
    limits.cpu: "40"
    limits.memory: 200Gi
    pods: "100"
```

### 8.2 LimitRange

```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: default-limits
  namespace: production
spec:
  limits:
  - type: Container
    max:
      cpu: "2"
      memory: "2Gi"
    min:
      cpu: "50m"
      memory: "64Mi"
    default:
      cpu: "200m"
      memory: "256Mi"
    defaultRequest:
      cpu: "100m"
      memory: "128Mi"
```

---

## 9. 多租户场景下的调度策略

### 9.1 命名空间隔离

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: tenant-a
  labels:
    tenant: a
---
apiVersion: v1
kind: Namespace
metadata:
  name: tenant-b
  labels:
    tenant: b
```

### 9.2 租户资源配额

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: tenant-a-quota
  namespace: tenant-a
spec:
  hard:
    requests.cpu: "20"
    requests.memory: "40Gi"
    pods: "100"
```

### 9.3 节点亲和性隔离

```bash
# 标记专用节点
kubectl taint nodes node-dedicated-1 dedicated=tenant-a:NoSchedule
```

---

## 总结

| 主题 | 核心要点 |
|------|----------|
| kube-scheduler | 默认调度器，负责 Pod 节点选择 |
| 资源请求/限制 | Requests 调度依据，Limits 运行上限 |
| 预选 (Predicates) | 过滤不满足条件的节点 |
| 优选 (Priorities) | 对可用节点打分排序 |
| 节点亲和性 | 控制 Pod 与节点的关系 |
| Pod 亲和性 | 控制 Pod 之间的关系 |
| 污点/容忍 | 节点的排斥机制 |
| 优先级/抢占 | 高优先级 Pod 可抢占资源 |
| ResourceQuota | 限制命名空间资源总量 |
| LimitRange | 限制单个容器资源 |

---

**第六章完**