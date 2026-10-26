-- LEFT JOIN keeps customers without orders; COUNT(o.id) counts only matched rows (COUNT(*) would give them 1).
SELECT c.name, COUNT(o.id) AS orders
FROM customers c
LEFT JOIN orders o ON o.customer_id = c.id
GROUP BY c.id, c.name
ORDER BY orders DESC, c.name;
