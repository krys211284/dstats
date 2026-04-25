package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.FullItemRead;
import krys.itemimport.ImportedItemCurrentBuildApplicationService;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ValidatedImportedItem;
import krys.web.HeroItemSelection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return saveImportedItem(importedItem, FullItemRead.empty());
    }

    public SavedImportedItem saveImportedItem(ValidatedImportedItem importedItem, FullItemRead fullItemRead) {
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
                importedItem.getRetributionChance(),
                fullItemRead
        );
        return repository.save(itemToSave);
    }

    public List<SavedImportedItem> getSavedItems() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing((SavedImportedItem item) -> item.getSlot().name())
                        .thenComparing(SavedImportedItem::getItemId))
                .toList();
    }

    public void deleteItem(long itemId) {
        repository.delete(itemId);
    }

    public SavedImportedItem requireCompatibleItem(HeroEquipmentSlot heroSlot, long itemId) {
        SavedImportedItem item = repository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono itemu o podanym id w bibliotece."));
        if (!heroSlot.supports(item.getSlot())) {
            throw new IllegalArgumentException("Nie można ustawić jako aktywnego itemu z niepasującego typu slotu.");
        }
        return item;
    }

    public List<SavedImportedItem> getCompatibleItems(HeroEquipmentSlot heroSlot) {
        return getSavedItems().stream()
                .filter(item -> heroSlot.supports(item.getSlot()))
                .toList();
    }

    public EffectiveCurrentBuildResolution resolveEffectiveCurrentBuild(CurrentBuildImportableStats manualBaseStats,
                                                                        HeroItemSelection selection) {
        List<HeroSlotItemAssignment> activeItems = resolveAssignments(selection);
        CurrentBuildImportableStats activeContribution = aggregateContribution(activeItems);
        CurrentBuildImportableStats effectiveStats = applyContribution(manualBaseStats, activeContribution);
        return new EffectiveCurrentBuildResolution(manualBaseStats, activeItems, activeContribution, effectiveStats);
    }

    public List<ItemLibrarySearchCombination> generateSearchCombinations() {
        List<SavedImportedItem> savedItems = getSavedItems();
        if (savedItems.isEmpty()) {
            throw new IllegalArgumentException("Tryb searcha po bibliotece itemów wymaga co najmniej jednego zapisanego itemu.");
        }

        Map<HeroEquipmentSlot, List<SavedImportedItem>> itemsBySlot = new EnumMap<>(HeroEquipmentSlot.class);
        for (HeroEquipmentSlot slot : HeroEquipmentSlot.values()) {
            itemsBySlot.put(slot, new ArrayList<>());
        }
        for (SavedImportedItem item : savedItems) {
            for (HeroEquipmentSlot heroSlot : HeroEquipmentSlot.compatibleWith(item.getSlot())) {
                itemsBySlot.get(heroSlot).add(item);
            }
        }

        List<ItemLibrarySearchCombination> combinations = new ArrayList<>();
        generateSearchCombinationsRecursive(
                HeroEquipmentSlot.values(),
                itemsBySlot,
                0,
                new ArrayList<>(),
                new HashSet<>(),
                combinations
        );
        return combinations;
    }

    public CurrentBuildImportableStats resolveEffectiveStats(CurrentBuildImportableStats manualBaseStats,
                                                             ItemLibrarySearchCombination combination) {
        return applyContribution(manualBaseStats, combination.getTotalContribution());
    }

    private void generateSearchCombinationsRecursive(HeroEquipmentSlot[] orderedSlots,
                                                     Map<HeroEquipmentSlot, List<SavedImportedItem>> itemsBySlot,
                                                     int index,
                                                     List<HeroSlotItemAssignment> currentSelection,
                                                     Set<Long> usedItemIds,
                                                     List<ItemLibrarySearchCombination> combinations) {
        if (index >= orderedSlots.length) {
            combinations.add(new ItemLibrarySearchCombination(
                    currentSelection,
                    aggregateContribution(currentSelection)
            ));
            return;
        }

        HeroEquipmentSlot slot = orderedSlots[index];
        generateSearchCombinationsRecursive(orderedSlots, itemsBySlot, index + 1, currentSelection, usedItemIds, combinations);
        for (SavedImportedItem item : itemsBySlot.get(slot)) {
            if (!usedItemIds.add(item.getItemId())) {
                continue;
            }
            currentSelection.add(new HeroSlotItemAssignment(slot, item));
            generateSearchCombinationsRecursive(orderedSlots, itemsBySlot, index + 1, currentSelection, usedItemIds, combinations);
            currentSelection.removeLast();
            usedItemIds.remove(item.getItemId());
        }
    }

    public CurrentBuildImportableStats resolveActiveItemsContribution(HeroItemSelection selection) {
        return aggregateContribution(resolveAssignments(selection));
    }

    private CurrentBuildImportableStats aggregateContribution(List<HeroSlotItemAssignment> items) {
        long weaponDamage = 0L;
        double strength = 0.0d;
        double intelligence = 0.0d;
        double thorns = 0.0d;
        double blockChance = 0.0d;
        double retributionChance = 0.0d;
        for (HeroSlotItemAssignment assignment : items) {
            ImportedItemCurrentBuildContribution contribution = contributionMapper.map(assignment.getItem());
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

    private List<HeroSlotItemAssignment> resolveAssignments(HeroItemSelection selection) {
        List<HeroSlotItemAssignment> assignments = new ArrayList<>();
        if (selection == null) {
            return assignments;
        }
        Map<Long, SavedImportedItem> itemsById = new java.util.HashMap<>();
        for (SavedImportedItem item : repository.findAll()) {
            itemsById.put(item.getItemId(), item);
        }
        for (Map.Entry<HeroEquipmentSlot, Long> entry : selection.getSelectedItemIdsBySlot().entrySet()) {
            SavedImportedItem item = itemsById.get(entry.getValue());
            if (item == null || !entry.getKey().supports(item.getSlot())) {
                continue;
            }
            assignments.add(new HeroSlotItemAssignment(entry.getKey(), item));
        }
        assignments.sort(Comparator
                .comparing((HeroSlotItemAssignment assignment) -> assignment.getHeroSlot().name())
                .thenComparing(assignment -> assignment.getItem().getItemId()));
        return assignments;
    }

    private static String buildDisplayName(ValidatedImportedItem importedItem) {
        return ItemLibraryPresentationSupport.slotDisplayName(importedItem.getSlot()) + " / " + importedItem.getSourceImageName();
    }
}
