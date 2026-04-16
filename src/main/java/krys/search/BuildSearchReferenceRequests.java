package krys.search;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Presety pomocnicze do smoke testów i regresji backendowego searcha M10. */
public final class BuildSearchReferenceRequests {
    private BuildSearchReferenceRequests() {
    }

    public static BuildSearchRequest create(BuildSearchReferenceScenario scenario) {
        return switch (scenario) {
            case FOUNDATION_M9 -> createFoundationM9();
        };
    }

    public static BuildSearchRequest createFoundationM9() {
        Map<SkillId, BuildSearchSkillSpace> skillSpaces = new EnumMap<>(SkillId.class);
        skillSpaces.put(SkillId.BRANDISH, new BuildSearchSkillSpace(
                SkillId.BRANDISH,
                List.of(0, 5),
                List.of(false, true),
                List.of(SkillUpgradeChoice.NONE, SkillUpgradeChoice.LEFT, SkillUpgradeChoice.RIGHT)
        ));
        skillSpaces.put(SkillId.HOLY_BOLT, new BuildSearchSkillSpace(
                SkillId.HOLY_BOLT,
                List.of(0, 5),
                List.of(false, true),
                List.of(SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(SkillId.CLASH, new BuildSearchSkillSpace(
                SkillId.CLASH,
                List.of(0, 5),
                List.of(false, true),
                List.of(SkillUpgradeChoice.NONE, SkillUpgradeChoice.LEFT)
        ));
        skillSpaces.put(SkillId.ADVANCE, new BuildSearchSkillSpace(
                SkillId.ADVANCE,
                List.of(0, 5),
                List.of(false, true),
                List.of(SkillUpgradeChoice.NONE, SkillUpgradeChoice.LEFT, SkillUpgradeChoice.RIGHT)
        ));

        return new BuildSearchRequest(
                List.of(13),
                List.of(8L),
                List.of(18.0d),
                List.of(0.0d),
                List.of(50.0d),
                List.of(50.0d),
                List.of(50.0d),
                skillSpaces,
                List.of(1, 2),
                9,
                5
        );
    }
}
