package dev.riqvip.tameall.companion;

import java.util.Objects;
import java.util.UUID;

/**
 * Pure server policy for the one-use Golden Wheat transaction. The platform
 * interaction hook calls this before consuming an item, and consumes only when
 * {@link BondingResult#succeeded()} is true.
 */
public final class CompanionBondingService {
    private CompanionBondingService() {}

    public static BondingResult bond(UUID ownerId, String creatureType, boolean livingEntity,
                                     UUID currentOwner, String dimension, BlockPoint position) {
        if (ownerId == null) return new BondingResult(BondingResult.Status.INVALID_OWNER, null);
        if (!livingEntity) return new BondingResult(BondingResult.Status.NOT_LIVING, null);
        if (!TamingPolicy.isEligible(creatureType, true)) {
            return new BondingResult(BondingResult.Status.EXCLUDED_BOSS, null);
        }
        if (currentOwner != null) {
            return new BondingResult(currentOwner.equals(ownerId)
                    ? BondingResult.Status.ALREADY_BONDED
                    : BondingResult.Status.OWNED_BY_OTHER, null);
        }
        CompanionState state = CompanionState.newlyBonded(ownerId, Objects.requireNonNull(creatureType, "creatureType"));
        if (dimension != null && position != null) state = state.withAnchor(dimension, position);
        return new BondingResult(BondingResult.Status.BONDED, state);
    }

    public static BondingResult bond(UUID ownerId, String creatureType, boolean livingEntity,
                                     boolean explicitlyExcluded, UUID currentOwner,
                                     String dimension, BlockPoint position) {
        if (explicitlyExcluded) return new BondingResult(BondingResult.Status.EXCLUDED_BOSS, null);
        return bond(ownerId, creatureType, livingEntity, currentOwner, dimension, position);
    }
}
