package dev.riqvip.tameall.network;

import net.minecraft.resources.Identifier;

/** Packet identifiers are kept in one place so client/server registration cannot drift. */
public final class NetworkIds {
    public static final Identifier ACTION = Identifier.fromNamespaceAndPath("tameall", "companion_action");
    public static final Identifier SNAPSHOT = Identifier.fromNamespaceAndPath("tameall", "companion_snapshot");
    public static final Identifier ROSTER = Identifier.fromNamespaceAndPath("tameall", "companion_roster");
    public static final Identifier ROSTER_ACTION = Identifier.fromNamespaceAndPath("tameall", "companion_roster_action");
    public static final Identifier TARGET_CATALOG = Identifier.fromNamespaceAndPath("tameall", "target_catalog");
    public static final Identifier TARGET_SELECTION = Identifier.fromNamespaceAndPath("tameall", "target_selection");
    public static final Identifier TARGET_SELECTION_RESULT = Identifier.fromNamespaceAndPath("tameall", "target_selection_result");

    private NetworkIds() {}
}
