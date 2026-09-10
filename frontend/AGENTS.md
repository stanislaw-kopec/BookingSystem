# Frontend — React i TypeScript

Obowiązują również cele i wymagania z głównego `AGENTS.md`.
Ten plik dotyczy kodu i konfiguracji w `frontend`.

## Organizacja i narzędzia

- Używaj Reacta z TypeScriptem i Vite. Komponenty z JSX mają rozszerzenie `.tsx`,
  pozostały kod TypeScript `.ts`. Zależności i dostępne polecenia określa `package.json`.
- Grupuj kod według funkcjonalności w `src/features`, gdy dana funkcjonalność powstaje.
  Wspólne komponenty przeznaczaj na elementy używane w wielu miejscach.
- Strona startowa i katalog z `GET /api/services` są zaimplementowane.
  `App.tsx` definiuje trasy React Router, `AppLayout` składa wspólny nagłówek,
  komunikaty konta i stopkę, a pliki w `pages` składają zawartość podstron.
  Opis warsztatu, usługi i logowanie są w osobnych katalogach `features`.
- Oddzielaj typy danych, komunikację z API i prezentację. Korzystaj z istniejącego
  sposobu stylowania. Router, biblioteki formularzy, UI czy pobierania danych dodawaj
  wtedy, gdy wdrażany etap ich potrzebuje; nie są jeszcze uzgodnionym zestawem narzędzi.
- Unikaj `any` i rzutowań maskujących błędy. Typy TypeScript nie weryfikują danych
  przychodzących w trakcie działania; obsługuj brakujące dane i błędne odpowiedzi API.

## Widoki wymagane przez użytkownika

- Nazwa warsztatu to „Mietek Customs”. Logo zawiera napis i gumową kaczkę w
  czapeczce w stylu hot roda. Grafikę przechowuj w `src/assets/branding`, importuj
  przez `WorkshopLogo` i używaj w nagłówku oraz sekcji o warsztacie. Nazwa w React
  pochodzi z `workshopInfo.name`; tytuł i opis dokumentu są w `index.html`.

- R01–R02: publiczna strona z informacjami o warsztacie i usługami; menu u góry oraz
  jeden przycisk „Logowanie / Rejestracja” po prawej. Jeden punkt wejścia może prowadzić
  do widoku przełączającego formularze logowania i rejestracji.
- Strona główna pokazuje kolejno `WorkshopOverview`, `ServicesSection` i
  `WorkshopLocation` z mapą Google. Lokalizacja jest ostatnią sekcją, także gdy
  personel widzi panel edycji oferty. Mapa używa demonstracyjnego punktu we Wrocławiu,
  wyraźnie opisanego jako przykładowy; współrzędne i opis są w `workshopInfo.ts`.
- R03: link „Mój profil” w menu konta oraz osobna podstrona `/profile` są dostępne tylko dla CLIENT.
  Formularz edytuje wymagane dane kontaktowe i adres, a przełącznik firmy odsłania
  wymagane pola rozliczeniowe. Zapisane dane pokazuj najpierw w trybie podglądu,
  a formularz dopiero po wybraniu edycji. Nie umieszczaj formularza na stronie startowej.
- R04–R05: „Moje pojazdy” w menu konta prowadzi do `/vehicles` z listą i formularzem
  dodawania. Szczegóły `/vehicles/:vehicleId` pokazują dane pojazdu i historię napraw
  wynikającą z faktur, z odniesieniem do właściwego dokumentu. Do czasu wdrożenia faktur
  pokazuj uczciwy pusty stan i nie twórz fikcyjnych napraw.
- R06–R07: publiczna trasa `/appointments` otwiera umawianie wizyty. CLIENT wybiera
  własny pojazd albo dodaje go w formularzu, dzień przyjęcia auta z API i opis usterki. Osobna,
  chroniona trasa `/my-appointments` („Moje wizyty”) pokazuje własne zgłoszenia
  i pozwala potwierdzić dzień ze statusem `TIME_PROPOSED` albo odwołać aktywną
  wizytę. Główne menu zawiera publiczny link „Umów wizytę”, a prywatny link
  „Moje wizyty” jest dostępny z menu konta.
  Gość podaje dane kontaktowe, dane pojazdu, dzień przyjęcia auta i opis. Po wysłaniu widzi numer
  referencyjny oraz informację o oczekiwaniu, ale nie otrzymuje panelu ani podglądu statusu.
- R08: `/staff/schedule` jest chronioną zakładką MECHANIC/ADMIN z graficznym,
  tygodniowym grafikiem aktywnych zgłoszeń pogrupowanych według dni od poniedziałku do piątku. `/staff/appointments`
  jest chronionym panelem MECHANIC/ADMIN z kolejką klientów i gości oraz akcjami
  potwierdzenia, odrzucenia i zaproponowania innego dnia. Propozycję gościa
  personel potwierdza w panelu po kontakcie telefonicznym lub mailowym.
- R10: w `/staff/appointments` przy zgłoszeniu `CONFIRMED` pokaż akcję „Praca zakończona”.
  Formularz wymaga opisu wykonanych prac i kwoty brutto do zapłaty przy odbiorze.
  Po zapisie zgłoszenie ma status `READY_FOR_PICKUP`, a klient widzi opis prac i kwotę
  w swoich wizytach. Nie dodawaj jeszcze płatności online ani rozbicia netto/VAT.
- R09: oferta znajduje się pod opisem warsztatu na stronie głównej. Kategorie
  pokazuje `ServiceCategoryCard`, a formularze edycji `CatalogManager` dla MECHANIC/ADMIN.
  Link „Usługi” przewija do sekcji, nie wymaga osobnej trasy ani routera.
- Menu i widoki dostosuj do zalogowania oraz uprawnień. Główne menu nie powinno
  dublować linków prywatnych z menu konta. Linki klienta, takie jak profil, pojazdy
  i własne wizyty, grupuj w rozwijanym menu konta w prawym górnym rogu. Linki
  personelu, takie jak grafik, zgłoszenia wizyt i zarządzanie ofertą, grupuj w sekcji
  „Panel warsztatu” tego samego menu.
- Wspólny przycisk otwiera okno przełączające działające formularze logowania
  i rejestracji. Rejestracja wymaga loginu, e-maila, hasła i powtórzenia hasła,
  pokazuje błędy przy polach i po sukcesie automatycznie loguje klienta.
- Na tym etapie stosuj prosty CSS. Kolory są w zmiennych `src/index.css`,
  a opis i dane warsztatu w `features/workshop/workshopInfo.ts`.
  Historia jest fikcyjnym tekstem demonstracyjnym w trzech akapitach, napisanym na
  prośbę użytkownika. Rzeczywistego adresu nie podano; zachowaj oznaczenie lokalizacji przykładowej.

## Rezerwacje, formularze i prywatność

- Publiczny link „Umów wizytę” w głównym menu prowadzi do `/appointments`. Podczas
  sprawdzania sesji nie pokazuj chwilowo formularza gościa zalogowanemu użytkownikowi.
- Pobieraj dostępność z backendu. Nie wyliczaj w przeglądarce, że dzień jest wolny,
  wyłącznie na podstawie dnia tygodnia lub lokalnej listy wizyt.
- Kalendarz grupuje wolne dni zwrócone przez API, pokazuje strefę `Europe/Warsaw`,
  dzienną pojemność i liczbę wolnych miejsc oraz pozwala przejść przez 30-dniowy
  horyzont. Nie hardkoduj listy wolnych dni ani limitu miejsc jako źródła dostępności.
- Nowy pojazd CLIENT zapisuj istniejącym `POST /api/vehicles` i automatycznie wybieraj
  go w formularzu. Dane pojazdu gościa wysyłaj wyłącznie jako część zgłoszenia.
- Po wysłaniu zgłoszenia pokazuj „Oczekujące na decyzję warsztatu”. Potwierdzenie
  wizyty pokazuj dopiero po odpowiedzi informującej o przyjęciu przez personel.
- Obsługuj sytuację, gdy dzień stał się niedostępny: pokaż komunikat, odśwież dni
  i umożliw ponowny wybór bez utraty wpisanego opisu usterki.
- Wyświetlaj daty według ustalonej strefy warsztatu i kontraktu API.
  Nie przyjmuj po cichu strefy komputera użytkownika jako strefy warsztatu.
- Pokazuj stany ładowania, pustej listy, błędu i sukcesu. Podczas wysyłania formularza
  zapobiegaj ponownemu wysłaniu; błędy walidacji wiąż z odpowiednimi polami.
- Ukrywanie panelu lub zabezpieczenie trasy nie zastępuje autoryzacji backendu.
  Po wylogowaniu usuwaj z widoku i pamięci klienta dane poprzedniej sesji.
- Statusy przedstawiaj po polsku: `PENDING` jako „Oczekujące”, `TIME_PROPOSED` jako
  „Zaproponowano nowy dzień”, `CONFIRMED` jako „Potwierdzone”, `READY_FOR_PICKUP`
  jako „Czeka na odbiór”, `CANCELLED` jako „Odwołane”, a `REJECTED` jako „Odrzucone”.
  Pokazuj pierwotny i proponowany dzień
  bez sugerowania wykonanej naprawy.
- Historia napraw i dane dokumentów pochodzą z API. Nie twórz fikcyjnych faktur
  ani lokalnych wpisów udających trwale zapisane dane.

## Komunikacja i uruchamianie

- W żądaniach do backendu używaj względnego prefiksu `/api`.
  Przeglądarka nie powinna odwoływać się do dockerowej nazwy `backend`.
- Używaj `src/api/apiClient.ts`: obsługuje błędy i pobiera aktualny token CSRF
  przed zmianami danych, także po logowaniu lub wylogowaniu. Sesję utrzymuje
  ciasteczko HttpOnly; nie zapisuj hasła ani tokenu sesji w localStorage.
- `useServiceCatalog` w `HomePage` jest wspólnym źródłem katalogu dla oferty i panelu.
  Po zapisie odśwież dane z API. Przewodnik po tej strukturze jest w
  `docs/service-catalog-walkthrough.md` w głównym folderze repozytorium.
- `AuthDialog` odpowiada za wybór trybu, a `LoginForm` i `RegistrationForm`
  za własne pola oraz wysłanie danych. `AuthProvider` przechowuje bieżącego użytkownika.
  Przewodnik po tym przepływie jest w `docs/client-registration-walkthrough.md`.
- Funkcja profilu znajduje się w `features/profile`: typy opisują kontrakt,
  `profileApi` odpowiada za HTTP, a `ClientProfileSection` zarządza pobraniem i zapisem.
  `ProfilePage` składa podstronę, a `RequireClient` chroni trasę w interfejsie.
  Backend pozostaje źródłem rzeczywistych uprawnień. Wylogowanie przekierowuje ze strony
  profilu i odmontowuje sekcję, usuwając jej dane ze stanu Reacta.
- Funkcja pojazdów znajduje się w `features/vehicles`: `vehiclesApi` odpowiada za HTTP,
  `VehicleForm` za dodawanie, `VehiclesSection` za listę, a `VehicleDetailsSection`
  za dane wybranego pojazdu i stan historii. `VehiclesPage` i `VehiclePage` składają trasy.
- Funkcja wizyt znajduje się w `features/appointments`: `appointmentsApi` sprawdza
  kontrakt HTTP, `useAppointmentAvailability` pobiera kalendarz, a osobne komponenty
  obsługują formularz gościa, formularz i listę CLIENT oraz kolejkę personelu.
  `AppointmentsPage` dobiera wariant formularza po zakończeniu sprawdzania sesji.
  `MyAppointmentsPage` wyświetla wyłącznie listę zgłoszeń i jest chroniona przez `RequireClient`, a
  `StaffAppointmentsPage` i `StaffSchedulePage` chroni `RequireStaff`. Przewodnik znajduje się w
  `docs/appointment-booking-walkthrough.md`.
- Zachowaj proxy w `vite.config.ts`: w Dockerze cel ustawia `API_PROXY_TARGET`,
  lokalnie używany jest `http://localhost:8080`. Prywatnych sekretów nie umieszczaj
  w kodzie frontendu ani zmiennych udostępnianych przeglądarce.
- Z katalogu `frontend`: `npm ci` instaluje zależności z lockfile,
  `npm run dev` uruchamia Vite, `npm run build` sprawdza TypeScript i buduje aplikację,
  a `npm run lint` uruchamia istniejący Oxlint.
- Z głównego folderu `docker compose up -d --build frontend` przebudowuje kontener.
  Kod w `src`, pliki `public` i `index.html` są podłączone do kontenera na bieżąco.
  Zachowaj polling dla Docker Desktop na Windows i oddzielne zależności kontenera.

## Sprawdzanie interfejsu

- Po zmianie kodu sprawdź odpowiednie budowanie/lint oraz zmieniony przepływ w UI.
  Widoki mają działać na telefonie i komputerze oraz być obsługiwane klawiaturą;
  formularze wymagają etykiet i czytelnych komunikatów.
- Gdy powstaną interaktywne przepływy, dobierz testy do ich zachowania: wyboru dnia,
  obsługi konfliktu, decyzji personelu i prezentacji historii właściwego pojazdu.
  Obecnie nie ma skonfigurowanego polecenia `npm test` ani frameworka testów UI.
