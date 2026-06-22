<img src="https://thconman.github.io/MobGrab/logo.png" width="200" alt="MobGrab logo">

# MobGrab

### Pick up any mob as an item — and place it back down exactly as it was.

![Paper 26.1.2 to 26.2](https://img.shields.io/badge/Paper-26.1.2_–_26.2-3FB950?style=for-the-badge&logo=papermc&logoColor=white)
![Java 25](https://img.shields.io/badge/Java-25-F89820?style=for-the-badge&logo=openjdk&logoColor=white)
![License MIT](https://img.shields.io/badge/License-MIT-58A6FF?style=for-the-badge)

---

**Sneak + right-click** any mob to pocket it as a head item. **Right-click a block** to set it back down — exactly as it was.

Move your **tamed pets** — cats, wolves, parrots, horses — and they come back with their owner, collar colour, and sit state intact. Relocate **villagers** without losing a single trade. Carry **any mob**: health, equipment, enchants, name tags, variants, age — nothing is lost in the round trip.

No NMS hacks, no resource pack. Just grab, carry, place.

## ✨ Features

| Feature | What it does |
|---|---|
| 🐾 **Pets, villagers & every mob** | All 90+ mobs supported — tamed cats/wolves/parrots/horses, traders, livestock, even bosses — each with its own head texture |
| 📦 **Full data preserved** | Owner, collar colour, sit state, trades, equipment, enchants, custom names, variants, age — survives every grab |
| 🔥 **Fireproof items** | Optional: pocketed mobs survive fire & lava like netherite. Toggle it live, no restart |
| 🎛️ **Admin GUI** | `/mobgrab gui` — category filters, search, bulk enable/disable |
| 🌍 **Per-world control** | Disable grabbing in specific worlds, or block regions with a WorldGuard `mob-grab` flag |
| 🧪 **Preset engine** | Build custom trader villagers in config, or snapshot any mob with `/mobgrab save` |
| 🔌 **Plays nice** | WorldGuard · GriefPrevention · PlotSquared · RoseStacker · Geyser/Floodgate (Bedrock form GUI) |

## 🚀 Quick Start

1. Drop **`MobGrab.jar`** into your server's `plugins/` folder.
2. Restart the server.
3. **Sneak + right-click** a mob — it becomes a head item.
4. **Right-click a block** — the mob returns, exactly as it was.
5. Run **`/mobgrab gui`** to choose which mobs are grabbable.

> Ops can grab and place out of the box. To let regular players grab mobs, grant `mobgrab.pickup.*` and `mobgrab.place` with a permissions plugin like LuckPerms.

## 🎮 Commands

| Command | Description |
|---|---|
| `/mobgrab gui` | Open the mob toggle GUI |
| `/mobgrab enable\|disable <mob>` | Turn a mob on or off for pickup |
| `/mobgrab status` | Show enabled count and current settings |
| `/mobgrab give <player\|@s> <preset>` | Hand out a preset mob item |
| `/mobgrab save <name>` | Save the mob you're looking at as a preset |
| `/mobgrab reload` | Reload config and presets |

## ⚙️ Requirements

- **Paper 26.1.2 or 26.2**
- **Java 25**

## 📖 Documentation

Full permissions, configuration, and the preset guide live here: **[thconman.github.io/MobGrab](https://thconman.github.io/MobGrab/)**

---

*Built for survival servers, creative builds, and anyone who's ever wanted to relocate a villager without a boat and three hours of regret.*
