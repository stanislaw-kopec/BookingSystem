const messages: Record<string, string> = {
  make: 'Podaj poprawną markę pojazdu.',
  model: 'Podaj poprawny model pojazdu.',
  productionYear: 'Podaj poprawny rok produkcji pojazdu.',
  registrationNumber: 'Sprawdź numer rejestracyjny. Może być nieprawidłowy albo już używany.',
  vin: 'Sprawdź VIN. Musi mieć 17 znaków bez liter I, O i Q oraz nie może być już używany.',
}

export function vehicleFieldErrors(fields: Record<string, string>): Record<string, string> {
  return Object.fromEntries(Object.keys(fields).map((field) => [field, messages[field] ?? fields[field]]))
}
