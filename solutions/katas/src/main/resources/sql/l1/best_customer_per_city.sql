-- The PAID filter must be in the JOIN condition: in WHERE it would drop the cities with no paid orders.
SELECT c.city, COALESCE(SUM(i.quantity * i.unit_price_cents), 0) AS revenue
FROM customers c
LEFT JOIN orders o      ON o.customer_id = c.id AND o.status = 'PAID'
LEFT JOIN order_items i ON i.order_id = o.id
WHERE c.city IS NOT NULL
GROUP BY c.city
ORDER BY revenue DESC, c.city;
