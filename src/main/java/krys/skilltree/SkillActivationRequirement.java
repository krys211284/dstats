package krys.skilltree;

import java.util.Objects;

/** Neutralne wymaganie aktywacji, niezależne od głównego zasobu klasy i od runtime. */
public record SkillActivationRequirement(String id, String displayName, int amount,
                                         SkillActivationConsumptionMode consumptionMode) {
    public SkillActivationRequirement {
        if (id == null || id.isBlank() || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Id i nazwa wymagania aktywacji nie mogą być puste.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Ilość wymagana do aktywacji musi być dodatnia.");
        }
        Objects.requireNonNull(consumptionMode, "consumptionMode");
    }
}
