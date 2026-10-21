package dev.quill.core.analysis;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;

/**
 * Practice mode. ONLY works in a local singleplayer world (the integrated server): the client cannot spawn
 * entities on someone else's server. Spawns a no-AI Husk (it does not burn in daylight) and moves it by pattern.
 * Patterns map to drill ids defined in the launcher's practice.py.
 */
public final class PracticeMode {
    private static final Map<String, String> DRILL_FOR = Map.of("stationary", "aim_static", "strafe", "track_strafe",
            "combo", "combo_strafe", "circle", "spacing_circle", "random", "reacquire");
    private static final Random RNG = new Random(42);
    private static Husk target;
    private static ServerLevel level;
    private static String pattern;
    private static long ticks;
    private static double bx;
    private static double by;
    private static double bz;
    private static double rx;
    private static double rz;

    private PracticeMode() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(PracticeMode::tick);
    }

    public static String patterns() {
        return String.join(", ", DRILL_FOR.keySet());
    }

    public static void start(MinecraftServer server, String pat, Consumer<String> say) {
        if (!DRILL_FOR.containsKey(pat)) {
            say.accept("Unknown pattern. Use: " + patterns());
            return;
        }
        stop(false, null);
        if (server.getPlayerList().getPlayers().isEmpty()) {
            say.accept("No player found");
            return;
        }
        ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
        level = sp.serverLevel();
        Vec3 look = sp.getLookAngle();
        bx = sp.getX() + look.x * 5;
        by = sp.getY();
        bz = sp.getZ() + look.z * 5;
        rx = bx;
        rz = bz;
        pattern = pat;
        ticks = 0;
        if (!spawn()) {
            say.accept("Could not spawn the practice target");
            return;
        }
        JsonObject m = new JsonObject();
        m.addProperty("drill", DRILL_FOR.get(pat));
        m.addProperty("pattern", pat);
        AnalysisCollector.marker("practice_start", m);
        say.accept("Practice started: " + pat + ". Stop with /quill practice stop");
    }

    private static boolean spawn() {
        Husk h = EntityType.HUSK.create(level);
        if (h == null) return false;
        h.moveTo(rx, by, rz, 0f, 0f);
        h.setNoAi(true);
        h.setSilent(true);
        h.setPersistenceRequired();
        h.setCustomName(Component.literal("Quill Target"));
        h.setCustomNameVisible(true);
        level.addFreshEntity(h);
        target = h;
        return true;
    }

    public static void stop(boolean announce, Consumer<String> say) {
        if (target != null) {
            target.discard();
            target = null;
            if (announce) AnalysisCollector.marker("practice_stop", null);
            if (say != null) say.accept("Practice stopped. Results are in the launcher's Analysis Stats tab.");
        } else if (say != null) {
            say.accept("Practice is not running.");
        }
    }

    private static void tick(MinecraftServer server) {
        if (target == null || level == null) return;
        if (target.isRemoved()) {                    // killed: respawn at the pattern's base so the drill continues
            if (!spawn()) {
                target = null;
                return;
            }
        }
        ticks++;
        double x = rx;
        double z = rz;
        switch (pattern) {
            case "strafe", "combo" -> {
                x = bx + Math.sin(ticks * 0.08) * 3.0;
                z = bz;
            }
            case "circle" -> {
                if (!server.getPlayerList().getPlayers().isEmpty()) {
                    ServerPlayer sp = server.getPlayerList().getPlayers().get(0);
                    x = sp.getX() + Math.cos(ticks * 0.05) * 3.5;
                    z = sp.getZ() + Math.sin(ticks * 0.05) * 3.5;
                }
            }
            case "random" -> {
                if (ticks % 40 == 0) {                // jump to a new spot; the launcher measures how fast you re-aim
                    x = bx + (RNG.nextDouble() - 0.5) * 8.0;
                    z = bz + (RNG.nextDouble() - 0.5) * 8.0;
                    AnalysisCollector.marker("target_shift", null);
                }
            }
            default -> { }
        }
        if (x != rx || z != rz) {
            rx = x;
            rz = z;
            target.moveTo(rx, by, rz, target.getYRot(), 0f);
        }
    }
}
