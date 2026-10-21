package dev.quill.core.hud;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The ONE editor for every HUD element: drag to move, right-click to turn on/off, ESC to save. */
public class HudEditorScreen extends Screen {
    private HudModule dragging;
    private int dragDx;
    private int dragDy;

    public HudEditorScreen() {
        super(Component.literal("Quill HUD Editor"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, this.width, this.height, 0x99000000);
        g.drawCenteredString(this.font, "Quill HUD Editor", this.width / 2, 8, 0xFFFFFFFF);
        g.drawCenteredString(this.font, "Drag = move   |   Right-click = turn on/off   |   ESC = save & close",
                this.width / 2, 22, 0xFFBBBBBB);
        for (HudModule m : HudManager.modules()) {
            int w = m.width(this.minecraft);
            int h = m.height(this.minecraft);
            if (m.enabled) {
                m.render(g, this.minecraft, m.x, m.y);
                g.renderOutline(m.x - 1, m.y - 1, w + 2, h + 2, 0xFF55FF55);
            } else {   // off elements are still shown (dim) so you can place them before turning them on
                g.fill(m.x, m.y, m.x + w, m.y + h, 0x44FFFFFF);
                g.drawString(this.font, m.title + " (off)", m.x + 3, m.y + 3, 0xFFAAAAAA, false);
                g.renderOutline(m.x - 1, m.y - 1, w + 2, h + 2, 0xFF777777);
            }
        }
    }

    private HudModule hit(double mx, double my) {
        List<HudModule> list = HudManager.modules();
        for (int i = list.size() - 1; i >= 0; i--) {
            HudModule m = list.get(i);
            if (mx >= m.x && mx <= m.x + m.width(this.minecraft) && my >= m.y && my <= m.y + m.height(this.minecraft)) {
                return m;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        HudModule m = hit(mx, my);
        if (m == null) return super.mouseClicked(mx, my, button);
        if (button == 0) {
            dragging = m;
            dragDx = (int) mx - m.x;
            dragDy = (int) my - m.y;
        } else if (button == 1) {
            m.enabled = !m.enabled;
            HudManager.save();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null && button == 0) {
            int w = dragging.width(this.minecraft);
            int h = dragging.height(this.minecraft);
            dragging.x = Math.max(0, Math.min(this.width - w, (int) mx - dragDx));
            dragging.y = Math.max(0, Math.min(this.height - h, (int) my - dragDy));
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging != null) {
            dragging = null;
            HudManager.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public void onClose() {
        HudManager.save();
        super.onClose();
    }
}
