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
    private final String effectDescription;
    private final AspectType aspectType;
    private final AspectRuntimeStatus runtimeStatus;
    private final Set<EquipmentSlot> allowedItemSlots;
    private final Set<HeroClass> heroClasses;
    private final List<String> tags;

    public AspectDefinition(String id,
                            String displayName,
                            String effectDescription,
                            Set<EquipmentSlot> allowedItemSlots,
                            Set<HeroClass> heroClasses,
                            List<String> tags) {
        this(id, displayName, effectDescription, AspectType.LEGENDARY, AspectRuntimeStatus.DESCRIPTIVE_ONLY,
                allowedItemSlots, heroClasses, tags);
    }

    public AspectDefinition(String id,
                            String displayName,
                            String effectDescription,
                            AspectType aspectType,
                            AspectRuntimeStatus runtimeStatus,
                            Set<EquipmentSlot> allowedItemSlots,
                            Set<HeroClass> heroClasses,
                            List<String> tags) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id aspektu jest wymagane.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Nazwa aspektu jest wymagana.");
        }
        if (effectDescription == null || effectDescription.isBlank()) {
            throw new IllegalArgumentException("Opis efektu aspektu jest wymagany.");
        }
        if (allowedItemSlots == null || allowedItemSlots.isEmpty()) {
            throw new IllegalArgumentException("Aspekt musi mieć co najmniej jeden dozwolony slot.");
        }
        this.id = id;
        this.displayName = displayName;
        this.effectDescription = effectDescription;
        this.aspectType = aspectType == null ? AspectType.LEGENDARY : aspectType;
        this.runtimeStatus = runtimeStatus == null ? AspectRuntimeStatus.DESCRIPTIVE_ONLY : runtimeStatus;
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

    public String getEffectDescription() {
        return effectDescription;
    }

    public AspectType getAspectType() {
        return aspectType;
    }

    public AspectRuntimeStatus getRuntimeStatus() {
        return runtimeStatus;
    }

    public boolean isUniqueAspect() {
        return aspectType == AspectType.UNIQUE;
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
