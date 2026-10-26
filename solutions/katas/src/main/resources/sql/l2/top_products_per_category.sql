-- Aggregate first (units per product), then rank within each category, then keep ranks 1-2.
-- A window function cannot be used in WHERE (it runs after WHERE), hence the subquery.
SELECT category, sku, units, rank
FROM (
    SELECT p.category, p.sku, SUM(i.quantity) AS units,
           ROW_NUMBER() OVER (PARTITION BY p.category ORDER BY SUM(i.quantity) DESC, p.sku) AS rank
    FROM order_items i
    JOIN orders o   ON o.id = i.order_id AND o.status = 'PAID'
    JOIN products p ON p.id = i.product_id
    GROUP BY p.category, p.sku
) ranked
WHERE rank <= 2
ORDER BY category, rank;
