package dev.riqvip.tameall.companion;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** World-persistent, server-wide journal. It is stored in the overworld data folder. */
public final class CompanionJournal extends SavedData {
    public static final SavedDataType<CompanionJournal> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("tameall", "companions"), CompanionJournal::new,
            CompanionCodecs.BOND_RECORD.listOf().xmap(CompanionJournal::new, CompanionJournal::records),
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final ConcurrentHashMap<UUID, BondRecord> records = new ConcurrentHashMap<>();

    public CompanionJournal() {}

    private CompanionJournal(List<BondRecord> entries) {
        entries.forEach(record -> records.put(record.bondId(), record));
    }

    private List<BondRecord> records() {
        return new ArrayList<>(records.values());
    }

    public static CompanionJournal get(ServerLevel anyLevel) {
        return anyLevel.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public void put(BondRecord record) {
        records.put(record.bondId(), record);
        setDirty();
    }

    public BondRecord get(UUID bondId) {
        return records.get(bondId);
    }

    public List<BondRecord> listOwned(UUID ownerId) {
        return records.values().stream()
                .filter(record -> record.ownerId().equals(ownerId))
                .sorted(java.util.Comparator.comparing(BondRecord::dead)
                        .thenComparing(record -> record.snapshot().displayName() == null ? record.creatureType()
                                : record.snapshot().displayName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(record -> record.bondId().toString()))
                .toList();
    }

    /** Removes only an owner-owned, confirmed-dead roster entry. */
    public boolean removeDead(UUID bondId, UUID ownerId) {
        BondRecord record = records.get(bondId);
        if (record == null || !record.dead() || !record.ownerId().equals(ownerId)) return false;
        records.remove(bondId, record);
        setDirty();
        return true;
    }

    /** Finds a live journal entry for an entity whose attachment was lost during an older save. */
    public BondRecord findLiveByEntity(UUID entityId) {
        return records.values().stream()
                .filter(record -> !record.dead() && entityId.equals(record.entityId()))
                .sorted(java.util.Comparator.comparingLong(BondRecord::lastSeenTick).reversed()
                        .thenComparing(record -> record.bondId().toString()))
                .findFirst().orElse(null);
    }

    public boolean hasLiveOwnerConflict(UUID entityId, UUID ownerId) {
        return records.values().stream().anyMatch(record -> !record.dead()
                && entityId.equals(record.entityId())
                && !ownerId.equals(record.ownerId()));
    }

    public boolean markDead(CompanionState state, String dimension, BlockPoint position, long tick) {
        BondRecord old = records.get(state.bondId());
        if (old != null && old.dead()) return false;
        put(BondRecord.fromState(state.markDead(), null, dimension, position, tick));
        return true;
    }

    public boolean markUnloaded(UUID entityId, String dimension, BlockPoint position, long tick) {
        boolean changed = false;
        for (BondRecord record : records.values()) {
            if (!record.dead() && entityId.equals(record.entityId())) {
                put(record.unloaded(dimension, position, tick));
                changed = true;
            }
        }
        return changed;
    }

    public boolean markUnknown(UUID bondId, String dimension, BlockPoint position, long tick) {
        BondRecord record = records.get(bondId);
        if (record == null || record.dead() || record.presence() == CompanionPresence.UNKNOWN) return false;
        put(record.unknown(dimension, position, tick));
        return true;
    }

    public boolean replaceEntity(CompanionState state, UUID entityId, String dimension,
                                 BlockPoint position, long tick) {
        BondRecord current = records.get(state.bondId());
        if (current != null && current.dead() && !state.dead()) return false;
        // A previous build could leave an old live record when retaming after a
        // failed attachment save. Keep one bond identity per owner/entity pair.
        records.values().removeIf(old -> !old.bondId().equals(state.bondId())
                && old.ownerId().equals(state.ownerId())
                && entityId != null && entityId.equals(old.entityId()));
        BondRecord replacement = BondRecord.fromState(state, entityId, dimension, position, tick);
        if (current != null) replacement = replacement.withTargetSelection(current.targetSelection(), tick);
        put(replacement);
        return true;
    }

    /** Updates a pending target include/exclude snapshot without loading an entity. */
    public boolean updateTargetSelection(UUID bondId, TargetSelection selection, long tick) {
        BondRecord current = records.get(bondId);
        if (current == null || current.dead() || selection == null) return false;
        put(current.withTargetSelection(selection, tick));
        return true;
    }

    public BondRegistry asRegistry() {
        BondRegistry registry = new BondRegistry();
        records.values().forEach(registry::put);
        return registry;
    }
}
