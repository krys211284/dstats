package krys.paladin;

/** Pojedyncze ulepszenie w grupie ulepszeń skilla Paladyna. */
public final class PaladinSkillUpgrade {
    private final String id;
    private final String name;
    private final PaladinSkillTreeStatus status;
    private final String sourceNote;

    public PaladinSkillUpgrade(String id, String name, PaladinSkillTreeStatus status, String sourceNote) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.status = status;
        this.sourceNote = requireText(sourceNote, "sourceNote");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PaladinSkillTreeStatus getStatus() {
        return status;
    }

    public String getSourceNote() {
        return sourceNote;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}
