package pl.autoserwis.profile;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pl.autoserwis.user.AppUser;

@Entity
@Table(name = "client_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;
    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;
    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;
    @Column(name = "contact_email", nullable = false, length = 254)
    private String contactEmail;
    @Column(name = "address_line", nullable = false, length = 150)
    private String addressLine;
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
    @Column(nullable = false, length = 80)
    private String city;
    @Column(name = "has_company_data", nullable = false)
    private boolean hasCompanyData;
    @Column(name = "company_name", length = 150)
    private String companyName;
    @Column(name = "tax_id", length = 32)
    private String taxId;
    @Column(name = "billing_address_line", length = 150)
    private String billingAddressLine;
    @Column(name = "billing_postal_code", length = 20)
    private String billingPostalCode;
    @Column(name = "billing_city", length = 80)
    private String billingCity;

    public ClientProfile(AppUser user) {
        this.user = user;
    }

    public void update(String firstName, String lastName, String phoneNumber, String contactEmail,
            String addressLine, String postalCode, String city, boolean hasCompanyData,
            String companyName, String taxId, String billingAddressLine,
            String billingPostalCode, String billingCity) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.contactEmail = contactEmail;
        this.addressLine = addressLine;
        this.postalCode = postalCode;
        this.city = city;
        this.hasCompanyData = hasCompanyData;
        this.companyName = companyName;
        this.taxId = taxId;
        this.billingAddressLine = billingAddressLine;
        this.billingPostalCode = billingPostalCode;
        this.billingCity = billingCity;
    }
}
