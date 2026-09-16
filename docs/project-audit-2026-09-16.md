# Audyt projektu Mietek Customs — 16.09.2026

> Raport opisuje stan w momencie audytu. Aktualne postępy napraw zapisujemy
> w [checkliście poprawek](audit-remediation-checklist.md).

## Ocena pod portfolio Junior / Mid Java Developer

Projekt ma wystarczający zakres funkcjonalny, aby stanowić główny projekt w CV na
Junior Java Developera. Pokazuje proces biznesowy od rezerwacji do odbioru auta,
uprawnienia, relacyjną bazę danych, transakcje i integracyjne testy. Przed prezentacją
warto skoncentrować się na poprawności istniejących funkcji i dowodach jakości.

Na poziomie Mid projekt może być materiałem do rozmowy o decyzjach technicznych,
ale liczba modułów nie potwierdza samodzielnie tego poziomu. Szczególnie istotna
będzie umiejętność wyjaśnienia bezpieczeństwa sesji, współbieżności, zapytań SQL,
testowania przypadków brzegowych oraz uruchamiania i diagnozowania aplikacji.

Poniższa ocena dotyczy repozytorium, nie poziomu wiedzy autora. Nie sprawdzałem
samodzielności implementacji ani umiejętności podczas rozmowy technicznej.

## Zakres i wyniki weryfikacji

Przejrzano backend, frontend, migracje, testy, konfigurację bezpieczeństwa,
Docker Compose, Dockerfile, workflow CI i dokumentację. Audyt nie zmienia
implementacji; ten raport jest jego jedynym nowym plikiem w repozytorium.

| Sprawdzenie | Wynik |
| --- | --- |
| Backend: `mvn -B -ntp test` | 81 testów, 0 błędów, 0 niepowodzeń, 0 pominięć |
| Baza testowa | Osobny PostgreSQL 17 uruchomiony przez Testcontainers |
| Frontend: `npm run lint` | Sukces |
| Frontend: `npm run build` | Sukces |
| `docker compose config --quiet` | Sukces; środowisko audytu zgłosiło ostrzeżenie o dostępie do lokalnej konfiguracji Dockera |

Testy backendu uruchomiono przez zainstalowany Maven. Próba użycia Windowsowego
wrappera zakończyła się błędem uruchomienia w środowisku audytu; nie kwalifikuję
tego jako potwierdzonego błędu projektu. Dostęp do zależności i Dockera wymagał
uruchomienia testów poza ograniczeniami piaskownicy.

Opisane niżej scenariusze błędów wynikają z analizy kodu. Nie odtwarzałem ich na
danych działającej aplikacji. Zielone testy potwierdzają istniejące scenariusze,
a nie brak problemów w scenariuszach nieobjętych testami. Nie wykonywałem pełnego
E2E w przeglądarce, pomiarów obciążeniowych, skanowania CVE ani pentestu. Przejrzano
konfigurację GitHub Actions, ale nie weryfikowano ostatniego zdalnego wykonania CI.

## Mocne strony

- Monolit podzielony według funkcji, z oddzielonymi kontrolerami, serwisami, DTO i repozytoriami.
- Spring Security z sesją, BCrypt, CSRF oraz uprawnieniami wymuszanymi przez backend.
- Zapytania klienta ograniczane do właściciela i 404 przy próbie dostępu do cudzego pojazdu.
- Flyway, ograniczenia bazy, `ddl-auto=validate` i wyłączone Open Session in View.
- Rzeczywiste blokowanie równoczesnych rezerwacji oraz test pięciu prób na cztery miejsca.
- Backendowa paginacja, ograniczenie rozmiaru strony i dodatkowy klucz stabilizujący sortowanie.
- Kopie danych kontaktowych i pojazdu w zgłoszeniu; późniejsza edycja pojazdu nie przepisuje zgłoszenia.
- Kwoty jako `BigDecimal`, obliczane przez backend.
- Testcontainers z PostgreSQL, CI, Swagger i lokalne dane demonstracyjne.
- Frontend pogrupowany według funkcji, walidowanie odpowiedzi HTTP, wspólny klient API/CSRF i anulowanie żądań.

## Ustalenia wymagające naprawy

P1 oznacza wysoki priorytet związany z bezpieczeństwem. P2 oznacza błąd poprawności
lub istotne ograniczenie działania. Kolejność niżej uwzględnia skutki dla projektu.

### A01 — P1: zmiana loginu może skierować istniejącą sesję do innego konta

**Kod:** `backend/src/main/java/pl/autoserwis/security/DatabaseUserDetailsService.java:19`,
`user/AdminAccountService.java:100`, `vehicle/VehicleService.java:144`.
Dwa ostatnie skrócone adresy są względem `backend/src/main/java/pl/autoserwis/`.

Principal przechowuje login bez niezmiennego identyfikatora użytkownika. Admin może
zmienić login, a serwisy później ponownie wyszukują właściciela według loginu sesji.

Scenariusz: A loguje się jako `anna`; admin zmienia login na `anna2`; B rejestruje
zwolniony login `anna`. Nadal aktywna sesja A wskazuje teraz dane B. Zanim login
zostanie ponownie użyty, operacje A mogą kończyć się 404.

**Poprawka:** własny principal z trwałym `userId`; wszystkie operacje na własnych
danych powinny korzystać z tego ID. Ustalić obsługę aktualizacji danych sesji.
**Test:** prawdziwe logowanie, zachowanie sesji, zmiana loginu, ponowne użycie starego
loginu i potwierdzenie, że sesja nigdy nie uzyska dostępu do danych innego konta.

### A02 — P1: dezaktywacja konta nie kończy jego istniejących sesji

**Kod:** `backend/src/main/java/pl/autoserwis/user/AdminAccountService.java:130`,
`backend/src/main/java/pl/autoserwis/security/DatabaseUserDetailsService.java:21`.

Status `enabled` jest sprawdzany przy logowaniu. Dezaktywacja aktualizuje bazę,
ale już zalogowany mechanik zachowuje sesję i uprawnienia. Reset hasła również
nie unieważnia dotychczasowych sesji.

**Poprawka:** unieważniać sesje albo weryfikować aktywność/wersję bezpieczeństwa
konta przy kolejnych żądaniach. Ustalić politykę resetu i samodzielnej zmiany hasła.
**Test:** logowanie mechanika → dezaktywacja przez admina → odmowa kolejnego
żądania w starej sesji. Oddzielnie sprawdzić reset hasła z drugą aktywną sesją.

### A03 — P2: konfiguracja grafiku może naruszyć limit istniejących rezerwacji

**Kod:** `backend/src/main/java/pl/autoserwis/appointment/WorkshopScheduleConfigService.java:55`,
`:79` i `:92`.

Obniżenie limitu domyślnego nie sprawdza zajętości. Usunięcie wyjątku również nie
sprawdza wynikowej dostępności: dzień z sześcioma wizytami może wrócić do limitu
czterech, a otwarta sobota z wizytami może stać się zamknięta.

Zapis wyjątku sprawdza zajętość, ale nie uczestniczy w blokadzie dnia stosowanej
przez rezerwacje. Równoległa rezerwacja i zmniejszenie limitu mogą zatem przejść
na podstawie nieaktualnego stanu.

**Poprawka:** wspólna polityka zmian dostępności i synchronizacja z rezerwacjami,
obejmująca zapis ustawień, zapis wyjątku i jego usunięcie. Odczyt konfiguracji
musi odbywać się we właściwym momencie transakcji.
**Testy:** obniżenie limitu poniżej zajętości, usunięcie zajętego wyjątku oraz
równoczesna rezerwacja i zamknięcie dnia.

### A04 — P2: grafik mechanika pomija weekendy i indywidualną pojemność dni

**Kod:** `frontend/src/features/appointments/components/StaffScheduleSection.tsx:74`,
`:102` i `:163`.

Widok generuje wyłącznie poniedziałek–piątek. Jeśli administrator otworzy sobotę
wyjątkiem, klient może zarezerwować wizytę, której mechanik nie zobaczy w grafiku.
Licznik zawsze używa globalnego limitu: przy wyjątku jednego miejsca i jednej
wizycie może pokazywać trzy wolne miejsca. Zamknięty dzień też wygląda na dostępny.

**Poprawka:** pobierać konfigurację każdej daty i uwzględnić dni otwarte wyjątkowo
oraz dni mające istniejące wizyty. Można połączyć tę pracę z A07.
**Test:** otwarta sobota, zamknięty dzień roboczy i limit różny od domyślnego.

### A05 — P2: ta sama faktura zmienia się po edycji profilu i upływie czasu

**Kod:** `backend/src/main/java/pl/autoserwis/vehicle/VehicleService.java:77`,
`backend/src/main/java/pl/autoserwis/appointment/AppointmentService.java:128`,
`backend/src/main/java/pl/autoserwis/invoice/InvoicePdfGenerator.java:71` i `:94`.

Generator odczytuje aktualny profil i bieżący dzień. Zmiana nazwiska, adresu lub
danych firmy zmienia nabywcę przy ponownym pobraniu dokumentu o tym samym numerze.
Pobranie następnego dnia zmienia datę wystawienia. Jest to sprzeczne również
z wymaganiem utrwalania danych wystawionego dokumentu w `backend/AGENTS.md`.

**Poprawka:** zapisać niezmienną kopię danych dokumentu w ustalonym momencie
wystawienia: nabywca, sprzedawca, data, numer, pozycje i kwoty. Nie wymaga to
wdrażania pełnej księgowości.
**Test:** treść ponownie pobranego dokumentu po zmianie profilu i daty zachowuje
te same dane biznesowe. Nie trzeba porównywać całych bajtów PDF.

### A06 — P2: obliczenia dokumentu mają niespójne zaokrąglenia i limity

**Kod:** `backend/src/main/java/pl/autoserwis/invoice/InvoicePdfGenerator.java:156`
i `:169`; `backend/src/main/java/pl/autoserwis/appointment/AppointmentRepairItem.java:50`;
`backend/src/main/java/pl/autoserwis/appointment/dto/RepairItemRequest.java:24`.

Netto wierszy jest zaokrąglane osobno, a podsumowanie ponownie liczone od sumy
brutto. Przykład dla stosowanego w kodzie przelicznika 1,23: dwie pozycje po
1,00 zł brutto mają po 0,81 zł netto, czyli łącznie 1,62 zł, podczas gdy
podsumowanie pokazuje 1,63 zł.

Ponadto dodatnia ilość i cena nie gwarantują poprawnej wartości pozycji:
0,01 × 0,01 po zaokrągleniu daje 0,00 i narusza ograniczenie bazy. Iloczyn albo
suma poprawnych pól wejściowych mogą też przekroczyć `NUMERIC(10,2)`. Zamiast
czytelnej walidacji operacja kończy się błędem zapisu.

**Poprawka:** wspólny kalkulator kwot i jawne reguły zaokrągleń; walidacja wartości
wynikowych pozycji i całej naprawy przed zapisem.
**Testy:** różnice groszowe, ułamkowa ilość, wynik zaokrąglony do zera, maksymalne
kwoty i suma przekraczająca limit. To ocena spójności obliczeń, nie audyt podatkowy.

### A07 — P2: otwarcie tygodnia grafiku pobiera całą historię wizyt

**Kod:** `backend/src/main/java/pl/autoserwis/appointment/AppointmentService.java:82`
i `:424`; `frontend/src/features/appointments/api/appointmentsApi.ts:229`.

Endpoint `/api/staff/appointments/all` pobiera wszystkie zgłoszenia. Dopiero
frontend wybiera tydzień i statusy. Mapowanie odpowiedzi odczytuje leniwą kolekcję
pozycji napraw bez zaplanowanego zbiorczego pobierania, co prowadzi do dodatkowych
zapytań na wizytę. Koszt widoku rośnie wraz z historią całego warsztatu.

**Poprawka:** endpoint po zakresie dat, odpowiednich statusach i z lekkim DTO
potrzebnym do grafiku. Sprawdzić liczbę SQL i plan zapytania na większym zestawie
danych; nie dodawać indeksów ani cache bez pomiaru. Paginacja listy zgłoszeń
nie rozwiązuje problemu oddzielnego endpointu grafiku.

### A08 — P2: ponowny wybór aktualnego filtra pozostawia stan ładowania

**Kod:** `frontend/src/features/appointments/components/ClientAppointmentsSection.tsx:87`,
`frontend/src/features/appointments/components/StaffAppointmentsSection.tsx:106`,
`frontend/src/components/ui/CustomSelect.tsx:80`.

Na pierwszej stronie listy ponowne wybranie tego samego statusu lub sortowania
ustawia `isLoading=true`, ale nie zmienia zależności efektu pobierającego dane.
Nie ma żądania, które później wyłączy ładowanie.

**Poprawka i test:** ignorować wybór niezmienionej wartości; sprawdzić ponowne
wybranie „Wszystkie statusy” oraz aktualnego sortowania w obu panelach.

### A09 — P2: błąd odświeżenia listy jest ukryty, pozostają stare wyniki

**Kod:** `frontend/src/features/appointments/components/ClientAppointmentsSection.tsx:163`,
`frontend/src/features/appointments/components/StaffAppointmentsSection.tsx:266`.

Błąd jest wyświetlany tylko przed pierwszym udanym pobraniem. Jeżeli użytkownik
zmieni filtr lub stronę, a żądanie się nie uda, nadal widzi stare dane pod nowymi
kontrolkami, bez informacji o błędzie.

**Poprawka:** jawny błąd odświeżenia i ponowienie; nie przedstawiać starych danych
jako wyniku nowych parametrów.
**Test:** udane pobranie → zmiana filtra → błąd API → widoczny komunikat i spójny widok.

### A10 — P2: frontend nie aktualizuje sesji po utracie uwierzytelnienia

**Kod:** `frontend/src/features/auth/AuthProvider.tsx:13`, `frontend/src/api/apiClient.ts:63`.

Po wygaśnięciu sesji lub wylogowaniu w innej karcie UI może nadal pokazywać konto
i wcześniej pobrane prywatne dane. Kolejne operacje zawodzą, ale formularz
logowania nie wraca automatycznie. To problem stanu UI, a nie obejście ochrony API.

**Poprawka:** centralna obsługa utraty sesji, wyczyszczenie prywatnych widoków,
możliwość ponownego logowania; rozważyć odświeżanie sesji po powrocie do karty.
**Test:** odpowiedź 401 oraz wylogowanie w drugiej karcie.

### A11 — P2/P3: komunikaty walidacji i dostępność list wymagają dopracowania

**Kod:** `frontend/src/features/profile/components/ClientProfileSection.tsx:64`,
`frontend/src/features/appointments/components/StaffAppointmentsSection.tsx:468`,
`frontend/src/components/ui/CustomSelect.tsx:135`.

- Część formularzy wyświetla angielskie `fieldErrors` bezpośrednio z API, np. błędny telefon w profilu.
- Formularz naprawy nie obsługuje indywidualnych kluczy typu `repairItems[0].quantity`.
- W `CustomSelect` strzałki zmieniają wyłącznie wizualnie aktywną opcję; brakuje powiązania aktywnej opcji z fokusem/ARIA i przewijania jej do widoku.

**Poprawka:** mapowanie błędów pól na polskie komunikaty bez przywracania i18n,
wskazywanie konkretnej pozycji naprawy oraz pełna obsługa fokusu i klawiatury
we wspólnym komponencie listy. Testować także długą listę i nawigację bez myszy.

## Testy, które przyniosą największy zysk

Obecne 81 testów stanowi wartościową podstawę. Istotniejsze od zwiększania liczby
jest objęcie nimi scenariuszy, które aktualnie przepuszczają błędy.

1. Prawdziwa sesja HTTP po zmianie loginu, dezaktywacji i resecie hasła. Samo `.with(user(...))` nie sprawdza cyklu życia logowania.
2. Zmiana konfiguracji grafiku równolegle z rezerwacją; także usuwanie wyjątków i redukcja limitu.
3. Kalkulacja kwot i treść PDF. Obecne sprawdzenie `%PDF` potwierdza format, ale nie poprawność dokumentu.
4. Kilka testów zachowania UI: filtry, awaria odświeżenia, sesja, wyjątki grafiku i klawiatura listy.
5. Jeden E2E: klient rezerwuje → mechanik potwierdza → zapisuje naprawę → oznacza odbiór → klient pobiera fakturę.

Frontend nie ma aktualnie automatycznych testów zachowania. Lint i kompilacja
TypeScript nie zastępują takich testów. Wstrzyknięcie `Clock` do reguł czasu
ułatwiłoby deterministyczne testy dat i niezmienności dokumentów.

## Uruchamianie, utrzymanie i prezentacja

- **Oddzielny wariant wdrożeniowy:** obecny Docker frontendu uruchamia Vite dev server. Jest odpowiedni do pracy lokalnej, ale demonstracja publiczna powinna serwować statyczny build i korzystać z konfiguracji przeznaczonej do wdrożenia.
- **Konfiguracja publicznego demo:** oddzielić ją od profilu `local`, jawnie ustawić sekrety i HTTPS/cookies oraz ograniczyć nadużycia logowania i publicznych rezerwacji. Hasła demonstracyjne i porty przypięte do localhost nie są same w sobie błędem lokalnego Compose.
- **Diagnostyka:** przydatne byłyby identyfikatory żądań i logowanie nieoczekiwanych błędów oraz ważnych operacji administracyjnych, bez haseł i zbędnych danych osobowych. Nie trzeba od razu stawiać rozbudowanego stosu monitoringu.
- **CI:** obecny workflow jest sensowny. Po dodaniu testów UI uruchamiać je w CI; publikować raporty błędów testów jako artefakty. Składnia Compose nie dowodzi poprawnego uruchomienia kontenerów.
- **README:** zastąpić cztery placeholdery rzeczywistymi screenami. Krótki film z pełnym scenariuszem może być alternatywą dla publicznego wdrożenia. Dodać widoczny wynik CI i krótkie znane ograniczenia.
- **Dokumentacja:** README deklaruje JUnit 5, natomiast uruchomiony zestaw korzysta z `junit-jupiter-api-6.0.3`. Uaktualnić wersję oraz roadmapę, której końcowa lista nadal wymienia jako przyszłe kroki już wykonane funkcje.
- **Czytelność kodu:** po poprawkach wydzielić formularz naprawy i akcje z dużego `StaffAppointmentsSection`, ujednolicić pobieranie PDF. Nie przebudowywać całej architektury.
- **Zasoby:** logo PNG waży około 1,45 MB; warto przygotować mniejszy plik do wyświetlania na stronie. Nie jest to priorytet przed bezpieczeństwem i poprawnością danych.

## Odniesienie do ofert pracy

Sprawdzono przykładowe ogłoszenia pracodawców dostępne 16.09.2026. Jest to mała
próbka, nie statystyczny opis rynku i nie gwarancja wyniku rekrutacji.

- [Futurum Technology — Junior Java Developer](https://jobs.smartrecruiters.com/FuturumTechnologyLtd/744000112652733-junior-java-developer): Java, Spring, rozwój i integracja komponentów, dokumentacja; React/Angular jako dodatkowy atut. Projekt daje materiał do pokazania tych umiejętności.
- [Sportradar — Mid Java Software Engineer](https://jobs.smartrecruiters.com/Sportradar/744000131440684-mid-java-software-engineer): doświadczenie zawodowe i dostarczanie oprogramowania produkcyjnego, utrzymywalny i testowany kod oraz współpraca. Technologie takie jak Kafka, Docker czy Kubernetes występują jako atuty, a nie zamiennik tych podstaw.

Wniosek dla tego repozytorium: naprawy, testy regresji i umiejętność obrony decyzji
dadzą więcej niż dokładanie narzędzi do listy technologii. Warto przygotować się
do wyjaśnienia transakcji i blokad PostgreSQL, sesji i CSRF, granic dostępu,
planów zapytań, zaokrąglania pieniędzy i kompromisów zastosowanego modelu domeny.

## Proponowana kolejność domknięcia

- [ ] Etap 1: A01–A02 — trwała tożsamość użytkownika, sesje i testy bezpieczeństwa.
- [ ] Etap 2: A03–A04 i A07 — spójny grafik, wyjątki, współbieżność i pobieranie po zakresie dat.
- [ ] Etap 3: A05–A06 — niezmienne dane dokumentów, kwoty i testy treści PDF.
- [ ] Etap 4: A08–A11 — stany UI, walidacja, sesja frontendu, dostępność i mały zestaw testów.
- [ ] Etap 5: scenariusz E2E, screeny, aktualna dokumentacja i powtarzalna prezentacja.
- [ ] Opcjonalnie: publiczne demo w osobnej konfiguracji wdrożeniowej.

Przyjęte kryterium zakończenia: znane istotne błędy naprawione z testami regresji,
cały scenariusz klient–warsztat działa, CI jest zielone, a obca osoba potrafi
uruchomić i zrozumieć aplikację na podstawie README.

SMS, e-maile, płatności online, pełna księgowość, mikroserwisy, Kafka, Kubernetes
oraz zamiana sesji na JWT nie są potrzebne do domknięcia obecnego zakresu.
Brak odzyskiwania hasła e-mailem wynika z uzgodnionej obsługi przez warsztat
i nie został potraktowany jako usterka.
