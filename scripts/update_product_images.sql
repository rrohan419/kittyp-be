-- Remap all Kittyp product images to verified, subject-matching Unsplash photos.
-- Run: PGPASSWORD=112358 psql -h localhost -p 5433 -U postgres -d kittyp -f scripts/update_product_images.sql

BEGIN;

-- Helper pattern: product_image_urls = '["https://images.unsplash.com/PHOTO_ID?auto=format&fit=crop&w=480&q=60&fm=webp"]'

-- ===== CATS: Litter =====
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1572266071126-492372e7a001?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-LIT-001'; -- kitten + litter bedding
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1574158622682-e40e69881006?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-LIT-002'; -- cat
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-LIT-003'; -- orange cat
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1727510153658-643787acb16a?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-LIT-004'; -- litter box product scene

-- ===== CATS: Toys / Food =====
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-001'; -- playful cat
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1592194996308-7b43878e84a6?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-002'; -- cat
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-003'; -- orange cat
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-009'; -- catnip mice
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1589924691995-400dc9ecc119?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-001'; -- kibble in bowl
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1606216794074-735e91aa2c92?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-002'; -- food/treat flatlay
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-003'; -- cat wet food context
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1572266071126-492372e7a001?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-011'; -- kitten food

-- ===== DOGS =====
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1548199973-03cce0bbc87b?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-004'; -- dogs
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1568640347023-a616a30bc3bd?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-005'; -- bone treats
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1611003228941-98852ba62227?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-012'; -- puppy
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-013'; -- golden retriever
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1530281700549-e82e7bf110d6?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-004'; -- dog play/beach
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1561037404-61cd46aa615b?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-005'; -- labrador fetch vibe
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1530281700549-e82e7bf110d6?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-010'; -- tough chew / active dog
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1759330805648-c1d9ec906939?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-004'; -- dog on leash walk
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1494947665470-20322015e3a8?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-009'; -- dogs on leashes / harness context
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1591768795000-25df3bf6620a?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-010'; -- pug in bed
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-001'; -- dog bath/shampoo
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1537151608828-ea2b11777ee8?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-003'; -- pug / paw care

-- ===== RODENTS / RABBITS =====
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1548767797-d8c844163c4c?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-006'; -- guinea pigs
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-007'; -- rabbit (hay)
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1425082661705-1834bfd09dca?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-014'; -- hamster
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-015'; -- rabbit pellets
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1584553421349-3557471bed79?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-001'; -- hamster bedding
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1425082661705-1834bfd09dca?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-005'; -- rodent habitat
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1425082661705-1834bfd09dca?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-006'; -- hamster wheel
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1548767797-d8c844163c4c?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-007'; -- chew sticks / rodents
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-011'; -- willow chews / rabbit
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1548767797-d8c844163c4c?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-011'; -- small pet bowl

-- ===== REPTILES (iguana) =====
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1610604695612-ae85d1a6e54e?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-008'; -- green iguana
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1610604695612-ae85d1a6e54e?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-016'; -- iguana calcium
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1503301360699-4f60cf292ec8?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-002'; -- iguana / substrate
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1503301360699-4f60cf292ec8?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-003'; -- basking / iguana sun
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1610604695612-ae85d1a6e54e?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-006'; -- terrarium / iguana
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1503301360699-4f60cf292ec8?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-007'; -- UVB / reptile
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1610604695612-ae85d1a6e54e?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-005'; -- climbing branch / iguana
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1503301360699-4f60cf292ec8?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-012'; -- hide cave / reptile

-- ===== BIRDS / FISH =====
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-009'; -- parrot
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-017'; -- cockatiel blend
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-008'; -- bird perch
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-TOY-012'; -- bird ladder
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-008'; -- bird cage liners
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-010'; -- fish
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-FOOD-018'; -- betta pellets
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-004'; -- aquarium water
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-HAB-009'; -- nano aquarium
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-013'; -- aquarium net

-- ===== SHARED ACCESSORIES / GROOMING =====
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1591946559594-8c6d3b7391eb?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-001'; -- dog + bowl
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1727510153658-643787acb16a?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-002'; -- litter scoop / litter box scene
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1767352630502-d07b98d73be1?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-003'; -- cat in carrier
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1767352630502-d07b98d73be1?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-006'; -- soft carrier
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1603398938378-e54eab446dde?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-007'; -- first aid supplies
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1727510153658-643787acb16a?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-008'; -- covered litter house
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1589924691995-400dc9ecc119?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-014'; -- travel bowl / kibble bowl
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1494947665470-20322015e3a8?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-ACC-015'; -- ID tag / dogs on leashes
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-002'; -- grooming / bath
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-004'; -- nail clipper / grooming
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1537151608828-ea2b11777ee8?auto=format&fit=crop&w=480&q=60&fm=webp"]'::jsonb, updated_at = NOW() WHERE sku = 'KITTYP-GRM-005'; -- ear wipes / dog care

COMMIT;

SELECT sku, name, product_image_urls->>0 AS image
FROM products
WHERE sku LIKE 'KITTYP-%'
ORDER BY sku;
