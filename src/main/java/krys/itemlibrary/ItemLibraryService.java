package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ImportedItemCurrentBuildApplicationService;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ValidatedImportedItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
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

    public void clearActiveItem(EquipmentSlot slot) {
        repository.saveSelection(getSelection().withoutSlot(slot));
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
        CurrentBuildImportableStats activeContribution = aggregateContribution(activeItems);
        CurrentBuildImportableStats effectiveStats = applyContribution(manualBaseStats, activeContribution);
        return new EffectiveCurrentBuildResolution(manualBaseStats, activeItems, activeContribution, effectiveStats);
    }

    public List<ItemLibrarySearchCombination> generateSearchCombinations() {
        List<SavedImportedItem> savedItems = getSavedItems();
        if (savedItems.isEmpty()) {
            throw new IllegalArgumentException("Tryb searcha po bibliotece itemów wymaga co najmniej jednego zapisanego itemu.");
        }

        Map<EquipmentSlot, List<SavedImportedItem>> itemsBySlot = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            itemsBySlot.put(slot, new ArrayList<>());
        }
        for (SavedImportedItem item : savedItems) {
            itemsBySlot.get(item.getSlot()).add(item);
        }

        List<ItemLibrarySearchCombination> combinations = new ArrayList<>();
        generateSearchCombinationsRecursive(EquipmentSlot.values(), itemsBySlot, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    public CurrentBuildImportableStats resolveEffectiveStats(CurrentBuildImportableStats manualBaseStats,
                                                             ItemLibrarySearchCombination combination) {
        return applyContribution(manualBaseStats, combination.getTotalContribution());
    }

    private void generateSearchCombinationsRecursive(EquipmentSlot[] orderedSlots,
                                                     Map<EquipmentSlot, List<SavedImportedItem>> itemsBySlot,
                                                     int index,
                                                     List<SavedImportedItem> currentSelection,
                                                     List<ItemLibrarySearchCombination> combinations) {
        if (index >= orderedSlots.length) {
            combinations.add(new ItemLibrarySearchCombination(
                    currentSelection,
                    aggregateContribution(currentSelection)
            ));
            return;
        }

        EquipmentSlot slot = orderedSlots[index];
        generateSearchCombinationsRecursive(orderedSlots, itemsBySlot, index + 1, currentSelection, combinations);
        for (SavedImportedItem item : itemsBySlot.get(slot)) {
            currentSelection.add(item);
            generateSearchCombinationsRecursive(orderedSlots, itemsBySlot, index + 1, currentSelection, combinations);
            currentSelection.removeLast();
        }
    }

    private CurrentBuildImportableStats aggregateContribution(List<SavedImportedItem> items) {
        long weaponDamage = 0L;
        double strength = 0.0d;
        double intelligence = 0.0d;
        double thorns = 0.0d;
        double blockChance = 0.0d;
        double retributionChance = 0.0d;
        for (SavedImportedItem item : items) {
            ImportedItemCurrentBuildContribution contribution = contributionMapper.map(item);
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

    private CurrentBuildImportableStats applyContribution(CurrentBuildImportableStats manualBaseStats,
                                                          CurrentBuildImportableStats contribution) {
        return applicationService.apply(
                manualBaseStats,
                new ImportedItemCurrentBuildContribution(
                        contribution.getWeaponDamage(),
                        contribution.getStrength(),
                        contribution.getIntelligence(),
                        contribution.getThorns(),
                        contribution.getBlockChance(),
                        contribution.getRetributionChance()
                ),
                krys.itemimport.CurrentBuildItemApplicationMode.ADD_CONTRIBUTION
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
        return ItemLibraryPresentationSupport.slotDisplayName(importedItem.getSlot()) + " / " + importedItem.getSourceImageName();
    }
}
