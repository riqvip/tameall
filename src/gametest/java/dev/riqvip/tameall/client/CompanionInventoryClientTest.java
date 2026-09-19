package dev.riqvip.tameall.client;

import dev.riqvip.tameall.ModItems;
import dev.riqvip.tameall.TameAll;
import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.companion.CompanionEquipment;
import dev.riqvip.tameall.menu.CompanionMenu;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

/** Exercises real interaction packets, the Inventory button, and the rendered entity preview. */
public final class CompanionInventoryClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            exerciseInventory(context, world, EntityTypes.ZOMBIE, "zombie");
            exerciseInventory(context, world, EntityTypes.COW, "cow");
        }
    }

    private void exerciseInventory(ClientGameTestContext context, TestSingleplayerContext world,
                                   EntityType<? extends Mob> type, String name) {
        context.getInput().resizeWindow(854, 480);
        setGuiScale(context, 2);
        int entityId = world.getServer().computeOnServer(server -> {
            var player = world.getConnection().getServerPlayer();
            var level = player.level();
            var mob = type.create(level, EntitySpawnReason.COMMAND);
            if (mob == null) throw new AssertionError("Companion creation failed: " + name);
            mob.setPos(player.getX() + 2, player.getY(), player.getZ());
            mob.setNoAi(true);
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            level.addFreshEntity(mob);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GOLDEN_WHEAT));
            return mob.getId();
        });
        context.waitFor(client -> client.level.getEntity(entityId) != null
                && client.player.getMainHandItem().is(ModItems.GOLDEN_WHEAT));
        context.runOnClient(client -> client.gameMode.interact(client.player,
                client.level.getEntity(entityId), new EntityHitResult(client.level.getEntity(entityId)),
                InteractionHand.MAIN_HAND));
        world.getServer().waitFor(server -> CompanionAttachments.get(
                world.getConnection().getServerLevel().getEntity(entityId)).isPresent());
        world.getServer().runOnServer(server -> world.getConnection().getServerPlayer()
                .setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY));
        context.waitFor(client -> client.player.getMainHandItem().isEmpty());
        openControls(context, entityId);
        if ("cow".equals(name)) {
            context.getInput().resizeWindow(1280, 720);
            setGuiScale(context, 2);
            // Let the vanilla recipe and advancement toasts expire so the
            // documentation captures show the complete companion panel.
            context.waitTicks(140);
            captureControlScreens(context);
        }
        clickTab(context, "Inventory");
        clickLiteralButton(context, "Open Inventory");
        context.waitTicks(20);
        context.runOnClient(client -> {
            if (!(client.player.containerMenu instanceof CompanionMenu menu)
                    || menu.entityId() != entityId
                    || !menu.getSlot(0).getItem().is(Items.IRON_HELMET)
                    || !menu.getSlot(4).getItem().is(Items.IRON_SWORD)
                    || !menu.getSlot(5).getItem().is(Items.SHIELD)
                    || !"minecraft:container/slot/saddle".equals(
                    String.valueOf(menu.getSlot(CompanionMenu.RIDING_SLOT).getNoItemIcon()))
                    || !InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD.equals(menu.getSlot(CompanionEquipment.OFFHAND).getNoItemIcon())) {
                throw new AssertionError("Companion inventory/equipment did not synchronize");
            }
        });
        context.takeScreenshot("companion-inventory-" + name);
        context.getInput().resizeWindow(1280, 720);
        for (int scale : new int[]{1, 2, 3}) {
            setGuiScale(context, scale);
            context.getInput().setCursorPos(100, 100);
            context.waitTicks(5);
            context.takeScreenshot("companion-inventory-" + name + "-scale-" + scale + "-left");
            context.getInput().setCursorPos(1100, 600);
            context.waitTicks(5);
            context.takeScreenshot("companion-inventory-" + name + "-scale-" + scale + "-right");
        }
        context.runOnClient(client -> client.player.closeContainer());
        context.waitFor(client -> client.gui.screen() == null);
        context.waitTicks(5);
        if ("cow".equals(name)) captureRoster(context, world);
        TameAll.LOGGER.info("PASS: {} tamed, Inventory opened, equipment synchronized, preview rendered at GUI scales 1/2/3", name);
    }

    private void setGuiScale(ClientGameTestContext context, int scale) {
        context.runOnClient(client -> {
            client.options.guiScale().set(scale);
            client.resizeGui();
        });
    }

    private void openControls(ClientGameTestContext context, int entityId) {
        context.getInput().holdShift();
        context.waitTicks(2);
        context.runOnClient(client -> client.gameMode.interact(client.player,
                client.level.getEntity(entityId), new EntityHitResult(client.level.getEntity(entityId)),
                InteractionHand.MAIN_HAND));
        context.waitForScreen(CompanionScreen.class);
        context.getInput().releaseShift();
    }

    private void captureControlScreens(ClientGameTestContext context) {
        context.takeScreenshot("companion-overview-current");
        clickTab(context, "Movement");
        context.takeScreenshot("companion-movement-current");
        clickMovementButton(context, 130);
        context.waitForScreen(CompanionTargetScreen.class);
        context.waitTicks(3);
        context.takeScreenshot("companion-target-editor-current");
        context.runOnClient(client -> client.gui.screen().onClose());
        context.waitForScreen(CompanionScreen.class);
        clickTab(context, "Collection");
        context.takeScreenshot("companion-collection-current");
        clickTab(context, "Abilities");
        context.takeScreenshot("companion-abilities-current");
    }

    private void captureRoster(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runOnServer(server -> world.getConnection().getServerPlayer()
                .setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.COMPANION_WHISTLE)));
        context.waitTicks(5);
        context.runOnClient(client -> client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND));
        context.waitForScreen(CompanionRosterScreen.class);
        context.waitTicks(3);
        context.takeScreenshot("companion-roster-current");
        context.runOnClient(client -> client.gui.screen().onClose());
        context.waitFor(client -> client.gui.screen() == null);
    }

    private void clickTab(ClientGameTestContext context, String label) {
        int tab = switch (label) {
            case "Overview" -> 0;
            case "Movement" -> 1;
            case "Collection" -> 2;
            case "Abilities" -> 3;
            case "Inventory" -> 4;
            default -> throw new IllegalArgumentException("Unknown companion tab: " + label);
        };
        context.runOnClient(client -> {
            if (!(client.gui.screen() instanceof CompanionScreen screen)) {
                throw new AssertionError("Expected companion controls while clicking " + label);
            }
            int guiWidth = client.getWindow().getGuiScaledWidth();
            int guiHeight = client.getWindow().getGuiScaledHeight();
            int panelWidth = Math.clamp(guiWidth - 8, 360, 420);
            int panelHeight = Math.clamp(guiHeight - 8, 220, 286);
            int left = (guiWidth - panelWidth) / 2;
            int top = (guiHeight - panelHeight) / 2;
            screen.mouseClicked(new MouseButtonEvent(left + 20, top + 47 + tab * 23 + 10,
                    new MouseButtonInfo(0, 0)), false);
        });
        context.waitTicks(2);
    }

    private void clickMovementButton(ClientGameTestContext context, int offset) {
        context.runOnClient(client -> {
            if (!(client.gui.screen() instanceof CompanionScreen screen)) {
                throw new AssertionError("Expected companion controls while clicking Movement content");
            }
            int guiWidth = client.getWindow().getGuiScaledWidth();
            int guiHeight = client.getWindow().getGuiScaledHeight();
            int panelWidth = Math.clamp(guiWidth - 8, 360, 420);
            int panelHeight = Math.clamp(guiHeight - 8, 220, 286);
            int left = (guiWidth - panelWidth) / 2;
            int top = (guiHeight - panelHeight) / 2;
            screen.mouseClicked(new MouseButtonEvent(left + 112 + 10, top + 58 + offset + 10,
                    new MouseButtonInfo(0, 0)), false);
        });
        context.waitTicks(2);
    }

    private void clickLiteralButton(ClientGameTestContext context, String label) {
        context.runOnClient(client -> {
            if (!(client.gui.screen() instanceof CompanionScreen screen)) {
                throw new AssertionError("Expected companion controls while clicking " + label);
            }
            int guiWidth = client.getWindow().getGuiScaledWidth();
            int guiHeight = client.getWindow().getGuiScaledHeight();
            int panelWidth = Math.clamp(guiWidth - 8, 360, 420);
            int panelHeight = Math.clamp(guiHeight - 8, 220, 286);
            int left = (guiWidth - panelWidth) / 2;
            int top = (guiHeight - panelHeight) / 2;
            if ("Inventory".equals(label)) {
                screen.mouseClicked(new MouseButtonEvent(left + 20, top + 47 + 4 * 23 + 10,
                        new MouseButtonInfo(0, 0)), false);
                return;
            }
            if ("Open Inventory".equals(label)) {
                screen.mouseClicked(new MouseButtonEvent(left + 112 + 10, top + 58 + 10,
                        new MouseButtonInfo(0, 0)), false);
                return;
            }
            for (var child : screen.children()) {
                if (child instanceof AbstractButton button && label.equals(button.getMessage().getString())) {
                    screen.mouseClicked(new MouseButtonEvent(button.getX() + 2, button.getY() + 2,
                            new MouseButtonInfo(0, 0)), false);
                    return;
                }
            }
            throw new AssertionError("Could not find visible companion button " + label);
        });
    }
}
