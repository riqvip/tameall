package dev.riqvip.tameall.menu;

import dev.riqvip.tameall.companion.CombatStance;
import dev.riqvip.tameall.companion.CompanionMode;
import dev.riqvip.tameall.companion.HealthDisplayMode;
import dev.riqvip.tameall.companion.NameplateMode;
import dev.riqvip.tameall.companion.TargetFilter;
import dev.riqvip.tameall.companion.TeleportMode;
import java.util.Objects;
import java.util.UUID;

/** Typed command sent by the client. The server still revalidates every field and action. */
public record CompanionActionRequest(UUID bondId, long expectedRevision, CompanionAction action,
                                     String text, int number, boolean value,
                                     CompanionMode mode, CombatStance stance,
                                     NameplateMode nameplate, HealthDisplayMode healthDisplay,
                                     TargetFilter targetFilter, TeleportMode teleportMode,
                                     double decimal) {
    public CompanionActionRequest {
        Objects.requireNonNull(bondId, "bondId");
        Objects.requireNonNull(action, "action");
        if (expectedRevision < 0) throw new IllegalArgumentException("revision");
        if (text != null && text.length() > 256) throw new IllegalArgumentException("text is too long");
        if (action == CompanionAction.SET_MODE && mode == null) throw new IllegalArgumentException("missing mode");
        if (action == CompanionAction.SET_STANCE && stance == null) throw new IllegalArgumentException("missing stance");
        if (action == CompanionAction.SET_NAMEPLATE && nameplate == null) throw new IllegalArgumentException("missing nameplate");
        if (action == CompanionAction.SET_HEALTH_DISPLAY && healthDisplay == null) throw new IllegalArgumentException("missing health display");
        if (action == CompanionAction.SET_TARGET_FILTER && targetFilter == null) throw new IllegalArgumentException("missing target filter");
        if (action == CompanionAction.SET_TELEPORT_MODE && teleportMode == null) throw new IllegalArgumentException("missing teleport mode");
        if (action == CompanionAction.SET_PICKUP_RADIUS
                && (!Double.isFinite(decimal) || decimal < 1.0D || decimal > 16.0D)) {
            throw new IllegalArgumentException("pickup radius out of range");
        }
        if (action == CompanionAction.SET_XP_RADIUS
                && (!Double.isFinite(decimal) || decimal < 1.0D || decimal > 16.0D)) {
            throw new IllegalArgumentException("xp radius out of range");
        }
    }

    /** Source compatibility for simple commands created by older screens. */
    public CompanionActionRequest(UUID bondId, long expectedRevision, CompanionAction action,
                                  String text, int number, boolean value,
                                  CompanionMode mode, CombatStance stance,
                                  NameplateMode nameplate, HealthDisplayMode healthDisplay,
                                  TargetFilter targetFilter) {
        this(bondId, expectedRevision, action, text, number, value, mode, stance,
                nameplate, healthDisplay, targetFilter, null, 0.0D);
    }

    public static CompanionActionRequest command(UUID bondId, long revision, CompanionAction action) {
        return new CompanionActionRequest(bondId, revision, action, null, 0, false,
                null, null, null, null, null, null, 0.0D);
    }
}
