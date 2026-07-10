package fi.digiflow.books.invoicing;

public enum InvoiceStatus {
    DRAFT("Luonnos"),
    SENT("Lähetetty"),
    PAID("Maksettu");

    private final String label;

    InvoiceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
