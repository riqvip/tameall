package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents any vanilla target goal from starting against an unassigned pet target. */
@Mixin(TargetGoal.class)
public abstract class TargetGoalMixin {
    @Shadow protected net.minecraft.world.entity.LivingEntity targetMob;
    @Shadow @Final protected net.minecraft.world.entity.Mob mob;

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void tameall$blockNativeStart(CallbackInfo callback) {
        if (CompanionCombat.rejectsNativeTarget(mob, targetMob)) {
            CompanionCombat.clearManagedTarget(mob);
            callback.cancel();
        }
    }
}
