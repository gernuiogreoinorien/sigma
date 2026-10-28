package com.campbosses;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class CampBossesMod implements ClientModInitializer {
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("campbosses", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.campbosses.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_O, category));

        CampBosses.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            CampBestiary.tick(mc);
            ChatGames.tick();
            while (menuKey.consumeClick()) {
                if (mc.screen == null && mc.player != null) mc.setScreen(new CampScreen(null));
            }
        });
    }
}
