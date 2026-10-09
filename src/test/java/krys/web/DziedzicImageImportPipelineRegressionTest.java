package krys.web;

import krys.item.HeroEquipmentSlot;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ItemImportValueMode;
import krys.itemimport.ValidatedImportedItem;
import krys.itemimport.image.AssembledItemImage;
import krys.itemimport.image.DeterministicItemPartStructureNormalizer;
import krys.itemimport.image.DeterministicPart1RoiSegmenter;
import krys.itemimport.image.ImageItemImportDraftMapper;
import krys.itemimport.image.Part1ReadParser;
import krys.itemimport.image.PropertyBlockOcrReader;
import krys.itemimport.image.PropertyBlockSemanticMapper;
import krys.itemimport.image.PropertyBlockSegmenter;
import krys.itemimport.image.PropertyBlockValueReader;
import krys.itemimport.image.StructuralItemPartSlot;
import krys.itemimport.image.TesseractPart1RoiOcrReader;
import krys.itemimport.image.TesseractPropertyBlockOcrReader;
import krys.itemimport.image.VisualPropertyBlockSegmenter;
import krys.itemimport.image.opencv.OpenCvHorizontalDividerDetector;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.MasterworkedAffixSource;
import krys.masterworking.MasterworkingResolvedItemValueResolver;
import krys.socketing.SocketContentType;
import krys.tempering.ItemTemperingAffix;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.TransfigurationValueProvenance;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresja realnego importu Dziedzica Zatracenia od PNG do verification i zapisu. */
class DziedzicImageImportPipelineRegressionTest {
    private final MasterworkingResolvedItemValueResolver valueResolver = new MasterworkingResolvedItemValueResolver();

    @Test
    void shouldKeepDziedzicDisplayedVerificationDataAndRuntimeValues() throws Exception {
        ImageItemImportDraftMapper.DraftResult draft = readDziedzicFullDraft();
        ItemImportEditableForm form = draft.form();
        String verificationHtml = ItemEditPageRenderer.renderImportVerificationForm(form, draft.reviewNotes(), "", false);

        assertEquals(ItemImportValueMode.GAME_DISPLAYED_VALUE, form.getValueMode());
        assertTrue(form.isMythicUnique());
        assertEquals("2004", form.getItemArmor());
        assertTrue(verificationHtml.contains("<input type=\"number\" min=\"0\" step=\"1\" name=\"itemArmor\" value=\"2004\">"));
        assertFalse(verificationHtml.contains("<input type=\"hidden\" name=\"itemArmor\" value=\"2004\">"));

        assertTrue(verificationHtml.contains("id=\"affixCount\" name=\"affixCount\" value=\"4\""));
        assertEquals(4, form.getAffixes().size());
        assertAffix(form, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 15.0d);
        assertAffix(form, ImportedItemAffixType.LUCKY_HIT_CHANCE, 25.0d);
        assertAffix(form, ImportedItemAffixType.MOVEMENT_SPEED, 25.0d);
        assertAffix(form, ImportedItemAffixType.CORE_SKILL_RANKS, 3.0d);

        assertTrue(form.getTransfiguration().isTransfigured());
        assertEquals(HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                form.getTransfiguration().getOutcome());
        assertNotNull(form.getTransfiguration().getAddedTransfigurationAffix());
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(115.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(TransfigurationValueProvenance.GAME_DISPLAYED_VALUE,
                form.getTransfiguration().getAddedTransfigurationAffix().getValueProvenance());
        assertTrue(draft.reviewNotes().stream().anyMatch(note -> note.contains("NUMERIC_OCR_AMBIGUOUS")));

        assertEquals(1, form.getTemperingAffixes().size());
        assertEquals(12.0d, valueResolver.resolveTemperingValue(
                form.getTemperingAffixes().getFirst(),
                form.getMasterworking()
        ), 0.0001d);
        assertTrue(verificationHtml.contains("name=\"temperingValue_0\" value=\"12\""));
        assertEquals(25, form.getMasterworking().getQualityCurrent());
        assertNotNull(form.getMasterworking().getPerfectedAffix());
        assertEquals(MasterworkedAffixSource.TEMPERING_AFFIX,
                form.getMasterworking().getPerfectedAffix().getSource());
        assertEquals("defense_max_animus", form.getMasterworking().getPerfectedAffix().getKey());

        assertEquals(2, form.getSocketing().getSocketCount());
        assertEquals(SocketContentType.DETECTED_STAT, form.getSocketing().socketAt(0).getContentType());
        assertEquals(SocketContentType.DETECTED_STAT, form.getSocketing().socketAt(1).getContentType());
        assertTrue(form.getSocketing().socketAt(0).getDetectedStat().getDisplayText().contains("+150"));
        assertTrue(form.getSocketing().socketAt(1).getDetectedStat().getDisplayText().contains("+120"));

        ItemImportEditableForm submittedWithoutChanges = displayedSubmissionWithoutChanges(form);
        ItemImportDisplayedValueFormNormalizer.NormalizedForm normalized =
                new ItemImportDisplayedValueFormNormalizer().normalize(submittedWithoutChanges);
        ItemImportFormMapper.MappingResult mapping = new ItemImportFormMapper().map(normalized.form());

        assertTrue(normalized.errors().isEmpty(), () -> normalized.errors().toString());
        assertTrue(mapping.getErrors().isEmpty(), () -> mapping.getErrors().toString());
        assertNotNull(mapping.getItem());
        assertSavedAffix(mapping.getItem(), ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 15.0d);
        assertSavedAffix(mapping.getItem(), ImportedItemAffixType.LUCKY_HIT_CHANCE, 25.0d);
        assertSavedAffix(mapping.getItem(), ImportedItemAffixType.MOVEMENT_SPEED, 25.0d);
        assertSavedAffix(mapping.getItem(), ImportedItemAffixType.CORE_SKILL_RANKS, 3.0d);

        ItemLibraryService service = new ItemLibraryService(
                new FileItemLibraryRepository(Files.createTempDirectory("dziedzic-import-runtime"))
        );
        SavedImportedItem saved = service.saveImportedItem(mapping.getItem(), form.getFullItemRead());
        EffectiveCurrentBuildResolution runtime = service.resolveEffectiveCurrentBuild(
                new krys.itemimport.CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.HELMET, saved.getItemId())
        );

        assertEquals(2004L, runtime.getActiveHeroItemStats().getItemArmor());
        assertEquals(15.0d, runtime.getActiveHeroItemStats().getCriticalChancePercent(), 0.0001d);
        assertEquals(15.0d, runtime.getActiveItemsContribution().getCriticalChancePercent(), 0.0001d);
        assertNotEquals(18.75d, runtime.getActiveHeroItemStats().getCriticalChancePercent(), 0.0001d);
    }

    private static ImageItemImportDraftMapper.DraftResult readDziedzicFullDraft() throws Exception {
        OpenCvItemImageAssembler assembler = new OpenCvItemImageAssembler();
        OpenCvHorizontalDividerDetector dividerDetector = new OpenCvHorizontalDividerDetector();
        DeterministicItemPartStructureNormalizer normalizer = new DeterministicItemPartStructureNormalizer();
        DeterministicPart1RoiSegmenter part1Segmenter = new DeterministicPart1RoiSegmenter();
        TesseractPart1RoiOcrReader part1OcrReader = new TesseractPart1RoiOcrReader();
        Part1ReadParser part1ReadParser = new Part1ReadParser();
        PropertyBlockSegmenter propertyBlockSegmenter = new VisualPropertyBlockSegmenter();
        PropertyBlockOcrReader propertyBlockOcrReader = new TesseractPropertyBlockOcrReader();
        PropertyBlockSemanticMapper semanticMapper = new PropertyBlockSemanticMapper();
        PropertyBlockValueReader valueReader = new PropertyBlockValueReader();

        AssembledItemImage image = assembler.assemble(List.of(request("dziedzic-zatracenia-helm-full.png")));
        var parts = normalizer.normalize(dividerDetector.detect(image).getParts());
        var part1 = parts.find(StructuralItemPartSlot.PART_1).orElseThrow();
        var part1Read = part1ReadParser.parse(part1Segmenter.segment(part1).getRois().stream()
                .map(part1OcrReader::read)
                .toList());

        java.util.ArrayList<ImageItemImportDraftMapper.BlockRead> blockReads = new java.util.ArrayList<>();
        for (StructuralItemPartSlot slot : List.of(StructuralItemPartSlot.PART_2, StructuralItemPartSlot.PART_3)) {
            var part = parts.find(slot);
            if (part.isEmpty()) {
                continue;
            }
            for (var block : propertyBlockSegmenter.segment(part.get()).getBlocks()) {
                var read = propertyBlockOcrReader.read(block);
                var semantic = semanticMapper.map(block, read);
                var value = valueReader.read(read, semantic);
                blockReads.add(new ImageItemImportDraftMapper.BlockRead(read, semantic, value));
            }
        }
        return new ImageItemImportDraftMapper().map(part1Read, blockReads, "dziedzic-zatracenia-helm-full.png");
    }

    private static ItemImageImportRequest request(String filename) throws Exception {
        return new ItemImageImportRequest(filename, "image/png",
                Files.readAllBytes(Path.of("src/test/resources/items", filename)));
    }

    private ItemImportEditableForm displayedSubmissionWithoutChanges(ItemImportEditableForm form) {
        List<ItemTemperingAffix> displayedTempering = form.getTemperingAffixes().stream()
                .map(affix -> new ItemTemperingAffix(
                        affix.getDefinitionId(),
                        affix.getCategory(),
                        valueResolver.resolveTemperingValue(affix, form.getMasterworking()),
                        affix.getDisplayText(),
                        affix.getSourceLine(),
                        affix.getRuntimeStatus(),
                        affix.isGreaterAffix()
                ))
                .toList();
        return new ItemImportEditableForm(
                form.getSourceImageName(),
                form.getSlot(),
                form.getWeaponDamage(),
                form.getStrength(),
                form.getIntelligence(),
                form.getThorns(),
                form.getBlockChance(),
                form.getRetributionChance(),
                form.getFullItemRead(),
                form.getAffixes(),
                form.getSelectedAspectId(),
                form.getDetails(),
                displayedTempering,
                form.getMasterworking(),
                form.getTransfiguration(),
                form.getSocketing(),
                ItemImportValueMode.GAME_DISPLAYED_VALUE
        );
    }

    private static void assertAffix(ItemImportEditableForm form, ImportedItemAffixType type, double expected) {
        ImportedItemAffix affix = form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == type)
                .findFirst()
                .orElseThrow();
        assertEquals(expected, affix.getValue(), 0.0001d);
    }

    private static void assertSavedAffix(ValidatedImportedItem item, ImportedItemAffixType type, double expected) {
        ImportedItemAffix affix = item.getAffixes().stream()
                .filter(candidate -> candidate.getType() == type)
                .findFirst()
                .orElseThrow();
        assertEquals(expected, affix.getValue(), 0.0001d);
    }
}
