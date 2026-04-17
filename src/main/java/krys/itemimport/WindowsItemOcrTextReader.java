package krys.itemimport;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Czyta tekst z kilku przygotowanych wariantów obrazu przez wbudowany OCR Windows. */
final class WindowsItemOcrTextReader implements ItemImageOcrTextReader {
    @Override
    public List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants) {
        List<ItemImageOcrTextVariant> results = new ArrayList<>();
        for (ItemImageOcrVariant variant : variants) {
            String text = readSingleVariant(variant.getImage());
            results.add(new ItemImageOcrTextVariant(variant.getVariantId(), text));
        }
        return List.copyOf(results);
    }

    private static String readSingleVariant(BufferedImage image) {
        Path variantPath = null;
        try {
            variantPath = Files.createTempFile("item-import-variant-", ".png");
            Files.write(variantPath, encodePng(image));
            return runWindowsOcr(variantPath);
        } catch (IOException exception) {
            return "";
        } finally {
            deleteQuietly(variantPath);
        }
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", outputStream);
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
