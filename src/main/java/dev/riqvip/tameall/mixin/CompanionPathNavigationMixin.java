package dev.riqvip.tameall.mixin;

import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents mounted attack goals from turning into autonomous pursuit. */
@Mixin(PathNavigation.class)
public abstract class CompanionPathNavigationMixin {
    @Shadow @Final protected Mob mob;

    @Inject(method = "moveTo(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
    private void tameall$blockMountedEntityPath(Entity target, double speed,
                                                 CallbackInfoReturnable<Boolean> callback) {
        if (CompanionRiding.isControlled(mob)) callback.setReturnValue(false);
    }

    @Inject(method = "moveTo(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void tameall$blockMountedPositionPath(double x, double y, double z, double speed,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if (CompanionRiding.isControlled(mob)) callback.setReturnValue(false);
    }

    @Inject(method = "moveTo(Lnet/minecraft/world/level/pathfinder/Path;D)Z", at = @At("HEAD"), cancellable = true)
    private void tameall$blockMountedPath(Path path, double speed,
                                           CallbackInfoReturnable<Boolean> callback) {
        if (CompanionRiding.isControlled(mob)) callback.setReturnValue(false);
    }
}
