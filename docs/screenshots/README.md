# Zrzuty ekranu do README

Ten folder jest przeznaczony na obrazy prezentujące aplikację w głównym `README.md`.
Przed wykonaniem zrzutów uruchom aplikację z profilem `local` i użyj danych
demonstracyjnych. Nie pokazuj prawdziwych danych osobowych ani haseł wpisanych
w formularzach.

## Stan

Pełny scenariusz demonstracyjny wykonano 17.09.2026. Faktura z tego przebiegu została
wyrenderowana i zapisana jako `06-invoice-preview.png`. Trzy kadry interfejsu do
głównego README pozostają do ręcznego zapisania z przeglądarki; ich dokładna
zawartość jest opisana poniżej.

## Lista zrzutów

Do głównego README przygotuj cztery obrazy pokazujące tę samą naprawę od zgłoszenia
do faktury. Pozostałe widoki z tabeli poniżej są uzupełnieniem do `docs/README.md`.

| Etap w głównym README | Plik | Widok i zawartość |
| --- | --- | --- |
| Zgłoszenie wizyty | `booking-request.png` | `/appointments`: wybrany pojazd, dostępny dzień i opis problemu z hamulcami |
| Potwierdzenie terminu | `03-staff-schedule.png` | `/staff/schedule`: grafik zawierający zgłoszenie tego pojazdu |
| Zakończenie naprawy | `repair-completion.png` | `/staff/appointments`: rozwinięty formularz „Praca zakończona”, opis prac oraz pozycje robocizny i części |
| Odbiór i faktura | `06-invoice-preview.png` | Gotowe: PDF pobrany po oznaczeniu odbioru tego samego auta |

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
- W głównym README pod każdym etapem znajduje się widoczny placeholder i komentarz
  HTML z gotowym odnośnikiem do obrazu. Po zapisaniu pliku usuń placeholder oraz
  znaczniki komentarza `<!--` i `-->`, pozostawiając sam odnośnik Markdown.
- W rozszerzonej dokumentacji możesz zastąpić tabelę placeholderów dodatkowymi
  obrazami. Stosuj tam ścieżki względem `docs`, np.
  `![Panel klienta](screenshots/02-client-appointments.png)`.
