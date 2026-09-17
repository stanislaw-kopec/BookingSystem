CREATE TABLE invoice_documents (
    appointment_id BIGINT PRIMARY KEY REFERENCES appointment_requests(id) ON DELETE CASCADE,
    number VARCHAR(255) NOT NULL UNIQUE,
    snapshot JSONB NOT NULL,
    pdf_content BYTEA NOT NULL,
    CONSTRAINT ck_invoice_snapshot CHECK (
        snapshot->>'version' = '1' AND snapshot->>'number' = number
    ),
    CONSTRAINT ck_invoice_pdf_nonempty CHECK (octet_length(pdf_content) > 0)
);
