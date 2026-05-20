package krys.paladin;

import java.util.List;

/** Opisowa definicja Przysięgi Paladyna jako mechaniki klasowej poza drzewem umiejętności. */
public final class PaladinOathDefinition {
    private final PaladinOathId id;
    private final String displayName;
    private final String subtitle;
    private final List<String> descriptionLines;
    private final List<String> secondaryDescriptionLines;
    private final PaladinOathFamily affectedSkillFamily;
    private final PaladinOathRuntimeStatus runtimeStatus;
    private final String sourceReference;

    public PaladinOathDefinition(PaladinOathId id,
                                 String displayName,
                                 String subtitle,
                                 List<String> descriptionLines,
                                 List<String> secondaryDescriptionLines,
                                 PaladinOathFamily affectedSkillFamily,
                                 PaladinOathRuntimeStatus runtimeStatus,
                                 String sourceReference) {
        this.id = id;
        this.displayName = displayName;
        this.subtitle = subtitle;
        this.descriptionLines = List.copyOf(descriptionLines);
        this.secondaryDescriptionLines = List.copyOf(secondaryDescriptionLines);
        this.affectedSkillFamily = affectedSkillFamily;
        this.runtimeStatus = runtimeStatus;
        this.sourceReference = sourceReference;
    }

    public PaladinOathId getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public List<String> getDescriptionLines() {
        return descriptionLines;
    }

    public List<String> getSecondaryDescriptionLines() {
        return secondaryDescriptionLines;
    }

    public PaladinOathFamily getAffectedSkillFamily() {
        return affectedSkillFamily;
    }

    public PaladinOathRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public String getSourceReference() {
        return sourceReference;
    }
}
