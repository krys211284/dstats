package krys.skilltree;

import krys.ranking.*;
import krys.web.DamageRankingRow;
import krys.web.DamageRankingSearchText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SkillChargeDataTest {
    // Golden N4: bazowy profil Kościanego Widma, niezależny od zwykłego cooldownu.
    @Test
    void shouldKeepPositiveImmutableChargeMetadata() {
        var charges = new SkillChargeData(2, 12);
        assertEquals(2, charges.baseCharges());
        assertEquals(12, charges.chargeCooldownSeconds());
        assertEquals(new SkillChargeData(2, 12), charges);
        assertEquals(charges, skill(null, charges).getChargeData());
    }

    @ParameterizedTest
    @CsvSource({"0,12", "-1,12", "2,0", "2,-1"})
    void shouldRejectNonPositiveChargeCountOrRecharge(int count, int cooldown) {
        assertThrows(IllegalArgumentException.class, () -> new SkillChargeData(count, cooldown));
    }

    @Test
    void shouldAllowMissingChargesAndPreserveDefaultsForAllEarlierCatalogSkills() {
        assertNull(skill(null, null).getChargeData());
        var simple = new CharacterTreeSkill("test", "Test", "Źródło", "test", null, null,
                SkillTreeType.DAMAGE, SkillTreeStatus.NEEDS_VERIFICATION, List.of(), "Opis");
        assertNull(simple.getChargeData());
        int previousSkills = 0;
        var provider = SkillTreeRegistryProvider.production();
        for (var character : provider.supportedClasses()) {
            for (var skill : provider.registryFor(character).allSkills()) {
                if (Set.of("macabre", "curse", "ultimate").contains(skill.getSkillGroup())) continue;
                assertNull(skill.getChargeData(), skill.getSkillId());
                previousSkills++;
            }
        }
        assertEquals(24 + 13, previousSkills);
    }

    @Test
    void shouldRejectSimultaneousOrdinaryCooldownAndChargeRecharge() {
        assertThrows(IllegalArgumentException.class, () -> skill(12, new SkillChargeData(2, 12)));
    }

    @Test
    void shouldPresentAndIndexChargesThroughTheNeutralCatalogWithoutRuntimeCalculations() {
        var charged = skill(null, new SkillChargeData(2, 12));
        var resource = new SkillResourceType("test", "Koszt testowy", "Generowanie testowe", "pkt.");
        var registry = new CharacterSkillTreeRegistry(PlayableClass.PALADIN, "Neutralny test", List.of(charged),
                resource, Map.of("test", "Test"));
        var service = new DamageRankingService(null, new SkillTreeRegistryProvider(List.of(registry)));
        for (var metric : DamageRankingMetric.values()) {
            var entry = service.rankDamageSkills(PlayableClass.PALADIN, metric).getFirst();
            assertNull(entry.getCooldownSeconds());
            assertNull(entry.getDamagePerUse());
            assertNull(entry.getEffectiveCycleSeconds());
            assertNull(entry.getTheoreticalDps());
            var row = new DamageRankingRow(entry, charged, resource, "Test");
            assertEquals("2 ładunki; 12 s / ładunek", row.getCooldownSummary());
            assertEquals(new SkillChargeData(2, 12), row.getChargeData());
            var search = DamageRankingSearchText.normalizedRowText(row);
            assertTrue(search.contains("2 ladunki"));
            assertTrue(search.contains("12 s / ladunek"));
        }
    }

    private static CharacterTreeSkill skill(Integer cooldown, SkillChargeData charges) {
        return new CharacterTreeSkill("test", "Test ładunków", "Źródło", "test", 150, 383,
                DamagePercentRankTable.of(Map.of(1, 150, 15, 383)), DamagePercentComponentRankTable.empty(),
                SkillTreeType.DAMAGE, SkillTreeStatus.NEEDS_VERIFICATION, List.of(), "Opis",
                Set.of(), Set.of(), SkillResourceData.EMPTY, 33, List.of(), List.of(), cooldown, List.of(), charges);
    }
}
