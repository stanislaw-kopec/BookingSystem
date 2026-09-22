# Plan poprawek po audycie

Źródło: [audyt z 16.09.2026](project-audit-2026-09-16.md).
Pracujemy małymi etapami. Punkt oznaczamy jako zakończony po implementacji i
weryfikacji; sam opis planu nie oznacza wykonania poprawki.

## Etap 1 — bezpieczeństwo kont i sesji (A01–A02)

Status: zakończony 16.09.2026.

- [x] Principal z niezmiennym ID konta; odczyt i zapis danych klienta oraz autorów decyzji według ID.
- [x] Weryfikacja aktywności konta i wersji sesji przy żądaniach.
- [x] Dezaktywacja i reset hasła odbierają dostęp istniejącym sesjom; ponowna aktywacja ich nie przywraca.
- [x] Samodzielna zmiana hasła zachowuje bieżącą sesję i odbiera dostęp pozostałym.
- [x] Testy prawdziwego logowania: zmiana i ponowne użycie loginu, dezaktywacja, reset, zmiana własnego hasła.
- [x] Pełny zestaw testów backendu i aktualizacja zasad bezpieczeństwa w AGENTS.md.

## Etap 2 — konfiguracja i widok grafiku (A03–A04, A07)

Status: zakończony 17.09.2026.

- [x] Walidacja obniżenia limitu i usunięcia wyjątku, gdy istnieją rezerwacje.
- [x] Wspólna synchronizacja zmian konfiguracji i rezerwacji, również przy równoczesnych żądaniach.
- [x] Grafik uwzględnia weekendy otwarte wyjątkiem, dni zamknięte i pojemność konkretnego dnia.
- [x] Endpoint po zakresie dat z lekkim DTO, zamiast pobierania całej historii.
- [x] Testy wyjątków i wyścigów; sprawdzenie zapytań SQL i potrzebnych indeksów.

## Etap 3 — faktury i kwoty (A05–A06)

Status: zakończony 17.09.2026. [Opis rozwiązania i ograniczeń](invoice-snapshots-walkthrough.md).

- [x] Utrwalenie danych nabywcy, sprzedawcy, numeru, daty i pozycji dokumentu.
- [x] Ponowne pobranie faktury nie zmienia danych po edycji profilu ani zmianie dnia.
- [x] Wspólne zasady obliczania i zaokrąglania netto, VAT i brutto.
- [x] Walidacja wynikowej wartości pozycji oraz sumy naprawy przed zapisem w bazie.
- [x] Testy treści PDF, różnic groszowych, małych wartości i przekroczenia limitów.

## Etap 4 — działanie frontendu (A08–A11)

Status: zakończony 17.09.2026. [Opis rozwiązania i testów](frontend-state-walkthrough.md).

- [x] Ponowny wybór aktualnego filtra/sortowania nie uruchamia nieskończonego ładowania.
- [x] Błąd odświeżenia listy jest widoczny; stare wyniki nie udają wyniku nowych filtrów.
- [x] Obsługa utraty sesji, usuwanie prywatnych widoków i ponowne logowanie; synchronizacja kart.
- [x] Polskie komunikaty walidacji oraz wskazanie konkretnej pozycji naprawy z błędem.
- [x] CustomSelect: fokus/ARIA, klawiatura i przewijanie aktywnej opcji.
- [x] Mały zestaw testów zachowania UI uruchamiany także w CI.

## Etap 5 — weryfikacja i prezentacja

Status: w toku od 17.09.2026. Cztery z pięciu punktów zakończone.

- [x] Scenariusz E2E: klient rezerwuje → personel potwierdza → naprawa → odbiór → PDF.
- [ ] Screeny zamiast placeholderów w README, ewentualnie krótki film demonstracyjny.
- [x] Aktualne wersje technologii w dokumentacji i uporządkowana roadmapa.
- [x] Raporty testów dostępne po nieudanym CI; sprawdzone uruchomienie z README.
- [x] Krótki opis znanych ograniczeń i najważniejszych decyzji technicznych.

Gotowy jest podgląd faktury `06-invoice-preview.png`. Pozostają trzy kadry interfejsu:
formularz zgłoszenia, grafik mechanika i formularz zakończenia naprawy.

## Opcjonalne działania po głównych poprawkach

- [x] Osobny wariant wdrożeniowy: statyczny frontend, HTTPS, konfiguracja i sekrety poza profilem local.
- [ ] Przed publicznym demo: ograniczanie nadużyć logowania i formularza rezerwacji.
- [ ] Identyfikatory żądań, diagnostyka błędów i ślad ważnych operacji administracyjnych.
- [ ] Podział dużego komponentu obsługi zgłoszeń, wspólne pobieranie PDF i mniejszy plik logo.

Pełna księgowość, płatności online, SMS/e-mail, mikroserwisy i zmiana sesji na JWT
pozostają poza tym planem. Po każdym etapie zapisujemy poniżej wynik sprawdzeń.

## Etap 6 — wariant wdrożeniowy

Status: zakończony 19.09.2026. [Instrukcja wdrożenia](deployment-guide.md).

- [x] Statyczny build Reacta serwowany przez Nginx zamiast Vite dev servera.
- [x] Wspólny origin dla SPA i `/api`; backend i PostgreSQL bez publicznych portów.
- [x] Caddy jako warstwa brzegowa z automatycznym HTTPS dla skonfigurowanej domeny.
- [x] Profil `production` bez danych demo, z bezpiecznym ciasteczkiem i wyłączonym Swaggerem.
- [x] Pierwszy administrator tworzony jednorazowo z konfiguracji środowiska.
- [x] Przykładowy plik zmiennych, instrukcja uruchomienia i walidacja Compose w CI.

## Etap 7 — czytelna architektura backendu

Status: w toku od 19.09.2026; etapy 7A i 7B zakończone. Szczegółowy audyt i kolejność prac znajdują się w
[planie refaktoryzacji backendu](backend-code-audit.md).

- [x] 7A: wspólny zegar, mapery odpowiedzi i wydzielona walidacja naprawy.
- [x] 7B: podział `AppointmentService` według przypadków użycia i ochrona przejść statusów w domenie.
- [ ] 7C: podział pakietów `appointment`, DTO i dużego testu integracyjnego.
- [ ] Etap 8: wspólne reguły kont, danych pojazdu i wyjątków API.

## Dziennik wykonania

- 16.09.2026: utworzono plan i rozpoczęto etap 1. Stan wyjściowy audytu: 81 testów backendu przeszło; lint i build frontendu przeszły.
- 16.09.2026: zakończono etap 1. Dodano migrację V16, principal z ID, kontrolę wersji sesji i testy prawdziwego logowania. Wynik `mvn -B -ntp test`: 90 testów, 0 niepowodzeń, 0 błędów, 0 pominięć. Zestaw obejmuje 9 nowych przypadków bezpieczeństwa sesji. Testy działają na odizolowanym PostgreSQL; działającej bazy aplikacji nie zmieniano. Frontend pozostaje bez zmian w tym etapie.
- 17.09.2026: zakończono etap 2. Pełny przebieg `mvn -B -ntp test` z 16.09: 114 testów, 0 niepowodzeń, 0 błędów, 0 pominięć. Dodano 15 przypadków konfiguracji, uprawnień, zakresu dat, zmiany czasu i zapytań SQL oraz 9 przypadków współbieżności. Lint i build frontendu przeszły. Testy używały odizolowanego PostgreSQL w Testcontainers.
- 17.09.2026: przebudowano lokalny Compose z zachowaniem wolumenu danych. Sprawdzono w przeglądarce logowanie mechanika, siedem dni grafiku z istniejącymi wyjątkami zamknięcia, poprzedni/następny tydzień, odświeżanie i pusty tydzień. Widok 390 × 844 mieści się bez poziomego przewijania; sprawdzono karty i otwarcie szczegółów klawiszem Enter. Otwarcie soboty i konflikt zmian konfiguracji pokrywają testy integracyjne; nie zmieniano w tym celu roboczych rezerwacji ani ustawień warsztatu.

- 17.09.2026: zakończono etap 3. Migracja V18 dodaje trwałe dokumenty faktur; zapis odbywa się przy odbiorze, a dla starszych napraw przy pierwszym pobraniu. Pełny końcowy przebieg testów backendu: 126 testów, 0 niepowodzeń, 0 błędów, 0 pominięć, w tym 12 nowych przypadków faktur i kwot. Zweryfikowano treść PDF-ów, niezmienność po zmianie danych i zegara, równoczesne pobrania oraz wycofanie odbioru po błędzie generatora. Wizualnie sprawdzono fakturę prywatną, firmową, trzystronicową (30 pozycji), zaokrąglony grosz i maksymalną kwotę. Frontend nie wymagał zmian; adresy pobierania pozostały takie same.
- 17.09.2026: zakończono etap 4. Dodano 22 testy frontendu w Vitest i React Testing Library oraz ich uruchamianie w CI. Testy obejmują powtórny wybór filtrów, błąd odświeżenia i ponowienie, spóźnione odpowiedzi, listę kont, utratę sesji przy JSON/PDF/CSRF, synchronizację kart, ponowne logowanie, walidację pozycji naprawy i klawiaturę CustomSelect. Wszystkie 22 testy przeszły; lint bez ostrzeżeń oraz build przeszły. W przeglądarce sprawdzono listy klienta i mechanika, ponowny wybór filtra, przewijanie aktywnej opcji, wylogowanie w drugiej karcie oraz formularz naprawy. Widok 390 × 844 nie ma poziomego przewijania. Formularza naprawy nie zapisano; nie zmieniano roboczych zgłoszeń. Uruchomiono lokalny Compose z istniejącym wolumenem. Backend nie wymagał zmian ani ponownego uruchamiania jego testów w tym etapie. Workflow został zaktualizowany, ale zdalny przebieg GitHub Actions nastąpi po pushu.
- 17.09.2026: wykonano cztery z pięciu punktów etapu 5. W działającym Compose przeprowadzono scenariusz zgłoszenia `a3b0d95d-6638-4ebc-b75b-b7364fc1beb2`: klient `anna.demo` zarezerwował Hondę Civic, mechanik potwierdził dzień, zapisał robociznę i część na 1130,00 zł brutto, zakończył naprawę i potwierdził odbiór. Wpis pojawił się w historii pojazdu, a PDF MC/2026/000014 pobrano i sprawdzono wizualnie. Testy backendu: 126/126; frontendu: 22/22; lint i build przeszły. Polecenie `docker compose up --build -d --wait` zakończyło się powodzeniem, trzy usługi były zdrowe, a aplikacja, health check i Swagger odpowiedziały HTTP 200. CI publikuje raporty nieudanych testów. Uzupełniono wersje, decyzje, ograniczenia i roadmapę. Pozostały trzy ręczne zrzuty interfejsu; podgląd faktury jest gotowy.
- 19.09.2026: zakończono etap 6. Dodano oddzielny stos wdrożeniowy: Caddy → Nginx ze statycznym SPA → Spring Boot → PostgreSQL. Testowy projekt Compose uruchomił cztery zdrowe usługi na oddzielnych sieciach i wolumenach. Strona oraz `/api/health` odpowiedziały HTTP 200, Swagger był wyłączony (404), a pierwszy administrator zalogował się z rolą ADMIN. Potwierdzono cache zasobów, brak cache HTML, nagłówki bezpieczeństwa oraz brak publicznych portów bazy i backendu. Pełne testy backendu: 129/129; frontendu: 22/22; lint i build przeszły. Oba pliki Compose przeszły walidację.
- 19.09.2026: zakończono etap 7A audytu backendu. Jeden bean `Clock` w strefie warsztatu zastąpił bezpośrednie odczyty czasu w kodzie produkcyjnym. Wydzielono mapery odpowiedzi zgłoszenia, historii i pozycji naprawy oraz walidator pozycji naprawy; `VehicleService` i `AppointmentService` nie duplikują już mapowania historii. Dodano 6 testów jednostkowych. Pełny wynik `mvn -B -ntp test`: 135 testów, 0 niepowodzeń, 0 błędów i 0 pominięć. Endpointy i schemat bazy pozostały bez zmian.
- 20.09.2026: zakończono etap 7B audytu backendu. Monolityczny `AppointmentService` zastąpiono usługami rezerwacji, zapytań, operacji klienta, decyzji personelu i obsługi napraw. Reguły przejść statusów przeniesiono do `AppointmentRequest`; kontrolery zachowały dotychczasowy kontrakt HTTP, a kolejność blokad i granice transakcji pozostały bez zmian. Dodano 5 testów jednostkowych maszyny stanów. Pełny wynik `mvn -B -ntp test`: 140 testów, 0 niepowodzeń, 0 błędów i 0 pominięć. Schemat bazy i frontend pozostały bez zmian.

## Jak działa poprawka grafiku

`ScheduleLocks` używa blokad transakcyjnych PostgreSQL. Zapisy zgłoszeń pobierają
blokadę współdzieloną przed odczytem konfiguracji, a zapisy administratora — wyłączną.
Następnie rezerwacja blokuje docelowy dzień i ponownie liczy zajęte miejsca.
Zapewnia to wspólną kolejność: konfiguracja → rekord zgłoszenia → docelowy dzień.
Jeżeli administrator zmieni limit jako pierwszy, rezerwacja sprawdzi nowe ustawienia.
Jeżeli pierwsza zapisze się rezerwacja, administrator uwzględni ją w walidacji.

Testy obu kolejności obejmują zmianę domyślnego limitu, usunięcie wyjątku,
zamknięcie dnia oraz proponowanie innej daty. Osobny test sprawdza użycie nowych
godzin pracy przez rezerwację czekającą na zatwierdzenie ustawień.

`GET /api/staff/appointments/schedule` wymaga `startDate` i `endDate` oraz dopuszcza
1–31 dni włącznie. Zastępuje nieograniczony `/all`. Zwraca lekkie DTO z aktywnymi
zgłoszeniami i konfiguracją każdego dnia. Odczyt `REPEATABLE_READ` zapewnia wspólny
obraz ustawień, wyjątków i wizyt. Test 28 zgłoszeń potwierdza 3 zapytania SQL,
bez ładowania encji zgłoszeń i ich relacji.

Migracja V17 zastępuje indeks wyrażenia daty indeksem `(status, current_start_at)`.
`EXPLAIN (ANALYZE, BUFFERS)` na 4000 rekordów testowych pokazał `Index Scan`
po nowym indeksie i odczyt 7 zgłoszeń wybranego tygodnia. To kontrola planu zapytania
na danych testowych, nie benchmark wydajności produkcyjnej.

Zmiana domyślnej pojemności waliduje dziś i przyszłość, również poza skróconym
horyzontem. System nie przechowuje historii konfiguracji: przeglądając dawny tydzień,
personel widzi aktualny limit domyślny i zachowane wyjątki. Faktury i kwoty opisuje ukończony etap 3; obsługę stanu frontendu opisuje ukończony etap 4.

## Jak działa poprawka sesji

Podczas logowania Spring Security zapisuje w principalu ID konta i aktualną
`sessionVersion`. Backend sprawdza je w bazie przy każdym uwierzytelnionym żądaniu.
Login można zmienić, ale ID pozostaje takie samo, więc zmiana nazwy nie przekierowuje
sesji do danych innej osoby. Nazwa widoczna w odpowiedzi sesji jest odświeżana.

Reset hasła lub zmiana aktywności zwiększa wersję konta. Stara sesja ma wcześniejszą
wartość, dlatego kolejne żądanie otrzymuje 401, a sesja jest czyszczona. Przy własnej
zmianie hasła zapisujemy nową wersję tylko w bieżącej sesji. Pozostałe urządzenia
muszą zalogować się ponownie. Nie przerywa to operacji, które już przeszły kontrolę
przed zatwierdzeniem zmiany konta.

Kosztem tego prostego rozwiązania jest jeden dodatkowy odczyt konta z bazy przy
uwierzytelnionym żądaniu. Nie potrzeba dodatkowego magazynu sesji ani JWT. Migracja
V16 dodaje licznik do istniejących kont bez usuwania danych. Automatyczna reakcja
interfejsu na 401 została dodana w etapie 4.
