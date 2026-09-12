package dev.riqvip.tameall.companion;

/** Constants and cheap geometric checks shared by follow/recall implementations. */
public final class TeleportPolicy {
    public static final double FOLLOW_DISTANCE_SQUARED = 16.0 * 16.0;
    public static final double FOLLOW_RECALL_DISTANCE_SQUARED = 32.0 * 32.0;
    public static final double FAR_DISTANCE_SQUARED = 32.0 * 32.0;
    public static final int SEARCH_RADIUS = 3;

    private TeleportPolicy() {}

    public static boolean shouldCatchUp(double distanceSquared) {
        return distanceSquared >= FOLLOW_DISTANCE_SQUARED;
    }

    public static boolean shouldRecall(double distanceSquared) {
        return distanceSquared >= FOLLOW_RECALL_DISTANCE_SQUARED;
    }
}
