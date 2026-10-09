package krys.runtime.legacy;

import krys.runtime.RuntimeSkillKey;
import krys.skill.SkillId;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/** Jawny most między czterema legacy identyfikatorami a neutralną tożsamością. */
public final class LegacyPaladinRuntimeSkillKeyAdapter {
    private LegacyPaladinRuntimeSkillKeyAdapter() { }

    public static RuntimeSkillKey toRuntimeKey(SkillId legacyId) {
        Objects.requireNonNull(legacyId, "legacyId");
        String skillId = switch (legacyId) {
            case BRANDISH -> "wymach";
            case HOLY_BOLT -> "swiety_pocisk";
            case CLASH -> "starcie";
            case ADVANCE -> "natarcie";
        };
        return new RuntimeSkillKey("paladin", skillId);
    }

    public static Optional<SkillId> toLegacySkillId(RuntimeSkillKey key) {
        Objects.requireNonNull(key, "key");
        return Arrays.stream(SkillId.values()).filter(legacyId -> toRuntimeKey(legacyId).equals(key)).findFirst();
    }
}
