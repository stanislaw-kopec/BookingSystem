# Profil klienta — przepływ krok po kroku

Profil jest dostępny tylko dla zalogowanego klienta. Konto odpowiada za login,
hasło i rolę, natomiast profil przechowuje dane używane przez warsztat.
Takie rozdzielenie ułatwi późniejsze powiązanie pojazdów, rezerwacji i faktur.

## 1. Konto a profil

`AppUser` istnieje od chwili rejestracji i służy Spring Security.
`ClientProfile` powstaje dopiero przy pierwszym zapisie formularza.
Relacja w bazie wygląda tak:

```text
app_users                         client_profiles
┌──────────────┐        1 : 0..1  ┌────────────────────┐
│ id           │◄─────────────────│ user_id UNIQUE     │
│ username     │                  │ first_name         │
│ email        │                  │ contact_email      │
│ password_hash│                  │ address_line       │
│ role         │                  │ dane firmy...      │
└──────────────┘                  └────────────────────┘
```

Jedno konto klienta może nie mieć jeszcze profilu albo mieć dokładnie jeden.
Ograniczenie `UNIQUE` na `user_id` zabezpiecza to również w PostgreSQL.

Migracja [V5__create_client_profiles.sql](../backend/src/main/resources/db/migration/V5__create_client_profiles.sql)
dodaje tabelę bez zmiany wcześniejszych kont i katalogu usług.

## 2. Dlaczego endpoint kończy się na /me?

Frontend korzysta z:

```http
GET /api/profile/me
PUT /api/profile/me
```

Nie wysyła `userId`. [ClientProfileController.java](../backend/src/main/java/pl/autoserwis/profile/ClientProfileController.java)
pobiera login z obiektu `Authentication`, a serwis odnajduje odpowiednie konto.
Klient nie może więc zmienić adresu żądania tak, aby wskazać profil innej osoby.

Spring Security dopuszcza do tych endpointów wyłącznie rolę `CLIENT`.
Sprawdzenie po stronie Reacta decyduje tylko o widoczności sekcji,
natomiast rzeczywistą granicę dostępu egzekwuje backend.

## 3. Pierwszy odczyt i zapis

Jeśli profil jeszcze nie istnieje, GET zwraca:

```json
{
  "configured": false,
  "firstName": "",
  "lastName": "",
  "contactEmail": "email-z-konta@example.com",
  "hasCompanyData": false
}
```

Odpowiedź zawiera również pozostałe puste pola. Dzięki temu frontend zawsze
otrzymuje jeden kształt danych i nie traktuje braku profilu jako błędu 404.
E-mail konta jest wartością początkową, którą klient może zmienić jako e-mail kontaktowy.

PUT działa jak operacja **upsert**: tworzy rekord przy pierwszym zapisie,
a później aktualizuje ten sam rekord. Pole `configured=true` informuje frontend,
że formularz był już poprawnie zapisany.

[ClientProfileService.java](../backend/src/main/java/pl/autoserwis/profile/ClientProfileService.java)
wykonuje tę decyzję w transakcji. Najpierw wyszukuje profil przez `user_id`,
następnie tworzy encję albo aktualizuje znalezioną.

## 4. Walidacja zależna od przełącznika

Podstawowe dane są wymagane przez Bean Validation w
[ClientProfileRequest.java](../backend/src/main/java/pl/autoserwis/profile/dto/ClientProfileRequest.java):

- imię i nazwisko,
- numer telefonu,
- kontaktowy adres e-mail,
- ulica i numer,
- kod pocztowy,
- miejscowość.

Dane firmy zależą od `hasCompanyData`. Pojedyncza adnotacja na polu nie zna
całego kontekstu formularza, dlatego tę regułę sprawdza serwis.
Jeśli przełącznik ma wartość `true`, wymagane są nazwa firmy, NIP,
adres rozliczeniowy, kod pocztowy i miejscowość.

Gdy klient zapisze `hasCompanyData=false`, backend ustawia pola firmy na `null`.
Nie ufa ukrytym wartościom przesłanym przez przeglądarkę. Chroni to przed
pozostawieniem nieaktualnych danych rozliczeniowych.

## 5. Struktura Reacta

Trasy aplikacji definiuje [App.tsx](../frontend/src/App.tsx). React Router dopasowuje
adres `/` do strony startowej, a `/profil` do [ProfilePage.tsx](../frontend/src/pages/ProfilePage.tsx).
Komponent [RequireClient.tsx](../frontend/src/features/auth/components/RequireClient.tsx)
czeka na odczyt sesji i wpuszcza na tę trasę wyłącznie użytkownika z rolą `CLIENT`.
Jest to zabezpieczenie interfejsu; backend niezależnie sprawdza rolę przy każdym żądaniu.

Wspólny nagłówek, logowanie i stopkę zawiera
[AppLayout.tsx](../frontend/src/components/layout/AppLayout.tsx). `Outlet` w tym komponencie
oznacza miejsce, w którym React Router wyświetla aktualnie wybraną stronę.

Kod samego formularza znajduje się w `frontend/src/features/profile`:

```text
profile/
├── types.ts
├── api/
│   └── profileApi.ts
├── components/
│   ├── ClientProfileSection.tsx
│   └── ProfileForm.tsx
└── profile.css
```

[types.ts](../frontend/src/features/profile/types.ts) opisuje dane wejściowe
i odpowiedź. [profileApi.ts](../frontend/src/features/profile/api/profileApi.ts)
zna adresy API oraz sprawdza odpowiedź w trakcie działania programu.

[ClientProfileSection.tsx](../frontend/src/features/profile/components/ClientProfileSection.tsx)
odpowiada za pobranie, stany ładowania, błąd, zapis i komunikat sukcesu.
[ProfileForm.tsx](../frontend/src/features/profile/components/ProfileForm.tsx)
odpowiada za pola oraz utworzenie obiektu przekazywanego do zapisu.

Podział jest ważny: formularz nie musi wiedzieć, pod jakim adresem znajduje się API,
a warstwa API nie zajmuje się wyglądem strony.

## 6. Jeden obiekt stanu formularza

Formularz ma wiele pól, dlatego zamiast osobnego `useState` dla każdego
przechowuje jeden obiekt:

```tsx
const [form, setForm] = useState<ClientProfileInput>(initialValues)

function update<K extends keyof ClientProfileInput>(
  field: K,
  value: ClientProfileInput[K],
) {
  setForm(current => ({ ...current, [field]: value }))
}
```

`keyof ClientProfileInput` oznacza: do funkcji można przekazać wyłącznie nazwę
istniejącego pola. Typ `ClientProfileInput[K]` pilnuje, aby pole tekstowe
otrzymało tekst, a `hasCompanyData` wartość logiczną.

Operator `...current` kopiuje dotychczasowe pola, a `[field]: value`
nadpisuje tylko jedno. React otrzymuje nowy obiekt i wykonuje ponowne renderowanie.

Warunek:

```tsx
{form.hasCompanyData && <div>Dane firmy...</div>}
```

powoduje montowanie pól firmy dopiero po zaznaczeniu przełącznika.
Jest to renderowanie warunkowe, a nie osobna strona.

## 7. Przepływ danych

```text
logowanie klienta
→ menu prowadzi do /profil
→ RequireClient sprawdza rolę CLIENT
→ ProfilePage montuje ClientProfileSection
→ GET /api/profile/me
→ ProfileForm otrzymuje dane przez props
→ klient zmienia pola
→ PUT /api/profile/me + CSRF
→ backend ustala właściciela z sesji
→ walidacja i zapis w PostgreSQL
→ odpowiedź odświeża formularz
```

Po wylogowaniu `RequireClient` przekierowuje użytkownika na `/`.
`ProfilePage` i `ClientProfileSection` zostają odmontowane, więc profil znika również
ze stanu Reacta.

## 8. Jak czytać kod

Najlepsza kolejność:

1. `types.ts` — poznaj kształt danych.
2. `ProfileForm.tsx` — zobacz pola i funkcję `update`.
3. `ClientProfileSection.tsx` — prześledź pobranie i zapis.
4. `profileApi.ts` — zobacz GET oraz PUT.
5. `ClientProfileController.java` — znajdź `authentication.getName()`.
6. `ClientProfileService.java` — prześledź upsert i dane firmy.
7. `ClientProfile.java` — porównaj encję z migracją.
8. `ClientProfileIntegrationTest.java` — przeczytaj scenariusze bezpieczeństwa.

Małe ćwiczenie: zaloguj się jako `client`, zaznacz dane firmy i obserwuj,
jak React dodaje pola bez przeładowania strony. Następnie odznacz przełącznik
i sprawdź w serwisie, dlaczego ukryte wartości nie zostaną zapisane.
