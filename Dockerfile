# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# 先只拷贝 pom，依赖没变时复用缓存层
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 日志目录：logback 默认写到 logs/，容器内需提前建好并给权限
RUN mkdir -p /app/logs \
    && addgroup -S app && adduser -S -G app app \
    && chown -R app:app /app

COPY --from=build /build/target/*.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 9000

USER app

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
