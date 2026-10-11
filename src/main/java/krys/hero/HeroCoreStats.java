package krys.hero;

/** Cztery podstawowe statystyki, niezależne od klasy i silnika obrażeń. */
public record HeroCoreStats(int strength, int intelligence, int willpower, int dexterity) {
    public HeroCoreStats {
        if (strength < 0 || intelligence < 0 || willpower < 0 || dexterity < 0) {
            throw new IllegalArgumentException("Podstawowe statystyki muszą być nieujemne.");
        }
    }
}
