# ============================================================
# mall-order 镜像（结构与 mall-user 相同，详细注释见 mall-user/Dockerfile）
# 构建命令（在 mall 目录下）：docker build -t mall/mall-order:local mall-order
# ============================================================
FROM eclipse-temurin:21-jre AS builder
WORKDIR /builder
COPY target/mall-order.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre
WORKDIR /application
RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
USER spring
EXPOSE 8082
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "application.jar"]
