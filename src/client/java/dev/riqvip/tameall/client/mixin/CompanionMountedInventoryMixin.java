package dev.riqvip.tameall.client.mixin;

import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.menu.CompanionAction;
import dev.riqvip.tameall.menu.CompanionActionRequest;
import dev.riqvip.tameall.network.CompanionActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Routes the normal inventory key to the ridden owner's companion menu. */
@Mixin(Minecraft.class)
public abstract class CompanionMountedInventoryMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void tameall$openRiddenCompanionInventory(CallbackInfo callback) {
        Minecraft client = (Minecraft) (Object) this;
        if (client.player == null || client.gui.screen() != null || client.level == null) return;
        Entity vehicle = client.player.getVehicle();
        if (!(vehicle instanceof LivingEntity living)) return;
        CompanionAttachments.getRidingView(living)
                .filter(view -> view.ownerId().equals(client.player.getUUID()))
                .ifPresent(view -> {
                    if (!client.options.keyInventory.consumeClick()) return;
                    CompanionActionRequest request = CompanionActionRequest.command(
                            view.bondId(), view.revision(), CompanionAction.OPEN_INVENTORY);
                    ClientPlayNetworking.send(new CompanionActionPayload(request));
                    callback.cancel();
                });
    }
}
