-- Calendar and availability queries filter instants, not the local-date expression.
CREATE INDEX ix_appointment_requests_status_start
    ON appointment_requests (status, current_start_at);

DROP INDEX ix_appointment_requests_active_day;
