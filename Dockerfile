# --- BUILD BOSQICHI ---
# Maven va JDK ni o'z ichiga olgan katta tasvirdan boshlaymiz
FROM maven:3.8.1-openjdk-17 AS builder
WORKDIR /app
# Loyiha fayllarini nusxalaymiz
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src
# Ilovani qurish va paketlash
RUN ./mvnw clean package -DskipTests

# --- YAKUNIY BOSQICH ---
# Faqat JRE (Java Runtime Environment) ni o'z ichiga olgan kichikroq tasvirdan boshlaymiz
FROM openjdk:17-jre-slim-bullseye
WORKDIR /app
# Yuqoridagi bosqichda yaratilgan JAR faylini nusxalaymiz
COPY --from=builder /app/target/eMaktab-0.0.1-SNAPSHOT.jar ./eMaktab.jar
# Ilovani ishga tushirish buyrug'i
CMD ["java", "-jar", "eMaktab.jar"]
