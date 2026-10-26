WITH monthly AS (
    SELECT to_char(o.created_at, 'YYYY-MM') AS month, SUM(i.quantity * i.unit_price_cents) AS revenue
    FROM orders o
    JOIN order_items i ON i.order_id = o.id
    WHERE o.status = 'PAID'
    GROUP BY 1
)
SELECT month, revenue,
       LAG(revenue) OVER (ORDER BY month)           AS previous,
       revenue - LAG(revenue) OVER (ORDER BY month) AS change
FROM monthly
ORDER BY month;
