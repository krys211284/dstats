package krys.ranking;

import krys.skill.SkillId;

import java.util.EnumMap;
import java.util.Map;

/** Metadane źródłowe dla legacy/test-only foundation `PaladinSkillDefs`. */
final class PaladinFoundationSkillSource {
    private static final String BASIC_PDF = "docs/paladin/source-pdfs/paladin_basic_skill_registry_final.pdf";
    private static final Map<SkillId, SourceMetadata> METADATA_BY_SKILL = createMetadata();

    private PaladinFoundationSkillSource() {
    }

    static SourceMetadata get(SkillId skillId) {
        SourceMetadata metadata = METADATA_BY_SKILL.get(skillId);
        if (metadata == null) {
            throw new IllegalArgumentException("Brak metadanych źródłowych dla skilla foundation: " + skillId);
        }
        return metadata;
    }

    private static Map<SkillId, SourceMetadata> createMetadata() {
        EnumMap<SkillId, SourceMetadata> metadata = new EnumMap<>(SkillId.class);
        metadata.put(SkillId.BRANDISH, new SourceMetadata(BASIC_PDF, "basic"));
        metadata.put(SkillId.HOLY_BOLT, new SourceMetadata(BASIC_PDF, "basic"));
        metadata.put(SkillId.CLASH, new SourceMetadata(BASIC_PDF, "basic"));
        metadata.put(SkillId.ADVANCE, new SourceMetadata(BASIC_PDF, "basic"));
        return Map.copyOf(metadata);
    }

    record SourceMetadata(String sourcePdf, String skillGroup) {
    }
}
