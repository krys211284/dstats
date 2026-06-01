package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;
import krys.socketing.GemCatalog;
import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketContentType;
import krys.socketing.SocketGemRuneStat;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.tempering.TemperingEligibilityRegistry;
import krys.tempering.TemperingPresentationSupport;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationPresentationSupport;
import krys.transfiguration.TransfigurationValueProvenance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Waliduje ręcznie poprawiony formularz itemu i buduje zatwierdzony model domenowy. */
public final class ItemImportFormMapper {
    private final AspectRegistry aspectRegistry;
    private final TemperingAffixRegistry temperingAffixRegistry;

    public ItemImportFormMapper() {
        this(ApplicationAspectRegistry.get(), ApplicationTemperingAffixRegistry.get());
    }

    ItemImportFormMapper(AspectRegistry aspectRegistry) {
        this(aspectRegistry, ApplicationTemperingAffixRegistry.get());
    }

    ItemImportFormMapper(AspectRegistry aspectRegistry, TemperingAffixRegistry temperingAffixRegistry) {
        this.aspectRegistry = aspectRegistry;
        this.temperingAffixRegistry = temperingAffixRegistry;
    }

    public MappingResult map(ItemImportEditableForm form) {
        try (ItemImportDebugTrace.Scope ignored = ItemImportDebugTrace.startOperation("ITEM-FORM-SUBMIT")) {
            ItemImportDebugTrace.log("FORM_SUBMIT_MAPPING", () -> "input "
                    + ItemImportDebugTrace.formatForm(form)
                    + " details=" + ItemImportDebugTrace.formatDetails(form.getDetails()));
            ItemImportDebugTrace.logAffixList("FORM_SUBMIT_MAPPING", form.getAffixes());
            List<String> errors = new ArrayList<>();
            EquipmentSlot slot = parseSlot(form.getSlot(), errors);
            Long weaponDamage = parseLong(resolveVisibleWeaponDamage(form), "Weapon damage", errors);
            RuntimeProjection projection = projectAffixes(form.getAffixes());
            Double strength = projection.strength();
            Double intelligence = projection.intelligence();
            Double thorns = projection.thorns();
            Double blockChance = projection.blockChance() + visibleImplicitBlockChance(form.getFullItemRead());
            Double retributionChance = projection.retributionChance();
            Double criticalChancePercent = projection.criticalChancePercent();

            if (slot == null || weaponDamage == null || strength == null || intelligence == null
                    || thorns == null || blockChance == null || retributionChance == null || criticalChancePercent == null) {
                MappingResult result = new MappingResult(null, errors);
                logMappingResult(result);
                return result;
            }

            if (slot != EquipmentSlot.MAIN_HAND && weaponDamage > 0L) {
                errors.add("Weapon damage można ustawić wyłącznie dla slotu MAIN_HAND.");
            }
            String selectedAspectId = validateAspect(form.getSelectedAspectId(), slot, errors);
            ItemImportDetails details = buildDetails(form, slot, errors);
            List<ItemTemperingAffix> temperingAffixes = validateTempering(form.getTemperingAffixes(), slot, details.getItemType(), details.getItemPower(), errors);
            ItemMasterworking masterworking = validateMasterworking(form.getMasterworking(), form.getAffixes(), temperingAffixes, errors);
            ItemTransfiguration transfiguration = validateTransfiguration(form.getTransfiguration(), form.getAffixes(), masterworking, errors);
            ItemSocketing socketing = validateSocketing(form.getSocketing(), errors);

            if (!errors.isEmpty()) {
                MappingResult result = new MappingResult(null, errors);
                logMappingResult(result);
                return result;
            }

            MappingResult result = new MappingResult(new ValidatedImportedItem(
                    form.getSourceImageName(),
                    slot,
                    weaponDamage,
                    strength,
                    intelligence,
                    thorns,
                    blockChance,
                    retributionChance,
                    form.getAffixes(),
                    selectedAspectId,
                    details,
                    temperingAffixes,
                    masterworking,
                    transfiguration,
                    socketing
            ), errors);
            logMappingResult(result);
            return result;
        }
    }

    private static ItemSocketing validateSocketing(ItemSocketing socketing, List<String> errors) {
        ItemSocketing safe = socketing == null ? ItemSocketing.empty() : socketing;
        int socketCount = safe.getSocketCount();
        if (socketCount < ItemSocketing.MIN_SOCKET_COUNT || socketCount > ItemSocketing.MAX_SOCKET_COUNT) {
            errors.add("Gniazda: liczba gniazd musi być od 0 do 2.");
            return safe;
        }
        if (safe.getSockets().size() > socketCount) {
            errors.add("Gniazda: liczba przesłanych gniazd nie może przekraczać wybranej liczby gniazd.");
            return safe;
        }
        List<ItemSocket> validated = new ArrayList<>();
        for (int index = 0; index < socketCount; index++) {
            ItemSocket socket = safe.socketAt(index);
            SocketContentType contentType = socket.getContentType() == null ? SocketContentType.EMPTY : socket.getContentType();
            String gemId = socket.getGemId() == null ? "" : socket.getGemId().trim();
            if (contentType == SocketContentType.EMPTY) {
                if (!gemId.isBlank()) {
                    errors.add("Gniazdo " + (index + 1) + ": puste gniazdo nie może mieć wybranego gema.");
                }
                validated.add(ItemSocket.empty(index));
                continue;
            }
            if (contentType == SocketContentType.DETECTED_STAT) {
                SocketGemRuneStat detectedStat = socket.getDetectedStat();
                if (detectedStat == null || detectedStat.getDisplayText().isBlank()) {
                    errors.add("Gniazdo " + (index + 1) + ": wykryty stat gema/runy wymaga tekstu.");
                    validated.add(new ItemSocket(index, contentType, gemId, detectedStat));
                    continue;
                }
                validated.add(ItemSocket.detectedStat(index, detectedStat));
                continue;
            }
            if (gemId.isBlank()) {
                errors.add("Gniazdo " + (index + 1) + ": gem jest wymagany.");
                validated.add(new ItemSocket(index, contentType, gemId));
                continue;
            }
            if (GemCatalog.findById(gemId).isEmpty()) {
                errors.add("Nieznany gem: " + gemId);
                validated.add(new ItemSocket(index, contentType, gemId));
                continue;
            }
            validated.add(ItemSocket.gem(index, gemId));
        }
        return new ItemSocketing(socketCount, validated);
    }

    private ItemTransfiguration validateTransfiguration(ItemTransfiguration transfiguration,
                                                        List<ImportedItemAffix> affixes,
                                                        ItemMasterworking masterworking,
                                                        List<String> errors) {
        ItemTransfiguration safe = transfiguration == null ? ItemTransfiguration.none() : transfiguration;
        if (!safe.isTransfigured()) {
            return ItemTransfiguration.none();
        }
        if (safe.getOutcome() == null || safe.getOutcome() == HoradricTransfigurationOutcome.NONE) {
            errors.add("Przeistoczenie: wynik przeistoczenia jest wymagany dla przeistoczonego itemu.");
            return safe;
        }
        safe = activeOnlyTransfigurationFields(safe);
        switch (safe.getOutcome()) {
            case INDESTRUCTIBLE, UNKNOWN -> {
                return safe;
            }
            case UPGRADE_TO_GREATER_AFFIX -> validateUpgradeableAffix(
                    safe.getUpgradedAffixRef(), affixes, "Przeistoczenie: ulepszany affix", errors);
            case BONUS_TRANSFIGURATION_AFFIX -> validateTransfigurationRoll(
                    safe.getAddedTransfigurationAffix(), masterworking, "Przeistoczenie: bonusowy affix", errors);
            case REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX -> {
                validateUpgradeableAffix(safe.getReplacedAffixRef(), affixes,
                        "Przeistoczenie: zastępowany affix", errors);
                validateTransfigurationRoll(safe.getReplacementTransfigurationAffix(),
                        masterworking, "Przeistoczenie: affix zastępujący", errors);
            }
            case BONUS_ITEM_QUALITY -> validateBonusQuality(safe.getBonusQuality(), errors);
            case NONE -> {
            }
        }
        return safe;
    }

    private static ItemTransfiguration activeOnlyTransfigurationFields(ItemTransfiguration transfiguration) {
        return switch (transfiguration.getOutcome()) {
            case UPGRADE_TO_GREATER_AFFIX -> new ItemTransfiguration(
                    true,
                    transfiguration.isLockedAfterTransfiguration(),
                    transfiguration.getTuningPrism(),
                    transfiguration.getOutcome(),
                    transfiguration.getUpgradedAffixRef(),
                    null,
                    "",
                    null,
                    null,
                    false,
                    transfiguration.getNotes());
            case BONUS_TRANSFIGURATION_AFFIX -> new ItemTransfiguration(
                    true,
                    transfiguration.isLockedAfterTransfiguration(),
                    transfiguration.getTuningPrism(),
                    transfiguration.getOutcome(),
                    "",
                    transfiguration.getAddedTransfigurationAffix(),
                    "",
                    null,
                    null,
                    false,
                    transfiguration.getNotes());
            case REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX -> new ItemTransfiguration(
                    true,
                    transfiguration.isLockedAfterTransfiguration(),
                    transfiguration.getTuningPrism(),
                    transfiguration.getOutcome(),
                    "",
                    null,
                    transfiguration.getReplacedAffixRef(),
                    transfiguration.getReplacementTransfigurationAffix(),
                    null,
                    false,
                    transfiguration.getNotes());
            case BONUS_ITEM_QUALITY -> new ItemTransfiguration(
                    true,
                    transfiguration.isLockedAfterTransfiguration(),
                    transfiguration.getTuningPrism(),
                    transfiguration.getOutcome(),
                    "",
                    null,
                    "",
                    null,
                    transfiguration.getBonusQuality(),
                    false,
                    transfiguration.getNotes());
            case INDESTRUCTIBLE, UNKNOWN -> new ItemTransfiguration(
                    true,
                    transfiguration.isLockedAfterTransfiguration(),
                    transfiguration.getTuningPrism(),
                    transfiguration.getOutcome(),
                    "",
                    null,
                    "",
                    null,
                    null,
                    transfiguration.getOutcome() == HoradricTransfigurationOutcome.INDESTRUCTIBLE,
                    transfiguration.getNotes());
            case NONE -> transfiguration;
        };
    }

    private static void validateUpgradeableAffix(String rawRef,
                                                 List<ImportedItemAffix> affixes,
                                                 String label,
                                                 List<String> errors) {
        if (rawRef == null || rawRef.isBlank()) {
            errors.add(label + " musi wskazywać istniejący zwykły affix.");
            return;
        }
        if (rawRef.startsWith("TEMPERING_AFFIX:")) {
            errors.add(label + " nie może wskazywać hartowania.");
            return;
        }
        ImportedItemAffixType type;
        try {
            type = ImportedItemAffixType.valueOf(rawRef);
        } catch (IllegalArgumentException exception) {
            errors.add(label + " nie istnieje w katalogu itemu.");
            return;
        }
        ImportedItemAffix affix = affixes.stream()
                .filter(candidate -> candidate.getType() == type)
                .findFirst()
                .orElse(null);
        if (affix == null) {
            errors.add(label + " nie występuje na itemie.");
            return;
        }
        if (affix.isGreaterAffix()) {
            errors.add(label + " nie może być już Greater Affix.");
        }
    }

    private static void validateTransfigurationRoll(TransfigurationAffixRoll roll,
                                                    ItemMasterworking masterworking,
                                                    String label,
                                                    List<String> errors) {
        if (roll == null || roll.isEmpty()) {
            errors.add(label + " wymaga definicji z katalogu Przeistoczenia.");
            return;
        }
        TransfigurationAffixDefinition definition = TransfigurationAffixCatalog.findById(roll.getDefinitionId()).orElse(null);
        if (definition == null) {
            errors.add(label + " nie istnieje w katalogu Przeistoczenia.");
            return;
        }
        if (!acceptsTransfigurationValue(definition, roll, masterworking)) {
            errors.add(label + " musi mieć wartość w zakresie "
                    + expectedRangeLabel(definition, roll, masterworking) + ".");
        }
    }

    private static boolean acceptsTransfigurationValue(TransfigurationAffixDefinition definition,
                                                       TransfigurationAffixRoll roll,
                                                       ItemMasterworking masterworking) {
        if (roll.getValueProvenance() == TransfigurationValueProvenance.SOURCE_ROLL) {
            return definition.accepts(roll.getDisplayedValue());
        }
        int quality = masterworking == null ? ItemMasterworking.DEFAULT_QUALITY_CURRENT : masterworking.getQualityCurrent();
        double multiplier = 1.0d + Math.max(0, quality) / 100.0d;
        double tolerance = definition.getValueKind() == krys.transfiguration.TransfigurationAffixValueKind.FLAT
                || definition.getValueKind() == krys.transfiguration.TransfigurationAffixValueKind.RANKS
                ? 1.0d
                : 0.1d;
        double min = definition.getMin() * multiplier - tolerance;
        double max = definition.getMax() * multiplier + tolerance;
        return roll.getDisplayedValue() >= min && roll.getDisplayedValue() <= max;
    }

    private static String expectedRangeLabel(TransfigurationAffixDefinition definition,
                                             TransfigurationAffixRoll roll,
                                             ItemMasterworking masterworking) {
        if (roll == null || roll.getValueProvenance() == TransfigurationValueProvenance.SOURCE_ROLL) {
            return TransfigurationPresentationSupport.formatRange(definition);
        }
        int quality = masterworking == null ? ItemMasterworking.DEFAULT_QUALITY_CURRENT : masterworking.getQualityCurrent();
        double multiplier = 1.0d + Math.max(0, quality) / 100.0d;
        return TransfigurationPresentationSupport.formatScaledRange(definition, multiplier);
    }

    private static void validateBonusQuality(Integer bonusQuality, List<String> errors) {
        if (bonusQuality == null) {
            errors.add("Przeistoczenie: bonusowa jakość jest wymagana.");
            return;
        }
        if (bonusQuality < 1 || bonusQuality > 15) {
            errors.add("Przeistoczenie: bonusowa jakość itemu dla non-2H musi być od 1 do 15.");
        }
    }

    private ItemMasterworking validateMasterworking(ItemMasterworking masterworking,
                                                    List<ImportedItemAffix> affixes,
                                                    List<ItemTemperingAffix> temperingAffixes,
                                                    List<String> errors) {
        ItemMasterworking safe = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        if (!ItemMasterworking.isAllowedQualityStep(safe.getQualityCurrent())) {
            errors.add("Doskonalenie: Jakość Doskonalenia musi być jednym z progów: "
                    + ItemMasterworking.allowedQualityStepsLabel()
                    + ".");
        }
        if (safe.getQualityMax() != ItemMasterworking.DEFAULT_QUALITY_MAX) {
            errors.add("Doskonalenie: Jakość maksymalna musi wynosić 25.");
        }
        validatePerfectedAffix(safe, affixes, temperingAffixes, errors);
        return safe;
    }

    private void validatePerfectedAffix(ItemMasterworking masterworking,
                                        List<ImportedItemAffix> affixes,
                                        List<ItemTemperingAffix> temperingAffixes,
                                        List<String> errors) {
        MasterworkedAffixSelection selection = masterworking.getPerfectedAffix();
        if (selection == null) {
            return;
        }
        if (masterworking.getQualityCurrent() < ItemMasterworking.DEFAULT_QUALITY_MAX) {
            errors.add("Doskonalenie: aktualny doskonalony afiks można wskazać dopiero przy jakości 25/25.");
            return;
        }
        if (!selection.hasRecognizedSource()) {
            errors.add("Doskonalenie: aktualny doskonalony afiks ma nieznany typ.");
            return;
        }
        if (selection.getKey().isBlank()) {
            errors.add("Doskonalenie: aktualny doskonalony afiks wymaga klucza.");
            return;
        }
        if (selection.getSource() == MasterworkedAffixSource.ORDINARY_AFFIX) {
            validateOrdinaryPerfectedAffix(selection, affixes, errors);
            return;
        }
        if (selection.getSource() == MasterworkedAffixSource.TEMPERING_AFFIX) {
            validateTemperingPerfectedAffix(selection, temperingAffixes, errors);
        }
    }

    private static void validateOrdinaryPerfectedAffix(MasterworkedAffixSelection selection,
                                                       List<ImportedItemAffix> affixes,
                                                       List<String> errors) {
        ImportedItemAffixType type;
        try {
            type = ImportedItemAffixType.valueOf(selection.getKey());
        } catch (IllegalArgumentException exception) {
            errors.add("Doskonalenie: wskazany zwykły affix nie istnieje w katalogu itemu.");
            return;
        }
        boolean itemHasAffix = affixes.stream().anyMatch(affix -> affix.getType() == type);
        if (!itemHasAffix) {
            errors.add("Doskonalenie: wskazany zwykły affix nie występuje na itemie.");
        }
    }

    private void validateTemperingPerfectedAffix(MasterworkedAffixSelection selection,
                                                 List<ItemTemperingAffix> temperingAffixes,
                                                 List<String> errors) {
        if (temperingAffixRegistry.findById(selection.getKey()).isEmpty()) {
            errors.add("Doskonalenie: wskazane hartowanie nie istnieje w katalogu.");
            return;
        }
        boolean itemHasTempering = temperingAffixes.stream()
                .anyMatch(affix -> affix.getDefinitionId().equals(selection.getKey()));
        if (!itemHasTempering) {
            errors.add("Doskonalenie: wskazane hartowanie nie występuje na itemie.");
        }
    }

    private List<ItemTemperingAffix> validateTempering(List<ItemTemperingAffix> affixes,
                                                       EquipmentSlot slot,
                                                       String itemType,
                                                       Long itemPower,
                                                       List<String> errors) {
        List<ItemTemperingAffix> validated = new ArrayList<>();
        if (affixes.size() > 1) {
            errors.add("Hartowanie: limit hartowania dla tego przedmiotu wynosi 1.");
            return List.of();
        }
        int index = 0;
        for (ItemTemperingAffix affix : affixes) {
            index++;
            if (!TemperingEligibilityRegistry.isCategoryAvailable(slot, itemType, affix.getCategory())) {
                errors.add("Hartowanie #" + index + ": kategoria " + affix.getCategory().getDisplayName()
                        + " nie jest dostępna dla tego typu itemu.");
                continue;
            }
            TemperingAffixDefinition definition = temperingAffixRegistry.findById(affix.getDefinitionId()).orElse(null);
            if (definition == null) {
                errors.add("Hartowanie #" + index + ": wybrany affix nie istnieje w katalogu hartowania.");
                continue;
            }
            if (definition.getCategory() != affix.getCategory()) {
                errors.add("Hartowanie #" + index + ": affix nie należy do wybranej kategorii.");
                continue;
            }
            if (affix.isGreaterAffix()) {
                if (itemPower == null || itemPower != 900L) {
                    errors.add("Hartowanie #" + index + ": Greater Affix przy hartowaniu jest dostępny tylko dla przedmiotów o mocy 900.");
                    continue;
                }
                if (!sameValue(affix.getValue(), definition.greaterAffixValue())) {
                    errors.add("Hartowanie #" + index + ": wartość Greater Affix musi wynosić "
                            + TemperingPresentationSupport.formatGreaterAffixValue(definition) + ".");
                    continue;
                }
            } else if (!definition.accepts(affix.getValue())) {
                errors.add("Hartowanie #" + index + ": wartość musi być w zakresie "
                        + TemperingPresentationSupport.formatRange(definition) + ".");
                continue;
            }
            String displayText = affix.getDisplayText().isBlank()
                    ? TemperingPresentationSupport.formatAffix(affix, temperingAffixRegistry)
                    : affix.getDisplayText();
            validated.add(new ItemTemperingAffix(
                    affix.getDefinitionId(),
                    affix.getCategory(),
                    affix.getValue(),
                    displayText,
                    definition.getRuntimeStatus(),
                    affix.isGreaterAffix()
            ));
        }
        return List.copyOf(validated);
    }

    private static boolean sameValue(double left, double right) {
        return Math.abs(left - right) < 0.0000001d;
    }

    private static ItemImportDetails buildDetails(ItemImportEditableForm form,
                                                  EquipmentSlot slot,
                                                  List<String> errors) {
        Long itemPower = parseOptionalLong(form.getItemPower(), "Moc przedmiotu", errors);
        Long weaponDps = parseOptionalLong(form.getWeaponDps(), "DPS broni", errors);
        Long weaponDamageMin = parseOptionalLong(form.getWeaponDamageMin(), "Minimalne obrażenia za trafienie", errors);
        Long weaponDamageMax = parseOptionalLong(form.getWeaponDamageMax(), "Maksymalne obrażenia za trafienie", errors);
        Long averageWeaponDamage = parseOptionalLong(form.getAverageWeaponDamage(), "Średnie obrażenia za trafienie", errors);
        Double attacksPerSecond = parseOptionalDouble(form.getAttacksPerSecond(), "Ataki na sekundę", errors);
        Long itemArmor = parseOptionalLong(form.getItemArmor(), "Pancerz", errors);

        if (weaponDamageMin != null && weaponDamageMax != null) {
            long calculatedAverage = Math.round((weaponDamageMin + weaponDamageMax) / 2.0d);
            if (averageWeaponDamage == null || averageWeaponDamage == 0L) {
                averageWeaponDamage = calculatedAverage;
            }
        }

        EquipmentSlot detailSlot = slot == null ? form.getDetails().getEquipmentSlot() : slot;
        return new ItemImportDetails(
                form.getItemName(),
                form.getItemType(),
                form.getItemRarity(),
                form.isAncient(),
                detailSlot,
                itemPower,
                weaponDps,
                weaponDamageMin,
                weaponDamageMax,
                averageWeaponDamage,
                attacksPerSecond,
                itemArmor,
                form.getUniqueEffectText(),
                form.getDetails().isMythicUnique()
        );
    }

    private String validateAspect(String rawAspectId, EquipmentSlot slot, List<String> errors) {
        if (rawAspectId == null || rawAspectId.isBlank()) {
            return "";
        }
        AspectDefinition aspect = aspectRegistry.findById(rawAspectId)
                .orElse(null);
        if (aspect == null) {
            errors.add("Wybrany aspekt nie istnieje w rejestrze aspektów.");
            return "";
        }
        if (!aspect.allowsSlot(slot)) {
            errors.add("Wybrany aspekt nie pasuje do slotu itemu.");
            return "";
        }
        return aspect.getId();
    }

    private static RuntimeProjection projectAffixes(List<ImportedItemAffix> affixes) {
        double strength = 0.0d;
        double intelligence = 0.0d;
        double thorns = 0.0d;
        double blockChance = 0.0d;
        double retributionChance = 0.0d;
        double criticalChancePercent = 0.0d;
        for (ImportedItemAffix affix : affixes) {
            switch (affix.getType().getRuntimeProjection()) {
                case STRENGTH -> strength += affix.getValue();
                case INTELLIGENCE -> intelligence += affix.getValue();
                case THORNS -> thorns += affix.getValue();
                case BLOCK_CHANCE -> blockChance += affix.getValue();
                case RETRIBUTION_CHANCE -> retributionChance += affix.getValue();
                case CRITICAL_STRIKE_CHANCE -> criticalChancePercent += affix.getValue();
                case NONE -> {
                }
            }
        }
        return new RuntimeProjection(strength, intelligence, thorns, blockChance, retributionChance, criticalChancePercent);
    }

    private static EquipmentSlot parseSlot(String rawValue, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            errors.add("Slot itemu jest wymagany.");
            return null;
        }
        try {
            return EquipmentSlot.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            errors.add("Niepoprawny slot itemu.");
            return null;
        }
    }

    private static Long parseLong(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0L;
        }
        try {
            long value = Long.parseLong(rawValue.replace(" ", ""));
            if (value < 0L) {
                errors.add(label + " nie może być ujemny.");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą całkowitą.");
            return null;
        }
    }

    private static Long parseOptionalLong(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return parseLong(rawValue, label, errors);
    }

    private static Double parseDouble(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0.0d;
        }
        try {
            double value = Double.parseDouble(rawValue.replace(',', '.'));
            if (value < 0.0d) {
                errors.add(label + " nie może być ujemny.");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą.");
            return null;
        }
    }

    private static Double parseOptionalDouble(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return parseDouble(rawValue, label, errors);
    }

    private static String resolveVisibleWeaponDamage(ItemImportEditableForm form) {
        if (form.getWeaponDamage() != null && !form.getWeaponDamage().isBlank()) {
            return form.getWeaponDamage();
        }
        String baseItemValue = form.getFullItemRead().getBaseItemValue();
        String normalized = normalize(baseItemValue);
        if (!normalized.contains("OBRAZEN") && !normalized.contains("DAMAGE")) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\s\\d{3})*").matcher(baseItemValue);
        return matcher.find() ? matcher.group().replace(" ", "") : "";
    }

    private static double visibleImplicitBlockChance(FullItemRead fullItemRead) {
        for (FullItemReadLine line : fullItemRead.getLines()) {
            String normalized = normalize(line.getText());
            if (!normalized.contains("SZANSY NA BLOK") && !normalized.contains("BLOCK CHANCE")) {
                continue;
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9]+(?:[,.][0-9]+)?)").matcher(line.getText());
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1).replace(',', '.'));
            }
        }
        return 0.0d;
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private static void logMappingResult(MappingResult result) {
        ItemImportDebugTrace.log("FORM_SUBMIT_MAPPING", () -> {
            if (result.getItem() == null) {
                return "result=ERROR errors=" + result.getErrors();
            }
            ValidatedImportedItem item = result.getItem();
            return "result=OK source=" + ItemImportDebugTrace.quote(item.getSourceImageName())
                    + " slot=" + item.getSlot()
                    + " mythicUnique=" + item.getDetails().isMythicUnique()
                    + " affixes=" + item.getAffixes().size()
                    + " tempering=" + item.getTemperingAffixes().size()
                    + " details=" + ItemImportDebugTrace.formatDetails(item.getDetails());
        });
        if (result.getItem() != null) {
            ItemImportDebugTrace.logAffixList("FORM_SUBMIT_MAPPING", result.getItem().getAffixes());
            for (int index = 0; index < result.getItem().getSocketing().getSockets().size(); index++) {
                int finalIndex = index;
                krys.socketing.ItemSocket socket = result.getItem().getSocketing().getSockets().get(index);
                ItemImportDebugTrace.log("FORM_SUBMIT_MAPPING", () -> "socketIndex=" + finalIndex
                        + " " + ItemImportDebugTrace.formatSocket(socket));
            }
        }
    }

    public static final class MappingResult {
        private final ValidatedImportedItem item;
        private final List<String> errors;

        public MappingResult(ValidatedImportedItem item, List<String> errors) {
            this.item = item;
            this.errors = List.copyOf(errors);
        }

        public ValidatedImportedItem getItem() {
            return item;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private record RuntimeProjection(double strength,
                                     double intelligence,
                                     double thorns,
                                     double blockChance,
                                     double retributionChance,
                                     double criticalChancePercent) {
    }
}
