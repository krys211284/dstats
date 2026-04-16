package krys.web;

import krys.app.CurrentBuildRequest;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentBuildFormMapperTest {
    private final CurrentBuildFormMapper formMapper = new CurrentBuildFormMapper();

    @Test
    void powinien_mapowac_formularz_gui_do_requestu_aplikacyjnego_m8() {
        Map<String, String> fields = new HashMap<>();
        fields.put("level", "25");
        fields.put("weaponDamage", "12");
        fields.put("strength", "30");
        fields.put("intelligence", "5");
        fields.put("thorns", "60");
        fields.put("blockChance", "40");
        fields.put("retributionChance", "20");
        fields.put("horizonSeconds", "15");
        fields.put(CurrentBuildFormData.rankFieldName(SkillId.BRANDISH), "5");
        fields.put(CurrentBuildFormData.choiceFieldName(SkillId.BRANDISH), SkillUpgradeChoice.LEFT.name());
        fields.put(CurrentBuildFormData.baseUpgradeFieldName(SkillId.BRANDISH), "true");
        fields.put(CurrentBuildFormData.rankFieldName(SkillId.ADVANCE), "5");
        fields.put(CurrentBuildFormData.choiceFieldName(SkillId.ADVANCE), SkillUpgradeChoice.RIGHT.name());
        fields.put(CurrentBuildFormData.baseUpgradeFieldName(SkillId.ADVANCE), "true");
        fields.put(CurrentBuildFormData.rankFieldName(SkillId.HOLY_BOLT), "0");
        fields.put(CurrentBuildFormData.choiceFieldName(SkillId.HOLY_BOLT), SkillUpgradeChoice.NONE.name());
        fields.put(CurrentBuildFormData.rankFieldName(SkillId.CLASH), "0");
        fields.put(CurrentBuildFormData.choiceFieldName(SkillId.CLASH), SkillUpgradeChoice.NONE.name());
        fields.put(CurrentBuildFormData.actionBarFieldName(1), SkillId.ADVANCE.name());
        fields.put(CurrentBuildFormData.actionBarFieldName(2), SkillId.BRANDISH.name());
        fields.put(CurrentBuildFormData.actionBarFieldName(3), "NONE");
        fields.put(CurrentBuildFormData.actionBarFieldName(4), "NONE");

        CurrentBuildFormMapper.MappingResult mappingResult = formMapper.map(CurrentBuildFormData.fromFormFields(fields));
        CurrentBuildRequest request = mappingResult.getRequest();

        assertTrue(mappingResult.getErrors().isEmpty());
        assertEquals(25, request.getLevel());
        assertEquals(12L, request.getWeaponDamage());
        assertEquals(30.0d, request.getStrength(), 0.0000001d);
        assertEquals(5.0d, request.getIntelligence(), 0.0000001d);
        assertEquals(60.0d, request.getThorns(), 0.0000001d);
        assertEquals(40.0d, request.getBlockChance(), 0.0000001d);
        assertEquals(20.0d, request.getRetributionChance(), 0.0000001d);
        assertEquals(2, request.getLearnedSkills().size());
        assertEquals(SkillUpgradeChoice.LEFT, request.getLearnedSkills().get(SkillId.BRANDISH).getChoiceUpgrade());
        assertEquals(SkillUpgradeChoice.RIGHT, request.getLearnedSkills().get(SkillId.ADVANCE).getChoiceUpgrade());
        assertEquals(SkillId.ADVANCE, request.getActionBar().get(0));
        assertEquals(SkillId.BRANDISH, request.getActionBar().get(1));
    }
}
