package dev.quill.core.hud;

import dev.quill.core.analysis.AnalysisCollector;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

/** All built-in HUD elements. One class per element, all drawn and edited by the same manager. */
public final class Modules {
    private Modules() {
    }

    public static List<HudModule> createAll() {
        List<HudModule> list = new ArrayList<>();
        list.add(new Keystrokes(6, 6));
        list.add(new Cps(6, 84));
        list.add(new Fps(6, 100));
        list.add(new Ping(6, 116));
        list.add(new Coords(6, 132));
        list.add(new Potions(6, 168));
        list.add(new Armor(200, 6));
        list.add(new Status(200, 110));
        list.add(new Combo(6, 196));
        return list;
    }

    /** Small translucent box with lines of text. */
    abstract static class TextModule extends HudModule {
        TextModule(String id, String title, int x, int y) {
            super(id, title, x, y);
        }

        abstract List<String> lines(Minecraft mc);

        private List<String> safeLines(Minecraft mc) {
            List<String> l = mc.player == null ? List.of(title) : lines(mc);
            return l.isEmpty() ? List.of(title) : l;
        }

        @Override
        public int width(Minecraft mc) {
            int w = 40;
            for (String s : safeLines(mc)) w = Math.max(w, mc.font.width(s) + 8);
            return w;
        }

        @Override
        public int height(Minecraft mc) {
            return safeLines(mc).size() * 10 + 4;
        }

        @Override
        public void render(GuiGraphics g, Minecraft mc, int x, int y) {
            g.fill(x, y, x + width(mc), y + height(mc), 0x66000000);
            int ty = y + 3;
            for (String s : safeLines(mc)) {
                g.drawString(mc.font, s, x + 4, ty, 0xFFFFFFFF, true);
                ty += 10;
            }
        }
    }

    static final class Fps extends TextModule {
        Fps(int x, int y) {
            super("fps", "FPS", x, y);
        }

        @Override
        List<String> lines(Minecraft mc) {
            return List.of("FPS: " + HudManager.fps());
        }
    }

    static final class Ping extends TextModule {
        Ping(int x, int y) {
            super("ping", "Ping", x, y);
        }

        @Override
        List<String> lines(Minecraft mc) {
            ClientPacketListener conn = mc.getConnection();
            LocalPlayer p = mc.player;
            if (conn == null || p == null) return List.of("Ping: -");
            PlayerInfo info = conn.getPlayerInfo(p.getUUID());
            return List.of("Ping: " + (info == null ? "-" : info.getLatency() + " ms"));
        }
    }

    static final class Cps extends TextModule {
        Cps(int x, int y) {
            super("cps", "CPS", x, y);
        }

        @Override
        List<String> lines(Minecraft mc) {
            return List.of("CPS: " + HudManager.CLICKS.left() + " | " + HudManager.CLICKS.right());
        }
    }

    static final class Coords extends TextModule {
        Coords(int x, int y) {
            super("coords", "Coordinates", x, y);
        }

        @Override
        List<String> lines(Minecraft mc) {
            LocalPlayer p = mc.player;
            String biome = mc.level == null ? "-" : mc.level.getBiome(p.blockPosition()).unwrapKey()
                    .map(k -> k.location().getPath()).orElse("unknown");
            return List.of(
                    String.format("XYZ: %.1f / %.1f / %.1f", p.getX(), p.getY(), p.getZ()),
                    "Facing: " + p.getDirection().getName(),
                    "Biome: " + biome);
        }
    }

    static final class Combo extends TextModule {
        Combo(int x, int y) {
            super("combo", "Combo Counter", x, y);
        }

        @Override
        List<String> lines(Minecraft mc) {
            long now = System.currentTimeMillis();
            return List.of("Combo: " + AnalysisCollector.combo().count(now) + "   Best: " + AnalysisCollector.combo().best());
        }
    }

    static final class Status extends TextModule {
        Status(int x, int y) {
            super("status", "Sprint/Sneak", x, y);
        }

        @Override
        List<String> lines(Minecraft mc) {
            LocalPlayer p = mc.player;
            return List.of(p.isSprinting() ? "Sprinting" : p.isShiftKeyDown() ? "Sneaking" : "Walking");
        }
    }

    static final class Potions extends TextModule {
        Potions(int x, int y) {
            super("potions", "Potion Status", x, y);
        }

        @Override
        List<String> lines(Minecraft mc) {
            List<String> out = new ArrayList<>();
            for (MobEffectInstance e : mc.player.getActiveEffects()) {
                String name = e.getEffect().value().getDisplayName().getString();
                String time;
                if (e.isInfiniteDuration()) {
                    time = "inf";
                } else {
                    int secs = e.getDuration() / 20;
                    time = (secs / 60) + ":" + String.format("%02d", secs % 60);
                }
                out.add(name + " " + (e.getAmplifier() + 1) + "  " + time);
            }
            return out;
        }
    }

    static final class Armor extends HudModule {
        Armor(int x, int y) {
            super("armor", "Armor Status", x, y);
        }

        @Override
        public int width(Minecraft mc) {
            return 70;
        }

        @Override
        public int height(Minecraft mc) {
            return 5 * 18 + 4;
        }

        @Override
        public void render(GuiGraphics g, Minecraft mc, int x, int y) {
            LocalPlayer p = mc.player;
            if (p == null) return;
            g.fill(x, y, x + width(mc), y + height(mc), 0x66000000);
            ItemStack[] stacks = {
                    p.getInventory().armor.get(3), p.getInventory().armor.get(2),
                    p.getInventory().armor.get(1), p.getInventory().armor.get(0), p.getMainHandItem()};
            int yy = y + 2;
            for (ItemStack s : stacks) {
                if (!s.isEmpty()) {
                    g.renderItem(s, x + 3, yy);
                    g.renderItemDecorations(mc.font, s, x + 3, yy);
                    if (s.isDamageableItem()) {
                        int left = s.getMaxDamage() - s.getDamageValue();
                        g.drawString(mc.font, String.valueOf(left), x + 24, yy + 4, 0xFFFFFFFF, true);
                    }
                }
                yy += 18;
            }
        }
    }

    static final class Keystrokes extends HudModule {
        Keystrokes(int x, int y) {
            super("keystrokes", "Keystrokes", x, y);
        }

        @Override
        public int width(Minecraft mc) {
            return 70;
        }

        @Override
        public int height(Minecraft mc) {
            return 70;
        }

        private void key(GuiGraphics g, Minecraft mc, int x, int y, int w, String label, boolean down) {
            g.fill(x, y, x + w, y + 22, down ? 0xAAFFFFFF : 0x66000000);
            g.drawCenteredString(mc.font, label, x + w / 2, y + 7, down ? 0xFF000000 : 0xFFFFFFFF);
        }

        @Override
        public void render(GuiGraphics g, Minecraft mc, int x, int y) {
            var o = mc.options;
            key(g, mc, x + 24, y, 22, "W", o.keyUp.isDown());
            key(g, mc, x, y + 24, 22, "A", o.keyLeft.isDown());
            key(g, mc, x + 24, y + 24, 22, "S", o.keyDown.isDown());
            key(g, mc, x + 48, y + 24, 22, "D", o.keyRight.isDown());
            key(g, mc, x, y + 48, 34, "LMB " + HudManager.CLICKS.left(), mc.mouseHandler.isLeftPressed());
            key(g, mc, x + 36, y + 48, 34, "RMB " + HudManager.CLICKS.right(), mc.mouseHandler.isRightPressed());
        }
    }
}
