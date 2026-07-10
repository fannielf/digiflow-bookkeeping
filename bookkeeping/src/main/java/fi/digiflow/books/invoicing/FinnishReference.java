package fi.digiflow.books.invoicing;

/**
 * Finnish bank reference number (viitenumero).
 *
 * The check digit is calculated with the 7-3-1 method: multiply digits
 * from right to left by 7, 3, 1, 7, 3, 1, ... sum the products, and the
 * check digit is the amount needed to reach the next multiple of ten.
 * See: https://www.finanssiala.fi (Muodostamisohjeet: kotimainen viitenumero)
 */
public final class FinnishReference {

    private FinnishReference() {
    }

    public static String forInvoiceNumber(int invoiceNumber) {
        if (invoiceNumber <= 0) {
            throw new IllegalArgumentException("Invoice number must be positive");
        }
        String base = Integer.toString(invoiceNumber);
        int[] weights = {7, 3, 1};
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            int digit = base.charAt(base.length() - 1 - i) - '0';
            sum += digit * weights[i % 3];
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return base + checkDigit;
    }
}
