package krys.socketing;

/** Opis prezentacyjny efektu gema dla jednego kontekstu itemu. */
public final class GemEffect {
    private final SocketEffectContext context;
    private final String displayText;

    public GemEffect(SocketEffectContext context, String displayText) {
        if (context == null) {
            throw new IllegalArgumentException("Kontekst efektu gema jest wymagany.");
        }
        if (displayText == null || displayText.isBlank()) {
            throw new IllegalArgumentException("Opis efektu gema jest wymagany.");
        }
        this.context = context;
        this.displayText = displayText;
    }

    public SocketEffectContext getContext() {
        return context;
    }

    public String getDisplayText() {
        return displayText;
    }
}
