-- The house. Fixed id so the website can reference it.
-- PLACEHOLDER values (name, capacity, prices): replace with the real ones before launch
-- (via a new migration now, via the admin dashboard from phase 5).
INSERT INTO property (id, slug, name, max_guests, base_nightly_price, cleaning_fee, currency, min_nights, time_zone)
VALUES ('6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b', 'tika-home', 'Tika Home Marrakech', 6, 120.00, 40.00, 'EUR', 2,
        'Africa/Casablanca');

INSERT INTO price_rule (id, property_id, name, start_date, end_date, nightly_price, min_nights, priority)
VALUES ('0b7d6c55-2f0e-4c41-8f1d-5b0a3c9e7d11', '6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b',
        'Spring high season 2027', '2027-03-15', '2027-05-01', 150.00, 3, 10),
       ('3e9a1f72-8c4b-4d2e-b6a0-9f8e7d6c5b44', '6f1c2b8e-4d3a-4f5e-9a7b-1c2d3e4f5a6b',
        'Christmas & New Year 2026', '2026-12-20', '2027-01-04', 180.00, 5, 20);
