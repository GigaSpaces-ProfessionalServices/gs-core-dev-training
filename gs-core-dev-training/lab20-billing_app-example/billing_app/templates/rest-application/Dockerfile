FROM eclipse-temurin:17-jdk-jammy AS build
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
