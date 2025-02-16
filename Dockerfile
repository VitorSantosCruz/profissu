FROM alpine:latest AS keygen

WORKDIR /tmp

RUN apk add --no-cache openssl && \
    openssl genrsa -out profissu.key 4096 && \
    openssl rsa -in profissu.key -pubout -out profissu.pub

FROM maven:3.9.6-amazoncorretto-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

COPY --from=keygen /tmp/profissu.key /app/src/main/resources/
COPY --from=keygen /tmp/profissu.pub /app/src/main/resources/

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S profissu && adduser -S -G profissu profissu

COPY --from=build --chown=profissu:profissu /app/target/profissu-0.0.1-SNAPSHOT.jar /app/profissu.jar

USER profissu

ENTRYPOINT ["java", "-jar", "profissu.jar"]
