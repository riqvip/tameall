package dev.riqvip.tameall.companion;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Persistent 27-slot cargo store used by the companion menu. */
public final class CompanionInventory {
    public static final int SIZE = 27;
    public static final AttachmentType<List<ItemStack>> CARGO = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "companion_cargo"), ItemStack.OPTIONAL_CODEC.listOf());

    private CompanionInventory() {}

    public static List<ItemStack> read(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return List.of();
        List<ItemStack> stored = target.getAttached(CARGO);
        if (stored == null) stored = List.of();
        ArrayList<ItemStack> copy = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) copy.add(i < stored.size() ? stored.get(i).copy() : ItemStack.EMPTY);
        return copy;
    }

    public static void write(Entity entity, List<ItemStack> items) {
        if (!(entity instanceof AttachmentTarget target)) return;
        ArrayList<ItemStack> copy = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) copy.add(i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
        target.setAttached(CARGO, List.copyOf(copy));
    }

    /** Inserts as much as possible into cargo and returns an independent remainder. */
    public static ItemStack insert(Entity entity, ItemStack incoming) {
        if (incoming == null || incoming.isEmpty()) return ItemStack.EMPTY;
        List<ItemStack> cargo = read(entity);
        ItemStack remainder = incoming.copy();
        boolean changed = false;
        for (int i = 0; i < SIZE && !remainder.isEmpty(); i++) {
            ItemStack slot = cargo.get(i);
            if (slot.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(slot, remainder)) continue;
            int space = Math.min(remainder.getMaxStackSize(), slot.getMaxStackSize()) - slot.getCount();
            if (space <= 0) continue;
            int moved = Math.min(space, remainder.getCount());
            slot.grow(moved); remainder.shrink(moved); changed = true;
        }
        for (int i = 0; i < SIZE && !remainder.isEmpty(); i++) {
            if (!cargo.get(i).isEmpty()) continue;
            int moved = Math.min(remainder.getMaxStackSize(), remainder.getCount());
            cargo.set(i, remainder.copyWithCount(moved));
            remainder.shrink(moved); changed = true;
        }
        if (changed) write(entity, cargo);
        return remainder;
    }

    /** Drops and clears cargo before spawning stacks, making the operation one-shot. */
    public static void dropAndClear(Entity entity, ServerLevel level) {
        List<ItemStack> items = read(entity);
        if (entity instanceof AttachmentTarget target) target.setAttached(CARGO, List.of());
        for (ItemStack stack : items) if (!stack.isEmpty()) entity.spawnAtLocation(level, stack.copy());
    }

    public static Container container(Entity entity) {
        return new SyncedContainer(entity);
    }

    private static final class SyncedContainer implements Container {
        private final Entity entity;

        private SyncedContainer(Entity entity) {
            this.entity = entity;
        }

        @Override public int getContainerSize() { return SIZE; }

        @Override public boolean isEmpty() {
            for (int index = 0; index < SIZE; index++) if (!getItem(index).isEmpty()) return false;
            return true;
        }

        @Override public ItemStack getItem(int index) {
            return index >= 0 && index < SIZE ? read(entity).get(index).copy() : ItemStack.EMPTY;
        }

        @Override public ItemStack removeItem(int index, int count) {
            ItemStack current = getItem(index);
            if (current.isEmpty() || count <= 0) return ItemStack.EMPTY;
            int removed = Math.min(count, current.getCount());
            ItemStack result = current.copyWithCount(removed);
            current.shrink(removed);
            setItem(index, current);
            return result;
        }

        @Override public ItemStack removeItemNoUpdate(int index) {
            ItemStack current = getItem(index);
            if (!current.isEmpty()) setItem(index, ItemStack.EMPTY);
            return current;
        }

        @Override public void setItem(int index, ItemStack stack) {
            if (index < 0 || index >= SIZE || stack == null) return;
            List<ItemStack> items = read(entity);
            items.set(index, stack.copy());
            write(entity, items);
        }

        @Override public void setChanged() {}

        @Override public void clearContent() {
            write(entity, java.util.Collections.nCopies(SIZE, ItemStack.EMPTY));
        }

        @Override public java.util.Iterator<ItemStack> iterator() {
            return java.util.stream.IntStream.range(0, SIZE).mapToObj(this::getItem).iterator();
        }

        @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return entity.isAlive() && entity.level() == player.level()
                    && entity.distanceToSqr(player) <= 8.0D * 8.0D
                    && CompanionAttachments.get(entity)
                    .map(state -> state.ownerId().equals(player.getUUID()) && !state.dead())
                    .orElse(false);
        }
    }
}
