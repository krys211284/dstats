package krys.search;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Parser CLI dla backendowego searcha M9. */
final class BuildSearchCliRequestParser {
    private BuildSearchCliRequestParser() {
    }

    static BuildSearchRequest parse(String[] args) {
        BuildSearchRequest referenceRequest = BuildSearchReferenceRequests.createFoundationM9();

        List<Integer> levelValues = new ArrayList<>(referenceRequest.getLevelValues());
        List<Long> weaponDamageValues = new ArrayList<>(referenceRequest.getWeaponDamageValues());
        List<Double> strengthValues = new ArrayList<>(referenceRequest.getStrengthValues());
        List<Double> intelligenceValues = new ArrayList<>(referenceRequest.getIntelligenceValues());
        List<Double> thornsValues = new ArrayList<>(referenceRequest.getThornsValues());
        List<Double> blockChanceValues = new ArrayList<>(referenceRequest.getBlockChanceValues());
        List<Double> retributionChanceValues = new ArrayList<>(referenceRequest.getRetributionChanceValues());
        int horizonSeconds = referenceRequest.getHorizonSeconds();
        int topResultsLimit = referenceRequest.getTopResultsLimit();
        Map<SkillId, SkillSearchConfigBuilder> skillBuilders = createSkillBuilders(referenceRequest.getSkillSpaces());
        List<Integer> actionBarSizes = new ArrayList<>(referenceRequest.getActionBarSizes());

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if ("--reference".equals(arg) && index + 1 < args.length) {
                BuildSearchRequest preset = BuildSearchReferenceRequests.create(
                        BuildSearchReferenceScenario.valueOf(args[++index].toUpperCase(Locale.ROOT))
                );
                levelValues = new ArrayList<>(preset.getLevelValues());
                weaponDamageValues = new ArrayList<>(preset.getWeaponDamageValues());
                strengthValues = new ArrayList<>(preset.getStrengthValues());
                intelligenceValues = new ArrayList<>(preset.getIntelligenceValues());
                thornsValues = new ArrayList<>(preset.getThornsValues());
                blockChanceValues = new ArrayList<>(preset.getBlockChanceValues());
                retributionChanceValues = new ArrayList<>(preset.getRetributionChanceValues());
                horizonSeconds = preset.getHorizonSeconds();
                topResultsLimit = preset.getTopResultsLimit();
                actionBarSizes = new ArrayList<>(preset.getActionBarSizes());
                skillBuilders = createSkillBuilders(preset.getSkillSpaces());
            } else if ("--level-values".equals(arg) && index + 1 < args.length) {
                levelValues = parseIntegerList(args[++index]);
            } else if ("--weapon-damage-values".equals(arg) && index + 1 < args.length) {
                weaponDamageValues = parseLongList(args[++index]);
            } else if ("--strength-values".equals(arg) && index + 1 < args.length) {
                strengthValues = parseDoubleList(args[++index]);
            } else if ("--intelligence-values".equals(arg) && index + 1 < args.length) {
                intelligenceValues = parseDoubleList(args[++index]);
            } else if ("--thorns-values".equals(arg) && index + 1 < args.length) {
                thornsValues = parseDoubleList(args[++index]);
            } else if ("--block-chance-values".equals(arg) && index + 1 < args.length) {
                blockChanceValues = parseDoubleList(args[++index]);
            } else if ("--retribution-chance-values".equals(arg) && index + 1 < args.length) {
                retributionChanceValues = parseDoubleList(args[++index]);
            } else if ("--bar-sizes".equals(arg) && index + 1 < args.length) {
                actionBarSizes = parseIntegerList(args[++index]);
            } else if ("--seconds".equals(arg) && index + 1 < args.length) {
                horizonSeconds = Integer.parseInt(args[++index]);
            } else if ("--top".equals(arg) && index + 1 < args.length) {
                topResultsLimit = Integer.parseInt(args[++index]);
            } else {
                index = tryConsumeSkillArgument(args, index, arg, skillBuilders);
            }
        }

        Map<SkillId, BuildSearchSkillSpace> skillSpaces = new EnumMap<>(SkillId.class);
        for (SkillSearchConfigBuilder builder : skillBuilders.values()) {
            skillSpaces.put(builder.skillId, new BuildSearchSkillSpace(
                    builder.skillId,
                    builder.rankValues,
                    builder.baseUpgradeValues,
                    builder.choiceUpgradeValues
            ));
        }

        return new BuildSearchRequest(
                levelValues,
                weaponDamageValues,
                strengthValues,
                intelligenceValues,
                thornsValues,
                blockChanceValues,
                retributionChanceValues,
                skillSpaces,
                actionBarSizes,
                horizonSeconds,
                topResultsLimit
        );
    }

    private static int tryConsumeSkillArgument(String[] args,
                                               int index,
                                               String arg,
                                               Map<SkillId, SkillSearchConfigBuilder> skillBuilders) {
        for (SkillId skillId : SkillId.values()) {
            String prefix = "--" + toCliKey(skillId) + "-";
            if (!arg.startsWith(prefix) || index + 1 >= args.length) {
                continue;
            }

            SkillSearchConfigBuilder builder = skillBuilders.get(skillId);
            if ((prefix + "ranks").equals(arg)) {
                builder.rankValues = parseIntegerList(args[++index]);
                return index;
            }
            if ((prefix + "base-upgrades").equals(arg)) {
                builder.baseUpgradeValues = parseBooleanList(args[++index]);
                return index;
            }
            if ((prefix + "choices").equals(arg)) {
                builder.choiceUpgradeValues = parseChoiceList(args[++index]);
                return index;
            }
        }
        return index;
    }

    private static Map<SkillId, SkillSearchConfigBuilder> createSkillBuilders(Map<SkillId, BuildSearchSkillSpace> skillSpaces) {
        Map<SkillId, SkillSearchConfigBuilder> builders = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            BuildSearchSkillSpace skillSpace = skillSpaces.get(skillId);
            builders.put(skillId, new SkillSearchConfigBuilder(
                    skillId,
                    new ArrayList<>(skillSpace.getRankValues()),
                    new ArrayList<>(skillSpace.getBaseUpgradeValues()),
                    new ArrayList<>(skillSpace.getChoiceUpgradeValues())
            ));
        }
        return builders;
    }

    private static List<Integer> parseIntegerList(String rawValues) {
        List<Integer> values = new ArrayList<>();
        for (String token : splitList(rawValues)) {
            values.add(Integer.parseInt(token));
        }
        return values;
    }

    private static List<Long> parseLongList(String rawValues) {
        List<Long> values = new ArrayList<>();
        for (String token : splitList(rawValues)) {
            values.add(Long.parseLong(token));
        }
        return values;
    }

    private static List<Double> parseDoubleList(String rawValues) {
        List<Double> values = new ArrayList<>();
        for (String token : splitList(rawValues)) {
            values.add(Double.parseDouble(token));
        }
        return values;
    }

    private static List<Boolean> parseBooleanList(String rawValues) {
        List<Boolean> values = new ArrayList<>();
        for (String token : splitList(rawValues)) {
            values.add(Boolean.parseBoolean(token));
        }
        return values;
    }

    private static List<SkillUpgradeChoice> parseChoiceList(String rawValues) {
        List<SkillUpgradeChoice> values = new ArrayList<>();
        for (String token : splitList(rawValues)) {
            values.add(SkillUpgradeChoice.valueOf(token.toUpperCase(Locale.ROOT)));
        }
        return values;
    }

    private static List<String> splitList(String rawValues) {
        if (rawValues == null || rawValues.isBlank()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (String token : rawValues.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private static String toCliKey(SkillId skillId) {
        return switch (skillId) {
            case BRANDISH -> "brandish";
            case HOLY_BOLT -> "holy-bolt";
            case CLASH -> "clash";
            case ADVANCE -> "advance";
        };
    }

    private static final class SkillSearchConfigBuilder {
        private final SkillId skillId;
        private List<Integer> rankValues;
        private List<Boolean> baseUpgradeValues;
        private List<SkillUpgradeChoice> choiceUpgradeValues;

        private SkillSearchConfigBuilder(SkillId skillId,
                                         List<Integer> rankValues,
                                         List<Boolean> baseUpgradeValues,
                                         List<SkillUpgradeChoice> choiceUpgradeValues) {
            this.skillId = skillId;
            this.rankValues = rankValues;
            this.baseUpgradeValues = baseUpgradeValues;
            this.choiceUpgradeValues = choiceUpgradeValues;
        }
    }
}
