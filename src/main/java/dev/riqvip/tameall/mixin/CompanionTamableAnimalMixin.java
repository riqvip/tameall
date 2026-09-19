package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionAttachments;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents vanilla wolf/cat/parrot taming from being layered onto a bond. */
@Mixin(TamableAnimal.class)
public abstract class CompanionTamableAnimalMixin {
    @Inject(method = "tame", at = @At("HEAD"), cancellable = true)
    private void tameall$blockNativeTaming(Player player, CallbackInfo callback) {
        TamableAnimal animal = (TamableAnimal) (Object) this;
        if (CompanionAttachments.get(animal)
                .map(state -> !state.dead())
                .orElse(false)) {
            callback.cancel();
        }
    }
}
