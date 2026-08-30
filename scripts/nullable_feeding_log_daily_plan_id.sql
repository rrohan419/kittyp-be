-- Hibernate ddl-auto=update will not drop NOT NULL on an existing column.
-- Apply on local/dev/prod so POST /nutrition/pets/{id}/feeding-logs without
-- dailyPlanId returns 201 instead of 500.
ALTER TABLE pet_feeding_log
    ALTER COLUMN daily_plan_id DROP NOT NULL;

-- Follow period chosen when a doctor sends a plan to the parent.
ALTER TABLE nutrition_plans
    ADD COLUMN IF NOT EXISTS duration_days INTEGER;
