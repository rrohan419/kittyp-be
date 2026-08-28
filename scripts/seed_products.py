#!/usr/bin/env python3
"""
Idempotent Kittyp multi-species catalog seed.

Defaults match application-local.properties (localhost:5433 / kittyp).

Usage:
  python3 scripts/seed_products.py

Env overrides:
  PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD

Prefer scripts/seed_products.sql if psycopg2 is unavailable:
  PGPASSWORD=112358 psql -h localhost -p 5433 -U postgres -d kittyp -f scripts/seed_products.sql
"""

from __future__ import annotations

import json
import os
import uuid

try:
    import psycopg2
except ImportError:
    raise SystemExit(
        "psycopg2 is required. Install with: pip install psycopg2-binary\n"
        "Or run: PGPASSWORD=112358 psql -h localhost -p 5433 -U postgres -d kittyp -f scripts/seed_products.sql"
    )

IMG = "https://images.unsplash.com/photo-{}?w=800&auto=format&fit=crop&q=80"

PRODUCTS = [
    # Cats — litter / toys / food / accessories
    {"sku": "KITTYP-LIT-001", "name": "Kittyp Pine Wood Clumping Litter", "description": "Our flagship eco-friendly pine wood litter with strong clumping, natural odor control, and low dust.", "price": 899.00, "category": "Litter", "stock": 120, "images": [IMG.format("1441974231531-c6227db76b6e")], "attributes": {"color": "Natural", "size": "5 kg", "material": "Pine wood"}},
    {"sku": "KITTYP-LIT-002", "name": "Kittyp Recycled Paper Litter", "description": "Biodegradable litter made from post-consumer recycled paper. Dust-free and gentle for multi-cat homes.", "price": 649.00, "category": "Litter", "stock": 90, "images": [IMG.format("1574158622682-e40e69881006")], "attributes": {"color": "White", "size": "4 kg", "material": "Recycled paper"}},
    {"sku": "KITTYP-LIT-003", "name": "Kittyp Odor-Control Litter Refill", "description": "Concentrated odor-neutralizing refill pellets for Kittyp litter boxes.", "price": 399.00, "category": "Litter", "stock": 150, "images": [IMG.format("1514888286974-6c03e2ca1dba")], "attributes": {"color": "Natural", "size": "1.5 kg", "material": "Plant fibers"}},
    {"sku": "KITTYP-TOY-001", "name": "Kittyp Feather Wand Teaser", "description": "Interactive feather wand that sparks chase instincts for cats.", "price": 349.00, "category": "Toys", "stock": 200, "images": [IMG.format("1526336024174-e58f5cdd8e13")], "attributes": {"color": "Multicolor", "size": "Standard", "material": "Wood & feathers"}},
    {"sku": "KITTYP-TOY-002", "name": "Kittyp Cardboard Scratcher Lounge", "description": "Corrugated cardboard scratcher lounge that protects furniture.", "price": 799.00, "category": "Toys", "stock": 80, "images": [IMG.format("1592194996308-7b43878e84a6")], "attributes": {"color": "Kraft", "size": "Large", "material": "Corrugated cardboard"}},
    {"sku": "KITTYP-TOY-003", "name": "Kittyp Treat Puzzle Ball", "description": "Slow-release puzzle ball for cats. Mental enrichment in a washable shell.", "price": 449.00, "category": "Toys", "stock": 110, "images": [IMG.format("1573865526739-10659fec78a5")], "attributes": {"color": "Teal", "size": "Medium", "material": "Food-safe plastic"}},
    {"sku": "KITTYP-FOOD-001", "name": "Kittyp Adult Dry Cat Kibble", "description": "Balanced dry food for adult cats with high-quality protein.", "price": 1299.00, "category": "Food", "stock": 75, "images": [IMG.format("1589924691995-400dc9ecc119")], "attributes": {"color": "N/A", "size": "3 kg", "material": "Chicken recipe"}},
    {"sku": "KITTYP-FOOD-002", "name": "Kittyp Soft Salmon Treats", "description": "Soft, high-protein salmon treats for training and bonding.", "price": 299.00, "category": "Food", "stock": 180, "images": [IMG.format("1606216794074-735e91aa2c92")], "attributes": {"color": "N/A", "size": "100 g", "material": "Salmon"}},
    {"sku": "KITTYP-FOOD-003", "name": "Kittyp Wet Food Pouch Pack", "description": "Variety pack of gravy wet food pouches for cats.", "price": 599.00, "category": "Food", "stock": 100, "images": [IMG.format("1516734212186-a967f81ad0d7")], "attributes": {"color": "N/A", "size": "12 x 85 g", "material": "Mixed proteins"}},
    {"sku": "KITTYP-ACC-001", "name": "Kittyp Stainless Double Bowl Set", "description": "Elevated stainless-steel food and water bowls on a non-slip base.", "price": 749.00, "category": "Accessories", "stock": 95, "images": [IMG.format("1552053831-71594a27632d")], "attributes": {"color": "Silver", "size": "2 x 400 ml", "material": "Stainless steel"}},
    {"sku": "KITTYP-ACC-002", "name": "Kittyp Ergonomic Litter Scoop", "description": "Deep-slot scoop designed for pine and paper litter.", "price": 249.00, "category": "Accessories", "stock": 220, "images": [IMG.format("1601758228041-f3b2795255f1")], "attributes": {"color": "Charcoal", "size": "Standard", "material": "ABS plastic"}},
    {"sku": "KITTYP-ACC-003", "name": "Kittyp Travel Carrier Pad", "description": "Washable, quilted carrier pad for vet trips and boarding.", "price": 549.00, "category": "Accessories", "stock": 70, "images": [IMG.format("1450778869180-41d0601e046e")], "attributes": {"color": "Soft grey", "size": "M", "material": "Polyester fleece"}},
    # Dogs
    {"sku": "KITTYP-FOOD-004", "name": "Kittyp Adult Dog Kibble", "description": "Complete nutrition for adult dogs with real chicken protein.", "price": 1499.00, "category": "Food", "stock": 85, "images": [IMG.format("1548199973-03cce0bbc87b")], "attributes": {"color": "N/A", "size": "5 kg", "material": "Chicken recipe"}},
    {"sku": "KITTYP-FOOD-005", "name": "Kittyp Dog Dental Chews", "description": "Daily dental chews that help reduce plaque for medium dogs.", "price": 449.00, "category": "Food", "stock": 140, "images": [IMG.format("1537151608828-ea2b11777ee8")], "attributes": {"color": "N/A", "size": "30 sticks", "material": "Dental formula"}},
    {"sku": "KITTYP-TOY-004", "name": "Kittyp Rope Tug Toy", "description": "Heavy-duty cotton rope tug for fetch and pull games.", "price": 399.00, "category": "Toys", "stock": 160, "images": [IMG.format("1530281700549-e82e7bf110d6")], "attributes": {"color": "Natural", "size": "Large", "material": "Cotton rope"}},
    {"sku": "KITTYP-TOY-005", "name": "Kittyp Squeaky Fetch Ball", "description": "Bouncy squeaker ball for park fetch sessions.", "price": 299.00, "category": "Toys", "stock": 175, "images": [IMG.format("1561037404-61cd46aa615b")], "attributes": {"color": "Orange", "size": "Medium", "material": "Natural rubber"}},
    {"sku": "KITTYP-ACC-004", "name": "Kittyp Reflective Dog Leash", "description": "Padded-handle leash with reflective stitching for safer evening walks.", "price": 599.00, "category": "Accessories", "stock": 120, "images": [IMG.format("1583337130417-3346a1be7dee")], "attributes": {"color": "Navy", "size": "1.5 m", "material": "Nylon"}},
    {"sku": "KITTYP-GRM-001", "name": "Kittyp Gentle Dog Shampoo", "description": "pH-balanced oatmeal shampoo for sensitive dog skin.", "price": 449.00, "category": "Grooming", "stock": 100, "images": [IMG.format("1558788353-f76d92427f16")], "attributes": {"color": "Clear", "size": "500 ml", "material": "Oatmeal formula"}},
    # Rodents
    {"sku": "KITTYP-FOOD-006", "name": "Kittyp Guinea Pig Pellets", "description": "Vitamin C–fortified pellets for guinea pigs.", "price": 499.00, "category": "Food", "stock": 90, "images": [IMG.format("1548767797-d8c844163c4c")], "attributes": {"color": "N/A", "size": "1 kg", "material": "Fortified pellets"}},
    {"sku": "KITTYP-FOOD-007", "name": "Kittyp Timothy Hay Bale", "description": "Premium timothy hay for rabbits, guinea pigs, and chinchillas.", "price": 649.00, "category": "Food", "stock": 70, "images": [IMG.format("1568640347023-a616a30bc3bd")], "attributes": {"color": "Natural", "size": "2 kg", "material": "Timothy hay"}},
    {"sku": "KITTYP-HAB-001", "name": "Kittyp Soft Paper Bedding", "description": "Ultra-absorbent recycled paper bedding for hamsters and mice.", "price": 399.00, "category": "Habitat", "stock": 130, "images": [IMG.format("1611003228941-98852ba62227")], "attributes": {"color": "White", "size": "10 L", "material": "Recycled paper"}},
    {"sku": "KITTYP-TOY-006", "name": "Kittyp Silent Hamster Wheel", "description": "Quiet-running exercise wheel for Syrian and dwarf hamsters.", "price": 549.00, "category": "Toys", "stock": 85, "images": [IMG.format("1568640347023-a616a30bc3bd")], "attributes": {"color": "Clear", "size": "21 cm", "material": "ABS plastic"}},
    {"sku": "KITTYP-TOY-007", "name": "Kittyp Natural Chew Sticks", "description": "Apple-wood chew sticks for rabbits and rodents.", "price": 249.00, "category": "Toys", "stock": 200, "images": [IMG.format("1441974231531-c6227db76b6e")], "attributes": {"color": "Natural", "size": "10 pack", "material": "Apple wood"}},
    # Reptiles
    {"sku": "KITTYP-FOOD-008", "name": "Kittyp Iguana Greens Blend", "description": "Dried leafy greens blend for herbivorous reptiles like iguanas.", "price": 699.00, "category": "Food", "stock": 60, "images": [IMG.format("1577493340887-b7bfff550145")], "attributes": {"color": "Green", "size": "500 g", "material": "Leafy greens"}},
    {"sku": "KITTYP-HAB-002", "name": "Kittyp Reptile Terrarium Substrate", "description": "Coconut-fiber substrate that holds humidity for tropical reptiles.", "price": 799.00, "category": "Habitat", "stock": 55, "images": [IMG.format("1558611848-73f7eb4001a1")], "attributes": {"color": "Brown", "size": "8 L", "material": "Coconut fiber"}},
    {"sku": "KITTYP-HAB-003", "name": "Kittyp Basking Heat Lamp", "description": "Daytime basking bulb for lizards and iguanas.", "price": 899.00, "category": "Habitat", "stock": 45, "images": [IMG.format("1506905925346-21bda4d32df4")], "attributes": {"color": "Clear", "size": "75 W", "material": "Glass bulb"}},
    {"sku": "KITTYP-ACC-005", "name": "Kittyp Climbing Branch Set", "description": "Natural cork bark climbing branches for arboreal reptiles.", "price": 649.00, "category": "Accessories", "stock": 50, "images": [IMG.format("1577493340887-b7bfff550145")], "attributes": {"color": "Natural", "size": "3-piece", "material": "Cork bark"}},
    # Birds & fish
    {"sku": "KITTYP-FOOD-009", "name": "Kittyp Parrot Seed Mix", "description": "Fortified seed and grain mix for medium parrots and cockatiels.", "price": 549.00, "category": "Food", "stock": 80, "images": [IMG.format("1552728089-57bdde30beb3")], "attributes": {"color": "N/A", "size": "1 kg", "material": "Seed blend"}},
    {"sku": "KITTYP-TOY-008", "name": "Kittyp Bird Swing Perch", "description": "Natural wood swing perch for small to medium birds.", "price": 349.00, "category": "Toys", "stock": 95, "images": [IMG.format("1552728089-57bdde30beb3")], "attributes": {"color": "Natural", "size": "Standard", "material": "Wood & rope"}},
    {"sku": "KITTYP-FOOD-010", "name": "Kittyp Tropical Fish Flakes", "description": "Color-enhancing flake food for community tropical fish.", "price": 299.00, "category": "Food", "stock": 150, "images": [IMG.format("1524704654690-b56c05c78a00")], "attributes": {"color": "N/A", "size": "100 g", "material": "Flake formula"}},
    {"sku": "KITTYP-HAB-004", "name": "Kittyp Aquarium Water Conditioner", "description": "Removes chlorine and chloramine for freshwater community tanks.", "price": 349.00, "category": "Habitat", "stock": 110, "images": [IMG.format("1524704654690-b56c05c78a00")], "attributes": {"color": "Blue", "size": "250 ml", "material": "Liquid concentrate"}},
    # Shared
    {"sku": "KITTYP-GRM-002", "name": "Kittyp Dual-Sided Grooming Brush", "description": "Soft pin and bristle sides for cats, dogs, and rabbits.", "price": 399.00, "category": "Grooming", "stock": 125, "images": [IMG.format("1516734212186-a967f81ad0d7")], "attributes": {"color": "Grey", "size": "One size", "material": "ABS & pins"}},
    {"sku": "KITTYP-ACC-006", "name": "Kittyp Soft-Sided Pet Carrier", "description": "Soft carrier with mesh panels for cats, small dogs, and rabbits.", "price": 1899.00, "category": "Accessories", "stock": 40, "images": [IMG.format("1450778869180-41d0601e046e")], "attributes": {"color": "Charcoal", "size": "M", "material": "Oxford fabric"}},
    {"sku": "KITTYP-ACC-007", "name": "Kittyp Pet First Aid Pouch", "description": "Compact first-aid pouch for walks and travel.", "price": 799.00, "category": "Accessories", "stock": 65, "images": [IMG.format("1551963831-b3b1ca40c98e")], "attributes": {"color": "Red", "size": "Travel", "material": "Nylon pouch"}},
]


def main() -> None:
    conn = psycopg2.connect(
        host=os.getenv("PGHOST", "localhost"),
        port=int(os.getenv("PGPORT", "5433")),
        dbname=os.getenv("PGDATABASE", "kittyp"),
        user=os.getenv("PGUSER", "postgres"),
        password=os.getenv("PGPASSWORD", "112358"),
    )
    cur = conn.cursor()
    inserted = 0
    updated = 0
    for product in PRODUCTS:
        cur.execute("SELECT 1 FROM products WHERE sku = %s", (product["sku"],))
        if cur.fetchone():
            cur.execute(
                """
                UPDATE products
                SET product_image_urls = %s::jsonb,
                    name = %s,
                    description = %s,
                    price = %s,
                    category = %s,
                    stock_quantity = %s,
                    attributes = %s::json,
                    updated_at = NOW()
                WHERE sku = %s
                """,
                (
                    json.dumps(product["images"]),
                    product["name"],
                    product["description"],
                    product["price"],
                    product["category"],
                    product["stock"],
                    json.dumps(product["attributes"]),
                    product["sku"],
                ),
            )
            updated += 1
            continue

        cur.execute(
            """
            INSERT INTO products (
                uuid, name, description, price, currency, status,
                product_image_urls, stock_quantity, sku, category, attributes,
                is_active, created_at, updated_at
            ) VALUES (
                %s, %s, %s, %s, 'INR', 'ACTIVE',
                %s::jsonb, %s, %s, %s, %s::json,
                true, NOW(), NOW()
            )
            """,
            (
                str(uuid.uuid4()),
                product["name"],
                product["description"],
                product["price"],
                json.dumps(product["images"]),
                product["stock"],
                product["sku"],
                product["category"],
                json.dumps(product["attributes"]),
            ),
        )
        inserted += 1

    conn.commit()
    cur.execute(
        """
        SELECT category, COUNT(*)
        FROM products
        WHERE sku LIKE 'KITTYP-%%' AND is_active = true
        GROUP BY category
        ORDER BY category
        """
    )
    print(f"inserted: {inserted}, updated: {updated}")
    print("by category:")
    for row in cur.fetchall():
        print(f"  {row[0]}: {row[1]}")
    conn.close()
    print("Products seeded successfully.")


if __name__ == "__main__":
    main()
