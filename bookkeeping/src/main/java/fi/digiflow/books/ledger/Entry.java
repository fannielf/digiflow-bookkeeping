package fi.digiflow.books.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One ledger entry (single-entry bookkeeping, yhdenkertainen kirjanpito).
 * You enter the gross amount from the receipt plus the VAT rate;
 * net and VAT amounts are calculated and stored by LedgerService.
 */
@Entity
@Table(name = "entries")
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryType type;

    @NotBlank
    private String description;

    /** Who you paid or who paid you. */
    private String counterparty;

    /** Amount including VAT, as on the receipt. */
    @NotNull
    @DecimalMin(value = "0.01", message = "Summan pitää olla suurempi kuin 0")
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal grossAmount;

    /** VAT percentage: 25.5, 14, 10 or 0. */
    @NotNull
    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal netAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal vatAmount;

    /** Voucher (tosite) number, sequential per calendar year. */
    private Integer voucherNumber;

    /** Set when this entry was created automatically from a paid invoice. */
    private Long invoiceId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public EntryType getType() {
        return type;
    }

    public void setType(EntryType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(BigDecimal netAmount) {
        this.netAmount = netAmount;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(BigDecimal vatAmount) {
        this.vatAmount = vatAmount;
    }

    public Integer getVoucherNumber() {
        return voucherNumber;
    }

    public void setVoucherNumber(Integer voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }
}
