CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    price TEXT NOT NULL,
    type TEXT NOT NULL,
    description TEXT,
    active INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    status TEXT NOT NULL,
    customer_name TEXT,
    customer_contact TEXT,
    channel TEXT NOT NULL
);

CREATE INDEX idx_orders_date_status ON orders(date, status);

CREATE TABLE order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL REFERENCES orders(id),
    product_id INTEGER NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price TEXT NOT NULL
);

CREATE INDEX idx_order_items_product ON order_items(product_id);

CREATE TABLE daily_stocks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    initial_quantity INTEGER NOT NULL CHECK (initial_quantity >= 0),
    product_id INTEGER NOT NULL REFERENCES products(id),
    CONSTRAINT uq_daily_stock_product_date UNIQUE (product_id, date)
);

CREATE TABLE ingredients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    unit TEXT NOT NULL
);

CREATE TABLE ingredient_purchases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    quantity TEXT NOT NULL,
    unit_price TEXT NOT NULL,
    origin TEXT NOT NULL,
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id)
);

CREATE INDEX idx_purchases_date ON ingredient_purchases(date);
