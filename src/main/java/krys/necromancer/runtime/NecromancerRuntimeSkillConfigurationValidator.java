package krys.necromancer.runtime;

import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.runtime.RuntimeSkillConfiguration;
import krys.skilltree.DamagePercentRankTable;

import java.util.Objects;

/** Waliduje strukturę konfiguracji katalogowej; nie potwierdza wsparcia wykonania obrażeń. */
public final class NecromancerRuntimeSkillConfigurationValidator {
    public void validate(RuntimeSkillConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        var key = configuration.skillKey();
        if (!"necromancer".equals(key.classId())) {
            throw new IllegalArgumentException("Validator wymaga namespace necromancer: " + key.asExternalId());
        }
        var skill = NecromancerSkillTreeRegistry.asCharacterRegistry().findSkill(key.skillId())
                .orElseThrow(() -> new IllegalArgumentException("Nieznana umiejętność katalogowa: " + key.asExternalId()));
        if (configuration.rank() > DamagePercentRankTable.MAX_RANK) {
            throw new IllegalArgumentException("Ranga katalogowa musi należeć do 0.." + DamagePercentRankTable.MAX_RANK);
        }
        for (var selection : configuration.selections().entrySet()) {
            var group = skill.getUpgradeGroups().stream()
                    .filter(candidate -> candidate.getId().equals(selection.getKey())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Nieznana grupa wyboru: " + selection.getKey()));
            if (group.getUpgrades().stream().noneMatch(upgrade -> upgrade.getId().equals(selection.getValue()))) {
                throw new IllegalArgumentException("Wybór " + selection.getValue() + " nie należy do " + group.getId());
            }
        }
    }
}
