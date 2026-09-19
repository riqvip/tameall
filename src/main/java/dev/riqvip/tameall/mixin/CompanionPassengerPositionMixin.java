package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Provides a consistent scaled seat for generic companion mounts. */
@Mixin(Entity.class)
public abstract class CompanionPassengerPositionMixin {
    @Inject(method = "getPassengerRidingPosition", at = @At("RETURN"), cancellable = true)
    private void tameall$scaledCompanionSeat(Entity passenger, CallbackInfoReturnable<Vec3> callback) {
        Entity vehicle = (Entity) (Object) this;
        if (passenger instanceof Player && vehicle instanceof net.minecraft.world.entity.LivingEntity living
                && CompanionAttachments.get(living).isPresent() && !CompanionRiding.usesNativeSeat(living)) {
            callback.setReturnValue(new Vec3(vehicle.getX(), vehicle.getY() + CompanionRiding.seatHeight(living), vehicle.getZ()));
        }
    }
}
