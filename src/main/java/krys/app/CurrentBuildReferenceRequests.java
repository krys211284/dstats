package krys.app;

import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.util.List;
import java.util.Map;

/** Fabryka pomocniczych requestów referencyjnych budowanych już na modelu wejścia użytkownika M8. */
public final class CurrentBuildReferenceRequests {
    private CurrentBuildReferenceRequests() {
    }

    public static CurrentBuildRequest create(CurrentBuildReferenceScenario scenario) {
        return switch (scenario) {
            case NONE -> throw new IllegalArgumentException("Scenariusz referencyjny NONE nie tworzy requestu");
            case HOLY_BOLT_JUDGEMENT -> createHolyBoltJudgement();
            case CLASH_PUNISHMENT -> createClashPunishment();
            case ADVANCE_FLASH -> createAdvanceFlash();
        };
    }

    public static CurrentBuildRequest createHolyBoltJudgement() {
        return new CurrentBuildRequest(
                13,
                8,
                18.0d,
                0.0d,
                50.0d,
                50.0d,
                50.0d,
                Map.of(
                        SkillId.HOLY_BOLT,
                        new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)
                ),
                List.of(SkillId.HOLY_BOLT),
                60
        );
    }

    public static CurrentBuildRequest createClashPunishment() {
        return new CurrentBuildRequest(
                13,
                8,
                18.0d,
                0.0d,
                50.0d,
                50.0d,
                50.0d,
                Map.of(
                        SkillId.CLASH,
                        new SkillState(SkillId.CLASH, 5, true, SkillUpgradeChoice.LEFT)
                ),
                List.of(SkillId.CLASH),
                60
        );
    }

    public static CurrentBuildRequest createAdvanceFlash() {
        return new CurrentBuildRequest(
                13,
                8,
                18.0d,
                0.0d,
                50.0d,
                50.0d,
                50.0d,
                Map.of(
                        SkillId.ADVANCE,
                        new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
                ),
                List.of(SkillId.ADVANCE),
                10
        );
    }
}
