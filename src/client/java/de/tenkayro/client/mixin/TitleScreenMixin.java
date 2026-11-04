package de.tenkayro.client.mixin;

import de.tenkayro.client.PlaytimeStorage;
import de.tenkayro.client.PlaytimeTextButton;
import de.tenkayro.client.PlayedStatsScreen;
import de.tenkayro.client.TitleScreenTextLayout;
import de.tenkayro.client.config.PlayedConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TitleScreen.class, priority = 100)
public abstract class TitleScreenMixin extends Screen {
	@Unique
	private PlaytimeTextButton played$playtimeButton;
	@Shadow
	private boolean fading;

	@Shadow
	private long fadeInStart;

	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void played$addPlaytimeLink(CallbackInfo info) {
		this.played$playtimeButton = this.addRenderableWidget(new PlaytimeTextButton(
			() -> this.minecraft.gui.setScreen(new PlayedStatsScreen(this))));
	}

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void played$beginTextLayout(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		TitleScreenTextLayout.beginFrame(this.height);
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void played$renderAllTime(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (!PlayedConfig.shouldShowTotalPlaytime()) {
			if (this.played$playtimeButton != null) this.played$playtimeButton.visible = false;
			TitleScreenTextLayout.endFrame();
			return;
		}

		net.minecraft.network.chat.Component text = PlayedConfig.formatMainMenuPlaytime(PlayedConfig.formatTime(PlaytimeStorage.getAllTimeMillis()));
		float alpha = this.played$getTitleAlpha();
		
		int y = TitleScreenTextLayout.nextBottomLeftY();
		// Slide in effect: start 10 pixels to the left and move to x=2
		int x = 2 + (int)((1.0f - alpha) * -10.0f);

		int alphaInt = (int)(alpha * 255.0f);
		int textWidth = this.font.width(java.util.Objects.requireNonNull(text));
		if (this.played$playtimeButton != null) {
			this.played$playtimeButton.setX(x);
			this.played$playtimeButton.setY(y);
			this.played$playtimeButton.setWidth(textWidth);
			this.played$playtimeButton.visible = alphaInt > 0 && textWidth > 0;
			this.played$playtimeButton.active = alpha >= 0.95F;
		}
		if (alphaInt > 0) {
			int color = net.minecraft.util.ARGB.color(alphaInt, 255, 255, 255);
			context.text(this.font, java.util.Objects.requireNonNull(text), x, y, color, true);
			if (this.played$playtimeButton != null && this.played$playtimeButton.active
				&& (this.played$playtimeButton.isFocused()
					|| (mouseX >= x && mouseX < x + textWidth && mouseY >= y && mouseY < y + 9))) {
				context.horizontalLine(x, x + textWidth - 1, y + 9, color);
			}
		}

		TitleScreenTextLayout.endFrame();
	}

	private float played$getTitleAlpha() {
		if (!this.fading) {
			return 1.0F;
		}

		float fadeProgress = (float)(Util.getMillis() - this.fadeInStart) / 2000.0F;
		fadeProgress = Mth.clamp(fadeProgress, 0.0F, 1.0F);
		return Mth.clampedMap(fadeProgress, 0.5F, 1.0F, 0.0F, 1.0F);
	}
}
