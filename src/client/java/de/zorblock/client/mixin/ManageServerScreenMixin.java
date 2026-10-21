package de.zorblock.client.mixin;

import de.zorblock.client.PlaytimeStorage;
import de.zorblock.client.ResetPlaytimeIconButton;
import de.zorblock.client.config.PlayedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ManageServerScreen.class)
public abstract class ManageServerScreenMixin extends Screen {
	private static final int PLAYED_ICON_MARGIN = 4;

	@Shadow
	@Final
	private ServerData serverData;

	protected ManageServerScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void played$addResetPlaytimeButton(CallbackInfo info) {
		if (!PlayedConfig.shouldShowEditServerResetButton() || this.serverData.ip.isBlank()) {
			return;
		}

		int x = this.width - ResetPlaytimeIconButton.SIZE - PLAYED_ICON_MARGIN;
		int y = this.height - ResetPlaytimeIconButton.SIZE - PLAYED_ICON_MARGIN;
		this.addRenderableWidget(new ResetPlaytimeIconButton(x, y, this::played$confirmResetPlaytime));
	}

	private void played$confirmResetPlaytime() {
		Minecraft minecraft = this.minecraft;
		Screen currentScreen = this;
		minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
			if (confirmed) {
				PlaytimeStorage.resetServerPlaytime(this.serverData.name, this.serverData.ip);
			}
			minecraft.gui.setScreen(currentScreen);
		}, Component.literal("Reset Playtime"), Component.literal("Reset the saved playtime for this server?"), Component.literal("Reset"), Component.literal("Cancel")));
	}
}
