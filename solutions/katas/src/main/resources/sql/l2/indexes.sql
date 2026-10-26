-- 1. Equality column first, then the ORDER BY column: the index already holds one customer's
--    events sorted by time, so the 20 latest are read backwards from the index, no sort.
CREATE INDEX events_customer_created_idx ON events (customer_id, created_at);
-- 2. An expression index: the query filters on lower(external_ref), so index exactly that.
CREATE INDEX events_external_ref_lower_idx ON events (lower(external_ref));
-- 3. A partial index: only the 0.1% PENDING rows are in it, so it is tiny and cheap to maintain.
CREATE INDEX events_pending_idx ON events (id) WHERE status = 'PENDING';
