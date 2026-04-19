# SoulEnchants

A custom enchantments + boss plugin for **Spigot 1.8.8**, themed around **Souls**, gated PvE progression, and high-impact gear.

## Features

- **70+ custom enchants** across Sword, Bow, Helmet, Chestplate, Leggings, Boots, Tools
- **7 tiers** — Common → Uncommon → Rare → Epic → Legendary → Mythic → Soul Enchant (red, drains the Souls currency on each trigger)
- **Random success + destroy rates** rolled per book; Magic Dust overrides success, White Scrolls absorb destruction
- **Two custom bosses:**
  - **The Veilweaver** — 3 phases, 750 HP, Loom Orbs, lasers, Final Thread Bind
  - **Ironheart Colossus** — 2 phases, 500 HP, Seismic Stomp, Rocket Charge, Iron Wall, Ground Slam
- **Souls currency** — earned only via boss kills + Soul Reaper bonus
- **Live sidebar scoreboard** showing HP, Souls, active cooldowns, and live boss phase + HP
- **Paginated admin menu** (`/ce menu`) — every enchant at max level, 100% success
- **Item cap of 9 enchants** per piece

## Build

```bash
mvn package
```

Output jar: `target/SoulEnchants-1.0.0-MVP.jar`

Drop into your Spigot 1.8.8 server's `plugins/` folder.

## Commands

- `/souls [give|take|set] [player] [amount]` — currency
- `/ce menu` — admin enchant catalog (free max-level books)
- `/ce list` — registered enchants
- `/ce book <player> <enchant> <level>` — give a book
- `/ce dust <player> <25|50|75|100>` — give Magic Dust
- `/ce scroll <player> <black|white>` — give a scroll
- `/ce summon <veilweaver|irongolem>` — spawn a boss
- `/ce despawn <veilweaver|irongolem>` — remove a boss

## Dependencies

- Spigot API 1.8.8 (provided)
- NBT-API 2.13.2 (shaded)
