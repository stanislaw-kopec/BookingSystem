# Mietek Customs — logo w Reacie

Logo przedstawia gumową kaczkę w czapeczce, płomienie w stylu hot roda oraz napis
„Mietek Customs”. PNG ma rozmiar 1536 × 1024 i przezroczyste tło. To grafika rastrowa,
więc do bardzo dużego druku warto w przyszłości przygotować wersję wektorową.

## Gdzie co znajduje się

| Plik | Odpowiedzialność |
| --- | --- |
| `frontend/src/assets/branding/mietek-customs-logo.png` | Właściwy plik graficzny, zapisany w repozytorium. |
| `frontend/src/features/workshop/components/WorkshopLogo.tsx` | Wspólny komponent wyświetlający logo z tekstem alternatywnym. |
| `frontend/src/features/workshop/workshopInfo.ts` | Nazwa warsztatu, opis, historia i lokalizacja. |
| `frontend/src/components/layout/SiteHeader.tsx` | Logo jako link do strony głównej. |
| `frontend/src/features/workshop/components/WorkshopOverview.tsx` | Większe logo obok wprowadzenia. |
| `frontend/src/App.css` | Rozmiar logo w nagłówku i na stronie głównej, także na telefonie. |
| `frontend/index.html` | Tytuł karty, opis strony i ikona karty. |

## Jak działa import obrazka

W `WorkshopLogo.tsx` importujemy plik:

```tsx
import logoUrl from '../../../assets/branding/mietek-customs-logo.png'
```

Zmienna `logoUrl` zawiera adres obrazka przygotowany przez Vite. Przekazujemy ją
do `src` elementu `<img>`. Przy budowaniu Vite kopiuje grafikę do wynikowego katalogu
`dist/assets` i nadaje nazwę z hashem, dzięki czemu przeglądarka rozpoznaje jej nowe wersje.
Nie odwołujemy się do ścieżki na dysku komputera autora.

`src/assets` służy tu grafikom używanym przez komponenty. W `public` trzymamy pliki,
które muszą być dostępne pod stałym adresem i nie wymagają importu. Logo jest częścią
interfejsu, dlatego umieściliśmy je w `src/assets/branding`.

Ten sam komponent wykorzystujemy z różnymi klasami CSS:

```tsx
<WorkshopLogo className="brand-logo" />
<WorkshopLogo className="workshop-hero-logo" />
```

`className` to właściwość (prop) przekazana do komponentu. Zmienia sposób wyświetlania,
a oba miejsca korzystają z jednego pliku PNG. `height: auto` zachowuje proporcje.
Nazwa w nagłówku i stopce pochodzi z `workshopInfo.name`; tytuł i opis w statycznym
`index.html` aktualizujemy osobno.

## Źródło grafiki

Wygenerowano wbudowanym narzędziem `image_gen`, bez użycia CLI. Prompt:

```text
Use case: logo-brand.
Create ONE finished original automotive workshop logo on a genuinely transparent background (alpha), not a mockup. Exact brand text: "Mietek Customs". A cheeky yellow RUBBER DUCK mascot wearing a backwards baseball cap, in classic hot-rod / kustom kulture cartoon style. Keep its recognizable bath-toy silhouette, round body and orange beak; confident playful expression. Integrate restrained flame motifs and dynamic hot-rod energy, clean bold black outlines and simple flat color areas. No actual car required. Pair the mascot with custom bold highly legible retro lettering: "Mietek" prominent and "Customs" beneath it. Balanced compact badge composition, mascot above/alongside lettering, approximately 3:2 overall artwork ratio, tight clean framing with only a small transparent safety margin. Yellow duck, orange flame accents, dark charcoal outlines, cap in dark charcoal. Legible on a light website background. Crisp professionally finished illustration, vector-like shapes but deliver raster image. No extra text, slogan, date, watermarks, checkerboard pattern, background rectangle, scene, product or sheet of multiple options. Spell Mietek correctly M-I-E-T-E-K.
```
