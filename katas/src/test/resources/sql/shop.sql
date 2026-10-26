-- The dojo's shop database: small enough to check every answer by hand.
CREATE TABLE customers (
    id          bigint PRIMARY KEY,
    name        text   NOT NULL,
    email       text   NOT NULL UNIQUE,
    city        text,                      -- may be unknown (NULL)
    created_at  date   NOT NULL
);

CREATE TABLE products (
    id           bigint PRIMARY KEY,
    sku          text   NOT NULL UNIQUE,
    name         text   NOT NULL,
    category     text   NOT NULL,
    price_cents  bigint NOT NULL CHECK (price_cents >= 0),
    stock        int    NOT NULL DEFAULT 0 CHECK (stock >= 0),
    version      int    NOT NULL DEFAULT 0
);

CREATE TABLE orders (
    id           bigint    PRIMARY KEY,
    customer_id  bigint    NOT NULL REFERENCES customers (id),
    status       text      NOT NULL CHECK (status IN ('PLACED', 'PAID', 'CANCELLED')),
    created_at   timestamp NOT NULL
);

CREATE TABLE order_items (
    order_id          bigint NOT NULL REFERENCES orders (id),
    product_id        bigint NOT NULL REFERENCES products (id),
    quantity          int    NOT NULL CHECK (quantity > 0),
    unit_price_cents  bigint NOT NULL,      -- the price AT THE TIME of the order
    PRIMARY KEY (order_id, product_id)
);

CREATE TABLE stock_counters (
    sku       text PRIMARY KEY,
    quantity  int  NOT NULL
);

INSERT INTO customers VALUES
    (1, 'Ann', 'ann@example.com', 'Berlin', '2026-01-02'),
    (2, 'Bob', 'bob@example.com', 'Paris',  '2026-01-05'),
    (3, 'Cat', 'cat@example.com', 'Berlin', '2026-02-01'),
    (4, 'Dan', 'dan@example.com', NULL,     '2026-02-10'),
    (5, 'Eve', 'eve@example.com', 'Madrid', '2026-03-01');

INSERT INTO products (id, sku, name, category, price_cents, stock) VALUES
    (1, 'MUG-1', 'Mug',   'kitchen',  1200,  50),
    (2, 'PAN-1', 'Pan',   'kitchen',  3500,  10),
    (3, 'KNF-1', 'Knife', 'kitchen',  2500,   0),
    (4, 'LMP-1', 'Lamp',  'living',   4900,   5),
    (5, 'RUG-1', 'Rug',   'living',   8900,   2),
    (6, 'PEN-1', 'Pen',   'office',    150, 500),
    (7, 'DSK-1', 'Desk',  'office',  19900,   1);

INSERT INTO orders VALUES
    (1, 1, 'PAID',      '2026-01-10 10:00'),
    (2, 1, 'PAID',      '2026-02-03 09:00'),
    (3, 2, 'PAID',      '2026-01-15 12:00'),
    (4, 2, 'CANCELLED', '2026-02-20 18:00'),
    (5, 3, 'PAID',      '2026-02-05 14:00'),
    (6, 4, 'PLACED',    '2026-03-02 08:00'),
    (7, 3, 'PAID',      '2026-03-10 16:00');

INSERT INTO order_items VALUES
    (1, 1,  2, 1200), (1, 6, 10, 150),
    (2, 2,  1, 3500), (2, 4,  1, 4900),
    (3, 3,  1, 2500), (3, 1,  1, 1200),
    (4, 5,  1, 8900),
    (5, 4,  2, 4900), (5, 6,  5, 150),
    (6, 1,  1, 1200),
    (7, 5,  1, 8900), (7, 2,  1, 3500);
