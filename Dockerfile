# ==========================================
# Etapa 1: Compilación y Construcción
# ==========================================
FROM gradle:8.8-jdk21-alpine AS builder

# Definimos el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos todo el código fuente del proyecto al contenedor
COPY . .

# Ejecutamos la compilación del módulo principal saltándonos los tests
# (Los tests ya los corrimos localmente, así el build en la nube es veloz)
RUN ./gradlew :app-service:bootJar -x test -x validateStructure --no-daemon

# ==========================================
# Etapa 2: Imagen de Ejecución Ligera
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Exponemos el puerto 8080 que es donde corre WebFlux
EXPOSE 8080

# Copiamos el archivo JAR generado en la etapa anterior
# El plugin de Bancolombia genera el JAR en applications/app-service/build/libs/
COPY --from=builder /app/applications/app-service/build/libs/*.jar app.jar

# Comando para arrancar la aplicación de forma óptima
ENTRYPOINT ["java", "-jar", "app.jar"]