-- Create the IANA time zone catalog table before deploying the application.
-- TimeZoneCatalogBootstrap seeds missing entries on application startup.
CREATE TABLE IF NOT EXISTS time_zones (
    timezone_id VARCHAR(100) PRIMARY KEY,
    display_name VARCHAR(150) NOT NULL
);
