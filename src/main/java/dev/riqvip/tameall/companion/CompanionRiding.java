package dev.riqvip.tameall.companion;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/** Server and client riding policy shared by interaction and movement hooks. */
public final class CompanionRiding {
    private static final Map<EntityType<?>, RidingProfile> PROFILES = new java.util.HashMap<>();

    public enum RidingProfile { NATIVE, GROUND, FLYING, AQUATIC, AMPHIBIOUS, HOPPING, STATIONARY }

    static {
        registerNative(EntityTypes.HORSE, EntityTypes.DONKEY, EntityTypes.MULE,
                EntityTypes.ZOMBIE_HORSE, EntityTypes.SKELETON_HORSE, EntityTypes.CAMEL,
                EntityTypes.CAMEL_HUSK, EntityTypes.PIG, EntityTypes.STRIDER,
                EntityTypes.HAPPY_GHAST, EntityTypes.NAUTILUS, EntityTypes.ZOMBIE_NAUTILUS);
        register(RidingProfile.AMPHIBIOUS, EntityTypes.DROWNED, EntityTypes.FROG);
        register(RidingProfile.AQUATIC, EntityTypes.AXOLOTL, EntityTypes.COD, EntityTypes.SALMON,
                EntityTypes.TROPICAL_FISH, EntityTypes.PUFFERFISH, EntityTypes.DOLPHIN,
                EntityTypes.SQUID, EntityTypes.GUARDIAN, EntityTypes.ELDER_GUARDIAN);
        register(RidingProfile.FLYING, EntityTypes.BAT, EntityTypes.BEE, EntityTypes.PARROT,
                EntityTypes.ALLAY, EntityTypes.VEX, EntityTypes.GHAST, EntityTypes.PHANTOM,
                EntityTypes.BLAZE);
        register(RidingProfile.HOPPING, EntityTypes.RABBIT, EntityTypes.SLIME, EntityTypes.MAGMA_CUBE);
        register(RidingProfile.STATIONARY, EntityTypes.SHULKER);
        // Llamas inherit the equine base classes but have no native steering.
        register(RidingProfile.GROUND, EntityTypes.LLAMA);
    }

    private CompanionRiding() {}

    /** Extension point for modded entities; registration is server-owned. */
    public static void register(RidingProfile profile, EntityType<?>... types) {
        if (profile == null || types == null) return;
        for (EntityType<?> type : types) if (type != null) PROFILES.put(type, profile);
    }

    private static void registerNative(EntityType<?>... types) { register(RidingProfile.NATIVE, types); }

    public static EquipmentSlot ridingEquipmentSlot(LivingEntity vehicle) {
        return vehicle != null && vehicle.getType() == EntityTypes.HAPPY_GHAST
                ? EquipmentSlot.BODY : EquipmentSlot.SADDLE;
    }

    public static ItemStack ridingEquipment(LivingEntity vehicle) {
        return vehicle == null ? ItemStack.EMPTY : vehicle.getItemBySlot(ridingEquipmentSlot(vehicle)).copy();
    }

    public static boolean isRidingEquipment(LivingEntity vehicle, ItemStack stack) {
        if (vehicle == null || stack == null || stack.isEmpty()) return false;
        EquipmentSlot slot = ridingEquipmentSlot(vehicle);
        return slot == EquipmentSlot.SADDLE ? stack.is(Items.SADDLE)
                : vehicle.isEquippableInSlot(stack, slot);
    }

    public static boolean canMount(Player player, LivingEntity vehicle, CompanionState state) {
        if (player == null || vehicle == null || state == null || state.dead()) return false;
        if (!player.getUUID().equals(state.ownerId()) || vehicle == player || vehicle.isPassenger()) return false;
        if (vehicle.getPassengers().size() >= 1) return false;
        return !state.settings().saddleRequired() || !ridingEquipment(vehicle).isEmpty();
    }

    /** The profile is deliberately conservative for unknown modded creatures. */
    public static RidingProfile profile(LivingEntity entity) {
        if (entity == null) return RidingProfile.STATIONARY;
        RidingProfile registered = PROFILES.get(entity.getType());
        if (registered != null) return registered;
        if (entity instanceof PlayerRideableJumping || entity instanceof ItemSteerable) return RidingProfile.NATIVE;
        if (entity instanceof WaterAnimal) return RidingProfile.AQUATIC;
        if (entity instanceof Mob mob
                && mob.getMoveControl().getClass().getSimpleName().contains("Flying")) return RidingProfile.FLYING;
        return entity instanceof Mob ? RidingProfile.GROUND : RidingProfile.STATIONARY;
    }

    public static boolean canJump(LivingEntity entity) {
        return switch (profile(entity)) {
            case NATIVE, GROUND, HOPPING, AMPHIBIOUS -> true;
            default -> false;
        };
    }

    public static boolean canFly(LivingEntity entity) {
        return profile(entity) == RidingProfile.FLYING;
    }

    public static boolean canSwim(LivingEntity entity) {
        RidingProfile profile = profile(entity);
        return profile == RidingProfile.AQUATIC || profile == RidingProfile.AMPHIBIOUS;
    }

    public static boolean isAuthorizedController(Entity vehicle, Player player) {
        if (!(vehicle instanceof LivingEntity living) || player == null) return false;
        if (living.level().isClientSide()) {
            return CompanionAttachments.getRidingView(living)
                    .map(view -> view.ownerId().equals(player.getUUID())).orElse(false);
        }
        return CompanionAttachments.get(living)
                .map(state -> !state.dead() && state.ownerId().equals(player.getUUID())).orElse(false);
    }

    public static boolean isControlled(Entity entity) {
        return entity instanceof LivingEntity living && living.getFirstPassenger() instanceof Player player
                && isAuthorizedController(living, player);
    }

    public static boolean isAddedController(LivingEntity vehicle, Player player) {
        return isAuthorizedController(vehicle, player) && profile(vehicle) != RidingProfile.NATIVE;
    }

    /** Whether the server should run the companion's normal attack AI while ridden. */
    public static boolean shouldRunMountedAi(LivingEntity vehicle) {
        if (!(vehicle instanceof Mob mob) || vehicle.level().isClientSide() || !isControlled(mob)) return false;
        return CompanionAttachments.get(mob)
                .map(state -> !state.dead() && state.settings().attackWhileMounted()
                        && state.settings().stance() != CombatStance.PASSIVE)
                .orElse(false);
    }

    public static boolean usesNativeSeat(LivingEntity vehicle) {
        return profile(vehicle) == RidingProfile.NATIVE;
    }

    /** Returns the legacy generic seat used by existing zombie and small-mob saves. */
    public static double seatHeight(LivingEntity vehicle) {
        double height = vehicle.getBbHeight();
        return vehicle.getBbHeight() * (vehicle.getBbWidth() > 1.25F ? 0.82D : 0.72D)
                + Math.max(0.05D, height * 0.05D);
    }

    public static Vec3 riddenInput(LivingEntity vehicle, Player rider, Vec3 vanillaInput) {
        RidingProfile profile = profile(vehicle);
        if (profile == RidingProfile.NATIVE) return vanillaInput;
        if (profile == RidingProfile.GROUND || profile == RidingProfile.HOPPING
                || profile == RidingProfile.AMPHIBIOUS) return localInput(rider);
        if (profile == RidingProfile.STATIONARY) return Vec3.ZERO;
        Vec3 local = localInput(rider);
        double vertical = -Math.sin(Math.toRadians(rider.getXRot())) * local.z;
        if (profile == RidingProfile.FLYING) {
            if (riderJumping(rider)) vertical += 1.0D;
        } else if (profile == RidingProfile.AQUATIC) {
            if (riderJumping(rider)) vertical += 1.0D;
        }
        return new Vec3(local.x, Math.clamp(vertical, -1.0D, 1.0D), local.z);
    }

    public static float riddenSpeed(LivingEntity vehicle, float vanillaSpeed) {
        if (profile(vehicle) == RidingProfile.NATIVE) return vanillaSpeed;
        double speed = attributeValue(vehicle, Attributes.MOVEMENT_SPEED, vanillaSpeed);
        if (profile(vehicle) == RidingProfile.FLYING
                && vehicle.getAttributes().hasAttribute(Attributes.FLYING_SPEED)) {
            speed = vehicle.getAttributeValue(Attributes.FLYING_SPEED);
        }
        if (!Double.isFinite(speed) || speed < 0.0D) return Float.isFinite(vanillaSpeed) ? vanillaSpeed : 0.0F;
        return (float) speed;
    }

    public static void tickRidden(LivingEntity vehicle, Player rider) {
        if (!isAddedController(vehicle, rider)) return;
        vehicle.setYRot(rider.getYRot());
        vehicle.setYHeadRot(rider.getYRot());
        vehicle.setJumping(riderJumping(rider));
        if (profile(vehicle) == RidingProfile.STATIONARY) vehicle.setDeltaMovement(Vec3.ZERO);
    }

    private static double attributeValue(LivingEntity vehicle,
                                         net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                         float fallback) {
        if (vehicle.getAttributes().hasAttribute(attribute)) return vehicle.getAttributeValue(attribute);
        return fallback;
    }

    private static Vec3 localInput(Player rider) {
        if (rider instanceof ServerPlayer serverPlayer) {
            Input input = serverPlayer.getLastClientInput();
            double x = input.left() == input.right() ? 0.0D : input.left() ? 1.0D : -1.0D;
            double z = input.forward() == input.backward() ? 0.0D : input.forward() ? 1.0D : -1.0D;
            return new Vec3(x, 0.0D, z);
        }
        return new Vec3(rider.xxa, 0.0D, rider.zza);
    }

    private static boolean riderJumping(Player rider) {
        return rider instanceof ServerPlayer serverPlayer
                ? serverPlayer.getLastClientInput().jump() : rider.isJumping();
    }

    public static void tickMountedAttack(ServerLevel level, Mob mob, CompanionState state, Player owner) {
        if (!isControlled(mob) || !state.settings().attackWhileMounted()
                || state.settings().stance() == CombatStance.PASSIVE) {
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            return;
        }
        double reach = maxMountedScanRange(mob);
        AABB area = mob.getBoundingBox().inflate(reach);
        TargetSelection selection = CompanionAttachments.getTargetSelection(mob)
                .orElseGet(() -> TargetSelection.fromLegacy(state.settings().targetFilter()));
        LivingEntity target = null;
        if (state.settings().stance() == CombatStance.ASSIST) {
            LivingEntity[] signals = { owner.getLastHurtMob(), owner.getLastHurtByMob(), mob.getLastHurtByMob() };
            for (LivingEntity candidate : signals) {
                if (candidate != null && candidate.isAlive() && mob.distanceToSqr(candidate) <= reach * reach
                        && validMountedTarget(level, mob, owner, candidate, selection, true)) {
                    target = candidate;
                    break;
                }
            }
        } else {
            target = level.getEntitiesOfClass(LivingEntity.class, area,
                    candidate -> candidate.isAlive() && candidate != owner
                            && validMountedTarget(level, mob, owner, candidate, selection, false))
                    .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        }
        if (target == null) {
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            return;
        }
        CompanionCombat.assignTarget(mob, target);
    }

    /** Called at the beginning of LivingEntity.aiStep so native goals see a fresh target. */
    public static void prepareMountedCombat(ServerLevel level, Mob mob) {
        CompanionState state = CompanionAttachments.get(mob).orElse(null);
        if (state == null || state.dead() || !isControlled(mob)) {
            return;
        }
        Player owner = level.getPlayerByUUID(state.ownerId());
        if (owner == null) {
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            return;
        }
        tickMountedAttack(level, mob, state, owner);
    }

    private static double maxMountedScanRange(Mob mob) {
        // Keep the scan bounded, then apply the native melee/ranged range in
        // validMountedTarget. Unknown ranged mobs use a conservative range;
        // species with custom goals are registered here as adapters.
        if (mob instanceof AbstractSkeleton) return 15.0D;
        if (mob instanceof Drowned || mob instanceof SnowGolem) return 10.0D;
        if (mob instanceof CrossbowAttackMob) return 8.0D;
        if (mob instanceof RangedAttackMob) return 10.0D;
        return 4.0D;
    }

    private static boolean nativeRangedAttack(Mob mob) {
        if (mob instanceof SnowGolem) return true;
        if (mob instanceof Drowned) return mob.getMainHandItem().is(Items.TRIDENT);
        if (mob instanceof AbstractSkeleton) return mob.getMainHandItem().is(Items.BOW);
        if (mob instanceof CrossbowAttackMob) return mob.getMainHandItem().is(Items.CROSSBOW);
        return mob instanceof RangedAttackMob && !mob.getMainHandItem().isEmpty();
    }

    private static boolean withinNativeAttackRange(Mob mob, LivingEntity candidate) {
        if (nativeRangedAttack(mob)) {
            double range = maxMountedScanRange(mob);
            return mob.distanceToSqr(candidate) <= range * range;
        }
        return mob.isWithinMeleeAttackRange(candidate);
    }

    private static boolean validMountedTarget(ServerLevel level, Mob mob, Player owner,
                                               LivingEntity candidate, TargetSelection selection,
                                               boolean signal) {
        return !CompanionCombat.rejectsAsTarget(mob, candidate)
                && !(candidate instanceof Player targetPlayer && !owner.canHarmPlayer(targetPlayer))
                && !owner.isAlliedTo(candidate) && !candidate.isAlliedTo(owner)
                && mob.hasLineOfSight(candidate)
                && withinNativeAttackRange(mob, candidate)
                && (signal || TargetMatcher.matches(level, candidate, selection));
    }

    public static void forget(Entity entity) {
        // Kept as a lifecycle hook for callers; no client movement state is retained.
    }
}
