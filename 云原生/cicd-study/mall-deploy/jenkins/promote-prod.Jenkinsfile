// ============================================================
// 生产发布流水线：把 dev 环境验证通过的镜像 tag "晋升"到 prod
//   核心原则 —— Build Once, Deploy Many：生产环境用的是和 dev 完全相同的镜像，绝不重新构建
//   Jenkins 只负责"审批 + 修改 Git"，真正的发布由 Argo CD 中的人工 Sync 完成
// ============================================================
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

    stages {
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

        stage('发布审批') {
            steps {
                // 流水线在这里暂停，等待有权限的人点击"发布"或"中止"
                timeout(time: 30, unit: 'MINUTES') {
                    input message: '确认将上述版本发布到生产环境？', ok: '发布'
                }
            }
        }

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
    }

    post {
        success { echo '生产配置已更新，请到 Argo CD 中检查差异（OutOfSync）后点击 Sync 完成发布' }
    }
}
