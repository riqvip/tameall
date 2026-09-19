package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Iron Golem overrides Mob#canAttack, so it needs the native-target guard. */
@Mixin(IronGolem.class)
public abstract class GolemMixin {
    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void tameall$preventUnprovokedPetTarget(LivingEntity target,
                                                     CallbackInfoReturnable<Boolean> callback) {
        Mob golem = (Mob) (Object) this;
        // Iron Golem's vanilla override refuses creepers and player-created
        // player targets. A bonded, explicitly managed target has already
        // passed TameAll's friendship/PvP policy, so let the native attack
        // goal proceed for that one authorized case.
        if (CompanionCombat.isCompanion(golem)
                && CompanionCombat.isManagedTarget(golem, target)
                && !CompanionCombat.rejectsAttack(golem, target)) {
            callback.setReturnValue(true);
        } else if (CompanionCombat.rejectsNativeTarget(golem, target)) {
            callback.setReturnValue(false);
        }
    }
}
