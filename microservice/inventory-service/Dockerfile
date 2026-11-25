FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY infrastructure ./infrastructure
COPY microservice ./microservice
RUN mvn -B -ntp -pl microservice/inventory-service -am package -DskipTests
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/microservice/inventory-service/target/inventory-service.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","/app/app.jar"]
