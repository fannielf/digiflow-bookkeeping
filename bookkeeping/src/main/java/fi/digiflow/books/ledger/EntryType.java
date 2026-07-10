package fi.digiflow.books.ledger;

public enum EntryType {
    INCOME("Tulo"),
    EXPENSE("Meno");

    private final String label;

    EntryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
