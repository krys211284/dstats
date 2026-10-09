package krys.necromancer;

import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.DamagePercentComponent;
import krys.skilltree.DamagePercentRankTable;
import krys.skilltree.SkillUpgrade;
import krys.skilltree.SkillTreeStatus;
import krys.skilltree.SkillTreeType;
import krys.skilltree.UpgradeDamageImpactType;
import krys.skilltree.UpgradeDamageModifier;
import krys.skilltree.UpgradeDamageModifierType;
import krys.skilltree.UpgradeDamageSafety;
import krys.skilltree.UpgradeDamageValueKind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static krys.skilltree.DamagePercentComponent.*;
import static org.junit.jupiter.api.Assertions.*;

class NecromancerCoreSkillTreeRegistryTest {
    // Golden values i nazwy z kontraktu N2, D4Guides EN/PL i głównego max-view D4Builds.
    private static final Map<String, List<String>> UPGRADE_NAMES = Map.of(
            "wlocznia_z_kosci", List.of("Premia do Obrażeń od Przebicia", "Animusz", "Rozszczepianie Cieni", "Włócznia Krwi",
                    "Kościane Kolce", "Premia do Obrażeń od Pierwszego Trafienia", "Redukcja Kosztu"),
            "mor", List.of("Premia do Obrażeń za Ograniczenie Kontroli", "Szansa na Szczęśliwy Traf", "Niestabilna Krew", "Wir",
                    "Przeszywający Mrok", "Premia do Rozmiaru", "Premia do Obrażeń Obszarowych"),
            "rozdarcie", List.of("Premia do Obrażeń", "Redukcja Kosztu", "Żniwiarski Lotos", "Nieubłagany Żniwiarz",
                    "Chłodna Pogoń", "Ograniczanie Kontroli i Generowanie Zwłok", "Zajadłość"),
            "naplyw_krwi", List.of("Przytłoczenie", "Osłabienie", "Igły", "Ty i Jaka Armia?", "Krwawa Jatka", "Umocnienie", "Premia do Obrażeń"),
            "krwawa_lanca", List.of("Przytłoczenie", "Redukcja Kosztu", "Umocnienie", "Rykoszet", "Ropiejąca Rana", "Okrwawione Kolce", "Poszukiwanie Krwi"),
            "szkieletowy_mag", List.of("Czas Działania Premii do Obrażeń", "Szansa na Trafienie Krytyczne", "Sabat", "Dar Śmierci",
                    "Osobliwość", "Premia do Obrażeń od Ograniczenia Kontroli", "Zajadłość, Animusz lub Przytłoczenie"));

    @Test
    void shouldKeepBasicBeforeCoreAndSevenUpgradesPerCoreSkillInTheExtendedCatalog() {
        var registry = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(23, registry.allSkills().size());
        assertEquals(List.of("basic", "basic", "basic", "basic", "core", "core", "core", "core", "core", "core", "corpse", "corpse", "corpse", "macabre", "macabre", "macabre", "macabre", "curse", "curse", "ultimate", "ultimate", "ultimate", "ultimate"),
                registry.allSkills().stream().map(CharacterTreeSkill::getSkillGroup).toList());
        assertEquals(UPGRADE_NAMES.keySet(), Set.copyOf(coreSkills().stream().map(CharacterTreeSkill::getSkillId).toList()));
        assertEquals("Główne / Core", registry.treeGroupDisplayName("core"));
        assertEquals("CORE", NecromancerSkillTreeRegistry.CORE.id());
        assertEquals("Główne", NecromancerSkillTreeRegistry.CORE.displayName());
        assertEquals(1, NecromancerSkillTreeRegistry.CORE.displayOrder());
        for (var skill : coreSkills()) {
            assertEquals(Set.of(NecromancerSkillTreeRegistry.CORE), skill.getSkillCategories());
            assertEquals(SkillTreeType.DAMAGE, skill.getType());
            assertEquals(SkillTreeStatus.NEEDS_VERIFICATION, skill.getStatus());
            assertEquals(List.of("grupa_1", "grupa_2", "grupa_3"), skill.getUpgradeGroups().stream().map(group -> group.getId()).toList());
            assertEquals(List.of(2, 2, 3), skill.getUpgradeGroups().stream().map(group -> group.getUpgrades().size()).toList());
            assertEquals(7, skill.getUpgradeDamageImpacts().size());
            assertEquals(Set.copyOf(UPGRADE_NAMES.get(skill.getSkillId())),
                    Set.copyOf(upgrades(skill).stream().map(SkillUpgrade::getName).toList()));
            assertTrue(skill.getNotes().contains("bez jawnej etykiety Rank 15"));
            assertTrue(skill.getNotes().contains("Rangi 2..14 pozostają nieznane"));
            assertTrue(skill.getSourceReference().startsWith("https://d4guides.gg/en/database/skills/"));
            assertTrue(skill.getNotes().contains(NecromancerSkillTreeRegistry.MAX_VIEW_SOURCE));
            assertTrue(skill.getNotes().contains("https://d4guides.gg/pl/baza-danych/umiejetnosci/" + skill.getSkillId().replace('_', '-')));
        }
        assertEquals(42, coreSkills().stream().mapToInt(skill -> upgrades(skill).size()).sum());
        assertEquals(161, registry.allSkills().stream().mapToInt(skill -> upgrades(skill).size()).sum());
        assertEquals(70, registry.allSkills().stream().filter(skill -> Set.of("basic", "core").contains(skill.getSkillGroup()))
                .mapToInt(skill -> upgrades(skill).size()).sum());
    }

    @Test
    void shouldKeepSimpleSourcePercentagesPerHitOrOneMageAttack() {
        for (var golden : Map.of("wlocznia_z_kosci", List.of(160, 408), "krwawa_lanca", List.of(160, 408),
                "szkieletowy_mag", List.of(80, 204)).entrySet()) {
            var skill = skill(golden.getKey());
            assertEquals(golden.getValue().getFirst(), skill.getBaseDamagePercentAtRank1());
            assertEquals(golden.getValue().getLast(), skill.getBaseDamagePercentAtTreeMaxRank());
            assertSparseRanks(skill.getBaseDamagePercentRanks(), golden.getValue().getFirst(), golden.getValue().getLast());
            assertTrue(skill.getComponentDamagePercentRanks().isEmpty());
        }
        assertTrue(skill("szkieletowy_mag").getNotes().contains("za atak jednego maga"));
        assertTrue(skill("szkieletowy_mag").getNotes().contains("maksymalnie 3 magów"));
    }

    @Test
    void shouldKeepComponentsSeparateAndBlightDamageOverSixSeconds() {
        assertComponents("mor", Map.of(PRIMARY_DAMAGE, List.of(40, 102), DAMAGE_OVER_DURATION, List.of(210, 536)));
        assertComponents("rozdarcie", Map.of(FIRST_STRIKE_DAMAGE, List.of(60, 153), SECOND_STRIKE_DAMAGE, List.of(100, 255)));
        assertComponents("naplyw_krwi", Map.of(PRIMARY_DAMAGE, List.of(20, 51), BURST_DAMAGE, List.of(125, 319)));
        var blight = skill("mor").getComponentDamagePercentRanks();
        assertEquals(6, blight.durationSecondsFor(DAMAGE_OVER_DURATION));
        assertFalse(blight.hasComponent(DAMAGE_PER_SECOND));
        assertTrue(skill("mor").getNotes().contains("w ciągu 6 s"));
    }

    @Test
    void shouldPreserveBaseCostsLuckyHitAndCanonicalTags() {
        var costsAndLuckyHits = Map.of("wlocznia_z_kosci", List.of(25, 50), "mor", List.of(20, 40),
                "rozdarcie", List.of(20, 40), "naplyw_krwi", List.of(30, 12), "krwawa_lanca", List.of(15, 50),
                "szkieletowy_mag", List.of(30, 20));
        var tags = Map.of("wlocznia_z_kosci", Set.of("CORE", "BONE", "PHYSICAL"), "mor", Set.of("CORE", "DARKNESS", "SHADOW"),
                "rozdarcie", Set.of("CORE", "DARKNESS", "SHADOW"), "naplyw_krwi", Set.of("CORE", "BLOOD", "PHYSICAL"),
                "krwawa_lanca", Set.of("CORE", "BLOOD", "PHYSICAL"), "szkieletowy_mag", Set.of("CORE", "SUMMON", "MINION"));
        for (var skill : coreSkills()) {
            var golden = costsAndLuckyHits.get(skill.getSkillId());
            assertEquals(BigDecimal.valueOf(golden.getFirst()), skill.getResourceData().cost());
            assertEquals(golden.getLast(), skill.getLuckyHitPercent());
            assertNull(skill.getResourceData().generationMin());
            assertNull(skill.getResourceData().generationMax());
            assertNull(skill.getResourceData().generationBonusKnown());
            assertEquals(tags.get(skill.getSkillId()), skill.getTags());
        }
    }

    @Test
    void shouldExposeLuckyHitAndMageDamageTypeConflictsWithoutInventingCanonicalTags() {
        var luckyHit = modifier("mor", "Szansa na Szczęśliwy Traf");
        assertEquals("+20%", luckyHit.getValue());
        assertEquals(UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW, luckyHit.getType());
        assertEquals(UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, luckyHit.getSafeForRankingDisplay());
        assertTrue(luckyHit.getNotes().contains("PL D4Guides i bieżący D4Builds"));
        assertTrue(luckyHit.getNotes().contains("changelog EN D4Guides 3.2.1"));
        assertEquals(UpgradeDamageImpactType.NEEDS_VERIFICATION, skill("mor").getUpgradeDamageImpacts().stream()
                .filter(impact -> impact.getUpgradeName().equals(luckyHit.getUpgradeName())).findFirst().orElseThrow().getType());
        var mage = skill("szkieletowy_mag");
        assertTrue(mage.getNotes().contains("D4Guides podaje Physical"));
        assertTrue(mage.getNotes().contains("D4Builds prezentuje Shadow Damage"));
        assertTrue(mage.getNotes().contains("Book of the Dead"));
        assertFalse(mage.getTags().contains("PHYSICAL"));
        assertFalse(mage.getTags().contains("SHADOW"));
    }

    @Test
    void shouldRetainVariantRankEndpointsWithoutReplacingBaseTables() {
        for (var golden : List.of(
                List.of("wlocznia_z_kosci", "Rozszczepianie Cieni", "163%", "416%"),
                List.of("wlocznia_z_kosci", "Włócznia Krwi", "160%", "408%"),
                List.of("wlocznia_z_kosci", "Kościane Kolce", "53%", "135%"),
                List.of("mor", "Niestabilna Krew", "315%", "803%"),
                List.of("rozdarcie", "Żniwiarski Lotos", "60%", "153%"),
                List.of("naplyw_krwi", "Igły", "60%", "153%"),
                List.of("naplyw_krwi", "Ty i Jaka Armia?", "20%", "51%"),
                List.of("naplyw_krwi", "Krwawa Jatka", "125%", "319%"),
                List.of("krwawa_lanca", "Ropiejąca Rana", "100%", "255%"))) {
            var modifier = modifier(golden.get(0), golden.get(1));
            assertEquals(golden.get(2), modifier.getValue());
            assertTrue(modifier.getNotes().contains(golden.get(3)), golden.toString());
            assertTrue(modifier.scalesWithSkillRank());
        }
        var wound = modifier("krwawa_lanca", "Ropiejąca Rana");
        assertEquals(UpgradeDamageModifierType.DAMAGE_OVER_TIME, wound.getType());
        assertTrue(wound.getCondition().contains("na sekundę"));
        assertTrue(wound.getNotes().contains("najwyżej 5 kumulacji"));
        assertEquals(UpgradeDamageImpactType.DAMAGE_OVER_TIME, skill("krwawa_lanca").getUpgradeDamageImpacts().stream()
                .filter(impact -> impact.getUpgradeName().equals("Ropiejąca Rana")).findFirst().orElseThrow().getType());
    }

    @Test
    void shouldDescribeRelativeLancesWithoutAbsoluteWeaponDamageCoefficients() {
        for (var golden : List.of(List.of("wlocznia_z_kosci", "Włócznia Krwi", "100% normalnych obrażeń"),
                List.of("krwawa_lanca", "Przytłoczenie", "50% normalnych obrażeń"),
                List.of("krwawa_lanca", "Okrwawione Kolce", "125% normalnych obrażeń"))) {
            var relative = skill(golden.get(0)).getUpgradeDamageModifiers().stream()
                    .filter(modifier -> modifier.getUpgradeName().equals(golden.get(1)) && modifier.getValue().equals(golden.get(2)))
                    .findFirst().orElseThrow();
            assertEquals(UpgradeDamageModifierType.ADDITIONAL_HIT_OR_STRIKE, relative.getType());
            assertEquals(UpgradeDamageValueKind.TEXT_ONLY, relative.getValueKind());
            assertTrue(relative.getAffectedComponent().contains("względem normalnej"));
            if (golden.get(0).equals("krwawa_lanca")) {
                assertNull(skill(golden.get(0)).getUpgradeDamageImpacts().stream()
                        .filter(impact -> impact.getUpgradeName().equals(golden.get(1))).findFirst().orElseThrow().getDamagePercent());
            }
        }
    }

    @Test
    void shouldKeepEnemyMinionStatusAndResourceConditionsExplicitAndOutsideRuntime() {
        assertTrue(modifier("wlocznia_z_kosci", "Premia do Obrażeń od Przebicia").getCondition().contains("każdego przeszytego wroga"));
        assertEquals(UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER, modifier("wlocznia_z_kosci", "Animusz").getType());
        assertTrue(skill("rozdarcie").getUpgradeDamageModifiers().stream().anyMatch(modifier ->
                modifier.getValue().equals("1%[x]") && modifier.getCondition().contains("Za każdego posiadanego sługę")));
        assertTrue(modifier("rozdarcie", "Chłodna Pogoń").getCondition().contains("inny wróg"));
        assertTrue(modifier("naplyw_krwi", "Premia bazowej nowy").getCondition().contains("maksymalnie 50%[x]"));
        assertTrue(modifier("naplyw_krwi", "Premia do Obrażeń").getCondition().contains("Healthy"));
        var death = modifier("szkieletowy_mag", "Dar Śmierci");
        assertEquals(UpgradeDamageModifierType.RESOURCE_OR_COST, death.getType());
        assertEquals("50%[x]", death.getValue());
        assertTrue(death.getNotes().contains("128%[x]"));
        assertTrue(death.getCondition().contains("każdego aktywnego maga"));
        assertEquals("Cała pozostała Esencja", modifier("szkieletowy_mag", "Osobliwość").getValue());
        assertTrue(skill("szkieletowy_mag").getUpgradeDamageModifiers().stream().anyMatch(modifier ->
                modifier.getValue().equals("+0.1 s za punkt Esencji") && modifier.getCondition().contains("faktycznie zużytej")));
        for (var skill : coreSkills()) {
            assertTrue(skill.getUpgradeDamageModifiers().stream().allMatch(modifier -> modifier.getSafeForRuntimeDps() == UpgradeDamageSafety.NO));
            assertTrue(skill.getUpgradeDamageModifiers().stream().filter(modifier -> modifier.getType() != UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW)
                    .allMatch(modifier -> modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.YES));
        }
    }

    private static void assertComponents(String id, Map<DamagePercentComponent, List<Integer>> golden) {
        var skill = skill(id);
        assertNull(skill.getBaseDamagePercentAtRank1());
        assertNull(skill.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(skill.getBaseDamagePercentRanks().isEmpty());
        assertEquals(golden.keySet(), skill.getComponentDamagePercentRanks().asMap().keySet());
        golden.forEach((component, values) -> assertSparseRanks(skill.getComponentDamagePercentRanks().tableFor(component), values.getFirst(), values.getLast()));
    }

    private static void assertSparseRanks(DamagePercentRankTable table, int rankOne, int maxView) {
        assertEquals(Map.of(1, rankOne, 15, maxView), table.asMap());
        for (int rank = 2; rank < 15; rank++) {
            assertNull(table.damagePercentAtRank(rank));
        }
    }

    private static List<CharacterTreeSkill> coreSkills() {
        return NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills().stream().filter(skill -> skill.getSkillGroup().equals("core")).toList();
    }

    private static List<SkillUpgrade> upgrades(CharacterTreeSkill skill) {
        return skill.getUpgradeGroups().stream().flatMap(group -> group.getUpgrades().stream()).toList();
    }

    private static CharacterTreeSkill skill(String id) {
        return NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(id);
    }

    private static UpgradeDamageModifier modifier(String id, String name) {
        return skill(id).getUpgradeDamageModifiers().stream().filter(modifier -> modifier.getUpgradeName().equals(name)).findFirst().orElseThrow();
    }
}
