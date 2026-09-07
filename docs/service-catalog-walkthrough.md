# Katalog usług — nauka krok po kroku

Ten etap łączy bazę PostgreSQL, API Springa i stronę Reacta. Zacznij od
uruchomienia aplikacji według [README](../README.md), a następnie przejdź poniższą
kolejność. Nie musisz poznawać wszystkich plików naraz.

## 1. Jedna strona, wiele komponentów

Komponent Reacta to funkcja opisująca fragment interfejsu za pomocą JSX.
Może przedstawiać przycisk, kartę kategorii albo całą stronę. Umieszczenie
`<ServicesSection />` w innym komponencie powoduje wyświetlenie sekcji w tym
miejscu. Nie otwiera nowej strony.
Zobacz [wprowadzenie do komponentów w dokumentacji Reacta](https://react.dev/learn/your-first-component).

Nasza strona składa się tak:

```text
App
└── AuthProvider
    └── HomePage
        ├── SiteHeader
        ├── WorkshopOverview
        ├── ServicesSection
        │   └── ServiceCategoryCard × liczba kategorii
        ├── CatalogManager           tylko MECHANIC / ADMIN
        │   ├── CategoryForm
        │   └── ServiceForm
        └── AuthDialog               po otwarciu logowania
```

`HomePage` jest miejscem składania strony. Opis warsztatu występuje przed
ofertą, bo `<WorkshopOverview />` stoi przed `<ServicesSection />`.
Link `href="#services"` przewija do sekcji mającej `id="services"`.
Do samego przewijania nadal wystarcza HTML. React Router został później dodany
do obsługi osobnych podstron, takich jak `/profile`.

## 2. Dlaczego takie foldery?

W [frontend/src](../frontend/src) kod jest pogrupowany według funkcji aplikacji:

| Miejsce | Odpowiedzialność |
| --- | --- |
| `pages/HomePage.tsx` | Składa całą stronę i łączy katalog z panelem edycji. |
| `components/layout/SiteHeader.tsx` | Nagłówek i menu, które mogą być wspólne dla kolejnych stron. |
| `features/workshop` | Opis, lokalizacja i informacje o warsztacie. |
| `features/services` | Wszystko dotyczące katalogu usług. |
| `features/auth` | Logowanie, bieżący użytkownik i wylogowanie. |
| `api/apiClient.ts` | Wspólna obsługa żądań HTTP, błędów i CSRF. |

W `features/services` znajdują się:

```text
services/
├── types.ts
├── api/
│   └── servicesApi.ts
├── hooks/
│   └── useServiceCatalog.ts
├── components/
│   ├── ServiceCategoryCard.tsx
│   ├── ServicesSection.tsx
│   ├── CatalogManager.tsx
│   ├── CategoryForm.tsx
│   └── ServiceForm.tsx
└── services.css
```

Przy zmianie katalogu pracujesz głównie w jednym folderze. Jednocześnie wygląd
karty, pobieranie danych i formularze mają osobne pliki, ponieważ mają różne zadania.
`.tsx` oznacza TypeScript z JSX, natomiast `.ts` zawiera kod bez JSX.

## 3. Dane: najpierw typy, potem API

Otwórz [types.ts](../frontend/src/features/services/types.ts).
`ServiceCategory` opisuje kategorię: identyfikator, nazwę, opis oraz tablicę usług.
`WorkshopService` opisuje usługę, w tym `categoryId`, czyli kategorię, do której należy.

Przykładowy fragment odpowiedzi `GET /api/services` wygląda tak
(identyfikatory są przykładowe):

```json
[
  {
    "id": 1,
    "name": "Elektryka",
    "description": "Obsługa układów elektrycznych.",
    "services": [
      {
        "id": 2,
        "categoryId": 1,
        "name": "Wymiana cewek",
        "description": "Wymiana cewek zapłonowych."
      }
    ]
  }
]
```

Dzięki temu frontend otrzymuje całą ofertę jednym żądaniem. Nie musi wysyłać
osobnego zapytania dla każdej kategorii.

W [servicesApi.ts](../frontend/src/features/services/api/servicesApi.ts)
znajdziesz funkcje komunikujące się z API. Przykładowo `getCatalog()` pobiera
ofertę, a `saveService()` wysyła dane formularza. Funkcje nie tworzą elementów strony.

TypeScript pomaga sprawdzać nasz kod podczas budowania. Odpowiedź serwera
przychodzi jednak dopiero podczas działania aplikacji, dlatego sprawdzamy też
jej rzeczywisty kształt, zanim potraktujemy ją jako listę kategorii.

## 4. Jak dane docierają do karty?

Otwórz [useServiceCatalog.ts](../frontend/src/features/services/hooks/useServiceCatalog.ts).

To własny hook, czyli funkcja wykorzystująca mechanizmy Reacta do obsługi
konkretnego zadania. Tutaj przechowuje katalog i zarządza jego pobieraniem:

- `categories` — aktualne dane.
- `isLoading` — informacja, czy trwa pobieranie.
- `error` — komunikat po nieudanym pobraniu.
- `reload()` — uruchamia ponowne pobranie.

`useState` przechowuje wartości pomiędzy renderowaniami. Wywołanie funkcji
takiej jak `setCategories(result)` informuje Reacta o zmianie danych,
po czym React aktualizuje potrzebne elementy widoku.

`useEffect` uruchamia pobranie przy wejściu na stronę i po żądaniu odświeżenia.
`AbortController` anuluje nieaktualne żądanie po opuszczeniu widoku lub
uruchomieniu kolejnego pobrania. Starsza odpowiedź nie nadpisuje wtedy nowszej.

`HomePage` wywołuje hook i przekazuje wynik do `ServicesSection`.
Ta sekcja pokazuje ładowanie, błąd z przyciskiem ponowienia, pustą ofertę
albo karty kategorii.

## 5. Props, map i key na jednym przykładzie

Otwórz [ServiceCategoryCard.tsx](../frontend/src/features/services/components/ServiceCategoryCard.tsx).
Ten komponent otrzymuje kategorię i ją wyświetla. Nie pobiera jej samodzielnie.

W [ServicesSection.tsx](../frontend/src/features/services/components/ServicesSection.tsx)
zobaczysz użycie odpowiadające temu fragmentowi:

```tsx
{categories.map((category) => (
  <ServiceCategoryCard key={category.id} category={category} />
))}
```

`map` przechodzi po tablicy kategorii i dla każdej tworzy kartę.
Lewa strona `category={category}` to nazwa właściwości komponentu,
a prawa to przekazywana wartość. Takie dane wejściowe nazywamy **props**.
Komponent potomny traktuje je jako dane do odczytu.
Zobacz [przekazywanie props w React](https://react.dev/learn/passing-props-to-a-component).

`key` pomaga Reactowi rozpoznać element listy po dodaniu, usunięciu czy zmianie
kolejności. Używamy identyfikatora z bazy, ponieważ pozostaje taki sam po zmianie nazwy.
`key` jest specjalną informacją dla Reacta i nie staje się zwykłym propsem karty.

## 6. Co się dzieje po kliknięciu „Zapisz usługę”?

Prześledź [ServiceForm.tsx](../frontend/src/features/services/components/ServiceForm.tsx)
oraz [CatalogManager.tsx](../frontend/src/features/services/components/CatalogManager.tsx):

1. Formularz przechowuje wpisywaną nazwę, opis i wybraną kategorię w swoim stanie.
2. `value` i `onChange` łączą pole formularza z tym stanem.
3. Po zatwierdzeniu formularz wywołuje przekazaną funkcję `onSave(input)`.
4. `CatalogManager` wybiera dodawanie lub aktualizację i wywołuje `servicesApi`.
5. Spring sprawdza rolę, dane i reguły biznesowe, a następnie zapisuje dane w bazie.
6. Po sukcesie panel wywołuje `onChanged()`, czyli przekazane z `HomePage` odświeżenie katalogu.
7. Lista publiczna i lista do edycji otrzymują nowe dane.

Formularz odpowiada za pola, a panel za wykonanie zapisu i jego rezultat.
W przypadku błędu wpisane dane pozostają w formularzu. Podczas zapisu
przyciski są blokowane, aby nie wysłać przypadkowo kilku takich samych operacji.

Publiczna oferta i panel korzystają z **jednego stanu katalogu w HomePage**.
To przykład umieszczenia współdzielonego stanu u najbliższego wspólnego rodzica.
Taką zasadę opisuje [dokumentacja o współdzieleniu stanu](https://react.dev/learn/sharing-state-between-components).

Zmiana `key` formularza przy wyborze innej usługi albo po udanym dodaniu
tworzy nową instancję formularza. Dzięki temu jego początkowe wartości pasują
do wybranej usługi lub znów są puste.

## 7. Co robi Spring?

Kod oferty jest w [pakiecie offering](../backend/src/main/java/pl/autoserwis/offering).

| Plik lub grupa | Zadanie |
| --- | --- |
| `ServiceCatalogController` | Mapuje adresy i metody HTTP na operacje. |
| `ServiceCatalogService` | Sprawdza duplikaty, powiązania, brak zasobu i reguły usuwania; zarządza transakcjami. |
| `ServiceCategoryRepository`, `WorkshopServiceRepository` | Pobierają i zapisują dane przez JPA. |
| `ServiceCategory`, `WorkshopService` | Encje odwzorowujące rekordy bazy. |
| `dto` | Osobne struktury danych wejściowych i odpowiedzi API. |

Baza ma relację **jedna kategoria → wiele usług**.
Kolumna `category_id` w tabeli `workshop_services` wskazuje kategorię.
Klucz obcy nie pozwala zapisać usługi dla nieistniejącej kategorii.

Migracje w `backend/src/main/resources/db/migration` tworzą tabele i ofertę
startową. Hibernate sprawdza zgodność encji ze schematem; nie zmienia go automatycznie.

Walidacja ma kilka poziomów: formularz pomaga użytkownikowi,
Bean Validation odrzuca nieprawidłowe wejście, serwis sprawdza reguły,
a ograniczenia bazy chronią spójność również przy równoczesnych zapisach.
Na przykład duplikat nazwy daje HTTP 409, a brak wymaganej nazwy — 400.

DTO oddzielają format API od encji. Dzięki temu odpowiedź nie zawiera
przypadkowych pól ani wewnętrznych powiązań JPA.

## 8. Dlaczego dodaliśmy logowanie?

Uprawnienia do edycji wymagają rozpoznania użytkownika na backendzie.
Samo schowanie przycisku w React nie wystarcza, bo żądanie HTTP można wysłać poza stroną.

Minimalny mechanizm tego etapu to Spring Security, konta zapisane w bazie,
hasła BCrypt oraz sesja w ciasteczku HttpOnly. Role MECHANIC i ADMIN mogą
zmieniać katalog; CLIENT i gość tylko go oglądają.

`AuthProvider` udostępnia informację o zalogowaniu komponentom, które jej potrzebują,
a `useAuth()` pozwala ją odczytać. Jest to osobny stan od katalogu usług.

`apiClient.ts` pobiera aktualny token CSRF przed operacją zmieniającą dane.
Ma to znaczenie również po zalogowaniu, ponieważ token wcześniejszej sesji
przestaje obowiązywać. Mechanizm jest opisany w
[dokumentacji ochrony CSRF Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

Dane kont lokalnych są w README. Rejestracji, profili i odzyskiwania hasła
jeszcze nie implementowaliśmy.

## 9. Gdzie później zmieniać wygląd i treści?

- [workshopInfo.ts](../frontend/src/features/workshop/workshopInfo.ts) — opis warsztatu, historia i adres. Obecnie prawdziwe dane czekają na uzupełnienie.
- [index.css](../frontend/src/index.css) — wspólne kolory, czcionki, przyciski i pola.
- [App.css](../frontend/src/App.css) — układ strony, nagłówek i okno logowania.
- [services.css](../frontend/src/features/services/services.css) — karty oraz panel katalogu.

Zastosowaliśmy zwykły CSS, bez dodatkowej biblioteki komponentów. Zmienne
zapisane na początku `index.css` pozwalają zmienić podstawowe kolory w jednym miejscu.

## 10. Małe ćwiczenia

1. Zmień opis w `workshopInfo.ts` i zobacz, który komponent go wyświetla.
2. W karcie kategorii dodaj tekst pokazujący liczbę usług: `{category.services.length}`.
3. Zaloguj się jako mechanik, dodaj własną kategorię i usługę, a potem odśwież stronę.
4. Wyloguj się i sprawdź, że oferta nadal jest widoczna, a panel edycji znika.

Na początek najlepiej czytać kod w kolejności:
**types → ServiceCategoryCard → ServicesSection → useServiceCatalog → HomePage**.
Formularze i logowanie poznaj po zrozumieniu wyświetlania oferty.
