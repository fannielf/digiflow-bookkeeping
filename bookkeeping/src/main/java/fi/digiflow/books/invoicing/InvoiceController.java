package fi.digiflow.books.invoicing;

import fi.digiflow.books.ledger.LedgerService;
import fi.digiflow.books.settings.BusinessProfile;
import fi.digiflow.books.settings.SettingsService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private static final int FORM_LINE_ROWS = 6;

    private final InvoiceService invoiceService;
    private final CustomerRepository customerRepository;
    private final SettingsService settingsService;

    public InvoiceController(InvoiceService invoiceService,
                             CustomerRepository customerRepository,
                             SettingsService settingsService) {
        this.invoiceService = invoiceService;
        this.customerRepository = customerRepository;
        this.settingsService = settingsService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("invoices", invoiceService.listAll());
        return "invoices/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        BusinessProfile profile = settingsService.getProfile();
        Invoice invoice = new Invoice();
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(profile.getDefaultPaymentTermDays()));
        padLines(invoice);
        model.addAttribute("invoice", invoice);
        addFormAttributes(model);
        return "invoices/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           RedirectAttributes redirectAttributes) {
        Invoice invoice = invoiceService.get(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            redirectAttributes.addFlashAttribute("error", "Vain luonnosta voi muokata.");
            return "redirect:/invoices/" + id;
        }
        // Copy into an unmanaged form object before padding: adding blank lines
        // to the JPA-managed entity would persist them via cascade on flush.
        Invoice form = copyForForm(invoice);
        padLines(form);
        model.addAttribute("invoice", form);
        addFormAttributes(model);
        return "invoices/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("invoice") Invoice invoice,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            padLines(invoice);
            addFormAttributes(model);
            return "invoices/form";
        }
        try {
            Invoice saved = invoiceService.save(invoice);
            redirectAttributes.addFlashAttribute("message",
                    "Lasku " + saved.getInvoiceNumber() + " tallennettu.");
            return "redirect:/invoices/" + saved.getId();
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices/" + invoice.getId();
        }
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoiceService.get(id));
        model.addAttribute("profile", settingsService.getProfile());
        return "invoices/view";
    }

    @PostMapping("/{id}/send")
    public String markSent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.markSent(id);
            redirectAttributes.addFlashAttribute("message", "Lasku merkitty lähetetyksi.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/pay")
    public String markPaid(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.markPaid(id);
            redirectAttributes.addFlashAttribute("message",
                    "Lasku merkitty maksetuksi ja tulo kirjattu kirjanpitoon.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoiceService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Lasku poistettu.");
            return "redirect:/invoices";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/invoices/" + id;
        }
    }

    private Invoice copyForForm(Invoice source) {
        Invoice form = new Invoice();
        form.setId(source.getId());
        form.setInvoiceNumber(source.getInvoiceNumber());
        form.setCustomer(source.getCustomer());
        form.setIssueDate(source.getIssueDate());
        form.setDueDate(source.getDueDate());
        form.setNotes(source.getNotes());
        for (InvoiceLine line : source.getLines()) {
            InvoiceLine copy = new InvoiceLine();
            copy.setDescription(line.getDescription());
            copy.setQuantity(line.getQuantity());
            copy.setUnitPrice(line.getUnitPrice());
            copy.setVatRate(line.getVatRate());
            form.getLines().add(copy);
        }
        return form;
    }

    private void padLines(Invoice invoice) {
        // New rows default to the general VAT rate so a registered seller
        // doesn't accidentally invoice at 0 %.
        BigDecimal defaultRate = settingsService.getProfile().isVatRegistered()
                ? new BigDecimal("25.5")
                : BigDecimal.ZERO;
        while (invoice.getLines().size() < FORM_LINE_ROWS) {
            InvoiceLine line = new InvoiceLine();
            line.setVatRate(defaultRate);
            invoice.getLines().add(line);
        }
    }

    private void addFormAttributes(Model model) {
        model.addAttribute("customers", customerRepository.findAll());
        model.addAttribute("vatRates", LedgerService.VAT_RATES);
        model.addAttribute("vatRegistered", settingsService.getProfile().isVatRegistered());
    }
}
