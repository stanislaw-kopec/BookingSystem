const messages: Record<string, string> = {
  'Capacity cannot be negative.': 'Liczba miejsc nie może być ujemna.',
  'Capacity can be at most 20.': 'Można ustawić najwyżej 20 miejsc.',
  'Default daily capacity must be greater than 0.': 'Domyślna liczba miejsc musi wynosić co najmniej 1.',
  'Default daily capacity can be at most 20.': 'Domyślna liczba miejsc może wynosić najwyżej 20.',
  'Booking horizon must be at least 7 days.': 'Horyzont rezerwacji musi wynosić co najmniej 7 dni.',
  'Booking horizon can be at most 180 days.': 'Horyzont rezerwacji może wynosić najwyżej 180 dni.',
  'A closed day must have 0 places.': 'Dzień zamknięty musi mieć 0 miejsc.',
  'An open day must have at least 1 place.': 'Dzień otwarty musi mieć co najmniej 1 miejsce.',
  'Workday end time must be later than start time.': 'Godzina zamknięcia musi być późniejsza od godziny otwarcia.',
  'Capacity cannot be lower than active appointment requests. Reschedule or cancel them first.': 'Limit jest niższy niż liczba aktywnych zgłoszeń. Najpierw przełóż lub odwołaj odpowiednie wizyty.',
  'This username is already taken.': 'Ten login jest już zajęty.',
  'An account with this email already exists.': 'Konto z tym adresem e-mail już istnieje.',
  'You already have a vehicle with this registration number.': 'Masz już pojazd z tym numerem rejestracyjnym.',
  'You already have a vehicle with this VIN.': 'Masz już pojazd z tym numerem VIN.',
  'Visit day must be in the future.': 'Wybierz przyszły dzień przyjęcia auta.',
  'Visit day must fit within the current booking horizon.': 'Wybierz dzień z dostępnego zakresu kalendarza.',
  'This day is unavailable in the workshop schedule.': 'Warsztat nie przyjmuje aut w tym dniu. Wybierz inny dzień.',
  'Quantity can have at most 2 decimal places.': 'Ilość może mieć najwyżej dwa miejsca po przecinku.',
  'Gross price can have at most 2 decimal places.': 'Cena brutto może mieć najwyżej dwa miejsca po przecinku.',
  'Enter a valid phone number.': 'Podaj poprawny numer telefonu (7–30 znaków).',
  'Enter a valid email address.': 'Podaj poprawny adres e-mail.',
  'Enter a phone number or an email address.': 'Podaj telefon lub adres e-mail.',
  'Current password is incorrect.': 'Obecne hasło jest nieprawidłowe.',
  'Passwords do not match.': 'Hasła nie są takie same.',
  'Password is too long after encoding.': 'Hasło jest zbyt długie. Skróć je lub użyj mniej znaków specjalnych.',
  'Username may contain ASCII letters, digits, dot, hyphen and underscore.': 'Login może zawierać litery bez polskich znaków, cyfry, kropkę, myślnik i podkreślenie.',
  'Registration number contains invalid characters.': 'Numer rejestracyjny może zawierać litery, cyfry, spacje i myślniki.',
  'VIN must have 17 characters and cannot contain I, O or Q.': 'VIN musi mieć 17 znaków, bez liter I, O oraz Q.',
  'Production year cannot be earlier than 1886.': 'Rok produkcji nie może być wcześniejszy niż 1886.',
  'Quantity must be greater than 0.': 'Ilość musi wynosić co najmniej 0,01.',
  'Gross price must be greater than 0.': 'Cena brutto musi wynosić co najmniej 0,01 zł.',
  'Quantity can have at most 6 integer digits and 2 decimal places.': 'Podaj ilość do 999 999,99, z najwyżej dwoma miejscami po przecinku.',
  'Gross price can have at most 8 integer digits and 2 decimal places.': 'Podaj cenę do 99 999 999,99 zł, z najwyżej dwoma miejscami po przecinku.',
  'Rounded item gross amount must be between 0.01 and 99999999.99.': 'Wartość pozycji po zaokrągleniu musi wynosić od 0,01 do 99 999 999,99 zł brutto. Sprawdź ilość i cenę.',
  'Total repair gross amount must be between 0.01 and 99999999.99.': 'Suma naprawy musi wynosić od 0,01 do 99 999 999,99 zł brutto.',
  'Add at least one repair item.': 'Dodaj co najmniej jedną pozycję naprawy.',
  'One repair order can have at most 30 items.': 'Naprawa może zawierać najwyżej 30 pozycji.',
  'Complete the repair item.': 'Uzupełnij tę pozycję naprawy.',
}

// This is presentation of the API validation contract, not a language switcher.
export function validationMessage(message: string): string {
  if (messages[message]) return messages[message]
  const maximum = message.match(/can have at most (\d+) characters\./)
  if (maximum) return `Wpisz najwyżej ${maximum[1]} znaków.`
  const range = message.match(/must be between (\d+) and (\d+) characters\./)
  if (range) return `Wpisz od ${range[1]} do ${range[2]} znaków.`
  const minimum = message.match(/must have at least (\d+) characters\./)
  if (minimum) return `Wpisz co najmniej ${minimum[1]} znaków.`
  const year = message.match(/^Production year cannot be later than (\d+)\./)
  if (year) return `Rok produkcji nie może być późniejszy niż ${year[1]}.`
  if (/^(Enter|Repeat|Describe) /.test(message)) return 'Uzupełnij to pole.'
  if (/^Select /.test(message)) return 'Wybierz poprawną wartość.'
  return 'Sprawdź wartość tego pola.'
}
