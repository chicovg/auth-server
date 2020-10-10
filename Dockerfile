FROM openjdk:8-alpine

COPY target/uberjar/auth-server.jar /auth-server/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/auth-server/app.jar"]
