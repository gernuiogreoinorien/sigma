package com.autoexp.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes vanilla's own right-click handler (the same code your right mouse button runs). */
@Mixin(Minecraft.class)
public interface MinecraftInvoker {
    @Invoker("startUseItem")
    void autoexp$startUseItem();
}
