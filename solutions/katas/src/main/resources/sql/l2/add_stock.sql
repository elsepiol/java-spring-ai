-- Atomic insert-or-update. "SELECT, then INSERT or UPDATE" races: two threads both see "no row".
INSERT INTO stock_counters (sku, quantity)
VALUES (?, ?)
ON CONFLICT (sku) DO UPDATE SET quantity = stock_counters.quantity + EXCLUDED.quantity;
