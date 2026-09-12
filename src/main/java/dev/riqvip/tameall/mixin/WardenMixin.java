package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Warden-specific anger and attack hooks, since it overrides Mob's methods. */
@Mixin(Warden.class)
public abstract class WardenMixin {
    @Inject(method = "canTargetEntity", at = @At("HEAD"), cancellable = true)
    private void tameall$rejectProtectedEntity(Entity target, CallbackInfoReturnable<Boolean> callback) {
        if (target instanceof LivingEntity living
                && CompanionCombat.rejectsNativeTarget((Warden) (Object) this, living)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "getEntityAngryAt", at = @At("RETURN"), cancellable = true)
    private void tameall$hideProtectedAnger(CallbackInfoReturnable<Optional<LivingEntity>> callback) {
        Warden warden = (Warden) (Object) this;
        if (callback.getReturnValue().map(target -> CompanionCombat.rejectsNativeTarget(warden, target)).orElse(false)) {
            callback.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "setAttackTarget", at = @At("HEAD"), cancellable = true)
    private void tameall$rejectProtectedAttackTarget(LivingEntity target, CallbackInfo callback) {
        if (CompanionCombat.rejectsNativeTarget((Warden) (Object) this, target)) callback.cancel();
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void tameall$preventProtectedAttack(net.minecraft.server.level.ServerLevel level,
                                                 Entity target,
                                                 CallbackInfoReturnable<Boolean> callback) {
        if (CompanionCombat.rejectsAttack((Warden) (Object) this, target)) callback.setReturnValue(false);
    }
}
