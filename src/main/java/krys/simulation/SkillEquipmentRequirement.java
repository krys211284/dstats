package krys.simulation;

import krys.skill.SkillId;

import java.util.Optional;

/** Minimalny kontrakt wymagań ekwipunku dla legalności skilla w runtime. */
final class SkillEquipmentRequirement {
    private SkillEquipmentRequirement() {
    }

    static Optional<String> blockingReason(SkillId skillId, HeroBuildSnapshot snapshot) {
        if (skillId != SkillId.CLASH) {
            return Optional.empty();
        }
        if (!snapshot.hasActiveWeapon() && !snapshot.hasActiveShield()) {
            return Optional.of("Starcie pominięte: wymaga aktywnej broni i tarczy.");
        }
        if (!snapshot.hasActiveWeapon()) {
            return Optional.of("Starcie pominięte: brak aktywnej broni.");
        }
        if (!snapshot.hasActiveShield()) {
            return Optional.of("Starcie pominięte: brak aktywnej tarczy.");
        }
        return Optional.empty();
    }
}
