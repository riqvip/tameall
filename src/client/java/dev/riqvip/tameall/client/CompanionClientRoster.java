package dev.riqvip.tameall.client;

import dev.riqvip.tameall.network.CompanionRosterPayload;
import net.minecraft.client.Minecraft;
import java.util.List;

/** Keeps the whistle roster synchronized with server-owned bond records. */
public final class CompanionClientRoster {
    private static CompanionRosterScreen active;
    private static List<CompanionRosterPayload.Entry> lastEntries = List.of();

    private CompanionClientRoster() {}

    public static void open(List<CompanionRosterPayload.Entry> entries) {
        lastEntries = entries == null ? List.of() : List.copyOf(entries);
        active = new CompanionRosterScreen(entries);
        Minecraft.getInstance().setScreenAndShow(active);
    }

    public static void accept(List<CompanionRosterPayload.Entry> entries) {
        accept(new CompanionRosterPayload(entries));
    }

    public static void accept(CompanionRosterPayload payload) {
        boolean displayed = active != null && Minecraft.getInstance().gui.screen() == active;
        if (displayed) {
            lastEntries = payload.entries();
            active.accept(payload.entries());
        } else if (payload.openScreen()) {
            open(payload.entries());
        }
    }

    static void closed(CompanionRosterScreen screen) {
        if (active == screen) active = null;
    }

    static void reopen() { open(lastEntries); }

    static void clear() { active = null; lastEntries = List.of(); }
}
