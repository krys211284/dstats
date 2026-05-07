package krys.web;

import krys.paladin.DamagePercentComponentRankTable;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.PaladinTreeSkill;
import krys.paladin.UpgradeDamageImpact;
import krys.paladin.UpgradeDamageModifier;
import krys.ranking.PaladinSkillDamageRankingEntry;

import java.util.List;

/** Wiersz widoku rankingu łączący opis rankingu z typem umiejętności z rejestru drzewa. */
public final class DamageRankingRow {
    private final PaladinSkillDamageRankingEntry entry;
    private final PaladinSkillTreeType type;
    private final String damageComponentsDescription;
    private final DamagePercentComponentRankTable componentDamagePercentRanks;
    private final List<UpgradeDamageImpact> upgradeDamageImpacts;
    private final List<UpgradeDamageModifier> upgradeDamageModifiers;

    public DamageRankingRow(PaladinSkillDamageRankingEntry entry, PaladinTreeSkill treeSkill) {
        this.entry = entry;
        this.type = treeSkill.getType();
        this.damageComponentsDescription = describeDamageComponents(treeSkill);
        this.componentDamagePercentRanks = treeSkill.getComponentDamagePercentRanks();
        this.upgradeDamageImpacts = List.copyOf(treeSkill.getUpgradeDamageImpacts());
        this.upgradeDamageModifiers = List.copyOf(treeSkill.getUpgradeDamageModifiers());
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

    public DamagePercentComponentRankTable getComponentDamagePercentRanks() {
        return componentDamagePercentRanks;
    }

    public List<UpgradeDamageImpact> getUpgradeDamageImpactsForGroup(String groupId) {
        return upgradeDamageImpacts.stream()
                .filter(impact -> impact.getGroupId().equals(groupId))
                .toList();
    }

    public List<UpgradeDamageModifier> getUpgradeDamageModifiersForGroup(String groupId) {
        return upgradeDamageModifiers.stream()
                .filter(modifier -> modifier.getUpgradeGroup().equals(groupId))
                .toList();
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

}
