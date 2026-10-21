package de.tenkayro.client.mixin;

import de.tenkayro.client.PlaytimeTracker;
import de.tenkayro.client.TitleScreenTextLayout;
import de.tenkayro.client.config.PlayedConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PauseScreen.class, priority = 100)
public abstract class GameMenuScreenMixin extends Screen {
	protected GameMenuScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void played$beginTextLayout(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		TitleScreenTextLayout.beginFrame(this.height);
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void played$renderIngamePlaytime(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (!PlayedConfig.shouldShowIngamePlaytime() || this.minecraft == null) {
			TitleScreenTextLayout.endFrame();
			return;
		}

		long millis = PlaytimeTracker.getCurrentPlaytimeMillis(this.minecraft);
		net.minecraft.network.chat.Component text = PlayedConfig.formatPauseMenuPlaytime(PlayedConfig.formatTime(millis));
		int y = TitleScreenTextLayout.nextBottomLeftY();
		int textWidth = this.font.width(java.util.Objects.requireNonNull(text));
		
		int padding = 3;
		int bgX1 = 2 - padding;
		int bgY1 = y - padding;
		int bgX2 = 2 + textWidth + padding;
		int bgY2 = y + 9 + padding;
		
		// Draw semi-transparent background
		context.fill(Math.max(0, bgX1), bgY1, bgX2, bgY2, 0x80000000);
		// Draw a stylish gold accent line on the left
		context.fill(Math.max(0, bgX1), bgY1, Math.max(0, bgX1) + 1, bgY2, 0xFFFFD700);
		
		context.text(this.font, java.util.Objects.requireNonNull(text), 2, y, 0xFFFFFFFF, true);
		TitleScreenTextLayout.endFrame();
	}
}
