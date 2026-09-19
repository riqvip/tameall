package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops a ranged goal that cached a protected target before it was cleared. */
@Mixin(RangedAttackGoal.class)
public abstract class RangedAttackGoalMixin {
    @Shadow @Final private Mob mob;
    @Shadow private LivingEntity target;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tameall$preventProtectedProjectile(CallbackInfo callback) {
        if (!cachedTargetIsValid()) {
            callback.cancel();
        }
    }

    /**
     * Vanilla's canContinueToUse() calls target.isAlive() after canUse() has
     * returned false. Stop the goal before that dereference when a cached
     * target was cleared or became unauthorized.
     */
    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void tameall$guardCachedTarget(CallbackInfoReturnable<Boolean> callback) {
        if (!cachedTargetIsValid() || (mob.getTarget() != null && mob.getTarget() != target)) {
            callback.setReturnValue(false);
        }
    }

    private boolean cachedTargetIsValid() {
        return CompanionCombat.isAttackTargetValid(mob, target);
    }
}
