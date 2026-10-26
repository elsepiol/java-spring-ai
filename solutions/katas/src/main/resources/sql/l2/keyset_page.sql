-- "Rows after the last one I saw": with an index on (price_cents, id) this reads 3 rows,
-- whatever the page number. OFFSET 30000 would read and throw away 30,000 rows first.
SELECT sku, price_cents
FROM products
WHERE (price_cents, id) > (?, ?)
ORDER BY price_cents, id
LIMIT 3;
