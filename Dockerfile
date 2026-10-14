# Imagen autonoma: compila dentro de Docker (no necesitas Java instalado en la instancia).
# En una t3.micro (1 GB) agrega swap antes de construir (ver README, seccion "Desplegar en EC2").
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-Xmx256m"
EXPOSE 8086
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
