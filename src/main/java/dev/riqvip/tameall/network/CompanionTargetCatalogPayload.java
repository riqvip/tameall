package dev.riqvip.tameall.network;

import dev.riqvip.tameall.companion.TargetCatalog;
import dev.riqvip.tameall.companion.TargetSelection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Server-provided target selector entries and the bond's current selection. */
public record CompanionTargetCatalogPayload(UUID bondId, long revision,
                                            List<TargetCatalog.Entry> entries,
                                            TargetSelection selection) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CompanionTargetCatalogPayload> ID =
            new CustomPacketPayload.Type<>(NetworkIds.TARGET_CATALOG);
    public static final StreamCodec<RegistryFriendlyByteBuf, CompanionTargetCatalogPayload> CODEC =
            CustomPacketPayload.codec(CompanionTargetCatalogPayload::write, CompanionTargetCatalogPayload::read);

    public CompanionTargetCatalogPayload {
        if (bondId == null || revision < 0 || selection == null) throw new IllegalArgumentException("invalid target catalog");
        entries = TargetCatalog.trimForWire(entries);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return ID; }

    private static void write(CompanionTargetCatalogPayload payload, RegistryFriendlyByteBuf buf) {
        buf.writeUUID(payload.bondId());
        buf.writeVarLong(payload.revision());
        buf.writeVarInt(payload.entries().size());
        for (TargetCatalog.Entry entry : payload.entries()) {
            buf.writeUtf(entry.id(), TargetSelection.MAX_ENTRY_LENGTH);
            buf.writeUtf(entry.label(), 256);
            buf.writeUtf(entry.kind(), 32);
            buf.writeCollection(entry.categories(), (b, category) -> b.writeUtf(category, 64));
        }
        writeSelection(buf, payload.selection());
    }

    private static CompanionTargetCatalogPayload read(RegistryFriendlyByteBuf buf) {
        UUID bond = buf.readUUID();
        long revision = buf.readVarLong();
        int count = buf.readVarInt();
        if (count < 0 || count > 512) throw new IllegalArgumentException("target catalog too large");
        List<TargetCatalog.Entry> entries = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new TargetCatalog.Entry(buf.readUtf(TargetSelection.MAX_ENTRY_LENGTH),
                    buf.readUtf(256), buf.readUtf(32), buf.readList(b -> b.readUtf(64))));
        }
        return new CompanionTargetCatalogPayload(bond, revision, entries, readSelection(buf));
    }

    public static void writeSelection(RegistryFriendlyByteBuf buf, TargetSelection selection) {
        writeStrings(buf, selection.includes());
        writeStrings(buf, selection.excludes());
    }

    public static TargetSelection readSelection(RegistryFriendlyByteBuf buf) {
        return new TargetSelection(readStrings(buf), readStrings(buf));
    }

    private static void writeStrings(RegistryFriendlyByteBuf buf, Set<String> values) {
        if (values.size() > TargetSelection.MAX_ENTRIES_PER_SIDE) throw new IllegalArgumentException("too many target entries");
        buf.writeVarInt(values.size());
        for (String value : values) buf.writeUtf(value, TargetSelection.MAX_ENTRY_LENGTH);
    }

    private static Set<String> readStrings(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > TargetSelection.MAX_ENTRIES_PER_SIDE) throw new IllegalArgumentException("too many target entries");
        java.util.LinkedHashSet<String> values = new java.util.LinkedHashSet<>();
        for (int i = 0; i < count; i++) values.add(buf.readUtf(TargetSelection.MAX_ENTRY_LENGTH));
        return values;
    }
}
