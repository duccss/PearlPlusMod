package com.pearlplus.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.pearlplus.screen.PearlPlusScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class MixinPauseScreen {

    @Inject(method = "createPauseMenu", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/gui/layouts/GridLayout;arrangeElements()V"
    ))
    public void createPauseMenu(final CallbackInfo ci, @Local GridLayout.RowHelper rowHelper) {
        if (FabricLoader.getInstance().isModLoaded("replaymod")) {
            rowHelper.addChild(new SpacerElement(98, Button.DEFAULT_HEIGHT), 2);
        }
        rowHelper.addChild(Button.builder(Component.literal("PearlPlus"), button -> {
            button.active = false;
            Minecraft.getInstance().setScreen(new PearlPlusScreen((PauseScreen) (Object) this));
        }).width(204).build(), 2);
    }
}
