package dev.riqvip.tameall.companion;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;

/** Server-side durability policy used by the small vanilla method hooks. */
public final class CompanionDurability {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private CompanionDurability() {}

    public static boolean isCompanion(LivingEntity entity) {
        return CompanionAttachments.get(entity).map(state -> !state.dead()).orElse(false);
    }

    public static boolean useDurability(LivingEntity entity) {
        return CompanionAttachments.get(entity)
                .map(state -> !state.dead() && state.settings().useDurability())
                .orElse(false);
    }

    public static boolean suppressDurability(LivingEntity entity) {
        return isCompanion(entity) && !useDurability(entity);
    }

    /** True when the companion should skip vanilla sunlight ignition and helmet wear. */
    public static boolean protectsFromSun(LivingEntity entity) {
        return CompanionAttachments.get(entity)
                .map(state -> !state.dead() && state.settings().magic().sunlightProtection())
                .orElse(false);
    }

    /** Applies the same per-hit amount vanilla uses for equipment damage. */
    public static void hurtArmor(LivingEntity entity, DamageSource source, float amount) {
        if (!useDurability(entity) || amount <= 0.0F) return;
        int damage = Math.max(1, (int) (amount / 4.0F));
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem() || !stack.canBeHurtBy(source)) continue;
            stack.hurtAndBreak(damage, entity, slot);
        }
    }

    /** Completes the weapon component's post-hit durability operation omitted by Mob in 26.2. */
    public static void postWeaponHit(Mob attacker, LivingEntity target) {
        if (!useDurability(attacker)) return;
        ItemStack weapon = attacker.getWeaponItem();
        if (!weapon.isEmpty()) weapon.postHurtEnemy(target, attacker);
    }

    /** Implements BlocksAttacks wear for bonded non-player mobs. */
    public static void hurtBlockingItem(BlocksAttacks attacks, Level level, ItemStack stack,
                                        LivingEntity wielder, InteractionHand hand, float blockedDamage) {
        if (!useDurability(wielder)) return;
        int damage = attacks.itemDamage().apply(blockedDamage);
        if (damage > 0) stack.hurtAndBreak(damage, wielder, hand.asEquipmentSlot());
    }
}
