package krys.app;

import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Parser CLI dla realnego modelu wejścia buildu M8. */
final class CurrentBuildCliRequestParser {
    private CurrentBuildCliRequestParser() {
    }

    static CurrentBuildRequest parse(String[] args) {
        CurrentBuildRequest referenceRequest = CurrentBuildReferenceRequests.createAdvanceFlash();

        int level = referenceRequest.getLevel();
        long weaponDamage = referenceRequest.getWeaponDamage();
        double strength = referenceRequest.getStrength();
        double intelligence = referenceRequest.getIntelligence();
        double thorns = referenceRequest.getThorns();
        double blockChance = referenceRequest.getBlockChance();
        double retributionChance = referenceRequest.getRetributionChance();
        int horizonSeconds = referenceRequest.getHorizonSeconds();

        Map<SkillId, SkillConfigBuilder> skillBuilders = createSkillBuilders(referenceRequest.getLearnedSkills());
        List<SkillId> actionBar = new ArrayList<>(referenceRequest.getActionBar());

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if ("--reference".equals(arg) && index + 1 < args.length) {
                CurrentBuildRequest preset = CurrentBuildReferenceRequests.create(
                        CurrentBuildReferenceScenario.valueOf(args[++index].toUpperCase(Locale.ROOT))
                );
                level = preset.getLevel();
                weaponDamage = preset.getWeaponDamage();
                strength = preset.getStrength();
                intelligence = preset.getIntelligence();
                thorns = preset.getThorns();
                blockChance = preset.getBlockChance();
                retributionChance = preset.getRetributionChance();
                horizonSeconds = preset.getHorizonSeconds();
                skillBuilders = createSkillBuilders(preset.getLearnedSkills());
                actionBar = new ArrayList<>(preset.getActionBar());
            } else if ("--level".equals(arg) && index + 1 < args.length) {
                level = Integer.parseInt(args[++index]);
            } else if ("--weapon-damage".equals(arg) && index + 1 < args.length) {
                weaponDamage = Long.parseLong(args[++index]);
            } else if ("--strength".equals(arg) && index + 1 < args.length) {
                strength = Double.parseDouble(args[++index]);
            } else if ("--intelligence".equals(arg) && index + 1 < args.length) {
                intelligence = Double.parseDouble(args[++index]);
            } else if ("--thorns".equals(arg) && index + 1 < args.length) {
                thorns = Double.parseDouble(args[++index]);
            } else if ("--block-chance".equals(arg) && index + 1 < args.length) {
                blockChance = Double.parseDouble(args[++index]);
            } else if ("--retribution-chance".equals(arg) && index + 1 < args.length) {
                retributionChance = Double.parseDouble(args[++index]);
            } else if ("--seconds".equals(arg) && index + 1 < args.length) {
                horizonSeconds = Integer.parseInt(args[++index]);
            } else if ("--action-bar".equals(arg) && index + 1 < args.length) {
                actionBar = parseActionBar(args[++index]);
            } else {
                index = tryConsumeSkillArgument(args, index, arg, skillBuilders);
            }
        }

        return new CurrentBuildRequest(
                level,
                weaponDamage,
                strength,
                intelligence,
                thorns,
                blockChance,
                retributionChance,
                buildLearnedSkills(skillBuilders),
                actionBar,
                horizonSeconds
        );
    }

    private static int tryConsumeSkillArgument(String[] args,
                                               int index,
                                               String arg,
                                               Map<SkillId, SkillConfigBuilder> skillBuilders) {
        for (SkillId skillId : SkillId.values()) {
            String prefix = "--" + toCliKey(skillId) + "-";
            if (!arg.startsWith(prefix) || index + 1 >= args.length) {
                continue;
            }

            SkillConfigBuilder builder = skillBuilders.computeIfAbsent(skillId, ignored -> new SkillConfigBuilder(skillId));
            if ((prefix + "rank").equals(arg)) {
                builder.rank = Integer.parseInt(args[++index]);
                return index;
            }
            if ((prefix + "base-upgrade").equals(arg)) {
                builder.baseUpgrade = Boolean.parseBoolean(args[++index]);
                return index;
            }
            if ((prefix + "choice").equals(arg)) {
                builder.choiceUpgrade = SkillUpgradeChoice.valueOf(args[++index].toUpperCase(Locale.ROOT));
                return index;
            }
        }
        return index;
    }

    private static Map<SkillId, SkillConfigBuilder> createSkillBuilders(Map<SkillId, SkillState> learnedSkills) {
        Map<SkillId, SkillConfigBuilder> builders = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            SkillState state = learnedSkills.get(skillId);
            SkillConfigBuilder builder = new SkillConfigBuilder(skillId);
            if (state != null) {
                builder.rank = state.getRank();
                builder.baseUpgrade = state.isBaseUpgrade();
                builder.choiceUpgrade = state.getChoiceUpgrade();
            }
            builders.put(skillId, builder);
        }
        return builders;
    }

    private static Map<SkillId, SkillState> buildLearnedSkills(Map<SkillId, SkillConfigBuilder> skillBuilders) {
        Map<SkillId, SkillState> learnedSkills = new EnumMap<>(SkillId.class);
        for (SkillConfigBuilder builder : skillBuilders.values()) {
            if (builder.rank <= 0) {
                continue;
            }
            learnedSkills.put(builder.skillId, new SkillState(
                    builder.skillId,
                    builder.rank,
                    builder.baseUpgrade,
                    builder.choiceUpgrade
            ));
        }
        return learnedSkills;
    }

    private static List<SkillId> parseActionBar(String rawActionBar) {
        if (rawActionBar == null || rawActionBar.isBlank()) {
            return List.of();
        }
        List<SkillId> actionBar = new ArrayList<>();
        for (String token : rawActionBar.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty() || "NONE".equalsIgnoreCase(trimmed)) {
                continue;
            }
            actionBar.add(SkillId.valueOf(trimmed.toUpperCase(Locale.ROOT)));
        }
        return actionBar;
    }

    private static String toCliKey(SkillId skillId) {
        return switch (skillId) {
            case BRANDISH -> "brandish";
            case HOLY_BOLT -> "holy-bolt";
            case CLASH -> "clash";
            case ADVANCE -> "advance";
        };
    }

    private static final class SkillConfigBuilder {
        private final SkillId skillId;
        private int rank;
        private boolean baseUpgrade;
        private SkillUpgradeChoice choiceUpgrade = SkillUpgradeChoice.NONE;

        private SkillConfigBuilder(SkillId skillId) {
            this.skillId = skillId;
        }
    }
}
