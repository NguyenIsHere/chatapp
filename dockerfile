FROM openjdk:20-jdk

WORKDIR /app

COPY build/libs/chatapp-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
