FROM maven:3-eclipse-temurin-26-alpine AS build-env

WORKDIR /app

# Copy pom files only — Docker layer cache: dep resolution is skipped entirely when poms are unchanged
COPY pom.xml .
COPY ecommerce-frontend/pom.xml ecommerce-frontend/
COPY ecommerce-backend/pom.xml ecommerce-backend/

# Pre-download Java dependencies; /root/.m2 is a BuildKit cache and persists between rebuilds
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B --no-transfer-progress

# Copy full source
COPY . .

# Build — three persistent BuildKit cache mounts avoid re-downloading on every run:
#   /root/.m2              Maven local repository
#   /root/.npm             npm package download cache (used by npm ci)
#   ecommerce-frontend/node     Node.js binary; frontend-maven-plugin skips download when present
ARG MAVEN_OPTS
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    --mount=type=cache,target=/app/ecommerce-frontend/node \
    mvn clean package -Dmaven.test.skip=true --no-transfer-progress

FROM eclipse-temurin:25-jre-alpine AS runtime

ENV TZ=Asia/Tehran

WORKDIR /usr/local/app/

# Single COPY into the final name — a follow-up `RUN mv` would duplicate the ~100MB jar layer.
COPY --from=build-env /app/ecommerce-backend/target/ecommerce-backend-*.jar /usr/local/app/ecom.jar

EXPOSE 8080

CMD ["sh", "-c", "java ${JVM_OPTS} -jar /usr/local/app/ecom.jar"]
