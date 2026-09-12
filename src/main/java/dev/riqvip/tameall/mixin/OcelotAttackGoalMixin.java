package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.OcelotAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Covers the ocelot's custom cached-target attack goal. */
@Mixin(OcelotAttackGoal.class)
public abstract class OcelotAttackGoalMixin {
    @Shadow @Final private Mob mob;
    @Shadow private LivingEntity target;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tameall$preventProtectedPounce(CallbackInfo callback) {
        if (CompanionCombat.rejectsAttack(mob, target)) {
            target = null;
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            callback.cancel();
        }
    }
}
