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

- [ ] Utrwalenie danych nabywcy, sprzedawcy, numeru, daty i pozycji dokumentu.
- [ ] Ponowne pobranie faktury nie zmienia danych po edycji profilu ani zmianie dnia.
- [ ] Wspólne zasady obliczania i zaokrąglania netto, VAT i brutto.
- [ ] Walidacja wynikowej wartości pozycji oraz sumy naprawy przed zapisem w bazie.
- [ ] Testy treści PDF, różnic groszowych, małych wartości i przekroczenia limitów.

## Etap 4 — działanie frontendu (A08–A11)

- [ ] Ponowny wybór aktualnego filtra/sortowania nie uruchamia nieskończonego ładowania.
- [ ] Błąd odświeżenia listy jest widoczny; stare wyniki nie udają wyniku nowych filtrów.
- [ ] Obsługa utraty sesji, usuwanie prywatnych widoków i ponowne logowanie; synchronizacja kart.
- [ ] Polskie komunikaty walidacji oraz wskazanie konkretnej pozycji naprawy z błędem.
- [ ] CustomSelect: fokus/ARIA, klawiatura i przewijanie aktywnej opcji.
- [ ] Mały zestaw testów zachowania UI uruchamiany także w CI.

## Etap 5 — weryfikacja i prezentacja

- [ ] Scenariusz E2E: klient rezerwuje → personel potwierdza → naprawa → odbiór → PDF.
- [ ] Screeny zamiast placeholderów w README, ewentualnie krótki film demonstracyjny.
- [ ] Aktualne wersje technologii w dokumentacji i uporządkowana roadmapa.
- [ ] Raporty testów dostępne po nieudanym CI; sprawdzone uruchomienie z README.
- [ ] Krótki opis znanych ograniczeń i najważniejszych decyzji technicznych.

## Opcjonalne działania po głównych poprawkach

- [ ] Osobny wariant wdrożeniowy: statyczny frontend, HTTPS, konfiguracja i sekrety poza profilem local.
- [ ] Przed publicznym demo: ograniczanie nadużyć logowania i formularza rezerwacji.
- [ ] Identyfikatory żądań, diagnostyka błędów i ślad ważnych operacji administracyjnych.
- [ ] Podział dużego komponentu obsługi zgłoszeń, wspólne pobieranie PDF i mniejszy plik logo.

Pełna księgowość, płatności online, SMS/e-mail, mikroserwisy i zmiana sesji na JWT
pozostają poza tym planem. Po każdym etapie zapisujemy poniżej wynik sprawdzeń.

## Dziennik wykonania

- 16.09.2026: utworzono plan i rozpoczęto etap 1. Stan wyjściowy audytu: 81 testów backendu przeszło; lint i build frontendu przeszły.
- 16.09.2026: zakończono etap 1. Dodano migrację V16, principal z ID, kontrolę wersji sesji i testy prawdziwego logowania. Wynik `mvn -B -ntp test`: 90 testów, 0 niepowodzeń, 0 błędów, 0 pominięć. Zestaw obejmuje 9 nowych przypadków bezpieczeństwa sesji. Testy działają na odizolowanym PostgreSQL; działającej bazy aplikacji nie zmieniano. Frontend pozostaje bez zmian w tym etapie.
- 17.09.2026: zakończono etap 2. Pełny przebieg `mvn -B -ntp test` z 16.09: 114 testów, 0 niepowodzeń, 0 błędów, 0 pominięć. Dodano 15 przypadków konfiguracji, uprawnień, zakresu dat, zmiany czasu i zapytań SQL oraz 9 przypadków współbieżności. Lint i build frontendu przeszły. Testy używały odizolowanego PostgreSQL w Testcontainers.
- 17.09.2026: przebudowano lokalny Compose z zachowaniem wolumenu danych. Sprawdzono w przeglądarce logowanie mechanika, siedem dni grafiku z istniejącymi wyjątkami zamknięcia, poprzedni/następny tydzień, odświeżanie i pusty tydzień. Widok 390 × 844 mieści się bez poziomego przewijania; sprawdzono karty i otwarcie szczegółów klawiszem Enter. Otwarcie soboty i konflikt zmian konfiguracji pokrywają testy integracyjne; nie zmieniano w tym celu roboczych rezerwacji ani ustawień warsztatu.

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
personel widzi aktualny limit domyślny i zachowane wyjątki. Faktury i kwoty pozostają
etapem 3; ogólne problemy obsługi stanu frontendu — etapem 4.

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
interfejsu na 401 pozostaje osobnym punktem etapu 4.
