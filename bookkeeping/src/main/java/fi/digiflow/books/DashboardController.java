package fi.digiflow.books;

import fi.digiflow.books.invoicing.Invoice;
import fi.digiflow.books.invoicing.InvoiceService;
import fi.digiflow.books.ledger.Entry;
import fi.digiflow.books.ledger.EntryType;
import fi.digiflow.books.ledger.LedgerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    private final LedgerService ledgerService;
    private final InvoiceService invoiceService;

    public DashboardController(LedgerService ledgerService, InvoiceService invoiceService) {
        this.ledgerService = ledgerService;
        this.invoiceService = invoiceService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        int year = LocalDate.now().getYear();
        List<Entry> entries = ledgerService.entriesForYear(year);

        BigDecimal income = sum(entries, EntryType.INCOME);
        BigDecimal expenses = sum(entries, EntryType.EXPENSE);
        List<Invoice> unpaid = invoiceService.listUnpaid();
        long overdueCount = unpaid.stream().filter(Invoice::isOverdue).count();

        model.addAttribute("year", year);
        model.addAttribute("income", income);
        model.addAttribute("expenses", expenses);
        model.addAttribute("result", income.subtract(expenses));
        model.addAttribute("unpaidInvoices", unpaid);
        model.addAttribute("overdueCount", overdueCount);
        return "dashboard";
    }

    private BigDecimal sum(List<Entry> entries, EntryType type) {
        return entries.stream()
                .filter(e -> e.getType() == type)
                .map(Entry::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
