package fi.digiflow.books.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final BusinessProfileRepository repository;

    public SettingsService(BusinessProfileRepository repository) {
        this.repository = repository;
    }

    /** Returns the profile, creating an empty one on first use. */
    @Transactional
    public BusinessProfile getProfile() {
        return repository.findById(1L).orElseGet(() -> {
            BusinessProfile profile = new BusinessProfile();
            profile.setBusinessName("Toiminimi");
            profile.setBusinessId("0000000-0");
            return repository.save(profile);
        });
    }

    @Transactional
    public void save(BusinessProfile form) {
        form.setId(1L);
        repository.save(form);
    }
}
