FROM openjdk:16
ADD target/scala-2.13/urlshortener_2.13-0.1.0-SNAPSHOT.jar urlshortener.jar
ENTRYPOINT ["java", "-jar","urlshortener.jar"]
EXPOSE 8080