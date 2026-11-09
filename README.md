# Shadow Client 26.2

A client-side Fabric utility mod for Minecraft Java Edition 26.2.

## Implemented

- Right Shift opens the ClickGUI.
- Visible Player List HUD: shows nearby players currently in line of sight (within 48 blocks, up to 6).
- Module List HUD: small Shadow Client status display.
- Both HUD options can be toggled in the ClickGUI.

## Build

Requires JDK 25. GitHub Actions builds the mod and uploads the JAR as an artifact.

The built mod JAR is in `build/libs/` after a successful build.

## Not implemented yet

Spawner Finder, Storage ESP, full Player ESP outlines, and Freecam are not implemented yet. The current player list intentionally only shows players in direct line of sight; it does not reveal players through walls. Automated attack macros are not included.

Use client-side visual utilities only where the world/server rules allow them.
