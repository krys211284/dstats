package krys.verification;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificationMatrixTest {
    private static final String AURA_PDF = "docs/paladin/source-pdfs/paladin_aura_skill_registry_final.pdf";
    private static final String JUSTICE_PDF = "docs/paladin/source-pdfs/diablo4_paladyn_sprawiedliwosc_umiejetnosci.pdf";
    private static final String SPECIAL_POWERS_PDF = "docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf";

    @Test
    void powinna_zawierac_wszystkie_kategorie_weryfikacji_z_kontraktu() {
        Set<String> categoryIds = Arrays.stream(VerificationCategory.values())
                .map(VerificationCategory::getId)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "singleTargetHitCount",
                "extraProjectileHitBehavior",
                "ricochetOrBounceBehavior",
                "delayedExplosionBehavior",
                "dotTickRate",
                "statusApplicationOrder",
                "cooldownReductionTiming",
                "resourceCostOrGeneration",
                "durationOrRefreshBehavior",
                "replacementVsAdditionalDamageComponent",
                "bossControlBehavior",
                "auraPassiveVsActiveBehavior"
        ), categoryIds);
    }

    @Test
    void powinna_miec_spojne_wpisy_poczatkowe() {
        assertDoesNotThrow(VerificationMatrix::validate);
        assertFalse(VerificationMatrix.all().isEmpty());

        Set<String> stableIds = new HashSet<>();
        for (VerificationMatrixEntry entry : VerificationMatrix.all()) {
            assertTrue(stableIds.add(entry.getStableId()), "Duplikat stable id: " + entry.getStableId());
            assertFalse(entry.getSkillId().isBlank());
            assertFalse(entry.getSkillGroup().isBlank());
            assertTrue(entry.getSourcePdf().startsWith("docs/paladin/source-pdfs/"));
            assertFalse(entry.getSourceNote().isBlank());
            assertNotNull(entry.getCategory());
            assertFalse(entry.getQuestion().isBlank());
            assertEquals(VerificationStatus.REQUIRES_VERIFICATION, entry.getCurrentStatus());
            assertFalse(entry.getImpacts().isEmpty());
            assertNotNull(entry.getDefaultEngineBehavior());
        }
    }

    @Test
    void powinna_zawierac_najwazniejsze_mechaniki_z_seedu_pdf() {
        Set<String> stableIds = VerificationMatrix.all().stream()
                .map(VerificationMatrixEntry::getStableId)
                .collect(Collectors.toSet());

        assertTrue(stableIds.contains("paladin-basic-ricochet-bounce-extra-projectile-single-target"));
        assertTrue(stableIds.contains("paladin-core-ricochet-bounce-extra-projectile-single-target"));
        assertTrue(stableIds.contains("paladin-aura-ricochet-bounce-extra-projectile-single-target"));
        assertTrue(stableIds.contains("shield-charge-tick-rate"));
        assertTrue(stableIds.contains("falling-star-start-landing-same-target"));
        assertTrue(stableIds.contains("spear-of-heavens-single-target-spear-and-explosion-count"));
        assertTrue(stableIds.contains("consecration-tick-rate"));
        assertTrue(stableIds.contains("consecration-buff-duration-refresh"));
        assertTrue(stableIds.contains("purification-echo-hit-behavior"));
        assertTrue(stableIds.contains("heavens-fury-ray-hit-frequency"));
        assertTrue(stableIds.contains("thorn-fortress-redoubt-dps-behavior"));
        assertTrue(stableIds.contains("zenith-first-second-use-behavior"));
        assertTrue(stableIds.contains("arbiter-of-justice-wing-strikes"));
    }

    @Test
    void powinna_miec_poprawne_zrodla_i_grupy_dla_poprawianych_wpisow() {
        assertSourceAndGroup("consecration-tick-rate", JUSTICE_PDF, "sprawiedliwosc");
        assertSourceAndGroup("consecration-buff-duration-refresh", JUSTICE_PDF, "sprawiedliwosc");
        assertSourceAndGroup("purification-echo-hit-behavior", JUSTICE_PDF, "sprawiedliwosc");
        assertEquals("oczyszczenie", VerificationMatrix.requireByStableId("purification-echo-hit-behavior").getSkillId());
        assertSourceAndGroup("heavens-fury-ray-hit-frequency", SPECIAL_POWERS_PDF, "moce_specjalne");
        assertSourceAndGroup("arbiter-of-justice-wing-strikes", SPECIAL_POWERS_PDF, "moce_specjalne");
    }

    @Test
    void konsekracja_i_oczyszczenie_nie_powinny_wskazywac_na_pdf_aur() {
        assertNotSourcePdf("consecration-tick-rate", AURA_PDF);
        assertNotSourcePdf("consecration-buff-duration-refresh", AURA_PDF);
        assertNotSourcePdf("purification-echo-hit-behavior", AURA_PDF);
    }

    @Test
    void moce_specjalne_powinny_wskazywac_na_pdf_mocy_specjalnych_jesli_sa_w_matrix() {
        assertSpecialPowersSourceIfPresent("heavens-fury-ray-hit-frequency");
        assertSpecialPowersSourceIfPresent("thorn-fortress-redoubt-dps-behavior");
        assertSpecialPowersSourceIfPresent("zenith-first-second-use-behavior");
        assertSpecialPowersSourceIfPresent("arbiter-of-justice-wing-strikes");
    }

    private static void assertSourceAndGroup(String stableId, String sourcePdf, String skillGroup) {
        VerificationMatrixEntry entry = VerificationMatrix.requireByStableId(stableId);

        assertEquals(sourcePdf, entry.getSourcePdf());
        assertEquals(skillGroup, entry.getSkillGroup());
    }

    private static void assertNotSourcePdf(String stableId, String forbiddenSourcePdf) {
        VerificationMatrixEntry entry = VerificationMatrix.requireByStableId(stableId);

        assertFalse(entry.getSourcePdf().equals(forbiddenSourcePdf));
    }

    private static void assertSpecialPowersSourceIfPresent(String stableId) {
        VerificationMatrix.findByStableId(stableId)
                .ifPresent(entry -> assertEquals(SPECIAL_POWERS_PDF, entry.getSourcePdf()));
    }
}
