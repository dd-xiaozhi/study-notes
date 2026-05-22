# 第七章：Kubernetes 运维与监控

## 目录

1. [集群部署方式](#1-集群部署方式)
2. [集群升级流程](#2-集群升级流程)
3. [etcd 备份与恢复](#3-etcd-备份与恢复)
4. [监控方案](#4-监控方案)
5. [日志收集方案](#5-日志收集方案)
6. [AutoScaling](#6-autoscaling)
7. [Helm 包管理器](#7-helm-包管理器)
8. [Operator 模式](#8-operator-模式)

---

## 1. 集群部署方式

Kubernetes 集群有多种部署方式，本章详细介绍四种主流方案。

### 1.1 kubeadm（官方推荐）

kubeadm 是 Kubernetes 官方提供的集群部署工具，适合生产环境。

#### 环境要求

- **控制平面节点**：
  - 2 核 CPU 或以上
  - 2GB 内存或以上
  - 20GB 磁盘空间或以上
  - Ubuntu 16.04+、CentOS 7+、Debian 9+ 等

- **工作节点**：
  - 1 核 CPU 或以上
  - 1GB 内存或以上
  - 20GB 磁盘空间或以上

#### 安装步骤

**1. 系统准备（所有节点）**

```bash
# 设置主机名
hostnamectl set-hostname k8s-master-1

# 关闭防火墙
systemctl stop firewalld
systemctl disable firewalld

# 关闭 SELinux
setenforce 0
sed -i 's/SELINUX=enforcing/SELINUX=disabled/g' /etc/selinux/config

# 关闭 swap
swapoff -a
sed -i '/ swap / s/^\(.*\)$/#\1/' /etc/fstab

# 开启 IP 转发
cat > /etc/sysctl.d/k8s.conf << EOF
net.bridge.bridge-nf-call-iptables  = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward                 = 1
EOF
sysctl --system
```

**2. 安装容器运行时（containerd）**

```bash
# 安装 containerd
yum install -y containerd

# 生成默认配置
mkdir -p /etc/containerd
containerd config default > /etc/containerd/config.toml

# 修改配置文件，启用 systemd cgroup 驱动
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml

# 重启 containerd
systemctl restart containerd
systemctl enable containerd
```

**3. 安装 kubeadm、kubelet 和 kubectl**

```bash
# 添加阿里云 Kubernetes 源
cat > /etc/yum.repos.d/kubernetes.repo << EOF
[kubernetes]
name=Kubernetes
baseurl=https://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64/
enabled=1
gpgcheck=0
EOF

# 安装组件
yum install -y kubeadm kubelet kubectl

# 设置 kubelet 开机自启
systemctl enable kubelet
```

**4. 初始化控制平面**

```bash
# 在主节点执行
kubeadm init \
  --apiserver-advertise-address=192.168.1.100 \
  --pod-network-cidr=10.244.0.0/16 \
  --service-cidr=10.96.0.0/12 \
  --kubernetes-version=v1.28.0
```

**5. 配置 kubectl**

```bash
mkdir -p $HOME/.kube
cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
chown $(id -u):$(id -g) $HOME/.kube/config
```

**6. 安装网络插件（Calico）**

```bash
kubectl apply -f https://docs.projectcalico.org/manifests/calico.yaml
```

**7. 将工作节点加入集群**

```bash
# 在主节点获取 join 命令
kubeadm token create --print-join-command

# 在工作节点执行（输出结果）
kubeadm join 192.168.1.100:6443 --token xxxxxxxx \
  --discovery-token-ca-cert-hash sha256:xxxxxxxx
```

**8. 验证集群状态**

```bash
kubectl get nodes
kubectl get pods -n kube-system
```

---

### 1.2 kops（AWS 云部署）

kops 是 Kubernetes Operations 的缩写，专门用于在 AWS 上自动化部署和管理 Kubernetes 集群。

#### 安装 kops

```bash
# 下载 kops
curl -LO https://github.com/kubernetes/kops/releases/download/v1.28.0/kops-linux-amd64
chmod +x kops-linux-amd64
sudo mv kops-linux-amd64 /usr/local/bin/kops

# 安装 kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

#### AWS 环境配置

```bash
# 配置 AWS 凭证
aws configure

# 创建 S3 存储桶用于存储集群状态
aws s3 mb s3://my-k8s-cluster-state --region us-west-2

# 创建 Route 53 托管区域
aws route53 create-hosted-zone --name k8s.example.com --caller-reference $(date +%s)
```

#### 部署集群

```bash
# 设置环境变量
export KOPS_STATE_STORE=s3://my-k8s-cluster-state
export AWS_REGION=us-west-2

# 使用 kops 创建集群配置
kops create cluster \
  --name=k8s.k8s.example.com \
  --cloud=aws \
  --zones=us-west-2a,us-west-2b \
  --master-size=t3.medium \
  --node-size=t3.large \
  --node-count=3 \
  --ssh-public-key=~/.ssh/id_rsa.pub

# 应用配置创建集群
kops update cluster --name=k8s.k8s.example.com --yes

# 验证集群创建
kops validate cluster --wait 10m
```

#### 常用 kops 命令

```bash
# 编辑集群配置
kops edit cluster k8s.k8s.example.com

# 编辑节点组配置
kops edit ig nodes --name=k8s.k8s.example.com

# 更新集群（应用配置更改）
kops update cluster --name=k8s.k8s.example.com --yes

# 升级 Kubernetes 版本
kops upgrade cluster k8s.k8s.example.com --yes

# 删除集群
kops delete cluster k8s.k8s.example.com --yes
```

---

### 1.3 minikube（本地开发）

minikube 是在本地单机运行 Kubernetes 的最佳方案，适合开发测试。

#### 安装 minikube

```bash
# Linux 安装
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# macOS 安装
brew install minikube

# Windows 安装（使用 Chocolatey）
choco install minikube
```

#### 安装 kubectl

```bash
# Linux/macOS
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo install kubectl /usr/local/bin/
```

#### 启动集群

```bash
# 使用 docker 驱动启动
minikube start --driver=docker --kubernetes-version=v1.28.0

# 使用虚拟化驱动启动
minikube start --driver=hyperv --kubernetes-version=v1.28.0

# 指定资源
minikube start --cpus=4 --memory=8g --disk-size=50g

# 使用国内镜像
minikube start \
  --image-mirror-country=cn \
  --registry-mirror=https://docker.mirrors.ustc.edu.cn
```

#### 常用命令

```bash
# 查看状态
minikube status

# 访问 Kubernetes 控制台
minikube dashboard

# 访问服务
minikube service nginx-service

# SSH 连接到节点
minikube ssh

# 停止集群
minikube stop

# 删除集群
minikube delete

# 查看日志
minikube logs

# 挂载文件
minikube mount /host/path:/mnt/path

# 获取集群 IP
minikube ip
```

#### 添加ons

```bash
# 安装 Ingress 插件
minikube addons enable ingress

# 安装存储类插件
minikube addons enable storage-provisioner

# 查看可用插件
minikube addons list
```

---

### 1.4 k3s（轻量级）

k3s 是 Rancher 提供的轻量级 Kubernetes 发行版，适合边缘计算、IoT 和资源受限环境。

#### k3s 与 k8s 的区别

| 特性 | k3s | 标准 k8s |
|------|-----|----------|
| 二进制大小 | < 100MB | > 300MB |
| 内存需求 | 512MB+ | 2GB+ |
| SQLite 支持 | 是 | 否 |
| Kubernetes API | 100% 兼容 | - |

#### 安装 k3s（单节点）

```bash
# 快速安装
curl -sfL https://get.k3s.io | sh -

# 查看状态
systemctl status k3s

# 获取 kubectl 配置
cat /etc/rancher/k3s/k3s.yaml
```

#### 安装 k3s（高可用）

```bash
# 第一步：在第一个服务器节点启动
curl -sfL https://get.k3s.io | sh -s - server \
  --cluster-init \
  --token=<CLUSTER_TOKEN> \
  --tls-san=<LOAD_BALANCER_DNS>

# 第二步和第三步：加入其他服务器节点
curl -sfL https://get.k3s.io | sh -s - server \
  --server https://<FIRST_NODE_IP>:6443 \
  --token=<CLUSTER_TOKEN> \
  --tls-san=<LOAD_BALANCER_DNS>

# 工作节点加入
curl -sfL https://get.k3s.io | sh -s - agent \
  --server https://<LOAD_BALANCER_DNS>:6443 \
  --token=<CLUSTER_TOKEN>
```

#### 配置私有镜像仓库

```bash
# 创建 registries.yaml
cat > /etc/rancher/k3s/registries.yaml << EOF
mirrors:
  "registry.example.com":
    endpoint:
      - "https://registry.example.com"
configs:
  "registry.example.com":
    auth:
      username: admin
      password: Harbor12345
EOF

# 重启 k3s
systemctl restart k3s
```

#### k3s 常用命令

```bash
# 查看节点
k3s kubectl get nodes

# 查看所有 pod
k3s kubectl get pod -A

# 查看服务
k3s crictl ps

# 停止/启动
systemctl stop k3s-agent
systemctl start k3s-agent
```

---

## 2. 集群升级流程

### 2.1 升级策略概述

Kubernetes 集群升级需要遵循以下原则：

1. **版本跨度**：只能逐个小版本升级（如 1.27 -> 1.28），不能跨版本
2. **控制平面优先**：先升级控制平面，再升级工作节点
3. **滚动升级**：工作节点采用滚动方式升级，保证服务连续性
4. **备份配置**：升级前务必备份 etcd 数据和重要配置

### 2.2 使用 kubeadm 升级集群

#### 升级控制平面

**1. 升级 kubeadm**

```bash
# 查看可用版本
yum list --showduplicates kubeadm --disableexcludes=kubernetes

# 升级 kubeadm
yum install -y kubeadm-1.28.1-0

# 验证 kubeadm 版本
kubeadm version
```

**2. 执行升级计划**

```bash
kubeadm upgrade plan v1.28.1
```

输出示例：

```
[upgrade/config] Making sure the configuration is correct:
[upgrade/config] Reading configuration from the cluster...
[upgrade/config] FYI: You can look at this config file with 'kubectl -n kube-system get cm kubeadm-config -o yaml'
[upgrade/plan] Computing the minimum number of nodes required: 3
Components that need to be upgraded:
- kube-apiserver: v1.27.0 -> v1.28.1
- kube-controller-manager: v1.27.0 -> v1.28.1
- kube-scheduler: v1.27.0 -> v1.28.1
- kube-proxy: v1.27.0 -> v1.28.1
- CoreDNS: v1.10.1 -> v1.11.1
```

**3. 执行升级**

```bash
# 升级控制平面组件
kubeadm upgrade apply v1.28.1

# 如果有多个控制平面节点，依次在其他节点执行
kubeadm upgrade node
```

**4. 升级 kubelet 和 kubectl**

```bash
yum install -y kubelet-1.28.1-0 kubectl-1.28.1-0

# 重启 kubelet
systemctl restart kubelet
```

**5. 验证升级**

```bash
kubectl get nodes
kubectl version --short
```

#### 升级工作节点

**方式一：滚动升级（推荐）**

```bash
# 第一步：封锁节点（标记为不可调度）
kubectl cordon <node-name>

# 第二步：排空节点上的 Pod
kubectl drain <node-name> --ignore-daemonsets --delete-emptydir-data

# 第三步：在工作节点上升级 kubeadm
yum install -y kubeadm-1.28.1-0
kubeadm upgrade node

# 第四步：升级 kubelet
yum install -y kubelet-1.28.1-0
systemctl restart kubelet

# 第五步：解除封锁
kubectl uncordon <node-name>
```

**方式二：批量升级多个节点**

```bash
# 创建升级脚本
cat > upgrade-nodes.sh << 'EOF'
#!/bin/bash
set -e

NEW_VERSION="1.28.1"
NODES=("worker-1" "worker-2" "worker-3")

for node in "${NODES[@]}"; do
    echo "========== 升级节点: $node =========="
    
    # 封锁节点
    kubectl cordon "$node"
    
    # 排空 Pod
    kubectl drain "$node" --ignore-daemonsets --delete-emptydir-data --force
    
    # SSH 到节点执行升级（需要配置 SSH 免密登录）
    ssh "$node" "yum install -y kubeadm-${NEW_VERSION}-0 kubelet-${NEW_VERSION}-0"
    ssh "$node" "kubeadm upgrade node"
    ssh "$node" "systemctl restart kubelet"
    
    # 解除封锁
    kubectl uncordon "$node"
    
    echo "节点 $node 升级完成"
done
EOF

chmod +x upgrade-nodes.sh
./upgrade-nodes.sh
```

### 2.3 使用 k3s 升级

```bash
# 查看当前版本
k3s --version

# 升级到指定版本
curl -sfL https://get.k3s.io | INSTALL_K3S_VERSION=v1.28.1-k3s1 sh -

# 或升级到最新版本
curl -sfL https://get.k3s.io | sh -
```

---

## 3. etcd 备份与恢复

etcd 是 Kubernetes 的核心数据存储，所有集群状态都存储在其中。

### 3.1 etcd 基础知识

- **默认端口**：2379（客户端通信）、2380（节点间通信）
- **数据目录**：/var/lib/etcd
- **备份文件格式**：snap（快照）和 wal（预写日志）

### 3.2 备份 etcd

#### 方法一：使用 etcdctl 命令行工具

```bash
# 安装 etcdctl
ETCD_VERSION=$(kubectl get pod -n kube-system -o jsonpath='{.items[0].spec.containers[0].image}' | cut -d':' -f2 | sed 's/.*v//')
curl -L https://github.com/etcd-io/etcd/releases/download/v${ETCD_VERSION}/etcd-v${ETCD_VERSION}-linux-amd64.tar.gz -o /tmp/etcd.tar.gz
tar xzf /tmp/etcd.tar.gz -C /tmp
sudo mv /tmp/etcd-v${ETCD_VERSION}-linux-amd64/etcdctl /usr/local/bin/

# 设置环境变量
export ETCDCTL_API=3
export ETCDCTL_CACERT=/etc/kubernetes/pki/etcd/ca.crt
export ETCDCTL_CERT=/etc/kubernetes/pki/etcd/server.crt
export ETCDCTL_KEY=/etc/kubernetes/pki/etcd/server.key

# 创建备份
BACKUP_DIR="/var/backups/etcd"
mkdir -p $BACKUP_DIR
BACKUP_FILE="${BACKUP_DIR}/etcd-snapshot-$(date +%Y%m%d-%H%M%S).db"

etcdctl snapshot save $BACKUP_FILE

# 验证备份
etcdctl snapshot status $BACKUP_FILE --write-backup=$BACKUP_DIR/status.txt
```

#### 方法二：使用 kubeadm 自动备份

kubeadm 会在升级前自动创建 etcd 快照，保存在 `/etc/kubernetes/tmp/` 目录。

```bash
# 查看备份文件
ls -la /etc/kubernetes/tmp/

# 备份路径：/etc/kubernetes/tmp/kubeadm-backup-*
```

#### 方法三：自动备份脚本

```bash
cat > /usr/local/bin/etcd-backup.sh << 'EOF'
#!/bin/bash

# 配置
ETCD_ENDPOINTS="https://127.0.0.1:2379"
BACKUP_DIR="/var/backups/etcd"
RETENTION_DAYS=7

# 创建备份目录
mkdir -p $BACKUP_DIR

# 生成备份文件名
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/etcd-snapshot-${TIMESTAMP}.db"

# 设置环境变量
export ETCDCTL_API=3
export ETCDCTL_CACERT=/etc/kubernetes/pki/etcd/ca.crt
export ETCDCTL_CERT=/etc/kubernetes/pki/etcd/server.crt
export ETCDCTL_KEY=/etc/kubernetes/pki/etcd/server.key

# 执行备份
echo "[$(date)] 开始备份 etcd..."
etcdctl snapshot save $BACKUP_FILE

if [ $? -eq 0 ]; then
    echo "[$(date)] etcd 备份成功: $BACKUP_FILE"
    
    # 压缩备份
    gzip $BACKUP_FILE
    echo "[$(date)] 备份已压缩: ${BACKUP_FILE}.gz"
    
    # 删除过期备份
    find $BACKUP_DIR -name "etcd-snapshot-*.db.gz" -mtime +$RETENTION_DAYS -delete
    echo "[$(date)] 已清理超过 ${RETENTION_DAYS} 天的备份"
else
    echo "[$(date)] etcd 备份失败!"
    exit 1
fi
EOF

chmod +x /usr/local/bin/etcd-backup.sh

# 配置 cron 任务（每天凌晨 2 点执行）
echo "0 2 * * * root /usr/local/bin/etcd-backup.sh >> /var/log/etcd-backup.log 2>&1" >> /etc/crontab
```

### 3.3 恢复 etcd

#### 恢复步骤

```bash
# 1. 停止相关服务
systemctl stop kube-apiserver
systemctl stop kubelet

# 2. 备份当前 etcd 数据目录
mv /var/lib/etcd /var/lib/etcd.bak

# 3. 创建新的 etcd 数据目录
mkdir -p /var/lib/etcd

# 4. 设置环境变量
export ETCDCTL_API=3
export ETCDCTL_CACERT=/etc/kubernetes/pki/etcd/ca.crt
export ETCDCTL_CERT=/etc/kubernetes/pki/etcd/server.crt
export ETCDCTL_KEY=/etc/kubernetes/pki/etcd/server.key

# 5. 执行恢复
# 方式一：从压缩包恢复
etcdctl snapshot restore /var/backups/etcd/etcd-snapshot-20240101-020000.db \
  --data-dir=/var/lib/etcd \
  --initial-cluster=k8s-master-1=https://192.168.1.100:2380 \
  --initial-cluster-token=etcd-cluster \
  --initial-advertise-peer-urls=https://192.168.1.100:2380

# 方式二：从 kubeadm 备份恢复
etcdctl snapshot restore /etc/kubernetes/tmp/kubeadm-backup-*/etcd-snapshot.db \
  --data-dir=/var/lib/etcd

# 6. 设置正确的权限
chown -R etcd:etcd /var/lib/etcd

# 7. 启动 etcd
systemctl start etcd

# 8. 验证恢复
etcdctl --endpoints=https://127.0.0.1:2379 endpoint health

# 9. 重启其他服务
systemctl start kubelet
systemctl start kube-apiserver

# 10. 验证集群状态
kubectl get nodes
kubectl get pods -n kube-system
```

### 3.4 etcd 快照管理

```bash
# 列出所有备份
ls -lh /var/backups/etcd/

# 查看备份状态
etcdctl snapshot status /var/backups/etcd/etcd-snapshot-20240101-020000.db

# 验证备份完整性
etcdctl snapshot status /var/backups/etcd/etcd-snapshot-20240101-020000.db --write-backup=/tmp/verify.txt
```

---

## 4. 监控方案

### 4.1 Prometheus + Grafana

#### 架构概述

```
+----------------+     +------------------+     +-------------+
|  Kubernetes    | --> |   Prometheus     | --> |  Grafana    |
|  (Metrics)      |     |   (采集/存储)      |     |  (可视化)    |
+----------------+     +------------------+     +-------------+
```

#### 安装 Prometheus（使用 prometheus-operator）

```bash
# 添加 Prometheus Operator Helm 仓库
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# 创建命名空间
kubectl create namespace monitoring

# 安装 kube-prometheus-stack
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --set grafana.adminPassword='PromAdmin123' \
  --set prometheus.prometheusSpec.retention=30d \
  --set prometheus.prometheusSpec.storageSpec.volumeClaimTemplate.spec.resources.requests.storage=50Gi \
  --set alertmanager.persistentVolume.storageClass=nfs-client

# 查看安装状态
kubectl get pods -n monitoring
```

#### 访问 Prometheus 和 Grafana

```bash
# Prometheus UI（通过 NodePort）
kubectl patch svc prometheus-grafana -n monitoring -p '{"spec":{"type":"NodePort"}}'
kubectl get svc prometheus-grafana -n monitoring

# Prometheus UI
kubectl patch svc prometheus-prometheus -n monitoring -p '{"spec":{"type":"NodePort"}}'

# 访问地址
# Prometheus: http://<node-ip>:9090
# Grafana: http://<node-ip>:3000
# AlertManager: http://<node-ip>:9093
```

#### Grafana 配置

Grafana 默认配置：
- **用户名**：admin
- **密码**：PromAdmin123（或通过 helm 安装时设置）

常用仪表板 ID：
- Kubernetes Cluster Monitoring: 6417
- Kubernetes Deployment StatefulSet DaemonSet ReplicaSet: 914
- Node Exporter Full: 1860
- Prometheus Stats: 2

#### 自定义监控配置示例

```yaml
# prometheus-rule.yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: custom-alerts
  namespace: monitoring
  labels:
    app: prometheus
spec:
  groups:
    - name: custom-alerts
      rules:
        - alert: HighCPUUsage
          expr: sum(rate(container_cpu_usage_seconds_total[5m])) by (pod) > 0.8
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Pod {{ $labels.pod }} CPU 使用率过高"
            description: "Pod {{ $labels.pod }} CPU 使用率超过 80% 已持续 5 分钟"
        
        - alert: HighMemoryUsage
          expr: sum(container_memory_working_set_bytes) by (pod) / sum(container_spec_memory_limit_bytes) by (pod) > 0.8
          for: 5m
          labels:
            severity: warning
          annotations:
            summary: "Pod {{ $labels.pod }} 内存使用率过高"
            description: "Pod {{ $labels.pod }} 内存使用率超过 80% 已持续 5 分钟"
        
        - alert: PodNotReady
          expr: sum by (pod) (kube_pod_status_ready{condition="true"}) == 0
          for: 10m
          labels:
            severity: critical
          annotations:
            summary: "Pod {{ $labels.pod }} 未就绪"
            description: "Pod {{ $labels.pod }} 未就绪已超过 10 分钟"
```

```bash
# 应用自定义告警规则
kubectl apply -f prometheus-rule.yaml
```

---

### 4.2 Kubernetes Dashboard

#### 安装 Dashboard

```bash
# 方式一：kubectl 部署
kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml

# 方式二：Helm 部署（推荐）
helm repo add kubernetes-dashboard https://kubernetes.github.io/dashboard
helm repo update
helm install kubernetes-dashboard kubernetes-dashboard/kubernetes-dashboard \
  --namespace kube-system \
  --set=service.type=NodePort \
  --set=protocolHttp=true
```

#### 创建 Admin Service Account

```bash
cat > admin-user.yaml << 'EOF'
apiVersion: v1
kind: ServiceAccount
metadata:
  name: admin-user
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: admin-user
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-admin
subjects:
- kind: ServiceAccount
  name: admin-user
  namespace: kube-system
EOF

kubectl apply -f admin-user.yaml

# 获取 Token
kubectl -n kube-system create token admin-user

# 或使用 Secret 获取 Token
kubectl get secret -n kube-system | grep admin-user
kubectl describe secret <secret-name> -n kube-system
```

#### 访问 Dashboard

```bash
# 查看 Dashboard 服务
kubectl get svc -n kube-system | grep kubernetes-dashboard

# 使用 kubectl proxy 访问（不推荐生产环境）
kubectl proxy

# 访问地址
# http://localhost:8001/api/v1/namespaces/kube-system/services/https:kubernetes-dashboard:/proxy/
```

---

### 4.3 Metrics Server

Metrics Server 是 Kubernetes 核心组件，提供集群资源指标。

#### 安装 Metrics Server

```bash
# 方式一：kubectl 部署
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 方式二：修改部署配置（解决证书问题）
cat > metrics-server.yaml << 'EOF'
apiVersion: apps/v1
kind: Deployment
metadata:
  name: metrics-server
  namespace: kube-system
spec:
  template:
    spec:
      containers:
      - args:
        - --cert-dir=/tmp
        - --secure-port=4443
        - --kubelet-insecure-tls=true
        - --kubelet-preferred-address-types=InternalIP
        name: metrics-server
        image: registry.k8s.io/metrics-server/metrics-server:v0.6.4
EOF

kubectl apply -f metrics-server.yaml
```

#### 验证安装

```bash
# 等待 Metrics Server 就绪
kubectl wait --for=condition=ready pod -l k8s-app=metrics-server -n kube-system --timeout=60s

# 查看节点指标
kubectl top nodes

# 查看 Pod 指标
kubectl top pods -A

# 查看特定命名空间的 Pod 指标
kubectl top pods -n <namespace>
```

---

## 5. 日志收集方案

### 5.1 ELK (Elasticsearch + Logstash + Kibana)

#### 架构

```
Pods --> Filebeat --> Logstash --> Elasticsearch <-- Kibana
                              |
                           Pipeline
```

#### 安装 ELK Stack

```bash
# 添加 Elastic Helm 仓库
helm repo add elastic https://helm.elastic.co
helm repo update

# 创建命名空间
kubectl create namespace elk

# 安装 Elasticsearch
helm install elasticsearch elastic/elasticsearch \
  --namespace elk \
  --set replicas=3 \
  --set minimumMasterNodes=2 \
  --set resources.requests.memory=2Gi \
  --set resources.limits.memory=4Gi \
  --set persistence.enabled=true \
  --set persistence.size=100Gi

# 安装 Logstash
helm install logstash elastic/logstash \
  --namespace elk \
  --set replicas=2 \
  --set resources.requests.memory=1Gi \
  --set resources.limits.memory=2Gi

# 安装 Kibana
helm install kibana elastic/kibana \
  --namespace elk \
  --set replicas=1 \
  --set service.type=NodePort
```

#### 部署 Filebeat 收集 Kubernetes 日志

```yaml
# filebeat-kubernetes.yaml
apiVersion: beat.k8s.elastic.co/v1beta1
kind: Beat
metadata:
  name: filebeat
  namespace: elk
spec:
  type: filebeat
  version: 8.11.0
  elasticsearchRef:
    name: elasticsearch
  kibanaRef:
    name: kibana
  config:
    filebeat.inputs:
    - type: container
      paths:
      - /var/log/containers/*.log
      processors:
      - add_kubernetes_metadata:
          host: ${NODE_NAME}
          matchers:
          - logs_path:
              logs_path: /var/log/containers/
    output.logstash:
      hosts: ["logstash-logstash.elk.svc:5044"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: filebeat
rules:
- apiGroups: [""]
  resources:
  - namespaces
  - pods
  verbs:
  - get
  - watch
  - list
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: filebeat
  namespace: elk
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: filebeat
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: filebeat
subjects:
- kind: ServiceAccount
  name: filebeat
  namespace: elk
```

```bash
kubectl apply -f filebeat-kubernetes.yaml
```

---

### 5.2 EFK (Elasticsearch + Fluentd + Kibana)

EFK 是 Kubernetes 日志收集的标准方案之一，Fluentd 比 Logstash 更轻量。

#### 安装 EFK Stack

```bash
# 创建命名空间
kubectl create namespace efk

# 安装 Elasticsearch
helm install elasticsearch elastic/elasticsearch \
  --namespace efk \
  --set replicas=3

# 安装 Kibana
helm install kibana elastic/kibana \
  --namespace efk \
  --set service.type=NodePort
```

#### 部署 Fluentd

```yaml
# fluentd.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: fluentd
  namespace: efk
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: fluentd
rules:
- apiGroups: [""]
  resources:
  - namespaces
  - pods
  - pods/log
  verbs:
  - get
  - list
  - watch
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: fluentd
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: fluentd
subjects:
- kind: ServiceAccount
  name: fluentd
  namespace: efk
---
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd
  namespace: efk
  labels:
    k8s-app: fluentd
spec:
  selector:
    matchLabels:
      k8s-app: fluentd
  template:
    metadata:
      labels:
        k8s-app: fluentd
    spec:
      serviceAccountName: fluentd
      tolerations:
      - key: node-role.kubernetes.io/master
        effect: NoSchedule
      containers:
      - name: fluentd
        image: fluent/fluentd-kubernetes-daemonset:v1.16-debian-elasticsearch-1
        env:
          - name: FLUENT_ELASTICSEARCH_HOST
            value: "elasticsearch.efk.svc"
          - name: FLUENT_ELASTICSEARCH_PORT
            value: "9200"
          - name: FLUENT_ELASTICSEARCH_SCHEME
            value: "http"
          - name: FLUENT_ELASTICSEARCH_USER
            value: "elastic"
          - name: FLUENT_ELASTICSEARCH_PASSWORD
            valueFrom:
              secretKeyRef:
                name: elasticsearch-master-credentials
                key: password
        resources:
          limits:
            memory: 512Mi
          requests:
            cpu: 100m
            memory: 200Mi
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
```

```bash
kubectl apply -f fluentd.yaml
```

---

### 5.3 Loki（推荐的日志方案）

Loki 是 Grafana 开发的日志聚合系统，与 Prometheus 风格一致，成本更低。

#### 安装 Loki

```bash
# 添加 Grafana Helm 仓库
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# 创建命名空间
kubectl create namespace loki

# 安装 Loki
helm install loki grafana/loki \
  --namespace loki \
  --set replicas=2 \
  --set persistence.enabled=true \
  --set persistence.size=50Gi \
  --set prometheus.enabled=true \
  --set grafana.enabled=true

# 安装 Promtail（ Loki 的日志收集代理）
helm install promtail grafana/promtail \
  --namespace loki \
  --set config.loki.address=http://loki:3100/loki/api/v1/push
```

#### 访问 Loki 和 Grafana

```bash
# 获取 Grafana 密码
kubectl get secret --namespace loki loki-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo

# 查看服务
kubectl get svc -n loki

# 访问 Grafana（NodePort）
# 用户名：admin
# 密码：通过上面命令获取
```

#### Loki 与 Grafana 集成

在 Grafana 中添加 Loki 数据源：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: loki-datasource
  namespace: loki
data:
  loki-datasource.yaml: |
    apiVersion: 1
    datasources:
    - name: Loki
      type: loki
      access: proxy
      url: http://loki:3100
      isDefault: true
      editable: false
```

---

## 6. AutoScaling

### 6.1 HPA (Horizontal Pod Autoscaler)

HPA 根据 CPU、内存等指标自动调整 Pod 副本数。

#### 基本用法

```yaml
# nginx-hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: nginx-hpa
  namespace: default
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: nginx
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
      policies:
      - type: Percent
        value: 10
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 15
      - type: Pods
        value: 4
        periodSeconds: 15
      selectPolicy: Max
```

```bash
# 应用 HPA 配置
kubectl apply -f nginx-hpa.yaml

# 查看 HPA 状态
kubectl get hpa

# 查看 HPA 详细信息
kubectl describe hpa nginx-hpa

# 手动测试扩缩容
kubectl run load-generator --image=busybox -- /bin/sh -c "while true; do wget -q -O- http://nginx; done"
kubectl delete pod load-generator
```

#### 基于自定义指标的 HPA

```yaml
# hpa-custom-metrics.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: nginx-hpa-custom
  namespace: default
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: nginx
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: External
    external:
      metric:
        name: nginx_requests_per_second
        selector:
          matchLabels:
            app: nginx
      target:
        type: AverageValue
        averageValue: "1000"
```

---

### 6.2 VPA (Vertical Pod Autoscaler)

VPA 自动调整 Pod 的资源请求（CPU/内存），适合有状态服务。

#### 安装 VPA

```bash
# 使用 kubectl 部署
kubectl apply -f https://raw.githubusercontent.com/kubernetes/autoscaler/master/vertical-pod-autoscaler/pkg/manifest/vpa.yaml
```

#### VPA 配置

```yaml
# nginx-vpa.yaml
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: nginx-vpa
  namespace: default
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: nginx
  updatePolicy:
    updateMode: "Auto"  # Auto/Off/Initial
  resourcePolicy:
    containerPolicies:
    - containerName: nginx
      minAllowed:
        cpu: 100m
        memory: 128Mi
      maxAllowed:
        cpu: 2
        memory: 2Gi
      controlledResources: ["cpu", "memory"]
```

#### VPA 模式

| 模式 | 说明 |
|------|------|
| Off | 只分析，不应用建议 |
| Initial | 只在 Pod 创建时应用一次 |
| Auto | 自动更新 Pod 资源（会重启 Pod） |

```bash
# 获取 VPA 建议
kubectl get vpa nginx-vpa -o yaml

# 查看建议详情
kubectl describe vpa nginx-vpa
```

---

### 6.3 CA (Cluster Autoscaler)

CA 自动调整集群节点数量以适应工作负载。

#### 云厂商部署

**AWS EKS**

```yaml
# cluster-autoscaler-autodiscover.yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: cluster-autoscaler
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: cluster-autoscaler
rules:
- apiGroups: [""]
  resources: ["events", "configmaps"]
  verbs: ["create", "patch"]
- apiGroups: [""]
  resources: ["configmaps"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["update", "patch"]
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["list", "watch"]
- apiGroups: [""]
  resources: ["services"]
  verbs: ["list", "watch"]
- apiGroups: ["apps"]
  resources: ["deployments"]
  verbs: ["get", "list", "watch", "update"]
- apiGroups: [""]
  resources: ["replicationcontrollers"]
  verbs: ["get", "list", "watch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: cluster-autoscaler
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: cluster-autoscaler
subjects:
- kind: ServiceAccount
  name: cluster-autoscaler
  namespace: kube-system
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cluster-autoscaler
  namespace: kube-system
  labels:
    app: cluster-autoscaler
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cluster-autoscaler
  template:
    metadata:
      labels:
        app: cluster-autoscaler
    spec:
      serviceAccountName: cluster-autoscaler
      containers:
      - image: registry.k8s.io/autoscaling/cluster-autoscaler:v1.28.0
        name: cluster-autoscaler
        resources:
          limits:
            cpu: 100m
            memory: 300Mi
          requests:
            cpu: 100m
            memory: 300Mi
        command:
        - /bin/sh
        - -c
        - |
          cluster-autoscaler
          --cloud-provider=aws
          --aws-use-static-instance-list=true
          --nodes=1:10:nodes.my-node-group
          --scale-down-delay-after-add=3m
          --scale-down-unneeded-time=5m
          --v=4
        env:
        - name: AWS_REGION
          value: us-west-2
```

#### Cluster Autoscaler 配置参数

```bash
# 常用配置参数
--nodes=min:max:nodegroup     # 节点组规模
--scale-down-delay-after-add  # 扩容后等待多久开始缩容
--scale-down-unneeded-time    # 节点空闲多久开始缩容
--scale-down-utilization-threshold  # 节点利用率阈值
--scan-interval              # 扫描间隔
--max-empty-bulk-delete       # 批量删除空节点的最大数量
```

---

## 7. Helm 包管理器

### 7.1 Helm 基础

Helm 是 Kubernetes 的包管理器，用于定义、安装和升级复杂的 Kubernetes 应用。

#### 安装 Helm

```bash
# Linux/macOS
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# macOS 使用 Homebrew
brew install helm

# Windows 使用 Chocolatey
choco install helm

# 验证安装
helm version
```

#### 基本概念

- **Chart**: Helm 包，包含 Kubernetes 资源 YAML 文件
- **Repository**: Chart 存储库
- **Release**: Chart 的运行实例
- **Values**: Chart 配置参数

### 7.2 Helm 仓库管理

```bash
# 添加官方 Helm Hub
helm repo add bitnami https://charts.bitnami.com/bitnami

# 添加阿里云镜像
helm repo add alibaba https://apphub.aliyuncs.com

# 更新仓库索引
helm repo update

# 搜索 Chart
helm search repo nginx
helm search repo bitnami/nginx --versions

# 查看已添加的仓库
helm repo list

# 删除仓库
helm repo remove bitnami

# 清理不需要的仓库
helm repo prune
```

### 7.3 安装应用

```bash
# 安装 Chart
helm install my-nginx bitnami/nginx

# 指定命名空间
helm install my-nginx bitnami/nginx -n nginx --create-namespace

# 覆盖默认值
helm install my-nginx bitnami/nginx \
  --set service.type=NodePort \
  --set replicaCount=3

# 使用 YAML 文件覆盖配置
helm install my-nginx bitnami/nginx -f values.yaml

# 查看安装状态
helm status my-nginx

# 列出所有 Release
helm list
helm list -A
```

### 7.4 自定义 Chart 配置

#### 创建 Chart

```bash
# 创建新 Chart
helm create my-chart

# 查看 Chart 结构
ls -la my-chart/
# Chart.yaml
# values.yaml
# charts/
# templates/
# templates/NOTES.txt
```

#### values.yaml 示例

```yaml
# values.yaml
replicaCount: 3

image:
  repository: nginx
  tag: 1.25
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
    - host: nginx.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: nginx-tls
      hosts:
        - nginx.example.com

resources:
  limits:
    cpu: 500m
    memory: 256Mi
  requests:
    cpu: 100m
    memory: 128Mi

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

#### 模板中使用 values

```yaml
# templates/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Chart.Name }}
  labels:
    app: {{ .Chart.Name }}
spec:
  replicas: {{ .Values.replicaCount }}
  selector:
    matchLabels:
      app: {{ .Chart.Name }}
  template:
    metadata:
      labels:
        app: {{ .Chart.Name }}
    spec:
      containers:
      - name: {{ .Chart.Name }}
        image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
        imagePullPolicy: {{ .Values.image.pullPolicy }}
        ports:
        - name: http
          containerPort: 80
        livenessProbe:
          httpGet:
            path: /
            port: http
        readinessProbe:
          httpGet:
            path: /
            port: http
        resources:
          {{- toYaml .Values.resources | nindent 10 }}
```

### 7.5 升级和回滚

```bash
# 升级 Release
helm upgrade my-nginx bitnami/nginx -f values.yaml

# 升级时设置参数
helm upgrade my-nginx bitnami/nginx --set replicaCount=5

# 查看历史版本
helm history my-nginx

# 回滚到指定版本
helm rollback my-nginx 1

# 回滚到指定版本并等待完成
helm rollback my-nginx 1 --wait

# 原子性回滚（失败自动回退）
helm rollback my-nginx 1 --wait --cleanup-on-fail
```

### 7.6 包管理操作

```bash
# 打包 Chart
helm package my-chart

# 验证 Chart
helm lint my-chart

# 模板渲染（查看最终 YAML，不实际安装）
helm template my-release my-chart -f values.yaml

# 调试模式（dry-run）
helm install my-nginx bitnami/nginx --dry-run --debug

# 卸载 Release
helm uninstall my-nginx

# 卸载 Release（保留历史）
helm uninstall my-nginx --keep-history
```

### 7.7 Helm 最佳实践

```yaml
# 生产环境建议使用的 values 文件结构
# values-production.yaml

replicaCount: 3

image:
  repository: your-registry.com/myapp
  tag: v1.2.3
  pullPolicy: IfNotPresent

service:
  type: ClusterIP

resources:
  limits:
    cpu: "1"
    memory: 1Gi
  requests:
    cpu: 500m
    memory: 512Mi

autoscaling:
  enabled: true

persistence:
  enabled: true
  size: 10Gi
  storageClass: nfs-client

podDisruptionBudget:
  enabled: true
  minAvailable: 2
```

---

## 8. Operator 模式

### 8.1 Operator 概念

Operator 是 Kubernetes 的扩展模式，用于自动化管理有状态应用。它通过自定义资源（CRD）和自定义控制器来实现。

#### 核心组件

- **CRD (Custom Resource Definition)**: 定义新的资源类型
- **Controller**: 监听资源变化，执行相应操作
- **Webhook**: 验证和修改资源

### 8.2 使用 Operator（以 cert-manager 为例）

#### 什么是 cert-manager

cert-manager 是自动化管理 TLS 证书的 Operator，支持 Let's Encrypt、Venafi 等证书颁发机构。

#### 安装 cert-manager

```bash
# 添加 Jetstack Helm 仓库
helm repo add jetstack https://charts.jetstack.io
helm repo update

# 安装 cert-manager
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager \
  --create-namespace \
  --set installCRDs=true \
  --set prometheus.enabled=true \
  --set webhook.timeoutSeconds=30
```

#### 配置 Let's Encrypt 证书

```yaml
# issuer-letsencrypt.yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
---
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: letsencrypt-staging
  namespace: default
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-staging
    solvers:
    - http01:
        ingress:
          class: nginx
```

#### 申请证书

```yaml
# certificate.yaml
apiVersion: cert-manager.io/v1
kind: Certificate
metadata:
  name: example-tls
  namespace: default
spec:
  secretName: example-tls
  issuerRef:
    name: letsencrypt-prod
    kind: ClusterIssuer
  dnsNames:
  - "*.example.com"
  - example.com
  duration: 2160h # 90 days
  renewBefore: 360h # 15 days
```

```bash
# 应用配置
kubectl apply -f issuer-letsencrypt.yaml
kubectl apply -f certificate.yaml

# 查看证书状态
kubectl get certificate
kubectl describe certificate example-tls

# 查看生成的 Secret
kubectl get secret example-tls
```

### 8.3 常用 Operator 推荐

| Operator | 用途 | GitHub |
|----------|------|--------|
| cert-manager | TLS 证书管理 | jetstack/cert-manager |
| Prometheus Operator | 监控 | prometheus-operator/prometheus-operator |
| Argo CD | GitOps 持续交付 | argoproj/argo-cd |
| Istio Operator | 服务网格 | istio/istio |
| Strimzi | Kafka 运营 | strimzi/strimzi-kafka-operator |
| TiDB Operator | TiDB 数据库 | pingcap/tidb-operator |
| Vault Operator | HashiCorp Vault | bank-vaults/vault-operator |

### 8.4 Operator Framework 简介

使用 kubebuilder 构建 Operator：

```bash
# 安装 kubebuilder
os=$(go env GOOS)
arch=$(go env GOARCH)
curl -L -O https://github.com/kubernetes-sigs/kubebuilder/releases/download/v3.11.0/kubebuilder_${os}_${arch}
mv kubebuilder_${os}_${arch} /usr/local/kubebuilder

# 创建新项目
kubebuilder init --domain myorg.com --repo github.com/myorg/myoperator

# 创建 API（CRD 和 Controller）
kubebuilder create api --group apps --version v1 --kind MyApp

# 实现 Controller 逻辑
# 编辑 controllers/myapp_controller.go

# 部署 Operator
make install
make deploy

# 测试
kubectl apply -f config/samples/apps_v1_myapp.yaml
kubectl get myapp
```

### 8.5 编写简单的 Operator（示例）

#### 定义 CRD

```yaml
# config/crd/bases/apps.myorg.com_myapps.yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: myapps.apps.myorg.com
spec:
  group: apps.myorg.com
  names:
    kind: MyApp
    listKind: MyAppList
    plural: myapps
    singular: myapp
  scope: Namespaced
  versions:
  - name: v1
    served: true
    storage: true
    schema:
      openAPIV3Schema:
        type: object
        properties:
          spec:
            type: object
            properties:
              image:
                type: string
              replicas:
                type: integer
              port:
                type: integer
```

#### 编写 Controller

```go
// controllers/myapp_controller.go
package controllers

import (
    appsv1 "k8s.io/api/apps/v1"
    corev1 "k8s.io/api/core/v1"
    "context"
    "fmt"
    "reflect"

    "github.com/go-logr/logr"
    "k8s.io/apimachinery/pkg/runtime"
    ctrl "sigs.k8s.io/controller-runtime"
    "sigs.k8s.io/controller-runtime/pkg/client"

    appsmyorgcomv1 "github.com/myorg/myoperator/api/v1"
)

type MyAppReconciler struct {
    client.Client
    Scheme *runtime.Scheme
    Log    logr.Logger
}

func (r *MyAppReconciler) Reconcile(ctx context.Context, req ctrl.Request) (ctrl.Result, error) {
    log := r.Log.WithValues("myapp", req.NamespacedName)

    // 获取 MyApp 资源
    myapp := &appsmyorgcomv1.MyApp{}
    if err := r.Get(ctx, req.NamespacedName, myapp); err != nil {
        return ctrl.Result{}, client.IgnoreNotFound(err)
    }

    // 定义 Deployment
    deploy := &appsv1.Deployment{}
    deploy.Name = myapp.Name
    deploy.Namespace = myapp.Namespace

    // 设置 OwnerReference
    if err := ctrl.SetControllerReference(myapp, deploy, r.Scheme); err != nil {
        return ctrl.Result{}, err
    }

    // 检查 Deployment 是否存在
    found := &appsv1.Deployment{}
    err := r.Get(ctx, req.NamespacedName, found)
    if err != nil && client.IgnoreNotFound(err) != nil {
        return ctrl.Result{}, err
    }

    if client.IgnoreNotFound(err) == nil {
        // Deployment 已存在，检查是否需要更新
        if !reflect.DeepEqual(deploy.Spec, found.Spec) {
            found.Spec = deploy.Spec
            if err := r.Update(ctx, found); err != nil {
                return ctrl.Result{}, err
            }
            log.Info("Deployment updated")
        }
    } else {
        // 创建 Deployment
        if err := r.Create(ctx, deploy); err != nil {
            return ctrl.Result{}, err
        }
        log.Info("Deployment created")
    }

    return ctrl.Result{}, nil
}

func (r *MyAppReconciler) SetupWithManager(mgr ctrl.Manager) error {
    return ctrl.NewControllerManagedBy(mgr).
        For(&appsmyorgcomv1.MyApp{}).
        Owns(&appsv1.Deployment{}).
        Complete(r)
}
```

---

## 总结

本章介绍了 Kubernetes 运维与监控的核心内容：

1. **集群部署方式**：kubeadm（生产环境推荐）、kops（AWS 云）、minikube（本地开发）、k3s（轻量级）
2. **集群升级**：控制平面优先，滚动升级工作节点
3. **etcd 备份与恢复**：定期快照备份是数据安全的关键
4. **监控方案**：Prometheus + Grafana 是主流选择，配合 Metrics Server 提供资源指标
5. **日志收集**：ELK/EFK 适合复杂场景，Loki + Grafana 是轻量级方案
6. **AutoScaling**：HPA（水平）、VPA（垂直）、CA（集群）三剑客
7. **Helm**：Kubernetes 包管理器，简化应用部署和管理
8. **Operator 模式**：扩展 Kubernetes 能力，自动化管理有状态应用

掌握这些运维技能，能够有效保障 Kubernetes 集群的稳定运行和高效管理。

---

## 下一步

- 学习 Kubernetes 安全最佳实践
- 了解 Service Mesh（Istio、Linkerd）
- 探索 GitOps 工作流（Argo CD、Flux）
- 研究多集群管理方案
