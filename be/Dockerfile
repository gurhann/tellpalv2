FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY pom.xml pom.xml
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
COPY tools/install-reading-webp.sh tools/install-reading-webp.sh
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre AS webp-tools
ARG TARGETARCH
RUN apt-get update && apt-get install -y --no-install-recommends curl ca-certificates gcc libc6-dev && rm -rf /var/lib/apt/lists/*
COPY tools/install-reading-webp.sh /tmp/install-reading-webp.sh
COPY tools/install-reading-supervisor.sh tools/reading-process-supervisor.c /tmp/
RUN case "$TARGETARCH" in arm64) platform=linux-aarch64 ;; amd64) platform=linux-x86-64 ;; *) exit 2 ;; esac \
    && sh /tmp/install-reading-webp.sh "$platform" /opt/tellpal-webp/bin \
    && sh /tmp/install-reading-supervisor.sh /opt/tellpal-supervisor/bin

FROM eclipse-temurin:25-jre
WORKDIR /app
ENV TELLPAL_READING_WEBP_TOOL_DIRECTORY=/opt/tellpal-webp/bin
ENV TELLPAL_READING_SUPERVISOR_DIRECTORY=/opt/tellpal-supervisor/bin
COPY --from=webp-tools /opt/tellpal-webp/ /opt/tellpal-webp/
COPY --from=webp-tools /opt/tellpal-supervisor/ /opt/tellpal-supervisor/

COPY --from=build /workspace/target/tellpal-v2-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
