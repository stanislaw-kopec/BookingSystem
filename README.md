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

Endpoint stanu powinien zwrócić odpowiedź zawierającą `"status":"UP"`. Backend udostępnia API,
a stronę otwieraj pod adresem frontendu. Jego katalog usług jest dostępny także
przez `http://localhost:8080/api/services`.

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
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Spring uruchamia automatycznie tylko usługę `postgres` i odczytuje jej dane
połączenia. Można też uruchomić `AutoServiceApiApplication` z IDE, z katalogiem
roboczym ustawionym na główny folder projektu lub `backend`.
W kontenerze ta integracja jest wyłączona, ponieważ usługami zarządza Compose.
Profil `local` tworzy konta demonstracyjne opisane poniżej; w IDE ustaw go jako
aktywny profil. Bez tego profilu backend nie tworzy kont demo.

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

## Katalog usług

Strona główna zawiera opis warsztatu oraz kategorie z usługami pobranymi z bazy.
Link „Usługi” przewija do sekcji na tej samej stronie. Oferta jest widoczna
bez logowania i dla zalogowanego klienta.

Po zalogowaniu jako mechanik lub administrator pojawia się „Zarządzaj ofertą”.
Panel pozwala dodawać, edytować i usuwać kategorie oraz usługi, a także przenosić
usługę do innej kategorii. Po zapisie publiczna lista na tej stronie jest odświeżana.
Pozostałe otwarte przeglądarki pobiorą zmiany po odświeżeniu strony.

Każda usługa należy do jednej kategorii. Nazwy kategorii są unikalne,
a nazwy usług unikalne w obrębie kategorii, bez rozróżniania wielkości liter.
Kategorii zawierającej usługi nie można usunąć: najpierw przenieś lub usuń jej usługi.

## Profil klienta

Po zalogowaniu z rolą klienta w prawym górnym rogu pojawia się menu konta. Zawiera
ono odnośniki „Mój profil”, „Moje pojazdy”, „Moje wizyty” oraz opcję wylogowania. Profil jest osobną podstroną
`http://localhost:5173/profile`. Zapisane dane są najpierw wyświetlane jako czytelne
podsumowanie, a formularz pojawia się dopiero po wybraniu „Edytuj profil”. Można w nim
zapisać imię, nazwisko, telefon, kontaktowy e-mail oraz adres. Opcjonalny przełącznik
firmy pokazuje nazwę firmy, NIP i osobny adres rozliczeniowy. Po włączeniu danych firmy
wszystkie te pola są wymagane. Wyłączenie przełącznika podczas zapisu usuwa dane firmy
z profilu.

E-mail kontaktowy profilu może być inny niż e-mail konta podany przy rejestracji.
Identyfikator klienta nie jest przesyłany z formularza — backend odczytuje właściciela
z zalogowanej sesji. Mechanik, administrator i użytkownik anonimowy nie mają dostępu
do endpointów profilu klienta.

## Moje pojazdy

Klient otwiera „Moje pojazdy” z menu konta w prawym górnym rogu. Podstrona
`http://localhost:5173/vehicles` pokazuje jego pojazdy i formularz dodawania.
Pojazd zawiera markę, model, rok produkcji, numer rejestracyjny i opcjonalny VIN.
Numer rejestracyjny jest zapisywany wielkimi literami bez spacji.

Kliknięcie pojazdu prowadzi do `http://localhost:5173/vehicles/{vehicleId}`.
Podstrona pokazuje szczegóły i sekcję historii napraw. Historia pozostaje pusta,
dopóki nie powstanie moduł faktur — zgłoszenie usterki ani rezerwacja nie są dowodem
wykonania naprawy. Backend ustala właściciela z sesji i dla cudzego pojazdu zwraca 404.

## Umawianie wizyty

Publiczna pozycja „Umów wizytę” prowadzi do `http://localhost:5173/appointments`.
Gość podaje imię, nazwisko, co najmniej telefon albo e-mail, dane pojazdu, wolny
termin oraz opis usterki. Wysłanie nie tworzy konta ani pojazdu w „Moich pojazdach”.
Po zapisie gość otrzymuje losowy numer referencyjny, a warsztat kontaktuje się z nim
telefonicznie lub mailowo. Nie ma publicznego podglądu statusu zgłoszenia.

Zalogowany klient wybiera jeden ze swoich pojazdów. Może też rozwinąć formularz
dodawania samochodu; zapisany pojazd zostaje od razu wybrany w zgłoszeniu. Utworzenie
zgłoszenia wymaga uzupełnionego profilu, ponieważ warsztat kopiuje z niego aktualne
dane kontaktowe. Osobna podstrona `/my-appointments` („Moje wizyty”) pokazuje
własne zgłoszenia i ich statusy. Zalogowany klient ma w górnym menu osobne linki
„Umów wizytę” oraz „Moje wizyty”; lista jest dostępna także z menu konta.
Jeśli personel zaproponuje inną godzinę, klient może ją potwierdzić w panelu.

Mechanik i administrator otwierają `http://localhost:5173/staff/appointments`.
Panel pozwala przyjąć lub odrzucić zgłoszenie oraz wskazać inny wolny termin.
W przypadku gościa personel potwierdza nową godzinę po uzgodnieniu jej poza aplikacją.

Pierwsza wersja kalendarza używa strefy `Europe/Warsaw`, godzin 08:00–16:00 od
poniedziałku do piątku oraz jednogodzinnych terminów na najbliższe 30 dni. Jeden
aktywny termin może mieć tylko jedno zgłoszenie. `PENDING`, `TIME_PROPOSED` i
`CONFIRMED` blokują godzinę; odrzucenie ją zwalnia. Backend ponownie sprawdza
dostępność podczas zapisu, a indeks PostgreSQL chroni także równoczesne żądania.

| Status | Znaczenie |
| --- | --- |
| `PENDING` | Zgłoszenie oczekuje na decyzję warsztatu |
| `TIME_PROPOSED` | Personel zaproponował inny termin |
| `CONFIRMED` | Termin został potwierdzony |
| `REJECTED` | Zgłoszenie zostało odrzucone, a termin zwolniony |

Migracje Flyway tworzą schemat i jednorazowo dodają ofertę startową: Elektryka,
Mechanika i Wulkanizacja, łącznie sześć usług. Migracja V8 rozszerza ofertę
o 15 propozycji usług w istniejących kategoriach,
pomijając nazwy już obecne i zachowując ich opisy. Kategorie usunięte lub przemianowane
nie są odtwarzane. Dalsza edycja odbywa się
w panelu; restart nie przywraca poprzedniej oferty. Nie zmieniaj zastosowanych
migracji — nowe zmiany schematu zapisuj w kolejnych plikach migracji.

## Lokalne konta demonstracyjne

Compose włącza profil Springa `local`. Przy uruchomieniu powstają brakujące konta:

| Login | Hasło | Dostęp do katalogu |
| --- | --- | --- |
| `mechanic` | `mechanic-local-2026` | Odczyt i edycja |
| `admin` | `admin-local-2026` | Odczyt i edycja |
| `client` | `client-local-2026` | Odczyt |

Skorzystaj z jednego przycisku „Logowanie / Rejestracja” w nagłówku.
Można przełączać się między formularzami. Rejestracja wymaga unikalnego loginu
i e-maila oraz hasła wpisanego dwukrotnie. Po utworzeniu konta klient jest
automatycznie logowany. Może następnie uzupełnić profil i dodać własne pojazdy.

To konta wyłącznie do lokalnej nauki. Hasła są zapisywane w bazie jako skróty BCrypt.
Wartości dla nowych kont można ustawić zmiennymi `DEV_ADMIN_PASSWORD`,
`DEV_MECHANIC_PASSWORD` i `DEV_CLIENT_PASSWORD` w środowisku procesu backendu.
W Dockerze trzeba przekazać te zmienne do `environment` usługi backend;
samo wpisanie ich do pliku `.env` nie przekazuje ich do kontenera.
Zmiana zmiennej nie nadpisuje hasła już istniejącego konta.
Wyłączenie profilu nie usuwa kont z bazy, więc bazy demonstracyjnej nie należy
używać jako bazy produkcyjnej.

Spring Security utrzymuje sesję przez ciasteczko HttpOnly. Operacje zapisu
wymagają właściwej roli oraz tokenu CSRF. Ukrywanie panelu we frontendzie
uzupełnia kontrolę uprawnień backendu.

## Dostępne API

| Metoda i ścieżka | Działanie | Dostęp |
| --- | --- | --- |
| `GET /api/services` | Kategorie z zagnieżdżonymi listami usług | Publiczny |
| `GET /api/services/{id}` | Szczegóły usługi | Publiczny |
| `GET /api/service-categories/{id}` | Kategoria z jej usługami | Publiczny |
| `POST /api/service-categories` | Dodanie kategorii | MECHANIC, ADMIN |
| `PUT /api/service-categories/{id}` | Edycja kategorii | MECHANIC, ADMIN |
| `DELETE /api/service-categories/{id}` | Usunięcie pustej kategorii | MECHANIC, ADMIN |
| `POST /api/services` | Dodanie usługi | MECHANIC, ADMIN |
| `PUT /api/services/{id}` | Edycja lub przeniesienie usługi | MECHANIC, ADMIN |
| `DELETE /api/services/{id}` | Usunięcie usługi | MECHANIC, ADMIN |
| `GET /api/auth/me` | Aktualny użytkownik lub `user: null` | Publiczny |
| `GET /api/auth/csrf` | Token i nazwa nagłówka CSRF | Publiczny |
| `POST /api/auth/register` | Utworzenie konta klienta | Publiczny, CSRF |
| `POST /api/auth/login` | Logowanie: formularz `username`, `password` | Publiczny, CSRF |
| `POST /api/auth/logout` | Zakończenie sesji | CSRF |
| `GET /api/profile/me` | Własny profil lub pusty formularz | CLIENT |
| `PUT /api/profile/me` | Utworzenie albo aktualizacja własnego profilu | CLIENT, CSRF |
| `GET /api/vehicles` | Lista własnych pojazdów | CLIENT |
| `GET /api/vehicles/{vehicleId}` | Szczegóły własnego pojazdu | CLIENT |
| `POST /api/vehicles` | Dodanie pojazdu do własnego konta | CLIENT, CSRF |
| `GET /api/appointments/availability` | Kalendarz terminów na 30 dni | Publiczny |
| `POST /api/appointments/guest` | Wysłanie zgłoszenia bez konta | Publiczny, CSRF |
| `GET /api/appointments` | Lista własnych zgłoszeń | CLIENT |
| `POST /api/appointments` | Zgłoszenie dla własnego pojazdu | CLIENT, CSRF |
| `POST /api/appointments/{id}/confirm-proposed` | Potwierdzenie nowego terminu | Właściciel CLIENT, CSRF |
| `GET /api/staff/appointments` | Kolejka wszystkich zgłoszeń | MECHANIC, ADMIN |
| `POST /api/staff/appointments/{id}/accept` | Potwierdzenie zgłoszonego terminu | MECHANIC, ADMIN, CSRF |
| `POST /api/staff/appointments/{id}/reject` | Odrzucenie zgłoszenia | MECHANIC, ADMIN, CSRF |
| `POST /api/staff/appointments/{id}/propose-time` | Propozycja innego terminu | MECHANIC, ADMIN, CSRF |
| `POST /api/staff/appointments/{id}/confirm-proposed` | Potwierdzenie terminu gościa po kontakcie | MECHANIC, ADMIN, CSRF |

Zapis kategorii przyjmuje JSON z `name` i opcjonalnym `description`.
Zapis usługi wymaga dodatkowo `categoryId`. Utworzenie zwraca 201 i nagłówek
`Location`, aktualizacja 200, a usunięcie 204. Błędy mają wspólny kształt
`{ "status": 400, "message": "...", "fieldErrors": { "name": "..." } }`.
Nieprawidłowe dane to 400, brak zasobu 404, a duplikat lub niepusta kategoria 409.
Brak logowania przy poprawnym tokenie CSRF zwraca 401; niewłaściwa rola
lub brak albo nieprawidłowy token CSRF — 403.

Rejestracja przyjmuje JSON:

```json
{
  "username": "jan.kowalski",
  "email": "jan@example.com",
  "password": "bezpieczne-haslo",
  "passwordConfirmation": "bezpieczne-haslo"
}
```

Login ma 3–30 znaków i może zawierać litery bez polskich znaków, cyfry, kropkę,
myślnik oraz podkreślenie. Hasło ma 8–64 znaki. Rola nie jest częścią żądania:
backend zawsze nadaje nowemu kontu rolę `CLIENT`. E-mail jest przechowywany małymi
literami. Login i e-mail są unikalne bez rozróżniania wielkości liter.

Dodanie pojazdu przyjmuje JSON:

```json
{
  "make": "Toyota",
  "model": "Corolla",
  "productionYear": 2020,
  "registrationNumber": "KR12AB",
  "vin": "WVWZZZ1JZXW000001"
}
```

Pole `vin` może być pustym tekstem. Backend nie przyjmuje `ownerId`.

Zgłoszenie zalogowanego klienta przyjmuje:

```json
{
  "vehicleId": 1,
  "slotStartAt": "2026-09-09T08:00:00+02:00",
  "problemDescription": "Podczas hamowania słychać metaliczny dźwięk."
}
```

Gość zamiast `vehicleId` podaje pola `vehicleMake`, `vehicleModel`,
`vehicleProductionYear`, `vehicleRegistrationNumber`, opcjonalny `vehicleVin` oraz
`firstName`, `lastName`, `phoneNumber` i `contactEmail`. Co najmniej jeden z dwóch
ostatnich sposobów kontaktu musi być podany. Opis ma od 10 do 2000 znaków.

## Nauka i sprawdzanie zmian

[Przewodnik krok po kroku po katalogu usług](docs/service-catalog-walkthrough.md)
wyjaśnia strukturę folderów, komponenty, props, stan oraz drogę danych do Springa.
[Przewodnik po rejestracji klienta](docs/client-registration-walkthrough.md)
pokazuje drogę danych z formularza Reacta do bazy i późniejszego logowania.
[Przewodnik po profilu klienta](docs/client-profile-walkthrough.md)
wyjaśnia relację z kontem, formularz warunkowy i ochronę własności danych.
[Przewodnik po pojazdach klienta](docs/client-vehicles-walkthrough.md)
opisuje model bazy, prywatne API, walidację oraz dwie podstrony Reacta.
[Przewodnik po umawianiu wizyty](docs/appointment-booking-walkthrough.md)
wyjaśnia statusy, dostępność, ochronę terminu przed równoczesnym zapisem oraz
różnicę między formularzem klienta i gościa.
Warsztat nazywa się **Mietek Customs**. Logo z kaczką znajduje się w
`frontend/src/assets/branding/mietek-customs-logo.png`. Opis importu grafiki i podziału
plików znajdziesz w [przewodniku po logo w Reacie](docs/workshop-branding.md).
Historia warsztatu to fikcyjny tekst w trzech akapitach przygotowany do prezentacji projektu.
Strona główna pokazuje kolejno opis warsztatu, nasze usługi oraz lokalizację z mapą
Google i linkiem do wyznaczania trasy. Mapa wskazuje demonstracyjny punkt we Wrocławiu.
Opis warsztatu i współrzędne mapy zmienisz w `frontend/src/features/workshop/workshopInfo.ts`;
kolory w zmiennych na początku `frontend/src/index.css`.

Testy backendu wymagają Docker Desktop i uruchamiają własną, tymczasową bazę
PostgreSQL przez Testcontainers. Nie korzystają z danych roboczych aplikacji.
Z folderu `backend`:

```powershell
.\mvnw.cmd test
```

Z folderu `frontend`:

```powershell
npm run build
npm run lint
```

Testy integracyjne sprawdzają uprawnienia, CRUD, walidację, logowanie, sesję i CSRF.
Budowa obrazu Docker pomija uruchomienie testów — sprawdzaj je osobno.

Szczegóły mechanizmów: [gotowość usług w Compose](https://docs.docker.com/compose/how-tos/startup-order/),
[proxy i wykrywanie zmian w Vite](https://vite.dev/config/server-options),
[integracja Spring Boot z Compose](https://docs.spring.io/spring-boot/reference/features/dev-services.html).
