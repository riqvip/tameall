package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents cached bow goals from firing at an owner or another companion. */
@Mixin(RangedBowAttackGoal.class)
public abstract class RangedBowAttackGoalMixin {
    @Shadow @Final private Monster mob;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tameall$preventProtectedArrow(CallbackInfo callback) {
        if (!CompanionCombat.isAttackTargetValid(mob, mob.getTarget())) {
            if (mob.isUsingItem()) mob.stopUsingItem();
            callback.cancel();
        }
    }
}
