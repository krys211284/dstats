package krys.necromancer.app;

import krys.combat.DirectDamageResult;
import krys.necromancer.combat.NecromancerSeason15DirectDamageProfile;
import krys.runtime.RuntimeSkillConfiguration;

import java.util.Objects;

/** Wynik pojedynczego trafienia N11, niezależny od symulacji Paladyna. */
public record NecromancerCurrentBuildCalculation(RuntimeSkillConfiguration configuration,
                                                 NecromancerSeason15DirectDamageProfile profile,
                                                 DirectDamageResult result) {
    public NecromancerCurrentBuildCalculation {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(result, "result");
    }
}
