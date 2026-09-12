package dev.riqvip.tameall.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

/** Manual recall request from the owner-scoped Companion Whistle roster. */
public record CompanionRosterActionPayload(UUID bondId, Action action) implements CustomPacketPayload {
    public static final Type<CompanionRosterActionPayload> ID = new Type<>(NetworkIds.ROSTER_ACTION);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionRosterActionPayload> CODEC =
            CustomPacketPayload.codec(CompanionRosterActionPayload::write, CompanionRosterActionPayload::read);

    public CompanionRosterActionPayload {
        if (bondId == null) throw new IllegalArgumentException("missing bond id");
        if (action == null) throw new IllegalArgumentException("missing roster action");
    }

    public CompanionRosterActionPayload(UUID bondId) { this(bondId, Action.RECALL); }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    private static void write(CompanionRosterActionPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(payload.bondId());
        buf.writeUtf(payload.action().name(), 16);
    }

    private static CompanionRosterActionPayload read(RegistryFriendlyByteBuf buf) {
        UUID bondId = buf.readUUID();
        try {
            return new CompanionRosterActionPayload(bondId, Action.valueOf(buf.readUtf(16)));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown roster action", exception);
        }
    }

    public enum Action { RECALL, REMOVE }
}
