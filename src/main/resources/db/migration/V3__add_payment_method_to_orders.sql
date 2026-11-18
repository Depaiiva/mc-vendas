-- Add payment_method to orders
ALTER TABLE orders ADD COLUMN payment_method TEXT;

-- Default historical confirmed/delivered sales to PIX
UPDATE orders
SET payment_method = 'PIX'
WHERE payment_method IS NULL AND status IN ('CONFIRMED', 'DELIVERED');
