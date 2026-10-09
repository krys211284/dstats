package krys.necromancer.bookofthedead;

/** Kolejność rodzin w źródłowej Księdze Umarłych. */
public enum NecromancerMinionFamily {
    SKELETAL_WARRIOR("Szkieletowi Wojownicy"),
    SKELETAL_MAGE("Szkieletowi Magowie"),
    GOLEM("Golemy");

    private final String displayName;

    NecromancerMinionFamily(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
