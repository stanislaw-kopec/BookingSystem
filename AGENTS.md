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
  Objaśnienia dla użytkownika mają być po polsku. Interfejs domyślnie działa po
  polsku, ale docelowo ma obsługiwać polski i angielski przez warstwę tłumaczeń,
  bez rozrzucania tekstów UI bezpośrednio po komponentach. Zachowuj kod napisany
  samodzielnie przez użytkownika.

## Uzgodnione wymagania funkcjonalne

Poniższy zakres opisuje docelowe zachowanie. Nie oznacza, że funkcje już istnieją.

| ID | Wymaganie |
| --- | --- |
| R01 | Warsztat nazywa się „Mietek Customs”; logo zawiera napis oraz gumową kaczkę w czapeczce w stylu hot roda i jest wyświetlane w nagłówku oraz sekcji o warsztacie. Publiczna strona startowa zawiera kolejno sekcje: o warsztacie (opis i historia), nasze usługi pogrupowane w kategorie oraz lokalizacja warsztatu z mapą Google. Oferta jest dostępna także bez logowania. Linki „Usługi” i „Lokalizacja” prowadzą do odpowiednich sekcji. Do czasu podania rzeczywistego adresu mapa pokazuje oznaczony jako demonstracyjny punkt we Wrocławiu. Historia warsztatu jest fikcyjnym tekstem w trzech akapitach, przygotowanym na prośbę użytkownika. |
| R02 | Na górze strony znajduje się menu z publiczną pozycją „Umów wizytę”, prowadzącą do `/appointments`. Po prawej stronie jest jeden wspólny przycisk „Logowanie / Rejestracja”, prowadzący do obu możliwości. Nie dodawaj dwóch osobnych przycisków w nagłówku. |
| R03 | Klient może zalogować się, zarejestrować i zarządzać wyłącznie własnym profilem na osobnej podstronie `/profile`. Profil zawiera imię, nazwisko, telefon, kontaktowy e-mail i adres oraz opcjonalne dane firmy: nazwę, NIP i adres rozliczeniowy. Po zalogowaniu klient otwiera profil z menu konta w prawym górnym rogu. Zapisany profil domyślnie pokazuje podsumowanie; formularz pojawia się po wybraniu edycji. |
| R04 | Pozycja „Moje pojazdy” w menu konta prowadzi do `/vehicles`. Klient może dodać pojazd z marką, modelem, rokiem produkcji, numerem rejestracyjnym i opcjonalnym VIN oraz przeglądać wyłącznie własne pojazdy. Wybranie pojazdu otwiera osobną podstronę `/vehicles/:vehicleId` ze szczegółami i historią napraw. |
| R05 | Historia napraw pojazdu w pierwszej wersji powstaje na podstawie zakończonych zgłoszeń `COMPLETED`, a docelowo może zostać rozbudowana o faktury wystawiane przez uprawniony personel. |
| R06 | Publiczna podstrona `/appointments` udostępnia kalendarz wolnych dni przyjęcia auta wynikający z konfiguracji grafiku warsztatu. Zalogowany klient wybiera własny pojazd lub dodaje go w formularzu, wybiera dzień przyjęcia auta i opisuje usterkę. Gość podaje dane pojazdu, imię i nazwisko, co najmniej telefon albo e-mail, dzień przyjęcia auta oraz opis; zgłoszenie gościa nie tworzy konta ani pojazdu w katalogu klienta. Interfejs informuje, że auto można zostawić rano albo po wcześniejszym uzgodnieniu dzień wcześniej. |
| R07 | Wysłane zgłoszenie ma status `PENDING` i oczekuje na decyzję personelu. Personel może je potwierdzić, odrzucić albo zaproponować inny dzień. Zalogowany klient widzi własne zgłoszenia, filtruje je po statusie, sortuje po dacie przyjęcia auta, przechodzi między stronami listy, potwierdza zaproponowany dzień i może odwołać aktywną wizytę na osobnej podstronie `/my-appointments` („Moje wizyty”). Paginacja, filtrowanie i sortowanie własnych wizyt są obsługiwane po stronie backendu. Główne menu zawiera publiczny link „Umów wizytę”, a prywatne linki klienta, w tym „Moje wizyty”, znajdują się w menu konta w prawym górnym rogu. Gość nie ma panelu ani publicznego podglądu statusu; warsztat kontaktuje się z nim telefonicznie lub mailowo. |
| R08 | Mechanik i administrator mają graficzną zakładkę `/staff/schedule` („Grafik”) z tygodniowym widokiem aktywnych zgłoszeń pogrupowanych według dni przyjęcia auta. Kliknięcie zgłoszenia w grafiku prowadzi do `/staff/appointments/:appointmentId`, gdzie personel widzi pełne szczegóły zgłoszenia i historię zakończonych napraw powiązanego pojazdu, jeśli pojazd istnieje w kartotece klienta. Panel `/staff/appointments` służy do obsługi zgłoszeń klientów oraz gości: potwierdzania, odrzucania i proponowania innego wolnego dnia. Lista zgłoszeń personelu obsługuje po stronie backendu paginację, filtrowanie po statusie i sortowanie po dacie przyjęcia auta. Dla gościa personel może potwierdzić propozycję po uzgodnieniu jej poza aplikacją. Panel zleceń napraw i pozostałe zarządzanie wizytami pozostają dalszym etapem. |
| R09 | Mechanik i administrator mogą dodawać, edytować i usuwać kategorie oferty oraz przypisane do nich usługi. Przykładowe kategorie to elektryka, wulkanizacja i mechanika. Każda usługa należy do jednej kategorii i może zostać przeniesiona do innej. |
| R10 | Mechanik i administrator mogą zakończyć potwierdzone zgłoszenie w panelu `/staff/appointments`, wpisując opis wykonanych prac oraz pozycje naprawy z podziałem na robociznę i części. Zakończenie naprawy ustawia status `READY_FOR_PICKUP` („Czeka na odbiór”). Płatność odbywa się na miejscu poza systemem. Backend wylicza końcową kwotę brutto z pozycji naprawy; płatność online, korekty i pełna obsługa księgowa pozostają poza obecnym etapem. |
| R11 | Mechanik i administrator mogą oznaczyć zgłoszenie ze statusem `READY_FOR_PICKUP` jako odebrane przez klienta. Akcja „Samochód został odebrany” ustawia status `COMPLETED` („Zakończone”). Zakończone zgłoszenia powiązane z pojazdem klienta są pierwszą wersją historii napraw widoczną na `/vehicles/:vehicleId`. |
| R12 | Klient może pobrać prostą fakturę PDF z historii napraw własnego pojazdu przy zakończonym zgłoszeniu `COMPLETED`, a mechanik lub administrator może pobrać tę samą fakturę z panelu personelu, aby wydrukować ją klientowi przy odbiorze auta. Faktura zawiera logo Mietek Customs, numer, datę wystawienia i sprzedaży, dane warsztatu, dane nabywcy imienne albo firmowe z profilu, pojazd, opis wykonanych prac, wyszczególnione pozycje robocizny i części, wartości netto i brutto oraz informację o płatności przy odbiorze. To pierwsza wersja dokumentu, bez osobnej tabeli faktur, korekt, numeracji księgowej i deklaracji zgodności prawno-księgowej. |
| R13 | Profil Springa `local` przygotowuje dane pokazowe do prezentacji aplikacji: klientów indywidualnych i firmowych, ich profile, pojazdy oraz zgłoszenia w różnych statusach, w tym naprawy gotowe do odbioru i zakończone z możliwością pobrania faktury PDF. Seed działa idempotentnie: tworzy brakujące rekordy demonstracyjne, ale nie nadpisuje istniejących kont, haseł, profili, pojazdów ani zgłoszeń. |
| R14 | Administrator ma dedykowany panel `/admin/schedule-settings` do konfiguracji grafiku warsztatu. Może ustawić domyślną liczbę miejsc dziennie, horyzont rezerwacji, godziny pracy oraz wyjątki dla konkretnych dat: dzień zamknięty albo niestandardową liczbę miejsc. Mechanik może oglądać grafik i obsługiwać zgłoszenia, ale nie zarządza konfiguracją dostępności. Publiczny kalendarz i propozycje terminów korzystają z konfiguracji zapisanej w backendzie. |
| R15 | Administrator ma dedykowany panel `/admin/staff-accounts` do zarządzania kontami mechaników: tworzenia kont, edycji loginu i e-maila, resetowania hasła oraz aktywowania albo dezaktywowania dostępu. Publiczna rejestracja tworzy wyłącznie konta klientów, a rola `MECHANIC` nie może zostać nadana przez formularz publiczny. Konto mechanika zawiera login, e-mail, hasło ustawione przez admina oraz status aktywności; backend zawsze nadaje rolę `MECHANIC` samodzielnie. Mechanik nie może tworzyć ani zarządzać kontami personelu. |
| R16 | Aplikacja ma zostać przygotowana do dwóch wersji językowych interfejsu: polskiej i angielskiej. Frontend powinien używać centralnej warstwy tłumaczeń zamiast tekstów wpisanych bezpośrednio w komponentach. Domyślnym językiem pozostaje polski, ale użytkownik ma móc przełączyć język UI na angielski. Backend powinien zwracać stabilne kody błędów i techniczne komunikaty angielskie, a frontend powinien tłumaczyć znane błędy na język aktualnie wybrany w interfejsie. Dane biznesowe z bazy, takie jak nazwy usług, mogą pozostać w jednym języku do czasu osobnej decyzji o tłumaczeniu treści zapisanych w bazie. |

## Reguły biznesowe i granice dostępu

- Podstawowy przepływ zgłoszenia: `PENDING` → `CONFIRMED`, `REJECTED` albo `CANCELLED`.
  Personel może też przejść z `PENDING` do `TIME_PROPOSED`; zalogowany klient
  potwierdza propozycję, a w przypadku gościa personel potwierdza ją po kontakcie.
  Klient może odwołać własne aktywne zgłoszenie ze statusu `PENDING`,
  `TIME_PROPOSED` albo `CONFIRMED`; status `CANCELLED` zwalnia miejsce. Wysłanie
  formularza nie jest automatycznym potwierdzeniem wizyty.
- Po wykonaniu naprawy personel może przejść z `CONFIRMED` do `READY_FOR_PICKUP`.
  Wymagany jest opis wykonanych prac i kwota brutto większa od zera. Status
  `READY_FOR_PICKUP` oznacza, że auto czeka na odbiór i płatność na miejscu poza systemem.
  Po odbiorze auta personel może przejść z `READY_FOR_PICKUP` do `COMPLETED`;
  ten status oznacza zakończone zgłoszenie widoczne w historii napraw pojazdu.
- Klient korzysta z własnego profilu, pojazdów, zgłoszeń i dokumentów.
  Personel korzysta z danych w zakresie przyznanych uprawnień. Konta mechaników tworzy wyłącznie administrator.
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
- Kalendarz pokazuje dostępność, ale backend ponownie sprawdza dzień przyjęcia auta
  przy wysłaniu zgłoszenia i proponowaniu nowego dnia. Nie dopuszczaj do przekroczenia
  dziennego limitu aktywnych zgłoszeń, także przy równoczesnych żądaniach.
- Kalendarz używa strefy `Europe/Warsaw`, dni od poniedziałku do piątku jako domyślnie
  roboczych, a limit miejsc, horyzont rezerwacji i godziny pracy wynikają z konfiguracji admina.
  Wewnętrznie wybrany dzień jest zapisywany jako skonfigurowany początek dnia roboczego,
  ale interfejs nie umawia klienta na konkretną godzinę. Statusy `PENDING`,
  `TIME_PROPOSED` i `CONFIRMED` zajmują miejsce w danym dniu; `READY_FOR_PICKUP`,
  `COMPLETED`, `REJECTED` i
  `CANCELLED` je zwalniają. Zmiana dnia zwalnia poprzedni i zajmuje nowy atomowo.
  Backend zabezpiecza równoległe próby zajęcia miejsc.
- Pierwszy zapis wykonanej naprawy powstaje przy statusie `READY_FOR_PICKUP`
  i zawiera opis wykonanych prac oraz kwotę brutto do zapłaty. Do historii pojazdu
  trafia po oznaczeniu odbioru auta statusem `COMPLETED`. Docelowe faktury mogą
  rozbudować ten zapis o dokument sprzedaży. Sam opis usterki
  lub przyjęcie rezerwacji nie stanowi wpisu potwierdzającego wykonanie naprawy.
- Katalog ma dwa poziomy: kategoria → usługa. Nie dodawaj kolejnych poziomów
  podkategorii bez nowego wymagania. Nazwa kategorii jest unikalna, a nazwa usługi
  unikalna w jej kategorii, bez rozróżniania wielkości liter. Nie usuwaj kategorii
  zawierającej usługi. Ceny, czas usług i ich powiązanie z rezerwacjami są poza obecnym etapem.

## Kwestie otwarte

Nie zapisuj poniższych decyzji jako uzgodnionych, dopóki nie wynikają z rozmowy.
Doprecyzowuj je przy etapie, którego dotyczą; nie blokują pozostałych prac.

- Wiele stanowisk, różne długości usług i przypisywanie mechanika pozostają dalszym
  etapem planowania pracy warsztatu po przyjęciu auta.
- Automatyczne wygasanie blokady zgłoszenia oczekującego, przekładanie
  potwierdzonych wizyt oraz odrzucenie propozycji dnia przez klienta.
- Powiadomienia e-mail/SMS i zabezpieczenie publicznego formularza przed spamem.
- Podział pozostałych uprawnień administratora, pracownika i mechanika: tworzenie kont administratorów oraz faktury. Edycję katalogu usług, podgląd grafiku i obsługę zgłoszeń przyznano rolom MECHANIC i ADMIN, a konfigurację grafiku oraz zarządzanie kontami mechaników roli ADMIN.
- Sposób edycji, usunięcia lub sprzedaży pojazdu oraz dostępu nowego właściciela
  do wcześniejszych dokumentów. Dalsze rozszerzenia profilu, np. kraj lub osobny
  adres rozliczeniowy osoby prywatnej, wymagają nowego ustalenia.
- Zakres pełnego fakturowania: osobna tabela faktur, pozycje prac/części, korekty,
  docelowa numeracja księgowa, eksport dokumentów i ewentualna integracja księgowa.
  Obecna wersja generuje prosty PDF z zakończonego zgłoszenia, bez płatności online
  i bez deklaracji zgodności prawno-księgowej.
- Odzyskiwanie haseł klientów i administratorów oraz samoobsługowa zmiana hasła. Rejestracja klienta i zarządzanie kontami mechaników przez admina są dostępne;
  obecne uwierzytelnianie używa sesji Spring Security i ochrony CSRF. Ewentualna zmiana mechanizmu
  uwierzytelniania wymaga konkretnej potrzeby, JWT nie jest wymaganiem.
- Tłumaczenie danych zapisanych w bazie, np. nazw i opisów usług, historii warsztatu
  oraz treści faktur PDF, wymaga osobnej decyzji. Pierwszy etap i18n obejmuje przede
  wszystkim teksty interfejsu i mapowanie błędów API.

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
  Profil `local` tworzy konta i dane demonstracyjne opisane w README. Klient może utworzyć
  konto z unikalnym loginem i e-mailem, a po rejestracji zostaje automatycznie
  zalogowany z rolą CLIENT. Klient może utworzyć i aktualizować własny profil,
  dodawać własne pojazdy oraz otwierać ich szczegóły. Widok historii napraw pokazuje
  zakończone naprawy i pozwala pobrać prostą fakturę PDF klientowi oraz personelowi. Działają publiczna dostępność,
  zgłoszenia wizyt klienta i gościa, panel własnych zgłoszeń, grafik MECHANIC/ADMIN
  oraz decyzje personelu z proponowaniem nowego dnia. Administrator zarządza
  podstawową konfiguracją grafiku, wyjątkami dni oraz kontami mechaników. Pełny moduł faktur pozostaje do rozbudowy.
- Twórz pakiety i katalogi przy wdrażaniu funkcji. Unikaj pustych szkieletów całego
  systemu, mikroserwisów oraz nowych narzędzi bez konkretnej potrzeby.

## Kolejność rozwoju i jakość

- Zrealizowane etapy: katalog usług, rejestracja, profil klienta, jego pojazdy,
  zgłoszenia wizyt z kalendarzem i decyzją personelu, zakończenie naprawy,
  odbiór auta, historia napraw, prosta faktura PDF oraz lokalne dane pokazowe.
  Kolejne etapy obejmują dalsze reguły harmonogramu, powiadomienia i pełniejszy moduł faktur.
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

