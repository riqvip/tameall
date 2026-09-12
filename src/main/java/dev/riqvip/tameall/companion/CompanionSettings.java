package dev.riqvip.tameall.companion;

import java.util.Objects;

/** Persisted user-facing settings for one bond. */
public record CompanionSettings(CompanionMode mode,
                                CombatStance stance,
                                MagicToggles magic,
                                NameplateMode nameplate,
                                HealthDisplayMode healthDisplay,
                                int guardRadius,
                                TargetFilter targetFilter,
                                TeleportMode teleportMode,
                                boolean pickupItems,
                                double pickupRadius,
                                boolean collectXpForMending,
                                double xpRadius,
                                boolean useDurability) {
    public static final int MIN_GUARD_RADIUS = 4;
    public static final int DEFAULT_GUARD_RADIUS = 16;
    /** Shared Follow/Guard operating area; the UI deliberately accepts three digits. */
    public static final int MAX_GUARD_RADIUS = 999;
    public static final int MIN_AREA_RADIUS = MIN_GUARD_RADIUS;
    public static final int DEFAULT_AREA_RADIUS = DEFAULT_GUARD_RADIUS;
    public static final int MAX_AREA_RADIUS = MAX_GUARD_RADIUS;
    public static final double MIN_PICKUP_RADIUS = 1.0D;
    public static final double DEFAULT_PICKUP_RADIUS = 1.4D;
    public static final double MAX_PICKUP_RADIUS = 16.0D;
    public static final double DEFAULT_XP_RADIUS = 4.0D;
    public static final double MAX_XP_RADIUS = 16.0D;

    public CompanionSettings {
        mode = Objects.requireNonNull(mode, "mode");
        stance = Objects.requireNonNull(stance, "stance");
        magic = Objects.requireNonNull(magic, "magic");
        nameplate = Objects.requireNonNull(nameplate, "nameplate");
        healthDisplay = Objects.requireNonNull(healthDisplay, "healthDisplay");
        targetFilter = Objects.requireNonNull(targetFilter, "targetFilter");
        teleportMode = Objects.requireNonNull(teleportMode, "teleportMode");
        guardRadius = Math.clamp(guardRadius, MIN_GUARD_RADIUS, MAX_GUARD_RADIUS);
        if (!Double.isFinite(pickupRadius)) pickupRadius = DEFAULT_PICKUP_RADIUS;
        pickupRadius = Math.clamp(pickupRadius, MIN_PICKUP_RADIUS, MAX_PICKUP_RADIUS);
        if (!Double.isFinite(xpRadius)) xpRadius = DEFAULT_XP_RADIUS;
        xpRadius = Math.clamp(xpRadius, MIN_PICKUP_RADIUS, MAX_XP_RADIUS);
    }

    /** Compatibility constructor for integrations from the previous release. */
    public CompanionSettings(CompanionMode mode, CombatStance stance, MagicToggles magic,
                             NameplateMode nameplate, HealthDisplayMode healthDisplay, int guardRadius,
                             TargetFilter targetFilter, TeleportMode teleportMode, boolean pickupItems,
                             double pickupRadius, boolean useDurability) {
        this(mode, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, false, DEFAULT_XP_RADIUS, useDurability);
    }

    public static CompanionSettings defaults() {
        return new CompanionSettings(CompanionMode.FOLLOW, CombatStance.ASSIST,
                MagicToggles.DEFAULT, NameplateMode.WHEN_TARGETED,
                HealthDisplayMode.ON, DEFAULT_GUARD_RADIUS, TargetFilter.HOSTILE_ONLY,
                TeleportMode.OUTSIDE_AREA, false, DEFAULT_PICKUP_RADIUS, false, DEFAULT_XP_RADIUS, true);
    }

    public int areaRadius() { return guardRadius; }
    public CompanionSettings withAreaRadius(int value) { return withGuardRadius(value); }

    public CompanionSettings withMode(CompanionMode value) {
        return copy(value, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withStance(CombatStance value) {
        return copy(mode, value, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withMagic(MagicToggles value) {
        return copy(mode, stance, value, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withNameplate(NameplateMode value) {
        return copy(mode, stance, magic, value, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withHealthDisplay(HealthDisplayMode value) {
        return copy(mode, stance, magic, nameplate, value, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withGuardRadius(int value) {
        return copy(mode, stance, magic, nameplate, healthDisplay, value, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withTargetFilter(TargetFilter value) {
        return copy(mode, stance, magic, nameplate, healthDisplay, guardRadius, value,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withTeleportMode(TeleportMode value) {
        return copy(mode, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                value, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withPickupItems(boolean value) {
        return copy(mode, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, value, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withPickupRadius(double value) {
        return copy(mode, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, value, collectXpForMending, xpRadius, useDurability);
    }

    public CompanionSettings withCollectXpForMending(boolean value) {
        return copy(mode, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, value, xpRadius, useDurability);
    }

    public CompanionSettings withXpRadius(double value) {
        return copy(mode, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, value, useDurability);
    }

    public CompanionSettings withUseDurability(boolean value) {
        return copy(mode, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, value);
    }

    private static CompanionSettings copy(CompanionMode mode, CombatStance stance, MagicToggles magic,
                                          NameplateMode nameplate, HealthDisplayMode healthDisplay,
                                          int guardRadius, TargetFilter targetFilter, TeleportMode teleportMode,
                                          boolean pickupItems, double pickupRadius, boolean collectXpForMending,
                                          double xpRadius, boolean useDurability) {
        return new CompanionSettings(mode, stance, magic, nameplate, healthDisplay, guardRadius, targetFilter,
                teleportMode, pickupItems, pickupRadius, collectXpForMending, xpRadius, useDurability);
    }
}
