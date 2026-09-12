package dev.riqvip.tameall.companion;

import java.util.UUID;

/** The owner whose hit caused a golem to become hostile to that owner's pets. */
public record GolemProvocation(UUID ownerId, long expiresAt) {
    public GolemProvocation {
        if (ownerId == null) throw new NullPointerException("ownerId");
        if (expiresAt < 0L) throw new IllegalArgumentException("expiresAt");
    }

    public boolean activeAt(long gameTime) { return gameTime < expiresAt; }
}
