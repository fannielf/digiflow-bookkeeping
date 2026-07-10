package fi.digiflow.books.settings;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public String edit(Model model) {
        model.addAttribute("profile", settingsService.getProfile());
        return "settings/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("profile") BusinessProfile profile,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "settings/form";
        }
        settingsService.save(profile);
        redirectAttributes.addFlashAttribute("message", "Asetukset tallennettu.");
        return "redirect:/settings";
    }
}
