# Kubernetes 教程第五章：Kubernetes 安全与认证

## 目录

1. [Kubernetes 安全模型概述](#1-kubernetes-安全模型概述)
2. [API Server 认证机制](#2-api-server-认证机制)
3. [授权机制（RBAC）](#3-授权机制rbac)
4. [Admission Controller（准入控制）](#4-admission-controller准入控制)
5. [Pod 安全策略](#5-pod-安全策略)
6. [Secret 加密存储和管理](#6-secret-加密存储和管理)
7. [网络安全策略（NetworkPolicy）](#7-网络安全策略networkpolicy)
8. [安全配置最佳实践](#8-安全配置最佳实践)

---

## 1. Kubernetes 安全模型

### 1.1 零信任安全模型

Kubernetes 采用**零信任安全模型**（Zero Trust Security），其核心原则是：

- **永不信任，始终验证**：无论请求来自内部还是外部，都需要进行身份验证和授权
- **最小权限原则**：用户和服务仅获得完成任务所需的最小权限
- **纵深防御**：多层安全防护，任何单一安全措施都不足以完全保护系统

### 1.2 Kubernetes 安全分层

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes 安全分层                        │
├─────────────────────────────────────────────────────────────┤
│  第7层 │  网络安全策略 (NetworkPolicy)                          │
├─────────────────────────────────────────────────────────────┤
│  第6层 │  Pod 安全策略 (PSP/PSS)                               │
├─────────────────────────────────────────────────────────────┤
│  第5层 │  Admission Controller (准入控制)                      │
├─────────────────────────────────────────────────────────────┤
│  第4层 │  RBAC (基于角色的访问控制)                             │
├─────────────────────────────────────────────────────────────┤
│  第3层 │  认证 (Authentication)                               │
├─────────────────────────────────────────────────────────────┤
│  第2层 │  API Server 网络暴露控制                              │
├─────────────────────────────────────────────────────────────┤
│  第1层 │  基础设施安全 (etcd, 节点安全)                          │
└─────────────────────────────────────────────────────────────┘
```

### 1.3 Kubernetes 安全关键组件

| 组件 | 作用 | 优先级 |
|------|------|--------|
| etcd | 存储所有集群数据，必须加密和访问控制 | 最高 |
| API Server | 集群统一入口，所有请求都经过它 | 最高 |
| Kubelet | 管理节点上的 Pod，需要安全配置 | 高 |
| etcd | 存储集群所有状态数据 | 最高 |

### 1.4 安全请求流程

```
Client Request
      │
      ▼
┌─────────────┐
│  认证阶段    │ ──→ 验证客户端身份 (证书/Token/OIDC)
│(Authentication)│
└─────────────┘
      │ 认证失败 → 401 Unauthorized
      ▼ 认证成功
┌─────────────┐
│  授权阶段    │ ──→ 检查客户端权限 (RBAC)
│(Authorization) │
└─────────────┘
      │ 授权失败 → 403 Forbidden
      ▼ 授权成功
┌─────────────┐
│  准入控制    │ ──→ 篡改请求内容或拒绝请求
│(Admission)   │
└─────────────┘
      │ 拒绝 → 400/500 错误
      ▼ 通过
┌─────────────┐
│   API Server │ ──→ 处理请求
│  处理请求    │
└─────────────┘
```

---

## 2. API Server 认证机制

Kubernetes API Server 支持多种认证方式，可以同时启用多种认证器。

### 2.1 证书认证（x509）

x509 证书认证是 Kubernetes 最常用、最安全的认证方式。

#### 2.1.1 证书认证原理

```
┌─────────────────────────────────────────────────────────────┐
│                    x509 证书认证流程                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   Client Certificate                                        │
│   (client.crt)                                              │
│        │                                                    │
│        │  包含:                                              │
│        │  - CN (Common Name) → 用户名                        │
│        │  - O (Organization) → 组                           │
│        │  - 公钥                                             │
│        ▼                                                    │
│   API Server 验证:                                          │
│   1. 证书是否由受信任 CA 签发                                 │
│   2. 证书是否在有效期内                                       │
│   3. 证书是否被吊销                                           │
│   4. 提取 CN/O 作为用户身份                                  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 2.1.2 证书认证配置

API Server 启动参数中指定 CA 证书：

```yaml
# kube-apiserver.yaml
apiVersion: v1
kind: Pod
metadata:
  name: kube-apiserver
spec:
  containers:
  - name: kube-apiserver
    command:
    - kube-apiserver
    - --tls-cert-file=/etc/kubernetes/pki/apiserver.crt    # 服务器证书
    - --tls-private-key-file=/etc/kubernetes/pki/apiserver.key
    - --client-ca-file=/etc/kubernetes/pki/ca.crt          # 客户端 CA 证书
    - --service-account-key-file=/etc/kubernetes/pki/sa.pub # ServiceAccount 签名密钥
    - --service-account-issuer=kubernetes.default.svc
```

#### 2.1.3 生成用户证书示例

```bash
# 1. 创建私钥
openssl genrsa -out client.key 2048

# 2. 创建证书签名请求 (CSR)
openssl req -new -key client.key -out client.csr -subj "/CN=admin/O=system:masters"

# 3. 使用 Kubernetes CA 签发证书
# 方法一：使用 kubeadm 生成的 CA
openssl x509 -req -in client.csr \
  -CA /etc/kubernetes/pki/ca.crt \
  -CAkey /etc/kubernetes/pki/ca.key \
  -CAcreateserial \
  -out client.crt \
  -days 365

# 4. 验证证书
openssl x509 -in client.crt -text -noout
```

#### 2.1.4 使用证书配置 Kubernetes Context

```bash
# 配置集群凭据
kubectl config set-credentials admin \
  --client-certificate=client.crt \
  --client-key=client.key

# 配置集群信息
kubectl config set-cluster kubernetes \
  --server=https://192.168.1.100:6443 \
  --certificate-authority=ca.crt

# 创建 context
kubectl config set-context admin@kubernetes \
  --cluster=kubernetes \
  --user=admin

# 使用 context
kubectl config use-context admin@kubernetes
```

### 2.2 Token 认证

#### 2.2.1 ServiceAccount Token

ServiceAccount 是 Kubernetes 内部用于 Pod 访问 API Server 的认证方式。

```yaml
# 创建一个 ServiceAccount
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-app
  namespace: default
---
# 自动创建对应的 Secret（包含 token）
apiVersion: v1
kind: Secret
metadata:
  name: my-app-token-xxx
  annotations:
    kubernetes.io/service-account.name: my-app
type: kubernetes.io/service-account-token
data:
  # token 是 base64 编码的 JWT
```

#### 2.2.2 手动创建 ServiceAccount Token（Bound Token）

```yaml
# 创建 ServiceAccount
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-app
  namespace: default
---
# 创建 Token Request
apiVersion: authentication.k8s.io/v1
kind: TokenRequest
metadata:
  name: my-app
  namespace: default
spec:
  audiences:
  - https://kubernetes.default.svc
  expirationSeconds: 3600
  boundObjectRef:
    apiVersion: v1
    kind: Pod
    name: my-app-pod
    uid: <pod-uid>
```

#### 2.2.3 Bootstrap Token

用于新节点加入集群时的初始认证。

```yaml
# 在 kube-system 命名空间创建 bootstrap token secret
apiVersion: v1
kind: Secret
metadata:
  name: bootstrap-token-07401b
  namespace: kube-system
type: bootstrap.kubernetes.io/token
data:
  # token ID (前缀是 token ID)
  token-id: "MDcwMDFi"
  # token secret (实际 secret)
  token-secret: " ZXhoYW9wdG9rZW5zZWNyZXQ="
  # 用途描述
  description: "default bootstrap token"
  # 允许的用途
  usage-bootstrap-authentication: "true"
  usage-bootstrap-signing: "true"
  # 过期时间（Unix 时间戳）
  expiration: "2027-01-01T00:00:00Z"
```

API Server 配置启用 Bootstrap Token 认证器：

```yaml
# kube-apiserver.yaml
- --enable-bootstrap-token-auth=true
```

#### 2.2.4 使用 Token 访问 API

```bash
# 获取 token
TOKEN=$(kubectl get secret my-app-token-xxx -n default -o jsonpath='{.data.token}' | base64 -d)

# 使用 token 访问 API
curl -k -H "Authorization: Bearer $TOKEN" https://192.168.1.100:6443/api/v1/namespaces/default/pods
```

### 2.3 OIDC 认证

OIDC（OpenID Connect）是基于 OAuth 2.0 的身份层协议，提供了一种标准化的方式来验证用户身份。

#### 2.3.1 OIDC 认证原理

```
┌─────────────────────────────────────────────────────────────┐
│                    OIDC 认证流程                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. 用户登录 IdP (如 Dex, Keycloak, Auth0)                    │
│     IdP 返回 ID Token (JWT)                                 │
│                                                             │
│  2. kubectl 使用 ID Token 访问 API Server                    │
│     curl -H "Authorization: Bearer <id_token>"               │
│                                                             │
│  3. API Server 验证 Token:                                   │
│     - 验证签名 (使用 IdP 公钥)                                 │
│     - 验证 claims (iss, aud, exp)                            │
│     - 提取用户信息                                            │
│                                                             │
│  4. API Server 使用 TokenReview API 验证                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 2.3.2 OIDC 配置示例

```yaml
# kube-apiserver.yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: kube-apiserver
    command:
    - kube-apiserver
    # OIDC 认证配置
    - --oidc-issuer-url=https://auth.example.com
    - --oidc-client-id=kubernetes
    - --oidc-ca-file=/etc/kubernetes/pki/oidc-ca.crt
    - --oidc-username-claim=email
    - --oidc-groups-claim=groups
```

#### 2.3.3 kubectl 配置 OIDC

```yaml
# ~/.kube/config
apiVersion: v1
kind: Config
clusters:
- name: kubernetes
  cluster:
    server: https://192.168.1.100:6443
    certificate-authority: /path/to/ca.crt
users:
- name: oidc-user
  user:
    exec:
      apiVersion: client.authentication.k8s.io/v1
      command: kubectl-oidc
      args:
      - get-token
      - --issuer=https://auth.example.com
      - --client-id=kubernetes
      - --client-secret=<client-secret>
      interactiveMode: IfAvailable
contexts:
- name: oidc-context
  context:
    cluster: kubernetes
    user: oidc-user
current-context: oidc-context
```

---

## 3. 授权机制（RBAC）

RBAC（Role-Based Access Control）是 Kubernetes 最常用的授权方式。

### 3.1 RBAC 核心概念

```
┌─────────────────────────────────────────────────────────────┐
│                    RBAC 核心概念                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Role/ClusterRole: 定义一组权限                               │
│  ┌─────────────────────────────────────────┐                │
│  │  rules:                                 │                │
│  │    - apiGroups: [""]                    │                │
│  │      resources: ["pods"]               │                │
│  │      verbs: ["get", "list", "watch"]   │                │
│  └─────────────────────────────────────────┘                │
│                                                             │
│  RoleBinding/ClusterRoleBinding: 将权限绑定到主体             │
│  ┌─────────────────────────────────────────┐                │
│  │  subjects:                              │                │
│  │    - kind: User                         │                │
│  │      name: john@example.com            │                │
│  │  roleRef:                               │                │
│  │    kind: Role                           │                │
│  │    name: pod-reader                     │                │
│  └─────────────────────────────────────────┘                │
│                                                             │
│  主体类型 (Subject):                                         │
│  - User (用户)                                               │
│  - Group (用户组)                                            │
│  - ServiceAccount (服务账号)                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Role 和 ClusterRole

#### 3.2.1 Role（命名空间级别）

```yaml
# role-read-pods.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-reader
  namespace: default          # Role 属于特定命名空间
rules:
# 规则1：读取 pods
- apiGroups: [""]            # "" 表示 core API Group
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
# 规则2：读取 pod 日志
- apiGroups: [""]
  resources: ["pods/log"]
  verbs: ["get"]
# 规则3：读取特定 pod
- apiGroups: [""]
  resources: ["pods/exec"]
  verbs: ["create"]
```

#### 3.2.2 ClusterRole（集群级别）

```yaml
# clusterrole-nodes-reader.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: node-reader
rules:
# 读取节点资源（集群级别）
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list", "watch"]
# 读取 PVC（集群级别）
- apiGroups: [""]
  resources: ["persistentvolumeclaims"]
  verbs: ["get", "list", "watch"]
# 读取特定 CRD
- apiGroups: ["apiextensions.k8s.io"]
  resources: ["customresourcedefinitions"]
  verbs: ["get", "list", "watch"]
```

#### 3.2.3 常用 verbs 说明

| Verb | 含义 | 对应 HTTP 方法 |
|------|------|----------------|
| get | 读取单个资源 | GET |
| list | 列出资源列表 | GET |
| watch | 监听资源变化 | GET (WebSocket) |
| create | 创建资源 | POST |
| update | 更新资源 | PUT |
| patch | 部分更新资源 | PATCH |
| delete | 删除资源 | DELETE |
| deletecollection | 删除资源集合 | DELETE |

#### 3.2.4 常用 apiGroups

| apiGroup | 说明 |
|-----------|------|
| "" | Core API Group（v1） |
| apps | Apps API Group（deployments, statefulsets等） |
| batch | Batch API Group（jobs, cronjobs） |
| networking.k8s.io | 网络相关（NetworkPolicy, Ingress等） |
| rbac.authorization.k8s.io | RBAC 资源 |
| storage.k8s.io | 存储类 |
| policy | PodSecurityPolicy |

### 3.3 RoleBinding 和 ClusterRoleBinding

#### 3.3.1 RoleBinding（绑定 Role 到命名空间内的主体）

```yaml
# rolebinding-read-pods.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: read-pods-binding
  namespace: default
subjects:                          # 绑定的主体
- kind: User
  name: john@example.com
  apiGroup: rbac.authorization.k8s.io
- kind: Group
  name: developers
  apiGroup: rbac.authorization.k8s.io
- kind: ServiceAccount
  name: my-app
  namespace: default
roleRef:                           # 引用的 Role
  kind: Role
  name: pod-reader
  apiGroup: rbac.authorization.k8s.io
```

#### 3.3.2 ClusterRoleBinding（绑定 ClusterRole 到集群范围内的主体）

```yaml
# clusterrolebinding-admin.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-binding
subjects:
- kind: User
  name: admin@example.com
  apiGroup: rbac.authorization.k8s.io
- kind: Group
  name: system:masters            # kubeconfig 中的超级用户组
  apiGroup: rbac.authorization.k8s.io
roleRef:
  kind: ClusterRole
  name: cluster-admin
  apiGroup: rbac.authorization.k8s.io
```

#### 3.3.3 RoleBinding 引用 ClusterRole

可以创建一个 RoleBinding 引用 ClusterRole，这样可以让主体在特定命名空间内拥有 ClusterRole 定义的权限。

```yaml
# rolebinding-cluster-reader.yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: read-all-pods-in-namespace
  namespace: monitoring
subjects:
- kind: ServiceAccount
  name: prometheus
  namespace: monitoring
roleRef:
  kind: ClusterRole
  name: pod-reader              # 这是一个 ClusterRole
  apiGroup: rbac.authorization.k8s.io
```

### 3.4 ServiceAccount 最佳实践

#### 3.4.1 为每个应用创建独立的 ServiceAccount

```yaml
# my-app-serviceaccount.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-app
  namespace: production
automountToken: false            # 关闭自动挂载 token，需要时手动挂载
```

#### 3.4.2 Pod 使用特定 ServiceAccount

```yaml
# my-app-pod.yaml
apiVersion: v1
kind: Pod
metadata:
  name: my-app
  namespace: production
spec:
  serviceAccountName: my-app      # 指定 ServiceAccount
  containers:
  - name: my-app
    image: my-app:v1
```

#### 3.4.3 完整的 RBAC 配置示例

```yaml
# 1. 创建 ServiceAccount
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: backend-app
  namespace: production
---
# 2. 创建 Role（只允许读取和更新 pods）
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: backend-pod-access
  namespace: production
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods/log"]
  verbs: ["get"]
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get", "update"]
  resourceNames: ["backend-config"]    # 只允许访问特定 configmap
---
# 3. 创建 RoleBinding
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: backend-pod-access-binding
  namespace: production
subjects:
- kind: ServiceAccount
  name: backend-app
  namespace: production
roleRef:
  kind: Role
  name: backend-pod-access
  apiGroup: rbac.authorization.k8s.io
```

### 3.5 常用内置 ClusterRole

Kubernetes 提供了一些内置的 ClusterRole：

| ClusterRole | 说明 |
|-------------|------|
| cluster-admin | 超级管理员，拥有所有权限 |
| admin | 命名空间管理员，拥有命名空间内所有权限 |
| edit | 命名空间编辑者，可以修改命名空间内大多数资源 |
| view | 命名空间查看者，只能读取资源 |

```bash
# 查看内置 ClusterRole
kubectl get clusterroles

# 查看 cluster-admin 的权限
kubectl describe clusterrole cluster-admin
```

---

## 4. Admission Controller（准入控制）

Admission Controller 是在请求经过认证和授权之后、处理之前的一层拦截器，可以修改请求或拒绝请求。

### 4.1 Admission Controller 类型

```
┌─────────────────────────────────────────────────────────────┐
│                 Admission Controller 类型                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  变更型 (Mutating):  修改请求对象后再继续处理                  │
│  ───────────────────────────────────────────────────         │
│  - AlwaysPullImages                                          │
│  - DefaultStorageClass                                       │
│  - MutatingAdmissionWebhook                                  │
│  - NamespaceAutoProvision                                    │
│  - ResourceQuota                                             │
│  - ...                                                       │
│                                                             │
│  验证型 (Validating):  验证请求对象，拒绝不合法请求            │
│  ───────────────────────────────────────────────────         │
│  - AlwaysDeny                                                │
│  - EventRateLimit                                            │
│  - NamespaceExists                                           │
│  - PersistentVolumeClaimResize                               │
│  - ValidatingAdmissionWebhook                                │
│  - ...                                                       │
│                                                             │
│  两型兼备:                                                   │
│  - AdmissionWebhook (通过 webhook 配置)                       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 启用 Admission Controller

```yaml
# kube-apiserver.yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: kube-apiserver
    command:
    - kube-apiserver
    - --enable-admission-plugins=\
      NodeRestriction,\
      AlwaysPullImages,\
      ResourceQuota,\
      LimitRanger,\
      PodSecurity,\
      ServiceAccountAdmission
```

### 4.3 常用 Admission Controller 详解

#### 4.3.1 AlwaysPullImages

强制所有镜像在启动时重新拉取，防止使用本地缓存的镜像。

```yaml
# kube-apiserver 启用
- --enable-admission-plugins=AlwaysPullImages
```

#### 4.3.2 ResourceQuota

限制命名空间内的资源使用量。

```yaml
# resource-quota.yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: production-quota
  namespace: production
spec:
  hard:
    requests.cpu: "10"
    requests.memory: 20Gi
    limits.cpu: "20"
    limits.memory: 40Gi
    pods: "100"
    services: "10"
    persistentvolumeclaims: "20"
```

#### 4.3.3 LimitRanger

为容器设置默认资源限制。

```yaml
# limit-range.yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: production-limits
  namespace: production
spec:
  limits:
  - type: Container
    default:
      cpu: 500m
      memory: 256Mi
    defaultRequest:
      cpu: 100m
      memory: 64Mi
    max:
      cpu: "4"
      memory: 8Gi
    min:
      cpu: 50m
      memory: 32Mi
  - type: PersistentVolumeClaim
    min: 1Gi
    max: 100Gi
```

### 4.4 Admission Webhook

#### 4.4.1 MutatingWebhookConfiguration 示例

```yaml
# mutating-webhook.yaml
apiVersion: admissionregistration.k8s.io/v1
kind: MutatingWebhookConfiguration
metadata:
  name: istio-sidecar-injector
webhooks:
- name: sidecar-injector.istio.io
  clientConfig:
    service:
      name: istiod
      namespace: istio-system
      path: "/inject"
    caBundle: <base64 encoded CA cert>
  rules:
  - operations: ["CREATE", "UPDATE"]
    apiGroups: [""]
    apiVersions: ["v1"]
    resources: ["pods"]
  namespaceSelector:
    matchLabels:
      istio-injection: enabled
  sideEffects: None
  admissionReviewVersions: ["v1", "v1beta1"]
  timeoutSeconds: 10
```

#### 4.4.2 ValidatingWebhookConfiguration 示例

```yaml
# validating-webhook.yaml
apiVersion: admissionregistration.k8s.io/v1
kind: ValidatingWebhookConfiguration
metadata:
  name: pod-policy.example.com
webhooks:
- name: pod-policy.example.com
  clientConfig:
    service:
      name: policy-controller
      namespace: kube-system
      path: "/validate-pod"
    caBundle: <base64 encoded CA cert>
  rules:
  - operations: ["CREATE", "UPDATE"]
    apiGroups: [""]
    apiVersions: ["v1"]
    resources: ["pods"]
  sideEffects: None
  admissionReviewVersions: ["v1", "v1beta1"]
  timeoutSeconds: 5
```

---

## 5. Pod 安全策略

Pod 安全策略（PodSecurityPolicy）已在 Kubernetes 1.21 废弃，被 Pod Security Standards（PSS）取代。

### 5.1 Pod Security Standards（PSS）

PSS 将 Pod 安全策略分为三个级别：

| 级别 | 说明 | 用途 |
|------|------|------|
| privileged | 完全不受限制 | 可信的工作负载 |
| baseline | 最少限制，禁止已知权限提升 | 非定制化工作负载 |
| restricted | 严格限制，遵循最佳实践 | 长期运行的生产工作负载 |

#### 5.1.1 使用 Namespace 标签设置 PSS

```yaml
# namespace-pss.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    # 选择 baseline 或 restricted 级别
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/enforce-version: latest
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/audit-version: latest
    pod-security.kubernetes.io/warn: restricted
    pod-security.kubernetes.io/warn-version: latest
```

### 5.2 PSP（已废弃）配置示例

如果你使用的是旧版本 Kubernetes，以下是 PSP 配置示例（仅供参考）：

```yaml
# pod-security-policy.yaml (已废弃，仅供学习)
apiVersion: policy/v1beta1
kind: PodSecurityPolicy
metadata:
  name: restricted
  annotations:
    seccomp.security.alpha.kubernetes.io/allowedProfileNames: 'runtime/default'
    apparmor.security.beta.kubernetes.io/allowedProfileNames: 'runtime/default'
    seccomp.security.alpha.kubernetes.io/defaultProfileName: 'runtime/default'
    apparmor.security.beta.kubernetes.io/defaultProfileName: 'runtime/default'
spec:
  privileged: false                  # 禁止 privileged 容器
  allowPrivilegeEscalation: false   # 禁止特权提升
  allowedCapabilities:               # 允许的 capabilities
  - default
  volumes:
  - 'configMap'
  - 'emptyDir'
  - 'projected'
  - 'secret'
  - 'downwardAPI'
  - 'persistentVolumeClaim'
  hostNetwork: false                # 禁止 host 网络
  hostIPC: false                    # 禁止 host IPC
  hostPID: false                    # 禁止 host PID
  runAsUser:
    rule: 'MustRunAsNonRoot'        # 必须非 root 运行
  seLinux:
    rule: 'RunAsAny'
  supplementalGroups:
    rule: 'RunAsAny'
  fsGroup:
    rule: 'RunAsAny'
```

### 5.3 Pod 安全上下文

在 Pod 或 Container 级别设置安全上下文：

```yaml
# secure-pod.yaml
apiVersion: v1
kind: Pod
metadata:
  name: secure-app
  namespace: production
spec:
  securityContext:                  # Pod 级别安全上下文
    runAsNonRoot: true
    runAsUser: 1000
    runAsGroup: 1000
    fsGroup: 1000
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: app
    image: nginx:1.21
    securityContext:               # Container 级别安全上下文
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      capabilities:
        drop:
        - ALL
        add:
        - NET_BIND_SERVICE
    resources:
      limits:
        memory: 128Mi
        cpu: 500m
      requests:
        memory: 64Mi
        cpu: 100m
    volumeMounts:
    - name: tmp
      mountPath: /tmp
  volumes:
  - name: tmp
    emptyDir: {}
```

---

## 6. Secret 加密存储和管理

### 6.1 Secret 概述

Secret 用于存储敏感信息，如密码、OAuth token、SSH key 等。Secret 有三种类型：

| 类型 | 说明 |
|------|------|
| Opaque | 通用 Secret，默认类型 |
| kubernetes.io/tls | TLS 证书 |
| kubernetes.io/service-account-token | ServiceAccount Token |
| kubernetes.io/dockerconfigjson | Docker 仓库认证信息 |

### 6.2 Secret 加密配置

#### 6.2.1 在 etcd 中加密 Secret

```yaml
# encryption-config.yaml
apiVersion: apiserver.config.k8s.io/v1
kind: EncryptionConfiguration
resources:
- resources:
  - secrets
  - configmaps
  providers:
  # 使用 AES-GCM 加密（推荐）
  - aesgcm:
      keys:
      - name: key1
        secret: <base64 encoded 32-byte key>
  # 使用 KMS 插件（生产环境推荐）
  - kms:
      name: my-kms-plugin
      endpoint: unix:///tmp/kms.socket
      timeout: 3s
  # 作为最后的后备（不加密，仅用于迁移）
  - identity: {}
```

#### 6.2.2 API Server 配置加密

```yaml
# kube-apiserver.yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: kube-apiserver
    command:
    - kube-apiserver
    - --encryption-provider-config=/etc/kubernetes/encryption-config.yaml
    volumeMounts:
    - name: encryption-config
      mountPath: /etc/kubernetes
      readOnly: true
  volumes:
  - name: encryption-config
    hostPath:
      path: /etc/kubernetes/encryption-config.yaml
      type: File
```

### 6.3 Secret 管理最佳实践

#### 6.3.1 创建 Secret

```bash
# 方法一：从文件创建
kubectl create secret generic db-credentials \
  --from-file=username=/path/to/username.txt \
  --from-file=password=/path/to/password.txt

# 方法二：从字面值创建
kubectl create secret generic api-key \
  --from-literal=api-key=my-secret-key

# 方法三：从 env 文件创建
kubectl create secret generic env-secrets \
  --from-env-file=.env

# 方法四：从 TLS 证书创建
kubectl create secret tls tls-secret \
  --cert=server.crt \
  --key=server.key

# 方法五：从 Docker registry 凭据创建
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=myuser \
  --docker-password=mypassword \
  --docker-email=myemail@example.com
```

#### 6.3.2 使用 Secret

```yaml
# pod-with-secret.yaml
apiVersion: v1
kind: Pod
metadata:
  name: app-with-secrets
spec:
  containers:
  - name: app
    image: my-app:v1
    env:
    # 将 Secret 作为环境变量（值会被注入）
    - name: DB_USERNAME
      valueFrom:
        secretKeyRef:
          name: db-credentials
          key: username
    - name: DB_PASSWORD
      valueFrom:
        secretKeyRef:
          name: db-credentials
          key: password
    envFrom:
    # 将所有 secret 导入为环境变量（前缀可加）
    - secretRef:
        name: api-key
        prefix: API_
    volumeMounts:
    # 将 Secret 挂载为文件
    - name: secrets
      mountPath: /etc/secrets
      readOnly: true
  volumes:
  - name: secrets
    secret:
      secretName: db-credentials
      defaultMode: 0400
```

#### 6.3.3 外部 Secret 管理（External Secrets Operator）

```yaml
# external-secret.yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: database-credentials
  namespace: production
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: vault-backend
    kind: ClusterSecretStore
  target:
    name: db-credentials
    creationPolicy: Owner
  data:
  - secretKey: username
    remoteRef:
      key: secret/database
      property: username
  - secretKey: password
    remoteRef:
      key: secret/database
      property: password
```

### 6.4 静态加密密钥轮换

```bash
# 1. 添加新的加密密钥
# 编辑 encryption-config.yaml 添加新密钥
- aesgcm:
    keys:
    - name: key1
      secret: <new base64 encoded 32-byte key>
    - name: key2
      secret: <old base64 encoded 32-byte key>

# 2. 重新加密所有 Secret
kubectl get secrets --all-namespaces -o json | \
  kubectl replace -f -

# 3. 移除旧密钥（确认加密成功后可移除）
```

---

## 7. 网络安全策略（NetworkPolicy）

### 7.1 NetworkPolicy 概述

NetworkPolicy 是 Kubernetes 提供的网络隔离机制，用于控制 Pod 之间的网络流量。

```
┌─────────────────────────────────────────────────────────────┐
│                 NetworkPolicy 作用示意                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│    ┌─────────────┐                                          │
│    │  frontend   │                                          │
│    │   (port 80) │                                          │
│    └──────┬──────┘                                          │
│           │ 允许 HTTP                                        │
│           ▼                                                  │
│    ┌─────────────┐                                          │
│    │   backend   │                                          │
│    │  (port 8080)│                                          │
│    └──────┬──────┘                                          │
│           │ 允许 TCP 8080                                    │
│           ▼                                                  │
│    ┌─────────────┐                                          │
│    │  database   │                                          │
│    │  (port 5432)│                                          │
│    └─────────────┘                                          │
│                                                             │
│    其他 Pod 无法访问 database                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 7.2 基础 NetworkPolicy

#### 7.2.1 禁止所有入口流量

```yaml
# deny-all-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-all-ingress
  namespace: production
spec:
  podSelector: {}              # 选择所有 Pod
  policyTypes:
  - Ingress                     # 定义入口策略
```

#### 7.2.2 允许特定 Pod 访问

```yaml
# allow-frontend-to-backend.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-frontend-to-backend
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: backend
  policyTypes:
  - Ingress
  ingress:
  - from:
    - podSelector:
        matchLabels:
          app: frontend     # 只允许带 frontend 标签的 Pod
    ports:
    - protocol: TCP
      port: 8080
```

### 7.3 完整网络隔离策略示例

```yaml
# 完整的三层架构网络策略
---
# 允许前端接收外部流量
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: frontend-policy
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: frontend
  policyTypes:
  - Ingress
  ingress:
  # 允许 HTTP/HTTPS
  - ports:
    - protocol: TCP
      port: 80
    - protocol: TCP
      port: 443
  # 允许来自同一命名空间前端 Pod 之间的通信
  - from:
    - podSelector:
        matchLabels:
          tier: frontend
---
# 允许后端接收前端流量
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-policy
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: backend
  policyTypes:
  - Ingress
  ingress:
  # 只允许来自前端的流量
  - from:
    - podSelector:
        matchLabels:
          tier: frontend
    ports:
    - protocol: TCP
      port: 8080
  # 允许健康检查
  - from: []
    ports:
    - protocol: TCP
      port: 8080
---
# 允许数据库只接收后端流量
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: database-policy
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: database
  policyTypes:
  - Ingress
  ingress:
  # 只允许来自后端的流量
  - from:
    - podSelector:
        matchLabels:
          tier: backend
    ports:
    - protocol: TCP
      port: 5432
---
# 禁止后端访问外部网络（可选）
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: backend-egress-policy
  namespace: production
spec:
  podSelector:
    matchLabels:
      tier: backend
  policyTypes:
  - Egress
  egress:
  # 只允许访问数据库
  - to:
    - podSelector:
        matchLabels:
          tier: database
    ports:
    - protocol: TCP
      port: 5432
  # 允许 DNS
  - to:
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: kube-system
    ports:
    - protocol: UDP
      port: 53
```

### 7.4 使用 namespaceSelector 隔离命名空间

```yaml
# allow-monitoring-to-app.yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-monitoring-to-app
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: my-app
  policyTypes:
  - Ingress
  ingress:
  # 允许 monitoring 命名空间的所有 Pod
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 8080
```

---

## 8. 安全配置最佳实践

### 8.1 集群级别安全配置

#### 8.1.1 API Server 安全配置

```yaml
# kube-apiserver-secure.yaml
apiVersion: v1
kind: Pod
metadata:
  name: kube-apiserver
spec:
  containers:
  - name: kube-apiserver
    command:
    # 安全绑定
    - kube-apiserver
    - --bind-address=0.0.0.0
    - --secure-port=6443
    - --insecure-port=0                    # 禁用不安全端口

    # TLS 安全配置
    - --tls-cert-file=/etc/kubernetes/pki/apiserver.crt
    - --tls-private-key-file=/etc/kubernetes/pki/apiserver.key
    - --client-ca-file=/etc/kubernetes/pki/ca.crt
    - --service-account-key-file=/etc/kubernetes/pki/sa.pub

    # 认证配置
    - --authorization-mode=Node,RBAC
    - --enable-bootstrap-token-auth=true
    - --oidc-issuer-url=https://auth.example.com
    - --oidc-client-id=kubernetes

    # 审计配置
    - --audit-log-path=/var/log/kubernetes/audit.log
    - --audit-log-maxage=30
    - --audit-log-maxbackup=10
    - --audit-log-maxsize=100
    - --audit-policy-file=/etc/kubernetes/audit-policy.yaml

    # 准入控制器
    - --enable-admission-plugins=\
      NodeRestriction,\
      AlwaysPullImages,\
      PodSecurity,\
      ServiceAccountAdmission,\
      ResourceQuota,\
      LimitRanger

    # 安全加固
    - --anonymous-auth=false
    - --encryption-provider-config=/etc/kubernetes/encryption-config.yaml
```

#### 8.1.2 审计策略配置

```yaml
# audit-policy.yaml
apiVersion: audit.k8s.io/v1
kind: Policy
rules:
# 不记录只读请求到 /healthz 和 /metrics
- level: None
  resources:
  - group: ""
    resources: ["healthz", "metrics"]
  nonResourceURLs:
  - "/healthz*"
  - "/metrics"

# 不记录 Secret 的读取（敏感数据保护）
- level: None
  resources:
  - group: ""
    resources: ["secrets"]
  verbs: ["get", "list", "watch"]

# 记录元数据级别的请求
- level: Metadata
  resources:
  - group: ""
    resources: ["pods", "services", "configmaps"]
  verbs: ["get", "list"]

# 记录完整的请求（包括请求体）用于关键资源
- level: RequestResponse
  resources:
  - group: ""
    resources: ["pods"]
  verbs: ["create", "update", "delete"]
  namespaces: ["production"]

# 所有其他请求记录请求级别
- level: Request
```

### 8.2 节点级别安全配置

#### 8.2.1 Kubelet 安全配置

```yaml
# kubelet config
apiVersion: kubelet.config.k8s.io/v1beta1
kind: KubeletConfiguration
authentication:
  # 禁用匿名认证
  anonymous:
    enabled: false
  # 使用 Webhook 认证
  webhook:
    enabled: true
    cacheTTL: 2m0s
authorization:
  # 使用 RBAC 授权
  mode: RBAC
TLSMinVersion: VersionTLS12
TLSCipherSuites:
- TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
- TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
- TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305
- TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
readOnlyPort: 0                    # 禁用只读端口
protectKernelDefaults: true
makeIPTablesUtilChains: true
```

### 8.3 完整的生产环境安全配置示例

#### 8.3.1 命名空间安全配置

```yaml
# production-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: production
  labels:
    # Pod 安全标准
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/enforce-version: latest
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/audit-version: latest
    # 监控标签
    name: production
---
# ResourceQuota
apiVersion: v1
kind: ResourceQuota
metadata:
  name: production-quota
  namespace: production
spec:
  hard:
    requests.cpu: "20"
    requests.memory: 40Gi
    limits.cpu: "40"
    limits.memory: 80Gi
    pods: "200"
    services: "20"
    persistentvolumeclaims: "50"
---
# LimitRange
apiVersion: v1
kind: LimitRange
metadata:
  name: production-limits
  namespace: production
spec:
  limits:
  - type: Container
    default:
      cpu: 1
      memory: 512Mi
    defaultRequest:
      cpu: 100m
      memory: 128Mi
    max:
      cpu: "8"
      memory: 16Gi
    min:
      cpu: 50m
      memory: 64Mi
  - type: Pod
    max:
      cpu: "16"
      memory: 32Gi
```

#### 8.3.2 应用安全配置模板

```yaml
# secure-application-template.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: my-app
  namespace: production
automountToken: false              # 关闭自动挂载
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: my-app-role
  namespace: production
rules:
# 只允许读取必要资源
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get"]
  resourceNames: ["my-app-config"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: my-app-rolebinding
  namespace: production
subjects:
- kind: ServiceAccount
  name: my-app
  namespace: production
roleRef:
  kind: Role
  name: my-app-role
  apiGroup: rbac.authorization.k8s.io
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: my-app-config
  namespace: production
data:
  app.yaml: |
    # 应用配置（非敏感）
---
apiVersion: v1
kind: Secret
metadata:
  name: my-app-secrets
  namespace: production
type: Opaque
stringData:
  api-key: "your-secret-api-key"
  database-password: "your-db-password"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
  namespace: production
  labels:
    app: my-app
    environment: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
        environment: production
    spec:
      serviceAccountName: my-app

      # 安全上下文
      securityContext:
        runAsNonRoot: true
        runAsUser: 1000
        runAsGroup: 1000
        fsGroup: 1000
        seccompProfile:
          type: RuntimeDefault

      # 亲和性配置（高可用）
      affinity:
        podAntiAffinity:
          requiredDuringSchedulingIgnoredDuringExecution:
          - labelSelector:
              matchExpressions:
              - key: app
                operator: In
                values:
                - my-app
            topologyKey: kubernetes.io/hostname

      containers:
      - name: my-app
        image: my-app:v1.0.0
        imagePullPolicy: Always

        # 安全上下文
        securityContext:
          allowPrivilegeEscalation: false
          readOnlyRootFilesystem: true
          capabilities:
            drop:
            - ALL

        # 资源限制
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 512Mi

        # 健康检查
        livenessProbe:
          httpGet:
            path: /healthz
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /ready
            port: 8080
          initialDelaySeconds: 5
          periodSeconds: 5

        # 环境变量（引用 Secret）
        env:
        - name: API_KEY
          valueFrom:
            secretKeyRef:
              name: my-app-secrets
              key: api-key
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: my-app-secrets
              key: database-password

        # 挂载
        volumeMounts:
        - name: tmp
          mountPath: /tmp
        - name: config
          mountPath: /etc/config
          readOnly: true

      volumes:
      - name: tmp
        emptyDir: {}
      - name: config
        configMap:
          name: my-app-config
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: my-app-network-policy
  namespace: production
spec:
  podSelector:
    matchLabels:
      app: my-app
  policyTypes:
  - Ingress
  - Egress
  ingress:
  # 允许来自前端或网关的流量
  - from:
    - podSelector:
        matchLabels:
          app: frontend
    ports:
    - protocol: TCP
      port: 8080
  egress:
  # 允许 DNS
  - to:
    - namespaceSelector:
        matchLabels:
          kubernetes.io/metadata.name: kube-system
    ports:
    - protocol: UDP
      port: 53
  # 允许访问数据库
  - to:
    - podSelector:
        matchLabels:
          tier: database
    ports:
    - protocol: TCP
      port: 5432
```

### 8.4 安全检查清单

#### 集群级别
- [ ] API Server 不暴露在公网
- [ ] etcd 使用 TLS 和加密
- [ ] RBAC 权限最小化
- [ ] 启用审计日志
- [ ] 禁用匿名认证
- [ ] 使用强 TLS 版本（1.2+）
- [ ] 启用 Admission Controller

#### 命名空间级别
- [ ] 设置 ResourceQuota
- [ ] 设置 LimitRange
- [ ] 强制 Pod Security Standards
- [ ] 隔离不同环境的命名空间

#### 应用级别
- [ ] 使用非 root 用户运行容器
- [ ] 使用只读根文件系统
- [ ] 删除不必要的 capabilities
- [ ] 禁止特权容器
- [ ] 使用 Secret 存储敏感信息
- [ ] 配置健康检查
- [ ] 配置资源限制
- [ ] 使用 NetworkPolicy 隔离网络

---

## 总结

Kubernetes 安全是一个多层次、全方位的防护体系：

1. **认证**：通过 x509 证书、Token 或 OIDC 验证客户端身份
2. **授权**：通过 RBAC 控制用户对资源的访问权限
3. **准入控制**：通过 Admission Controller 拦截和验证请求
4. **Pod 安全**：通过 PSP/PSS 和安全上下文保护工作负载
5. **Secret 安全**：加密存储敏感信息
6. **网络安全**：通过 NetworkPolicy 实现网络隔离

遵循最小权限原则，层层设防，才能构建安全的 Kubernetes 集群。

---

> 上一章：[Kubernetes 运维与调试](./chapter4-kubernetes-operations.md)
>
> 下一章：[Kubernetes 存储与持久化](./chapter6-kubernetes-storage.md)
