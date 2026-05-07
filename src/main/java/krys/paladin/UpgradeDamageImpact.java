package krys.paladin;

import java.util.Objects;

/** Opisowy wpływ pojedynczego ulepszenia na obrażenia, bez wykonywania kalkulacji. */
public final class UpgradeDamageImpact {
    private final String upgradeName;
    private final String groupId;
    private final UpgradeDamageImpactType type;
    private final String description;
    private final Integer damagePercent;

    public UpgradeDamageImpact(String upgradeName,
                               String groupId,
                               UpgradeDamageImpactType type,
                               String description,
                               Integer damagePercent) {
        this.upgradeName = requireText(upgradeName, "upgradeName");
        this.groupId = requireText(groupId, "groupId");
        this.type = Objects.requireNonNull(type, "type");
        this.description = requireText(description, "description");
        this.damagePercent = damagePercent;
    }

    public static UpgradeDamageImpact fromUpgrade(String groupId, PaladinSkillUpgrade upgrade) {
        UpgradeDamageImpactType type = classify(upgrade);
        return new UpgradeDamageImpact(
                upgrade.getName(),
                groupId,
                type,
                descriptionFor(type),
                null
        );
    }

    public String getUpgradeName() {
        return upgradeName;
    }

    public String getGroupId() {
        return groupId;
    }

    public UpgradeDamageImpactType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDamagePercent() {
        return damagePercent;
    }

    public boolean affectsDamage() {
        return switch (type) {
            case DIRECT_DAMAGE_PERCENT,
                    ADDITIONAL_HIT,
                    DAMAGE_OVER_TIME,
                    BURST_DAMAGE,
                    CONDITIONAL_DAMAGE,
                    NEEDS_VERIFICATION -> true;
            case STATUS_OR_UTILITY, COOLDOWN_OR_COST, NO_DAMAGE_IMPACT -> false;
        };
    }

    private static UpgradeDamageImpactType classify(PaladinSkillUpgrade upgrade) {
        String id = upgrade.getId();
        String name = upgrade.getName().toLowerCase();

        if (upgrade.getStatus() == PaladinSkillTreeStatus.UNSUPPORTED) {
            return UpgradeDamageImpactType.NEEDS_VERIFICATION;
        }
        if (containsAny(id, name, "obrazen", "obrażeń", "cios", "uderzenie", "pocisk", "rozdarcie", "klinga", "zemsta", "odwet", "cierni", "osad", "osąd", "porazenie", "porażenie")) {
            return UpgradeDamageImpactType.NEEDS_VERIFICATION;
        }
        if (containsAny(id, name, "spowolnienie", "odsloniecie", "odsłonięcie", "oslabienie", "osłabienie", "nieograniczenie", "umocnienie", "nieustepliwosc", "nieustępliwość", "blok", "zdrowia", "pancerz", "leczenie", "krzepkosc", "krzepkość", "rozmiar", "dodatkowe_cele", "dodatkowy_ladunek", "dodatkowy ładunek")) {
            return UpgradeDamageImpactType.STATUS_OR_UTILITY;
        }
        if (containsAny(id, name, "koszt", "odnowienia", "cooldown", "szybkosc", "szybkość użycia", "szybkości użycia", "generowanie", "zasob")) {
            return UpgradeDamageImpactType.COOLDOWN_OR_COST;
        }
        return UpgradeDamageImpactType.NO_DAMAGE_IMPACT;
    }

    private static boolean containsAny(String id, String name, String... fragments) {
        for (String fragment : fragments) {
            if (id.contains(fragment) || name.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static String descriptionFor(UpgradeDamageImpactType type) {
        return switch (type) {
            case DIRECT_DAMAGE_PERCENT -> "bezpośrednia zmiana procentu obrażeń";
            case ADDITIONAL_HIT -> "dodatkowe trafienie";
            case DAMAGE_OVER_TIME -> "obrażenia w czasie";
            case BURST_DAMAGE -> "obrażenia wybuchowe";
            case CONDITIONAL_DAMAGE -> "obrażenia warunkowe";
            case STATUS_OR_UTILITY -> "status albo efekt użytkowy, bez bezpośredniego doliczania obrażeń";
            case COOLDOWN_OR_COST -> "koszt, zasób albo odnowienie, bez bezpośredniego doliczania obrażeń";
            case NO_DAMAGE_IMPACT -> "brak bezpośredniego wpływu na obrażenia";
            case NEEDS_VERIFICATION -> "możliwy wpływ na obrażenia, wymaga weryfikacji";
        };
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
