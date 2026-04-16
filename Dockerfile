# ─── Stage 1: Build ───────────────────────────────────────────────────────────
# Compila el JAR con Maven y Java 21
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar el wrapper y el pom primero para aprovechar la caché de capas de Docker:
# si solo cambia código fuente (no dependencias), esta capa no se re-ejecuta.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copiar fuentes y compilar
COPY src ./src
RUN ./mvnw clean package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
# Imagen mínima JRE 21 sin herramientas de desarrollo
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Usuario no-root para mayor seguridad
RUN addgroup -S nortcali && adduser -S nortcali -G nortcali

# Directorio para logs del perfil prod
RUN mkdir -p logs && chown -R nortcali:nortcali /app

USER nortcali

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082

# Health check usando el endpoint de Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
