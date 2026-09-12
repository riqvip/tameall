package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops cached melee goals from swinging at a protected entity. */
@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin {
    @Shadow @Final protected PathfinderMob mob;

    @Inject(method = "checkAndPerformAttack", at = @At("HEAD"), cancellable = true)
    private void tameall$preventProtectedSwing(LivingEntity target, CallbackInfo callback) {
        if (CompanionCombat.rejectsAttack(mob, target)) callback.cancel();
    }
}
