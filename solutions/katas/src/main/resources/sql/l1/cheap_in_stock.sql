SELECT sku, price_cents
FROM products
WHERE price_cents < 3000
  AND stock > 0
ORDER BY price_cents;
