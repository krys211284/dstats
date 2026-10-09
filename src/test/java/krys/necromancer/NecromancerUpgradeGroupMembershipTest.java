package krys.necromancer;

import krys.skilltree.SkillUpgrade;
import krys.skilltree.SkillUpgradeGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerUpgradeGroupMembershipTest {
    // Golden membership: jawne Group 1 / Group 2 / T z Icy Veins; polskie nazwy z D4Guides, etapy N1–N6.
    private static final Map<String, Map<String, Set<String>>> GOLDEN_GROUPS = Map.ofEntries(
            Map.entry("wlocznia_z_kosci", groups(
                    Set.of("Premia do Obrażeń od Pierwszego Trafienia", "Redukcja Kosztu"),
                    Set.of("Premia do Obrażeń od Przebicia", "Animusz"),
                    Set.of("Kościane Kolce", "Włócznia Krwi", "Rozszczepianie Cieni"))),
            Map.entry("mor", groups(
                    Set.of("Premia do Obrażeń Obszarowych", "Premia do Rozmiaru"),
                    Set.of("Premia do Obrażeń za Ograniczenie Kontroli", "Szansa na Szczęśliwy Traf"),
                    Set.of("Przeszywający Mrok", "Wir", "Niestabilna Krew"))),
            Map.entry("rozdarcie", groups(
                    Set.of("Premia do Obrażeń", "Redukcja Kosztu"),
                    Set.of("Zajadłość", "Ograniczanie Kontroli i Generowanie Zwłok"),
                    Set.of("Żniwiarski Lotos", "Nieubłagany Żniwiarz", "Chłodna Pogoń"))),
            Map.entry("naplyw_krwi", groups(
                    Set.of("Premia do Obrażeń", "Umocnienie"),
                    Set.of("Przytłoczenie", "Osłabienie"),
                    Set.of("Krwawa Jatka", "Ty i Jaka Armia?", "Igły"))),
            Map.entry("krwawa_lanca", groups(
                    Set.of("Rykoszet", "Umocnienie"),
                    Set.of("Przytłoczenie", "Redukcja Kosztu"),
                    Set.of("Poszukiwanie Krwi", "Okrwawione Kolce", "Ropiejąca Rana"))),
            Map.entry("szkieletowy_mag", groups(
                    Set.of("Czas Działania Premii do Obrażeń", "Szansa na Trafienie Krytyczne"),
                    Set.of("Zajadłość, Animusz lub Przytłoczenie", "Premia do Obrażeń od Ograniczenia Kontroli"),
                    Set.of("Sabat", "Dar Śmierci", "Osobliwość"))),
            Map.entry("kosciane_odlamki", groups(
                    Set.of("Pociski", "Generowanie Esencji"),
                    Set.of("Odsłonięcie", "Animusz"),
                    Set.of("Rykoszetujące Kolce", "Krwawe Odłamki", "Poszukiwacze Cienia"))),
            Map.entry("zniwa", groups(
                    Set.of("Szansa na Trafienie Krytyczne", "Generowanie Esencji"),
                    Set.of("Zajadłość", "Generowanie Zwłok"),
                    Set.of("Przejmujący Chłód", "Selekcja Naturalna", "Dorzynki"))),
            Map.entry("rozklad", groups(
                    Set.of("Premia do Obrażeń", "Bariera"),
                    Set.of("Ograniczenie Kontroli", "Szansa na Szczęśliwy Traf"),
                    Set.of("Targanie", "Gnijący Rozprysk", "Próchnienie"))),
            Map.entry("krwotok", groups(
                    Set.of("Szybkość Użycia", "Krwawa Kula"),
                    Set.of("Przytłoczenie", "Osłabienie"),
                    Set.of("Krwawy Wrzód", "Zmrożenie Krwi", "Rozdarcie Dusz"))),
            Map.entry("wybuch_zwlok", groups(
                    Set.of("Wiele Zwłok", "Generowanie Esencji"),
                    Set.of("Krwawa Kula", "Wydajne Zwłoki"),
                    Set.of("Krwawa Miazga", "Miazmaty", "Szrapnel"))),
            Map.entry("trupie_macki", groups(
                    Set.of("Szansa na Trafienie Krytyczne", "Generowanie Esencji"),
                    Set.of("Szansa na Szczęśliwy Traf", "Odsłonięcie"),
                    Set.of("Gorzkie Żniwa", "Chodź Tu!", "Szczęki Śmierci"))),
            Map.entry("szkieletowy_wojownik", groups(
                    Set.of("Premia do Obrażeń", "Leczenie"),
                    Set.of("Animusz", "Odsłonięcie"),
                    Set.of("Władca Marionetek", "Służba i Ofiara", "Litania Śmierci"))),
            Map.entry("krwawa_mgla", groups(
                    Set.of("Szybkość Ruchu", "Trafienie Krytyczne"),
                    Set.of("Przytłoczenie", "Odsłonięcie i Spowolnienie"),
                    Set.of("Transfuzja Krwi", "Przypływ Krwi", "Pochłaniająca Mgła"))),
            Map.entry("wiezienie_z_kosci", groups(
                    Set.of("Generowanie Esencji", "Redukcja Czasu Odnowienia"),
                    Set.of("Animusz", "Odsłonięcie"),
                    Set.of("Cierń", "Bezdenna Ciemność", "Uwięzienie Życia"))),
            Map.entry("kosciane_widmo", groups(
                    Set.of("Ładunki", "Umiejętność Główna"),
                    Set.of("Maksymalna Esencja", "Premia do Obrażeń"),
                    Set.of("Poltergeisty", "Niedokończone Porachunki", "Projekcja Astralna"))),
            Map.entry("golem", groups(
                    Set.of("Nieustępliwość", "Premia do Obrażeń"),
                    Set.of("Osłabienie", "Animusz, Przytłoczenie lub Zajadłość"),
                    Set.of("Grobowy Kwiat", "Plugawa Żarłoczność", "Gigantyczność"))),
            Map.entry("zelazna_dziewica", groups(
                    Set.of("Generowanie Esencji", "Egzekucja i Umocnienie"),
                    Set.of("Zajadłość i Przytłoczenie", "Odsłonięcie"),
                    Set.of("Złośliwa Radość", "Mistrz Małodobry", "Krwawa Pannica"))),
            Map.entry("zniedoleznienie", groups(
                    Set.of("Szansa na Trafienie Krytyczne", "Redukcja Czasu Odnowienia"),
                    Set.of("Szybkość Ruchu", "Animusz"),
                    Set.of("Oszałamiająca Klątwa", "Upuszczanie Zdrowia", "Świętokradczy Amok"))),
            Map.entry("armia_umarlych", groups(
                    Set.of("Redukcja Czasu Odnowienia", "Premia do Obrażeń"),
                    Set.of("Pasywna Premia", "Generowanie Zwłok"),
                    Set.of("Niezłomne Dowództwo", "Piętrzenie Ciał", "Martwe Zimno"))),
            Map.entry("fala_krwi", groups(
                    Set.of("Premia do Obrażeń", "Redukcja Obrażeń"),
                    Set.of("Przytłoczenie", "Krwawa Kula"),
                    Set.of("Krwawy Przypływ", "Ścieżka Ciemności", "Hematolagnia"))),
            Map.entry("nawalnica_kosci", groups(
                    Set.of("Bariera", "Odsłonięcie"),
                    Set.of("Redukcja Obrażeń", "Wydłużenie Czasu Działania"),
                    Set.of("Rzucone Kości", "Cień i Kość", "Wygłodniały Cyklon"))),
            Map.entry("spetanie_dusz", groups(
                    Set.of("Szybkość Ruchu", "Bariera"),
                    Set.of("Zajadłość", "Odsłonięcie i Ograniczanie Kontroli"),
                    Set.of("Destylacja Duszy", "Wir Dusz", "Zamarznięte Pustkowia"))));

    @ParameterizedTest(name = "{0}: członkostwo i spójność trzech reprezentacji")
    @MethodSource("sourceGroups")
    void shouldMatchExactGoldenMembersAndEveryModifierAndImpactGroup(String skillId, Map<String, Set<String>> expected) {
        var skill = NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill(skillId);
        assertEquals(expected.size(), skill.getUpgradeGroups().size());
        assertEquals(expected.keySet(), skill.getUpgradeGroups().stream().map(SkillUpgradeGroup::getId).collect(Collectors.toSet()));
        for (var group : skill.getUpgradeGroups()) {
            assertEquals(expected.get(group.getId()).size(), group.getUpgrades().size());
            assertEquals(expected.get(group.getId()), group.getUpgrades().stream().map(SkillUpgrade::getName).collect(Collectors.toSet()),
                    skillId + " / " + group.getId());
            for (var upgrade : group.getUpgrades()) {
                var modifiers = skill.getUpgradeDamageModifiers().stream()
                        .filter(modifier -> modifier.getUpgradeName().equals(upgrade.getName())).toList();
                assertFalse(modifiers.isEmpty(), upgrade.getName());
                for (var modifier : modifiers) {
                    assertEquals(group.getId(), modifier.getUpgradeGroup(), upgrade.getName() + " / " + modifier.getValue());
                }
                var impacts = skill.getUpgradeDamageImpacts().stream()
                        .filter(impact -> impact.getUpgradeName().equals(upgrade.getName())).toList();
                assertEquals(1, impacts.size(), upgrade.getName());
                assertEquals(group.getId(), impacts.getFirst().getGroupId(), upgrade.getName());
            }
        }
    }

    @Test
    void shouldKeepOneAlternativeFromEachDisjointGroupForEveryNecromancerCatalogCombination() {
        var skills = NecromancerSkillTreeRegistry.asCharacterRegistry().allSkills();
        assertEquals(GOLDEN_GROUPS.keySet(), skills.stream().map(skill -> skill.getSkillId()).collect(Collectors.toSet()));
        for (var skill : skills) {
            var groups = skill.getUpgradeGroups().stream().collect(Collectors.toMap(SkillUpgradeGroup::getId, group -> group));
            var golden = GOLDEN_GROUPS.get(skill.getSkillId());
            // Rozłączne alternatywy: node nie może należeć do dwóch grup wyboru.
            var allIds = groups.values().stream().flatMap(group -> group.getUpgrades().stream()).map(SkillUpgrade::getId).toList();
            assertEquals(allIds.size(), Set.copyOf(allIds).size(), skill.getSkillId());
            Set<List<String>> combinations = new HashSet<>();
            for (var first : groups.get("grupa_1").getUpgrades()) {
                for (var second : groups.get("grupa_2").getUpgrades()) {
                    for (var transformation : groups.get("grupa_3").getUpgrades()) {
                        // Katalogowy wybór to dokładnie jeden Enhancement z każdej pary i jedna Transformation.
                        var choice = Map.of("grupa_1", first, "grupa_2", second, "grupa_3", transformation);
                        assertEquals(golden.keySet(), choice.keySet());
                        choice.forEach((groupId, upgrade) -> {
                            assertTrue(golden.get(groupId).contains(upgrade.getName()), skill.getSkillId() + " / " + upgrade.getName());
                            assertTrue(groups.get(groupId).getUpgrades().contains(upgrade));
                        });
                        assertTrue(combinations.add(List.of(first.getId(), second.getId(), transformation.getId())));
                    }
                }
            }
            // 2 × 2 × 3 wynika ze źródłowych alternatyw, bez sumowania ich efektów.
            assertEquals(2 * 2 * 3, combinations.size(), skill.getSkillId());
        }
    }

    private static Stream<Arguments> sourceGroups() {
        return GOLDEN_GROUPS.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> Arguments.of(entry.getKey(), entry.getValue()));
    }

    private static Map<String, Set<String>> groups(Set<String> first, Set<String> second, Set<String> transformations) {
        return Map.of("grupa_1", first, "grupa_2", second, "grupa_3", transformations);
    }
}
