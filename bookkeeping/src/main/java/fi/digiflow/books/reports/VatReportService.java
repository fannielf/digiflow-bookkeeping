package fi.digiflow.books.reports;

import fi.digiflow.books.ledger.Entry;
import fi.digiflow.books.ledger.EntryRepository;
import fi.digiflow.books.ledger.EntryType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class VatReportService {

    private final EntryRepository entryRepository;

    public VatReportService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    /**
     * Builds a VAT summary for a quarter. This uses the payment date of each
     * entry (maksuperusteinen alv), which small businesses may use in Finland.
     */
    @Transactional(readOnly = true)
    public VatReport quarterReport(int year, int quarter) {
        LocalDate start = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate end = start.plusMonths(3).minusDays(1);

        List<Entry> entries =
                entryRepository.findByDateBetweenOrderByDateAscVoucherNumberAsc(start, end);

        BigDecimal outputVat = BigDecimal.ZERO;
        BigDecimal inputVat = BigDecimal.ZERO;
        // Sales per rate, highest rate first.
        Map<BigDecimal, BigDecimal[]> salesByRate = new TreeMap<>(Comparator.reverseOrder());

        for (Entry entry : entries) {
            BigDecimal net = zeroIfNull(entry.getNetAmount());
            BigDecimal vat = zeroIfNull(entry.getVatAmount());
            if (entry.getType() == EntryType.INCOME) {
                outputVat = outputVat.add(vat);
                BigDecimal[] totals = salesByRate.computeIfAbsent(entry.getVatRate(),
                        r -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                totals[0] = totals[0].add(net);
                totals[1] = totals[1].add(vat);
            } else {
                inputVat = inputVat.add(vat);
            }
        }

        List<VatReport.RateLine> rateLines = new ArrayList<>();
        salesByRate.forEach((rate, totals) ->
                rateLines.add(new VatReport.RateLine(rate, totals[0], totals[1])));

        return new VatReport(start, end, outputVat, inputVat,
                outputVat.subtract(inputVat), rateLines);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return (value != null) ? value : BigDecimal.ZERO;
    }
}
