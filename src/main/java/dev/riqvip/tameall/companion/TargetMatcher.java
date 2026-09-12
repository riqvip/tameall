package dev.riqvip.tameall.companion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

/** Resolves target-selection ids against the server's current registry and tag set. */
public final class TargetMatcher {
    private TargetMatcher() {}

    public static boolean matches(ServerLevel level, LivingEntity candidate, TargetSelection selection) {
        if (candidate == null || selection == null || selection.includes().isEmpty()) return false;
        for (String excluded : selection.excludes()) {
            if (matchesEntry(level, candidate, excluded)) return false;
        }
        for (String included : selection.includes()) {
            if (matchesEntry(level, candidate, included)) return true;
        }
        return false;
    }

    public static boolean matchesEntry(ServerLevel level, LivingEntity candidate, String id) {
        if (id == null || candidate == null) return false;
        if (TargetSelection.GROUP_HOSTILE.equals(id)) return candidate instanceof Monster;
        if (TargetSelection.GROUP_ALL_LIVING.equals(id)) return candidate instanceof LivingEntity;
        if (TargetSelection.GROUP_PLAYERS.equals(id)) return candidate instanceof Player;
        if (TargetSelection.GROUP_ILLAGERS.equals(id)) return candidate.getType().builtInRegistryHolder().is(EntityTypeTags.ILLAGER);
        if (TargetSelection.GROUP_RAIDERS.equals(id)) return candidate.getType().builtInRegistryHolder().is(EntityTypeTags.RAIDERS);
        if (TargetSelection.GROUP_UNDEAD.equals(id)) return candidate.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD);
        if (TargetSelection.GROUP_ZOMBIES.equals(id)) return candidate.getType().builtInRegistryHolder().is(EntityTypeTags.ZOMBIES);
        if (TargetSelection.GROUP_SKELETONS.equals(id)) return candidate.getType().builtInRegistryHolder().is(EntityTypeTags.SKELETONS);
        if (TargetSelection.GROUP_AQUATIC.equals(id)) return candidate.getType().builtInRegistryHolder().is(EntityTypeTags.AQUATIC);
        if (TargetSelection.GROUP_ARTHROPODS.equals(id)) return candidate.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD);
        if (id.startsWith("type:")) {
            Identifier identifier = Identifier.tryParse(id.substring("type:".length()));
            return identifier != null && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(identifier)
                    .map(holder -> holder.value() == candidate.getType()).orElse(false);
        }
        if (id.startsWith("tag:")) {
            Identifier identifier = Identifier.tryParse(id.substring("tag:".length()));
            if (identifier == null) return false;
            TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, identifier);
            return candidate.getType().builtInRegistryHolder().is(tag);
        }
        return false;
    }
}
