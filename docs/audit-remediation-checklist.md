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

- [ ] Walidacja obniżenia limitu i usunięcia wyjątku, gdy istnieją rezerwacje.
- [ ] Wspólna synchronizacja zmian konfiguracji i rezerwacji, również przy równoczesnych żądaniach.
- [ ] Grafik uwzględnia weekendy otwarte wyjątkiem, dni zamknięte i pojemność konkretnego dnia.
- [ ] Endpoint po zakresie dat z lekkim DTO, zamiast pobierania całej historii.
- [ ] Testy wyjątków i wyścigów; sprawdzenie zapytań SQL i potrzebnych indeksów.

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
