-- 1. Add unit_cost_snapshot to order_items
ALTER TABLE order_items ADD COLUMN unit_cost_snapshot TEXT;

-- 2. Add total_cost to ingredient_purchases
ALTER TABLE ingredient_purchases ADD COLUMN total_cost TEXT;

-- 3. Backfill total_cost for existing ingredient_purchases
UPDATE ingredient_purchases
SET total_cost = printf('%.2f', CAST(quantity AS REAL) * CAST(unit_price AS REAL))
WHERE total_cost IS NULL;

-- 4. Convert legacy units (KILO_GRAMS -> GRAMS, LITERS -> MILLILITERS) for existing ingredients & purchases
UPDATE ingredient_purchases
SET quantity = printf('%.3f', CAST(quantity AS REAL) * 1000)
WHERE ingredient_id IN (SELECT id FROM ingredients WHERE unit = 'KILO_GRAMS' OR unit = 'LITERS');

UPDATE ingredients SET unit = 'GRAMS' WHERE unit = 'KILO_GRAMS';
UPDATE ingredients SET unit = 'MILLILITERS' WHERE unit = 'LITERS';

-- 5. Create product_ingredients (ficha técnica) table
CREATE TABLE product_ingredients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id INTEGER NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    ingredient_id INTEGER NOT NULL REFERENCES ingredients(id),
    quantity TEXT NOT NULL,
    recipe_yield INTEGER NOT NULL CHECK (recipe_yield > 0)
);

CREATE INDEX idx_product_ingredients_product ON product_ingredients(product_id);
CREATE INDEX idx_product_ingredients_ingredient ON product_ingredients(ingredient_id);
