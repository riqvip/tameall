package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRuntime;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes permanent entity removal so journal status is not based on stale UUIDs. */
@Mixin(Entity.class)
public abstract class EntityRemovalMixin {
    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void tameall$observeRemoval(Entity.RemovalReason reason, CallbackInfo callback) {
        CompanionRuntime.onEntityRemoved((Entity) (Object) this, reason);
    }
}
