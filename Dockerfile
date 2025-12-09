# Dockerfile para Render.com
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copiar archivos de configuración de Gradle
COPY gradle ./gradle
COPY gradlew ./gradlew
COPY build.gradle.kts ./build.gradle.kts
COPY settings.gradle.kts ./settings.gradle.kts

# Dar permisos de ejecución a gradlew
RUN chmod +x ./gradlew

# Copiar código fuente
COPY src ./src

# Construir la aplicación
RUN ./gradlew build -x test --no-daemon

# Imagen final
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR construido
COPY --from=build /app/build/libs/*.jar app.jar

# Exponer el puerto (Render usa la variable PORT automáticamente)
EXPOSE 10000

# Comando para ejecutar la aplicación
# Render proporciona la variable PORT automáticamente
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]

