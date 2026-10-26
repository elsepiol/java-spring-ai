-- city <> 'Berlin' is NULL (not true) when city is NULL, so Dan was silently dropped.
SELECT name
FROM customers
WHERE city IS DISTINCT FROM 'Berlin'      -- or: city <> 'Berlin' OR city IS NULL
ORDER BY name;
