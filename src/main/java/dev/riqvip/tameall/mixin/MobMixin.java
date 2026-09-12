package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionDurability;
import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fills the durability paths that vanilla leaves player-only for mobs. */
@Mixin(Mob.class)
public abstract class MobMixin {
    /** Reject owner/pet and every unmanaged native target before vanilla stores it. */
    @Inject(method = "asValidTarget", at = @At("HEAD"), cancellable = true)
    private void tameall$rejectProtectedTarget(LivingEntity target,
                                                 CallbackInfoReturnable<LivingEntity> callback) {
        if (CompanionCombat.rejectsNativeTarget((Mob) (Object) this, target)) callback.setReturnValue(null);
    }

    /**
     * Brain and custom entity code sometimes checks canAttack directly without
     * going through Mob#setTarget. Keep that path consistent with the target
     * setter so native goals cannot select an unmanaged target after taming.
     */
    @Inject(method = "canAttack", at = @At("HEAD"), cancellable = true)
    private void tameall$rejectProtectedCanAttack(LivingEntity target,
                                                    CallbackInfoReturnable<Boolean> callback) {
        if (target instanceof LivingEntity living
                && CompanionCombat.rejectsAttack((Mob) (Object) this, living)) callback.setReturnValue(false);
    }

    /**
     * Clear target/anger state on both sides of the vanilla AI phase.  The
     * runtime's own target assignment runs later in the level tick and is
     * therefore preserved when it is legal.
     */
    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void tameall$sanitizeBeforeAi(CallbackInfo callback) {
        CompanionCombat.sanitize((Mob) (Object) this);
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void tameall$sanitizeAfterAi(CallbackInfo callback) {
        CompanionCombat.sanitize((Mob) (Object) this);
    }

    /** Last-resort guard for custom goals that bypass the normal target field. */
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void tameall$preventProtectedAttack(ServerLevel level, Entity target,
                                                 CallbackInfoReturnable<Boolean> callback) {
        if (target instanceof LivingEntity living
                && CompanionCombat.rejectsAttack((Mob) (Object) this, living)) callback.setReturnValue(false);
    }

    @Inject(method = "burnUndead", at = @At("HEAD"), cancellable = true)
    private void tameall$protectFromSun(CallbackInfo callback) {
        if (CompanionDurability.protectsFromSun((Mob) (Object) this)) callback.cancel();
    }

    @Inject(method = "doHurtTarget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;hurtEnemy(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z",
            shift = At.Shift.AFTER))
    private void tameall$postWeaponHit(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.entity.Entity target,
                                       org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> callback) {
        Mob mob = (Mob) (Object) this;
        if (target instanceof LivingEntity living) CompanionDurability.postWeaponHit(mob, living);
    }

    @Redirect(method = "burnUndead", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V"))
    private void tameall$guardSunDamage(ItemStack stack, int value) {
        Mob mob = (Mob) (Object) this;
        if (!CompanionDurability.suppressDurability(mob)) stack.setDamageValue(value);
    }
}
