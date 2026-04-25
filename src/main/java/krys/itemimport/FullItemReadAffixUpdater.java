package krys.itemimport;

import java.util.ArrayList;
import java.util.List;

/** Aktualizuje sekcję edytowalnych affixów w pełnym odczycie, bez ruszania nagłówka, implicitów i aspektu. */
public final class FullItemReadAffixUpdater {
    public FullItemRead withEditedAffixes(FullItemRead fullItemRead, List<ImportedItemAffix> editedAffixes) {
        FullItemRead safeRead = fullItemRead == null ? FullItemRead.empty() : fullItemRead;
        List<ImportedItemAffix> safeAffixes = editedAffixes == null ? List.of() : editedAffixes;
        List<FullItemReadLine> updatedLines = new ArrayList<>();
        boolean inserted = false;
        for (FullItemReadLine line : safeRead.getLines()) {
            if (ImportedItemAffixExtractor.isEditableAffixLine(line)) {
                if (!inserted) {
                    appendAffixLines(updatedLines, safeAffixes);
                    inserted = true;
                }
                continue;
            }
            updatedLines.add(line);
        }
        if (!inserted) {
            appendAffixLines(updatedLines, safeAffixes);
        }
        return new FullItemRead(
                safeRead.getItemName(),
                safeRead.getItemTypeLine(),
                safeRead.getRarity(),
                safeRead.getItemPower(),
                safeRead.getBaseItemValue(),
                updatedLines
        );
    }

    private static void appendAffixLines(List<FullItemReadLine> lines, List<ImportedItemAffix> affixes) {
        for (ImportedItemAffix affix : affixes) {
            lines.add(new FullItemReadLine(FullItemReadLineType.AFFIX, affix.toDisplayLine()));
        }
    }
}
