# SoulEnchants

A full-scope custom-enchant, mythic-weapon, and boss plugin for **Spigot 1.8.8**. Themed around a Soul-gated currency, it ships with ~70 custom enchants across six tiers, nine mythic weapons with dual-effect ability slots, three multi-phase world bosses, client-side cosmetics via Lunar Client's Apollo API, and a Nordic-style item-attached mask system.

```
▸ 70+ custom enchants         ▸ 9 mythic weapons + ability slots
▸ 3 world bosses              ▸ 6-tier soul-gem currency
▸ Soul license gating         ▸ Apollo cooldown / waypoint integration
▸ /g ping + /lunar test       ▸ Nordic mask attach-to-helmet
▸ Reloadable YAML balance     ▸ 54-slot "Soul Vault" admin hub
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

Every balance knob lives in [`enchants.yml`](src/main/resources/enchants.yml) — reload with `/ce reload`.

### Soul currency
Two-stage economy. **Soul Bank** is the ledger (per-player balance in `souls.yml`). **Soul Gems** are one-way portable batteries minted via `/souls withdraw <amount>`. Soul-tier enchants (Soul Strike, Soul Drain, Divine Immolation, Nature's Wrath, Soul Burst, Phoenix, Soul Shield, Stormbringer) **require a gem in inventory to fire** and drain it on proc. No ledger fallback — mint discipline matters.

### Mythic weapons (v1.1)
Nine named items, each with a unique effect and a **secondary ability slot** (Nordic-Bard pattern). Combinatorial — bind Crimson Tongue's Bleed-heal onto Stormbringer's lightning chain for 81 unique combos.

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

Every mythic ships pre-enchanted (Sword: Sharpness V / Axe: Sharpness VI / Bow: Power V, plus Unbreaking III + Fire Aspect II + Looting III) and is **unbreakable**.

### World bosses
| Boss | HP | Phases | Signature mechanic |
|---|---|---|---|
| The Veilweaver | 15,000 | 3 | Thread Lash → Shatter Bolt → Apocalypse Weave |
| Ironheart Colossus | 8,000 | 2 | Seismic Stomp → Rocket Charge → Iron Wall |
| Modock, King of Atlantis | rift | 3 | Three-arena phase progression |

Live Apollo waypoints update on spawn and clear on death. Top damage dealer gets 50% of the soul reward.

### Cosmetic masks (v1.1, Nordic-style)
Masks are real inventory items — player heads with custom textures. Drag onto any helmet to attach (writes `se_mask_attached` NBT + appends `ATTACHED: <name>` lore line). Right-click the helmet to detach. The packet injector reads the helmet's NBT each tick and rewrites outgoing `ENTITY_EQUIPMENT` packets so other players see the mask visual. **Real helmet stays equipped** — enchants, durability, armor points all function normally.

### Apollo / Lunar Client integration
Drop [Apollo](https://lunarclient.dev/apollo/downloads) in your plugins folder:

- **Cooldown ring** above the hotbar for every whitelisted ability (Stormcaller, Guardians, Soul Shield, Phoenix, Reflect, Stormbringer, Dawnbringer, Nature's Wrath, Aegis, Rush, Overshield). Per-ability icon + countdown.
- **Live boss waypoints** — push on spawn, clear on death, hide if player is in another world.
- **`/g ping`** — ping your location to every online guildmate, labelled with their distance to you, auto-remove after 5 seconds.

Legacy `LunarClient-API` is supported as a fallback. Diagnostic: `/lunar status`, test with `/lunar test`.

### Guilds
10-player max, founder-led, shared vault, friendly-fire disabled. `/guild top` leaderboard, persistent storage. `/g ping` is the newest addition.

---

## Commands

All commands have colour-styled help panels grouped by intent. Full output: type `/<cmd>` with no args.

| Command | Summary |
|---|---|
| `/ce` | SoulEnchants admin hub — `/ce god` opens the Soul Vault GUI |
| `/ce reload [enchants\|loot\|all]` | Live-reload balance without a restart |
| `/souls` | Profile view — tier, bank balance, gem balance, licence state |
| `/souls withdraw <amount>` | **One-way.** Mint a Soul Gem from bank balance (`k`/`m`/`b` suffixes) |
| `/souls give\|take\|set <player> <amount>` | Admin — modify Soul Bank balance |
| `/mythic list \| give <id> [player] \| infuse <id> \| clear` | Mythic weapon admin + ability slot management |
| `/mask list \| give <id> [player]` | Browse cosmetic masks + hand them out |
| `/lunar status \| test` | Apollo bridge diagnostic |
| `/bless [player]` | Strip negative potion effects |
| `/boss list \| kill` | Running-boss control |
| `/shop` · `/quests` · `/mob` · `/rift` · `/modock` | Secondary systems |
| `/lootfilter togglemessage` | Per-player drop blacklist |
| `/guild` / `/g` | Guild management — `create`, `invite`, `join`, `leave`, `disband`, `vault`, `info`, `top`, `ping` |

Aliases surfaced: `/soul` · `/g` · `/mw`

---

## Admin GUI — `/ce god`

A glass-framed Soul Vault hub. Every tile follows the same lore template (description · stats · `▸ Click to <verb>`) so eye-flow is consistent across the whole panel.

```
Row 1 — GEAR      Enchants · Mythics · Masks · Boss Loot
Row 2 — SUPPORT   Reagents · Consumables · Loot Boxes · Recipes · Godset(PvE) · God Set(PvP)
Row 3 — SPAWN     Soul Gem Mint · Summon Boss · Custom Mobs
Row 4 — Close
```

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

- **v1.1** (current) — Soul Gems, mythic weapons, Nordic masks, Apollo bridge, `/g ping`, reloadable YAML, aesthetic overhaul
- **v1.0-MVP** — Original scope: enchants + two bosses + souls ledger + sidebar

---

## Credits

Built by fulls on Spigot 1.8.8. Enchant-system architecture inspired by Nordic's CrankedEnchants (CrankedPvP). Cooldown + waypoint integration via Lunar Client's Apollo API.
