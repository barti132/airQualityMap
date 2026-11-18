# syntax=docker/dockerfile:1

# ---- Etap 1: build -------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Najpierw tylko pliki z zależnościami: warstwa z pobranym ~/.m2 jest ponownie
# używana, dopóki pom.xml się nie zmieni (zmiana kodu nie pobiera zależności od nowa).
# chmod: w repo mvnw nie ma bitu wykonywalnego (100644), a na Linuxie jest potrzebny.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src src
# Testy uruchamia CI (./mvnw verify), więc obraz ich nie powtarza.
RUN ./mvnw -B -ntp -DskipTests package \
    && cp target/air-quality-map-*.jar app.jar

# ---- Etap 2: runtime -----------------------------------------------------------
FROM eclipse-temurin:21-jre

# Użytkownik nie-root. /data to katalog na plik SQLite (WAL tworzy obok pliki -wal i -shm,
# więc zapisywalny musi być cały katalog, nie tylko plik bazy).
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin app \
    && mkdir /data \
    && chown app:app /data

WORKDIR /app
COPY --from=build --chown=app:app /workspace/app.jar app.jar

USER app

# MaxRAMPercentage zamiast sztywnego -Xmx: sterta wynika z limitu pamięci kontenera
# (mem_limit w Compose), więc obraz nie wymaga przebudowy przy zmianie planu Lightsail.
# ExitOnOutOfMemoryError: po OOM proces ma paść, a Docker go zrestartuje.
ENV AQM_DB_PATH=/data/air-quality.db \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

VOLUME /data
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

# sh -c + exec: JAVA_OPTS jest rozwijane, a java dostaje PID 1 i odbiera SIGTERM
# (graceful shutdown Springa).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
