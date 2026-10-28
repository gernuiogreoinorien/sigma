package com.campbosses.mixin;

import com.campbosses.CampBosses;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads action bar messages and big titles (e.g. "RIP ICE LEADER!"). Chat comes from Fabric API's message event. */
@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void campbosses$onActionBar(Component message, boolean animateColor, CallbackInfo ci) {
        CampBosses.onMessage(message);
    }

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void campbosses$onTitle(Component title, CallbackInfo ci) {
        CampBosses.onMessage(title);
    }

    @Inject(method = "setSubtitle", at = @At("HEAD"))
    private void campbosses$onSubtitle(Component subtitle, CallbackInfo ci) {
        CampBosses.onMessage(subtitle);
    }
}
