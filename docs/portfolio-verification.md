# Weryfikacja wersji portfolio

Data sprawdzenia: 17.09.2026. Aplikacja działała lokalnie jako trzy zdrowe usługi
Docker Compose: PostgreSQL, backend Spring Boot i frontend React.

## Pełny scenariusz użytkownika

Scenariusz wykonano na danych demonstracyjnych, bez prawdziwych danych osobowych.

1. Klient `anna.demo` wybrał Hondę Civic `DW8CIVIC`, dzień 21.09.2026 i opisał
   metaliczny dźwięk podczas hamowania.
2. Backend utworzył zgłoszenie `a3b0d95d-6638-4ebc-b75b-b7364fc1beb2` ze statusem
   `PENDING` i zmniejszył liczbę wolnych miejsc z czterech do trzech.
3. Konto `mechanic` zobaczyło zgłoszenie w tygodniowym grafiku i potwierdziło termin.
4. Mechanik zapisał wykonane prace oraz dwie pozycje: robociznę 350,00 zł i części
   780,00 zł. Backend obliczył 1130,00 zł brutto i ustawił `READY_FOR_PICKUP`.
5. Po potwierdzeniu odbioru status zmienił się na `COMPLETED`. Naprawa pojawiła się
   w „Moich wizytach” i historii pojazdu, z przyciskiem pobrania faktury.
6. Pobrano fakturę `MC/2026/000014`. Dokument A4 ma logo, dane stron i pojazdu,
   opis prac, obie pozycje, 918,70 zł netto, 211,30 zł VAT i 1130,00 zł brutto.

Podgląd dokumentu znajduje się w
[`screenshots/06-invoice-preview.png`](screenshots/06-invoice-preview.png).

## Wyniki automatycznych sprawdzeń

| Sprawdzenie | Wynik |
| --- | --- |
| `mvn -B -ntp test` | 126 testów, 0 błędów i 0 niepowodzeń |
| `npm run test:ci` | 22 testy w 5 plikach, wszystkie przeszły |
| `npm run lint` | bez błędów i ostrzeżeń |
| `npm run build` | produkcyjny build Vite zakończony powodzeniem |
| `docker compose config --quiet` | konfiguracja poprawna |
| `docker compose up --build -d --wait` | trzy usługi uruchomione ze stanem `healthy` |

Po przebudowie aplikacja, `/api/health` i Swagger UI odpowiedziały kodem HTTP 200.

Testy backendu uruchamiają własny PostgreSQL 17 w Testcontainers i nie zmieniają
lokalnych danych demonstracyjnych. Workflow GitHub Actions wykonuje te same główne
kontrole, a po nieudanych testach udostępnia raport Surefire lub JUnit jako artefakt.

## Jak powtórzyć prezentację

Uruchom aplikację poleceniem z głównego README, a następnie użyj kont `anna.demo`
i `mechanic`. Do krótkiej prezentacji można wykorzystać istniejące przykładowe
zgłoszenia; utworzenie nowego pełnego przebiegu zapisze kolejne dane w lokalnym
wolumenie PostgreSQL.

Scenariusz E2E został wykonany ręcznie w prawdziwej przeglądarce. Nie jest częścią
automatycznego CI, ponieważ obecny zestaw testów koncentruje się na regułach backendu
i zachowaniu kluczowych komponentów frontendu.
