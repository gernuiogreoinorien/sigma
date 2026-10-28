package com.autoexp;

import com.autoexp.mixin.MinecraftInvoker;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Watches the action bar for a "Gain X EXP" objective (e.g.
 * "Stage IV - Gain 9,300 EXP. - 2,120.4/9,300 (28m 40s)").
 * While the objective is unfinished, it selects an EXP Lootbox in the hotbar,
 * waits a moment, right-clicks it, waits again, and switches back to the slot
 * you were holding before. It repeats until the goal is reached or you run out
 * of EXP Lootboxes.
 */
public class AutoExpLootbox implements ClientModInitializer {

    // ---------- Settings (20 ticks = 1 second) ----------
    /** Ticks to wait after switching to the lootbox before right-clicking, so the server registers the slot change. */
    private static final int PRE_CLICK_DELAY = 1;
    /** Ticks to keep holding the lootbox after right-clicking before switching back. */
    private static final int POST_CLICK_DELAY = 2;
    /** Minimum ticks between the start of one open and the start of the next. */
    private static final int MIN_OPEN_INTERVAL = 6;
    /** If the progress number hasn't changed after an open, try again after this many ticks. */
    private static final int MAX_WAIT_FOR_UPDATE = 6;
    /** Stop if the server hasn't re-sent the Gain EXP message for this many ticks (it normally refreshes every second). */
    private static final int MESSAGE_TIMEOUT = 25;
    // ----------------------------------------------------

    // Matches "... Gain 9,300 EXP ... 2,120.4/9,300 ..."
    private static final Pattern OBJECTIVE = Pattern.compile(
            "Gain\\s+[\\d,.]+[kKmM]?\\s+EXP.*?([\\d,]+(?:\\.\\d+)?)\\s*/\\s*([\\d,]+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    private enum Phase { IDLE, WAIT_BEFORE_CLICK, WAIT_BEFORE_RETURN }

    private static KeyMapping toggleKey;
    private static boolean enabled = true;

    // Latest objective seen in the action bar
    private static double progress = -1;
    private static double goal = -1;
    private static long lastSeenTick = -100000;

    private static long tick = 0;
    private static long lastOpenTick = -100000;
    private static double progressAtLastOpen = -1;
    private static boolean warnedNoBoxes = false;

    private static Phase phase = Phase.IDLE;
    private static int phaseTimer = 0;
    private static int lootboxSlot = -1;
    private static int returnSlot = 0;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("autoexplootbox", "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.autoexplootbox.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(AutoExpLootbox::onTick);
    }

    /** Called from GuiMixin whenever the server sets the action bar text. */
    public static void onActionBar(Component message) {
        String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null) return;
        Matcher m = OBJECTIVE.matcher(text);
        if (m.find()) {
            try {
                progress = Double.parseDouble(m.group(1).replace(",", ""));
                goal = Double.parseDouble(m.group(2).replace(",", ""));
                lastSeenTick = tick;
                return;
            } catch (NumberFormatException ignored) {
            }
        }
        // Any other action bar message means the Gain EXP objective is no longer showing.
        goal = -1;
        progress = -1;
    }

    /** True only while the Gain EXP objective is currently on screen and not yet complete. */
    private static boolean objectiveActive() {
        return tick - lastSeenTick <= MESSAGE_TIMEOUT && goal > 0 && progress < goal;
    }

    private static void onTick(Minecraft mc) {
        tick++;
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) {
            phase = Phase.IDLE;
            return;
        }

        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            warnedNoBoxes = false;
            player.displayClientMessage(Component.literal("[Auto EXP Lootbox] ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(enabled ? "ON" : "OFF")
                            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        }

        Inventory inv = player.getInventory();

        // An open in progress always finishes by putting you back on your previous slot.
        if (phase != Phase.IDLE) {
            runSequence(mc, player, inv);
            return;
        }

        if (!enabled || mc.screen != null || !player.isAlive() || player.isUsingItem()) return;

        if (!objectiveActive()) {
            warnedNoBoxes = false;
            return;
        }

        // Pace the opens: wait for the server to update the progress number (or time out).
        long since = tick - lastOpenTick;
        boolean progressUpdated = progress != progressAtLastOpen;
        if (since < MIN_OPEN_INTERVAL) return;
        if (!progressUpdated && since < MAX_WAIT_FOR_UPDATE) return;

        int slot = findLootboxSlot(inv);
        if (slot < 0) {
            if (!warnedNoBoxes) {
                warnedNoBoxes = true;
                player.displayClientMessage(Component.literal("[Auto EXP Lootbox] No EXP Lootboxes in your hotbar.")
                        .withStyle(ChatFormatting.YELLOW), false);
            }
            return;
        }
        warnedNoBoxes = false;

        // Start: remember what you were holding, then switch to the lootbox.
        returnSlot = inv.getSelectedSlot();
        lootboxSlot = slot;
        lastOpenTick = tick;
        progressAtLastOpen = progress;
        inv.setSelectedSlot(slot);
        phase = Phase.WAIT_BEFORE_CLICK;
        phaseTimer = PRE_CLICK_DELAY;
    }

    private static void runSequence(Minecraft mc, LocalPlayer player, Inventory inv) {
        if (phaseTimer > 0) {
            phaseTimer--;
            return;
        }

        if (phase == Phase.WAIT_BEFORE_CLICK) {
            // Only click if the objective is still showing, we're still on the lootbox, and nothing got in the way.
            boolean ok = objectiveActive() && mc.screen == null && player.isAlive()
                    && inv.getSelectedSlot() == lootboxSlot && isExpLootbox(inv.getItem(lootboxSlot));
            if (ok) {
                // Real right-click: clicks the block you're looking at (like placing it),
                // or uses it in the air if you're not looking at anything.
                ((MinecraftInvoker) mc).autoexp$startUseItem();
            }
            phase = Phase.WAIT_BEFORE_RETURN;
            phaseTimer = POST_CLICK_DELAY;
            return;
        }

        // WAIT_BEFORE_RETURN: go back to the slot you had before (e.g. your sword).
        if (inv.getSelectedSlot() == lootboxSlot) inv.setSelectedSlot(returnSlot);
        phase = Phase.IDLE;
    }

    /** First hotbar slot holding an EXP Lootbox. */
    private static int findLootboxSlot(Inventory inv) {
        for (int i = 0; i < 9; i++) {
            if (isExpLootbox(inv.getItem(i))) return i;
        }
        return -1;
    }

    private static boolean isExpLootbox(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("exp") && lower.contains("lootbox");
    }
}
