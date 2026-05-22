# Kubernetes 教程第四章：Kubernetes 存储管理

## 目录
1. [Kubernetes 存储架构](#1-kubernetes-存储架构)
2. [Volume 类型详解](#2-volume-类型详解)
3. [PersistentVolume 和 PersistentVolumeClaim](#3-persistentvolume-和-persistentvolumeclaim)
4. [StorageClass 与动态存储供应](#4-storageclass-与动态存储供应)
5. [CSI 容器存储接口](#5-csi-容器存储接口)
6. [有状态应用的存储最佳实践](#6-有状态应用的存储最佳实践)

---

## 1. Kubernetes 存储架构

### 1.1 存储架构概述

Kubernetes 的存储体系分为两个主要层级：

```
┌─────────────────────────────────────────────────────────────┐
│                     Kubernetes 存储架构                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐    │
│  │    Pod      │────▶│   Volume    │────▶│  Storage    │    │
│  │             │     │             │     │   Backend   │    │
│  └─────────────┘     └─────────────┘     └─────────────┘    │
│        │                   │                   │           │
│        │            ┌──────┴──────┐            │           │
│        │            │             │            │           │
│  ┌─────▼─────┐  ┌────▼────┐  ┌─────▼─────┐  ┌───▼───┐      │
│  │  Pod Spec │  │Volume插件│  │  PV/PVC   │  │  CSI  │      │
│  │ (临时存储)│  │(内置+CSI)│  │ (持久存储) │  │ 驱动  │      │
│  └───────────┘  └─────────┘  └───────────┘  └───────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 存储组件交互流程

```
┌──────────────────────────────────────────────────────────────────┐
│                     存储请求处理流程                              │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  用户创建 Pod                                                     │
│       │                                                          │
│       ▼                                                          │
│  ┌─────────────────┐                                            │
│  │  Pod Spec       │                                            │
│  │  (定义 Volume)   │                                            │
│  └────────┬────────┘                                            │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐     ┌─────────────────┐                    │
│  │  Volume Plugin  │────▶│  挂载到容器     │                    │
│  │  (或 CSI)       │     │                │                    │
│  └────────┬────────┘     └─────────────────┘                    │
│           │                                                      │
│           ▼                                                      │
│  ┌─────────────────┐                                            │
│  │  Storage        │                                            │
│  │  Backend        │                                            │
│  └─────────────────┘                                            │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Volume 类型详解

### 2.1 emptyDir（临时存储）

**用途**：作为临时工作空间，适合缓存、临时文件等场景。

**特性**：
- Pod 被调度到节点时创建
- Pod 删除时自动删除
- 初始为空容器
- 可设置 medium 为 Memory 支持 tmpfs（内存文件系统）

**示例 1：基本使用**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: emptydir-pod
spec:
  containers:
  - name: app
    image: nginx
    volumeMounts:
    - name: cache-volume
      mountPath: /cache
  volumes:
  - name: cache-volume
    emptyDir: {}
```

**示例 2：使用内存文件系统**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: emptydir-memory-pod
spec:
  containers:
  - name: app
    image: nginx
    volumeMounts:
    - name: cache-volume
      mountPath: /tmp
    resources:
      limits:
        memory: 128Mi
  volumes:
  - name: cache-volume
    emptyDir:
      medium: Memory
      sizeLimit: 128Mi
```

**使用场景**：
- 同一 Pod 内多容器共享文件
- 临时缓存目录
- 合并计算结果

---

### 2.2 hostPath（节点路径映射）

**用途**：将节点文件系统中的文件或目录挂载到容器中。

**特性**：
- 绕过 Kubernetes 管理，直接访问节点文件系统
- 需要注意 Pod 调度到的节点
- 常用于系统级 Pod（如日志收集、监控代理）

**警告**：hostPath 可能导致安全风险和生产问题，仅在必要场景使用。

**示例 1：基本使用**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: hostpath-pod
spec:
  containers:
  - name: app
    image: nginx
    volumeMounts:
    - name: log-volume
      mountPath: /var/log/nginx
  volumes:
  - name: log-volume
    hostPath:
      path: /data/nginx-logs
      type: DirectoryOrCreate
```

**type 可选值**：
| Type | 说明 |
|------|------|
| `DirectoryOrCreate` | 目录不存在时创建 |
| `Directory` | 目录必须存在 |
| `FileOrCreate` | 文件不存在时创建 |
| `File` | 文件必须存在 |
| `Socket` | Unix 套接字必须存在 |
| `CharDevice` | 字符设备必须存在 |
| `BlockDevice` | 块设备必须存在 |

---

### 2.3 gitRepo（已废弃）

**重要提醒**：`gitRepo` 卷类型在 Kubernetes 1.16+ 已废弃，1.25+ 已移除。

**替代方案**：使用 Init Container 克隆代码，或使用 Sidecar 容器同步。

**示例：使用 Init Container 替代 gitRepo**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: git-sync-pod
spec:
  initContainers:
  - name: git-clone
    image: alpine/git
    args:
    - clone
    - https://github.com/example/repo.git
    - /data
    volumeMounts:
    - name: data
      mountPath: /data
  containers:
  - name: app
    image: nginx
    volumeMounts:
    - name: data
      mountPath: /usr/share/nginx/html
      readOnly: true
  volumes:
  - name: data
    emptyDir: {}
```

---

### 2.4 NFS（网络文件系统）

**用途**：使用 NFS 服务器作为共享存储，多 Pod 可同时读写。

**特性**：
- 支持 ReadWriteMany 访问模式
- 持久化数据，不随 Pod 删除而删除
- 需要外部 NFS 服务器

**示例：NFS Volume 配置**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nfs-pod
spec:
  containers:
  - name: app
    image: nginx
    volumeMounts:
    - name: nfs-storage
      mountPath: /var/www/html
  volumes:
  - name: nfs-storage
    nfs:
      server: 192.168.1.100
      path: /exports/html
      readOnly: false
```

---

### 2.5 persistentVolumeClaim（持久卷声明）

**用途**：从预配置的 PersistentVolume 中申请存储资源。

**示例：Pod 使用 PVC**

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: pvc-pod
spec:
  containers:
  - name: app
    image: nginx
    volumeMounts:
    - name: persistent-storage
      mountPath: /data
  volumes:
  - name: persistent-storage
    persistentVolumeClaim:
      claimName: my-pvc
      readOnly: false
```

---

## 3. PersistentVolume 和 PersistentVolumeClaim

### 3.1 核心概念

```
┌─────────────────────────────────────────────────────────────┐
│                    PV/PVC 关系图                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐         ┌─────────────┐                    │
│  │ 管理员创建   │         │   用户创建   │                    │
│  │   PV        │◀────────│   PVC       │                    │
│  └──────┬──────┘  绑定    └──────┬──────┘                    │
│         │                        │                           │
│         │    StorageClass        │                           │
│         │    AccessMode          │                           │
│         │    Capacity            │                           │
│         │    ReclaimPolicy       │                           │
│         ▼                        ▼                           │
│  ┌─────────────────────────────────────────┐                │
│  │           Storage Backend               │                │
│  │     (NFS, iSCSI, Cloud Storage)         │                │
│  └─────────────────────────────────────────┘                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 访问模式（Access Modes）

| 模式 | 缩写 | 说明 |
|------|------|------|
| ReadWriteOnce | RWO | 单节点读写（最常用） |
| ReadOnlyMany | ROX | 多节点只读 |
| ReadWriteMany | RWX | 多节点读写 |
| ReadWriteOncePod | RWOP | 单 Pod 读写（Kubernetes 1.22+） |

### 3.3 回收策略（Reclaim Policy）

| 策略 | 说明 |
|------|------|
| Retain | 保留数据，需要管理员手动清理 |
| Delete | 删除 PV 时自动删除存储资源 |
| Recycle | 清除数据后重用（已废弃，推荐动态供应） |

### 3.4 生命周期状态

```
┌─────────────────────────────────────────────────────────────────┐
│                    PV 生命周期状态                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│    Pending          Bound            Released         Deleted    │
│       │               │                  │               │      │
│       ▼               ▼                  ▼               ▼      │
│  ┌─────────┐     ┌─────────┐        ┌─────────┐     ┌─────────┐ │
│  │  等待   │────▶│  已绑定  │───────▶│ 已释放  │────▶│  已删除  │ │
│  │  PVC    │     │   PVC   │        │(数据保留)│     │ (存储   │ │
│  │  匹配   │     │         │        │         │     │  删除)  │ │
│  └─────────┘     └─────────┘        └─────────┘     └─────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.5 PV 定义示例

**示例 1：NFS PersistentVolume**

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-nfs
  labels:
    type: nfs
    environment: production
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  nfs:
    server: 192.168.1.100
    path: /exports/data
```

**示例 2：Local PersistentVolume**

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-local
spec:
  capacity:
    storage: 50Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  local:
    path: /mnt/disks/ssd1
  nodeAffinity:
    required:
      nodeSelectorTerms:
      - matchExpressions:
        - key: kubernetes.io/hostname
          operator: In
          values:
          - node-1
          - node-2
```

**示例 3：AWS EBS PersistentVolume**

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-aws-ebs
spec:
  capacity:
    storage: 100Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  awsElasticBlockStore:
    volumeID: aws://us-east-1a/vol-0a1b2c3d4e5f
    fsType: ext4
```

### 3.6 PVC 定义示例

**示例 1：基础 PVC**

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
```

**示例 2：带存储类别的 PVC**

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: my-pvc-with-storageclass
spec:
  storageClassName: fast-ssd
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```

### 3.7 完整使用示例

**Step 1: 创建 PV**

```yaml
# pv-nfs.yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: pv-nfs-001
spec:
  capacity:
    storage: 5Gi
  accessModes:
    - ReadWriteOnce
  persistentVolumeReclaimPolicy: Retain
  nfs:
    server: 192.168.1.100
    path: /exports/pv001
```

```bash
kubectl apply -f pv-nfs.yaml
kubectl get pv
```

**Step 2: 创建 PVC**

```yaml
# pvc-basic.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pvc-basic
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 3Gi
```

```bash
kubectl apply -f pvc-basic.yaml
kubectl get pvc
# NAME        STATUS   VOLUME     CAPACITY   ACCESS MODES   AGE
# pvc-basic   Bound    pv-nfs-001   5Gi       RWO            10s
```

---

## 4. StorageClass 与动态存储供应

### 4.1 为什么需要 StorageClass

```
┌─────────────────────────────────────────────────────────────┐
│               静态供应 vs 动态供应                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  静态供应（Static Provisioning）:                            │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐               │
│  │ 管理员   │───▶│   PV     │───▶│   PVC    │               │
│  │ 手动创建  │    │          │    │          │               │
│  └──────────┘    └──────────┘    └──────────┘               │
│  问题：管理员必须预先创建所有 PV                              │
│                                                             │
│  动态供应（Dynamic Provisioning）:                           │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌────────┐ │
│  │   用户   │───▶│   PVC    │───▶│StorageClass│───▶│ Provisioner│
│  │          │    │          │    │           │    │创建PV  │ │
│  └──────────┘    └──────────┘    └──────────┘    └────────┘ │
│  优势：按需自动创建 PV                                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 StorageClass 核心字段

| 字段 | 说明 |
|------|------|
| `provisioner` | 存储供应商（内置或 CSI） |
| `parameters` | 供应商特定参数 |
| `reclaimPolicy` | 默认为 Delete |
| `allowVolumeExpansion` | 是否允许扩展 |
| `mountOptions` | 挂载选项 |
| `volumeBindingMode` | 绑定模式 |

### 4.3 StorageClass 示例

**示例 1：AWS EBS StorageClass**

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ebs-sc
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"
reclaimPolicy: Retain
allowVolumeExpansion: true
volumeBindingMode: WaitForFirstConsumer
```

**示例 2：NFS StorageClass**

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: nfs-sc
provisioner: k8s-sigs.io/nfs-subdir-external-provisioner
parameters:
  archiveOnDelete: "false"
  onDelete: "delete"
reclaimPolicy: Retain
```

**示例 3：Local StorageClass**

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: local-sc
provisioner: kubernetes.io/no-provisioner
volumeBindingMode: WaitForFirstConsumer
reclaimPolicy: Retain
```

### 4.4 volumeBindingMode 详解

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| Immediate | PVC 创建时立即绑定 PV | 已知 Pod 调度位置 |
| WaitForFirstConsumer | 首个 Pod 使用 PVC 时才绑定 | 本地存储，需要考虑节点位置 |

### 4.5 动态供应完整示例

**Step 1: 安装 NFS Provisioner**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: nfs-provisioner
spec:
  selector:
    matchLabels:
      app: nfs-provisioner
  template:
    metadata:
      labels:
        app: nfs-provisioner
    spec:
      serviceAccountName: nfs-provisioner
      containers:
      - name: nfs-provisioner
        image: k8s.gcr.io/sig-storage/nfs-subdir-external-provisioner:v4.0.2
        volumeMounts:
        - name: nfs-client-root
          mountPath: /persistentvolumes
        env:
        - name: PROVISIONER_NAME
          value: k8s-sigs.io/nfs-subdir-external-provisioner
        - name: NFS_SERVER
          value: 192.168.1.100
        - name: NFS_PATH
          value: /exports
      volumes:
      - name: nfs-client-root
        nfs:
          server: 192.168.1.100
          path: /exports
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: nfs-provisioner
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: nfs-provisioner-runner
rules:
  - apiGroups: [""]
    resources: ["persistentvolumes"]
    verbs: ["get", "list", "watch", "create", "delete"]
  - apiGroups: [""]
    resources: ["persistentvolumeclaims"]
    verbs: ["get", "list", "watch", "update"]
  - apiGroups: ["storage.k8s.io"]
    resources: ["storageclasses"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["events"]
    verbs: ["create", "update", "patch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: run-nfs-provisioner
subjects:
- kind: ServiceAccount
  name: nfs-provisioner
  namespace: default
roleRef:
  kind: ClusterRole
  name: nfs-provisioner-runner
  apiGroup: rbac.authorization.k8s.io
```

**Step 2: 用户直接创建 PVC（自动创建 PV）**

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pvc-dynamic
spec:
  storageClassName: nfs-storage
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 2Gi
```

```bash
# 查看自动创建的 PV
kubectl get pv
# NAME                                       CAPACITY   ACCESS MODES   RECLAIM POLICY   STATUS   CLAIM
# pvc-0a1b2c3d-xxxx-xxxx-xxxx-xxxxxxxxxxxx   2Gi        RWO            Retain           Bound    default/pvc-dynamic
```

---

## 5. CSI 容器存储接口

### 5.1 CSI 架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        CSI 架构图                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────┐         ┌─────────────┐         ┌──────────┐  │
│  │   CO (K8s)  │◀───────▶│   CSI RPC   │◀───────▶│   CSI    │  │
│  │             │  gRPC   │   协议      │  gRPC   │  Driver  │  │
│  └─────────────┘         └─────────────┘         └────┬─────┘  │
│                                                          │       │
│                                                          ▼       │
│                                                  ┌──────────────┐ │
│                                                  │   Storage    │ │
│                                                  │   Backend    │ │
│                                                  └──────────────┘ │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 CSI 优势

| 特性 | 传统 Volume 插件 | CSI |
|------|------------------|-----|
| 部署方式 | Kubernetes 核心代码 | 独立部署 |
| 更新方式 | 需要 K8s 版本升级 | 无需升级 K8s |
| 供应商支持 | 需要合并到 K8s 源码 | 独立维护 |
| 功能完整性 | 部分功能受限 | 完整功能支持 |

### 5.3 常用 CSI 驱动

| 驱动 | 存储类型 | 供应商 |
|------|----------|--------|
| aws-ebs-csi-driver | AWS EBS | AWS |
| gcp-pd-csi-driver | GCP Persistent Disk | Google Cloud |
| azuredisk-csi-driver | Azure Disk | Azure |
| nfs-csi-driver | NFS | 社区 |
| ceph-csi | Ceph RBD/CephFS | Ceph |
| longhorn | Block Storage | Longhorn |

### 5.4 CSI 使用示例

**安装 AWS EBS CSI Driver**

```bash
helm repo add aws-ebs-csi-driver https://kubernetes.github.io/aws-ebs-csi-driver
helm repo update
helm upgrade --install aws-ebs-csi-driver aws-ebs-csi-driver/aws-ebs-csi-driver \
  --namespace kube-system \
  --set enableVolumeScheduling=true \
  --set enableVolumeResizing=true
```

**使用 CSI 驱动的 StorageClass**

```yaml
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: ebs-csi-sc
provisioner: ebs.csi.aws.com
parameters:
  type: gp3
  encrypted: "true"
reclaimPolicy: Delete
allowVolumeExpansion: true
volumeBindingMode: WaitForFirstConsumer
```

**CSI Snapshot 示例**

```yaml
# VolumeSnapshotClass
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshotClass
metadata:
  name: ebs-snapshot-class
driver: ebs.csi.aws.com
deletionPolicy: Delete
---
# 创建快照
apiVersion: snapshot.storage.k8s.io/v1
kind: VolumeSnapshot
metadata:
  name: my-snapshot
spec:
  volumeSnapshotClassName: ebs-snapshot-class
  source:
    persistentVolumeClaimName: my-pvc
---
# 从快照恢复
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: restored-pvc
spec:
  storageClassName: ebs-csi-sc
  dataSource:
    name: my-snapshot
    kind: VolumeSnapshot
    apiGroup: snapshot.storage.k8s.io
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

---

## 6. 有状态应用的存储最佳实践

### 6.1 有状态 vs 无状态应用

```
┌─────────────────────────────────────────────────────────────────┐
│                  有状态 vs 无状态应用对比                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  无状态应用 (Stateless):                                        │
│  - 数据不持久化，存储在外部数据库                                │
│  - Pod 可以随意终止和重建                                        │
│  - 使用 ConfigMap/Secret 配置                                   │
│  - 示例: Nginx, API Gateway, Web 服务器                         │
│                                                                 │
│  有状态应用 (Stateful):                                          │
│  - 需要持久化存储                                                │
│  - 有唯一标识（网络标识、存储标识）                              │
│  - 需要稳定的部署、扩缩容、删除顺序                               │
│  - 示例: 数据库、消息队列、分布式存储                            │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 StatefulSet 存储管理

```
┌─────────────────────────────────────────────────────────────────┐
│                   StatefulSet 存储结构                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  StatefulSet: web                                               │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐                     ││
│  │  │ web-0   │  │ web-1   │  │ web-2   │                     ││
│  │  │   │     │  │   │     │  │   │     │                     ││
│  │  │   ▼     │  │   ▼     │  │   ▼     │                     ││
│  │  │[pvc-web-0]│[pvc-web-1]│[pvc-web-2]│                     ││
│  │  └─────────┘  └─────────┘  └─────────┘                     ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  特点:                                                           │
│  - VolumeClaimTemplate: 每个 Pod 有独立的 PVC                   │
│  - 稳定网络标识: web-0, web-1, web-2                            │
│  - 顺序部署/扩缩容: web-0 -> web-1 -> web-2                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.3 VolumeClaimTemplate 详解

**示例：StatefulSet with VolumeClaimTemplate**

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: web
spec:
  serviceName: "nginx"
  replicas: 3
  selector:
    matchLabels:
      app: nginx
  template:
    metadata:
      labels:
        app: nginx
    spec:
      containers:
      - name: nginx
        image: nginx:latest
        ports:
        - containerPort: 80
          name: web
        volumeMounts:
        - name: www
          mountPath: /usr/share/nginx/html
  volumeClaimTemplates:
  - metadata:
      name: www
    spec:
      accessModes: [ "ReadWriteOnce" ]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 1Gi
```

### 6.4 数据库存储最佳实践

**示例：MySQL 部署配置**

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: mysql
  labels:
    app: mysql
spec:
  selector:
    matchLabels:
      app: mysql
  serviceName: mysql
  replicas: 1
  template:
    metadata:
      labels:
        app: mysql
    spec:
      securityContext:
        fsGroup: 999
      containers:
      - name: mysql
        image: mysql:8.0
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysql-secret
              key: password
        ports:
        - containerPort: 3306
          name: mysql
        volumeMounts:
        - name: mysql-data
          mountPath: /var/lib/mysql
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 1000m
            memory: 2Gi
        livenessProbe:
          exec:
            command: ["mysqladmin", "ping", "-h", "localhost"]
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          exec:
            command: ["mysqladmin", "ping", "-h", "localhost"]
          initialDelaySeconds: 30
          periodSeconds: 5
  volumeClaimTemplates:
  - metadata:
      name: mysql-data
    spec:
      accessModes: ["ReadWriteOnce"]
      storageClassName: "fast-ssd"
      resources:
        requests:
          storage: 50Gi
```

### 6.5 数据备份与恢复策略

**备份 PVC 数据 Job**

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: backup-pvc
spec:
  template:
    spec:
      containers:
      - name: backup
        image: alpine/tar
        command:
        - /bin/sh
        - -c
        - |
          cd /data
          tar czf /backup/backup-$(date +%Y%m%d-%H%M%S).tar.gz .
        volumeMounts:
        - name: source-data
          mountPath: /data
        - name: backup-nfs
          mountPath: /backup
      volumes:
      - name: source-data
        persistentVolumeClaim:
          claimName: mysql-data-mysql-0
      - name: backup-nfs
        nfs:
          server: 192.168.1.100
          path: /exports/backups
      restartPolicy: OnFailure
  backoffLimit: 4
```

### 6.6 存储安全最佳实践

**示例：使用只读挂载**

```yaml
volumes:
- name: config
  persistentVolumeClaim:
    claimName: config-pvc
    readOnly: true
```

---

## 总结

本章介绍了 Kubernetes 存储管理的核心概念：

1. **存储架构**：Volume 是 Pod 与存储后端的桥梁
2. **Volume 类型**：emptyDir、hostPath、nfs、PVC 等各有用途
3. **PV/PVC**：解耦存储供应与使用，支持持久化存储
4. **StorageClass**：实现动态存储供应自动化
5. **CSI**：标准化容器存储接口，支持多供应商
6. **有状态应用**：使用 StatefulSet + VolumeClaimTemplate 管理稳定存储

**最佳实践要点**：
- 生产环境优先使用动态供应
- 选择合适的 StorageClass 和 AccessMode
- 重要数据定期备份
- 遵循最小权限原则配置存储访问
- 监控存储容量使用情况

---

**第四章完**