package dev.quill.core.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class HudManager {
    private static final List<HudModule> MODULES = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Clicks CLICKS = new Clicks();
    private static int frames;
    private static int fps;
    private static long lastFpsTime = System.currentTimeMillis();

    private HudManager() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("quill-hud.json");
    }

    public static List<HudModule> modules() {
        return MODULES;
    }

    public static int fps() {
        return fps;
    }

    public static void load() {
        MODULES.clear();
        MODULES.addAll(Modules.createAll());
        try {
            Path f = file();
            if (Files.exists(f)) {
                Map<String, Saved> saved = GSON.fromJson(Files.readString(f), new TypeToken<Map<String, Saved>>() { }.getType());
                if (saved != null) {
                    for (HudModule m : MODULES) {
                        Saved s = saved.get(m.id);
                        if (s != null) {
                            m.enabled = s.enabled;
                            m.x = s.x;
                            m.y = s.y;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // bad config: keep defaults
        }
    }

    public static void save() {
        Map<String, Saved> out = new LinkedHashMap<>();
        for (HudModule m : MODULES) {
            out.put(m.id, new Saved(m.enabled, m.x, m.y));
        }
        try {
            Files.writeString(file(), GSON.toJson(out));
        } catch (IOException ignored) {
            // cannot write config: ignore
        }
    }

    public static void tick(Minecraft mc) {
        for (HudModule m : MODULES) {
            if (m.enabled) m.tick(mc);
        }
    }

    public static void render(GuiGraphics g, Minecraft mc) {
        long now = System.currentTimeMillis();
        frames++;
        if (now - lastFpsTime >= 1000L) {
            fps = frames;
            frames = 0;
            lastFpsTime = now;
        }
        CLICKS.update(mc);
        if (mc.player == null || mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
        for (HudModule m : MODULES) {
            if (m.enabled) m.render(g, mc, m.x, m.y);
        }
    }

    private static final class Saved {
        boolean enabled;
        int x;
        int y;

        Saved(boolean enabled, int x, int y) {
            this.enabled = enabled;
            this.x = x;
            this.y = y;
        }
    }
}
