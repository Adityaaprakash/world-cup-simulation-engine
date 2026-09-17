FROM eclipse-temurin:22-jdk-alpine AS builder

WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
# Resolve dependencies to improve caching
RUN ./mvnw dependency:go-offline -B -DskipTests

COPY src ./src
RUN ./mvnw package -DskipTests

# Run stage
FROM eclipse-temurin:22-jre-alpine
WORKDIR /app

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -q -O - http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
