package krys.runtime.legacy;

import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
import krys.skill.SkillId;
import krys.skill.SkillState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Most mapy i paska legacy; normalizacja null i duplikatów pozostaje na tej granicy. */
public final class LegacyPaladinRuntimeSkillLoadoutAdapter {
    private static final String CLASS_ID = "paladin";

    private LegacyPaladinRuntimeSkillLoadoutAdapter() { }

    public static RuntimeSkillLoadout toRuntimeLoadout(Map<SkillId, SkillState> skills, List<SkillId> actionBar) {
        Map<RuntimeSkillKey, RuntimeSkillConfiguration> configurations = new HashMap<>();
        if (skills != null) {
            for (var entry : skills.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "skillId");
                var state = Objects.requireNonNull(entry.getValue(), "state");
                if (entry.getKey() != state.getSkillId()) {
                    throw new IllegalArgumentException("Klucz mapy legacy musi odpowiadać SkillState.skillId");
                }
                var configuration = LegacyPaladinRuntimeSkillConfigurationAdapter.toRuntimeConfiguration(state);
                configurations.put(configuration.skillKey(), configuration);
            }
        }
        // Historyczna kolejność pierwszego wystąpienia i deduplikacja należą wyłącznie do wejścia legacy.
        var uniqueBar = new LinkedHashSet<>(actionBar == null ? List.<SkillId>of() : actionBar);
        var runtimeBar = uniqueBar.stream().map(LegacyPaladinRuntimeSkillKeyAdapter::toRuntimeKey).toList();
        return new RuntimeSkillLoadout(CLASS_ID, configurations, runtimeBar);
    }

    public static Optional<LegacyView> toLegacyView(RuntimeSkillLoadout loadout) {
        Objects.requireNonNull(loadout, "loadout");
        if (!CLASS_ID.equals(loadout.classId())) {
            return Optional.empty();
        }
        var skills = new EnumMap<SkillId, SkillState>(SkillId.class);
        for (var configuration : loadout.configurations().values()) {
            var state = LegacyPaladinRuntimeSkillConfigurationAdapter.toLegacySkillState(configuration)
                    .orElseThrow(() -> new IllegalArgumentException("Nieznana tożsamość legacy: " + configuration.skillKey().asExternalId()));
            skills.put(state.getSkillId(), state);
        }
        var actionBar = new ArrayList<SkillId>();
        for (var key : loadout.actionBar()) {
            actionBar.add(LegacyPaladinRuntimeSkillKeyAdapter.toLegacySkillId(key)
                    .orElseThrow(() -> new IllegalArgumentException("Nieznany skill paska legacy: " + key.asExternalId())));
        }
        return Optional.of(new LegacyView(skills, actionBar));
    }

    /** Niemutowalny widok kompatybilności; nie stanowi drugiego źródła prawdy requestu ani snapshotu. */
    public record LegacyView(Map<SkillId, SkillState> learnedSkills, List<SkillId> actionBar) {
        public LegacyView {
            var copy = new EnumMap<SkillId, SkillState>(SkillId.class);
            copy.putAll(Objects.requireNonNull(learnedSkills, "learnedSkills"));
            learnedSkills = Collections.unmodifiableMap(copy);
            actionBar = List.copyOf(Objects.requireNonNull(actionBar, "actionBar"));
        }
    }
}
