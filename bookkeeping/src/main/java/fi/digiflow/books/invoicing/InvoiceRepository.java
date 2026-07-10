package fi.digiflow.books.invoicing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByOrderByInvoiceNumberDesc();

    List<Invoice> findByStatusOrderByDueDateAsc(InvoiceStatus status);

    @Query("select coalesce(max(i.invoiceNumber), 100) from Invoice i")
    int findMaxInvoiceNumber();
}
