package krys.itemimport.image;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Typowane, pełne snapshoty OCR; odczyt fixture nigdy nie uruchamia Tesseracta. */
public final class OcrGoldenFixtures {
    private static final String VERSION = "1";
    private static final Map<Class<?>, List<String>> FIELD_ORDER = Map.of(
            PropertyBlock.class, List.of("sourcePart", "order", "x", "y", "width", "height",
                    "anchorType", "anchor", "format", "imageBytes", "diagnostics"),
            PropertyBlockAnchor.class, List.of("type", "x", "y", "width", "height", "confidence"),
            PropertyBlockReadResult.class, List.of("sourcePart", "blockOrder", "anchorType", "rawText",
                    "durationMillis", "preprocessing", "failureDiagnostics", "numericEvidence", "profileResults"),
            PropertyBlockNumericEvidence.class, List.of("tokenOrder", "selectedValue", "supportingProfiles",
                    "alternativeValues", "supportCount", "ambiguous"),
            PropertyBlockOcrProfileResult.class, List.of("profile", "rawText", "numericTokens", "score", "failure"),
            Part1RoiOcrResult.class, List.of("type", "rawText", "durationMillis", "preprocessing",
                    "subRegions", "greaterAffixCount"),
            Part1RoiOcrSubRegion.class, List.of("type", "x", "y", "width", "height", "format", "imageBytes", "preprocessing"));
    private static final Set<Class<?>> MODELS = FIELD_ORDER.keySet();

    private OcrGoldenFixtures() {
    }

    public record BlockRead(PropertyBlock block, PropertyBlockReadResult read) {
    }

    public static List<BlockRead> blocks(List<String> filenames, StructuralItemPartSlot slot) {
        Properties data = load(key(filenames) + "-" + slot + ".xml");
        List<BlockRead> results = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(required(data, "count")); i++) {
            results.add(new BlockRead(
                    (PropertyBlock) decode(data, "block." + i, PropertyBlock.class),
                    (PropertyBlockReadResult) decode(data, "read." + i, PropertyBlockReadResult.class)));
        }
        return List.copyOf(results);
    }

    public static List<Part1RoiOcrResult> part1(String... filenames) {
        Properties data = load(key(List.of(filenames)) + "-PART_1.xml");
        List<Part1RoiOcrResult> results = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(required(data, "count")); i++) {
            results.add((Part1RoiOcrResult) decode(data, "read." + i, Part1RoiOcrResult.class));
        }
        return List.copyOf(results);
    }

    // Porównujemy wszystkie stabilne pola, łącznie z obrazami i evidence, poza czasem pomiaru.
    public static void assertSameRead(Object expected, Object actual) {
        Properties first = properties();
        Properties second = properties();
        encode(first, "read", expected);
        encode(second, "read", actual);
        first.keySet().removeIf(key -> key.toString().endsWith(".durationMillis"));
        second.keySet().removeIf(key -> key.toString().endsWith(".durationMillis"));
        assertEquals(first, second);
    }

    public static void captureBlock(List<String> filenames, PropertyBlock block, PropertyBlockReadResult read) {
        if (!Boolean.getBoolean("dstats.captureOcrFixtures")) {
            return;
        }
        String name = key(filenames) + "-" + block.getSourcePart() + ".xml";
        Path path = Path.of("src/test/resources/ocr", name);
        Properties data = Files.exists(path) ? load(name) : properties();
        int index = block.getOrder() - 1;
        data.setProperty("count", Integer.toString(Math.max(index + 1,
                Integer.parseInt(data.getProperty("count", "0")))));
        encode(data, "block." + index, block);
        encode(data, "read." + index, read);
        save(name, data);
    }

    public static void capturePart1(List<String> filenames, List<Part1RoiOcrResult> reads) {
        if (!Boolean.getBoolean("dstats.captureOcrFixtures")) {
            return;
        }
        Properties data = properties();
        data.setProperty("count", Integer.toString(reads.size()));
        for (int i = 0; i < reads.size(); i++) {
            encode(data, "read." + i, reads.get(i));
        }
        save(key(filenames) + "-PART_1.xml", data);
    }

    private static String key(List<String> filenames) {
        return String.join("+", filenames).replace(".png", "");
    }

    private static Properties properties() {
        Properties result = new Properties() {
            @Override
            public Set<Map.Entry<Object, Object>> entrySet() {
                TreeMap<Object, Object> sorted = new TreeMap<>();
                for (Map.Entry<Object, Object> entry : super.entrySet()) {
                    sorted.put(entry.getKey(), entry.getValue());
                }
                return new LinkedHashSet<>(sorted.entrySet());
            }
        };
        result.setProperty("schema", VERSION);
        return result;
    }

    private static Properties load(String name) {
        Properties result = properties();
        try (InputStream input = OcrGoldenFixtures.class.getResourceAsStream("/ocr/" + name)) {
            // Podczas jawnego zapisu fixture czytamy najnowszy plik, przed kopiowaniem resources przez Maven.
            if (Boolean.getBoolean("dstats.captureOcrFixtures")) {
                try (InputStream file = Files.newInputStream(Path.of("src/test/resources/ocr", name))) {
                    result.loadFromXML(file);
                }
            } else {
                if (input == null) {
                    throw new IllegalStateException("Brak golden OCR fixture: " + name);
                }
                result.loadFromXML(input);
            }
            if (!VERSION.equals(required(result, "schema"))) {
                throw new IllegalStateException("Nieobsługiwana wersja fixture: " + name);
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Nie udało się odczytać OCR fixture " + name, exception);
        }
    }

    private static void save(String name, Properties data) {
        try {
            Path directory = Path.of("src/test/resources/ocr");
            Files.createDirectories(directory);
            try (OutputStream output = Files.newOutputStream(directory.resolve(name))) {
                data.storeToXML(output, "Rzeczywisty output bundled pol.traineddata; schema=" + VERSION, "UTF-8");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Nie udało się zapisać OCR fixture " + name, exception);
        }
    }

    // Zamknięta lista modeli i pełne pola zamiast samego tekstu; nowe pole wymaga nowej wersji fixture.
    private static void encode(Properties data, String key, Object value) {
        if (value == null) {
            data.setProperty(key + ".null", "true");
        } else if (value instanceof byte[] bytes) {
            data.setProperty(key, Base64.getEncoder().encodeToString(bytes));
        } else if (value instanceof List<?> list) {
            data.setProperty(key + ".size", Integer.toString(list.size()));
            for (int i = 0; i < list.size(); i++) {
                encode(data, key + "." + i, list.get(i));
            }
        } else if (MODELS.contains(value.getClass())) {
            try {
                for (Field field : fields(value.getClass())) {
                    field.setAccessible(true);
                    encode(data, key + "." + field.getName(), field.get(value));
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        } else {
            data.setProperty(key, value.toString());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object decode(Properties data, String key, Type type) {
        if ("true".equals(data.getProperty(key + ".null")) || "null".equals(data.getProperty(key)) && type != String.class) {
            return null;
        }
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() == List.class) {
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < Integer.parseInt(required(data, key + ".size")); i++) {
                values.add(decode(data, key + "." + i, parameterized.getActualTypeArguments()[0]));
            }
            return List.copyOf(values);
        }
        Class<?> model = (Class<?>) type;
        if (MODELS.contains(model)) {
            try {
                List<Field> fields = fields(model);
                Class<?>[] types = fields.stream().map(Field::getType).toArray(Class<?>[]::new);
                Constructor<?> constructor = model.getConstructor(types);
                Object[] values = fields.stream()
                        .map(field -> decode(data, key + "." + field.getName(), field.getGenericType()))
                        .toArray();
                return constructor.newInstance(values);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Model fixture zmienił kontrakt: " + model.getSimpleName(), exception);
            }
        }
        String value = required(data, key);
        if (model == String.class) return value;
        if (model == int.class) return Integer.parseInt(value);
        if (model == long.class) return Long.parseLong(value);
        if (model == double.class) return Double.parseDouble(value);
        if (model == boolean.class) return Boolean.parseBoolean(value);
        if (model == byte[].class) return Base64.getDecoder().decode(value);
        if (model.isEnum()) return Enum.valueOf((Class<Enum>) model, value);
        throw new IllegalStateException("Nieobsługiwany typ fixture: " + type);
    }

    private static List<Field> fields(Class<?> model) {
        var actualNames = java.util.Arrays.stream(model.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .map(Field::getName).collect(java.util.stream.Collectors.toSet());
        if (!actualNames.equals(Set.copyOf(FIELD_ORDER.get(model)))) {
            throw new IllegalStateException("Model fixture wymaga aktualizacji schematu: " + model.getSimpleName());
        }
        return FIELD_ORDER.get(model).stream().map(name -> {
            try {
                return model.getDeclaredField(name);
            } catch (NoSuchFieldException exception) {
                throw new IllegalStateException(exception);
            }
        }).toList();
    }

    private static String required(Properties data, String key) {
        String value = data.getProperty(key);
        if (value == null) throw new IllegalStateException("Brak pola fixture: " + key);
        return value;
    }
}
