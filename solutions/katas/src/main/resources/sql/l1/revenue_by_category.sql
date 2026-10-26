-- WHERE filters rows before grouping; HAVING filters groups after.
SELECT p.category, SUM(i.quantity * i.unit_price_cents) AS revenue
FROM order_items i
JOIN orders o   ON o.id = i.order_id
JOIN products p ON p.id = i.product_id
WHERE o.status = 'PAID'
GROUP BY p.category
HAVING SUM(i.quantity * i.unit_price_cents) >= 5000
ORDER BY revenue DESC;
