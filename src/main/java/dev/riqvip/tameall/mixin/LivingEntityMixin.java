package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents vanilla loot/equipment/XP death handling from duplicating companion drops. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @org.spongepowered.asm.mixin.injection.Redirect(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void tameall$redirectArmorWear(LivingEntity entity, DamageSource source, float amount) {
        if (dev.riqvip.tameall.companion.CompanionDurability.isCompanion(entity)) {
            dev.riqvip.tameall.companion.CompanionDurability.hurtArmor(entity, source, amount);
        } else {
            ((LivingEntityInvoker) (Object) entity).tameall$invokeHurtArmor(source, amount);
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void tameall$suppressNativeDeathLoot(ServerLevel level, DamageSource source, CallbackInfo callback) {
        if (CompanionAttachments.get((LivingEntity) (Object) this).isPresent()) callback.cancel();
    }
}
