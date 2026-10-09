package krys.necromancer.bookofthedead;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static krys.necromancer.bookofthedead.BookOfDeadChoiceType.*;
import static krys.necromancer.bookofthedead.NecromancerMinionSpecializationId.*;
import static org.junit.jupiter.api.Assertions.*;

class BookOfDeadDomainValidationTest {
    private static final NecromancerBookOfDeadRegistry BOOK = NecromancerBookOfDeadRegistry.sourceCatalog();

    @Test
    void registryShouldRejectDuplicateOrMissingSpecializations() {
        var duplicate = new ArrayList<>(BOOK.all());
        duplicate.add(duplicate.getFirst());
        assertThrows(IllegalArgumentException.class, () -> new NecromancerBookOfDeadRegistry(duplicate));
        var missing = new ArrayList<>(BOOK.all());
        missing.removeLast();
        assertThrows(IllegalArgumentException.class, () -> new NecromancerBookOfDeadRegistry(missing));
    }

    @Test
    void registryShouldCopyInputAndRestoreFamilyUnlockOrder() {
        var input = new ArrayList<>(BOOK.all());
        Collections.reverse(input);
        var registry = new NecromancerBookOfDeadRegistry(input);
        input.clear();
        assertEquals(BOOK.all(), registry.all());
        assertThrows(UnsupportedOperationException.class, () -> registry.all().clear());
        assertThrows(UnsupportedOperationException.class, () -> registry.byId().clear());
        for (var family : NecromancerMinionFamily.values()) {
            assertThrows(UnsupportedOperationException.class, () -> registry.byFamily(family).clear());
        }
    }

    @Test
    void specializationShouldRejectDuplicateOrMissingChoices() {
        var definition = BOOK.find(SKIRMISHER).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> copy(definition, definition.passiveFacts(),
                List.of(definition.choice(UPGRADE_1), definition.choice(UPGRADE_1), definition.choice(SACRIFICE))));
        assertThrows(IllegalArgumentException.class, () -> copy(definition, definition.passiveFacts(),
                List.of(definition.choice(UPGRADE_1), definition.choice(SACRIFICE))));
    }

    @Test
    void specializationShouldCopyAndProtectDescriptionsAndChoices() {
        var source = BOOK.find(BONE_MAGE).orElseThrow();
        var passive = new ArrayList<>(source.passiveFacts());
        var choices = new ArrayList<>(source.choices());
        Collections.reverse(choices);
        var definition = copy(source, passive, choices);
        passive.clear();
        choices.clear();
        assertEquals(source, definition);
        assertThrows(UnsupportedOperationException.class, () -> definition.passiveFacts().clear());
        assertThrows(UnsupportedOperationException.class, () -> definition.choices().clear());
        var effects = new ArrayList<>(source.choice(UPGRADE_2).effects());
        var conflicts = new ArrayList<>(source.choice(UPGRADE_2).sourceConflicts());
        var choice = new BookOfDeadChoiceDefinition(UPGRADE_2, "Opis", effects, List.of(), Optional.empty(), conflicts);
        effects.clear();
        conflicts.clear();
        assertEquals(source.choice(UPGRADE_2).effects(), choice.effects());
        assertEquals(source.choice(UPGRADE_2).sourceConflicts(), choice.sourceConflicts());
        assertThrows(UnsupportedOperationException.class, () -> choice.effects().clear());
        assertThrows(UnsupportedOperationException.class, () -> choice.sourceConflicts().clear());
        var sacrifice = source.choice(SACRIFICE);
        var bonuses = new ArrayList<>(sacrifice.playerBonuses());
        var copy = new BookOfDeadChoiceDefinition(SACRIFICE, "Sacrifice", List.of(), bonuses,
                sacrifice.summonPenalty(), List.of());
        bonuses.clear();
        assertEquals(sacrifice.playerBonuses(), copy.playerBonuses());
        assertThrows(UnsupportedOperationException.class, () -> copy.playerBonuses().clear());
    }

    @Test
    void choicesShouldRejectMissingSacrificePartsOrSacrificePartsInsideUpgrade() {
        var sacrifice = BOOK.find(SKIRMISHER).orElseThrow().choice(SACRIFICE);
        assertThrows(IllegalArgumentException.class, () -> new BookOfDeadChoiceDefinition(SACRIFICE, "Opis",
                List.of(), List.of(), sacrifice.summonPenalty(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new BookOfDeadChoiceDefinition(SACRIFICE, "Opis",
                List.of(), sacrifice.playerBonuses(), Optional.empty(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> new BookOfDeadChoiceDefinition(UPGRADE_1, "Opis",
                sacrifice.playerBonuses(), sacrifice.playerBonuses(), sacrifice.summonPenalty(), List.of()));
    }

    @Test
    void specializationShouldRejectWrongFamilyPenaltyAndInconsistentConflict() {
        var definition = BOOK.find(SKIRMISHER).orElseThrow();
        var golemSacrifice = BOOK.find(BONE_GOLEM).orElseThrow().choice(SACRIFICE);
        assertThrows(IllegalArgumentException.class, () -> copy(definition, definition.passiveFacts(),
                List.of(definition.choice(UPGRADE_1), definition.choice(UPGRADE_2), golemSacrifice)));
        var bone = BOOK.find(BONE_MAGE).orElseThrow();
        var source = bone.choice(UPGRADE_2);
        var wrong = new BookOfDeadChoiceDefinition(UPGRADE_2, "Opis", List.of(
                new BookOfDeadSourceFact("fortify", "Fortify", new BigDecimal("3"),
                        BookOfDeadSourceFact.Unit.PERCENT_OF_MAXIMUM_LIFE)), List.of(), Optional.empty(), source.sourceConflicts());
        assertThrows(IllegalArgumentException.class, () -> copy(bone, bone.passiveFacts(),
                List.of(bone.choice(UPGRADE_1), wrong, bone.choice(SACRIFICE))));
    }

    @Test
    void sourceFactsShouldRejectGuessedAmountsMissingNumbersAndDuplicateFacts() {
        assertThrows(IllegalArgumentException.class, () -> new BookOfDeadSourceFact("thorns", "Ciernie",
                new BigDecimal("3481"), BookOfDeadSourceFact.Unit.SOURCE_DEPENDENT_AMOUNT));
        assertThrows(IllegalArgumentException.class, () -> new BookOfDeadSourceFact("damage", "Obrażenia",
                null, BookOfDeadSourceFact.Unit.PERCENT_OF_NORMAL_DAMAGE));
        assertThrows(IllegalArgumentException.class, () -> new BookOfDeadSourceFact(" ", "Opis",
                null, BookOfDeadSourceFact.Unit.TEXT));
        var fact = BOOK.find(SKIRMISHER).orElseThrow().passiveFacts().getFirst();
        assertThrows(IllegalArgumentException.class, () -> new BookOfDeadChoiceDefinition(UPGRADE_1,
                "Opis", List.of(fact, fact), List.of(), Optional.empty(), List.of()));
    }

    @Test
    void specializationShouldRejectBlankMetadataAndInvalidUnlock() {
        var source = BOOK.find(SKIRMISHER).orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> new NecromancerMinionSpecializationDefinition(
                source.id(), " ", source.canonicalName(), source.unlockLevel(), source.theme(), source.passiveDescription(),
                source.passiveFacts(), source.choices(), Optional.empty(), source.sourceReference(), source.runtimeStatus()));
        assertThrows(IllegalArgumentException.class, () -> new NecromancerMinionSpecializationDefinition(
                source.id(), source.displayName(), source.canonicalName(), 0, source.theme(), source.passiveDescription(),
                source.passiveFacts(), source.choices(), Optional.empty(), source.sourceReference(), source.runtimeStatus()));
    }

    private static NecromancerMinionSpecializationDefinition copy(NecromancerMinionSpecializationDefinition source,
            List<BookOfDeadSourceFact> passive, List<BookOfDeadChoiceDefinition> choices) {
        return new NecromancerMinionSpecializationDefinition(source.id(), source.displayName(), source.canonicalName(),
                source.unlockLevel(), source.theme(), source.passiveDescription(), passive, choices,
                source.additionalUnlockRequirement(), source.sourceReference(), source.runtimeStatus());
    }
}
