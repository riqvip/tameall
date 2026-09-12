package dev.riqvip.tameall.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/** Server acknowledgement for a target draft save. */
public record CompanionTargetSelectionResultPayload(UUID bondId, long revision,
                                                    boolean success, String message)
        implements CustomPacketPayload {
    public static final Type<CompanionTargetSelectionResultPayload> ID =
            new Type<>(NetworkIds.TARGET_SELECTION_RESULT);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionTargetSelectionResultPayload> CODEC =
            CustomPacketPayload.codec(CompanionTargetSelectionResultPayload::write,
                    CompanionTargetSelectionResultPayload::read);

    public CompanionTargetSelectionResultPayload {
        if (bondId == null || revision < 0 || message == null || message.length() > 256) {
            throw new IllegalArgumentException("invalid target result");
        }
    }

    private static void write(CompanionTargetSelectionResultPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(payload.bondId());
        buf.writeVarLong(payload.revision());
        buf.writeBoolean(payload.success());
        buf.writeUtf(payload.message(), 256);
    }

    private static CompanionTargetSelectionResultPayload read(RegistryFriendlyByteBuf buf) {
        return new CompanionTargetSelectionResultPayload(buf.readUUID(), buf.readVarLong(),
                buf.readBoolean(), buf.readUtf(256));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }
}
