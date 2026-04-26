package krys.itemimport;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;

/** Przygotowuje kilka wariantów obrazu itemu pod OCR z heurystycznym wycięciem obszaru tekstowego. */
final class ItemImageOcrPreprocessor {
    List<ItemImageOcrVariant> prepareVariants(BufferedImage originalImage) {
        CropBox cropBox = detectTextCropBox(originalImage);
        BufferedImage croppedImage = crop(originalImage, cropBox);

        List<ItemImageOcrVariant> variants = new ArrayList<>();
        variants.add(new ItemImageOcrVariant(
                "original",
                copyImage(originalImage),
                0,
                0,
                originalImage.getWidth(),
                originalImage.getHeight()
        ));
        variants.add(new ItemImageOcrVariant(
                "text-crop",
                copyImage(croppedImage),
                cropBox.x(),
                cropBox.y(),
                cropBox.width(),
                cropBox.height()
        ));
        variants.add(new ItemImageOcrVariant(
                "text-crop-gray-x2-contrast",
                adjustContrast(toGrayscale(upscale(croppedImage, 2.0d)), 1.35f, 10.0f),
                cropBox.x(),
                cropBox.y(),
                cropBox.width(),
                cropBox.height()
        ));
        BufferedImage thresholdBase = adjustContrast(toGrayscale(upscale(croppedImage, 3.0d)), 1.45f, 14.0f);
        variants.add(new ItemImageOcrVariant(
                "text-crop-gray-x3-threshold",
                applyThreshold(thresholdBase),
                cropBox.x(),
                cropBox.y(),
                cropBox.width(),
                cropBox.height()
        ));
        variants.add(new ItemImageOcrVariant(
                "text-crop-gray-x3-sharpen",
                sharpen(thresholdBase),
                cropBox.x(),
                cropBox.y(),
                cropBox.width(),
                cropBox.height()
        ));
        CropBox bottomEffectCropBox = bottomEffectCrop(originalImage);
        variants.add(new ItemImageOcrVariant(
                "bottom-effect-x4",
                upscale(crop(originalImage, bottomEffectCropBox), 4.0d),
                bottomEffectCropBox.x(),
                bottomEffectCropBox.y(),
                bottomEffectCropBox.width(),
                bottomEffectCropBox.height()
        ));
        return List.copyOf(variants);
    }

    private static CropBox detectTextCropBox(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int minX = Math.max(0, width / 25);
        int maxX = Math.min(width - 1, width - Math.max(1, width / 25));
        int minY = Math.max(0, height / 30);
        int maxY = Math.min(height - 1, height - Math.max(1, height / 6));

        int rowThreshold = Math.max(6, width / 80);
        int[] rowHits = new int[height];
        for (int y = minY; y <= maxY; y++) {
            int hits = 0;
            for (int x = minX; x <= maxX; x++) {
                if (isLikelyTextPixel(image, x, y)) {
                    hits++;
                }
            }
            rowHits[y] = hits;
        }

        int textTop = firstIndexAtLeast(rowHits, minY, maxY, rowThreshold);
        int textBottom = lastIndexAtLeast(rowHits, minY, maxY, rowThreshold);
        if (textTop < 0 || textBottom < textTop) {
            return fallbackCrop(width, height);
        }

        int colThreshold = Math.max(6, (textBottom - textTop + 1) / 20);
        int[] colHits = new int[width];
        for (int x = minX; x <= maxX; x++) {
            int hits = 0;
            for (int y = textTop; y <= textBottom; y++) {
                if (isLikelyTextPixel(image, x, y)) {
                    hits++;
                }
            }
            colHits[x] = hits;
        }

        int textLeft = firstIndexAtLeast(colHits, minX, maxX, colThreshold);
        int textRight = lastIndexAtLeast(colHits, minX, maxX, colThreshold);
        if (textLeft < 0 || textRight < textLeft) {
            return fallbackCrop(width, height);
        }

        int padX = Math.max(18, width / 40);
        int padY = Math.max(18, height / 45);
        int cropX = clamp(textLeft - padX, 0, width - 1);
        int cropY = clamp(textTop - padY, 0, height - 1);
        int cropRight = clamp(textRight + padX, cropX + 1, width);
        int cropBottom = clamp(textBottom + padY, cropY + 1, height - Math.max(1, height / 12));

        int cropWidth = Math.max(1, cropRight - cropX);
        int cropHeight = Math.max(1, cropBottom - cropY);
        return new CropBox(cropX, cropY, cropWidth, cropHeight);
    }

    private static CropBox fallbackCrop(int width, int height) {
        int x = Math.max(0, width / 6);
        int y = Math.max(0, height / 18);
        int croppedWidth = Math.max(1, width - x - (width / 10));
        int croppedHeight = Math.max(1, height - y - (height / 5));
        return new CropBox(x, y, croppedWidth, croppedHeight);
    }

    private static CropBox bottomEffectCrop(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int y = clamp((int) Math.round(height * 0.78d), 0, height - 1);
        int bottom = clamp((int) Math.round(height * 0.96d), y + 1, height);
        return new CropBox(0, y, width, bottom - y);
    }

    private static boolean isLikelyTextPixel(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int brightness = (red + green + blue) / 3;
        int chroma = Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue));
        return brightness >= 125 && chroma <= 150;
    }

    private static int firstIndexAtLeast(int[] values, int from, int to, int threshold) {
        for (int index = from; index <= to; index++) {
            if (values[index] >= threshold) {
                return index;
            }
        }
        return -1;
    }

    private static int lastIndexAtLeast(int[] values, int from, int to, int threshold) {
        for (int index = to; index >= from; index--) {
            if (values[index] >= threshold) {
                return index;
            }
        }
        return -1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static BufferedImage crop(BufferedImage image, CropBox cropBox) {
        BufferedImage cropped = new BufferedImage(cropBox.width(), cropBox.height(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = cropped.createGraphics();
        graphics.drawImage(
                image,
                0,
                0,
                cropBox.width(),
                cropBox.height(),
                cropBox.x(),
                cropBox.y(),
                cropBox.x() + cropBox.width(),
                cropBox.y() + cropBox.height(),
                null
        );
        graphics.dispose();
        return cropped;
    }

    private static BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static BufferedImage upscale(BufferedImage source, double scale) {
        int targetWidth = Math.max((int) Math.round(source.getWidth() * scale), source.getWidth());
        int targetHeight = Math.max((int) Math.round(source.getHeight() * scale), source.getHeight());
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaledImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return scaledImage;
    }

    private static BufferedImage toGrayscale(BufferedImage image) {
        BufferedImage grayscale = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null).filter(image, grayscale);
        return grayscale;
    }

    private static BufferedImage adjustContrast(BufferedImage image, float scaleFactor, float offset) {
        BufferedImage contrasted = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        new RescaleOp(scaleFactor, offset, null).filter(image, contrasted);
        return contrasted;
    }

    private static BufferedImage applyThreshold(BufferedImage image) {
        BufferedImage thresholdImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        int threshold = computeThreshold(image);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int gray = image.getRaster().getSample(x, y, 0);
                int color = gray >= threshold ? 0x00FFFFFF : 0x00000000;
                thresholdImage.setRGB(x, y, color);
            }
        }
        return thresholdImage;
    }

    private static int computeThreshold(BufferedImage image) {
        long sum = 0L;
        int pixels = image.getWidth() * image.getHeight();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                sum += image.getRaster().getSample(x, y, 0);
            }
        }
        int average = (int) (sum / Math.max(1, pixels));
        return clamp(average + 18, 110, 210);
    }

    private static BufferedImage sharpen(BufferedImage image) {
        Kernel kernel = new Kernel(3, 3, new float[]{
                0.0f, -1.0f, 0.0f,
                -1.0f, 5.2f, -1.0f,
                0.0f, -1.0f, 0.0f
        });
        ConvolveOp convolveOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        BufferedImage sharpened = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        convolveOp.filter(image, sharpened);
        return sharpened;
    }

    private record CropBox(int x, int y, int width, int height) {
    }
}
