package krys.necromancer.app;

import krys.necromancer.combat.NecromancerSeason15DirectDamageProfile;
import krys.necromancer.combat.NecromancerSeason15DirectDamageResolver;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;

import java.util.Objects;

/** Pobiera Krwotok z kanonicznego loadoutu i deleguje matematykę do niezmienionego N8B. */
public final class NecromancerCurrentBuildCalculationService {
    public static final RuntimeSkillKey KRWOTOK = new RuntimeSkillKey("necromancer", "krwotok");
    private final NecromancerSeason15DirectDamageResolver resolver = new NecromancerSeason15DirectDamageResolver();

    public NecromancerCurrentBuildCalculation calculate(RuntimeSkillLoadout loadout,
                                                        NecromancerSeason15DirectDamageProfile profile) {
        RuntimeSkillConfiguration configuration = requireKrwotokOnActionBar(loadout);
        return new NecromancerCurrentBuildCalculation(configuration, profile, resolver.calculate(configuration, profile));
    }

    public RuntimeSkillConfiguration requireKrwotokOnActionBar(RuntimeSkillLoadout loadout) {
        Objects.requireNonNull(loadout, "loadout");
        if (!"necromancer".equals(loadout.classId())) {
            throw new IllegalArgumentException("N11 wymaga loadoutu Nekromanty.");
        }
        RuntimeSkillConfiguration configuration = loadout.configurations().get(KRWOTOK);
        if (configuration == null) {
            throw new IllegalArgumentException("Wybierz Krwotok R1.");
        }
        if (!loadout.actionBar().contains(KRWOTOK)) {
            throw new IllegalArgumentException("Krwotok musi znajdować się na pasku akcji.");
        }
        return configuration;
    }
}
