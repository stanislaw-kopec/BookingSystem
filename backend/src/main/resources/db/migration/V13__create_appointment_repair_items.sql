CREATE TABLE appointment_repair_items (
    id BIGSERIAL PRIMARY KEY,
    appointment_request_id BIGINT NOT NULL REFERENCES appointment_requests(id) ON DELETE CASCADE,
    item_order INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('LABOR', 'PART')),
    name VARCHAR(160) NOT NULL,
    quantity NUMERIC(8, 2) NOT NULL CHECK (quantity > 0),
    unit_gross_amount NUMERIC(10, 2) NOT NULL CHECK (unit_gross_amount > 0),
    total_gross_amount NUMERIC(10, 2) NOT NULL CHECK (total_gross_amount > 0),
    CONSTRAINT uq_appointment_repair_items_order UNIQUE (appointment_request_id, item_order)
);
