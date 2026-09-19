package dev.riqvip.tameall.menu;

import dev.riqvip.tameall.TameAll;
import dev.riqvip.tameall.companion.CompanionEquipment;
import dev.riqvip.tameall.companion.CompanionContainer;
import dev.riqvip.tameall.companion.CompanionAttachments;
import dev.riqvip.tameall.companion.CompanionRiding;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ArmorSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/** Vanilla-style pet equipment, cargo, and owner-inventory menu. */
public final class CompanionMenu extends AbstractContainerMenu {
    public static final int EQUIPMENT_SIZE = CompanionEquipment.SIZE;
    public static final int RIDING_SLOT = EQUIPMENT_SIZE;
    public static final int CARGO_START = EQUIPMENT_SIZE + 1;
    public static final int CONTAINER_SLOT = CARGO_START + 27;
    public static final int COMPANION_SIZE = CONTAINER_SLOT + 1;
    public static final int PLAYER_START = COMPANION_SIZE;
    public static final int HOTBAR_START = PLAYER_START + 27;
    public static final int TOTAL_SIZE = HOTBAR_START + 9;

    private final Container companion;
    private final LivingEntity entity;
    private final int entityId;
    private final boolean clientMenu;

    private CompanionMenu(int id, Inventory inventory, Container companion,
                          LivingEntity entity, int entityId, boolean clientMenu) {
        super(TameAll.COMPANION_MENU, id);
        this.companion = companion;
        this.entity = entity;
        this.entityId = entityId;
        this.clientMenu = clientMenu;

        if (entity != null) {
            addSlot(new ArmorSlot(companion, entity, EquipmentSlot.HEAD, 0, 8, 8, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET));
            addSlot(new ArmorSlot(companion, entity, EquipmentSlot.CHEST, 1, 8, 26, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE));
            addSlot(new ArmorSlot(companion, entity, EquipmentSlot.LEGS, 2, 8, 44, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS));
            addSlot(new ArmorSlot(companion, entity, EquipmentSlot.FEET, 3, 8, 62, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS));
        } else {
            addSlot(new CompanionEquipmentSlot(companion, entity, 0, 8, 8, EquipmentSlot.HEAD, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET));
            addSlot(new CompanionEquipmentSlot(companion, entity, 1, 8, 26, EquipmentSlot.CHEST, InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE));
            addSlot(new CompanionEquipmentSlot(companion, entity, 2, 8, 44, EquipmentSlot.LEGS, InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS));
            addSlot(new CompanionEquipmentSlot(companion, entity, 3, 8, 62, EquipmentSlot.FEET, InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS));
        }
        addSlot(new CompanionHandSlot(companion, CompanionEquipment.MAINHAND, 112, 62));
        addSlot(new CompanionHandSlot(companion, CompanionEquipment.OFFHAND, 148, 62,
                InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD));
        addSlot(new CompanionSaddleSlot(companion, entity, RIDING_SLOT, 148, 34));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new CompanionCargoSlot(companion, entity, CARGO_START + row * 9 + column,
                        8 + column * 18, 90 + row * 18));
            }
        }
        addSlot(new CompanionContainerSlot(companion, entity, CONTAINER_SLOT, 112, 34));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        8 + column * 18, 152 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 210));
        }
    }

    public static CompanionMenu server(int id, Inventory inventory, LivingEntity entity) {
        return new CompanionMenu(id, inventory, CompanionEquipment.container(entity), entity,
                entity.getId(), false);
    }

    /** Client constructor used by the extended menu opening packet. */
    public static CompanionMenu client(int id, Inventory inventory, int entityId) {
        LivingEntity entity = inventory.player.level().getEntity(entityId) instanceof LivingEntity living ? living : null;
        return new CompanionMenu(id, inventory, new SimpleContainer(COMPANION_SIZE), entity,
                entityId, true);
    }

    public int entityId() { return entityId; }

    public boolean isClientMenu() { return clientMenu; }

    @Override public boolean stillValid(Player player) {
        return clientMenu || companion.stillValid(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(slotIndex);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack original = source.getItem().copy();
        ItemStack moving = original.copy();
        boolean moved;
        if (slotIndex < COMPANION_SIZE) {
            moved = moveItemStackTo(moving, PLAYER_START, TOTAL_SIZE, true);
        } else {
            moved = false;
            for (int equipmentIndex = 0; equipmentIndex < 4 && !moving.isEmpty(); equipmentIndex++) {
                Slot destination = slots.get(equipmentIndex);
                if (!destination.hasItem() && destination.mayPlace(moving)) {
                    moved |= moveItemStackTo(moving, equipmentIndex, equipmentIndex + 1, false);
                }
            }
            if (!moving.isEmpty() && !slots.get(RIDING_SLOT).hasItem()
                    && slots.get(RIDING_SLOT).mayPlace(moving)) {
                moved |= moveItemStackTo(moving, RIDING_SLOT, RIDING_SLOT + 1, false);
            }
            if (!moving.isEmpty() && !slots.get(CONTAINER_SLOT).hasItem()
                    && slots.get(CONTAINER_SLOT).mayPlace(moving)) {
                moved |= moveItemStackTo(moving, CONTAINER_SLOT, CONTAINER_SLOT + 1, false);
            }
            if (!moving.isEmpty() && !slots.get(CompanionEquipment.OFFHAND).hasItem()
                    && moving.getItem() instanceof ShieldItem) {
                moved |= moveItemStackTo(moving, CompanionEquipment.OFFHAND, CompanionEquipment.OFFHAND + 1, false);
            }
            if (!moving.isEmpty()) moved |= moveItemStackTo(moving, CARGO_START, PLAYER_START, false);
        }
        if (!moved) return ItemStack.EMPTY;
        source.setByPlayer(moving, original);
        if (moving.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();
        source.onTake(player, moving);
        return original;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        companion.stopOpen(player);
    }

    private static final class CompanionEquipmentSlot extends Slot {
        private final Identifier emptyIcon;
        private final EquipmentSlot equipmentSlot;
        private final LivingEntity entity;

        private CompanionEquipmentSlot(Container container, LivingEntity entity, int index, int x, int y,
                                       EquipmentSlot equipmentSlot, Identifier emptyIcon) {
            super(container, index, x, y);
            this.emptyIcon = emptyIcon;
            this.equipmentSlot = equipmentSlot;
            this.entity = entity;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return entity != null && entity.isEquippableInSlot(stack, equipmentSlot);
        }

        @Override public int getMaxStackSize() { return 1; }

        @Override public Identifier getNoItemIcon() { return emptyIcon; }
    }

    private static final class CompanionHandSlot extends Slot {
        private final Identifier emptyIcon;

        private CompanionHandSlot(Container container, int index, int x, int y) {
            this(container, index, x, y, null);
        }

        private CompanionHandSlot(Container container, int index, int x, int y, Identifier emptyIcon) {
            super(container, index, x, y);
            this.emptyIcon = emptyIcon;
        }

        @Override public boolean mayPlace(ItemStack stack) { return !stack.isEmpty(); }

        @Override public Identifier getNoItemIcon() { return emptyIcon; }
    }

    private static final class CompanionSaddleSlot extends Slot {
        private final LivingEntity entity;

        private CompanionSaddleSlot(Container container, LivingEntity entity, int index, int x, int y) {
            super(container, index, x, y);
            this.entity = entity;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return entity != null && CompanionRiding.isRidingEquipment(entity, stack);
        }
        @Override public int getMaxStackSize() { return 1; }

        @Override public Identifier getNoItemIcon() {
            return entity != null && CompanionRiding.ridingEquipmentSlot(entity) == EquipmentSlot.SADDLE
                    ? Identifier.withDefaultNamespace("container/slot/saddle") : null;
        }
    }

    private static final class CompanionCargoSlot extends Slot {
        private final Container companion;
        private final int containerIndex;
        private final LivingEntity entity;

        private CompanionCargoSlot(Container companion, LivingEntity entity, int index, int x, int y) {
            super(companion, index, x, y);
            this.companion = companion;
            this.entity = entity;
            this.containerIndex = CONTAINER_SLOT;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return !companion.getItem(containerIndex).isEmpty()
                    || entity != null && CompanionContainer.canStoreCargo(entity);
        }

        @Override public boolean isActive() {
            return !companion.getItem(containerIndex).isEmpty() || hasItem()
                    || entity != null && CompanionAttachments.getRidingView(entity)
                    .map(view -> !view.cargoContainerRequired()).orElse(false);
        }
    }

    private static final class CompanionContainerSlot extends Slot {
        private final LivingEntity entity;

        private CompanionContainerSlot(Container container, LivingEntity entity, int index, int x, int y) {
            super(container, index, x, y);
            this.entity = entity;
        }

        @Override public boolean mayPlace(ItemStack stack) {
            return entity != null && CompanionContainer.canPlace(entity, stack);
        }

        @Override public int getMaxStackSize() { return 1; }

        @Override public Identifier getNoItemIcon() {
            return TameAll.id("container/slot/container");
        }
    }
}
