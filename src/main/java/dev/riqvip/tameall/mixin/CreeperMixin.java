package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRuntime;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives bonded creepers a safe reusable blast and records lethal deaths. */
@Mixin(Creeper.class)
public abstract class CreeperMixin {
    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void tameall$handleCompanionExplosion(CallbackInfo callback) {
        Creeper creeper = (Creeper) (Object) this;
        if (CompanionRuntime.handleSafeCreeperExplosion(creeper)) {
            callback.cancel();
        } else {
            // Vanilla discards creepers directly from this method, so the
            // ordinary AFTER_DEATH callback cannot be the source of truth.
            CompanionRuntime.handleLethalCreeperExplosion(creeper);
        }
    }
}
