-- Hibernate ddl-auto=update cannot add a NOT NULL column to a populated table.
-- Apply on local/staging/prod if GET/POST /ai/nutrition/plans/filter returns
-- "column np1_0.status does not exist".
ALTER TABLE nutrition_plans
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
