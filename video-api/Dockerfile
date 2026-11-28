# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY video-api/pom.xml video-api/
COPY video-processor/pom.xml video-processor/
RUN ./mvnw -B -ntp -pl video-api -am -DskipTests dependency:go-offline
COPY video-api/src/ video-api/src/
RUN ./mvnw -B -ntp -pl video-api -am -DskipTests clean package

FROM eclipse-temurin:25-jre
RUN groupadd -g 1001 fiapx && useradd -u 1001 -g 1001 -M -s /usr/sbin/nologin fiapx
WORKDIR /deployments
COPY --from=build --chown=1001:1001 /src/video-api/target/quarkus-app/lib/ lib/
COPY --from=build --chown=1001:1001 /src/video-api/target/quarkus-app/*.jar ./
COPY --from=build --chown=1001:1001 /src/video-api/target/quarkus-app/app/ app/
COPY --from=build --chown=1001:1001 /src/video-api/target/quarkus-app/quarkus/ quarkus/
USER 1001
EXPOSE 8080

# Sem isto a JVM lê a memória do HOST, não o limite do cgroup, e o pod é morto por OOM
# antes de o GC achar que precisa coletar.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=55", "-jar", "/deployments/quarkus-run.jar"]
