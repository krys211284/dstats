package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ImportedItemCurrentBuildApplicationService;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ValidatedImportedItem;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Warstwa aplikacyjna minimalnej biblioteki itemów nad current build. */
public final class ItemLibraryService {
    private final ItemLibraryRepository repository;
    private final SavedImportedItemCurrentBuildContributionMapper contributionMapper;
    private final ImportedItemCurrentBuildApplicationService applicationService;

    public ItemLibraryService(ItemLibraryRepository repository) {
        this(
                repository,
                new SavedImportedItemCurrentBuildContributionMapper(),
                new ImportedItemCurrentBuildApplicationService()
        );
    }

    ItemLibraryService(ItemLibraryRepository repository,
                       SavedImportedItemCurrentBuildContributionMapper contributionMapper,
                       ImportedItemCurrentBuildApplicationService applicationService) {
        this.repository = repository;
        this.contributionMapper = contributionMapper;
        this.applicationService = applicationService;
    }

    public SavedImportedItem saveImportedItem(ValidatedImportedItem importedItem) {
        SavedImportedItem itemToSave = new SavedImportedItem(
                0L,
                buildDisplayName(importedItem),
                importedItem.getSourceImageName(),
                importedItem.getSlot(),
                importedItem.getWeaponDamage(),
                importedItem.getStrength(),
                importedItem.getIntelligence(),
                importedItem.getThorns(),
                importedItem.getBlockChance(),
                importedItem.getRetributionChance()
        );
        return repository.save(itemToSave);
    }

    public List<SavedImportedItem> getSavedItems() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing((SavedImportedItem item) -> item.getSlot().name())
                        .thenComparing(SavedImportedItem::getItemId))
                .toList();
    }

    public ActiveItemSelection getSelection() {
        return sanitizeSelection(repository.loadSelection());
    }

    public void setActiveItem(EquipmentSlot slot, long itemId) {
        SavedImportedItem item = repository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono itemu o podanym id w bibliotece."));
        if (item.getSlot() != slot) {
            throw new IllegalArgumentException("Nie można ustawić jako aktywnego itemu z innego slotu.");
        }
        repository.saveSelection(getSelection().withSelectedItem(slot, itemId));
    }

    public void deleteItem(long itemId) {
        repository.delete(itemId);
        repository.saveSelection(getSelection().withoutItemId(itemId));
    }

    public List<SavedImportedItem> getActiveItems() {
        ActiveItemSelection selection = getSelection();
        Map<Long, SavedImportedItem> itemsById = repository.findAll().stream()
                .collect(Collectors.toMap(SavedImportedItem::getItemId, Function.identity()));
        return selection.getSelectedItemIdsBySlot().entrySet().stream()
                .map(entry -> itemsById.get(entry.getValue()))
                .filter(item -> item != null)
                .sorted(Comparator.comparing((SavedImportedItem item) -> item.getSlot().name())
                        .thenComparing(SavedImportedItem::getItemId))
                .toList();
    }

    public EffectiveCurrentBuildResolution resolveEffectiveCurrentBuild(CurrentBuildImportableStats manualBaseStats) {
        List<SavedImportedItem> activeItems = getActiveItems();
        CurrentBuildImportableStats activeContribution = aggregateActiveContribution(activeItems);
        CurrentBuildImportableStats effectiveStats = applicationService.apply(
                manualBaseStats,
                new ImportedItemCurrentBuildContribution(
                        activeContribution.getWeaponDamage(),
                        activeContribution.getStrength(),
                        activeContribution.getIntelligence(),
                        activeContribution.getThorns(),
                        activeContribution.getBlockChance(),
                        activeContribution.getRetributionChance()
                ),
                krys.itemimport.CurrentBuildItemApplicationMode.ADD_CONTRIBUTION
        );
        return new EffectiveCurrentBuildResolution(manualBaseStats, activeItems, activeContribution, effectiveStats);
    }

    private CurrentBuildImportableStats aggregateActiveContribution(List<SavedImportedItem> activeItems) {
        long weaponDamage = 0L;
        double strength = 0.0d;
        double intelligence = 0.0d;
        double thorns = 0.0d;
        double blockChance = 0.0d;
        double retributionChance = 0.0d;
        for (SavedImportedItem activeItem : activeItems) {
            ImportedItemCurrentBuildContribution contribution = contributionMapper.map(activeItem);
            weaponDamage += contribution.getWeaponDamage();
            strength += contribution.getStrength();
            intelligence += contribution.getIntelligence();
            thorns += contribution.getThorns();
            blockChance += contribution.getBlockChance();
            retributionChance += contribution.getRetributionChance();
        }
        return new CurrentBuildImportableStats(
                weaponDamage,
                strength,
                intelligence,
                thorns,
                blockChance,
                retributionChance
        );
    }

    private ActiveItemSelection sanitizeSelection(ActiveItemSelection selection) {
        Map<Long, SavedImportedItem> itemsById = repository.findAll().stream()
                .collect(Collectors.toMap(SavedImportedItem::getItemId, Function.identity()));

        ActiveItemSelection sanitizedSelection = ActiveItemSelection.empty();
        boolean changed = false;
        for (Map.Entry<EquipmentSlot, Long> entry : selection.getSelectedItemIdsBySlot().entrySet()) {
            SavedImportedItem item = itemsById.get(entry.getValue());
            if (item == null || item.getSlot() != entry.getKey()) {
                changed = true;
                continue;
            }
            sanitizedSelection = sanitizedSelection.withSelectedItem(entry.getKey(), item.getItemId());
        }
        if (changed) {
            repository.saveSelection(sanitizedSelection);
        }
        return sanitizedSelection;
    }

    private static String buildDisplayName(ValidatedImportedItem importedItem) {
        return importedItem.getSlot().name() + " / " + importedItem.getSourceImageName();
    }
}
