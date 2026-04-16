package krys.search;

import krys.app.CurrentBuildRequest;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pojedynczy legalny kandydat searcha M12 gotowy do zbudowania snapshotu i oceny runtime. */
public final class BuildSearchCandidate {
    private final CurrentBuildRequest currentBuildRequest;

    public BuildSearchCandidate(CurrentBuildRequest currentBuildRequest) {
        this.currentBuildRequest = currentBuildRequest;
    }

    public CurrentBuildRequest getCurrentBuildRequest() {
        return currentBuildRequest;
    }

    public String getInputProfileDescription() {
        return "level=" + currentBuildRequest.getLevel()
                + " | weaponDamage=" + currentBuildRequest.getWeaponDamage()
                + " | strength=" + formatWholeNumber(currentBuildRequest.getStrength())
                + " | intelligence=" + formatWholeNumber(currentBuildRequest.getIntelligence())
                + " | thorns=" + formatWholeNumber(currentBuildRequest.getThorns())
                + " | blockChance=" + formatPercent(currentBuildRequest.getBlockChance())
                + " | retributionChance=" + formatPercent(currentBuildRequest.getRetributionChance());
    }

    public String getLearnedSkillsDescription() {
        Map<SkillId, SkillState> learnedSkills = currentBuildRequest.getLearnedSkills();
        if (learnedSkills.isEmpty()) {
            return "Brak";
        }

        List<String> labels = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            SkillState state = learnedSkills.get(skillId);
            if (state == null || state.getRank() <= 0) {
                continue;
            }
            labels.add(PaladinSkillDefs.get(skillId).getName()
                    + " rank " + state.getRank()
                    + " | base=" + (state.isBaseUpgrade() ? "tak" : "nie")
                    + " | choice=" + PaladinSkillDefs.getChoiceDisplayName(skillId, state.getChoiceUpgrade()));
        }
        return String.join(" || ", labels);
    }

    public String getActionBarSkillsDescription() {
        if (currentBuildRequest.getActionBar().isEmpty()) {
            return "Pusty";
        }

        List<String> labels = new ArrayList<>();
        for (SkillId skillId : currentBuildRequest.getActionBar()) {
            SkillState state = currentBuildRequest.getLearnedSkills().get(skillId);
            if (state == null || state.getRank() <= 0) {
                labels.add(PaladinSkillDefs.get(skillId).getName() + " OFF");
                continue;
            }
            labels.add(buildSkillDescription(skillId, state));
        }
        return String.join(" || ", labels);
    }

    public String getActionBarDescription() {
        if (currentBuildRequest.getActionBar().isEmpty()) {
            return "Pusty";
        }

        List<String> labels = new ArrayList<>();
        for (SkillId skillId : currentBuildRequest.getActionBar()) {
            labels.add(PaladinSkillDefs.get(skillId).getName());
        }
        return String.join(" -> ", labels);
    }

    public String toDeterministicKey() {
        return getInputProfileDescription()
                + " | skills=" + getLearnedSkillsDescription()
                + " | bar=" + getActionBarDescription()
                + " | horizon=" + currentBuildRequest.getHorizonSeconds();
    }

    private static String buildSkillDescription(SkillId skillId, SkillState state) {
        return PaladinSkillDefs.get(skillId).getName()
                + " rank " + state.getRank()
                + " | base=" + (state.isBaseUpgrade() ? "tak" : "nie")
                + " | choice=" + PaladinSkillDefs.getChoiceDisplayName(skillId, state.getChoiceUpgrade());
    }

    private static String formatWholeNumber(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }
}
