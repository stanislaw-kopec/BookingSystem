CREATE TABLE workshop_schedule_settings (
    id BIGINT PRIMARY KEY,
    default_daily_capacity INTEGER NOT NULL CHECK (default_daily_capacity BETWEEN 1 AND 20),
    booking_horizon_days INTEGER NOT NULL CHECK (booking_horizon_days BETWEEN 7 AND 180),
    workday_start TIME NOT NULL,
    workday_end TIME NOT NULL,
    CONSTRAINT ck_workshop_schedule_workday_hours CHECK (workday_start < workday_end)
);

INSERT INTO workshop_schedule_settings (id, default_daily_capacity, booking_horizon_days, workday_start, workday_end)
VALUES (1, 4, 30, TIME '08:00', TIME '16:00');

CREATE TABLE schedule_day_overrides (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL UNIQUE,
    capacity INTEGER NOT NULL CHECK (capacity BETWEEN 0 AND 20),
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    note VARCHAR(200),
    CONSTRAINT ck_schedule_day_override_closed_capacity CHECK ((closed = TRUE AND capacity = 0) OR (closed = FALSE AND capacity > 0))
);
