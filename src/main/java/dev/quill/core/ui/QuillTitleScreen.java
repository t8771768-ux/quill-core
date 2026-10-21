package dev.quill.core.ui;

import dev.quill.core.hud.HudEditorScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/** Quill Client main menu (replaces the vanilla title screen). */
public class QuillTitleScreen extends Screen {
    public QuillTitleScreen() {
        super(Component.literal("Quill Client"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 6;
        add("Singleplayer", x, y, () -> this.minecraft.setScreen(new SelectWorldScreen(this)));
        add("Multiplayer", x, y + 24, () -> this.minecraft.setScreen(new JoinMultiplayerScreen(this)));
        add("HUD Editor", x, y + 48, () -> this.minecraft.setScreen(new HudEditorScreen()));
        add("Options", x, y + 72, () -> this.minecraft.setScreen(new OptionsScreen(this, this.minecraft.options)));
        add("Quit Game", x, y + 104, () -> this.minecraft.stop());
    }

    private void add(String label, int x, int y, Runnable action) {
        this.addRenderableWidget(Button.builder(Component.literal(label), b -> action.run()).bounds(x, y, 200, 20).build());
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        g.fillGradient(0, 0, this.width, this.height, 0xFF0E0F14, 0xFF1C1733);
        g.blit(QuillBadge.LOGO_64, this.width / 2 - 32, this.height / 2 - 104, 64, 64, 0f, 0f, 64, 64, 64, 64);
        g.pose().pushPose();
        g.pose().scale(2f, 2f, 1f);
        g.drawCenteredString(this.font, "Quill Client", this.width / 4, (this.height / 2 - 34) / 2, 0xFFFFFFFF);
        g.pose().popPose();
        super.render(g, mouseX, mouseY, delta);
        g.drawString(this.font, "Minecraft 1.21.1", 6, this.height - 12, 0xFF8A86A8, false);
    }
}
