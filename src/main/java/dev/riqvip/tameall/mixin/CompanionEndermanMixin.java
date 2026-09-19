package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ridden Endermen cannot teleport away from their owner. */
@Mixin(EnderMan.class)
public abstract class CompanionEndermanMixin {
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
    private void tameall$blockRiddenTeleport(CallbackInfoReturnable<Boolean> callback) {
        if (CompanionRiding.isControlled((EnderMan) (Object) this)) callback.setReturnValue(false);
    }
}
