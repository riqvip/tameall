package dev.riqvip.tameall.companion;

/**
 * Optional species adaptations. These are persisted per bond, rather than
 * being global server settings, so a player can deliberately keep a dangerous
 * or habitat-limited companion.
 */
public record MagicToggles(boolean sunlightProtection,
                           boolean drowningProtection,
                           boolean reusableExplosions,
                           boolean preventVexExpiry,
                           boolean allowEndermanBlockPickup) {
    /** New bonds keep survival-changing adaptations disabled. Endermen retain their vanilla pickup behavior. */
    public static final MagicToggles DEFAULT = new MagicToggles(false, false, false, false, true);

    /** Compatibility constructor for callers compiled against the former three-toggle model. */
    public MagicToggles(boolean sunlightProtection, boolean habitatAdaptation, boolean safeSpecialAbilities) {
        this(sunlightProtection, habitatAdaptation, safeSpecialAbilities, safeSpecialAbilities, !safeSpecialAbilities);
    }

    public MagicToggles withSunlightProtection(boolean value) {
        return new MagicToggles(value, drowningProtection, reusableExplosions, preventVexExpiry,
                allowEndermanBlockPickup);
    }

    public MagicToggles withDrowningProtection(boolean value) {
        return new MagicToggles(sunlightProtection, value, reusableExplosions, preventVexExpiry,
                allowEndermanBlockPickup);
    }

    /** Legacy accessor retained for old integrations; the UI calls this drowning protection. */
    public boolean habitatAdaptation() { return drowningProtection; }

    public MagicToggles withHabitatAdaptation(boolean value) {
        return withDrowningProtection(value);
    }

    public MagicToggles withReusableExplosions(boolean value) {
        return new MagicToggles(sunlightProtection, drowningProtection, value, preventVexExpiry,
                allowEndermanBlockPickup);
    }

    public MagicToggles withPreventVexExpiry(boolean value) {
        return new MagicToggles(sunlightProtection, drowningProtection, reusableExplosions, value,
                allowEndermanBlockPickup);
    }

    public MagicToggles withAllowEndermanBlockPickup(boolean value) {
        return new MagicToggles(sunlightProtection, drowningProtection, reusableExplosions, preventVexExpiry, value);
    }

    /** Legacy aggregate accessor retained while old saved data is migrated. */
    public boolean safeSpecialAbilities() { return reusableExplosions || preventVexExpiry; }

    /** Legacy aggregate mutator maps the old switch to the two protected special abilities. */
    public MagicToggles withSafeSpecialAbilities(boolean value) {
        return new MagicToggles(sunlightProtection, drowningProtection, value, value, !value);
    }
}
