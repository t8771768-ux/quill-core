package dev.quill.core.analysis;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Version adapter (Fabric 1.21.1): records REAL client-side observations as JSON lines for the launcher's
 * analysis engine. Nothing here computes scores; it only reports what was observed.
 *  - attempt: the player attacked an entity (client-side event; NOT a confirmed hit)
 *  - hit: the target's hurt flash started within 10 ticks of an attempt. The server never says who caused
 *    damage, so attribution is inferred. An attempt with no hurt flash is never reported as a hit.
 *  - sample (20 Hz while a target exists): positions, look direction, crosshair-vs-hitbox geometry
 * Writing happens on a background thread; per-tick work is a few vector operations.
 */
public final class AnalysisCollector {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "quill-analysis-io");
        t.setDaemon(true);
        return t;
    });
    private static volatile BufferedWriter out;
    private static volatile ComboCounter combo = new ComboCounter(2000);

    private record Pending(int entityId, String uuid, String name, int tick) { }

    private static final ArrayDeque<Pending> PENDING = new ArrayDeque<>();
    private static final Map<Integer, Integer> LAST_HURT = new HashMap<>();
    private static int tick;
    private static int lastTargetId = -1;
    private static int lastTargetTick = -10_000;
    private static boolean playerHurtPrev;
    private static boolean wasDead;

    private AnalysisCollector() {
    }

    public static ComboCounter combo() {
        return combo;
    }

    public static void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> startSession());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> endSession("disconnect"));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide && entity instanceof LivingEntity) onAttempt(entity);
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(AnalysisCollector::onTick);
    }

    private static Path dir() {
        return FabricLoader.getInstance().getGameDir().resolve("quill-analysis");
    }

    private static int readTimeout() {
        try {
            Path f = dir().resolve("config.json");
            if (Files.exists(f)) {
                int v = JsonParser.parseString(Files.readString(f)).getAsJsonObject().get("combo_timeout_ms").getAsInt();
                return Math.max(200, Math.min(10_000, v));
            }
        } catch (Exception ignored) {
            // bad config: default
        }
        return 2000;
    }

    private static void startSession() {
        PENDING.clear();
        LAST_HURT.clear();
        lastTargetId = -1;
        playerHurtPrev = false;
        wasDead = false;
        combo = new ComboCounter(readTimeout());
        IO.execute(() -> {
            try {
                closeQuietly();
                Files.createDirectories(dir());
                out = Files.newBufferedWriter(dir().resolve("events.jsonl"), StandardCharsets.UTF_8);   // new file per session: bounded size
            } catch (IOException e) {
                out = null;
            }
        });
        JsonObject o = new JsonObject();
        o.addProperty("mc", "1.21.1");
        o.addProperty("loader", "fabric");
        marker("session_start", o);
    }

    private static void endSession(String reason) {
        combo.interrupt();
        PENDING.clear();
        marker(reason, null);
        IO.execute(AnalysisCollector::closeQuietly);
    }

    private static void closeQuietly() {
        try {
            if (out != null) out.close();
        } catch (IOException ignored) {
            // nothing to do
        }
        out = null;
    }

    /** Writes one event line. Also used by practice mode for its markers. */
    public static void marker(String type, JsonObject extra) {
        JsonObject o = extra == null ? new JsonObject() : extra;
        o.addProperty("type", type);
        o.addProperty("t", System.currentTimeMillis());
        String line = o.toString();
        IO.execute(() -> {
            try {
                if (out != null) {
                    out.write(line);
                    out.write('\n');
                    out.flush();
                }
            } catch (IOException ignored) {
                // disk problem: drop the event, never block the game
            }
        });
    }

    private static void onAttempt(Entity e) {
        int id = e.getId();
        String uuid = e.getStringUUID();
        String name = e.getName().getString();
        PENDING.addLast(new Pending(id, uuid, name, tick));
        while (PENDING.size() > 16) PENDING.pollFirst();
        if (e instanceof LivingEntity le) LAST_HURT.put(id, le.hurtTime);   // baseline so old i-frames are not counted
        lastTargetId = id;
        lastTargetTick = tick;
        JsonObject o = new JsonObject();
        o.addProperty("target", uuid);
        o.addProperty("name", name);
        marker("attempt", o);
    }

    private static double r3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static Entity pickTarget(Minecraft mc, LocalPlayer p) {
        if (lastTargetId != -1 && tick - lastTargetTick <= 100) {
            Entity e = mc.level.getEntity(lastTargetId);
            if (e instanceof LivingEntity le && le.isAlive() && le.distanceTo(p) < 16f) return e;
        }
        Entity c = mc.crosshairPickEntity;
        return (c instanceof LivingEntity le && le.isAlive()) ? c : null;
    }

    private static void onTick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        tick++;
        // 1) confirm hits: target hurt flash starting shortly after one of our attempts
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending pd = it.next();
            Entity e = mc.level.getEntity(pd.entityId());
            if (e instanceof LivingEntity le) {
                int prev = LAST_HURT.getOrDefault(pd.entityId(), 0);
                if (le.hurtTime > 0 && prev == 0 && tick - pd.tick() <= 10) {
                    JsonObject o = new JsonObject();
                    o.addProperty("target", pd.uuid());
                    o.addProperty("name", pd.name());
                    o.addProperty("basis", "hurt_flash");
                    marker("hit", o);
                    combo.hit(pd.uuid(), System.currentTimeMillis());
                    it.remove();
                    continue;
                }
            }
            if (e == null || tick - pd.tick() > 12) it.remove();
        }
        for (Pending pd : PENDING) {
            if (mc.level.getEntity(pd.entityId()) instanceof LivingEntity le) LAST_HURT.put(pd.entityId(), le.hurtTime);
        }
        if (PENDING.isEmpty()) LAST_HURT.clear();
        // 2) player state
        boolean hurt = p.hurtTime > 0;
        if (hurt && !playerHurtPrev) combo.interrupt();
        boolean changed = hurt != playerHurtPrev;
        playerHurtPrev = hurt;
        boolean dead = p.isDeadOrDying();
        if (dead && !wasDead) {
            combo.interrupt();
            marker("death", null);
        } else if (!dead && wasDead) {
            marker("respawn", null);
        }
        wasDead = dead;
        // 3) aim / spacing sample
        Entity target = pickTarget(mc, p);
        if (target == null && !changed && tick % 10 != 0) return;
        Vec3 dm = p.getDeltaMovement();
        JsonObject o = new JsonObject();
        o.addProperty("px", r3(p.getX()));
        o.addProperty("py", r3(p.getY()));
        o.addProperty("pz", r3(p.getZ()));
        o.addProperty("vx", r3(dm.x));
        o.addProperty("vz", r3(dm.z));
        o.addProperty("yaw", r3(p.getYRot()));
        o.addProperty("pitch", r3(p.getXRot()));
        o.addProperty("hurt", hurt ? 1 : 0);
        if (target != null) {
            Vec3 eye = p.getEyePosition();
            Vec3 look = p.getViewVector(1.0F);
            AABB box = target.getBoundingBox();
            Vec3 to = box.getCenter().subtract(eye);
            double len = to.length();
            double cos = len < 1e-6 ? 1.0 : Math.max(-1.0, Math.min(1.0, look.dot(to) / (look.length() * len)));
            boolean on = box.contains(eye) || box.clip(eye, eye.add(look.scale(6.0))).isPresent();
            o.addProperty("tid", target.getStringUUID());
            o.addProperty("tname", target.getName().getString());
            o.addProperty("tx", r3(target.getX()));
            o.addProperty("ty", r3(target.getY()));
            o.addProperty("tz", r3(target.getZ()));
            o.addProperty("err", r3(Math.toDegrees(Math.acos(cos))));
            o.addProperty("on", on);
        }
        marker("sample", o);
    }
}
