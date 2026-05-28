package krys.itemimport;

import krys.item.EquipmentSlot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Scala wyniki z wielu wariantów OCR w jeden deterministyczny candidate parse result. */
final class ItemImageImportCandidateMerger {
    private static final Pattern ROLL_RANGE_PATTERN = Pattern.compile(
            "\\[\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?)"
                    + "(?:\\s*[-–—−]\\s*\\+?\\s*([0-9]+(?:\\s+[0-9]{3})*(?:[,.][0-9]+)?))?"
                    + "\\s*]\\s*%?"
    );

    ItemImageImportCandidateParseResult merge(ItemImageMetadata metadata,
                                              int analyzedVariantCount,
                                              List<ItemImageImportCandidateParseResult> parseResults) {
        ItemImportFieldCandidate<EquipmentSlot> slotCandidate = mergeField(
                parseResults.stream().map(ItemImageImportCandidateParseResult::getSlotCandidate).toList(),
                "Nie udało się rozpoznać slotu / typu itemu z OCR."
        );
        FullItemRead fullItemRead = mergeFullItemRead(parseResults);
        ItemImportFieldCandidate<Long> weaponDamageCandidate = mergeField(
                parseResults.stream().map(ItemImageImportCandidateParseResult::getWeaponDamageCandidate).toList(),
                "Nie udało się rozpoznać pola `WEAPON DAMAGE` z OCR."
        );
        ItemImportFieldCandidate<Double> strengthCandidate = mergeField(
                parseResults.stream().map(ItemImageImportCandidateParseResult::getStrengthCandidate).toList(),
                "Nie udało się rozpoznać pola `Strength` z OCR."
        );
        ItemImportFieldCandidate<Double> intelligenceCandidate = mergeField(
                parseResults.stream().map(ItemImageImportCandidateParseResult::getIntelligenceCandidate).toList(),
                "Nie udało się rozpoznać pola `Intelligence` z OCR."
        );
        ItemImportFieldCandidate<Double> thornsCandidate = mergeField(
                parseResults.stream().map(ItemImageImportCandidateParseResult::getThornsCandidate).toList(),
                "Nie udało się rozpoznać pola `Thorns` z OCR."
        );
        ItemImportFieldCandidate<Double> blockChanceCandidate = mergeField(
                parseResults.stream().map(ItemImageImportCandidateParseResult::getBlockChanceCandidate).toList(),
                "Nie udało się rozpoznać pola `Block chance` z OCR."
        );
        ItemImportFieldCandidate<Double> retributionChanceCandidate = mergeField(
                parseResults.stream().map(ItemImageImportCandidateParseResult::getRetributionChanceCandidate).toList(),
                "Nie udało się rozpoznać pola `Retribution chance` z OCR."
        );

        return new ItemImageImportCandidateParseResult(
                metadata,
                fullItemRead,
                slotCandidate,
                weaponDamageCandidate,
                strengthCandidate,
                intelligenceCandidate,
                thornsCandidate,
                blockChanceCandidate,
                retributionChanceCandidate,
                buildImportNotice(analyzedVariantCount, slotCandidate, weaponDamageCandidate, strengthCandidate,
                        intelligenceCandidate, thornsCandidate, blockChanceCandidate, retributionChanceCandidate)
        );
    }

    private static FullItemRead mergeFullItemRead(List<ItemImageImportCandidateParseResult> parseResults) {
        Map<String, FullItemReadLine> mergedLines = new LinkedHashMap<>();
        String itemName = "";
        String itemTypeLine = "";
        String rarity = "";
        String itemPower = "";
        String baseItemValue = "";
        List<ItemImportDetails> sourceDetails = new ArrayList<>();

        for (ItemImageImportCandidateParseResult parseResult : parseResults) {
            FullItemRead read = parseResult.getFullItemRead();
            if (read == null || !read.hasAnyData()) {
                continue;
            }
            sourceDetails.add(read.getDetails());
            if (itemName.isBlank() && !read.getItemName().isBlank()) {
                itemName = read.getItemName();
            }
            if (itemTypeLine.isBlank() && !read.getItemTypeLine().isBlank()) {
                itemTypeLine = read.getItemTypeLine();
            }
            if (rarity.isBlank() && !read.getRarity().isBlank()) {
                rarity = read.getRarity();
            }
            if (itemPower.isBlank() && !read.getItemPower().isBlank()) {
                itemPower = read.getItemPower();
            }
            if (baseItemValue.isBlank() && !read.getBaseItemValue().isBlank()) {
                baseItemValue = read.getBaseItemValue();
            }
            for (FullItemReadLine line : read.getLines()) {
                if (line.getText().isBlank()) {
                    continue;
                }
                String key = fullReadLineDeduplicationKey(line);
                FullItemReadLine existingLine = mergedLines.get(key);
                if (existingLine == null) {
                    logAffixMerge(key, null, line, line, "first candidate for dedup key");
                    mergedLines.put(key, line);
                } else if (shouldReplaceMergedLine(key, existingLine, line)) {
                    logAffixMerge(key, existingLine, line, line, mergeReplacementReason(key, existingLine, line));
                    mergedLines.put(key, line);
                } else {
                    logAffixMerge(key, existingLine, line, existingLine, mergeKeepReason(key, existingLine, line));
                }
            }
        }

        FullItemRead mergedRead = ItemImageImportTextParser.buildFullItemRead(
                mergedLines.values().stream()
                        .map(FullItemReadLine::getText)
                        .toList()
        );
        return new FullItemRead(
                chooseBetterItemName(itemName, mergedRead.getItemName()),
                itemTypeLine.isBlank() ? mergedRead.getItemTypeLine() : itemTypeLine,
                rarity.isBlank() ? mergedRead.getRarity() : rarity,
                itemPower.isBlank() ? mergedRead.getItemPower() : itemPower,
                baseItemValue.isBlank() ? mergedRead.getBaseItemValue() : baseItemValue,
                List.copyOf(mergedLines.values()),
                mergeDetails(mergedRead.getDetails(), sourceDetails)
        );
    }

    private static boolean shouldReplaceMergedLine(String key, FullItemReadLine existingLine, FullItemReadLine candidateLine) {
        if (key != null && key.startsWith("AFFIX:")) {
            boolean existingHasCompatibleSingleReference = hasCompatibleSingleReference(key, existingLine.getText());
            boolean candidateHasCompatibleSingleReference = hasCompatibleSingleReference(key, candidateLine.getText());
            if (candidateHasCompatibleSingleReference && !existingHasCompatibleSingleReference) {
                return true;
            }
            if (existingHasCompatibleSingleReference && !candidateHasCompatibleSingleReference) {
                return false;
            }
            boolean existingHasRollRange = hasActualRollRange(existingLine.getText());
            boolean candidateHasRollRange = hasActualRollRange(candidateLine.getText());
            if (candidateHasRollRange && !existingHasRollRange) {
                return true;
            }
            if (existingHasRollRange && !candidateHasRollRange) {
                return false;
            }
        }
        return lineQualityScore(candidateLine) > lineQualityScore(existingLine);
    }

    private static void logAffixMerge(String key,
                                      FullItemReadLine existingLine,
                                      FullItemReadLine candidateLine,
                                      FullItemReadLine selectedLine,
                                      String reason) {
        if (!ItemImportDebugTrace.isEnabled() || key == null || !key.startsWith("AFFIX:")) {
            return;
        }
        ItemImportDebugTrace.log("AFFIX_MERGE", () -> "dedupKey=" + ItemImportDebugTrace.quote(key)
                + " existing=" + formatLineCandidate(existingLine)
                + " candidate=" + formatLineCandidate(candidateLine)
                + " selected=" + formatLineCandidate(selectedLine)
                + " reason=" + ItemImportDebugTrace.quote(reason)
                + " candidateHasReferenceOrRange=" + hasActualRollRange(candidateLine == null ? "" : candidateLine.getText())
                + " selectedHasReferenceOrRange=" + hasActualRollRange(selectedLine == null ? "" : selectedLine.getText()));
    }

    private static String formatLineCandidate(FullItemReadLine line) {
        if (line == null) {
            return "null";
        }
        return "{type=" + line.getType()
                + ", score=" + lineQualityScore(line)
                + ", text=" + ItemImportDebugTrace.compactText(line.getText())
                + ", tokens=" + ItemImportDebugTrace.numericTokens(line.getText())
                + "}";
    }

    private static String mergeReplacementReason(String key, FullItemReadLine existingLine, FullItemReadLine candidateLine) {
        if (key != null && key.startsWith("AFFIX:")) {
            boolean existingHasCompatibleSingleReference = hasCompatibleSingleReference(key, existingLine.getText());
            boolean candidateHasCompatibleSingleReference = hasCompatibleSingleReference(key, candidateLine.getText());
            if (candidateHasCompatibleSingleReference && !existingHasCompatibleSingleReference) {
                return "candidate has compatible single reference from OCR";
            }
            boolean existingHasRollRange = hasActualRollRange(existingLine.getText());
            boolean candidateHasRollRange = hasActualRollRange(candidateLine.getText());
            if (candidateHasRollRange && !existingHasRollRange) {
                return "candidate has OCR range/reference fragment";
            }
        }
        return "candidate has higher line quality score";
    }

    private static String mergeKeepReason(String key, FullItemReadLine existingLine, FullItemReadLine candidateLine) {
        if (key != null && key.startsWith("AFFIX:")) {
            boolean existingHasCompatibleSingleReference = hasCompatibleSingleReference(key, existingLine.getText());
            boolean candidateHasCompatibleSingleReference = hasCompatibleSingleReference(key, candidateLine.getText());
            if (existingHasCompatibleSingleReference && !candidateHasCompatibleSingleReference) {
                return "existing candidate keeps compatible single reference from OCR";
            }
            boolean existingHasRollRange = hasActualRollRange(existingLine.getText());
            boolean candidateHasRollRange = hasActualRollRange(candidateLine.getText());
            if (existingHasRollRange && !candidateHasRollRange) {
                return "existing candidate keeps OCR range/reference fragment";
            }
        }
        return "existing candidate has equal or higher line quality score";
    }

    private static boolean hasCompatibleSingleReference(String key, String text) {
        String[] keyParts = key == null ? new String[0] : key.split(":");
        if (keyParts.length < 3) {
            return false;
        }
        double displayedValue = parseRollRangeNumber(keyParts[2]);
        if (Double.isNaN(displayedValue)) {
            return false;
        }
        Matcher matcher = ROLL_RANGE_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find() || matcher.group(2) != null) {
            return false;
        }
        double referenceValue = parseRollRangeNumber(matcher.group(1));
        return !Double.isNaN(referenceValue)
                && referenceValue <= displayedValue + 0.0001d
                && displayedValue <= referenceValue * 1.25d + 0.25d;
    }

    private static boolean hasActualRollRange(String text) {
        Matcher matcher = ROLL_RANGE_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return false;
        }
        double min = parseRollRangeNumber(matcher.group(1));
        double max = matcher.group(2) == null ? min : parseRollRangeNumber(matcher.group(2));
        return min <= max;
    }

    private static double parseRollRangeNumber(String token) {
        try {
            return Double.parseDouble((token == null ? "" : token).replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException exception) {
            return Double.NaN;
        }
    }

    private static ItemImportDetails mergeDetails(ItemImportDetails rebuiltDetails, List<ItemImportDetails> sourceDetails) {
        ItemImportDetails safeRebuiltDetails = rebuiltDetails == null ? ItemImportDetails.empty() : rebuiltDetails;
        List<ItemImportDetails> details = new ArrayList<>();
        details.add(safeRebuiltDetails);
        for (ItemImportDetails sourceDetail : sourceDetails) {
            if (sourceDetail != null && sourceDetail.hasAnyData()) {
                details.add(sourceDetail);
            }
        }
        Long weaponDamageMin = firstLong(details, DetailLongField.WEAPON_DAMAGE_MIN);
        Long weaponDamageMax = firstLong(details, DetailLongField.WEAPON_DAMAGE_MAX);
        return new ItemImportDetails(
                firstText(details, DetailTextField.ITEM_NAME),
                firstText(details, DetailTextField.ITEM_TYPE),
                firstText(details, DetailTextField.ITEM_RARITY),
                firstBoolean(details, safeRebuiltDetails.isAncient()),
                firstSlot(details),
                firstLong(details, DetailLongField.ITEM_POWER),
                firstLong(details, DetailLongField.WEAPON_DPS),
                weaponDamageMin,
                weaponDamageMax,
                firstLong(details, DetailLongField.AVERAGE_WEAPON_DAMAGE),
                firstDouble(details),
                firstLong(details, DetailLongField.ITEM_ARMOR),
                firstText(details, DetailTextField.UNIQUE_EFFECT_TEXT),
                firstMythicUnique(details, safeRebuiltDetails.isMythicUnique())
        );
    }

    private static boolean firstMythicUnique(List<ItemImportDetails> details, boolean fallback) {
        for (ItemImportDetails detail : details) {
            if (detail.isMythicUnique()) {
                return true;
            }
        }
        return fallback;
    }

    private static String firstText(List<ItemImportDetails> details, DetailTextField field) {
        if (field == DetailTextField.ITEM_NAME) {
            String bestName = "";
            for (ItemImportDetails detail : details) {
                bestName = chooseBetterItemName(bestName, detail.getItemName());
            }
            return bestName;
        }
        for (ItemImportDetails detail : details) {
            String value = switch (field) {
                case ITEM_NAME -> detail.getItemName();
                case ITEM_TYPE -> detail.getItemType();
                case ITEM_RARITY -> detail.getItemRarity();
                case UNIQUE_EFFECT_TEXT -> detail.getUniqueEffectText();
            };
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String chooseBetterItemName(String current, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return current == null ? "" : current;
        }
        if (current == null || current.isBlank()) {
            return candidate;
        }
        int candidateScore = itemNameQualityScore(candidate);
        int currentScore = itemNameQualityScore(current);
        if (candidateScore != currentScore) {
            return candidateScore > currentScore ? candidate : current;
        }
        return candidate.length() > current.length() ? candidate : current;
    }

    private static int itemNameQualityScore(String value) {
        String text = value == null ? "" : value;
        int score = normalizeForDeduplication(text).replaceAll("[^A-Z0-9]", "").length();
        if (text.matches(".*[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ].*")) {
            score += 30;
        }
        if (text.equals(text.toUpperCase(Locale.ROOT))) {
            score += 10;
        }
        if (hasSuspiciousMixedTitleCase(text)) {
            score -= 80;
        }
        return score;
    }

    private static boolean hasSuspiciousMixedTitleCase(String value) {
        String lettersOnly = value == null ? "" : value.replaceAll("[^\\p{L}]", "");
        if (lettersOnly.length() < 5) {
            return false;
        }
        int uppercase = 0;
        int lowercase = 0;
        for (int index = 0; index < lettersOnly.length(); index++) {
            char character = lettersOnly.charAt(index);
            if (Character.isUpperCase(character)) {
                uppercase++;
            } else if (Character.isLowerCase(character)) {
                lowercase++;
            }
        }
        return uppercase >= 4 && lowercase > 0 && uppercase > lowercase * 3;
    }

    private static boolean firstBoolean(List<ItemImportDetails> details, boolean fallback) {
        for (ItemImportDetails detail : details) {
            if (detail.isAncient()) {
                return true;
            }
        }
        return fallback;
    }

    private static EquipmentSlot firstSlot(List<ItemImportDetails> details) {
        for (ItemImportDetails detail : details) {
            if (detail.getEquipmentSlot() != null) {
                return detail.getEquipmentSlot();
            }
        }
        return null;
    }

    private static Long firstLong(List<ItemImportDetails> details, DetailLongField field) {
        for (ItemImportDetails detail : details) {
            Long value = switch (field) {
                case ITEM_POWER -> detail.getItemPower();
                case WEAPON_DPS -> detail.getWeaponDps();
                case WEAPON_DAMAGE_MIN -> detail.getWeaponDamageMin();
                case WEAPON_DAMAGE_MAX -> detail.getWeaponDamageMax();
                case AVERAGE_WEAPON_DAMAGE -> detail.getAverageWeaponDamage();
                case ITEM_ARMOR -> detail.getItemArmor();
            };
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static Double firstDouble(List<ItemImportDetails> details) {
        for (ItemImportDetails detail : details) {
            if (detail.getAttacksPerSecond() != null) {
                return detail.getAttacksPerSecond();
            }
        }
        return null;
    }

    private static String fullReadLineDeduplicationKey(FullItemReadLine line) {
        String normalized = normalizeForDeduplication(line.getText());
        FullItemReadLineType type = line.getType();
        if (type == FullItemReadLineType.ITEM_POWER && normalized.contains("MOC PRZEDMIOTU")) {
            return "ITEM_POWER:" + firstNumber(normalized);
        }
        if (type == FullItemReadLineType.BASE_STAT && normalized.contains("PANCERZ")) {
            return "BASE_ARMOR:" + firstNumber(normalized);
        }
        if (normalized.contains("REDUKCJI BLOKOWANYCH OBRAZEN")) {
            return "IMPLICIT:BLOCKED_DAMAGE_REDUCTION";
        }
        if (normalized.contains("SZANSY NA BLOK")) {
            return "IMPLICIT:BLOCK_CHANCE";
        }
        if (normalized.contains("OBRAZEN OD BRONI W GLOWNEJ RECE")) {
            return "IMPLICIT:MAIN_HAND_WEAPON_DAMAGE";
        }
        if (normalized.contains(" SILY")) {
            return "AFFIX:STRENGTH:" + firstNumber(normalized);
        }
        if (normalized.contains("ODPORNOSCI NA WSZYSTKIE ZYWIOLY")) {
            return "AFFIX:ALL_RESISTANCE:" + firstNumber(normalized);
        }
        if (normalized.contains("ODPORNOSCI NA: OGIEN") || normalized.contains("ODPORNOSCI NA OGIEN")) {
            return "AFFIX:FIRE_RESISTANCE:" + firstNumber(normalized);
        }
        if (normalized.contains("REDUKCJI OBRAZEN")) {
            return "AFFIX:DAMAGE_REDUCTION:" + firstNumber(normalized);
        }
        if (normalized.contains("CIERNI")) {
            return "AFFIX:THORNS:" + firstNumber(normalized);
        }
        String collapsed = normalized.replaceAll("[^A-Z0-9]", "");
        if (normalized.contains("SZANSY NA TRAFIENIE KRYTYCZNE") || normalized.contains("SZANSA NA TRAFIENIE KRYTYCZNE")) {
            return "AFFIX:CRITICAL_STRIKE_CHANCE:" + firstNumber(normalized);
        }
        if (isLuckyHitChanceLine(normalized, collapsed)) {
            return "AFFIX:LUCKY_HIT_CHANCE:" + firstNumber(normalized);
        }
        if (normalized.contains("SZYBKOSCI RUCHU")) {
            return "AFFIX:MOVEMENT_SPEED:" + firstNumber(normalized);
        }
        if (normalized.contains("UMIEJETNOSCI") && normalized.contains("GLOWNE")) {
            return "AFFIX:CORE_SKILL_RANKS:" + firstNumber(normalized);
        }
        if (normalized.contains("CZASU ODNOWIENIA")) {
            return "AFFIX:COOLDOWN_REDUCTION:" + firstNumber(normalized);
        }
        if (normalized.contains("OBRAZEN Z UPLYWEM CZASU")) {
            return "AFFIX:DAMAGE_OVER_TIME_MULTIPLIER:" + firstNumber(normalized);
        }
        if (normalized.contains("GDY MASZ UMOCNIENIE")) {
            return "ASPECT:FORTIFY_DAMAGE";
        }
        if (normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")) {
            return "ASPECT:DAMAGE_INCREASED";
        }
        if (normalized.contains("TA PREMIA JEST TRZY RAZY WIEKSZA")) {
            return "ASPECT:STANDING_STILL_MULTIPLIER";
        }
        if (normalized.contains("ROZJUSZENIE")) {
            return "SEASONAL:ROZJUSZENIE:" + firstNumber(normalized);
        }
        return type.name() + ":" + normalized.replaceAll("\\s+", " ").trim();
    }

    private static boolean isLuckyHitChanceLine(String normalized, String collapsed) {
        if (collapsed.contains("PODSTAWOWEGOZASOBU")
                || collapsed.contains("PODSTAWOWYZASOB")
                || collapsed.contains("ODZYSKANIE")) {
            return false;
        }
        return normalized.contains("SZCZESLIWY TRAF")
                || normalized.contains("SZCZESNWY TRAF")
                || normalized.contains("SZANSY TRAF")
                || normalized.contains("SZANSY WY TRAF")
                || (collapsed.contains("SZANS") && collapsed.contains("TRAF") && !collapsed.contains("TRAFIENIEKRYTYCZNE"));
    }

    private static int lineQualityScore(FullItemReadLine line) {
        String text = line.getText() == null ? "" : line.getText();
        int score = text.length();
        String trimmedText = text == null ? "" : text.trim();
        if (trimmedText.startsWith("*")
                || trimmedText.startsWith("★")
                || trimmedText.startsWith("⭐")
                || trimmedText.startsWith("✦")
                || trimmedText.startsWith("✧")
                || trimmedText.startsWith("✱")
                || trimmedText.startsWith("✳")
                || trimmedText.startsWith("✴")
                || trimmedText.startsWith("✵")
                || trimmedText.startsWith("✶")
                || trimmedText.startsWith("✷")
                || trimmedText.startsWith("✸")
                || trimmedText.startsWith("✹")
                || trimmedText.startsWith("✺")
                || trimmedText.startsWith("✻")
                || trimmedText.startsWith("✼")
                || trimmedText.startsWith("✽")
                || trimmedText.startsWith("✾")
                || trimmedText.startsWith("❋")
                || trimmedText.startsWith("❂")
                || trimmedText.startsWith("◆")
                || trimmedText.startsWith("◇")
                || trimmedText.startsWith("♦")
                || trimmedText.startsWith("●")
                || trimmedText.startsWith("•")) {
            score += 300;
        }
        if (text.matches(".*[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ].*")) {
            score += 50;
        }
        if (text.contains("[") && text.contains("]")) {
            score += 20;
        }
        if (text.contains("%[x]")) {
            score += 30;
        }
        if (text.contains("+[")) {
            score -= 3;
        }
        if (text.contains("1001") || text.contains("451") || text.contains("5061")) {
            score -= 5;
        }
        return score;
    }

    private static String firstNumber(String normalizedText) {
        Matcher matcher = Pattern.compile("[0-9]+(?:[,.][0-9]+)?").matcher(normalizedText);
        return matcher.find() ? matcher.group().replace(',', '.') : "";
    }

    private static String normalizeForDeduplication(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    private static <T> ItemImportFieldCandidate<T> mergeField(List<ItemImportFieldCandidate<T>> candidates,
                                                              String unknownNote) {
        List<ItemImportFieldCandidate<T>> knownCandidates = new ArrayList<>();
        for (ItemImportFieldCandidate<T> candidate : candidates) {
            if (candidate.getSuggestedValue() != null) {
                knownCandidates.add(candidate);
            }
        }
        if (knownCandidates.isEmpty()) {
            return ItemImportFieldCandidate.unknown(unknownNote);
        }

        List<FieldValueGroup<T>> groups = buildGroups(knownCandidates);
        groups.sort((left, right) -> {
            int byConfidence = Integer.compare(right.bestConfidenceScore(), left.bestConfidenceScore());
            if (byConfidence != 0) {
                return byConfidence;
            }
            int byOccurrences = Integer.compare(right.occurrences(), left.occurrences());
            if (byOccurrences != 0) {
                return byOccurrences;
            }
            return Integer.compare(left.firstIndex(), right.firstIndex());
        });

        FieldValueGroup<T> bestGroup = groups.getFirst();
        ItemImportFieldCandidate<T> bestCandidate = bestGroup.bestCandidate();
        if (groups.size() == 1) {
            return bestCandidate;
        }

        FieldValueGroup<T> secondGroup = groups.get(1);
        boolean sameRanking = bestGroup.bestConfidenceScore() == secondGroup.bestConfidenceScore()
                && bestGroup.occurrences() == secondGroup.occurrences();
        if (!sameRanking) {
            return bestCandidate;
        }

        return new ItemImportFieldCandidate<>(
                bestCandidate.getRawValue(),
                bestCandidate.getSuggestedValue(),
                lowerConfidence(bestCandidate.getConfidence()),
                bestCandidate.getNote() + " Równorzędne warianty OCR dały sprzeczne wartości, więc pole zostało zachowane z obniżoną pewnością."
        );
    }

    private static <T> List<FieldValueGroup<T>> buildGroups(List<ItemImportFieldCandidate<T>> candidates) {
        List<FieldValueGroup<T>> groups = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            ItemImportFieldCandidate<T> candidate = candidates.get(index);
            FieldValueGroup<T> existingGroup = findGroup(groups, candidate.getSuggestedValue());
            if (existingGroup == null) {
                groups.add(new FieldValueGroup<>(
                        candidate.getSuggestedValue(),
                        candidate,
                        confidenceScore(candidate.getConfidence()),
                        1,
                        index
                ));
                continue;
            }

            ItemImportFieldCandidate<T> bestCandidate = existingGroup.bestCandidate();
            int bestConfidenceScore = existingGroup.bestConfidenceScore();
            if (confidenceScore(candidate.getConfidence()) > bestConfidenceScore) {
                bestCandidate = candidate;
                bestConfidenceScore = confidenceScore(candidate.getConfidence());
            }
            groups.set(groups.indexOf(existingGroup), new FieldValueGroup<>(
                    existingGroup.suggestedValue(),
                    bestCandidate,
                    bestConfidenceScore,
                    existingGroup.occurrences() + 1,
                    existingGroup.firstIndex()
            ));
        }
        return groups;
    }

    private static <T> FieldValueGroup<T> findGroup(List<FieldValueGroup<T>> groups, T suggestedValue) {
        for (FieldValueGroup<T> group : groups) {
            if (Objects.equals(group.suggestedValue(), suggestedValue)) {
                return group;
            }
        }
        return null;
    }

    private static int confidenceScore(ItemImportFieldConfidence confidence) {
        return switch (confidence) {
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
            case UNKNOWN -> 0;
        };
    }

    private static ItemImportFieldConfidence lowerConfidence(ItemImportFieldConfidence confidence) {
        return switch (confidence) {
            case HIGH -> ItemImportFieldConfidence.MEDIUM;
            case MEDIUM -> ItemImportFieldConfidence.LOW;
            case LOW, UNKNOWN -> ItemImportFieldConfidence.UNKNOWN;
        };
    }

    private static String buildImportNotice(int analyzedVariantCount,
                                            ItemImportFieldCandidate<?>... candidates) {
        long recognizedCount = 0L;
        for (ItemImportFieldCandidate<?> candidate : candidates) {
            if (candidate.getSuggestedValue() != null) {
                recognizedCount++;
            }
        }
        if (recognizedCount == 0L) {
            return "OCR nie rozpoznał czytelnego tekstu z obrazu nawet po analizie "
                    + analyzedVariantCount
                    + " wariantów. Import nadal wymaga ręcznego potwierdzenia wszystkich pól.";
        }
        return "OCR rozpoznał " + recognizedCount + " z 7 pól foundation po analizie "
                + analyzedVariantCount
                + " wariantów obrazu. Wynik nadal wymaga ręcznego potwierdzenia użytkownika.";
    }

    private record FieldValueGroup<T>(T suggestedValue,
                                      ItemImportFieldCandidate<T> bestCandidate,
                                      int bestConfidenceScore,
                                      int occurrences,
                                      int firstIndex) {
    }

    private enum DetailTextField {
        ITEM_NAME,
        ITEM_TYPE,
        ITEM_RARITY,
        UNIQUE_EFFECT_TEXT
    }

    private enum DetailLongField {
        ITEM_POWER,
        WEAPON_DPS,
        WEAPON_DAMAGE_MIN,
        WEAPON_DAMAGE_MAX,
        AVERAGE_WEAPON_DAMAGE,
        ITEM_ARMOR
    }
}
