package de.tenkayro.client.mixin;

import de.tenkayro.client.PlaytimeTracker;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseActivityMixin {
    @Unique private double played$lastX = Double.NaN;
    @Unique private double played$lastY = Double.NaN;

    @Unique private void played$activity(long window) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().handle() && client.isWindowActive()) PlaytimeTracker.activity();
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void played$move(long window, double x, double y, double dx, double dy, CallbackInfo ci) {
        if (!Double.isNaN(played$lastX) && (x != played$lastX || y != played$lastY || dx != 0 || dy != 0)) played$activity(window);
        played$lastX = x;
        played$lastY = y;
    }

    @Inject(method = "onButton", at = @At("HEAD"))
    private void played$button(long window, MouseButtonInfo button, int action, CallbackInfo ci) {
        if (action != 0) played$activity(window);
    }

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void played$scroll(long window, double x, double y, CallbackInfo ci) {
        if (x != 0 || y != 0) played$activity(window);
    }
}
