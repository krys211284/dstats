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

class NecromancerCurseSkillTreeRegistryTest {
    // Golden N5: D4Guides R1, jawne grupy Icy Veins oraz max-view głównej domeny D4Builds.
    @Test
    void shouldAppendTwoCurseSkillsAndFourteenUpgradesToThePreviousSeventeenAndOneHundredNineteen() {
        var registry = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(23, registry.allSkills().size());
        assertEquals(161, upgrades(registry.allSkills()));
        var previous = registry.allSkills().stream().filter(skill -> Set.of("basic", "core", "corpse", "macabre").contains(skill.getSkillGroup())).toList();
        assertEquals(17, previous.size());
        assertEquals(119, upgrades(previous));
        assertEquals(List.of("basic", "core", "corpse", "macabre", "curse", "ultimate"),
                registry.allSkills().stream().map(CharacterTreeSkill::getSkillGroup).distinct().toList());
        assertEquals(List.of("zelazna_dziewica", "zniedoleznienie"), curse().stream().map(CharacterTreeSkill::getSkillId).toList());
        assertEquals(14, upgrades(curse()));
        assertEquals(new SourceSkillCategory("CURSE", "Klątwy", 4), NecromancerSkillTreeRegistry.CURSE);
        assertEquals("Klątwy / Curse", registry.treeGroupDisplayName("curse"));
        assertTrue(registry.supportsSourceCategory("CURSE"));
        assertTrue(registry.supportsMechanicTag("CURSE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"zelazna_dziewica", "zniedoleznienie"})
    void shouldKeepBaseResourcesMetadataAndAllUpgradeRuntimeFlags(String id) {
        var skill = skill(id);
        assertEquals(SkillTreeStatus.NEEDS_VERIFICATION, skill.getStatus());
        assertEquals(Set.of(NecromancerSkillTreeRegistry.CURSE), skill.getSkillCategories());
        assertEquals(new BigDecimal("10"), skill.getResourceData().cost());
        assertFalse(skill.getResourceData().hasGeneration());
        assertNull(skill.getCooldownSeconds());
        assertNull(skill.getChargeData());
        assertTrue(skill.getActivationRequirements().isEmpty());
        assertNull(skill.getBaseDamagePercentAtRank1());
        assertNull(skill.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(skill.getBaseDamagePercentRanks().isEmpty());
        assertTrue(skill.getNotes().contains("30 s"));
        assertEquals(List.of(2, 2, 3), skill.getUpgradeGroups().stream().map(group -> group.getUpgrades().size()).toList());
        assertEquals(7, skill.getUpgradeDamageImpacts().size());
        assertTrue(skill.getUpgradeDamageModifiers().stream().allMatch(modifier -> modifier.getSafeForRuntimeDps() == UpgradeDamageSafety.NO));
    }

    @Test
    void shouldKeepMaidenAsTwoSparseMechanismsWithIntervalOnlyForPeriodicDamage() {
        var maiden = skill("zelazna_dziewica");
        assertEquals("Żelazna Dziewica", maiden.getSkillName());
        assertEquals(SkillTreeType.DAMAGE, maiden.getType());
        assertEquals(Set.of("CURSE", "PROFANE", "PHYSICAL"), maiden.getTags());
        assertEquals(5, maiden.getLuckyHitPercent());
        var components = maiden.getComponentDamagePercentRanks();
        assertEquals(Set.of(PERIODIC_DAMAGE, EVENT_TRIGGERED_DAMAGE), components.asMap().keySet());
        for (var component : components.asMap().keySet()) {
            assertEquals(Map.of(1, 30, 15, 77), components.tableFor(component).asMap());
            for (int rank = 2; rank <= 14; rank++) assertNull(components.damagePercentAt(component, rank));
            assertNull(components.durationSecondsFor(component));
        }
        assertEquals(2, components.intervalSecondsFor(PERIODIC_DAMAGE));
        assertNull(components.intervalSecondsFor(EVENT_TRIGGERED_DAMAGE));
        assertTrue(maiden.getNotes().contains("przeklęty wróg zadaje bezpośrednie obrażenia"));
        assertTrue(maiden.getNotes().contains("Aspect of Crippling Darkness"));
    }

    @Test
    void shouldKeepZeroVariantCostAndConditionalGenerationOutsideBaseResources() {
        var modifiers = modifiers("zelazna_dziewica", "Generowanie Esencji");
        assertEquals(2, modifiers.size());
        assertTrue(modifiers.stream().allMatch(modifier -> modifier.getType() == RESOURCE_OR_COST));
        assertEquals(Set.of("Koszt wariantu: 0 Esencji", "+5 Esencji za nowo przeklętego wroga"),
                Set.copyOf(modifiers.stream().map(UpgradeDamageModifier::getValue).toList()));
        assertTrue(modifiers.getLast().getCondition().contains("nie był już"));
        assertEquals(new BigDecimal("10"), skill("zelazna_dziewica").getResourceData().cost());
        assertFalse(skill("zelazna_dziewica").getResourceData().hasGeneration());
        assertTrue(modifier("zelazna_dziewica", "Egzekucja i Umocnienie", STATUS_DAMAGE_ENABLER).getNotes().contains("nie podaje progu"));
        assertEquals("Umocnienie: 5% maksymalnego zdrowia", modifier("zelazna_dziewica", "Egzekucja i Umocnienie", DEFENSE_OR_UTILITY).getValue());
        assertTrue(modifier("zelazna_dziewica", "Zajadłość i Przytłoczenie").getValue().contains("do 15%; 1 Ferocity albo 1 Overpower"));
        assertEquals(STATUS_DAMAGE_ENABLER, modifier("zelazna_dziewica", "Odsłonięcie").getType());
    }

    @Test
    void shouldKeepSchadenfreudeAsDynamicThornsWithoutAWeaponCoefficient() {
        var thorns = modifier("zelazna_dziewica", "Złośliwa Radość");
        assertEquals(THORNS_DAMAGE_MODIFIER, thorns.getType());
        assertEquals(UpgradeDamageValueKind.TEXT_ONLY, thorns.getValueKind());
        assertEquals(UpgradeDamageSafety.YES, thorns.getUpgradeHasDamageModifier());
        assertEquals(UpgradeDamageSafety.NEEDS_MANUAL_REVIEW, thorns.getSafeForRankingDisplay());
        assertEquals(UpgradeDamageSafety.NO, thorns.getSafeForRuntimeDps());
        assertTrue(thorns.getValue().contains("X Cierni"));
        assertTrue(thorns.getValue().contains("100% twoich Cierni"));
        assertTrue(thorns.getCondition().contains("Co najmniej 1"));
        assertTrue(thorns.getComponentDamagePercentRanks().isEmpty());
        assertNull(skill("zelazna_dziewica").getUpgradeDamageImpacts().stream()
                .filter(impact -> impact.getUpgradeName().equals("Złośliwa Radość")).findFirst().orElseThrow().getDamagePercent());
    }

    @Test
    void shouldKeepTortureArtistConversionAndStunOnlyInTheVariant() {
        var conversion = modifier("zelazna_dziewica", "Mistrz Małodobry");
        assertEquals(STATUS_DAMAGE_ENABLER, conversion.getType());
        assertTrue(conversion.getValue().contains("Darkness / Shadow; 30% szansy na Stun 2 s"));
        assertTrue(conversion.getNotes().contains("R1 30% / max-view 77%"));
        assertTrue(conversion.getComponentDamagePercentRanks().isEmpty());
        assertEquals(Set.of("CURSE", "PROFANE", "PHYSICAL"), skill("zelazna_dziewica").getTags());
    }

    @Test
    void shouldKeepBloodMaidenBurstSeparateFromTheFourOrbsAndBaseComponents() {
        var burst = modifier("zelazna_dziewica", "Krwawa Pannica", FLAT_COMPONENT_PERCENT);
        assertEquals(Set.of(BURST_DAMAGE), burst.getComponentDamagePercentRanks().asMap().keySet());
        assertEquals(Map.of(1, 150, 15, 383), burst.getComponentDamagePercentRanks().tableFor(BURST_DAMAGE).asMap());
        for (int rank = 2; rank <= 14; rank++) assertNull(burst.getComponentDamagePercentRanks().damagePercentAt(BURST_DAMAGE, rank));
        assertNull(burst.getComponentDamagePercentRanks().durationSecondsFor(BURST_DAMAGE));
        assertNull(burst.getComponentDamagePercentRanks().intervalSecondsFor(BURST_DAMAGE));
        assertTrue(burst.getCondition().contains("dostępnych"));
        assertTrue(burst.createsNewDamageComponent());
        assertEquals(UpgradeDamageValueKind.HIT_COUNT, modifier("zelazna_dziewica", "Krwawa Pannica", DEFENSE_OR_UTILITY).getValueKind());
        assertEquals("Zbiera 4 Blood Orbs", modifier("zelazna_dziewica", "Krwawa Pannica", DEFENSE_OR_UTILITY).getValue());
        assertTrue(modifier("zelazna_dziewica", "Krwawa Pannica", STATUS_DAMAGE_ENABLER).getValue().contains("5%"));
        assertFalse(skill("zelazna_dziewica").getComponentDamagePercentRanks().hasComponent(BURST_DAMAGE));
    }

    @Test
    void shouldKeepDecrepifyAsSupportWithSourceSlowOutsideDamageTables() {
        var decrepify = skill("zniedoleznienie");
        assertEquals("Zniedołężnienie", decrepify.getSkillName());
        assertEquals(SkillTreeType.SUPPORT, decrepify.getType());
        assertEquals(Set.of("CURSE", "PROFANE"), decrepify.getTags());
        assertNull(decrepify.getLuckyHitPercent());
        assertTrue(decrepify.getComponentDamagePercentRanks().isEmpty());
        assertTrue(decrepify.getNotes().contains("Weaken / Osłabienie"));
        assertTrue(decrepify.getNotes().contains("R1 40%, max-view 56%"));
        assertEquals("+5%[+]", modifier("zniedoleznienie", "Szansa na Trafienie Krytyczne").getValue());
        var cooldown = modifier("zniedoleznienie", "Redukcja Czasu Odnowienia");
        assertEquals(CAST_SPEED_OR_COOLDOWN, cooldown.getType());
        assertTrue(cooldown.getValue().contains("do 15%; losowy aktywny cooldown -1 s"));
        assertEquals("+2%[x] za wroga", modifier("zniedoleznienie", "Szybkość Ruchu").getValue());
        assertEquals("Lucky Hit: do 25%; 1 Resolve", modifier("zniedoleznienie", "Animusz").getValue());
    }

    @Test
    void shouldKeepDazeHealingAndMinionReplacementAsUtilityWithoutDamageRankTables() {
        var daze = modifier("zniedoleznienie", "Oszałamiająca Klątwa");
        assertEquals("Daze R1 3.0 s / max-view 4.2 s", daze.getValue());
        assertTrue(daze.getCondition().contains("Pierwsze"));
        var heal = modifier("zniedoleznienie", "Upuszczanie Zdrowia");
        assertTrue(heal.getValue().contains("R1 10% / max-view 25.5%"));
        assertTrue(heal.getCondition().contains("maksymalnie raz na sekundę"));
        assertEquals("20% szansy na Blood Orb", modifiers("zniedoleznienie", "Upuszczanie Zdrowia").getLast().getValue());
        var minions = modifiers("zniedoleznienie", "Świętokradczy Amok");
        assertEquals(2, minions.size());
        assertEquals(Set.of(CAST_SPEED_OR_COOLDOWN, DEFENSE_OR_UTILITY), Set.copyOf(minions.stream().map(UpgradeDamageModifier::getType).toList()));
        for (var modifier : minions) {
            assertTrue(modifier.getValue().contains("Minions R1 +20%[x] / max-view +51%[x]"));
            assertTrue(modifier.getNotes().toLowerCase(java.util.Locale.ROOT).contains("zastępuje cel i efekt"));
            assertTrue(modifier.getNotes().contains("zamiast wrogów"));
        }
        assertTrue(skill("zniedoleznienie").getUpgradeDamageModifiers().stream().allMatch(modifier -> modifier.getComponentDamagePercentRanks().isEmpty()));
        assertTrue(minions.getFirst().getNotes().contains("Weaken i Slow nie są przenoszone"));
        assertEquals(Set.of("CURSE", "PROFANE"), skill("zniedoleznienie").getTags());
    }

    @Test
    void shouldKeepAllNineteenCatalogRowsWithoutRuntimeAndPreserveTwentyFourPaladinSkills() {
        var provider = SkillTreeRegistryProvider.production();
        assertEquals(24, provider.registryFor(PlayableClass.PALADIN).allSkills().size());
        var service = new DamageRankingService(null, provider);
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

    private static int upgrades(List<CharacterTreeSkill> skills) {
        return skills.stream().flatMap(skill -> skill.getUpgradeGroups().stream()).mapToInt(group -> group.getUpgrades().size()).sum();
    }
    private static List<CharacterTreeSkill> curse() {
        return NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills().stream().filter(skill -> skill.getSkillGroup().equals("curse")).toList();
    }
    private static CharacterTreeSkill skill(String id) { return NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(id); }
    private static List<UpgradeDamageModifier> modifiers(String id, String name) {
        return skill(id).getUpgradeDamageModifiers().stream().filter(modifier -> modifier.getUpgradeName().equals(name)).toList();
    }
    private static UpgradeDamageModifier modifier(String id, String name) { return modifiers(id, name).getFirst(); }
    private static UpgradeDamageModifier modifier(String id, String name, UpgradeDamageModifierType type) {
        return modifiers(id, name).stream().filter(modifier -> modifier.getType() == type).findFirst().orElseThrow();
    }
}
