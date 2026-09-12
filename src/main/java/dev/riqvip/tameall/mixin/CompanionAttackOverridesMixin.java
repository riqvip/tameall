package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Covers vanilla entities that override Mob#doHurtTarget.  A mixin on Mob
 * alone cannot intercept those virtual overrides, which would otherwise let
 * a stale target produce a special attack (sting, knockback, hunger, etc.)
 * even though the normal damage event rejects it.
 */
@Mixin({
        Bee.class,
        IronGolem.class,
        Panda.class,
        Creeper.class,
        Ravager.class,
        Zoglin.class,
        Creaking.class,
        Hoglin.class,
        CaveSpider.class,
        WitherSkeleton.class,
        Husk.class,
        Zombie.class
})
public abstract class CompanionAttackOverridesMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void tameall$preventProtectedOverrideAttack(ServerLevel level, Entity target,
                                                         CallbackInfoReturnable<Boolean> callback) {
        if (target instanceof LivingEntity living
                && CompanionCombat.rejectsAttack((net.minecraft.world.entity.Mob) (Object) this, living)) {
            callback.setReturnValue(false);
        }
    }
}
