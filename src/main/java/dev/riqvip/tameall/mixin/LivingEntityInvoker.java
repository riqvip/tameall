package dev.riqvip.tameall.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accesses the protected vanilla armor hook after the call is redirected. */
@Mixin(LivingEntity.class)
public interface LivingEntityInvoker {
    @Invoker("hurtArmor")
    void tameall$invokeHurtArmor(DamageSource source, float amount);
}
