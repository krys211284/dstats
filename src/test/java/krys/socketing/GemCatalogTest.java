package krys.socketing;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje kompletność i wartości GemCatalog v1. */
class GemCatalogTest {
    @Test
    void shouldContainSevenFamiliesEightTiersAndVerifiedDefinitions() {
        List<GemDefinition> definitions = GemCatalog.all();

        assertEquals(56, definitions.size());
        assertEquals(7, GemFamily.values().length);
        for (GemFamily family : GemFamily.values()) {
            assertEquals(8, GemCatalog.byFamily(family).size());
        }
        Set<String> ids = new HashSet<>();
        for (GemDefinition definition : definitions) {
            assertTrue(ids.add(definition.getId()));
            assertEquals(GemValueVerificationStatus.VERIFIED_SCREENSHOT, definition.getVerificationStatus());
        }
    }

    @Test
    void shouldUsePolishNamesForSelectedDefinitions() {
        assertEquals("Wspaniały Rubin", gem("ruby_grand").getDisplayName());
        assertEquals("Nieskazitelny Horadryjski Rubin", gem("ruby_flawless_horadric").getDisplayName());
        assertEquals("Wspaniała Czaszka", gem("skull_grand").getDisplayName());
        assertEquals("Nieskazitelna Horadryjska Czaszka", gem("skull_flawless_horadric").getDisplayName());
        assertEquals("Wspaniały Diament", gem("diamond_grand").getDisplayName());
        assertEquals("Nieskazitelny Horadryjski Diament", gem("diamond_flawless_horadric").getDisplayName());
    }

    @Test
    void shouldExposeFullRubyValueProgression() {
        assertGem("ruby_chipped", "Surowy Rubin", "x14% Ognia i Świętości", "+10 siły", "+50 odporności na Ogień");
        assertGem("ruby_crude", "Nadkruszony Rubin", "x16% Ognia i Świętości", "+20 siły", "+250 odporności na Ogień");
        assertGem("ruby_standard", "Rubin", "x18% Ognia i Świętości", "+30 siły", "+450 odporności na Ogień");
        assertGem("ruby_flawless", "Nieskazitelny Rubin", "x20% Ognia i Świętości", "+40 siły", "+900 odporności na Ogień");
        assertGem("ruby_royal", "Królewski Rubin", "x22% Ognia i Świętości", "+60 siły", "+1 750 odporności na Ogień");
        assertGem("ruby_grand", "Wspaniały Rubin", "x24% Ognia i Świętości", "+90 siły", "+2 625 odporności na Ogień");
        assertGem("ruby_horadric", "Horadryjski Rubin", "x28% Ognia i Świętości", "+120 siły", "+3 500 odporności na Ogień");
        assertGem("ruby_flawless_horadric", "Nieskazitelny Horadryjski Rubin", "x32% Ognia i Świętości", "+150 siły", "+4 375 odporności na Ogień");
    }

    @Test
    void shouldExposeSelectedGrandAndHoradricValuesForOtherFamilies() {
        assertGem("sapphire_grand", "Wspaniały Szafir", "x24% Zimna", "+90 siły woli", "+2 625 odporności na Zimno");
        assertGem("emerald_grand", "Wspaniały Szmaragd", "x24% Trucizny", "+90 zręczności", "+2 625 odporności na Truciznę");
        assertGem("topaz_grand", "Wspaniały Topaz", "x24% Błyskawic", "+90 inteligencji", "+2 625 odporności na Błyskawice");
        assertGem("amethyst_grand", "Wspaniały Ametyst", "x24% Cienia", "+18,0% generowania bariery", "+2 625 odporności na Cień");
        assertGem("amethyst_flawless_horadric", "Nieskazitelny Horadryjski Ametyst", "x32% Cienia", "+30,0% generowania bariery", "+4 375 odporności na Cień");
        assertGem("skull_grand", "Wspaniała Czaszka", "x24% Fizycznych", "+2,5% otrzymywanego leczenia", "+2 625 odporności na obrażenia Fizyczne");
        assertGem("skull_flawless_horadric", "Nieskazitelna Horadryjska Czaszka", "x32% Fizycznych", "+3,0% otrzymywanego leczenia", "+4 375 odporności na obrażenia Fizyczne");
        assertGem("diamond_grand", "Wspaniały Diament", "x20% wszystkich obrażeń", "+30 pkt. do wszystkich współczynników", "+375 odporności na wszystkie żywioły");
        assertGem("diamond_flawless_horadric", "Nieskazitelny Horadryjski Diament", "x28% wszystkich obrażeń", "+50 pkt. do wszystkich współczynników", "+625 odporności na wszystkie żywioły");
    }

    private static void assertGem(String id, String name, String weapon, String armor, String jewelry) {
        GemDefinition definition = gem(id);
        assertEquals(name, definition.getDisplayName());
        assertEquals(weapon, definition.getWeaponEffect().getDisplayText());
        assertEquals(armor, definition.getArmorEffect().getDisplayText());
        assertEquals(jewelry, definition.getJewelryEffect().getDisplayText());
    }

    private static GemDefinition gem(String id) {
        return GemCatalog.findById(id).orElseThrow();
    }
}
