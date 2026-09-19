package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player-controlled vehicle normally skips the server AI phase because the
 * client owns its movement. Companions still need that phase for native melee,
 * ranged, and special attack goals, while movement remains on vanilla vehicle
 * networking. This mixin re-enables only that narrow attack path.
 */
@Mixin(LivingEntity.class)
public abstract class CompanionMountedAiMixin {
    @Inject(method = "aiStep", at = @At("HEAD"))
    private void tameall$prepareMountedCombat(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level() instanceof ServerLevel level && entity instanceof net.minecraft.world.entity.Mob mob) {
            CompanionRiding.prepareMountedCombat(level, mob);
        }
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;isEffectiveAi()Z"))
    private boolean tameall$allowMountedAttackAi(LivingEntity entity) {
        return entity.isEffectiveAi() || CompanionRiding.shouldRunMountedAi(entity);
    }
}
