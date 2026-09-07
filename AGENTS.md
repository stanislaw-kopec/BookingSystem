# BookingSystem — cele i zasady projektu

## Zakres instrukcji

Ten plik dotyczy całego repozytorium. Przed zmianami w backendzie przeczytaj
`backend/AGENTS.md`, a przed zmianami we frontendzie `frontend/AGENTS.md`.
Pliki z podkatalogów uzupełniają wspólne zasady o wymagania swojej części aplikacji.

## Cel i sposób współpracy

- Budujemy system obsługi warsztatu samochodowego jako projekt portfolio pomagający
  użytkownikowi przygotować się do pierwszej pracy jako Java Developer.
- Użytkownik uczy się Springa oraz Reacta z TypeScriptem. Wyjaśniaj po polsku cel
  zmian, przepływ danych i istotne decyzje, tak aby potrafił sam opowiedzieć o kodzie.
- Przy prośbie o naukę prowadź przez małe przykłady i ćwiczenia. Przy zleceniu
  implementacji wykonuj uzgodniony zakres i objaśniaj wynik.
- Rozwijaj projekt małymi, działającymi etapami. Rutynowe decyzje techniczne
  podejmuj samodzielnie; nie rozszerzaj bieżącego zadania na wszystkie opisane funkcje.
- Nazwy w kodzie i komunikaty commitów pisz po angielsku. Interfejs i objaśnienia
  dla użytkownika mają być po polsku. Zachowuj kod napisany samodzielnie przez użytkownika.

## Uzgodnione wymagania funkcjonalne

Poniższy zakres opisuje docelowe zachowanie. Nie oznacza, że funkcje już istnieją.

| ID | Wymaganie |
| --- | --- |
| R01 | Publiczna strona startowa przedstawia podstawowe informacje o warsztacie, jego lokalizację i historię. Poniżej znajduje się oferta usług pogrupowanych w kategorie, dostępna także bez logowania. Link „Usługi” w menu prowadzi do tej sekcji strony. |
| R02 | Na górze strony znajduje się menu. Po prawej stronie jest jeden wspólny przycisk „Logowanie / Rejestracja”, prowadzący do obu możliwości. Nie dodawaj dwóch osobnych przycisków w nagłówku. |
| R03 | Klient może zalogować się, zarejestrować i zarządzać własnym profilem: aktualnymi danymi kontaktowymi oraz danymi rozliczeniowymi, także firmy, jeśli ją posiada. |
| R04 | Zakładka „Moje pojazdy” umożliwia dodanie pojazdu i przeglądanie własnych pojazdów. Wybranie pojazdu otwiera jego szczegóły i historię napraw. |
| R05 | Historia napraw pojazdu powstaje na podstawie faktur wystawianych przez uprawnionego mechanika/pracownika i pozostaje powiązana z danym pojazdem. |
| R06 | Klient ma zakładkę rezerwacji z kalendarzem wolnych terminów od poniedziałku do piątku. Zgłoszenie zawiera wybrany termin, pojazd klienta i opis usterki. |
| R07 | Wysłane zgłoszenie oczekuje na decyzję personelu. Mechanik/pracownik może je przyjąć albo odrzucić; klient widzi aktualny status. |
| R08 | Personel warsztatu ma panel do przyjmowania zleceń od klientów, obsługi zgłoszeń i zarządzania wizytami. Docelowo występują uprawnienia pracownika/mechanika i administratora. |
| R09 | Mechanik i administrator mogą dodawać, edytować i usuwać kategorie oferty oraz przypisane do nich usługi. Przykładowe kategorie to elektryka, wulkanizacja i mechanika. Każda usługa należy do jednej kategorii i może zostać przeniesiona do innej. |

## Reguły biznesowe i granice dostępu

- Podstawowy przepływ zgłoszenia: „Oczekujące” → „Przyjęte” albo „Odrzucone”.
  Wysłanie formularza nie jest automatycznym potwierdzeniem wizyty.
- Klient korzysta z własnego profilu, pojazdów, zgłoszeń i dokumentów.
  Personel korzysta z danych w zakresie przyznanych uprawnień.
- Rezerwacja musi odnosić się do pojazdu należącego do klienta składającego zgłoszenie.
- Kalendarz pokazuje dostępność, ale backend ponownie sprawdza ją przy zapisie
  i przy przyjęciu zgłoszenia. Nie dopuszczaj do nakładających się potwierdzonych
  wizyt tego samego mechanika, także przy równoczesnych żądaniach.
- Dostępność wynika z harmonogramu warsztatu i jego zasobów. Sposób uwzględniania
  stanowisk i zgłoszeń oczekujących pozostaje do ustalenia.
- Historia napraw opisuje wykonane prace udokumentowane fakturą. Sam opis usterki
  lub przyjęcie rezerwacji nie stanowi wpisu potwierdzającego wykonanie naprawy.
- Katalog ma dwa poziomy: kategoria → usługa. Nie dodawaj kolejnych poziomów
  podkategorii bez nowego wymagania. Nazwa kategorii jest unikalna, a nazwa usługi
  unikalna w jej kategorii, bez rozróżniania wielkości liter. Nie usuwaj kategorii
  zawierającej usługi. Ceny, czas usług i ich powiązanie z rezerwacjami są poza obecnym etapem.

## Kwestie otwarte

Nie zapisuj poniższych decyzji jako uzgodnionych, dopóki nie wynikają z rozmowy.
Doprecyzowuj je przy etapie, którego dotyczą; nie blokują pozostałych prac.

- Rzeczywiste dane warsztatu, godziny pracy, strefa czasowa, święta i dni zamknięcia.
- Długość i odstępy terminów, liczba mechaników i stanowisk oraz sposób przypisywania
  mechanika do wizyty. Czy termin oznacza przyjęcie samochodu, czy czas całej naprawy?
- Czy oczekujące zgłoszenie blokuje termin i na jak długo; zasady anulowania,
  przekładania wizyt oraz kolejne stany obsługi naprawy.
- Podział pozostałych uprawnień administratora, pracownika i mechanika: harmonogram,
  konta oraz faktury. Edycję katalogu usług już przyznano rolom MECHANIC i ADMIN.
- Zakres pól profilu, danych firmy i pojazdu. Sposób obsługi sprzedaży lub usunięcia
  pojazdu oraz dostępu do wcześniejszych dokumentów.
- Zakres fakturowania: tworzenie dokumentu w aplikacji czy zapis dokumentu zewnętrznego,
  pozycje prac/części, dane rozliczenia, numeracja, korekty i ewentualny PDF.
  Nie zakładaj integracji księgowej ani płatności online.
- Odzyskiwanie haseł i zarządzanie kontami. Rejestracja klienta jest dostępna;
  obecne uwierzytelnianie używa sesji Spring Security i ochrony CSRF. Ewentualna zmiana mechanizmu
  uwierzytelniania wymaga konkretnej potrzeby, JWT nie jest wymaganiem.

## Architektura i stan techniczny

- Jedno repozytorium: backend Spring Boot w `backend`, React z TypeScriptem w `frontend`.
  Backend rozwijamy jako monolit z pakietami według funkcjonalności.
- Obecnie: Java 25, Spring Boot 4.1.1, Maven, JPA/Hibernate, Flyway, Bean Validation,
  PostgreSQL 17, React, TypeScript i Vite. Dokładne wersje sprawdzaj w manifestach.
- Docker Compose uruchamia trzy usługi: `postgres`, `backend` i `frontend`.
  Zachowaj istniejący wolumen bazy i możliwość uruchamiania aplikacji lokalnie z IDE.
- API używa prefiksu `/api`; `/api/health` służy do sprawdzania gotowości backendu.
  Frontend korzysta z proxy Vite. Szczegóły uruchomienia zawiera `README.md`.
- Działają katalog kategorii i usług w PostgreSQL, publiczna strona oraz panel
  edycji dla MECHANIC/ADMIN. Logowanie korzysta z sesji, haseł BCrypt i CSRF.
  Profil `local` tworzy konta demonstracyjne opisane w README. Klient może utworzyć
  konto z unikalnym loginem i e-mailem, a po rejestracji zostaje automatycznie
  zalogowany z rolą CLIENT. Profile, pojazdy, rezerwacje, zlecenia i faktury pozostają do zbudowania.
- Twórz pakiety i katalogi przy wdrażaniu funkcji. Unikaj pustych szkieletów całego
  systemu, mikroserwisów oraz nowych narzędzi bez konkretnej potrzeby.

## Kolejność rozwoju i jakość

- Zrealizowany etap: usługi warsztatu w PostgreSQL → `GET /api/services` → lista
  na stronie startowej oraz edycja przez personel. Kolejne etapy obejmują konta i profile, pojazdy,
  dostępność i zgłoszenia, panel personelu, zlecenia oraz faktury i historię napraw.
- Dodawaj potrzebne testy wraz z funkcją. Priorytety to reguły rezerwacji,
  współbieżność, uprawnienia do cudzych danych i poprawne powiązania dokumentów.
- Cele jakościowe portfolio: czytelne REST API i DTO, migracje bazy, walidacja,
  spójna obsługa błędów, testy jednostkowe i integracyjne, dokumentacja oraz CI.
  Testcontainers jest skonfigurowany; OpenAPI i GitHub Actions wdrażaj w etapach,
  które ich potrzebują.
- Dobieraj sprawdzenia do zmian. Dla samych instrukcji wystarcza przegląd treści
  i różnic; nie uruchamiaj całego środowiska ani nie dodawaj testów dokumentacji.
- Po implementacji opisz, co działa, jak to sprawdzono i co pozostaje niegotowe.
  Nie utożsamiaj poprawnego budowania z zaliczeniem testów.
- Zachowuj pracę użytkownika i nie twórz commitów bez jego polecenia.
  Proponowane komunikaty commitów mają być zgodne z Conventional Commits.

## Aktualizowanie wymagań

- Wymagania można rozszerzać i zmieniać. Nowe ustalenia użytkownika aktualizuj
  w odpowiednim pliku `AGENTS.md`, zachowując identyfikatory istniejących wymagań.
- Oddzielaj potwierdzone wymagania, propozycje i kwestie otwarte. Po rozstrzygnięciu
  kwestii otwartej przenieś decyzję do właściwych zasad i usuń sprzeczne zapisy.
- Zmiana wymagań opisuje nowy cel; sama aktualizacja dokumentacji nie oznacza
  zlecenia natychmiastowej implementacji całego nowego zakresu.

## Podstawowe polecenia

Z głównego folderu: `docker compose config --quiet` sprawdza konfigurację,
`docker compose up --build -d --wait` uruchamia całość, a `docker compose ps`
pokazuje stan usług. Pełna instrukcja, adresy i sposób zatrzymywania są w `README.md`.
