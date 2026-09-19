# Mietek Customs — dokumentacja techniczna i przewodnik

To rozszerzona dokumentacja projektu przeznaczona do nauki i dalszego rozwijania
aplikacji. Krótsza prezentacja dla rekrutera znajduje się w
[`README.md`](../README.md) w głównym folderze repozytorium.

<p align="center">
  <img src="../frontend/src/assets/branding/mietek-customs-logo.png" alt="Logo warsztatu Mietek Customs" width="260">
</p>

Pełnostackowa aplikacja portfolio do obsługi warsztatu samochodowego. Pozwala klientom
zarządzać pojazdami i umawiać wizyty, a pracownikom prowadzić zgłoszenie od przyjęcia
samochodu aż do zakończenia naprawy i wystawienia faktury PDF. Administrator konfiguruje
grafik warsztatu oraz zarządza kontami użytkowników.

Projekt powstał jako praktyczne ćwiczenie umiejętności potrzebnych na stanowisku
Java Developera: projektowania REST API, modelowania domeny, bezpieczeństwa,
transakcji, pracy z PostgreSQL, testów integracyjnych i uruchamiania aplikacji w Dockerze.

## Najważniejsze funkcje

| Obszar | Możliwości |
| --- | --- |
| Klient | Rejestracja, profil prywatny lub firmowy, własne pojazdy, edycja pojazdu, umawianie i odwoływanie wizyt, potwierdzanie proponowanego dnia, historia napraw i faktury PDF |
| Gość | Wysłanie zgłoszenia bez tworzenia konta, z danymi kontaktowymi i pojazdem |
| Mechanik | Tygodniowy grafik, kolejka zgłoszeń, decyzje o terminach, szczegóły pojazdu i historii, pozycje robocizny i części, zakończenie naprawy oraz potwierdzenie odbioru |
| Administrator | Funkcje mechanika, konfiguracja godzin i pojemności grafiku, wyjątki dla konkretnych dni oraz zarządzanie kontami klientów i personelu |
| Oferta warsztatu | Publiczny katalog usług podzielony na kategorie, edytowany przez mechanika lub administratora |

## Stack technologiczny

| Warstwa | Technologie |
| --- | --- |
| Backend | Java 25, Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation |
| Baza danych | PostgreSQL 17, Flyway, Hibernate |
| Frontend | React 19.2.8, TypeScript 6.0.2, Vite 8.2.2, React Router 7.18.3 |
| Dokumenty | OpenPDF 2.4.0 — generowanie faktur PDF |
| Testy | JUnit 5, Spring MockMvc, AssertJ, Testcontainers, Vitest 5.0.1, React Testing Library |
| Narzędzia | Maven Wrapper, Node.js 24, npm, Docker Compose, GitHub Actions, springdoc-openapi 3.1.1 |

## Architektura

```mermaid
flowchart LR
    U[Przeglądarka] --> R[React + TypeScript]
    R -->|REST /api, sesja i CSRF| C[Kontrolery Spring MVC]
    C --> S[Serwisy i reguły biznesowe]
    S --> J[Spring Data JPA]
    J --> P[(PostgreSQL)]
    S --> F[Generator faktur PDF]
    M[Flyway] --> P
```

Backend jest monolitem podzielonym na pakiety funkcjonalne. React komunikuje się
z REST API przez względne adresy `/api`. Uwierzytelnianie korzysta z sesji Spring
Security i ciasteczka HttpOnly, a operacje zmieniające dane wymagają tokenu CSRF.

## Najciekawsze elementy techniczne

- Backend ustala właściciela profilu, pojazdu i zgłoszenia z zalogowanej sesji,
  zamiast ufać identyfikatorowi przesłanemu przez frontend.
- Dostęp do funkcji jest sprawdzany na backendzie dla ról `CLIENT`, `MECHANIC`
  i `ADMIN`; samo ukrycie elementu interfejsu nie stanowi zabezpieczenia.
- Rezerwacja ponownie sprawdza pojemność dnia wewnątrz transakcji i blokuje
  odpowiedni rekord, aby równoczesne żądania nie przekroczyły limitu warsztatu.
- Listy wizyt i kont korzystają z backendowej paginacji, filtrowania i sortowania.
- Flyway wersjonuje schemat bazy, a profil `local` tworzy idempotentne dane pokazowe.
- Faktura PDF powstaje na podstawie zakończonej naprawy i zawiera osobne pozycje
  robocizny oraz części, a także wartości netto i brutto.
- API zwraca wspólny format błędów ze stabilnymi kodami technicznymi, które frontend
  mapuje na polskie komunikaty.
- Testy integracyjne uruchamiają odizolowany PostgreSQL przez Testcontainers.

## Zrzuty ekranu

Przykładowa faktura pochodzi z pełnego scenariusza demonstracyjnego opisanego w
[`portfolio-verification.md`](portfolio-verification.md). Lista pozostałych kadrów
do wykonania znajduje się w [`screenshots/README.md`](screenshots/README.md).

![Przykładowa faktura z pozycjami netto i brutto](screenshots/06-invoice-preview.png)

| Widok | Docelowy plik | Co powinien pokazywać |
| --- | --- | --- |
| Strona główna | `docs/screenshots/01-home-page.png` | Logo, opis warsztatu i ofertę usług |
| Panel klienta | `docs/screenshots/02-client-appointments.png` | Wizyty w różnych statusach, filtry i paginację |
| Grafik mechanika | `docs/screenshots/03-staff-schedule.png` | Tygodniowy układ zgłoszeń warsztatu |
| Szczegóły naprawy | `docs/screenshots/04-repair-details.png` | Dane pojazdu, historię oraz pozycje naprawy |
| Panel administratora | `docs/screenshots/05-admin-panel.png` | Zarządzanie grafikiem albo kontami użytkowników |
| Faktura | `docs/screenshots/06-invoice-preview.png` | Przykładowy dokument PDF bez danych prywatnych |

## Szybkie uruchomienie

Wymagany jest Docker Desktop z kontenerami Linux oraz Docker Compose w wersji
co najmniej 2.20.3. Z głównego folderu projektu uruchom:

```powershell
docker compose up --build -d --wait
```

Następnie otwórz:

- aplikację: http://localhost:5173,
- Swagger UI: http://localhost:8080/swagger-ui.html,
- stan backendu: http://localhost:8080/api/health.

Najwygodniejsze konto do pierwszej prezentacji to `anna.demo` z hasłem
`client-local-2026`. Wszystkie konta demonstracyjne opisuje dalsza część README.

## Scenariusz prezentacji

1. Otwórz stronę główną bez logowania i pokaż publiczną ofertę oraz formularz zgłoszenia gościa.
2. Zaloguj się jako `anna.demo` i pokaż profil, dwa pojazdy oraz wizyty w różnych statusach.
3. Otwórz historię Hondy Civic i pobierz fakturę za zakończoną naprawę.
4. Zaloguj się jako `mechanic`, otwórz tygodniowy grafik i przejdź do szczegółów zgłoszenia.
5. Pokaż decyzję o terminie, formularz zakończenia naprawy z robocizną i częściami oraz potwierdzenie odbioru.
6. Zaloguj się jako `admin`, zmień limit wybranego dnia w konfiguracji grafiku i pokaż panel kont.
7. Na końcu otwórz Swagger UI oraz workflow GitHub Actions, aby pokazać kontrakt API i automatyczną weryfikację projektu.

Gotowy przebieg z użytymi danymi i oczekiwanymi rezultatami zawiera
[raport weryfikacji portfolio](portfolio-verification.md).

## Spis treści

- [Cała aplikacja w Dockerze](#cała-aplikacja-w-dockerze)
- [Wariant wdrożeniowy](deployment-guide.md)
- [Audyt jakości kodu backendu](backend-code-audit.md)
- [Praca nad kodem](#praca-nad-kodem)
- [Spring i React uruchamiane lokalnie](#spring-i-react-uruchamiane-lokalnie)
- [Główne moduły aplikacji](#katalog-usług)
- [Lokalne konta demonstracyjne](#lokalne-konta-demonstracyjne)
- [Dokumentacja API](#dokumentacja-api-swagger--openapi)
- [Dostępne API](#dostępne-api)
- [GitHub Actions](#automatyczne-sprawdzanie-w-github-actions)
- [Nauka i sprawdzanie zmian](#nauka-i-sprawdzanie-zmian)

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

Po zalogowaniu jako mechanik lub administrator w menu konta pojawia się
„Zarządzaj ofertą”. Panel pozwala dodawać, edytować i usuwać kategorie oraz usługi,
a także przenosić usługę do innej kategorii. Po zapisie publiczna lista na tej stronie jest odświeżana.
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
Podstrona pokazuje szczegóły, przycisk „Edytuj pojazd” i sekcję historii napraw.
Edycja pozwala poprawić markę, model, rocznik, numer rejestracyjny oraz VIN, ale tylko właścicielowi pojazdu.
Historia pokazuje zakończone naprawy powiązane z tym pojazdem. Wpis pojawia się,
gdy personel zakończy pracę, a potem oznaczy samochód jako odebrany przez klienta.
Przy każdym wpisie można pobrać prostą fakturę PDF. Dane firmy albo dane imienne i adresowe
klienta są utrwalane z profilu przy odbiorze auta, razem z numerem, datami, pozycjami,
kwotami i gotowym PDF-em. Kolejne pobrania zwracają zapisany dokument, również po
edycji profilu. Starsze zakończone naprawy bez dokumentu utrwalają go przy pierwszym
pobraniu na podstawie dostępnych wtedy danych. Backend ustala właściciela z sesji
i dla cudzego pojazdu zwraca 404. Szczegóły: [utrwalanie faktur i kwoty](invoice-snapshots-walkthrough.md).

## Zmiana własnego hasła

Każdy zalogowany klient, mechanik i administrator może wybrać „Zmień hasło” w menu konta.
Podstrona `http://localhost:5173/account/security` wymaga podania obecnego hasła,
nowego hasła oraz jego powtórzenia. Backend sprawdza obecne hasło i zapisuje nowe
w postaci skrótu BCrypt. Operacja wymaga aktywnej sesji oraz tokenu CSRF.

Odzyskiwanie zapomnianego hasła przez e-mail nie jest częścią obecnej wersji.

## Umawianie wizyty

Publiczna pozycja „Umów wizytę” prowadzi do `http://localhost:5173/appointments`.
Gość podaje imię, nazwisko, co najmniej telefon albo e-mail, dane pojazdu, wolny
dzień przyjęcia auta oraz opis usterki. Wysłanie nie tworzy konta ani pojazdu w „Moich pojazdach”.
Po zapisie gość otrzymuje losowy numer referencyjny, a warsztat kontaktuje się z nim
telefonicznie lub mailowo. Nie ma publicznego podglądu statusu zgłoszenia.

Zalogowany klient wybiera jeden ze swoich pojazdów. Może też rozwinąć formularz
dodawania samochodu; zapisany pojazd zostaje od razu wybrany w zgłoszeniu. Utworzenie
zgłoszenia wymaga uzupełnionego profilu, ponieważ warsztat kopiuje z niego aktualne
dane kontaktowe. Osobna podstrona `/my-appointments` („Moje wizyty”) pokazuje
własne zgłoszenia i ich statusy. Listę można filtrować po statusie, sortować po
dacie przyjęcia auta oraz przeglądać stronami; te operacje wykonuje backend na
podstawie parametrów `status`, `page`, `size` i `sortDirection`. Panel pozwala potwierdzić
zaproponowany dzień oraz odwołać aktywną wizytę. Główne menu ma publiczny link „Umów wizytę”, a lista
własnych wizyt jest dostępna z menu konta.
Jeśli personel zaproponuje inny dzień, klient może go potwierdzić w panelu.
Po zakończeniu naprawy klient widzi status „Czeka na odbiór”, opis wykonanych prac
oraz kwotę brutto do zapłaty na miejscu. Gdy auto zostanie odebrane, status zmienia się
na „Zakończone”, wpis trafia do historii napraw pojazdu, a przy zakończonej wizycie
w „Moich wizytach” pojawia się przycisk pobrania faktury PDF.

Mechanik i administrator mają w menu konta zakładkę
`http://localhost:5173/staff/schedule` („Grafik”), która pokazuje aktywne zgłoszenia
na tygodniowym kalendarzu pracy warsztatu. Szczegółowa kolejka zgłoszeń jest pod
`http://localhost:5173/staff/appointments`. Panel pozwala filtrować zgłoszenia po
statusie, sortować je po dacie przyjęcia auta i przeglądać stronami. Pozwala też
przyjąć lub odrzucić zgłoszenie oraz wskazać inny wolny dzień. W przypadku gościa personel potwierdza
nowy dzień po uzgodnieniu go poza aplikacją.
Kliknięcie karty zgłoszenia w grafiku otwiera większą stronę szczegółów
`/staff/appointments/{appointmentId}`. Personel widzi tam pełny opis zgłoszenia,
dane kontaktowe i historię zakończonych napraw tego pojazdu, jeśli zgłoszenie jest
powiązane z pojazdem klienta. Dla zgłoszenia gościa bez kartoteki pojazdu historia jest pusta.
Po potwierdzeniu wizyty personel może wybrać „Praca zakończona”, wpisać wykonane
prace i końcową kwotę brutto. Przy statusie „Czeka na odbiór” personel widzi przycisk
„Samochód został odebrany”, który ustawia status „Zakończone”. System nie obsługuje
płatności online; płatność odbywa się przy odbiorze auta poza aplikacją.

Pierwsza wersja kalendarza używa strefy `Europe/Warsaw`, dni od poniedziałku do
piątku oraz horyzontu 30 dni. Warsztat przyjmuje bazowo 4 aktywne zgłoszenia na
jeden dzień. `PENDING`, `TIME_PROPOSED` i `CONFIRMED` zajmują miejsce w danym dniu;
odrzucenie albo odwołanie je zwalnia. Backend ponownie sprawdza dostępność podczas
zapisu i blokuje wybrany dzień w transakcji, żeby równoczesne żądania nie przekroczyły limitu.
Statusy `READY_FOR_PICKUP` i `COMPLETED` nie zajmują już miejsca w kalendarzu przyjęć.
Dla `COMPLETED` klient może pobrać PDF faktury z historii pojazdu.

| Status | Znaczenie |
| --- | --- |
| `PENDING` | Zgłoszenie oczekuje na decyzję warsztatu |
| `TIME_PROPOSED` | Personel zaproponował inny dzień |
| `CONFIRMED` | Dzień został potwierdzony |
| `READY_FOR_PICKUP` | Naprawa zakończona, auto czeka na odbiór i płatność na miejscu |
| `COMPLETED` | Samochód odebrany przez klienta, zgłoszenie trafia do historii napraw pojazdu |
| `CANCELLED` | Klient odwołał wizytę, a miejsce zostało zwolnione |
| `REJECTED` | Zgłoszenie zostało odrzucone, a miejsce zwolnione |

## Zarządzanie kontami użytkowników

Administrator otwiera z menu konta panel `http://localhost:5173/admin/accounts`.
Lista obejmuje klientów, mechaników i administratorów, obsługuje wyszukiwanie po loginie lub e-mailu,
filtrowanie po roli i aktywności oraz paginację wykonywaną przez backend.

Administrator może poprawić login i e-mail, aktywować albo dezaktywować konto oraz
ustawić nowe hasło klientowi lub mechanikowi. Reset hasła zakłada, że użytkownik
wcześniej potwierdzi swoją tożsamość bezpośrednio w warsztacie. Warsztat przekazuje
mu nowe hasło poza aplikacją, a użytkownik może później ustawić własne przez stronę
„Zmień hasło”. Aplikacja nie wysyła wiadomości resetujących i nie udostępnia formularza
„Nie pamiętam hasła”.

Panel pozwala tworzyć konta mechaników i administratorów. Rola wynika z wybranej,
dedykowanej operacji backendu i nie jest dostępna w publicznej rejestracji. Konta klientów
powstają przez publiczną rejestrację. Konta administratorów są widoczne na liście, ale
chronione przed edycją, resetem hasła i dezaktywacją w tym panelu.

Migracje Flyway tworzą schemat i jednorazowo dodają ofertę startową: Elektryka,
Mechanika i Wulkanizacja, łącznie sześć usług. Migracja V8 rozszerza ofertę
o 15 propozycji usług w istniejących kategoriach,
pomijając nazwy już obecne i zachowując ich opisy. Kategorie usunięte lub przemianowane
nie są odtwarzane. Dalsza edycja odbywa się
w panelu; restart nie przywraca poprzedniej oferty. Nie zmieniaj zastosowanych
migracji — nowe zmiany schematu zapisuj w kolejnych plikach migracji.

## Lokalne konta demonstracyjne

Compose włącza profil Springa `local`. Przy uruchomieniu powstają brakujące konta:

| Login | Hasło | Rola i dane pokazowe |
| --- | --- | --- |
| `mechanic` | `mechanic-local-2026` | Mechanik: grafik, zgłoszenia i naprawy |
| `admin` | `admin-local-2026` | Administrator: grafik, oferta i konta użytkowników |
| `client` | `client-local-2026` | Klient bez rozbudowanych danych demonstracyjnych |
| `anna.demo` | `client-local-2026` | Klient z profilem, pojazdami, wizytami i fakturami |
| `firma.demo` | `client-local-2026` | Klient firmowy z profilem, pojazdem i fakturą na firmę |

Skorzystaj z jednego przycisku „Logowanie / Rejestracja” w nagłówku.
Można przełączać się między formularzami. Rejestracja wymaga unikalnego loginu
i e-maila oraz hasła wpisanego dwukrotnie. Po utworzeniu konta klient jest
automatycznie logowany. Może następnie uzupełnić profil i dodać własne pojazdy.

Oprócz samych kont profil `local` przygotowuje dane pokazowe do prezentacji:

- `anna.demo` ma uzupełniony profil osoby prywatnej, dwa pojazdy oraz zgłoszenia
  w kilku statusach: oczekujące, z zaproponowanym innym dniem, gotowe do odbioru
  i zakończone.
- `firma.demo` ma uzupełnione dane firmowe, pojazd dostawczy, potwierdzoną wizytę
  oraz zakończoną naprawę, dla której faktura PDF używa danych firmy.
- Konto `mechanic` pozwala pokazać grafik, kolejkę zgłoszeń, proponowanie innego
  dnia, zakończenie naprawy oraz oznaczenie odbioru auta.

Seeder tworzy tylko brakujące rekordy demonstracyjne. Jeśli zmienisz profil, pojazd
albo wizytę ręcznie, kolejne uruchomienie aplikacji nie nadpisze tych danych.

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

## Dokumentacja API Swagger / OpenAPI

Backend udostępnia automatyczną dokumentację API przez `springdoc-openapi`.
Po uruchomieniu aplikacji Swagger UI jest dostępny pod adresem:

```text
http://localhost:8080/swagger-ui.html
```

Specyfikację OpenAPI w formacie JSON można pobrać tutaj:

```text
http://localhost:8080/v3/api-docs/booking-system
```

Dokumentacja opisuje endpointy z prefiksem `/api`. Endpointy Swaggera są publiczne,
ale właściwe endpointy aplikacji nadal wymagają takich samych ról i tokenu CSRF jak
przy normalnym użyciu frontendu.

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
| `PUT /api/auth/password` | Zmiana własnego hasła po podaniu obecnego | Zalogowany użytkownik, CSRF |
| `GET /api/profile/me` | Własny profil lub pusty formularz | CLIENT |
| `PUT /api/profile/me` | Utworzenie albo aktualizacja własnego profilu | CLIENT, CSRF |
| `GET /api/vehicles` | Lista własnych pojazdów | CLIENT |
| `GET /api/vehicles/{vehicleId}` | Szczegóły własnego pojazdu | CLIENT |
| `GET /api/vehicles/{vehicleId}/repair-history` | Historia zakończonych napraw własnego pojazdu | CLIENT |
| `GET /api/vehicles/{vehicleId}/repair-history/{appointmentId}/invoice` | Pobranie faktury PDF za zakończoną naprawę | CLIENT |
| `POST /api/vehicles` | Dodanie pojazdu do własnego konta | CLIENT, CSRF |
| `PUT /api/vehicles/{vehicleId}` | Edycja własnego pojazdu | Właściciel CLIENT, CSRF |
| `GET /api/appointments/availability` | Kalendarz dni na 30 dni | Publiczny |
| `POST /api/appointments/guest` | Wysłanie zgłoszenia bez konta | Publiczny, CSRF |
| `GET /api/appointments` | Strona własnych zgłoszeń; obsługuje `status`, `page`, `size`, `sortDirection` | CLIENT |
| `POST /api/appointments` | Zgłoszenie dla własnego pojazdu | CLIENT, CSRF |
| `POST /api/appointments/{id}/confirm-proposed` | Potwierdzenie nowego dnia | Właściciel CLIENT, CSRF |
| `POST /api/appointments/{id}/cancel` | Odwołanie aktywnej wizyty | Właściciel CLIENT, CSRF |
| `GET /api/staff/appointments` | Strona zgłoszeń personelu; obsługuje `status`, `page`, `size`, `sortDirection` | MECHANIC, ADMIN |
| `GET /api/staff/appointments/schedule?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` | Grafik dla 1–31 dni: aktywne zgłoszenia, limity i dni zamknięte | MECHANIC, ADMIN |
| `GET /api/staff/appointments/{id}` | Szczegóły pojedynczego zgłoszenia dla personelu | MECHANIC, ADMIN |
| `GET /api/staff/appointments/{id}/repair-history` | Historia napraw pojazdu z danego zgłoszenia | MECHANIC, ADMIN |
| `GET /api/staff/appointments/{id}/invoice` | Pobranie faktury zakończonej naprawy przez personel | MECHANIC, ADMIN |
| `POST /api/staff/appointments/{id}/accept` | Potwierdzenie zgłoszonego dnia | MECHANIC, ADMIN, CSRF |
| `POST /api/staff/appointments/{id}/reject` | Odrzucenie zgłoszenia | MECHANIC, ADMIN, CSRF |
| `POST /api/staff/appointments/{id}/propose-time` | Propozycja innego dnia | MECHANIC, ADMIN, CSRF |
| `POST /api/staff/appointments/{id}/confirm-proposed` | Potwierdzenie dnia gościa po kontakcie | MECHANIC, ADMIN, CSRF |
| `POST /api/staff/appointments/{id}/complete-repair` | Zakończenie naprawy i ustawienie odbioru auta | MECHANIC, ADMIN, CSRF |
| `POST /api/staff/appointments/{id}/mark-picked-up` | Potwierdzenie odbioru samochodu i zakończenie zgłoszenia | MECHANIC, ADMIN, CSRF |
| `GET /api/admin/schedule` | Konfiguracja grafiku i wyjątki dni | ADMIN |
| `PUT /api/admin/schedule/settings` | Zmiana godzin, horyzontu i domyślnej pojemności | ADMIN, CSRF |
| `PUT /api/admin/schedule/overrides/{date}` | Zapis zamknięcia lub pojemności konkretnego dnia | ADMIN, CSRF |
| `DELETE /api/admin/schedule/overrides/{date}` | Usunięcie wyjątku dnia | ADMIN, CSRF |
| `GET /api/admin/accounts` | Stronicowana lista kont z wyszukiwaniem i filtrami | ADMIN |
| `POST /api/admin/accounts/mechanics` | Utworzenie konta mechanika | ADMIN, CSRF |
| `POST /api/admin/accounts/administrators` | Utworzenie chronionego konta administratora | ADMIN, CSRF |
| `PUT /api/admin/accounts/{accountId}` | Edycja loginu i e-maila klienta lub mechanika | ADMIN, CSRF |
| `PUT /api/admin/accounts/{accountId}/password` | Ustawienie nowego hasła klienta lub mechanika | ADMIN, CSRF |
| `PUT /api/admin/accounts/{accountId}/status` | Aktywacja albo dezaktywacja klienta lub mechanika | ADMIN, CSRF |

Zapis kategorii przyjmuje JSON z `name` i opcjonalnym `description`.
Zapis usługi wymaga dodatkowo `categoryId`. Utworzenie zwraca 201 i nagłówek
`Location`, aktualizacja 200, a usunięcie 204. Błędy mają wspólny kształt
`{ "status": 400, "code": "VALIDATION_FAILED", "message": "...", "fieldErrors": { "name": "..." } }`.
Pole `code` jest stabilnym identyfikatorem technicznym, a `message` jest angielskim
komunikatem technicznym. Frontend mapuje znane kody na czytelne polskie komunikaty.
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
  "visitDate": "2026-09-10",
  "problemDescription": "Podczas hamowania słychać metaliczny dźwięk."
}
```

Gość zamiast `vehicleId` podaje pola `vehicleMake`, `vehicleModel`,
`vehicleProductionYear`, `vehicleRegistrationNumber`, opcjonalny `vehicleVin` oraz
`firstName`, `lastName`, `phoneNumber` i `contactEmail`. Co najmniej jeden z dwóch
ostatnich sposobów kontaktu musi być podany. Opis ma od 10 do 2000 znaków.
Kalendarz wizyt wybiera dzień przyjęcia auta, a pierwsza wersja warsztatu ma limit
4 aktywnych zgłoszeń na jeden dzień roboczy.

## Automatyczne sprawdzanie w GitHub Actions

Repozytorium ma workflow CI w `.github/workflows/ci.yml`. GitHub uruchamia go po
`push` na gałąź `main` oraz przy pull requestach do `main`. Workflow składa się
z trzech niezależnych zadań:

- backend: ustawia Javę 25 i uruchamia `./mvnw test` w katalogu `backend`,
- frontend: ustawia Node.js 24, wykonuje `npm ci`, `npm run lint`, `npm run test:ci` i `npm run build`,
- Docker Compose: sprawdza poprawność `compose.yaml` poleceniem `docker compose config --quiet`.

Testy backendu korzystają z Testcontainers, więc na runnerze używany jest Docker
dostępny w środowisku GitHub Actions. Jeżeli testy nie przejdą, workflow zachowuje
raporty Surefire jako `backend-test-reports` albo raport JUnit Vitest jako
`frontend-test-reports`. Artefakty są dostępne przez 7 dni na stronie danego przebiegu.
Osobny krok sprawdza także składnię `compose.production.yaml` z przykładowym plikiem
zmiennych środowiskowych.

## Nauka i sprawdzanie zmian

[Audyt jakości kodu backendu](backend-code-audit.md) opisuje ocenę zasad SOLID i DRY,
docelowy podział pakietu `appointment` oraz bezpieczną kolejność refaktoryzacji.
[Przewodnik krok po kroku po katalogu usług](service-catalog-walkthrough.md)
wyjaśnia strukturę folderów, komponenty, props, stan oraz drogę danych do Springa.
[Przewodnik po rejestracji klienta](client-registration-walkthrough.md)
pokazuje drogę danych z formularza Reacta do bazy i późniejszego logowania.
[Przewodnik po profilu klienta](client-profile-walkthrough.md)
wyjaśnia relację z kontem, formularz warunkowy i ochronę własności danych.
[Przewodnik po pojazdach klienta](client-vehicles-walkthrough.md)
opisuje model bazy, prywatne API, walidację oraz dwie podstrony Reacta.
[Przewodnik po umawianiu wizyty](appointment-booking-walkthrough.md)
wyjaśnia statusy, dostępność, ochronę dziennego limitu przed równoczesnym zapisem oraz
różnicę między formularzem klienta i gościa.
Warsztat nazywa się **Mietek Customs**. Logo z kaczką znajduje się w
`frontend/src/assets/branding/mietek-customs-logo.png`. Opis importu grafiki i podziału
plików znajdziesz w [przewodniku po logo w Reacie](workshop-branding.md).
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
npm test
```

Testy integracyjne sprawdzają uprawnienia, CRUD, walidację, logowanie, sesję, CSRF oraz administracyjne zarządzanie kontami użytkowników i tworzenie kont personelu.
Budowa obrazu Docker pomija uruchomienie testów — sprawdzaj je osobno.

Szczegóły mechanizmów: [gotowość usług w Compose](https://docs.docker.com/compose/how-tos/startup-order/),
[proxy i wykrywanie zmian w Vite](https://vite.dev/config/server-options),
[integracja Spring Boot z Compose](https://docs.spring.io/spring-boot/reference/features/dev-services.html).

Opis obsługi sesji, błędów list i testów frontendu: [przewodnik etapu 4](frontend-state-walkthrough.md).
