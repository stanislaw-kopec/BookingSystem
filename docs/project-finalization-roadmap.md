# Roadmapa domykania projektu BookingSystem

> Aktualna kolejność napraw i status ich wykonania znajdują się w
> [checkliście poprawek po audycie](audit-remediation-checklist.md).
> Poniżej zachowano wcześniejszy plan funkcji i materiały do nauki.

Ten plik zbiera rzeczy, które warto zrobić przed uznaniem projektu za gotowy do pokazania w portfolio. Celem nie jest dokładanie wielu nowych funkcji warsztatu, tylko doprowadzenie aplikacji do stanu, w którym dobrze pokazuje umiejętności Java Developera: czytelne API, testy, dokumentację, bezpieczeństwo, Docker i sensowny frontend.

## Najważniejszy wniosek

Projekt jest już funkcjonalnie mocny: ma role, logowanie, profil klienta, pojazdy, umawianie wizyt, panel personelu, konfigurację grafiku, zarządzanie kontami użytkowników i tworzenie kont personelu, historię napraw, faktury PDF, migracje bazy, Docker Compose i testy integracyjne. Największy zysk przed zakończeniem projektu dadzą teraz elementy jakościowe i prezentacyjne, a nie kolejny duży moduł biznesowy.

## Priorytet 1: OpenAPI / Swagger — wdrożone

Automatyczna dokumentacja API została dodana przez `springdoc-openapi`. Swagger UI jest dostępny po uruchomieniu backendu pod `/swagger-ui.html`, a specyfikacja OpenAPI pod `/v3/api-docs/booking-system`.

Obecny zakres:

- dodano Swagger UI przez `springdoc-openapi`,
- dodano podstawowe informacje OpenAPI o aplikacji, sesji i CSRF,
- dokumentacja obejmuje endpointy z prefiksem `/api`,
- dodano testy sprawdzające publiczny dostęp do OpenAPI i Swagger UI.

Dlaczego warto:

- rekruter lub techniczny rozmówca może szybko zobaczyć API,
- łatwiej opowiedzieć o kontraktach DTO,
- projekt wygląda bardziej profesjonalnie.

## Priorytet 2: GitHub Actions / CI — wdrożone

Projekt ma testy backendowe, build frontendu oraz workflow GitHub Actions uruchamiany po pushu i przy pull requestach do `main`.

Obecny zakres:

- backend: Java 25 i `./mvnw test`,
- frontend: Node.js 24, `npm ci`, `npm run lint`, `npm run build`,
- konfiguracja Docker Compose: `docker compose config --quiet`.

Dlaczego warto:

- pokazuje dbałość o jakość,
- ułatwia utrzymanie projektu,
- na rozmowie można powiedzieć, że kod jest automatycznie weryfikowany.

## Priorytet 3: README pod portfolio — przygotowane

README rozpoczyna się opisem biznesowym i prezentuje projekt osobie, która pierwszy raz widzi repozytorium.

Obecny zakres:

- krótki opis biznesowy aplikacji,
- stack technologiczny,
- listę głównych funkcji,
- konta demonstracyjne,
- krótki scenariusz demo,
- sekcję „Najciekawsze elementy techniczne”,
- diagram architektury,
- cztery zrzuty pokazujące przepływ od zgłoszenia do faktury.

Zrzuty wykonano 22.09.2026 i zastąpiono nimi placeholdery w głównym README.

Mocne punkty do pokazania:

- Spring Security z sesją i CSRF,
- role `CLIENT`, `MECHANIC`, `ADMIN`,
- migracje Flyway,
- PostgreSQL,
- transakcyjne blokowanie miejsc w grafiku,
- backendowa paginacja, filtrowanie i sortowanie,
- generowanie PDF faktury,
- Docker Compose,
- Testcontainers,
- walidacja i spójny format błędów.

## Priorytet 4: testy zarządzania kontami — wdrożone

Obszar zarządzania kontami personelu jest pokryty testami integracyjnymi.

Obecny zakres testów:

- admin może utworzyć konto mechanika i administratora,
- mechanik i klient nie mogą tworzyć kont personelu,
- użytkownik anonimowy nie ma dostępu do panelu admina,
- publiczna rejestracja tworzy wyłącznie konto klienta,
- dezaktywowany mechanik nie może się zalogować,
- admin może edytować, dezaktywować i zresetować hasło klienta lub mechanika,
- konta administratorów są widoczne, ale chronione przed edycją, resetem hasła i dezaktywacją.

Dlaczego warto:

- to dobry temat na rozmowę o bezpieczeństwie,
- pokazuje rozumienie ról i autoryzacji,
- wzmacnia backendową część portfolio.

## Priorytet 5: uporządkowanie komunikatów

Aplikacja pozostaje z polskim interfejsem. Dwujęzyczność została wycofana, więc przed zakończeniem projektu warto utrzymać prosty kierunek: teksty widoczne dla użytkownika są po polsku, a techniczne nazwy klas, DTO, endpointów, enumów i pól pozostają po angielsku.

### Frontend

Docelowy kierunek:

- nie dodawać mechanizmu wielojęzyczności ani przełącznika języka bez nowej decyzji,
- utrzymywać czytelne polskie etykiety statusów i komunikatów,
- mapować stabilne kody błędów API na przyjazne polskie komunikaty,
- zachować obsługę `message` jako fallback dla nieznanych błędów backendu.

### Backend

Docelowy kierunek:

- błędy API powinny mieć stabilny kod techniczny, np. `VEHICLE_NOT_FOUND`, `APPOINTMENT_DAY_FULL`, `VALIDATION_FAILED`,
- komunikat tekstowy może pozostać techniczny,
- frontend powinien opierać czytelny komunikat głównie na kodzie błędu,
- nie należy tłumaczyć nazw klas, pól DTO, endpointów ani enumów.

### Faktury PDF i treści biznesowe

Faktura pozostaje po polsku. Nazwy usług, opisy napraw i inne dane zapisane w bazie są wyświetlane tak, jak zostały zapisane.

## Opcjonalnie: edycja pojazdu — wdrożone

Klient może edytować podstawowe dane własnego pojazdu z jego podstrony szczegółów.

Minimalny zakres:

- edycja marki, modelu, rocznika, rejestracji i VIN,
- dostęp tylko dla właściciela,
- walidacja duplikatu rejestracji i VIN,
- test integracyjny.

To naturalna funkcja, ale mniej ważna niż OpenAPI, CI i README.

## Opcjonalnie: zmiana hasła przez użytkownika — wdrożone

Każda zalogowana rola ma samoobsługową zmianę własnego hasła po podaniu obecnego hasła.

Minimalny zakres:

- osobna podstrona lub sekcja konta,
- stare hasło,
- nowe hasło,
- powtórzenie nowego hasła,
- backend sprawdza stare hasło i zapisuje nowe przez BCrypt.

Nie robić jeszcze odzyskiwania hasła przez e-mail, jeśli nie ma osobnej potrzeby.

## Produkcyjniejszy Docker frontendu

Obecnie frontend w Dockerze działa przez Vite dev server, co jest dobre do nauki i developmentu. Dla portfolio można dodać osobny wariant produkcyjny.

Opcje:

- build statyczny i serwowanie przez nginx,
- osobny plik Compose dla produkcyjnego uruchomienia,
- zachowanie obecnego trybu developerskiego do codziennej pracy.

To jest dobre uzupełnienie, ale po OpenAPI, CI i README.

## Proponowana kolejność kończenia projektu

1. Zrobić commit obecnych zmian UI.
2. Dodać OpenAPI / Swagger.
3. Dodać GitHub Actions / CI.
4. Dodać testy kont mechaników.
5. Przerobić README pod portfolio — przygotowane; pozostają zrzuty ekranu.
6. Utrzymać stabilne kody błędów API i mapowanie komunikatów w frontendzie.
7. Edycja pojazdu — wdrożona.
8. Samoobsługowa zmiana hasła — wdrożona.
9. Opcjonalnie przygotować produkcyjniejszy Docker frontendu.

## Czego nie rozbudowywać przed pierwszą prezentacją

Na tym etapie nie warto robić zbyt dużych modułów, które mogą rozciągnąć projekt bez dużego zysku rekrutacyjnego:

- pełna księgowość faktur,
- płatności online,
- SMS/e-mail z prawdziwą integracją,
- wiele stanowisk warsztatowych i zaawansowany algorytm planowania,
- mikroserwisy,
- JWT tylko dlatego, że jest popularne,
- rozbudowane UI frameworki bez konkretnej potrzeby.

## Krótka wersja

Przed publikacją pierwszej wersji pozostają trzy główne kroki:

1. Dodać przygotowane zrzuty ekranu do README.
2. Opcjonalnie przygotować produkcyjny wariant Dockera dla frontendu.
3. Przejść końcowy scenariusz demonstracyjny i oznaczyć wersję tagiem.

