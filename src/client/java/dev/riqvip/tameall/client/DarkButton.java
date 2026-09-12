package dev.riqvip.tameall.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
/** A compact dark button whose label is deliberately rendered without a shadow. */
final class DarkButton extends AbstractWidget {
    @FunctionalInterface
    interface PressHandler {
        void press(DarkButton button);
    }

    private final Font font;
    private final PressHandler pressHandler;

    DarkButton(Font font, int x, int y, int width, int height, Component message,
               PressHandler pressHandler) {
        super(x, y, width, height, message);
        this.font = font;
        this.pressHandler = pressHandler;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int background = !active ? 0xFF292929
                : isHoveredOrFocused() ? 0xFF505050 : 0xFF3A3A3A;
        int edge = !active ? 0xFF414141 : isHoveredOrFocused() ? 0xFF909090 : 0xFF696969;
        graphics.fill(getX(), getY(), getRight(), getBottom(), edge);
        graphics.fill(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1, background);
        String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(0, width - 10));
        int textColor = active ? CompanionUiStyle.TEXT : CompanionUiStyle.MUTED_TEXT;
        graphics.text(font, Component.literal(label),
                getX() + (width - font.width(label)) / 2, getY() + (height - 8) / 2,
                textColor, false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (active && event.button() == 0) pressHandler.press(this);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (active && isFocused() && (event.key() == 257 || event.key() == 32)) {
            pressHandler.press(this);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
