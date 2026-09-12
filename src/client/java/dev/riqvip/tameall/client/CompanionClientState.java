package dev.riqvip.tameall.client;

import dev.riqvip.tameall.menu.CompanionMenuSnapshot;
import java.util.Optional;

/** Client cache; it is replaced only by authoritative server snapshots. */
public final class CompanionClientState {
    private static CompanionMenuSnapshot latest;

    private CompanionClientState() {}

    public static void accept(CompanionMenuSnapshot snapshot) {
        if (latest == null || !snapshot.bondId().equals(latest.bondId())
                || snapshot.revision() >= latest.revision()) latest = snapshot;
    }

    public static Optional<CompanionMenuSnapshot> latest() { return Optional.ofNullable(latest); }
    public static void clear() { latest = null; }
}
