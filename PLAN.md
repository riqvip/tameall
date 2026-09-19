# TameAll implementation record

This document records the approved feature contract and the checks run for the
Minecraft Java 26.2 implementation.

## Approved behavior

Golden Wheat is the guaranteed bonding item: one wheat plus eight gold ingots.
Gilded Wheat is the lower-cost one-in-six bonding attempt: one wheat plus eight
gold nuggets. A failed Gilded Wheat attempt consumes the item and emits smoke;
successful bonding consumes the item, displays hearts, persists the UUID bond,
and awards `tameall:we_come_in_peace`. Golden Wheat heals an injured owned
companion by up to 8 health; Gilded Wheat heals up to 4. Neither item is
consumed when the companion is already full. Hearts are emitted only for a
successful bond or when an injured companion reaches full health; feeding a
full companion emits no hearts. Recipe advancements unlock both
wheat recipes from wheat and the whistle recipe from Golden Wheat.

Vanilla's own tame state stays distinct from TameAll bonding. Naturally tamed
wolves, cats, parrots, nautiluses, and horse-family mobs are left to vanilla
and cannot also be bonded with Golden Wheat. A Golden Wheat bond blocks the
corresponding native taming transition, while crouch interaction and ordinary
feeding and equipment behavior remain available.

Movement uses one circular operating area, shared by Follow, Guard, and Defend
Area. The radius is clamped to 4..999 (default 16). Follow centers on the owner;
Guard centers on its saved anchor and roams inside the circle; Stay holds its
anchor. Only loaded entities are considered and no routine behavior forces a
chunk to load. Whistle recall may load the journaled source chunk, then moves
the existing entity safely near its owner.

Combat is explicit and independent of movement. Passive clears targets. Assist
responds to owner or companion combat signals and may follow those signals
outside the proactive target list. Defend Area scans visible living entities in
the operating circle. Its target selection stores bounded include and exclude
sets; includes use OR, exclusions win, and the server rejects malformed sets.
The catalog contains groups, dynamic entity tags, and registered entity types.
The groups include Hostile, All living, Players, Illagers, Raiders, Undead,
Zombies, Skeletons, Aquatic, and Arthropods. Raiders is the broader pillager
family: evoker, illusioner, pillager, vindicator, ravager, and witch; Illagers
exclude ravager and witch. Removed content leaves unknown saved IDs visible as
unavailable, where matching always returns false. New targets require line of
sight and either ranged capability, close reach, or a path. Existing targets
remain eligible for 60 ticks after sight loss.

The owner, all bonded companions, allied/protected entities, and friendly vex
summons are hard-protected at target acquisition, target setters, Brain memory,
anger state, cached attack goals, and damage time. Once a mob is bonded, every
native species target and attack path is blocked until the companion runtime
assigns an explicit target for Assist or Defend Area. This covers Fox prey,
golem targets, pillager villagers, neutral anger, Brain behaviors, custom attack
goals, and projectile damage. Golems are neutral to a bond
until the owner or one of that owner's pets deals real damage; that provocation
is persisted for 200 ticks. Projectile sources resolve to their shooter, so a
ranged companion provokes a golem exactly like a melee companion. Bonded
illagers can attack wild illagers. Vexes spawned by a bonded evoker inherit
owner protection without becoming roster entries; a vex intentionally bonded
with wheat is independent.

New bonds use opt-in survival adaptations. Sunlight protection, drowning
protection, reusable Creeper explosions, and Vex-expiry protection default to
off. Enabling them and item-durability immunity requires permission level 2;
turning them off is always permitted. A Creeper with reusable explosions off
follows vanilla's lethal, block-damaging explosion and is marked dead before
discard; with it on, the custom explosion has no block damage, obeys the target
selection, and has a cooldown. Enderman block pickup defaults on and can be
disabled without permission; disabling only prevents future block removal.
Legacy `habitat_adaptation` is read as drowning protection, legacy
`safe_special_abilities` is read as reusable explosions plus Vex-expiry
protection, and legacy safe=true also disables Enderman block pickup. Existing
sunlight and habitat values are preserved when present.

Collect XP for Mending is independent and defaults off. It scans loaded XP orbs
and active companions with damaged Mending equipment, gives nearby player
pickup priority, chooses one nearest eligible companion per orb, repairs at the
vanilla 2-durability-per-XP rate, and leaves surplus XP as an orb. It has no XP
bar or storage. Item and XP radii are independently clamped to 1..16.

The controls UI and inventory title use shadowless labels; the inventory title
uses the saved pet name or localized species name, while the player inventory
section remains labeled `Inventory`. The companion
controls, roster, target editor, and inventory share a dark panel style;
armor-only shift-click routes to equipment and ordinary shift-clicked items go
to cargo. The native saddle and offhand placeholders remain visible. The
overview nametag field saves automatically on focus loss, Enter,
or screen close, with no separate Rename button. Health and nametag settings
are labeled **Health tag** and **Nametag**, and controls expose hover
descriptions. Each action has one home: Overview owns health and nametag;
Movement owns movement, combat, area, teleport, and targets; Collection owns
item pickup, XP, and durability; Abilities owns only mob-specific abilities.
The roster displays movement/combat settings instead of `Ready`,
colors living rows green and dead rows red, keeps the raw entity ID on the
colored status line beneath the friendly display name, and the controls header
places the gray entity ID below the title without overlapping the close control.
The target editor keeps Save and Cancel visible,
scrolls its hierarchical list, keeps IDs in a bounded column before the state
buttons, and prompts on Escape when a draft is dirty with Save, Discard, and
Keep editing actions.

Movement controls use an internal scroll area at small window heights. The
companion inventory keeps its cargo grid in place but disables it until a chest,
trapped chest, barrel, or shulker box is equipped. Existing cargo can still be
withdrawn safely from legacy saves. Permission level 2 can disable the physical
container requirement per pet; shulker contents are packed back into the item
when it is removed. Companion inventory validity is owner and liveness based,
without a distance cutoff while the menu is open.

Bonded pets can be mounted by their owner. A saddle is required by default and
is stored in the native saddle equipment slot exposed by the companion inventory;
permission level 2 can waive the requirement per pet. Added mounts expose a
synchronized controller view and use ground, hopping, flying, aquatic, and
stationary profiles with movement attributes and effects. Native horse, pig,
strider, Happy Ghast, and nautilus controls remain native. Generic seats retain
the established zombie/small-mob position, while native seats are not overridden.
Mounted movement suspends follow/guard teleport and navigation, and server-side
input is applied with collision-aware movement. Optional Attack while mounted
uses the existing target filter, stance, line of sight, and native melee/ranged
reach without allowing the pet to pursue or steer the mount. Native attack AI
is re-enabled only for the mounted companion; navigation, move-control, and
autonomous jump outputs remain suppressed. Cached ranged targets are stopped
through their native goal lifecycle so clearing a target cannot crash a snow
golem. Bonded iron golems retain authorized managed targets while their native
creeper/player restrictions are bypassed only for those targets.

While the owner is riding a companion, the normal inventory key opens that
companion's inventory through the same revisioned server action used by the
controls screen. Vanilla player inventory behavior is unchanged for ordinary
mounts.

## Persistence and packet boundaries

`CompanionState`, settings, target selection, equipment, cargo, Vex lifetime,
golem provocation, summon affiliation, Creeper cooldown, and the UUID journal
use bounded codecs or attachments. Empty equipment/cargo slots serialize as
optional item stacks. The physical cargo container persists separately;
shulker contents use Minecraft's container item component and are packed back
into the item on removal. Roster refreshes fact-check loaded UUIDs and report
`ALIVE`, `UNLOADED`, `UNKNOWN`, or confirmed `DEAD`; a missing record is never
guessed dead. Entity-load reconciliation restores a live bond only from the
matching journal UUID and never creates a replacement. Dead records remain
until the owner removes them.

Every client mutation carries a bond UUID and expected revision. The server
rechecks ownership, liveness, same-level distance, revision, target selection
shape, numeric bounds, permission gates, and inventory menu validity before
mutating persistent state. Unknown target IDs are accepted only as stable,
well-formed unavailable entries; the matcher treats them as false.

## Verification snapshot

The final source set is compiled with Java 25, Fabric Loom 1.17.20, Fabric API
0.160.0+26.2, and Minecraft 26.2. The headless server GameTest suite contains
27 required tests covering defaults, saddle policy, owner input acceptance,
species riding profiles, target exclusions/catalog, Mending XP,
bond eligibility, passive/assist/defend policy, companion protection, sunlight,
golem neutrality/provocation, bonded iron-golem attacks, mounted zombie melee,
mounted drowned tridents, ranged cached-target cleanup, bonded iron-golem Follow navigation,
bonded illager combat, native Fox/skeleton/pillager
aggression suppression, generic discarded-companion lifecycle cleanup, lethal
Creeper roster cleanup, and reusable Creeper blasts. The latest run reports all
27 required tests passed. Resource loading reports 1,588 recipes and 1,691
advancements.

`compileJava`, `compileClientJava`, `runGameTestServer`, and
`runClientGameTest` are the current release checks. The latest server run
reports all 27 required tests passed. The client run also passes, covering
inventory interaction at GUI scales 1/2/3, current control and target screens,
roster rendering, and ridden zombie input with a safe dismount. The release
JAR `build/libs/tameall-0.2.0.jar` has SHA-256
`e604182717e52cc8f5a29022c9ddafa1402a755c25e92a4df1e934f1729fc352`.
Manual visual riding and multiplayer observation remain release boundaries. CI
runs the client GameTest with a virtual display and retains its logs and
screenshots.

## Manual smoke checks

The mod has also been exercised through ongoing play in a real Minecraft
client during development. The checks below extend that coverage with focused
edge cases and regression scenarios.

1. Craft both wheat items and the whistle; verify recipe-book unlocks and the
   first-tame advancement.
2. Bond a creature, feed it while injured and full, and inspect hearts, the
   controls labels, the gray ID, and the dark Inventory screen.
3. Exercise Follow, Stay, Guard, a 999-block area, target includes/excludes,
   Assist signals, Defend Area, and the 60-tick sight-loss pursuit.
4. Test golem neutrality, owner/pet provocation, evoker vex friendliness,
   wild-illager combat, Creeper lethal versus reusable modes, and Enderman
   pickup toggling.
5. Enable Mending XP on damaged equipment, verify player priority and surplus
   orbs, then use the whistle to recall both loaded and unloaded existing
   companions.
