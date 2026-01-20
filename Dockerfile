# --- BUILD BOSQICHI ---
FROM maven:3.8.1-openjdk-17 AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw clean package -DskipTests

# --- YAKUNIY BOSQICH ---
# Alternativ JRE tasviri, agar yuqoridagisi ishlamasa, bunisini sinab ko'ring
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/eMaktab-0.0.1-SNAPSHOT.jar ./eMaktab.jar
CMD ["java", "-jar", "eMaktab.jar"]
