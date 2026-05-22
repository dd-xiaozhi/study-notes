# Kubernetes 教程第三章：Kubernetes 网络与通信

---

## 3.1 Kubernetes 网络模型概述

### 3.1.1 CNI 规范基础

Kubernetes 采用 **CNI（Container Network Interface）** 作为容器网络的抽象规范。CNI 定义了一套标准接口，让 Kubernetes 能够与各种网络插件协同工作。

**CNI 的核心职责：**
- 容器网络命名空间管理
- 网络插件调用顺序
- IP 地址分配
- 网络策略应用

**CNI 工作流程：**

```
┌─────────────────────────────────────────────────────────────────┐
│                      Kubernetes API Server                       │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Kubelet (每个 Node 上)                      │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────────┐│
│  │ CNI Plugin   │◄──│ Network      │◄──│ Pod 创建/删除事件     ││
│  │ (Calico等)   │   │ Namespace    │   │                      ││
│  └──────────────┘   └──────────────┘   └──────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                     容器网络命名空间                              │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ eth0@vethXXX ──► vethXXX@ifXX (宿主机) ──► 网桥/物理网卡    ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 3.1.2 Kubernetes 网络设计原则

Kubernetes 遵循以下核心网络原则：

1. **每个 Pod 拥有唯一 IP 地址** - Pod 被视为虚拟机器，IP 不共享
2. **Pod 间通信无需 NAT** - 同一 Pod 内的容器共享网络命名空间
3. **Node 与 Pod 通信无需 NAT** - 容器外的世界能看到真实 IP
4. **Service 是一组 Pod 的抽象** - 稳定的虚拟 IP，负载均衡到后端 Pod

---

## 3.2 三层网络架构详解

### 3.2.1 Pod 网络

**Pod 网络是容器级别的虚拟网络，每个 Pod 拥有独立的网络命名空间。**

**Pod 网络特点：**
- Pod 内所有容器共享同一 IP（Pause 容器共享）
- 同一 Pod 内的容器通过 localhost 通信
- 不同 Pod 之间通过 Pod IP 直接通信

**Pod 网络配置示例：**

```yaml
# Pod 网络配置示例
apiVersion: v1
kind: Pod
metadata:
  name: network-demo-pod
  labels:
    app: demo
spec:
  containers:
  - name: nginx
    image: nginx:1.24
    ports:
    - containerPort: 80
```

```bash
# 查看 Pod IP
kubectl get pods -o wide

# NAME           READY   STATUS    IP           NODE
# network-demo   1/1     Running   10.1.1.5     node-1
```

### 3.2.2 Service 网络

**Service 是 Kubernetes 的服务发现机制，提供稳定的虚拟 IP（ClusterIP）。**

**Service 网络特点：**
- ClusterIP 是虚拟 IP，不存在于网络接口
- kube-proxy 通过 iptables/ipvs 规则转发流量
- 支持会话亲和性（Session Affinity）
- 自动负载均衡到后端 Pod

### 3.2.3 Node 网络

**Node 网络是宿主机层面的物理/虚拟网络，用于节点间通信和管理流量。**

**Node 网络职责：**
- 节点间 Pod 流量传输
- kubelet 与 API Server 通信
- 外部访问 NodePort/LoadBalancer 服务
- 存储和数据平面流量

---

## 3.3 Kubernetes DNS 原理

### 3.3.1 DNS 架构概述

Kubernetes 使用 **CoreDNS** 作为集群内部的 DNS 服务。

**DNS 域名规则：**
- Service: `<service-name>.<namespace>.svc.cluster.local`
- Pod: `<pod-ip>.<namespace>.pod.cluster.local` (IP 中点替换为破折号)
- 默认域名后缀可简化: `<service-name>.<namespace>`

### 3.3.2 CoreDNS 部署与配置

```yaml
# CoreDNS 配置示例 (ConfigMap)
apiVersion: v1
kind: ConfigMap
metadata:
  name: coredns
  namespace: kube-system
data:
  Corefile: |
    .:53 {
        errors
        health {
           lameduck 5s
        }
        ready
        kubernetes cluster.local in-addr.arpa ip6.arpa {
           pods insecure
           fallthrough in-addr.arpa ip6.arpa
           ttl 30
        }
        prometheus :9153
        forward . /etc/resolv.conf {
           max_concurrent 1000
        }
        cache 30
        loop
        reload
        loadbalance
    }
```

### 3.3.3 DNS 查询示例

```bash
# 进入一个测试 Pod
kubectl run dns-test --image=busybox:1.28 -- sleep 3600

# 测试 Service DNS 解析
kubectl exec -it dns-test -- nslookup kubernetes

# 测试 Pod DNS 解析
# Pod IP 10.1.2.3 会被解析为 10-1-2-3.default.pod.cluster.local
```

---

## 3.4 Service 类型详解

### 3.4.1 ClusterIP Service

**ClusterIP 是默认的 Service 类型，仅在集群内部可访问。**

```yaml
# ClusterIP Service 定义
apiVersion: v1
kind: Service
metadata:
  name: backend-service
  namespace: production
spec:
  type: ClusterIP
  selector:
    app: backend
    tier: api
  ports:
  - name: http
    protocol: TCP
    port: 80        # Service 端口
    targetPort: 8080 # 后端 Pod 端口
```

### 3.4.2 NodePort Service

**NodePort 通过节点端口暴露服务，在每个节点上监听一个端口（30000-32767）。**

```yaml
# NodePort Service 定义
apiVersion: v1
kind: Service
metadata:
  name: web-nodeport-service
spec:
  type: NodePort
  selector:
    app: web
  ports:
  - name: http
    protocol: TCP
    port: 80           # Service 内部端口
    targetPort: 8080   # 后端 Pod 端口
    nodePort: 30080    # 节点端口（可选）
```

### 3.4.3 LoadBalancer Service

**LoadBalancer 将服务暴露给外部云负载均衡器。**

```yaml
# LoadBalancer Service 定义
apiVersion: v1
kind: Service
metadata:
  name: web-loadbalancer
spec:
  type: LoadBalancer
  selector:
    app: web
  ports:
  - name: http
    port: 80
    targetPort: 8080
```

### 3.4.4 ExternalName Service

**ExternalName 将 Service 映射到外部域名。**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: external-database
  namespace: production
spec:
  type: ExternalName
  externalName: mysql.prod.example.com
```

### 3.4.5 Headless Service

**ClusterIP 设置为 None 时是 Headless Service，DNS 直接返回 Pod IP。**

```yaml
apiVersion: v1
kind: Service
metadata:
  name: statefulset-headless
spec:
  type: ClusterIP
  clusterIP: None  # Headless 关键配置
  selector:
    app: nginx
  ports:
  - port: 80
    targetPort: 80
```

---

## 3.5 Ingress 网络

### 3.5.1 Ingress 概念

**Ingress 是 HTTP/HTTPS 路由的 Kubernetes 资源对象，提供基于域名和路径的七层路由。**

### 3.5.2 Ingress 资源定义

```yaml
# 基本 Ingress 定义
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: web-ingress
  namespace: production
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  ingressClassName: nginx
  rules:
  - host: web.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: web-frontend
            port:
              number: 80
  tls:
  - hosts:
    - web.example.com
    secretName: web-tls-secret
```

### 3.5.3 Ingress Controller

**Nginx Ingress Controller 部署：**

```yaml
# Nginx Ingress Controller 部署
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: nginx
  annotations:
    ingressclass.kubernetes.io/is-default-class: "true"
spec:
  controller: k8s.io/ingress-nginx
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ingress-nginx-controller
  namespace: ingress-nginx
spec:
  replicas: 2
  selector:
    matchLabels:
      app: ingress-nginx
  template:
    metadata:
      labels:
        app: ingress-nginx
    spec:
      containers:
      - name: controller
        image: registry.k8s.io/ingress-nginx/controller:v1.9.4
        args:
          - /nginx-ingress-controller
          - --publish-service=$(POD_NAMESPACE)/ingress-nginx-controller
          - --ingress-class=nginx
        ports:
        - name: http
          containerPort: 80
        - name: https
          containerPort: 443
```

---

## 3.6 NetworkPolicy

### 3.6.1 NetworkPolicy 概念

**NetworkPolicy 是 Kubernetes 的网络隔离机制，定义 Pod 之间的流量规则。**

### 3.6.2 NetworkPolicy 资源定义

```yaml
# 基础 NetworkPolicy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: api-network-policy
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: api
      tier: backend
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          tier: frontend
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: kube-system
    ports:
    - protocol: UDP
      port: 53
```

---

## 3.7 Pod 间通信机制

### 3.7.1 同一节点上 Pod 通信

同一节点上的 Pod 通过虚拟以太网设备（veth pair）连接到网桥进行通信。

### 3.7.2 不同节点上 Pod 通信

跨节点 Pod 通信依赖 CNI 插件实现：
- **隧道模式（VxLAN）**：Flannel 使用
- **路由模式（BGP）**：Calico 使用

---

## 3.8 常见 CNI 插件介绍

### 3.8.1 Calico

**Calico 是基于 BGP 的高性能网络插件，支持网络策略和加密。**

```yaml
# Calico Operator 安装
apiVersion: operator.tigera.io/v1
kind: Installation
metadata:
  name: default
spec:
  calicoNetwork:
    ipPools:
    - name: default-ipv4-ippool
      cidr: 10.1.0.0/16
      natOutgoing: Enabled
```

### 3.8.2 Flannel

**Flannel 是 CoreOS 开发的简单高效 CNI 插件，使用 VxLAN 或 UDP 封装。**

```yaml
# Flannel 部署
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: kube-flannel-ds
  namespace: kube-flannel
spec:
  selector:
    matchLabels:
      app: flannel
  template:
    spec:
      containers:
      - name: flanneld
        image: quay.io/coreos/flannel:v0.21.5
        args:
        - --ip-masq
        - --kube-subnet-mgr
        env:
        - name: POD_CIDR
          value: 10.244.0.0/16
```

### 3.8.3 Weave Net

**Weave Net 支持自动发现和加密。**

```bash
# 一键部署
kubectl apply -f "https://cloud.weave.works/k8s/net?k8s-version=$(kubectl version | base64 | tr -d '\n')"
```

### 3.8.4 Cilium

**Cilium 是基于 eBPF 的高性能网络插件，提供 HTTP/L7 层可见性和安全策略。**

```yaml
# Cilium 配置示例
apiVersion: v1
kind: ConfigMap
metadata:
  name: cilium-config
  namespace: kube-system
data:
  ipv4-native-routing-cidr: 10.1.0.0/16
  enable-wireguard: "true"
  enable-l7-proxy: "true"
```

**CiliumNetworkPolicy（L7 策略）：**

```yaml
apiVersion: cilium.io/v2
kind: CiliumNetworkPolicy
metadata:
  name: api-l7-policy
spec:
  endpointSelector:
    matchLabels:
      app: api
  ingress:
  - fromEndpoints:
    - matchLabels:
        app: frontend
    toPorts:
    - ports:
      - port: "8080"
        protocol: TCP
      rules:
        http:
        - method: GET
          path: /api/v1.*
```

---

## 3.9 总结与最佳实践

### 网络选型建议

| 场景 | 推荐 CNI | 原因 |
|------|---------|------|
| 裸金属服务器 | Calico | BGP 路由，高性能 |
| 云环境 | Calico/Flannel | 云厂商集成好 |
| 安全敏感 | Cilium | eBPF + 加密 |
| 简单部署 | Flannel | 轻量易用 |
| 服务网格 | Cilium | eBPF + Hubble |

### 网络安全建议

1. **启用 NetworkPolicy** - 默认拒绝，按需开放
2. **使用 Cilium/Calico 加密** - Pod 间通信加密
3. **Ingress TLS** - 外部 HTTPS 终止
4. **Service 端口最小化** - 只暴露必要端口
5. **定期审计 DNS** - CoreDNS 访问控制

---

**第三章完**