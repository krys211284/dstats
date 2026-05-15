package krys.web;

import krys.paladin.PaladinTreeSkill;
import krys.paladin.UpgradeDamageModifier;
import krys.skill.SkillId;

import java.util.List;

/** Prezentacyjny most między legacy `SkillId` runtime a opisowym rejestrem drzewa Paladyna. */
public final class HeroAssignedSkillPresentation {
    private final SkillId legacySkillId;
    private final String displayName;
    private final PaladinTreeSkill treeSkill;
    private final int currentRank;
    private final Integer currentDamagePercent;
    private final List<ModifierPresentation> activeModifiers;
    private final List<ModifierPresentation> baseEffects;
    private final ModifierPresentation faithGenerationModifier;

    HeroAssignedSkillPresentation(SkillId legacySkillId,
                                  String displayName,
                                  PaladinTreeSkill treeSkill,
                                  int currentRank,
                                  Integer currentDamagePercent,
                                  List<ModifierPresentation> activeModifiers,
                                  List<ModifierPresentation> baseEffects,
                                  ModifierPresentation faithGenerationModifier) {
        this.legacySkillId = legacySkillId;
        this.displayName = displayName;
        this.treeSkill = treeSkill;
        this.currentRank = currentRank;
        this.currentDamagePercent = currentDamagePercent;
        this.activeModifiers = List.copyOf(activeModifiers);
        this.baseEffects = List.copyOf(baseEffects);
        this.faithGenerationModifier = faithGenerationModifier;
    }

    public SkillId getLegacySkillId() {
        return legacySkillId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean hasTreeSkill() {
        return treeSkill != null;
    }

    public PaladinTreeSkill getTreeSkill() {
        return treeSkill;
    }

    public int getCurrentRank() {
        return currentRank;
    }

    public Integer getCurrentDamagePercent() {
        return currentDamagePercent;
    }

    public List<ModifierPresentation> getActiveModifiers() {
        return activeModifiers;
    }

    public List<ModifierPresentation> getBaseEffects() {
        return baseEffects;
    }

    public ModifierPresentation getFaithGenerationModifier() {
        return faithGenerationModifier;
    }

    /** Pojedynczy wpis modyfikatora renderowany jako nazwa plus tooltip źródłowy. */
    public static final class ModifierPresentation {
        private final String name;
        private final String tooltip;
        private final boolean runtimeSafe;

        ModifierPresentation(UpgradeDamageModifier modifier) {
            this.name = modifier.getUpgradeName();
            this.tooltip = modifier.getRankingTooltipSourceLabel()
                    + ": "
                    + modifier.getUpgradeName()
                    + " — "
                    + modifier.getRankingTooltipDescription();
            this.runtimeSafe = modifier.getSafeForRuntimeDps() == krys.paladin.UpgradeDamageSafety.YES;
        }

        public String getName() {
            return name;
        }

        public String getTooltip() {
            return tooltip;
        }

        public boolean isRuntimeSafe() {
            return runtimeSafe;
        }
    }
}
