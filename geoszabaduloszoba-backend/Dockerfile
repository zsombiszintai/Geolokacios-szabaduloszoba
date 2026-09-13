# --- Építési fázis (Build stage) ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Másoljuk a maven/gradle fájlokat a függőségek letöltéséhez
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Másoljuk a forráskódot és lefordítjuk
COPY src ./src
RUN ./mvnw clean package -DskipTests

# --- Futtatási fázis (Runtime stage) ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Átmásoljuk az épített jar fájlt az építési fázisból
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]