package krys.itemimport.image;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Zamrożone bajty PNG i pełna diagnostyka stitching z pomiaru przed optymalizacją reprezentacji RGB. */
final class ImageAssemblyGoldenFixtures {
    private ImageAssemblyGoldenFixtures() {
    }

    static void assertGolden(String label, AssembledItemImage image) throws Exception {
        Properties actual = new Properties();
        actual.setProperty("image.sha256", HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(image.getImageBytes())));
        actual.setProperty("image.width", Integer.toString(image.getWidth()));
        actual.setProperty("image.height", Integer.toString(image.getHeight()));
        actual.setProperty("image.sourceImageCount", Integer.toString(image.getSourceImageCount()));
        actual.setProperty("image.format", image.getFormat());
        actual.setProperty("image.compositionPolicy", image.getCompositionPolicy());
        actual.setProperty("image.assemblyStatus", image.getAssemblyStatus());
        actual.setProperty("stitches.size", Integer.toString(image.getStitchDiagnostics().size()));
        for (int i = 0; i < image.getStitchDiagnostics().size(); i++) {
            var stitch = image.getStitchDiagnostics().get(i);
            String key = "stitch." + i + ".";
            actual.setProperty(key + "firstSourceIndex", Integer.toString(stitch.getFirstSourceIndex()));
            actual.setProperty(key + "secondSourceIndex", Integer.toString(stitch.getSecondSourceIndex()));
            actual.setProperty(key + "offsetX", Integer.toString(stitch.getOffsetX()));
            actual.setProperty(key + "offsetY", Integer.toString(stitch.getOffsetY()));
            actual.setProperty(key + "overlapHeight", Integer.toString(stitch.getOverlapHeight()));
            actual.setProperty(key + "seamY", Integer.toString(stitch.getSeamY()));
            actual.setProperty(key + "confidence", Double.toString(stitch.getConfidence()));
            actual.setProperty(key + "status", stitch.getStatus());
        }
        String name = label + ".xml";
        if (Boolean.getBoolean("dstats.captureAssemblyFixtures")) {
            Path directory = Path.of("src/test/resources/vision");
            Files.createDirectories(directory);
            try (var output = Files.newOutputStream(directory.resolve(name))) {
                actual.storeToXML(output, "Golden assembly przed zmianą reprezentacji RGB", "UTF-8");
            }
        } else {
            Properties expected = new Properties();
            try (var input = ImageAssemblyGoldenFixtures.class.getResourceAsStream("/vision/" + name)) {
                if (input == null) throw new IllegalStateException("Brak golden assembly: " + name);
                expected.loadFromXML(input);
            }
            assertEquals(expected, actual, label);
        }
    }
}
