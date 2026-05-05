package krys.verification;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static krys.verification.VerificationCategory.AURA_PASSIVE_VS_ACTIVE_BEHAVIOR;
import static krys.verification.VerificationCategory.COOLDOWN_REDUCTION_TIMING;
import static krys.verification.VerificationCategory.DELAYED_EXPLOSION_BEHAVIOR;
import static krys.verification.VerificationCategory.DOT_TICK_RATE;
import static krys.verification.VerificationCategory.EXTRA_PROJECTILE_HIT_BEHAVIOR;
import static krys.verification.VerificationCategory.REPLACEMENT_VS_ADDITIONAL_DAMAGE_COMPONENT;
import static krys.verification.VerificationCategory.RICOCHET_OR_BOUNCE_BEHAVIOR;
import static krys.verification.VerificationCategory.SINGLE_TARGET_HIT_COUNT;
import static krys.verification.VerificationCategory.STATUS_APPLICATION_ORDER;
import static krys.verification.VerificationCategory.DURATION_OR_REFRESH_BEHAVIOR;
import static krys.verification.VerificationDefaultEngineBehavior.BLOCKED;
import static krys.verification.VerificationDefaultEngineBehavior.IGNORED;
import static krys.verification.VerificationDefaultEngineBehavior.METADATA_ONLY;
import static krys.verification.VerificationImpact.COOLDOWN;
import static krys.verification.VerificationImpact.DPS;
import static krys.verification.VerificationImpact.POSITIONING;
import static krys.verification.VerificationImpact.STATUS;
import static krys.verification.VerificationImpact.SURVIVABILITY;
import static krys.verification.VerificationStatus.REQUIRES_VERIFICATION;

/** Statyczna macierz mechanik Paladyna wymagających weryfikacji przed runtime DPS. */
public final class VerificationMatrix {
    private static final String BASIC_PDF = "docs/paladin/source-pdfs/paladin_basic_skill_registry_final.pdf";
    private static final String CORE_PDF = "docs/paladin/source-pdfs/paladin_core_skill_registry_final.pdf";
    private static final String AURA_PDF = "docs/paladin/source-pdfs/paladin_aura_skill_registry_final.pdf";
    private static final String COURAGE_PDF = "docs/paladin/source-pdfs/diablo4_paladyn_odwaga_umiejetnosci.pdf";
    private static final String JUSTICE_PDF = "docs/paladin/source-pdfs/diablo4_paladyn_sprawiedliwosc_umiejetnosci.pdf";
    private static final String SPECIAL_POWERS_PDF = "docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf";

    private static final String SEED_NOTE = "Seed macierzy z tooltipów oznaczonych DO WERYFIKACJI / DO_WERYFIKACJI; przed zmianą statusu wymagany test empiryczny.";

    private static final List<VerificationMatrixEntry> ENTRIES = List.of(
            entry(
                    "paladin-basic-ricochet-bounce-extra-projectile-single-target",
                    "basic_registry",
                    "basic",
                    null,
                    BASIC_PDF,
                    RICOCHET_OR_BOUNCE_BEHAVIOR,
                    "Czy rykoszety, odbicia, przeskoki albo dodatkowe pociski z Basic mogą trafić ten sam cel w modelu single target?",
                    impacts(DPS, POSITIONING),
                    IGNORED
            ),
            entry(
                    "paladin-core-ricochet-bounce-extra-projectile-single-target",
                    "core_registry",
                    "core",
                    null,
                    CORE_PDF,
                    EXTRA_PROJECTILE_HIT_BEHAVIOR,
                    "Czy rykoszety, odbicia, przeskoki albo dodatkowe pociski z Core mogą trafić ten sam cel w modelu single target?",
                    impacts(DPS, POSITIONING),
                    IGNORED
            ),
            entry(
                    "paladin-aura-ricochet-bounce-extra-projectile-single-target",
                    "aura_registry",
                    "aura",
                    null,
                    AURA_PDF,
                    RICOCHET_OR_BOUNCE_BEHAVIOR,
                    "Czy rykoszety, odbicia, przeskoki albo dodatkowe pociski z Aur mogą trafić ten sam cel w modelu single target?",
                    impacts(DPS, POSITIONING),
                    IGNORED
            ),
            entry(
                    "shield-charge-tick-rate",
                    "szarza_z_tarcza",
                    "odwaga",
                    null,
                    COURAGE_PDF,
                    DOT_TICK_RATE,
                    "Jaki jest tick rate Szarży z Tarczą i które ticki mogą liczyć się przeciwko pojedynczemu celowi?",
                    impacts(DPS, POSITIONING),
                    BLOCKED
            ),
            entry(
                    "falling-star-start-landing-same-target",
                    "spadajaca_gwiazda",
                    "odwaga",
                    null,
                    COURAGE_PDF,
                    SINGLE_TARGET_HIT_COUNT,
                    "Czy start i lądowanie Spadającej Gwiazdy mogą trafić ten sam cel?",
                    impacts(DPS, POSITIONING),
                    BLOCKED
            ),
            entry(
                    "spear-of-heavens-single-target-spear-and-explosion-count",
                    "wlocznia_niebios",
                    "sprawiedliwosc",
                    null,
                    JUSTICE_PDF,
                    DELAYED_EXPLOSION_BEHAVIOR,
                    "Ile włóczni i wybuchów Włóczni Niebios może trafić pojedynczy cel?",
                    impacts(DPS, POSITIONING),
                    BLOCKED
            ),
            entry(
                    "consecration-tick-rate",
                    "konsekracja",
                    "aura",
                    null,
                    AURA_PDF,
                    DOT_TICK_RATE,
                    "Jaki jest tick rate Konsekracji i czy ticki są zależne od pozycji celu?",
                    impacts(DPS, POSITIONING),
                    BLOCKED
            ),
            entry(
                    "consecration-buff-duration-refresh",
                    "konsekracja",
                    "aura",
                    "buff",
                    AURA_PDF,
                    DURATION_OR_REFRESH_BEHAVIOR,
                    "Jak działają buffy Konsekracji: czas trwania, odświeżenie i warunek aktywności?",
                    impacts(DPS, STATUS, SURVIVABILITY),
                    METADATA_ONLY
            ),
            entry(
                    "purification-echo-hit-behavior",
                    "echo_oczyszczenia",
                    "aura",
                    null,
                    AURA_PDF,
                    STATUS_APPLICATION_ORDER,
                    "Czy Echo Oczyszczenia jest osobnym trafieniem, efektem statusu czy wyłącznie metadanym tooltipa?",
                    impacts(DPS, STATUS),
                    METADATA_ONLY
            ),
            entry(
                    "heavens-fury-ray-hit-frequency",
                    "furia_niebios",
                    "aura",
                    null,
                    AURA_PDF,
                    EXTRA_PROJECTILE_HIT_BEHAVIOR,
                    "Jak działają promienie Furii Niebios i jaka jest ich częstotliwość trafień w pojedynczy cel?",
                    impacts(DPS, POSITIONING),
                    BLOCKED
            ),
            entry(
                    "thorn-fortress-redoubt-dps-behavior",
                    "cierniowa_reduta_fortecy",
                    "moce_specjalne",
                    null,
                    SPECIAL_POWERS_PDF,
                    AURA_PASSIVE_VS_ACTIVE_BEHAVIOR,
                    "Czy Cierniowa Reduta Fortecy działa pasywnie, aktywnie, defensywnie albo jako wkład w reactive DPS?",
                    impacts(DPS, SURVIVABILITY, STATUS),
                    METADATA_ONLY
            ),
            entry(
                    "zenith-first-second-use-behavior",
                    "zenit",
                    "moce_specjalne",
                    null,
                    SPECIAL_POWERS_PDF,
                    COOLDOWN_REDUCTION_TIMING,
                    "Jak rozróżnić pierwsze i drugie użycie Zenitu oraz kiedy efekt może zmienić cooldown albo sekwencję castów?",
                    impacts(DPS, COOLDOWN, POSITIONING),
                    BLOCKED
            ),
            entry(
                    "arbiter-of-justice-wing-strikes",
                    "arbiter_sprawiedliwosci",
                    "sprawiedliwosc",
                    "uderzenia_skrzydel",
                    JUSTICE_PDF,
                    REPLACEMENT_VS_ADDITIONAL_DAMAGE_COMPONENT,
                    "Czy uderzenia skrzydeł Arbitra Sprawiedliwości są dodatkowymi komponentami obrażeń, zamiennikiem trafienia czy efektem pozycyjnym?",
                    impacts(DPS, POSITIONING),
                    BLOCKED
            )
    );

    static {
        validateUniqueStableIds(ENTRIES);
    }

    private VerificationMatrix() {
    }

    public static List<VerificationMatrixEntry> all() {
        return ENTRIES;
    }

    public static Optional<VerificationMatrixEntry> findByStableId(String stableId) {
        return ENTRIES.stream()
                .filter(entry -> entry.getStableId().equals(stableId))
                .findFirst();
    }

    public static VerificationMatrixEntry requireByStableId(String stableId) {
        return findByStableId(stableId)
                .orElseThrow(() -> new IllegalArgumentException("Nieznany wpis Verification Matrix: " + stableId));
    }

    public static void validate() {
        validateUniqueStableIds(ENTRIES);
        for (VerificationMatrixEntry entry : ENTRIES) {
            if (entry.getCurrentStatus() == REQUIRES_VERIFICATION && entry.getDefaultEngineBehavior() == null) {
                throw new IllegalStateException("Wpis requiresVerification musi mieć default engine behavior: " + entry.getStableId());
            }
        }
    }

    private static VerificationMatrixEntry entry(String stableId,
                                                 String skillId,
                                                 String skillGroup,
                                                 String modifierId,
                                                 String sourcePdf,
                                                 VerificationCategory category,
                                                 String question,
                                                 Set<VerificationImpact> impacts,
                                                 VerificationDefaultEngineBehavior defaultEngineBehavior) {
        return new VerificationMatrixEntry(
                stableId,
                skillId,
                skillGroup,
                modifierId,
                sourcePdf,
                SEED_NOTE,
                category,
                question,
                REQUIRES_VERIFICATION,
                impacts,
                defaultEngineBehavior
        );
    }

    private static Set<VerificationImpact> impacts(VerificationImpact first, VerificationImpact... rest) {
        EnumSet<VerificationImpact> result = EnumSet.of(first);
        result.addAll(List.of(rest));
        return result;
    }

    private static void validateUniqueStableIds(List<VerificationMatrixEntry> entries) {
        Set<String> ids = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (VerificationMatrixEntry entry : entries) {
            if (!ids.add(entry.getStableId())) {
                duplicates.add(entry.getStableId());
            }
        }
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException("Zduplikowane stable id Verification Matrix: " + duplicates);
        }
    }
}
