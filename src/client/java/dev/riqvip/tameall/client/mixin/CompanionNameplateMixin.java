package dev.riqvip.tameall.client.mixin;

import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.companion.NameplateMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes a companion's When Targeted nameplate mean the crosshair target. */
@Mixin(LivingEntityRenderer.class)
public abstract class CompanionNameplateMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void tameall$showHoveredName(LivingEntity entity, double distance,
                                          CallbackInfoReturnable<Boolean> callback) {
        CompanionAttachments.getPresentation(entity).ifPresent(presentation -> {
            if (presentation.nameplate() == NameplateMode.WHEN_TARGETED) {
                callback.setReturnValue(Minecraft.getInstance().getEntityRenderDispatcher().crosshairPickEntity == entity);
            }
        });
    }
}
