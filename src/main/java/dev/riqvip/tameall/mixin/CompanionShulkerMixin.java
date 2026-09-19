package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ridden stationary shulkers do not relocate to another attachment face. */
@Mixin(Shulker.class)
public abstract class CompanionShulkerMixin {
    @Inject(method = "teleportSomewhere", at = @At("HEAD"), cancellable = true)
    private void tameall$blockRiddenTeleport(CallbackInfoReturnable<Boolean> callback) {
        if (CompanionRiding.isControlled((Shulker) (Object) this)) callback.setReturnValue(false);
    }
}
