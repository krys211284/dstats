package krys.skilltree;

/** Kategoria ze źródeł klasy; nowe kategorie nie wymagają rozszerzania wspólnego enumu. */
public record SourceSkillCategory(String id, String displayName, int displayOrder) {
    public SourceSkillCategory {
        if (id == null || id.isBlank() || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Id i nazwa kategorii nie mogą być puste.");
        }
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getDisplayOrder() { return displayOrder; }
}
