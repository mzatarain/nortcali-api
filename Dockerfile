# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar wrapper y pom primero para aprovechar caché de capas:
# si solo cambia código fuente, esta capa no se re-ejecuta.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Usuario no-root
RUN addgroup -S nortcali && adduser -S nortcali -G nortcali
RUN mkdir -p logs && chown -R nortcali:nortcali /app

USER nortcali

COPY --from=builder /app/target/*.jar app.jar

# Perfil prod por defecto — docker-compose puede sobreescribirlo.
# PRODUCTION_ENV activa el guard de arranque en NortcaliApiApplication.
ENV SPRING_PROFILES_ACTIVE=prod
ENV PRODUCTION_ENV=true

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-jar", "app.jar"]
