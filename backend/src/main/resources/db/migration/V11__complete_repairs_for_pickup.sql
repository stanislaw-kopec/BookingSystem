ALTER TABLE appointment_requests
    DROP CONSTRAINT IF EXISTS appointment_requests_status_check;

ALTER TABLE appointment_requests
    ADD CONSTRAINT appointment_requests_status_check
        CHECK (status IN ('PENDING', 'TIME_PROPOSED', 'CONFIRMED', 'READY_FOR_PICKUP', 'CANCELLED', 'REJECTED'));

ALTER TABLE appointment_requests
    ADD COLUMN repair_description VARCHAR(2000),
    ADD COLUMN total_gross_amount NUMERIC(10, 2),
    ADD COLUMN repair_completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN repair_completed_by_id BIGINT REFERENCES app_users(id);

ALTER TABLE appointment_requests
    ADD CONSTRAINT ck_appointment_repair_completion CHECK (
        (
            status = 'READY_FOR_PICKUP'
            AND repair_description IS NOT NULL
            AND total_gross_amount IS NOT NULL
            AND total_gross_amount > 0
            AND repair_completed_at IS NOT NULL
            AND repair_completed_by_id IS NOT NULL
        )
        OR
        (
            status <> 'READY_FOR_PICKUP'
            AND repair_description IS NULL
            AND total_gross_amount IS NULL
            AND repair_completed_at IS NULL
            AND repair_completed_by_id IS NULL
        )
    );
