package krys.paladin;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaladinDocumentationTest {
    private static final Path SOURCE_MD = Path.of("docs/paladin/source-md");
    private static final Path RANK_TABLES_JSON = SOURCE_MD.resolve("paladin_fextralife_rank_tables.json");
    private static final List<String> REQUIRED_SOURCE_MD_FILES = List.of(
            "paladin_basic_skill_registry_final.md",
            "paladin_core_skill_registry_final.md",
            "paladin_aura_skill_registry_final.md",
            "diablo4_paladyn_odwaga_umiejetnosci.md",
            "diablo4_paladyn_sprawiedliwosc_umiejetnosci.md",
            "moce_specjalne_diablo4.md",
            "paladin_fextralife_rank_tables.md",
            "paladin_fextralife_rank_tables.json",
            "paladin_fextralife_html_manifest.md"
    );

    @Test
    void source_md_powinien_istniec_z_wymaganymi_edytowalnymi_materialami() {
        assertTrue(Files.isDirectory(SOURCE_MD));

        for (String fileName : REQUIRED_SOURCE_MD_FILES) {
            assertTrue(Files.isRegularFile(SOURCE_MD.resolve(fileName)), fileName);
        }
    }

    @Test
    void source_md_readme_i_shasums_nie_sa_wymaganymi_plikami() {
        assertFalse(REQUIRED_SOURCE_MD_FILES.contains("README.md"));
        assertFalse(REQUIRED_SOURCE_MD_FILES.contains("SHASUMS.txt"));
    }

    @Test
    void fextralife_json_powinien_byc_poprawnym_jsonem_z_tabela_rang_blogoslawionego_mlota() throws IOException {
        Map<String, Object> root = object(readJson(RANK_TABLES_JSON));
        List<Object> skills = array(root.get("skills"));

        assertEquals(24, skills.size());

        Map<String, Object> blessedHammer = skills.stream()
                .map(PaladinDocumentationTest::object)
                .filter(skill -> "blogoslawiony_mlot".equals(skill.get("skillId")))
                .findFirst()
                .orElse(null);
        assertNotNull(blessedHammer);

        List<Object> rankTable = array(blessedHammer.get("rankTable"));
        assertEquals(15, rankTable.size());

        Set<Integer> ranks = rankTable.stream()
                .map(PaladinDocumentationTest::object)
                .map(rankEntry -> ((Number) rankEntry.get("rank")).intValue())
                .collect(Collectors.toSet());
        assertEquals(IntStream.rangeClosed(1, 15).boxed().collect(Collectors.toSet()), ranks);

        assertEquals(115, firstBracketPercentForRank(rankTable, 1));
        assertEquals(293, firstBracketPercentForRank(rankTable, 15));
    }

    @Test
    void readme_powinny_opisywac_nowy_kontrakt_source_md_json_i_blokady_runtime_dps() throws IOException {
        String docsReadme = readText(Path.of("docs/paladin/README.md")).toLowerCase();
        String mainReadme = readText(Path.of("README.md")).toLowerCase();
        String combined = docsReadme + "\n" + mainReadme;

        assertTrue(combined.contains("source-md/"));
        assertTrue(combined.contains("edytowalnym źródłem") || combined.contains("edytowalne źródło"));
        assertTrue(combined.contains("source-pdfs/"));
        assertTrue(combined.contains("archiwum"));
        assertTrue(combined.contains("porównawcze") || combined.contains("porównania"));
        assertTrue(combined.contains("paladin_fextralife_rank_tables.json"));
        assertTrue(combined.contains("pomocniczym") || combined.contains("pomocniczy"));
        assertTrue(combined.contains("ekstrakt"));
        assertTrue(combined.contains("runtime dps"));
        assertTrue(combined.contains("nie odblokowuje") || combined.contains("nie odblokowują"));
    }

    private static int firstBracketPercentForRank(List<Object> rankTable, int rank) {
        Map<String, Object> rankEntry = rankTable.stream()
                .map(PaladinDocumentationTest::object)
                .filter(entry -> ((Number) entry.get("rank")).intValue() == rank)
                .findFirst()
                .orElseThrow();
        return ((Number) array(rankEntry.get("bracketPercents")).get(0)).intValue();
    }

    private static Object readJson(Path path) throws IOException {
        return new JsonParser(readText(path)).parse();
    }

    private static String readText(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> object(Object value) {
        return assertInstanceOf(Map.class, value);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> array(Object value) {
        return assertInstanceOf(List.class, value);
    }

    private static final class JsonParser {
        private final String text;
        private int index;

        private JsonParser(String text) {
            this.text = text;
        }

        private Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw new IllegalArgumentException("Nadmiarowe znaki JSON od pozycji " + index);
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            char current = peek();
            if (current == '{') {
                return parseObject();
            }
            if (current == '[') {
                return parseArray();
            }
            if (current == '"') {
                return parseString();
            }
            if (current == 't') {
                consumeLiteral("true");
                return Boolean.TRUE;
            }
            if (current == 'f') {
                consumeLiteral("false");
                return Boolean.FALSE;
            }
            if (current == 'n') {
                consumeLiteral("null");
                return null;
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (tryConsume('}')) {
                return object;
            }
            do {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
            } while (tryConsume(','));
            expect('}');
            return object;
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> array = new ArrayList<>();
            skipWhitespace();
            if (tryConsume(']')) {
                return array;
            }
            do {
                array.add(parseValue());
                skipWhitespace();
            } while (tryConsume(','));
            expect(']');
            return array;
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < text.length()) {
                char current = text.charAt(index++);
                if (current == '"') {
                    return result.toString();
                }
                if (current == '\\') {
                    result.append(parseEscapedCharacter());
                } else {
                    result.append(current);
                }
            }
            throw new IllegalArgumentException("Nie zamknięto stringa JSON");
        }

        private char parseEscapedCharacter() {
            char escaped = text.charAt(index++);
            return switch (escaped) {
                case '"' -> '"';
                case '\\' -> '\\';
                case '/' -> '/';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> parseUnicodeEscape();
                default -> throw new IllegalArgumentException("Nieobsługiwana sekwencja escape JSON: \\" + escaped);
            };
        }

        private char parseUnicodeEscape() {
            String hex = text.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Number parseNumber() {
            int start = index;
            if (tryConsume('-')) {
                // Minus jest częścią liczby; właściwe cyfry są czytane niżej.
            }
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            boolean decimal = false;
            if (index < text.length() && text.charAt(index) == '.') {
                decimal = true;
                index++;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                decimal = true;
                index++;
                if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                    index++;
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            String number = text.substring(start, index);
            if (number.isBlank() || "-".equals(number)) {
                throw new IllegalArgumentException("Niepoprawna liczba JSON od pozycji " + start);
            }
            return decimal ? Double.parseDouble(number) : Integer.parseInt(number);
        }

        private void consumeLiteral(String literal) {
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("Oczekiwano literału JSON " + literal + " od pozycji " + index);
            }
            index += literal.length();
        }

        private void expect(char expected) {
            skipWhitespace();
            if (!tryConsume(expected)) {
                throw new IllegalArgumentException("Oczekiwano '" + expected + "' na pozycji " + index);
            }
        }

        private boolean tryConsume(char expected) {
            if (index < text.length() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private char peek() {
            if (index >= text.length()) {
                throw new IllegalArgumentException("Nieoczekiwany koniec JSON");
            }
            return text.charAt(index);
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }
    }
}
