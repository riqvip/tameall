package dev.riqvip.tameall;

import dev.riqvip.tameall.companion.CompanionBondingService;
import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.companion.CompanionCombat;
import dev.riqvip.tameall.companion.CombatStance;
import dev.riqvip.tameall.companion.CompanionState;
import dev.riqvip.tameall.companion.CompanionRuntime;
import dev.riqvip.tameall.companion.CompanionEquipment;
import dev.riqvip.tameall.companion.CompanionJournal;
import dev.riqvip.tameall.companion.TargetFilter;
import dev.riqvip.tameall.companion.TargetPolicy;
import dev.riqvip.tameall.companion.TargetMatcher;
import dev.riqvip.tameall.companion.TargetSelection;
import dev.riqvip.tameall.companion.TargetCatalog;
import dev.riqvip.tameall.companion.TamingPolicy;
import dev.riqvip.tameall.companion.CompanionRiding;
import dev.riqvip.tameall.companion.CompanionContainer;
import dev.riqvip.tameall.companion.CompanionInventory;
import net.minecraft.core.component.DataComponents;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.ExperienceOrb;
import java.util.UUID;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import java.util.List;

/** Small server GameTests for universal bonding and explicit combat policy. */
public final class TameAllGameTests {
    @GameTest(maxTicks = 1)
    public void newBondsUseOptInAdaptations(GameTestHelper helper) {
        CompanionState state = CompanionState.newlyBonded(UUID.randomUUID(), "minecraft:zombie");
        helper.assertFalse(state.settings().magic().sunlightProtection(), "sun protection defaulted on");
        helper.assertFalse(state.settings().magic().drowningProtection(), "drowning protection defaulted on");
        helper.assertFalse(state.settings().magic().reusableExplosions(), "reusable explosion defaulted on");
        helper.assertFalse(state.settings().magic().preventVexExpiry(), "vex expiry protection defaulted on");
        helper.assertTrue(state.settings().magic().allowEndermanBlockPickup(), "Enderman pickup defaulted off");
        helper.assertTrue(state.settings().areaRadius() == 16, "area default changed");
        helper.assertTrue(state.settings().pickupRadius() <= 16.0D, "pickup radius exceeds cap");
        helper.assertFalse(state.settings().collectXpForMending(), "Mending XP defaulted on");
        helper.assertTrue(state.settings().saddleRequired(), "mount saddle requirement defaulted off");
        helper.assertFalse(state.settings().attackWhileMounted(), "mounted attack defaulted on");
        helper.assertTrue(state.settings().cargoContainerRequired(), "cargo container requirement defaulted off");
        helper.succeed();
    }

    @GameTest(maxTicks = 1)
    public void bondedMountRequiresSaddleByDefault(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        Mob zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie != null, "mount entity could not be created");
        helper.getLevel().addFreshEntity(zombie);
        CompanionState state = CompanionState.newlyBonded(owner.getUUID(), "minecraft:zombie");
        CompanionAttachments.set(zombie, state);
        helper.assertFalse(CompanionRiding.canMount(owner, zombie, state), "unsaddled pet accepted a mount");
        zombie.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        helper.assertTrue(CompanionRiding.canMount(owner, zombie, state), "saddled pet rejected its owner");
        helper.assertTrue(owner.startRiding(zombie, true, false), "owner could not mount bonded pet");
        helper.assertTrue(zombie.getFirstPassenger() == owner, "mount passenger was not attached");
        helper.succeed();
    }

    @GameTest(maxTicks = 1)
    public void cargoContainerPolicyAndShulkerPacking(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        Mob zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie != null, "cargo test entity could not be created");
        helper.getLevel().addFreshEntity(zombie);
        CompanionState state = CompanionState.newlyBonded(owner.getUUID(), "minecraft:zombie");
        CompanionAttachments.set(zombie, state);
        helper.assertFalse(CompanionContainer.canStoreCargo(zombie), "cargo was available without a container");
        CompanionContainer.attach(zombie, new ItemStack(Items.CHEST));
        helper.assertTrue(CompanionContainer.canStoreCargo(zombie), "chest did not unlock cargo");
        CompanionInventory.write(zombie, List.of(new ItemStack(Items.DIAMOND)));
        CompanionContainer.remove(zombie, helper.getLevel());
        helper.assertFalse(CompanionInventory.hasAnyItems(zombie), "removing a chest did not eject cargo");

        state = state.withSettings(state.settings().withCargoContainerRequired(false));
        CompanionAttachments.set(zombie, state);
        helper.assertTrue(CompanionContainer.canStoreCargo(zombie), "permission waiver did not unlock cargo");
        CompanionContainer.attach(zombie, new ItemStack(Items.SHULKER_BOX));
        CompanionInventory.write(zombie, List.of(new ItemStack(Items.DIAMOND)));
        helper.assertTrue(CompanionContainer.displayStack(zombie).get(DataComponents.CONTAINER) != null,
                "shulker container did not expose packed cargo");
        CompanionContainer.remove(zombie, helper.getLevel());
        helper.assertFalse(CompanionInventory.hasAnyItems(zombie), "shulker removal left duplicate cargo outside the box");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void addedGroundMountAcceptsOwnerInput(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(0.5D, 2.0D, 0.5D);
        Mob zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie != null, "movement mount could not be created");
        zombie.setPos(0.5D, 2.0D, 2.5D);
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        zombie.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        helper.getLevel().addFreshEntity(zombie);
        CompanionState state = CompanionState.newlyBonded(owner.getUUID(), "minecraft:zombie");
        CompanionAttachments.set(zombie, state);
        helper.assertTrue(owner.startRiding(zombie, true, false), "owner did not mount movement pet");
        helper.assertTrue(CompanionRiding.isControlled(zombie), "server did not recognize owner controller");
        helper.assertTrue(zombie.getControllingPassenger() == owner, "vanilla controlling passenger hook did not resolve owner");
        owner.setYRot(0.0F);
        owner.setLastClientInput(new Input(true, false, false, false, false, false, false));
        helper.assertTrue(CompanionRiding.riddenInput(zombie, owner, net.minecraft.world.phys.Vec3.ZERO).z > 0.5D,
                "riding profile did not decode forward input");
        helper.assertTrue(CompanionRiding.riddenSpeed(zombie, 0.0F) > 0.0F,
                "riding profile did not resolve movement speed");
        helper.runAtTickTime(10, () -> {
            helper.assertTrue(CompanionRiding.riddenInput(zombie, owner, net.minecraft.world.phys.Vec3.ZERO).z > 0.5D,
                    "added ground mount stopped consuming forward input");
            helper.assertTrue(zombie.getFirstPassenger() == owner, "mount lost its owner passenger");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 1)
    public void ridingProfilesUseSpeciesAdapters(GameTestHelper helper) {
        Mob drowned = EntityTypes.DROWNED.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob llama = EntityTypes.LLAMA.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(drowned != null && llama != null, "profile fixtures could not be created");
        helper.assertTrue(CompanionRiding.profile(drowned) == CompanionRiding.RidingProfile.AMPHIBIOUS,
                "drowned did not resolve to amphibious riding");
        helper.assertTrue(CompanionRiding.profile(llama) == CompanionRiding.RidingProfile.GROUND,
                "llama incorrectly used a native profile");
        helper.assertTrue(CompanionRiding.riddenSpeed(drowned, 0.0F) > 0.0F,
                "amphibious profile did not resolve movement speed");
        helper.assertTrue(CompanionRiding.ridingEquipmentSlot(
                EntityTypes.HAPPY_GHAST.create(helper.getLevel(), EntitySpawnReason.COMMAND))
                == EquipmentSlot.BODY, "Happy Ghast did not resolve a body harness slot");
        helper.succeed();
    }

    @GameTest(maxTicks = 1)
    public void targetSelectionExclusionsWin(GameTestHelper helper) {
        Mob zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob cow = EntityTypes.COW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie != null && cow != null, "target entities could not be created");
        TargetSelection selection = new TargetSelection(
                java.util.Set.of(TargetSelection.GROUP_ALL_LIVING),
                java.util.Set.of("type:minecraft:zombie"));
        helper.assertFalse(TargetMatcher.matches(helper.getLevel(), zombie, selection), "excluded zombie matched");
        helper.assertTrue(TargetMatcher.matches(helper.getLevel(), cow, selection), "included cow did not match");
        helper.succeed();
    }

    @GameTest(maxTicks = 1)
    public void targetCatalogContainsGroupsAndTypes(GameTestHelper helper) {
        var entries = TargetCatalog.entries(helper.getLevel());
        helper.assertTrue(entries.size() <= 512, "target catalog exceeded packet bound");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.id().equals(TargetSelection.GROUP_RAIDERS)),
                "raider group missing");
        helper.assertTrue(entries.stream().anyMatch(entry -> entry.id().equals("type:minecraft:zombie")
                        && entry.categories().contains(TargetSelection.GROUP_HOSTILE)
                        && entry.categories().contains(TargetSelection.GROUP_ALL_LIVING)),
                "zombie type/group classification missing");
        helper.assertTrue(entries.stream().anyMatch(entry -> "tag".equals(entry.kind())
                        && entry.categories().contains("advanced")), "advanced entity tags missing");
        helper.succeed();
    }

    @GameTest(maxTicks = 30)
    public void mendingXpOnlyRepairsOptedInCompanion(GameTestHelper helper) {
        Mob zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie != null, "mending companion could not be created");
        // GameTest mock players share the test level origin; keep this isolated
        // so the player's vanilla XP pickup priority does not short-circuit it.
        zombie.setPos(20.5D, 2.0D, 20.5D);
        zombie.setNoAi(true);
        zombie.setNoGravity(true);
        helper.getLevel().addFreshEntity(zombie);
        CompanionState state = CompanionState.newlyBonded(UUID.randomUUID(), "minecraft:zombie");
        state = state.withSettings(state.settings().withCollectXpForMending(true));
        CompanionAttachments.set(zombie, state);
        var mending = helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.MENDING);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(mending, 1);
        sword.setDamageValue(10);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, sword);
        CompanionEquipment.ensure(zombie);
        ExperienceOrb orb = new ExperienceOrb(helper.getLevel(), 21.0D, 2.0D, 20.5D, 5);
        orb.setNoGravity(true);
        helper.getLevel().addFreshEntity(orb);
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.MAINHAND).getDamageValue() < 10,
                    "opted-in companion did not repair Mending gear");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 1)
    public void registryEligibility(GameTestHelper helper) {
        BuiltInRegistries.ENTITY_TYPE.entrySet().forEach(entry -> {
            Identifier id = entry.getKey().identifier();
            String value = id.toString();
            if (value.equals(TamingPolicy.WITHER) || value.equals(TamingPolicy.ENDER_DRAGON)
                    || value.equals(TamingPolicy.PLAYER)) {
                helper.assertFalse(TamingPolicy.isEligible(value, true), "excluded entity was eligible: " + value);
            } else {
                var created = entry.getValue().create(helper.getLevel(), EntitySpawnReason.COMMAND);
                if (created instanceof LivingEntity) helper.assertTrue(TamingPolicy.isEligible(value, true), "living entity was not eligible: " + value);
            }
        });
        helper.succeed();
    }

    @GameTest(maxTicks = 1)
    public void bondingConsumesOnlySuccessfulPolicy(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        var bonded = CompanionBondingService.bond(owner, "minecraft:zombie", true, null, "minecraft:overworld", null);
        helper.assertTrue(bonded.succeeded(), "eligible mob did not bond");
        helper.assertFalse(CompanionBondingService.bond(owner, TamingPolicy.WITHER, true, null, null, null).succeeded(), "wither bonded");
        helper.assertFalse(CompanionBondingService.bond(owner, TamingPolicy.ENDER_DRAGON, true, null, null, null).succeeded(), "ender dragon bonded");
        helper.succeed();
    }

    @GameTest(maxTicks = 1)
    public void passiveNeverTargets(GameTestHelper helper) {
        UUID owner = UUID.randomUUID(), companion = UUID.randomUUID(), target = UUID.randomUUID();
        helper.assertFalse(TargetPolicy.mayAssist(CombatStance.PASSIVE, owner, companion, target, null, true), "passive assisted");
        helper.assertFalse(TargetPolicy.mayDefend(CombatStance.PASSIVE, owner, companion, target, null, true, TargetFilter.ALL_LIVING), "passive defended");
        helper.succeed();
    }

    @GameTest(maxTicks = 1)
    public void assistOnlyUsesSignals(GameTestHelper helper) {
        UUID owner = UUID.randomUUID(), companion = UUID.randomUUID(), target = UUID.randomUUID();
        helper.assertTrue(TargetPolicy.mayAssist(CombatStance.ASSIST, owner, companion, target, null, true), "assist ignored signal");
        helper.assertFalse(TargetPolicy.mayDefend(CombatStance.ASSIST, owner, companion, target, null, true, TargetFilter.HOSTILE_ONLY), "assist proactively scanned");
        helper.succeed();
    }

    /** Native target goals must not reintroduce species-specific aggression after taming. */
    @GameTest(maxTicks = 40)
    public void nativeAggressionIsDisabledForCompanions(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(30.5D, 2.0D, 30.5D);
        Mob fox = EntityTypes.FOX.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob chicken = EntityTypes.CHICKEN.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob skeleton = EntityTypes.SKELETON.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob golem = EntityTypes.IRON_GOLEM.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob pillager = EntityTypes.PILLAGER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob villager = EntityTypes.VILLAGER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(fox != null && chicken != null && skeleton != null && golem != null
                && pillager != null && villager != null, "native aggression entities could not be created");
        fox.setPos(1.5D, 2.0D, 1.5D); chicken.setPos(3.0D, 2.0D, 1.5D);
        skeleton.setPos(6.5D, 2.0D, 1.5D); golem.setPos(8.0D, 2.0D, 1.5D);
        pillager.setPos(11.5D, 2.0D, 1.5D); villager.setPos(13.0D, 2.0D, 1.5D);
        fox.setNoGravity(true); chicken.setNoGravity(true); skeleton.setNoGravity(true);
        golem.setNoGravity(true); pillager.setNoGravity(true); villager.setNoGravity(true);
        helper.getLevel().addFreshEntity(fox); helper.getLevel().addFreshEntity(chicken);
        helper.getLevel().addFreshEntity(skeleton); helper.getLevel().addFreshEntity(golem);
        helper.getLevel().addFreshEntity(pillager); helper.getLevel().addFreshEntity(villager);
        CompanionState passiveFox = CompanionState.newlyBonded(owner.getUUID(), "minecraft:fox");
        passiveFox = passiveFox.withSettings(passiveFox.settings().withStance(CombatStance.PASSIVE));
        CompanionState passiveSkeleton = CompanionState.newlyBonded(owner.getUUID(), "minecraft:skeleton");
        passiveSkeleton = passiveSkeleton.withSettings(passiveSkeleton.settings().withStance(CombatStance.PASSIVE));
        CompanionState passivePillager = CompanionState.newlyBonded(owner.getUUID(), "minecraft:pillager");
        passivePillager = passivePillager.withSettings(passivePillager.settings().withStance(CombatStance.PASSIVE));
        CompanionAttachments.set(fox, passiveFox);
        CompanionAttachments.set(skeleton, passiveSkeleton);
        CompanionAttachments.set(pillager, passivePillager);
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(fox.getTarget() == null, "tamed fox retained natural prey target");
            helper.assertTrue(skeleton.getTarget() == null, "tamed skeleton retained natural golem target");
            helper.assertTrue(pillager.getTarget() == null, "tamed pillager retained natural villager target");
            helper.assertTrue(chicken.isAlive() && golem.isAlive() && villager.isAlive(),
                    "native aggression damaged a protected test target");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 1)
    public void defendFiltersAndProtectsPets(GameTestHelper helper) {
        UUID owner = UUID.randomUUID(), companion = UUID.randomUUID(), target = UUID.randomUUID();
        UUID petOwner = UUID.randomUUID();
        helper.assertTrue(TargetPolicy.mayDefend(CombatStance.DEFEND_AREA, owner, companion, target, null, true, TargetFilter.HOSTILE_ONLY), "hostile was filtered");
        helper.assertTrue(TargetPolicy.mayDefend(CombatStance.DEFEND_AREA, owner, companion, target, null, false, TargetFilter.ALL_LIVING), "all-living target was filtered");
        helper.assertFalse(TargetPolicy.mayDefend(CombatStance.DEFEND_AREA, owner, companion, target, petOwner, true, TargetFilter.ALL_LIVING), "bonded pet was targetable");
        helper.assertFalse(TargetPolicy.mayDefend(CombatStance.DEFEND_AREA, owner, companion, owner, null, true, TargetFilter.ALL_LIVING), "owner was targetable");
        helper.succeed();
    }

    /** The live Mob target boundary must reject one bonded creature targeting another. */
    @GameTest(maxTicks = 1)
    public void bondedMobsNeverBecomeTargets(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        EntityType<?> zombieType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "zombie"));
        Mob first = (Mob) zombieType.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob second = (Mob) zombieType.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(first != null && second != null, "test zombies could not be created");
        first.setPos(1.5D, 2.0D, 1.5D);
        second.setPos(3.5D, 2.0D, 1.5D);
        helper.getLevel().addFreshEntity(first);
        helper.getLevel().addFreshEntity(second);
        CompanionAttachments.set(first, CompanionState.newlyBonded(owner, "minecraft:zombie"));
        CompanionAttachments.set(second, CompanionState.newlyBonded(UUID.randomUUID(), "minecraft:zombie"));
        // Zombies do not register the Brain slots used by other species.
        // This is the cleanup invoked on the first world tick after taming.
        first.setLastHurtByMob(second);
        CompanionCombat.sanitize(first);
        helper.assertTrue(first.getLastHurtByMob() == null, "protected retaliation survived cleanup");
        first.setTarget(second);
        helper.assertTrue(CompanionCombat.rejects(first, second), "bonded target was not protected");
        helper.assertFalse(first.canAttack(second), "canAttack accepted another companion");
        helper.assertTrue(first.getTarget() == null, "vanilla target field retained another companion");
        helper.assertFalse(first.doHurtTarget(helper.getLevel(), second),
                "zombie override attack was not blocked");
        helper.succeed();
    }

    /** Sun protection must intercept Mob#burnUndead before ignition or helmet wear. */
    @GameTest(maxTicks = 120)
    public void sunlightProtectionPreventsBurn(GameTestHelper helper) {
        EntityType<?> zombieType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "zombie"));
        Mob zombie = (Mob) zombieType.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie != null, "test zombie could not be created");
        zombie.setPos(1.5D, 2.0D, 1.5D);
        helper.getLevel().addFreshEntity(zombie);
        CompanionState state = CompanionState.newlyBonded(UUID.randomUUID(), "minecraft:zombie");
        // Sunlight protection is opt-in for new bonds.
        state = state.withSettings(state.settings().withMagic(state.settings().magic().withSunlightProtection(true)));
        CompanionAttachments.set(zombie, state);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        helper.runAtTickTime(100, () -> {
            helper.assertFalse(zombie.isOnFire(), "sun-protected zombie ignited");
            helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.HEAD).getDamageValue() == 0,
                    "sun-protected helmet took damage");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 1)
    public void golemsIgnorePetsUntilProvoked(GameTestHelper helper) {
        var ownerPlayer = helper.makeMockServerPlayerInLevel();
        UUID owner = ownerPlayer.getUUID();
        Mob pet = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob iron = EntityTypes.IRON_GOLEM.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob snow = EntityTypes.SNOW_GOLEM.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(pet != null && iron != null && snow != null, "golem test entities could not be created");
        pet.setPos(1.5D, 2.0D, 1.5D); iron.setPos(3.5D, 2.0D, 1.5D); snow.setPos(5.5D, 2.0D, 1.5D);
        helper.getLevel().addFreshEntity(pet); helper.getLevel().addFreshEntity(iron); helper.getLevel().addFreshEntity(snow);
        CompanionAttachments.set(pet, CompanionState.newlyBonded(owner, "minecraft:zombie"));
        Mob[] golems = {iron, snow};
        for (int index = 0; index < golems.length; index++) {
            Mob golem = golems[index];
            golem.setTarget(pet);
            CompanionCombat.sanitize(golem);
            helper.assertTrue(golem.getTarget() == null, "unprovoked golem targeted a pet");
            CompanionCombat.recordGolemProvocation(golem, index == 0 ? ownerPlayer : pet, 1.0F);
            golem.setTarget(pet);
            CompanionCombat.sanitize(golem);
            helper.assertTrue(golem.getTarget() == pet, "provoked golem did not retain the pet target");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 160)
    public void bondedIronGolemExecutesManagedAssistAttack(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(0.5D, 2.0D, 0.5D);
        Mob golem = EntityTypes.IRON_GOLEM.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob target = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(golem != null && target != null, "bonded golem attack entities could not be created");
        golem.setPos(1.5D, 2.0D, 1.5D);
        target.setPos(3.0D, 2.0D, 1.5D);
        golem.setNoGravity(true);
        target.setNoAi(true);
        target.setNoGravity(true);
        helper.getLevel().addFreshEntity(golem);
        helper.getLevel().addFreshEntity(target);
        CompanionState state = CompanionState.newlyBonded(owner.getUUID(), "minecraft:iron_golem");
        state = state.withSettings(state.settings().withStance(CombatStance.ASSIST));
        CompanionAttachments.set(golem, state);
        owner.setLastHurtMob(target);
        CompanionCombat.assignTarget(golem, target);
        float before = target.getHealth();
        helper.runAtTickTime(10, () -> helper.assertTrue(golem.getTarget() == target,
                "bonded iron golem did not acquire the managed Assist target"));
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < before,
                    "bonded iron golem did not execute its managed Assist attack");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 1)
    public void neutralCompanionSanitizePreservesFollowNavigation(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        Mob golem = EntityTypes.IRON_GOLEM.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(golem != null, "follow navigation golem could not be created");
        golem.setPos(1.5D, 2.0D, 1.5D);
        golem.setNoGravity(true);
        helper.getLevel().addFreshEntity(golem);
        CompanionAttachments.set(golem, CompanionState.newlyBonded(owner.getUUID(), "minecraft:iron_golem"));
        Path followPath = new Path(List.of(new Node(8, 2, 1)), new BlockPos(8, 2, 1), false);
        boolean started = golem.getNavigation().moveTo(followPath, 1.0D);
        helper.assertTrue(started, "iron golem follow path could not be started");
        helper.assertFalse(golem.getNavigation().isDone(), "iron golem follow path was already complete");
        CompanionCombat.sanitize(golem);
        helper.assertFalse(golem.getNavigation().isDone(),
                "neutral companion sanitization stopped follow navigation");
        helper.succeed();
    }

    @GameTest(maxTicks = 45)
    public void mountedZombieExecutesNativeMeleeAttack(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(0.5D, 2.0D, 0.5D);
        Mob zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob target = EntityTypes.COW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie != null && target != null, "mounted melee entities could not be created");
        zombie.setPos(1.5D, 2.0D, 0.5D);
        target.setPos(2.3D, 2.0D, 0.5D);
        zombie.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        target.setNoAi(true);
        target.setNoGravity(true);
        helper.getLevel().addFreshEntity(zombie);
        helper.getLevel().addFreshEntity(target);
        CompanionState state = CompanionState.newlyBonded(owner.getUUID(), "minecraft:zombie");
        state = state.withSettings(state.settings().withStance(CombatStance.ASSIST)
                .withAttackWhileMounted(true));
        CompanionAttachments.set(zombie, state);
        owner.setLastHurtMob(target);
        helper.assertTrue(owner.startRiding(zombie, true, false), "owner did not mount attack test zombie");
        float before = target.getHealth();
        helper.runAtTickTime(35, () -> {
            helper.assertTrue(target.getHealth() < before,
                    "mounted zombie did not execute its native melee attack");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 180)
    public void mountedDrownedExecutesNativeTridentAttack(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(0.5D, 2.0D, 0.5D);
        Mob drowned = EntityTypes.DROWNED.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob target = EntityTypes.SKELETON.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(drowned != null && target != null, "mounted ranged entities could not be created");
        drowned.setPos(1.5D, 2.0D, 0.5D);
        target.setPos(6.0D, 2.0D, 0.5D);
        drowned.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        drowned.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        target.setNoAi(true);
        target.setNoGravity(true);
        helper.getLevel().addFreshEntity(drowned);
        helper.getLevel().addFreshEntity(target);
        CompanionState state = CompanionState.newlyBonded(owner.getUUID(), "minecraft:drowned");
        state = state.withSettings(state.settings().withStance(CombatStance.DEFEND_AREA)
                .withAttackWhileMounted(true));
        CompanionAttachments.set(drowned, state);
        helper.assertTrue(owner.startRiding(drowned, true, false), "owner did not mount ranged test drowned");
        float before = target.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < before,
                    "mounted drowned did not execute its native trident attack");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 45)
    public void rangedGoalStopsAfterCachedTargetClears(GameTestHelper helper) {
        Mob snow = EntityTypes.SNOW_GOLEM.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob target = EntityTypes.COW.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(snow != null && target != null, "ranged crash fixtures could not be created");
        snow.setPos(1.5D, 2.0D, 1.5D);
        target.setPos(5.0D, 2.0D, 1.5D);
        target.setNoAi(true);
        target.setNoGravity(true);
        helper.getLevel().addFreshEntity(snow);
        helper.getLevel().addFreshEntity(target);
        snow.setTarget(target);
        helper.runAtTickTime(8, () -> {
            snow.setTarget(null);
            helper.assertTrue(snow.getTarget() == null, "ranged target was not cleared for regression");
        });
        helper.runAtTickTime(35, helper::succeed);
    }

    @GameTest(maxTicks = 1)
    public void bondedIllagerCanFightWildIllager(GameTestHelper helper) {
        Mob pet = EntityTypes.PILLAGER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Mob wild = EntityTypes.PILLAGER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(pet != null && wild != null, "illager test entities could not be created");
        CompanionAttachments.set(pet, CompanionState.newlyBonded(UUID.randomUUID(), "minecraft:pillager"));
        helper.assertFalse(CompanionCombat.rejects(pet, wild), "illager family friendship blocked pet combat");
        helper.succeed();
    }

    @GameTest(maxTicks = 1)
    public void lethalCreeperDeathUpdatesRoster(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        Mob creeper = EntityTypes.CREEPER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(creeper != null, "creeper test entity could not be created");
        creeper.setPos(1.5D, 2.0D, 1.5D);
        helper.getLevel().addFreshEntity(creeper);
        CompanionState state = CompanionState.newlyBonded(owner, "minecraft:creeper");
        CompanionAttachments.set(creeper, state);
        CompanionRuntime.handleLethalCreeperExplosion((net.minecraft.world.entity.monster.Creeper) creeper);
        helper.assertTrue(CompanionAttachments.get(creeper).map(CompanionState::dead).orElse(false),
                "lethal creeper was not marked dead");
        helper.assertTrue(CompanionJournal.get(helper.getLevel()).get(state.bondId()).dead(),
                "lethal creeper roster entry remained live");
        helper.succeed();
    }

    /** Permanent removal of any bonded mob is reconciled through the shared lifecycle path. */
    @GameTest(maxTicks = 8)
    public void discardedCompanionUpdatesRoster(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        Mob zombie = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(zombie != null, "discarded companion could not be created");
        zombie.setPos(1.5D, 2.0D, 1.5D);
        zombie.setNoGravity(true);
        helper.getLevel().addFreshEntity(zombie);
        CompanionState state = CompanionState.newlyBonded(owner, "minecraft:zombie");
        CompanionAttachments.set(zombie, state);
        CompanionJournal.get(helper.getLevel()).replaceEntity(state, zombie.getUUID(), "minecraft:overworld",
                new dev.riqvip.tameall.companion.BlockPoint(1, 2, 1), helper.getLevel().getGameTime());
        zombie.discard();
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(CompanionAttachments.get(zombie).map(CompanionState::dead).orElse(false),
                    "discarded companion attachment remained live");
            helper.assertTrue(CompanionJournal.get(helper.getLevel()).get(state.bondId()).dead(),
                    "discarded companion roster entry remained live");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 1)
    public void safeCreeperBlastIsReusable(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        owner.setPos(0.5D, 2.0D, 0.5D);
        var creeper = EntityTypes.CREEPER.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        var target = EntityTypes.ZOMBIE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        helper.assertTrue(creeper != null && target != null, "safe creeper entities could not be created");
        creeper.setPos(1.5D, 2.0D, 1.5D); target.setPos(3.0D, 2.0D, 1.5D);
        helper.getLevel().addFreshEntity(creeper); helper.getLevel().addFreshEntity(target);
        CompanionState state = CompanionState.newlyBonded(owner.getUUID(), "minecraft:creeper");
        state = state.withSettings(state.settings().withMagic(
                new dev.riqvip.tameall.companion.MagicToggles(false, false, true, false, true)));
        CompanionAttachments.set(creeper, state);
        // A target must be installed through the same explicit signal path the
        // runtime uses; a native creeper target is intentionally rejected.
        CompanionCombat.assignTarget(creeper, target);
        owner.setLastHurtMob(target);
        helper.assertTrue(creeper.getTarget() == target, "creeper target was not set");
        helper.assertFalse(CompanionCombat.rejects(creeper, target), "combat policy rejected the wild target");
        helper.assertFalse(owner.isAlliedTo(target), "mock owner unexpectedly allied with the target");
        float before = target.getHealth();
        helper.assertTrue(CompanionRuntime.handleSafeCreeperExplosion(
                (net.minecraft.world.entity.monster.Creeper) creeper), "safe blast was not handled");
        helper.assertTrue(creeper.isAlive(), "safe blast killed the companion");
        helper.assertTrue(target.getHealth() < before, "safe blast did not damage its permitted target");
        helper.assertTrue(CompanionAttachments.getCreeperCooldown(creeper).isPresent(),
                "safe blast did not start its cooldown");
        helper.succeed();
    }
}
