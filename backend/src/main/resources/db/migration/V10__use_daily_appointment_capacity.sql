DROP INDEX IF EXISTS uq_appointment_requests_active_slot;

CREATE INDEX ix_appointment_requests_active_day
    ON appointment_requests (((current_start_at AT TIME ZONE 'Europe/Warsaw')::date))
    WHERE status IN ('PENDING', 'TIME_PROPOSED', 'CONFIRMED');
