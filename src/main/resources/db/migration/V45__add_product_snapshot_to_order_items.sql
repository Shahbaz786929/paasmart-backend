-- OrderItem previously only snapshotted unit_price, not the product's name/image.
-- If a seller later edited or deleted the product, old orders would show wrong
-- or broken product info. These columns are nullable because a product's
-- history at order time can't be reconstructed for already-deleted products.
ALTER TABLE order_items ADD COLUMN product_name VARCHAR(255);
ALTER TABLE order_items ADD COLUMN product_image_url VARCHAR(1000);

-- Best-effort backfill for existing rows: pulls the product's CURRENT name/first
-- image where the product still exists. This is not the historical name/image
-- (that data was never captured), but it's better than leaving it blank.
UPDATE order_items oi
SET product_name = p.name,
    product_image_url = split_part(p.images, ',', 1)
FROM products p
WHERE oi.product_id = p.id
  AND oi.product_name IS NULL;