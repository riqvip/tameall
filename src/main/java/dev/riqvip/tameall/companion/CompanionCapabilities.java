package dev.riqvip.tameall.companion;

import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Vex;

/** Server-derived capability flags used to keep irrelevant controls out of the UI. */
public record CompanionCapabilities(boolean sunlightProtection,
                                    boolean drowningProtection,
                                    boolean reusableExplosions,
                                    boolean preventVexExpiry,
                                    boolean endermanBlockPickup,
                                    boolean equipmentDurability) {
    public static CompanionCapabilities forEntity(LivingEntity entity) {
        boolean daylight = entity.getType().builtInRegistryHolder().is(EntityTypeTags.BURN_IN_DAYLIGHT);
        boolean drowning = !entity.canBreatheUnderwater();
        return new CompanionCapabilities(daylight, drowning, entity instanceof Creeper,
                entity instanceof Vex, entity instanceof EnderMan, true);
    }

    public static CompanionCapabilities defaults() {
        return new CompanionCapabilities(true, true, true, true, true, true);
    }
}
