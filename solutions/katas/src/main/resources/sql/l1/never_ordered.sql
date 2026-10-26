-- An anti-join. NOT EXISTS is safe with NULLs; NOT IN (subquery) is not (see not_in_berlin).
SELECT p.sku
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM order_items i WHERE i.product_id = p.id)
ORDER BY p.sku;
