# Orbital Laser Cinematic

**High-end 3-minute space weapon cinematic system for Minecraft Java (Forge 1.20.1)**

Designed for high graphical load machines.

## What it does

When activated, the camera leaves the planet in a long cinematic sequence:

| Time       | Phase                                      |
|------------|--------------------------------------------|
| 0:00-0:25  | Slow rise from the player, looking at sky  |
| 0:25-1:10  | Camera leaves the planet into "space"      |
| 1:10-1:50  | Approach the satellite, show charge-up     |
| 1:50-2:15  | Final charge + FIRE                        |
| 2:15-2:45  | Follow the massive beam down to the surface|
| 2:45-3:00  | Impact + aftermath                         |

Total duration: **exactly 3 minutes** (3600 ticks).

## How to test

1. Build the mod with `./gradlew build`
2. Put the jar in your mods folder
3. In-game run:

```
/orbitallaser start
```

To stop early:

```
/orbitallaser stop
```

## Current status

- ✅ Full 3-minute timeline with 6 cinematic phases
- ✅ Camera that leaves the planet
- ✅ Satellite entity with charge & firing state
- ✅ Command system
- ⏳ Beam renderer (next)
- ⏳ High-quality particles & energy effects
- ⏳ Sound design
- ⏳ Multiplayer sync packets

## Next steps

I can continue adding:
- Custom beam renderer (thick energy beam with core + glow + distortion)
- Particle systems for charge-up and impact
- Screen shake and post-processing feel
- Satellite model / renderer
- Better camera paths (more cinematic curves)

Just tell me what you want next and I keep building on this repo.

---

Created for a high-end machine. The cinematic is intentionally heavy on camera movement and duration.