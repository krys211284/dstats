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

        for (ItemImageImportCandidateParseResult parseResult : parseResults) {
            FullItemRead read = parseResult.getFullItemRead();
            if (read == null || !read.hasAnyData()) {
                continue;
            }
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
                if (existingLine == null || lineQualityScore(line) > lineQualityScore(existingLine)) {
                    mergedLines.put(key, line);
                }
            }
        }

        return new FullItemRead(itemName, itemTypeLine, rarity, itemPower, baseItemValue, List.copyOf(mergedLines.values()));
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
        if (normalized.contains("CIERNI")) {
            return "AFFIX:THORNS:" + firstNumber(normalized);
        }
        if (normalized.contains("SZCZESLIWY TRAF")) {
            return "AFFIX:LUCKY_HIT:" + firstNumber(normalized);
        }
        if (normalized.contains("CZASU ODNOWIENIA")) {
            return "AFFIX:COOLDOWN_REDUCTION:" + firstNumber(normalized);
        }
        if (normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")) {
            return "ASPECT:DAMAGE_WHILE_STANDING";
        }
        if (normalized.contains("TA PREMIA JEST TRZY RAZY WIEKSZA")) {
            return "ASPECT:STANDING_STILL_MULTIPLIER";
        }
        if (normalized.contains("ROZJUSZENIE")) {
            return "SEASONAL:ROZJUSZENIE:" + firstNumber(normalized);
        }
        return type.name() + ":" + normalized.replaceAll("\\s+", " ").trim();
    }

    private static int lineQualityScore(FullItemReadLine line) {
        String text = line.getText();
        int score = text.length();
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
}
