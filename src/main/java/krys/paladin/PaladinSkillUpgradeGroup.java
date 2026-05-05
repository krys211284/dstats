package krys.paladin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Grupa ulepszeń skilla Paladyna. */
public final class PaladinSkillUpgradeGroup {
    private final String id;
    private final String name;
    private final List<PaladinSkillUpgrade> upgrades;

    public PaladinSkillUpgradeGroup(String id, String name, List<PaladinSkillUpgrade> upgrades) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.upgrades = Collections.unmodifiableList(new ArrayList<>(upgrades));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<PaladinSkillUpgrade> getUpgrades() {
        return upgrades;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
