package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.companion.CompanionState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops a bonded Enderman from removing a block when its pickup option is off. */
@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EndermanPickupMixin extends Goal {
    @Shadow @Final private EnderMan enderman;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tameall$disableCompanionPickup(CallbackInfoReturnable<Boolean> callback) {
        CompanionState state = CompanionAttachments.get(enderman).orElse(null);
        if (state != null && !state.dead() && !state.settings().magic().allowEndermanBlockPickup()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tameall$disableActiveCompanionPickup(org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
        CompanionState state = CompanionAttachments.get(enderman).orElse(null);
        if (state != null && !state.dead() && !state.settings().magic().allowEndermanBlockPickup()) {
            callback.cancel();
        }
    }
}
