package fi.digiflow.books.invoicing;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Lets Spring bind the invoice form's customer <select> (which submits the
 * customer id as text) straight into the Invoice.customer field.
 */
@Component
public class StringToCustomerConverter implements Converter<String, Customer> {

    private final CustomerRepository customerRepository;

    public StringToCustomerConverter(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer convert(String source) {
        if (source.isBlank()) {
            return null;
        }
        return customerRepository.findById(Long.valueOf(source)).orElse(null);
    }
}
