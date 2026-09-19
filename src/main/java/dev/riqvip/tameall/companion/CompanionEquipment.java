package dev.riqvip.tameall.companion;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent companion equipment independent of a particular mob class. The six
 * exposed slots map to the native armor, main-hand, and off-hand slots. On a
 * first bond the entity's existing equipment is captured, so binding never
 * silently deletes a saddle, armor piece, or held item.
 */
public final class CompanionEquipment {
    public static final int HEAD = 0;
    public static final int CHEST = 1;
    public static final int LEGS = 2;
    public static final int FEET = 3;
    public static final int MAINHAND = 4;
    public static final int OFFHAND = 5;
    public static final int SIZE = 6;
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    public static final AttachmentType<List<ItemStack>> ITEMS = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "companion_equipment"), ItemStack.OPTIONAL_CODEC.listOf());

    private CompanionEquipment() {}

    /** Captures native equipment exactly once and applies it to the persistent attachment. */
    public static List<ItemStack> ensure(LivingEntity entity) {
        if (!(entity instanceof AttachmentTarget target)) return emptyItems();
        List<ItemStack> stored = target.getAttached(ITEMS);
        if (stored == null || stored.size() < SIZE) {
            ArrayList<ItemStack> captured = new ArrayList<>(SIZE);
            for (EquipmentSlot slot : SLOTS) captured.add(entity.getItemBySlot(slot).copy());
            target.setAttached(ITEMS, List.copyOf(captured));
            disableNativeEquipmentDrops(entity);
            return captured;
        }
        disableNativeEquipmentDrops(entity);
        return copySlots(stored);
    }

    public static List<ItemStack> read(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return emptyItems();
        List<ItemStack> stored = target.getAttached(ITEMS);
        if (stored == null) return emptyItems();
        return copySlots(stored);
    }

    public static void write(Entity entity, List<ItemStack> items) {
        if (!(entity instanceof AttachmentTarget target)) return;
        ArrayList<ItemStack> copy = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) copy.add(i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
        target.setAttached(ITEMS, List.copyOf(copy));
        if (entity instanceof LivingEntity living) apply(living, copy);
    }

    public static ItemStack get(Entity entity, int slot) {
        if (slot < 0 || slot >= SIZE) throw new IndexOutOfBoundsException("equipment slot");
        List<ItemStack> items = read(entity);
        return items.get(slot).copy();
    }

    public static void set(Entity entity, int slot, ItemStack stack) {
        if (slot < 0 || slot >= SIZE) throw new IndexOutOfBoundsException("equipment slot");
        List<ItemStack> items = read(entity);
        items.set(slot, stack == null ? ItemStack.EMPTY : stack.copy());
        write(entity, items);
    }

    /** Applies the attachment through native LivingEntity equipment APIs. */
    public static void apply(LivingEntity entity, List<ItemStack> items) {
        for (int i = 0; i < SIZE; i++) {
            entity.setItemSlot(SLOTS[i], i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
        }
        disableNativeEquipmentDrops(entity);
    }

    /** Keeps native equipment changes in the attachment, optionally restoring durability. */
    public static void captureNative(LivingEntity entity) {
        captureNative(entity, true);
    }

    public static void captureNative(LivingEntity entity, boolean useDurability) {
        if (!(entity instanceof AttachmentTarget target) || target.getAttached(ITEMS) == null) return;
        ArrayList<ItemStack> nativeItems = new ArrayList<>(SIZE);
        for (EquipmentSlot slot : SLOTS) nativeItems.add(entity.getItemBySlot(slot).copy());
        List<ItemStack> stored = copySlots(target.getAttached(ITEMS));
        if (!useDurability) {
            for (int i = 0; i < SIZE; i++) {
                ItemStack previous = stored.get(i);
                ItemStack current = nativeItems.get(i);
                if (!previous.isEmpty() && previous.isDamageableItem()
                        && !current.isEmpty() && ItemStack.isSameItem(previous, current)) {
                    ItemStack restored = current.copy();
                    restored.setDamageValue(previous.getDamageValue());
                    nativeItems.set(i, restored);
                    if (!ItemStack.matches(current, restored)) entity.setItemSlot(SLOTS[i], restored.copy());
                }
            }
        }
        if (!ItemStack.listMatches(stored, nativeItems)) target.setAttached(ITEMS, List.copyOf(nativeItems));
        disableNativeEquipmentDrops(entity);
    }

    public static Container container(Entity entity) {
        return new EquipmentCargoContainer(entity);
    }

    /** Drops exactly one copy of the attached gear and clears it before spawning items. */
    public static void dropAndClear(LivingEntity entity, ServerLevel level) {
        List<ItemStack> items = read(entity);
        EquipmentSlot ridingSlot = CompanionRiding.ridingEquipmentSlot(entity);
        ItemStack ridingGear = entity.getItemBySlot(ridingSlot).copy();
        entity.setItemSlot(ridingSlot, ItemStack.EMPTY);
        if (entity instanceof AttachmentTarget target) target.setAttached(ITEMS, List.of());
        for (ItemStack stack : items) if (!stack.isEmpty()) entity.spawnAtLocation(level, stack.copy());
        if (!ridingGear.isEmpty()) entity.spawnAtLocation(level, ridingGear);
    }

    public static EquipmentSlot nativeSlot(int index) {
        if (index < 0 || index >= SIZE) throw new IndexOutOfBoundsException("equipment slot");
        return SLOTS[index];
    }

    private static List<ItemStack> copySlots(List<ItemStack> stored) {
        ArrayList<ItemStack> copy = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) copy.add(i < stored.size() ? stored.get(i).copy() : ItemStack.EMPTY);
        return copy;
    }

    private static List<ItemStack> emptyItems() {
        ArrayList<ItemStack> empty = new ArrayList<>(SIZE);
        for (int i = 0; i < SIZE; i++) empty.add(ItemStack.EMPTY);
        return empty;
    }

    private static void disableNativeEquipmentDrops(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.Mob mob)) return;
        for (EquipmentSlot slot : SLOTS) mob.setDropChance(slot, 0.0F);
    }

    /** Unified companion view: six equipment slots, saddle, 27 cargo slots, and a container slot. */
    private static final class EquipmentCargoContainer implements Container {
        private static final int SADDLE_INDEX = SIZE;
        private static final int CONTAINER_INDEX = SIZE + 1 + CompanionInventory.SIZE;
        private static final int MENU_SIZE = CONTAINER_INDEX + 1;
        private final Entity entity;

        private EquipmentCargoContainer(Entity entity) {
            this.entity = entity;
            if (entity instanceof LivingEntity living) ensure(living);
        }

        @Override public int getContainerSize() { return MENU_SIZE; }

        @Override public boolean isEmpty() {
            for (int index = 0; index < MENU_SIZE; index++) if (!getItem(index).isEmpty()) return false;
            return true;
        }

        @Override public ItemStack getItem(int index) {
            if (index < 0 || index >= MENU_SIZE) return ItemStack.EMPTY;
            if (index < SIZE && entity instanceof LivingEntity living) {
                return living.getItemBySlot(nativeSlot(index)).copy();
            }
            if (index == SADDLE_INDEX && entity instanceof LivingEntity living) {
                return CompanionRiding.ridingEquipment(living);
            }
            if (index == CONTAINER_INDEX) return CompanionContainer.displayStack(entity);
            if (index > SADDLE_INDEX && index < MENU_SIZE) {
                return CompanionInventory.read(entity).get(index - SADDLE_INDEX - 1).copy();
            }
            return ItemStack.EMPTY;
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
            if (index < 0 || index >= MENU_SIZE || stack == null) return;
            if (index < SIZE) {
                if (entity instanceof LivingEntity living) {
                    boolean useDurability = CompanionAttachments.get(living)
                            .map(state -> state.settings().useDurability()).orElse(true);
                    captureNative(living, useDurability);
                }
                CompanionEquipment.set(entity, index, stack);
            } else if (index == SADDLE_INDEX && entity instanceof LivingEntity living) {
                living.setItemSlot(CompanionRiding.ridingEquipmentSlot(living), stack.copy());
            } else if (index == CONTAINER_INDEX && entity instanceof LivingEntity living) {
                if (stack.isEmpty()) {
                    CompanionContainer.remove(entity, living.level() instanceof ServerLevel level ? level : null);
                } else if (CompanionContainer.canPlace(living, stack)) {
                    CompanionContainer.attach(entity, stack);
                }
            } else if (index > SADDLE_INDEX && index < MENU_SIZE) {
                List<ItemStack> cargo = CompanionInventory.read(entity);
                cargo.set(index - SADDLE_INDEX - 1, stack.copy());
                CompanionInventory.write(entity, cargo);
            }
        }

        /** Values are written by each slot operation; no stale snapshot is ever flushed. */
        @Override public void setChanged() {}

        @Override public void clearContent() {
            for (int index = 0; index < MENU_SIZE; index++) setItem(index, ItemStack.EMPTY);
        }

        @Override public boolean canPlaceItem(int index, ItemStack stack) {
            if (index > SADDLE_INDEX && index < CONTAINER_INDEX) {
                return entity instanceof LivingEntity living && CompanionContainer.canStoreCargo(living);
            }
            if (index == CONTAINER_INDEX && entity instanceof LivingEntity living) {
                return CompanionContainer.canPlace(living, stack);
            }
            if (index == SADDLE_INDEX && entity instanceof LivingEntity living) {
                return CompanionRiding.isRidingEquipment(living, stack);
            }
            if (index == MAINHAND) return !stack.isEmpty();
            if (index == OFFHAND) return !stack.isEmpty();
            if (index >= HEAD && index <= FEET && entity instanceof LivingEntity living) {
                return !stack.isEmpty() && living.getEquipmentSlotForItem(stack) == nativeSlot(index);
            }
            return false;
        }

        @Override public java.util.Iterator<ItemStack> iterator() {
            return java.util.stream.IntStream.range(0, MENU_SIZE).mapToObj(this::getItem).iterator();
        }

        @Override public boolean stillValid(Player player) {
            return entity instanceof LivingEntity living && living.isAlive()
                    && living instanceof net.minecraft.world.entity.Entity e
                    && e.level() == player.level()
                    && CompanionAttachments.get(living).map(state -> state.ownerId().equals(player.getUUID()) && !state.dead()).orElse(false);
        }
    }
}
