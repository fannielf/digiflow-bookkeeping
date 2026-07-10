package fi.digiflow.books.invoicing;

import fi.digiflow.books.ledger.Entry;
import fi.digiflow.books.ledger.EntryType;
import fi.digiflow.books.ledger.LedgerService;
import fi.digiflow.books.settings.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final LedgerService ledgerService;
    private final SettingsService settingsService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          LedgerService ledgerService,
                          SettingsService settingsService) {
        this.invoiceRepository = invoiceRepository;
        this.ledgerService = ledgerService;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public List<Invoice> listAll() {
        return invoiceRepository.findAllByOrderByInvoiceNumberDesc();
    }

    @Transactional(readOnly = true)
    public List<Invoice> listUnpaid() {
        return invoiceRepository.findByStatusOrderByDueDateAsc(InvoiceStatus.SENT);
    }

    @Transactional(readOnly = true)
    public Invoice get(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    /**
     * Creates or updates an invoice from the submitted form.
     * Only drafts can be edited. Blank line rows are skipped.
     */
    @Transactional
    public Invoice save(Invoice form) {
        Invoice invoice;
        if (form.getId() != null) {
            invoice = get(form.getId());
            requireDraft(invoice);
            invoice.getLines().clear();
        } else {
            invoice = new Invoice();
        }

        invoice.setCustomer(form.getCustomer());
        invoice.setIssueDate(form.getIssueDate());
        invoice.setDueDate(form.getDueDate());
        invoice.setNotes(form.getNotes());

        boolean vatRegistered = settingsService.getProfile().isVatRegistered();

        for (InvoiceLine submitted : form.getLines()) {
            if (submitted.isBlank()) {
                continue;
            }
            InvoiceLine line = new InvoiceLine();
            line.setDescription(submitted.getDescription());
            line.setQuantity(orDefault(submitted.getQuantity(), BigDecimal.ONE));
            line.setUnitPrice(orDefault(submitted.getUnitPrice(), BigDecimal.ZERO));
            // Not in the VAT register -> everything is invoiced at 0 %.
            line.setVatRate(vatRegistered
                    ? orDefault(submitted.getVatRate(), BigDecimal.ZERO)
                    : BigDecimal.ZERO);
            invoice.addLine(line);
        }

        if (invoice.getInvoiceNumber() == null) {
            int number = invoiceRepository.findMaxInvoiceNumber() + 1;
            invoice.setInvoiceNumber(number);
            invoice.setReferenceNumber(FinnishReference.forInvoiceNumber(number));
        }
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void markSent(Long id) {
        Invoice invoice = get(id);
        requireDraft(invoice);
        if (invoice.getLines().isEmpty()) {
            throw new IllegalStateException("Laskulla ei ole yhtään riviä.");
        }
        invoice.setStatus(InvoiceStatus.SENT);
    }

    /**
     * Marks the invoice paid and records the income in the ledger.
     * One ledger entry is created per VAT rate so the VAT report stays correct.
     */
    @Transactional
    public void markPaid(Long id) {
        Invoice invoice = get(id);
        if (invoice.getStatus() != InvoiceStatus.SENT) {
            throw new IllegalStateException("Vain lähetetyn laskun voi merkitä maksetuksi.");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidDate(LocalDate.now());

        for (Map.Entry<BigDecimal, BigDecimal[]> group : invoice.getTotalsByRate().entrySet()) {
            BigDecimal rate = group.getKey();
            BigDecimal net = group.getValue()[0];
            BigDecimal vat = group.getValue()[1];

            Entry entry = new Entry();
            entry.setDate(invoice.getPaidDate());
            entry.setType(EntryType.INCOME);
            entry.setDescription("Lasku " + invoice.getInvoiceNumber()
                    + " – " + invoice.getCustomer().getName());
            entry.setCounterparty(invoice.getCustomer().getName());
            entry.setGrossAmount(net.add(vat));
            entry.setVatRate(rate);
            entry.setNetAmount(net);
            entry.setVatAmount(vat);
            entry.setInvoiceId(invoice.getId());
            ledgerService.saveWithExplicitVat(entry);
        }
    }

    @Transactional
    public void delete(Long id) {
        Invoice invoice = get(id);
        requireDraft(invoice);
        invoiceRepository.delete(invoice);
    }

    private void requireDraft(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Vain luonnosta voi muokata tai poistaa.");
        }
    }

    private BigDecimal orDefault(BigDecimal value, BigDecimal fallback) {
        return (value != null) ? value : fallback;
    }
}
