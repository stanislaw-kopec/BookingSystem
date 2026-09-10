# Umawianie wizyty — przewodnik po implementacji

Ten etap łączy publiczny kalendarz, dwa warianty formularza Reacta i obsługę
zgłoszeń przez personel. Najważniejsze rozróżnienie brzmi: klient najpierw wysyła
**zgłoszenie**, a warsztat dopiero później potwierdza **wizytę**.

## 1. Przepływ statusów

```text
PENDING ───────────────> CONFIRMED ───────────────> READY_FOR_PICKUP ───────────────> COMPLETED
    │                         ▲
    ├───────────────> REJECTED
    ├──────────────> CANCELLED
    │
    └──> TIME_PROPOSED ───────┘
```

- `PENDING` oznacza zgłoszenie oczekujące na decyzję warsztatu.
- `CONFIRMED` oznacza potwierdzony dzień przyjęcia auta.
- `REJECTED` kończy zgłoszenie i zwalnia miejsce w danym dniu.
- `CANCELLED` oznacza wizytę odwołaną przez klienta i zwalnia miejsce w danym dniu.
- `TIME_PROPOSED` oznacza, że personel wskazał inny dzień. Klient z kontem
  potwierdza go w swoim panelu. Dla gościa personel zapisuje potwierdzenie po
  kontakcie telefonicznym lub mailowym.
- `READY_FOR_PICKUP` oznacza, że mechanik zakończył naprawę, opisał wykonane prace
  i podał kwotę brutto do zapłaty przy odbiorze auta. Płatność odbywa się poza systemem.
- `COMPLETED` oznacza, że klient odebrał samochód, a zgłoszenie jest widoczne
  w historii napraw pojazdu.

Przejścia sprawdza backend. Ukrycie przycisku w Reacie poprawia interfejs, ale nie
chroni danych przed ręcznie przygotowanym żądaniem HTTP.

## 2. Klient z kontem i gość

Zalogowany klient wysyła `vehicleId`, `visitDate` i opis. Nie wysyła `clientId`, danych
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

## 4. Dostępność dni

[AppointmentSchedule.java](../backend/src/main/java/pl/autoserwis/appointment/AppointmentSchedule.java)
jest jednym miejscem z zasadami pierwszej wersji:

- strefa `Europe/Warsaw`,
- poniedziałek–piątek,
- dzienna pojemność 4 aktywnych zgłoszeń,
- techniczny początek dnia roboczego 08:00,
- 30-dniowy horyzont.

API zwraca datę, dzienną pojemność, liczbę wolnych miejsc oraz techniczny początek
i koniec dnia roboczego z przesunięciem strefy czasowej. Frontend tylko prezentuje
odpowiedź. Nie uznaje dnia za wolny na podstawie własnego zegara ani lokalnej tablicy.

## 5. Ochrona przed przekroczeniem dziennego limitu

Odczyt kalendarza i zapis zgłoszenia są dwiema osobnymi operacjami. Dwie osoby mogą
zobaczyć ostatnie wolne miejsce w danym dniu, zanim pierwsza z nich naciśnie
„Wyślij zgłoszenie”.

Backend zakłada transakcyjną blokadę na wybrany dzień i ponownie liczy aktywne
zgłoszenia ze statusami `PENDING`, `TIME_PROPOSED` albo `CONFIRMED`. Dzięki temu
równoczesne zapisy nie powinny przekroczyć limitu 4 aut dziennie. Przegrane żądanie
otrzymuje HTTP 409, a frontend odświeża kalendarz bez usuwania opisu usterki.

Odrzucenie zmienia status na `REJECTED`, więc miejsce wraca do dostępnej pojemności.
Propozycja personelu zmienia bieżący dzień w jednej transakcji: poprzedni zostaje
zwolniony, a nowy zajęty.

Odwołanie przez klienta zmienia status na `CANCELLED`. Taki status także nie blokuje
miejsca, więc dzień może wrócić do kalendarza jako dostępny.

Zakończenie naprawy zmienia status na `READY_FOR_PICKUP`. Ten status pokazuje, że
auto czeka na odbiór i płatność na miejscu, ale nie blokuje już kalendarza przyjęć.
Po odebraniu samochodu personel zmienia status na `COMPLETED`; taki wpis pojawia się
w historii napraw pojazdu.

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
POST /api/staff/appointments/{id}/propose-time              propozycja dnia
POST /api/staff/appointments/{id}/confirm-proposed          potwierdzenie gościa
POST /api/staff/appointments/{id}/complete-repair           zakończenie naprawy
POST /api/staff/appointments/{id}/mark-picked-up            potwierdzenie odbioru auta
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
w tygodniowym widoku dni od poniedziałku do piątku. `StaffAppointmentsPage` pod
`/staff/appointments` pozostaje miejscem podejmowania decyzji o zgłoszeniach.
W tym samym panelu przy statusie `CONFIRMED` pojawia się akcja „Praca zakończona”,
która zapisuje opis wykonanych prac i kwotę brutto do zapłaty. Przy statusie
`READY_FOR_PICKUP` mechanik widzi przycisk „Samochód został odebrany”, który ustawia
status `COMPLETED`. Szczegóły pojazdu pobierają historię z
`GET /api/vehicles/{vehicleId}/repair-history`, czyli z zakończonych zgłoszeń klienta.
Każdy wpis historii ma przycisk „Pobierz fakturę”, który pobiera PDF z
`GET /api/vehicles/{vehicleId}/repair-history/{appointmentId}/invoice`. PDF używa
danych firmowych klienta, jeśli profil ma je uzupełnione, albo danych imiennych
w przeciwnym razie.

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
kalendarza dostępnych dni. Te same dni wykorzystują formularz klienta,
formularz gościa oraz proponowanie nowego dnia przez personel. Grafik personelu
korzysta z `GET /api/staff/appointments`, bo pokazuje zapisane zgłoszenia, oraz
z publicznej dostępności, żeby pokazać bazową pojemność dnia.

`ClientAppointmentForm` pobiera własne pojazdy. Jeśli klient doda samochód wewnątrz
formularza, komponent używa istniejącego `POST /api/vehicles`, dopisuje odpowiedź do
listy i od razu wybiera nowy pojazd.

## 8. Co pozostaje na później

Ta wersja nie wysyła e-maili ani SMS-ów, nie obsługuje świąt, wielu stanowisk,
indywidualnych limitów na konkretne dni ani przypisywania mechanika. Dzień wizyty
oznacza dzień przyjęcia auta do warsztatu, a nie czas trwania naprawy. Te reguły
można później rozbudować bez zmiany znaczenia istniejących zgłoszeń i statusów.
