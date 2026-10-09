package krys.skilltree;

import java.util.List;

/** Grupa ulepszeń skilla postaci. */
public final class SkillUpgradeGroup {
    private final String id;
    private final String name;
    private final List<SkillUpgrade> upgrades;

    public SkillUpgradeGroup(String id, String name, List<SkillUpgrade> upgrades) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.upgrades = List.copyOf(upgrades);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<SkillUpgrade> getUpgrades() {
        return upgrades;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
