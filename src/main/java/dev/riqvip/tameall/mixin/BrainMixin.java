package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps Brain-powered companions from retaining an owner or another pet. */
@Mixin(Brain.class)
public abstract class BrainMixin {
    /** Clear stale memories before the behavior pass runs. */
    @Inject(method = "tickEachRunningBehavior", at = @At("HEAD"))
    private void tameall$sanitizeBeforeBehaviors(ServerLevel level, LivingEntity entity,
                                                  CallbackInfo callback) {
        if (entity instanceof Mob mob) CompanionCombat.sanitize(mob);
    }

    /** Sensors and behaviors can write memories during the pass; clean them up immediately. */
    @Inject(method = "tickEachRunningBehavior", at = @At("TAIL"))
    private void tameall$sanitizeAfterBehaviors(ServerLevel level, LivingEntity entity,
                                                 CallbackInfo callback) {
        if (entity instanceof Mob mob) CompanionCombat.sanitize(mob);
    }
}
