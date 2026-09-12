package dev.riqvip.tameall.client;

import dev.riqvip.tameall.network.CompanionRosterPayload;
import net.minecraft.client.Minecraft;
import java.util.List;

/** Keeps the whistle roster synchronized with server-owned bond records. */
public final class CompanionClientRoster {
    private static CompanionRosterScreen active;

    private CompanionClientRoster() {}

    public static void open(List<CompanionRosterPayload.Entry> entries) {
        active = new CompanionRosterScreen(entries);
        Minecraft.getInstance().setScreenAndShow(active);
    }

    public static void accept(List<CompanionRosterPayload.Entry> entries) {
        if (active != null) active.accept(entries);
        else open(entries);
    }

    static void closed(CompanionRosterScreen screen) {
        if (active == screen) active = null;
    }
}
