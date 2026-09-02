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

- Odwzoruj wymagania R01–R09. Użytkownik, pojazd, usługa warsztatu, zgłoszenie,
  wizyta, zlecenie naprawy i faktura mają różne odpowiedzialności.
  Szczegółowe encje i relacje dobieraj przy implementacji konkretnego etapu.
- Zgłoszenie obejmuje klienta, jego pojazd, termin, opis usterki oraz status decyzji.
  Przyjęcie i odrzucenie muszą być dostępnymi tylko personelowi operacjami serwisowymi.
- Waliduj dozwolone przejścia statusów. Odnotowuj czas i autora decyzji personelu.
  Nie pozwalaj na dwukrotne przetworzenie tego samego zgłoszenia.
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

## Dostępność i współbieżność

- Backend oblicza wolne terminy od poniedziałku do piątku na podstawie harmonogramu
  oraz ograniczeń zasobów, także gdy klient wysyła żądanie poza interfejsem kalendarza.
- Reguły długości wizyty, przypisywania mechanika, dni zamknięcia i blokowania
  terminów przez oczekujące zgłoszenia wymagają ustalenia przed budową kalendarza.
- Przyjęcie zgłoszenia i zajęcie odpowiednich zasobów muszą być atomowe.
  Samo odczytanie wolnego terminu przed zapisem nie zabezpiecza przed wyścigiem.
  Dobierz zabezpieczenie transakcyjne/bazodanowe do modelu i sprawdź równoległe żądania.
- Waliduj początek i koniec przedziału, zakaz rezerwowania przeszłości i nakładanie
  wizyt. Jawnie ustal obsługę strefy czasowej; kontrakt API ma jednoznacznie opisywać czas.
- Konflikt dostępności zwracaj jako HTTP 409 z komunikatem umożliwiającym ponowny
  wybór terminu. Nie zgłaszaj sukcesu po nieudanym zapisie lub konflikcie.

## Baza i dokumenty napraw

- Zmiany schematu wykonuj migracjami Flyway w `src/main/resources/db/migration`.
  Kolejne zmiany wprowadzaj nowymi migracjami. Nie zastępuj ich `ddl-auto=update`.
- Kwoty pieniężne przechowuj i obliczaj w `BigDecimal` oraz odpowiednim typie
  `numeric` w PostgreSQL; ustal skalę, walutę i zaokrąglenia z modelem rozliczeń.
- Faktura musi mieć powiązanie pozwalające pokazać udokumentowane naprawy właściwego
  pojazdu i klienta. Nie generuj historii wykonanych napraw ze zgłoszeń oczekujących.
- Zachowuj dane i pozycje wystawionego dokumentu z momentu jego wystawienia.
  Późniejsza zmiana profilu klienta lub oferty usług nie może zmieniać historii faktury.
- Sposób wystawiania, statusy, korekty, numeracja i eksport dokumentów pozostają
  do ustalenia. Nie deklaruj zgodności księgowej na podstawie samego modelu danych.
- Stosuj Bean Validation dla wejścia oraz walidację biznesową w serwisach.
  Utrzymuj spójny format błędów API bez ujawniania szczegółów bazy lub danych innych klientów.

## Sprawdzanie zmian

- Testuj reguły statusów, granice terminów, własność pojazdu i dostęp do dokumentów.
  Dla przyjmowania zgłoszeń dodaj test konfliktu równoczesnych prób rezerwacji.
- Testy integracyjne korzystają z odizolowanego PostgreSQL przez Testcontainers
  i `PostgresTestConfiguration`. Nie używaj roboczego wolumenu użytkownika do testów.
- Z katalogu `backend`: `.\mvnw.cmd test` uruchamia testy,
  `.\mvnw.cmd spring-boot:run` uruchamia lokalny backend. Na systemach Unix użyj `./mvnw`.
- Testy wymagają działającego Docker Desktop. Automatyczne Compose i konta demo
  są wyłączone w profilu testowym. Testy katalogu obejmują CRUD, walidację,
  uprawnienia, logowanie, sesję i CSRF. Dockerfile pomija uruchamianie testów,
  więc udany obraz nie potwierdza ich zaliczenia.
- Z głównego folderu `docker compose up -d --build backend` przebudowuje backend.
  Po zmianie infrastruktury sprawdź stan usług i odpowiedź `/api/health`.
