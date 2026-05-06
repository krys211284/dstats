package krys.web;

import krys.paladin.PaladinSkillTreeType;
import krys.ranking.PaladinSkillDamageRankingEntry;

/** Wiersz widoku rankingu łączący opis rankingu z typem umiejętności z rejestru drzewa. */
public final class PaladinSkillDamageRankingRow {
    private final PaladinSkillDamageRankingEntry entry;
    private final PaladinSkillTreeType type;

    public PaladinSkillDamageRankingRow(PaladinSkillDamageRankingEntry entry, PaladinSkillTreeType type) {
        this.entry = entry;
        this.type = type;
    }

    public PaladinSkillDamageRankingEntry getEntry() {
        return entry;
    }

    public PaladinSkillTreeType getType() {
        return type;
    }

    public boolean isDpsCalculable() {
        return entry.getDamagePerUse() != null
                || entry.getTheoreticalDps() != null;
    }

    public Double getSingleTargetDps() {
        return entry.getTheoreticalDps();
    }
}
