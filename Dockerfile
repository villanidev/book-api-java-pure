# Stage 1: Build the application
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN mvn clean package

# Stage 2: Build native image
FROM ghcr.io/graalvm/native-image-community:21.0.1-ol9 AS native
WORKDIR /build
COPY --from=build /build/target/*.jar /app.jar
RUN native-image -jar /app.jar --no-fallback

# Stage 3: Create minimal runtime image
FROM oraclelinux:9-slim
WORKDIR /app
COPY --from=native /build/book-api /app/book-api
ENTRYPOINT ["/app/book-api"]
EXPOSE 8080