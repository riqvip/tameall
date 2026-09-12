package dev.riqvip.tameall.companion;

import java.util.UUID;

/** Pure target-policy helpers shared by runtime logic and regression tests. */
public final class TargetPolicy {
    private TargetPolicy() {}

    public static boolean mayAssist(CombatStance stance, UUID ownerId, UUID companionId,
                                    UUID targetId, UUID targetOwnerId, boolean ownerSignal) {
        if (stance != CombatStance.ASSIST || targetId == null) return false;
        return ownerSignal && !isProtected(ownerId, companionId, targetId, targetOwnerId);
    }

    public static boolean mayDefend(CombatStance stance, UUID ownerId, UUID companionId,
                                    UUID targetId, UUID targetOwnerId, boolean hostile,
                                    TargetFilter filter) {
        if (stance != CombatStance.DEFEND_AREA || targetId == null) return false;
        if (isProtected(ownerId, companionId, targetId, targetOwnerId)) return false;
        return filter == TargetFilter.ALL_LIVING || hostile;
    }

    public static boolean mayAttack(CombatStance stance, UUID ownerId, UUID companionId,
                                    UUID targetId, UUID targetOwnerId, boolean targetIsHostile,
                                    boolean targetIsOwnerTarget) {
        return targetIsOwnerTarget
                ? mayAssist(stance, ownerId, companionId, targetId, targetOwnerId, true)
                : mayDefend(stance, ownerId, companionId, targetId, targetOwnerId,
                        targetIsHostile, TargetFilter.HOSTILE_ONLY);
    }

    public static boolean mayRetaliate(CombatStance stance, UUID ownerId, UUID companionId,
                                       UUID attackerId, UUID attackerOwnerId) {
        return mayAssist(stance, ownerId, companionId, attackerId, attackerOwnerId, true);
    }

    private static boolean isProtected(UUID ownerId, UUID companionId, UUID targetId, UUID targetOwnerId) {
        return ownerId.equals(targetId) || companionId.equals(targetId) || targetOwnerId != null;
    }
}
