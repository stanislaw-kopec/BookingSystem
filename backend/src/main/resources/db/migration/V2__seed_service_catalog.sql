-- Initial, editable catalogue based on the workshop owner's examples.
INSERT INTO service_categories (name, description) VALUES
    ('Elektryka', 'Diagnostyka i obsługa układów elektrycznych samochodu.'),
    ('Wulkanizacja', 'Wymiana, wyważanie i naprawa ogumienia.'),
    ('Mechanika', 'Obsługa i naprawa podzespołów mechanicznych.');

INSERT INTO workshop_services (category_id, name, description)
SELECT category.id, item.name, item.description
FROM (VALUES
    ('Elektryka', 'Wymiana świec', 'Wymiana świec zapłonowych.'),
    ('Elektryka', 'Wymiana cewek', 'Wymiana cewek zapłonowych.'),
    ('Wulkanizacja', 'Wymiana opon', 'Sezonowa wymiana opon.'),
    ('Wulkanizacja', 'Wyważanie kół', 'Sprawdzenie i wyważenie kół.'),
    ('Mechanika', 'Wymiana rozrządu', 'Wymiana elementów układu rozrządu.'),
    ('Mechanika', 'Wymiana oleju i filtrów', 'Podstawowa obsługa eksploatacyjna samochodu.')
) AS item(category_name, name, description)
JOIN service_categories category ON category.name = item.category_name;
