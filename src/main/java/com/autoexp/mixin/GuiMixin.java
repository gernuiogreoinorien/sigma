package com.autoexp.mixin;

import com.autoexp.AutoExpLootbox;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every action-bar message passes through Gui#setOverlayMessage. */
@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void autoexp$onActionBar(Component message, boolean animateColor, CallbackInfo ci) {
        AutoExpLootbox.onActionBar(message);
    }
}
