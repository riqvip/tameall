package dev.riqvip.tameall.companion;

/** Persistent ready tick for a safe, reusable companion creeper blast. */
public record CreeperCooldown(long readyAt) {
    public CreeperCooldown {
        if (readyAt < 0L) throw new IllegalArgumentException("readyAt");
    }
}
