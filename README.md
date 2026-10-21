# Quill Core (Fabric, Minecraft 1.21.1)
Client-side mod for Quill Client. Fabric 1.21.1 is the only supported version so far.

## What it adds
- Own HUD manager: ONE editor (Right Shift) for keystrokes, CPS, FPS, ping, coordinates/facing/biome, potion status,
  armor status, sprint/sneak status and a live combo counter. All HUDs start OFF. Config: config/quill-hud.json.
  This replaces OneConfig on Fabric (Fabric profiles never install OneConfig).
- Quill main menu, plus the logo + "Quill Client" in the pause menu, inventory and chests.
- Analysis Stats adapter (analysis/AnalysisCollector): writes quill-analysis/events.jsonl for the launcher.
- Practice mode: `/quill practice start <stationary|strafe|combo|circle|random>` and `/quill practice stop`.
  Local singleplayer worlds only.

## What is observed vs inferred (Analysis Stats)
| Event | Source | Status |
|---|---|---|
| Attack attempt | Fabric AttackEntityCallback (client) | observed (NOT a confirmed hit) |
| Confirmed hit | target hurt flash within 10 ticks of an attempt | inferred (server never names the attacker) |
| Aim / spacing | crosshair ray vs target hitbox, positions, 20 Hz | observed |
| Knockback | player hurt flash + sudden velocity change | estimated |
| Server stun mechanics | no reliable client data | unavailable |

## Build (no setup on your PC)
1. Push this folder to GitHub, Actions tab -> build-quill-core -> Run workflow (it also runs the JUnit tests).
2. Download the jar from the artifact and copy `quill-core-1.21.1-1.0.0.jar` to `%APPDATA%\.pvpclient\quill\`.
   Quill Client copies it into every Fabric 1.21.1 profile when you press PLAY.

## Other versions
Minecraft's code changes between versions, so each version needs its own port and build
(jar name must start with quill-core-<minecraft version>). Until a port exists the launcher shows
"Unavailable" for that version instead of fake data.
