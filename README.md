# MobGrab

A PaperMC plugin that lets players pick up mobs as items and place them back down — full
entity state preserved (health, equipment, trades, name, variant, age, …). Includes a
villager/mob preset engine.

- **Website:** https://thconman.github.io/MobGrab/
- **Requires:** Paper **26.1.2 or 26.2** · **Java 25** — one jar, auto-adapts to the server version

## Features

- **Sneak + right-click** a mob → it becomes a head item; **right-click a block** to place it back.
- **All living mobs** supported, each with a custom head texture (camel husk, parched,
  mannequin, nautilus, …; plus the **sulfur cube** automatically on 26.2 servers).
- **Fireproof mob items** (optional) so a pocketed mob survives fire/lava like netherite —
  toggle live from the admin GUI.
- **Admin GUI** (`/mobgrab gui`) with category filters, search-by-page, and bulk enable/disable.
- **Region protection**: respects WorldGuard / GriefPrevention / PlotSquared, plus a custom
  WorldGuard **`mob-grab`** flag to disable grabbing in specific regions.
- **Per-world disable**, per-mob permissions, RoseStacker stacks, Geyser/Floodgate (Bedrock)
  form GUI, and a preset engine for custom villagers/mobs.

## Build

Requires JDK 25 (Paper 26.2's runtime). The Gradle wrapper is included.

```bash
./gradlew build                                   # -> build/libs/MobGrab.jar
./gradlew build -PpluginDir=/path/to/server/plugins   # build straight into a server
```

## Commands

| Command | Description |
|---|---|
| `/mobgrab gui` | Open the mob-toggle GUI |
| `/mobgrab reload` | Reload config & presets |
| `/mobgrab enable\|disable <mob>` | Enable/disable a mob for pickup |
| `/mobgrab status` | Show enabled count + settings |
| `/mobgrab give <player\|@s> <preset>` | Give a preset mob item |
| `/mobgrab list` | List presets |
| `/mobgrab save <name>` | Save the mob you're looking at as a preset |
| `/mobgrab delete <name>` | Delete a preset |
| `/mobgrab update` | Update the plugin from the latest GitHub release |

## Key permissions

- `mobgrab.pickup.*` — pick up all mobs *(default op)* (or grant per-mob/bundle to players: `mobgrab.pickup.<mob>`, `.passive`, `.hostile`, `.utility`, `.villager`, `.boss`)
- `mobgrab.place` — place mobs back down *(default op)*
- `mobgrab.admin` — GUI, reload, enable/disable, presets *(default op)*
- `mobgrab.bypass.protection` — ignore region checks *(default op)*

> Admins (OP) can grab/place out of the box. To let **non-op players** pick up mobs, grant
> `mobgrab.pickup.*` (or a bundle) + `mobgrab.place` with a permissions plugin like LuckPerms.

## Config highlights (`config.yml`)

- `fireproof-items` — mob items survive fire/lava (default `true`)
- `disabled-worlds` — worlds where MobGrab is fully off
- `blacklist-mode`, `cooldown-seconds`, sound/particle effects, `enabled-mobs` toggles

## License

MIT — see [LICENSE](LICENSE).
