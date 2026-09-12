package dev.riqvip.tameall.client;

import dev.riqvip.tameall.menu.CompanionMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;

/** Taller vanilla inventory view for a bonded companion. */
public final class CompanionInventoryScreen extends AbstractContainerScreen<CompanionMenu> {
    public CompanionInventoryScreen(CompanionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 232);
        this.titleLabelX = 28;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 143;
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(new DarkButton(font, leftPos + 153, topPos + 4, 18, 18,
                Component.literal("×"), ignored -> onClose()));
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        CompanionUiStyle.panel(g, leftPos, topPos, imageWidth, imageHeight);
        g.fill(leftPos + 28, topPos + 18, leftPos + 96, topPos + 78, 0xFF171717);
        for (int index = 0; index < menu.slots.size(); index++) {
            if (index >= CompanionMenu.TOTAL_SIZE) break;
            var slot = menu.getSlot(index);
            int x = leftPos + slot.x - 1;
            int y = topPos + slot.y - 1;
            drawSlot(g, x, y);
        }
    }

    private void drawSlot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF111111);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF696969);
        g.fill(x + 2, y + 2, x + 16, y + 16, 0xFF303030);
    }

    @Override public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractContents(g, mouseX, mouseY, delta);
        Entity entity = Minecraft.getInstance().level == null ? null
                : Minecraft.getInstance().level.getEntity(menu.entityId());
        if (entity instanceof LivingEntity living) {
            int scale = Math.max(18, Math.min(38, (int) (32.0F / Math.max(0.55F, living.getBbHeight()))));
            // Vanilla takes absolute corners, not width/height. Reversed corners
            // become negative GPU texture dimensions and crash on the first frame.
            // The float arguments are vertical offset, mouse X, then mouse Y.
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, leftPos + 28, topPos + 18,
                    leftPos + 96, topPos + 78, scale, 0.0F, mouseX, mouseY, living);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // AbstractContainerScreen has already translated the pose to the panel.
        g.text(font, font.plainSubstrByWidth(title.getString(), 120), titleLabelX, titleLabelY,
                CompanionUiStyle.TEXT, false);
        g.text(font, Component.literal("Companion cargo"), 8, 80, CompanionUiStyle.MUTED_TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                CompanionUiStyle.TEXT, false);
        g.text(font, Component.literal("Hand"), 108, 50, CompanionUiStyle.MUTED_TEXT, false);
        g.text(font, Component.literal("Off"), 148, 50, CompanionUiStyle.MUTED_TEXT, false);
    }
}
