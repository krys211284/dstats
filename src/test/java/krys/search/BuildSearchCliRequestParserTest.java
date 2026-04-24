package krys.search;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildSearchCliRequestParserTest {

    @Test
    void powinien_mapowac_wejscie_cli_do_requestu_searcha_m9() {
        BuildSearchRequest request = BuildSearchCliRequestParser.parse(new String[]{
                "--use-item-library",
                "--level-values", "13,15",
                "--weapon-damage-values", "8,12",
                "--strength-values", "18,30",
                "--intelligence-values", "0,5",
                "--thorns-values", "0,50",
                "--block-chance-values", "0,50",
                "--retribution-chance-values", "0,50",
                "--brandish-ranks", "0,5",
                "--brandish-base-upgrades", "false,true",
                "--brandish-choices", "NONE,LEFT,RIGHT",
                "--bar-sizes", "1,2",
                "--seconds", "15",
                "--top", "7"
        });

        assertEquals(List.of(13, 15), request.getLevelValues());
        assertEquals(List.of(8L, 12L), request.getWeaponDamageValues());
        assertEquals(List.of(18.0d, 30.0d), request.getStrengthValues());
        assertTrue(request.isUseItemLibrary());
        assertEquals(List.of(1, 2), request.getActionBarSizes());
        assertEquals(15, request.getHorizonSeconds());
        assertEquals(7, request.getTopResultsLimit());
        assertEquals(List.of(0, 5), request.getSkillSpace(SkillId.BRANDISH).getRankValues());
        assertEquals(List.of(Boolean.FALSE, Boolean.TRUE), request.getSkillSpace(SkillId.BRANDISH).getBaseUpgradeValues());
        assertEquals(List.of(SkillUpgradeChoice.NONE, SkillUpgradeChoice.LEFT, SkillUpgradeChoice.RIGHT),
                request.getSkillSpace(SkillId.BRANDISH).getChoiceUpgradeValues());
    }

    @Test
    void powinien_obslugiwac_preset_referencyjny_dla_searcha() {
        BuildSearchRequest request = BuildSearchCliRequestParser.parse(new String[]{
                "--reference", "FOUNDATION_M9"
        });

        assertEquals(9, request.getHorizonSeconds());
        assertEquals(5, request.getTopResultsLimit());
        assertTrue(request.getSkillSpaces().containsKey(SkillId.CLASH));
        assertEquals(List.of(SkillUpgradeChoice.NONE, SkillUpgradeChoice.LEFT),
                request.getSkillSpace(SkillId.CLASH).getChoiceUpgradeValues());
    }
}
