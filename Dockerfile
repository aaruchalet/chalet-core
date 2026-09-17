FROM eclipse-temurin:21-jre

LABEL maintainer="manjeet.kumar"

WORKDIR /opt/chalet-core

COPY --chown=10001:10001 build/libs/*.jar chalet-core.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "chalet-core.jar"]