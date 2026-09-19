package dev.riqvip.tameall.client;

import dev.riqvip.tameall.companion.CompanionCapabilities;
import dev.riqvip.tameall.companion.CompanionMode;
import dev.riqvip.tameall.companion.HealthDisplayMode;
import dev.riqvip.tameall.companion.NameplateMode;
import dev.riqvip.tameall.companion.CombatStance;
import dev.riqvip.tameall.companion.TeleportMode;
import dev.riqvip.tameall.menu.CompanionAction;
import dev.riqvip.tameall.menu.CompanionActionRequest;
import dev.riqvip.tameall.menu.CompanionMenuSnapshot;
import dev.riqvip.tameall.companion.CompanionNames;
import dev.riqvip.tameall.network.CompanionActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Compact tabbed companion controls that fit both windowed and fullscreen GUI scales. */
public final class CompanionScreen extends Screen {
    private static final int MIN_WIDTH = 360;
    private static final int MAX_WIDTH = 420;
    private static final int MIN_HEIGHT = 220;
    private static final int MAX_HEIGHT = 286;
    private static final int CONTENT_TOP = 58;

    private enum Tab { OVERVIEW, MOVEMENT, COLLECTION, ABILITIES, INVENTORY }

    private CompanionMenuSnapshot snapshot;
    private EditBox rename;
    private EditBox areaRadius;
    private EditBox pickupRadius;
    private EditBox xpRadius;
    private Tab tab = Tab.OVERVIEW;
    private int left;
    private int top;
    private int widthPanel;
    private int heightPanel;
    private int contentLeft;
    private String savedName;
    private boolean nameDirty;
    private int movementScroll;
    private boolean draggingMovementScrollbar;

    public CompanionScreen(CompanionMenuSnapshot snapshot) {
        super(Component.translatable("screen.tameall.companion"));
        this.snapshot = snapshot;
        this.savedName = snapshot.displayName() == null ? "" : snapshot.displayName();
    }

    @Override
    protected void init() {
        widthPanel = Math.clamp(width - 8, MIN_WIDTH, MAX_WIDTH);
        heightPanel = Math.clamp(height - 8, MIN_HEIGHT, MAX_HEIGHT);
        left = (width - widthPanel) / 2;
        top = (height - heightPanel) / 2;
        contentLeft = left + 112;
        movementScroll = Math.clamp(movementScroll, 0, movementMaxScroll());
        clearWidgets();
        addButton(Component.literal("×"), left + widthPanel - 24, top + 6, 18, 18, this::onClose);
        int y = top + 47;
        for (Tab value : Tab.values()) {
            Tab selected = value;
            DarkButton button = addButton(Component.literal(tabLabel(value)), left + 8, y, 94, 20,
                    () -> selectTab(selected));
            button.active = value != tab;
            y += 23;
        }
        if (tab == Tab.OVERVIEW) buildOverview();
        else if (tab == Tab.MOVEMENT) buildMovement();
        else if (tab == Tab.COLLECTION) buildCollection();
        else if (tab == Tab.ABILITIES) buildAbilities();
        else if (tab == Tab.INVENTORY) buildInventoryPage();
        else buildInventoryPage();
    }

    private void selectTab(Tab value) {
        saveNameIfChanged();
        tab = value;
        rebuildWidgets();
    }

    private void buildOverview() {
        rename = new EditBox(font, contentLeft, top + CONTENT_TOP, widthPanel - 124, 20,
                Component.translatable("tameall.rename"));
        rename.setMaxLength(64);
        rename.setTextShadow(false);
        rename.setTextColor(CompanionUiStyle.TEXT);
        rename.setTextColorUneditable(CompanionUiStyle.MUTED_TEXT);
        rename.setValue(snapshot.displayName() == null ? "" : snapshot.displayName());
        rename.setHint(Component.literal("Nametag (auto-saves)"));
        rename.setTooltip(Tooltip.create(Component.literal("Type a nametag; leaving the field saves it automatically.")));
        rename.setResponder(value -> nameDirty = !value.equals(savedName));
        addRenderableWidget(rename);
        addButton(Component.literal(healthLabel()), contentLeft, top + CONTENT_TOP + 30, widthPanel - 124, 20,
                this::cycleHealth).setTooltip(Tooltip.create(Component.literal(
                "Controls whether this companion's health tag is shown above it.")));
        addButton(Component.literal(nameplateLabel()), contentLeft, top + CONTENT_TOP + 56, widthPanel - 124, 20,
                this::cycleNameplate).setTooltip(Tooltip.create(Component.literal(
                "Controls when this companion's nametag is visible.")));
    }

    private void buildMovement() {
        movementButton(Component.literal(modeLabel()), 0,
                this::cycleMode).setTooltip(Tooltip.create(Component.literal(
                "Follow, Stay, or Guard determines how the companion moves inside its shared area.")));
        movementButton(Component.literal(stanceLabel()), 26,
                this::cycleStance).setTooltip(Tooltip.create(Component.literal(
                "Passive never attacks, Assist follows combat signals, and Defend Area scans selected targets.")));
        areaRadius = new EditBox(font, contentLeft, movementY(52), 76, 20, Component.literal("Area radius"));
        areaRadius.setMaxLength(3);
        styleEditBox(areaRadius, "Shared operating area radius, from 4 to 999 blocks.");
        areaRadius.setValue(Integer.toString(snapshot.settings().areaRadius()));
        areaRadius.setHint(Component.literal("4-999"));
        areaRadius.setVisible(movementVisible(areaRadius.getY(), 20));
        addRenderableWidget(areaRadius);
        movementButton(Component.literal("Save area"), 52, contentLeft + 82, widthPanel - 206,
                this::applyAreaRadius).setTooltip(Tooltip.create(Component.literal("Apply the area radius.")));
        movementButton(Component.literal(teleportLabel()), 78,
                this::cycleTeleport).setTooltip(Tooltip.create(Component.literal(
                "Choose when the companion may teleport back to its operating area.")));
        movementButton(Component.literal("Set guard point"), 104,
                () -> send(CompanionActionRequest.command(snapshot.bondId(), snapshot.revision(),
                        CompanionAction.SET_GUARD_ANCHOR))).setTooltip(Tooltip.create(Component.literal(
                "Save your current position as the Guard and Stay anchor.")));
        movementButton(Component.literal("Edit targets"), 130,
                () -> send(CompanionActionRequest.command(snapshot.bondId(), snapshot.revision(),
                        CompanionAction.OPEN_TARGET_SELECTOR))).setTooltip(Tooltip.create(Component.literal(
                "Choose multiple target groups or individual living entities.")));
        DarkButton saddle = movementButton(Component.literal(saddleLabel()), 156,
                () -> toggle(CompanionAction.SET_SADDLE_REQUIRED,
                        !snapshot.settings().saddleRequired()));
        saddle.active = snapshot.cheatsAllowed() || !snapshot.settings().saddleRequired();
        saddle.setTooltip(Tooltip.create(Component.literal(snapshot.cheatsAllowed()
                ? "Require a saddle before mounting this companion."
                : "Removing the saddle requirement needs permission level 2.")));
        movementButton(Component.literal(mountedAttackLabel()), 182,
                () -> toggle(CompanionAction.SET_ATTACK_WHILE_MOUNTED,
                        !snapshot.settings().attackWhileMounted())).setTooltip(Tooltip.create(Component.literal(
                "When enabled, the companion attacks eligible targets within reach while you steer it.")));
    }

    private DarkButton movementButton(Component label, int offset, Runnable action) {
        return movementButton(label, offset, contentLeft, widthPanel - 124, action);
    }

    private DarkButton movementButton(Component label, int offset, int x, int buttonWidth, Runnable action) {
        DarkButton button = addButton(label, x, movementY(offset), buttonWidth, 20, action);
        button.visible = movementVisible(button.getY(), 20);
        return button;
    }

    private int movementY(int offset) { return top + CONTENT_TOP + offset - movementScroll; }

    private boolean movementVisible(int y, int height) {
        return y >= top + CONTENT_TOP && y + height <= top + heightPanel - 14;
    }

    private int movementMaxScroll() {
        int contentBottom = top + CONTENT_TOP + 202;
        int viewportBottom = top + heightPanel - 14;
        return Math.max(0, contentBottom - viewportBottom);
    }

    private void buildCollection() {
        addButton(Component.literal(pickupLabel()), contentLeft, top + CONTENT_TOP, widthPanel - 124, 20,
                () -> toggle(CompanionAction.SET_PICKUP_ITEMS, !snapshot.settings().pickupItems()))
                .setTooltip(Tooltip.create(Component.literal("Allow the companion to collect nearby item drops.")));
        pickupRadius = new EditBox(font, contentLeft, top + CONTENT_TOP + 26, 76, 20, Component.literal("Item radius"));
        pickupRadius.setMaxLength(4);
        styleEditBox(pickupRadius, "Item pickup radius, from 1 to 16 blocks.");
        pickupRadius.setValue(String.format(Locale.ROOT, "%.1f", snapshot.settings().pickupRadius()));
        pickupRadius.setHint(Component.literal("1-16"));
        addRenderableWidget(pickupRadius);
        addButton(Component.literal("Save radius"), contentLeft + 82, top + CONTENT_TOP + 26, widthPanel - 206, 20,
                this::applyPickupRadius).setTooltip(Tooltip.create(Component.literal("Apply the item pickup radius.")));
        addButton(Component.literal(xpLabel()), contentLeft, top + CONTENT_TOP + 52, widthPanel - 124, 20,
                () -> toggle(CompanionAction.SET_COLLECT_XP, !snapshot.settings().collectXpForMending()))
                .setTooltip(Tooltip.create(Component.literal(
                "Collect XP only to repair damaged Mending equipment; surplus XP remains on the ground.")));
        xpRadius = new EditBox(font, contentLeft, top + CONTENT_TOP + 78, 76, 20, Component.literal("XP radius"));
        xpRadius.setMaxLength(4);
        styleEditBox(xpRadius, "XP collection radius, from 1 to 16 blocks.");
        xpRadius.setValue(String.format(Locale.ROOT, "%.1f", snapshot.settings().xpRadius()));
        xpRadius.setHint(Component.literal("1-16"));
        addRenderableWidget(xpRadius);
        addButton(Component.literal("Save radius"), contentLeft + 82, top + CONTENT_TOP + 78, widthPanel - 206, 20,
                this::applyXpRadius).setTooltip(Tooltip.create(Component.literal("Apply the XP collection radius.")));
        DarkButton durability = addButton(Component.literal(durabilityLabel()), contentLeft,
                top + CONTENT_TOP + 104, widthPanel - 124, 20,
                () -> toggle(CompanionAction.SET_USE_DURABILITY, !snapshot.settings().useDurability()));
        durability.setTooltip(Tooltip.create(Component.literal(
                "When enabled, the companion's equipment uses normal durability; disabling this protection requires permission level 2.")));
        durability.active = snapshot.cheatsAllowed() || !snapshot.settings().useDurability();
        DarkButton cargo = addButton(Component.literal(cargoContainerLabel()), contentLeft,
                top + CONTENT_TOP + 130, widthPanel - 124, 20,
                () -> toggle(CompanionAction.SET_CARGO_CONTAINER_REQUIRED,
                        !snapshot.settings().cargoContainerRequired()));
        cargo.active = snapshot.cheatsAllowed() || !snapshot.settings().cargoContainerRequired();
        cargo.setTooltip(Tooltip.create(Component.literal(snapshot.cheatsAllowed()
                ? "Require a chest, barrel, trapped chest, or shulker box before using cargo."
                : "Using cargo without a container requires permission level 2.")));
    }

    private void buildAbilities() {
        CompanionCapabilities capabilities = snapshot.capabilities();
        int row = 0;
        if (capabilities.sunlightProtection()) {
            addAbility(Component.literal(sunlightLabel()), CompanionAction.SET_SUNLIGHT_PROTECTION,
                    snapshot.settings().magic().sunlightProtection(), row++, "Prevents daylight burning.");
        }
        if (capabilities.drowningProtection()) {
            addAbility(Component.literal(drowningLabel()), CompanionAction.SET_DROWNING_PROTECTION,
                    snapshot.settings().magic().drowningProtection(), row++, "Prevents drowning and restores air.");
        }
        if (capabilities.reusableExplosions()) {
            addAbility(Component.literal(reusableLabel()), CompanionAction.SET_REUSABLE_EXPLOSIONS,
                    snapshot.settings().magic().reusableExplosions(), row++, "Uses reusable entity-only blasts.");
        }
        if (capabilities.preventVexExpiry()) {
            addAbility(Component.literal(vexExpiryLabel()), CompanionAction.SET_PREVENT_VEX_EXPIRY,
                    snapshot.settings().magic().preventVexExpiry(), row++, "Prevents limited-life Vex expiry.");
        }
        if (capabilities.endermanBlockPickup()) {
            addAbility(Component.literal(endermanPickupLabel()), CompanionAction.SET_ENDERMAN_BLOCK_PICKUP,
                    snapshot.settings().magic().allowEndermanBlockPickup(), row++, "Allows new block pickups.");
        }
    }

    private void addAbility(Component label, CompanionAction action, boolean enabled, int row, String description) {
        DarkButton button = addButton(Component.literal(label.getString() + (enabled ? " ON" : " OFF")),
                contentLeft, top + CONTENT_TOP + row * 26, widthPanel - 124, 20,
                () -> toggle(action, !enabled));
        button.active = snapshot.cheatsAllowed() || enabled || action == CompanionAction.SET_ENDERMAN_BLOCK_PICKUP;
        button.setTooltip(Tooltip.create(Component.literal(description + (snapshot.cheatsAllowed()
                ? "" : " Enabling requires permission level 2."))));
    }

    private void buildInventoryPage() {
        addButton(Component.literal("Open Inventory"), contentLeft, top + CONTENT_TOP, widthPanel - 124, 20,
                () -> send(CompanionActionRequest.command(snapshot.bondId(), snapshot.revision(),
                        CompanionAction.OPEN_INVENTORY))).setTooltip(Tooltip.create(Component.literal(
                "Open the companion's equipment and cargo.")));
        DarkButton hint = addButton(Component.literal("Equipment and cargo open in the Inventory tab."), contentLeft,
                top + CONTENT_TOP + 30, widthPanel - 124, 20, () -> {});
        hint.active = false;
    }

    public void accept(CompanionMenuSnapshot update) {
        if (update.bondId().equals(snapshot.bondId()) && update.revision() >= snapshot.revision()) {
            snapshot = update;
            savedName = update.displayName() == null ? "" : update.displayName();
            nameDirty = false;
            rebuildWidgets();
        }
    }

    private DarkButton addButton(Component label, int x, int y, int buttonWidth, int buttonHeight,
                                 Runnable action) {
        DarkButton button = new DarkButton(font, x, y, buttonWidth, buttonHeight, label, ignored -> action.run());
        addRenderableWidget(button);
        return button;
    }

    private void styleEditBox(EditBox field, String description) {
        field.setTextShadow(false);
        field.setTextColor(CompanionUiStyle.TEXT);
        field.setTextColorUneditable(CompanionUiStyle.MUTED_TEXT);
        field.setTooltip(Tooltip.create(Component.literal(description)));
    }

    private void saveNameIfChanged() {
        if (rename == null || !nameDirty) return;
        String value = rename.getValue();
        if (value.equals(savedName)) {
            nameDirty = false;
            return;
        }
        savedName = value;
        nameDirty = false;
        send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), CompanionAction.RENAME,
                value, 0, false, null, null, null, null, null, null, 0.0D));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (tab == Tab.MOVEMENT && event.button() == 0 && movementScrollBarContains(event.x(), event.y())) {
            draggingMovementScrollbar = true;
            updateMovementScroll(event.y());
            return true;
        }
        boolean wasEditing = rename != null && rename.isFocused();
        boolean insideRename = rename != null && event.x() >= rename.getX() && event.x() <= rename.getRight()
                && event.y() >= rename.getY() && event.y() <= rename.getBottom();
        // Send the rename before another control emits its own revisioned
        // request.  Otherwise the second click could race the name packet and
        // be rejected as stale by the server.
        if (wasEditing && !insideRename) saveNameIfChanged();
        boolean handled = super.mouseClicked(event, doubleClick);
        return handled;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingMovementScrollbar) {
            updateMovementScroll(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingMovementScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tab == Tab.MOVEMENT && scrollY != 0.0D
                && mouseX >= left + 100 && mouseX <= left + widthPanel
                && mouseY >= top + CONTENT_TOP && mouseY <= top + heightPanel - 14) {
            movementScroll = Math.clamp(movementScroll + (scrollY < 0.0D ? 18 : -18),
                    0, movementMaxScroll());
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean movementScrollBarContains(double x, double y) {
        return movementMaxScroll() > 0 && x >= left + widthPanel - 10 && x <= left + widthPanel - 3
                && y >= top + CONTENT_TOP && y <= top + heightPanel - 14;
    }

    private void updateMovementScroll(double y) {
        int max = movementMaxScroll();
        int trackTop = top + CONTENT_TOP;
        int trackHeight = Math.max(1, top + heightPanel - 14 - trackTop);
        movementScroll = Math.clamp((int) ((y - trackTop) * max / (double) trackHeight), 0, max);
        rebuildWidgets();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (rename != null && rename.isFocused() && event.key() == 257) {
            saveNameIfChanged();
            rename.setFocused(false);
            return true;
        }
        if (event.key() == 256) saveNameIfChanged();
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        saveNameIfChanged();
        super.onClose();
    }

    private void send(CompanionActionRequest request) { ClientPlayNetworking.send(new CompanionActionPayload(request)); }

    private void toggle(CompanionAction action, boolean value) {
        send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), action,
                null, 0, value, null, null, null, null, null, null, 0.0D));
    }

    private void applyAreaRadius() {
        try {
            int value = Integer.parseInt(areaRadius.getValue().trim());
            if (value < 4 || value > 999) throw new NumberFormatException();
            send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), CompanionAction.SET_GUARD_RADIUS,
                    null, value, false, null, null, null, null, null, null, 0.0D));
        } catch (NumberFormatException ignored) { areaRadius.setValue(Integer.toString(snapshot.settings().areaRadius())); }
    }

    private void applyPickupRadius() { applyDecimal(CompanionAction.SET_PICKUP_RADIUS, pickupRadius, snapshot.settings().pickupRadius()); }
    private void applyXpRadius() { applyDecimal(CompanionAction.SET_XP_RADIUS, xpRadius, snapshot.settings().xpRadius()); }

    private void applyDecimal(CompanionAction action, EditBox field, double fallback) {
        try {
            double value = Double.parseDouble(field.getValue().trim());
            if (!Double.isFinite(value) || value < 1.0D || value > 16.0D) throw new NumberFormatException();
            send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), action,
                    null, 0, false, null, null, null, null, null, null, value));
        } catch (NumberFormatException ignored) { field.setValue(String.format(Locale.ROOT, "%.1f", fallback)); }
    }

    private void cycleMode() {
        CompanionMode[] values = CompanionMode.values();
        send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), CompanionAction.SET_MODE,
                null, 0, false, values[(snapshot.settings().mode().ordinal() + 1) % values.length],
                null, null, null, null, null, 0.0D));
    }

    private void cycleStance() {
        CombatStance[] values = CombatStance.values();
        send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), CompanionAction.SET_STANCE,
                null, 0, false, null, values[(snapshot.settings().stance().ordinal() + 1) % values.length],
                null, null, null, null, 0.0D));
    }

    private void cycleNameplate() {
        NameplateMode[] values = NameplateMode.values();
        send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), CompanionAction.SET_NAMEPLATE,
                null, 0, false, null, null, values[(snapshot.settings().nameplate().ordinal() + 1) % values.length],
                null, null, null, 0.0D));
    }

    private void cycleHealth() {
        HealthDisplayMode[] values = HealthDisplayMode.values();
        send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), CompanionAction.SET_HEALTH_DISPLAY,
                null, 0, false, null, null, null, values[(snapshot.settings().healthDisplay().ordinal() + 1) % values.length],
                null, null, 0.0D));
    }

    private void cycleTeleport() {
        TeleportMode[] values = TeleportMode.values();
        send(new CompanionActionRequest(snapshot.bondId(), snapshot.revision(), CompanionAction.SET_TELEPORT_MODE,
                null, 0, false, null, null, null, null, null,
                values[(snapshot.settings().teleportMode().ordinal() + 1) % values.length], 0.0D));
    }

    private String tabLabel(Tab value) { return switch (value) {
        case OVERVIEW -> "Overview"; case MOVEMENT -> "Movement"; case COLLECTION -> "Collection";
        case ABILITIES -> "Abilities"; case INVENTORY -> "Inventory";
    }; }
    private String modeLabel() { return "Movement: " + word(snapshot.settings().mode().name()); }
    private String stanceLabel() { return "Combat: " + word(snapshot.settings().stance().name()); }
    private String healthLabel() { return "Health tag: " + snapshot.settings().healthDisplay().name(); }
    private String nameplateLabel() { return "Nametag: " + word(snapshot.settings().nameplate().name()); }
    private String teleportLabel() { return "Teleport: " + word(snapshot.settings().teleportMode().name()); }
    private String pickupLabel() { return "Pickup items: " + onOff(snapshot.settings().pickupItems()); }
    private String xpLabel() { return "Collect XP for Mending: " + onOff(snapshot.settings().collectXpForMending()); }
    private String durabilityLabel() { return "Item durability: " + onOff(snapshot.settings().useDurability()); }
    private String cargoContainerLabel() { return "Cargo container required: " + onOff(snapshot.settings().cargoContainerRequired()); }
    private String saddleLabel() { return "Saddle required: " + onOff(snapshot.settings().saddleRequired()); }
    private String mountedAttackLabel() { return "Attack while mounted: " + onOff(snapshot.settings().attackWhileMounted()); }
    private String sunlightLabel() { return "Sun protection"; }
    private String drowningLabel() { return "Drowning protection"; }
    private String reusableLabel() { return "Reusable explosions"; }
    private String vexExpiryLabel() { return "Vex expiry protection"; }
    private String endermanPickupLabel() { return "Enderman block pickup"; }
    private String onOff(boolean value) { return value ? "ON" : "OFF"; }
    private String word(String value) { return value.toLowerCase(Locale.ROOT).replace('_', ' '); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        CompanionUiStyle.panel(g, left, top, widthPanel, heightPanel);
        g.text(font, title, left + 12, top + 12, CompanionUiStyle.TEXT, false);
        String name = CompanionNames.displayName(snapshot.displayName(), snapshot.creatureType());
        g.text(font, Component.literal(font.plainSubstrByWidth(name, 94)), left + 8, top + 27,
                CompanionUiStyle.TEXT, false);
        String id = font.plainSubstrByWidth(snapshot.creatureType(), Math.max(100, widthPanel - 150));
        g.text(font, Component.literal(id), left + widthPanel - 12 - font.width(id), top + 28,
                CompanionUiStyle.ID_TEXT, false);
        g.text(font, Component.literal(String.format(Locale.ROOT, "%.1f / %.1f HP", snapshot.health(), snapshot.maxHealth())),
                contentLeft, top + 43, CompanionUiStyle.STATUS_DEAD, false);
        if (tab == Tab.MOVEMENT && movementMaxScroll() > 0) {
            int trackTop = top + CONTENT_TOP;
            int trackBottom = top + heightPanel - 14;
            g.fill(left + widthPanel - 8, trackTop, left + widthPanel - 5, trackBottom, 0xFF4A4A4A);
            int max = movementMaxScroll();
            int thumbHeight = Math.max(16, (trackBottom - trackTop) * (trackBottom - trackTop)
                    / Math.max(1, trackBottom - trackTop + max));
            int thumbTop = trackTop + (trackBottom - trackTop - thumbHeight) * movementScroll / max;
            g.fill(left + widthPanel - 9, thumbTop, left + widthPanel - 4,
                    thumbTop + thumbHeight, 0xFFAAAAAA);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }
}
