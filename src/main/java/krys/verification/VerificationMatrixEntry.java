package krys.verification;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Pojedynczy wpis macierzy weryfikacji mechanik Paladyna. */
public final class VerificationMatrixEntry {
    private final String stableId;
    private final String skillId;
    private final String skillGroup;
    private final String modifierId;
    private final String sourcePdf;
    private final String sourceNote;
    private final VerificationCategory category;
    private final String question;
    private final VerificationStatus currentStatus;
    private final Set<VerificationImpact> impacts;
    private final VerificationDefaultEngineBehavior defaultEngineBehavior;

    public VerificationMatrixEntry(String stableId,
                                   String skillId,
                                   String skillGroup,
                                   String modifierId,
                                   String sourcePdf,
                                   String sourceNote,
                                   VerificationCategory category,
                                   String question,
                                   VerificationStatus currentStatus,
                                   Set<VerificationImpact> impacts,
                                   VerificationDefaultEngineBehavior defaultEngineBehavior) {
        this.stableId = requireText(stableId, "stableId");
        this.skillId = requireText(skillId, "skillId");
        this.skillGroup = requireText(skillGroup, "skillGroup");
        this.modifierId = normalizeOptionalText(modifierId);
        this.sourcePdf = requireText(sourcePdf, "sourcePdf");
        this.sourceNote = requireText(sourceNote, "sourceNote");
        this.category = Objects.requireNonNull(category, "category");
        this.question = requireText(question, "question");
        this.currentStatus = Objects.requireNonNull(currentStatus, "currentStatus");
        this.impacts = copyImpacts(impacts);
        this.defaultEngineBehavior = Objects.requireNonNull(defaultEngineBehavior, "defaultEngineBehavior");
    }

    public String getStableId() {
        return stableId;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getSkillGroup() {
        return skillGroup;
    }

    public String getModifierId() {
        return modifierId;
    }

    public String getSourcePdf() {
        return sourcePdf;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    public VerificationCategory getCategory() {
        return category;
    }

    public String getQuestion() {
        return question;
    }

    public VerificationStatus getCurrentStatus() {
        return currentStatus;
    }

    public Set<VerificationImpact> getImpacts() {
        return impacts;
    }

    public VerificationDefaultEngineBehavior getDefaultEngineBehavior() {
        return defaultEngineBehavior;
    }

    public boolean hasImpact(VerificationImpact impact) {
        return impacts.contains(impact);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static Set<VerificationImpact> copyImpacts(Set<VerificationImpact> impacts) {
        Objects.requireNonNull(impacts, "impacts");
        if (impacts.isEmpty()) {
            throw new IllegalArgumentException("Wpis macierzy musi mieć co najmniej jeden impact.");
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(impacts));
    }
}
