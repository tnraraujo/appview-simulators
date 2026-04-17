# Build Stage
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Copia os arquivos de configuração do Gradle
COPY build.gradle settings.gradle ./
# Copia o código-fonte
COPY src ./src

# Compila o projeto (gera o bootJar)
RUN gradle bootJar --no-daemon

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o JAR do estágio de build (ignorando jars '-plain')
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# Define a porta padrão do simulador
EXPOSE 8186

# Comando de inicialização
ENTRYPOINT ["java", "-jar", "app.jar"]

