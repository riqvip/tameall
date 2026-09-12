package dev.riqvip.tameall.network;

import dev.riqvip.tameall.companion.CombatStance;
import dev.riqvip.tameall.companion.CompanionMode;
import dev.riqvip.tameall.companion.CompanionSettings;
import dev.riqvip.tameall.companion.CompanionCapabilities;
import dev.riqvip.tameall.companion.HealthDisplayMode;
import dev.riqvip.tameall.companion.MagicToggles;
import dev.riqvip.tameall.companion.NameplateMode;
import dev.riqvip.tameall.companion.TargetFilter;
import dev.riqvip.tameall.companion.TeleportMode;
import dev.riqvip.tameall.menu.CompanionMenuSnapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;
import java.util.UUID;

/** Authoritative GUI state pushed after a menu action is accepted. */
public record CompanionSnapshotPayload(CompanionMenuSnapshot snapshot, boolean openScreen) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CompanionSnapshotPayload> ID =
            new CustomPacketPayload.Type<>(NetworkIds.SNAPSHOT);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionSnapshotPayload> CODEC =
            CustomPacketPayload.codec(CompanionSnapshotPayload::write, CompanionSnapshotPayload::read);

    public CompanionSnapshotPayload(CompanionMenuSnapshot snapshot) { this(snapshot, false); }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    private static void write(CompanionSnapshotPayload payload, RegistryFriendlyByteBuf buf) {
        CompanionMenuSnapshot s = payload.snapshot;
        buf.writeBoolean(payload.openScreen);
        buf.writeUUID(s.bondId());
        buf.writeBoolean(s.entityId() != null);
        if (s.entityId() != null) buf.writeUUID(s.entityId());
        buf.writeUtf(s.creatureType(), 256);
        writeNullable(buf, s.displayName(), 64);
        buf.writeFloat(s.health()); buf.writeFloat(s.maxHealth());
        writeSettings(buf, s.settings());
        buf.writeBoolean(s.dead()); buf.writeVarLong(s.revision());
        buf.writeBoolean(s.cheatsAllowed());
        writeCapabilities(buf, s.capabilities());
        buf.writeCollection(s.inventorySummary(), (b, item) -> b.writeUtf(item, 256));
    }

    private static CompanionSnapshotPayload read(RegistryFriendlyByteBuf buf) {
        boolean openScreen = buf.readBoolean();
        UUID bond = buf.readUUID();
        UUID entity = buf.readBoolean() ? buf.readUUID() : null;
        String type = buf.readUtf(256);
        String name = readNullable(buf, 64);
        float health = buf.readFloat(), max = buf.readFloat();
        CompanionSettings settings = readSettings(buf);
        boolean dead = buf.readBoolean();
        long revision = buf.readVarLong();
        boolean cheatsAllowed = buf.readBoolean();
        CompanionCapabilities capabilities = readCapabilities(buf);
        List<String> inventory = buf.readList(b -> b.readUtf(256));
        return new CompanionSnapshotPayload(new CompanionMenuSnapshot(bond, entity, type, name, health, max,
                settings, inventory, dead, revision, cheatsAllowed, capabilities), openScreen);
    }

    private static void writeSettings(RegistryFriendlyByteBuf buf, CompanionSettings s) {
        buf.writeUtf(s.mode().name(), 64); buf.writeUtf(s.stance().name(), 64);
        buf.writeBoolean(s.magic().sunlightProtection()); buf.writeBoolean(s.magic().drowningProtection());
        buf.writeBoolean(s.magic().reusableExplosions()); buf.writeBoolean(s.magic().preventVexExpiry());
        buf.writeBoolean(s.magic().allowEndermanBlockPickup());
        buf.writeUtf(s.nameplate().name(), 64); buf.writeUtf(s.healthDisplay().name(), 64);
        buf.writeVarInt(s.guardRadius()); buf.writeUtf(s.targetFilter().name(), 64);
        buf.writeUtf(s.teleportMode().name(), 64);
        buf.writeBoolean(s.pickupItems()); buf.writeDouble(s.pickupRadius()); buf.writeBoolean(s.collectXpForMending());
        buf.writeDouble(s.xpRadius());
        buf.writeBoolean(s.useDurability());
    }

    private static CompanionSettings readSettings(RegistryFriendlyByteBuf buf) {
        CompanionMode mode = enumValue(CompanionMode.class, buf.readUtf(64));
        CombatStance stance = enumValue(CombatStance.class, buf.readUtf(64));
        MagicToggles magic = new MagicToggles(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean());
        NameplateMode nameplate = enumValue(NameplateMode.class, buf.readUtf(64));
        HealthDisplayMode health = enumValue(HealthDisplayMode.class, buf.readUtf(64));
        return new CompanionSettings(mode, stance, magic, nameplate, health, buf.readVarInt(),
                enumValue(TargetFilter.class, buf.readUtf(64)),
                enumValue(TeleportMode.class, buf.readUtf(64)), buf.readBoolean(), buf.readDouble(),
                buf.readBoolean(), buf.readDouble(), buf.readBoolean());
    }

    private static void writeCapabilities(RegistryFriendlyByteBuf buf, CompanionCapabilities c) {
        buf.writeBoolean(c.sunlightProtection());
        buf.writeBoolean(c.drowningProtection());
        buf.writeBoolean(c.reusableExplosions());
        buf.writeBoolean(c.preventVexExpiry());
        buf.writeBoolean(c.endermanBlockPickup());
        buf.writeBoolean(c.equipmentDurability());
    }

    private static CompanionCapabilities readCapabilities(RegistryFriendlyByteBuf buf) {
        return new CompanionCapabilities(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("unknown enum value", ex); }
    }

    private static void writeNullable(RegistryFriendlyByteBuf buf, String value, int max) {
        buf.writeBoolean(value != null);
        if (value != null) buf.writeUtf(value, max);
    }

    private static String readNullable(RegistryFriendlyByteBuf buf, int max) {
        return buf.readBoolean() ? buf.readUtf(max) : null;
    }
}
