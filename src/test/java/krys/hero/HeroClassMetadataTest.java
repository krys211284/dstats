package krys.hero;

import krys.item.ItemStatType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeroClassMetadataTest {
    @Test
    void profileClassesShouldHaveStableTechnicalIdsAndPolishNames() {
        assertArrayEquals(new HeroClass[]{HeroClass.PALADIN, HeroClass.NECROMANCER}, HeroClass.values());
        assertEquals("paladin", HeroClass.PALADIN.getTechnicalId());
        assertEquals("Paladyn", HeroClass.PALADIN.getDisplayName());
        assertEquals("necromancer", HeroClass.NECROMANCER.getTechnicalId());
        assertEquals("Nekromanta", HeroClass.NECROMANCER.getDisplayName());
    }

    @Test
    void necromancerProfileShouldNotIntroduceAnUnverifiedRuntimeDefinitionOrBaseline() {
        assertEquals(ItemStatType.STRENGTH, HeroClassDefs.get(HeroClass.PALADIN).getMainStatType());
        assertThrows(IllegalArgumentException.class, () -> HeroClassDefs.get(HeroClass.NECROMANCER));
        assertTrue(HeroClassStatBaselines.find(HeroClass.NECROMANCER, 70).isEmpty());
        assertTrue(HeroClassStatBaselines.find(HeroClass.PALADIN, 70).isPresent());
    }
}
