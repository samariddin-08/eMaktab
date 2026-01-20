# ... (BUILD BOSQICHI o'zgarishsiz qoladi) ...

# --- YAKUNIY BOSQICH ---
# Faqat JRE (Java Runtime Environment) ni o'z ichiga olgan kichikroq tasvirdan boshlaymiz
FROM openjdk:17.0.10-jre-slim-bullseye  # <--- Shu qatorni o'zgartiring, aniq versiyani kiriting
WORKDIR /app
# Yuqoridagi bosqichda yaratilgan JAR faylini nusxalaymiz
COPY --from=builder /app/target/eMaktab-0.0.1-SNAPSHOT.jar ./eMaktab.jar
# Ilovani ishga tushirish buyrug'i
CMD ["java", "-jar", "eMaktab.jar"]
