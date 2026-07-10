package fi.digiflow.books.settings;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Your own business details, printed on invoices.
 * Stored as a single row with id = 1.
 */
@Entity
@Table(name = "business_profile")
public class BusinessProfile {

    @Id
    private Long id = 1L;

    @NotBlank
    private String businessName;

    private String ownerName;

    /** Y-tunnus, e.g. 1234567-8 */
    @NotBlank
    private String businessId;

    private String streetAddress;
    private String postalCode;
    private String city;
    private String email;
    private String phone;

    /** Bank account for invoices */
    private String iban;
    private String bic;

    /** Are you in the VAT register (arvonlisäverovelvollisten rekisteri)? */
    private boolean vatRegistered = false;

    @NotNull
    @Min(0)
    private Integer defaultPaymentTermDays = 14;

    /** VAT id shown on invoices when VAT-registered: FI + Y-tunnus without the dash. */
    public String getVatId() {
        if (businessId == null) {
            return "";
        }
        return "FI" + businessId.replace("-", "");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public boolean isVatRegistered() {
        return vatRegistered;
    }

    public void setVatRegistered(boolean vatRegistered) {
        this.vatRegistered = vatRegistered;
    }

    public Integer getDefaultPaymentTermDays() {
        return defaultPaymentTermDays;
    }

    public void setDefaultPaymentTermDays(Integer defaultPaymentTermDays) {
        this.defaultPaymentTermDays = defaultPaymentTermDays;
    }
}
