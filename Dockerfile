# Dockerfile para Render.com
# Usa Gradle directamente (no necesita gradlew ni gradle.properties)

FROM gradle:8.9-jdk21-alpine AS build

WORKDIR /app

# Copiar archivos de configuración PRIMERO (para mejor cache de Docker)
COPY build.gradle.kts ./build.gradle.kts
COPY settings.gradle.kts ./settings.gradle.kts

# Copiar código fuente DESPUÉS (cambia más frecuentemente)
COPY src ./src

# Construir la aplicación (Gradle ya está instalado en la imagen)
RUN gradle build -x test --no-daemon

# Imagen final (más liviana, solo JRE)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR construido
COPY --from=build /app/build/libs/*.jar app.jar

# Exponer el puerto (Render usa la variable PORT automáticamente)
EXPOSE 10000

# Comando para ejecutar la aplicación
# Render proporciona la variable PORT automáticamente
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]
