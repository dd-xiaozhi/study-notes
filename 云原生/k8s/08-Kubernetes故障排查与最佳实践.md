# 第八章：Kubernetes 故障排查与最佳实践

故障排查是 Kubernetes 运维中最关键的技能之一。本章将详细介绍故障排查的方法论、工具、常见问题的解决方案，以及集群的最佳实践。

## 8.1 故障排查方法论

### 8.1.1 排查思路

Kubernetes 故障排查遵循"从外到内、从上到下"的原则：

```
┌─────────────────────────────────────────────────────────────┐
│                     用户请求                                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Ingress/Service (网络层)        检查: endpoints, selector   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Pod (应用层)                  检查: status, logs, events    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Node (基础设施层)              检查: capacity, allocatable  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  etcd / API Server (控制平面)   检查: 集群状态, 证书, 网络    │
└─────────────────────────────────────────────────────────────┘
```

### 8.1.2 排查命令速查表

| 问题类型 | 排查命令 |
|---------|---------|
| Pod 状态异常 | `kubectl get pod -o wide` `kubectl describe pod` |
| 日志查看 | `kubectl logs` `kubectl logs --previous` |
| 临时容器 | `kubectl debug` `kubectl exec` |
| 网络问题 | `kubectl port-forward` `kubectl proxy` |
| 资源瓶颈 | `kubectl top pod` `kubectl top node` |
| 事件追踪 | `kubectl get events --sort-by='.lastTimestamp'` |

---

## 8.2 核心排查工具详解

### 8.2.1 kubectl describe

`kubectl describe` 是排查问题的首选工具，它提供资源的详细描述，包括关联资源和最近事件。

#### 基本语法

```bash
kubectl describe <resource-type> <resource-name> [-n <namespace>]
```

#### 查看 Pod 详情

```bash
# 查看单个 Pod 详细信息
kubectl describe pod nginx-pod

# 查看特定命名空间的 Pod
kubectl describe pod nginx-pod -n default

# 查看所有 Pod（包含所有命名空间）
kubectl describe pod -A
```

**输出解析示例：**

```yaml
Name:             nginx-deployment-7fb96c846b-abc123
Namespace:        default
Priority:         0
Service Account:  default
Node:             node-1/192.168.1.101
Start Time:       Mon, 19 May 2026 10:00:00 +0800
Labels:           app=nginx
                  pod-template-hash=7fb96c846b
Annotations:      <none>
Status:           Running
# ^^^^ 关键：Pod 当前状态

Conditions:
  Type              Status
  PodScheduled      True   # ^^^^ Pod 是否已调度
  Initialized       True   # ^^^^ 初始化容器是否完成
  ContainersReady   True   # ^^^^ 容器是否就绪
  Ready             True   # ^^^^ 是否可以接收流量

Volumes:
  nginx-config:  ConfigMap (r/w, size=1Ki)

Events:                    # ^^^^ 关键：问题线索
  Type     Reason            Age   From             Message
  ----     ------            ----  ----             -------
  Normal   Scheduled         5m    default-scheduler Successfully assigned
  Normal   Pulling           4m    kubelet           Pulling image "nginx:1.25"
  Normal   Pulled            3m    kubelet           Successfully pulled image
  Normal   Created           3m    kubelet           Created container nginx
  Normal   Started           3m    kubelet           Started container nginx
```

#### 查看 Node 详情

```bash
# 查看集群所有 Node 概览
kubectl get nodes -o wide

# 查看特定 Node 详细信息
kubectl describe node node-1

# 输出关键信息：
# - 分配的资源 (Allocated resources)
# - 条件状态 (Conditions) - 重点关注 MemoryPressure, DiskPressure, PIDPressure
# - 事件 (Events)
```

#### 查看 Service 详情

```bash
kubectl describe service nginx-service

# 关键检查点：
# - Endpoints - 是否有匹配的 Pod IP
# - Selector - 是否与 Pod labels 匹配
# - Port/TargetPort - 端口配置是否正确
```

### 8.2.2 kubectl logs

`kubectl logs` 用于查看容器日志，是排查应用问题的核心工具。

#### 基本语法

```bash
kubectl logs <pod-name> [-c <container-name>] [options]
```

#### 常用选项

```bash
# 查看当前日志
kubectl logs nginx-pod

# 查看上一个终止容器的日志（容器重启后排障用）
kubectl logs nginx-pod --previous

# 实时跟踪日志
kubectl logs -f nginx-pod

# 查看最近 100 行
kubectl logs nginx-pod --tail=100

# 查看指定时间范围的日志
kubectl logs nginx-pod --since=1h
kubectl logs nginx-pod --since-time="2026-05-19T10:00:00Z"

# 多容器 Pod：指定容器
kubectl logs multi-container-pod -c sidecar

# 导出日志到文件
kubectl logs nginx-pod > nginx.log

# 管道处理：实时查看包含"ERROR"的日志
kubectl logs -f nginx-pod | grep ERROR
```

#### 完整日志分析示例

```bash
# 查看所有容器日志（包括已终止的）
kubectl logs nginx-pod --all-containers=true

# 查看最近 1 小时的错误日志
kubectl logs nginx-pod --since=1h 2>&1 | grep -i error

# 结合 timestamps 查看
kubectl logs nginx-pod --timestamps

# tail 与 follow 组合使用
kubectl logs nginx-pod -f --tail=50
```

### 8.2.3 kubectl exec

`kubectl exec` 用于在容器中执行命令，是进入容器进行深度排查的利器。

#### 基本语法

```bash
kubectl exec <pod-name> -- <command> [-c <container-name>]
```

#### 常用场景

```bash
# 进入容器交互式 Shell
kubectl exec -it nginx-pod -- /bin/bash

# 使用 sh（如果 bash 不可用）
kubectl exec -it nginx-pod -- /bin/sh

# 执行单个命令
kubectl exec nginx-pod -- ls -la /app

# 执行多个命令
kubectl exec nginx-pod -- sh -c "ls -la /app && cat /app/config.yaml"

# 多容器 Pod：指定容器
kubectl exec -it multi-container-pod -c main-app -- /bin/bash

# 以特定用户执行
kubectl exec nginx-pod -- whoami

# 查看环境变量
kubectl exec nginx-pod -- env

# 检查 DNS 配置
kubectl exec nginx-pod -- cat /etc/resolv.conf

# 测试网络连通性
kubectl exec nginx-pod -- ping -c 3 google.com
kubectl exec nginx-pod -- nc -zv target-service 8080
kubectl exec nginx-pod -- curl -v http://localhost:8080/health
```

#### 实际排障示例

```bash
# 场景：应用启动失败，需要检查配置和依赖

# 1. 检查文件系统
kubectl exec nginx-pod -- ls -la /app/
kubectl exec nginx-pod -- cat /app/config.yaml

# 2. 检查进程状态
kubectl exec nginx-pod -- ps aux

# 3. 检查网络连接
kubectl exec nginx-pod -- netstat -tlnp
kubectl exec nginx-pod -- ss -tlnp

# 4. 检查可用内存/磁盘
kubectl exec nginx-pod -- df -h
kubectl exec nginx-pod -- free -h

# 5. 检查依赖服务
kubectl exec nginx-pod -- nc -zv mysql-service 3306
kubectl exec nginx-pod -- curl -v http://redis-service:6379/ping
```

### 8.2.4 kubectl port-forward

`kubectl port-forward` 用于将本地端口转发到 Pod 端口，用于调试服务而不需要暴露 Service。

#### 基本语法

```bash
kubectl port-forward <pod-name> <local-port>:<pod-port>
```

#### 常用场景

```bash
# 转发单个端口
kubectl port-forward nginx-pod 8080:80

# 转发到不同端口
kubectl port-forward nginx-pod 3000:80

# 转发多个端口（后台运行）
kubectl port-forward nginx-pod 8080:80 8443:443 &

# 访问远程集群的 Pod
kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80

# 转发到 Service
kubectl port-forward svc/redis-service 6379:6379

# 常用场景：本地调试数据库
kubectl port-forward mysql-pod 3306:3306
# 然后本地使用 mysql -h localhost -P 3306 -u root -p
```

#### 持久化端口转发

```bash
# 使用 socat 在后台保持转发
kubectl port-forward nginx-pod 8080:80 &
echo "Pod is forwarded to localhost:8080"

# 或使用 SSH 隧道持久化
ssh -L 8080:localhost:8080 user@k8s-node-1
```

### 8.2.5 kubectl proxy

`kubectl proxy` 启动一个代理服务器，通过 Kubernetes API 访问 Service、Pod 或其他资源。

#### 基本语法

```bash
kubectl proxy [--port=<port>] [--www=<static-files-dir>] [--api-prefix=/]
```

#### 常用场景

```bash
# 启动默认代理（端口 8001）
kubectl proxy

# 指定端口
kubectl proxy --port=8080

# 访问 Dashboard
kubectl proxy
# 然后浏览器访问: http://localhost:8001/api/v1/namespaces/kubernetes-dashboard/services/https:kubernetes-dashboard:/proxy/

# 访问特定 Service
kubectl proxy
# API 调用:
curl http://localhost:8001/api/v1/namespaces/default/services/nginx-service/proxy/

# 直接访问 Pod
curl http://localhost:8001/api/v1/namespaces/default/pods/nginx-pod/proxy/

# 获取完整 API 文档
curl http://localhost:8001/openapi/v3
```

### 8.2.6 kubectl debug（临时容器）

`kubectl debug` 是 Kubernetes 1.23+ 引入的功能，用于创建临时调试容器，是排查问题时最重要的工具之一。

#### 与 kubectl exec 的区别

| 特性 | kubectl exec | kubectl debug |
|-----|-------------|---------------|
| 镜像要求 | 使用原容器镜像 | 可使用专用调试镜像 |
| 工具可用性 | 取决于原镜像 | 使用完整调试工具 |
| 权限 | 受原容器限制 | 可拥有更高权限 |
| 保留性 | 临时命令 | 调试容器可保留 |
| 挂载 | 使用原容器文件系统 | 可挂载新卷 |

#### 常用命令

```bash
# 创建临时调试容器（使用默认镜像）
kubectl debug nginx-pod -it --image=busybox --share-processes --copy-to=debugger

# 使用指定调试镜像
kubectl debug nginx-pod -it --image=Tooltip/debug-tools:latest -- /bin/bash

# 调试 Init Container 问题
kubectl debug nginx-pod -it --container=init-myservice -- /bin/sh

# 为 Pod 创建调试副本（修改配置）
kubectl debug nginx-pod --copy-to=debug-nginx --set-image=*=debug-nginx:1.0

# 查看调试容器
kubectl get pod debug-nginx

# 进入调试容器
kubectl exec -it debug-nginx -c debugger -- /bin/bash

# 调试 Node（需要在 Node 上运行特权容器）
kubectl debug node/node-1 -it --image=busybox -- chroot /host

# 清理调试容器
kubectl delete pod debug-nginx
```

#### 完整调试场景示例

```bash
# 场景：Pod 网络不通，原容器缺少网络工具

# 1. 使用带完整工具的镜像创建调试容器
kubectl debug nginx-pod -it --image=nicolaka/netshoot:latest --share-processes --copy-to=net-debug

# 2. 在调试容器中运行网络诊断
kubectl exec -it net-debug -- /bin/bash

# 调试容器内可用的工具：
# - tcpdump: packet analyzer
# - netstat: network statistics
# - nslookup/dig: DNS lookup
# - ip/ifconfig: network interface config
# - curl/wget: HTTP client
# - ping: ICMP ping
# - traceroute: trace network path
# - telnet: test connections

# 3. 在调试容器中执行
ip addr
ip route
ss -tlnp
tcpdump -i any host 10.0.0.1 -nn
curl -v http://target-service

# 4. 完成后清理
kubectl delete pod net-debug
```

---

## 8.3 常见问题排查

### 8.3.1 Pod 一直处于 Pending 状态

**问题描述：** Pod 创建后一直处于 Pending 状态，不被调度到任何 Node。

**排查步骤：**

```bash
# 1. 查看 Pod 详情和事件
kubectl get pod <pod-name> -o wide
kubectl describe pod <pod-name>

# 重点关注 Events 部分：
# - "FailedScheduling" 表示调度问题
# - "MaybeUnschedulable" 表示资源不足
```

**常见原因及解决方案：**

#### 原因 1：资源不足

```bash
# 检查 Node 资源分配情况
kubectl describe node | grep -A 5 "Allocated resources"

# 检查 ResourceQuota
kubectl get resourcequota -n <namespace>

# 解决方案：增加资源或调整请求/限制
```

**修复示例：**

```yaml
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: app
    resources:
      requests:          # 降低请求值
        memory: "64Mi"
        cpu: "100m"
      limits:            # 保持或降低限制
        memory: "128Mi"
        cpu: "200m"
```

#### 原因 2：节点选择器/亲和性不匹配

```bash
# 检查 Pod 的 nodeSelector 和 affinity
kubectl get pod nginx-pod -o jsonpath='{.spec.nodeSelector}'
kubectl get pod nginx-pod -o jsonpath='{.spec.affinity}'

# 检查 Node 标签
kubectl get nodes --show-labels
```

**修复示例：**

```yaml
# 移除 nodeSelector 以允许调度到任何节点
spec:
  nodeSelector: {}  # 清空或移除此字段

# 或添加正确的标签
kubectl label node node-1 disktype=ssd
```

#### 原因 3：PVC 未绑定

```bash
# 检查 PVC 状态
kubectl get pvc
kubectl describe pvc <pvc-name>

# 常见问题：StorageClass 不存在或 PVC 仍在 Pending
```

**解决方案：**

```bash
# 检查 StorageClass
kubectl get storageclass

# 如果不存在，创建 PVC 前先确认 StorageClass 存在
kubectl get pvc -A

# 或使用默认 StorageClass
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: myclaim
spec:
  storageClassName: ""  # 使用默认
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
```

#### 原因 4：Taints 和 Tolerations 不匹配

```bash
# 检查 Node Taints
kubectl describe node | grep Taints

# 检查 Pod Tolerations
kubectl get pod nginx-pod -o jsonpath='{.spec.tolerations}'
```

**解决方案：**

```bash
# 方案1：移除 Node 上的 Taint
kubectl taint nodes node-1 key=value:NoSchedule-

# 方案2：为 Pod 添加匹配的 Toleration
kubectl patch pod nginx-pod -p '{"spec":{"tolerations":[{"key":"key","operator":"Exists","effect":"NoSchedule"}]}}'
```

### 8.3.2 Pod 一直处于 Waiting 状态

**问题描述：** Pod 处于 Waiting 状态，通常表示镜像拉取失败。

**排查步骤：**

```bash
kubectl describe pod <pod-name>

# 查看 Events 中的 "Waiting" 原因：
# - "ContainerCreating" - 容器创建中
# - "ErrImagePull" - 镜像拉取失败
# - "ImagePullBackOff" - 镜像拉取后退回重试
```

**常见原因及解决方案：**

#### 原因 1：镜像名称错误

```bash
# 检查镜像配置
kubectl get pod nginx-pod -o jsonpath='{.spec.containers[*].image}'

# 验证镜像是否可访问
docker pull <image-name>
```

**解决方案：**

```yaml
# 修正镜像名称
spec:
  containers:
  - name: nginx
    image: nginx:1.25.3  # 使用完整镜像地址
```

#### 原因 2：镜像仓库认证失败

```bash
# 检查 Secret 是否存在
kubectl get secret -n <namespace>

# 检查 ServiceAccount 是否关联了正确的 Secret
kubectl describe serviceaccount default
```

**解决方案：**

```bash
# 创建 Docker Registry Secret
kubectl create secret docker-registry my-registry-secret \
  --docker-server=https://index.docker.io/v1/ \
  --docker-username=<username> \
  --docker-password=<password> \
  --docker-email=<email>

# 在 Pod 中引用
kubectl patch serviceaccount default -p '{"imagePullSecrets":[{"name":"my-registry-secret"}]}'

# 或在 Pod spec 中直接指定
```

```yaml
spec:
  imagePullSecrets:
  - name: my-registry-secret
```

#### 原因 3：私有仓库网络问题

```bash
# 检查是否有网络策略阻止
kubectl get networkpolicy -n <namespace>

# 检查 DNS 解析
kubectl exec nginx-pod -- nslookup registry.example.com
```

#### 原因 4：镜像标签 latest 导致的问题

```bash
# Kubernetes 默认不缓存标签为 latest 的镜像
# 解决方案：使用特定版本标签
```

### 8.3.3 Pod 一直处于 ImagePullBackOff

**问题描述：** Kubernetes 尝试拉取镜像失败后进入指数退避重试状态。

**排查步骤：**

```bash
kubectl describe pod <pod-name>

# Events 示例：
# Type     Reason                 Age                  From
# ----     ------                 ----                 ----
# Normal   Pulling                4m25s                kubelet
# Warning  Failed                 3m58s                kubelet
# Error    ErrImagePull           3m58s                kubelet
# Warning  BackOff                58s                  kubelet
# Reason:  ImagePullBackOff

# 查看详细错误信息
kubectl get event --field-selector involvedObject.name=<pod-name> --sort-by='.lastTimestamp'
```

**常见原因及解决方案：**

#### 原因 1：镜像不存在

```bash
# 本地验证镜像
docker images | grep <image-name>

# 在 Node 上验证（如果可以 SSH）
ssh node-1 "docker pull <image-name>"
```

**解决方案：**

```bash
# 拉取正确镜像到本地节点
docker pull <correct-image-name>
docker tag <correct-image-name> <wrong-image-name>

# 或更新 Pod 配置使用正确镜像
kubectl set image pod/nginx nginx=<correct-image>
```

#### 原因 2：镜像仓库访问限制

```bash
# 如果使用私有仓库，检查认证
kubectl get secret <secret-name> -o jsonpath='{.data.\.dockerconfigjson}' | base64 -d
```

#### 原因 3：节点 kubelet 配置问题

```bash
# SSH 到 Node 检查 kubelet 日志
ssh node-1 "journalctl -u kubelet -n 100"

# 检查 /etc/containerd/config.toml 或 /etc/docker/daemon.json
```

### 8.3.4 Pod 一直处于 CrashLoopBackOff

**问题描述：** 容器启动后立即崩溃，Kubernetes 尝试重启但持续失败。

**排查步骤：**

```bash
# 1. 查看 Pod 状态和重启次数
kubectl get pod -o wide

# 输出示例：
# NAME          READY   STATUS              RESTARTS   AGE
# nginx-pod     0/1     CrashLoopBackOff    5          2m

# 2. 查看详细日志
kubectl logs nginx-pod --previous

# 3. 查看事件
kubectl describe pod nginx-pod
```

**常见原因及解决方案：**

#### 原因 1：应用启动命令错误

```bash
# 检查 command 和 args 配置
kubectl get pod nginx-pod -o jsonpath='{.spec.containers[*].command}'
kubectl get pod nginx-pod -o jsonpath='{.spec.containers[*].args}'
```

**解决方案：**

```yaml
# 修正启动命令
spec:
  containers:
  - name: app
    command: ["python", "app.py"]
    args: ["--config", "/config/settings.yaml"]
```

#### 原因 2：配置文件错误或缺失

```bash
# 检查 ConfigMap 和 Secret 挂载
kubectl describe pod nginx-pod | grep -A 10 "Volumes"

# 验证配置文件内容
kubectl exec nginx-pod -- cat /config/app.conf
```

#### 原因 3：依赖服务不可用

```bash
# 检查依赖的 Service
kubectl get svc -n <namespace>

# 测试依赖连接
kubectl exec nginx-pod -- nc -zv dependency-service 8080
kubectl exec nginx-pod -- curl http://dependency-service:8080/health
```

**解决方案：** 添加健康检查和启动延迟，确保依赖服务就绪

```yaml
spec:
  containers:
  - name: app
    readinessProbe:
      httpGet:
        path: /health
        port: 8080
      initialDelaySeconds: 10
      periodSeconds: 5
    initContainers:
    - name: wait-for-dependencies
      image: busybox:1.36
      command: ['sh', '-c', 'until nc -zv dependency-service 8080; do echo waiting; sleep 2; done']
```

#### 原因 4：权限问题

```bash
# 检查容器运行用户
kubectl exec nginx-pod -- id

# 检查文件系统权限
kubectl exec nginx-pod -- ls -la /app
kubectl exec nginx-pod -- ls -la /var/log
```

**解决方案：**

```yaml
# 设置合适的用户或挂载必要卷
spec:
  securityContext:
    runAsUser: 1000
    runAsGroup: 1000
    fsGroup: 1000
  containers:
  - name: app
    securityContext:
      readOnlyRootFilesystem: true
    volumeMounts:
    - name: app-data
      mountPath: /app/data
    - name: tmp
      mountPath: /tmp
```

### 8.3.5 Service 无法访问

**问题描述：** Service 创建成功但无法访问。

**排查步骤：**

```bash
# 1. 确认 Service 存在
kubectl get svc -A

# 2. 查看 Service 详情
kubectl describe svc <service-name>

# 3. 检查 Endpoints
kubectl get endpoints <service-name>

# 4. 测试连通性
kubectl run test --rm -it --image=busybox -- wget -qO- http://<service-name>:<port>
```

#### 常见问题 1：Endpoints 为空

```bash
# 检查 Endpoints
kubectl get endpoints nginx-service

# 输出：NAME            ENDPOINTS
#      nginx-service   <none>

# 检查 Pod 是否匹配 Service Selector
kubectl describe svc nginx-service | grep -A 3 "Selector"

# 列出带有 selector 标签的 Pod
kubectl get pods -l app=nginx --show-labels
```

**解决方案：**

```bash
# 确保 Pod 标签匹配 Service Selector
kubectl label pod nginx-pod app=nginx --overwrite

# 或修改 Service Selector 匹配现有 Pod
kubectl patch svc nginx-service -p '{"spec":{"selector":{"app":"nginx"}}}'
```

#### 常见问题 2：端口配置错误

```bash
# 检查 Service 端口配置
kubectl get svc nginx-service -o yaml

# 常见错误：
# - Service port 与容器端口不匹配
# - targetPort 类型错误（字符串 vs 数字）
```

**解决方案：**

```yaml
# 正确配置示例
apiVersion: v1
kind: Service
metadata:
  name: nginx-service
spec:
  selector:
    app: nginx
  ports:
  - name: http
    protocol: TCP
    port: 80          # Service 暴露的端口
    targetPort: 8080  # 容器监听的端口
  - name: https
    protocol: TCP
    port: 443
    targetPort: 8443
```

#### 常见问题 3：类型选择错误

```bash
# ClusterIP vs NodePort vs LoadBalancer
kubectl get svc nginx-service -o jsonpath='{.spec.type}'
```

**解决方案：**

```bash
# 集群内部访问：使用 ClusterIP
# 外部访问测试：使用 NodePort
kubectl patch svc nginx-service -p '{"spec":{"type":"NodePort"}}'

# 云环境外部访问：使用 LoadBalancer
kubectl patch svc nginx-service -p '{"spec":{"type":"LoadBalancer"}}'
```

### 8.3.6 网络通信问题

#### Pod 间通信故障

```bash
# 1. 检查 CNI 插件状态
kubectl get pods -n kube-system -l k8s-app=cilium
kubectl get pods -n kube-system -l k8s-app=calico-node

# 2. 检查 NetworkPolicy
kubectl get networkpolicy -A

# 3. 使用 netshoot 调试
kubectl debug nginx-pod -it --image=nicolaka/netshoot:latest --copy-to=net-debug

# 4. 在调试容器中测试
kubectl exec -it net-debug -- /bin/bash
# 然后执行以下命令：

# 检查 DNS 解析
nslookup nginx-service
dig nginx-service.default.svc.cluster.local

# 检查路由
ip route
ip route show table all

# 检查 iptables 规则
iptables -L -n
iptables -L -n -t nat | grep SERVICE

# 测试连通性
ping <target-pod-ip>
telnet <target-service> 80

# 抓包分析
tcpdump -i any -nn port 80
```

#### Service DNS 解析问题

```bash
# 1. 检查 CoreDNS 是否运行
kubectl get pods -n kube-system -l k8s-app=kube-dns

# 2. 检查 CoreDNS 日志
kubectl logs -n kube-system -l k8s-app=kube-dns --tail=100

# 3. 测试 DNS 解析
kubectl exec nginx-pod -- nslookup kubernetes.default
kubectl exec nginx-pod -- nslookup nginx-service.default.svc.cluster.local

# 4. 检查 /etc/resolv.conf
kubectl exec nginx-pod -- cat /etc/resolv.conf

# 输出示例：
# nameserver 10.96.0.10
# search default.svc.cluster.local svc.cluster.local cluster.local
# options ndots:5
```

**DNS 问题修复：**

```bash
# 方案1：修复 CoreDNS 配置
kubectl edit configmap coredns -n kube-system

# 方案2：重启 CoreDNS
kubectl rollout restart deployment coredns -n kube-system

# 方案3：检查 kube-dns 服务
kubectl get svc kube-dns -n kube-system
```

### 8.3.7 存储挂载问题

#### PVC 无法挂载

```bash
# 1. 检查 PVC 状态
kubectl get pvc

# 输出示例：
# NAME            STATUS    VOLUME   CAPACITY   ACCESS MODES
# myclaim         Pending                                      # 问题状态

# 2. 查看 PVC 详情
kubectl describe pvc myclaim

# 常见问题：
# - "waiting for a volume to be created, either by external provisioner"
# - "no storage class available"
```

**解决方案：**

```bash
# 检查 StorageClass
kubectl get storageclass

# 输出示例：
# NAME                 PROVISIONER           RECLAIMPOLICY
# standard (default)   kubernetes.io/gce-pd  Delete
# fast                pd.csi.storage.gke.io  Delete

# 如果没有默认 StorageClass，指定一个
kubectl patch pvc myclaim -p '{"spec":{"storageClassName":"standard"}}'

# 或创建新的 PVC 使用存在的 StorageClass
```

#### 挂载的 Volume 权限问题

```bash
# 1. 检查 Pod 中的 Volume 挂载
kubectl describe pod nginx-pod | grep -A 20 "Mounts"

# 2. 检查 Volume 权限
kubectl exec nginx-pod -- ls -la /mnt/data

# 3. 检查 SecurityContext
kubectl get pod nginx-pod -o yaml | grep -A 10 securityContext
```

**解决方案：**

```yaml
# 设置卷的 fsGroup
spec:
  securityContext:
    fsGroup: 2000
  containers:
  - name: app
    volumeMounts:
    - name: data
      mountPath: /mnt/data
  volumes:
  - name: data
    persistentVolumeClaim:
      claimName: myclaim
```

#### ReadWriteOnce PVC 被多个 Pod 使用

```bash
# RWO 只能被单个节点上的 Pod 挂载
# 检查 Pod 分布
kubectl get pod -o wide -l app=nginx

# 解决方案：使用 ReadWriteMany (RWX) 存储
# 或部署在同一节点
```

---

## 8.4 Kubernetes 审计日志

Kubernetes 审计日志记录所有对 API Server 的请求，是安全分析和故障排查的重要工具。

### 8.4.1 启用审计日志

```bash
# kube-apiserver 启动参数中添加：
--audit-policy-file=/etc/kubernetes/audit-policy.yaml
--audit-log-path=/var/log/kubernetes/audit.log
--audit-log-maxsize=100
--audit-log-maxbackup=5
--audit-log-maxage=30
```

**审计策略配置示例** (`/etc/kubernetes/audit-policy.yaml`)：

```yaml
apiVersion: audit.k8s.io/v1
kind: Policy
# 不为 Node 调试请求生成审计
omitStages:
  - RequestReceived
rules:
  # 第一个匹配规则覆盖以下请求

  # 记录 ConfigMap 变更（敏感数据）
  - level: RequestResponse
    resources:
    - group: ""
      resources: ["configmaps"]
    verbs: ["create", "update", "patch", "delete"]

  # 记录 Secret 访问（生产环境）
  - level: Metadata
    resources:
    - group: ""
      resources: ["secrets"]

  # 记录 Deployment/Pod 操作
  - level: RequestResponse
    resources:
    - group: apps
      resources: ["deployments", "replicasets", "statefulsets"]
    - group: ""
      resources: ["pods"]
    verbs: ["*"]

  # 记录所有其他请求
  - level: Metadata
    resources:
    - group: ""
    - group: apps
    - group: networking.k8s.io
    - group: storage.k8s.io
```

### 8.4.2 审计日志级别

| 级别 | 说明 | 记录内容 |
|-----|------|---------|
| None | 不记录 | 什么都不记录 |
| Metadata | 元数据 | 请求用户、时间戳、资源、操作 |
| Request | 请求 | Metadata + 请求体（不含响应） |
| RequestResponse | 完整 | 请求和响应的完整信息 |

### 8.4.3 查看和分析审计日志

```bash
# 查看审计日志文件
cat /var/log/kubernetes/audit.log | jq .

# 日志格式示例：
{
  "kind": "Event",
  "apiVersion": "audit.k8s.io/v1",
  "level": "RequestResponse",
  "timestamp": "2026-05-19T10:30:00Z",
  "auditID": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "stage": "ResponseComplete",
  "requestURI": "/api/v1/namespaces/default/pods",
  "verb": "create",
  "user": {
    "username": "admin",
    "groups": ["system:masters"]
  },
  "sourceIPs": ["192.168.1.100"],
  "responseStatus": {
    "code": 201
  },
  "requestObject": { ... },
  "responseObject": { ... }
}

# 统计各类操作
cat audit.log | jq -r '.verb' | sort | uniq -c | sort -rn

# 查找特定用户的操作
cat audit.log | jq -r 'select(.user.username=="service-account-name") | .requestURI'

# 查找失败的认证尝试
cat audit.log | jq -r 'select(.responseStatus.code==401) | .user.username, .sourceIPs, .responseStatus.reason'

# 查找敏感资源访问
cat audit.log | jq -r 'select(.objectRef.resource=="secrets") | .verb, .objectRef.name, .user.username'
```

### 8.4.4 审计日志工具

```bash
# 使用 kube-apiserver-audit 工具分析
# 或者使用 EFK/ELK Stack 收集审计日志

# Fluentd 配置示例：
<source>
  @type tail
  path /var/log/kubernetes/audit.log
  pos_file /var/log/audit.log.pos
  format json
  tag kube-apiserver-audit
</source>
```

---

## 8.5 集群最佳实践

### 8.5.1 资源限制设置

#### Resource Requests 和 Limits

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: resource-demo
spec:
  containers:
  - name: app
    image: nginx:1.25
    resources:
      requests:        # 调度时需要的最小资源
        memory: "128Mi"
        cpu: "100m"
      limits:          # 运行时不允许超过的资源上限
        memory: "256Mi"
        cpu: "500m"
    # QoS 类别：
    # - requests == limits: Guaranteed（最高优先级，不被驱逐）
    # - 仅设置 limits: Burstable（可突发，可能被驱逐）
    # - 仅设置 requests 或都不设置: BestEffort（最易被驱逐）
```

#### LimitRange 设置命名空间默认限制

```yaml
apiVersion: v1
kind: LimitRange
metadata:
  name: default-limits
spec:
  limits:
  - type: Container
    default:              # 默认 limits（如果未指定）
      memory: "256Mi"
      cpu: "200m"
    defaultRequest:        # 默认 requests（如果未指定）
      memory: "128Mi"
      cpu: "100m"
    max:                   # 最大限制
      memory: "1Gi"
      cpu: "1"
    min:                   # 最小请求
      memory: "64Mi"
      cpu: "50m"
    maxLimitRequestRatio:  # limits/requests 的最大比例
      memory: 4
      cpu: 4
```

#### ResourceQuota 设置命名空间总资源上限

```yaml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: compute-resources
spec:
  hard:
    requests.cpu: "10"
    requests.memory: "20Gi"
    limits.cpu: "20"
    limits.memory: "40Gi"
    pods: "50"
    services: "10"
    count/persistentvolumeclaims: "20"
```

#### 生产环境推荐配置

```yaml
# 生产环境应用资源配置示例
apiVersion: apps/v1
kind: Deployment
metadata:
  name: production-app
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  template:
    spec:
      containers:
      - name: app
        image: myapp:v1.0
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 10
          successThreshold: 1
          failureThreshold: 3
        livenessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 15
          failureThreshold: 3
```

### 8.5.2 健康检查配置

#### 三种探针类型

| 探针类型 | 用途 | 失败后果 |
|---------|------|---------|
| readinessProbe | Pod 是否可接收流量 | 从 Service Endpoints 移除 |
| livenessProbe | 容器是否存活 | 重启容器 |
| startupProbe | 应用是否启动完成 | 延迟其他探针执行 |

#### 探针配置示例

```yaml
spec:
  containers:
  - name: webapp
    image: webapp:v1
    
    # 就绪探针 - 检查应用是否准备好接收请求
    readinessProbe:
      httpGet:
        path: /ready
        port: 8080
      initialDelaySeconds: 5      # 启动后等待 5 秒开始检查
      periodSeconds: 10           # 每 10 秒检查一次
      timeoutSeconds: 3           # 超时 3 秒视为失败
      successThreshold: 1        # 成功阈值（必须为 1）
      failureThreshold: 3         # 连续失败 3 次标记为未就绪
    
    # 存活探针 - 检查容器是否存活
    livenessProbe:
      httpGet:
        path: /health
        port: 8080
      initialDelaySeconds: 15     # 等待应用完全启动
      periodSeconds: 20            # 每 20 秒检查
      timeoutSeconds: 2
      successThreshold: 1
      failureThreshold: 3         # 连续失败 3 次重启容器
    
    # 启动探针 - 用于慢启动应用
    startupProbe:
      httpGet:
        path: /started
        port: 8080
      failureThreshold: 30        # 最多尝试 30 次
      periodSeconds: 10            # 每 10 秒一次 = 最多等待 300 秒
```

#### TCP 端口探针

```yaml
    # TCP Socket 探针 - 适用于数据库、Redis 等
    readinessProbe:
      tcpSocket:
        port: 5432
      initialDelaySeconds: 10
      periodSeconds: 5
    
    # Exec 探针 - 执行自定义命令
    livenessProbe:
      exec:
        command:
        - sh
        - -c
        - "redis-cli ping | grep PONG"
      initialDelaySeconds: 5
      periodSeconds: 10
```

#### 探针配置最佳实践

```yaml
# 避免的问题：
# 1. initialDelaySeconds 太短 - 应用还没启动完就被杀了
# 2. periodSeconds 太短 - 增加 API Server 负担
# 3. failureThreshold 太低 - 可能误判
# 4. successThreshold > 1 - readinessProbe 必须为 1

# 推荐配置：
readinessProbe:
  httpGet:
    path: /healthz
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 2
  failureThreshold: 3

livenessProbe:
  httpGet:
    path: /live
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 15
  timeoutSeconds: 3
  failureThreshold: 3
```

### 8.5.3 滚动更新策略

#### 滚动更新配置

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rolling-update-demo
spec:
  replicas: 10
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 25%        # 最多额外启动 3 个 Pod（25% of 10 = 2.5 → 3）
      maxUnavailable: 25% # 最多不可用 3 个 Pod（25% of 10 = 2.5 → 3）
  # 更新流程：
  # 1. 新 Pod Ready（maxSurge 允许超出）
  # 2. 旧 Pod Terminating
  # 3. 重复直到新版本完全替换
```

#### 不可中断应用的更新策略

```yaml
# 零停机更新配置
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1              # 额外启动 1 个
      maxUnavailable: 0        # 旧 Pod 必须完全被替换才终止

# 保证：
# - 任何时候都有 10 个 Pod 运行
# - 新 Pod Ready 后才终止旧 Pod
# - 适合有状态服务或数据库
```

#### 回滚配置

```bash
# 检查 rollout 历史
kubectl rollout history deployment/rolling-update-demo

# 输出：
# deployment.apps/rolling-update-demo 
# REVISION  CHANGE-CAUSE
# 1         kubectl apply --record=true
# 2         kubectl apply --record=true

# 查看特定版本详情
kubectl rollout history deployment/rolling-update-demo --revision=2

# 回滚到上一个版本
kubectl rollout undo deployment/rolling-update-demo

# 回滚到指定版本
kubectl rollout undo deployment/rolling-update-demo --to-revision=1

# 暂停/恢复 rollout
kubectl rollout pause deployment/rolling-update-demo
kubectl rollout resume deployment/rolling-update-demo

# 实时监控 rollout 状态
kubectl rollout status deployment/rolling-update-demo --timeout=5m
```

#### 蓝绿部署（Blue-Green Deployment）

```yaml
# 版本 v1 (blue) - 当前生产版本
---
apiVersion: v1
kind: Service
metadata:
  name: myapp-service
spec:
  selector:
    app: myapp
    version: v1
  ports:
  - port: 80
    targetPort: 8080

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-v1
spec:
  replicas: 10
  selector:
    matchLabels:
      app: myapp
      version: v1
  template:
    metadata:
      labels:
        app: myapp
        version: v1
    spec:
      containers:
      - name: myapp
        image: myapp:v1
---
# 版本 v2 (green) - 新版本
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-v2
spec:
  replicas: 10
  selector:
    matchLabels:
      app: myapp
      version: v2
  template:
    metadata:
      labels:
        app: myapp
        version: v2
    spec:
      containers:
      - name: myapp
        image: myapp:v2

# 切换流量：修改 Service selector
kubectl patch service myapp-service -p '{"spec":{"selector":{"version":"v2"}}}'

# 快速回滚：改回 v1
kubectl patch service myapp-service -p '{"spec":{"selector":{"version":"v1"}}}'
```

### 8.5.4 灾难恢复

#### 备份策略

```bash
# 1. 备份 etcd 数据
ETCDCTL_API=3 etcdctl snapshot save /backup/etcd-snapshot.db \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key

# 2. 备份 Kubernetes 资源
kubectl get all -A -o yaml > /backup/k8s-resources.yaml

# 3. 备份 Secret 和 ConfigMap
kubectl get secret -A -o yaml > /backup/secrets.yaml
kubectl get configmap -A -o yaml > /backup/configmaps.yaml

# 4. 使用 Velero 备份（生产推荐）
velero backup create backup-2026-05-19 \
  --include-namespaces default,production \
  --ttl 720h

# 5. 查看备份
velero backup get
velero backup describe backup-2026-05-19
```

#### 恢复流程

```bash
# 方式 1：恢复 etcd（完整集群恢复）
# 停止 kube-apiserver
systemctl stop kube-apiserver

# 恢复 etcd 数据
ETCDCTL_API=3 etcdctl snapshot restore /backup/etcd-snapshot.db \
  --data-dir=/var/lib/etcd/restore

# 修改 etcd 配置文件指向恢复数据
# 重启 etcd
systemctl restart etcd

# 重启 kube-apiserver
systemctl start kube-apiserver

# 方式 2：使用 Velero 恢复
velero restore create --from-backup backup-2026-05-19

# 查看恢复状态
velero restore get
velero restore describe restore-xxxxx

# 方式 3：恢复特定命名空间
kubectl apply -f /backup/ns-default-resources.yaml
```

#### 定期备份脚本

```bash
#!/bin/bash
# /usr/local/bin/k8s-backup.sh

BACKUP_DIR="/backup/k8s"
DATE=$(date +%Y%m%d_%H%M%S)

# 创建备份目录
mkdir -p ${BACKUP_DIR}/${DATE}

# 备份 etcd
ETCDCTL_API=3 etcdctl snapshot save ${BACKUP_DIR}/${DATE}/etcd.db \
  --endpoints=https://127.0.0.1:2379 \
  --cacert=/etc/kubernetes/pki/etcd/ca.crt \
  --cert=/etc/kubernetes/pki/etcd/server.crt \
  --key=/etc/kubernetes/pki/etcd/server.key

# 备份所有资源
kubectl get all -A -o yaml > ${BACKUP_DIR}/${DATE}/resources.yaml

# 备份 Secret（加密存储）
kubectl get secret -A -o yaml | \
  kubectl create secret generic backup-secrets -n default --dry-run=client -o yaml | \
  openssl enc -aes-256-cbc -pass pass:${BACKUP_PASSWORD} > ${BACKUP_DIR}/${DATE}/secrets.enc

# 保留最近 30 天的备份
find ${BACKUP_DIR} -type d -mtime +30 -exec rm -rf {} \;

# 上传到对象存储
aws s3 sync ${BACKUP_DIR}/${DATE} s3://my-k8s-backups/${DATE}/

echo "Backup completed: ${DATE}"
```

### 8.5.5 高可用设计

#### 控制平面高可用

```yaml
# 推荐：3 个 etcd 节点 + 3 个 API Server 节点 + 3 个 Controller/Scheduler

# etcd 集群配置示例：
# 节点 1: etcd-1.internal (192.168.1.10)
# 节点 2: etcd-2.internal (192.168.1.11)
# 节点 3: etcd-3.internal (192.168.1.12)

# API Server 负载均衡：
# - 云厂商：ELB/ALB
# - 内部：HAProxy + Keepalived

# kube-apiserver 启动参数（每个节点）：
--enable-aggregator-routing=true
--endpoint-reconciler-type=lease
```

#### 工作节点高可用

```yaml
# 推荐：至少 3 个工作节点，跨可用区分布

# Node Pool 配置示例（GKE/EKS/AKS）：
# nodepool-primary:
#   - 3 nodes in zone-a
#   - machine-type: n2-standard-4
# nodepool-memory:
#   - 2 nodes in zone-b
#   - machine-type: n2-highmem-8
# nodepool-compute:
#   - 2 nodes in zone-c
#   - machine-type: n2-highcpu-8
```

#### Pod 高可用拓扑

```yaml
# Pod 反亲和性 - 分散到不同节点
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ha-app
spec:
  replicas: 5
  selector:
    matchLabels:
      app: ha-app
  template:
    spec:
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchLabels:
                  app: ha-app
              topologyKey: kubernetes.io/hostname
      # 确保关键应用分布在不同节点
      
      topologySpreadConstraints:
      - maxSkew: 1
        topologyKey: topology.kubernetes.io/zone
        whenUnsatisfiable: DoNotSchedule
        labelSelector:
          matchLabels:
            app: ha-app
      # 分散到不同可用区

      containers:
      - name: app
        image: myapp:v1
```

#### Service 高可用

```yaml
# 确保 Service 有多个 Endpoints
apiVersion: v1
kind: Service
metadata:
  name: ha-service
spec:
  selector:
    app: ha-app
  ports:
  - port: 80
    targetPort: 8080
  # Kubernetes 会自动负载均衡到所有 Endpoints
  # 使用 sessionAffinity: ClientIP 可启用会话粘性
```

#### 跨集群高可用

```yaml
# Federation 或 Multi-Cluster 架构
# 方案 1： federation v2（Kubernetes Federation）
# 方案 2： Karmada（华为开源）
# 方案 3：阿里云 ACK One
# 方案 4：自定义 DNS 轮询 + 健康检查

# DNS 轮询方案示例：
# 部署两个集群的 Service：
# cluster1: myapp.cluster1.internal (10.0.1.100)
# cluster2: myapp.cluster2.internal (10.0.2.100)

# 全局负载均衡器配置：
# 域名: myapp.example.com
# 解析: 
#   - 30% → cluster1
#   - 30% → cluster2
#   - 40% → cluster3
# 健康检查: /health 每 10 秒
# 故障切换: 3 次失败后切换到备用集群
```

---

## 8.6 Cost Optimization（成本优化）

### 8.6.1 资源优化策略

#### 正确设置资源请求

```bash
# 分析当前资源使用情况
kubectl top pod -A

# 使用 Vertical Pod Autoscaler (VPA) 建议
kubectl get vpa -A

# 推荐：使用 Goldilocks 工具进行资源推荐
kubectl get recommendation -A

# Goldilocks 部署：
# kubectl apply -f https://raw.githubusercontent.com/FairwindsOps/goldilocks/master/hack/manifests/goldilocks.yaml
```

#### 资源请求优化示例

```yaml
# 查看当前 Pod 资源配置
apiVersion: v1
kind: Pod
metadata:
  name: optimize-demo
spec:
  containers:
  - name: app
    # 优化前：过多资源请求导致资源浪费
    resources:
      requests:
        memory: "2Gi"
        cpu: "1000m"
      limits:
        memory: "4Gi"
        cpu: "2000m"
    
    # 优化后：根据实际使用量设置
    resources:
      requests:
        memory: "256Mi"    # 实际使用 ~200Mi
        cpu: "100m"        # 实际使用 ~50m
      limits:
        memory: "512Mi"
        cpu: "200m"
```

### 8.6.2 Spot 实例与预处理

```yaml
# 使用 Spot/Preemptible 实例降低成本 60-80%

# Node 池配置示例：
# spot-pool:
#   enable: true
#   maxPrice: -1  # 使用市场价格，不设置上限
#   weight: 50    # 较低优先级，仅运行可容忍中断的工作负载

apiVersion: apps/v1
kind: Deployment
metadata:
  name: spot-tolerant-app
spec:
  template:
    spec:
      # 容忍 Spot 实例回收
      tolerations:
      - key: "cloud.google.com/gke-spot"
        operator: "Equal"
        value: "true"
        effect: "NoSchedule"
      
      # 使用优先级较低的 PodDisruptionBudget
      priorityClassName: spot-tolerant
      
      # 添加中断检查点
      lifecycle:
        preStop:
          exec:
            command: ["/bin/sh", "-c", "sleep 10"]
```

### 8.6.3 缩减不必要的资源

```bash
# 1. 删除未使用的资源
kubectl get all -A | grep -v kube-system
kubectl delete deployment unused-app -n default

# 2. 检查未绑定的 PVC（仍在计费）
kubectl get pvc -A
# 删除未使用的 PVC
kubectl delete pvc unused-pvc

# 3. 清理过期 Job 和 CronJob
kubectl get job -A
kubectl delete job completed-job -n default

# 4. 检查镜像大小 - 使用 distroless 或 alpine 基础镜像
# 大镜像占用存储和拉取时间
```

### 8.6.4 自动伸缩优化

```yaml
# Horizontal Pod Autoscaler (HPA)
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: app
  minReplicas: 2      # 生产环境最小值
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70  # CPU 70% 时扩容
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80  # 内存 80% 时扩容
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300  # 扩容冷却 5 分钟
      policies:
      - type: Percent
        value: 10                     # 每次最多缩减 10%
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100                    # 紧急情况可翻倍扩容
        periodSeconds: 15
```

```yaml
# Vertical Pod Autoscaler (VPA)
apiVersion: autoscaling.k8s.io/v1
kind: VerticalPodAutoscaler
metadata:
  name: app-vpa
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: app
  updatePolicy:
    updateMode: "Auto"  # 或 "Off" 仅提供建议
  resourcePolicy:
    containerPolicies:
    - containerName: app
      minAllowed:
        memory: 128Mi
        cpu: 50m
      maxAllowed:
        memory: 4Gi
        cpu: 2
      controlledResources: ["cpu", "memory"]
```

```yaml
# Cluster Autoscaler 配置
# 云厂商自动扩展节点池
# 示例：GKE
apiVersion: v1
kind: ConfigMap
metadata:
  name: cluster-autoscaler-config
  namespace: kube-system
data:
  # 节点池自动扩展配置
  maxNodesTotal: "50"
  coresTotal: "100"
  memoryTotal: "200Gi"
  # 缩容配置
  scale-down-delay-after-add: 10m
  scale-down-unneeded-time: 10m
  scale-down-utilization-threshold: 0.5
```

### 8.6.5 成本监控工具

```bash
# 1. Kubecost - 实时成本可视化
kubectl apply -f https://raw.githubusercontent.com/kubecost/cost-analyzer-helm-chart/master/kubecost.yaml

# 访问 Kubecost Dashboard
kubectl port-forward -n kubecost svc/cost-analyzer 9090

# 2. Prometheus + Grafana 成本看板
# 使用 kube-prometheus-stack

# 3. AWS Cost Explorer + CUR 数据

# 4. 云厂商原生工具：
# - GCP: Cloud Billing API + Kubernetes Engine 成本导出
# - AWS: Cost Explorer + EKS 成本分配标签
# - Azure: Cost Management + AKS 成本分析
```

### 8.6.6 成本优化清单

```markdown
## Kubernetes 成本优化清单

### 资源层面
- [ ] 使用 Goldilocks/VPA 确定正确的资源请求
- [ ] 删除未使用的 Pod、Deployment、Service
- [ ] 清理未绑定的 PVC（仍在计费）
- [ ] 使用更小的镜像（alpine/distroless）

### 计算层面
- [ ] 使用 Spot/Preemptible 实例运行可容忍中断的工作负载
- [ ] 配置合适的 minReplicas（不是 1！）
- [ ] 启用 HPA 自动伸缩
- [ ] 启用 Cluster Autoscaler

### 存储层面
- [ ] 使用合适的 StorageClass（Standard vs SSD）
- [ ] 清理过期快照
- [ ] 配置 PVC 回收策略

### 网络层面
- [ ] 减少跨可用区流量
- [ ] 使用 Internal LoadBalancer
- [ ] 配置合适的 Service 类型

### 监控层面
- [ ] 部署 Kubecost
- [ ] 设置成本预算告警
- [ ] 每月审查成本报告
```

---

## 8.7 DevOps 与 GitOps 工作流

### 8.7.1 CI/CD 流水线设计

```yaml
# 典型 GitLab CI / GitHub Actions 流水线

# .gitlab-ci.yml 或 .github/workflows/deploy.yml
stages:
  - build
  - test
  - scan
  - deploy-staging
  - e2e-test
  - deploy-production

variables:
  IMAGE_REGISTRY: registry.example.com
  APP_NAME: myapp
  K8S_CLUSTER: production

build:
  stage: build
  image: docker:24
  services:
    - docker:dind
  script:
    - docker build -t $IMAGE_REGISTRY/$APP_NAME:$CI_COMMIT_SHA .
    - docker push $IMAGE_REGISTRY/$APP_NAME:$CI_COMMIT_SHA
    - docker tag $IMAGE_REGISTRY/$APP_NAME:$CI_COMMIT_SHA $IMAGE_REGISTRY/$APP_NAME:latest
    - docker push $IMAGE_REGISTRY/$APP_NAME:latest
  only:
    - main

test:
  stage: test
  image: python:3.11
  script:
    - pip install pytest pytest-cov
    - pytest --cov=./src tests/

security-scan:
  stage: scan
  image: aquasec/trivy:latest
  script:
    - trivy image --exit-code 1 --severity HIGH,CRITICAL $IMAGE_REGISTRY/$APP_NAME:$CI_COMMIT_SHA
  allow_failure: true  # 继续部署但告警

deploy-staging:
  stage: deploy-staging
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/$APP_NAME app=$IMAGE_REGISTRY/$APP_NAME:$CI_COMMIT_SHA -n staging
    - kubectl rollout status deployment/$APP_NAME -n staging --timeout=5m
  environment:
    name: staging
    url: https://staging.example.com
  only:
    - main

e2e-test:
  stage: e2e-test
  image: cypress/base:18
  script:
    - npm ci
    - npx cypress run --config baseUrl=https://staging.example.com
  dependencies:
    - deploy-staging

deploy-production:
  stage: deploy-production
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/$APP_NAME app=$IMAGE_REGISTRY/$APP_NAME:$CI_COMMIT_SHA -n production
    - kubectl rollout status deployment/$APP_NAME -n production --timeout=10m
    - kubectl annotate deployment/$APP_NAME kubernetes.io/change-cause="$CI_COMMIT_MESSAGE" -n production
  environment:
    name: production
    url: https://production.example.com
  when: manual
  only:
    - main
```

### 8.7.2 GitOps 工作流 - ArgoCD

```yaml
# ArgoCD Application 定义

# argocd-app.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: production-app
  namespace: argocd
  finalizers:
    - resources-finalizer.argocd.argoproj.io
spec:
  project: default
  source:
    repoURL: https://github.com/org/k8s-manifests.git
    targetRevision: main
    path: production/app
    kustomize:
      images:
      - myapp=registry.example.com/myapp:v1.2.3
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true              # 自动删除不在 Git 中的资源
      selfHeal: true           # 自动同步集群状态到 Git 状态
      allowEmpty: false
    syncOptions:
    - CreateNamespace=true
    - Validate=true
    - ServerSideApply=false
    retry:
      limit: 5
      backoff:
        duration: 5s
        factor: 2
        maxDuration: 3m
```

```bash
# 安装 ArgoCD
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# 安装 ArgoCD CLI
brew install argocd

# 登录 ArgoCD
argocd login <argocd-server>

# 创建 Application
argocd app create production-app \
  --repo https://github.com/org/k8s-manifests.git \
  --path production/app \
  --dest-server https://kubernetes.default.svc \
  --dest-namespace production

# 同步应用
argocd app sync production-app

# 查看应用状态
argocd app get production-app

# 设置自动同步
argocd app set production-app --auto-prune --self-heal
```

### 8.7.3 GitOps 工作流 - Flux

```yaml
# Flux v2 安装配置

# flux-system/gotk-components.yaml
# Flux 自动从 Git 仓库同步

apiVersion: source.toolkit.fluxcd.io/v1
kind: GitRepository
metadata:
  name: k8s-manifests
  namespace: flux-system
spec:
  interval: 1m
  url: https://github.com/org/k8s-manifests.git
  ref:
    branch: main
  secretRef:
    name: flux-git-auth

---
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: production
  namespace: flux-system
spec:
  interval: 5m
  path: ./production
  prune: true
  sourceRef:
    kind: GitRepository
    name: k8s-manifests
  targetNamespace: production
  patches:
  - patch: |-
      apiVersion: apps/v1
      kind: Deployment
      metadata:
        name: myapp
      spec:
        replicas: 3
    target:
      name: myapp
      namespace: production
  images:
  - name: myapp
    newName: registry.example.com/myapp
    newTag: v1.2.3
```

### 8.7.4 Kustomize 环境管理

```yaml
# 目录结构
# k8s-manifests/
# ├── base/
# │   ├── deployment.yaml
# │   ├── service.yaml
# │   └── kustomization.yaml
# ├── overlays/
# │   ├── staging/
# │   │   ├── kustomization.yaml
# │   │   └── env.yaml
# │   └── production/
# │       ├── kustomization.yaml
# │       └── replicas.yaml

# base/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 1
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: app
        image: myapp:latest
        ports:
        - containerPort: 8080
        env:
        - name: LOG_LEVEL
          value: debug

# base/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
- deployment.yaml
- service.yaml
commonLabels:
  managed-by: kustomize

# overlays/staging/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
bases:
- ../../base
namespace: staging
patches:
- patch: |-
    apiVersion: apps/v1
    kind: Deployment
    spec:
      replicas: 2
  targets:
  - path: ../../base/deployment.yaml
patchesJson6902:
- target:
    version: v1
    kind: Deployment
    name: myapp
  patch: |-
    - op: replace
      path: /spec/template/spec/containers/0/env/0/value
      value: info
    - op: replace
      path: /spec/replicas
      value: 2

# overlays/production/kustomization.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
bases:
- ../../base
namespace: production
patches:
- patch: |-
    apiVersion: apps/v1
    kind: Deployment
    spec:
      replicas: 5
  targets:
  - path: ../../base/deployment.yaml
replicas:
- name: myapp
  count: 5
images:
- name: myapp
  newName: registry.example.com/myapp
  newTag: v1.2.3-prod
```

```bash
# 构建和预览
kubectl kustomize overlays/production

# 应用到集群
kubectl apply -k overlays/production

# 使用 ArgoCD 时，ArgoCD 自动读取 kustomization.yaml
```

### 8.7.5 Helm Charts 管理

```bash
# 创建 Helm Chart
helm create myapp

# Chart 结构
# myapp/
# ├── Chart.yaml
# ├── values.yaml
# ├── templates/
# │   ├── deployment.yaml
# │   ├── service.yaml
# │   ├── _helpers.tpl
# │   └── NOTES.txt

# values.yaml 示例
replicaCount: 3

image:
  repository: myapp
  tag: v1.0.0
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 80

resources:
  limits:
    cpu: 500m
    memory: 256Mi
  requests:
    cpu: 100m
    memory: 128Mi

ingress:
  enabled: true
  annotations:
    kubernetes.io/ingress.class: nginx
  hosts:
  - host: myapp.example.com
    paths:
    - path: /
      pathType: Prefix

# 生产环境覆盖
# values-production.yaml
replicaCount: 5
image:
  tag: v1.2.0-prod
resources:
  limits:
    cpu: 1000m
    memory: 512Mi
ingress:
  enabled: true
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
  hosts:
  - host: myapp.example.com
    tls:
    - secretName: myapp-tls
      hosts:
      - myapp.example.com

# 部署
helm upgrade --install myapp ./myapp \
  --namespace production \
  --values ./values-production.yaml \
  --atomic \          # 失败时自动回滚
  --cleanup-on-fail

# 查看发布历史
helm history myapp -n production

# 回滚
helm rollback myapp 1 -n production

# 模板调试
helm template myapp ./myapp --debug --dry-run
```

### 8.7.6 密钥管理 - Sealed Secrets

```yaml
# Sealed Secrets 允许将加密的 Secret 提交到 Git

# 安装 Sealed Secrets Controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/latest/download/controller.yaml

# 获取公钥（用于加密）
kubectl get secret -n kube-system -l sealedsecrets.bitnami.com/sealed-secrets-key -o yaml

# 安装 kubeseal CLI
brew install kubeseal

# 创建 Secret 并加密
kubectl create secret generic db-credentials \
  --from-literal=username=admin \
  --from-literal=password=secret123 \
  -n production \
  --dry-run=client \
  -o yaml > temp-secret.yaml

kubeseal --format=yaml < temp-secret.yaml > sealed-secret.yaml

# sealed-secret.yaml 可以安全提交到 Git
# Sealed Secrets Controller 自动解密
apiVersion: bitnami.com/v1alpha1
kind: SealedSecret
metadata:
  name: db-credentials
  namespace: production
spec:
  encryptedData:
    username: AgA2...==
    password: AgB3...==
```

### 8.7.7 完整 GitOps 流程示例

```bash
#!/bin/bash
# CI/CD Pipeline 执行流程

# 1. Developer 提交代码到 feature branch
git checkout -b feature/new-feature
git commit -m "Add new feature"
git push origin feature/new-feature

# 2. CI 触发自动构建和测试
# - 运行单元测试
# - 构建 Docker 镜像
# - 安全扫描
# - 推送到镜像仓库
# - 更新镜像标签

# 3. 创建 Pull Request
# - 触发 E2E 测试
# - 合并到 main

# 4. ArgoCD/Flow 检测到变更
# - 自动拉取最新代码
# - 使用 Kustomize 渲染清单
# - 同步到 Staging 环境
# - 等待手动批准

# 5. 手动批准后同步到 Production
argocd app sync production-app

# 6. 监控部署状态
argocd app wait production-app --timeout 300

# 7. 验证应用健康
kubectl rollout status deployment/myapp -n production
kubectl get hpa myapp -n production

# 8. 告警和通知
# - Slack: 部署成功/失败通知
# - PagerDuty: 关键问题告警
```

---

## 8.8 本章小结

本章详细介绍了 Kubernetes 故障排查与最佳实践的核心内容：

### 故障排查工具
- `kubectl describe` - 查看资源详细信息和事件
- `kubectl logs` - 查看容器日志
- `kubectl exec` - 在容器中执行命令
- `kubectl port-forward` - 端口转发调试
- `kubectl proxy` - API 代理访问
- `kubectl debug` - 临时调试容器

### 常见问题解决方案
- **Pending**: 检查资源、节点选择器、 PVC 绑定、Taints
- **Waiting/ImagePullBackOff**: 验证镜像名称、仓库认证、网络连通
- **CrashLoopBackOff**: 检查启动命令、配置文件、依赖服务

### 最佳实践
- **资源限制**: 合理设置 requests 和 limits，使用 LimitRange 和 ResourceQuota
- **健康检查**: 配置 readinessProbe、livenessProbe 和 startupProbe
- **滚动更新**: 使用 maxSurge 和 maxUnavailable 控制更新节奏
- **灾难恢复**: 定期备份 etcd 和资源清单，制定恢复预案
- **高可用**: 多副本分散部署，跨可用区分布

### 成本优化
- 正确设置资源请求，使用 VPA 推荐
- 利用 Spot 实例运行可容忍中断工作负载
- 配置 HPA 和 Cluster Autoscaler
- 使用 Kubecost 监控成本

### GitOps 工作流
- ArgoCD/Flow 自动同步 Git 仓库到集群
- Kustomize/Helm 管理多环境配置
- Sealed Secrets 安全管理密钥
- 完整 CI/CD 流水线实现自动化部署

掌握这些技能，将帮助你快速定位和解决 Kubernetes 集群中的问题，同时构建可靠、高效的云原生应用。
