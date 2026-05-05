package krys.verification;

/** Obszar kalkulacji albo zachowania, na który może wpływać niepewna mechanika. */
public enum VerificationImpact {
    DPS("dps"),
    RESOURCE("resource"),
    COOLDOWN("cooldown"),
    STATUS("status"),
    SURVIVABILITY("survivability"),
    POSITIONING("positioning");

    private final String id;

    VerificationImpact(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
