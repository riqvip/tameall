package dev.riqvip.tameall.companion;

/** Automatic catch-up policy. Manual recall from the Companion Whistle ignores this setting. */
public enum TeleportMode {
    /** Teleport when the companion leaves twice its configured follow/guard area. */
    OUTSIDE_AREA,
    /** Teleport only after it is more than 32 blocks from its owner. */
    FAR_FROM_OWNER,
    /** Never automatically cross dimensions or catch up by teleporting. */
    NEVER
}
