package dev.riqvip.tameall.companion;

/** Native Vex expiry state captured before expiry protection temporarily pauses it. */
public record VexLifetime(boolean hadLimitedLife, int remainingTicks) {
    public VexLifetime {
        if (remainingTicks < 0) throw new IllegalArgumentException("negative vex lifetime");
    }
}
