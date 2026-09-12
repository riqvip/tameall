package dev.riqvip.tameall.companion;

import java.util.Objects;

/**
 * Lifecycle transitions that must preserve one bond identity. Native entity
 * conversion hooks call these transitions and then copy the returned metadata
 * to the replacement entity.
 */
public final class CompanionLifecycle {
    private CompanionLifecycle() {}

    /** Conversion (zombie to drowned, adult growth, etc.) keeps the same bond. */
    public static CompanionState converted(CompanionState oldState, String newCreatureType) {
        Objects.requireNonNull(oldState, "oldState");
        if (!TamingPolicy.isEligible(newCreatureType, true)) {
            throw new IllegalArgumentException("converted type is not an eligible living creature");
        }
        return new CompanionState(oldState.bondId(), oldState.ownerId(), newCreatureType,
                oldState.displayName(), oldState.settings(), oldState.anchorDimension(), oldState.anchor(),
                false, oldState.revision() + 1);
    }

    /** Splitting mobs must not clone a bond; the original remains the one companion. */
    public static CompanionState onSplit(CompanionState original) {
        return Objects.requireNonNull(original, "original");
    }

    public static CompanionState died(CompanionState state) {
        return Objects.requireNonNull(state, "state").markDead();
    }
}
