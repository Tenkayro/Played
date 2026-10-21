package de.tenkayro.client.mixin;

import de.tenkayro.client.PlayedHud;
import de.tenkayro.client.config.PlayedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
	@Shadow
	private Component footer;

	@Unique
	private Component played$originalFooter;

	@Unique
	private boolean played$footerModified;

	@Inject(method = "extractRenderState", at = @At("HEAD"))
	private void played$addPlaytimeFooter(
		GuiGraphicsExtractor graphics,
		int width,
		Scoreboard scoreboard,
		Objective objective,
		CallbackInfo callbackInfo
	) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!PlayedConfig.shouldShowTabListPlaytime() || minecraft.player == null || minecraft.level == null) {
			return;
		}

		this.played$originalFooter = this.footer;
		@NonNull Component playtime = PlayedHud.getDisplayText(minecraft);
		this.footer = this.footer == null
			? playtime
			: this.footer.copy().append(Component.literal("\n")).append(playtime);
		this.played$footerModified = true;
	}

	@Inject(method = "extractRenderState", at = @At("RETURN"))
	private void played$restoreFooter(
		GuiGraphicsExtractor graphics,
		int width,
		Scoreboard scoreboard,
		Objective objective,
		CallbackInfo callbackInfo
	) {
		if (this.played$footerModified) {
			this.footer = this.played$originalFooter;
			this.played$originalFooter = null;
			this.played$footerModified = false;
		}
	}
}
