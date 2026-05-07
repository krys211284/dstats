package krys.web;

import krys.paladin.PaladinSkillTreeType;
import krys.paladin.PaladinTreeSkill;
import krys.paladin.UpgradeDamageImpact;
import krys.ranking.PaladinSkillDamageRankingEntry;

import java.util.List;
import java.util.stream.Collectors;

/** Wiersz widoku rankingu łączący opis rankingu z typem umiejętności z rejestru drzewa. */
public final class DamageRankingRow {
    private final PaladinSkillDamageRankingEntry entry;
    private final PaladinSkillTreeType type;
    private final String damageComponentsDescription;
    private final String damageAffectingUpgradesDescription;
    private final String nonDamageUpgradesDescription;

    public DamageRankingRow(PaladinSkillDamageRankingEntry entry, PaladinTreeSkill treeSkill) {
        this.entry = entry;
        this.type = treeSkill.getType();
        this.damageComponentsDescription = describeDamageComponents(treeSkill);
        this.damageAffectingUpgradesDescription = describeUpgrades(treeSkill.getUpgradeDamageImpacts(), true);
        this.nonDamageUpgradesDescription = describeUpgrades(treeSkill.getUpgradeDamageImpacts(), false);
    }

    public PaladinSkillDamageRankingEntry getEntry() {
        return entry;
    }

    public PaladinSkillTreeType getType() {
        return type;
    }

    public String getDamageComponentsDescription() {
        return damageComponentsDescription;
    }

    public String getDamageAffectingUpgradesDescription() {
        return damageAffectingUpgradesDescription;
    }

    public String getNonDamageUpgradesDescription() {
        return nonDamageUpgradesDescription;
    }

    public Integer getBaseDamagePercentAtRank1() {
        return entry.getBaseDamagePercentAtRank1();
    }

    public Integer getBaseDamagePercentAtTreeMaxRank() {
        return entry.getBaseDamagePercentAtTreeMaxRank();
    }

    public boolean isDpsCalculable() {
        return entry.getDamagePerUse() != null
                || entry.getTheoreticalDps() != null;
    }

    public Double getSingleTargetDps() {
        return entry.getTheoreticalDps();
    }

    private static String describeDamageComponents(PaladinTreeSkill treeSkill) {
        if (!treeSkill.getBaseDamagePercentRanks().isEmpty()) {
            return "prosta tabela bazowych procentów obrażeń 1..15";
        }
        return switch (treeSkill.getType()) {
            case DAMAGE -> "brak prostej tabeli bazowych procentów; wymaga weryfikacji komponentów";
            case NON_DAMAGE, SUPPORT, DEFENSIVE, MOBILITY, SPECIAL, UNCLASSIFIED -> "brak prostego komponentu obrażeń w bieżącym modelu";
        };
    }

    private static String describeUpgrades(List<UpgradeDamageImpact> impacts, boolean affectingDamage) {
        String description = impacts.stream()
                .filter(impact -> impact.affectsDamage() == affectingDamage)
                .map(DamageRankingRow::formatImpact)
                .collect(Collectors.joining("; "));
        return description.isBlank() ? "brak" : description;
    }

    private static String formatImpact(UpgradeDamageImpact impact) {
        String value = impact.getDamagePercent() == null ? "" : " (" + impact.getDamagePercent() + "%)";
        return impact.getGroupId() + ": " + impact.getUpgradeName() + " - " + impact.getDescription() + value;
    }
}
