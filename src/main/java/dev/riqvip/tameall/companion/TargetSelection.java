package dev.riqvip.tameall.companion;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit include/exclude entries for proactive DEFEND_AREA targeting.
 * Entries are stable ids such as {@code group:hostile},
 * {@code tag:minecraft:undead}, and {@code type:minecraft:zombie}.
 * Exclusions always win over inclusions.
 */
public record TargetSelection(Set<String> includes, Set<String> excludes) {
    public static final int MAX_ENTRIES_PER_SIDE = 128;
    public static final int MAX_ENTRY_LENGTH = 256;
    public static final String GROUP_HOSTILE = "group:hostile";
    public static final String GROUP_ALL_LIVING = "group:all_living";
    public static final String GROUP_PLAYERS = "group:players";
    public static final String GROUP_ILLAGERS = "group:illagers";
    public static final String GROUP_RAIDERS = "group:raiders";
    public static final String GROUP_UNDEAD = "group:undead";
    public static final String GROUP_ZOMBIES = "group:zombies";
    public static final String GROUP_SKELETONS = "group:skeletons";
    public static final String GROUP_AQUATIC = "group:aquatic";
    public static final String GROUP_ARTHROPODS = "group:arthropods";

    public TargetSelection {
        includes = normalize(includes, "includes");
        excludes = normalize(excludes, "excludes");
    }

    public static TargetSelection hostileOnly() {
        return new TargetSelection(Set.of(GROUP_HOSTILE), Set.of());
    }

    public static TargetSelection allLiving() {
        return new TargetSelection(Set.of(GROUP_ALL_LIVING), Set.of());
    }

    public static TargetSelection fromLegacy(TargetFilter filter) {
        return filter == TargetFilter.ALL_LIVING ? allLiving() : hostileOnly();
    }

    public boolean isLegacyHostileOnly() {
        return this.equals(hostileOnly());
    }

    public boolean isLegacyAllLiving() {
        return this.equals(allLiving());
    }

    public boolean isEmpty() { return includes.isEmpty(); }

    public String summary() {
        if (isLegacyHostileOnly()) return "hostile";
        if (isLegacyAllLiving()) return "all living";
        return includes.size() + " include / " + excludes.size() + " exclude";
    }

    public static boolean isValidShape(Collection<String> values) {
        if (values == null || values.size() > MAX_ENTRIES_PER_SIDE) return false;
        for (String value : values) {
            if (value == null || value.isBlank() || value.length() > MAX_ENTRY_LENGTH) return false;
        }
        return true;
    }

    private static Set<String> normalize(Set<String> values, String name) {
        Objects.requireNonNull(values, name);
        if (!isValidShape(values)) throw new IllegalArgumentException("invalid target selection " + name);
        return Set.copyOf(new LinkedHashSet<>(values));
    }

    /** Wire/codec helper that preserves deterministic order for the screen. */
    public List<String> includeList() { return List.copyOf(includes); }
    public List<String> excludeList() { return List.copyOf(excludes); }
}
