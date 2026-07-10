package fi.digiflow.books.invoicing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinnishReferenceTest {

    @Test
    void calculatesCheckDigitWith731Method() {
        // 101 -> 1*7 + 0*3 + 1*1 = 8 -> check digit 2
        assertEquals("1012", FinnishReference.forInvoiceNumber(101));
        // 102 -> 2*7 + 0*3 + 1*1 = 15 -> check digit 5
        assertEquals("1025", FinnishReference.forInvoiceNumber(102));
        // 1234567 -> sum 118 -> check digit 2
        assertEquals("12345672", FinnishReference.forInvoiceNumber(1234567));
    }

    @Test
    void checkDigitIsZeroWhenSumIsMultipleOfTen() {
        // 19 -> 9*7 + 1*3 = 66 ... find one: 55 -> 5*7 + 5*3 = 50 -> check digit 0
        assertEquals("550", FinnishReference.forInvoiceNumber(55));
    }

    @Test
    void rejectsNonPositiveNumbers() {
        assertThrows(IllegalArgumentException.class, () -> FinnishReference.forInvoiceNumber(0));
    }
}
