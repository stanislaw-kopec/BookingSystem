# Rejestracja klienta — przepływ krok po kroku

Rejestracja jest pionowym fragmentem aplikacji: zaczyna się w formularzu Reacta,
przechodzi przez API Springa i kończy rekordem w PostgreSQL. Po sukcesie frontend
używa istniejącego logowania, dlatego nowe konto od razu otrzymuje sesję.

## 1. Podział komponentów Reacta

Okno autoryzacji składa się z trzech komponentów:

```text
AuthDialog
├── LoginForm
└── RegistrationForm
```

[AuthDialog.tsx](../frontend/src/features/auth/components/AuthDialog.tsx)
przechowuje tylko stan `mode`: `login` albo `register`. Kliknięcie zakładki
zmienia ten stan, a React renderuje odpowiedni formularz. Dzięki temu logowanie
i rejestracja nie mieszają swoich pól, błędów ani stanu wysyłania.

[RegistrationForm.tsx](../frontend/src/features/auth/components/RegistrationForm.tsx)
ma cztery kontrolowane pola. Każde pole posiada wartość w `useState`,
a `onChange` aktualizuje ją podczas pisania:

```tsx
const [email, setEmail] = useState('')

<input
  value={email}
  onChange={(event) => setEmail(event.target.value)}
/>
```

Przeglądarka wykonuje podstawową walidację przez `required`, `minLength`,
`maxLength`, `type="email"` i `pattern`. Przed wysłaniem komponent sprawdza
też, czy oba hasła są identyczne. To daje szybką informację bez żądania do serwera.

## 2. Dlaczego backend waliduje ponownie?

Kod działający w przeglądarce można pominąć i wysłać żądanie innym programem.
Dlatego formularz pomaga użytkownikowi, ale o przyjęciu danych zawsze decyduje backend.

[RegistrationRequest.java](../backend/src/main/java/pl/autoserwis/auth/dto/RegistrationRequest.java)
opisuje kontrakt wejściowy i reguły Bean Validation. Backend sprawdza format loginu,
e-mail, długość hasła i obecność każdego pola.

[RegistrationService.java](../backend/src/main/java/pl/autoserwis/auth/RegistrationService.java)
realizuje reguły zależne od kilku pól oraz danych w bazie:

1. sprawdza zgodność hasła i powtórzenia,
2. pilnuje 72-bajtowego ograniczenia BCrypt,
3. normalizuje e-mail do małych liter,
4. szuka zajętego loginu i e-maila,
5. koduje hasło przez BCrypt,
6. tworzy konto zawsze z rolą `CLIENT`.

Rola nie występuje w `RegistrationRequest`. Użytkownik nie może więc wpisać
w żądaniu `ADMIN` i nadać sobie uprawnień. Jest to decyzja serwera.

## 3. Droga żądania

Po zatwierdzeniu formularza zachodzi następujący przepływ:

```text
RegistrationForm
  → AuthProvider.register()
    → POST /api/auth/register
      → RegistrationController
        → RegistrationService
          → UserRepository
            → PostgreSQL
    → POST /api/auth/login
    → GET /api/auth/me
  → nagłówek pokazuje zalogowanego klienta
```

[authApi.ts](../frontend/src/features/auth/api/authApi.ts) zna adresy HTTP
i format danych. [AuthProvider.tsx](../frontend/src/features/auth/AuthProvider.tsx)
łączy kilka operacji w jeden proces: rejestracja, logowanie i pobranie bieżącego
użytkownika. Komponent formularza nie musi wiedzieć, jak działa sesja.

Wszystkie operacje POST przechodzą przez
[apiClient.ts](../frontend/src/api/apiClient.ts), który pobiera aktualny token CSRF.
Po logowaniu `GET /api/auth/me` zwraca login i rolę, a `AuthProvider`
aktualizuje wspólny stan użytkownika. Nagłówek reaguje na zmianę automatycznie.

## 4. Baza danych

Migracja [V4__add_user_email.sql](../backend/src/main/resources/db/migration/V4__add_user_email.sql)
dodaje kolumnę `email` do istniejącej tabeli `app_users`. Stare konta
demonstracyjne otrzymują techniczne adresy w domenie `local.invalid`.
Nowe migracje rozszerzają już utworzoną bazę bez usuwania danych.

Indeksy na `lower(username)` i `lower(email)` wymuszają unikalność bez
rozróżniania wielkości liter. Sprawdzenie w serwisie daje czytelny komunikat,
a indeks chroni dane również wtedy, gdy dwa żądania nadejdą niemal jednocześnie.

Hasło nie trafia do kolumny w jawnej postaci. `PasswordEncoder` tworzy skrót
BCrypt, który zawiera losową sól. Podczas logowania Spring porównuje wpisane hasło
z zapisanym skrótem.

## 5. Obsługa błędów

Backend zwraca wspólny obiekt błędu z mapą `fieldErrors`, na przykład:

```json
{
  "status": 409,
  "code": "REGISTRATION_CONFLICT",
  "message": "Account cannot be created.",
  "fieldErrors": {
    "email": "An account with this email already exists."
  }
}
```

`code` jest stabilny dla kodu frontendu, a tekst `message` można traktować jako
techniczny fallback. `RegistrationForm` przypisuje wiadomość do właściwego pola przez
`aria-describedby`. Czytnik ekranu może połączyć pole z błędem,
a `aria-invalid` informuje, które pole wymaga poprawy. Ogólny komunikat
pozostaje nad przyciskiem wysyłania.

Kod 400 oznacza nieprawidłowe dane, 409 zajęty login lub e-mail,
a 403 brak prawidłowego tokenu CSRF.

## 6. Jak czytać ten etap

Najłatwiejsza kolejność:

1. `RegistrationForm.tsx` — pola i obsługa przycisku.
2. `authApi.ts` — żądanie HTTP.
3. `AuthProvider.tsx` — rejestracja połączona z logowaniem.
4. `RegistrationController.java` — wejście do backendu.
5. `RegistrationRequest.java` — reguły pojedynczych pól.
6. `RegistrationService.java` — reguły biznesowe i zapis.
7. `V4__add_user_email.sql` — zmiana bazy.
8. `RegistrationIntegrationTest.java` — przykłady oczekiwanego zachowania.

Małe ćwiczenie: wpisz dwa różne hasła i sprawdź błąd generowany przez Reacta.
Następnie spróbuj ponownie z zajętym loginem `client`. Drugi błąd pochodzi
z backendu, mimo że oba są pokazane przez ten sam formularz.
