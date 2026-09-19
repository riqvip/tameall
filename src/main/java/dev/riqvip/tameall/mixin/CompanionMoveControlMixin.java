package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops stale autonomous move-control output while an owner is steering. */
@Mixin(MoveControl.class)
public abstract class CompanionMoveControlMixin {
    @Shadow @Final protected Mob mob;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tameall$blockAutonomousMove(CallbackInfo callback) {
        if (CompanionRiding.isControlled(mob)) {
            ((MoveControl) (Object) this).setWait();
            callback.cancel();
        }
    }
}
