package dev.riqvip.tameall.companion;

import java.util.Objects;
import java.util.UUID;

/**
 * Complete persisted bond metadata. Inventory and native mob data remain on
 * the entity; this object carries only the ownership and controller state that
 * must survive conversion, unload, death, and dimension changes.
 */
public record CompanionState(UUID bondId,
                             UUID ownerId,
                             String creatureType,
                             String displayName,
                             CompanionSettings settings,
                             String anchorDimension,
                             BlockPoint anchor,
                             boolean dead,
                             long revision) {
    public static final int MAX_NAME_LENGTH = 64;

    public CompanionState {
        bondId = Objects.requireNonNull(bondId, "bondId");
        ownerId = Objects.requireNonNull(ownerId, "ownerId");
        creatureType = Objects.requireNonNull(creatureType, "creatureType");
        if (creatureType.isBlank() || creatureType.length() > 256) {
            throw new IllegalArgumentException("invalid creature type");
        }
        if (displayName != null && displayName.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("display name is too long");
        }
        settings = Objects.requireNonNull(settings, "settings");
        if ((anchorDimension == null) != (anchor == null)) {
            throw new IllegalArgumentException("anchor dimension and position must be supplied together");
        }
        if (revision < 0) {
            throw new IllegalArgumentException("revision cannot be negative");
        }
    }

    public static CompanionState newlyBonded(UUID ownerId, String creatureType) {
        return new CompanionState(UUID.randomUUID(), ownerId, creatureType, null,
                CompanionSettings.defaults(), null, null, false, 0);
    }

    public CompanionState rename(String value) {
        String name = value == null || value.isBlank() ? null : value.substring(0, Math.min(value.length(), MAX_NAME_LENGTH));
        return next(name, settings, anchorDimension, anchor, dead);
    }

    public CompanionState withSettings(CompanionSettings value) {
        return next(displayName, Objects.requireNonNull(value, "settings"), anchorDimension, anchor, dead);
    }

    public CompanionState withAnchor(String dimension, BlockPoint position) {
        return next(displayName, settings, dimension, position, dead);
    }

    public CompanionState markDead() {
        return next(displayName, settings, anchorDimension, anchor, true);
    }

    /** Marks a persisted entity as alive again after its chunk is loaded. */
    public CompanionState markAlive() {
        return next(displayName, settings, anchorDimension, anchor, false);
    }

    private CompanionState next(String name, CompanionSettings value, String dimension,
                                BlockPoint position, boolean deadValue) {
        return new CompanionState(bondId, ownerId, creatureType, name, value, dimension, position,
                deadValue, revision + 1);
    }
}
