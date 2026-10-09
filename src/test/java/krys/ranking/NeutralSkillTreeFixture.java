package krys.ranking;

import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.SkillResourceType;
import krys.skilltree.SkillTreeStatus;
import krys.skilltree.SourceSkillCategory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Sztuczny rejestr bez danych nowej klasy; liczby pochodzą z istniejących, audytowanych tabel Paladyna. */
public final class NeutralSkillTreeFixture {
    public static final SourceSkillCategory CATEGORY = new SourceSkillCategory("TEST_CATEGORY", "Kategoria testowa", 0);
    public static final String TAG = "TEST_MECHANIC";
    public static final SkillResourceType RESOURCE = new SkillResourceType(
            "test_energy", "Koszt energii testowej", "Generowanie energii testowej", "pkt. energii testowej");

    private NeutralSkillTreeFixture() { }

    public static CharacterSkillTreeRegistry registry() {
        CharacterSkillTreeRegistry reference = SkillTreeRegistryProvider.paladinOnly().registryFor(PlayableClass.PALADIN);
        // Używamy istniejącego klucza wyboru klasy; w produkcji nie dodajemy klasy fikcyjnej ani Nekromanty.
        return new CharacterSkillTreeRegistry(PlayableClass.PALADIN, "NeutralTestRegistry", List.of(
                copy("test_direct", "Testowa umiejętność", reference.requireSkill("starcie"), SkillTreeStatus.NEEDS_VERIFICATION),
                copy("test_component", "Testowe komponenty", reference.requireSkill("zenit"), SkillTreeStatus.UNSUPPORTED),
                copy("test_utility", "Testowe wsparcie", reference.requireSkill("forteca"), SkillTreeStatus.NON_DAMAGE)
        ), RESOURCE, Map.of("test_group", "Grupa testowa"));
    }

    private static CharacterTreeSkill copy(String id, String name, CharacterTreeSkill reference, SkillTreeStatus status) {
        return new CharacterTreeSkill(id, name, "test-source.md", "test_group",
                reference.getBaseDamagePercentAtRank1(), reference.getBaseDamagePercentAtTreeMaxRank(),
                reference.getBaseDamagePercentRanks(), reference.getComponentDamagePercentRanks(),
                reference.getType(), status, reference.getUpgradeGroups(), "Opis testowy z potwierdzonymi wartościami źródłowymi.",
                Set.of(CATEGORY), Set.of(TAG), reference.getResourceData(), reference.getLuckyHitPercent(),
                reference.getUpgradeDamageImpacts(), reference.getUpgradeDamageModifiers());
    }
}
