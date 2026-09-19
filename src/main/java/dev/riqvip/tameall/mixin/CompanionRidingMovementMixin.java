package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives added companion mounts the same travel hooks used by vanilla mounts. */
@Mixin(LivingEntity.class)
public abstract class CompanionRidingMovementMixin {
    @Inject(method = "getRiddenInput", at = @At("HEAD"), cancellable = true)
    private void tameall$addedRiddenInput(Player rider, Vec3 input,
                                          CallbackInfoReturnable<Vec3> callback) {
        LivingEntity vehicle = (LivingEntity) (Object) this;
        if (CompanionRiding.isAddedController(vehicle, rider)) {
            callback.setReturnValue(CompanionRiding.riddenInput(vehicle, rider, input));
        }
    }

    @Inject(method = "getRiddenSpeed", at = @At("RETURN"), cancellable = true)
    private void tameall$addedRiddenSpeed(Player rider, CallbackInfoReturnable<Float> callback) {
        LivingEntity vehicle = (LivingEntity) (Object) this;
        if (CompanionRiding.isAddedController(vehicle, rider)) {
            callback.setReturnValue(CompanionRiding.riddenSpeed(vehicle, callback.getReturnValue()));
        }
    }

    @Inject(method = "tickRidden", at = @At("HEAD"))
    private void tameall$tickAddedRider(Player rider, Vec3 input, CallbackInfo callback) {
        CompanionRiding.tickRidden((LivingEntity) (Object) this, rider);
    }
}
