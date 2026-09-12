package dev.riqvip.tameall.menu;

import dev.riqvip.tameall.companion.CompanionSettings;
import dev.riqvip.tameall.companion.CompanionState;
import dev.riqvip.tameall.companion.CompanionCapabilities;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Server snapshot rendered by the client. No client action mutates this object; a new
 * revision must arrive from the server after every accepted request.
 */
public record CompanionMenuSnapshot(UUID bondId, UUID entityId, String creatureType,
                                    String displayName, float health, float maxHealth,
                                    CompanionSettings settings,
                                    List<String> inventorySummary, boolean dead, long revision,
                                    boolean cheatsAllowed, CompanionCapabilities capabilities) {
    public CompanionMenuSnapshot {
        Objects.requireNonNull(bondId, "bondId");
        Objects.requireNonNull(creatureType, "creatureType");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(capabilities, "capabilities");
        inventorySummary = inventorySummary == null ? List.of() : List.copyOf(inventorySummary);
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || health < 0 || maxHealth <= 0 || health > maxHealth) {
            throw new IllegalArgumentException("invalid health");
        }
        if (revision < 0) throw new IllegalArgumentException("revision");
    }

    public CompanionMenuSnapshot(UUID bondId, UUID entityId, String creatureType,
                                 String displayName, float health, float maxHealth,
                                 CompanionSettings settings, List<String> inventorySummary,
                                 boolean dead, long revision) {
        this(bondId, entityId, creatureType, displayName, health, maxHealth, settings,
                inventorySummary, dead, revision, false, CompanionCapabilities.defaults());
    }

    public CompanionMenuSnapshot(UUID bondId, UUID entityId, String creatureType,
                                 String displayName, float health, float maxHealth,
                                 CompanionSettings settings, List<String> inventorySummary,
                                 boolean dead, long revision, boolean cheatsAllowed) {
        this(bondId, entityId, creatureType, displayName, health, maxHealth, settings,
                inventorySummary, dead, revision, cheatsAllowed, CompanionCapabilities.defaults());
    }

    public static CompanionMenuSnapshot fromState(CompanionState state, UUID entityId,
                                                   float health, float maxHealth) {
        return fromState(state, entityId, health, maxHealth, false);
    }

    public static CompanionMenuSnapshot fromState(CompanionState state, UUID entityId,
                                                   float health, float maxHealth,
                                                   boolean cheatsAllowed) {
        return new CompanionMenuSnapshot(state.bondId(), entityId, state.creatureType(),
                state.displayName(), health, maxHealth, state.settings(),
                List.of(), state.dead(), state.revision(), cheatsAllowed);
    }

    public static CompanionMenuSnapshot fromState(CompanionState state,
                                                   net.minecraft.world.entity.LivingEntity entity,
                                                   boolean cheatsAllowed) {
        return new CompanionMenuSnapshot(state.bondId(), entity.getUUID(), state.creatureType(),
                state.displayName(), entity.getHealth(), entity.getMaxHealth(), state.settings(),
                List.of(), state.dead(), state.revision(), cheatsAllowed,
                CompanionCapabilities.forEntity(entity));
    }
}
