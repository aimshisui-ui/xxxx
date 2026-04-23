# SoulEnchants

A full-scope custom-enchant, mythic-weapon, boss, and pet plugin for **Spigot 1.8.8**. Themed around a soul-gated currency, built to feel like a paid plugin.

```
▸ 80+ custom enchants         ▸ 12 mythic weapons + ability slots
▸ 6 world bosses              ▸ 7 hybrid pets (egg + armor-stand companion)
▸ 13 cosmetic masks           ▸ 6-tier soul-gem currency
▸ Soul license gating         ▸ Apollo cooldown / waypoint integration
▸ /g ping + /lunar test       ▸ Nordic mask attach-to-helmet
▸ Reloadable YAML balance     ▸ Soul Vault admin hub (/ce god)
```

---

## Quick start

```bash
# Build
mvn package
# Output: target/SoulEnchants-1.1.0.jar

# Drop the jar in your server's plugins/ folder, restart.
# Optional plugins (hot-loaded if present):
#   Apollo         — Lunar Client cooldowns + waypoints
#   ProtocolLib    — mask helmet-override rendering
```

---

## Core systems

### Custom enchants

Six tiers — Common · Uncommon · Rare · Epic · Legendary · Soul Enchant (red). Books roll random success-% and destroy-% on creation; Magic Dust overrides success; White Scrolls absorb a single destruction. Max 9 enchants per piece.

**v1.2 added 16 new enchants.** Six axe debuffs, four PvE-focused armor enchants, six god-tier armor fillers that finally push utility filler (aquatic / night vision / depth strider) out of godset slots.

| Category | Enchant | Slot | Effect |
|---|---|---|---|
| Axe debuff | Marrowbreak | Axe | 25%/lvl — Weakness II 5s |
| Axe debuff | Crushing Blow | Axe | 20%/lvl — Slow III 3s |
| Axe debuff | Pulverize | Axe | 15%/lvl — Nausea III + Slow II 4s |
| Axe debuff | Exsanguinate | Axe | 10%/lvl — 5s true-damage DoT |
| Axe stateful | Hunter's Mark | Axe | Mark 10s · +12%/lvl vs marked |
| Axe stateful | Overwhelm | Axe | +6%/lvl per consecutive hit (max 5) |
| Armor PvE | Soul Warden | Chest | Regen after mob hit (60s CD) |
| Armor PvE | Mobslayer's Ward | Any armor | -10%/lvl damage from custom mobs |
| Armor PvE | Radiant Shell | Any armor | -1 flat per piece (-4 full set) |
| Armor PvE | Dreadmantle | Helmet | Aura Weakness on nearby mobs on hit |
| Armor god | Thornback | Any armor | Reflect 5%/lvl as TRUE damage (stacks per piece) |
| Armor god | Warden's Eye | Helmet | Mark attackers with particle ring |
| Armor god | Bulwark | Chest | -6%/lvl from mobs · Resistance II below 40% HP |
| Armor god | Voidwalker | Boots | 8%/lvl dodge · permanent Speed I |
| Armor god | Oathbound | Helmet | Cleanse Slow/Weakness/Wither on hit (30s CD) |
| Armor god | Entombed | Legs | Slow IV + MF III aura on attackers below 30% HP |

Every balance knob lives in [`enchants.yml`](src/main/resources/enchants.yml) — reload with `/ce reload`.

### Mythic weapons

Twelve named items, each with a unique effect and a **secondary ability slot** — bind one mythic's effect onto another for 12 × 11 = 132 unique combinations.

| Mythic | Mode | Effect |
|---|---|---|
| Crimson Tongue | held | Heal per nearby Bleed tick |
| Wraithcleaver | held | Heal per Cleave AoE proc |
| Stormbringer | held | Chain-lightning on crit, soul-gated |
| Voidreaver | held | Souls on kill + Speed aura |
| Dawnbringer | aura | Periodic debuff purge + Regen for allies in radius |
| Sunderer | held | Greataxe — armor strip + true damage |
| Phoenix Feather | held | On-kill heal + radius ignite |
| Soulbinder | held | Bow — souls per hit + true damage |
| Tidecaller | hotbar | Water Breathing + Depth Strider aura |
| **Graverend (v1.2)** | held | Heal 10% of killed mob's max HP + 20% dmg vs bosses |
| **Emberlash (v1.2)** | held | 4-block fire splash + 4s ignite on every swing |
| **Ruinhammer (v1.2)** | held | Stacking kill bonus + 5-block shockwave at 10 stacks |

Every mythic ships pre-enchanted (Sword: Sharpness V / Axe: Sharpness VI / Bow: Power V, plus Unbreaking III + Fire Aspect II + Looting III) and is **unbreakable**.

### Pet system (v1.2) — hybrid model

7 pet archetypes. Each pet is a **dual-surface entity**:

- **Inventory egg** — players carry an egg item with NBT-pinned progress (level, XP, UID). Eggs trade freely, stack-merge naturally, persist through death as loot-drops.
- **Spawned companion** — an invisible armor stand wearing the pet's visual helmet (skull / block / netherrack / ice / etc.) that follows the player at ~1.4 blocks behind, teleports past 24 blocks, despawns cleanly on world change / quit.

Right-click to summon / despawn. **Sneak + right-click** fires the active ability. XP drips from every mob kill.

| Pet | Archetype | Passive | Active |
|---|---|---|---|
| Ethereal Wisp | Utility | Night Vision + Speed I + Regen I + 25% souls on kill | Phase — 8s Invis + Speed III + 8 HP heal |
| Stone Guardian | Defensive | Permanent Resistance I + Health Boost II (+4 hearts) | Bulwark — 8 hearts Absorption + Resistance III + knockback pulse |
| Hellhound | Offensive | Permanent Strength I + Speed I · kills → Haste II 4s refresh | Bloodfrenzy — 8s Strength III + Speed II + 25% lifesteal |
| Frost Sprite | Crowd Control | 10-block Slow II + MF I aura on enemies | Glacier Burst — 12-block Slow V + MF III for 5s + 6 dmg |
| Ember Fox | Offensive | Fire Resistance + Strength I · attackers ignite 4s | Inferno Ring — 8-block 3-ring burst + 6s residual flame aura |
| Shadow Raven | Utility | 5-block Weakness aura on mobs · 10% mob-kill loot-dup | Assassinate — 6s Str III + Spd II + 8-block Weakness III |
| The Seer | Progression | Haste II + Resistance I · +50% souls + XP per kill | Mark — reveal entities in 20 blocks · 12s Str II + Res II |

Manage with `/pet list | info | despawn | give <id> [player] | xp <n>`. Eggs mint fresh from `/pet give` or via the **Pets tile in `/ce god` → Support row (slot 23)**.

### Soul currency

Two-stage economy. **Soul Bank** is the ledger (per-player balance in `souls.yml`). **Soul Gems** are one-way portable batteries minted via `/souls withdraw <amount>`. Soul-tier enchants **require a gem in inventory to fire** and drain it on proc. No ledger fallback — mint discipline matters.

### World bosses

v1.1 shipped the Veilweaver, Ironheart Colossus, and Hollow King. **v1.2 adds three godset-tier elites** — tuned so a non-godset player can't survive the opening minute.

| Boss | HP | Base | Signature mechanics |
|---|---|---|---|
| The Veilweaver | 15,000 | encounter | Thread Lash → Shatter Bolt → Apocalypse Weave (3 phases) |
| Ironheart Colossus | 8,000 | encounter | Seismic Stomp → Rocket Charge → Iron Wall (2 phases) |
| Modock, King of Atlantis | rift | encounter | Three-arena phase progression |
| The Hollow King | 25,000 | skeleton | Meteor + chain lightning + forced-melee enforcer + 6-pup death split |
| **The Broodmother (v1.2)** | 18,000 | spider | Web-slow aura · venom cloud · 8-pup death split · Graverend drop |
| **The Wurm-Lord (v1.2)** | 22,000 | zombie pigman | Fire aura · 5-block meteors · magma-cube summons · Ruinhammer drop |
| **The Choirmaster (v1.2)** | 20,000 | wither skeleton | 4-bounce chain lightning · soul-steal aura · monk summons · Emberlash drop |

Live Apollo waypoints update on spawn and clear on death. Top damage dealer gets 50% of the soul reward. Summon from `/ce god → Spawn tab` or `/ce summon <id>`.

### Cosmetic masks (Nordic-style)

Masks are real inventory items — player heads / skulls / pumpkins with custom flavor. Drag onto any helmet to attach (writes `se_mask_attached` NBT + appends `ATTACHED: <name>` lore line). Right-click the helmet to detach. The packet injector reads the helmet's NBT each tick and rewrites outgoing `ENTITY_EQUIPMENT` packets so other players see the mask visual. **Real helmet stays equipped** — enchants, durability, armor points all function normally.

13 masks ship: Pumpkin Head, Jack-o'-Lantern, Dragon Skull, Wither Skull, Zombie Veil, Skeletal Crown, Creeper Mask, plus **v1.2 additions** Duelist's Mask, Tyrant's Crown, Battle-Scar, Hunter's Veil, Witchwood Mask, Soulfire Mask.

### Apollo / Lunar Client integration

Drop [Apollo](https://lunarclient.dev/apollo/downloads) in your plugins folder:

- **Cooldown ring** above the hotbar for every whitelisted ability. Per-ability icon + countdown.
- **Live boss waypoints** — push on spawn, clear on death, hide if player is in another world.
- **`/g ping`** — ping your location to every online guildmate, labelled with their distance to you, auto-remove after 5 seconds.

Legacy `LunarClient-API` is supported as a fallback. Diagnostic: `/lunar status`, test with `/lunar test`.

### Guilds

10-player max, founder-led, shared vault, friendly-fire disabled. `/guild top` leaderboard, persistent storage.

---

## Commands

All commands have colour-styled help panels grouped by intent. Full output: type `/<cmd>` with no args.

| Command | Summary |
|---|---|
| `/ce` | SoulEnchants admin hub — `/ce god` opens the Soul Vault GUI |
| `/ce bossset` | Equip the PvE loadout — armor + 2 classic mythics + 3 v1.2 mythics, **every mythic fully enchanted to PvE spec** |
| `/ce godset` | Equip the PvP loadout — armor + sword + axe + 2 mythics |
| `/ce summon <boss\|mob>` | Spawn any CustomMob or encounter boss at your location |
| `/ce reload [enchants\|loot\|all]` | Live-reload balance without a restart |
| `/souls` | Profile view — tier, bank balance, gem balance, licence state |
| `/souls withdraw <amount>` | **One-way.** Mint a Soul Gem from bank balance (`k`/`m`/`b` suffixes) |
| `/souls give\|take\|set <player> <amount>` | Admin — modify Soul Bank balance |
| `/mythic list \| give <id> [player] \| infuse <id> \| clear` | Mythic weapon admin + ability slot management |
| `/mask list \| give <id> [player]` | Browse cosmetic masks + hand them out |
| `/pet list \| info \| despawn \| give <id> [p] \| xp <n>` | **(v1.2)** hybrid pet system |
| `/lunar status \| test` | Apollo bridge diagnostic |
| `/bless [player]` | Strip negative potion effects |
| `/boss list \| kill` | Running-boss control |
| `/shop` · `/quests` · `/mob` · `/rift` · `/modock` | Secondary systems |
| `/lootfilter togglemessage` | Per-player drop blacklist |
| `/guild` / `/g` | Guild management — `create`, `invite`, `join`, `leave`, `disband`, `vault`, `info`, `top`, `ping` |

Aliases: `/soul` · `/g` · `/mw` · `/pets`

---

## Admin GUI — `/ce god`

A glass-framed Soul Vault hub. Every tile follows the same lore template (description · stats · `▸ Click to <verb>`) so eye-flow is consistent across the whole panel.

```
Row 1 — GEAR      Enchants · Mythics · Masks · Boss Loot
Row 2 — SUPPORT   Reagents · Consumables · Loot Boxes · Recipes · Pets · Boss-Killer Set · God Set
Row 3 — SPAWN     Soul Gem Mint · Summon Boss · Custom Mobs
Row 4 — Close
```

The **Summon Boss** tile opens a 45-slot panel with tiles for Veilweaver, Ironheart Colossus, Modock, The Hollow King, and the three v1.2 elites (Broodmother / Wurm-Lord / Choirmaster). Click-to-summon runs the correct code path per boss type.

Recipe view opens a real `InventoryType.WORKBENCH` so it reads exactly like a crafting table.

---

## Configuration

| File | Contents |
|---|---|
| [`config.yml`](src/main/resources/config.yml) | Souls on kill, ore book-drop rates, veilweaver reward |
| [`enchants.yml`](src/main/resources/enchants.yml) | ~80 balance knobs for every enchant |
| [`mythics.yml`](src/main/resources/mythics.yml) | Per-mythic proc rates, cooldowns, soul costs |

Reload with `/ce reload` — no restart. YAML schema migrations are automatic: new keys added in a release merge into the on-disk copy, your tuning is preserved.

---

## Build

```bash
mvn package
```

Output: `target/SoulEnchants-1.1.0.jar`. Requires JDK 8+ at build time (tested on JDK 17).

---

## Dependencies

| Dep | Scope | Why |
|---|---|---|
| Spigot API 1.8.8 | provided | runtime |
| NBT-API 2.13.2 | shaded | item NBT on 1.8 |
| Apollo API 1.2.5 | provided | optional — enables Lunar cooldowns + waypoints |
| ProtocolLib | softdepend | optional — enables mask helmet rendering |

Both Apollo and ProtocolLib are optional at runtime — the plugin no-ops the relevant features if they're absent and logs a single startup notice.

---

## Version history

- **v1.2** (current) — Pet system (7 archetypes), 16 new enchants, 3 new PvE mythics (Graverend / Emberlash / Ruinhammer), 6 new masks, 3 godset-tier elite bosses, god-tier armor enchants (Thornback / Warden's Eye / Bulwark / Voidwalker / Oathbound / Entombed), `/ce bossset` ships every mythic perfectly enchanted.
- **v1.1** — Soul Gems, mythic weapons, Nordic masks, Apollo bridge, `/g ping`, reloadable YAML, aesthetic overhaul
- **v1.0-MVP** — Original scope: enchants + two bosses + souls ledger + sidebar

---

## Credits

Built by fulls on Spigot 1.8.8. Enchant-system architecture inspired by Nordic's CrankedEnchants (CrankedPvP). Pet model inspired by Nordic's CrankedPets but reimagined with a spawned armor-stand companion (Nordic's are inventory-only). Cooldown + waypoint integration via Lunar Client's Apollo API.
