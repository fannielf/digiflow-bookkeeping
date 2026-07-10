package fi.digiflow.books.reports;

import fi.digiflow.books.settings.SettingsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/reports/vat")
public class VatReportController {

    private final VatReportService vatReportService;
    private final SettingsService settingsService;

    public VatReportController(VatReportService vatReportService,
                               SettingsService settingsService) {
        this.vatReportService = vatReportService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public String report(@RequestParam(name = "year", required = false) Integer year,
                         @RequestParam(name = "quarter", required = false) Integer quarter,
                         Model model) {
        LocalDate today = LocalDate.now();
        int selectedYear = (year != null) ? year : today.getYear();
        int selectedQuarter = (quarter != null) ? quarter : (today.getMonthValue() - 1) / 3 + 1;
        // Guard against hand-edited URLs like ?quarter=5
        selectedQuarter = Math.max(1, Math.min(4, selectedQuarter));

        model.addAttribute("report", vatReportService.quarterReport(selectedYear, selectedQuarter));
        model.addAttribute("year", selectedYear);
        model.addAttribute("quarter", selectedQuarter);
        model.addAttribute("vatRegistered", settingsService.getProfile().isVatRegistered());
        return "reports/vat";
    }
}
