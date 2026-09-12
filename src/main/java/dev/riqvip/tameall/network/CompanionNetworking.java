package dev.riqvip.tameall.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** Registers the packet types and leaves all game-state mutation to the server callback. */
public final class CompanionNetworking {
    private CompanionNetworking() {}

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(CompanionActionPayload.ID, CompanionActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CompanionRosterActionPayload.ID, CompanionRosterActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CompanionTargetSelectionPayload.ID, CompanionTargetSelectionPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CompanionSnapshotPayload.ID, CompanionSnapshotPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CompanionRosterPayload.ID, CompanionRosterPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CompanionTargetCatalogPayload.ID, CompanionTargetCatalogPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CompanionTargetSelectionResultPayload.ID, CompanionTargetSelectionResultPayload.CODEC);
    }

    public static void registerServer(CompanionServerHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(CompanionActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handler.handle(context.player(), payload)));
    }

    public static void registerRosterActionServer(RosterActionServerHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(CompanionRosterActionPayload.ID,
                (payload, context) -> context.server().execute(() -> handler.handle(context.player(), payload)));
    }

    public static void registerTargetSelectionServer(TargetSelectionServerHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(CompanionTargetSelectionPayload.ID,
                (payload, context) -> context.server().execute(() -> handler.handle(context.player(), payload)));
    }

    @FunctionalInterface
    public interface CompanionServerHandler {
        void handle(ServerPlayer player, CompanionActionPayload payload);
    }

    @FunctionalInterface
    public interface RosterActionServerHandler {
        void handle(ServerPlayer player, CompanionRosterActionPayload payload);
    }

    @FunctionalInterface
    public interface TargetSelectionServerHandler {
        void handle(ServerPlayer player, CompanionTargetSelectionPayload payload);
    }
}
