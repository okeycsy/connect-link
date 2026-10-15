# 외부 미리보기용 이미지. app.jar 하나를 만들어 실행한다.
# 1단계: 프론트 빌드 (Node 22)
FROM node:22-bookworm-slim AS frontend
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2단계: 서버 빌드 (JDK 17). 프론트는 1단계 결과를 그대로 쓴다.
FROM eclipse-temurin:17-jdk AS backend
WORKDIR /src
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY backend/ backend/
COPY frontend/build.gradle.kts frontend/
COPY --from=frontend /src/frontend/dist frontend/dist
RUN chmod +x gradlew && ./gradlew :backend:bootJar -x :frontend:npmRunBuild -x :frontend:npmInstall --no-daemon -q

# 3단계: 실행 (JRE 17)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /src/backend/build/libs/app.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx384m -XX:+UseSerialGC -Duser.timezone=Asia/Seoul"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
