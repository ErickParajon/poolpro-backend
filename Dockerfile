# Dockerfile para Render.com
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Instalar bash (necesario para gradlew)
RUN apk add --no-cache bash# Copiar archivos de configuración de Gradle PRIMERO (para mejor cache)
COPY gradle ./gradle
COPY gradlew ./
COPY gradle.properties ./gradle.properties
COPY build.gradle.kts ./build.gradle.kts
COPY settings.gradle.kts ./settings.gradle.kts

# Dar permisos de ejecución a gradlew
RUN chmod +x ./gradlew

# Copiar código fuente DESPUÉS (cambia más frecuentemente)
COPY src ./src

# Construir la aplicación
RUN ./gradlew build -x test --no-daemon

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
