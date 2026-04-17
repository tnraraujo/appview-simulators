# Primeiro estagio: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia os arquivos de build
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Faz o download das dependÃªncias offline para otimizaÃ§Ã£o
RUN ./gradlew dependencies --no-daemon

# Copia e compila o cÃ³digo fonte
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Segundo estagio: Imagem Final (menor)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# CriaÃ§Ã£o de um usuÃ¡rio nÃ£o-root (boa prÃ¡tica de seguranÃ§a)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia apenas o JAR do estÃ¡gio de build
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# Libera a porta configurada no application.yml do ccm
EXPOSE 8184

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

