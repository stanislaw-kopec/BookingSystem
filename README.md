# BookingSystem

React z TypeScriptem, backend Spring Boot i baza PostgreSQL.

## Cała aplikacja w Dockerze

Uruchom Docker Desktop z kontenerami Linux. Potrzebny jest Docker Compose
w wersji co najmniej 2.20.3. Java, Maven i Node.js są instalowane w obrazach,
więc nie musisz ich instalować lokalnie do tego sposobu uruchamiania.

W PowerShell, z głównego folderu `BookingSystem`:

```powershell
docker compose up --build
```

Pierwsze uruchomienie pobierze obrazy oraz zależności. Docker uruchomi bazę,
poczeka na jej gotowość, uruchomi Springa i następnie frontend.
Nie uruchamiaj równocześnie lokalnego Springa lub Vite na tych samych portach.

| Usługa | Adres |
| --- | --- |
| Frontend React | http://localhost:5173 |
| Backend Spring Boot | http://localhost:8080 |
| Stan backendu i połączenia z bazą | http://localhost:8080/api/health |
| Ten sam stan przez frontend | http://localhost:5173/api/health |
| PostgreSQL | `localhost:5433` |

Endpoint stanu powinien zwrócić `{"status":"UP"}`. Backend nie ma jeszcze
kontrolera strony głównej, więc odpowiedź 404 pod `http://localhost:8080/`
jest oczekiwana. Stronę otwieraj pod adresem frontendu.

Uruchomienie w tle i sprawdzenie gotowości wszystkich usług:

```powershell
docker compose up --build -d --wait
```

Stan, logi i zatrzymanie całej aplikacji:

```powershell
docker compose ps
docker compose logs -f
docker compose down
```

`Ctrl+C` kończy podgląd logów. `docker compose down` usuwa kontenery,
ale zachowuje dane w wolumenie bazy. Opcja `down -v` usuwa także te dane.

## Praca nad kodem

To konfiguracja do lokalnego developmentu: frontend działa na serwerze Vite.
Zmiany w `frontend/src`, `frontend/public` i `frontend/index.html` są widoczne
w kontenerze od razu; Vite odświeża stronę po zapisaniu zmian. Kontener ma własne
zależności Linux, niezależne od lokalnego folderu `node_modules` z Windows.

Po zmianie kodu Java, zasobów Springa lub `backend/pom.xml` przebuduj backend:

```powershell
docker compose up -d --build backend
```

Po zmianie zależności lub konfiguracji frontendu przebuduj frontend:

```powershell
docker compose up -d --build frontend
```

## Połączenie Reacta ze Springiem

Z Reacta używaj względnych adresów zaczynających się od `/api`, np.
`fetch('/api/health')`. Vite przekazuje je do Springa, a przeglądarka komunikuje
się z jednym adresem frontendu. Przekierowanie jest skonfigurowane
w `frontend/vite.config.ts`; działa w trybie developerskim.

W Dockerze Vite korzysta z `http://backend:8080`, a Spring z bazy pod adresem
`postgres:5432`. Są to nazwy usług w sieci Compose. Podczas pracy lokalnej
Vite przekazuje żądania do `http://localhost:8080`.

## Spring i React uruchamiane lokalnie

Ten wariant wymaga lokalnego JDK 25 i Node.js zgodnego z Vite oraz działającego
Docker Desktop. Jeśli wcześniej działała cała aplikacja w Dockerze, najpierw
zwolnij porty aplikacji (z głównego folderu projektu):

```powershell
docker compose stop frontend backend
```

Backend, w pierwszym terminalu:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Spring uruchamia automatycznie tylko usługę `postgres` i odczytuje jej dane
połączenia. Można też uruchomić `AutoServiceApiApplication` z IDE, z katalogiem
roboczym ustawionym na główny folder projektu lub `backend`.
W kontenerze ta integracja jest wyłączona, ponieważ usługami zarządza Compose.

Frontend, w drugim terminalu otwartym w głównym folderze projektu:

```powershell
cd frontend
npm ci
npm run dev
```

## Baza danych

Konfiguracja PostgreSQL pozostaje w głównym `compose.yaml`.
Plik `backend/compose.yaml` wczytuje tę samą konfigurację. Oba korzystają
z projektu `booking-system` i wolumenu `booking-system_postgres_data`.

Uruchomienie wyłącznie bazy (z głównego folderu projektu):

```powershell
docker compose up -d --wait postgres
```

Lokalne dane połączenia do narzędzia takiego jak klient bazy w IntelliJ:

| Ustawienie | Wartość |
| --- | --- |
| Host | `localhost` |
| Port | `5433` |
| Baza | `booking_system` |
| Użytkownik | `booking_user` |
| Hasło | `booking_password` |

Te dane służą do lokalnego developmentu. Baza jest dostępna tylko z tego komputera.
Port `5433` pozwala uniknąć konfliktu z usługą, która już korzysta z portu `5432`.

Szczegóły mechanizmów: [gotowość usług w Compose](https://docs.docker.com/compose/how-tos/startup-order/),
[proxy i wykrywanie zmian w Vite](https://vite.dev/config/server-options),
[integracja Spring Boot z Compose](https://docs.spring.io/spring-boot/reference/features/dev-services.html).
