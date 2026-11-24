# 1단계: Gradle을 이용한 애플리케이션 빌드 환경 (Java 21)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# 빌드 속도 향상을 위한 레이어 캐싱 (설정 파일 먼저 복사)
COPY gradlew ./gradlew
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 실행 권한 부여 및 의존성 다운로드
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 코드만 고쳐도 의존성 다시 받는것 방지
COPY src ./src

RUN ./gradlew bootJar --no-daemon
# [핵심 수정] plain.jar를 지워버리고, 남은 실제 실행 가능한 JAR 파일 이름을 project.jar로 통일합니다.
RUN mv build/libs/*.jar build/libs/project.jar


# 2단계: 실제 실행 환경 (경량화된 JRE 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 1단계에서 빌드된 jar 파일 복사
#COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
# 명확하게 지정된 project.jar만 컨테이너로 가져옵니다.
COPY --from=build /app/build/libs/project.jar app.jar

# 컨테이너 포트 노출
EXPOSE 8082

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]