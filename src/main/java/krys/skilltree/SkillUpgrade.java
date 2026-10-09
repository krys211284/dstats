package krys.skilltree;

import java.util.Objects;

/** Pojedyncze ulepszenie w grupie ulepszeń skilla postaci. */
public final class SkillUpgrade {
    private final String id;
    private final String name;
    private final SkillTreeStatus status;
    private final String sourceNote;

    public SkillUpgrade(String id, String name, SkillTreeStatus status, String sourceNote) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.status = Objects.requireNonNull(status, "status");
        this.sourceNote = requireText(sourceNote, "sourceNote");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SkillTreeStatus getStatus() {
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
