package krys.skilltree;

/** Źródłowa nazwa zasobu i etykiety prezentacji, bez implementowania gospodarki zasobem. */
public record SkillResourceType(String id, String costLabel, String generationLabel, String unitLabel) {
    public static final SkillResourceType UNSPECIFIED = new SkillResourceType(
            "resource", "Koszt zasobu", "Generowanie zasobu", "pkt. zasobu");

    public SkillResourceType {
        if (id == null || id.isBlank() || costLabel == null || costLabel.isBlank()
                || generationLabel == null || generationLabel.isBlank() || unitLabel == null || unitLabel.isBlank()) {
            throw new IllegalArgumentException("Opis zasobu nie może zawierać pustych pól.");
        }
    }
}
