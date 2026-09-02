# Frontend — React i TypeScript

Obowiązują również cele i wymagania z głównego `AGENTS.md`.
Ten plik dotyczy kodu i konfiguracji w `frontend`.

## Organizacja i narzędzia

- Używaj Reacta z TypeScriptem i Vite. Komponenty z JSX mają rozszerzenie `.tsx`,
  pozostały kod TypeScript `.ts`. Zależności i dostępne polecenia określa `package.json`.
- Grupuj kod według funkcjonalności w `src/features`, gdy dana funkcjonalność powstaje.
  Wspólne komponenty przeznaczaj na elementy używane w wielu miejscach.
- Strona startowa i katalog z `GET /api/services` są zaimplementowane.
  `App.tsx` udostępnia kontekst logowania, a `pages/HomePage.tsx` składa widok.
  Opis warsztatu, usługi i logowanie są w osobnych katalogach `features`.
- Oddzielaj typy danych, komunikację z API i prezentację. Korzystaj z istniejącego
  sposobu stylowania. Router, biblioteki formularzy, UI czy pobierania danych dodawaj
  wtedy, gdy wdrażany etap ich potrzebuje; nie są jeszcze uzgodnionym zestawem narzędzi.
- Unikaj `any` i rzutowań maskujących błędy. Typy TypeScript nie weryfikują danych
  przychodzących w trakcie działania; obsługuj brakujące dane i błędne odpowiedzi API.

## Widoki wymagane przez użytkownika

- R01–R02: publiczna strona z informacjami o warsztacie i usługami; menu u góry oraz
  jeden przycisk „Logowanie / Rejestracja” po prawej. Jeden punkt wejścia może prowadzić
  do widoku przełączającego formularze logowania i rejestracji.
- R03: profil zalogowanego klienta z edycją danych kontaktowych i rozliczeniowych,
  uwzględniający klienta z firmą. Pola i ich obowiązkowość doprecyzuj przy implementacji.
- R04–R05: „Moje pojazdy” z listą i dodawaniem pojazdu; szczegóły wybranego pojazdu
  pokazują historię napraw wynikającą z faktur, z odniesieniem do właściwego dokumentu.
- R06–R07: zakładka rezerwacji z kalendarzem wolnych terminów od poniedziałku do piątku
  oraz formularzem wyboru własnego pojazdu i opisu usterki. Klient widzi status zgłoszenia.
- R08: panel personelu do obsługi zgłoszeń, przyjmowania zleceń i zarządzania wizytami,
  z akcjami przyjęcia i odrzucenia dostępnymi zgodnie z uprawnieniami.
- R09: oferta znajduje się pod opisem warsztatu na stronie głównej. Kategorie
  pokazuje `ServiceCategoryCard`, a formularze edycji `CatalogManager` dla MECHANIC/ADMIN.
  Link „Usługi” przewija do sekcji, nie wymaga osobnej trasy ani routera.
- Menu i widoki dostosuj do zalogowania oraz uprawnień. Konkretne nazwy pozostałych
  pozycji menu i wygląd po zalogowaniu dobieraj w ramach projektowania tych widoków.
- Aktualnie wspólny przycisk otwiera działające logowanie i informację o przyszłej
  rejestracji. Nie przedstawiaj rejestracji jako gotowej funkcji.
- Na tym etapie stosuj prosty CSS. Kolory są w zmiennych `src/index.css`,
  a opis i dane warsztatu w `features/workshop/workshopInfo.ts`.
  Rzeczywistego adresu i historii nie podano; zachowaj uczciwe teksty tymczasowe.

## Rezerwacje, formularze i prywatność

- Pobieraj dostępność z backendu. Nie wyliczaj w przeglądarce, że termin jest wolny,
  wyłącznie na podstawie dnia tygodnia lub lokalnej listy wizyt.
- Po wysłaniu zgłoszenia pokazuj „Oczekujące na decyzję warsztatu”. Potwierdzenie
  wizyty pokazuj dopiero po odpowiedzi informującej o przyjęciu przez personel.
- Obsługuj sytuację, gdy termin stał się niedostępny: pokaż komunikat, odśwież terminy
  i umożliw ponowny wybór bez utraty wpisanego opisu usterki.
- Wyświetlaj daty według ustalonej strefy warsztatu i kontraktu API.
  Nie przyjmuj po cichu strefy komputera użytkownika jako strefy warsztatu.
- Pokazuj stany ładowania, pustej listy, błędu i sukcesu. Podczas wysyłania formularza
  zapobiegaj ponownemu wysłaniu; błędy walidacji wiąż z odpowiednimi polami.
- Ukrywanie panelu lub zabezpieczenie trasy nie zastępuje autoryzacji backendu.
  Po wylogowaniu usuwaj z widoku i pamięci klienta dane poprzedniej sesji.
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
- Gdy powstaną interaktywne przepływy, dobierz testy do ich zachowania: wyboru terminu,
  obsługi konfliktu, decyzji personelu i prezentacji historii właściwego pojazdu.
  Obecnie nie ma skonfigurowanego polecenia `npm test` ani frameworka testów UI.
