# Pojazdy klienta — przewodnik po implementacji

Ten etap dodaje zapis pojazdów w PostgreSQL, prywatne REST API oraz dwie podstrony Reacta:
listę `/vehicles` i szczegóły `/vehicles/:vehicleId`.

## 1. Własność pojazdu

Tabela `vehicles` ma klucz obcy `owner_id` wskazujący na `app_users`:

```text
app_users (1) ──────── (0..n) vehicles
```

Formularz nie wysyła `ownerId`. [VehicleController.java](../backend/src/main/java/pl/autoserwis/vehicle/VehicleController.java)
przekazuje do serwisu nazwę użytkownika z `Authentication`, a
[VehicleService.java](../backend/src/main/java/pl/autoserwis/vehicle/VehicleService.java)
odnajduje konto bieżącej sesji.

Szczegóły pobiera metoda repozytorium `findByIdAndOwner_Id`. Jeden warunek SQL sprawdza
jednocześnie identyfikator pojazdu i właściciela. Dzięki temu klient nie może odczytać
cudzego pojazdu przez zmianę liczby w adresie. Brak pojazdu i cudzy pojazd zwracają 404,
więc odpowiedź nie ujawnia, czy obcy identyfikator istnieje.

## 2. Migracja i model

[V6__create_vehicles.sql](../backend/src/main/resources/db/migration/V6__create_vehicles.sql)
tworzy kolumny:

- `make` — marka,
- `model` — model,
- `production_year` — rok produkcji,
- `registration_number` — numer rejestracyjny,
- `vin` — opcjonalny VIN.

Indeksy gwarantują, że jeden klient nie zapisze dwa razy tego samego numeru
rejestracyjnego ani VIN. Numery innego klienta nie powodują konfliktu, co nie blokuje
przyszłej obsługi sprzedaży pojazdu.

## 3. Walidacja i normalizacja

[VehicleRequest.java](../backend/src/main/java/pl/autoserwis/vehicle/dto/VehicleRequest.java)
obsługuje podstawowe ograniczenia Bean Validation. Serwis sprawdza dodatkowo, czy rok
nie wykracza dalej niż następny rok kalendarzowy.

Przed zapisem numer rejestracyjny traci spacje i jest zamieniany na wielkie litery.
VIN również jest zapisywany wielkimi literami. Dzięki temu `kr 12 ab` i `KR12AB`
nie tworzą dwóch wpisów.

## 4. Endpointy

```text
GET  /api/vehicles              lista własnych pojazdów
GET  /api/vehicles/{vehicleId}  szczegóły własnego pojazdu
POST /api/vehicles              dodanie pojazdu
```

Wszystkie wymagają sesji z rolą `CLIENT`. POST dodatkowo wymaga tokenu CSRF, który
istniejący `apiClient` pobiera automatycznie.

## 5. Struktura Reacta

```text
vehicles/
├── types.ts
├── api/
│   └── vehiclesApi.ts
├── components/
│   ├── VehicleForm.tsx
│   ├── VehiclesSection.tsx
│   └── VehicleDetailsSection.tsx
└── vehicles.css
```

- `vehiclesApi.ts` sprawdza dane otrzymane z API również podczas działania aplikacji.
- `VehicleForm.tsx` przechowuje wartości pól i tworzy `VehicleInput`.
- `VehiclesSection.tsx` pobiera listę, obsługuje pusty stan oraz dodanie pojazdu.
- `VehicleDetailsSection.tsx` pobiera jeden własny pojazd.

Trasy znajdują się w [App.tsx](../frontend/src/App.tsx), a `RequireClient` chroni je
w interfejsie. Rzeczywistą ochronę danych nadal zapewnia Spring Security i warunek
właściciela w zapytaniu backendu.

## 6. Historia napraw

Strona szczegółów ma już sekcję „Historia napraw”, ale nie tworzy lokalnej listy
przykładowych wpisów. Historia ma wynikać z wystawionych faktur, a ten moduł jeszcze
nie istnieje. Po jego wdrożeniu sekcja otrzyma dane z API powiązane z tym pojazdem.

Takie rozdzielenie chroni znaczenie danych: opis usterki mówi, co klient zgłosił,
a faktura potwierdza, jakie prace warsztat rzeczywiście wykonał.
