# 第 13 章 生产发布：审批、晋升与回滚

> 能自动部署到开发环境只是起点；**能安全上线、出问题能一分钟内回滚**，才是一条合格的交付流水线。

---

## 本章目标

学完本章，你应该能够：

1. 说清楚 **环境晋升（Promotion）** 与 **Build Once, Deploy Many** 原则：为什么生产环境不允许重新构建镜像
2. 创建 Jenkins 任务 `mall-promote-prod`，完成一次 **带人工审批** 的生产发布
3. 在 Argo CD 中查看发布差异（DIFF）、手动同步，并亲眼观察 2 副本的 **滚动更新零停机**
4. 完成 3 个故障演练：dev 配置错误用 `git revert` 回滚；prod 用 Argo CD 紧急回滚并补齐 Git；`kubectl` 手工改动被 selfHeal 改回
5. 掌握一份软件工程师视角的 **发布检查清单**

预计耗时：2～3 小时

---

## 本章在全景中的位置

第 12 章我们打通了"push 代码 → 自动部署到 dev"。本章补上最后、也是最关键的一段：**dev 验证通过之后，如何受控地发布到 prod，以及出了问题如何撤回**。

```mermaid
flowchart LR
    dev(["👨‍💻 开发者"]):::person
    gitea["Gitea<br/>mall 源码仓库"]:::scm
    ci["Jenkins · mall<br/>构建 / 测试 / 镜像"]:::ci
    devdeploy["mall-deploy<br/>envs/dev/*.yaml"]:::scm
    devenv["mall-dev<br/>自动同步"]:::app

    promote["Jenkins · mall-promote-prod<br/>对比版本 → 审批 → 晋升"]:::focus
    proddeploy["mall-deploy<br/>envs/prod/*.yaml"]:::focus
    argocd["Argo CD<br/>DIFF / 手动 SYNC / 回滚"]:::focus
    prodenv["mall-prod<br/>2 副本滚动更新"]:::focus
    approver(["🧑‍💼 审批人"]):::person

    dev --> gitea --> ci --> devdeploy --> devenv
    devdeploy -- "复制 image.tag" --> promote
    approver -- "点击「发布」" --> promote
    promote --> proddeploy --> argocd
    argocd -- "人工 SYNC" --> prodenv

    classDef person fill:#3b2f5c,stroke:#a78bfa,stroke-width:2px,color:#f5f3ff
    classDef scm fill:#14352a,stroke:#34d399,stroke-width:2px,color:#ecfdf5
    classDef ci fill:#1e3a5f,stroke:#60a5fa,stroke-width:2px,color:#eff6ff
    classDef app fill:#1f2937,stroke:#9ca3af,stroke-width:1px,color:#f9fafb
    classDef focus fill:#713f12,stroke:#facc15,stroke-width:3px,color:#fefce8
```

当前起点（第 12 章结束时的状态）：

| 环境 | 运行版本 | 同步方式 | 副本数 | 访问地址 |
|---|---|---|---|---|
| mall-dev | 7 位 Git 短哈希（如 `a1b2c3d`），带第 12 章新增的 `message` 字段 | 自动同步（prune + selfHeal） | 1 | http://mall-dev.localhost |
| mall-prod | `v1`（第 11 章首次同步的版本） | 手动同步 | 2 | http://mall.localhost |

---

## 13.1 为什么生产发布要"单独对待"

开发环境出问题，影响的是同事；生产环境出问题，影响的是用户和收入。所以两个环境在流程上刻意做了不同的设计：

| 对比项 | mall-dev（开发环境） | mall-prod（生产环境） |
|---|---|---|
| 谁触发 | push 到 `main` 自动触发 | 人工点击 `mall-promote-prod` |
| 镜像从哪来 | 流水线刚构建的新镜像 | **直接复用 dev 验证过的镜像**，不重新构建 |
| 是否审批 | 否 | 是（Jenkins `input` 步骤） |
| Argo CD 同步 | 自动（Git 一变集群就变） | 手动（OutOfSync 后由人点击 SYNC） |
| 副本数 | 1 | 2（滚动更新时不中断服务） |
| 出问题怎么办 | `git revert` 后自动恢复 | Argo CD 紧急回滚 + 补齐 Git |

这种"一个镜像，按顺序走过多个环境"的过程，就叫 **环境晋升（Promotion）**：

```mermaid
flowchart LR
    build["🔨 构建一次<br/>mall-user:a1b2c3d"]:::ci
    devenv["mall-dev<br/>功能自测 / 联调"]:::app
    gate1{"验证通过？"}:::cd
    approve["🧑‍💼 审批<br/>Jenkins input"]:::focus
    prodenv["mall-prod<br/>正式对外服务"]:::app
    fix["修复后重新提交<br/>生成新的镜像 tag"]:::warn

    build --> devenv --> gate1
    gate1 -- "是" --> approve --> prodenv
    gate1 -- "否" --> fix --> build

    classDef ci fill:#1e3a5f,stroke:#60a5fa,stroke-width:2px,color:#eff6ff
    classDef cd fill:#4a2c12,stroke:#fb923c,stroke-width:2px,color:#fff7ed
    classDef app fill:#1f2937,stroke:#9ca3af,stroke-width:1px,color:#f9fafb
    classDef focus fill:#713f12,stroke:#facc15,stroke-width:3px,color:#fefce8
    classDef warn fill:#450a0a,stroke:#f87171,stroke-width:2px,color:#fef2f2
```

> 💡 真实公司里通常是 dev → test（测试环境）→ staging（预发布）→ prod 多级晋升。本教程只保留 dev 和 prod 两级，原理完全相同，多一级就是多一个 `envs/xxx` 目录和一个 Argo CD 应用。

---

## 13.2 Build Once, Deploy Many：生产绝不重新构建

先看一个反面教材。很多团队早期的做法是"发生产时，切到 release 分支重新打包一次"：

```text
 ❌ 错误做法：每个环境各自构建
 ──────────────────────────────────────────────────────────────────────
  main@a1b2c3d ──mvn package──▶ jar ──docker build──▶ 镜像 A ──▶ dev   ✔ 测过
  main@a1b2c3d ──mvn package──▶ jar ──docker build──▶ 镜像 B ──▶ prod  ❓ 没测过
                      ▲                    ▲
                      │                    └─ 基础镜像 eclipse-temurin:21-jre 可能已更新
                      └─ SNAPSHOT 依赖 / 依赖仓库内容可能已变化

 ✅ 正确做法：只构建一次，各环境只改"用哪个 tag"
 ──────────────────────────────────────────────────────────────────────
  main@a1b2c3d ──构建一次──▶ localhost:5000/mall/mall-user:a1b2c3d
                                          │
                     ┌────────────────────┼─────────────────────┐
                     ▼                                          ▼
          envs/dev/mall-user.yaml                    envs/prod/mall-user.yaml
            tag: "a1b2c3d"   ──── 验证通过后复制 tag ────▶  tag: "a1b2c3d"
```

重新构建的风险在于：**你在 dev 测过的是镜像 A，上生产的却是镜像 B**。哪怕源码完全相同，基础镜像、依赖版本、构建机环境的微小差异都可能让 B 与 A 不同。

在我们的项目里，这个原则落地得非常简单：**晋升 = 把 `envs/dev/*.yaml` 里的 `image.tag` 字符串复制到 `envs/prod/*.yaml`**。没有 `mvn`，没有 `docker build`，只有一次 Git 提交。

> 🧠 原理：镜像 tag 用 Git 短哈希的另一个好处在这里体现出来了——prod 上跑的 `a1b2c3d`，可以直接在 Gitea 的 mall 仓库里搜到对应的提交，"线上到底跑的是哪版代码"一目了然。

---

## 13.3 审批门（Approval Gate）：两道闸

我们的生产发布设置了两道人工关卡，各管一件事：

| 关卡 | 在哪里 | 管什么 | 谁来操作（真实公司） |
|---|---|---|---|
| 第一道：Jenkins `input` | `mall-promote-prod` 流水线的「发布审批」阶段 | **要不要发**：看过版本对比后，决定是否把 dev 的版本写入 prod 配置 | 项目负责人 / 发布经理 |
| 第二道：Argo CD 手动 SYNC | mall-*-prod 应用 | **发的到底是什么**：DIFF 中确认只有预期的改动，然后选择合适的时机真正发布 | 值班开发 / 运维 |

```text
  Jenkins input                     Git（mall-deploy）                Argo CD SYNC
 ┌───────────────┐   批准后写入    ┌───────────────────┐   OutOfSync   ┌───────────────┐
 │ 对比版本       │ ─────────────▶ │ envs/prod/*.yaml  │ ────────────▶ │ 看 DIFF        │
 │ 「发布」/Abort │                │ tag: a1b2c3d      │               │ 点 SYNC        │
 └───────────────┘                └───────────────────┘               └───────┬───────┘
   决定"要不要发"                      留下审计记录                            ▼
                                     （谁、何时、改了什么）              mall-prod 滚动更新
```

> 💡 为什么 Git 提交本身就是审计记录？因为 `mall-deploy` 的每一次提交都有作者、时间、提交说明和精确的 diff。半年后有人问"10 月 12 日下午生产发了什么"，翻 Git 历史就有答案。

---

## 13.4 发布策略一览

Kubernetes 的 Deployment 原生支持两种策略：**Recreate** 和 **RollingUpdate**（我们用的就是后者）。蓝绿和金丝雀则需要借助额外工具。作为软件工程师，你至少要知道它们的区别：

```text
 Recreate（重建）：先全部停掉，再全部启动 —— 中间有停机
   旧 ■■ ──────┐
               └─ 全部删除 ── ⛔ 服务中断 ── 新 □□ 启动
   适用：不允许新旧版本并存的场景（如独占资源、不兼容的数据库变更）

 RollingUpdate（滚动更新）：逐个替换 —— 本教程使用
   ■■  →  ■■□  →  ■□  →  ■□□  →  □□
          起1新    下1旧   起1新    下1旧        （maxSurge=1, maxUnavailable=0）
   适用：绝大多数无状态服务；代价是发布过程中新旧版本短暂并存

 蓝绿发布：两套完整环境，一次性切换流量
   蓝(旧) ■■ ◀── 100% 流量          蓝(旧) ■■   （保留，随时切回）
   绿(新) □□       0%        ──▶    绿(新) □□ ◀── 100% 流量
   适用：要求瞬间切换、瞬间回滚；代价是双倍资源

 金丝雀发布：先放一小部分流量给新版本，观察后逐步放大
   新 □ ◀── 10% ──▶ 观察指标 ──▶ 50% ──▶ 100%
   旧 ■■■ ◀── 90%
   适用：用户量大、需要控制故障影响面；需要指标监控配合
```

| 策略 | 停机 | 额外资源 | 回滚速度 | 新旧并存 | 实现方式 |
|---|---|---|---|---|---|
| Recreate | 有 | 无 | 慢（重新部署） | 否 | Deployment 原生 |
| RollingUpdate | 无 | 少量（maxSurge） | 中（再滚一次） | 是（短暂） | Deployment 原生 ✅ |
| 蓝绿 | 无 | 双倍 | 快（切流量） | 否 | Argo Rollouts / 两套 Service |
| 金丝雀 | 无 | 少量 | 快（流量切回） | 是（可控比例） | Argo Rollouts / 服务网格 |

如果以后需要金丝雀，Argo 家族的 **Argo Rollouts** 是与 Argo CD 配套的方案。它用一个 `Rollout` 资源替代 `Deployment`，在策略里声明分步放量（以下仅为示意）：

```yaml
# 示意：Argo Rollouts 的金丝雀策略（本教程不安装，了解即可）
apiVersion: argoproj.io/v1alpha1
kind: Rollout
metadata:
  name: mall-user
spec:
  replicas: 4
  strategy:
    canary:
      steps:
        - setWeight: 25          # 先把 25% 的副本换成新版本
        - pause: {duration: 5m}  # 观察 5 分钟
        - setWeight: 50
        - pause: {}              # 无限期暂停，等人工确认后继续
  # selector / template 与 Deployment 写法相同，此处省略
```

---

## 13.5 动手：确认起点状态

### 步骤 1：同步本地的 mall-deploy 仓库

> ⚠️ 本地的 `mall-deploy` 仓库**一定是落后的**——因为 Jenkins 每次部署 dev 都会往远端推送提交。**以后每次在本地改 mall-deploy 之前，都先 `git pull`**，这是本章最容易踩的坑。

```bash
# 位置：/d/cicd-lab/mall-deploy
git pull
git log --oneline -5
```

预期输出（哈希值每个人都不同，下同）：

```text
Updating 3c4d5e6..7e8f9a0
Fast-forward
 envs/dev/mall-order.yaml | 2 +-
 envs/dev/mall-user.yaml  | 2 +-
 2 files changed, 2 insertions(+), 2 deletions(-)
7e8f9a0 (HEAD -> main, origin/main) deploy(dev): mall a1b2c3d
6d7e8f9 deploy(dev): mall 9b8c7d6
3c4d5e6 init: mall-deploy
```

### 步骤 2：对比 dev 与 prod 的镜像 tag

```bash
# 位置：/d/cicd-lab/mall-deploy
grep -H "tag:" envs/dev/*.yaml envs/prod/*.yaml
```

预期输出：

```text
envs/dev/mall-order.yaml:  tag: "a1b2c3d"
envs/dev/mall-user.yaml:  tag: "a1b2c3d"
envs/prod/mall-order.yaml:  tag: "v1"
envs/prod/mall-user.yaml:  tag: "v1"
```

> 💡 yq 写入后 tag 两边的引号可能有也可能没有，二者在 YAML 中等价，不影响结果。

### 步骤 3：确认集群中两个环境的实际版本

```bash
# 位置：任意目录
kubectl get deploy -n mall-prod -o custom-columns='NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image,READY:.status.readyReplicas'
curl -s http://mall.localhost/api/users/version; echo
curl -s http://mall-dev.localhost/api/users/version; echo
```

预期输出：

```text
NAME         IMAGE                                READY
mall-order   localhost:5000/mall/mall-order:v1    2
mall-user    localhost:5000/mall/mall-user:v1     2
{"service":"mall-user","version":"v1","host":"mall-user-5d8f9c7b6-aaaaa"}
{"service":"mall-user","version":"a1b2c3d","message":"...","host":"mall-user-7f6c5d4e3-xxxxx"}
```

> 💡 接口返回的是 `Map.of(...)` 序列化出来的 JSON，**字段顺序不固定**，你看到的顺序和上面不同是正常的。`message` 的内容就是你在第 12 章添加的那段文字。

> ✅ 检查点：prod 是 `v1`、dev 是短哈希、两者 tag 不同——这正是"待发布"的状态。

---

## 13.6 动手：创建生产发布任务 mall-promote-prod

### 步骤 1：先读懂这条流水线

生产发布流水线的脚本已经放在 GitOps 仓库里：[`mall-deploy/jenkins/promote-prod.Jenkinsfile`](../mall-deploy/jenkins/promote-prod.Jenkinsfile)。为什么放在 `mall-deploy` 而不是 `mall`？因为它操作的对象是**部署配置**，与业务代码无关——谁管配置仓库，谁就管发布流程。

逐段来看。

**① 头部：全局选项与环境变量**

```groovy
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        SERVICES    = 'mall-user mall-order'
        DEPLOY_REPO = 'host.docker.internal:3000/mall/mall-deploy.git'
    }
```

- `disableConcurrentBuilds()`：同一时间只允许一个生产发布在跑，避免两个人同时发布互相覆盖
- `SERVICES`：本次要晋升的服务列表，和 mall 流水线保持一致

**② 阶段「对比版本」：先让审批人看清楚要发什么**

```groovy
        stage('对比版本') {
            steps {
                sh '''
                    echo "======== 本次发布内容 ========"
                    for svc in $SERVICES; do
                        echo "$svc:  prod 当前 $(yq '.image.tag' envs/prod/$svc.yaml)  =>  将发布 $(yq '.image.tag' envs/dev/$svc.yaml)"
                    done
                '''
            }
        }
```

这里的 `envs/prod/...`、`envs/dev/...` 是相对路径——任务开始时 Jenkins 已经自动把 mall-deploy 仓库检出到工作空间（声明式流水线的隐式 `Checkout SCM` 阶段）。`yq '.image.tag' 文件` 用来读取 YAML 中某个字段的值。

**③ 阶段「发布审批」：流水线在这里暂停等人**

```groovy
        stage('发布审批') {
            steps {
                // 流水线在这里暂停，等待有权限的人点击"发布"或"中止"
                timeout(time: 30, unit: 'MINUTES') {
                    input message: '确认将上述版本发布到生产环境？', ok: '发布'
                }
            }
        }
```

- `input`：暂停流水线，在页面上出现一个确认框，按钮文字就是 `ok` 参数的值「发布」，另一个按钮是 `Abort`（中止）
- `timeout(30 分钟)`：没人处理就自动中止，避免一个"挂起"的发布无限期占着位置

**④ 阶段「更新生产配置」：把 dev 的 tag 复制给 prod**

```groovy
        stage('更新生产配置') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'gitea-cred',
                        usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                    sh '''
                        for svc in $SERVICES; do
                            TAG="$(yq '.image.tag' envs/dev/$svc.yaml)" yq -i '.image.tag = strenv(TAG)' "envs/prod/$svc.yaml"
                        done
                        git config user.name  "jenkins"
                        git config user.email "jenkins@mall.local"
                        git add envs/prod
                        if git diff --cached --quiet; then
                            echo "生产环境已经是最新版本，无需发布"
                            exit 0
                        fi
                        git commit -m "release(prod): promote $(yq '.image.tag' envs/dev/mall-user.yaml) from dev"
                        git push "http://$GIT_USER:$GIT_TOKEN@$DEPLOY_REPO" HEAD:main
                    '''
                }
            }
        }
```

逐行解释关键点：

| 代码 | 作用 |
|---|---|
| `TAG="$(yq ... dev ...)" yq -i '.image.tag = strenv(TAG)' prod` | 读出 dev 的 tag 放进临时环境变量 `TAG`，再用 `strenv(TAG)` 以**字符串**形式写进 prod 文件（防止纯数字的哈希被当成数字） |
| `git diff --cached --quiet` | 暂存区没有变化时返回 0：说明 prod 已经和 dev 一致，直接成功退出 |
| `git commit -m "release(prod): ..."` | 提交说明以 `release(prod)` 开头，日后在 Git 历史里一眼就能找到所有生产发布 |
| `git push ... HEAD:main` | Jenkins 检出的是"游离 HEAD"（detached HEAD），所以要显式推送到远端的 `main` 分支 |

**⑤ 结尾：提示下一步**

```groovy
    post {
        success { echo '生产配置已更新，请到 Argo CD 中检查差异（OutOfSync）后点击 Sync 完成发布' }
    }
```

> 🧠 原理：注意整条流水线**没有任何一行 `kubectl` 或 `helm` 命令**。Jenkins 不直接操作生产集群，它只改 Git；真正的发布动作由 Argo CD 完成。这样生产集群的凭据只需要交给 Argo CD 一个组件，Jenkins 被攻破也无法直接篡改生产。

### 步骤 2：在 Jenkins 中新建任务

1. 浏览器打开 http://host.docker.internal:8080 ，用 `admin` / `Admin@123456` 登录
2. 左侧菜单点击 **New Item**（新建任务）
3. 填写：

| 字段 | 填写值 |
|---|---|
| Enter an item name（任务名） | `mall-promote-prod` |
| 任务类型 | 选择 **Pipeline**（流水线） |

4. 点击 **OK**，进入配置页面
5. 在 **General**（常规）区域的 **Description**（描述）中可以填：`生产发布：把 dev 验证通过的镜像晋升到 prod（需审批）`
6. 向下滚动到 **Pipeline**（流水线）区域，按下表填写：

| 字段 | 填写值 | 说明 |
|---|---|---|
| Definition（定义） | `Pipeline script from SCM` | 流水线脚本从代码仓库读取，而不是写在 Jenkins 页面里 |
| SCM | `Git` | |
| Repositories → Repository URL（仓库地址） | `http://host.docker.internal:3000/mall/mall-deploy.git` | 注意是 **mall-deploy** 仓库 |
| Repositories → Credentials（凭据） | 选择 `mall-admin/****** (gitea-cred)` | 下拉框里显示的文字取决于你创建凭据时填写的描述 |
| Branches to build → Branch Specifier（分支） | `*/main` | ⚠️ 默认值是 `*/master`，**必须改成 `*/main`** |
| Script Path（脚本路径） | `jenkins/promote-prod.Jenkinsfile` | 相对于仓库根目录 |
| Lightweight checkout（轻量级检出） | ✅ 勾选（默认已勾选） | 读取脚本时只拉取这一个文件，速度更快 |

7. 点击页面底部的 **Save**（保存）

> ⚠️ 填完 Repository URL 后，如果下面立刻出现红字 `Failed to connect to repository`，通常是凭据还没选——选好凭据后红字会消失。如果选了凭据仍然报错，检查 `gitea-cred` 里的令牌是否正确（不同版本菜单位置可能略有差异）。

> ✅ 检查点：Jenkins 首页（Dashboard）能看到两个任务：`mall`（多分支流水线）和 `mall-promote-prod`（流水线）。

---

## 13.7 动手：第一次生产发布

整个发布过程的时序如下，先建立整体印象，再一步步操作：

```mermaid
---
config:
  theme: dark
---
sequenceDiagram
    autonumber
    actor A as 审批人
    participant J as Jenkins<br/>mall-promote-prod
    participant G as Gitea<br/>mall-deploy
    participant C as Argo CD
    participant K as K8s<br/>mall-prod

    A->>J: Build Now
    J->>G: 检出 mall-deploy
    J->>J: 对比版本（prod v1 => dev a1b2c3d）
    J-->>A: 暂停，等待审批
    A->>J: 点击「发布」
    J->>J: yq 把 dev 的 tag 写入 envs/prod/*.yaml
    J->>G: git push（release(prod) 提交）
    loop 每 60 秒
        C->>G: 拉取最新提交并渲染 Helm
    end
    C-->>A: mall-user-prod / mall-order-prod 显示 OutOfSync
    A->>C: 查看 DIFF，点击 SYNC
    C->>K: kubectl apply（新镜像 tag）
    K->>K: 滚动更新：起 1 新 → 就绪 → 下 1 旧 → ……
    K-->>C: 全部就绪
    C-->>A: Synced + Healthy
```

### 步骤 1：先演示一次「中止」

先体验"不批准"会发生什么：

1. 进入任务 `mall-promote-prod`，点击左侧 **Build Now**（立即构建）
2. 左下角 **Build History**（构建历史）出现 `#1`，点击它，再点击左侧 **Console Output**（控制台输出）
3. 日志滚动到「对比版本」阶段时，你会看到：

```text
[Pipeline] { (对比版本)
[Pipeline] sh
+ echo ======== 本次发布内容 ========
======== 本次发布内容 ========
+ for svc in $SERVICES
...
mall-user:  prod 当前 v1  =>  将发布 a1b2c3d
mall-order:  prod 当前 v1  =>  将发布 a1b2c3d
```

4. 紧接着流水线进入「发布审批」阶段并暂停，日志末尾出现：

```text
[Pipeline] { (发布审批)
[Pipeline] timeout
Timeout set to expire in 30 min
[Pipeline] {
[Pipeline] input
确认将上述版本发布到生产环境？
发布 or Abort
```

> 💡 审批入口有好几个，任选其一（不同版本菜单位置可能略有差异）：
> - Console Output 日志末尾的 **发布** / **Abort** 链接
> - 构建页面左侧菜单的 **Paused for Input**（等待输入）
> - 构建页面的流水线阶段图中，「发布审批」阶段上的确认提示

5. 这次点击 **Abort**（中止）。日志末尾变为：

```text
Rejected by admin
...
Finished: ABORTED
```

6. 打开 Gitea 的 mall-deploy 仓库提交历史（http://host.docker.internal:3000/mall/mall-deploy/commits/branch/main ），确认**没有**新增任何提交。

> ✅ 检查点：中止的构建显示为灰色（ABORTED），Git 没有任何变化——"不批准就什么都不会发生"。

### 步骤 2：正式发布

1. 再次点击 **Build Now**，生成构建 `#2`
2. 打开 `#2` 的 Console Output，确认版本对比内容无误
3. 这次点击 **发布**
4. 日志继续执行「更新生产配置」，最终输出（凭据会被 Jenkins 自动打码为 `****`）：

```text
Approved by admin
[Pipeline] { (更新生产配置)
[Pipeline] withCredentials
Masking supported pattern matches of $GIT_USER or $GIT_TOKEN
[Pipeline] sh
+ git add envs/prod
+ git diff --cached --quiet
+ git commit -m 'release(prod): promote a1b2c3d from dev'
[detached HEAD 9f8e7d6] release(prod): promote a1b2c3d from dev
 2 files changed, 2 insertions(+), 2 deletions(-)
+ git push http://****:****@host.docker.internal:3000/mall/mall-deploy.git HEAD:main
To http://host.docker.internal:3000/mall/mall-deploy.git
   7e8f9a0..9f8e7d6  HEAD -> main
...
生产配置已更新，请到 Argo CD 中检查差异（OutOfSync）后点击 Sync 完成发布
Finished: SUCCESS
```

5. 回到 Gitea 的 mall-deploy 提交历史，最新一条是 `release(prod): promote a1b2c3d from dev`，作者 `jenkins`。点进去可以看到只改了两行：

```diff
 image:
   repository: localhost:5000/mall/mall-user
-  tag: "v1"
+  tag: "a1b2c3d"
```

> ✅ 检查点：Gitea 中出现 `release(prod)` 提交，`envs/prod/mall-user.yaml` 和 `envs/prod/mall-order.yaml` 的 tag 都已变为 dev 的短哈希。**此时 prod 集群还没有任何变化**——请继续往下。

### 步骤 3：在 Argo CD 中查看差异

1. 浏览器打开 http://argocd.localhost ，用 `admin` 登录
2. 等待最多 60 秒（我们把同步检查周期设成了 60 秒），或者点击应用卡片上的 **REFRESH**（刷新）
3. 应用列表中你会看到：

| 应用 | Sync Status（同步状态） | Health（健康状态） |
|---|---|---|
| mysql-prod | Synced（已同步） | Healthy |
| mall-user-prod | **OutOfSync**（未同步，黄色） | Healthy |
| mall-order-prod | **OutOfSync**（未同步，黄色） | Healthy |

> 🧠 原理：Health 仍是 Healthy，因为集群中正在运行的 v1 本身没毛病；OutOfSync 的意思是"Git 里描述的期望状态 ≠ 集群里的实际状态"。手动同步的应用只会提示、不会自作主张。

4. 点击 `mall-user-prod` 进入详情页，点击顶部的 **APP DIFF**（新版本界面中按钮名为 **DIFF**，查看差异）
5. 差异中应该只有两处变化（勾选 **Compact diff** 只显示改动行）：

```diff
       containers:
         - env:
             - name: APP_VERSION
-              value: v1
+              value: a1b2c3d
 ...
-          image: 'localhost:5000/mall/mall-user:v1'
+          image: 'localhost:5000/mall/mall-user:a1b2c3d'
```

> ⚠️ 发布前看 DIFF 是一个必须养成的习惯。如果 DIFF 里出现了你没预料到的变化（比如副本数、资源配额、某个配置项），**先停下来搞清楚再发**——那说明 mall-deploy 中混入了别人的改动。

### 步骤 4：打开两个"观察窗口"

为了亲眼看到滚动更新，先在发布前开好两个 Git Bash 窗口。

**窗口 A：实时观察 Pod 变化**

```bash
# 位置：任意目录（窗口 A）
kubectl get pods -n mall-prod -l app=mall-user -w
```

`-w`（watch）会持续输出 Pod 的每一次状态变化，按 `Ctrl+C` 退出。

**窗口 B：每 0.5 秒请求一次版本接口**

```bash
# 位置：任意目录（窗口 B）
while true; do curl -s -w "  [%{http_code}]\n" http://mall.localhost/api/users/version; sleep 0.5; done
```

`-w "  [%{http_code}]\n"` 会在响应体后面打印 HTTP 状态码，方便发现失败的请求。此时输出全部是 `v1`：

```text
{"service":"mall-user","version":"v1","host":"mall-user-5d8f9c7b6-aaaaa"}  [200]
{"service":"mall-user","version":"v1","host":"mall-user-5d8f9c7b6-bbbbb"}  [200]
```

注意 `host` 在两个 Pod 之间交替出现——这就是 Service 的负载均衡。

### 步骤 5：同步 mall-user-prod

1. 回到 Argo CD 的 `mall-user-prod` 详情页，点击顶部 **SYNC**（同步）
2. 右侧弹出同步面板，保持默认选项（不勾选 PRUNE、DRY RUN、FORCE 等），点击面板顶部的 **SYNCHRONIZE**（开始同步）
3. 立即切到窗口 A，你会看到类似下面的滚动过程：

```text
NAME                         READY   STATUS              RESTARTS   AGE
mall-user-5d8f9c7b6-aaaaa    1/1     Running             0          2d
mall-user-5d8f9c7b6-bbbbb    1/1     Running             0          2d
mall-user-7f6c5d4e3-ccccc    0/1     Pending             0          0s     ← maxSurge=1：先多起 1 个新 Pod（共 3 个）
mall-user-7f6c5d4e3-ccccc    0/1     ContainerCreating   0          0s
mall-user-7f6c5d4e3-ccccc    0/1     Running             0          2s     ← 已启动但未就绪，不接收流量
mall-user-7f6c5d4e3-ccccc    1/1     Running             0          28s    ← 就绪探针通过，开始接收流量
mall-user-5d8f9c7b6-aaaaa    1/1     Terminating         0          2d     ← 这时才下线 1 个旧 Pod
mall-user-7f6c5d4e3-ddddd    0/1     Pending             0          0s     ← 起第 2 个新 Pod
mall-user-7f6c5d4e3-ddddd    0/1     ContainerCreating   0          0s
mall-user-7f6c5d4e3-ddddd    0/1     Running             0          2s
mall-user-7f6c5d4e3-ddddd    1/1     Running             0          27s
mall-user-5d8f9c7b6-bbbbb    1/1     Terminating         0          2d     ← 下线最后 1 个旧 Pod
```

4. 同时窗口 B 中，新旧版本会**交替出现一小段时间**，然后全部变为新版本：

```text
{"service":"mall-user","version":"v1","host":"mall-user-5d8f9c7b6-bbbbb"}  [200]
{"version":"a1b2c3d","service":"mall-user","message":"...","host":"mall-user-7f6c5d4e3-ccccc"}  [200]   ← 新旧并存
{"service":"mall-user","version":"v1","host":"mall-user-5d8f9c7b6-bbbbb"}  [200]
{"version":"a1b2c3d","service":"mall-user","message":"...","host":"mall-user-7f6c5d4e3-ddddd"}  [200]
{"version":"a1b2c3d","service":"mall-user","message":"...","host":"mall-user-7f6c5d4e3-ccccc"}  [200]   ← 全部切换完成
```

5. 按同样的方法 SYNC `mall-order-prod`（可以把窗口 A 的 `-l app=mall-user` 改成 `-l app=mall-order` 观察）

> ⚠️ 窗口 B 中如果偶尔出现 1～2 个 `[502]` 或 `[000]`，不要慌，原因在下面的"原理"中解释，课后练习 1 会带你修复它。

### 步骤 6：验证发布结果

```bash
# 位置：任意目录
kubectl rollout status deployment/mall-user -n mall-prod
kubectl rollout status deployment/mall-order -n mall-prod
kubectl get deploy -n mall-prod -o custom-columns='NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image,READY:.status.readyReplicas'
```

预期输出：

```text
deployment "mall-user" successfully rolled out
deployment "mall-order" successfully rolled out
NAME         IMAGE                                     READY
mall-order   localhost:5000/mall/mall-order:a1b2c3d    2
mall-user    localhost:5000/mall/mall-user:a1b2c3d     2
```

再做一次**冒烟测试**（用最核心的业务流程确认服务真的可用）：

```bash
# 位置：任意目录
# 1) 创建用户，记下返回的 id
curl -s -X POST http://mall.localhost/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"prod-smoke-1","nickname":"smoke"}'; echo

# 2) 用上一步返回的 id 下单（这里假设返回的 id 是 1，请替换成你的实际值）
curl -s -X POST http://mall.localhost/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"productName":"keyboard","amount":299.00}'; echo
```

预期输出（字段顺序可能不同）：

```text
{"id":1,"username":"prod-smoke-1","nickname":"smoke","createdAt":"2026-10-12T20:15:30.123"}
{"id":1,"userId":1,"username":"prod-smoke-1","productName":"keyboard","amount":299.00,"createdAt":"2026-10-12T20:15:41.456"}
```

> ⚠️ 如果第 1 步返回 `409`（用户名已存在），把 `prod-smoke-1` 换成别的名字即可。

> ✅ 检查点：Argo CD 中 `mall-user-prod`、`mall-order-prod` 都是 **Synced + Healthy**；prod 的版本接口返回新 tag；冒烟测试下单成功（订单服务成功调用了用户服务）。

### 🧠 原理：滚动更新为什么能做到零停机

关键是两个参数和一个探针的配合（见 [deployment.yaml](../mall-deploy/charts/springboot-app/templates/deployment.yaml)）：

```yaml
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1          # 更新期间最多比期望副本数多 1 个
      maxUnavailable: 0    # 更新期间可用副本数一个都不能少
  ...
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: http
```

```text
 时间 ──────────────────────────────────────────────────────────────────────▶
 期望副本=2      t0          t1              t2          t3              t4
 旧 Pod A   ■ 接流量 ───────────────────────── ✖ 下线
 旧 Pod B   ■ 接流量 ──────────────────────────────────────────────────── ✖ 下线
 新 Pod C               □ 启动中(不接流量) ─ ■ 就绪,接流量 ───────────────────────▶
 新 Pod D                                               □ 启动中 ─ ■ 就绪,接流量 ─▶
 可用副本数      2           2               2(A下线前C已就绪)   2            2
                             ▲                               ▲
                     maxSurge=1 允许先多起 1 个     maxUnavailable=0：
                                                   必须等新 Pod 就绪才能下线旧 Pod
```

- **readinessProbe（就绪探针）** 决定 Pod 什么时候被加入 Service 的后端列表：Spring Boot 启动完成前，`/actuator/health/readiness` 不返回 UP，Pod 就不会收到任何流量
- **maxUnavailable: 0** 保证"先有新的、才去旧的"，可用实例数始终不低于 2
- 所以对用户来说，任何时刻都有就绪的 Pod 在服务

**那偶发的 502 是哪来的？** 旧 Pod 被删除时，有两件事**同时**发生：① kubelet 给容器发送停止信号，Spring Boot 开始优雅关闭；② Endpoint 控制器把它从 Service 后端列表摘除，Traefik 随后更新路由。如果 ① 比 ② 快，就会有极少数请求被转发到一个已经不再接收新请求的 Pod。生产上的通用解法是给容器加一个 `preStop` 钩子先等几秒，让 ② 先完成——见课后练习 1。

---

## 13.8 GitOps 下的三种回滚方式

发布总有失败的时候。在 GitOps 体系下，"回滚"有三种做法，它们的地位完全不同：

| 方式 | 操作 | 速度 | 与 Git 一致？ | 适用场景 | 评价 |
|---|---|---|---|---|---|
| **① git revert** | 在 mall-deploy 中撤销出问题的提交并 push | 1 分钟内（等 Argo CD 检测 + 同步） | ✅ 始终一致 | 所有环境的常规回滚 | **首选** |
| **② Argo CD History and Rollback** | 在 Argo CD 中选择历史版本回滚 | 最快（立即执行） | ❌ 回滚后集群与 Git 不一致，必须补一次 git revert | 生产紧急止血；**仅限未开启自动同步的应用** | 应急手段 |
| **③ kubectl rollout undo / set image** | 直接改集群 | 快 | ❌ 不一致 | — | **反模式**：开启 selfHeal 的应用会被立刻改回；未开启的则造成"配置漂移" |

```mermaid
flowchart TD
    q0(["发现线上问题"]):::warn --> q1{"应用开启了自动同步？"}
    q1 -- "是（mall-*-dev）" --> r1["① git revert 出问题的提交<br/>push 后 Argo CD 自动回滚"]:::focus
    q1 -- "否（mall-*-prod）" --> q2{"需要秒级止血？"}
    q2 -- "否" --> r2["① git revert 并 push<br/>然后在 Argo CD 中 SYNC"]:::focus
    q2 -- "是" --> r3["② Argo CD History and Rollback<br/>立即恢复上一个版本"]:::cd
    r3 --> r4["⚠️ 紧接着补做 ①<br/>让 Git 回到同一版本"]:::warn
    bad["③ kubectl rollout undo"]:::warn -. "不要用" .-> q0

    classDef warn fill:#450a0a,stroke:#f87171,stroke-width:2px,color:#fef2f2
    classDef focus fill:#713f12,stroke:#facc15,stroke-width:3px,color:#fefce8
    classDef cd fill:#4a2c12,stroke:#fb923c,stroke-width:2px,color:#fff7ed
```

> 🧠 原理：**Git 是唯一事实来源（Single Source of Truth）**。只要集群状态和 Git 不一致，就意味着"下一次有人点 SYNC，回滚就会被悄悄撤销，坏版本会重新上线"。所以无论用哪种方式止血，最终都必须让 Git 回到正确版本。

另外，Argo CD 会**主动拒绝**对开启了自动同步的应用执行 History and Rollback，报错信息为：

```text
rollback cannot be initiated when auto-sync is enabled
```

原因很简单：回滚刚执行完，自动同步就会把应用重新同步到 Git 的最新版本，回滚毫无意义。

下面用三个演练把这三种方式都亲手做一遍。

---

## 13.9 故障演练 1：dev 配置写错 → git revert 回滚

**场景**：有人修改了 dev 环境的配置，不小心把数据库名写错了。

### 步骤 1：制造故障

```bash
# 位置：/d/cicd-lab/mall-deploy
git pull
sed -i 's/DB_NAME: mall_user$/DB_NAME: mall_user_typo/' envs/dev/mall-user.yaml
git diff
```

预期 `git diff` 输出：

```diff
diff --git a/envs/dev/mall-user.yaml b/envs/dev/mall-user.yaml
--- a/envs/dev/mall-user.yaml
+++ b/envs/dev/mall-user.yaml
@@ -9,7 +9,7 @@ port: 8081
 config:
   DB_HOST: mysql
   DB_PORT: "3306"
-  DB_NAME: mall_user
+  DB_NAME: mall_user_typo
   DB_USERNAME: mall
```

提交并推送：

```bash
# 位置：/d/cicd-lab/mall-deploy
git commit -am "test: 故意写错 dev 数据库名（故障演练）"
git push
```

### 步骤 2：观察故障现象

先开一个窗口观察 Pod：

```bash
# 位置：任意目录（窗口 A）
kubectl get pods -n mall-dev -l app=mall-user -w
```

60 秒内 Argo CD 会自动同步（也可以在 `mall-user-dev` 上点 REFRESH 加速）。ConfigMap 内容变了 → `checksum/config` 注解变了 → Deployment 开始滚动更新：

```text
NAME                         READY   STATUS             RESTARTS      AGE
mall-user-7f6c5d4e3-xxxxx    1/1     Running            0             1h     ← 旧 Pod：继续正常服务
mall-user-6b5a4c3d2-yyyyy    0/1     Pending            0             0s     ← 新 Pod（错误配置）
mall-user-6b5a4c3d2-yyyyy    0/1     ContainerCreating  0             0s
mall-user-6b5a4c3d2-yyyyy    0/1     Running            0             2s
mall-user-6b5a4c3d2-yyyyy    0/1     Error              0             15s    ← 连不上数据库，进程退出
mall-user-6b5a4c3d2-yyyyy    0/1     Running            1 (3s ago)    18s    ← 自动重启
mall-user-6b5a4c3d2-yyyyy    0/1     Error              1 (14s ago)   29s
mall-user-6b5a4c3d2-yyyyy    0/1     CrashLoopBackOff   1 (12s ago)   40s    ← 进入退避重启
```

查看新 Pod 崩溃前的日志（把 Pod 名换成你自己的）：

```bash
# 位置：任意目录
kubectl logs -n mall-dev mall-user-6b5a4c3d2-yyyyy --previous | grep -iE "denied|unknown database" | head -3
```

会看到类似这样的错误（具体措辞可能因 MySQL / 驱动版本略有不同）：

```text
java.sql.SQLSyntaxErrorException: Access denied for user 'mall'@'%' to database 'mall_user_typo'
```

> 💡 为什么是 "Access denied" 而不是 "Unknown database"？因为 `mall` 账号只被授权访问 `mall_user` 和 `mall_order` 两个库，MySQL 先做权限检查，所以报的是无权访问。

**关键观察**：这时候服务还好吗？

```bash
# 位置：任意目录
curl -s -w "  [%{http_code}]\n" http://mall-dev.localhost/api/users/version
```

```text
{"service":"mall-user","version":"a1b2c3d","message":"...","host":"mall-user-7f6c5d4e3-xxxxx"}  [200]
```

**服务完全正常！** 因为 `maxUnavailable: 0`，新 Pod 永远不就绪，旧 Pod 就永远不会被下线。错误的发布被"卡"在了半路，用户无感知。

再看 Argo CD 中的 `mall-user-dev`：

| 状态 | 值 | 含义 |
|---|---|---|
| Sync Status | Synced | 集群已经与 Git（错误的配置）一致 |
| Health | **Progressing**（约 10 分钟后变为 **Degraded**） | Deployment 卡在滚动更新中；超过 `progressDeadlineSeconds`（默认 600 秒）后被判定为失败 |

> 🧠 原理：Synced 只代表"集群和 Git 一样"，不代表"服务健康"。看发布结果一定要同时看 **Sync Status** 和 **Health** 两个维度。

### 步骤 3：用 git revert 回滚

先确认要撤销的是哪个提交：

```bash
# 位置：/d/cicd-lab/mall-deploy
git log --oneline -3
```

```text
4a5b6c7 (HEAD -> main, origin/main) test: 故意写错 dev 数据库名（故障演练）
9f8e7d6 release(prod): promote a1b2c3d from dev
7e8f9a0 deploy(dev): mall a1b2c3d
```

> ⚠️ 务必确认最上面一条就是你的故障提交。如果这期间 Jenkins 又推送了新的 deploy 提交，就不能用 `HEAD`，要用故障提交的哈希：`git revert --no-edit 4a5b6c7`。

```bash
# 位置：/d/cicd-lab/mall-deploy
git revert --no-edit HEAD
git push
git log --oneline -2
```

```text
[main 8b9c0d1] Revert "test: 故意写错 dev 数据库名（故障演练）"
 1 file changed, 1 insertion(+), 1 deletion(-)
...
8b9c0d1 (HEAD -> main, origin/main) Revert "test: 故意写错 dev 数据库名（故障演练）"
4a5b6c7 test: 故意写错 dev 数据库名（故障演练）
```

`git revert` 不会删除历史，而是**新增一个"反向提交"**——故障本身和修复过程都完整地留在了历史里。

### 步骤 4：观察恢复

Argo CD 同步后，窗口 A 中：

```text
mall-user-6b5a4c3d2-yyyyy    0/1     Terminating        5 (40s ago)   4m     ← 错误的新 Pod 被删除
```

旧 Pod 安然无恙。原因是：配置恢复后，Pod 模板（包括 `checksum/config`）与旧的 ReplicaSet **完全相同**，K8s 直接把 Deployment 指回旧 ReplicaSet，不需要重新启动任何 Pod。

> ✅ 检查点：`mall-user-dev` 恢复 **Synced + Healthy**；`kubectl get pods -n mall-dev -l app=mall-user` 只剩 1 个 1/1 Running 的 Pod；整个演练过程中 dev 接口始终可用。

整个演练的时序：

```mermaid
---
config:
  theme: dark
---
sequenceDiagram
    autonumber
    actor D as 开发者
    participant G as Gitea<br/>mall-deploy
    participant C as Argo CD
    participant K as K8s<br/>mall-dev
    participant U as 用户请求

    D->>G: push 错误配置（DB_NAME 写错）
    C->>G: 检测到新提交
    C->>K: 自动同步：更新 ConfigMap + Deployment
    K->>K: 启动新 Pod（错误配置）
    K-->>K: 新 Pod 连库失败 → CrashLoopBackOff
    U->>K: 请求
    K-->>U: 旧 Pod 正常响应（maxUnavailable=0）
    C-->>D: Health: Progressing → Degraded
    D->>G: git revert + push
    C->>G: 检测到 revert 提交
    C->>K: 自动同步：配置恢复
    K->>K: Deployment 指回旧 ReplicaSet，删除故障 Pod
    C-->>D: Synced + Healthy
```

mall-deploy 仓库的提交历史变化：

```mermaid
---
config:
  theme: dark
---
gitGraph
    commit id: "deploy(dev): a1b2c3d"
    commit id: "release(prod): a1b2c3d"
    commit id: "test: 写错 DB_NAME" type: REVERSE
    commit id: "Revert test: 写错 DB_NAME" type: HIGHLIGHT
```

---

## 13.10 故障演练 2：prod 用 Argo CD 紧急回滚 → 补齐 Git

**场景**：刚发布到生产的 `a1b2c3d` 被用户反馈有严重问题，需要**立刻**恢复到 `v1`。

### 步骤 1：用 Argo CD 回滚 mall-user-prod

1. Argo CD 中进入 `mall-user-prod` 详情页
2. 点击顶部 **HISTORY AND ROLLBACK**（历史与回滚）
3. 弹出的列表中是这个应用每一次同步的记录，每条包含 Revision（Git 提交哈希）、部署时间等信息。你应该能看到至少两条：
   - 较新的一条：本章 13.7 中同步的 `release(prod)` 提交
   - 较旧的一条：第 11 章首次同步时的提交（对应 `v1`）
4. 在**较旧的那一条**右侧点击 **⋮**（更多操作）→ **Rollback**（回滚）
5. 弹出确认框，点击 **OK**（不同版本菜单位置可能略有差异）

切到观察窗口，你会看到 mall-prod 中 mall-user 又进行了一次滚动更新，版本接口回到 `v1`：

```bash
# 位置：任意目录
curl -s http://mall.localhost/api/users/version; echo
```

```text
{"service":"mall-user","version":"v1","host":"mall-user-5d8f9c7b6-eeeee"}
```

止血成功。**但是**，回到 Argo CD 应用列表看一眼：

| 应用 | Sync Status | 说明 |
|---|---|---|
| mall-user-prod | **OutOfSync** | 集群是 v1，Git 里还是 a1b2c3d |
| mall-order-prod | Synced | 没动过它 |

```text
             Git（mall-deploy）                       集群（mall-prod）
  ┌────────────────────────────────┐        ┌────────────────────────────────┐
  │ envs/prod/mall-user.yaml       │   ≠    │ mall-user 运行 v1               │  ← OutOfSync
  │   tag: "a1b2c3d"   ← 坏版本！   │        │                                │
  └────────────────────────────────┘        └────────────────────────────────┘
        ⚠️ 此时任何人点一下 SYNC，坏版本 a1b2c3d 就会重新上线！
```

这就是 Argo CD 回滚的代价：**它只改了集群，没改 Git**。现在必须马上补齐 Git。

### 步骤 2：在 Git 中撤销 release 提交

```bash
# 位置：/d/cicd-lab/mall-deploy
git pull
git log --oneline -5 -- envs/prod
```

`-- envs/prod` 表示只看修改过 prod 配置的提交：

```text
9f8e7d6 release(prod): promote a1b2c3d from dev
3c4d5e6 init: mall-deploy
```

撤销这次生产发布：

```bash
# 位置：/d/cicd-lab/mall-deploy
git revert --no-edit 9f8e7d6
git push
grep -H "tag:" envs/prod/*.yaml
```

```text
[main 1c2d3e4] Revert "release(prod): promote a1b2c3d from dev"
 2 files changed, 2 insertions(+), 2 deletions(-)
...
envs/prod/mall-order.yaml:  tag: "v1"
envs/prod/mall-user.yaml:  tag: "v1"
```

### 步骤 3：让两个应用都回到一致状态

在 Argo CD 中对两个 prod 应用分别点击 **REFRESH**，状态变为：

| 应用 | Sync Status | 原因 | 操作 |
|---|---|---|---|
| mall-user-prod | **Synced** | Git 回到 v1，集群也是 v1，两者重新一致 | 无需操作 |
| mall-order-prod | **OutOfSync** | Git 回到 v1，集群还是 a1b2c3d | 点击 **SYNC** → **SYNCHRONIZE** |

> 🧠 为什么 mall-order 也要回滚？因为 `a1b2c3d` 这次发布是 **mall-user 和 mall-order 一起发的**（同一个 release 提交），它们是一个"发布单元"。只回滚其中一个，会产生一个从未在 dev 测试过的版本组合（mall-user v1 + mall-order a1b2c3d）。回滚也要以发布单元为粒度。

> ✅ 检查点：两个 prod 应用都是 **Synced + Healthy**；`envs/prod/*.yaml` 的 tag 都是 `v1`；`curl http://mall.localhost/api/orders/version` 返回 `v1`。

### 步骤 4：把生产恢复到最新版本（为后续章节做准备）

本演练中的"严重问题"是我们假设的。演练结束后，重新执行一次正式发布，让 prod 回到最新版本：

1. Jenkins 中对 `mall-promote-prod` 点 **Build Now** → 审批时点 **发布**
2. Argo CD 中分别 SYNC `mall-user-prod`、`mall-order-prod`

> 💡 你会发现第二次发布的 Git 历史是：`release` → `Revert "release"` → `release`。完整、真实、可追溯——这正是 GitOps 想要的效果。

---

## 13.11 故障演练 3（可选）：kubectl 手工"回滚"会怎样

**场景**：某位同学习惯了直接用 kubectl，想把 dev 的 mall-user 手工切回 v1。

```bash
# 位置：任意目录
# 1) 查看当前镜像
kubectl get deploy mall-user -n mall-dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'

# 2) 手工把镜像改成 v1（容器名是 app）
kubectl set image deployment/mall-user app=localhost:5000/mall/mall-user:v1 -n mall-dev

# 3) 每隔 5 秒看一次镜像，连续看 6 次
for i in 1 2 3 4 5 6; do kubectl get deploy mall-user -n mall-dev -o jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'; sleep 5; done
```

预期输出：

```text
localhost:5000/mall/mall-user:a1b2c3d
deployment.apps/mall-user image updated
localhost:5000/mall/mall-user:v1          ← 手工改动生效了……
localhost:5000/mall/mall-user:a1b2c3d     ← ……几秒后被 Argo CD 改了回去
localhost:5000/mall/mall-user:a1b2c3d
localhost:5000/mall/mall-user:a1b2c3d
localhost:5000/mall/mall-user:a1b2c3d
localhost:5000/mall/mall-user:a1b2c3d
```

（改回的时间点因环境而异，通常在几秒到一分钟之间。）

在 Argo CD 的 `mall-user-dev` 详情页中，你能看到应用短暂变为 OutOfSync 后又自动恢复 Synced，这就是 `selfHeal: true` 的作用。`kubectl rollout undo deployment/mall-user -n mall-dev` 的结果完全一样。

> 🧠 原理：Argo CD 持续比对"Git 中的期望状态"和"集群中的实际状态"，一旦发现偏差（drift），开启 selfHeal 的应用会自动纠正。这保证了**集群永远是 Git 的镜像**，任何绕过 Git 的修改都留不下来。对于没有开启 selfHeal 的 prod 应用，手工改动会留下来，但应用会一直显示 OutOfSync——同样会被发现。

---

## 13.12 发布检查清单（软件工程师视角）

每次发布到生产前后，对照这份清单过一遍：

| 阶段 | 检查项 | 怎么查 |
|---|---|---|
| **发布前** | dev 上验证过本次所有改动（接口、日志无异常） | `curl http://mall-dev.localhost/...`、`kubectl logs -n mall-dev ...` |
| | 清楚本次发布包含哪些提交 | Gitea 中对比 prod 当前 tag 与 dev tag 之间的 mall 提交 |
| | 有没有配置变更？各环境的配置都改了吗 | 对比 `envs/dev` 与 `envs/prod` |
| | 有没有数据库表结构变更？新旧版本代码是否都能兼容新表结构 | 代码评审；生产项目用 Flyway 管理（第 14 章） |
| | 回滚方案是什么？回滚后数据是否兼容 | 默认方案：git revert 本次 release 提交 |
| | 发布时间是否合适（避开业务高峰），相关人员是否知晓 | 团队约定 |
| **发布中** | Argo CD DIFF 中只有预期的变化 | APP DIFF / DIFF |
| | 滚动更新正常推进，没有 CrashLoopBackOff | `kubectl get pods -n mall-prod -w` |
| | 新 Pod 启动日志无 ERROR | `kubectl logs -n mall-prod <新Pod名>` |
| **发布后** | 版本接口返回新 tag | `curl http://mall.localhost/api/users/version` |
| | 核心业务流程冒烟测试通过 | 创建用户 → 下单 |
| | 持续观察 15～30 分钟：错误日志、响应时间、业务指标 | 日志 / 监控（第 14 章进阶） |
| | Argo CD 显示 Synced + Healthy，Git 与集群一致 | Argo CD 应用列表 |

---

## 13.13 常见坑

| 现象 | 原因 | 解决 |
|---|---|---|
| 审批等待期间，其他构建一直在排队（Build Queue） | `input` 位于 `agent any` 的流水线中，等待期间**一直占用一个执行器**；内置节点默认只有 2 个执行器 | 及时审批或中止；也可以在 Manage Jenkins → Nodes → Built-In Node → Configure 中调大 Number of executors。生产实践中会把 `input` 放到不占用执行器的位置（如 stage 级 `agent none`） |
| Argo CD 回滚时报 `rollback cannot be initiated when auto-sync is enabled` | 应用开启了自动同步（如 mall-*-dev） | 自动同步的应用用 git revert 回滚 |
| Argo CD 回滚成功，但过了一会儿坏版本又上线了 | 回滚后没有修正 Git，有人点了 SYNC | 回滚后**立即** git revert 对应的 release 提交 |
| `mall-promote-prod` 推送失败：`! [rejected] HEAD -> main (fetch first)` | 任务检出代码后、推送前，mall 流水线恰好往 mall-deploy 推送了新的 deploy(dev) 提交 | 重新执行一次 `mall-promote-prod` 即可（它会检出最新代码） |
| 本地 `git push` mall-deploy 被拒绝 | 本地仓库落后于远端（Jenkins 推送过） | 先 `git pull` 再改；已经提交了就 `git pull --rebase` 后再 push |
| prod 首次同步或 MySQL 重建后，应用 Pod 反复重启几次才好 | 应用比 MySQL 先启动，连不上数据库就退出，等 MySQL 就绪后重启成功 | 正常现象，等待 1～2 分钟；排查时先看 `kubectl get pods -n mall-prod` 中 `mysql-0` 是否 1/1 |
| 点了 SYNC 之后一直是 Progressing | 新 Pod 起不来（镜像拉不到、配置错误、探针失败） | 按第 14 章的 Pod 排错决策树逐项排查 |
| 发布后 OutOfSync 迟迟不出现 | Argo CD 每 60 秒检查一次 Git | 点击 REFRESH 立即刷新 |
| 发布过程中偶发 502 | Pod 终止与流量摘除存在竞态 | 课后练习 1：添加 preStop |

---

## 本章小结

| 要点 | 一句话总结 |
|---|---|
| 环境晋升 | 同一个镜像按 dev → prod 的顺序逐级通过验证 |
| Build Once, Deploy Many | 生产不重新构建，晋升只是复制 tag |
| 两道审批门 | Jenkins input 决定"发不发"，Argo CD 手动 SYNC 确认"发什么" |
| 滚动更新零停机 | `maxSurge: 1` + `maxUnavailable: 0` + 就绪探针 |
| 错误发布被拦截 | 新 Pod 不就绪，旧 Pod 就不会下线，用户无感知 |
| 回滚首选 git revert | 简单、可追溯、Git 与集群始终一致 |
| Argo CD 回滚是应急手段 | 仅限非自动同步应用，事后必须补齐 Git |
| Git 是唯一事实来源 | 绕过 Git 的修改要么被 selfHeal 改回，要么变成漂移隐患 |

```mermaid
flowchart LR
    subgraph REL["发布"]
        direction TB
        a1["dev 验证通过"]:::app --> a2["mall-promote-prod<br/>对比版本"]:::ci --> a3["input 审批"]:::focus --> a4["release(prod) 提交"]:::scm --> a5["Argo CD DIFF + SYNC"]:::cd --> a6["滚动更新<br/>零停机"]:::app
    end
    subgraph RB["回滚"]
        direction TB
        b1{"自动同步？"}:::cd
        b1 -- "是" --> b2["git revert + push"]:::focus
        b1 -- "否" --> b3["git revert + push + SYNC"]:::focus
        b1 -- "否且十万火急" --> b4["Argo CD Rollback<br/>→ 立即补 git revert"]:::warn
    end
    a6 -. "发现问题" .-> b1

    classDef scm fill:#14352a,stroke:#34d399,stroke-width:2px,color:#ecfdf5
    classDef ci fill:#1e3a5f,stroke:#60a5fa,stroke-width:2px,color:#eff6ff
    classDef cd fill:#4a2c12,stroke:#fb923c,stroke-width:2px,color:#fff7ed
    classDef app fill:#1f2937,stroke:#9ca3af,stroke-width:1px,color:#f9fafb
    classDef focus fill:#713f12,stroke:#facc15,stroke-width:3px,color:#fefce8
    classDef warn fill:#450a0a,stroke:#f87171,stroke-width:2px,color:#fef2f2
    style REL fill:#0b1220,stroke:#facc15,color:#fde68a
    style RB fill:#0b1220,stroke:#f87171,color:#fca5a5
```

---

## 课后练习

**练习 1：消灭滚动更新中的偶发 502**

在 mall-deploy 的 [deployment.yaml](../mall-deploy/charts/springboot-app/templates/deployment.yaml) 中，给容器加一个 `preStop` 钩子，并适当延长终止宽限期：

```yaml
    spec:
      terminationGracePeriodSeconds: 40      # 与 containers 同级
      containers:
        - name: app
          # ……原有内容保持不变……
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]   # 先等 10 秒，让 Service/Traefik 摘除流量
```

提交 → 观察 dev 自动生效 → 到 Argo CD 中对 `mall-user-prod`、`mall-order-prod` 执行 Sync → 重复 13.7 的观察实验，看 502 是否消失。

> 💡 注意这里**不需要**运行 `mall-promote-prod`：晋升流水线只搬运镜像 tag，而 Chart 模板是 dev 和 prod **共用**的——模板一改，prod 应用会直接变成 OutOfSync。这也提醒我们：修改共享 Chart 等于同时修改所有环境，生产团队通常会给 Chart 打版本号，让各环境按需升级。

思考：为什么 `terminationGracePeriodSeconds` 要大于 `sleep` 的时间加上 Spring Boot 优雅停机的时间？

**练习 2：限制谁能审批**

把 `input` 改为只允许指定用户审批：

```groovy
input message: '确认将上述版本发布到生产环境？', ok: '发布', submitter: 'admin'
```

在 Jenkins 中再创建一个普通用户，用它登录后尝试审批，观察结果。

**练习 3：思考题**

既然 Jenkins 里已经有人审批过了，为什么 prod 的 Argo CD 应用还要设置成手动同步？如果改成自动同步，流程上会少什么、多什么风险？

---

下一章 👉 [第 14 章 排错手册、资源清理与进阶路线](14-troubleshooting.md)
