package dev.quill.core.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Small "logo + Quill Client" mark in the bottom-right of the pause menu, inventory and chests. */
public final class QuillBadge {
    public static final ResourceLocation LOGO_32 = ResourceLocation.fromNamespaceAndPath("quill_core", "textures/gui/logo_32.png");
    public static final ResourceLocation LOGO_64 = ResourceLocation.fromNamespaceAndPath("quill_core", "textures/gui/logo_64.png");

    private QuillBadge() {
    }

    public static void draw(GuiGraphics g, Font font, int screenW, int screenH) {
        String text = "Quill Client";
        int y = screenH - 24;
        g.blit(LOGO_32, screenW - 26, y - 2, 20, 20, 0f, 0f, 32, 32, 32, 32);
        g.drawString(font, text, screenW - font.width(text) - 30, y + 3, 0xFFFFFFFF, true);
    }
}
