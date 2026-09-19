package dev.riqvip.tameall.client;

import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.companion.CompanionState;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/** Regression coverage for the real client-controlled added riding path. */
public final class CompanionRidingClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            int zombieId = world.getServer().computeOnServer(server -> {
                var player = world.getConnection().getServerPlayer();
                var level = player.level();
                var zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
                if (zombie == null) throw new AssertionError("Zombie creation failed");
                zombie.setPos(player.getX() + 2.0D, player.getY(), player.getZ() + 2.0D);
                zombie.setNoAi(true);
                zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                zombie.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                zombie.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
                level.addFreshEntity(zombie);
                CompanionAttachments.set(zombie,
                        CompanionState.newlyBonded(player.getUUID(), "minecraft:zombie"));
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                return zombie.getId();
            });

            context.waitFor(client -> client.level.getEntity(zombieId) != null
                    && client.player.getMainHandItem().isEmpty());
            context.runOnClient(client -> {
                var entity = client.level.getEntity(zombieId);
                client.gameMode.interact(client.player, entity,
                        new EntityHitResult(entity), InteractionHand.MAIN_HAND);
            });
            world.getServer().waitFor(server -> {
                var player = world.getConnection().getServerPlayer();
                return player.getVehicle() != null && player.getVehicle().getId() == zombieId;
            });
            context.waitFor(client -> client.player.getVehicle() != null
                    && client.player.getVehicle().getId() == zombieId);

            double[] before = world.getServer().computeOnServer(server -> {
                var entity = world.getConnection().getServerLevel().getEntity(zombieId);
                return new double[]{entity.getX(), entity.getZ()};
            });
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(20);
            context.getInput().releaseKey(options -> options.keyUp);
            context.waitTicks(5);
            double[] after = world.getServer().computeOnServer(server -> {
                var entity = world.getConnection().getServerLevel().getEntity(zombieId);
                return new double[]{entity.getX(), entity.getZ()};
            });
            double displacement = Math.hypot(after[0] - before[0], after[1] - before[1]);
            if (displacement < 0.08D) throw new AssertionError("zombie did not move under rider input");

            context.runOnClient(client -> client.player.setYRot(90.0F));
            context.getInput().holdKey(options -> options.keyUp);
            context.waitTicks(8);
            context.getInput().releaseKey(options -> options.keyUp);
            context.getInput().holdKeyFor(options -> options.keyJump, 3);
            context.waitTicks(8);

            context.runOnClient(client -> {
                if (client.player.getVehicle() == null || client.player.getVehicle().getId() != zombieId) {
                    throw new AssertionError("client lost zombie controller state");
                }
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            });
            context.waitTicks(8);
            context.takeScreenshot("companion-riding-zombie");
            context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
            context.getInput().holdShift();
            context.waitTicks(3);
            context.getInput().releaseShift();
            world.getServer().waitFor(server -> world.getConnection().getServerPlayer().getVehicle() == null);
        }
    }
}
