package krys.paladin;

import krys.skilltree.UpgradeDamageImpact;
import krys.skilltree.UpgradeDamageImpactType;

/** Interpretacja wpływu lokalnych ulepszeń Paladyna na obrażenia. */
public final class PaladinUpgradeDamageImpacts {
    private PaladinUpgradeDamageImpacts() { }

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

}
