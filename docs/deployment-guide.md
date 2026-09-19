# Wariant wdrożeniowy

Konfiguracja wdrożeniowa jest oddzielona od wygodnego środowiska developerskiego.
Zwykły `compose.yaml` nadal uruchamia Vite i profil `local` z danymi demonstracyjnymi.
Plik `compose.production.yaml` uruchamia cztery usługi:

```text
Internet → Caddy (HTTPS) → Nginx (React + /api proxy) → Spring Boot → PostgreSQL
```

- Caddy jest jedyną usługą wystawiającą porty hosta.
- Nginx serwuje wynik `npm run build`; w kontenerze nie działa serwer developerski.
- Backend i baza są dostępne wyłącznie w prywatnej sieci Compose.
- Frontend i API mają wspólny origin, więc sesja i CSRF działają bez CORS.
- Profil Springa `production` nie uruchamia generatorów danych demonstracyjnych.

## Przygotowanie serwera i domeny

Potrzebny jest serwer z Dockerem i Docker Compose, domena wskazująca rekordem DNS
na ten serwer oraz dostępne z internetu porty 80 i 443. Caddy pobiera i odnawia
certyfikat automatycznie, gdy `APP_ADDRESS` zawiera prawdziwą domenę.

Skopiuj wzór konfiguracji:

```powershell
Copy-Item .env.production.example .env.production
```

W `.env.production` ustaw:

- `APP_ADDRESS` na domenę, np. `workshop.example.com`,
- `HTTP_PORT=80` i `HTTPS_PORT=443`,
- długie, losowe `POSTGRES_PASSWORD`,
- login, e-mail i silne hasło pierwszego administratora,
- `SESSION_COOKIE_SECURE=true`.

Nie commituj `.env.production`. Plik jest ignorowany przez Git. Hasła nie trafiają
do obrazu frontendu ani do kodu dostępnego w przeglądarce.

## Pierwsze uruchomienie

```powershell
docker compose --env-file .env.production -f compose.production.yaml up --build -d --wait
```

Przy pustej bazie backend wykona migracje Flyway i utworzy jednego administratora.
Jeżeli administrator już istnieje, konfiguracja startowa nie zmienia jego loginu,
e-maila ani hasła. Po pierwszym poprawnym uruchomieniu usuń wartości
`BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL` i `BOOTSTRAP_ADMIN_PASSWORD`
z `.env.production`, a następnie uruchom stos ponownie.

Sprawdź:

```powershell
docker compose --env-file .env.production -f compose.production.yaml ps
```

- aplikacja: `https://twoja-domena`,
- health check przez reverse proxy: `https://twoja-domena/api/health`,
- Swagger jest domyślnie wyłączony.

Do zatrzymania użyj:

```powershell
docker compose --env-file .env.production -f compose.production.yaml down
```

To polecenie zachowuje bazę i dane Caddy. Opcji `--volumes` używaj wyłącznie wtedy,
gdy świadomie chcesz trwale usunąć dane wdrożenia.

## Lokalny smoke test wariantu produkcyjnego

Przykładowy plik można wykorzystać do testu HTTP na porcie 8088. Dane w nim nie są
bezpieczne i nie nadają się do publicznego wdrożenia.

```powershell
docker compose -p booking-system-production-smoke `
  --env-file .env.production.example `
  -f compose.production.yaml up --build -d --wait
```

Otwórz `http://localhost:8088`. W tym trybie `SESSION_COOKIE_SECURE=false` jest
celowym wyjątkiem, ponieważ test nie korzysta z HTTPS.

## Ustawienia produkcyjne

- Ciasteczko sesji jest domyślnie `HttpOnly`, `SameSite=Lax` i `Secure`.
- Spring rozpoznaje `X-Forwarded-Proto` przekazane przez Caddy i Nginx.
- Swagger/OpenAPI można chwilowo włączyć zmiennymi `SPRINGDOC_*`, ale nie powinien
  być publiczny bez dodatkowej ochrony.
- HTML nie jest cache'owany, a zasoby z hashem mają roczny cache.
- Nginx dodaje `nosniff`, `SAMEORIGIN` i bezpieczną politykę referrera.
- Wolumen `postgres_production_data` jest niezależny od developerskiego `postgres_data`.

Automatyczny HTTPS nie zastępuje kopii zapasowych, monitoringu, aktualizacji obrazów
ani ochrony publicznych formularzy przed nadużyciami. Są to kolejne możliwe etapy.
