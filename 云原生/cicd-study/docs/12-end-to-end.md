# 第 12 章 全链路打通：从 git push 到自动上线

> 到目前为止，CI（第 6～8 章）和 CD（第 9～11 章）各自都能独立运转，但它们之间还隔着一道"人工"：镜像推上去之后，得有人去改 `mall-deploy` 里的 tag。本章补上最后一块拼图——**让流水线自己去改 GitOps 仓库**。从此，你只需要 `git push`，几分钟后新版本就自动运行在开发环境里。

---

## 本章目标

完成本章后，你应该能够：

1. 读懂 Jenkinsfile 最终版新增的「部署到开发环境」阶段：CI 如何通过**修改 Git** 来"触发" CD。
2. 说清楚 **Push 式部署**（CI 直接 `kubectl`）与 **Pull 式部署**（GitOps）的区别，以及本教程选择后者的理由。
3. 完成一次端到端演示：修改代码 → `git push` → 约 5 分钟后 `mall-dev` 自动运行新版本，全程不碰任何部署命令。
4. 从线上正在运行的版本**反查到源码提交**，完成"可追溯"闭环。
5. 当"push 了但 dev 没更新"时，能沿着链路逐站排查。

预计耗时：**60 分钟**。

---

## 本章在全景中的位置

```mermaid
flowchart LR
    dev(["👨‍💻 开发者"]):::person
    subgraph HOST["宿主机 Docker"]
        direction LR
        mall["Gitea<br/>mall 仓库"]:::scm
        jenkins["Jenkins<br/>Jenkinsfile 最终版"]:::ci
        sonar["SonarQube"]:::ci
        registry["Registry"]:::ci
        deploy["Gitea<br/>mall-deploy 仓库"]:::scm
    end
    subgraph K8S["Kubernetes"]
        direction LR
        argocd["Argo CD"]:::cd
        devns["mall-dev"]:::app
        prodns["mall-prod<br/>（第 13 章）"]:::app
    end

    dev -- "① push" --> mall
    mall -- "② Webhook" --> jenkins
    jenkins -- "③ 扫描/门禁" --> sonar
    jenkins -- "④ push 镜像" --> registry
    jenkins == "⑤ 修改 envs/dev 的 image.tag<br/>（本章新增）" ==> deploy
    argocd -- "⑥ 发现变化" --> deploy
    argocd -- "⑦ 自动同步" --> devns
    argocd -. "人工审批 + 同步" .-> prodns

    classDef person fill:#3b2f5c,stroke:#a78bfa,stroke-width:2px,color:#f5f3ff
    classDef scm fill:#14352a,stroke:#34d399,stroke-width:2px,color:#ecfdf5
    classDef ci fill:#1e3a5f,stroke:#60a5fa,stroke-width:2px,color:#eff6ff
    classDef cd fill:#4a2c12,stroke:#fb923c,stroke-width:2px,color:#fff7ed
    classDef app fill:#1f2937,stroke:#9ca3af,stroke-width:1px,color:#f9fafb
    style HOST fill:#0b1220,stroke:#60a5fa,color:#93c5fd
    style K8S fill:#0b1220,stroke:#fb923c,color:#fdba74
    linkStyle 4 stroke:#facc15,stroke-width:3px
```

图中加粗的黄色箭头 ⑤ 就是本章要接上的那一段。

---

## 12.1 前置检查

开始之前，确认前面各章的成果都在。

### 步骤 1：CI 基础设施在运行

```bash
# 位置：/d/cicd-lab/infra
docker compose ps --format "table {{.Name}}\t{{.Status}}"
```

预期 5 个容器均为 `Up`：`gitea`、`jenkins`、`registry`、`sonar-db`、`sonarqube`。

### 步骤 2：Argo CD 应用全部健康

```bash
# 位置：任意目录
kubectl get applications -n argocd
```

预期输出：

```text
NAME              SYNC STATUS   HEALTH STATUS
mall-order-dev    Synced        Healthy
mall-order-prod   Synced        Healthy
mall-user-dev     Synced        Healthy
mall-user-prod    Synced        Healthy
mysql-dev         Synced        Healthy
mysql-prod        Synced        Healthy
```

### 步骤 3：开发环境当前运行的是 v1

```bash
# 位置：任意目录
curl -s http://mall-dev.localhost/api/users/version; echo
curl -s http://mall-dev.localhost/api/orders/version; echo
```

预期输出（JSON 字段顺序不固定，`host` 是 Pod 名）：

```text
{"service":"mall-user","version":"v1","host":"mall-user-5c4b3a291-q2w3e"}
{"service":"mall-order","version":"v1","host":"mall-order-6d5c4b3a2-k8x7p"}
```

### 步骤 4：本地两个仓库同步到最新

```bash
# 位置：/d/cicd-lab
cd /d/cicd-lab/mall && git pull
cd /d/cicd-lab/mall-deploy && git pull --rebase
grep -n "tag:" envs/dev/*.yaml
```

预期输出：

```text
envs/dev/mall-order.yaml:4:  tag: "v1"
envs/dev/mall-user.yaml:4:  tag: "v1"
```

> ✅ 检查点：以上 4 步全部符合预期，再继续。如果某个应用不是 `Synced / Healthy`，先回到第 11 章排查。

---

## 12.2 最后一块拼图：CI 怎么"通知" CD？

### 12.2.1 两种思路

镜像已经推到仓库了，怎么让集群用上它？业界有两种做法：

```mermaid
flowchart TB
    subgraph PUSH["❌ Push 式：CI 直接操作集群"]
        direction LR
        j1["Jenkins<br/>持有集群管理员凭据"]:::ci -- "kubectl set image / helm upgrade" --> k1["Kubernetes"]:::warn
    end
    subgraph PULL["✅ Pull 式（GitOps）：CI 只改 Git"]
        direction LR
        j2["Jenkins<br/>只持有 Git 凭据"]:::ci -- "git commit：image.tag = 新哈希" --> g2["mall-deploy 仓库<br/>（唯一可信来源）"]:::scm
        a2["Argo CD<br/>运行在集群内部"]:::cd -- "拉取并对比" --> g2
        a2 -- "apply" --> k2["Kubernetes"]:::focus
    end

    classDef scm fill:#14352a,stroke:#34d399,stroke-width:2px,color:#ecfdf5
    classDef ci fill:#1e3a5f,stroke:#60a5fa,stroke-width:2px,color:#eff6ff
    classDef cd fill:#4a2c12,stroke:#fb923c,stroke-width:2px,color:#fff7ed
    classDef focus fill:#713f12,stroke:#facc15,stroke-width:3px,color:#fefce8
    classDef warn fill:#450a0a,stroke:#f87171,stroke-width:2px,color:#fef2f2
    style PUSH fill:#0b1220,stroke:#f87171,color:#fca5a5
    style PULL fill:#0b1220,stroke:#34d399,color:#6ee7b7
```

| 对比项 | Push 式（CI 里 `kubectl`） | Pull 式 / GitOps（本教程） |
|---|---|---|
| 集群凭据放在哪 | **CI 服务器上**，Jenkins 被攻破 = 集群被攻破 | **不出集群**，Argo CD 在集群内部运行，Jenkins 只需要 Git 写权限 |
| 网络要求 | CI 必须能访问集群 API Server | 只需要集群能访问 Git |
| "现在线上是什么版本" | 要去翻 Jenkins 构建日志，或者登录集群查 | **看 Git 就知道**，Git 里写的就是线上的 |
| 审计 | 分散在各次构建日志中 | 每次变更都是一个 Git 提交：谁、何时、改了什么 |
| 回滚 | 重新跑一次旧版本的流水线 | `git revert` 一个提交（第 13 章） |
| 有人手工改了集群 | 发现不了，配置逐渐"漂移" | Argo CD 检测到差异并自动纠正（第 11 章已验证 selfHeal） |
| 多集群 / 多环境 | CI 要分别连每个集群 | 每个集群的 Argo CD 各自拉取 |

> 🧠 一句话总结：**CI 的职责到"产出一个可部署的版本"为止，"部署"这件事交给 GitOps**。Jenkins 和集群之间没有任何直接连接——这正是本章新增阶段只有 `git` 命令、没有 `kubectl` 的原因。

### 12.2.2 新增阶段做了什么

```text
 Jenkins 工作空间（mall 仓库，提交 8e7d6c5）
 ┌───────────────────────────────────────────────────────────────────────────┐
 │  前面的阶段：测试 ✓  扫描 ✓  门禁 ✓  镜像 localhost:5000/mall/*:8e7d6c5 ✓  │
 │                                                                           │
 │  「部署到开发环境」                                                        │
 │   1. git clone mall-deploy 到子目录 ./mall-deploy                          │
 │   2. yq 修改 envs/dev/mall-user.yaml   image.tag: "v1" → "8e7d6c5"         │
 │      yq 修改 envs/dev/mall-order.yaml  image.tag: "v1" → "8e7d6c5"         │
 │   3. git commit -m "deploy(dev): mall 8e7d6c5"                             │
 │   4. git push → Gitea mall-deploy 仓库                                     │
 └───────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼  （最多 60 秒）
   Argo CD 发现 mall-deploy 有新提交 → 渲染 Helm → 与集群对比 → OutOfSync → 自动同步
```

---

## 12.3 动手：Jenkinsfile 最终版（v4）

### 步骤 1：用教程目录中的最终版覆盖

最终版就是教程资料中的 `mall/Jenkinsfile`。直接复制过来（把前面的路径换成你的教程资料实际路径）：

```bash
# 位置：/d/cicd-lab/mall
cp /d/Work/Java/study/study/cicd-study/mall/Jenkinsfile ./Jenkinsfile
git diff --stat
```

预期输出类似（具体数字以实际为准）：

```text
 Jenkinsfile | 28 ++++++++++++++++++++++++++--
 1 file changed, 26 insertions(+), 2 deletions(-)
```

只有头部注释、`DEPLOY_REPO` 一行和新的 stage 发生了变化。可以用 `git diff` 查看具体内容。

如果找不到教程资料目录，也可以用编辑器打开 `D:\cicd-lab\mall\Jenkinsfile`，全选替换为以下完整内容：

```groovy
// ============================================================
// mall 流水线（最终版）
//   所有分支：编译 + 单元测试（CI）
//   main 分支：+ 代码扫描 + 质量门禁 + 构建推送镜像 + 更新 GitOps 仓库（CD，由 Argo CD 部署到 dev）
// ============================================================
pipeline {
    agent any

    options {
        timestamps()                                    // 日志带时间戳
        disableConcurrentBuilds()                       // 同一分支不并发构建，避免互相覆盖镜像/配置
        buildDiscarder(logRotator(numToKeepStr: '20'))  // 只保留最近 20 次构建记录
    }

    environment {
        REGISTRY     = 'localhost:5000'                                  // 私有镜像仓库
        IMAGE_PREFIX = "${REGISTRY}/mall"                                // 镜像名前缀
        SERVICES     = 'mall-user mall-order'                            // 需要构建的服务（目录名 = 镜像名）
        DEPLOY_REPO  = 'host.docker.internal:3000/mall/mall-deploy.git'  // GitOps 配置仓库
    }

    stages {
        stage('准备') {
            steps {
                script {
                    // 镜像 tag 使用 Git 提交短哈希：任何一个运行中的镜像都能精确追溯到源码版本
                    env.IMAGE_TAG = sh(script: 'git rev-parse --short=7 HEAD', returnStdout: true).trim()
                }
                echo "分支: ${env.BRANCH_NAME}，镜像 tag: ${env.IMAGE_TAG}"
            }
        }

        stage('编译与单元测试') {
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always {
                    // 收集测试报告，Jenkins 页面上可以看到测试趋势和失败用例
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('代码质量扫描') {
            when { branch 'main' }   // SonarQube 社区版只支持分析一个分支
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh 'mvn -B sonar:sonar'
                }
            }
        }

        stage('质量门禁') {
            when { branch 'main' }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    // 等待 SonarQube 通过 Webhook 回调分析结果，门禁不通过则流水线失败
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('构建并推送镜像') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'registry-cred',
                        usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                    sh '''
                        echo "$REG_PASS" | docker login "$REGISTRY" -u "$REG_USER" --password-stdin
                        for svc in $SERVICES; do
                            docker build -t "$IMAGE_PREFIX/$svc:$IMAGE_TAG" "$svc"
                            docker push "$IMAGE_PREFIX/$svc:$IMAGE_TAG"
                        done
                    '''
                }
            }
        }

        stage('部署到开发环境') {
            when { branch 'main' }
            steps {
                // CD 的本质：只修改 GitOps 仓库中的镜像 tag，真正的部署由 Argo CD 完成
                withCredentials([usernamePassword(credentialsId: 'gitea-cred',
                        usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    sh '''
                        rm -rf mall-deploy
                        git clone "http://$GIT_USER:$GIT_TOKEN@$DEPLOY_REPO" mall-deploy
                        cd mall-deploy
                        for svc in $SERVICES; do
                            yq -i '.image.tag = strenv(IMAGE_TAG)' "envs/dev/$svc.yaml"
                        done
                        git config user.name  "jenkins"
                        git config user.email "jenkins@mall.local"
                        git add envs/dev
                        git diff --cached --quiet || git commit -m "deploy(dev): mall $IMAGE_TAG"
                        git push origin HEAD:main
                    '''
                }
            }
        }
    }

    post {
        success { echo '流水线执行成功' }
        failure { echo '流水线执行失败，请查看上面的日志定位失败的阶段' }
        always  { cleanWs() }   // 清理工作空间，避免下次构建受残留文件影响
    }
}
```

### 步骤 2：本次新增逐行讲解

和 v3 相比，只有两处新增：

```diff
     environment {
         REGISTRY     = 'localhost:5000'                                  // 私有镜像仓库
         IMAGE_PREFIX = "${REGISTRY}/mall"                                // 镜像名前缀
         SERVICES     = 'mall-user mall-order'                            // 需要构建的服务（目录名 = 镜像名）
+        DEPLOY_REPO  = 'host.docker.internal:3000/mall/mall-deploy.git'  // GitOps 配置仓库
     }
 ...
+        stage('部署到开发环境') {
+            ...（见上方完整内容）
+        }
```

**`DEPLOY_REPO`**

| 代码 | 说明 |
|---|---|
| `'host.docker.internal:3000/mall/mall-deploy.git'` | **故意不带 `http://`**，因为下面要把用户名和令牌拼进去：`http://用户:令牌@` + `DEPLOY_REPO`。地址用 `host.docker.internal`，与 Gitea 的 ROOT_URL 保持一致 |

**「部署到开发环境」阶段**

| 代码 | 说明 |
|---|---|
| `when { branch 'main' }` | 只有 main 自动部署到 dev。feature 分支、PR 到「编译与单元测试」就结束了 |
| `withCredentials([usernamePassword(credentialsId: 'gitea-cred', ...)])` | 取出第 6 章创建的 `gitea-cred`（`mall-admin` + Gitea 令牌），注入为 `GIT_USER`、`GIT_TOKEN` |
| `rm -rf mall-deploy` | 保证幂等：万一工作空间里有上次残留的目录，先删掉，`git clone` 就不会报"目录已存在" |
| `git clone "http://$GIT_USER:$GIT_TOKEN@$DEPLOY_REPO" mall-deploy` | 把用户名和令牌放进 URL 实现免交互认证。**日志中这两个值会被自动替换成 `****`**。令牌会留在克隆出的 `.git/config` 中，但构建结束时 `cleanWs()` 会把整个工作空间删掉 |
| `for svc in $SERVICES; do ... done` | 依次处理 `envs/dev/mall-user.yaml`、`envs/dev/mall-order.yaml` |
| `yq -i '.image.tag = strenv(IMAGE_TAG)' "envs/dev/$svc.yaml"` | 用 yq **原地修改**（`-i`）YAML 中的 `image.tag`。详见下方说明 |
| `git config user.name / user.email` | 设置本次提交的作者为 `jenkins`，在 Gitea 中一眼就能分辨"人改的"还是"流水线改的" |
| `git add envs/dev` | 只暂存 dev 目录的改动，**绝不会碰到 prod** |
| `git diff --cached --quiet \|\| git commit -m "deploy(dev): mall $IMAGE_TAG"` | `git diff --cached --quiet` 在"没有暂存的改动"时返回 0，`\|\|` 后面就不执行。用来防止**空提交**：例如同一个提交被重复构建，tag 没变，`git commit` 会因为 "nothing to commit" 返回非 0，导致流水线失败 |
| `git push origin HEAD:main` | 把当前提交推到远端 `main`。写成 `HEAD:main` 是明确的"本地 HEAD → 远端 main"，不依赖本地分支名 |

> 🧠 **为什么用 `yq` 和 `strenv`，而不用 `sed`？**
>
> 1. **yq 理解 YAML 结构**。`.image.tag` 精确定位到 `image` 下的 `tag`，不会误改其他地方的 `tag:`；而且它会保留文件中的注释和格式。用 `sed` 改 YAML 是在"按文本行碰运气"。
> 2. **`strenv(IMAGE_TAG)` 保证写进去的是字符串**。Git 短哈希由 0-9a-f 组成，大约每 27 次提交就会有一次**全是数字**（例如 `1234567`）。如果写成数字，Helm 读取 values 时会把它当成浮点数，渲染出来变成 `image: localhost:5000/mall/mall-user:1.234567e+06`——一个根本不存在的镜像，Pod 直接 `ImagePullBackOff`。`strenv` 会把值当作字符串写入，yq 输出时自动加引号：`tag: "1234567"`。
>
> 这类 bug 平时很难复现，一旦出现又很难查。**写 GitOps 流水线时，tag 一律按字符串处理**。

> 🧠 **`sh` 步骤默认带 `set -e`**：Jenkins 执行 `sh` 时使用 `sh -xe`，`-x` 让每条命令打印在日志中（前面带 `+`），`-e` 让任何一条命令失败都立刻终止整个步骤。所以上面每一条命令都必须"成功或被显式处理"，这也是要用 `||` 处理空提交的原因。

### 步骤 3：先别急着推——想一想会发生什么

推送这个 Jenkinsfile 本身就是一次 `main` 分支的提交，所以它**会触发第一次全自动部署**：dev 的镜像 tag 会从 `v1` 变成这次提交的哈希。代码功能没有变化，正好用来验证管道是通的（"冒烟测试"）。

```bash
# 位置：/d/cicd-lab/mall
git add Jenkinsfile
git commit -m "ci: 流水线最终版 - 自动更新 GitOps 仓库部署到 dev"
git push
git rev-parse --short=7 HEAD
```

记下最后一行输出的 7 位哈希，下面用 `<TAG1>` 指代它。

---

## 12.4 动手：第一次全自动部署（冒烟测试）

### 步骤 1：Jenkins——6 个阶段全绿

```text
 Checkout ✓ ─▶ 准备 ✓ ─▶ 编译与单元测试 ✓ ─▶ 代码质量扫描 ✓ ─▶ 质量门禁 ✓ ─▶ 构建并推送镜像 ✓ ─▶ 部署到开发环境 ✓ ─▶ Post ✓
```

「部署到开发环境」阶段的日志（`<TAG1>` 处为你的哈希）：

```text
+ rm -rf mall-deploy
+ git clone http://****:****@host.docker.internal:3000/mall/mall-deploy.git mall-deploy
Cloning into 'mall-deploy'...
+ cd mall-deploy
+ yq -i '.image.tag = strenv(IMAGE_TAG)' envs/dev/mall-user.yaml
+ yq -i '.image.tag = strenv(IMAGE_TAG)' envs/dev/mall-order.yaml
+ git config user.name jenkins
+ git config user.email jenkins@mall.local
+ git add envs/dev
+ git diff --cached --quiet
+ git commit -m 'deploy(dev): mall <TAG1>'
[main 7a6b5c4] deploy(dev): mall <TAG1>
 2 files changed, 2 insertions(+), 2 deletions(-)
+ git push origin HEAD:main
To http://host.docker.internal:3000/mall/mall-deploy.git
   3c4d5e6..7a6b5c4  HEAD -> main
```

注意 `****:****`——用户名和令牌都被 Jenkins 打码了。

### 步骤 2：Gitea——mall-deploy 出现 jenkins 的提交

浏览器打开 http://host.docker.internal:3000/mall/mall-deploy/commits/branch/main ，最新一条提交：

| 字段 | 值 |
|---|---|
| 作者 | `jenkins` |
| 提交信息 | `deploy(dev): mall <TAG1>` |
| 改动（点进去看 Diff） | `envs/dev/mall-user.yaml`、`envs/dev/mall-order.yaml` 各一行：`tag: "v1"` → `tag: "<TAG1>"` |

### 步骤 3：Argo CD——检测并同步

最多 60 秒后（等不及就在 Argo CD UI 中点应用卡片上的 **REFRESH**），`mall-user-dev`、`mall-order-dev` 会经历：

```mermaid
---
config:
  theme: dark
---
stateDiagram-v2
    [*] --> Synced_v1: 运行 v1
    Synced_v1 --> OutOfSync: 拉到新提交，期望 TAG1，实际 v1
    OutOfSync --> Syncing: automated 自动同步
    Syncing --> Progressing: Deployment 滚动更新中
    Progressing --> Synced_Healthy: 新 Pod 就绪，旧 Pod 下线
    Synced_Healthy --> [*]
```

命令行观察：

```bash
# 位置：任意目录
kubectl get applications -n argocd
kubectl get deploy -n mall-dev -o 'custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image'
```

预期输出：

```text
NAME              SYNC STATUS   HEALTH STATUS
mall-order-dev    Synced        Healthy
...
NAME         IMAGE
mall-order   localhost:5000/mall/mall-order:<TAG1>
mall-user    localhost:5000/mall/mall-user:<TAG1>
```

### 步骤 4：访问验证

```bash
# 位置：任意目录
curl -s http://mall-dev.localhost/api/users/version; echo
```

预期：`version` 已经变成 `<TAG1>`。

```text
{"service":"mall-user","version":"<TAG1>","host":"mall-user-7f6e5d4c3-a1b2c"}
```

> ✅ 检查点：
> 1. Jenkins 7 个阶段（含 Checkout）全绿；
> 2. mall-deploy 中有 `jenkins` 的提交；
> 3. dev 两个 Deployment 的镜像都是 `<TAG1>`，接口返回的 version 是 `<TAG1>`；
> 4. **prod 仍然是 v1**：`curl -s http://mall.localhost/api/users/version` 返回 `"version":"v1"`。prod 何时更新、如何审批，是第 13 章的内容。

---

## 12.5 动手：一次真实的功能上线（端到端演示）

冒烟测试证明管道是通的。现在来一次"真正的需求"：**给版本接口增加一个 `message` 字段**，然后只做一件事——`git push`，看着它自己上线。

### 步骤 1：打开观察窗口

为了看清每一站，建议开 **3 个 Git Bash 窗口**：

| 窗口 | 用途 | 命令 |
|---|---|---|
| 窗口 A | 改代码、提交 | 在 `/d/cicd-lab/mall` 中操作 |
| 窗口 B | 观察 Pod 滚动更新 | `kubectl get pods -n mall-dev -w` |
| 窗口 C | 每 2 秒访问一次接口 | 见下方命令 |

窗口 C 执行：

```bash
# 位置：任意目录
while true; do echo "$(date +%T) $(curl -s -m 2 http://mall-dev.localhost/api/users/version)"; sleep 2; done
```

预期每两秒打印一行，例如：

```text
21:30:01 {"service":"mall-user","version":"<TAG1>","host":"mall-user-7f6e5d4c3-a1b2c"}
```

浏览器同时打开：Jenkins `mall` → `main` 页面、Argo CD 首页（http://argocd.localhost ）。

### 步骤 2：修改代码

用编辑器打开 `D:\cicd-lab\mall\mall-user\src\main\java\com\xiaozhi\mall\user\VersionController.java`，修改为：

```java
package com.xiaozhi.mall.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * 版本信息：用来观察"当前请求落在哪个 Pod、运行的是哪个镜像版本"，
 * 是验证滚动更新、负载均衡、回滚效果的利器。
 */
@RestController
public class VersionController {

    /** 由部署环境注入（K8s 中等于镜像 tag），本地运行时为 local */
    @Value("${app.version}")
    private String version;

    @GetMapping("/api/users/version")
    public Map<String, String> version() throws UnknownHostException {
        return Map.of(
                "service", "mall-user",
                "version", version,
                "host", InetAddress.getLocalHost().getHostName(),
                "message", "Hello CI/CD");
    }
}
```

改动只有两处：把 `host` 那一行末尾的 `);` 改成 `,`，再在下面新增一行 `"message", "Hello CI/CD");`。

> 💡 `Map.of` 最多支持 **10 个键值对**（20 个参数），超过时改用 `Map.ofEntries(Map.entry(k, v), ...)`。另外 `Map.of` 返回的 Map 遍历顺序不固定，所以接口返回的 JSON 字段顺序每次可能不同，这是正常的。

本地先跑一遍测试（好习惯：**本地能过再推送**）：

```bash
# 位置：/d/cicd-lab/mall
mvn -B -q -pl mall-user verify
echo "exit=$?"
```

预期：只打印 `exit=0`。原有的 `versionEndpoint` 测试只断言了 `service` 和 `version` 两个字段，新增字段不影响它。

### 步骤 3：推送，开始计时

```bash
# 位置：/d/cicd-lab/mall
git add mall-user
git commit -m "feat(user): 版本接口增加 message 字段"
git push
git rev-parse --short=7 HEAD
```

记下哈希，下面用 `<TAG2>` 指代。**从这一刻起，你不需要再做任何操作**，只需要按下表逐站观察：

| 站点 | 去哪里看 | 预期现象 | 大约耗时（从 push 起算） |
|---|---|---|---|
| ① Gitea Webhook | `mall/mall` → Settings → Webhooks → 点开 Webhook → Recent Deliveries | 最新一条推送记录，响应 200 | 1 秒内 |
| ② Jenkins 触发 | `mall` → `main` | 出现新构建，Changes 中是 `feat(user): 版本接口增加 message 字段` | 几秒内 |
| ③ 编译与单元测试 | 阶段视图 | 绿色，Tests 共 19 个、0 失败 | 约 1～2 分钟 |
| ④ 代码质量扫描 + 质量门禁 | 阶段视图；http://localhost:9000/dashboard?id=mall | 门禁 Passed（新代码只有 2 行，少于 20 行，不检查覆盖率） | 约 1 分钟 |
| ⑤ 构建并推送镜像 | 阶段日志；`curl -s -u admin:Admin@123456 http://localhost:5000/v2/mall/mall-user/tags/list` | tag 列表中出现 `<TAG2>`；日志中大部分层是 `Layer already exists` | 约 30～60 秒 |
| ⑥ 部署到开发环境 | Gitea `mall/mall-deploy` 提交历史 | `jenkins` 提交 `deploy(dev): mall <TAG2>` | 几秒 |
| ⑦ Argo CD 检测 | Argo CD UI | `mall-user-dev`、`mall-order-dev` 变为 OutOfSync → Syncing | 0～60 秒（可点 REFRESH） |
| ⑧ 滚动更新 | 窗口 B | 新 Pod 创建 → 就绪 → 旧 Pod Terminating | 约 30～60 秒 |
| ⑨ 新版本生效 | 窗口 C | `version` 变为 `<TAG2>`，并多出 `"message":"Hello CI/CD"` | — |

整个过程大约 **4～6 分钟**。

### 步骤 4：重点观察滚动更新

窗口 B 的输出大致如下（Pod 名后缀因人而异）：

```text
NAME                          READY   STATUS              RESTARTS   AGE
mall-order-6d5c4b3a2-k8x7p    1/1     Running             0          12m
mall-user-7f6e5d4c3-a1b2c     1/1     Running             0          12m
mysql-0                       1/1     Running             0          3h
mall-user-58d7c6b5a-x9y8z     0/1     Pending             0          0s
mall-user-58d7c6b5a-x9y8z     0/1     ContainerCreating   0          0s
mall-user-58d7c6b5a-x9y8z     0/1     Running             0          2s     ← 容器已启动，探针尚未通过
mall-order-5b4a39281-m5n6o    0/1     Pending             0          0s     ← mall-order 也换了新 tag
...
mall-user-58d7c6b5a-x9y8z     1/1     Running             0          20s    ← readinessProbe 通过，开始接流量
mall-user-7f6e5d4c3-a1b2c     1/1     Terminating         0          13m    ← 然后才下线旧 Pod
mall-user-7f6e5d4c3-a1b2c     0/1     Terminating         0          13m
```

窗口 C 的输出会在某一刻切换：

```text
21:34:10 {"service":"mall-user","version":"<TAG1>","host":"mall-user-7f6e5d4c3-a1b2c"}
21:34:12 {"service":"mall-user","version":"<TAG1>","host":"mall-user-7f6e5d4c3-a1b2c"}
21:34:14 {"message":"Hello CI/CD","service":"mall-user","version":"<TAG2>","host":"mall-user-58d7c6b5a-x9y8z"}
21:34:16 {"message":"Hello CI/CD","service":"mall-user","version":"<TAG2>","host":"mall-user-58d7c6b5a-x9y8z"}
```

> 🧠 **为什么是"先起新的，再停旧的"？** Chart 中配置了 `maxSurge: 1, maxUnavailable: 0`：允许临时多出 1 个 Pod，但任何时刻可用 Pod 数不能少于期望值。新 Pod 必须通过 **readinessProbe**（`/actuator/health/readiness`）才算可用，之后旧 Pod 才会被终止。这就是第 9、10 章那些探针配置在真实发布中的作用。
>
> 💡 窗口 C 在切换瞬间偶尔出现一次空行或 502，是因为旧 Pod 被终止与 Ingress 摘除它之间存在短暂竞争。生产环境通常会给容器加一个 `preStop` 钩子（例如 `sleep 5`）再配合 Spring Boot 的优雅停机来消除这个窗口，第 14 章会提到。

你也注意到了：**只改了 mall-user，mall-order 也被重新部署了**。因为当前流水线每次都构建全部服务、并把两个服务的 tag 都改成同一个哈希。这是单仓库（monorepo）最简单的做法，代价是多了一次无意义的重启；第 14 章的进阶路线会介绍"只构建和部署有变化的服务"。

### 步骤 5：停止观察

在窗口 B、C 中按 `Ctrl + C` 结束。

> ✅ 检查点：
>
> ```bash
> # 位置：任意目录
> curl -s http://mall-dev.localhost/api/users/version; echo
> curl -s http://mall.localhost/api/users/version; echo
> ```
>
> 第一行（dev）包含 `"version":"<TAG2>"` 和 `"message":"Hello CI/CD"`；第二行（prod）仍然是 `"version":"v1"`、没有 `message`。**同一份代码，dev 已上线新功能，prod 纹丝不动**——环境隔离生效。

---

## 12.6 全链路时序图

```mermaid
---
config:
  theme: dark
---
sequenceDiagram
    autonumber
    actor Dev as 开发者
    participant G as Gitea（mall）
    participant J as Jenkins
    participant S as SonarQube
    participant R as Registry
    participant D as Gitea（mall-deploy）
    participant A as Argo CD
    participant K as K8s（mall-dev）

    Dev->>G: git push（feat: message 字段）
    G->>J: Webhook（约 1 秒）
    Note over J: 准备：IMAGE_TAG = TAG2
    J->>J: mvn clean verify（约 1～2 分钟）
    J->>S: mvn sonar:sonar（约 30～60 秒）
    S-->>J: Webhook：Quality gate OK（约 5～15 秒）
    J->>R: docker build + push mall-user/mall-order:TAG2（约 30～60 秒）
    J->>D: git commit + push：envs/dev image.tag = TAG2（约 5 秒）
    J-->>G: 回写提交状态 ✓
    loop 每 60 秒（或手动 REFRESH）
        A->>D: 拉取 main 最新提交
    end
    A->>A: helm template 渲染，与集群对比 → OutOfSync
    A->>K: apply Deployment（image: TAG2）
    K->>K: 滚动更新：新 Pod 就绪 → 旧 Pod 下线（约 30～60 秒）
    A->>A: Synced + Healthy
    Dev->>K: curl http://mall-dev.localhost/api/users/version
    K-->>Dev: version = TAG2，message = Hello CI/CD
```

回头看看这张图：**开发者只做了第 1 步**。编译、测试、扫描、打镜像、改配置、部署、健康检查，全部自动完成，而且每一步都留有记录。这就是 CI/CD 的价值。

---

## 12.7 动手：从线上反查源码——可追溯闭环

线上出问题时，第一个问题永远是："**现在跑的到底是哪份代码？**" 有了 Git 哈希 tag，这个问题 10 秒就能回答。

### 步骤 1：查出 dev 正在运行的镜像

```bash
# 位置：任意目录
IMAGE=$(kubectl get deploy mall-user -n mall-dev -o jsonpath='{.spec.template.spec.containers[0].image}')
echo "$IMAGE"
TAG=${IMAGE##*:}
echo "$TAG"
```

预期输出：

```text
localhost:5000/mall/mall-user:<TAG2>
<TAG2>
```

`${IMAGE##*:}` 是 Shell 的字符串截取：删掉最后一个 `:` 及其左边的所有内容，剩下的就是 tag。

### 步骤 2：在源码仓库中查看这个提交

```bash
# 位置：/d/cicd-lab/mall
git show --stat "$TAG"
```

预期输出：

```text
commit <TAG2>xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
Author: ...
Date:   ...

    feat(user): 版本接口增加 message 字段

 .../java/com/xiaozhi/mall/user/VersionController.java | 3 ++-
 1 file changed, 2 insertions(+), 1 deletion(-)
```

### 步骤 3：其它可以对照的线索

| 线索 | 在哪看 | 能回答什么 |
|---|---|---|
| Jenkins 构建 | `mall` → `main` → 对应构建的 Changes / Console Output | 这个镜像是哪次构建产出的、测试和门禁结果如何 |
| SonarQube | 项目 `mall` → Activity | 这次提交的代码质量快照 |
| Registry | `tags/list` | 镜像确实存在于仓库中 |
| mall-deploy 提交 | Gitea `mall/mall-deploy` 提交历史 | **什么时候**、由谁（jenkins）把 dev 切到了这个版本 |
| Argo CD | 应用 → **HISTORY AND ROLLBACK** | 每次同步的时间和对应的 Git 修订 |
| 接口 | `/api/users/version` | 正在响应请求的 Pod 及其版本 |

```mermaid
flowchart RL
    pod["运行中的 Pod<br/>mall-user-58d7c6b5a-x9y8z"]:::app --> dep["Deployment<br/>image: mall-user:TAG2"]:::cd
    dep --> argo["Argo CD 同步记录<br/>mall-deploy 修订 7a6b5c4…"]:::cd
    argo --> mdc["mall-deploy 提交<br/>deploy(dev): mall TAG2<br/>作者 jenkins"]:::scm
    dep --> img["Registry<br/>mall/mall-user:TAG2"]:::ci
    img --> build["Jenkins 构建 #N<br/>测试 ✓ 门禁 ✓"]:::ci
    build --> src["mall 源码提交 TAG2<br/>feat(user): 版本接口增加 message 字段"]:::focus

    classDef scm fill:#14352a,stroke:#34d399,stroke-width:2px,color:#ecfdf5
    classDef ci fill:#1e3a5f,stroke:#60a5fa,stroke-width:2px,color:#eff6ff
    classDef cd fill:#4a2c12,stroke:#fb923c,stroke-width:2px,color:#fff7ed
    classDef app fill:#1f2937,stroke:#9ca3af,stroke-width:1px,color:#f9fafb
    classDef focus fill:#713f12,stroke:#facc15,stroke-width:3px,color:#fefce8
```

> 🧠 这就是第 8 章坚持"不用 latest、用 Git 哈希做 tag"的回报：**从任何一个运行中的 Pod 出发，都能顺藤摸瓜找到源码、构建记录、质量报告和部署记录**。审计、排障、回滚都建立在这条链路之上。

---

## 12.8 全链路排错地图

"我 push 了，但 dev 没更新"——按下面的顺序逐站排查，**先确定卡在哪一站，再看那一站的日志**：

```mermaid
flowchart TD
    start(["push 了，但 dev 没更新"]):::warn --> q1{"Jenkins 有新构建吗？"}:::ci
    q1 -- "没有" --> f1["查 Gitea Webhook 推送记录<br/>（第 6 章 6.12 节）"]:::app
    q1 -- "有" --> q2{"哪个阶段失败？"}:::ci
    q2 -- "编译与单元测试" --> f2["看 Tests 报告，本地复现<br/>（第 6 章）"]:::app
    q2 -- "扫描 / 门禁" --> f3["看 SonarQube 失败条件<br/>（第 7 章）"]:::app
    q2 -- "构建并推送镜像" --> f4["看 docker 报错<br/>（第 8 章）"]:::app
    q2 -- "部署到开发环境" --> f5["git clone / push 报错<br/>见下表"]:::app
    q2 -- "全部成功" --> q3{"mall-deploy 有新提交吗？"}:::scm
    q3 -- "没有" --> f6["日志中是否走了<br/>git diff --cached --quiet 分支<br/>（tag 没变）"]:::app
    q3 -- "有" --> q4{"Argo CD 是 Synced 到<br/>这个提交吗？"}:::cd
    q4 -- "否" --> f7["点 REFRESH；检查应用 Conditions<br/>和仓库连接（第 11 章）"]:::app
    q4 -- "是" --> q5{"Pod 正常吗？"}:::cd
    q5 -- "ImagePullBackOff" --> f8["镜像不在本机镜像库<br/>见下表"]:::app
    q5 -- "CrashLoopBackOff / 0/1" --> f9["kubectl logs / describe<br/>（第 9 章）"]:::app
    q5 -- "Running 1/1" --> f10["接口访问方式问题<br/>检查 Ingress 与域名（第 10 章）"]:::app

    classDef scm fill:#14352a,stroke:#34d399,stroke-width:2px,color:#ecfdf5
    classDef ci fill:#1e3a5f,stroke:#60a5fa,stroke-width:2px,color:#eff6ff
    classDef cd fill:#4a2c12,stroke:#fb923c,stroke-width:2px,color:#fff7ed
    classDef app fill:#1f2937,stroke:#9ca3af,stroke-width:1px,color:#f9fafb
    classDef warn fill:#450a0a,stroke:#f87171,stroke-width:2px,color:#fef2f2
```

### 常见坑

| 现象 | 原因 | 解决 |
|---|---|---|
| 「部署到开发环境」`git push` 报 `! [rejected] HEAD -> main (fetch first)` / `non-fast-forward` | 在 Jenkins clone 之后、push 之前，有别人（通常是你自己）也往 `mall-deploy` 推了提交 | 重新运行一次构建即可；想彻底避免，可以在 `git push` 前加一行 `git pull --rebase origin main`（课后练习 2） |
| 你本地推送 `mall-deploy` 被拒绝 | Jenkins 往远端推过提交，你本地落后了 | **改 mall-deploy 之前先 `git pull --rebase`**；已经提交了就 `git pull --rebase` 后再 `git push` |
| `git clone` 报 `Authentication failed` / `403` | `gitea-cred` 中的令牌错误，或令牌没有仓库写权限 | 第 5 章重新生成令牌，更新 Jenkins 凭据 `gitea-cred` |
| `yq: command not found` | Jenkins 镜像不是第 4 章定制的那个 | `docker exec jenkins yq --version` 检查，必要时 `docker compose up -d --build jenkins` |
| Jenkins 全绿、mall-deploy 也有新提交，但 Argo CD 迟迟不同步 | 还没到 60 秒轮询；或者 Argo CD 连不上 Gitea | 点 **REFRESH**；查看应用详情中的错误信息；`kubectl get applications -n argocd` 查看状态 |
| 新 Pod 报 `ImagePullBackOff`，`kubectl describe pod` 显示 `failed to resolve reference "localhost:5000/mall/...` | 镜像不在 Docker Desktop 的本机镜像库中（例如执行过 `docker image prune -a`）。kind 节点只能通过内置 registry-mirror 从本机镜像库取镜像，**拉不了 HTTP 私有仓库中的镜像**（第 9 章 9.10 节） | 在 Windows 上执行 `docker pull localhost:5000/mall/mall-user:<tag>` 把镜像拉回本机，然后删除出问题的 Pod 让它重建；或者重新运行一次流水线 |
| 新 Pod `CrashLoopBackOff`，Argo CD 显示 `Degraded` | 新版本代码或配置有问题 | **旧 Pod 仍在提供服务**（`maxUnavailable: 0`），先别慌；用 `kubectl logs` 定位，回滚方法见第 13 章 |
| feature 分支或 PR 构建中「部署到开发环境」显示为 skipped | `when { branch 'main' }` 的设计行为 | 正常现象，只有合并到 main 才部署 |
| 重复构建同一个提交，mall-deploy 没有新提交 | tag 没变，`git diff --cached --quiet` 判断无改动，跳过了提交 | 正常现象；Argo CD 也不会有任何动作 |
| 只改了一个服务，两个服务都重启了 | 流水线构建并部署全部服务 | 设计如此，见 12.5 节步骤 4 的说明 |

---

## 本章小结

| 知识点 | 一句话总结 |
|---|---|
| CI 与 CD 的交接 | Jenkins 只修改 `mall-deploy` 中的 `image.tag` 并推送，部署由 Argo CD 完成 |
| Pull 优于 Push | 集群凭据不出集群；Git 即线上状态、即审计记录、即回滚手段；配置漂移可自愈 |
| yq + strenv | 结构化修改 YAML，且保证 tag 是字符串，避免纯数字哈希被 Helm 渲染成科学计数法 |
| 防空提交 | `git diff --cached --quiet \|\| git commit`，让重复构建也能成功 |
| 凭据安全 | 令牌通过 `withCredentials` 注入，日志自动打码，工作空间构建后清理 |
| 滚动更新 | `maxSurge: 1, maxUnavailable: 0` + readinessProbe：先起新、再停旧，发布不中断 |
| 可追溯 | Pod → 镜像 tag → 构建 → 源码提交，10 秒定位线上版本 |
| 环境隔离 | 流水线只改 `envs/dev`，prod 不受影响 |

```mermaid
flowchart LR
    p(["git push"]):::person --> ci["CI<br/>测试 → 扫描 → 门禁 → 镜像"]:::ci --> gitops["GitOps 仓库<br/>envs/dev image.tag"]:::focus --> cd["Argo CD<br/>自动同步"]:::cd --> devenv["mall-dev<br/>新版本上线"]:::scm
    gitops -. "第 13 章：审批后晋升<br/>envs/prod image.tag" .-> prod["mall-prod"]:::app

    classDef person fill:#3b2f5c,stroke:#a78bfa,stroke-width:2px,color:#f5f3ff
    classDef scm fill:#14352a,stroke:#34d399,stroke-width:2px,color:#ecfdf5
    classDef ci fill:#1e3a5f,stroke:#60a5fa,stroke-width:2px,color:#eff6ff
    classDef cd fill:#4a2c12,stroke:#fb923c,stroke-width:2px,color:#fff7ed
    classDef app fill:#1f2937,stroke:#9ca3af,stroke-width:1px,color:#f9fafb
    classDef focus fill:#713f12,stroke:#facc15,stroke-width:3px,color:#fefce8
```

本章结束时的状态：

| 环境 | 镜像 tag | 接口特征 | 同步方式 |
|---|---|---|---|
| mall-dev | `<TAG2>`（7 位 Git 短哈希） | 带 `"message":"Hello CI/CD"` | 自动 |
| mall-prod | `v1` | 无 `message` | 手动 |

`mall` 仓库的 Jenkinsfile 已经与教程资料中的 `mall/Jenkinsfile`（最终版）完全一致。

---

## 课后练习

1. **门禁守住了上线**：在 `PriceCalculator` 中加一行 `BigDecimal bad = new BigDecimal(0.1);` 并在 `payAmount` 中使用它，推送后观察：流水线停在「质量门禁」，「构建并推送镜像」和「部署到开发环境」都没有执行，dev 的版本保持不变。然后用 `git revert` 撤销这次提交并推送，确认恢复。
2. **加固推送**：在「部署到开发环境」的 `git push origin HEAD:main` 之前加一行 `git pull --rebase origin main`，思考它解决了什么问题、又可能引入什么新问题（提示：如果两个构建同时修改了同一行）。练习完可以保留，也可以恢复成教程版本。
3. **思考题**：如果公司要求"dev 环境的每次部署都要通知到企业微信/钉钉群"，你会在哪里加通知？Jenkins 的 `post { success { ... } }`，还是 Argo CD 的 Notifications？两种方式通知的"含义"有什么不同？（提示：一个是"配置已更新"，一个是"部署已完成"。）

---

下一章：[第 13 章 生产发布：审批、晋升与回滚](13-release-rollback.md)
