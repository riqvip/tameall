package dev.riqvip.tameall.network;

import dev.riqvip.tameall.companion.TargetSelection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Untrusted selector result; the server validates ownership, revision, and ids. */
public record CompanionTargetSelectionPayload(UUID bondId, long expectedRevision,
                                               TargetSelection selection) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CompanionTargetSelectionPayload> ID =
            new CustomPacketPayload.Type<>(NetworkIds.TARGET_SELECTION);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionTargetSelectionPayload> CODEC =
            CustomPacketPayload.codec(CompanionTargetSelectionPayload::write, CompanionTargetSelectionPayload::read);

    public CompanionTargetSelectionPayload {
        if (bondId == null || expectedRevision < 0 || selection == null) throw new IllegalArgumentException("invalid target selection");
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    private static void write(CompanionTargetSelectionPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(payload.bondId());
        buf.writeVarLong(payload.expectedRevision());
        CompanionTargetCatalogPayload.writeSelection(buf, payload.selection());
    }

    private static CompanionTargetSelectionPayload read(RegistryFriendlyByteBuf buf) {
        return new CompanionTargetSelectionPayload(buf.readUUID(), buf.readVarLong(),
                CompanionTargetCatalogPayload.readSelection(buf));
    }
}
