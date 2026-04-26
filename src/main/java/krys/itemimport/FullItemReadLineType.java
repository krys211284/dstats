package krys.itemimport;

/** Typ linii pełnego odczytu OCR, niezależny od tego, czy runtime umie ją liczyć. */
public enum FullItemReadLineType {
    ITEM_NAME("Nazwa"),
    TYPE_OR_SLOT("Typ / slot"),
    RARITY("Rzadkość"),
    ITEM_POWER("Moc przedmiotu"),
    BASE_STAT("Bazowa wartość"),
    IMPLICIT("Implicit / linia bazowa"),
    AFFIX("Affix"),
    ASPECT("Aspekt / moc"),
    SOCKET("Gniazdo"),
    OTHER("Inna linia");

    private final String displayName;

    FullItemReadLineType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
