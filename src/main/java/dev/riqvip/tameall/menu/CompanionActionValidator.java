package dev.riqvip.tameall.menu;

import java.util.Objects;
import java.util.UUID;

/**
 * Server-side gate that must run before dispatching a menu action. Integrations supply
 * the native distance, ownership, alive-entity, and claim checks.
 */
public final class CompanionActionValidator {
    private CompanionActionValidator() {}

    public static void validate(CompanionMenuSnapshot snapshot, CompanionActionRequest request,
                                UUID playerId, boolean owner, boolean entityPresent,
                                boolean withinInteractionRange) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(playerId, "playerId");
        if (!snapshot.bondId().equals(request.bondId())) throw new IllegalArgumentException("unknown bond");
        if (snapshot.dead()) throw new IllegalArgumentException("companion is dead");
        if (!owner) throw new IllegalArgumentException("not companion owner");
        if (!entityPresent) throw new IllegalArgumentException("companion is unavailable");
        if (!withinInteractionRange) throw new IllegalArgumentException("companion is too far away");
        if (snapshot.revision() != request.expectedRevision()) throw new IllegalArgumentException("stale menu revision");
        if (!isApplicable(snapshot, request.action())) throw new IllegalArgumentException("ability does not apply to this companion");
        if (request.action() == CompanionAction.SET_GUARD_RADIUS
                && (request.number() < 4 || request.number() > 999)) {
            throw new IllegalArgumentException("guard radius out of range");
        }
        if (request.action() == CompanionAction.SET_PICKUP_RADIUS
                && (!Double.isFinite(request.decimal()) || request.decimal() < 1.0D || request.decimal() > 16.0D)) {
            throw new IllegalArgumentException("pickup radius out of range");
        }
        if (request.action() == CompanionAction.SET_XP_RADIUS
                && (!Double.isFinite(request.decimal()) || request.decimal() < 1.0D || request.decimal() > 16.0D)) {
            throw new IllegalArgumentException("xp radius out of range");
        }
    }

    private static boolean isApplicable(CompanionMenuSnapshot snapshot, CompanionAction action) {
        return switch (action) {
            case SET_SUNLIGHT_PROTECTION -> snapshot.capabilities().sunlightProtection();
            case SET_DROWNING_PROTECTION, SET_HABITAT_ADAPTATION -> snapshot.capabilities().drowningProtection();
            case SET_REUSABLE_EXPLOSIONS -> snapshot.capabilities().reusableExplosions();
            case SET_PREVENT_VEX_EXPIRY -> snapshot.capabilities().preventVexExpiry();
            case SET_ENDERMAN_BLOCK_PICKUP -> snapshot.capabilities().endermanBlockPickup();
            case SET_USE_DURABILITY -> snapshot.capabilities().equipmentDurability();
            case SET_SAFE_SPECIAL_ABILITIES -> snapshot.capabilities().reusableExplosions()
                    || snapshot.capabilities().preventVexExpiry();
            default -> true;
        };
    }
}
