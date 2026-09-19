package dev.riqvip.tameall.companion;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single safety boundary for companion targeting and attacks.
 *
 * Vanilla target goals normally call {@link Mob#setTarget(LivingEntity)}, but
 * Brain-powered creatures and a few special goals keep their own target or
 * anger state. Keeping the policy here lets the runtime, mixins, and damage
 * guard agree that an owner or any bonded creature is never an enemy and that
 * a tamed mob's native species target is never an attack authorization.
 */
public final class CompanionCombat {
    private CompanionCombat() {}
    private static final Map<UUID, UUID> MANAGED_TARGETS = new ConcurrentHashMap<>();
    /** Synchronous multi-target authorization used by a reusable Creeper blast. */
    private static final Map<UUID, Set<UUID>> EXPLICIT_DAMAGE_TARGETS = new ConcurrentHashMap<>();

    /** Returns true when the mob is an active TameAll companion. */
    public static boolean isCompanion(Mob mob) {
        return CompanionAttachments.get(mob)
                .map(state -> !state.dead())
                .orElse(false);
    }

    /** A bonded mob or a vex affiliated with a bonded evoker. */
    public static boolean isProtectedMob(Mob mob) {
        return isCompanion(mob) || isFriendlySummon(mob);
    }

    public static boolean isFriendlySummon(Entity entity) {
        if (!(entity instanceof Vex vex) || CompanionAttachments.get(entity).isPresent()) return false;
        LivingEntity owner = vex.getOwner();
        if (owner != null && CompanionAttachments.get(owner)
                .map(state -> !state.dead()).orElse(false)) return true;
        return CompanionAttachments.getSummonAffiliation(entity).isPresent();
    }

    /** Returns the player UUID whose bond controls this entity, if any. */
    public static UUID ownerIdOf(Entity entity) {
        if (entity == null) return null;
        CompanionState state = CompanionAttachments.get(entity).orElse(null);
        if (state != null && !state.dead()) return state.ownerId();
        if (entity instanceof Vex vex) {
            LivingEntity owner = vex.getOwner();
            if (owner != null) {
                state = CompanionAttachments.get(owner).orElse(null);
                if (state != null && !state.dead()) return state.ownerId();
            }
            return CompanionAttachments.getSummonAffiliation(entity)
                    .map(SummonAffiliation::ownerPlayerId).orElse(null);
        }
        return null;
    }

    /**
     * Returns true when a companion must not target or attack the candidate.
     * This intentionally uses the owner's UUID, so the protection still works
     * while the owner is in another dimension or is not currently loaded.
     */
    public static boolean rejects(Mob attacker, LivingEntity candidate) {
        if (candidate == null || !isProtectedMob(attacker)) return false;
        return rejectsFriendly(attacker, candidate);
    }

    /** Attack-time guard for cached/custom goals on companions. */
    public static boolean rejectsAttack(Mob attacker, LivingEntity candidate) {
        if (candidate == null || !isProtectedMob(attacker)) return false;
        if (isCompanion(attacker) && !isManagedTarget(attacker, candidate)
                && !isExplicitDamageTarget(attacker, candidate)) return true;
        return rejectsFriendly(attacker, candidate);
    }

    public static boolean rejectsAttack(Mob attacker, Entity candidate) {
        return candidate instanceof LivingEntity living && rejectsAttack(attacker, living);
    }

    /**
     * Shared attack-time validity check for native goals.  Unlike target
     * acquisition this method does not assign or clear a target; it only says
     * whether a goal may continue using the target it already cached.
     */
    public static boolean isAttackTargetValid(Mob attacker, LivingEntity candidate) {
        return candidate != null && candidate.isAlive() && !rejectsAttack(attacker, candidate);
    }

    /** Friendship and golem rules used while the runtime is selecting a target. */
    public static boolean rejectsAsTarget(Mob attacker, LivingEntity candidate) {
        return rejectsFriendly(attacker, candidate) || rejectsUnprovokedGolem(attacker, candidate);
    }

    /** Native AI may never install a target on a companion unless the runtime installed it. */
    public static boolean rejectsNativeTarget(Mob attacker, LivingEntity candidate) {
        if (candidate == null) return false;
        return rejectsAsTarget(attacker, candidate)
                || (isCompanion(attacker) && !isManagedTarget(attacker, candidate));
    }

    public static void assignTarget(Mob attacker, LivingEntity target) {
        if (target == null) {
            clearManagedTarget(attacker);
            attacker.setTarget(null);
        } else {
            MANAGED_TARGETS.put(attacker.getUUID(), target.getUUID());
            attacker.setTarget(target);
        }
    }

    public static void clearManagedTarget(Mob attacker) {
        MANAGED_TARGETS.remove(attacker.getUUID());
    }

    public static boolean isManagedTarget(Mob attacker, LivingEntity candidate) {
        UUID target = MANAGED_TARGETS.get(attacker.getUUID());
        return target != null && candidate != null && target.equals(candidate.getUUID());
    }

    /** Authorizes a bounded synchronous set of targets for one custom attack. */
    public static void beginExplicitDamage(Mob attacker, Set<UUID> targets) {
        if (attacker == null || targets == null || targets.isEmpty()) return;
        EXPLICIT_DAMAGE_TARGETS.put(attacker.getUUID(), Set.copyOf(targets));
    }

    public static void endExplicitDamage(Mob attacker) {
        if (attacker != null) EXPLICIT_DAMAGE_TARGETS.remove(attacker.getUUID());
    }

    private static boolean isExplicitDamageTarget(Mob attacker, LivingEntity candidate) {
        Set<UUID> targets = EXPLICIT_DAMAGE_TARGETS.get(attacker.getUUID());
        return targets != null && candidate != null && targets.contains(candidate.getUUID());
    }

    private static boolean rejectsFriendly(Mob attacker, LivingEntity candidate) {
        if (candidate == null || !isProtectedMob(attacker)) return false;
        UUID ownerId = ownerIdOf(attacker);
        if (ownerId == null) return false;

        if (candidate == attacker || ownerId.equals(candidate.getUUID())) return true;
        // Every bonded entity is friendly to every other bonded entity.  Do
        // not require the target to be alive here: stale native target fields
        // must be cleared even if the target died between AI phases.
        if (CompanionAttachments.get(candidate).isPresent() || isFriendlySummon(candidate)) return true;
        if (attacker.isAlliedTo(candidate) || candidate.isAlliedTo(attacker)) {
            // Vanilla illagers consider their whole family allied. A bonded
            // illager must still be able to attack a wild family member.
            if (!(isIllagerFamily(attacker) && isIllagerFamily(candidate)
                    && (isProtectedMob(attacker) || isProtectedMob((Mob) candidate)))) return true;
            if (attacker.getTeam() != null && candidate.getTeam() != null
                    && attacker.getTeam().isAlliedTo(candidate.getTeam())) return true;
        }

        // Preserve vanilla's owner/team PvP rule when the owner is loaded.
        // The UUID check above is the authoritative owner protection when it
        // is not loaded.
        if (candidate instanceof Player targetPlayer
                && attacker.level().getEntity(ownerId) instanceof Player owner
                && !owner.canHarmPlayer(targetPlayer)) {
            return true;
        }
        return false;
    }

    /** Convenience overload for attack APIs that accept a non-living Entity. */
    public static boolean rejects(Mob attacker, Entity candidate) {
        return candidate instanceof LivingEntity living && rejects(attacker, living);
    }

    private static boolean rejectsUnprovokedGolem(Mob attacker, LivingEntity candidate) {
        if (!(attacker instanceof IronGolem || attacker instanceof SnowGolem)) return false;
        UUID ownerId = ownerIdOf(candidate);
        if (ownerId == null) return false;
        GolemProvocation provocation = CompanionAttachments.getGolemProvocation(attacker).orElse(null);
        return provocation == null || !provocation.activeAt(attacker.level().getGameTime())
                || !provocation.ownerId().equals(ownerId);
    }

    private static boolean isIllagerFamily(Entity entity) {
        return entity instanceof Raider;
    }

    /** Records a real damaging hit on a golem by a player or that player's pet. */
    public static void recordGolemProvocation(LivingEntity victim, Entity source, float damageTaken) {
        if (damageTaken <= 0.0F || !(victim instanceof IronGolem || victim instanceof SnowGolem)) return;
        Entity attacker = source;
        // DamageSource#getEntity is the projectile for arrows, tridents, and
        // similar attacks. Resolve the shooter before looking up the bond so a
        // ranged companion provokes the golem exactly like a melee hit.
        if (attacker instanceof Projectile projectile && projectile.getOwner() != null) {
            attacker = projectile.getOwner();
        }
        if (attacker instanceof OwnableEntity ownable && ownable.getOwner() != null) attacker = ownable.getOwner();
        UUID ownerId = ownerIdOf(attacker);
        if (attacker instanceof Player player) ownerId = player.getUUID();
        if (ownerId != null) {
            CompanionAttachments.setGolemProvocation(victim,
                    new GolemProvocation(ownerId, victim.level().getGameTime() + 200L));
        }
    }

    /** Captures the native vex owner while the evoker is loaded. */
    public static void refreshSummonAffiliation(LivingEntity entity) {
        if (!(entity instanceof Vex vex) || CompanionAttachments.get(entity).isPresent()) return;
        LivingEntity owner = vex.getOwner();
        CompanionState state = owner == null ? null : CompanionAttachments.get(owner).orElse(null);
        if (state != null && !state.dead()) {
            CompanionAttachments.setSummonAffiliation(entity,
                    new SummonAffiliation(owner.getUUID(), state.ownerId()));
        }
    }

    /**
     * Removes forbidden ordinary target, retaliation, neutral anger, and Brain
     * target state.  It is safe to call once before and once after a vanilla AI
     * phase; legal targets are left untouched for the companion runtime.
     */
    public static void sanitize(Mob mob) {
        if (!isProtectedMob(mob) && !(mob instanceof IronGolem) && !(mob instanceof SnowGolem)) return;

        LivingEntity rawTarget = mob.getTargetUnchecked();
        boolean clearedTarget = rejectsNativeTarget(mob, rawTarget);
        if (clearedTarget) {
            clearManagedTarget(mob);
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setAggressive(false);
        }

        LivingEntity lastHurtBy = mob.getLastHurtByMob();
        if (rejectsAsTarget(mob, lastHurtBy)) mob.setLastHurtByMob(null);
        if (rejectsAsTarget(mob, mob.getLastHurtMob())) mob.setLastHurtMob(null);

        Brain<?> brain = mob.getBrain();
        boolean clearedMemory = false;
        clearedMemory |= clearEntityMemory(mob, brain, MemoryModuleType.ATTACK_TARGET);
        clearedMemory |= clearEntityMemory(mob, brain, MemoryModuleType.HURT_BY_ENTITY);
        clearedMemory |= clearEntityMemory(mob, brain, MemoryModuleType.INTERACTION_TARGET);
        clearedMemory |= clearEntityMemory(mob, brain, MemoryModuleType.AVOID_TARGET);
        clearedMemory |= clearEntityMemory(mob, brain, MemoryModuleType.NEAREST_HOSTILE);
        clearedMemory |= clearEntityMemory(mob, brain, MemoryModuleType.NEAREST_ATTACKABLE);
        clearedMemory |= clearEntityMemory(mob, brain, MemoryModuleType.ROAR_TARGET);
        clearedMemory |= clearAngryAt(mob, brain);

        if (mob instanceof NeutralMob neutral) {
            var angerReference = neutral.getPersistentAngerTarget();
            LivingEntity angerTarget = angerReference == null
                    ? null : angerReference.getEntity(mob.level(), LivingEntity.class);
            if (isCompanion(mob)) {
                // stopBeingAngry() also calls setTarget(null), which destroys
                // a legal target selected by the companion runtime. Clear
                // only native anger metadata and leave the managed target in
                // place for the normal attack goal.
                neutral.setPersistentAngerTarget(null);
                neutral.setPersistentAngerEndTime(NeutralMob.NO_ANGER_END_TIME);
            } else if (rejectsAsTarget(mob, angerTarget)) {
                neutral.stopBeingAngry();
                clearedMemory = true;
            }
        }

        // Warden anger is kept outside the regular NeutralMob interface.
        if (mob instanceof Warden warden) {
            warden.getAngerManagement().getActiveEntity().filter(target -> isCompanion(mob)
                    || rejectsAsTarget(mob, target))
                    .ifPresent(target -> {
                        warden.clearAnger(target);
                    });
        }
        if (clearedMemory && mob.getTargetUnchecked() == null) {
            clearManagedTarget(mob);
            mob.getNavigation().stop();
            mob.setAggressive(false);
        }
    }

    private static <T> boolean clearEntityMemory(Mob mob, Brain<?> brain, MemoryModuleType<T> type) {
        // Not every mob registers every memory type (notably ROAR_TARGET), so
        // getMemoryInternal returns null for an unregistered slot, and an
        // empty Optional for a registered slot without a value.
        var memory = brain.getMemoryInternal(type);
        Object value = memory == null ? null : memory.orElse(null);
        if (value instanceof LivingEntity living && rejectsNativeTarget(mob, living)) {
            brain.eraseMemory(type);
            return true;
        }
        return false;
    }

    private static boolean clearAngryAt(Mob mob, Brain<?> brain) {
        var memory = brain.getMemoryInternal(MemoryModuleType.ANGRY_AT);
        UUID angryAt = memory == null ? null : memory.orElse(null);
        if (angryAt == null) return false;
        if (isCompanion(mob)) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            return true;
        }
        if (angryAt.equals(ownerIdOf(mob))) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            return true;
        }
        Entity entity = mob.level().getEntity(angryAt);
        if (entity instanceof LivingEntity living && rejectsNativeTarget(mob, living)) {
            brain.eraseMemory(MemoryModuleType.ANGRY_AT);
            return true;
        }
        return false;
    }
}
