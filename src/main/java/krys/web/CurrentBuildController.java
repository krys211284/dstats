package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;
import krys.app.CurrentBuildRequest;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Kontroler HTTP dla pierwszego klikalnego GUI M4. */
public final class CurrentBuildController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final CurrentBuildCalculationService calculationService;
    private final CurrentBuildPageRenderer renderer;

    public CurrentBuildController(CurrentBuildCalculationService calculationService,
                                  CurrentBuildPageRenderer renderer) {
        this.calculationService = calculationService;
        this.renderer = renderer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                renderPage(exchange, buildPageModel(CurrentBuildFormData.defaultValues(), List.of(), null));
                return;
            }
            if ("POST".equals(method)) {
                renderPage(exchange, handlePost(exchange));
                return;
            }
            exchange.getResponseHeaders().set("Allow", "GET, POST");
            exchange.sendResponseHeaders(405, -1);
        } finally {
            exchange.close();
        }
    }

    private CurrentBuildPageModel handlePost(HttpExchange exchange) throws IOException {
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(parseUrlEncodedBody(exchange));
        List<String> errors = new ArrayList<>();
        CurrentBuildCalculation calculation = tryCalculate(formData, errors);
        return buildPageModel(formData, errors, calculation);
    }

    private CurrentBuildCalculation tryCalculate(CurrentBuildFormData formData, List<String> errors) {
        SkillId skillId = parseSkillId(formData.getSkillId(), errors);
        Integer rank = parseRank(formData.getRank(), errors);
        SkillUpgradeChoice choiceUpgrade = parseChoiceUpgrade(formData.getChoiceUpgrade(), errors);
        Integer horizonSeconds = parseHorizon(formData.getHorizonSeconds(), errors);

        if (skillId != null && choiceUpgrade != null) {
            validateChoiceForSkill(skillId, choiceUpgrade, formData.isBaseUpgrade(), errors);
        }

        if (!errors.isEmpty() || skillId == null || rank == null || choiceUpgrade == null || horizonSeconds == null) {
            return null;
        }

        try {
            return calculationService.calculate(new CurrentBuildRequest(
                    skillId,
                    rank,
                    formData.isBaseUpgrade(),
                    choiceUpgrade,
                    horizonSeconds
            ));
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
            return null;
        }
    }

    private CurrentBuildPageModel buildPageModel(CurrentBuildFormData formData,
                                                 List<String> errors,
                                                 CurrentBuildCalculation calculation) {
        return new CurrentBuildPageModel(
                formData,
                buildSkillOptions(formData),
                buildRankOptions(formData),
                buildChoiceOptions(formData),
                errors,
                calculation,
                buildChoiceHelpText(formData)
        );
    }

    private static List<CurrentBuildPageModel.SelectOption> buildSkillOptions(CurrentBuildFormData formData) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            options.add(new CurrentBuildPageModel.SelectOption(
                    skillId.name(),
                    PaladinSkillDefs.get(skillId).getName(),
                    skillId.name().equals(formData.getSkillId())
            ));
        }
        return options;
    }

    private static List<CurrentBuildPageModel.SelectOption> buildRankOptions(CurrentBuildFormData formData) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        for (int rank = 1; rank <= 5; rank++) {
            String rankValue = Integer.toString(rank);
            options.add(new CurrentBuildPageModel.SelectOption(
                    rankValue,
                    rankValue,
                    rankValue.equals(formData.getRank())
            ));
        }
        return options;
    }

    private static List<CurrentBuildPageModel.SelectOption> buildChoiceOptions(CurrentBuildFormData formData) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        for (SkillUpgradeChoice choiceUpgrade : PaladinSkillDefs.getFoundationChoiceUpgrades()) {
            options.add(new CurrentBuildPageModel.SelectOption(
                    choiceUpgrade.name(),
                    toChoiceLabel(choiceUpgrade),
                    choiceUpgrade.name().equals(formData.getChoiceUpgrade())
            ));
        }
        return options;
    }

    private static String toChoiceLabel(SkillUpgradeChoice choiceUpgrade) {
        return switch (choiceUpgrade) {
            case NONE -> "Brak";
            case LEFT -> "Powrót światłości (Brandish)";
            case RIGHT -> "Krzyżowe uderzenie (Vulnerable) (Brandish)";
            case MIDDLE -> "Miecz Mistrzostwa";
        };
    }

    private static String buildChoiceHelpText(CurrentBuildFormData formData) {
        SkillId skillId = tryParseSkillId(formData.getSkillId());
        if (skillId == SkillId.HOLY_BOLT) {
            return "Dla Holy Bolt w aktualnym foundation dodatkowy modyfikator pozostaje ustawiony na „Brak”.";
        }
        return "Aktualne dodatkowe modyfikatory foundation dotyczą Brandish. Bazowe rozszerzenie musi być włączone, aby użyć dodatkowego modyfikatora.";
    }

    private static SkillId parseSkillId(String rawSkillId, List<String> errors) {
        SkillId skillId = tryParseSkillId(rawSkillId);
        if (skillId == null) {
            errors.add("Wybrany skill nie należy do aktualnego foundation.");
        }
        return skillId;
    }

    private static SkillId tryParseSkillId(String rawSkillId) {
        try {
            return SkillId.valueOf(rawSkillId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    private static Integer parseRank(String rawRank, List<String> errors) {
        try {
            int rank = Integer.parseInt(rawRank);
            if (rank < 1 || rank > 5) {
                errors.add("Rank skilla w GUI M4 musi mieścić się w zakresie 1..5.");
                return null;
            }
            return rank;
        } catch (NumberFormatException exception) {
            errors.add("Rank skilla musi być liczbą całkowitą.");
            return null;
        }
    }

    private static SkillUpgradeChoice parseChoiceUpgrade(String rawChoiceUpgrade, List<String> errors) {
        try {
            return SkillUpgradeChoice.valueOf(rawChoiceUpgrade);
        } catch (IllegalArgumentException | NullPointerException exception) {
            errors.add("Wybrany dodatkowy modyfikator nie należy do aktualnego foundation.");
            return null;
        }
    }

    private static Integer parseHorizon(String rawHorizonSeconds, List<String> errors) {
        try {
            int horizonSeconds = Integer.parseInt(rawHorizonSeconds);
            if (horizonSeconds <= 0) {
                errors.add("Horyzont symulacji musi być dodatni.");
                return null;
            }
            return horizonSeconds;
        } catch (NumberFormatException exception) {
            errors.add("Horyzont symulacji musi być liczbą całkowitą.");
            return null;
        }
    }

    private static void validateChoiceForSkill(SkillId skillId,
                                               SkillUpgradeChoice choiceUpgrade,
                                               boolean baseUpgrade,
                                               List<String> errors) {
        if (!baseUpgrade && choiceUpgrade != SkillUpgradeChoice.NONE) {
            errors.add("Dodatkowy modyfikator wymaga włączenia bazowego rozszerzenia.");
            return;
        }

        Set<SkillUpgradeChoice> validChoices = new LinkedHashSet<>();
        validChoices.add(SkillUpgradeChoice.NONE);
        validChoices.addAll(PaladinSkillDefs.get(skillId).getAvailableChoiceUpgrades());
        if (!validChoices.contains(choiceUpgrade)) {
            errors.add("Wybrany dodatkowy modyfikator nie jest dostępny dla wskazanego skilla w aktualnym foundation.");
        }
    }

    private static Map<String, String> parseUrlEncodedBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> fields = new LinkedHashMap<>();
        if (body.isBlank()) {
            return fields;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            String[] keyValue = pair.split("=", 2);
            String key = decodeUrlPart(keyValue[0]);
            String value = keyValue.length > 1 ? decodeUrlPart(keyValue[1]) : "";
            fields.put(key, value);
        }
        return fields;
    }

    private static String decodeUrlPart(String rawValue) {
        return URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
    }

    private void renderPage(HttpExchange exchange, CurrentBuildPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}
