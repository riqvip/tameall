package dev.riqvip.tameall.companion;

/** A serialized block coordinate used by companion anchors and journal entries. */
public record BlockPoint(int x, int y, int z) {
    public long squaredDistanceTo(BlockPoint other) {
        long dx = (long) x - other.x;
        long dy = (long) y - other.y;
        long dz = (long) z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public BlockPoint min(BlockPoint other) {
        return new BlockPoint(Math.min(x, other.x), Math.min(y, other.y), Math.min(z, other.z));
    }

    public BlockPoint max(BlockPoint other) {
        return new BlockPoint(Math.max(x, other.x), Math.max(y, other.y), Math.max(z, other.z));
    }
}
