package krys.paladin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static krys.paladin.PaladinSkillTreeStatus.NEEDS_VERIFICATION;
import static krys.paladin.PaladinSkillTreeStatus.UNSUPPORTED;
import static krys.paladin.PaladinSkillTreeType.DAMAGE;
import static krys.paladin.PaladinSkillTreeType.DEFENSIVE;
import static krys.paladin.PaladinSkillTreeType.SPECIAL;
import static krys.paladin.PaladinSkillTreeType.SUPPORT;
import static krys.paladin.PaladinSkillTreeType.UNCLASSIFIED;

/** Pełny aplikacyjny rejestr drzewa Paladyna oparty o PDF-y źródłowe. */
public final class PaladinSkillTreeRegistry {
    public static final String JUSTICE_PDF = "docs/paladin/source-pdfs/diablo4_paladyn_sprawiedliwosc_umiejetnosci.pdf";
    public static final String SPECIAL_POWERS_PDF = "docs/paladin/source-pdfs/moce_specjalne_diablo4.pdf";

    private static final String NO_SAFE_DPS_MODEL = "Skill istnieje w drzewie z PDF, ale nie ma jeszcze bezpiecznego modelu DPS w DamageEngine.";
    private static final Map<String, PaladinTreeSkill> SKILLS_BY_ID = createSkills();

    private PaladinSkillTreeRegistry() {
    }

    public static List<PaladinTreeSkill> allSkills() {
        return List.copyOf(SKILLS_BY_ID.values());
    }

    public static Optional<PaladinTreeSkill> findSkill(String skillId) {
        return Optional.ofNullable(SKILLS_BY_ID.get(skillId));
    }

    public static PaladinTreeSkill requireSkill(String skillId) {
        return findSkill(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Nieznany skill Paladyna w rejestrze PDF: " + skillId));
    }

    private static Map<String, PaladinTreeSkill> createSkills() {
        LinkedHashMap<String, PaladinTreeSkill> skills = new LinkedHashMap<>();
        put(skills, skill("skazanie", "Skazanie", JUSTICE_PDF, "sprawiedliwosc", UNCLASSIFIED, UNSUPPORTED, List.of(), NO_SAFE_DPS_MODEL));
        put(skills, skill("wlocznia_niebios", "Włócznia Niebios", JUSTICE_PDF, "sprawiedliwosc", DAMAGE, NEEDS_VERIFICATION, List.of(), "Liczba włóczni i wybuchów dla single target wymaga weryfikacji."));
        put(skills, skill("konsekracja", "Konsekracja", JUSTICE_PDF, "sprawiedliwosc", DAMAGE, NEEDS_VERIFICATION, List.of(), "Tick rate, czas działania buffów i odświeżanie wymagają weryfikacji."));
        put(skills, skill("oczyszczenie", "Oczyszczenie", JUSTICE_PDF, "sprawiedliwosc", SUPPORT, NEEDS_VERIFICATION, List.of(), "Echo Oczyszczenia wymaga weryfikacji przed wpływem na DPS albo statusy."));
        put(skills, skill("furia_niebios", "Furia Niebios", SPECIAL_POWERS_PDF, "moce_specjalne", DAMAGE, NEEDS_VERIFICATION, List.of(), "Promienie i częstotliwość trafień wymagają weryfikacji."));
        put(skills, skill("cierniowa_reduta_fortecy", "Cierniowa Reduta Fortecy", SPECIAL_POWERS_PDF, "moce_specjalne", DEFENSIVE, NEEDS_VERIFICATION, List.of(), "Wpływ na reactive DPS albo wyłącznie defensywę wymaga weryfikacji."));
        put(skills, skill("zenit", "Zenit", SPECIAL_POWERS_PDF, "moce_specjalne", SPECIAL, NEEDS_VERIFICATION, zenithUpgradeGroups(), "Pierwsze i drugie użycie oraz skracanie cooldownu wymagają weryfikacji."));
        put(skills, skill("arbiter_sprawiedliwosci", "Arbiter Sprawiedliwości", SPECIAL_POWERS_PDF, "moce_specjalne", DAMAGE, NEEDS_VERIFICATION, List.of(), "Uderzenia skrzydeł wymagają weryfikacji jako komponenty obrażeń albo efekt pozycyjny."));
        return Map.copyOf(skills);
    }

    private static PaladinTreeSkill skill(String skillId,
                                          String skillName,
                                          String sourcePdf,
                                          String skillGroup,
                                          PaladinSkillTreeType type,
                                          PaladinSkillTreeStatus status,
                                          List<PaladinSkillUpgradeGroup> upgradeGroups,
                                          String notes) {
        return new PaladinTreeSkill(skillId, skillName, sourcePdf, skillGroup, type, status, upgradeGroups, notes);
    }

    private static void put(Map<String, PaladinTreeSkill> skills, PaladinTreeSkill skill) {
        PaladinTreeSkill previous = skills.put(skill.getSkillId(), skill);
        if (previous != null) {
            throw new IllegalStateException("Zduplikowany skill Paladyna: " + skill.getSkillId());
        }
    }

    private static List<PaladinSkillUpgradeGroup> zenithUpgradeGroups() {
        return List.of(
                new PaladinSkillUpgradeGroup("grupa_1", "Grupa 1", List.of(
                        upgrade("szansa_na_trafienie_krytyczne", "Szansa na Trafienie Krytyczne"),
                        upgrade("oslabienie", "Osłabienie")
                )),
                new PaladinSkillUpgradeGroup("grupa_2", "Grupa 2", List.of(
                        upgrade("nieustepliwosc", "Nieustępliwość"),
                        upgrade("oslabienie_cooldown", "Osłabienie: zabijanie osłabionych wrogów podczas działania Zenitu skraca jego czas odnowienia o 2 sek.")
                )),
                new PaladinSkillUpgradeGroup("grupa_3", "Grupa 3", List.of(
                        upgrade("empirejska_klinga", "Empirejska Klinga"),
                        upgrade("rozdarcie", "Rozdarcie"),
                        upgrade("homilia_stali", "Homilia Stali")
                ))
        );
    }

    private static PaladinSkillUpgrade upgrade(String id, String name) {
        return new PaladinSkillUpgrade(id, name, NEEDS_VERIFICATION, "Układ grup Zenitu z poprawionego PDF Mocy Specjalnych.");
    }
}
