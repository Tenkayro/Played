package de.zorblock.client.mixin;

import de.zorblock.client.TitleScreenTextLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class DrawContextMixin {
	@Inject(method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V", at = @At("HEAD"))
	private void played$recordStringText(Font textRenderer, String text, int x, int y, int color, CallbackInfo info) {
		TitleScreenTextLayout.recordText(x, y, color);
	}

	@Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V", at = @At("HEAD"))
	private void played$recordText(Font textRenderer, Component text, int x, int y, int color, CallbackInfo info) {
		TitleScreenTextLayout.recordText(x, y, color);
	}

	@Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)V", at = @At("HEAD"))
	private void played$recordOrderedText(Font textRenderer, FormattedCharSequence text, int x, int y, int color, CallbackInfo info) {
		TitleScreenTextLayout.recordText(x, y, color);
	}
}
