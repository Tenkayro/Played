package de.tenkayro.client.mixin;

import de.tenkayro.client.PlaytimeStorage;
import de.tenkayro.client.ResetPlaytimeIconButton;
import de.tenkayro.client.config.PlayedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EditWorldScreen.class)
public abstract class EditWorldScreenMixin extends Screen {
	private static final int PLAYED_ICON_MARGIN = 4;

	@Shadow
	@Final
	private LevelStorageSource.LevelStorageAccess levelAccess;

	protected EditWorldScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "init", at = @At("RETURN"))
	private void played$addResetPlaytimeButton(CallbackInfo info) {
		if (!PlayedConfig.shouldShowEditWorldResetButton()) {
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
				PlaytimeStorage.resetWorldPlaytime(this.levelAccess.getLevelPath(LevelResource.ROOT));
			}
			minecraft.gui.setScreen(currentScreen);
		}, Component.literal("Reset Playtime"), Component.literal("Reset the saved playtime for this world?"), Component.literal("Reset"), Component.literal("Cancel")));
	}
}
