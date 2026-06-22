# MobGrab 2.1.3 — Human Testing Checklist (universal jar)

**One jar runs on both Paper 26.1.2 and 26.2.** Auto-validated on headless 26.1.2 and 26.2
servers (loads, reloads, summons the new mobs, zero plugin errors). The items below need a
real client and a human. The **sulfur cube** only exists on 26.2 — on your 26.1.2 server it
simply won't appear (everything else is identical).

## Build / deploy
```bash
cd ~/MobGrab
./gradlew build                       # -> build/libs/MobGrab.jar
# or drop straight into your SMP:
./gradlew build -PpluginDir=/home/con/smp/plugins
```
Server must run **Paper 26.1.2** on **Java 25**.

## Smoke (already auto-tested, re-confirm in-game)
- [ ] Server starts, console shows `MobGrab v2.1.3 enabled` + `Loaded 90 mob toggles`.
- [ ] `/mobgrab gui` opens the settings chest.

## All-mobs + new mobs
- [ ] In the GUI, page through — every mob has a head icon (no blank/steve heads).
- [ ] New mobs appear and toggle: **camel_husk, parched, mannequin, nautilus,
      zombie_nautilus** (also giant/illusioner).
- [ ] Sneak + right-click each new mob → you get a head item; right-click a block to place it
      back with its state preserved.

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
