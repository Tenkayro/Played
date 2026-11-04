package de.tenkayro.client.mixin;

import de.tenkayro.client.PlaytimeTracker;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardActivityMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void played$input(long window, int action, KeyEvent event, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().handle() && client.isWindowActive() && action != 0) PlaytimeTracker.activity();
    }
}
