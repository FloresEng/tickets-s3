# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar archivos de configuración de dependencias 
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el JAR omitiendo tests
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Etapa de ejecución 
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el ejecutable .jar 
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto 
EXPOSE 8082

# Comando de arranque 
ENTRYPOINT ["java", "-jar", "app.jar"]