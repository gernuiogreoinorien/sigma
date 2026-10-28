package com.campbosses;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * While the "Camp Bestiary" menu is open, reads each boss item's lore:
 *   "Spawning In: 2m 27s"  -> sets that boss's timer
 *   "Boss Spawned!"        -> marks it Alive
 */
public class CampBestiary {
    private static final Pattern SPAWNING_IN = Pattern.compile("Spawning\\s+In:?\\s*(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PART = Pattern.compile("(\\d+)\\s*(d|h|m|s)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPAWNED = Pattern.compile("Boss\\s+Spawned", Pattern.CASE_INSENSITIVE);

    private static AbstractContainerScreen<?> lastScreen = null;
    private static final Set<String> syncedThisOpen = new HashSet<>();
    private static int tickCounter = 0;

    public static void tick(Minecraft mc) {
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)
                || !screen.getTitle().getString().toLowerCase(Locale.ROOT).contains("camp bestiary")) {
            if (lastScreen != null) finishOpen(mc);
            return;
        }
        if (screen != lastScreen) {
            if (lastScreen != null) finishOpen(mc);
            lastScreen = screen;
            syncedThisOpen.clear();
            tickCounter = 0;
        }
        // Read a few ticks after opening (items arrive from the server) and then every second.
        if (tickCounter++ % 20 != 3) return;

        for (Slot slot : screen.getMenu().slots) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            String boss = CampBosses.findBoss(clean(stack.getHoverName()));
            if (boss == null) continue;
            ItemLore lore = stack.get(DataComponents.LORE);
            if (lore == null) continue;

            for (Component line : lore.lines()) {
                String text = clean(line);
                if (SPAWNED.matcher(text).find()) {
                    CampBosses.setFromBestiary(boss, null);
                    syncedThisOpen.add(boss);
                    break;
                }
                Matcher m = SPAWNING_IN.matcher(text);
                if (m.find()) {
                    long ms = parseDuration(m.group(1));
                    if (ms >= 0) {
                        CampBosses.setFromBestiary(boss, ms);
                        syncedThisOpen.add(boss);
                    }
                    break;
                }
            }
        }
    }

    private static void finishOpen(Minecraft mc) {
        if (!syncedThisOpen.isEmpty() && mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Camp Bosses] ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("Updated " + syncedThisOpen.size() + " timers from the Bestiary.")
                            .withStyle(ChatFormatting.GREEN)), false);
        }
        lastScreen = null;
        syncedThisOpen.clear();
    }

    /** "1h 2m 27s" -> milliseconds, or -1 if nothing found. */
    private static long parseDuration(String s) {
        Matcher m = TIME_PART.matcher(s);
        long total = 0;
        boolean any = false;
        while (m.find()) {
            any = true;
            long n = Long.parseLong(m.group(1));
            switch (m.group(2).toLowerCase(Locale.ROOT)) {
                case "d" -> total += n * 86_400_000L;
                case "h" -> total += n * 3_600_000L;
                case "m" -> total += n * 60_000L;
                default -> total += n * 1000L;
            }
        }
        return any ? total : -1;
    }

    private static String clean(Component c) {
        String s = ChatFormatting.stripFormatting(c.getString());
        return s == null ? "" : s;
    }
}
