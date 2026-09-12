package dev.riqvip.tameall.network;

import dev.riqvip.tameall.companion.CombatStance;
import dev.riqvip.tameall.companion.CompanionMode;
import dev.riqvip.tameall.companion.HealthDisplayMode;
import dev.riqvip.tameall.companion.NameplateMode;
import dev.riqvip.tameall.companion.TargetFilter;
import dev.riqvip.tameall.companion.TeleportMode;
import dev.riqvip.tameall.menu.CompanionAction;
import dev.riqvip.tameall.menu.CompanionActionRequest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

/** Wire representation of a typed, revisioned menu request. */
public record CompanionActionPayload(UUID bondId, long expectedRevision, CompanionAction action,
                                     String text, int number, boolean value,
                                     CompanionMode mode, CombatStance stance, NameplateMode nameplate,
                                     HealthDisplayMode healthDisplay, TargetFilter targetFilter,
                                     TeleportMode teleportMode, double decimal) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CompanionActionPayload> ID =
            new CustomPacketPayload.Type<>(NetworkIds.ACTION);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionActionPayload> CODEC =
            CustomPacketPayload.codec(CompanionActionPayload::write, CompanionActionPayload::read);

    public CompanionActionPayload(CompanionActionRequest request) {
        this(request.bondId(), request.expectedRevision(), request.action(), request.text(), request.number(),
                request.value(), request.mode(), request.stance(), request.nameplate(), request.healthDisplay(),
                request.targetFilter(), request.teleportMode(), request.decimal());
    }

    public CompanionActionRequest request() {
        return new CompanionActionRequest(bondId, expectedRevision, action, text, number, value,
                mode, stance, nameplate, healthDisplay, targetFilter, teleportMode, decimal);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    private static void write(CompanionActionPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(payload.bondId);
        buf.writeVarLong(payload.expectedRevision);
        buf.writeUtf(payload.action.name(), 64);
        writeNullableString(buf, payload.text);
        buf.writeVarInt(payload.number);
        buf.writeBoolean(payload.value);
        writeEnum(buf, payload.mode); writeEnum(buf, payload.stance);
        writeEnum(buf, payload.nameplate); writeEnum(buf, payload.healthDisplay);
        writeEnum(buf, payload.targetFilter);
        writeEnum(buf, payload.teleportMode);
        buf.writeDouble(payload.decimal);
    }

    private static CompanionActionPayload read(RegistryFriendlyByteBuf buf) {
        UUID bond = buf.readUUID();
        long revision = buf.readVarLong();
        CompanionAction action = enumValue(CompanionAction.class, buf.readUtf(64));
        String text = readNullableString(buf);
        int number = buf.readVarInt();
        boolean value = buf.readBoolean();
        CompanionMode mode = readEnum(buf, CompanionMode.class);
        CombatStance stance = readEnum(buf, CombatStance.class);
        NameplateMode nameplate = readEnum(buf, NameplateMode.class);
        HealthDisplayMode health = readEnum(buf, HealthDisplayMode.class);
        TargetFilter filter = readEnum(buf, TargetFilter.class);
        TeleportMode teleport = readEnum(buf, TeleportMode.class);
        double decimal = buf.readDouble();
        return new CompanionActionPayload(bond, revision, action, text, number, value,
                mode, stance, nameplate, health, filter, teleport, decimal);
    }

    private static void writeNullableString(RegistryFriendlyByteBuf buf, String value) {
        buf.writeBoolean(value != null);
        if (value != null) buf.writeUtf(value, 256);
    }

    private static String readNullableString(RegistryFriendlyByteBuf buf) {
        return buf.readBoolean() ? buf.readUtf(256) : null;
    }

    private static <E extends Enum<E>> void writeEnum(RegistryFriendlyByteBuf buf, E value) {
        buf.writeBoolean(value != null);
        if (value != null) buf.writeUtf(value.name(), 64);
    }

    private static <E extends Enum<E>> E readEnum(RegistryFriendlyByteBuf buf, Class<E> type) {
        return buf.readBoolean() ? enumValue(type, buf.readUtf(64)) : null;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("unknown enum value", ex); }
    }
}
