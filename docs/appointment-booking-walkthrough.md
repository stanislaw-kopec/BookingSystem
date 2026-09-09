# Umawianie wizyty — przewodnik po implementacji

Ten etap łączy publiczny kalendarz, dwa warianty formularza Reacta i obsługę
zgłoszeń przez personel. Najważniejsze rozróżnienie brzmi: klient najpierw wysyła
**zgłoszenie**, a warsztat dopiero później potwierdza **wizytę**.

## 1. Przepływ statusów

```text
PENDING ───────────────> CONFIRMED
    │                         ▲
    ├───────────────> REJECTED
    ├──────────────> CANCELLED
    │
    └──> TIME_PROPOSED ───────┘
```

- `PENDING` oznacza zgłoszenie oczekujące na decyzję warsztatu.
- `CONFIRMED` oznacza potwierdzony termin.
- `REJECTED` kończy zgłoszenie i zwalnia termin.
- `CANCELLED` oznacza wizytę odwołaną przez klienta i zwalnia termin.
- `TIME_PROPOSED` oznacza, że personel wskazał inny termin. Klient z kontem
  potwierdza go w swoim panelu. Dla gościa personel zapisuje potwierdzenie po
  kontakcie telefonicznym lub mailowym.

Przejścia sprawdza backend. Ukrycie przycisku w Reacie poprawia interfejs, ale nie
chroni danych przed ręcznie przygotowanym żądaniem HTTP.

## 2. Klient z kontem i gość

Zalogowany klient wysyła `vehicleId`, termin i opis. Nie wysyła `clientId`, danych
właściciela ani statusu. [AppointmentService.java](../backend/src/main/java/pl/autoserwis/appointment/AppointmentService.java)
pobiera użytkownika z sesji, sprawdza własność pojazdu i wymaga uzupełnionego profilu.

Gość podaje imię, nazwisko, co najmniej jeden sposób kontaktu oraz dane pojazdu.
Te informacje zostają zapisane bez tworzenia konta, profilu i rekordu w `vehicles`.
Gość otrzymuje losowy numer referencyjny, ale nie ma publicznego endpointu do
przeglądania zgłoszenia.

## 3. Dlaczego zgłoszenie przechowuje kopię danych

[V7__create_appointment_requests.sql](../backend/src/main/resources/db/migration/V7__create_appointment_requests.sql)
zapisuje kopię danych kontaktowych i podstawowych danych pojazdu także dla klienta
z kontem. Relacje z klientem i pojazdem nadal pozwalają sprawdzić własność.

Kopia zachowuje stan z chwili zgłoszenia. Jeżeli klient później zmieni numer telefonu,
e-mail lub numer rejestracyjny, stare zgłoszenie nadal pokazuje dane, na podstawie
których warsztat podejmował decyzję.

## 4. Dostępność terminów

[AppointmentSchedule.java](../backend/src/main/java/pl/autoserwis/appointment/AppointmentSchedule.java)
jest jednym miejscem z zasadami pierwszej wersji:

- strefa `Europe/Warsaw`,
- poniedziałek–piątek,
- godziny 08:00–16:00,
- jednogodzinne okna,
- 30-dniowy horyzont.

API zwraca zarówno początek, jak i koniec okna z przesunięciem strefy czasowej.
Frontend tylko prezentuje odpowiedź. Nie uznaje terminu za wolny na podstawie
własnego zegara ani lokalnej tablicy.

## 5. Ochrona przed zajęciem jednego terminu dwa razy

Odczyt kalendarza i zapis zgłoszenia są dwiema osobnymi operacjami. Dwie osoby mogą
zobaczyć ten sam wolny termin, zanim pierwsza z nich naciśnie „Wyślij zgłoszenie”.

Częściowy unikalny indeks w PostgreSQL pozwala istnieć tylko jednemu zgłoszeniu
z danym `current_start_at` i aktywnym statusem `PENDING`, `TIME_PROPOSED` albo
`CONFIRMED`. Dzięki temu baza rozstrzyga także równoczesne zapisy. Przegrane żądanie
otrzymuje HTTP 409, a frontend odświeża kalendarz bez usuwania opisu usterki.

Odrzucenie zmienia status na `REJECTED`, więc indeks przestaje blokować termin.
Propozycja personelu zmienia bieżący termin w jednej transakcji: poprzedni zostaje
zwolniony, a nowy zajęty.

Odwołanie przez klienta zmienia status na `CANCELLED`. Taki status także nie blokuje
terminu, więc okno może wrócić do kalendarza.

## 6. Endpointy i uprawnienia

```text
GET  /api/appointments/availability                         publiczny kalendarz
POST /api/appointments/guest                                zgłoszenie gościa
GET  /api/appointments                                      własne zgłoszenia CLIENT
POST /api/appointments                                      nowe zgłoszenie CLIENT
POST /api/appointments/{id}/confirm-proposed                potwierdzenie CLIENT
POST /api/appointments/{id}/cancel                          odwołanie wizyty CLIENT

GET  /api/staff/appointments                                kolejka personelu
POST /api/staff/appointments/{id}/accept                    przyjęcie
POST /api/staff/appointments/{id}/reject                    odrzucenie
POST /api/staff/appointments/{id}/propose-time              propozycja terminu
POST /api/staff/appointments/{id}/confirm-proposed          potwierdzenie gościa
```

Operacje CLIENT korzystają z właściciela sesji. Operacje personelu wymagają roli
`MECHANIC` albo `ADMIN`. Każdy POST, włącznie z formularzem publicznym, przechodzi
przez ochronę CSRF obsługiwaną przez wspólny `apiClient`.

## 7. Struktura Reacta

`AppointmentsPage` pod `/appointments` wyświetla wyłącznie formularz umawiania
wizyty. `MyAppointmentsPage` pod `/my-appointments` składa osobną stronę z
`ClientAppointmentsSection`, czyli listą własnych zgłoszeń, potwierdzaniem propozycji
i odwołaniem aktywnej wizyty.
Trasę listy chroni `RequireClient`. Po wysłaniu formularza klient może przejść do niej
przez link w komunikacie sukcesu lub pozycję „Moje wizyty” w menu konta.

`StaffSchedulePage` pod `/staff/schedule` składa zakładkę „Grafik” dla MECHANIC/ADMIN.
Używa tej samej listy zgłoszeń personelu co kolejka, ale prezentuje aktywne zgłoszenia
w tygodniowej siatce poniedziałek-piątek. `StaffAppointmentsPage` pod
`/staff/appointments` pozostaje miejscem podejmowania decyzji o zgłoszeniach.

```text
appointments/
├── types.ts
├── dateTime.ts
├── api/
│   └── appointmentsApi.ts
├── hooks/
│   └── useAppointmentAvailability.ts
└── components/
    ├── AvailabilityCalendar.tsx
    ├── ClientAppointmentForm.tsx
    ├── GuestAppointmentSection.tsx
    ├── ClientAppointmentsSection.tsx
    ├── StaffAppointmentsSection.tsx
    ├── StaffScheduleSection.tsx
    ├── AppointmentDetails.tsx
    └── AppointmentStatusBadge.tsx
```

[appointmentsApi.ts](../frontend/src/features/appointments/api/appointmentsApi.ts)
oddziela komunikację HTTP od komponentów i sprawdza odpowiedzi również podczas
działania aplikacji. `useAppointmentAvailability` odpowiada za pobranie i odświeżenie
kalendarza dostępnych terminów. Te same terminy wykorzystują formularz klienta,
formularz gościa oraz proponowanie nowej godziny przez personel. Grafik personelu
korzysta z `GET /api/staff/appointments`, bo pokazuje zapisane zgłoszenia, a nie
wyliczoną dostępność.

`ClientAppointmentForm` pobiera własne pojazdy. Jeśli klient doda samochód wewnątrz
formularza, komponent używa istniejącego `POST /api/vehicles`, dopisuje odpowiedź do
listy i od razu wybiera nowy pojazd.

## 8. Co pozostaje na później

Ta wersja nie wysyła e-maili ani SMS-ów, nie obsługuje świąt, wielu stanowisk
i różnych długości napraw. Termin jest obecnie wspólnym jednogodzinnym oknem
warsztatu. Te reguły można później rozbudować bez zmiany znaczenia istniejących
zgłoszeń i statusów.
