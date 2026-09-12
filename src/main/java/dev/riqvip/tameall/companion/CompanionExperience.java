package dev.riqvip.tameall.companion;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Optional XP attraction used only to repair damaged Mending equipment. */
public final class CompanionExperience {
    public static final double DEFAULT_RADIUS = CompanionSettings.DEFAULT_XP_RADIUS;
    public static final double MAX_RADIUS = CompanionSettings.MAX_XP_RADIUS;
    private static final double PLAYER_PRIORITY_RADIUS = 8.0D;
    private static final double CONSUME_DISTANCE = 1.15D;
    private static final EquipmentSlot[] EQUIPMENT = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    private CompanionExperience() {}

    public static void tick(ServerLevel level) {
        List<ExperienceOrb> orbs = new ArrayList<>();
        List<LivingEntity> companions = new ArrayList<>();
        for (var raw : level.getAllEntities()) {
            if (raw instanceof ExperienceOrb orb && orb.isAlive()) orbs.add(orb);
            if (raw instanceof LivingEntity living && living.isAlive()
                    && hasMendingDamage(living)
                    && CompanionAttachments.get(living).map(state -> !state.dead()
                            && state.settings().collectXpForMending()).orElse(false)) companions.add(living);
        }
        // Only already loaded entities are considered. No chunks are forced in
        // for an optional convenience feature.
        for (ExperienceOrb orb : orbs) attract(level, orb, companions);
    }

    private static void attract(ServerLevel level, ExperienceOrb orb, List<LivingEntity> companions) {
        if (!orb.isAlive() || orb.getValue() <= 0) return;
        // Vanilla's player attraction takes priority. This also avoids making
        // an orb visibly choose a pet when the owner is already collecting it.
        for (Player player : level.players()) {
            if (player.distanceToSqr(orb) <= PLAYER_PRIORITY_RADIUS * PLAYER_PRIORITY_RADIUS) return;
        }
        LivingEntity companion = companions.stream()
                .filter(value -> value.distanceToSqr(orb) <= CompanionAttachments.get(value)
                        .map(state -> state.settings().xpRadius() * state.settings().xpRadius()).orElse(0.0D))
                .min(Comparator.comparingDouble(value -> value.distanceToSqr(orb)))
                .orElse(null);
        if (companion == null) return;
        double distance = companion.distanceToSqr(orb);
        double radius = CompanionAttachments.get(companion)
                .map(state -> state.settings().xpRadius()).orElse(DEFAULT_RADIUS);
        if (distance > CONSUME_DISTANCE * CONSUME_DISTANCE) {
            // A short, damped pull gives the same visual behavior as vanilla's
            // player attraction without teleporting the orb.
            Vec3 delta = companion.position().add(0.0D, companion.getBbHeight() * 0.5D, 0.0D)
                    .subtract(orb.position());
            if (delta.lengthSqr() > 0.0001D) {
                Vec3 velocity = delta.normalize().scale(Math.min(0.35D, 0.06D + 0.02D * Math.max(0.0D, radius - Math.sqrt(distance))));
                orb.setDeltaMovement(velocity);
            }
            return;
        }
        int value = orb.getValue();
        int consumed = repairOne(level, companion, value);
        if (consumed <= 0) return;
        orb.discard();
        if (consumed < value) ExperienceOrb.award(level, orb.position(), value - consumed);
    }

    private static boolean hasMendingDamage(LivingEntity entity) {
        Holder<Enchantment> mending = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.MENDING).orElse(null);
        if (mending == null) return false;
        for (EquipmentSlot slot : EQUIPMENT) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem() && stack.isDamaged()
                    && EnchantmentHelper.getItemEnchantmentLevel(mending, stack) > 0) return true;
        }
        return false;
    }

    private static int repairOne(ServerLevel level, LivingEntity entity, int availableXp) {
        Holder<Enchantment> mending = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.MENDING).orElse(null);
        if (mending == null) return 0;
        for (EquipmentSlot slot : EQUIPMENT) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()
                    || EnchantmentHelper.getItemEnchantmentLevel(mending, stack) <= 0) continue;
            int repair = Math.min(stack.getDamageValue(), Math.max(1, availableXp * 2));
            stack.setDamageValue(stack.getDamageValue() - repair);
            CompanionEquipment.captureNative(entity, true);
            return Math.max(1, (repair + 1) / 2);
        }
        return 0;
    }
}
