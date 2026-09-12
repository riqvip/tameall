package dev.riqvip.tameall.companion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * In-memory index with atomic replacement semantics. The platform adapter owns
 * serialization to the world's persistent state; this class intentionally has
 * no Minecraft dependency and is easy to exercise in unit tests.
 */
public final class BondRegistry {
    private final ConcurrentMap<UUID, BondRecord> records = new ConcurrentHashMap<>();

    public void put(BondRecord record) {
        records.put(record.bondId(), record);
    }

    public Optional<BondRecord> get(UUID bondId) {
        return Optional.ofNullable(records.get(bondId));
    }

    public Optional<BondRecord> findOwned(UUID ownerId, UUID bondId) {
        BondRecord record = records.get(bondId);
        return record != null && record.ownerId().equals(ownerId) ? Optional.of(record) : Optional.empty();
    }

    public List<BondRecord> listOwned(UUID ownerId) {
        return records.values().stream()
                .filter(record -> record.ownerId().equals(ownerId))
                .sorted(Comparator.comparing(BondRecord::bondId))
                .toList();
    }

    public Optional<BondRecord> removeIf(UUID bondId, Predicate<BondRecord> predicate) {
        BondRecord current = records.get(bondId);
        if (current != null && predicate.test(current) && records.remove(bondId, current)) {
            return Optional.of(current);
        }
        return Optional.empty();
    }

    public int size() {
        return records.size();
    }

    public void clear() {
        records.clear();
    }
}
