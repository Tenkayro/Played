package de.zorblock.client.mixin;

import de.zorblock.client.PlaytimeStorage;
import de.zorblock.client.config.PlayedConfig;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.class)
public abstract class ServerSelectionListMixin {
	@Shadow
	@Final
	private List<ServerSelectionList.OnlineServerEntry> onlineServers;

	@Inject(method = "updateOnlineServers", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;refreshEntries()V"))
	private void played$sortOnlineServers(ServerList serverList, CallbackInfo info) {
		PlayedConfig.SortMode sortMode = PlayedConfig.getSortServerList();
		if (sortMode == PlayedConfig.SortMode.DISABLED || this.onlineServers.size() < 2) {
			return;
		}

		Comparator<ServerSelectionList.OnlineServerEntry> comparator = Comparator.comparingLong(ServerSelectionListMixin::played$getServerMillis);
		if (sortMode == PlayedConfig.SortMode.UP_DOWN) {
			comparator = comparator.reversed();
		}

		this.onlineServers.sort(comparator.thenComparing(entry -> entry.getServerData().name, String.CASE_INSENSITIVE_ORDER));
	}

	private static long played$getServerMillis(ServerSelectionList.OnlineServerEntry entry) {
		ServerData serverData = entry.getServerData();
		return PlaytimeStorage.getServerMillis(serverData.name, serverData.ip);
	}
}
