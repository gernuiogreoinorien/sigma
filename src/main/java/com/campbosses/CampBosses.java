package com.campbosses;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects camp boss messages like
 *   "CAMP » Poseidon spawning in 1 minutes."
 *   "CAMP ALERT! (Area #11) Poseidon has spawned!"
 *   "RIP Ice Leader!"  (defeated -> respawn timer starts)
 * and shows popups, plays a sound, and keeps spawn timers in the top-left corner.
 */
public class CampBosses {

    private static final Pattern SPAWNING_IN = Pattern.compile(
            "spawning\\s+in\\s+(\\d+)\\s*(seconds?|secs?|s\\b|minutes?|mins?|m\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HAS_SPAWNED = Pattern.compile("has\\s+spawned", Pattern.CASE_INSENSITIVE);
    private static final Pattern RIP = Pattern.compile("\\bRIP\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern AREA = Pattern.compile("Area\\s*#\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /** Real-time millis when each boss is expected to spawn next (only once we actually know). */
    private static final Map<String, Long> nextSpawn = new HashMap<>();
    /** Bosses that have spawned and haven't been killed yet. */
    private static final java.util.Set<String> alive = new java.util.HashSet<>();
    /** True while this mod is showing its own popup, so we don't read our own title as a server message. */
    private static boolean showingOwnTitle = false;
    /** Ignore the same message arriving twice (chat + action bar, etc.). */
    private static final Map<String, Long> lastEvent = new HashMap<>();

    private static final long SHOW_OVERDUE_MS = 2 * 60_000;

    public static void init() {
        CampConfig.get();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) onMessage(message);
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("campbosses", "camp_timers"),
                (graphics, delta) -> renderHud(graphics));
    }

    /** Also called for action bar messages (from GuiMixin). */
    public static void onMessage(Component message) {
        if (showingOwnTitle) return;
        String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null) return;
        ChatGames.onMessage(text);
        String boss = findBoss(text);
        if (boss == null) return;

        Matcher soon = SPAWNING_IN.matcher(text);
        if (soon.find()) {
            int amount = Integer.parseInt(soon.group(1));
            boolean seconds = soon.group(2).toLowerCase(Locale.ROOT).startsWith("s");
            long ms = amount * (seconds ? 1000L : 60_000L);
            nextSpawn.put(boss, System.currentTimeMillis() + ms);
            alive.remove(boss);
            if (isDuplicate(boss + "|soon|" + amount)) return;
            String when = amount + (seconds ? (amount == 1 ? " second" : " seconds") : (amount == 1 ? " minute" : " minutes"));
            alert(boss,
                    Component.literal(boss).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.literal("spawning in " + when).withStyle(ChatFormatting.YELLOW),
                    1.0f);
            return;
        }

        // "RIP Ice Leader!" = defeated: now we know exactly when it respawns.
        if (RIP.matcher(text).find()) {
            alive.remove(boss);
            int mins = CampConfig.get().interval(boss);
            if (mins > 0) nextSpawn.put(boss, System.currentTimeMillis() + mins * 60_000L);
            else nextSpawn.remove(boss);
            return;
        }

        if (HAS_SPAWNED.matcher(text).find()) {
            alive.add(boss);
            nextSpawn.remove(boss);
            if (isDuplicate(boss + "|spawned")) return;
            Matcher area = AREA.matcher(text);
            Component sub = area.find()
                    ? Component.literal("Area #" + area.group(1)).withStyle(ChatFormatting.AQUA)
                    : Component.literal("Go get it!").withStyle(ChatFormatting.AQUA);
            alert(boss,
                    Component.literal(boss + " has spawned!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                    sub,
                    1.6f);
        }
    }

    static String findBoss(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        for (String b : CampConfig.BOSSES) {
            if (lower.contains(b.toLowerCase(Locale.ROOT))) return b;
        }
        return null;
    }

    /** Update a boss from the Camp Bestiary menu. spawnInMs = null means "already spawned". */
    static void setFromBestiary(String boss, Long spawnInMs) {
        if (spawnInMs == null) {
            alive.add(boss);
            nextSpawn.remove(boss);
        } else {
            alive.remove(boss);
            nextSpawn.put(boss, System.currentTimeMillis() + spawnInMs);
        }
    }

    private static boolean isDuplicate(String key) {
        long now = System.currentTimeMillis();
        Long last = lastEvent.get(key);
        lastEvent.put(key, now);
        return last != null && now - last < 5000;
    }

    private static void alert(String boss, Component title, Component subtitle, float pitch) {
        if (!CampConfig.get().alertsOn(boss)) return;
        popup(title, subtitle, pitch);
    }

    /** Shows a title popup and plays the alert sound (if sound is on). */
    static void popup(Component title, Component subtitle, float pitch) {
        CampConfig cfg = CampConfig.get();
        Minecraft mc = Minecraft.getInstance();
        showingOwnTitle = true;
        try {
            mc.gui.setTimes(5, 50, 15);
            mc.gui.setSubtitle(subtitle);
            mc.gui.setTitle(title);
        } finally {
            showingOwnTitle = false;
        }
        if (cfg.sound) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, pitch));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, pitch * 1.25f));
        }
    }

    private static void renderHud(GuiGraphics g) {
        CampConfig cfg = CampConfig.get();
        if (!cfg.hud) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        Font font = mc.font;
        long now = System.currentTimeMillis();

        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        for (String boss : CampConfig.BOSSES) {
            if (!cfg.alertsOn(boss)) continue;   // bosses turned off in the menu are hidden
            String status;
            int color;
            Long at = nextSpawn.get(boss);
            if (alive.contains(boss)) {
                status = "Alive";
                color = 0xFF55FF55;
            } else if (at == null || now - at > SHOW_OVERDUE_MS) {
                status = "N/A";
                color = 0xFF888888;
            } else if (at <= now) {
                status = "Spawning!";
                color = 0xFFFF5555;
            } else {
                long left = at - now;
                long sec = left / 1000;
                status = String.format("%d:%02d", sec / 60, sec % 60);
                color = left <= 60_000 ? 0xFFFFAA00 : 0xFFFFFFFF;
            }
            lines.add(boss + ": " + status);
            colors.add(color);
        }
        if (cfg.chatGames) {
            lines.add("Chat Games: " + ChatGames.status(now));
            colors.add(ChatGames.color(now));
        }
        if (lines.isEmpty()) return;

        int x = 4, y = 4, lineH = font.lineHeight + 2;
        int width = font.width("Camp Bosses");
        for (String l : lines) width = Math.max(width, font.width(l));
        g.fill(x - 2, y - 2, x + width + 2, y + lineH * (lines.size() + 1), 0x80000000);
        g.drawString(font, "Camp Bosses", x, y, 0xFF55FFFF, true);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x, y + lineH * (i + 1), colors.get(i), true);
        }
    }
}
