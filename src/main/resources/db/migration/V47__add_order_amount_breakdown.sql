ALTER TABLE orders ADD COLUMN IF NOT EXISTS subtotal_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS wallet_used DECIMAL(10,2) NOT NULL DEFAULT 0;

-- Purane orders ka hisaab nikaalo: items ka total aur wallet se kata hua paisa
UPDATE orders o SET
    subtotal_amount = COALESCE((SELECT SUM(oi.subtotal) FROM order_items oi WHERE oi.order_id = o.id), 0),
    wallet_used = COALESCE((SELECT SUM(-wt.amount) FROM wallet_transactions wt
                            WHERE wt.order_id = o.id AND wt.type = 'ORDER_PAYMENT'), 0);

-- total_amount = items + delivery - discount - wallet, isliye discount nikal sakte hain
UPDATE orders SET discount_amount =
    GREATEST(subtotal_amount + COALESCE(delivery_fee, 0) - total_amount - wallet_used, 0);