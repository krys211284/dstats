package krys.socketing;

/** Kontekst efektu gema zależny od typu itemu z gniazdem. */
public enum SocketEffectContext {
    WEAPON("Broń"),
    ARMOR("Pancerz"),
    JEWELRY("Biżuteria");

    private final String displayName;

    SocketEffectContext(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
