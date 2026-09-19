package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opens the vanilla passenger hooks for bonded pets while retaining ownership checks. */
@Mixin(Mob.class)
public abstract class CompanionRidingMixin {
    @Inject(method = "getControllingPassenger", at = @At("RETURN"), cancellable = true)
    private void tameall$controlBondedMount(CallbackInfoReturnable<LivingEntity> callback) {
        Mob mob = (Mob) (Object) this;
        if (callback.getReturnValue() == null && mob.getFirstPassenger() instanceof Player player
                && CompanionRiding.isAuthorizedController(mob, player)) {
            callback.setReturnValue(player);
        }
    }
}
