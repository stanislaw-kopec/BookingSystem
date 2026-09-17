# Etap 4 — stan interfejsu, sesja i testy

## Listy i odświeżanie

Listy wizyt klienta i personelu ignorują ponowny wybór aktualnego filtra lub
sortowania. Zmiana parametrów włącza ładowanie, ukrywa poprzednie wyniki i uruchamia
zapytanie. Błąd pokazuje komunikat oraz przycisk ponowienia z tymi samymi parametrami.
Filtry pozostają dostępne także podczas ładowania i po błędzie. Lista kont również
usuwa poprzedni wynik po błędzie; wyszukiwanie pozwala ponowić zapytanie.

Efekty używają `AbortController`: odpowiedź poprzedniego zapytania nie zastępuje
wyniku nowszego filtra. Jeśli po zmianie danych wybrana strona już nie istnieje,
komponent pobiera ostatnią dostępną stronę, zachowując stan ładowania.

## Sesja w przeglądarce

`apiClient.ts` wspólnie obsługuje JSON, przygotowanie CSRF oraz pobieranie PDF.
Odpowiedź 401 powiadamia `AuthProvider`, który usuwa użytkownika i odmontowuje
dotychczasowe widoki. Chronione trasy wracają na stronę główną, gdzie można ponownie
się zalogować. Nieudane logowanie samo w sobie nie jest zdarzeniem wygaśnięcia sesji.

`sessionEvents.ts` przechowuje licznik zmiany sesji. Żądanie zapamiętuje jego wartość;
spóźniona odpowiedź z wcześniejszej sesji nie jest przekazywana komponentom.
Dotyczy to również odpowiedzi 401, która nie może wylogować nowo zalogowanego konta.
Osobny licznik w providerze chroni przed spóźnionym odczytem `/api/auth/me`.

Logowanie i wylogowanie wysyłają do innych kart sygnał przez zdarzenie `storage`.
Zapisywany jest tylko losowy identyfikator zmiany, bez danych konta, hasła ani tokenu.
Inna karta odmontowuje poprzedni widok i sprawdza sesję w API. Powrót do okna lub
karty także odświeża sesję, ale nie przerywa trwającego logowania lub wylogowania.
Autoryzacja i ciasteczko HttpOnly nadal należą do Spring Security.

Nie ma stałego odpytywania serwera ani połączenia push: reset hasła na innym
urządzeniu zostanie wykryty przy następnym żądaniu lub powrocie do karty. Gdy
przeglądarka blokuje localStorage, pozostaje kontrola przy żądaniach i powrocie.
Nieudana weryfikacja sesji ukrywa prywatne widoki; niezapisany formularz może zostać
utracony. Sygnał między kartami nigdy nie jest źródłem uprawnień.

## Walidacja i dostępność

`validationMessages.ts` przedstawia angielskie błędy pól API po polsku, bez i18n.
Znane reguły mają konkretne wskazówki; nieznana reguła otrzymuje polski komunikat
ogólny. Kody błędów całej operacji nadal obsługuje `errorMessage`.
Przy nowej regule backendu należy dopisać odpowiednią wskazówkę.

Klucz `repairItems[2].quantity` oznacza ilość w trzeciej pozycji naprawy.
Formularz pokazuje numer pozycji, błąd przy właściwym polu oraz łączy je przez
`aria-describedby` i `aria-invalid`. Wartości formularza pozostają do poprawienia.

`CustomSelect` zachowuje fokus na elemencie `combobox`, a aktywną opcję wskazuje
przez `aria-activedescendant`. Strzałki omijają wyłączone opcje, Home/End przechodzą
na krańce listy, Enter/Spacja zatwierdzają, Escape i Tab zamykają listę. Aktywna
opcja przewija się do widoku. Kliknięcie poza listą również ją zamyka.

## Uruchamianie testów

Z katalogu `frontend`:

```sh
npm ci
npm test
npm run lint
npm run build
```

`npm run test:watch` uruchamia testy po kolejnych zmianach podczas nauki.
Vitest, React Testing Library i jsdom sprawdzają zachowanie komponentów oraz
obsługę żądań. Testy mają kontrolowane odpowiedzi API i nie zmieniają bazy aplikacji.
Zestaw obejmuje filtry, błędy i ponowienia, spóźnione odpowiedzi, sesje, PDF,
walidację pozycji naprawy oraz klawiaturę list rozwijanych.

GitHub Actions wykonuje `npm test` przed budowaniem frontendu. Testy jsdom nie
weryfikują rzeczywistego układu ani przewijania; te elementy sprawdzono dodatkowo
w przeglądarce na komputerze i przy szerokości 390 px. Pełny scenariusz E2E
z backendem pozostaje etapem 5.
