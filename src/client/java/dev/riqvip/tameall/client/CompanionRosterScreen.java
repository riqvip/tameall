package dev.riqvip.tameall.client;

import dev.riqvip.tameall.companion.CompanionPresence;
import dev.riqvip.tameall.network.CompanionRosterActionPayload;
import dev.riqvip.tameall.network.CompanionRosterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Compact owner-only directory opened by the Companion Whistle. */
public final class CompanionRosterScreen extends Screen {
    private static final int MIN_WIDTH = 360;
    private static final int MAX_WIDTH = 520;
    private static final int MIN_HEIGHT = 200;
    private static final int MAX_HEIGHT = 330;
    private static final int ROW_HEIGHT = 40;
    private List<CompanionRosterPayload.Entry> entries;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int rowTop;
    private int visibleRows;
    private int scrollOffset;
    private boolean draggingScrollbar;

    public CompanionRosterScreen(List<CompanionRosterPayload.Entry> entries) {
        super(Component.translatable("screen.tameall.roster"));
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    @Override
    protected void init() {
        panelWidth = Math.clamp(width - 8, MIN_WIDTH, MAX_WIDTH);
        panelHeight = Math.clamp(height - 8, MIN_HEIGHT, MAX_HEIGHT);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        rowTop = top + 42;
        visibleRows = Math.max(3, (panelHeight - 58) / ROW_HEIGHT);
        clearWidgets();
        addButton(Component.literal("×"), left + panelWidth - 24, top + 6, 18, 18, this::onClose);
        int end = Math.min(entries.size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < end; index++) {
            CompanionRosterPayload.Entry entry = entries.get(index);
            int y = rowTop + (index - scrollOffset) * ROW_HEIGHT;
            boolean dead = entry.dead();
            DarkButton action = addButton(Component.translatable(dead ? "tameall.roster.remove" : "tameall.roster.recall"),
                    left + panelWidth - 116, y + 2, 102, 20,
                    () -> ClientPlayNetworking.send(new CompanionRosterActionPayload(entry.bondId(),
                            dead ? CompanionRosterActionPayload.Action.REMOVE : CompanionRosterActionPayload.Action.RECALL)));
            // Recall is useful for both loaded and unloaded/unknown records:
            // the server is responsible for attempting the recorded-chunk
            // load and returning the authoritative status.
            action.active = true;
            if (entry.presence() == CompanionPresence.UNKNOWN) {
                action.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("tameall.roster.unknown")));
            } else if (entry.presence() == CompanionPresence.UNLOADED) {
                action.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Try to load this companion from its saved location.")));
            }
        }
    }

    private DarkButton addButton(Component label, int x, int y, int buttonWidth, int buttonHeight, Runnable action) {
        DarkButton button = new DarkButton(font, x, y, buttonWidth, buttonHeight, label, ignored -> action.run());
        addRenderableWidget(button);
        return button;
    }

    public void accept(List<CompanionRosterPayload.Entry> update) {
        entries = update == null ? List.of() : List.copyOf(update);
        scrollOffset = Math.min(scrollOffset, Math.max(0, entries.size() - visibleRows));
        rebuildWidgets();
    }

    @Override
    public void onClose() {
        CompanionClientRoster.closed(this);
        super.onClose();
    }

    @Override
    public void removed() {
        CompanionClientRoster.closed(this);
        super.removed();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= left && mouseX <= left + panelWidth && mouseY >= rowTop
                && mouseY <= top + panelHeight - 12 && scrollY != 0.0D) {
            int max = Math.max(0, entries.size() - visibleRows);
            scrollOffset = Math.clamp(scrollOffset + (scrollY < 0.0D ? 1 : -1), 0, max);
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int trackX = left + panelWidth - 8;
        if (event.button() == 0 && event.x() >= trackX - 3 && event.x() <= trackX + 3
                && event.y() >= rowTop && event.y() <= top + panelHeight - 12) {
            draggingScrollbar = true;
            updateScroll(event.y());
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScroll(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    private void updateScroll(double y) {
        int max = Math.max(0, entries.size() - visibleRows);
        int trackHeight = Math.max(1, top + panelHeight - 12 - rowTop);
        scrollOffset = Math.clamp((int) ((y - rowTop) * max / (double) trackHeight), 0, max);
        rebuildWidgets();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        CompanionUiStyle.panel(g, left, top, panelWidth, panelHeight);
        g.text(font, title, left + 12, top + 12, CompanionUiStyle.TEXT, false);
        g.text(font, Component.literal("Recall loads an unloaded companion when possible."),
                left + 12, top + 26, CompanionUiStyle.MUTED_TEXT, false);
        int end = Math.min(entries.size(), scrollOffset + visibleRows);
        for (int index = scrollOffset; index < end; index++) {
            CompanionRosterPayload.Entry entry = entries.get(index);
            int y = rowTop + (index - scrollOffset) * ROW_HEIGHT;
            int color = statusColor(entry.presence());
            String status = statusLabel(entry.presence());
            String settings = word(entry.mode().name()) + " · " + word(entry.stance().name());
            String location = entry.dimension() == null ? "Unknown location" : entry.dimension();
            if (entry.position() != null) location += " " + entry.position().x() + "," + entry.position().y() + "," + entry.position().z();
            g.text(font, Component.literal(font.plainSubstrByWidth(entry.displayName(), panelWidth - 140)),
                    left + 12, y, CompanionUiStyle.TEXT, false);
            g.text(font, Component.literal(settings), left + 12, y + 12, CompanionUiStyle.MUTED_TEXT, false);
            String details = entry.creatureType() + " · " + status + " · " + location;
            g.text(font, Component.literal(font.plainSubstrByWidth(details, panelWidth - 140)),
                    left + 12, y + 24, color, false);
        }
        int trackTop = rowTop;
        int trackBottom = top + panelHeight - 12;
        g.fill(left + panelWidth - 8, trackTop, left + panelWidth - 5, trackBottom, 0xFF4A4A4A);
        int max = Math.max(0, entries.size() - visibleRows);
        if (max > 0) {
            int thumbHeight = Math.max(16, (trackBottom - trackTop) * visibleRows / entries.size());
            int thumbTop = trackTop + (trackBottom - trackTop - thumbHeight) * scrollOffset / max;
            g.fill(left + panelWidth - 9, thumbTop, left + panelWidth - 4, thumbTop + thumbHeight, 0xFFAAAAAA);
        }
        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    private int statusColor(CompanionPresence presence) {
        return switch (presence) {
            case ALIVE -> CompanionUiStyle.STATUS_ALIVE;
            case DEAD -> CompanionUiStyle.STATUS_DEAD;
            case UNLOADED, UNKNOWN -> CompanionUiStyle.STATUS_UNKNOWN;
        };
    }

    private String statusLabel(CompanionPresence presence) {
        return switch (presence) {
            case ALIVE -> "Alive"; case UNLOADED -> "Unloaded";
            case UNKNOWN -> "Unknown"; case DEAD -> "Dead";
        };
    }

    private String word(String value) { return value.toLowerCase(java.util.Locale.ROOT).replace('_', ' '); }
}
