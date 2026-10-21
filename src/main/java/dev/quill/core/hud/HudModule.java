package dev.quill.core.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** One movable HUD element. All elements are managed by HudManager and edited in ONE editor screen. */
public abstract class HudModule {
    public final String id;
    public final String title;
    public boolean enabled = false;   // default OFF
    public int x;
    public int y;

    protected HudModule(String id, String title, int x, int y) {
        this.id = id;
        this.title = title;
        this.x = x;
        this.y = y;
    }

    public abstract int width(Minecraft mc);

    public abstract int height(Minecraft mc);

    public abstract void render(GuiGraphics g, Minecraft mc, int x, int y);

    public void tick(Minecraft mc) {
    }
}
