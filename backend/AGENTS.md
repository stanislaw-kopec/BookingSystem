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
- Zachowaj prefiks `/api`, działanie `/api/health` i istniejącą konfigurację Compose.
  Lokalny Spring zarządza tylko usługą `postgres`; w kontenerze integracja Springa
  z Compose jest wyłączona i używane są ustawienia połączenia ze zmiennych środowiska.

## Model domeny i przepływy

- Odwzoruj wymagania R01–R11. Użytkownik, pojazd, usługa warsztatu, zgłoszenie,
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
- Personel może zakończyć wyłącznie zgłoszenie ze statusem `CONFIRMED`. Zakończenie
  zapisuje opis wykonanych prac, kwotę brutto do zapłaty, czas zamknięcia i użytkownika
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
  BCrypt i ciasteczko sesji HttpOnly. Role wynikają z bazy, nie z formularza klienta.
- `local` tworzy brakujące konta demonstracyjne; nie zmienia haseł istniejących kont.
  Nie stosuj demonstracyjnej bazy ani jej kont we wdrożeniu produkcyjnym.

## Rejestracja klienta

- Publiczny `POST /api/auth/register` tworzy wyłącznie użytkownika z rolą CLIENT.
  Żądanie zawiera `username`, `email`, `password` i `passwordConfirmation` oraz wymaga CSRF.
- Login ma 3–30 znaków i ograniczony alfabet, e-mail jest normalizowany do małych liter,
  a hasło ma 8–64 znaki i nie może przekroczyć limitu 72 bajtów BCrypt.
  Login i e-mail są unikalne bez rozróżniania wielkości liter.
- Backend ponownie sprawdza zgodność haseł. Nigdy nie przyjmuj roli z formularza rejestracji.
  Po rejestracji frontend loguje użytkownika istniejącym mechanizmem sesji.
- E-mail jest obecnie daną konta potrzebną do unikalności i przyszłego odzyskiwania
  dostępu. Pełne dane kontaktowe i rozliczeniowe powstaną w osobnym profilu klienta.

## Profil klienta

- `GET /api/profile/me` i `PUT /api/profile/me` są dostępne wyłącznie dla CLIENT.
  Nazwę użytkownika pobieraj z `Authentication`; nie dodawaj identyfikatora właściciela do DTO.
- Brak zapisanego profilu zwraca pusty formularz z e-mailem konta i `configured=false`.
  PUT tworzy profil albo aktualizuje istniejący rekord jeden-do-jednego z `app_users`.
- Wymagane dane to imię, nazwisko, telefon, kontaktowy e-mail, ulica i numer,
  kod pocztowy oraz miejscowość. Jeśli `hasCompanyData=true`, wymagaj także nazwy
  firmy, NIP i pełnego adresu rozliczeniowego. Przy `false` wyczyść dane firmy.
- E-mail kontaktowy profilu jest niezależny od e-maila konta używanego do rejestracji.
  Zwracaj wyłącznie DTO i zachowaj walidację zarówno Bean Validation, jak i reguł warunkowych.

## Pojazdy klienta

- Pakiet `vehicle` obsługuje `GET /api/vehicles`, `GET /api/vehicles/{vehicleId}`
  i `POST /api/vehicles`. Endpointy są dostępne wyłącznie dla CLIENT, a POST wymaga CSRF.
- Właściciela ustalaj przez nazwę użytkownika z `Authentication`. Żądanie nie zawiera
  `ownerId`; pobieraj listę i szczegóły zapytaniami repozytorium ograniczonymi do właściciela.
  Cudzy lub nieistniejący identyfikator pojazdu zwraca ten sam błąd 404.
- Pojazd zawiera markę, model, rok produkcji, numer rejestracyjny i opcjonalny VIN.
  Rok mieści się od 1886 do następnego roku kalendarzowego. VIN ma 17 znaków bez I, O i Q.
- Numer rejestracyjny normalizuj do wielkich liter bez spacji, a VIN do wielkich liter.
  Numer rejestracyjny i podany VIN są unikalne dla jednego właściciela bez rozróżniania
  wielkości liter. Sprawdzaj konflikt w serwisie i zachowaj indeksy bazy na wypadek wyścigu.
- Pierwszy zapis zakończonej naprawy znajduje się przy zgłoszeniu ze statusem
  `READY_FOR_PICKUP`. Docelowa historia napraw będzie mogła zostać połączona z fakturami.
  Nie uznawaj samego zgłoszenia albo potwierdzenia wizyty za wykonaną naprawę.

## Dostępność i współbieżność

- Backend oblicza wolne dni przyjęcia auta w strefie `Europe/Warsaw`: od poniedziałku
  do piątku, z limitem 4 aktywnych zgłoszeń dziennie i na najbliższe 30 dni.
  Kontrakt API zwraca datę, pojemność dnia, liczbę wolnych miejsc oraz techniczne
  znaczniki początku i końca dnia roboczego. Frontend nie wylicza dostępności samodzielnie.
- Pierwsza wersja używa jednego wspólnego zasobu warsztatu. Zgłoszenia `PENDING`,
  `TIME_PROPOSED` i `CONFIRMED` zajmują miejsce w bieżącym dniu. `READY_FOR_PICKUP`,
  `REJECTED` i
  `CANCELLED` zwalniają miejsce, a propozycja nowego dnia atomowo zwalnia poprzedni
  dzień i zajmuje nowy.
- Samo odczytanie wolnego dnia przed zapisem nie zabezpiecza przed wyścigiem.
  Używaj transakcyjnej blokady dnia oraz ponownego zliczenia aktywnych zgłoszeń
  przed zapisem. Podczas decyzji blokuj aktualizowany rekord. Sprawdzaj własność,
  aktualny status i dostępność w tej samej transakcji.
- Waliduj zakaz rezerwowania przeszłości, dzień tygodnia i horyzont 30 dni również
  wtedy, gdy żądanie omija interfejs kalendarza. Wybrany dzień zapisuj wewnętrznie
  jako 08:00 w strefie warsztatu, bez umawiania klienta na konkretną godzinę.
- Konflikt dostępności zwracaj jako HTTP 409 z komunikatem umożliwiającym ponowny
  wybór dnia. Nie zgłaszaj sukcesu po nieudanym zapisie lub konflikcie.

## Zgłoszenia wizyt

- `GET /api/appointments/availability` jest publiczny. `POST /api/appointments/guest`
  zapisuje gościa. `GET` i `POST /api/appointments`,
  `POST /api/appointments/{id}/confirm-proposed` oraz
  `POST /api/appointments/{id}/cancel` należą do CLIENT.
- Endpointy pod `/api/staff/appointments` udostępniają MECHANIC/ADMIN listę oraz
  akcje `accept`, `reject`, `propose-time`, `confirm-proposed` dla gościa i
  `complete-repair`.
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
  zakończenia wymaga opisu wykonanych prac i kwoty brutto większej od zera.
- Przechowuj pierwotny dzień, bieżący dzień, kopie danych kontaktowych i pojazdu,
  opis, publiczny losowy numer referencyjny, czas utworzenia, dane decyzji oraz
  dane zakończenia naprawy.

## Baza i dokumenty napraw

- Zmiany schematu wykonuj migracjami Flyway w `src/main/resources/db/migration`.
  Kolejne zmiany wprowadzaj nowymi migracjami. Nie zastępuj ich `ddl-auto=update`.
- Kwoty pieniężne przechowuj i obliczaj w `BigDecimal` oraz odpowiednim typie
  `numeric` w PostgreSQL; ustal skalę, walutę i zaokrąglenia z modelem rozliczeń.
- Faktura musi mieć powiązanie pozwalające pokazać udokumentowane naprawy właściwego
  pojazdu i klienta. Nie generuj historii wykonanych napraw ze zgłoszeń oczekujących.
- Obecny zapis zakończenia naprawy przechowuje jedną kwotę brutto i opis prac.
  Nie rozbijaj jej jeszcze na netto, VAT, pozycje faktury ani płatności online bez
  osobnego wymagania.
- Zachowuj dane i pozycje wystawionego dokumentu z momentu jego wystawienia.
  Późniejsza zmiana profilu klienta lub oferty usług nie może zmieniać historii faktury.
- Sposób wystawiania, statusy, korekty, numeracja i eksport dokumentów pozostają
  do ustalenia. Nie deklaruj zgodności księgowej na podstawie samego modelu danych.
- Stosuj Bean Validation dla wejścia oraz walidację biznesową w serwisach.
  Utrzymuj spójny format błędów API bez ujawniania szczegółów bazy lub danych innych klientów.

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
