FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# Dá permissão de execução ao gradlew e faz o build do JAR
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# Imagem final
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o JAR do builder para a imagem final
COPY --from=builder /app/build/libs/*.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
