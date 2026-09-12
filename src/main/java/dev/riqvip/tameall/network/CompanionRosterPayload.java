package dev.riqvip.tameall.network;

import dev.riqvip.tameall.companion.BlockPoint;
import dev.riqvip.tameall.companion.BondRecord;
import dev.riqvip.tameall.companion.CombatStance;
import dev.riqvip.tameall.companion.CompanionMode;
import dev.riqvip.tameall.companion.CompanionPresence;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.List;
import java.util.UUID;

/** Owner-scoped companion directory opened by the reusable Companion Whistle. */
public record CompanionRosterPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<CompanionRosterPayload> ID = new Type<>(NetworkIds.ROSTER);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionRosterPayload> CODEC =
            CustomPacketPayload.codec(CompanionRosterPayload::write, CompanionRosterPayload::read);

    public CompanionRosterPayload {
        entries = entries == null ? List.of() : List.copyOf(entries);
        if (entries.size() > 256) throw new IllegalArgumentException("too many companions");
    }

    public static CompanionRosterPayload from(List<BondRecord> records) {
        if (records == null || records.isEmpty()) return new CompanionRosterPayload(List.of());
        // Keep the packet bounded even if an owner has accumulated more bonds
        // than the roster UI can display in one response.
        return new CompanionRosterPayload(records.stream().limit(256).map(Entry::from).toList());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    private static void write(CompanionRosterPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buf.writeUUID(entry.bondId());
            buf.writeUtf(entry.displayName(), 64);
            buf.writeUtf(entry.creatureType(), 256);
            writeNullable(buf, entry.dimension(), 256);
            buf.writeBoolean(entry.position() != null);
            if (entry.position() != null) {
                buf.writeInt(entry.position().x()); buf.writeInt(entry.position().y()); buf.writeInt(entry.position().z());
            }
            buf.writeUtf(entry.presence().name(), 32);
            buf.writeUtf(entry.mode().name(), 32);
            buf.writeUtf(entry.stance().name(), 32);
        }
    }

    private static CompanionRosterPayload read(RegistryFriendlyByteBuf buf) {
        int encodedCount = buf.readVarInt();
        if (encodedCount < 0) throw new IllegalArgumentException("negative roster size");
        int count = Math.min(encodedCount, 256);
        java.util.ArrayList<Entry> entries = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID id = buf.readUUID();
            String name = buf.readUtf(64);
            String type = buf.readUtf(256);
            String dimension = readNullable(buf, 256);
            BlockPoint position = buf.readBoolean() ? new BlockPoint(buf.readInt(), buf.readInt(), buf.readInt()) : null;
            CompanionPresence presence = enumValue(CompanionPresence.class, buf.readUtf(32));
            entries.add(new Entry(id, name, type, dimension, position, presence,
                    enumValue(CompanionMode.class, buf.readUtf(32)),
                    enumValue(CombatStance.class, buf.readUtf(32))));
        }
        return new CompanionRosterPayload(entries);
    }

    private static void writeNullable(RegistryFriendlyByteBuf buf, String value, int max) {
        buf.writeBoolean(value != null);
        if (value != null) buf.writeUtf(value, max);
    }

    private static String readNullable(RegistryFriendlyByteBuf buf, int max) {
        return buf.readBoolean() ? buf.readUtf(max) : null;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("unknown roster setting", ex); }
    }

    public record Entry(UUID bondId, String displayName, String creatureType, String dimension,
                        BlockPoint position, CompanionPresence presence,
                        CompanionMode mode, CombatStance stance) {
        /** Compatibility constructor for integrations using the former flags. */
        public Entry(UUID bondId, String displayName, String creatureType, String dimension,
                     BlockPoint position, boolean dead, boolean available,
                     CompanionMode mode, CombatStance stance) {
            this(bondId, displayName, creatureType, dimension, position,
                    dead ? CompanionPresence.DEAD : available ? CompanionPresence.ALIVE : CompanionPresence.UNKNOWN,
                    mode, stance);
        }

        public Entry {
            if (bondId == null || displayName == null || displayName.isBlank()
                    || creatureType == null || creatureType.isBlank() || presence == null
                    || mode == null || stance == null) {
                throw new IllegalArgumentException("invalid roster entry");
            }
        }

        private static Entry from(BondRecord record) {
            String name = record.snapshot().displayName();
            if (name == null || name.isBlank()) name = record.creatureType();
            return new Entry(record.bondId(), name, record.creatureType(), record.dimension(),
                    record.position(), record.presence(),
                    record.snapshot().settings().mode(), record.snapshot().settings().stance());
        }

        public boolean dead() { return presence == CompanionPresence.DEAD; }
        public boolean available() { return presence == CompanionPresence.ALIVE; }
    }
}
