package dev.riqvip.tameall.companion;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/** Physical cargo-container binding for a companion. */
public final class CompanionContainer {
    public static final AttachmentType<ItemStack> CONTENTS = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "companion_container"), ItemStack.OPTIONAL_CODEC);

    private CompanionContainer() {}

    public static boolean isContainerItem(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(Items.CHEST) || stack.is(Items.TRAPPED_CHEST)
                || stack.is(Items.BARREL) || isShulker(stack));
    }

    public static boolean isShulker(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && stack.getItem() instanceof BlockItem block
                && block.getBlock() instanceof ShulkerBoxBlock;
    }

    public static ItemStack get(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return ItemStack.EMPTY;
        ItemStack stack = target.getAttached(CONTENTS);
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    public static boolean has(Entity entity) { return !get(entity).isEmpty(); }

    /** Cargo is visible for legacy saves so old items can be withdrawn safely. */
    public static boolean canUseCargo(LivingEntity entity) {
        if (entity == null) return false;
        if (CompanionAttachments.get(entity)
                .map(state -> !state.settings().cargoContainerRequired())
                .orElse(false)) return true;
        return has(entity) || CompanionInventory.hasAnyItems(entity);
    }

    /** New cargo may be inserted only with a container or the level-2 waiver. */
    public static boolean canStoreCargo(LivingEntity entity) {
        if (entity == null) return false;
        return CompanionAttachments.get(entity)
                .map(state -> !state.settings().cargoContainerRequired() || has(entity))
                .orElse(has(entity));
    }

    public static boolean canPlace(LivingEntity entity, ItemStack incoming) {
        if (!isContainerItem(incoming)) return false;
        ItemStack current = get(entity);
        if (!current.isEmpty() && CompanionInventory.hasAnyItems(entity)) return false;
        if (isShulker(incoming) && CompanionInventory.hasAnyItems(entity)) return false;
        return true;
    }

    /** Returns the slot item with current cargo packed into a shulker, if applicable. */
    public static ItemStack displayStack(Entity entity) {
        ItemStack current = get(entity);
        if (!isShulker(current)) return current;
        List<ItemStack> cargo = CompanionInventory.read(entity);
        ItemStack displayed = current.copy();
        if (hasAny(cargo)) displayed.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(cargo));
        else displayed.remove(DataComponents.CONTAINER);
        return displayed;
    }

    /** Binds a new physical container and imports any prefilled shulker contents. */
    public static void attach(Entity entity, ItemStack incoming) {
        if (!(entity instanceof AttachmentTarget target) || !isContainerItem(incoming)) return;
        ItemStack stored = incoming.copy();
        if (isShulker(stored)) {
            ItemContainerContents contents = stored.get(DataComponents.CONTAINER);
            if (contents != null) {
                NonNullList<ItemStack> imported = NonNullList.withSize(CompanionInventory.SIZE, ItemStack.EMPTY);
                contents.copyInto(imported);
                CompanionInventory.write(entity, imported);
            }
            stored.remove(DataComponents.CONTAINER);
        }
        target.setAttached(CONTENTS, stored);
    }

    /** Removes the physical container; ordinary containers eject cargo at the pet. */
    public static void remove(Entity entity, ServerLevel level) {
        ItemStack current = displayStack(entity);
        if (entity instanceof AttachmentTarget target) target.removeAttached(CONTENTS);
        if (isShulker(current)) {
            CompanionInventory.clear(entity);
        } else if (level != null && CompanionInventory.hasAnyItems(entity)) {
            CompanionInventory.dropAndClear(entity, level);
        }
    }

    /** Drops the container exactly once during companion death. */
    public static void dropAndClear(LivingEntity entity, ServerLevel level) {
        ItemStack current = displayStack(entity);
        if (entity instanceof AttachmentTarget target) target.removeAttached(CONTENTS);
        if (isShulker(current)) CompanionInventory.clear(entity);
        if (!current.isEmpty()) entity.spawnAtLocation(level, current);
    }

    /** Transfers the container and its cargo across a vanilla conversion. */
    public static void transfer(Entity oldEntity, Entity newEntity) {
        ItemStack current = displayStack(oldEntity);
        List<ItemStack> cargo = CompanionInventory.read(oldEntity);
        if (!current.isEmpty() && newEntity instanceof AttachmentTarget target) {
            ItemStack stored = current.copy();
            stored.remove(DataComponents.CONTAINER);
            target.setAttached(CONTENTS, stored);
        }
        if (hasAny(cargo)) CompanionInventory.write(newEntity, cargo);
        clear(oldEntity);
    }

    public static void clear(Entity entity) {
        if (entity instanceof AttachmentTarget target) target.removeAttached(CONTENTS);
        CompanionInventory.clear(entity);
    }

    private static boolean hasAny(List<ItemStack> items) {
        for (ItemStack stack : items) if (stack != null && !stack.isEmpty()) return true;
        return false;
    }
}
