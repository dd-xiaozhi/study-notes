# ============================================================
# 定制 Jenkins 镜像：在官方 LTS 镜像基础上预装流水线需要的工具和插件
# ============================================================
FROM jenkins/jenkins:lts-jdk21

USER root

# 1) Docker CLI + buildx：通过挂载的 docker.sock 调用宿主机 Docker 引擎构建/推送镜像
COPY --from=docker:cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker:cli /usr/local/libexec/docker/cli-plugins/docker-buildx /usr/local/libexec/docker/cli-plugins/docker-buildx

# 2) Maven：直接从官方 Maven 镜像复制，避免 apt 安装缓慢；settings.xml 配置了阿里云镜像
COPY --from=maven:3.9-eclipse-temurin-21 /usr/share/maven /usr/share/maven
RUN ln -s /usr/share/maven/bin/mvn /usr/bin/mvn
COPY settings.xml /usr/share/maven/conf/settings.xml

# 3) yq：在流水线中修改 GitOps 仓库里的 YAML
COPY --from=mikefarah/yq:4 /usr/bin/yq /usr/bin/yq

USER jenkins

# 4) 预装插件。国内下载慢时，可取消下一行注释，使用清华镜像站下载插件
# ENV JENKINS_UC_DOWNLOAD=https://mirrors.tuna.tsinghua.edu.cn/jenkins
COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt
