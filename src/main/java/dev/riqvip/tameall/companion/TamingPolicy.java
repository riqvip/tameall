package dev.riqvip.tameall.companion;

import java.util.Locale;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

/** Shared eligibility policy used by the interaction handler and lifecycle hooks. */
public final class TamingPolicy {
    public static final String WITHER = "minecraft:wither";
    public static final String ENDER_DRAGON = "minecraft:ender_dragon";
    public static final String PLAYER = "minecraft:player";
    /** Datapack extension point for unusual entities that should not be bonded. */
    public static final TagKey<EntityType<?>> EXCLUDED = TagKey.create(
            Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("tameall", "companion_exclusions"));

    private TamingPolicy() {}

    /**
     * A mob adapter supplies whether the target is a LivingEntity. Keeping that
     * check outside the id list means modded living creatures work by default.
     */
    public static boolean isEligible(String entityId, boolean livingEntity) {
        if (!livingEntity || entityId == null) {
            return false;
        }
        String normalized = entityId.toLowerCase(Locale.ROOT);
        return !WITHER.equals(normalized) && !ENDER_DRAGON.equals(normalized) && !PLAYER.equals(normalized);
    }

    public static boolean isEligible(String entityId, boolean livingEntity, boolean explicitlyExcluded) {
        return !explicitlyExcluded && isEligible(entityId, livingEntity);
    }
}
