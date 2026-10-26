-- PostgreSQL 11+: ADD COLUMN with a constant DEFAULT is instant (no table rewrite); old rows read
-- the default from the catalog, and old INSERTs that omit the column get it too.
ALTER TABLE customers ADD COLUMN status text NOT NULL DEFAULT 'ACTIVE';

-- NOT VALID: the constraint is enforced for new writes immediately, without scanning old rows
-- under an exclusive lock. VALIDATE then scans with a weaker lock that allows reads and writes.
ALTER TABLE customers ADD CONSTRAINT customers_status_check CHECK (status IN ('ACTIVE', 'BLOCKED')) NOT VALID;
ALTER TABLE customers VALIDATE CONSTRAINT customers_status_check;

-- A partial index: only blocked customers. In production use CREATE INDEX CONCURRENTLY (no write
-- lock), which cannot run inside a transaction: Flyway needs it in its own migration with
-- executeInTransaction=false.
CREATE INDEX customers_blocked_idx ON customers (id) WHERE status = 'BLOCKED';
