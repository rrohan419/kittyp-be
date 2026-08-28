-- Map all KITTYP-* SKUs to local /product-photos/*.webp assets (no SVG primary).
-- Reuses closest archetype when a dedicated photo is unavailable.
BEGIN;

-- ===== ACCESSORIES =====
UPDATE products SET product_image_urls = '["/product-photos/double-bowl.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-001';
UPDATE products SET product_image_urls = '["/product-photos/litter-scoop.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-002';
UPDATE products SET product_image_urls = '["/product-photos/pet-carrier.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-003';
UPDATE products SET product_image_urls = '["/product-photos/dog-leash.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-004';
UPDATE products SET product_image_urls = '["/product-photos/bird-ladder.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-005'; -- climbing branch (wood structure)
UPDATE products SET product_image_urls = '["/product-photos/pet-carrier.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-006';
UPDATE products SET product_image_urls = '["/product-photos/paw-balm.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-007'; -- first-aid / care
UPDATE products SET product_image_urls = '["/product-photos/litter-house.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-008';
UPDATE products SET product_image_urls = '["/product-photos/dog-leash.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-009'; -- harness
UPDATE products SET product_image_urls = '["/product-photos/dog-bed.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-010';
UPDATE products SET product_image_urls = '["/product-photos/double-bowl.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-011';
UPDATE products SET product_image_urls = '["/product-photos/litter-house.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-012'; -- hide cave / enclosure
UPDATE products SET product_image_urls = '["/product-photos/litter-scoop.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-013'; -- net & gravel scoop
UPDATE products SET product_image_urls = '["/product-photos/double-bowl.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-014';
UPDATE products SET product_image_urls = '["/product-photos/dog-leash.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-015'; -- ID tag / walk gear

-- ===== FOOD =====
UPDATE products SET product_image_urls = '["/product-photos/kibble.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-001';
UPDATE products SET product_image_urls = '["/product-photos/salmon-treats.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-002';
UPDATE products SET product_image_urls = '["/product-photos/wet-food-pouches.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-003';
UPDATE products SET product_image_urls = '["/product-photos/kibble.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-004';
UPDATE products SET product_image_urls = '["/product-photos/dental-chews.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-005';
UPDATE products SET product_image_urls = '["/product-photos/guinea-pellets.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-006';
UPDATE products SET product_image_urls = '["/product-photos/hay-bale.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-007';
UPDATE products SET product_image_urls = '["/product-photos/iguana-greens.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-008';
UPDATE products SET product_image_urls = '["/product-photos/kibble.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-009'; -- bird seed (kibble archetype)
UPDATE products SET product_image_urls = '["/product-photos/kibble.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-010'; -- fish flakes
UPDATE products SET product_image_urls = '["/product-photos/kibble.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-011';
UPDATE products SET product_image_urls = '["/product-photos/kibble.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-012';
UPDATE products SET product_image_urls = '["/product-photos/salmon-treats.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-013'; -- training bites
UPDATE products SET product_image_urls = '["/product-photos/guinea-pellets.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-014'; -- hamster muesli
UPDATE products SET product_image_urls = '["/product-photos/guinea-pellets.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-015';
UPDATE products SET product_image_urls = '["/product-photos/iguana-greens.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-016'; -- calcium / reptile
UPDATE products SET product_image_urls = '["/product-photos/kibble.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-017'; -- cockatiel blend
UPDATE products SET product_image_urls = '["/product-photos/guinea-pellets.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-018'; -- betta pellets

-- ===== GROOMING =====
UPDATE products SET product_image_urls = '["/product-photos/dog-shampoo.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-001';
UPDATE products SET product_image_urls = '["/product-photos/grooming-brush.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-002';
UPDATE products SET product_image_urls = '["/product-photos/paw-balm.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-003';
UPDATE products SET product_image_urls = '["/product-photos/nail-clippers.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-004';
UPDATE products SET product_image_urls = '["/product-photos/ear-wipes.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-005';

-- ===== HABITAT =====
UPDATE products SET product_image_urls = '["/product-photos/wood-litter.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-001'; -- paper bedding
UPDATE products SET product_image_urls = '["/product-photos/wood-litter.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-002'; -- substrate
UPDATE products SET product_image_urls = '["/product-photos/iguana-greens.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-003'; -- basking / reptile
UPDATE products SET product_image_urls = '["/product-photos/double-bowl.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-004'; -- water conditioner
UPDATE products SET product_image_urls = '["/product-photos/hamster-wheel.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-005'; -- rodent habitat
UPDATE products SET product_image_urls = '["/product-photos/litter-house.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-006'; -- terrarium enclosure
UPDATE products SET product_image_urls = '["/product-photos/iguana-greens.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-007'; -- UVB / reptile
UPDATE products SET product_image_urls = '["/product-photos/wood-litter.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-008'; -- cage liners
UPDATE products SET product_image_urls = '["/product-photos/litter-house.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-009'; -- nano aquarium enclosure

-- ===== LITTER =====
UPDATE products SET product_image_urls = '["/product-photos/wood-litter.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-LIT-001';
UPDATE products SET product_image_urls = '["/product-photos/wood-litter.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-LIT-002';
UPDATE products SET product_image_urls = '["/product-photos/wood-litter.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-LIT-003';
UPDATE products SET product_image_urls = '["/product-photos/wood-litter.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-LIT-004';

-- ===== TOYS =====
UPDATE products SET product_image_urls = '["/product-photos/feather-wand.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-001';
UPDATE products SET product_image_urls = '["/product-photos/scratcher.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-002';
UPDATE products SET product_image_urls = '["/product-photos/puzzle-ball.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-003';
UPDATE products SET product_image_urls = '["/product-photos/rope-tug.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-004';
UPDATE products SET product_image_urls = '["/product-photos/fetch-ball.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-005';
UPDATE products SET product_image_urls = '["/product-photos/hamster-wheel.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-006';
UPDATE products SET product_image_urls = '["/product-photos/chew-sticks.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-007';
UPDATE products SET product_image_urls = '["/product-photos/bird-swing.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-008';
UPDATE products SET product_image_urls = '["/product-photos/catnip-mice.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-009';
UPDATE products SET product_image_urls = '["/product-photos/chew-bone.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-010';
UPDATE products SET product_image_urls = '["/product-photos/willow-balls.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-011';
UPDATE products SET product_image_urls = '["/product-photos/bird-ladder.webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-012';

COMMIT;

SELECT sku, name, product_image_urls->>0 AS image
FROM products
WHERE sku LIKE 'KITTYP-%'
ORDER BY sku;
