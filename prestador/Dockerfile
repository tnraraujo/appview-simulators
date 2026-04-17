FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/prestador-0.0.1-SNAPSHOT.jar app.jar
ENV PORT=8085
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
