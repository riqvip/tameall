package dev.riqvip.tameall.companion;

import java.util.Objects;
import java.util.UUID;

/** Server-wide index entry used by the companion roster. */
public record BondRecord(UUID bondId,
                         UUID ownerId,
                         String creatureType,
                         UUID entityId,
                         String dimension,
                         BlockPoint position,
                         boolean dead,
                         CompanionPresence presence,
                         long lastSeenTick,
                         CompanionState snapshot,
                         TargetSelection targetSelection) {
    public BondRecord {
        Objects.requireNonNull(bondId, "bondId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(creatureType, "creatureType");
        Objects.requireNonNull(snapshot, "snapshot");
        targetSelection = targetSelection == null ? TargetSelection.fromLegacy(snapshot.settings().targetFilter()) : targetSelection;
        Objects.requireNonNull(presence, "presence");
        if (dead && presence != CompanionPresence.DEAD) {
            throw new IllegalArgumentException("dead bond must have DEAD presence");
        }
        if (!dead && presence == CompanionPresence.DEAD) {
            throw new IllegalArgumentException("live bond cannot have DEAD presence");
        }
        if (!snapshot.bondId().equals(bondId) || !snapshot.ownerId().equals(ownerId)
                || !snapshot.creatureType().equals(creatureType)) {
            throw new IllegalArgumentException("bond record and snapshot identity differ");
        }
        if ((dimension == null) != (position == null)) {
            throw new IllegalArgumentException("dimension and position must be supplied together");
        }
        if (lastSeenTick < 0) {
            throw new IllegalArgumentException("last seen tick cannot be negative");
        }
    }

    public static BondRecord fromState(CompanionState state, UUID entityId,
                                       String dimension, BlockPoint position, long tick) {
        Objects.requireNonNull(state, "state");
        return new BondRecord(state.bondId(), state.ownerId(), state.creatureType(), entityId,
                dimension, position, state.dead(), state.dead() ? CompanionPresence.DEAD : CompanionPresence.ALIVE,
                tick, state, TargetSelection.fromLegacy(state.settings().targetFilter()));
    }

    /** Compatibility constructor for callers that have not yet captured a state snapshot. */
    public BondRecord(UUID bondId, UUID ownerId, String creatureType, UUID entityId,
                      String dimension, BlockPoint position, boolean dead, long lastSeenTick) {
        this(bondId, ownerId, creatureType, entityId, dimension, position, dead,
                dead ? CompanionPresence.DEAD : CompanionPresence.UNKNOWN, lastSeenTick,
                new CompanionState(bondId, ownerId, creatureType, null,
                        CompanionSettings.defaults(), dimension, position,
                        dead, 0), TargetSelection.fromLegacy(CompanionSettings.defaults().targetFilter()));
    }

    public BondRecord located(UUID id, String dimensionName, BlockPoint point, long tick) {
        return new BondRecord(bondId, ownerId, creatureType, id, dimensionName, point, false, CompanionPresence.ALIVE, tick,
                snapshot, targetSelection);
    }

    public BondRecord unloaded(String dimensionName, BlockPoint point, long tick) {
        return new BondRecord(bondId, ownerId, creatureType, entityId, dimensionName, point, false, CompanionPresence.UNLOADED, tick,
                snapshot, targetSelection);
    }

    public BondRecord deadAt(String dimensionName, BlockPoint point, long tick) {
        return new BondRecord(bondId, ownerId, creatureType, null, dimensionName, point, true, CompanionPresence.DEAD, tick,
                snapshot.markDead(), targetSelection);
    }

    public BondRecord unknown(String dimensionName, BlockPoint point, long tick) {
        return new BondRecord(bondId, ownerId, creatureType, entityId, dimensionName, point, false,
                CompanionPresence.UNKNOWN, tick, snapshot, targetSelection);
    }

    public BondRecord withSnapshot(CompanionState value, long tick) {
        return new BondRecord(bondId, ownerId, creatureType, entityId, dimension, position,
                value.dead(), value.dead() ? CompanionPresence.DEAD : presence, tick, value, targetSelection);
    }

    public BondRecord withTargetSelection(TargetSelection value, long tick) {
        return new BondRecord(bondId, ownerId, creatureType, entityId, dimension, position, dead, presence, tick,
                snapshot, value);
    }
}
