package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionDurability;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Allows bonded non-player entities to wear shields through the vanilla component. */
@Mixin(BlocksAttacks.class)
public abstract class BlocksAttacksMixin {
    @Inject(method = "hurtBlockingItem", at = @At("HEAD"), cancellable = true)
    private void tameall$hurtCompanionShield(Level level, ItemStack stack, LivingEntity wielder,
                                             InteractionHand hand, float blockedDamage, CallbackInfo callback) {
        if (CompanionDurability.isCompanion(wielder)) {
            CompanionDurability.hurtBlockingItem((BlocksAttacks) (Object) this, level, stack,
                    wielder, hand, blockedDamage);
            callback.cancel();
        }
    }
}
