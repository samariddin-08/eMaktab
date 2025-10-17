FROM openjdk:17-jdk-slim
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package
CMD ["java", "-jar", "target/eMaktab-0.0.1-SNAPSHOT.jar", "--server.port=$PORT"]
