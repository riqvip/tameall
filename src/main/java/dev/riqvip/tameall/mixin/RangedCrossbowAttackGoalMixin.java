package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents cached crossbow goals from firing at a protected entity. */
@Mixin(RangedCrossbowAttackGoal.class)
public abstract class RangedCrossbowAttackGoalMixin {
    @Shadow @Final private Monster mob;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tameall$preventProtectedBolt(CallbackInfo callback) {
        if (!CompanionCombat.isAttackTargetValid(mob, mob.getTarget())) {
            if (mob.isUsingItem()) mob.stopUsingItem();
            if (mob instanceof net.minecraft.world.entity.monster.CrossbowAttackMob crossbow) {
                crossbow.setChargingCrossbow(false);
            }
            callback.cancel();
        }
    }
}
