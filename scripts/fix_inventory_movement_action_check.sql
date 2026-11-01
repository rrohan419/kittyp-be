-- Allow RECEIVE / REVIEW (and the other current values) on inventory movements.
-- Hibernate ddl-auto=update does not refresh the Postgres CHECK constraint created for the enum.
ALTER TABLE clinic_inventory_movements DROP CONSTRAINT IF EXISTS clinic_inventory_movements_action_check;
ALTER TABLE clinic_inventory_movements ADD CONSTRAINT clinic_inventory_movements_action_check CHECK (
  (action)::text = ANY ((ARRAY[
    'OPENING',
    'ADJUST',
    'CONSUME',
    'RESERVE',
    'RELEASE',
    'RECEIVE',
    'REVIEW'
  ])::text[])
);
