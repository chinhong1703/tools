FROM eclipse-temurin:17-jre as runtime

WORKDIR /app

COPY target/demomodulithapp-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
