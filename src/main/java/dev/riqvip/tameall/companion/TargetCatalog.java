package dev.riqvip.tameall.companion;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Server-built list of target groups and living entity types shown by the selector. */
public final class TargetCatalog {
    public record Entry(String id, String label, String kind, List<String> categories) {
        public Entry(String id, String label, String kind) {
            this(id, label, kind, List.of());
        }

        public Entry {
            if (id == null || id.isBlank() || id.length() > TargetSelection.MAX_ENTRY_LENGTH) {
                throw new IllegalArgumentException("invalid target catalog id");
            }
            if (label == null || label.length() > 256) throw new IllegalArgumentException("invalid target catalog label");
            if (kind == null || kind.length() > 32) throw new IllegalArgumentException("invalid target catalog kind");
            categories = categories == null ? List.of() : List.copyOf(categories);
            if (categories.size() > 16 || categories.stream().anyMatch(value -> value == null || value.length() > 64)) {
                throw new IllegalArgumentException("invalid target catalog categories");
            }
        }
    }

    private static final Map<String, String> GROUPS = Map.ofEntries(
            Map.entry(TargetSelection.GROUP_HOSTILE, "Hostile mobs"),
            Map.entry(TargetSelection.GROUP_ALL_LIVING, "All living"),
            Map.entry(TargetSelection.GROUP_PLAYERS, "Players"),
            Map.entry(TargetSelection.GROUP_ILLAGERS, "Illagers"),
            Map.entry(TargetSelection.GROUP_RAIDERS, "Raiders"),
            Map.entry(TargetSelection.GROUP_UNDEAD, "Undead"),
            Map.entry(TargetSelection.GROUP_ZOMBIES, "Zombies"),
            Map.entry(TargetSelection.GROUP_SKELETONS, "Skeletons"),
            Map.entry(TargetSelection.GROUP_AQUATIC, "Aquatic"),
            Map.entry(TargetSelection.GROUP_ARTHROPODS, "Arthropods"));
    private static final List<String> GROUP_ORDER = List.of(
            TargetSelection.GROUP_HOSTILE, TargetSelection.GROUP_ALL_LIVING,
            TargetSelection.GROUP_PLAYERS, TargetSelection.GROUP_ILLAGERS,
            TargetSelection.GROUP_RAIDERS, TargetSelection.GROUP_UNDEAD,
            TargetSelection.GROUP_ZOMBIES, TargetSelection.GROUP_SKELETONS,
            TargetSelection.GROUP_AQUATIC, TargetSelection.GROUP_ARTHROPODS);

    private TargetCatalog() {}

    public static List<Entry> entries(ServerLevel level) {
        Map<String, Entry> result = new LinkedHashMap<>();
        for (String id : GROUP_ORDER) {
            result.put(id, new Entry(id, GROUPS.get(id), "group", List.of("groups")));
        }
        // Classify without adding anything to the world.  This keeps arrows,
        // boats, projectiles, and decorative entities out of a combat selector.
        BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().identifier().toString()))
                .forEach(entry -> {
                    Entity created = null;
                    try { created = entry.getValue().create(level, EntitySpawnReason.COMMAND); }
                    catch (RuntimeException ignored) { }
                    if (!(created instanceof LivingEntity living)) {
                        if (created != null) created.discard();
                        return;
                    }
                    List<String> categories = GROUP_ORDER.stream()
                            .filter(id -> TargetMatcher.matchesEntry(level, living, id))
                            .toList();
                    if (categories.isEmpty()) categories = List.of("uncategorized");
                    String id = "type:" + entry.getKey().identifier();
                    result.putIfAbsent(id, new Entry(id, entry.getValue().getDescription().getString(), "type", categories));
                    created.discard();
                });
        level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).listTagIds()
                .map(tag -> "tag:" + tag.location())
                .sorted()
                .forEach(id -> result.putIfAbsent(id, new Entry(id, id.substring("tag:".length()), "tag", List.of("advanced"))));
        return result.values().stream().limit(512).toList();
    }

    public static boolean isKnownGroup(String id) {
        return GROUPS.containsKey(id);
    }

    public static boolean isWellFormedEntry(String id) {
        if (id == null || id.length() > TargetSelection.MAX_ENTRY_LENGTH || id.isBlank()) return false;
        if (isKnownGroup(id)) return true;
        if (id.startsWith("type:") || id.startsWith("tag:")) {
            return Identifier.tryParse(id.substring(id.indexOf(':') + 1)) != null;
        }
        return false;
    }

    public static List<Entry> trimForWire(List<Entry> entries) {
        if (entries == null || entries.size() > 512) throw new IllegalArgumentException("target catalog too large");
        return List.copyOf(new ArrayList<>(entries));
    }
}
