package com.campbosses;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Menu to turn popups on/off per camp boss and set each boss's respawn timer. */
public class CampScreen extends Screen {
    private static final int ROW_H = 22;
    private static final int TOGGLE_W = 140;
    private static final int SMALL_W = 20;
    private static final int COL_W = TOGGLE_W + SMALL_W * 2 + 4;

    private final Screen parent;

    public CampScreen(Screen parent) {
        super(Component.literal("Camp Boss Alerts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        CampConfig cfg = CampConfig.get();
        int perCol = (CampConfig.BOSSES.size() + 1) / 2;
        int gap = 12;
        int left = this.width / 2 - COL_W - gap / 2;
        int top = 32;

        for (int i = 0; i < CampConfig.BOSSES.size(); i++) {
            String boss = CampConfig.BOSSES.get(i);
            int col = i / perCol, row = i % perCol;
            int x = left + col * (COL_W + gap);
            int y = top + row * ROW_H;

            Button toggle = Button.builder(bossLabel(boss), b -> {
                cfg.alerts.put(boss, !cfg.alertsOn(boss));
                b.setMessage(bossLabel(boss));
            }).bounds(x, y, TOGGLE_W, 20).build();
            addRenderableWidget(toggle);

            addRenderableWidget(Button.builder(Component.literal("-"), b -> {
                cfg.intervals.put(boss, Math.max(0, cfg.interval(boss) - 5));
                toggle.setMessage(bossLabel(boss));
            }).bounds(x + TOGGLE_W + 2, y, SMALL_W, 20).build());

            addRenderableWidget(Button.builder(Component.literal("+"), b -> {
                cfg.intervals.put(boss, cfg.interval(boss) + 5);
                toggle.setMessage(bossLabel(boss));
            }).bounds(x + TOGGLE_W + SMALL_W + 4, y, SMALL_W, 20).build());
        }

        int bottom = top + perCol * ROW_H + 10;
        int cx = this.width / 2;
        addRenderableWidget(Button.builder(onOff("Sound", cfg.sound), b -> {
            cfg.sound = !cfg.sound;
            b.setMessage(onOff("Sound", cfg.sound));
        }).bounds(cx - 206, bottom, 100, 20).build());
        addRenderableWidget(Button.builder(onOff("Timer HUD", cfg.hud), b -> {
            cfg.hud = !cfg.hud;
            b.setMessage(onOff("Timer HUD", cfg.hud));
        }).bounds(cx - 102, bottom, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Keybinds..."), b -> {
            CampConfig.save();
            this.minecraft.setScreen(new KeyBindsScreen(this, this.minecraft.options));
        }).bounds(cx + 2, bottom, 100, 20).build());
        // Chat Games row: [toggle] [-][Alert 20s][+]  [-][Every 5m][+]
        int y2 = bottom + 24;
        int x2 = cx - 188;
        addRenderableWidget(Button.builder(onOff("Chat Games", cfg.chatGames), b -> {
            cfg.chatGames = !cfg.chatGames;
            b.setMessage(onOff("Chat Games", cfg.chatGames));
        }).bounds(x2, y2, 140, 20).build());
        x2 += 146;

        Button warnLabel = Button.builder(warnText(), b -> {}).bounds(x2 + 22, y2, 70, 20).build();
        warnLabel.active = false;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            cfg.chatGamesWarnSeconds = Math.max(5, cfg.chatGamesWarnSeconds - 5);
            warnLabel.setMessage(warnText());
        }).bounds(x2, y2, 20, 20).build());
        addRenderableWidget(warnLabel);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            cfg.chatGamesWarnSeconds = Math.min(240, cfg.chatGamesWarnSeconds + 5);
            warnLabel.setMessage(warnText());
        }).bounds(x2 + 94, y2, 20, 20).build());
        x2 += 120;

        Button everyLabel = Button.builder(everyText(), b -> {}).bounds(x2 + 22, y2, 70, 20).build();
        everyLabel.active = false;
        addRenderableWidget(Button.builder(Component.literal("-"), b -> {
            cfg.chatGamesIntervalMinutes = Math.max(1, cfg.chatGamesIntervalMinutes - 1);
            everyLabel.setMessage(everyText());
        }).bounds(x2, y2, 20, 20).build());
        addRenderableWidget(everyLabel);
        addRenderableWidget(Button.builder(Component.literal("+"), b -> {
            cfg.chatGamesIntervalMinutes = Math.min(60, cfg.chatGamesIntervalMinutes + 1);
            everyLabel.setMessage(everyText());
        }).bounds(x2 + 94, y2, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(cx + 106, bottom, 100, 20).build());
    }

    private static Component bossLabel(String boss) {
        CampConfig cfg = CampConfig.get();
        boolean on = cfg.alertsOn(boss);
        int mins = cfg.interval(boss);
        MutableComponent c = Component.literal(boss + " ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
        c.append(Component.literal(" " + (mins > 0 ? mins + "m" : "?")).withStyle(ChatFormatting.GRAY));
        return c;
    }

    private static Component warnText() {
        return Component.literal("Alert " + CampConfig.get().chatGamesWarnSeconds + "s");
    }

    private static Component everyText() {
        return Component.literal("Every " + CampConfig.get().chatGamesIntervalMinutes + "m");
    }

    private static Component onOff(String name, boolean on) {
        return Component.literal(name + ": ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        g.drawCenteredString(this.font, Component.literal("Click a boss to toggle its popup. -/+ changes its respawn timer."),
                this.width / 2, this.height - 14, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        CampConfig.save();
        this.minecraft.setScreen(parent);
    }
}
