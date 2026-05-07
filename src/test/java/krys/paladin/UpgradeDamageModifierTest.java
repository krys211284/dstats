package krys.paladin;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeDamageModifierTest {
    @Test
    void enumy_powinny_zawierac_kontraktowe_typy_modyfikatorow_i_wartosci() {
        assertEquals(Set.of(
                UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT,
                UpgradeDamageModifierType.ADDITIVE_DAMAGE_PERCENT,
                UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT,
                UpgradeDamageModifierType.RANK_SCALING_COMPONENT_PERCENT,
                UpgradeDamageModifierType.ADDITIONAL_HIT_OR_STRIKE,
                UpgradeDamageModifierType.DAMAGE_OVER_TIME,
                UpgradeDamageModifierType.THORNS_DAMAGE_MODIFIER,
                UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER,
                UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN,
                UpgradeDamageModifierType.RESOURCE_OR_COST,
                UpgradeDamageModifierType.DEFENSE_OR_UTILITY,
                UpgradeDamageModifierType.NO_DAMAGE_IMPACT,
                UpgradeDamageModifierType.NEEDS_MANUAL_REVIEW
        ), Set.of(UpgradeDamageModifierType.values()));
        assertEquals(Set.of(
                UpgradeDamageValueKind.PERCENT_X,
                UpgradeDamageValueKind.PERCENT_PLUS,
                UpgradeDamageValueKind.COMPONENT_PERCENT,
                UpgradeDamageValueKind.HIT_COUNT,
                UpgradeDamageValueKind.SECONDS,
                UpgradeDamageValueKind.TEXT_ONLY,
                UpgradeDamageValueKind.NONE,
                UpgradeDamageValueKind.NEEDS_MANUAL_REVIEW
        ), Set.of(UpgradeDamageValueKind.values()));
    }

    @Test
    void wymach_powinien_miec_jednoznaczne_modyfikatory_z_lokalnego_markdowna() {
        assertModifier("zwiekszenie_obrazen", "Zwiększenie Obrażeń",
                UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT, "20%[X]", UpgradeDamageValueKind.PERCENT_X,
                false, true);
        assertModifier("krzyzowe_uderzenie", "Krzyżowe Uderzenie",
                UpgradeDamageModifierType.ADDITIONAL_HIT_OR_STRIKE, "2 dodatkowe łuki; 120%", UpgradeDamageValueKind.COMPONENT_PERCENT,
                true, true);
        assertModifier("powracajaca_swiatlosc", "Powracająca Światłość",
                UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT, "52%", UpgradeDamageValueKind.COMPONENT_PERCENT,
                true, true);

        UpgradeDamageModifier castSpeed = modifier("szybkosc_uzycia", "Szybkość Użycia");
        assertEquals(UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN, castSpeed.getType());
        assertEquals("20%[+]", castSpeed.getValue());
        assertFalse(castSpeed.isDirectDamageModifier());

        UpgradeDamageModifier vulnerable = modifier("odsloniecie", "Odsłonięcie");
        assertEquals(UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER, vulnerable.getType());
        assertEquals("20%", vulnerable.getValue());
        assertFalse(vulnerable.isDirectDamageModifier());

        UpgradeDamageModifier faith = modifier("generowanie_wiary", "Generowanie Wiary");
        assertEquals(UpgradeDamageModifierType.RESOURCE_OR_COST, faith.getType());
        assertEquals("5 Faith", faith.getValue());
        assertFalse(faith.isDirectDamageModifier());
    }

    @Test
    void modyfikator_nie_moze_odblokowac_runtime_dps() {
        assertThrows(IllegalArgumentException.class, () -> new UpgradeDamageModifier(
                "grupa_1",
                "Test",
                UpgradeDamageSafety.YES,
                UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT,
                "20%[X]",
                UpgradeDamageValueKind.PERCENT_X,
                "test",
                false,
                "PRIMARY_DAMAGE",
                false,
                UpgradeDamageSafety.YES,
                UpgradeDamageSafety.YES,
                "Nie wolno odblokować runtime DPS."
        ));
    }

    @Test
    void null_i_puste_pola_powinny_byc_odrzucane() {
        assertThrows(IllegalArgumentException.class, () -> new UpgradeDamageModifier(
                "",
                "Test",
                UpgradeDamageSafety.NO,
                UpgradeDamageModifierType.NO_DAMAGE_IMPACT,
                "brak",
                UpgradeDamageValueKind.NONE,
                "test",
                false,
                "none",
                false,
                UpgradeDamageSafety.YES,
                UpgradeDamageSafety.NO,
                "opis"
        ));
        assertThrows(NullPointerException.class, () -> new UpgradeDamageModifier(
                "grupa_1",
                "Test",
                null,
                UpgradeDamageModifierType.NO_DAMAGE_IMPACT,
                "brak",
                UpgradeDamageValueKind.NONE,
                "test",
                false,
                "none",
                false,
                UpgradeDamageSafety.YES,
                UpgradeDamageSafety.NO,
                "opis"
        ));
    }

    private static void assertModifier(String upgradeId,
                                       String upgradeName,
                                       UpgradeDamageModifierType expectedType,
                                       String expectedValue,
                                       UpgradeDamageValueKind expectedValueKind,
                                       boolean expectedCreatesNewDamageComponent,
                                       boolean expectedDirectDamageModifier) {
        UpgradeDamageModifier modifier = modifier(upgradeId, upgradeName);

        assertEquals(UpgradeDamageSafety.YES, modifier.getSafeForRankingDisplay());
        assertEquals(UpgradeDamageSafety.NO, modifier.getSafeForRuntimeDps());
        assertEquals(expectedType, modifier.getType());
        assertEquals(expectedValue, modifier.getValue());
        assertEquals(expectedValueKind, modifier.getValueKind());
        assertEquals(expectedCreatesNewDamageComponent, modifier.createsNewDamageComponent());
        assertEquals(expectedDirectDamageModifier, modifier.isDirectDamageModifier());
    }

    private static UpgradeDamageModifier modifier(String upgradeId, String upgradeName) {
        return UpgradeDamageModifier.fromUpgrade("wymach", "grupa_1",
                new PaladinSkillUpgrade(upgradeId, upgradeName, PaladinSkillTreeStatus.NEEDS_VERIFICATION, "test"));
    }
}
