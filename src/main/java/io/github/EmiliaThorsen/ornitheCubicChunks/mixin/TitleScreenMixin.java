package io.github.EmiliaThorsen.ornitheCubicChunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.EmiliaThorsen.ornitheCubicChunks.ornitheCubicChunksMod;

import net.minecraft.client.gui.screen.TitleScreen;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

	@Inject(method = "init", at = @At("TAIL"))
	public void ornitheCubicChunksMod$onInit(CallbackInfo ci) {
		ornitheCubicChunksMod.LOGGER.info("ornithe cubic chunks mod is loaded!");

	}
}
