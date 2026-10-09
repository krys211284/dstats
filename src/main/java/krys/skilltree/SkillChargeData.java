package krys.skilltree;

/** Niemutowalne źródłowe ładunki; odnowienie dotyczy jednego ładunku, bez obliczania cyklu użycia. */
public record SkillChargeData(int baseCharges, int chargeCooldownSeconds) {
    public SkillChargeData {
        if (baseCharges <= 0 || chargeCooldownSeconds <= 0) {
            throw new IllegalArgumentException("Liczba ładunków i czas odnowienia ładunku muszą być dodatnie.");
        }
    }
}
