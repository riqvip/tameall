package dev.riqvip.tameall.companion;

import java.util.Locale;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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

    /**
     * Vanilla has two independent tame-state implementations: TamableAnimal
     * (wolves, cats, parrots, and nautiluses) and AbstractHorse (horses,
     * llamas, donkeys, mules, and their variants). Camels inherit the horse
     * API but are intentionally always rideable rather than naturally tameable.
     */
    public static boolean isNaturallyTamed(LivingEntity entity) {
        if (entity instanceof TamableAnimal tamable) return tamable.isTame();
        return entity instanceof AbstractHorse horse
                && !(entity instanceof Camel)
                && horse.isTamed();
    }

    /**
     * Stops a TameAll bond from entering a vanilla taming interaction. This is
     * deliberately item/type-specific so owner feeding, armor, and other
     * ordinary companion interactions remain available.
     */
    public static boolean isNativeTamingInteraction(LivingEntity entity, ItemStack stack) {
        if (entity == null || stack == null) return false;
        if (entity instanceof AbstractHorse && !(entity instanceof Camel) && stack.isEmpty()) return true;
        if (stack.isEmpty()) return false;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        return switch (id) {
            case "minecraft:wolf" -> stack.is(Items.BONE);
            case "minecraft:cat" -> stack.is(ItemTags.CAT_FOOD);
            case "minecraft:parrot" -> stack.is(ItemTags.PARROT_FOOD);
            case "minecraft:nautilus", "minecraft:zombie_nautilus" ->
                    stack.is(ItemTags.NAUTILUS_TAMING_ITEMS);
            default -> false;
        };
    }
}
