package krys.itemimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pełniejszy odczyt widocznego itemu z OCR, oddzielony od mappingu foundation runtime. */
public final class FullItemRead {
    private final String itemName;
    private final String itemTypeLine;
    private final String rarity;
    private final String itemPower;
    private final String baseItemValue;
    private final List<FullItemReadLine> lines;

    public FullItemRead(String itemName,
                        String itemTypeLine,
                        String rarity,
                        String itemPower,
                        String baseItemValue,
                        List<FullItemReadLine> lines) {
        this.itemName = normalize(itemName);
        this.itemTypeLine = normalize(itemTypeLine);
        this.rarity = normalize(rarity);
        this.itemPower = normalize(itemPower);
        this.baseItemValue = normalize(baseItemValue);
        this.lines = Collections.unmodifiableList(new ArrayList<>(lines == null ? List.of() : lines));
    }

    public static FullItemRead empty() {
        return new FullItemRead("", "", "", "", "", List.of());
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemTypeLine() {
        return itemTypeLine;
    }

    public String getRarity() {
        return rarity;
    }

    public String getItemPower() {
        return itemPower;
    }

    public String getBaseItemValue() {
        return baseItemValue;
    }

    public List<FullItemReadLine> getLines() {
        return lines;
    }

    public boolean hasAnyData() {
        return !itemName.isBlank()
                || !itemTypeLine.isBlank()
                || !rarity.isBlank()
                || !itemPower.isBlank()
                || !baseItemValue.isBlank()
                || !lines.isEmpty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
