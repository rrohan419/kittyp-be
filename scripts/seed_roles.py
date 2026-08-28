import psycopg2

conn = psycopg2.connect(
    host="localhost",
    dbname="kittyp-local",
    user="postgres",
    password="Postgres@123",
)
cur = conn.cursor()
cur.execute("SELECT name FROM roles ORDER BY name")
print("before:", [r[0] for r in cur.fetchall()])

roles = [
    "ROLE_USER",
    "ROLE_MODERATOR",
    "ROLE_ADMIN",
    "ROLE_DOCTOR",
    "ROLE_CLINIC_ADMIN",
    "ROLE_CLINIC_STAFF",
]
for role in roles:
    cur.execute(
        """
        INSERT INTO roles (name, is_active, created_at, updated_at)
        SELECT %s, true, NOW(), NOW()
        WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = %s)
        """,
        (role, role),
    )

conn.commit()
cur.execute("SELECT name FROM roles ORDER BY name")
print("after:", [r[0] for r in cur.fetchall()])
conn.close()
print("Roles seeded successfully.")
