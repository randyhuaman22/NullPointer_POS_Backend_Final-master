FROM maven:3.9-eclipse-temurin-21 AS compilacion
WORKDIR /workspace
COPY . .
RUN mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 -pl infrastructure/eureka-server -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /workspace/infrastructure/eureka-server/target/eureka-server.jar /app/aplicacion.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/aplicacion.jar"]
