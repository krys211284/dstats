package krys.verification;

/** Błąd użycia mechaniki, której nie wolno jeszcze dopuścić do kalkulacji. */
public final class VerificationBlockedMechanicException extends RuntimeException {
    public VerificationBlockedMechanicException(String message) {
        super(message);
    }
}
