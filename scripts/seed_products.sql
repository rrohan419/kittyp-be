-- Kittyp multi-species catalog seed (idempotent).
-- Updates images on existing SKUs and inserts additional pet products.
--
-- Usage (local):
--   PGPASSWORD=112358 psql -h localhost -p 5433 -U postgres -d kittyp -f scripts/seed_products.sql

-- Refresh images on existing starter SKUs (more relevant visuals)
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-LIT-001';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1574158622682-e40e69881006?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-LIT-002';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-LIT-003';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-TOY-001';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-TOY-002';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-TOY-003';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1589924691995-400dc9ecc119?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-FOOD-001';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1606216794074-735e91aa2c92?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-FOOD-002';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-FOOD-003';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1552053831-71594a27632d?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-ACC-001';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-ACC-002';
UPDATE products SET product_image_urls = '["https://images.unsplash.com/photo-1450778869180-41d0601e046e?w=800&auto=format&fit=crop&q=80"]'::jsonb, updated_at = NOW()
WHERE sku = 'KITTYP-ACC-003';

INSERT INTO products (
    uuid, name, description, price, currency, status,
    product_image_urls, stock_quantity, sku, category, attributes,
    is_active, created_at, updated_at
)
SELECT
    gen_random_uuid()::text,
    v.name,
    v.description,
    v.price,
    'INR',
    'ACTIVE',
    v.images::jsonb,
    v.stock,
    v.sku,
    v.category,
    v.attributes::json,
    true,
    NOW(),
    NOW()
FROM (
    VALUES
    -- Existing starter catalog (insert if missing)
    (
        'KITTYP-LIT-001',
        'Kittyp Pine Wood Clumping Litter',
        'Our flagship eco-friendly pine wood litter with strong clumping, natural odor control, and low dust. Soft on paws and kinder to the planet.',
        899.00, 'Litter', 120,
        '["https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"5 kg","material":"Pine wood"}'
    ),
    (
        'KITTYP-LIT-002',
        'Kittyp Recycled Paper Litter',
        'Biodegradable litter made from post-consumer recycled paper. Dust-free, low tracking, and gentle for multi-cat homes.',
        649.00, 'Litter', 90,
        '["https://images.unsplash.com/photo-1574158622682-e40e69881006?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"White","size":"4 kg","material":"Recycled paper"}'
    ),
    (
        'KITTYP-LIT-003',
        'Kittyp Odor-Control Litter Refill',
        'Concentrated odor-neutralizing refill pellets for Kittyp litter boxes. Extend freshness between full litter changes.',
        399.00, 'Litter', 150,
        '["https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"1.5 kg","material":"Plant fibers"}'
    ),
    (
        'KITTYP-TOY-001',
        'Kittyp Feather Wand Teaser',
        'Interactive feather wand that sparks chase instincts. Durable shaft with replaceable feather tip for daily play.',
        349.00, 'Toys', 200,
        '["https://images.unsplash.com/photo-1526336024174-e58f5cdd8e13?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Multicolor","size":"Standard","material":"Wood & feathers"}'
    ),
    (
        'KITTYP-TOY-002',
        'Kittyp Cardboard Scratcher Lounge',
        'Corrugated cardboard scratcher lounge that protects furniture while giving cats a satisfying claw workout.',
        799.00, 'Toys', 80,
        '["https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Kraft","size":"Large","material":"Corrugated cardboard"}'
    ),
    (
        'KITTYP-TOY-003',
        'Kittyp Treat Puzzle Ball',
        'Slow-release puzzle ball that dispenses treats as your cat rolls it. Mental enrichment in a durable, washable shell.',
        449.00, 'Toys', 110,
        '["https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Teal","size":"Medium","material":"Food-safe plastic"}'
    ),
    (
        'KITTYP-FOOD-001',
        'Kittyp Adult Dry Cat Kibble',
        'Balanced dry food for adult cats with high-quality protein, omega fatty acids for coat health, and no artificial fillers.',
        1299.00, 'Food', 75,
        '["https://images.unsplash.com/photo-1589924691995-400dc9ecc119?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"3 kg","material":"Chicken recipe"}'
    ),
    (
        'KITTYP-FOOD-002',
        'Kittyp Soft Salmon Treats',
        'Soft, high-protein salmon treats for training and bonding. Grain-conscious recipe cats love.',
        299.00, 'Food', 180,
        '["https://images.unsplash.com/photo-1606216794074-735e91aa2c92?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"100 g","material":"Salmon"}'
    ),
    (
        'KITTYP-FOOD-003',
        'Kittyp Wet Food Pouch Pack',
        'Variety pack of gravy wet food pouches — chicken, tuna, and salmon. Convenient single servings for hydration and taste.',
        599.00, 'Food', 100,
        '["https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"12 x 85 g","material":"Mixed proteins"}'
    ),
    (
        'KITTYP-ACC-001',
        'Kittyp Stainless Double Bowl Set',
        'Elevated stainless-steel food and water bowls on a non-slip base. Easy to clean and stable for messy eaters.',
        749.00, 'Accessories', 95,
        '["https://images.unsplash.com/photo-1552053831-71594a27632d?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Silver","size":"2 x 400 ml","material":"Stainless steel"}'
    ),
    (
        'KITTYP-ACC-002',
        'Kittyp Ergonomic Litter Scoop',
        'Deep-slot scoop designed for pine and paper litter. Comfort grip handle and hanging hole for tidy storage.',
        249.00, 'Accessories', 220,
        '["https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Charcoal","size":"Standard","material":"ABS plastic"}'
    ),
    (
        'KITTYP-ACC-003',
        'Kittyp Travel Carrier Pad',
        'Washable, quilted carrier pad that cushions vet trips and boarding. Non-slip underside keeps it in place.',
        549.00, 'Accessories', 70,
        '["https://images.unsplash.com/photo-1450778869180-41d0601e046e?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Soft grey","size":"M","material":"Polyester fleece"}'
    ),

    -- Dogs
    (
        'KITTYP-FOOD-004',
        'Kittyp Adult Dog Kibble',
        'Complete nutrition for adult dogs with real chicken protein, joint-supporting nutrients, and a crunchy bite dogs love.',
        1499.00, 'Food', 85,
        '["https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"5 kg","material":"Chicken recipe"}'
    ),
    (
        'KITTYP-FOOD-005',
        'Kittyp Dog Dental Chews',
        'Daily dental chews that help reduce plaque while rewarding good behavior. Sized for medium dogs.',
        449.00, 'Food', 140,
        '["https://images.unsplash.com/photo-1537151608828-ea2b11777ee8?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"30 sticks","material":"Dental formula"}'
    ),
    (
        'KITTYP-TOY-004',
        'Kittyp Rope Tug Toy',
        'Heavy-duty cotton rope tug for fetch and pull games. Helps clean teeth during play.',
        399.00, 'Toys', 160,
        '["https://images.unsplash.com/photo-1530281700549-e82e7bf110d6?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"Large","material":"Cotton rope"}'
    ),
    (
        'KITTYP-TOY-005',
        'Kittyp Squeaky Fetch Ball',
        'Bouncy squeaker ball for park fetch sessions. Floats in water and durable enough for enthusiastic chewers.',
        299.00, 'Toys', 175,
        '["https://images.unsplash.com/photo-1561037404-61cd46aa615b?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Orange","size":"Medium","material":"Natural rubber"}'
    ),
    (
        'KITTYP-ACC-004',
        'Kittyp Reflective Dog Leash',
        'Padded-handle leash with reflective stitching for safer evening walks. Secure clip and 1.5 m length.',
        599.00, 'Accessories', 120,
        '["https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Navy","size":"1.5 m","material":"Nylon"}'
    ),
    (
        'KITTYP-GRM-001',
        'Kittyp Gentle Dog Shampoo',
        'pH-balanced oatmeal shampoo for sensitive dog skin. Rinses clean with a light, pet-safe fragrance.',
        449.00, 'Grooming', 100,
        '["https://images.unsplash.com/photo-1558788353-f76d92427f16?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Clear","size":"500 ml","material":"Oatmeal formula"}'
    ),

    -- Rodents & rabbits
    (
        'KITTYP-FOOD-006',
        'Kittyp Guinea Pig Pellets',
        'Vitamin C–fortified pellets for guinea pigs. Balanced fiber blend for digestive health.',
        499.00, 'Food', 90,
        '["https://images.unsplash.com/photo-1548767797-d8c844163c4c?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"1 kg","material":"Fortified pellets"}'
    ),
    (
        'KITTYP-FOOD-007',
        'Kittyp Timothy Hay Bale',
        'Premium long-strand timothy hay for rabbits, guinea pigs, and chinchillas. Dust-extracted for healthier breathing.',
        649.00, 'Food', 70,
        '["https://images.unsplash.com/photo-1568640347023-a616a30bc3bd?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"2 kg","material":"Timothy hay"}'
    ),
    (
        'KITTYP-HAB-001',
        'Kittyp Soft Paper Bedding',
        'Ultra-absorbent recycled paper bedding for hamsters, mice, and gerbils. Low dust and easy spot cleaning.',
        399.00, 'Habitat', 130,
        '["https://images.unsplash.com/photo-1611003228941-98852ba62227?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"White","size":"10 L","material":"Recycled paper"}'
    ),
    (
        'KITTYP-TOY-006',
        'Kittyp Silent Hamster Wheel',
        'Quiet-running exercise wheel sized for Syrian and dwarf hamsters. Solid running surface protects tiny feet.',
        549.00, 'Toys', 85,
        '["https://images.unsplash.com/photo-1568640347023-a616a30bc3bd?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Clear","size":"21 cm","material":"ABS plastic"}'
    ),
    (
        'KITTYP-TOY-007',
        'Kittyp Natural Chew Sticks',
        'Apple-wood chew sticks for rabbits and rodents. Supports dental wear and natural foraging behavior.',
        249.00, 'Toys', 200,
        '["https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"10 pack","material":"Apple wood"}'
    ),

    -- Reptiles (iguana & friends)
    (
        'KITTYP-FOOD-008',
        'Kittyp Iguana Greens Blend',
        'Dried leafy greens blend formulated for herbivorous reptiles like iguanas. Rehydrate or mix into fresh salads.',
        699.00, 'Food', 60,
        '["https://images.unsplash.com/photo-1577493340887-b7bfff550145?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Green","size":"500 g","material":"Leafy greens"}'
    ),
    (
        'KITTYP-HAB-002',
        'Kittyp Reptile Terrarium Substrate',
        'Coconut-fiber substrate that holds humidity for tropical reptiles. Easy to spot-clean and replace.',
        799.00, 'Habitat', 55,
        '["https://images.unsplash.com/photo-1558611848-73f7eb4001a1?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Brown","size":"8 L","material":"Coconut fiber"}'
    ),
    (
        'KITTYP-HAB-003',
        'Kittyp Basking Heat Lamp',
        'Daytime basking bulb to create a proper thermal gradient for lizards and iguanas. Fits standard reptile fixtures.',
        899.00, 'Habitat', 45,
        '["https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Clear","size":"75 W","material":"Glass bulb"}'
    ),
    (
        'KITTYP-ACC-005',
        'Kittyp Climbing Branch Set',
        'Natural cork bark climbing branches for arboreal reptiles. Creates vertical enrichment in the terrarium.',
        649.00, 'Accessories', 50,
        '["https://images.unsplash.com/photo-1577493340887-b7bfff550145?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"3-piece","material":"Cork bark"}'
    ),

    -- Birds & aquatic
    (
        'KITTYP-FOOD-009',
        'Kittyp Parrot Seed Mix',
        'Fortified seed and grain mix for medium parrots and cockatiels. Includes dried fruit pieces for foraging fun.',
        549.00, 'Food', 80,
        '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"1 kg","material":"Seed blend"}'
    ),
    (
        'KITTYP-TOY-008',
        'Kittyp Bird Swing Perch',
        'Natural wood swing perch that encourages balance and play for small to medium birds.',
        349.00, 'Toys', 95,
        '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"Standard","material":"Wood & rope"}'
    ),
    (
        'KITTYP-FOOD-010',
        'Kittyp Tropical Fish Flakes',
        'Color-enhancing flake food for community tropical fish. Floats briefly then sinks for mid and bottom feeders.',
        299.00, 'Food', 150,
        '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"100 g","material":"Flake formula"}'
    ),
    (
        'KITTYP-HAB-004',
        'Kittyp Aquarium Water Conditioner',
        'Removes chlorine and chloramine while adding slime-coat support. Safe for freshwater community tanks.',
        349.00, 'Habitat', 110,
        '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Blue","size":"250 ml","material":"Liquid concentrate"}'
    ),

    -- Shared care
    (
        'KITTYP-GRM-002',
        'Kittyp Dual-Sided Grooming Brush',
        'Soft pin side and bristle side for cats, dogs, and rabbits. Reduces shedding and distributes natural oils.',
        399.00, 'Grooming', 125,
        '["https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Grey","size":"One size","material":"ABS & pins"}'
    ),
    (
        'KITTYP-ACC-006',
        'Kittyp Soft-Sided Pet Carrier',
        'Airline-inspired soft carrier with mesh panels for cats, small dogs, and rabbits. Padded base and shoulder strap.',
        1899.00, 'Accessories', 40,
        '["https://images.unsplash.com/photo-1450778869180-41d0601e046e?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Charcoal","size":"M","material":"Oxford fabric"}'
    ),
    (
        'KITTYP-ACC-007',
        'Kittyp Pet First Aid Pouch',
        'Compact first-aid pouch with gauze, antiseptic wipes, tick remover, and emergency card for walks and travel.',
        799.00, 'Accessories', 65,
        '["https://images.unsplash.com/photo-1551963831-b3b1ca40c98e?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Red","size":"Travel","material":"Nylon pouch"}'
    ),

    -- More cats
    (
        'KITTYP-LIT-004',
        'Kittyp Multi-Cat Clay-Free Litter',
        'High-capacity plant-based litter engineered for multi-cat households. Extra odor lock without clay dust.',
        1099.00, 'Litter', 70,
        '["https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"8 kg","material":"Plant fibers"}'
    ),
    (
        'KITTYP-TOY-009',
        'Kittyp Catnip Mice Twin Pack',
        'Soft plush mice filled with potent catnip. Perfect for batting, carrying, and solo play.',
        249.00, 'Toys', 180,
        '["https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Grey","size":"2-pack","material":"Plush & catnip"}'
    ),
    (
        'KITTYP-FOOD-011',
        'Kittyp Kitten Growth Formula',
        'Calorie-dense kitten kibble with DHA for brain development and small kibble size for tiny jaws.',
        1199.00, 'Food', 65,
        '["https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"2 kg","material":"Chicken & fish"}'
    ),
    (
        'KITTYP-ACC-008',
        'Kittyp Covered Litter House',
        'Hooded litter enclosure with carbon filter and swing door to contain scatter and odor.',
        1599.00, 'Accessories', 35,
        '["https://images.unsplash.com/photo-1574158622682-e40e69881006?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Beige","size":"Large","material":"PP plastic"}'
    ),

    -- More dogs
    (
        'KITTYP-FOOD-012',
        'Kittyp Puppy Starter Kibble',
        'Small-bite puppy formula with calcium for strong bones and DHA for learning.',
        1399.00, 'Food', 70,
        '["https://images.unsplash.com/photo-1611003228941-98852ba62227?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"3 kg","material":"Chicken recipe"}'
    ),
    (
        'KITTYP-FOOD-013',
        'Kittyp Dog Jerky Training Bites',
        'Soft jerky bites for high-repetition training. Low-fat and easy to break apart.',
        399.00, 'Food', 150,
        '["https://images.unsplash.com/photo-1552053831-71594a27632d?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"200 g","material":"Chicken jerky"}'
    ),
    (
        'KITTYP-TOY-010',
        'Kittyp Tough Chew Bone',
        'Durable nylon-style chew bone for power chewers. Helps occupy busy dogs between walks.',
        499.00, 'Toys', 110,
        '["https://images.unsplash.com/photo-1530281700549-e82e7bf110d6?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Blue","size":"Large","material":"Durable nylon"}'
    ),
    (
        'KITTYP-ACC-009',
        'Kittyp Adjustable Dog Harness',
        'No-choke harness with reflective trim and dual D-rings for everyday walks.',
        899.00, 'Accessories', 90,
        '["https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Olive","size":"M","material":"Nylon mesh"}'
    ),
    (
        'KITTYP-ACC-010',
        'Kittyp Orthopedic Dog Bed',
        'Memory-foam bolster bed that supports joints for senior and large-breed dogs.',
        2499.00, 'Accessories', 30,
        '["https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Grey","size":"L","material":"Memory foam"}'
    ),
    (
        'KITTYP-GRM-003',
        'Kittyp Paw Balm Stick',
        'Moisturizing paw balm for cracked pads after hot pavement or winter salt.',
        349.00, 'Grooming', 100,
        '["https://images.unsplash.com/photo-1558788353-f76d92427f16?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"15 g","material":"Shea & beeswax"}'
    ),

    -- More rodents / small pets
    (
        'KITTYP-FOOD-014',
        'Kittyp Hamster Muesli Mix',
        'Varied seed and grain mix with dried veggies for Syrian and dwarf hamsters.',
        349.00, 'Food', 120,
        '["https://images.unsplash.com/photo-1568640347023-a616a30bc3bd?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"500 g","material":"Seed muesli"}'
    ),
    (
        'KITTYP-FOOD-015',
        'Kittyp Rabbit Pellets',
        'High-fiber rabbit pellets with timothy base — no sugary extras.',
        549.00, 'Food', 85,
        '["https://images.unsplash.com/photo-1548767797-d8c844163c4c?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"1.5 kg","material":"Timothy pellets"}'
    ),
    (
        'KITTYP-HAB-005',
        'Kittyp Multi-Level Rodent Habitat',
        'Ventilated habitat with platforms and hideouts for hamsters and gerbils.',
        2199.00, 'Habitat', 25,
        '["https://images.unsplash.com/photo-1611003228941-98852ba62227?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Clear","size":"Large","material":"Acrylic & wire"}'
    ),
    (
        'KITTYP-TOY-011',
        'Kittyp Willow Ball Chews',
        'Hand-woven willow balls for rabbits and guinea pigs to toss and nibble.',
        199.00, 'Toys', 160,
        '["https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"3-pack","material":"Willow"}'
    ),
    (
        'KITTYP-ACC-011',
        'Kittyp Ceramic Small-Pet Bowl',
        'Heavy ceramic dish that resists tipping for rabbits, guinea pigs, and ferrets.',
        299.00, 'Accessories', 140,
        '["https://images.unsplash.com/photo-1551963831-b3b1ca40c98e?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"White","size":"250 ml","material":"Ceramic"}'
    ),

    -- More reptiles
    (
        'KITTYP-FOOD-016',
        'Kittyp Calcium Dust Supplement',
        'Reptile calcium powder with D3 for dusting feeder insects and greens.',
        449.00, 'Food', 95,
        '["https://images.unsplash.com/photo-1577493340887-b7bfff550145?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"White","size":"100 g","material":"Calcium + D3"}'
    ),
    (
        'KITTYP-HAB-006',
        'Kittyp Glass Terrarium Starter',
        'Front-opening glass terrarium kit sized for juvenile iguanas and bearded dragons.',
        4999.00, 'Habitat', 15,
        '["https://images.unsplash.com/photo-1558611848-73f7eb4001a1?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Black","size":"90 cm","material":"Glass"}'
    ),
    (
        'KITTYP-HAB-007',
        'Kittyp UVB Tube Lamp',
        'UVB lighting tube to support vitamin D synthesis for diurnal reptiles.',
        1299.00, 'Habitat', 40,
        '["https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"White","size":"24 in","material":"Fluorescent UVB"}'
    ),
    (
        'KITTYP-ACC-012',
        'Kittyp Reptile Hide Cave',
        'Resin hide cave that creates a secure dark retreat for lizards and snakes.',
        549.00, 'Accessories', 55,
        '["https://images.unsplash.com/photo-1577493340887-b7bfff550145?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Rock grey","size":"Medium","material":"Resin"}'
    ),

    -- Birds & fish extras
    (
        'KITTYP-FOOD-017',
        'Kittyp Cockatiel Daily Blend',
        'Balanced seed and pellet blend tailored for cockatiels with added vitamins.',
        449.00, 'Food', 90,
        '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"750 g","material":"Seed & pellet"}'
    ),
    (
        'KITTYP-TOY-012',
        'Kittyp Foraging Bird Ladder',
        'Wooden ladder with hanging foraging cups to keep clever birds busy.',
        449.00, 'Toys', 70,
        '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Natural","size":"Standard","material":"Wood"}'
    ),
    (
        'KITTYP-HAB-008',
        'Kittyp Bird Cage Liner Pads',
        'Absorbent disposable cage liners that simplify daily cleaning.',
        299.00, 'Habitat', 130,
        '["https://images.unsplash.com/photo-1552728089-57bdde30beb3?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"White","size":"30 sheets","material":"Paper pulp"}'
    ),
    (
        'KITTYP-FOOD-018',
        'Kittyp Betta Pellets',
        'Slow-sinking micro pellets formulated for betta fish protein needs.',
        249.00, 'Food', 160,
        '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"N/A","size":"50 g","material":"Micro pellets"}'
    ),
    (
        'KITTYP-HAB-009',
        'Kittyp Nano Aquarium Starter Kit',
        'All-in-one nano tank with filter and LED light — ideal for bettas or shrimp.',
        2999.00, 'Habitat', 20,
        '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Black","size":"20 L","material":"Glass"}'
    ),
    (
        'KITTYP-ACC-013',
        'Kittyp Aquarium Net & Gravel Scoop',
        'Fine-mesh fish net paired with a gravel scoop for routine tank care.',
        199.00, 'Accessories', 150,
        '["https://images.unsplash.com/photo-1524704654690-b56c05c78a00?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Green","size":"Standard","material":"Plastic & mesh"}'
    ),

    -- Shared care extras
    (
        'KITTYP-GRM-004',
        'Kittyp Nail Clipper Set',
        'Safety-guard nail clippers with file for cats, small dogs, and rabbits.',
        349.00, 'Grooming', 115,
        '["https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Black","size":"One size","material":"Stainless steel"}'
    ),
    (
        'KITTYP-GRM-005',
        'Kittyp Ear Cleaning Wipes',
        'Gentle alcohol-free ear wipes for dogs and cats — 30 count.',
        299.00, 'Grooming', 140,
        '["https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"White","size":"30 wipes","material":"Non-woven"}'
    ),
    (
        'KITTYP-ACC-014',
        'Kittyp Collapsible Travel Bowl',
        'Silicone collapsible bowl for hikes and road trips with dogs or cats.',
        349.00, 'Accessories', 170,
        '["https://images.unsplash.com/photo-1450778869180-41d0601e046e?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Teal","size":"350 ml","material":"Food-grade silicone"}'
    ),
    (
        'KITTYP-ACC-015',
        'Kittyp GPS-Ready ID Tag',
        'Laser-engravable stainless ID tag with QR emergency profile link.',
        449.00, 'Accessories', 200,
        '["https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=800&auto=format&fit=crop&q=80"]',
        '{"color":"Silver","size":"Round","material":"Stainless steel"}'
    )
) AS v(sku, name, description, price, category, stock, images, attributes)
WHERE NOT EXISTS (
    SELECT 1 FROM products p WHERE p.sku = v.sku
);

SELECT category, COUNT(*) AS products
FROM products
WHERE sku LIKE 'KITTYP-%' AND is_active = true
GROUP BY category
ORDER BY category;

SELECT sku, name, category, price
FROM products
WHERE sku LIKE 'KITTYP-%'
ORDER BY sku;
