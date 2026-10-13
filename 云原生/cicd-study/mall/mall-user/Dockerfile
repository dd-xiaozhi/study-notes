# ============================================================
# mall-user 镜像
# 前置条件：已执行 mvn package，生成 target/mall-user.jar
# 构建命令（在 mall 目录下）：docker build -t mall/mall-user:local mall-user
# ============================================================

# ---------- 第 1 阶段：把 fat jar 按"变化频率"拆成多层 ----------
FROM eclipse-temurin:21-jre AS builder
WORKDIR /builder
COPY target/mall-user.jar application.jar
# Spring Boot 官方推荐的分层解压：依赖层 / 加载器层 / 快照依赖层 / 业务代码层
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- 第 2 阶段：运行时镜像 ----------
FROM eclipse-temurin:21-jre
WORKDIR /application

# 使用非 root 用户运行，降低容器被攻破后的风险
RUN groupadd --system spring && useradd --system --gid spring spring

# 越不常变的层越靠前：只改业务代码时，前三层都能命中缓存，推送/拉取只传最后一层
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./

USER spring
EXPOSE 8081
# 让 JVM 按容器内存上限计算堆大小（K8s 中由 resources.limits.memory 决定）
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "application.jar"]
