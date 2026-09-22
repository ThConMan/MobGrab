# MobGrab 2.2.0 — Human Testing Checklist

**Targets Paper 26.2 only.** 2.2.0 compiles against the 26.2 API (`AbstractCubeMob`) and
declares `api-version: '26.2'`, so it will not load on 26.1.2 — use 2.1.3 for those servers.
The items below need a real client and a human.

## Build / deploy
```bash
cd ~/MobGrab
./gradlew build                       # -> build/libs/MobGrab.jar
# or drop straight into your SMP:
./gradlew build -PpluginDir=/home/con/smp/plugins
```
Server must run **Paper 26.2** on **Java 25**.

## Smoke (already auto-tested, re-confirm in-game)
- [ ] Server starts, console shows `MobGrab v2.2.0 enabled` + `Loaded 91 mob toggles`.
- [ ] `/mobgrab gui` opens the settings chest.

## All-mobs + new mobs
- [ ] In the GUI, page through — every mob has a head icon (no blank/steve heads).
- [ ] New mobs appear and toggle: **camel_husk, parched, mannequin, nautilus,
      zombie_nautilus** (also giant/illusioner).
- [ ] Sneak + right-click each new mob → you get a head item; right-click a block to place it
      back with its state preserved.

## Cube mobs (26.2 split — regression check)
26.2 made `MagmaCube` a sibling of `Slime` rather than a subclass. MobGrab now branches on
`AbstractCubeMob`, so all three cube mobs must behave identically:
- [ ] Grab a **slime**, a **magma cube**, and a **sulfur cube** of a non-default size — each
      head item's lore shows the correct **Size** line (Tiny / Small / Big).
- [ ] Place each back down → it respawns at that same size.
- [ ] A preset saved with a `size:` value applies to a **magma cube** and a **sulfur cube**,
      not just a slime.

## Fireproof items (your request)
- [ ] In the GUI, the **Fireproof Items** button (netherite ingot) toggles ON/OFF and persists.
- [ ] With it ON, grab a mob, drop the item in lava → it survives (like netherite).
- [ ] With it OFF, the item burns.

## WorldGuard flag (your request — needs WorldGuard installed)
- [ ] In a region: `/region flag <id> mob-grab deny` → grabbing is blocked there.
- [ ] Set it `allow` (or clear) → grabbing works again.
- [ ] No WorldGuard installed → plugin still loads clean (flag silently unavailable).

## Per-world disable (your request)
- [ ] Add a world to `disabled-worlds:` in config.yml, `/mobgrab reload`.
- [ ] In that world, pickup AND placement do nothing; other worlds unaffected.

## Safety fixes
- [ ] Fill your inventory, try to grab a mob → "inventory full", the **mob is NOT removed**.
- [ ] Disable a mob in the GUI, then try to place one you grabbed earlier → blocked.
- [ ] Spam right-click to place → rate-limited by the cooldown.

## Notes
- Bedrock/Geyser, RoseStacker stacks, GriefPrevention/PlotSquared regions: untested here
  (no such plugins on the test box) — exercise if your SMP runs them.
