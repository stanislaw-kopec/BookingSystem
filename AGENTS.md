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
- Po angielsku nazywaj foldery, pliki, klasy, funkcje, zmienne, trasy frontendu,
  techniczne identyfikatory HTML/CSS, elementy bazy danych i komunikaty commitów.
  Interfejs oraz objaśnienia dla użytkownika mają być po polsku. Zachowuj kod
  napisany samodzielnie przez użytkownika.

## Uzgodnione wymagania funkcjonalne

Poniższy zakres opisuje docelowe zachowanie. Nie oznacza, że funkcje już istnieją.

| ID | Wymaganie |
| --- | --- |
| R01 | Warsztat nazywa się „Mietek Customs”; logo zawiera napis oraz gumową kaczkę w czapeczce w stylu hot roda i jest wyświetlane w nagłówku oraz sekcji o warsztacie. Publiczna strona startowa zawiera kolejno sekcje: o warsztacie (opis i historia), nasze usługi pogrupowane w kategorie oraz lokalizacja warsztatu z mapą Google. Oferta jest dostępna także bez logowania. Linki „Usługi” i „Lokalizacja” prowadzą do odpowiednich sekcji. Do czasu podania rzeczywistego adresu mapa pokazuje oznaczony jako demonstracyjny punkt we Wrocławiu. Historia warsztatu jest fikcyjnym tekstem w trzech akapitach, przygotowanym na prośbę użytkownika. |
| R02 | Na górze strony znajduje się menu z publiczną pozycją „Umów wizytę”, prowadzącą do `/appointments`. Po prawej stronie jest jeden wspólny przycisk „Logowanie / Rejestracja”, prowadzący do obu możliwości. Nie dodawaj dwóch osobnych przycisków w nagłówku. |
| R03 | Klient może zalogować się, zarejestrować i zarządzać wyłącznie własnym profilem na osobnej podstronie `/profile`. Profil zawiera imię, nazwisko, telefon, kontaktowy e-mail i adres oraz opcjonalne dane firmy: nazwę, NIP i adres rozliczeniowy. Po zalogowaniu klient otwiera profil z menu konta w prawym górnym rogu. Zapisany profil domyślnie pokazuje podsumowanie; formularz pojawia się po wybraniu edycji. |
| R04 | Pozycja „Moje pojazdy” w menu konta prowadzi do `/vehicles`. Klient może dodać pojazd z marką, modelem, rokiem produkcji, numerem rejestracyjnym i opcjonalnym VIN oraz przeglądać wyłącznie własne pojazdy. Wybranie pojazdu otwiera osobną podstronę `/vehicles/:vehicleId` ze szczegółami i historią napraw. |
| R05 | Historia napraw pojazdu powstaje na podstawie faktur wystawianych przez uprawnionego mechanika/pracownika i pozostaje powiązana z danym pojazdem. |
| R06 | Publiczna podstrona `/appointments` udostępnia kalendarz wolnych terminów od poniedziałku do piątku. Zalogowany klient wybiera własny pojazd lub dodaje go w formularzu, wybiera termin i opisuje usterkę. Gość podaje dane pojazdu, imię i nazwisko, co najmniej telefon albo e-mail, termin oraz opis; zgłoszenie gościa nie tworzy konta ani pojazdu w katalogu klienta. |
| R07 | Wysłane zgłoszenie ma status `PENDING` i oczekuje na decyzję personelu. Personel może je potwierdzić, odrzucić albo zaproponować inny termin. Zalogowany klient widzi własne zgłoszenia i potwierdza zaproponowany termin na osobnej podstronie `/my-appointments` („Moje wizyty”). Górne menu klienta zawiera osobne pozycje „Umów wizytę” i „Moje wizyty”, a link w menu konta także prowadzi do listy wizyt. Gość nie ma panelu ani publicznego podglądu statusu; warsztat kontaktuje się z nim telefonicznie lub mailowo. |
| R08 | Mechanik i administrator mają panel `/staff/appointments` do obsługi zgłoszeń klientów oraz gości: potwierdzania, odrzucania i proponowania innego wolnego terminu. Dla gościa personel może potwierdzić propozycję po uzgodnieniu jej poza aplikacją. Panel zleceń napraw i pozostałe zarządzanie wizytami pozostają dalszym etapem. |
| R09 | Mechanik i administrator mogą dodawać, edytować i usuwać kategorie oferty oraz przypisane do nich usługi. Przykładowe kategorie to elektryka, wulkanizacja i mechanika. Każda usługa należy do jednej kategorii i może zostać przeniesiona do innej. |

## Reguły biznesowe i granice dostępu

- Podstawowy przepływ zgłoszenia: `PENDING` → `CONFIRMED` albo `REJECTED`.
  Personel może też przejść z `PENDING` do `TIME_PROPOSED`; zalogowany klient
  potwierdza propozycję, a w przypadku gościa personel potwierdza ją po kontakcie.
  Wysłanie formularza nie jest automatycznym potwierdzeniem wizyty.
- Klient korzysta z własnego profilu, pojazdów, zgłoszeń i dokumentów.
  Personel korzysta z danych w zakresie przyznanych uprawnień.
- Identyfikator właściciela profilu wynika z zalogowanej sesji. API klienta nie
  przyjmuje identyfikatora użytkownika, którego profil ma zostać odczytany lub zapisany.
- Właściciel pojazdu również wynika z sesji. Lista i szczegóły używają zapytań
  ograniczonych do tego właściciela, a próba odczytu cudzego pojazdu zwraca 404.
  Numer rejestracyjny i podany VIN są unikalne wśród pojazdów jednego klienta,
  bez rozróżniania wielkości liter. Numer rejestracyjny zapisuj bez spacji i wielkimi literami.
- Dane firmy są opcjonalne jako całość. Po włączeniu tej części wymagane są nazwa firmy,
  NIP, ulica i numer, kod pocztowy oraz miejscowość adresu rozliczeniowego.
- Rezerwacja musi odnosić się do pojazdu należącego do klienta składającego zgłoszenie.
- Zalogowany klient musi mieć uzupełniony profil przed wysłaniem zgłoszenia. Dane
  kontaktowe i pojazdu są kopiowane do zgłoszenia, aby późniejsza edycja profilu
  lub pojazdu nie zmieniła danych, na podstawie których warsztat podjął decyzję.
- Zgłoszenie gościa przechowuje kopię danych kontaktowych i pojazdu. Nie tworzy
  rekordu w `app_users`, `client_profiles` ani `vehicles`.
- Kalendarz pokazuje dostępność, ale backend ponownie sprawdza termin przy wysłaniu
  zgłoszenia i proponowaniu nowej godziny. Nie dopuszczaj do więcej niż jednego
  aktywnego zgłoszenia w tym samym oknie, także przy równoczesnych żądaniach.
- Pierwsza wersja kalendarza używa strefy `Europe/Warsaw`, dni od poniedziałku
  do piątku, godzin 08:00–16:00, jednogodzinnych terminów i horyzontu 30 dni.
  Warsztat ma w tym modelu jeden wspólny zasób. Statusy `PENDING`, `TIME_PROPOSED`
  i `CONFIRMED` blokują bieżący termin; `REJECTED` go zwalnia. Zmiana terminu
  zwalnia poprzedni i zajmuje nowy atomowo. Baza zabezpiecza równoległe próby zajęcia.
- Historia napraw opisuje wykonane prace udokumentowane fakturą. Sam opis usterki
  lub przyjęcie rezerwacji nie stanowi wpisu potwierdzającego wykonanie naprawy.
- Katalog ma dwa poziomy: kategoria → usługa. Nie dodawaj kolejnych poziomów
  podkategorii bez nowego wymagania. Nazwa kategorii jest unikalna, a nazwa usługi
  unikalna w jej kategorii, bez rozróżniania wielkości liter. Nie usuwaj kategorii
  zawierającej usługi. Ceny, czas usług i ich powiązanie z rezerwacjami są poza obecnym etapem.

## Kwestie otwarte

Nie zapisuj poniższych decyzji jako uzgodnionych, dopóki nie wynikają z rozmowy.
Doprecyzowuj je przy etapie, którego dotyczą; nie blokują pozostałych prac.

- Rzeczywiste godziny pracy, święta i dni zamknięcia. Obecne godziny i strefa są
  ustawieniem pierwszej wersji, dopóki użytkownik nie poda danych warsztatu.
- Wiele stanowisk, różne długości usług, przypisywanie mechanika oraz odpowiedź,
  czy termin oznacza przyjęcie samochodu, czy czas całej naprawy.
- Automatyczne wygasanie blokady zgłoszenia oczekującego, anulowanie i przekładanie
  potwierdzonych wizyt oraz odrzucenie propozycji terminu przez klienta.
- Powiadomienia e-mail/SMS i zabezpieczenie publicznego formularza przed spamem.
- Podział pozostałych uprawnień administratora, pracownika i mechanika: harmonogram,
  konta oraz faktury. Edycję katalogu usług już przyznano rolom MECHANIC i ADMIN.
- Sposób edycji, usunięcia lub sprzedaży pojazdu oraz dostępu nowego właściciela
  do wcześniejszych dokumentów. Dalsze rozszerzenia profilu, np. kraj lub osobny
  adres rozliczeniowy osoby prywatnej, wymagają nowego ustalenia.
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
  zalogowany z rolą CLIENT. Klient może utworzyć i aktualizować własny profil,
  dodawać własne pojazdy oraz otwierać ich szczegóły. Widok historii napraw jest gotowy,
  ale pozostaje pusty do czasu wdrożenia faktur. Działają publiczna dostępność,
  zgłoszenia wizyt klienta i gościa, panel własnych zgłoszeń oraz decyzje MECHANIC/ADMIN
  z proponowaniem nowego terminu. Zlecenia napraw i faktury pozostają do zbudowania.
- Twórz pakiety i katalogi przy wdrażaniu funkcji. Unikaj pustych szkieletów całego
  systemu, mikroserwisów oraz nowych narzędzi bez konkretnej potrzeby.

## Kolejność rozwoju i jakość

- Zrealizowane etapy: katalog usług, rejestracja, profil klienta, jego pojazdy oraz
  zgłoszenia wizyt z kalendarzem i decyzją personelu. Kolejne etapy obejmują zlecenia
  napraw, faktury z danymi historii napraw i dalsze reguły harmonogramu.
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
