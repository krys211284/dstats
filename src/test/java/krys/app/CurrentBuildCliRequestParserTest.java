package krys.app;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentBuildCliRequestParserTest {

    @Test
    void powinien_mapowac_wejscie_cli_do_aplikacyjnego_requestu_m8() {
        CurrentBuildRequest request = CurrentBuildCliRequestParser.parse(new String[]{
                "--level", "25",
                "--weapon-damage", "12",
                "--strength", "30",
                "--intelligence", "5",
                "--thorns", "60",
                "--block-chance", "40",
                "--retribution-chance", "20",
                "--brandish-rank", "5",
                "--brandish-base-upgrade", "true",
                "--brandish-choice", "LEFT",
                "--advance-rank", "5",
                "--advance-base-upgrade", "true",
                "--advance-choice", "RIGHT",
                "--action-bar", "ADVANCE,BRANDISH",
                "--seconds", "15"
        });

        assertEquals(25, request.getLevel());
        assertEquals(12L, request.getWeaponDamage());
        assertEquals(30.0d, request.getStrength(), 0.0000001d);
        assertEquals(5.0d, request.getIntelligence(), 0.0000001d);
        assertEquals(60.0d, request.getThorns(), 0.0000001d);
        assertEquals(40.0d, request.getBlockChance(), 0.0000001d);
        assertEquals(20.0d, request.getRetributionChance(), 0.0000001d);
        assertEquals(15, request.getHorizonSeconds());
        assertEquals(2, request.getLearnedSkills().size());
        assertEquals(SkillUpgradeChoice.LEFT, request.getLearnedSkills().get(SkillId.BRANDISH).getChoiceUpgrade());
        assertEquals(SkillUpgradeChoice.RIGHT, request.getLearnedSkills().get(SkillId.ADVANCE).getChoiceUpgrade());
        assertEquals(SkillId.ADVANCE, request.getActionBar().get(0));
        assertEquals(SkillId.BRANDISH, request.getActionBar().get(1));
    }

    @Test
    void powinien_obslugiwac_preset_referencyjny_w_cli_jako_tryb_pomocniczy() {
        CurrentBuildRequest request = CurrentBuildCliRequestParser.parse(new String[]{
                "--reference", "HOLY_BOLT_JUDGEMENT"
        });

        assertEquals(13, request.getLevel());
        assertEquals(8L, request.getWeaponDamage());
        assertTrue(request.getLearnedSkills().containsKey(SkillId.HOLY_BOLT));
        assertEquals(List.of(SkillId.HOLY_BOLT), request.getActionBar());
    }
}
