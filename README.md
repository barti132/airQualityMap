# Air Quality Map

Aplikacja webowa pokazująca na mapie aktualną jakość powietrza w Polsce, na podstawie danych z wielu niezależnych źródeł (stacje oficjalne i czujniki obywatelskie). Docelowo z alertami e-mail, gdy jakość powietrza w wybranym miejscu się pogorszy.

## Status

W budowie. Gotowy jest szkielet backendu: Spring Boot, SQLite z migracjami Flyway, obraz Dockera i lokalne uruchomienie przez Docker Compose. Kolejne kroki: pobieranie danych (GIOŚ, Sensor.Community), API stacji, frontend z mapą, alerty e-mail.

## Stack

- **Backend:** Java 21, Spring Boot, Maven (wrapper `mvnw`), SQLite + Flyway
- **Frontend:** Angular + Leaflet (jeszcze nie utworzony)
- **Uruchomienie:** Docker, Docker Compose
- **Docelowo:** AWS Lightsail, SES, Litestream (backup bazy do S3), GitHub Actions

## Struktura repozytorium

```
backend/              Spring Boot + Maven (Dockerfile w środku)
docker-compose.yml    lokalne uruchomienie backendu
.env.example          szablon zmiennych dla Compose
```

Katalog `frontend/` dojdzie wraz z interfejsem.

## Uruchomienie lokalne

### Docker Compose

Wymagania: Docker z Docker Compose.

```bash
cp .env.example .env            # opcjonalnie; bez .env działają wartości domyślne
docker compose up --build
```

Backend odpowiada na `http://localhost:8080` (port zmienisz przez `BACKEND_PORT` w `.env`). Sprawdzenie, czy działa:

```bash
curl http://localhost:8080/actuator/health
```

Baza SQLite leży w nazwanym wolumenie `aqm-data` (w kontenerze `/data`), więc przeżywa restart i `docker compose down`. Usunięcie danych: `docker compose down -v`.

### Backend bez Dockera

Wymagania: JDK 21.

```bash
cd backend
mkdir -p data                   # katalog na plik bazy; sterownik SQLite go nie tworzy
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Na Windowsie w PowerShell: `$env:SPRING_PROFILES_ACTIVE="local"; .\mvnw.cmd spring-boot:run`.

Domyślna ścieżka bazy jest względna (`./data/air-quality.db`), więc aplikację trzeba uruchamiać z katalogu `backend/` (w IntelliJ: *Working directory* = `backend`). Inną ścieżkę ustawisz zmienną `AQM_DB_PATH`.

## Testy

```bash
cd backend
./mvnw verify
```

Testy integracyjne używają tymczasowego pliku SQLite, więc nie wymagają Dockera ani osobnej bazy i nie dotykają bazy deweloperskiej.

## Konfiguracja

Konfiguracja przez zmienne środowiskowe. Sekretów nie umieszczamy w repozytorium ani w obrazach.

| Zmienna | Gdzie | Domyślnie | Opis |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Compose, backend | `prod` (Compose) | Profil Springa: `local` (logi DEBUG) lub `prod` |
| `BACKEND_PORT` | Compose | `8080` | Port backendu na hoście |
| `BACKEND_MEM_LIMIT` | Compose | `512m` | Limit pamięci kontenera; sterta JVM to 75% tej wartości |
| `AQM_DB_PATH` | backend | `./data/air-quality.db` (w obrazie `/data/air-quality.db`) | Ścieżka do pliku SQLite |

## Baza danych

SQLite w trybie WAL (jeden proces zapisujący), schemat zarządzany migracjami Flyway w `backend/src/main/resources/db/migration`. Czasy są przechowywane jako epoch seconds w UTC.
