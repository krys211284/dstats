package krys.necromancer;

import krys.ranking.*;
import krys.skilltree.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static krys.skilltree.DamagePercentComponent.*;
import static krys.skilltree.UpgradeDamageModifierType.*;
import static org.junit.jupiter.api.Assertions.*;

class NecromancerMacabreSkillTreeRegistryTest {
    @Test
    void shouldAppendFourMacabreSkillsAndTwentyEightUpgradesToThePreviousThirteenAndNinetyOne() {
        var registry = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(23, registry.allSkills().size());
        assertEquals(161, upgradeCount(registry.allSkills()));
        var previous = registry.allSkills().stream().filter(skill -> Set.of("basic", "core", "corpse").contains(skill.getSkillGroup())).toList();
        assertEquals(13, previous.size());
        assertEquals(91, upgradeCount(previous));
        assertEquals(List.of("krwawa_mgla", "wiezienie_z_kosci", "kosciane_widmo", "golem"),
                macabre().stream().map(CharacterTreeSkill::getSkillId).toList());
        assertEquals(28, upgradeCount(macabre()));
        assertEquals(List.of("basic", "core", "corpse", "macabre", "curse", "ultimate"),
                registry.allSkills().stream().map(CharacterTreeSkill::getSkillGroup).distinct().toList());
        assertEquals("Makabra / Macabre", registry.treeGroupDisplayName("macabre"));
        assertEquals(new SourceSkillCategory("MACABRE", "Makabra", 3), NecromancerSkillTreeRegistry.MACABRE);
        for (var skill : macabre()) {
            assertEquals(Set.of(NecromancerSkillTreeRegistry.MACABRE), skill.getSkillCategories());
            assertEquals(SkillTreeStatus.NEEDS_VERIFICATION, skill.getStatus());
            assertEquals(List.of(2, 2, 3), skill.getUpgradeGroups().stream().map(group -> group.getUpgrades().size()).toList());
            assertEquals(7, skill.getUpgradeDamageImpacts().size());
            assertTrue(skill.getUpgradeDamageModifiers().stream().allMatch(modifier -> modifier.getSafeForRuntimeDps() == UpgradeDamageSafety.NO));
            assertTrue(skill.getNotes().contains("bez jawnej etykiety Rank 15"));
            assertTrue(skill.getActivationRequirements().isEmpty());
            assertEquals(SkillResourceData.EMPTY, skill.getResourceData());
        }
    }

    // Golden N4: jawny tekst bieżących źródeł i punkty max-view z głównej domeny D4Builds.
    @ParameterizedTest
    @CsvSource(value = {"krwawa_mgla,DAMAGE,9,24", "wiezienie_z_kosci,DEFENSIVE,NULL,15",
            "kosciane_widmo,DAMAGE,33,NULL", "golem,DAMAGE,20,16"}, nullValues = "NULL")
    void shouldPreserveSourceTypeLuckyHitAndOrdinaryCooldown(String id, SkillTreeType type, Integer luckyHit, Integer cooldown) {
        var skill = skill(id);
        assertEquals(type, skill.getType());
        assertEquals(luckyHit, skill.getLuckyHitPercent());
        assertEquals(cooldown, skill.getCooldownSeconds());
        var tags = Map.of("krwawa_mgla", Set.of("MACABRE", "BLOOD", "PHYSICAL"),
                "wiezienie_z_kosci", Set.of("MACABRE", "BONE"),
                "kosciane_widmo", Set.of("MACABRE", "BONE", "PHYSICAL"),
                "golem", Set.of("MACABRE", "MINION", "SUMMON"));
        assertEquals(tags.get(id), skill.getTags());
        if (!id.equals("kosciane_widmo")) assertNull(skill.getChargeData());
    }

    @Test
    void shouldKeepMistAsTotalDamageOverThreeSecondsAndBloodRushAsASeparateVariant() {
        var mist = skill("krwawa_mgla");
        assertNull(mist.getBaseDamagePercentAtRank1());
        assertNull(mist.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(mist.getBaseDamagePercentRanks().isEmpty());
        var table = mist.getComponentDamagePercentRanks();
        assertEquals(Set.of(DAMAGE_OVER_DURATION), table.asMap().keySet());
        assertSparse(table.tableFor(DAMAGE_OVER_DURATION), 420, 1071);
        assertEquals(3, table.durationSecondsFor(DAMAGE_OVER_DURATION));
        assertTrue(mist.getNotes().contains("R1 6.0%, max-view 15.3%"));
        assertVariant("krwawa_mgla", "Pochłaniająca Mgła", DAMAGE_OVER_DURATION, 600, 1530, 3, false);
        assertTrue(modifier("krwawa_mgla", "Pochłaniająca Mgła").getCondition().contains("zastępuje"));
        assertTrue(modifier("krwawa_mgla", "Przytłoczenie").getCondition().contains("tylko z Przypływem Krwi"));
        assertEquals("Zużywa 1 pobliskie Zwłoki", modifier("krwawa_mgla", "Przypływ Krwi", RESOURCE_OR_COST).getValue());
        assertEquals("Odnowienie wariantu: 8 s", modifier("krwawa_mgla", "Przypływ Krwi", CAST_SPEED_OR_COOLDOWN).getValue());
        assertTrue(modifier("krwawa_mgla", "Przypływ Krwi").getNotes().contains("R1 3 s, max-view 4.2 s"));
        assertEquals(24, mist.getCooldownSeconds());
        assertTrue(mist.getActivationRequirements().isEmpty());
        assertTrue(modifier("krwawa_mgla", "Transfuzja Krwi").getComponentDamagePercentRanks().isEmpty());
    }

    @Test
    void shouldKeepPrisonWithoutBaseDamageAndSeparateThornsFromTheConfirmedGroundCoefficient() {
        var prison = skill("wiezienie_z_kosci");
        assertNull(prison.getLuckyHitPercent());
        assertNull(prison.getBaseDamagePercentAtRank1());
        assertNull(prison.getBaseDamagePercentAtTreeMaxRank());
        assertTrue(prison.getBaseDamagePercentRanks().isEmpty());
        assertTrue(prison.getComponentDamagePercentRanks().isEmpty());
        assertTrue(prison.getNotes().contains("R1 6 s, max-view 8.4 s"));
        var thorns = modifier("wiezienie_z_kosci", "Cierń");
        assertEquals(THORNS_DAMAGE_MODIFIER, thorns.getType());
        assertEquals(UpgradeDamageValueKind.TEXT_ONLY, thorns.getValueKind());
        assertTrue(thorns.getValue().contains("100% twoich Cierni"));
        assertTrue(thorns.getComponentDamagePercentRanks().isEmpty());
        assertNull(prison.getUpgradeDamageImpacts().stream().filter(impact -> impact.getUpgradeName().equals("Cierń"))
                .findFirst().orElseThrow().getDamagePercent());
        assertVariant("wiezienie_z_kosci", "Bezdenna Ciemność", PRIMARY_DAMAGE, 450, 1148, null, true);
        assertEquals(DAMAGE_OVER_TIME, modifier("wiezienie_z_kosci", "Bezdenna Ciemność").getType());
        assertTrue(modifier("wiezienie_z_kosci", "Bezdenna Ciemność").getNotes().contains("300% -> 450%"));
        assertTrue(modifier("wiezienie_z_kosci", "Bezdenna Ciemność").getNotes().contains("https://news.blizzard.com/"));
        assertTrue(modifier("wiezienie_z_kosci", "Redukcja Czasu Odnowienia").getAffectedComponent().contains("Aktywne czasy odnowienia"));
        assertEquals("+50 Esencji; dodatkowo +25 za Bossa", modifier("wiezienie_z_kosci", "Generowanie Esencji").getValue());
        assertTrue(modifier("wiezienie_z_kosci", "Uwięzienie Życia", RESOURCE_OR_COST).getValue().contains("R1 +20% / max-view +51%"));
        assertTrue(modifier("wiezienie_z_kosci", "Uwięzienie Życia", CAST_SPEED_OR_COOLDOWN).getValue().contains("R1 +20% / max-view +51%"));
        assertEquals(SkillResourceData.EMPTY, prison.getResourceData());
    }

    @Test
    void shouldPreserveBaseChargesAndKeepCoreEnhancementCostsAndMultipliersConditionalInGroupOne() {
        var spirit = skill("kosciane_widmo");
        assertSparse(spirit.getBaseDamagePercentRanks(), 150, 383);
        assertEquals(new SkillChargeData(2, 12), spirit.getChargeData());
        assertNull(spirit.getCooldownSeconds());
        assertEquals("+1 ładunek; charge cooldown 12 -> 10 s", modifier("kosciane_widmo", "Ładunki").getValue());
        var core = spirit.getUpgradeDamageModifiers().stream().filter(modifier -> modifier.getUpgradeName().equals("Umiejętność Główna")).toList();
        assertEquals(4, core.size());
        assertTrue(core.stream().allMatch(modifier -> modifier.getUpgradeGroup().equals("grupa_1")));
        assertEquals(Set.of("koszt bazowy wariantu 30 Esencji", "zużywa całą pozostałą Esencję"),
                Set.copyOf(core.stream().filter(modifier -> modifier.getType() == RESOURCE_OR_COST).map(UpgradeDamageModifier::getValue).toList()));
        assertEquals("Wariant bez cooldownu i odnowienia ładunków", modifier("kosciane_widmo", "Umiejętność Główna", CAST_SPEED_OR_COOLDOWN).getValue());
        var multiplier = modifier("kosciane_widmo", "Umiejętność Główna", MULTIPLICATIVE_DAMAGE_PERCENT);
        assertEquals("+3%[x] per Essence consumed", multiplier.getValue());
        assertTrue(multiplier.getCondition().contains("każdy punkt Esencji"));
        assertTrue(modifier("kosciane_widmo", "Premia do Obrażeń").getCondition().contains("co sekundę"));
        assertEquals("+10%[x] / sek.; maks. +30%[x]", modifier("kosciane_widmo", "Premia do Obrażeń").getValue());
        assertEquals(SkillResourceData.EMPTY, spirit.getResourceData());
        assertEquals(Set.of(NecromancerSkillTreeRegistry.MACABRE), spirit.getSkillCategories());
    }

    @Test
    void shouldKeepSpiritVariantCoefficientsAndCountsOutsideItsBaseTable() {
        assertVariant("kosciane_widmo", "Poltergeisty", ADDITIONAL_STRIKE_DAMAGE, 30, 77, null, true);
        assertEquals("3 małe duchy", modifier("kosciane_widmo", "Poltergeisty", ADDITIONAL_HIT_OR_STRIKE).getValue());
        assertVariant("kosciane_widmo", "Niedokończone Porachunki", PRIMARY_DAMAGE, 250, 638, null, true);
        assertVariant("kosciane_widmo", "Projekcja Astralna", PRIMARY_DAMAGE, 100, 255, null, false);
        assertTrue(modifier("kosciane_widmo", "Projekcja Astralna").getCondition().contains("zastępuje"));
        assertEquals(150, skill("kosciane_widmo").getBaseDamagePercentAtRank1());
        assertEquals(383, skill("kosciane_widmo").getBaseDamagePercentAtTreeMaxRank());
    }

    @Test
    void shouldKeepGolemAttackCommandRelativeGravebloomAndFelGluttonySeparate() {
        var golem = skill("golem");
        assertSparse(golem.getBaseDamagePercentRanks(), 125, 319);
        assertTrue(golem.getNotes().contains("za jeden atak jednego Golema"));
        assertTrue(golem.getNotes().contains("odradza się 10 s"));
        assertTrue(golem.getNotes().contains("Aktywnie: rozkaz"));
        assertTrue(golem.getNotes().contains("Physical or Shadow"));
        assertTrue(golem.getNotes().contains("Book of the Dead"));
        var relative = modifier("golem", "Grobowy Kwiat");
        assertEquals(UpgradeDamageValueKind.TEXT_ONLY, relative.getValueKind());
        assertEquals("60% normalnych obrażeń", relative.getValue());
        assertTrue(relative.getComponentDamagePercentRanks().isEmpty());
        assertTrue(golem.getUpgradeDamageImpacts().stream().filter(impact -> impact.getUpgradeName().equals("Grobowy Kwiat"))
                .allMatch(impact -> impact.getDamagePercent() == null));
        assertTrue(golem.getUpgradeDamageModifiers().stream().anyMatch(modifier ->
                modifier.getUpgradeName().equals("Grobowy Kwiat") && modifier.getValue().equals("3 mniejsze Golemy")
                        && modifier.getValueKind() == UpgradeDamageValueKind.HIT_COUNT));
        assertEquals("+30%[+]", modifier("golem", "Grobowy Kwiat", CAST_SPEED_OR_COOLDOWN).getValue());
        assertVariant("golem", "Plugawa Żarłoczność", BURST_DAMAGE, 300, 765, null, true);
        assertEquals("Pasywnie zużywa pobliskie Zwłoki co sekundę", modifier("golem", "Plugawa Żarłoczność", RESOURCE_OR_COST).getValue());
        assertTrue(modifier("golem", "Plugawa Żarłoczność", CAST_SPEED_OR_COOLDOWN).getCondition().contains("dostępnych pobliskich Zwłok"));
        assertTrue(modifier("golem", "Gigantyczność", CAST_SPEED_OR_COOLDOWN).getAffectedComponent().contains("innych sług"));
        assertTrue(modifier("golem", "Gigantyczność").getValue().contains("R1 +20%[x] / max-view +51%[x]"));
        assertEquals(16, golem.getCooldownSeconds());
        assertTrue(golem.getActivationRequirements().isEmpty());
    }

    @Test
    void shouldKeepEveryRankingMetricWithoutEngineValuesAndPreserveThePaladinCatalog() {
        var provider = SkillTreeRegistryProvider.production();
        assertEquals(24, provider.registryFor(PlayableClass.PALADIN).allSkills().size());
        var service = new DamageRankingService(null, provider);
        for (var metric : DamageRankingMetric.values()) {
            var entries = service.rankDamageSkills(PlayableClass.NECROMANCER, metric);
            assertEquals(23, entries.size());
            for (var entry : entries) {
                assertEquals(skill(entry.getSkillId()).getCooldownSeconds(), entry.getCooldownSeconds());
                assertNull(entry.getDamagePerUse());
                assertNull(entry.getEffectiveCycleSeconds());
                assertNull(entry.getTheoreticalDps());
            }
        }
    }

    private static void assertVariant(String id, String name, DamagePercentComponent component,
                                      int rankOne, int maxView, Integer duration, boolean newComponent) {
        var modifier = modifier(id, name);
        var table = modifier.getComponentDamagePercentRanks();
        assertEquals(Set.of(component), table.asMap().keySet());
        assertSparse(table.tableFor(component), rankOne, maxView);
        assertEquals(duration, table.durationSecondsFor(component));
        assertEquals(newComponent, modifier.createsNewDamageComponent());
    }

    private static void assertSparse(DamagePercentRankTable table, int rankOne, int maxView) {
        assertEquals(Map.of(1, rankOne, 15, maxView), table.asMap());
        for (int rank = 2; rank <= 14; rank++) assertNull(table.damagePercentAtRank(rank));
    }

    private static int upgradeCount(List<CharacterTreeSkill> skills) {
        return skills.stream().flatMap(skill -> skill.getUpgradeGroups().stream()).mapToInt(group -> group.getUpgrades().size()).sum();
    }

    private static List<CharacterTreeSkill> macabre() {
        return NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills().stream().filter(skill -> skill.getSkillGroup().equals("macabre")).toList();
    }

    private static CharacterTreeSkill skill(String id) {
        return NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(id);
    }

    private static UpgradeDamageModifier modifier(String id, String name) {
        return skill(id).getUpgradeDamageModifiers().stream().filter(modifier -> modifier.getUpgradeName().equals(name)).findFirst().orElseThrow();
    }

    private static UpgradeDamageModifier modifier(String id, String name, UpgradeDamageModifierType type) {
        return skill(id).getUpgradeDamageModifiers().stream()
                .filter(modifier -> modifier.getUpgradeName().equals(name) && modifier.getType() == type).findFirst().orElseThrow();
    }
}
