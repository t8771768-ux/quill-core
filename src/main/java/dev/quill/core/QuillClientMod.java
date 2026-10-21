package dev.quill.core;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.quill.core.analysis.AnalysisCollector;
import dev.quill.core.analysis.PracticeMode;
import dev.quill.core.hud.HudEditorScreen;
import dev.quill.core.hud.HudManager;
import dev.quill.core.ui.QuillBadge;
import dev.quill.core.ui.QuillTitleScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.lwjgl.glfw.GLFW;

public class QuillClientMod implements ClientModInitializer {
    public static KeyMapping editorKey;

    @Override
    public void onInitializeClient() {
        HudManager.load();            // every HUD starts OFF; turn them on in the editor
        AnalysisCollector.init();
        PracticeMode.init();
        editorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.quill.hud_editor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.quill"));
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (editorKey.consumeClick()) {
                mc.setScreen(new HudEditorScreen());
            }
            HudManager.tick(mc);
        });
        HudRenderCallback.EVENT.register((graphics, delta) -> HudManager.render(graphics, Minecraft.getInstance()));

        // Quill main menu instead of the vanilla title screen
        ScreenEvents.BEFORE_INIT.register((client, screen, w, h) -> {
            if (screen instanceof TitleScreen) {
                client.execute(() -> {
                    if (client.screen instanceof TitleScreen) client.setScreen(new QuillTitleScreen());
                });
            }
        });
        // Quill logo + text in the pause menu, inventory and chests
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof PauseScreen || screen instanceof AbstractContainerScreen<?>) {
                ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> QuillBadge.draw(g, client.font, s.width, s.height));
            }
        });

        // /quill practice start <stationary|strafe|combo|circle|random>   /quill practice stop
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("quill")
                        .then(ClientCommandManager.literal("practice")
                                .then(ClientCommandManager.literal("start")
                                        .then(ClientCommandManager.argument("pattern", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    Minecraft mc = Minecraft.getInstance();
                                                    MinecraftServer server = mc.getSingleplayerServer();
                                                    String pat = StringArgumentType.getString(ctx, "pattern");
                                                    if (server == null) {
                                                        ctx.getSource().sendFeedback(Component.literal(
                                                                "Practice mode needs a local singleplayer world."));
                                                        return 0;
                                                    }
                                                    server.execute(() -> PracticeMode.start(server, pat, msg -> mc.execute(() -> {
                                                        if (mc.player != null) mc.player.displayClientMessage(Component.literal(msg), false);
                                                    })));
                                                    return 1;
                                                })))
                                .then(ClientCommandManager.literal("stop")
                                        .executes(ctx -> {
                                            Minecraft mc = Minecraft.getInstance();
                                            MinecraftServer server = mc.getSingleplayerServer();
                                            if (server == null) return 0;
                                            server.execute(() -> PracticeMode.stop(true, msg -> mc.execute(() -> {
                                                if (mc.player != null) mc.player.displayClientMessage(Component.literal(msg), false);
                                            })));
                                            return 1;
                                        })))));
    }
}
