package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionAttachments;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps a TameAll horse from gaining a second, native tame state by bucking. */
@Mixin(AbstractHorse.class)
public abstract class CompanionHorseTamingMixin {
    @Inject(method = "tameWithName", at = @At("HEAD"), cancellable = true)
    private void tameall$blockNativeHorseTaming(Player player, CallbackInfoReturnable<Boolean> callback) {
        AbstractHorse horse = (AbstractHorse) (Object) this;
        if (CompanionAttachments.get(horse)
                .map(state -> !state.dead())
                .orElse(false)) {
            callback.setReturnValue(false);
        }
    }
}
