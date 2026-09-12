package dev.riqvip.tameall.client;

import dev.riqvip.tameall.menu.CompanionMenuSnapshot;
import dev.riqvip.tameall.network.CompanionTargetCatalogPayload;
import dev.riqvip.tameall.network.CompanionTargetSelectionResultPayload;
import net.minecraft.client.Minecraft;

/** Entry point for the crouch-right-click companion controls screen. */
public final class CompanionScreens {
    private static CompanionScreen active;
    private static CompanionTargetScreen targetActive;
    private CompanionScreens() {}

    public static void open(CompanionMenuSnapshot snapshot) {
        CompanionClientState.accept(snapshot);
        active = new CompanionScreen(snapshot);
        targetActive = null;
        Minecraft.getInstance().setScreenAndShow(active);
    }

    public static void openLatest() {
        CompanionClientState.latest().ifPresent(CompanionScreens::open);
    }

    public static void openTargetSelector(CompanionTargetCatalogPayload payload) {
        targetActive = new CompanionTargetScreen(payload);
        Minecraft.getInstance().setScreenAndShow(targetActive);
    }

    public static void targetSelectionResult(CompanionTargetSelectionResultPayload payload) {
        if (targetActive != null) targetActive.acceptResult(payload);
    }

    public static void accept(CompanionMenuSnapshot snapshot) {
        accept(snapshot, false);
    }

    /** Applies a server snapshot, optionally opening the screen for a fresh crouch interaction. */
    public static void accept(CompanionMenuSnapshot snapshot, boolean openScreen) {
        if (openScreen) {
            open(snapshot);
            return;
        }
        CompanionClientState.accept(snapshot);
        if (active != null) active.accept(snapshot);
    }
}
