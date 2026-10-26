DELETE FROM newsletter
WHERE id IN (
    SELECT id
    FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY lower(email) ORDER BY id) AS n FROM newsletter) numbered
    WHERE n > 1
);
-- Then prevent it from happening again: CREATE UNIQUE INDEX ON newsletter (lower(email));
