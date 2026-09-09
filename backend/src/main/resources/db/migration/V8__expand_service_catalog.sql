-- Add to existing categories without replacing user-edited names or descriptions.
-- Missing or renamed categories are intentionally left unchanged.
INSERT INTO workshop_services (category_id, name, description)
SELECT category.id, item.name, item.description
FROM (VALUES
    ('Elektryka', 'Diagnostyka komputerowa', 'Odczyt kodów usterek i analiza parametrów pracy podzespołów pojazdu.'),
    ('Elektryka', 'Kontrola i wymiana akumulatora', 'Sprawdzenie kondycji akumulatora oraz jego wymiana i dopasowanie do pojazdu.'),
    ('Elektryka', 'Diagnostyka układu ładowania', 'Kontrola alternatora, napięcia ładowania i połączeń elektrycznych.'),
    ('Elektryka', 'Naprawa układu rozruchowego', 'Sprawdzenie rozrusznika i instalacji odpowiedzialnej za uruchamianie silnika.'),
    ('Elektryka', 'Naprawa oświetlenia', 'Diagnozowanie usterek lamp, wymiana żarówek i naprawa połączeń.'),
    ('Wulkanizacja', 'Naprawa przebitej opony', 'Ocena uszkodzenia i naprawa opony, jeśli jej stan pozwala na dalsze użytkowanie.'),
    ('Wulkanizacja', 'Wymiana zaworów w kołach', 'Wymiana zaworów i sprawdzenie szczelności połączenia z obręczą.'),
    ('Wulkanizacja', 'Obsługa czujników TPMS', 'Diagnostyka, wymiana i programowanie czujników ciśnienia w oponach.'),
    ('Wulkanizacja', 'Kontrola stanu ogumienia', 'Sprawdzenie bieżnika, ciśnienia, wieku opon i widocznych uszkodzeń.'),
    ('Wulkanizacja', 'Sezonowa wymiana kompletnych kół', 'Przekładka kół letnich lub zimowych oraz kontrola ciśnienia i dokręcenia.'),
    ('Mechanika', 'Wymiana hamulców', 'Wymiana tarcz i klocków hamulcowych oraz kontrola elementów układu.'),
    ('Mechanika', 'Naprawa zawieszenia', 'Diagnostyka i wymiana zużytych amortyzatorów, wahaczy, tulei i łączników.'),
    ('Mechanika', 'Wymiana sprzęgła', 'Ocena stanu i wymiana elementów układu sprzęgła.'),
    ('Mechanika', 'Serwis układu chłodzenia', 'Kontrola szczelności, wymiana płynu chłodniczego, termostatu lub chłodnicy.'),
    ('Mechanika', 'Naprawa układu wydechowego', 'Kontrola szczelności i mocowań oraz wymiana uszkodzonych elementów wydechu.')
) AS item(category_name, name, description)
JOIN service_categories category ON lower(category.name) = lower(item.category_name)
ON CONFLICT (category_id, lower(name)) DO NOTHING;
