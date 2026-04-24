# SoulEnchants

A full-scope custom-enchant, mythic-weapon, boss, and pet plugin for **Spigot 1.8.8**. Themed around a soul-gated currency, built to feel like a paid plugin. v1.4 adds a brand-new boss (Oakenheart), refactors boss AI onto a generic FSM, and hardens plugin lifecycle with a cleanup utility layer.

```
▸ 80+ custom enchants         ▸ 12 mythic weapons + ability slots
▸ 6 world bosses              ▸ 7 hybrid pets (egg + armor-stand companion)
▸ 14 masks (tiered)           ▸ 6-tier soul-gem currency
▸ Soul license gating         ▸ Apollo cooldown / waypoint integration
▸ /g ping + /lunar test       ▸ Nordic mask attach-to-helmet
▸ Reloadable YAML balance     ▸ Soul Vault admin hub (/ce god)
▸ FSM-driven boss AI          ▸ Crash-safe temp-block persistence
```

---

## Quick start

```bash
# Build
mvn package
# Output: target/SoulEnchants-1.1.0.jar

# Drop the jar in your server's plugins/ folder, restart.
# Optional plugins (hot-loaded if present):
#   Apollo         — Lunar Client cooldowns + waypoints + enhanced titles + notifications + holograms
#   ProtocolLib    — mask helmet-override rendering
```

---

## Core systems

### Custom enchants

Six tiers — Common · Uncommon · Rare · Epic · Legendary · Soul Enchant (red). Books roll random success-% and destroy-% on creation; Magic Dust overrides success; White Scrolls absorb a single destruction. Max 9 enchants per piece, extendable to 14 with **Slot Orbs** (Weapon / Armor variants).

**Recent additions:**
- **Rage** (Legendary, weapon) — consecutive hits on the same target stack `+(lvl × stack × 2)` bonus damage, max 10 stacks, 30s decay, resets on damage taken. Live action-bar meter.
- **Severance / Reaping Slash** — dedicated anti-heal enchants; bleed L4+ also applies anti-heal. Multiple sources stack multiplicatively with diminishing returns.
- **ObsidianShield** (Epic, armor) — permanent Fire Resistance while worn.
- **Slayer** now also deals flat TRUE damage vs bosses + minions (bypasses armor).

Every balance knob lives in [`enchants.yml`](src/main/resources/enchants.yml) — reload with `/ce reload`.

### Mythic weapons

Twelve named items, each with a unique effect and a **secondary ability slot** — bind one mythic's effect onto another for 12 × 11 = 132 unique combinations. Custom death messages per mythic (e.g. "X was rent asunder by Graverend").

Every mythic ships pre-enchanted (Sword: Sharpness V / Axe: Sharpness VI / Bow: Power V, plus Unbreaking III + Fire Aspect II + Looting III) and is **unbreakable**.

### Pet system — hybrid model

7 pet archetypes, each a dual-surface entity: an inventory egg (NBT-pinned progress) + a spawned follower armor-stand companion. Sneak + right-click fires the active ability.

### Cosmetic masks (v1.4 redesign)

Masks no longer grant free potion effects. Each mask has a **tier** (LOW · MID · HIGH) and a **power block** — any combination of:
- Aura boosts (`+1` to an effect you already have — no free apply)
- Outgoing damage multipliers (with optional "vs players" modifier)
- Incoming damage reductions (with optional "below 50% HP" gate)
- Fire / explosion / potion-effect immunities
- Named **custom abilities**

**Abilities:**
- **Stalker** (Hunter's Veil) — crouch still for 2s → Invisibility until you move or attack
- **Ironwill** (Dragon Skull) — fully immune to Bleed stack application
- **Frostguard** (Wither Skull) — immune to Slow + Mining Fatigue potions
- **Soul Harvest** (Tyrant's Crown) — 20% max-HP heal on any kill, 30s CD
- **Phantom Dash** (Void Mask) — 2s Invisibility burst while sprinting, 10s CD

Attach / detach exactly as before — drag onto any helmet to attach, right-click to remove. Browse via `/ce god → Masks` (tier-banded 54-slot view).

### Soul currency

Two-stage economy. **Soul Bank** is the ledger. **Soul Gems** are one-way portable batteries minted via `/souls withdraw <amount>`. Soul-tier enchants **require a gem in inventory to fire** and drain it on proc. No ledger fallback — mint discipline matters.

### World bosses — v1.4 FSM refactor

Every boss now runs on a generic finite-state-machine framework (`bosses/fsm/`): a `BossState<C>` interface + a `StateMachine<C>` driver + per-boss state classes in `bosses/<boss>/states/`. Each attack / phase transition is now a ~50-80-line file. Previously every boss was a ~550-line class of switch-case + nested `BukkitRunnable`s.

| Boss | HP | Theme | Signature mechanics |
|---|---|---|---|
| The Veilweaver | 15,000 | reality-bending wither skeleton | Thread Lash · Shatter Bolt · Dimensional Rift · Loom Laser · Echo Clones · Reality Fracture · Apocalypse Weave · Final Thread Bind (3 phases) |
| Ironheart Colossus | 8,000 | iron golem | Seismic Stomp · Boulder Throw · Rocket Charge · Magnetic Pull · Iron Wall · Ground Slam (2 phases + Reinforce at 25%) |
| **Oakenheart — Forest Sovereign** ★ v1.4 | 22,000 | ancient tree spirit | Thorn Lash (cone) · Root Bind (AoE slow) · Sapling Swarm · Falling Grove (oak-log meteors) · Briar Prison (cobweb cage + DoT) · Withering Aura (sneak-or-bleed) · 3 phases |
| The Hollow King | 25,000 | wither skeleton | Meteor + chain lightning + 6-pup death split |
| The Broodmother | 18,000 | spider | Web traps · venom cloud · 8-pup death split · Graverend drop |
| The Wurm-Lord | 22,000 | zombie pigman | Fire aura · burrow strike · magma-cube summons · Ruinhammer drop |
| The Choirmaster | 20,000 | wither skeleton | 3-bounce chain lightning · soul-steal aura · monk summons · Emberlash drop |

**Oakenheart** is summon-only: craft a **Ritual Sapling** from 4× Heartwood + 4× Verdant Tear + 1× Oakensap Essence (all drops from Oakenheart and sapling_sprout minions) and right-click on grass or dirt in the main world. Drops a guaranteed **Oaken Crown** + reagent stack, with a ~8% chance of the **Briar Mantle** boss chestplate and ~5% chance of the **Thornbound Gauntlet** boss axe.

*(Modock, the Atlantis boss, was removed in v1.4 and replaced with Oakenheart.)*

Top damage dealer gets the souls reward. Summon non-summon-item bosses from `/ce god → Spawn tab` or `/ce summon <id>`.

### Apollo / Lunar Client integration

Drop [Apollo](https://lunarclient.dev/apollo/downloads) in your plugins folder:

- **Cooldown ring** above the hotbar for every whitelisted ability
- **Live boss waypoints** — push on spawn, clear on death
- **Enhanced titles** with fade/scale timing for boss spawns + phase transitions
- **Notification toasts** for anti-heal, Nature's Wrath, Divine Immolation, Phoenix, Soul Shield procs
- **Holograms** for floating soul-gain numbers
- **`/g ping`** — guild-wide 5s marker

Legacy `LunarClient-API` fallback. Diagnostic: `/lunar status`, test with `/lunar test`.

Note: Rich Presence was removed — Lunar requires server listing in their curated ServerMappings allow-list for Discord RPC to render, which is impractical for private servers.

### Guilds

10-player max, founder-led, shared vault, friendly-fire disabled. `/guild top` leaderboard, persistent storage.

---

## Architecture (v1.4 refactor)

**Phase 1 — Cleanup hygiene** (commit `71a0cc9`):
- `util/MapManager` — single quit-eviction registry. Every UUID-keyed cache in the plugin (28+ maps across CombatListener, MaskAbilityTask, GUI state) registers here once; a single `PlayerQuitEvent` listener evicts across all of them.
- `util/TempBlockTracker` — crash-safe temp-block lifecycle. IronGolem's Iron Wall, Broodmother's cobweb traps, Oakenheart's Briar Prison — all route through `place(loc, mat, ticks, tag)`. Persisted to `tempblocks.yml` every write so a mid-fight crash restores every block on the next boot instead of leaving permanent world damage.
- Hardened `onDisable` — every subsystem stop/save wrapped in its own try/catch; `TempBlockTracker.restoreAll()` + `MapManager.clearAll()` + `scheduler.cancelTasks(plugin)` as mandatory final steps.
- `/ce debug` admin command prints live UUID-cache entry count + temp-block count + per-map breakdown.

**Phase 4 — onEnable god-class split** (commit `72d0cde`):
- The 230-line `onEnable` body is now a 23-phase lifecycle list:
  ```java
  safePhase("cleanup-infra",   this::initCleanupInfrastructure);
  safePhase("world-bootstrap", this::initWorlds);
  safePhase("configs",         this::initConfigs);
  // ... 20 more phases
  ```
- Each phase method contains its original inline init code but runs in its own try/catch. A throwing subsystem logs `[phase:lunar] FAILED — ...` and the rest of the plugin keeps loading.

**Phase 3 — Boss FSM refactor** (commits `e612648` + v1.4):
- New `bosses/fsm/` package: `BossState<C>` interface + `StateMachine<C>` driver. Generic context type so the same framework drives every boss.
- **IronGolemBoss** refactored to FSM first. ~550-line monolithic class → 150-line orchestration + 9 state files (`golem/states/*State.java`).
- **Veilweaver** follows the same pattern (`veilweaver/states/IdleState.java` + `PhaseTransitionState.java`). Attacks were already extracted to `VeilweaverAttacks` static calls, so Veilweaver's FSM is lean.
- **Oakenheart** is FSM-native from day one (9 state files).
- CustomMob-based bosses (Hollow King + 3 elites) keep their existing declarative `AbilityFactory` pattern — already well-structured, doesn't benefit from FSM migration.

---

## Commands

| Command | Summary |
|---|---|
| `/ce` | SoulEnchants admin hub — `/ce god` opens the Soul Vault GUI |
| `/ce bossset` · `/ce godset` | PvE / PvP loadouts (fully enchanted) |
| `/ce summon <id>` | Spawn any CustomMob or boss at your location |
| `/ce debug` | Live UUID-cache + temp-block diagnostic |
| `/ce reload [enchants\|loot\|all]` | Live-reload balance without restart |
| `/souls` · `/souls withdraw <amount>` · `/souls give\|take\|set` | Soul Bank + Gems |
| `/mythic list \| give <id> [player] \| infuse <id> \| clear` | Mythic admin + ability slots |
| `/mask list \| give <id> [player]` | Browse / hand out masks |
| `/pet list \| info \| despawn \| give <id> [p] \| xp <n>` | Hybrid pet system |
| `/oakenheart status \| summon \| abort \| give [player]` | ★ v1.4 — Forest Sovereign control |
| `/lunar status \| test` | Apollo bridge diagnostic |
| `/boss list \| kill` | Running-boss control |
| `/guild` / `/g` | Guild management |
| `/shop` · `/quests` · `/mob` · `/rift` · `/lootfilter` | Secondary systems |

---

## Configuration

| File | Contents |
|---|---|
| [`config.yml`](src/main/resources/config.yml) | Souls on kill, ore book-drop rates, boss reward pool |
| [`enchants.yml`](src/main/resources/enchants.yml) | ~80 balance knobs |
| [`mythics.yml`](src/main/resources/mythics.yml) | Per-mythic proc rates, cooldowns, soul costs |
| `tempblocks.yml` *(generated)* | Live temp-block persistence for crash recovery |

Reload with `/ce reload` — no restart.

---

## Build

```bash
mvn package
# target/SoulEnchants-1.1.0.jar
```

Requires JDK 8+ at build time (tested on JDK 17).

---

## Dependencies

| Dep | Scope | Why |
|---|---|---|
| Spigot API 1.8.8 | provided | runtime |
| NBT-API 2.13.2 | shaded | item NBT on 1.8 |
| Apollo API 1.2.5 | provided | optional — Lunar enhancements |
| Adventure API 4.17 | provided | Apollo Component types (Apollo bundles at runtime) |
| ProtocolLib | softdepend | optional — mask helmet rendering |

Every optional dep is hot-loaded with a no-op fallback if absent.

---

## Version history

- **v1.4** (current) — Phase 1 cleanup hygiene (MapManager + TempBlockTracker), Phase 4 onEnable god-class split, Phase 3 boss FSM refactor (IronGolem + Veilweaver), new Oakenheart boss (Forest Sovereign, 3 phases, summon-only via Ritual Sapling), Modock removed entirely, TierChatPrefix removed.
- **v1.3** — Slot orbs (9 → 14 enchant slots), Rage enchant ported from Nordic, mask system redesign with tiers + abilities, ObsidianShield, Choirmaster nerf, boss armor TTK fix.
- **v1.2** — Pet system (7 archetypes), 16 new enchants, 3 new PvE mythics, 6 new masks, 3 godset-tier elite bosses, god-tier armor enchants, `/ce bossset` with perfectly-enchanted mythics.
- **v1.1** — Soul Gems, mythic weapons, Nordic masks, Apollo bridge, `/g ping`, reloadable YAML, aesthetic overhaul.
- **v1.0-MVP** — Enchants + two bosses + souls ledger + sidebar.

---

## Credits

Built by fulls on Spigot 1.8.8. Enchant system inspired by Nordic's CrankedEnchants. Pet model inspired by Nordic's CrankedPets but reimagined with a spawned armor-stand companion. Cooldown + waypoint integration via Lunar Client's Apollo API.
