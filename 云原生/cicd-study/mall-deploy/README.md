# mall-deploy · GitOps 配置仓库

这个仓库**只放"部署配置"，不放业务代码**。Argo CD 持续监听本仓库，把集群状态同步成仓库描述的样子。

```
mall-deploy/
├── charts/springboot-app/   # 通用 Spring Boot Helm Chart（所有服务共用）
├── envs/
│   ├── dev/                 # 开发环境 values：image.tag 由 mall 流水线自动更新
│   └── prod/                # 生产环境 values：image.tag 由 promote-prod 流水线从 dev 晋升
├── infra/mysql/             # MySQL（StatefulSet + Headless Service + Secret + 初始化脚本）
├── argocd/                  # Argo CD Application 定义（dev 自动同步 / prod 手动同步）
└── jenkins/                 # 生产发布（晋升）流水线
```

谁会修改这个仓库：

| 修改方 | 修改内容 | 触发效果 |
|---|---|---|
| Jenkins `mall` 流水线 | `envs/dev/*.yaml` 的 `image.tag` | Argo CD 自动同步到 `mall-dev` |
| Jenkins `mall-promote-prod` 流水线 | `envs/prod/*.yaml` 的 `image.tag` | Argo CD 显示 OutOfSync，人工 Sync 后发布到 `mall-prod` |
| 开发/运维人员 | Chart 模板、副本数、资源配额等 | 同上，按环境生效 |
