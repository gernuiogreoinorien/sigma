package com.campbosses;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Tracks the CHATGAMES event (e.g. "CHATGAMES  Solve 83-36 for a reward!").
 * Each new game resets the countdown to the next one (5 minutes by default),
 * and a popup + sound plays 20 seconds before the next game.
 */
public class ChatGames {
    private static long nextGame = -1;      // real-time millis, -1 = unknown
    private static long lastStart = -100000;
    private static boolean warned = false;

    private static final long OVERDUE_MS = 2 * 60_000;

    static void onMessage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        boolean isStart = lower.contains("for a reward")
                || (lower.contains("chatgames") && !lower.contains("answered") && !lower.contains("received"));
        if (!isStart) return;

        long now = System.currentTimeMillis();
        if (now - lastStart < 30_000) return;   // same game (header + question as separate lines)
        lastStart = now;
        nextGame = now + CampConfig.get().chatGamesIntervalMinutes * 60_000L;
        warned = false;
    }

    static void tick() {
        CampConfig cfg = CampConfig.get();
        if (!cfg.chatGames || nextGame < 0 || warned) return;
        long left = nextGame - System.currentTimeMillis();
        if (left <= cfg.chatGamesWarnSeconds * 1000L && left > 0) {
            warned = true;
            CampBosses.popup(
                    Component.literal("Chat Game").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                    Component.literal("starting in " + Math.round(left / 1000.0) + " seconds").withStyle(ChatFormatting.YELLOW),
                    1.2f);
        }
    }

    static String status(long now) {
        if (nextGame < 0 || now - nextGame > OVERDUE_MS) return "N/A";
        long left = nextGame - now;
        if (left <= 0) return "Any second!";
        long s = left / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    static int color(long now) {
        if (nextGame < 0 || now - nextGame > OVERDUE_MS) return 0xFF888888;
        long left = nextGame - now;
        if (left <= 0) return 0xFFFF55FF;
        return left <= CampConfig.get().chatGamesWarnSeconds * 1000L ? 0xFFFFAA00 : 0xFFFFFFFF;
    }
}
