-- KATA sql L2 "indexes": tracks/04-sql/L2-practical.md, section 1.
-- IndexesTest creates a 200,000-row "events" table, runs THIS file, runs ANALYZE, and then checks
-- the EXPLAIN plans of three queries (read them in IndexesTest). Write at most three CREATE INDEX
-- statements so that:
--   1. "the 20 latest events of one customer" needs no Seq Scan and no Sort;
--   2. a case-insensitive lookup by external_ref needs no Seq Scan;
--   3. "the 10 oldest PENDING events" uses an index smaller than 100 kB (only 0.1% are PENDING).
SELECT 1;
