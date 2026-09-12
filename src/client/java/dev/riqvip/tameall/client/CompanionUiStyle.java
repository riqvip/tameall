package dev.riqvip.tameall.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared dark palette used by the companion controls, roster, target editor, and inventory. */
final class CompanionUiStyle {
    static final int PANEL = 0xFF242424;
    static final int PANEL_EDGE = 0xFF5A5A5A;
    static final int PANEL_HIGHLIGHT = 0xFF777777;
    static final int TEXT = 0xFFE0E0E0;
    static final int MUTED_TEXT = 0xFF9E9E9E;
    static final int ID_TEXT = 0xFFAAAAAA;
    static final int STATUS_ALIVE = 0xFF55CC66;
    static final int STATUS_DEAD = 0xFFFF6666;
    static final int STATUS_UNKNOWN = 0xFFAAAAAA;
    static final int ERROR = 0xFFFF6666;

    private CompanionUiStyle() {}

    static void panel(GuiGraphicsExtractor graphics, int left, int top, int width, int height) {
        graphics.fill(left, top, left + width, top + height, 0xFF101010);
        graphics.fill(left + 1, top + 1, left + width - 1, top + 3, PANEL_HIGHLIGHT);
        graphics.fill(left + 2, top + 3, left + width - 2, top + height - 2, PANEL_EDGE);
        graphics.fill(left + 4, top + 5, left + width - 4, top + height - 4, PANEL);
    }
}
