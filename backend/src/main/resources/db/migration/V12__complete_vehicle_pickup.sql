ALTER TABLE appointment_requests
    DROP CONSTRAINT IF EXISTS appointment_requests_status_check;

ALTER TABLE appointment_requests
    ADD CONSTRAINT appointment_requests_status_check
        CHECK (status IN ('PENDING', 'TIME_PROPOSED', 'CONFIRMED', 'READY_FOR_PICKUP', 'COMPLETED', 'CANCELLED', 'REJECTED'));

ALTER TABLE appointment_requests
    ADD COLUMN vehicle_picked_up_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN vehicle_picked_up_by_id BIGINT REFERENCES app_users(id);

ALTER TABLE appointment_requests
    DROP CONSTRAINT ck_appointment_repair_completion;

ALTER TABLE appointment_requests
    ADD CONSTRAINT ck_appointment_repair_completion
        CHECK (
            (
                status IN ('READY_FOR_PICKUP', 'COMPLETED')
                AND repair_description IS NOT NULL
                AND total_gross_amount IS NOT NULL
                AND total_gross_amount > 0
                AND repair_completed_at IS NOT NULL
                AND repair_completed_by_id IS NOT NULL
            )
            OR
            (
                status NOT IN ('READY_FOR_PICKUP', 'COMPLETED')
                AND repair_description IS NULL
                AND total_gross_amount IS NULL
                AND repair_completed_at IS NULL
                AND repair_completed_by_id IS NULL
            )
        );

ALTER TABLE appointment_requests
    ADD CONSTRAINT ck_appointment_vehicle_pickup
        CHECK (
            (
                status = 'COMPLETED'
                AND vehicle_picked_up_at IS NOT NULL
                AND vehicle_picked_up_by_id IS NOT NULL
            )
            OR
            (
                status <> 'COMPLETED'
                AND vehicle_picked_up_at IS NULL
                AND vehicle_picked_up_by_id IS NULL
            )
        );
