# ---------- Étape de build (Maven multi-modules) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copie des POMs seuls pour mettre en cache le téléchargement des dépendances
COPY pom.xml ./
COPY jadwal-common/pom.xml jadwal-common/pom.xml
COPY jadwal-referentiel/pom.xml jadwal-referentiel/pom.xml
COPY jadwal-abonnement/pom.xml jadwal-abonnement/pom.xml
COPY jadwal-enseignant/pom.xml jadwal-enseignant/pom.xml
COPY jadwal-pedagogie/pom.xml jadwal-pedagogie/pom.xml
COPY jadwal-planning/pom.xml jadwal-planning/pom.xml
COPY jadwal-scolarite/pom.xml jadwal-scolarite/pom.xml
COPY jadwal-solver/pom.xml jadwal-solver/pom.xml
COPY jadwal-export/pom.xml jadwal-export/pom.xml
COPY jadwal-api/pom.xml jadwal-api/pom.xml
RUN mvn -B -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline || true

# Copie des sources puis build
COPY jadwal-common/src jadwal-common/src
COPY jadwal-referentiel/src jadwal-referentiel/src
COPY jadwal-abonnement/src jadwal-abonnement/src
COPY jadwal-enseignant/src jadwal-enseignant/src
COPY jadwal-pedagogie/src jadwal-pedagogie/src
COPY jadwal-planning/src jadwal-planning/src
COPY jadwal-scolarite/src jadwal-scolarite/src
COPY jadwal-solver/src jadwal-solver/src
COPY jadwal-export/src jadwal-export/src
COPY jadwal-api/src jadwal-api/src
RUN mvn -B package -DskipTests

# ---------- Étape d'exécution ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S jadwal && adduser -S jadwal -G jadwal
USER jadwal
WORKDIR /app
COPY --from=build /app/jadwal-api/target/jadwal-api-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
