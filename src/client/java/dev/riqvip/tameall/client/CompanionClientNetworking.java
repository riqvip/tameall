package dev.riqvip.tameall.client;

import dev.riqvip.tameall.network.CompanionSnapshotPayload;
import dev.riqvip.tameall.network.CompanionRosterPayload;
import dev.riqvip.tameall.network.CompanionTargetCatalogPayload;
import dev.riqvip.tameall.network.CompanionTargetSelectionResultPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Client-only receiver for authoritative companion menu snapshots. */
public final class CompanionClientNetworking {
    private CompanionClientNetworking() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(CompanionSnapshotPayload.ID,
                (payload, context) -> context.client().execute(() ->
                        CompanionScreens.accept(payload.snapshot(), payload.openScreen())));
        ClientPlayNetworking.registerGlobalReceiver(CompanionRosterPayload.ID,
                (payload, context) -> context.client().execute(() -> CompanionClientRoster.accept(payload.entries())));
        ClientPlayNetworking.registerGlobalReceiver(CompanionTargetCatalogPayload.ID,
                (payload, context) -> context.client().execute(() -> CompanionScreens.openTargetSelector(payload)));
        ClientPlayNetworking.registerGlobalReceiver(CompanionTargetSelectionResultPayload.ID,
                (payload, context) -> context.client().execute(() -> CompanionScreens.targetSelectionResult(payload)));
    }
}
