package krys.necromancer;

import krys.ranking.*;
import krys.skilltree.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static krys.skilltree.DamagePercentComponent.*;
import static krys.skilltree.UpgradeDamageModifierType.*;
import static org.junit.jupiter.api.Assertions.*;

class NecromancerUltimateSkillTreeRegistryTest {
    // Golden N6: R1/mechaniki Icy Veins i D4Guides, max-view głównej domeny D4Builds.
    @Test
    void shouldCompleteAllSixGroupsAndPreservePreviousNineteenSkillsAndPaladinCount() {
        var registry = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(23, registry.allSkills().size());
        assertEquals(161, upgrades(registry.allSkills()));
        var previous = registry.allSkills().stream().filter(skill -> !skill.getSkillGroup().equals("ultimate")).toList();
        assertEquals(19, previous.size());
        assertEquals(133, upgrades(previous));
        assertEquals(List.of("basic", "core", "corpse", "macabre", "curse", "ultimate"),
                registry.allSkills().stream().map(CharacterTreeSkill::getSkillGroup).distinct().toList());
        assertEquals(List.of("armia_umarlych", "fala_krwi", "nawalnica_kosci", "spetanie_dusz"),
                ultimate().stream().map(CharacterTreeSkill::getSkillId).toList());
        assertEquals(28, upgrades(ultimate()));
        assertEquals(new SourceSkillCategory("ULTIMATE", "Moce Specjalne", 5), NecromancerSkillTreeRegistry.ULTIMATE);
        assertEquals("Moce Specjalne / Ultimate", registry.treeGroupDisplayName("ultimate"));
        assertTrue(registry.supportsSourceCategory("ULTIMATE"));
        assertTrue(registry.supportsMechanicTag("ULTIMATE"));
        assertEquals(24, SkillTreeRegistryProvider.production().registryFor(PlayableClass.PALADIN).allSkills().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"armia_umarlych", "fala_krwi", "nawalnica_kosci", "spetanie_dusz"})
    void shouldKeepUltimateMetadataAndAllRuntimeFlags(String id) {
        var skill = skill(id);
        assertEquals(SkillTreeType.DAMAGE, skill.getType());
        assertEquals(SkillTreeStatus.NEEDS_VERIFICATION, skill.getStatus());
        assertEquals(Set.of(NecromancerSkillTreeRegistry.ULTIMATE), skill.getSkillCategories());
        assertEquals(Map.of("armia_umarlych", 60, "fala_krwi", 50, "nawalnica_kosci", 60, "spetanie_dusz", 40).get(id), skill.getCooldownSeconds());
        assertEquals(Map.of("armia_umarlych", 64, "fala_krwi", 50, "nawalnica_kosci", 40, "spetanie_dusz", 20).get(id), skill.getLuckyHitPercent());
        assertEquals(Map.of("armia_umarlych", Set.of("ULTIMATE", "SUMMON", "PHYSICAL"),
                "fala_krwi", Set.of("ULTIMATE", "BLOOD", "PHYSICAL"),
                "nawalnica_kosci", Set.of("ULTIMATE", "BONE", "PHYSICAL"),
                "spetanie_dusz", Set.of("ULTIMATE", "DARKNESS", "SHADOW")).get(id), skill.getTags());
        assertNull(skill.getChargeData());
        assertTrue(skill.getActivationRequirements().isEmpty());
        assertNull(skill.getResourceData().cost());
        assertEquals(List.of(2, 2, 3), skill.getUpgradeGroups().stream().map(group -> group.getUpgrades().size()).toList());
        assertEquals(7, skill.getUpgradeDamageImpacts().size());
        assertTrue(skill.getUpgradeDamageModifiers().stream().allMatch(mod -> mod.getSafeForRuntimeDps() == UpgradeDamageSafety.NO));
        if (!id.equals("spetanie_dusz")) assertEquals(SkillResourceData.EMPTY, skill.getResourceData());
    }

    @Test
    void shouldKeepArmyPerExplosionWithoutCastTotalsOrPassiveDamageTables() {
        var army = skill("armia_umarlych");
        sparse(army.getBaseDamagePercentRanks(), 120, 306);
        assertEquals(120, army.getBaseDamagePercentAtRank1());
        assertEquals(306, army.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(army.getComponentDamagePercentRanks().isEmpty());
        assertTrue(army.getNotes().contains("per explosion; liczba eksplozji nie jest stała"));
        assertTrue(army.getNotes().contains("7 s"));
        assertTrue(army.getNotes().contains("2 s"));
        assertTrue(army.getNotes().contains("5 stosów"));
        var passive = modifier("armia_umarlych", "Pasywna Premia");
        assertEquals(STATUS_DAMAGE_ENABLER, passive.getType());
        assertEquals("1 Volatile Skeleton co 4 s", passive.getValue());
        assertTrue(passive.getComponentDamagePercentRanks().isEmpty());
        assertEquals(DEFENSE_OR_UTILITY, modifier("armia_umarlych", "Generowanie Zwłok").getType());
        assertTrue(modifier("armia_umarlych", "Premia do Obrażeń").getCondition().contains("każdego"));
        assertTrue(modifier("armia_umarlych", "Piętrzenie Ciał").getCondition().contains("Każda eksplozja"));
        var cold = modifier("armia_umarlych", "Martwe Zimno");
        variant(cold, PRIMARY_DAMAGE, 150, 383, null, false);
        assertTrue(cold.getNotes().contains("Freeze zamiast Stun"));
        assertEquals(60, army.getCooldownSeconds());
        assertTrue(modifier("armia_umarlych", "Redukcja Czasu Odnowienia").getValue().contains("60 → 40 s"));
    }

    @Test
    void shouldKeepConfirmedCommanderRankOneScalingAndExplicitUnknownMaxView() {
        var commander = modifier("armia_umarlych", "Niezłomne Dowództwo");
        assertEquals(MULTIPLICATIVE_DAMAGE_PERCENT, commander.getType());
        assertTrue(commander.getValue().contains("R1 +30%[x]"));
        assertTrue(commander.scalesWithSkillRank());
        assertEquals(UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, commander.getSafeForRankingDisplay());
        assertTrue(commander.getComponentDamagePercentRanks().isEmpty());
        assertTrue(commander.getNotes().contains("Brak potwierdzonego max-view / rank15"));
        assertTrue(commander.getNotes().contains("D4Builds nadal pokazuje 30%"));
        assertEquals("90% redukcji obrażeń sług", modifiers("armia_umarlych", "Niezłomne Dowództwo").getLast().getValue());
    }

    @Test
    void shouldKeepBloodWaveVariantsSeparateAndNoCooldownAndCostOverridesOutsideBase() {
        var wave = skill("fala_krwi");
        sparse(wave.getBaseDamagePercentRanks(), 500, 1275);
        assertTrue(wave.getComponentDamagePercentRanks().isEmpty());
        assertTrue(modifier("fala_krwi", "Premia do Obrażeń").getCondition().contains("Fortified"));
        assertEquals("1 Overpower; Maximum Overpower +2", modifier("fala_krwi", "Przytłoczenie").getValue());
        assertEquals(STATUS_DAMAGE_ENABLER, modifier("fala_krwi", "Przytłoczenie").getType());
        assertEquals("Do 2 Blood Orbs", modifier("fala_krwi", "Krwawa Kula").getValue());
        variant(modifier("fala_krwi", "Krwawy Przypływ"), ADDITIONAL_STRIKE_DAMAGE, 500, 1275, null, true);
        assertEquals("+1 fala", modifiers("fala_krwi", "Krwawy Przypływ").getLast().getValue());
        var trail = modifier("fala_krwi", "Ścieżka Ciemności");
        variant(trail, PRIMARY_DAMAGE, 800, 2040, null, true);
        assertEquals(DAMAGE_OVER_TIME, trail.getType());
        var hematolagnia = modifiers("fala_krwi", "Hematolagnia");
        variant(hematolagnia.getFirst(), PRIMARY_DAMAGE, 300, 765, null, false);
        assertEquals(List.of(RANK_SCALING_COMPONENT_PERCENT, RESOURCE_OR_COST, CAST_SPEED_OR_COOLDOWN),
                hematolagnia.stream().map(UpgradeDamageModifier::getType).toList());
        assertEquals("Koszt: 70 Esencji", hematolagnia.get(1).getValue());
        assertTrue(hematolagnia.getLast().getValue().contains("no cooldown"));
        assertTrue(hematolagnia.getFirst().getNotes().contains("no Knock Back"));
        assertEquals(50, wave.getCooldownSeconds());
        assertEquals(SkillResourceData.EMPTY, wave.getResourceData());
    }

    @Test
    void shouldKeepBoneStormTotalDurationWithoutCorpseActivationRequirementOrAutomaticExtension() {
        var storm = skill("nawalnica_kosci");
        durationBase(storm, 400, 1020, 10);
        var extension = modifier("nawalnica_kosci", "Wydłużenie Czasu Działania");
        assertEquals(RESOURCE_OR_COST, extension.getType());
        assertEquals("Do 4 Zwłok; +2 s za każde", extension.getValue());
        assertTrue(storm.getActivationRequirements().isEmpty());
        variant(modifier("nawalnica_kosci", "Cień i Kość"), DAMAGE_OVER_DURATION, 40, 102, 2, true);
        var roll = modifiers("nawalnica_kosci", "Rzucone Kości");
        assertEquals(List.of(STATUS_DAMAGE_ENABLER, CAST_SPEED_OR_COOLDOWN), roll.stream().map(UpgradeDamageModifier::getType).toList());
        assertTrue(roll.stream().allMatch(mod -> mod.scalesWithSkillRank() && mod.getComponentDamagePercentRanks().isEmpty()
                && mod.getValue().contains("15%[+] / max-view +38%")));
        var cyclone = modifier("nawalnica_kosci", "Wygłodniały Cyklon");
        assertEquals(CAST_SPEED_OR_COOLDOWN, cyclone.getType());
        assertTrue(cyclone.getValue().contains("R1 20 s / max-view 28 s"));
        assertTrue(cyclone.scalesWithSkillRank());
        assertTrue(cyclone.getComponentDamagePercentRanks().isEmpty());
        assertEquals(60, storm.getCooldownSeconds());
    }

    @Test
    void shouldKeepSoulriftResourcePerAbsorbedSoulAndBaseOverDuration() {
        var soulrift = skill("spetanie_dusz");
        durationBase(soulrift, 350, 893, 8);
        var data = soulrift.getResourceData();
        assertEquals(SkillResourceGenerationFrequency.PER_EVENT, data.generationFrequency());
        assertEquals(new BigDecimal("2"), data.generationMin());
        assertEquals(new BigDecimal("2"), data.generationMax());
        assertEquals(new BigDecimal("2"), data.generationMaxKnown());
        assertNull(data.generationBonusKnown());
        assertTrue(data.hasGeneration());
        for (String text : List.of("pochłonięcie jednej duszy przeciwnika", "0.25 s", "3 przeciwników", "Bossów 3 dusze jednocześnie"))
            assertTrue(soulrift.getNotes().contains(text), text);
        var anima = modifier("spetanie_dusz", "Destylacja Duszy");
        assertEquals(MULTIPLICATIVE_DAMAGE_PERCENT, anima.getType());
        assertTrue(anima.getValue().contains("R1 +30%[x] / max-view +77%[x]"));
        assertTrue(anima.scalesWithSkillRank());
        assertTrue(anima.getAffectedComponent().contains("Globalne"));
        assertTrue(anima.getComponentDamagePercentRanks().isEmpty());
    }

    @Test
    void shouldKeepSoulVortexPerSingleSoulAndFrozenExplosionConditionalWithoutFrequency() {
        var vortex = modifier("spetanie_dusz", "Wir Dusz");
        variant(vortex, EVENT_TRIGGERED_DAMAGE, 25, 64, null, true);
        assertEquals("Pochłonięcie jednej duszy przeciwnika", vortex.getCondition());
        assertEquals("Redukcja cooldownu 15 s", modifiers("spetanie_dusz", "Wir Dusz").getLast().getValue());
        var frozen = modifier("spetanie_dusz", "Zamarznięte Pustkowia");
        variant(frozen, BURST_DAMAGE, 75, 191, null, true);
        assertTrue(frozen.getCondition().contains("Tylko"));
        assertTrue(frozen.getCondition().contains("Frozen"));
        assertTrue(frozen.getCondition().contains("pochłonięcie"));
        assertEquals(STATUS_DAMAGE_ENABLER, modifiers("spetanie_dusz", "Zamarznięte Pustkowia").getLast().getType());
        assertTrue(modifiers("spetanie_dusz", "Zamarznięte Pustkowia").getLast().getComponentDamagePercentRanks().isEmpty());
        assertEquals(40, skill("spetanie_dusz").getCooldownSeconds());
    }

    @Test
    void shouldKeepAllCatalogMetricsWithoutRuntimeDamageOrCycle() {
        var service = new DamageRankingService(null, SkillTreeRegistryProvider.production());
        for (var metric : DamageRankingMetric.values()) {
            var entries = service.rankDamageSkills(PlayableClass.NECROMANCER, metric);
            assertEquals(23, entries.size());
            for (var entry : entries) {
                assertNull(entry.getDamagePerUse());
                assertNull(entry.getEffectiveCycleSeconds());
                assertNull(entry.getTheoreticalDps());
            }
        }
    }

    private static void durationBase(CharacterTreeSkill skill, int first, int max, int seconds) {
        assertTrue(skill.getBaseDamagePercentRanks().isEmpty());
        assertNull(skill.getBaseDamagePercentAtRank1());
        assertNull(skill.getBaseDamagePercentAtTreeMaxRank());
        var table = skill.getComponentDamagePercentRanks();
        sparse(table.tableFor(DAMAGE_OVER_DURATION), first, max);
        assertEquals(seconds, table.durationSecondsFor(DAMAGE_OVER_DURATION));
        assertNull(table.intervalSecondsFor(DAMAGE_OVER_DURATION));
    }
    private static void variant(UpgradeDamageModifier mod, DamagePercentComponent component, int first, int max, Integer duration, boolean creates) {
        var table = mod.getComponentDamagePercentRanks();
        sparse(table.tableFor(component), first, max);
        assertEquals(duration, table.durationSecondsFor(component));
        assertNull(table.intervalSecondsFor(component));
        assertEquals(creates, mod.createsNewDamageComponent());
        assertTrue(mod.scalesWithSkillRank());
    }
    private static void sparse(DamagePercentRankTable table, int first, int max) {
        assertEquals(Map.of(1, first, 15, max), table.asMap());
        for (int rank = 2; rank < 15; rank++) assertNull(table.damagePercentAtRank(rank));
    }
    private static CharacterTreeSkill skill(String id) { return NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(id); }
    private static List<UpgradeDamageModifier> modifiers(String id, String name) { return skill(id).getUpgradeDamageModifiers().stream().filter(mod -> mod.getUpgradeName().equals(name)).toList(); }
    private static UpgradeDamageModifier modifier(String id, String name) { return modifiers(id, name).getFirst(); }
    private static List<CharacterTreeSkill> ultimate() { return NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills().stream().filter(skill -> skill.getSkillGroup().equals("ultimate")).toList(); }
    private static int upgrades(List<CharacterTreeSkill> skills) { return skills.stream().flatMap(skill -> skill.getUpgradeGroups().stream()).mapToInt(group -> group.getUpgrades().size()).sum(); }
}

