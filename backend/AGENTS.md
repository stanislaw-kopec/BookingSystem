# Backend — Spring Boot

Obowiązują również cele i wymagania z głównego `AGENTS.md`.
Ten plik dotyczy kodu i konfiguracji w `backend`.

## Organizacja i istniejące ustawienia

- Używaj Javy 25, Spring Boota i Mavena zgodnie z `pom.xml`.
  Główny pakiet to `pl.autoserwis`, a klasa startowa to `AutoServiceApiApplication`.
- Dziel kod według funkcjonalności, np. użytkownicy, pojazdy, oferta warsztatu,
  rezerwacje, zlecenia i fakturowanie. Nazwy pakietów pisz po angielsku.
  Dodawaj je wraz z implementacją, bez tworzenia wszystkich modułów z wyprzedzeniem.
- Kontrolery obsługują HTTP i DTO, serwisy reguły biznesowe oraz transakcje,
  a repozytoria dostęp do danych. Nie zwracaj encji JPA bezpośrednio z API.
- Zachowaj prefiks `/api`, działanie `/api/health`, publiczne endpointy Swaggera `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` i istniejącą konfigurację Compose.
  Lokalny Spring zarządza tylko usługą `postgres`; w kontenerze integracja Springa
  z Compose jest wyłączona i używane są ustawienia połączenia ze zmiennych środowiska.
- Czas aplikacji pobieraj przez bean `workshopClock` z `TimeConfiguration`, ustawiony
  na strefę `Europe/Warsaw`. Nie dodawaj bezpośrednich wywołań `Instant.now()`,
  `LocalDate.now()`, `Year.now()` ani `ZonedDateTime.now()` w kodzie produkcyjnym;
  wstrzyknięty `Clock` pozwala testom kontrolować bieżący czas.
- Moduł `appointment` ma pakiety `api`, `application`, `domain`, `persistence`,
  `repair` i `schedule`. DTO endpointów wizyt znajdują się w `api.dto`, DTO napraw
  w `repair.dto`, a DTO grafiku w `schedule.dto`.
- Mapowanie zgłoszeń, historii napraw i pozycji do DTO znajduje się odpowiednio w
  `application.AppointmentResponseMapper`, `repair.RepairHistoryMapper` i
  `repair.RepairItemResponseMapper`.
  Normalizację wejściowych pozycji naprawy wykonuje `RepairItemValidator`. Nie
  duplikuj tych operacji w serwisach pojazdów, wizyt ani nowych kontrolerach.
- Przypadki użycia wizyt są rozdzielone między klasy w `appointment.application`:
  `AppointmentBookingService`, `AppointmentQueryService`, `ClientAppointmentService`
  i `StaffAppointmentService`, oraz `appointment.repair.RepairWorkflowService`.
  Nie twórz ponownie jednego serwisu pośredniczącego we wszystkich operacjach.
- Testy integracyjne wizyt są podzielone na dostępność, tworzenie zgłoszeń, operacje
  klienta, workflow personelu, naprawy i autoryzację. Wspólne dane testowe utrzymuje
  `AppointmentIntegrationTestSupport`, a testy współbieżności pozostają osobnymi
  klasami. Dodawaj scenariusz do klasy odpowiadającej jego przypadkowi użycia.
- `AppointmentRequest` chroni dozwolone przejścia statusów. Serwisy aplikacyjne
  odpowiadają za transakcje, blokady, pobranie zależności oraz walidację danych
  wejściowych, a zmianę stanu wykonują przez metody encji.
- `AppointmentRepository` pozostaje repozytorium agregatu `AppointmentRequest`, także
  dla zapytań list, grafiku, historii i operacji wymagających blokad. Rozważ podział
  dopiero po powstaniu osobnego modelu odczytu, niezależnego modułu albo wyraźnej
  granicy domenowej; nie twórz nakładających się repozytoriów wyłącznie dla porządku.

## Model domeny i przepływy

- Odwzoruj wymagania R01–R17. Użytkownik, pojazd, usługa warsztatu, zgłoszenie,
  wizyta, zlecenie naprawy i faktura mają różne odpowiedzialności.
  Szczegółowe encje i relacje dobieraj przy implementacji konkretnego etapu.
- Zgłoszenie zalogowanego klienta obejmuje właściciela, jego pojazd, kopię danych
  kontaktowych i pojazdu, dzień przyjęcia auta, opis usterki oraz status decyzji. Zgłoszenie gościa
  przechowuje wyłącznie kopię podanych danych i nie tworzy konta ani pojazdu.
- Przyjęcie, odrzucenie i proponowanie innego dnia są operacjami personelu.
  Waliduj przejścia `PENDING` → `CONFIRMED`, `REJECTED` albo `TIME_PROPOSED`.
  `TIME_PROPOSED` przechodzi do `CONFIRMED` po potwierdzeniu klienta z kontem albo
  personelu po kontakcie z gościem. Odnotowuj czas i autora decyzji personelu.
  Nie pozwalaj na dwukrotne przetworzenie tego samego zgłoszenia.
- Klient może odwołać wyłącznie własne aktywne zgłoszenie ze statusem `PENDING`,
  `TIME_PROPOSED` albo `CONFIRMED`. Odwołanie ustawia `CANCELLED`, nie zapisuje
  akcji personelu i zwalnia miejsce w kalendarzu.
- Lista własnych zgłoszeń klienta używa paginacji backendowej. `GET /api/appointments`
  przyjmuje `status`, `page`, `size` i `sortDirection`, ogranicza wynik do właściciela
  wynikającego z sesji i sortuje po dacie przyjęcia auta.
- Lista zgłoszeń personelu również używa paginacji backendowej. `GET /api/staff/appointments`
  przyjmuje `status`, `page`, `size` i `sortDirection`, a grafik korzysta z osobnego
  endpointu `GET /api/staff/appointments/schedule?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD`.
  Zakres ma 1–31 dni włącznie. Zwracaj lekkie DTO aktywnych zgłoszeń oraz limity,
  wolne miejsca i oznaczenia zamknięcia każdego dnia, także weekendów. Odczyt ustawień,
  wyjątków i zgłoszeń odbywa się w jednej transakcji `REPEATABLE_READ`.
- Personel może pobrać szczegóły pojedynczego zgłoszenia oraz historię napraw pojazdu
  powiązanego z tym zgłoszeniem. Zgłoszenie gościa bez trwałego pojazdu w kartotece
  zwraca pustą historię napraw.
- Personel może zakończyć wyłącznie zgłoszenie ze statusem `CONFIRMED`. Zakończenie
  zapisuje opis wykonanych prac, pozycje robocizny i części, wyliczoną kwotę brutto do zapłaty, czas zamknięcia i użytkownika
  personelu, a status przechodzi na `READY_FOR_PICKUP`. Płatność odbywa się poza systemem.
- Sprawdzaj własność pojazdu i zasobu na backendzie. Tożsamość klienta przy zapisie
  ma wynikać z uwierzytelnienia; dane przesłane przez przeglądarkę nie nadają uprawnień.
- Zabezpieczaj prywatne dane i operacje przez istniejącą konfigurację Spring Security.
  Samo ukrycie menu w React nie jest autoryzacją. Zakres ról ustalono częściowo
  i należy go doprecyzować zgodnie z sekcją kwestii otwartych w głównym pliku.

## Katalog usług i aktualne logowanie

- Pakiet `offering` zawiera kategorie i usługi. `GET /api/services` zwraca listę
  kategorii z zagnieżdżonymi usługami, także pustych kategorii. Odczyt jest publiczny.
- Zapis przez POST/PUT/DELETE pod `/api/services` i `/api/service-categories`
  jest dostępny wyłącznie dla MECHANIC oraz ADMIN. CLIENT nie może zmieniać oferty.
- Przycinaj nazwy i opisy. Unikalność nazw bez rozróżniania wielkości liter
  zabezpieczają także indeksy bazy. Usunięcie niepustej kategorii zwraca 409;
  nie wprowadzaj kaskadowego usuwania usług.
- Sesje obsługuje Spring Security. Zachowaj CSRF dla operacji zmieniających dane,
  BCrypt i ciasteczko sesji HttpOnly. Role wynikają z bazy, nie z formularza klienta. Konta mechaników i administratorów tworzy wyłącznie ADMIN przez endpointy administracyjne.
- Principal sesji to `AccountPrincipal` z niezmiennym `userId` i `sessionVersion`.
  Kontrolery przekazują ID z `@AuthenticationPrincipal` do serwisów; login służy do logowania
  i prezentacji, nigdy do ustalania właściciela danych lub autora decyzji.
  `AccountSessionFilter` sprawdza aktualną aktywność konta i wersję sesji przed CSRF/autoryzacją.
  Zmiana loginu odświeża nazwę w sesji bez zmiany tożsamości. Sesje bez właściwego principala
  są odrzucane; nie dodawaj awaryjnego wyszukiwania właściciela po loginie.
- Zmiana hasła i aktywności zwiększa `sessionVersion` w tej samej transakcji co zapis konta.
  Operacje administracyjne i zmiana własnego hasła blokują rekord konta, aby równoczesne zapisy
  nie gubiły unieważnienia sesji. Reset administracyjny unieważnia wszystkie stare sesje;
  samodzielna zmiana hasła po udanym zapisie aktualizuje wersję tylko bieżącej sesji.
- `local` tworzy brakujące konta i dane demonstracyjne; nie zmienia haseł, profili,
  pojazdów ani zgłoszeń, które już istnieją. Nie stosuj demonstracyjnej bazy ani jej
  kont we wdrożeniu produkcyjnym.
- Profil `production` nie ładuje danych demo. Jeżeli nie istnieje żaden administrator,
  `ProductionAdminBootstrap` wymaga loginu, e-maila i hasła ze zmiennych środowiskowych,
  waliduje je i tworzy jedno konto ADMIN. Po utworzeniu administratora kolejne starty
  nie zmieniają konta ani hasła. Profil ufa nagłówkom reverse proxy, domyślnie wymaga
  bezpiecznego ciasteczka sesji i wyłącza publiczną dokumentację OpenAPI.

## Rejestracja klienta

- Publiczny `POST /api/auth/register` tworzy wyłącznie użytkownika z rolą CLIENT.
  Żądanie zawiera `username`, `email`, `password` i `passwordConfirmation` oraz wymaga CSRF.
- Login ma 3–30 znaków i ograniczony alfabet, e-mail jest normalizowany do małych liter,
  a hasło ma 8–64 znaki i nie może przekroczyć limitu 72 bajtów BCrypt.
  Login i e-mail są unikalne bez rozróżniania wielkości liter.
- Normalizację loginu i e-maila, zgodność haseł oraz limit bajtów BCrypt realizuj
  przez `AccountCredentialsPolicy`. Serwisy rejestracji, zmiany hasła i administracji
  kontami nie powinny powielać tych reguł; sprawdzenie unikalności pozostaje w
  odpowiednim przypadku użycia.
- Backend ponownie sprawdza zgodność haseł. Nigdy nie przyjmuj roli z formularza rejestracji.
  Po rejestracji frontend loguje użytkownika istniejącym mechanizmem sesji.
- `GET /api/admin/accounts` zwraca stronicowaną listę kont CLIENT, MECHANIC i ADMIN z wyszukiwaniem po loginie lub e-mailu oraz filtrowaniem po roli i aktywności. Endpointy `POST /api/admin/accounts/mechanics`, `POST /api/admin/accounts/administrators`, `PUT /api/admin/accounts/{accountId}`, `PUT /api/admin/accounts/{accountId}/password` i `PUT /api/admin/accounts/{accountId}/status` są dostępne wyłącznie dla ADMIN.
  Endpointy POST samodzielnie nadają odpowiednio rolę MECHANIC albo ADMIN i nie przyjmują roli w żądaniu. Edycja, reset hasła i zmiana aktywności dotyczą wyłącznie kont CLIENT i MECHANIC; konta ADMIN są widoczne, ale chronione przed tymi operacjami. Używaj tych samych reguł loginu, e-maila, hasła, unikalności i limitu BCrypt co przy rejestracji klienta. Dezaktywowane konto nie może się zalogować. Dotychczasowe endpointy `/api/admin/staff/mechanics` pozostają zgodnością wsteczną i nadal dotyczą tylko mechaników.
- Reset hasła klienta lub mechanika zakłada wcześniejszą weryfikację tożsamości poza aplikacją, bez automatycznych wiadomości e-mail i tokenów odzyskiwania. Administrator ustawia nowe hasło i przekazuje je użytkownikowi, który może potem skorzystać z samoobsługowej zmiany hasła.
- E-mail jest obecnie daną konta potrzebną do unikalności i przyszłego odzyskiwania
  dostępu. Pełne dane kontaktowe i rozliczeniowe powstaną w osobnym profilu klienta.
- `PUT /api/auth/password` jest dostępny dla każdego zalogowanego użytkownika i wymaga CSRF.
  Żądanie zawiera obecne hasło, nowe hasło i jego powtórzenie. Sprawdzaj obecne hasło
  przez `PasswordEncoder`, zgodność nowych haseł oraz limit 72 bajtów BCrypt.

## Profil klienta

- `GET /api/profile/me` i `PUT /api/profile/me` są dostępne wyłącznie dla CLIENT.
  ID użytkownika pobieraj z `AccountPrincipal`; nie dodawaj identyfikatora właściciela do DTO.
- Brak zapisanego profilu zwraca pusty formularz z e-mailem konta i `configured=false`.
  PUT tworzy profil albo aktualizuje istniejący rekord jeden-do-jednego z `app_users`.
- Wymagane dane to imię, nazwisko, telefon, kontaktowy e-mail, ulica i numer,
  kod pocztowy oraz miejscowość. Jeśli `hasCompanyData=true`, wymagaj także nazwy
  firmy, NIP i pełnego adresu rozliczeniowego. Przy `false` wyczyść dane firmy.
- E-mail kontaktowy profilu jest niezależny od e-maila konta używanego do rejestracji.
  Zwracaj wyłącznie DTO i zachowaj walidację zarówno Bean Validation, jak i reguł warunkowych.

## Pojazdy klienta

- Pakiet `vehicle` obsługuje `GET /api/vehicles`, `GET /api/vehicles/{vehicleId}`,
  `POST /api/vehicles` i `PUT /api/vehicles/{vehicleId}`. Endpointy są dostępne wyłącznie dla CLIENT, a POST i PUT wymagają CSRF.
- Właściciela ustalaj przez niezmienne ID konta z `AccountPrincipal`. Żądanie nie zawiera
  `ownerId`; pobieraj listę i szczegóły zapytaniami repozytorium ograniczonymi do właściciela.
  Cudzy lub nieistniejący identyfikator pojazdu zwraca ten sam błąd 404.
- Pojazd zawiera markę, model, rok produkcji, numer rejestracyjny i opcjonalny VIN.
  Rok mieści się od 1886 do następnego roku kalendarzowego. VIN ma 17 znaków bez I, O i Q.
- Numer rejestracyjny normalizuj do wielkich liter bez spacji, a VIN do wielkich liter.
  Numer rejestracyjny i podany VIN są unikalne dla jednego właściciela bez rozróżniania
  wielkości liter. Sprawdzaj konflikt w serwisie i zachowaj indeksy bazy na wypadek wyścigu.
- Normalizację i walidację marki, modelu, roku, numeru rejestracyjnego i VIN wykonuj
  przez `VehicleDataNormalizer` zarówno dla pojazdów klienta, jak i zgłoszeń gościa.
  Normalizer zwraca kanoniczne nazwy pól pojazdu; przypadek użycia gościa mapuje je
  na nazwy pól swojego DTO.
- Edycja zmienia podstawowe dane wyłącznie pojazdu należącego do zalogowanego klienta.
  Podczas kontroli unikalności pomijaj aktualizowany pojazd, aby można było pozostawić jego dotychczasową rejestrację i VIN.
- Pierwszy zapis zakończonej naprawy znajduje się przy zgłoszeniu ze statusem
  `READY_FOR_PICKUP`. Docelowa historia napraw będzie mogła zostać połączona z fakturami.
  Nie uznawaj samego zgłoszenia albo potwierdzenia wizyty za wykonaną naprawę.

## Dostępność i współbieżność

- Backend oblicza wolne dni przyjęcia auta w strefie `Europe/Warsaw`: domyślnie od poniedziałku
  do piątku, z limitem miejsc, horyzontem rezerwacji i godzinami pracy z konfiguracji admina.
  Kontrakt API zwraca datę, pojemność dnia, liczbę wolnych miejsc oraz techniczne
  znaczniki początku i końca dnia roboczego. Frontend nie wylicza dostępności samodzielnie.
- Obecna wersja używa jednego wspólnego zasobu warsztatu. Zgłoszenia `PENDING`,
  `TIME_PROPOSED` i `CONFIRMED` zajmują miejsce w bieżącym dniu. `READY_FOR_PICKUP`,
  `REJECTED` i
  `CANCELLED` zwalniają miejsce, a propozycja nowego dnia atomowo zwalnia poprzedni
  dzień i zajmuje nowy.
- Samo odczytanie wolnego dnia przed zapisem nie zabezpiecza przed wyścigiem.
  Każdy zapis zgłoszenia najpierw pobiera współdzieloną blokadę konfiguracji przez
  `ScheduleLocks`, zanim odczyta ustawienia. Zapis ustawień i wyjątków pobiera tę samą
  blokadę wyłącznie. Kolejność blokad: konfiguracja → rekord zgłoszenia → docelowy dzień.
  Używaj transakcyjnej blokady dnia oraz ponownego zliczenia aktywnych zgłoszeń
  przed zapisem. Podczas decyzji blokuj aktualizowany rekord. Sprawdzaj własność,
  aktualny status i dostępność w tej samej transakcji.
- Waliduj zakaz rezerwowania przeszłości, dostępność dnia i skonfigurowany horyzont również
  wtedy, gdy żądanie omija interfejs kalendarza. Wybrany dzień zapisuj wewnętrznie
  jako skonfigurowany początek pracy w strefie warsztatu, bez umawiania klienta na konkretną godzinę.
- Zmiana domyślnego limitu sprawdza aktywne wizyty od dziś, również poza nowym
  horyzontem rezerwacji, z uwzględnieniem wyjątków. Zapis i usunięcie wyjątku sprawdzają
  zajętość tej daty. Nie dopuszczaj limitu niższego niż liczba aktywnych wizyt;
  zwracaj HTTP 409 z kodem `SCHEDULE_CAPACITY_CONFLICT` i zachowuj poprzednią konfigurację.
- Konflikt dostępności zwracaj jako HTTP 409 z komunikatem umożliwiającym ponowny
  wybór dnia. Nie zgłaszaj sukcesu po nieudanym zapisie lub konflikcie.

## Zgłoszenia wizyt

- `GET /api/appointments/availability` jest publiczny. `POST /api/appointments/guest`
  zapisuje gościa. `GET` i `POST /api/appointments`,
  `POST /api/appointments/{id}/confirm-proposed` oraz
  `POST /api/appointments/{id}/cancel` należą do CLIENT.
- Endpointy pod `/api/staff/appointments` udostępniają MECHANIC/ADMIN listę oraz
  akcje `accept`, `reject`, `propose-time`, `confirm-proposed` dla gościa,
  `complete-repair` i `mark-picked-up`.
- Publiczny odczyt dostępności nie ujawnia danych klientów ani zgłoszeń. Publiczny
  zapis gościa wymaga tokenu CSRF, imienia i nazwiska oraz co najmniej telefonu albo
  poprawnego e-maila. Opis usterki ma od 10 do 2000 znaków.
- CLIENT tworzy zgłoszenie bez `clientId`, statusu i danych właściciela. Backend
  pobiera użytkownika z `Authentication`, wymaga uzupełnionego profilu i sprawdza,
  czy wybrany `vehicleId` do niego należy.
- CLIENT pobiera tylko własne zgłoszenia, może potwierdzić wyłącznie własną propozycję
  z aktualnym statusem `TIME_PROPOSED` i odwołać własne aktywne zgłoszenie. Gość
  nie ma publicznego endpointu odczytu statusu.
- MECHANIC i ADMIN pobierają kolejkę zgłoszeń i wykonują operacje decyzji. Propozycja
  nowego dnia musi wskazywać wolny dzień zwrócony przez te same reguły dostępności.
  Personel może zatwierdzić propozycję gościa dopiero po kontakcie poza aplikacją.
- MECHANIC i ADMIN mogą zakończyć naprawę dla statusu `CONFIRMED`. Formularz
  zakończenia wymaga opisu wykonanych prac oraz co najmniej jednej pozycji robocizny lub części z dodatnią ilością i ceną brutto.
- Przechowuj pierwotny dzień, bieżący dzień, kopie danych kontaktowych i pojazdu,
  opis, publiczny losowy numer referencyjny, czas utworzenia, dane decyzji oraz
  dane zakończenia naprawy.

## Baza i dokumenty napraw

- Zmiany schematu wykonuj migracjami Flyway w `src/main/resources/db/migration`.
  Kolejne zmiany wprowadzaj nowymi migracjami. Nie zastępuj ich `ddl-auto=update`.
- Kwoty pieniężne przechowuj i obliczaj w `BigDecimal` oraz odpowiednim typie
  `numeric` w PostgreSQL; ustal skalę, walutę i zaokrąglenia z modelem rozliczeń.
- Faktura musi mieć powiązanie pozwalające pokazać udokumentowane naprawy właściwego
  pojazdu i klienta. Klient pobiera PDF tylko dla własnego pojazdu i zakończonego zgłoszenia,
  a mechanik lub administrator może pobrać PDF zakończonego zgłoszenia klienta z kontem
  przez endpoint personelu. Nie generuj historii wykonanych napraw ze zgłoszeń oczekujących.
- Zapis zakończenia naprawy przechowuje opis prac oraz pozycje robocizny i części.
  Backend wylicza sumę brutto z pozycji, a prosty PDF faktury wyszczególnia te pozycje oraz pokazuje wartości netto i brutto.
  Nie dodawaj jeszcze korekt, płatności online ani pełnej integracji księgowej bez osobnego wymagania.
- Zachowuj dane i pozycje wystawionego dokumentu z momentu jego wystawienia.
  Późniejsza zmiana profilu klienta lub oferty usług nie może zmieniać historii faktury.
- `InvoiceService` utrwala dokument przy przejściu klienta z kontem do `COMPLETED`,
  w tej samej transakcji co odbiór. Błąd wygenerowania dokumentu wycofuje odbiór.
  `invoice_documents` ma jeden rekord na zgłoszenie, unikalny numer, wersjonowany
  `InvoiceSnapshot` w JSONB i gotowy PDF w BYTEA. Encja jest niezmienna (`@Immutable`).
  Nie generuj ponownie zapisanego PDF-u z aktualnego profilu, logo ani zegara.
- Starsze zakończone naprawy bez dokumentu utrwalaj przy pierwszym autoryzowanym
  pobraniu; blokada rekordu zgłoszenia i ponowny odczyt dokumentu chronią równoczesne
  pobrania. To zgodność ze starymi danymi, nie odtworzenie dawnego profilu klienta.
  Taki odczyt wymaga transakcji pozwalającej na zapis. Kontrolę właściciela/roli
  wykonuj przed wywołaniem serwisu dokumentów. Goście nie otrzymują faktur w tym zakresie.
- `RepairAmounts`: cena wejściowa jest brutto, wartość pozycji to ilość × cena
  zaokrąglona do 2 miejsc `HALF_UP`. Po zaokrągleniu pozycja i cała naprawa muszą
  mieścić się w 0,01–99 999 999,99; błędy zwracaj jako walidację przed zapisem encji.
  Przy obecnej demonstracyjnej stawce 23% netto pozycji to brutto / 1,23 (`HALF_UP`),
  VAT to brutto minus netto. Suma netto/VAT/brutto dokumentu jest sumą odpowiednich
  wartości pozycji. PDF pokazuje wejściową cenę jednostkową brutto, bez zaokrąglonej
  ceny jednostkowej netto sugerującej inny wynik mnożenia.
- Sposób wystawiania, statusy, korekty, numeracja i eksport dokumentów pozostają
  do ustalenia. Nie deklaruj zgodności księgowej na podstawie samego modelu danych.
- Stosuj Bean Validation dla wejścia oraz walidację biznesową w serwisach.
  Utrzymuj spójny format błędów API bez ujawniania szczegółów bazy lub danych innych klientów.
  Kontrolowane wyjątki aplikacji dziedziczą po `ApiException`, które przechowuje status
  HTTP, stabilny `ApiErrorCode` i opcjonalne błędy pól. `GlobalExceptionHandler` mapuje
  je wspólnym handlerem; osobny handler dodawaj tylko dla innego formatu lub sposobu
  obsługi błędu, a nie dla kolejnej klasy wyjątku biznesowego.
  Docelowo błędy API powinny zawierać stabilny kod techniczny i angielski komunikat
  techniczny, a frontend powinien tłumaczyć znane kody na język wybrany w UI.
  Nie traktuj polskich tekstów z backendu jako docelowych komunikatów interfejsu.

## Sprawdzanie zmian

- Testuj reguły statusów, granice dni rezerwacji, własność pojazdu i dostęp do dokumentów.
  Dla przyjmowania zgłoszeń dodaj test konfliktu równoczesnych prób rezerwacji.
- Testy integracyjne korzystają z odizolowanego PostgreSQL przez Testcontainers
  i `PostgresTestConfiguration`. Nie używaj roboczego wolumenu użytkownika do testów.
- Z katalogu `backend`: `.\mvnw.cmd test` uruchamia testy,
  `.\mvnw.cmd spring-boot:run` uruchamia lokalny backend. Na systemach Unix użyj `./mvnw`.
- Testy wymagają działającego Docker Desktop. Automatyczne Compose i konta demo
  są wyłączone w profilu testowym. Testy katalogu obejmują CRUD, walidację,
  uprawnienia, logowanie, rejestrację, profil, pojazdy, zgłoszenia wizyt i ich
  współbieżność, sesję oraz CSRF. Dockerfile pomija uruchamianie testów, więc udany
  obraz nie potwierdza ich zaliczenia.
- Z głównego folderu `docker compose up -d --build backend` przebudowuje backend.
  Po zmianie infrastruktury sprawdź stan usług i odpowiedź `/api/health`.
