WITH totals AS (
    SELECT o.customer_id, o.id AS order_id, o.created_at, SUM(i.quantity * i.unit_price_cents) AS order_total
    FROM orders o
    JOIN order_items i ON i.order_id = o.id
    WHERE o.status = 'PAID'
    GROUP BY o.customer_id, o.id, o.created_at
)
SELECT customer_id, order_id, order_total,
       SUM(order_total) OVER (PARTITION BY customer_id ORDER BY created_at
                              ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_total
FROM totals
ORDER BY customer_id, created_at;
