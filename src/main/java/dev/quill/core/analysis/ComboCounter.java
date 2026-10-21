package dev.quill.core.analysis;

/**
 * Live combo counter. No Minecraft imports, so it is unit-testable without the game.
 * Rules (same as the launcher's analysis engine): a confirmed hit on the SAME target within timeoutMs of the
 * previous hit extends the combo; a different target, a timeout, or an interrupt (taking damage, death,
 * world change) ends it.
 */
public final class ComboCounter {
    private final long timeoutMs;
    private String target;
    private int count;
    private int best;
    private long lastHitMs;

    public ComboCounter(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public void hit(String targetId, long nowMs) {
        expire(nowMs);
        if (target != null && target.equals(targetId)) {
            count++;
        } else {
            target = targetId;
            count = 1;
        }
        lastHitMs = nowMs;
        best = Math.max(best, count);
    }

    public void expire(long nowMs) {
        if (target != null && nowMs - lastHitMs > timeoutMs) reset();
    }

    public void interrupt() {
        reset();
    }

    private void reset() {
        target = null;
        count = 0;
    }

    public int count(long nowMs) {
        expire(nowMs);
        return count;
    }

    public int best() {
        return best;
    }
}
