package dev.riqvip.tameall.companion;

import dev.riqvip.tameall.ModItems;
import dev.riqvip.tameall.menu.CompanionAction;
import dev.riqvip.tameall.menu.CompanionActionRequest;
import dev.riqvip.tameall.menu.CompanionActionValidator;
import dev.riqvip.tameall.menu.CompanionMenuSnapshot;
import dev.riqvip.tameall.network.CompanionActionPayload;
import dev.riqvip.tameall.network.CompanionRosterActionPayload;
import dev.riqvip.tameall.network.CompanionRosterPayload;
import dev.riqvip.tameall.network.CompanionSnapshotPayload;
import dev.riqvip.tameall.network.CompanionTargetCatalogPayload;
import dev.riqvip.tameall.network.CompanionTargetSelectionPayload;
import dev.riqvip.tameall.network.CompanionTargetSelectionResultPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import dev.riqvip.tameall.menu.CompanionMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.core.particles.ParticleTypes;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.Entity.RemovalReason;

/** Common server runtime for universal bonding and companion behaviour. */
public final class CompanionRuntime {
    private static final double FOLLOW_TELEPORT_MULTIPLIER = 2.0D;
    private static final long LOST_SIGHT_PURSUIT_TICKS = 60L;
    private static BiConsumer<ServerPlayer, LivingEntity> menuOpener = (player, entity) -> {};
    private static final Map<UUID, Long> LOST_SIGHT_AT = new HashMap<>();
    private static final Map<UUID, PendingRemoval> PENDING_REMOVALS = new HashMap<>();

    private record PendingRemoval(Entity entity, ServerLevel level, RemovalReason reason, long tick) {}

    private CompanionRuntime() {}

    public static void initialize() {
        CompanionEquipment.ITEMS.toString();
        menuOpener = (player, entity) -> CompanionAttachments.get(entity)
                .ifPresent(state -> sendSnapshot(player, entity, state, true));
        UseEntityCallback.EVENT.register(CompanionRuntime::onUseEntity);
        ServerEntityEvents.ENTITY_LOAD.register(CompanionRuntime::onEntityLoad);
        ServerEntityEvents.ENTITY_UNLOAD.register(CompanionRuntime::onEntityUnload);
        ServerLivingEntityEvents.AFTER_DEATH.register(CompanionRuntime::onDeath);
        ServerLivingEntityEvents.MOB_CONVERSION.register(CompanionRuntime::onConversion);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(CompanionRuntime::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(CompanionRuntime::afterDamage);
        ServerTickEvents.START_LEVEL_TICK.register(CompanionRuntime::prepareLevel);
        ServerTickEvents.END_LEVEL_TICK.register(CompanionRuntime::tickLevel);
    }

    /** Client UI registers this callback; the server remains the sole authority. */
    public static void setMenuOpener(BiConsumer<ServerPlayer, LivingEntity> opener) {
        menuOpener = opener == null ? (player, entity) -> {} : opener;
    }

    public static void handleAction(ServerPlayer player, CompanionActionPayload payload) {
        CompanionActionRequest request;
        try { request = payload.request(); } catch (RuntimeException rejected) { return; }
        var server = player.level().getServer();
        BondRecord record = CompanionJournal.get(server.overworld()).get(request.bondId());
        LivingEntity living = record == null ? null : findBondEntity(server, record, false);
        if (living == null) return;
        CompanionState state = CompanionAttachments.get(living).orElse(null);
        if (state == null || !state.ownerId().equals(player.getUUID())) return;
        boolean near = living.level() == player.level() && living.distanceToSqr(player) <= 8.0D * 8.0D;
        try {
            CompanionActionValidator.validate(
                    CompanionMenuSnapshot.fromState(state, living, cheatsAllowed(player)),
                    request, player.getUUID(), true, living.isAlive(), near);
        } catch (RuntimeException rejected) { return; }
        if (request.action() == CompanionAction.OPEN_INVENTORY) {
            openInventory(player, living, state);
            return;
        }
        if (request.action() == CompanionAction.OPEN_TARGET_SELECTOR) {
            sendTargetCatalog(player, living, state);
            return;
        }
        if (isCheatAction(request.action(), request.value()) && !cheatsAllowed(player)) return;

        CompanionSettings settings = state.settings();
        CompanionSettings changed = settings;
        CompanionState updated = state;
        try {
            switch (request.action()) {
                case RENAME -> updated = state.rename(request.text());
                case SET_MODE -> {
                    changed = settings.withMode(request.mode());
                    if (request.mode() == CompanionMode.STAY ||
                            (request.mode() == CompanionMode.GUARD && state.anchor() == null)) {
                        if (living.level() instanceof ServerLevel level) {
                            updated = updated.withAnchor(dimension(level), blockPos(living));
                        }
                    }
                }
                case SET_STANCE -> changed = settings.withStance(request.stance());
                case SET_NAMEPLATE -> changed = settings.withNameplate(request.nameplate());
                case SET_HEALTH_DISPLAY -> changed = settings.withHealthDisplay(request.healthDisplay());
                case SET_GUARD_RADIUS -> changed = settings.withGuardRadius(request.number());
                case SET_TARGET_FILTER -> {
                    changed = settings.withTargetFilter(request.targetFilter());
                    CompanionAttachments.setTargetSelection(living, TargetSelection.fromLegacy(request.targetFilter()));
                }
                case SET_TELEPORT_MODE -> changed = settings.withTeleportMode(request.teleportMode());
                case SET_PICKUP_ITEMS -> changed = settings.withPickupItems(request.value());
                case SET_PICKUP_RADIUS -> changed = settings.withPickupRadius(request.decimal());
                case SET_COLLECT_XP -> changed = settings.withCollectXpForMending(request.value());
                case SET_XP_RADIUS -> changed = settings.withXpRadius(request.decimal());
                case SET_USE_DURABILITY -> changed = settings.withUseDurability(request.value());
                case SET_GUARD_ANCHOR -> {
                    if (living.level() instanceof ServerLevel level) updated = updated.withAnchor(dimension(level), blockPos(living));
                }
                case SET_SUNLIGHT_PROTECTION -> changed = settings.withMagic(settings.magic().withSunlightProtection(request.value()));
                case SET_DROWNING_PROTECTION, SET_HABITAT_ADAPTATION ->
                        changed = settings.withMagic(settings.magic().withDrowningProtection(request.value()));
                case SET_REUSABLE_EXPLOSIONS ->
                        changed = settings.withMagic(settings.magic().withReusableExplosions(request.value()));
                case SET_PREVENT_VEX_EXPIRY ->
                        changed = settings.withMagic(settings.magic().withPreventVexExpiry(request.value()));
                case SET_ENDERMAN_BLOCK_PICKUP ->
                        changed = settings.withMagic(settings.magic().withAllowEndermanBlockPickup(request.value()));
                case SET_SAFE_SPECIAL_ABILITIES ->
                        changed = settings.withMagic(settings.magic().withSafeSpecialAbilities(request.value()));
                default -> { }
            }
        } catch (RuntimeException rejected) {
            return;
        }
        if (changed != settings) updated = updated.withSettings(changed);
        if (updated != state) {
            CompanionAttachments.set(living, updated);
            applyPresentation(living, updated);
            if (living.level() instanceof ServerLevel level) {
                CompanionJournal.get(level).replaceEntity(updated, living.getUUID(), dimension(level), blockPos(living), level.getGameTime());
            }
        }
        sendSnapshot(player, living, updated, false);
    }

    public static void sendRoster(ServerPlayer player) {
        CompanionJournal journal = CompanionJournal.get(player.level().getServer().overworld());
        refreshRosterPresence(player.level().getServer(), journal, player.getUUID());
        ServerPlayNetworking.send(player, CompanionRosterPayload.from(journal.listOwned(player.getUUID())));
    }

    private static void refreshRosterPresence(net.minecraft.server.MinecraftServer server,
                                              CompanionJournal journal, UUID ownerId) {
        for (BondRecord record : journal.listOwned(ownerId)) {
            if (record.dead()) continue;
            LivingEntity found = findBondEntity(server, record, false);
            if (found != null && found.isAlive()) {
                CompanionState state = CompanionAttachments.get(found).orElse(null);
                if (state == null && record.entityId() != null
                        && record.entityId().equals(found.getUUID())
                        && found.level() instanceof ServerLevel foundLevel) {
                    // Older saves can retain the journal while losing the
                    // attachment. Restore only the matching UUID, never an
                    // arbitrary same-type entity.
                    state = restoreFromJournal(found, foundLevel, record);
                }
                if (state != null && state.bondId().equals(record.bondId())
                        && state.ownerId().equals(ownerId) && !state.dead()
                        && found.level() instanceof ServerLevel level) {
                    journal.replaceEntity(state, found.getUUID(), dimension(level), blockPos(found), level.getGameTime());
                }
            } else if (record.presence() == CompanionPresence.ALIVE) {
                journal.markUnknown(record.bondId(), record.dimension(), record.position(),
                        Math.max(record.lastSeenTick(), server.overworld().getGameTime()));
            }
        }
    }

    /** Sends a bounded, server-resolved catalog for the owner-only selector. */
    private static void sendTargetCatalog(ServerPlayer player, LivingEntity living, CompanionState state) {
        if (!(living.level() instanceof ServerLevel level)) return;
        TargetSelection selection = CompanionAttachments.getTargetSelection(living)
                .orElseGet(() -> TargetSelection.fromLegacy(state.settings().targetFilter()));
        List<TargetCatalog.Entry> catalog = new java.util.ArrayList<>(TargetCatalog.entries(level));
        java.util.HashSet<String> known = new java.util.HashSet<>();
        for (TargetCatalog.Entry entry : catalog) known.add(entry.id());
        for (String id : selection.includes()) if (known.add(id)) {
            catalog.add(new TargetCatalog.Entry(id, "Unavailable: " + id, "unavailable"));
        }
        for (String id : selection.excludes()) if (known.add(id)) {
            catalog.add(new TargetCatalog.Entry(id, "Unavailable: " + id, "unavailable"));
        }
        // Keep every selected entry visible even when a content pack makes the
        // live catalog reach the packet limit. This also preserves an explicit
        // type/tag that would otherwise sort beyond the first 512 entries.
        Set<String> selectedIds = new java.util.LinkedHashSet<>();
        selectedIds.addAll(selection.includes());
        selectedIds.addAll(selection.excludes());
        List<TargetCatalog.Entry> selected = catalog.stream()
                .filter(entry -> selectedIds.contains(entry.id()) && !"unavailable".equals(entry.kind()))
                .toList();
        List<TargetCatalog.Entry> unavailable = catalog.stream()
                .filter(entry -> "unavailable".equals(entry.kind())).toList();
        Set<String> selectedKnown = selected.stream().map(TargetCatalog.Entry::id).collect(java.util.stream.Collectors.toSet());
        unavailable = unavailable.stream().filter(entry -> !selectedKnown.contains(entry.id())).toList();
        int baseLimit = Math.max(0, 512 - selected.size() - unavailable.size());
        List<TargetCatalog.Entry> bounded = new java.util.ArrayList<>(selected);
        bounded.addAll(catalog.stream()
                .filter(entry -> !selectedIds.contains(entry.id()) && !"unavailable".equals(entry.kind()))
                .limit(baseLimit).toList());
        bounded.addAll(unavailable);
        ServerPlayNetworking.send(player, new CompanionTargetCatalogPayload(state.bondId(), state.revision(),
                TargetCatalog.trimForWire(bounded), selection));
    }

    /** Applies a selector result only after re-checking the live bond and every entry shape. */
    public static void handleTargetSelection(ServerPlayer player, CompanionTargetSelectionPayload payload) {
        var server = player.level().getServer();
        BondRecord record = CompanionJournal.get(server.overworld()).get(payload.bondId());
        LivingEntity living = record == null ? null : findBondEntity(server, record, false);
        if (living == null || !(living.level() instanceof ServerLevel level)) {
            sendTargetResult(player, payload, false, "Companion is unavailable.");
            return;
        }
        CompanionState state = CompanionAttachments.get(living).orElse(null);
        if (state == null || state.dead() || !state.ownerId().equals(player.getUUID())) {
            sendTargetResult(player, payload, false, "You do not own this companion.");
            return;
        }
        if (living.level() != player.level() || living.distanceToSqr(player) > 8.0D * 8.0D
                || state.revision() != payload.expectedRevision()) {
            sendTargetResult(player, payload, false, "The companion settings changed; reopen the selector.");
            return;
        }
        TargetSelection selection = payload.selection();
        if (!validTargetSelection(level, selection)) {
            sendTargetResult(player, payload, false, "That target selection is invalid.");
            return;
        }
        CompanionAttachments.setTargetSelection(living, selection);
        CompanionState updated = state.withSettings(state.settings().withTargetFilter(
                selection.isLegacyHostileOnly() ? TargetFilter.HOSTILE_ONLY
                        : selection.isLegacyAllLiving() ? TargetFilter.ALL_LIVING : TargetFilter.CUSTOM));
        CompanionAttachments.set(living, updated);
        CompanionJournal.get(level).replaceEntity(updated, living.getUUID(), dimension(level), blockPos(living), level.getGameTime());
        sendSnapshot(player, living, updated, false);
        sendTargetResult(player, payload, true, "Targets saved.");
    }

    private static void sendTargetResult(ServerPlayer player, CompanionTargetSelectionPayload payload,
                                         boolean success, String message) {
        ServerPlayNetworking.send(player, new CompanionTargetSelectionResultPayload(payload.bondId(),
                success ? payload.expectedRevision() + 1L : payload.expectedRevision(), success, message));
    }

    private static boolean validTargetSelection(ServerLevel level, TargetSelection selection) {
        if (selection == null || !TargetSelection.isValidShape(selection.includes())
                || !TargetSelection.isValidShape(selection.excludes())) return false;
        for (String entry : selection.includes()) if (!validTargetEntry(level, entry)) return false;
        for (String entry : selection.excludes()) if (!validTargetEntry(level, entry)) return false;
        return true;
    }

    private static boolean validTargetEntry(ServerLevel level, String entry) {
        // Unknown type/tag ids are retained as visibly unavailable entries so a
        // world can be edited after a content mod is removed. Matcher resolution
        // treats them as false and therefore never broadens the target set.
        return TargetCatalog.isWellFormedEntry(entry);
    }

    public static void handleRosterAction(ServerPlayer player, CompanionRosterActionPayload payload) {
        CompanionJournal journal = CompanionJournal.get(player.level().getServer().overworld());
        BondRecord record = journal.get(payload.bondId());
        if (record != null && record.ownerId().equals(player.getUUID())
                && payload.action() == CompanionRosterActionPayload.Action.REMOVE) {
            journal.removeDead(record.bondId(), player.getUUID());
        } else if (record != null && record.ownerId().equals(player.getUUID()) && !record.dead()) {
            LivingEntity living = findBondEntity(player.level().getServer(), record, true);
            if (living != null && living.isAlive() && CompanionAttachments.get(living)
                    .map(state -> state.ownerId().equals(player.getUUID()) && !state.dead()).orElse(false)) {
                if (recallToOwner(living, player) && living.level() instanceof ServerLevel level) {
                    CompanionState state = CompanionAttachments.get(living).orElse(null);
                    if (state != null) journal.replaceEntity(state, living.getUUID(), dimension(level),
                            blockPos(living), level.getGameTime());
                }
            }
        }
        sendRoster(player);
    }

    private static void sendSnapshot(ServerPlayer player, LivingEntity entity, CompanionState state, boolean openScreen) {
        ServerPlayNetworking.send(player, new CompanionSnapshotPayload(
                CompanionMenuSnapshot.fromState(state, entity, cheatsAllowed(player)), openScreen));
    }

    private static void applyPresentation(LivingEntity entity, CompanionState state) {
        String base = state.displayName() == null || state.displayName().isBlank()
                ? state.creatureType() : state.displayName();
        boolean showName = state.settings().nameplate() == NameplateMode.ALWAYS;
        boolean showHealth = state.settings().healthDisplay() == HealthDisplayMode.ON;
        entity.setCustomName(Component.literal(showHealth
                ? base + " [" + String.format(java.util.Locale.ROOT, "%.1f/%.1f", entity.getHealth(), entity.getMaxHealth()) + "]"
                : base));
        entity.setCustomNameVisible(showName);
        CompanionAttachments.syncPresentation(entity, state);
    }

    private static void openInventory(ServerPlayer player, LivingEntity entity, CompanionState state) {
        CompanionEquipment.ensure(entity);
        CompanionEquipment.captureNative(entity, state.settings().useDurability());
        player.openMenu(new ExtendedMenuProvider<Integer>() {
            @Override public Component getDisplayName() {
                return Component.translatable("tameall.inventory");
            }
            @Override public Integer getScreenOpeningData(ServerPlayer ignored) { return entity.getId(); }
            @Override public AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory,
                                                               net.minecraft.world.entity.player.Player ignored) {
                return CompanionMenu.server(id, inventory, entity);
            }
        });
    }

    private static boolean isCheatAction(CompanionAction action, boolean value) {
        // Enabling survival-changing abilities is permission-gated; turning one
        // back off is always allowed so a non-cheat owner can remove an advantage
        // inherited from an older save or an administrator.
        return ((action == CompanionAction.SET_USE_DURABILITY && !value)
                || (value && action == CompanionAction.SET_SUNLIGHT_PROTECTION)
                || (value && action == CompanionAction.SET_DROWNING_PROTECTION)
                || (value && action == CompanionAction.SET_HABITAT_ADAPTATION)
                || (value && action == CompanionAction.SET_REUSABLE_EXPLOSIONS)
                || (value && action == CompanionAction.SET_PREVENT_VEX_EXPIRY)
                || (value && action == CompanionAction.SET_SAFE_SPECIAL_ABILITIES));
    }

    /** Permission level 2 (gamemaster) is the cheat gate used by vanilla commands. */
    private static boolean cheatsAllowed(ServerPlayer player) {
        return player.permissions() instanceof LevelBasedPermissionSet permissions
                && permissions.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS);
    }

    /** Finds an existing entity, loading its recorded chunk when the owner asks via the whistle. */
    private static LivingEntity findBondEntity(net.minecraft.server.MinecraftServer server, BondRecord record,
                                               boolean loadRecordedChunk) {
        if (record.entityId() == null) return null;
        Entity found;
        for (ServerLevel level : server.getAllLevels()) {
            // UUIDs are server-global, so a loaded entity that moved before
            // its journal tick must still be found. Only chunk loading is
            // constrained to the recorded dimension and position.
            if (loadRecordedChunk && record.dimension() != null
                    && !record.dimension().equals(dimension(level))) continue;
            if (loadRecordedChunk && record.position() != null) {
                BlockPos position = new BlockPos(record.position().x(), record.position().y(), record.position().z());
                // getChunkAt synchronously loads the recorded source chunk and lets
                // vanilla's entity manager restore the original UUID; no replacement
                // entity is ever created here.
                level.getChunkAt(position);
            }
            found = level.getEntity(record.entityId());
            if (found instanceof LivingEntity living) return living;
            for (Entity candidate : level.getAllEntities()) {
                if (record.entityId().equals(candidate.getUUID()) && candidate instanceof LivingEntity living) return living;
            }
        }
        return null;
    }

    private static boolean recallToOwner(LivingEntity living, ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel targetLevel)) return false;
        if (living.level() != targetLevel) {
            if (!living.teleportTo(targetLevel, owner.getX(), owner.getY(), owner.getZ(),
                    Collections.emptySet(), owner.getYRot(), owner.getXRot(), false)) return false;
        }
        return safeCatchUp(targetLevel, living, owner);
    }

    private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand,
                                                  Entity target, EntityHitResult hit) {
        if (!(target instanceof LivingEntity living) || target instanceof Player) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        boolean golden = held.getItem() == ModItems.GOLDEN_WHEAT;
        boolean gilded = held.getItem() == ModItems.GILDED_WHEAT;
        if ((golden || gilded) && !CompanionAttachments.get(target).isPresent() && !player.isCrouching()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (!(level instanceof ServerLevel server) || held.isEmpty()) return InteractionResult.PASS;
            // Older builds could lose the entity attachment when an empty item
            // slot failed to encode. Recover the original bond by UUID before
            // allowing a new bond to be created.
            BondRecord recorded = CompanionJournal.get(server).findLiveByEntity(target.getUUID());
            if (recorded != null) {
                CompanionJournal journal = CompanionJournal.get(server);
                if (recorded.ownerId().equals(player.getUUID())
                        && !journal.hasLiveOwnerConflict(target.getUUID(), player.getUUID())) {
                    CompanionState recovered = restoreFromJournal(living, server, recorded);
                    if (recovered != null) return InteractionResult.SUCCESS;
                }
                // A live journal entry belongs to somebody else. Never let a
                // second player create a duplicate bond for this entity.
                return InteractionResult.SUCCESS;
            }
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
            CompanionState existing = CompanionAttachments.get(target).orElse(null);
            BondingResult result = CompanionBondingService.bond(player.getUUID(), type, living.isAlive(),
                    target.getType().builtInRegistryHolder().is(TamingPolicy.EXCLUDED),
                    existing == null ? null : existing.ownerId(), dimension(server), blockPos(target));
            if (!result.succeeded()) return InteractionResult.SUCCESS;
            if (gilded && server.getRandom().nextInt(6) != 0) {
                held.consume(1, player);
                // The failed Gilded Wheat attempt is deliberately visible but
                // has no bond side effects, just like a failed wolf taming try.
                server.sendParticles(ParticleTypes.SMOKE, living.getX(), living.getY() + living.getBbHeight() * 0.7D,
                        living.getZ(), 5, 0.25D, 0.25D, 0.25D, 0.01D);
                return InteractionResult.SUCCESS;
            }
            if (result.succeeded()) {
                CompanionState state = result.state();
                if (living.getCustomName() != null) state = state.rename(living.getCustomName().getString());
                CompanionAttachments.set(target, state);
                CompanionEquipment.ensure(living); living.skipDropExperience(); applyPresentation(living, state);
                if (target instanceof Mob mob) {
                    mob.setPersistenceRequired();
                    CompanionCombat.clearManagedTarget(mob);
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }
                CompanionAttachments.setTargetSelection(target, TargetSelection.hostileOnly());
                held.consume(1, player); showHearts(server, living);
                CompanionJournal.get(server).replaceEntity(state, target.getUUID(), dimension(server), blockPos(target), server.getGameTime());
                awardFirstTame(player);
            }
            return InteractionResult.SUCCESS;
        }
        CompanionState state = CompanionAttachments.get(target).orElse(null);
        if (state != null && player.getUUID().equals(state.ownerId()) && (golden || gilded) && !player.isCrouching()) {
            if (!level.isClientSide() && level instanceof ServerLevel server) {
                if (living.getHealth() < living.getMaxHealth()) {
                    float before = living.getHealth();
                    float amount = golden ? 8.0F : 4.0F;
                    living.heal(Math.min(amount, living.getMaxHealth() - living.getHealth()));
                    held.consume(1, player);
                    if (living.getHealth() > before && living.getHealth() >= living.getMaxHealth()) {
                        showHearts(server, living);
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (state != null && player.getUUID().equals(state.ownerId()) && player.isCrouching()) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) menuOpener.accept(serverPlayer, living);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Restores a bond whose attachment was omitted by an older invalid save. */
    private static CompanionState restoreFromJournal(LivingEntity living, ServerLevel level,
                                                      BondRecord recorded) {
        if (recorded.dead() || !recorded.entityId().equals(living.getUUID())) return null;
        CompanionState state = recorded.snapshot().markAlive();
        if (!state.ownerId().equals(recorded.ownerId())) return null;
        CompanionAttachments.set(living, state);
        if (living instanceof Mob mob) {
            mob.setPersistenceRequired();
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        CompanionEquipment.ensure(living);
        if (CompanionAttachments.getTargetSelection(living).isEmpty()) {
            CompanionAttachments.setTargetSelection(living, TargetSelection.fromLegacy(state.settings().targetFilter()));
        }
        CompanionEquipment.captureNative(living, state.settings().useDurability());
        living.skipDropExperience();
        applyPresentation(living, state);
        CompanionJournal.get(level).replaceEntity(state, living.getUUID(), dimension(level),
                blockPos(living), level.getGameTime());
        return state;
    }

    private static void onEntityLoad(Entity entity, ServerLevel level) {
        if (!(entity instanceof LivingEntity living)) return;
        if (entity instanceof Mob mob) CompanionCombat.clearManagedTarget(mob);
        CompanionJournal journal = CompanionJournal.get(level);
        CompanionState state = CompanionAttachments.get(entity).orElse(null);
        if (state == null) {
            BondRecord recorded = journal.findLiveByEntity(entity.getUUID());
            if (recorded != null && !journal.hasLiveOwnerConflict(entity.getUUID(), recorded.ownerId())) {
                state = restoreFromJournal(living, level, recorded);
            }
        }
        if (state == null || state.dead()) return;
        if (entity instanceof Mob mob) mob.setPersistenceRequired();
        CompanionEquipment.ensure(living);
        if (CompanionAttachments.getTargetSelection(living).isEmpty()) {
            CompanionAttachments.setTargetSelection(living, TargetSelection.fromLegacy(state.settings().targetFilter()));
        }
        // Native entity data is the source of truth on load; the attachment is
        // only the persistent mirror used by the menu and journal.
        CompanionEquipment.captureNative(living, state.settings().useDurability());
        living.skipDropExperience();
        applyPresentation(living, state);
        journal.replaceEntity(state, entity.getUUID(), dimension(level), blockPos(entity), level.getGameTime());
    }

    private static void onEntityUnload(Entity entity, ServerLevel level) {
        if (!(entity instanceof LivingEntity living)) return;
        if (entity instanceof Mob mob) CompanionCombat.clearManagedTarget(mob);
        CompanionAttachments.get(living).ifPresent(state -> {
            if (!state.dead()) {
                CompanionJournal.get(level).markUnloaded(living.getUUID(), dimension(level), blockPos(living),
                        level.getGameTime());
            }
        });
    }

    /** Called by Entity#setRemoved; permanent removal is reconciled at end of tick. */
    public static void onEntityRemoved(Entity entity, RemovalReason reason) {
        if (!(entity instanceof LivingEntity) || !(entity.level() instanceof ServerLevel level)) return;
        if (reason == RemovalReason.UNLOADED_TO_CHUNK || reason == RemovalReason.UNLOADED_WITH_PLAYER
                || reason == RemovalReason.CHANGED_DIMENSION) return;
        if (CompanionAttachments.get(entity).map(state -> !state.dead()).orElse(false)) {
            PENDING_REMOVALS.put(entity.getUUID(), new PendingRemoval(entity, level, reason, level.getGameTime()));
        }
    }

    private static void processPendingRemovals(ServerLevel level) {
        var iterator = PENDING_REMOVALS.entrySet().iterator();
        while (iterator.hasNext()) {
            PendingRemoval pending = iterator.next().getValue();
            if (pending.level() != level || pending.tick() > level.getGameTime()) continue;
            iterator.remove();
            Entity entity = pending.entity();
            CompanionState state = CompanionAttachments.get(entity).orElse(null);
            if (state == null || state.dead() || !entity.isRemoved()) continue;
            boolean replacement = hasLiveBondEntity(level.getServer(), state.bondId(), entity.getUUID());
            if (!replacement) handleCompanionDeath((LivingEntity) entity, level);
        }
    }

    /**
     * A conversion can remove the old entity and publish its replacement in a
     * later callback.  Reconcile by bond id across all loaded levels before
     * marking the old attachment dead; the old entity UUID must not be reused.
     */
    private static boolean hasLiveBondEntity(net.minecraft.server.MinecraftServer server,
                                             UUID bondId, UUID excludedEntityId) {
        for (ServerLevel other : server.getAllLevels()) {
            for (Entity candidate : other.getAllEntities()) {
                if (!(candidate instanceof LivingEntity living)
                        || candidate.getUUID().equals(excludedEntityId)) continue;
                if (CompanionAttachments.get(living)
                        .map(state -> state.bondId().equals(bondId) && !state.dead() && living.isAlive())
                        .orElse(false)) return true;
            }
        }
        return false;
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        handleCompanionDeath(entity, level);
    }

    /** Idempotent cleanup shared by ordinary deaths and Creeper#explodeCreeper. */
    public static void handleCompanionDeath(LivingEntity entity, ServerLevel level) {
        CompanionAttachments.get(entity).ifPresent(state -> {
            if (state.dead()) return;
            if (entity instanceof Mob mob) CompanionCombat.clearManagedTarget(mob);
            CompanionEquipment.captureNative(entity, state.settings().useDurability());
            CompanionState dead = CompanionLifecycle.died(state);
            CompanionAttachments.set(entity, dead);
            CompanionEquipment.dropAndClear(entity, level);
            CompanionInventory.dropAndClear(entity, level);
            entity.skipDropExperience();
            CompanionJournal.get(level).markDead(dead, dimension(level), blockPos(entity), level.getGameTime());
        });
    }

    /** Called before vanilla discards a Creeper after its lethal explosion. */
    public static void handleLethalCreeperExplosion(Creeper creeper) {
        if (creeper.level() instanceof ServerLevel level) handleCompanionDeath(creeper, level);
    }

    /**
     * Replaces a bonded Creeper's safe-abilities explosion with a reusable,
     * entity-only blast. Returning false leaves vanilla's lethal path intact.
     */
    public static boolean handleSafeCreeperExplosion(Creeper creeper) {
        CompanionState state = CompanionAttachments.get(creeper).orElse(null);
        if (state == null || state.dead() || !state.settings().magic().reusableExplosions()) return false;
        if (!(creeper.level() instanceof ServerLevel level)) return true;
        long now = level.getGameTime();
        if (CompanionAttachments.getCreeperCooldown(creeper).map(cooldown -> now < cooldown.readyAt()).orElse(false)) {
            creeper.setSwellDir(-1);
            return true;
        }
        Player owner = level.getPlayerInAnyDimension(state.ownerId());
        if (owner == null || owner.level() != level) {
            creeper.setSwellDir(-1);
            CompanionCombat.clearManagedTarget(creeper);
            creeper.setTarget(null);
            return true;
        }
        float radius = creeper.isPowered() ? 6.0F : 3.0F;
        LivingEntity directTarget = creeper.getTarget();
        Set<UUID> permitted = creeperBlastTargets(level, creeper, state, owner, radius);
        if (permitted.isEmpty()) {
            creeper.setSwellDir(-1);
            CompanionCombat.clearManagedTarget(creeper);
            creeper.setTarget(null);
            return true;
        }
        ExplosionDamageCalculator calculator = new ExplosionDamageCalculator() {
            @Override public boolean shouldBlockExplode(net.minecraft.world.level.Explosion explosion,
                                                        net.minecraft.world.level.BlockGetter getter,
                                                        net.minecraft.core.BlockPos pos,
                                                        net.minecraft.world.level.block.state.BlockState block,
                                                        float power) { return false; }

            @Override public boolean shouldDamageEntity(net.minecraft.world.level.Explosion explosion, Entity entity) {
                return entity instanceof LivingEntity && permitted.contains(entity.getUUID());
            }

            @Override public float getKnockbackMultiplier(Entity entity) {
                return permitted.contains(entity.getUUID()) ? 1.0F : 0.0F;
            }
        };
        DamageSource blastSource = level.damageSources().explosion(creeper, creeper);
        Map<UUID, Float> healthBefore = new HashMap<>();
        for (UUID id : permitted) {
            Entity candidate = entityByUuid(level, id);
            if (candidate == null && directTarget != null && id.equals(directTarget.getUUID())) candidate = directTarget;
            if (candidate instanceof LivingEntity living) healthBefore.put(id, living.getHealth());
        }
        CompanionCombat.beginExplicitDamage(creeper, permitted);
        try {
            level.explode(creeper, blastSource, calculator,
                    creeper.getX(), creeper.getY(), creeper.getZ(), radius, false, Level.ExplosionInteraction.NONE);
            // A custom entity-only calculator can still receive a zero visibility
            // result inside a dense test/building. Ensure every explicitly selected
            // target receives the reusable blast's damage without touching blocks.
            for (Map.Entry<UUID, Float> entry : healthBefore.entrySet()) {
                Entity candidate = entityByUuid(level, entry.getKey());
                if (candidate == null && directTarget != null && entry.getKey().equals(directTarget.getUUID())) candidate = directTarget;
                if (candidate instanceof LivingEntity living && living.isAlive()
                        && living.getHealth() >= entry.getValue()) {
                    living.hurtServer(level, blastSource, Math.max(1.0F, radius * 2.0F));
                }
            }
        } finally {
            CompanionCombat.endExplicitDamage(creeper);
        }
        CompanionAttachments.setCreeperCooldown(creeper, new CreeperCooldown(now + 60L));
        creeper.setSwellDir(-1);
        CompanionCombat.clearManagedTarget(creeper);
        creeper.setTarget(null);
        return true;
    }

    private static Set<UUID> creeperBlastTargets(ServerLevel level, Creeper creeper,
                                                  CompanionState state, Player owner, float radius) {
        Set<UUID> targets = new HashSet<>();
        AABB area = creeper.getBoundingBox().inflate(radius * 1.25D);
        boolean assist = state.settings().stance() == CombatStance.ASSIST;
        LivingEntity directTarget = creeper.getTarget();
        boolean directSignal = assist && directTarget != null
                && (directTarget == owner.getLastHurtMob()
                || directTarget == owner.getLastHurtByMob()
                || directTarget == creeper.getLastHurtByMob());
        if (directTarget != null && directTarget.isAlive()
                && area.contains(directTarget.getX(), directTarget.getY(), directTarget.getZ())
                && isLegalTarget(level, creeper, state, owner, directTarget, directSignal)
                && canEngageFromStay(creeper, state, directTarget)) {
            targets.add(directTarget.getUUID());
        }
        for (Entity raw : level.getAllEntities()) {
            if (!(raw instanceof LivingEntity candidate) || !candidate.isAlive() || candidate == creeper
                    || !area.contains(candidate.getX(), candidate.getY(), candidate.getZ())) continue;
            boolean signal = assist && (candidate == owner.getLastHurtMob()
                    || candidate == owner.getLastHurtByMob() || candidate == creeper.getLastHurtByMob());
            if (isLegalTarget(level, creeper, state, owner, candidate, signal)
                    && canEngageFromStay(creeper, state, candidate)) targets.add(candidate.getUUID());
        }
        return targets;
    }

    private static Entity entityByUuid(ServerLevel level, UUID id) {
        Entity entity = level.getEntity(id);
        if (entity != null) return entity;
        for (Entity candidate : level.getAllEntities()) {
            if (id.equals(candidate.getUUID())) return candidate;
        }
        return null;
    }

    private static void onConversion(Mob oldMob, Mob newMob, net.minecraft.world.entity.ConversionParams params) {
        CompanionAttachments.get(oldMob).ifPresent(state -> {
            CompanionEquipment.captureNative(oldMob, state.settings().useDurability());
            String type = BuiltInRegistries.ENTITY_TYPE.getKey(newMob.getType()).toString();
            CompanionState converted = CompanionLifecycle.converted(state, type);
            CompanionAttachments.set(newMob, converted); CompanionEquipment.ensure(newMob);
            CompanionAttachments.getTargetSelection(oldMob).ifPresentOrElse(
                    value -> CompanionAttachments.setTargetSelection(newMob, value),
                    () -> CompanionAttachments.setTargetSelection(newMob,
                            TargetSelection.fromLegacy(converted.settings().targetFilter())));
            CompanionEquipment.apply(newMob, CompanionEquipment.read(oldMob));
            CompanionInventory.write(newMob, CompanionInventory.read(oldMob)); applyPresentation(newMob, converted);
            if (newMob.level() instanceof ServerLevel level) {
                newMob.setPersistenceRequired();
                CompanionJournal.get(level).replaceEntity(converted, newMob.getUUID(), dimension(level), blockPos(newMob), level.getGameTime());
            }
            CompanionEquipment.write(oldMob, List.of()); CompanionInventory.write(oldMob, List.of()); CompanionAttachments.clear(oldMob);
        });
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        CompanionState state = CompanionAttachments.get(entity).orElse(null);
        Entity attacker = source.getEntity();
        if (attacker instanceof Projectile projectile && projectile.getOwner() != null) {
            attacker = projectile.getOwner();
        }
        if (attacker != null) {
            if (attacker instanceof Mob attackerMob
                    && (CompanionCombat.rejectsAsTarget(attackerMob, entity)
                    || CompanionCombat.rejectsAttack(attackerMob, entity))) return false;
            CompanionState attackerState = CompanionAttachments.get(attacker).orElse(null);
            if (attackerState != null && (attackerState.ownerId().equals(entity.getUUID()) || state != null)) return false;
        }
        if (state == null) return true;
        if (state.settings().magic().drowningProtection() && source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)) return false;
        return true;
    }

    private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage,
                                    float damageTaken, boolean blocked) {
        if (blocked || damageTaken <= 0.0F) return;
        CompanionCombat.recordGolemProvocation(entity, source.getEntity(), damageTaken);
    }

    private static void showHearts(ServerLevel level, LivingEntity entity) {
        level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getBbHeight() * 0.72D,
                entity.getZ(), 7, Math.min(0.45D, entity.getBbWidth() * 0.35D), 0.35D,
                Math.min(0.45D, entity.getBbWidth() * 0.35D), 0.02D);
    }

    private static void awardFirstTame(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.level().getServer() == null) return;
        var advancement = player.level().getServer().getAdvancements()
                .get(net.minecraft.resources.Identifier.fromNamespaceAndPath("tameall", "we_come_in_peace"));
        if (advancement != null) serverPlayer.getAdvancements().award(advancement, "tame");
    }

    private static void tickLevel(ServerLevel level) {
        processPendingRemovals(level);
        CompanionExperience.tick(level);
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living) CompanionAttachments.get(living).ifPresent(state -> tickCompanion(level, living, state));
        }
    }

    private static void prepareLevel(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            CompanionCombat.refreshSummonAffiliation(living);
            if (living instanceof Mob mob) CompanionCombat.sanitize(mob);
            CompanionState state = CompanionAttachments.get(living).orElse(null);
            if (state == null || state.dead()) continue;
            MagicToggles magic = state.settings().magic();
            if (magic.drowningProtection() && living.isInWater()) {
                living.setAirSupply(living.getMaxAirSupply());
                if (level.getGameTime() % 10L == 0L) level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE,
                        living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(), 3, 0.25D, 0.25D, 0.25D, 0.01D);
            }
            protectSpecialAbility(living, magic);
        }
    }

    private static void protectSpecialAbility(LivingEntity living, MagicToggles magic) {
        if (living instanceof Vex vex) {
            if (magic.preventVexExpiry()) {
                VexLifetime lifetime = CompanionAttachments.getVexLifetime(vex).orElse(null);
                if (lifetime == null && vex instanceof dev.riqvip.tameall.mixin.VexLifetimeAccess access) {
                    int remaining = access.tameall$limitedLifeTicks();
                    // A legacy save may already contain the old aggregate
                    // protection's Integer.MAX_VALUE sentinel. Restore the
                    // normal two-minute native lifetime when that sentinel is
                    // later disabled.
                    if (remaining >= Integer.MAX_VALUE / 2) remaining = 1200;
                    lifetime = new VexLifetime(access.tameall$hasLimitedLife(),
                            Math.max(0, remaining));
                    CompanionAttachments.setVexLifetime(vex, lifetime);
                }
                vex.setLimitedLife(Integer.MAX_VALUE);
            } else {
                restoreVexLifetime(vex);
            }
        }
    }

    private static void restoreVexLifetime(Vex vex) {
        VexLifetime lifetime = CompanionAttachments.getVexLifetime(vex).orElse(null);
        if (lifetime == null || !(vex instanceof dev.riqvip.tameall.mixin.VexLifetimeAccess access)) return;
        access.tameall$setHasLimitedLife(lifetime.hadLimitedLife());
        access.tameall$setLimitedLifeTicks(lifetime.remainingTicks());
        CompanionAttachments.clearVexLifetime(vex);
    }

    private static void tickCompanion(ServerLevel level, LivingEntity entity, CompanionState state) {
        if (state.dead() || !entity.isAlive()) return;
        CompanionEquipment.captureNative(entity, state.settings().useDurability());
        if (state.settings().pickupItems()) collectNearbyItems(level, entity, state.settings().pickupRadius());
        if (entity instanceof Mob mob) {
            // This is deliberately repeated here as a server-side safety net;
            // MobMixin/BrainMixin enforce the same rule during native AI.
            CompanionCombat.sanitize(mob);
        }
        Player owner = level.getPlayerInAnyDimension(state.ownerId());
        if (owner == null) {
            if (entity instanceof Mob mob) {
                CompanionCombat.clearManagedTarget(mob);
                mob.setTarget(null);
            }
            return;
        }
        if (entity instanceof Mob mob) {
            applyMovement(level, mob, state, owner);
            updateCombatTarget(level, mob, state, owner);
            CompanionCombat.sanitize(mob);
            updateShieldUse(mob);
        }
        applyPresentation(entity, state);
        CompanionJournal.get(level).replaceEntity(state, entity.getUUID(), dimension(level), blockPos(entity), level.getGameTime());
    }

    private static void applyMovement(ServerLevel level, Mob mob, CompanionState state, Player owner) {
        CompanionMode mode = state.settings().mode();
        if (mode == CompanionMode.FOLLOW) {
            if (mob.getPose() == Pose.SITTING) mob.setPose(Pose.STANDING);
            if (owner.level() != level && owner.level() instanceof ServerLevel targetLevel) {
                if (state.settings().teleportMode() != TeleportMode.NEVER) {
                    mob.teleportTo(targetLevel, owner.getX(), owner.getY(), owner.getZ(),
                            Collections.emptySet(), owner.getYRot(), owner.getXRot(), false);
                }
                return;
            }
            double radius = state.settings().guardRadius();
            double distance = mob.distanceToSqr(owner);
            boolean teleport = switch (state.settings().teleportMode()) {
                case OUTSIDE_AREA -> distance > radius * radius * FOLLOW_TELEPORT_MULTIPLIER * FOLLOW_TELEPORT_MULTIPLIER;
                case FAR_FROM_OWNER -> distance > TeleportPolicy.FAR_DISTANCE_SQUARED;
                case NEVER -> false;
            };
            if (teleport) safeCatchUp(level, mob, owner);
            else if (distance > radius * radius) mob.getNavigation().moveTo(owner, 1.15D);
            return;
        }
        BlockPoint anchor = state.anchor();
        if (anchor == null || !dimension(level).equals(state.anchorDimension())) return;
        if (mode == CompanionMode.STAY) {
            mob.getNavigation().stop();
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            mob.setPose(Pose.SITTING);
            double dx = mob.getX() - (anchor.x() + 0.5D), dz = mob.getZ() - (anchor.z() + 0.5D);
            if (dx * dx + dz * dz > 2.25D) {
                mob.setPos(anchor.x() + 0.5D, anchor.y(), anchor.z() + 0.5D);
                mob.setOnGround(true);
            }
        } else {
            if (mob.getPose() == Pose.SITTING) mob.setPose(Pose.STANDING);
            guard(level, mob, state);
        }
    }

    private static void guard(ServerLevel level, Mob mob, CompanionState state) {
        BlockPoint anchor = state.anchor();
        double dx = mob.getX() - (anchor.x() + 0.5D), dz = mob.getZ() - (anchor.z() + 0.5D);
        double radius = state.settings().guardRadius();
        if (dx * dx + dz * dz > radius * radius) {
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            mob.getNavigation().moveTo(anchor.x() + 0.5D, anchor.y(), anchor.z() + 0.5D, 1.1D);
        } else if (mob.getTarget() == null && mob.getNavigation().isDone()
                && mob instanceof PathfinderMob pathfinder && level.getGameTime() % 80L == 0L) {
            Vec3 candidate = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPos(pathfinder,
                    (int) Math.min(radius, 16.0D), 5);
            if (candidate != null && inCircle(candidate.x, anchor.x() + 0.5D, candidate.z, anchor.z() + 0.5D, radius)) {
                pathfinder.getNavigation().moveTo(candidate.x, candidate.y, candidate.z, 0.8D);
            }
        }
        if (level.getGameTime() % 20L == 0L) {
            for (int i = 0; i < 16; i++) {
                double angle = Math.PI * 2.0D * i / 16.0D;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        anchor.x() + 0.5D + Math.cos(angle) * radius, anchor.y() + 0.15D,
                        anchor.z() + 0.5D + Math.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void updateCombatTarget(ServerLevel level, Mob mob, CompanionState state, Player owner) {
        CombatStance stance = state.settings().stance();
        LivingEntity current = mob.getTarget();
        if (current != null && (!isLegalTarget(level, mob, state, owner, current, false)
                || !canEngageFromStay(mob, state, current))) {
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            LOST_SIGHT_AT.remove(mob.getUUID());
        } else if (current != null && !mob.hasLineOfSight(current)) {
            long lostAt = LOST_SIGHT_AT.computeIfAbsent(mob.getUUID(), ignored -> level.getGameTime());
            if (level.getGameTime() - lostAt > LOST_SIGHT_PURSUIT_TICKS) {
                CompanionCombat.clearManagedTarget(mob);
                mob.setTarget(null);
                LOST_SIGHT_AT.remove(mob.getUUID());
            }
        } else if (current != null) {
            LOST_SIGHT_AT.remove(mob.getUUID());
        }
        if (stance == CombatStance.PASSIVE || owner.level() != level) {
            CompanionCombat.clearManagedTarget(mob);
            mob.setTarget(null);
            return;
        }
        if (stance == CombatStance.ASSIST) {
            LivingEntity[] signals = { owner.getLastHurtMob(), owner.getLastHurtByMob(), mob.getLastHurtByMob() };
            for (LivingEntity candidate : signals) {
                if (candidate != null && isLegalTarget(level, mob, state, owner, candidate, true)
                        && canEngageFromStay(mob, state, candidate)) {
                    CompanionCombat.assignTarget(mob, candidate); return;
                }
            }
            return;
        }
        double radius = state.settings().guardRadius();
        long scanInterval = radius > 64.0D ? 5L : 1L;
        if ((Math.floorMod(mob.getUUID().hashCode(), (int) scanInterval)
                != Math.floorMod((int) level.getGameTime(), (int) scanInterval))) return;
        double[] center = operatingCenter(mob, state, owner);
        AABB area = new AABB(center[0] - radius, mob.getY() - radius, center[1] - radius,
                center[0] + radius, mob.getY() + radius, center[1] + radius);
        for (LivingEntity candidate : level.getEntities(EntityTypeTest.forClass(LivingEntity.class), area,
                value -> value.isAlive() && inCircle(value, center[0], center[1], radius))) {
            if (isLegalTarget(level, mob, state, owner, candidate, false)) {
                if (!canEngageFromStay(mob, state, candidate)) continue;
                if (!canAcquireTarget(mob, candidate)) continue;
                CompanionCombat.assignTarget(mob, candidate); return;
            }
        }
        CompanionCombat.clearManagedTarget(mob);
        mob.setTarget(null);
    }

    private static boolean isLegalTarget(ServerLevel level, Mob mob, CompanionState state, Player owner,
                                         LivingEntity candidate, boolean signal) {
        if (CompanionCombat.rejectsAsTarget(mob, candidate)
                || candidate == owner || !candidate.isAlive()
                || owner.isAlliedTo(candidate) || candidate.isAlliedTo(owner)) return false;
        if (candidate instanceof Player targetPlayer && !owner.canHarmPlayer(targetPlayer)) return false;
        if (CompanionAttachments.get(candidate).isPresent()) return false;
        if (!inOperatingArea(mob, state, owner, candidate)) return false;
        if (state.settings().stance() == CombatStance.PASSIVE) return false;
        if (state.settings().stance() == CombatStance.ASSIST) return signal;
        if (signal) return true;
        TargetSelection selection = CompanionAttachments.getTargetSelection(mob)
                .orElseGet(() -> TargetSelection.fromLegacy(state.settings().targetFilter()));
        return TargetMatcher.matches(level, candidate, selection);
    }

    private static boolean canAcquireTarget(Mob mob, LivingEntity candidate) {
        if (!mob.hasLineOfSight(candidate)) return false;
        if (mob instanceof RangedAttackMob) return true;
        if (mob.distanceToSqr(candidate) <= 16.0D * 16.0D) return true;
        return mob.getNavigation().createPath(candidate, 0) != null;
    }

    private static boolean inOperatingArea(Mob mob, CompanionState state, Player owner, LivingEntity target) {
        double[] center = operatingCenter(mob, state, owner);
        return inCircle(target, center[0], center[1], state.settings().guardRadius());
    }

    private static boolean canEngageFromStay(Mob mob, CompanionState state, LivingEntity target) {
        return state.settings().mode() != CompanionMode.STAY || mob.distanceToSqr(target) <= 8.0D * 8.0D;
    }

    private static double[] operatingCenter(Mob mob, CompanionState state, Player owner) {
        if (state.settings().mode() == CompanionMode.FOLLOW && owner.level() == mob.level()) return new double[] { owner.getX(), owner.getZ() };
        BlockPoint anchor = state.anchor();
        return anchor == null ? new double[] { mob.getX(), mob.getZ() } : new double[] { anchor.x() + 0.5D, anchor.z() + 0.5D };
    }

    private static boolean inCircle(LivingEntity entity, double x, double z, double radius) {
        return inCircle(entity.getX(), x, entity.getZ(), z, radius);
    }

    private static boolean inCircle(double entityX, double x, double entityZ, double z, double radius) {
        double dx = entityX - x, dz = entityZ - z;
        return dx * dx + dz * dz <= radius * radius;
    }

    private static void updateShieldUse(Mob mob) {
        ItemStack shield = mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        if (shield.is(net.minecraft.world.item.Items.SHIELD) && mob.getTarget() != null
                && mob.distanceToSqr(mob.getTarget()) <= 12.0D * 12.0D) {
            if (!mob.isUsingItem()) mob.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
        } else if (mob.isUsingItem() && mob.getUsedItemHand() == net.minecraft.world.InteractionHand.OFF_HAND) mob.stopUsingItem();
    }

    private static void collectNearbyItems(ServerLevel level, LivingEntity entity, double radius) {
        AABB area = entity.getBoundingBox().inflate(radius, radius * 0.75D, radius);
        List<ItemEntity> items = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), area,
                item -> item.isAlive() && !item.hasPickUpDelay() && !item.getItem().isEmpty());
        for (ItemEntity item : items) {
            ItemStack before = item.getItem().copy();
            ItemStack remainder = CompanionInventory.insert(entity, before);
            int accepted = before.getCount() - remainder.getCount();
            if (accepted > 0) {
                entity.onItemPickup(item);
                entity.take(item, accepted);
            }
            if (remainder.isEmpty()) item.discard();
            else item.setItem(remainder);
        }
    }

    private static boolean placeNearOwner(ServerLevel level, LivingEntity entity, Player owner) { return safeCatchUp(level, entity, owner); }

    private static boolean safeCatchUp(ServerLevel level, LivingEntity entity, Player owner) {
        BlockPos base = owner.blockPosition();
        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) for (int dy = -1; dy <= 2; dy++) {
                BlockPos candidate = base.offset(dx, dy, dz);
                if (!level.isLoaded(candidate) || !level.loadedAndEntityCanStandOn(candidate.below(), entity)) continue;
                double x = candidate.getX() + 0.5D, y = candidate.getY(), z = candidate.getZ() + 0.5D;
                AABB moved = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ());
                if (level.noCollision(entity, moved)) {
                    entity.setPos(x, y, z); entity.setOnGround(true);
                    if (entity instanceof Mob mob) mob.getNavigation().stop();
                    return true;
                }
            }
        }
        return false;
    }

    private static String dimension(ServerLevel level) { return level.dimension().identifier().toString(); }

    private static BlockPoint blockPos(Entity entity) {
        BlockPos position = entity.blockPosition();
        return new BlockPoint(position.getX(), position.getY(), position.getZ());
    }
}
