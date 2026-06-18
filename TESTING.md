# MobGrab 2.1.0 — Human Testing Checklist

Built + automatically validated on a headless **Paper 26.2** server (loads, reloads,
summons all new mobs, zero errors). The items below need a real 26.2 client and a human.

## Build / deploy
```bash
cd ~/MobGrab
./gradlew build                       # -> build/libs/MobGrab.jar
# or drop straight into your SMP:
./gradlew build -PpluginDir=/home/con/smp/plugins
```
Server must run **Java 25** (Paper 26.2 requires it).

## Smoke (already auto-tested, re-confirm in-game)
- [ ] Server starts, console shows `MobGrab v2.1.0 enabled` + `Loaded 91 mob toggles`.
- [ ] `/mobgrab gui` opens the settings chest.

## All-mobs + new mobs
- [ ] In the GUI, page through — every mob has a head icon (no blank/steve heads).
- [ ] New mobs appear and toggle: **sulfur_cube, camel_husk, parched, mannequin,
      nautilus, zombie_nautilus** (also giant/illusioner).
- [ ] Sneak + right-click a **sulfur cube** → you get a head item that *looks like a
      sulfur cube* (yellow cracked cube w/ eyes); lore shows **Size** + **Explosive**.
- [ ] Right-click a block with it → the sulfur cube comes back with its state.
- [ ] Repeat grab/place for camel_husk, parched, mannequin, a nautilus.

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
