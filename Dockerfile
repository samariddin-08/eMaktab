# =========================================================================
# Stage 1: Build the application (Compile and package)
# =========================================================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy dependency files first to utilize Docker layer caching
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/

# Ensure lines end properly and flag file as executable
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copy source code and build the final jar file
COPY src ./src
RUN ./mvnw clean package -DskipTests

# =========================================================================
# Stage 2: Runtime environment (Keep it small to fit Render's 512MB RAM limit)
# =========================================================================
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the compiled jar from the build stage
COPY --from=build /app/target/eMaktab-0.0.1-SNAPSHOT.jar app.jar

# Enforce strict memory controls so the JVM doesn't exceed 512MB RAM
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+UseContainerSupport"

EXPOSE 8080

# Execute the application
CMD ["java", "-jar", "app.jar"]
