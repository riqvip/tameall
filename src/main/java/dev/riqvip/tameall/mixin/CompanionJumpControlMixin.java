package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.JumpControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents autonomous jump requests from competing with the rider. */
@Mixin(JumpControl.class)
public abstract class CompanionJumpControlMixin {
    @Shadow @Final private Mob mob;
    @Shadow private boolean jump;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tameall$blockAutonomousJump(CallbackInfo callback) {
        if (CompanionRiding.isControlled(mob)) {
            jump = false;
            callback.cancel();
        }
    }
}
