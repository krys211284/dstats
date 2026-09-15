package krys.itemimport.image;

/** Czyta surowy tekst OCR z pojedynczego ROI PART_1. */
public interface Part1RoiOcrReader {
    Part1RoiOcrResult read(Part1Roi roi);
}
