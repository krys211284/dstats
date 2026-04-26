package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.hero.HeroClass;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Znany aspekt, który może zostać wybrany na zatwierdzanym itemie. */
public final class AspectDefinition {
    private final String id;
    private final String displayName;
    private final Set<EquipmentSlot> allowedItemSlots;
    private final Set<HeroClass> heroClasses;
    private final List<String> tags;

    public AspectDefinition(String id,
                            String displayName,
                            Set<EquipmentSlot> allowedItemSlots,
                            Set<HeroClass> heroClasses,
                            List<String> tags) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id aspektu jest wymagane.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Nazwa aspektu jest wymagana.");
        }
        if (allowedItemSlots == null || allowedItemSlots.isEmpty()) {
            throw new IllegalArgumentException("Aspekt musi mieć co najmniej jeden dozwolony slot.");
        }
        this.id = id;
        this.displayName = displayName;
        this.allowedItemSlots = EnumSet.copyOf(allowedItemSlots);
        this.heroClasses = heroClasses == null || heroClasses.isEmpty()
                ? EnumSet.noneOf(HeroClass.class)
                : EnumSet.copyOf(heroClasses);
        this.tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<EquipmentSlot> getAllowedItemSlots() {
        return Set.copyOf(allowedItemSlots);
    }

    public Set<HeroClass> getHeroClasses() {
        return Set.copyOf(heroClasses);
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean allowsSlot(EquipmentSlot slot) {
        return slot != null && allowedItemSlots.contains(slot);
    }
}
