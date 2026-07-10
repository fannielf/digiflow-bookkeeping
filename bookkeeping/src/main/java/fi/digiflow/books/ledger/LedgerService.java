package fi.digiflow.books.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class LedgerService {

    public static final List<BigDecimal> VAT_RATES = List.of(
            new BigDecimal("25.5"),
            new BigDecimal("14"),
            new BigDecimal("10"),
            BigDecimal.ZERO);

    private final EntryRepository entryRepository;

    public LedgerService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    @Transactional(readOnly = true)
    public List<Entry> entriesForYear(int year) {
        return entryRepository.findByDateBetweenOrderByDateAscVoucherNumberAsc(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    @Transactional(readOnly = true)
    public Entry getEntry(Long id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + id));
    }

    @Transactional
    public Entry save(Entry entry) {
        calculateVat(entry);
        if (entry.getVoucherNumber() == null) {
            entry.setVoucherNumber(nextVoucherNumber(entry.getDate().getYear()));
        }
        return entryRepository.save(entry);
    }

    /**
     * Saves an entry whose net and VAT amounts are already known
     * (e.g. taken from an invoice), skipping recalculation so the
     * ledger matches the invoice to the cent.
     */
    @Transactional
    public Entry saveWithExplicitVat(Entry entry) {
        if (entry.getVoucherNumber() == null) {
            entry.setVoucherNumber(nextVoucherNumber(entry.getDate().getYear()));
        }
        return entryRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        entryRepository.deleteById(id);
    }

    /**
     * Splits the gross amount into net + VAT.
     * Example: 125.50 gross at 25.5 % -> net 100.00, VAT 25.50.
     */
    private void calculateVat(Entry entry) {
        BigDecimal gross = entry.getGrossAmount();
        BigDecimal rate = entry.getVatRate();
        BigDecimal divisor = BigDecimal.ONE.add(
                rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        BigDecimal net = gross.divide(divisor, 2, RoundingMode.HALF_UP);
        entry.setNetAmount(net);
        entry.setVatAmount(gross.subtract(net));
    }

    private int nextVoucherNumber(int year) {
        int max = entryRepository.findMaxVoucherNumber(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return max + 1;
    }
}
