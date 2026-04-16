package krys.itemimport;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/** Czyta tekst z obrazu przez wbudowany OCR Windows, z dodatkową próbą na wersji wzmocnionej. */
final class WindowsItemOcrTextReader {
    String readText(byte[] originalImageBytes, BufferedImage image) {
        Path originalPath = null;
        Path enhancedPath = null;
        try {
            originalPath = Files.createTempFile("item-import-original-", ".png");
            Files.write(originalPath, originalImageBytes);

            enhancedPath = Files.createTempFile("item-import-enhanced-", ".png");
            Files.write(enhancedPath, buildEnhancedImageBytes(image));

            String originalText = runWindowsOcr(originalPath);
            String enhancedText = runWindowsOcr(enhancedPath);
            return mergeTexts(originalText, enhancedText);
        } catch (IOException exception) {
            return "";
        } finally {
            deleteQuietly(originalPath);
            deleteQuietly(enhancedPath);
        }
    }

    private static byte[] buildEnhancedImageBytes(BufferedImage image) throws IOException {
        int targetWidth = Math.max(image.getWidth() * 2, 1200);
        int targetHeight = Math.max(image.getHeight() * 2, 800);
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(image, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        BufferedImage grayscaleImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(scaledImage, grayscaleImage);

        BufferedImage thresholdImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < targetHeight; y++) {
            for (int x = 0; x < targetWidth; x++) {
                int gray = grayscaleImage.getRaster().getSample(x, y, 0);
                int color = gray >= 145 ? 0x00FFFFFF : 0x00000000;
                thresholdImage.setRGB(x, y, color);
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(thresholdImage, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private static String runWindowsOcr(Path imagePath) {
        String escapedPath = imagePath.toAbsolutePath().toString().replace("'", "''");
        String command = """
                $ErrorActionPreference='Stop';
                $OutputEncoding=[System.Text.UTF8Encoding]::new($false);
                Add-Type -AssemblyName System.Runtime.WindowsRuntime;
                function AwaitOperation($asyncOp,[Type]$resultType) {
                    $method=[System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
                        $_.Name -eq 'AsTask' -and $_.IsGenericMethod -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
                    } | Select-Object -First 1;
                    $generic=$method.MakeGenericMethod($resultType);
                    $task=$generic.Invoke($null,@($asyncOp));
                    return $task.GetAwaiter().GetResult();
                }
                $null=[Windows.Storage.StorageFile,Windows.Storage,ContentType=WindowsRuntime];
                $null=[Windows.Storage.FileAccessMode,Windows.Storage,ContentType=WindowsRuntime];
                $null=[Windows.Storage.Streams.IRandomAccessStream,Windows.Storage.Streams,ContentType=WindowsRuntime];
                $null=[Windows.Graphics.Imaging.BitmapDecoder,Windows.Graphics.Imaging,ContentType=WindowsRuntime];
                $null=[Windows.Graphics.Imaging.SoftwareBitmap,Windows.Graphics.Imaging,ContentType=WindowsRuntime];
                $null=[Windows.Media.Ocr.OcrEngine,Windows.Media.Ocr,ContentType=WindowsRuntime];
                $null=[Windows.Media.Ocr.OcrResult,Windows.Media.Ocr,ContentType=WindowsRuntime];
                $file=AwaitOperation ([Windows.Storage.StorageFile]::GetFileFromPathAsync('%s')) ([Windows.Storage.StorageFile]);
                $stream=AwaitOperation ($file.OpenAsync([Windows.Storage.FileAccessMode]::Read)) ([Windows.Storage.Streams.IRandomAccessStream]);
                $decoder=AwaitOperation ([Windows.Graphics.Imaging.BitmapDecoder]::CreateAsync($stream)) ([Windows.Graphics.Imaging.BitmapDecoder]);
                $bitmap=AwaitOperation ($decoder.GetSoftwareBitmapAsync()) ([Windows.Graphics.Imaging.SoftwareBitmap]);
                $engine=[Windows.Media.Ocr.OcrEngine]::TryCreateFromUserProfileLanguages();
                $result=AwaitOperation ($engine.RecognizeAsync($bitmap)) ([Windows.Media.Ocr.OcrResult]);
                [Console]::Out.Write($result.Text);
                """.formatted(escapedPath);

        try {
            Process process = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Command",
                    command
            ).redirectErrorStream(true).start();
            byte[] outputBytes = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return "";
            }
            return new String(outputBytes, StandardCharsets.UTF_8).trim();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "";
        } catch (IOException exception) {
            return "";
        }
    }

    private static String mergeTexts(String firstText, String secondText) {
        Set<String> mergedLines = new LinkedHashSet<>();
        appendLines(mergedLines, firstText);
        appendLines(mergedLines, secondText);
        return String.join(System.lineSeparator(), mergedLines);
    }

    private static void appendLines(Set<String> lines, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        for (String line : text.split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isBlank()) {
                lines.add(trimmedLine);
            }
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
        }
    }
}
