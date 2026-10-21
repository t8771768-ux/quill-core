package dev.quill.core.hud;

import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;

/** Counts mouse clicks per second, sampled every rendered frame so fast clicking is not missed. */
public final class Clicks {
    private final ArrayDeque<Long> left = new ArrayDeque<>();
    private final ArrayDeque<Long> right = new ArrayDeque<>();
    private boolean lastLeft;
    private boolean lastRight;

    public void update(Minecraft mc) {
        long now = System.currentTimeMillis();
        boolean l = mc.mouseHandler.isLeftPressed();
        boolean r = mc.mouseHandler.isRightPressed();
        if (l && !lastLeft) left.addLast(now);
        if (r && !lastRight) right.addLast(now);
        lastLeft = l;
        lastRight = r;
        while (!left.isEmpty() && now - left.peekFirst() > 1000L) left.pollFirst();
        while (!right.isEmpty() && now - right.peekFirst() > 1000L) right.pollFirst();
    }

    public int left() {
        return left.size();
    }

    public int right() {
        return right.size();
    }
}
