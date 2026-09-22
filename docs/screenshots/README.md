# Zrzuty ekranu do README

Ten folder jest przeznaczony na obrazy prezentujące aplikację w głównym `README.md`.
Przed wykonaniem zrzutów uruchom aplikację z profilem `local` i użyj danych
demonstracyjnych. Nie pokazuj prawdziwych danych osobowych ani haseł wpisanych
w formularzach.

## Stan

Komplet czterech obrazów do głównego README jest gotowy. Fakturę z pełnego scenariusza
demonstracyjnego zapisano 17.09.2026, a trzy kadry interfejsu wykonano 22.09.2026
z danych profilu `local`. Formularzy widocznych na zrzutach nie wysłano.

## Lista zrzutów

Główne README używa czterech obrazów przedstawiających etapy procesu od zgłoszenia
do faktury. Pozostałe widoki z tabeli poniżej mogą w przyszłości uzupełnić
`docs/README.md`.

| Etap w głównym README | Plik | Widok i zawartość |
| --- | --- | --- |
| Zgłoszenie wizyty | `booking-request.png` | `/appointments`: wybrany pojazd, dostępny dzień i opis problemu z hamulcami |
| Potwierdzenie terminu | `03-staff-schedule.png` | `/staff/schedule`: tydzień z aktywnymi zgłoszeniami i zamkniętymi dniami |
| Zakończenie naprawy | `repair-completion.png` | `/staff/appointments`: rozwinięty formularz „Praca zakończona”, opis prac oraz pozycje robocizny i części |
| Odbiór i faktura | `06-invoice-preview.png` | PDF pobrany po oznaczeniu odbioru auta |

## Dodatkowe widoki do dokumentacji technicznej

| Plik | Widok | Zalecana zawartość |
| --- | --- | --- |
| `01-home-page.png` | Strona główna | Nagłówek z logo, sekcja o warsztacie i początek katalogu usług |
| `02-client-appointments.png` | `/my-appointments` | Kilka statusów wizyt, filtr statusu i sortowanie |
| `03-staff-schedule.png` | `/staff/schedule` | Cały tydzień grafiku z kilkoma zgłoszeniami |
| `04-repair-details.png` | `/staff/appointments/{id}` | Informacje o pojeździe, opis usterki i historia napraw |
| `05-admin-panel.png` | `/admin/accounts` | Filtry kont, role i dostępne akcje administratora |
| `06-invoice-preview.png` | Pobrana faktura PDF | Logo, pojazd, robocizna, części oraz podsumowanie netto i brutto |

## Wskazówki

- Użyj jednego rozmiaru okna, najlepiej około 1440 × 900 pikseli.
- Kadruj zawartość aplikacji bez paska zakładek, pulpitu i innych programów.
- Zachowaj ten sam motyw na wszystkich obrazach.
- Dla faktury użyj wyłącznie danych demonstracyjnych.
- W rozszerzonej dokumentacji możesz zastąpić tabelę placeholderów dodatkowymi
  obrazami. Stosuj tam ścieżki względem `docs`, np.
  `![Panel klienta](screenshots/02-client-appointments.png)`.
