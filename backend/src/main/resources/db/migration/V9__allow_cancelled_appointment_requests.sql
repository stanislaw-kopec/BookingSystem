ALTER TABLE appointment_requests
    DROP CONSTRAINT IF EXISTS appointment_requests_status_check;

ALTER TABLE appointment_requests
    ADD CONSTRAINT appointment_requests_status_check
        CHECK (status IN ('PENDING', 'TIME_PROPOSED', 'CONFIRMED', 'CANCELLED', 'REJECTED'));
