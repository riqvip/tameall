package dev.riqvip.tameall.companion;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import java.util.Optional;

/** Persistent attachment shared by every living vanilla or modded creature. */
public final class CompanionAttachments {
    public static final AttachmentType<CompanionState> STATE = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "companion_state"), CompanionCodecs.STATE);

    /**
     * The full state intentionally stays server-owned. This small synced view is
     * enough for the client renderer to implement "when targeted" nameplates
     * for every companion, including companions that are not currently open in
     * the controls screen.
     */
    public record Presentation(NameplateMode nameplate, HealthDisplayMode healthDisplay) {
        public Presentation {
            if (nameplate == null || healthDisplay == null) throw new NullPointerException("presentation");
        }
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Presentation> PRESENTATION_CODEC =
            StreamCodec.of((buf, value) -> {
                buf.writeUtf(value.nameplate().name(), 32);
                buf.writeUtf(value.healthDisplay().name(), 32);
            }, buf -> new Presentation(enumValue(NameplateMode.class, buf.readUtf(32)),
                    enumValue(HealthDisplayMode.class, buf.readUtf(32))));

    public static final AttachmentType<Presentation> PRESENTATION = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath("tameall", "companion_presentation"),
            builder -> builder.syncWith(PRESENTATION_CODEC, AttachmentSyncPredicate.all()));

    public static final AttachmentType<GolemProvocation> GOLEM_PROVOCATION = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "golem_provocation"), CompanionCodecs.GOLEM_PROVOCATION);

    public static final AttachmentType<SummonAffiliation> SUMMON_AFFILIATION = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "summon_affiliation"), CompanionCodecs.SUMMON_AFFILIATION);

    public static final AttachmentType<CreeperCooldown> CREEPER_COOLDOWN = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "creeper_cooldown"), CompanionCodecs.CREEPER_COOLDOWN);

    /** Large, explicit include/exclude lists live separately from the compact menu settings record. */
    public static final AttachmentType<TargetSelection> TARGET_SELECTION = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "target_selection"), CompanionCodecs.TARGET_SELECTION);

    public static final AttachmentType<VexLifetime> VEX_LIFETIME = AttachmentRegistry.createPersistent(
            Identifier.fromNamespaceAndPath("tameall", "vex_lifetime"), CompanionCodecs.VEX_LIFETIME);

    private CompanionAttachments() {}

    public static Optional<CompanionState> get(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return Optional.empty();
        return Optional.ofNullable(target.getAttached(STATE));
    }

    public static void set(Entity entity, CompanionState state) {
        if (!(entity instanceof AttachmentTarget target)) {
            throw new IllegalArgumentException("entity does not expose Fabric attachments");
        }
        target.setAttached(STATE, state);
        syncPresentation(entity, state);
    }

    public static void clear(Entity entity) {
        if (entity instanceof AttachmentTarget target) {
            target.removeAttached(STATE);
            target.removeAttached(PRESENTATION);
            target.removeAttached(TARGET_SELECTION);
            target.removeAttached(VEX_LIFETIME);
        }
    }

    public static Optional<Presentation> getPresentation(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return Optional.empty();
        return Optional.ofNullable(target.getAttached(PRESENTATION));
    }

    public static Optional<GolemProvocation> getGolemProvocation(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return Optional.empty();
        return Optional.ofNullable(target.getAttached(GOLEM_PROVOCATION));
    }

    public static void setGolemProvocation(Entity entity, GolemProvocation value) {
        if (entity instanceof AttachmentTarget target) target.setAttached(GOLEM_PROVOCATION, value);
    }

    public static Optional<SummonAffiliation> getSummonAffiliation(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return Optional.empty();
        return Optional.ofNullable(target.getAttached(SUMMON_AFFILIATION));
    }

    public static void setSummonAffiliation(Entity entity, SummonAffiliation value) {
        if (entity instanceof AttachmentTarget target) target.setAttached(SUMMON_AFFILIATION, value);
    }

    public static Optional<CreeperCooldown> getCreeperCooldown(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return Optional.empty();
        return Optional.ofNullable(target.getAttached(CREEPER_COOLDOWN));
    }

    public static void setCreeperCooldown(Entity entity, CreeperCooldown value) {
        if (entity instanceof AttachmentTarget target) target.setAttached(CREEPER_COOLDOWN, value);
    }

    public static Optional<TargetSelection> getTargetSelection(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return Optional.empty();
        return Optional.ofNullable(target.getAttached(TARGET_SELECTION));
    }

    public static void setTargetSelection(Entity entity, TargetSelection value) {
        if (entity instanceof AttachmentTarget target) target.setAttached(TARGET_SELECTION, value);
    }

    public static void clearTargetSelection(Entity entity) {
        if (entity instanceof AttachmentTarget target) target.removeAttached(TARGET_SELECTION);
    }

    public static Optional<VexLifetime> getVexLifetime(Entity entity) {
        if (!(entity instanceof AttachmentTarget target)) return Optional.empty();
        return Optional.ofNullable(target.getAttached(VEX_LIFETIME));
    }

    public static void setVexLifetime(Entity entity, VexLifetime value) {
        if (entity instanceof AttachmentTarget target) target.setAttached(VEX_LIFETIME, value);
    }

    public static void clearVexLifetime(Entity entity) {
        if (entity instanceof AttachmentTarget target) target.removeAttached(VEX_LIFETIME);
    }

    /** Refreshes the client-visible subset after loading a persisted state. */
    public static void syncPresentation(Entity entity, CompanionState state) {
        if (entity instanceof AttachmentTarget target && state != null) {
            target.setAttached(PRESENTATION, new Presentation(state.settings().nameplate(),
                    state.settings().healthDisplay()));
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("unknown presentation value", ex); }
    }
}
