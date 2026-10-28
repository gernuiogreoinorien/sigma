# Camp Bosses (Fabric, Minecraft 1.21.11, client-side)

- Popup + sound when a camp boss is about to spawn ("X spawning in N minutes") and when it spawns.
- Timer list in the top-left: N/A until a boss is defeated ("RIP ..."), then counts down its respawn time.
  Shows "Alive" while it is up.
- Press O (default) to open the menu: click a boss to turn its popups on/off, -/+ changes its respawn timer,
  toggle sound and the timer HUD, and "Keybinds..." to change keys.
- Open the Camp Bestiary menu on the server to instantly sync every boss timer ("Spawning In" / "Boss Spawned!").
- Chat Games: timer in the list (5 min after each game starts) and a popup + sound 20s before the next one.
- Settings are saved in `.minecraft/config/camp-bosses.json`.

## Build
Install Java 21, then in this folder run `.\gradlew.bat build` (PowerShell) or `./gradlew build`.
The mod is `build/libs/camp-bosses-1.0.0.jar`. Needs Fabric API for 1.21.11.
