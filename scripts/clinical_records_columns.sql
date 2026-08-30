-- Hibernate ddl-auto=update will not widen this CHECK; inserts of LAB_REPORT/SURGERY fail without it.
ALTER TABLE health_events DROP CONSTRAINT IF EXISTS health_events_type_check;
ALTER TABLE health_events ADD CONSTRAINT health_events_type_check CHECK (
    type::text = ANY (ARRAY[
        'VACCINATION', 'DEWORMING', 'VET_VISIT', 'GROOMING', 'ILLNESS', 'MEDICATION', 'CUSTOM',
        'LAB_REPORT', 'SURGERY'
    ]::text[])
);

ALTER TABLE health_events ADD COLUMN IF NOT EXISTS visit_uuid VARCHAR(64);
ALTER TABLE pet_vaccine_schedule ADD COLUMN IF NOT EXISTS certificate_url VARCHAR(1024);

INSERT INTO vaccine_master (created_at, is_active, name, species, initial_age_weeks, repeat_interval_months, description)
SELECT now(), true, v.name, v.species, v.initial_age_weeks, v.repeat_interval_months, v.description
FROM (VALUES
    ('Rabies', 'DOG', 12, 12, 'Core canine rabies'),
    ('DHPP', 'DOG', 6, 12, 'Distemper, hepatitis, parvovirus, parainfluenza'),
    ('Bordetella', 'DOG', 8, 12, 'Kennel cough'),
    ('Leptospirosis', 'DOG', 12, 12, 'Leptospira'),
    ('Rabies', 'CAT', 12, 12, 'Core feline rabies'),
    ('FVRCP', 'CAT', 6, 12, 'Feline viral rhinotracheitis, calicivirus, panleukopenia'),
    ('FeLV', 'CAT', 8, 12, 'Feline leukemia')
) AS v(name, species, initial_age_weeks, repeat_interval_months, description)
WHERE NOT EXISTS (
    SELECT 1 FROM vaccine_master existing
    WHERE existing.name = v.name AND existing.species = v.species
);
