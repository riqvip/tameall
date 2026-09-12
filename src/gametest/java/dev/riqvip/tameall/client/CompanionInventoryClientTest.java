package dev.riqvip.tameall.client;

import dev.riqvip.tameall.ModItems;
import dev.riqvip.tameall.TameAll;
import dev.riqvip.tameall.companion.CompanionAttachments;
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
import net.minecraft.world.phys.EntityHitResult;

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
        openInventory(context, entityId);
        context.waitTicks(20);
        context.runOnClient(client -> {
            if (!(client.player.containerMenu instanceof CompanionMenu menu)
                    || menu.entityId() != entityId
                    || !menu.getSlot(0).getItem().is(Items.IRON_HELMET)
                    || !menu.getSlot(4).getItem().is(Items.IRON_SWORD)
                    || !menu.getSlot(5).getItem().is(Items.SHIELD)) {
                throw new AssertionError("Companion inventory/equipment did not synchronize");
            }
        });
        context.takeScreenshot("companion-inventory-" + name);
        context.runOnClient(client -> client.player.closeContainer());
        context.waitTicks(5);
        openInventory(context, entityId);
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
        context.waitTicks(5);
        TameAll.LOGGER.info("PASS: {} tamed, Inventory clicked twice, equipment synchronized, preview rendered at GUI scales 1/2/3", name);
    }

    private void setGuiScale(ClientGameTestContext context, int scale) {
        context.runOnClient(client -> {
            client.options.guiScale().set(scale);
            client.resizeGui();
        });
    }

    private void openInventory(ClientGameTestContext context, int entityId) {
        context.getInput().holdShift();
        context.waitTicks(2);
        context.runOnClient(client -> client.gameMode.interact(client.player,
                client.level.getEntity(entityId), new EntityHitResult(client.level.getEntity(entityId)),
                InteractionHand.MAIN_HAND));
        context.waitForScreen(CompanionScreen.class);
        context.getInput().releaseShift();
        context.clickScreenButton("tameall.inventory");
        context.waitForScreen(CompanionInventoryScreen.class);
    }
}
