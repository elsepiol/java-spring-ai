-- KATA sql L1 (find the bug): the names of all customers who do not live in Berlin, A-Z.
-- Lesson: tracks/04-sql/L1-basics.md, section 6 (NULL). This query looks right. Which customer is missing, and why?
SELECT name
FROM customers
WHERE city <> 'Berlin'
ORDER BY name;
