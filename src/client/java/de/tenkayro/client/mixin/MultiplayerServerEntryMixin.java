package de.tenkayro.client.mixin;

import de.tenkayro.client.ListPlaytimeLayout;
import de.tenkayro.client.PlaytimeStorage;
import de.tenkayro.client.config.PlayedConfig;
import de.tenkayro.client.config.PlayedModMenu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class MultiplayerServerEntryMixin extends ServerSelectionList.Entry implements SelectableEntry {
	private static final int MOVE_BUTTON_SIZE = 32;
	private static final long OPEN_CONFIG_DOUBLE_CLICK_MILLIS = 500L;
	private static final long SORTING_PREVENTS_REORDER_TOOLTIP_MILLIS = 1800L;
	@SuppressWarnings("null")
	private static final @NonNull List<Component> SORTING_PREVENTS_REORDER_TOOLTIP = List.of(
		Component.literal("Played sorting is active."),
		Component.literal("Double-click for config.")
	);

	private long played$showReorderTooltipUntilMillis;
	private long played$lastBlockedMoveClickMillis;
	private int played$reorderTooltipX;
	private int played$reorderTooltipY;

	private long played$cachedMillis = -1L;
	private int played$cachedScreenWidth = -1;
	private boolean played$cachedHasScrollbar = false;
	private ListPlaytimeLayout.Placement played$cachedPlacement = null;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private JoinMultiplayerScreen screen;

	@Shadow
	@Final
	private ServerData serverData;

	@Shadow
	@Final
	ServerSelectionList this$0;

	@Inject(method = "swap", at = @At("HEAD"), cancellable = true)
	private void played$preventManualReorderWhileSorted(int fromIndex, int toIndex, CallbackInfo info) {
		if (PlayedConfig.getSortServerList() != PlayedConfig.SortMode.DISABLED) {
			info.cancel();
		}
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
	private void played$showSortingReorderTooltipOnClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> info) {
		if (PlayedConfig.getSortServerList() == PlayedConfig.SortMode.DISABLED) {
			this.played$clearReorderTooltip();
			return;
		}

		int index = this.this$0.children().indexOf((ServerSelectionList.Entry) (Object) this);
		if (index < 0) {
			this.played$clearReorderTooltip();
			return;
		}

		int relativeMouseX = (int) event.x() - this.getContentX();
		int relativeMouseY = (int) event.y() - this.getContentY();
		if (this.played$isMouseOverMoveButton(index, relativeMouseX, relativeMouseY)) {
			long currentMillis = Util.getMillis();
			if (currentMillis - this.played$lastBlockedMoveClickMillis <= OPEN_CONFIG_DOUBLE_CLICK_MILLIS) {
				this.played$clearReorderTooltip();
				this.minecraft.gui.setScreen(new PlayedModMenu().getModConfigScreenFactory().create(this.screen));
				info.setReturnValue(true);
				return;
			}

			this.played$lastBlockedMoveClickMillis = currentMillis;
			this.played$showReorderTooltipUntilMillis = currentMillis + SORTING_PREVENTS_REORDER_TOOLTIP_MILLIS;
			this.played$reorderTooltipX = (int) event.x();
			this.played$reorderTooltipY = (int) event.y();
			info.setReturnValue(true);
		}
	}

	@Inject(method = "extractContent", at = @At("RETURN"))
	private void played$renderSortingReorderTooltip(
		GuiGraphicsExtractor context,
		int mouseX,
		int mouseY,
		boolean hovered,
		float tickDelta,
		CallbackInfo info
	) {
		if (PlayedConfig.getSortServerList() == PlayedConfig.SortMode.DISABLED) {
			this.played$clearReorderTooltip();
			return;
		}

		long currentMillis = Util.getMillis();
		if (currentMillis > this.played$showReorderTooltipUntilMillis) {
			this.played$clearReorderTooltip();
			return;
		}

		if (this.played$showReorderTooltipUntilMillis > 0L && this.isMouseOver(mouseX, mouseY)) {
			context.setComponentTooltipForNextFrame(
				this.minecraft.font,
				SORTING_PREVENTS_REORDER_TOOLTIP,
				this.played$reorderTooltipX,
				this.played$reorderTooltipY
			);
		}
	}

	private boolean played$isMouseOverMoveButton(int index, int relativeMouseX, int relativeMouseY) {
		int serverCount = this.screen.getServers().size();
		return (index > 0 && this.mouseOverTopLeftQuarter(relativeMouseX, relativeMouseY, MOVE_BUTTON_SIZE))
			|| (index < serverCount - 1 && this.mouseOverBottomLeftQuarter(relativeMouseX, relativeMouseY, MOVE_BUTTON_SIZE));
	}

	private void played$clearReorderTooltip() {
		this.played$showReorderTooltipUntilMillis = 0L;
		this.played$lastBlockedMoveClickMillis = 0L;
	}

	@Inject(method = "extractContent", at = @At("RETURN"))
	private void played$renderServerPlaytime(
		GuiGraphicsExtractor context,
		int mouseX,
		int mouseY,
		boolean hovered,
		float tickDelta,
		CallbackInfo info
	) {
		if (!PlayedConfig.shouldShowServerPlaytime()) {
			return;
		}

		long millis = PlaytimeStorage.getServerMillis(this.serverData.name, this.serverData.ip);
		long seconds = millis / 1000L;
		int screenWidth = this.minecraft.getWindow().getGuiScaledWidth();
		boolean hasScrollbar = this.this$0.maxScrollAmount() > 0;

		if (this.played$cachedMillis != seconds || this.played$cachedScreenWidth != screenWidth || this.played$cachedHasScrollbar != hasScrollbar || this.played$cachedPlacement == null) {
			this.played$cachedMillis = seconds;
			this.played$cachedScreenWidth = screenWidth;
			this.played$cachedHasScrollbar = hasScrollbar;
			this.played$cachedPlacement = ListPlaytimeLayout.nextToScrollbar(
				this.minecraft.font,
				millis,
				this.this$0.getRowRight(),
				this.this$0.scrollbarWidth(),
				hasScrollbar,
				screenWidth
			);
		}

		ListPlaytimeLayout.Placement placement = this.played$cachedPlacement;

		int textWidth = this.minecraft.font.width(java.util.Objects.requireNonNull(placement.text()));
		int bgX1 = placement.x() - 4;
		int bgY1 = this.getContentY() - 1;
		int bgX2 = placement.x() + textWidth + 4;
		int bgY2 = this.getContentY() + 10;
		
		if (de.tenkayro.client.config.PlayedConfig.shouldShowListBackground()) {
			context.fill(bgX1, bgY1, bgX2, bgY2, 0x60000000);
		}
		context.text(this.minecraft.font, java.util.Objects.requireNonNull(placement.text()), placement.x(), this.getContentY() + 1, 0xFFDDDDDD, true);
	}
}
