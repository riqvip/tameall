package dev.riqvip.tameall.companion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.List;

/** Mojang codecs for persistent companion state and roster snapshots. */
public final class CompanionCodecs {
    private static final Codec<String> ID = Codec.STRING.validate(value ->
            value.length() <= 256 && !value.isBlank() ? DataResult.success(value)
                    : DataResult.error(() -> "invalid identifier"));
    public static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(value -> {
        try { return DataResult.success(UUID.fromString(value)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "invalid UUID"); }
    }, UUID::toString);

    public static final Codec<BlockPoint> BLOCK_POS = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(BlockPoint::x),
            Codec.INT.fieldOf("y").forGetter(BlockPoint::y),
            Codec.INT.fieldOf("z").forGetter(BlockPoint::z)
    ).apply(instance, BlockPoint::new));

    private static final Codec<CompanionMode> MODE = Codec.STRING.comapFlatMap(value -> {
        if ("WORK".equals(value)) return DataResult.success(CompanionMode.FOLLOW);
        try { return DataResult.success(CompanionMode.valueOf(value)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "unknown CompanionMode"); }
    }, Enum::name);

    private static final Codec<NameplateMode> NAMEPLATE = Codec.STRING.comapFlatMap(value -> {
        if ("NEVER".equals(value)) return DataResult.success(NameplateMode.WHEN_TARGETED);
        try { return DataResult.success(NameplateMode.valueOf(value)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "unknown NameplateMode"); }
    }, Enum::name);

    private static final Codec<HealthDisplayMode> HEALTH = Codec.STRING.comapFlatMap(value -> {
        if ("ALWAYS".equals(value) || "WHEN_TARGETED".equals(value)) return DataResult.success(HealthDisplayMode.ON);
        if ("NEVER".equals(value)) return DataResult.success(HealthDisplayMode.OFF);
        try { return DataResult.success(HealthDisplayMode.valueOf(value)); }
        catch (IllegalArgumentException ex) { return DataResult.error(() -> "unknown HealthDisplayMode"); }
    }, Enum::name);

    private static final Codec<Set<String>> TARGET_ENTRIES = Codec.STRING.listOf()
            .xmap(Set::copyOf, java.util.List::copyOf)
            .validate(values -> TargetSelection.isValidShape(values)
                    ? DataResult.success(values)
                    : DataResult.error(() -> "invalid target selection entries"));

    public static final Codec<TargetSelection> TARGET_SELECTION = RecordCodecBuilder.create(instance -> instance.group(
            TARGET_ENTRIES.optionalFieldOf("includes", Set.of()).forGetter(TargetSelection::includes),
            TARGET_ENTRIES.optionalFieldOf("excludes", Set.of()).forGetter(TargetSelection::excludes)
    ).apply(instance, TargetSelection::new));

    /**
     * The optional legacy fields deliberately remain readable so worlds from the
     * single "safe abilities" build migrate without silently changing behavior.
     */
    public static final Codec<MagicToggles> MAGIC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("sunlight_protection").forGetter(value -> Optional.of(value.sunlightProtection())),
            Codec.BOOL.optionalFieldOf("drowning_protection").forGetter(value -> Optional.of(value.drowningProtection())),
            Codec.BOOL.optionalFieldOf("reusable_explosions").forGetter(value -> Optional.of(value.reusableExplosions())),
            Codec.BOOL.optionalFieldOf("prevent_vex_expiry").forGetter(value -> Optional.of(value.preventVexExpiry())),
            Codec.BOOL.optionalFieldOf("allow_enderman_block_pickup").forGetter(value -> Optional.of(value.allowEndermanBlockPickup())),
            Codec.BOOL.optionalFieldOf("habitat_adaptation").forGetter(value -> Optional.<Boolean>empty()),
            Codec.BOOL.optionalFieldOf("safe_special_abilities").forGetter(value -> Optional.<Boolean>empty())
    ).apply(instance, (sun, drowning, reusable, vex, enderman, legacyDrowning, legacySafe) -> {
        boolean legacySpecial = legacySafe.orElse(false);
        return new MagicToggles(
                sun.orElse(false),
                drowning.orElseGet(() -> legacyDrowning.orElse(false)),
                reusable.orElse(legacySpecial),
                vex.orElse(legacySpecial),
                enderman.orElse(!legacySpecial));
    }));

    private static final Codec<Double> PICKUP_RADIUS = Codec.DOUBLE.validate(value ->
            Double.isFinite(value) && value >= CompanionSettings.MIN_PICKUP_RADIUS
                    && value <= CompanionSettings.MAX_PICKUP_RADIUS
                    ? DataResult.success(value)
                    : DataResult.error(() -> "pickup radius out of range"));
    private static final Codec<Double> XP_RADIUS = Codec.DOUBLE.validate(value ->
            Double.isFinite(value) && value >= CompanionSettings.MIN_PICKUP_RADIUS
                    && value <= CompanionSettings.MAX_XP_RADIUS
                    ? DataResult.success(value)
                    : DataResult.error(() -> "xp radius out of range"));

    public static final Codec<CompanionSettings> SETTINGS = RecordCodecBuilder.create(instance -> instance.group(
            MODE.fieldOf("mode").forGetter(CompanionSettings::mode),
            enumCodec(CombatStance.class).fieldOf("stance").forGetter(CompanionSettings::stance),
            MAGIC.fieldOf("magic").forGetter(CompanionSettings::magic),
            NAMEPLATE.fieldOf("nameplate").forGetter(CompanionSettings::nameplate),
            HEALTH.fieldOf("health_display").forGetter(CompanionSettings::healthDisplay),
            Codec.intRange(CompanionSettings.MIN_GUARD_RADIUS, CompanionSettings.MAX_GUARD_RADIUS)
                    .optionalFieldOf("guard_radius", CompanionSettings.DEFAULT_GUARD_RADIUS)
                    .forGetter(CompanionSettings::guardRadius),
            enumCodec(TargetFilter.class).optionalFieldOf("target_filter", TargetFilter.HOSTILE_ONLY)
                    .forGetter(CompanionSettings::targetFilter),
            enumCodec(TeleportMode.class).optionalFieldOf("teleport_mode", TeleportMode.OUTSIDE_AREA)
                    .forGetter(CompanionSettings::teleportMode),
            Codec.BOOL.optionalFieldOf("pickup_items", false).forGetter(CompanionSettings::pickupItems),
            PICKUP_RADIUS.optionalFieldOf("pickup_radius", CompanionSettings.DEFAULT_PICKUP_RADIUS)
                    .forGetter(CompanionSettings::pickupRadius),
            Codec.BOOL.optionalFieldOf("collect_xp_for_mending", false)
                    .forGetter(CompanionSettings::collectXpForMending),
            XP_RADIUS.optionalFieldOf("xp_radius", CompanionSettings.DEFAULT_XP_RADIUS)
                    .forGetter(CompanionSettings::xpRadius),
            Codec.BOOL.optionalFieldOf("use_durability", true).forGetter(CompanionSettings::useDurability),
            Codec.BOOL.optionalFieldOf("saddle_required", true).forGetter(CompanionSettings::saddleRequired),
            Codec.BOOL.optionalFieldOf("attack_while_mounted", false).forGetter(CompanionSettings::attackWhileMounted),
            Codec.BOOL.optionalFieldOf("cargo_container_required", true).forGetter(CompanionSettings::cargoContainerRequired)
    ).apply(instance, CompanionSettings::new));

    public static final Codec<CompanionState> STATE = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("bond_id").forGetter(CompanionState::bondId),
            UUID_CODEC.fieldOf("owner_id").forGetter(CompanionState::ownerId),
            ID.fieldOf("creature_type").forGetter(CompanionState::creatureType),
            Codec.STRING.optionalFieldOf("display_name").forGetter(state -> Optional.ofNullable(state.displayName())),
            SETTINGS.fieldOf("settings").forGetter(CompanionState::settings),
            Codec.STRING.optionalFieldOf("anchor_dimension").forGetter(state -> Optional.ofNullable(state.anchorDimension())),
            BLOCK_POS.optionalFieldOf("anchor").forGetter(state -> Optional.ofNullable(state.anchor())),
            Codec.BOOL.optionalFieldOf("dead", false).forGetter(CompanionState::dead),
            Codec.LONG.optionalFieldOf("revision", 0L).forGetter(CompanionState::revision)
    ).apply(instance, (bond, owner, type, name, settings, dimension, anchor, dead, revision) ->
            new CompanionState(bond, owner, type, name.orElse(null), settings,
                    dimension.orElse(null), anchor.orElse(null), dead, revision)));

    public static final Codec<BondRecord> BOND_RECORD = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("bond_id").forGetter(BondRecord::bondId),
            UUID_CODEC.fieldOf("owner_id").forGetter(BondRecord::ownerId),
            ID.fieldOf("creature_type").forGetter(BondRecord::creatureType),
            UUID_CODEC.optionalFieldOf("entity_id").forGetter(record -> Optional.ofNullable(record.entityId())),
            Codec.STRING.optionalFieldOf("dimension").forGetter(record -> Optional.ofNullable(record.dimension())),
            BLOCK_POS.optionalFieldOf("position").forGetter(record -> Optional.ofNullable(record.position())),
            Codec.BOOL.fieldOf("dead").forGetter(BondRecord::dead),
            enumCodec(CompanionPresence.class).optionalFieldOf("presence")
                    .forGetter(record -> Optional.of(record.presence())),
            Codec.LONG.fieldOf("last_seen_tick").forGetter(BondRecord::lastSeenTick),
            STATE.fieldOf("snapshot").forGetter(BondRecord::snapshot),
            TARGET_SELECTION.optionalFieldOf("target_selection")
                    .forGetter(record -> Optional.of(record.targetSelection()))
    ).apply(instance, (bond, owner, type, entity, dimension, position, dead, presence, tick, snapshot, targets) ->
            new BondRecord(bond, owner, type, entity.orElse(null), dimension.orElse(null),
                    position.orElse(null), dead,
                    presence.orElse(dead ? CompanionPresence.DEAD : CompanionPresence.UNKNOWN), tick, snapshot,
                    targets.orElseGet(() -> TargetSelection.fromLegacy(snapshot.settings().targetFilter())))));

    public static final Codec<GolemProvocation> GOLEM_PROVOCATION = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("owner_id").forGetter(GolemProvocation::ownerId),
            Codec.LONG.fieldOf("expires_at").forGetter(GolemProvocation::expiresAt)
    ).apply(instance, GolemProvocation::new));

    public static final Codec<SummonAffiliation> SUMMON_AFFILIATION = RecordCodecBuilder.create(instance -> instance.group(
            UUID_CODEC.fieldOf("owner_entity_id").forGetter(SummonAffiliation::ownerEntityId),
            UUID_CODEC.fieldOf("owner_player_id").forGetter(SummonAffiliation::ownerPlayerId)
    ).apply(instance, SummonAffiliation::new));

    public static final Codec<CreeperCooldown> CREEPER_COOLDOWN = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("ready_at").forGetter(CreeperCooldown::readyAt)
    ).apply(instance, CreeperCooldown::new));

    public static final Codec<VexLifetime> VEX_LIFETIME = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("had_limited_life").forGetter(VexLifetime::hadLimitedLife),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("remaining_ticks").forGetter(VexLifetime::remainingTicks)
    ).apply(instance, VexLifetime::new));

    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(value -> {
            try { return DataResult.success(Enum.valueOf(type, value)); }
            catch (IllegalArgumentException ex) { return DataResult.error(() -> "unknown " + type.getSimpleName()); }
        }, Enum::name);
    }

    private CompanionCodecs() {}
}
