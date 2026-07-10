package fi.digiflow.books.ledger;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/ledger")
public class LedgerController {

    private final LedgerService ledgerService;
    private final SettingsService settingsService;

    public LedgerController(LedgerService ledgerService, SettingsService settingsService) {
        this.ledgerService = ledgerService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public String list(@RequestParam(name = "year", required = false) Integer year, Model model) {
        int selectedYear = (year != null) ? year : LocalDate.now().getYear();
        List<Entry> entries = ledgerService.entriesForYear(selectedYear);

        BigDecimal incomeTotal = sum(entries, EntryType.INCOME);
        BigDecimal expenseTotal = sum(entries, EntryType.EXPENSE);

        model.addAttribute("entries", entries);
        model.addAttribute("year", selectedYear);
        model.addAttribute("incomeTotal", incomeTotal);
        model.addAttribute("expenseTotal", expenseTotal);
        model.addAttribute("result", incomeTotal.subtract(expenseTotal));
        return "ledger/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        Entry entry = new Entry();
        entry.setDate(LocalDate.now());
        entry.setType(EntryType.EXPENSE);
        // Default to the general rate for VAT-registered users so 0 % is a
        // deliberate choice, not an accident.
        if (settingsService.getProfile().isVatRegistered()) {
            entry.setVatRate(new BigDecimal("25.5"));
        }
        model.addAttribute("entry", entry);
        addFormAttributes(model);
        return "ledger/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("entry", ledgerService.getEntry(id));
        addFormAttributes(model);
        return "ledger/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("entry") Entry entry,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model);
            return "ledger/form";
        }
        ledgerService.save(entry);
        redirectAttributes.addFlashAttribute("message", "Kirjaus tallennettu.");
        return "redirect:/ledger?year=" + entry.getDate().getYear();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        int year = ledgerService.getEntry(id).getDate().getYear();
        ledgerService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Kirjaus poistettu.");
        return "redirect:/ledger?year=" + year;
    }

    private void addFormAttributes(Model model) {
        model.addAttribute("vatRates", LedgerService.VAT_RATES);
        model.addAttribute("types", EntryType.values());
    }

    private BigDecimal sum(List<Entry> entries, EntryType type) {
        return entries.stream()
                .filter(e -> e.getType() == type)
                .map(Entry::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
