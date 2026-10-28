# Auto EXP Lootbox (Fabric, Minecraft 1.21.11, client-side)

When the action bar shows a "Gain ... EXP" objective (e.g. `Stage IV - Gain 9,300 EXP. - 2,120.4/9,300`),
the mod selects an EXP Lootbox from your hotbar, right-clicks it, and switches back to the slot you were holding (e.g. your sword).
It repeats until the progress reaches the goal or you run out of boxes.

Toggle on/off: K (rebind in Options > Controls > Key Binds > Auto EXP Lootbox).

## Build
1. Install Java 21 (https://adoptium.net, Temurin 21).
2. Open a terminal in this folder and run:
   - Windows: `gradlew.bat build`
   - macOS/Linux: `./gradlew build`
3. The mod is `build/libs/auto-exp-lootbox-1.0.0.jar`.

## Install
Put the jar plus Fabric API (for 1.21.11) in `.minecraft/mods` and launch with Fabric Loader for 1.21.11.

## Tweaks
Settings are at the top of `src/main/java/com/autoexp/AutoExpLootbox.java` (delays).
