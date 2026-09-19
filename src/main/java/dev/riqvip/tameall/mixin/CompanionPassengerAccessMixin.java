package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows a bonded pet to accept its owner through Entity's passenger gate. */
@Mixin(Entity.class)
public abstract class CompanionPassengerAccessMixin {
    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
    private void tameall$allowOwnerPassenger(Entity passenger, CallbackInfoReturnable<Boolean> callback) {
        Entity vehicle = (Entity) (Object) this;
        if (passenger instanceof Player player && vehicle instanceof LivingEntity living
                && CompanionAttachments.get(living)
                .filter(state -> CompanionRiding.canMount(player, living, state)).isPresent()) {
            callback.setReturnValue(true);
        }
    }
}
