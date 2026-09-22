package pl.autoserwis.invoice;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import pl.autoserwis.PostgresTestConfiguration;
import pl.autoserwis.appointment.*;
import pl.autoserwis.profile.*;
import pl.autoserwis.user.*;
import pl.autoserwis.vehicle.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.ArgumentMatchers.any;
import static pl.autoserwis.DatabaseTestUsers.databaseUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@Transactional
class InvoiceIntegrationTest {
    @Autowired InvoiceService invoices;
    @Autowired InvoiceDocumentRepository documents;
    @MockitoSpyBean InvoicePdfGenerator generator;
    @Autowired RepairWorkflowService service;
    @Autowired AppointmentRepository appointments;
    @Autowired UserRepository users;
    @Autowired VehicleRepository vehicles;
    @Autowired ClientProfileRepository profiles;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired MockMvc mvc;
    @MockitoBean(name = "workshopClock") Clock clock;

    @BeforeEach
    void setDate() {
        when(clock.getZone()).thenReturn(AppointmentSchedule.TIME_ZONE);
        when(clock.instant()).thenReturn(Instant.parse("2026-09-17T10:00:00Z"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void pickupPersistsTheBuyerAndPdfBeforeTheFirstDownload(boolean company) throws Exception {
        Fixture fixture = fixture(company, false, 2);
        service.markPickedUp(fixture.staffId(), fixture.appointmentId());
        entityManager.flush();
        entityManager.clear();
        InvoiceDocument saved = documents.findById(fixture.appointmentId()).orElseThrow();
        InvoiceSnapshot snapshot = saved.getSnapshot();
        assertThat(snapshot.issuedOn()).isEqualTo(LocalDate.of(2026, 9, 17));
        assertThat(snapshot.totalNet()).isEqualByComparingTo("1.62");
        assertThat(snapshot.totalVat()).isEqualByComparingTo("0.38");
        assertThat(snapshot.totalGross()).isEqualByComparingTo("2.00");
        assertThat(snapshot.sellerLines()).contains("Mietek Customs");
        assertThat(snapshot.buyerLines()).contains(company ? "Żółty Serwis sp. z o.o." : "Anna Żółć");

        ClientProfile profile = profiles.findByUser_Id(fixture.clientId()).orElseThrow();
        profile.update("Changed", "Buyer", "999888777", "changed@example.test", "Changed Street 1", "00-001", "Warsaw",
            true, "Changed Company", "1234567890", "Changed Billing 2", "00-002", "Warsaw");
        vehicles.findById(fixture.vehicleId()).orElseThrow().update("Changed", "Car", 2020, "NEW123", null);
        when(clock.instant()).thenReturn(Instant.parse("2027-02-01T12:00:00Z"));
        entityManager.flush();
        entityManager.clear();

        byte[] clientPdf = mvc.perform(get("/api/vehicles/{vehicle}/repair-history/{id}/invoice", fixture.vehicleId(), fixture.appointmentId())
                .with(databaseUser(fixture.clientName()).roles("CLIENT")))
            .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andReturn().getResponse().getContentAsByteArray();
        byte[] staffPdf = mvc.perform(get("/api/staff/appointments/{id}/invoice", fixture.appointmentId())
                .with(databaseUser(fixture.staffName()).roles("MECHANIC")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(clientPdf).isEqualTo(saved.getPdfContent()).isEqualTo(staffPdf);
        String text = pdfText(clientPdf);
        assertThat(text).contains("17.09.2026", snapshot.number(), "1,62", "0,38", "2,00", "Toyota", "KR12345")
            .contains(company ? "Żółty Serwis" : "Anna Żółć")
            .doesNotContain("Changed", "NEW123", "01.02.2027");
        writePreview(company ? "company" : "individual", clientPdf);
    }

    @Test
    void legacyCompletedRepairIsIssuedOnceAtFirstDownload() {
        Fixture fixture = fixture(false, true, 2);
        assertThat(documents.findById(fixture.appointmentId())).isEmpty();
        byte[] first = invoices.documentFor(fixture.appointmentId()).content();
        entityManager.flush();
        entityManager.clear();
        when(clock.instant()).thenReturn(Instant.parse("2030-01-01T12:00:00Z"));
        assertThat(invoices.documentFor(fixture.appointmentId()).content()).isEqualTo(first);
        assertThat(documents.findById(fixture.appointmentId()).orElseThrow().getSnapshot().issuedOn())
            .isEqualTo(LocalDate.of(2026, 9, 17));
    }

    @Test
    void legacyRepairWithoutItemsKeepsItsGrossAmount() {
        Fixture fixture = fixture(false, true, 2);
        appointments.findById(fixture.appointmentId()).orElseThrow().getRepairItems().clear();
        InvoiceSnapshot invoice = issueAndReload(fixture.appointmentId());
        assertThat(invoice.items()).hasSize(1);
        assertThat(invoice.items().getFirst().gross()).isEqualByComparingTo("2.00");
        assertThat(invoice.totalGross()).isEqualByComparingTo("2.00");
    }

    @Test
    void invoiceRemainsPrivateAndUnavailableBeforePickup() throws Exception {
        Fixture fixture = fixture(false, false, 2);
        mvc.perform(get("/api/staff/appointments/{id}/invoice", fixture.appointmentId())
                .with(databaseUser(fixture.staffName()).roles("MECHANIC")))
            .andExpect(status().isNotFound());
        service.markPickedUp(fixture.staffId(), fixture.appointmentId());
        mvc.perform(get("/api/staff/appointments/{id}/invoice", fixture.appointmentId()))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/staff/appointments/{id}/invoice", fixture.appointmentId())
                .with(databaseUser(fixture.clientName()).roles("CLIENT")))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/vehicles/{vehicle}/repair-history/{id}/invoice", fixture.vehicleId(), fixture.appointmentId())
                .with(databaseUser("invoice-stranger").roles("CLIENT")))
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({"0.01,0.01,1", "999999.99,99999999.99,1", "1,60000000.00,2"})
    void invalidCalculatedAmountsReturn400AndDoNotCompleteRepair(String quantity, String price, int count) throws Exception {
        Fixture fixture = fixture(false, false, 0);
        String item = """
            {"type":"LABOR","name":"Test work","quantity":%s,"unitGrossAmount":%s}
            """.formatted(quantity, price);
        String json = "{\"repairDescription\":\"Completed repair work\",\"repairItems\":["
            + String.join(",", java.util.Collections.nCopies(count, item)) + "]}";
        mvc.perform(post("/api/staff/appointments/{id}/complete-repair", fixture.appointmentId())
                .with(databaseUser(fixture.staffName()).roles("MECHANIC")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("APPOINTMENT_VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
        assertThat(appointments.findById(fixture.appointmentId()).orElseThrow().getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(documents.findById(fixture.appointmentId())).isEmpty();
    }

    @Test
    void acceptsRoundedCentAndMaximumRepresentableAmount() throws Exception {
        for (String[] values : List.of(new String[]{"0.50", "0.01", "0.01"}, new String[]{"1", "99999999.99", "99999999.99"})) {
            Fixture fixture = fixture(false, false, 0);
            mvc.perform(post("/api/staff/appointments/{id}/complete-repair", fixture.appointmentId())
                    .with(databaseUser(fixture.staffName()).roles("MECHANIC")).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON).content("""
                        {"repairDescription":"Completed repair work","repairItems":[
                          {"type":"LABOR","name":"Test work","quantity":%s,"unitGrossAmount":%s}]}
                        """.formatted(values[0], values[1])))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY_FOR_PICKUP"));
            entityManager.flush();
            assertThat(appointments.findById(fixture.appointmentId()).orElseThrow().getTotalGrossAmount())
                .isEqualByComparingTo(values[2]);
            service.markPickedUp(fixture.staffId(), fixture.appointmentId());
            writePreview(values[2].equals("0.01") ? "rounded-cent" : "maximum-amount",
                invoices.documentFor(fixture.appointmentId()).content());
        }
    }

    @Test
    void pdfContainsEveryItemAcrossMultiplePages() throws Exception {
        Fixture fixture = fixture(true, true, 30);
        byte[] pdf = invoices.documentFor(fixture.appointmentId()).content();
        try (PdfReader reader = new PdfReader(pdf)) {
            assertThat(reader.getNumberOfPages()).isGreaterThan(1);
        }
        String text = pdfText(pdf);
        for (int index = 1; index <= 30; index++) assertThat(text).contains("Pozycja " + index);
        assertThat(text).contains("Razem netto", "24,30", "5,70", "30,00");
        writePreview("multipage", pdf);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedPdfGenerationRollsBackVehiclePickup() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Fixture fixture = transaction.execute(status -> fixture(false, false, 2));
        try {
            doThrow(new InvoiceGenerationException("Simulated PDF failure", null)).when(generator).generate(any());
            assertThatThrownBy(() -> service.markPickedUp(fixture.staffId(), fixture.appointmentId()))
                .isInstanceOf(InvoiceGenerationException.class);
            AppointmentRequest appointment = appointments.findById(fixture.appointmentId()).orElseThrow();
            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.READY_FOR_PICKUP);
            assertThat(appointment.getVehiclePickedUpAt()).isNull();
            assertThat(documents.findById(fixture.appointmentId())).isEmpty();
        } finally {
            reset(generator);
            cleanup(fixture);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrentFirstDownloadsPersistExactlyOneDocument() throws Exception {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        Fixture fixture = transaction.execute(status -> fixture(false, true, 2));
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<byte[]>> results = IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out waiting for download");
                return invoices.documentFor(fixture.appointmentId()).content();
            })).toList();
            start.countDown();
            assertThat(results.getFirst().get(15, TimeUnit.SECONDS)).isEqualTo(results.getLast().get(15, TimeUnit.SECONDS));
            assertThat(documents.findById(fixture.appointmentId())).isPresent();
        } finally {
            cleanup(fixture);
        }
    }

    private void cleanup(Fixture fixture) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                appointments.deleteById(fixture.appointmentId());
                appointments.flush();
                vehicles.deleteById(fixture.vehicleId());
                profiles.delete(profiles.findByUser_Id(fixture.clientId()).orElseThrow());
                users.deleteById(fixture.clientId());
                users.deleteById(fixture.staffId());
        });
    }

    private InvoiceSnapshot issueAndReload(Long id) {
        invoices.documentFor(id);
        entityManager.flush();
        entityManager.clear();
        return documents.findById(id).orElseThrow().getSnapshot();
    }

    private Fixture fixture(boolean company, boolean completed, int itemCount) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AppUser client = users.save(new AppUser("invoice-" + suffix, suffix + "@example.test", "unused", UserRole.CLIENT));
        AppUser staff = users.save(new AppUser("staff-" + suffix, "staff-" + suffix + "@example.test", "unused", UserRole.MECHANIC));
        ClientProfile profile = new ClientProfile(client);
        profile.update("Anna", "Żółć", "500600700", suffix + "@example.test", "Długa 10", "50-001", "Wrocław",
            company, company ? "Żółty Serwis sp. z o.o." : null, company ? "1234567890" : null,
            company ? "Firmowa 15" : null, company ? "50-002" : null, company ? "Wrocław" : null);
        profiles.save(profile);
        Vehicle vehicle = vehicles.save(new Vehicle(client, "Toyota", "Yaris", 2020, "KR12345", null));
        AppointmentRequest appointment = appointments.save(new AppointmentRequest(UUID.randomUUID(), AppointmentRequesterType.CLIENT,
            client, vehicle, "Anna", "Żółć", "500600700", suffix + "@example.test", "Toyota", "Yaris", 2020, "KR12345", null,
            Instant.parse("2026-09-01T06:00:00Z"), "Engine needs inspection", Instant.now()));
        appointment.accept(staff, Instant.now());
        if (itemCount > 0) appointment.completeRepair(staff, "Wymiana części i kontrola działania pojazdu.",
            IntStream.rangeClosed(1, itemCount).mapToObj(index -> new RepairItemDraft(index % 2 == 0 ? RepairItemType.PART : RepairItemType.LABOR,
                "Pozycja " + index + " - wymiana elementu i sprawdzenie działania", new BigDecimal("1.00"), new BigDecimal("1.00"))).toList(), Instant.now());
        if (completed) appointment.markPickedUp(staff, Instant.parse("2026-09-03T10:00:00Z"));
        return new Fixture(client.getId(), staff.getId(), vehicle.getId(), appointment.getId(), client.getUsername(), staff.getUsername());
    }

    private String pdfText(byte[] pdf) throws Exception {
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) text.append(extractor.getTextFromPage(page));
            return text.toString();
        }
    }

    private void writePreview(String name, byte[] pdf) throws Exception {
        String directory = System.getProperty("invoice.previewDir");
        if (directory != null) {
            Path target = Path.of(directory);
            Files.createDirectories(target);
            Files.write(target.resolve(name + ".pdf"), pdf);
        }
    }

    private record Fixture(Long clientId, Long staffId, Long vehicleId, Long appointmentId, String clientName, String staffName) {}
}
