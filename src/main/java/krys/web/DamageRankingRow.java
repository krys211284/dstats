package krys.web;

import krys.paladin.PaladinSkillTreeType;
import krys.ranking.PaladinSkillDamageRankingEntry;

/** Wiersz widoku rankingu łączący opis rankingu z typem umiejętności z rejestru drzewa. */
public final class DamageRankingRow {
    private final PaladinSkillDamageRankingEntry entry;
    private final PaladinSkillTreeType type;

    public DamageRankingRow(PaladinSkillDamageRankingEntry entry, PaladinSkillTreeType type) {
        this.entry = entry;
        this.type = type;
    }

    public PaladinSkillDamageRankingEntry getEntry() {
        return entry;
    }

    public PaladinSkillTreeType getType() {
        return type;
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
}
