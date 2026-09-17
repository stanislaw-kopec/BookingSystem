# Utrwalanie faktur i obliczanie kwot

## Dlaczego zapisujemy dokument

Wcześniej każde pobranie odczytywało aktualny profil klienta oraz bieżącą datę.
Ten sam numer faktury mógł więc oznaczać innego nabywcę lub dzień wystawienia.
Teraz przejście do `COMPLETED` dla klienta z kontem zapisuje dokument w tej samej
transakcji co odbiór auta. Jeśli tworzenie PDF-u się nie powiedzie, odbiór też
nie zostanie zatwierdzony i personel może ponowić operację.

`InvoiceService` pobiera profil tylko podczas wystawiania. `InvoiceSnapshot` jest
rekordem Javy opisującym numer, daty, walutę, stawkę, dane stron i pojazdu, opis,
pozycje i podsumowania. Wersja 1 przechowuje strony i pojazd jako uporządkowane
wiersze dokumentu; nie są to relacje do edytowalnego profilu lub kartoteki.
`InvoicePdfGenerator` przyjmuje wyłącznie tę kopię danych, bez repozytoriów i zegara.

Migracja V18 tworzy `invoice_documents`. Klucz główny jest identyfikatorem zgłoszenia,
więc istnieje najwyżej jeden dokument na naprawę. Numer ma dodatkowe ograniczenie
unikalności. Dane dokumentu są zapisane w JSONB, a gotowy PDF w BYTEA. JSONB pasuje
do niezmiennej, wersjonowanej treści, której obecnie nie filtrujemy ani nie edytujemy
po pojedynczych polach. Pełny moduł księgowy mógłby potrzebować osobnych tabel pozycji.

Przechowywanie PDF-u zwiększa rozmiar bazy, ale zapewnia identyczny plik także po
zmianie logo, czcionki albo generatora. To prosty kompromis dla obecnej skali portfolio.
`@Immutable` oraz brak operacji edycji chronią dokument na poziomie aplikacji;
nie jest to zabezpieczenie przed administratorem bazy ani system archiwizacji księgowej.

## Pobieranie i starsze naprawy

Dotychczasowe adresy API i przyciski pozostają bez zmian. Serwis pojazdów sprawdza
własność, a API personelu rolę i zakończenie naprawy. Dopiero potem wywoływany jest
serwis dokumentów. Goście nie otrzymują faktur w obecnym zakresie.

Dla napraw zakończonych przed V18 dokument jest utrwalany przy pierwszym pobraniu.
Ta ścieżka wykonuje zapis w transakcji: blokuje zgłoszenie, ponownie sprawdza, czy
dokument już istnieje, i dopiero wtedy go wystawia. Dwa równoczesne pobrania zwracają
ten sam zapisany plik. Kolejne pobrania są już odczytami istniejącego dokumentu.

Nie da się odzyskać starych danych profilu ani dawniej pobranych plików z poprzedniej
wersji aplikacji. Data wystawienia takiego dokumentu jest datą pierwszego utrwalenia,
a data sprzedaży pozostaje datą odbioru. Stare naprawy bez wyszczególnionych pozycji
otrzymują jeden wiersz z opisem naprawy i zachowaną kwotą brutto.

## Zasady kwot

Źródłem ceny jest kwota jednostkowa **brutto** podana przez mechanika. `RepairAmounts`
jest wspólnym miejscem obliczeń:

1. Brutto pozycji = ilość × cena brutto, zaokrąglone `HALF_UP` do 2 miejsc.
2. Netto pozycji = brutto pozycji / 1,23, zaokrąglone `HALF_UP` do 2 miejsc.
3. VAT pozycji = brutto minus netto.
4. Kwoty całego dokumentu są sumami odpowiednich kwot pozycji.

Przykład: dwie pozycje po 1,00 zł brutto dają łącznie **1,62 zł netto + 0,38 zł VAT
= 2,00 zł brutto**. Nie przeliczamy ponownie netto od łącznego brutto, ponieważ
dałoby to 1,63 zł, niezgodne z sumą wierszy. Cena jednostkowa na PDF-ie jest brutto.

Po zaokrągleniu zarówno pojedyncza pozycja, jak i cała naprawa muszą mieć wartość
od 0,01 do 99 999 999,99 zł, zgodną z `NUMERIC(10,2)`. Dlatego 0,01 × 0,01 jest
odrzucane jako 0,00. Przekroczenie limitu iloczynu lub sumy daje HTTP 400 i błąd
walidacji, zamiast błędu bazy. Walidacja następuje przed zmianą statusu naprawy.

Stawka 23% i dane sprzedawcy są założeniami demonstracyjnymi odziedziczonymi po
poprzedniej wersji. Dokument nie potwierdza wpłaty: płatności są obsługiwane poza
systemem. Numer `MC/rok/ID-zgłoszenia` jest stabilny, ale nie jest pełną numeracją
księgową. Korekty, zmienne stawki, integracje i pełna zgodność księgowa pozostają poza zakresem.

## Co sprawdzają testy

`InvoiceIntegrationTest` używa prawdziwego PostgreSQL w Testcontainers i obejmuje
profil prywatny oraz firmowy, niezmienność PDF-u po zmianie profilu, pojazdu i zegara,
dostęp właściciela/personelu, starsze naprawy, równoczesne pobrania, wycofanie odbioru
po błędzie generatora, małe kwoty, przekroczenia limitów i dokument wielostronicowy.
Treść PDF-u jest odczytywana przez OpenPDF; kontrola wizualna uzupełnia testy tekstu.

Przykładowe pliki do lokalnego przeglądu można wygenerować z katalogu `backend`:

```powershell
mvn -B -ntp test '-Dtest=InvoiceIntegrationTest' '-Dinvoice.previewDir=target/invoice-preview'
```

Pliki trafiają do ignorowanego katalogu `target`, zawierają wyłącznie dane testowe.
