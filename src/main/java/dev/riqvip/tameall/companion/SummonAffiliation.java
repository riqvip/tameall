package dev.riqvip.tameall.companion;

import java.util.UUID;

/** Persistent affiliation for an evoker's vex; it is not a companion bond. */
public record SummonAffiliation(UUID ownerEntityId, UUID ownerPlayerId) {
    public SummonAffiliation {
        if (ownerEntityId == null || ownerPlayerId == null) throw new NullPointerException("summon owner");
    }
}
