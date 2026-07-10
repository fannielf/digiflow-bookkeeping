package fi.digiflow.books.ledger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

class LedgerServiceTest {

    private EntryRepository entryRepository;
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        entryRepository = mock(EntryRepository.class);
        when(entryRepository.save(any(Entry.class))).then(returnsFirstArg());
        when(entryRepository.findMaxVoucherNumber(any(), any())).thenReturn(0);
        ledgerService = new LedgerService(entryRepository);
    }

    @Test
    void splitsGrossIntoNetAndVatAtGeneralRate() {
        Entry entry = newEntry("125.50", "25.5");

        Entry saved = ledgerService.save(entry);

        assertEquals(new BigDecimal("100.00"), saved.getNetAmount());
        assertEquals(new BigDecimal("25.50"), saved.getVatAmount());
    }

    @Test
    void splitsGrossIntoNetAndVatAtReducedRate() {
        Entry entry = newEntry("100.00", "14");

        Entry saved = ledgerService.save(entry);

        assertEquals(new BigDecimal("87.72"), saved.getNetAmount());
        assertEquals(new BigDecimal("12.28"), saved.getVatAmount());
    }

    @Test
    void zeroRateMeansNoVat() {
        Entry entry = newEntry("50.00", "0");

        Entry saved = ledgerService.save(entry);

        assertEquals(new BigDecimal("50.00"), saved.getNetAmount());
        assertEquals(new BigDecimal("0.00"), saved.getVatAmount());
    }

    @Test
    void assignsNextVoucherNumberWithinYear() {
        when(entryRepository.findMaxVoucherNumber(
                eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(41);

        Entry saved = ledgerService.save(newEntry("10.00", "0"));

        assertEquals(42, saved.getVoucherNumber());
    }

    private Entry newEntry(String gross, String vatRate) {
        Entry entry = new Entry();
        entry.setDate(LocalDate.of(2026, 7, 9));
        entry.setType(EntryType.EXPENSE);
        entry.setDescription("Testikirjaus");
        entry.setGrossAmount(new BigDecimal(gross));
        entry.setVatRate(new BigDecimal(vatRate));
        return entry;
    }
}
