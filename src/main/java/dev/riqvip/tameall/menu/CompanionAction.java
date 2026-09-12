package dev.riqvip.tameall.menu;

/** Explicit commands accepted by the companion menu. */
public enum CompanionAction {
    RENAME,
    SET_MODE,
    SET_STANCE,
    SET_NAMEPLATE,
    SET_HEALTH_DISPLAY,
    SET_GUARD_RADIUS,
    SET_TARGET_FILTER,
    SET_GUARD_ANCHOR,
    SET_TELEPORT_MODE,
    SET_PICKUP_ITEMS,
    SET_PICKUP_RADIUS,
    SET_COLLECT_XP,
    SET_XP_RADIUS,
    SET_USE_DURABILITY,
    SET_SUNLIGHT_PROTECTION,
    SET_DROWNING_PROTECTION,
    SET_REUSABLE_EXPLOSIONS,
    SET_PREVENT_VEX_EXPIRY,
    SET_ENDERMAN_BLOCK_PICKUP,
    OPEN_INVENTORY,
    OPEN_TARGET_SELECTOR,
    /** Legacy wire names accepted for old clients and converted server-side. */
    SET_HABITAT_ADAPTATION,
    SET_SAFE_SPECIAL_ABILITIES
}
