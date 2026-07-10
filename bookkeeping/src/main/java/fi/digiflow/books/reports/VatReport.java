package fi.digiflow.books.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Numbers you need for the OmaVero VAT return for one period.
 *
 * @param outputVat   VAT collected on sales (myynnin vero)
 * @param inputVat    deductible VAT on purchases (vähennettävä vero)
 * @param payable     outputVat - inputVat; negative means a refund
 * @param salesByRate sales broken down per VAT rate, as reported in OmaVero
 */
public record VatReport(
        LocalDate start,
        LocalDate end,
        BigDecimal outputVat,
        BigDecimal inputVat,
        BigDecimal payable,
        List<RateLine> salesByRate) {

    /** Net sales and VAT for one rate. */
    public record RateLine(BigDecimal rate, BigDecimal net, BigDecimal vat) {
    }
}
