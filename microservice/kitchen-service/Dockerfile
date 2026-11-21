FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 -pl microservice/kitchen-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /workspace/microservice/kitchen-service/target/kitchen-service.jar /app/aplicacion.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/aplicacion.jar"]
