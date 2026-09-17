package pl.autoserwis.invoice;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "invoice_documents")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceDocument {
    @Id
    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(nullable = false, unique = true, updatable = false)
    private String number;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private InvoiceSnapshot snapshot;

    @Column(name = "pdf_content", nullable = false, updatable = false)
    private byte[] pdfContent;

    public InvoiceDocument(Long appointmentId, InvoiceSnapshot snapshot, byte[] pdfContent) {
        this.appointmentId = appointmentId;
        this.number = snapshot.number();
        this.snapshot = snapshot;
        this.pdfContent = pdfContent.clone();
    }

    public InvoiceFile file() {
        return new InvoiceFile(snapshot.filename(), pdfContent.clone());
    }
}
