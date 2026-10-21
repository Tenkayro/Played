package de.zorblock.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.core.RegistryAccess;

public final class MiniMessageUtil {
    private static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private MiniMessageUtil() {
    }

    @SuppressWarnings("null")
    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        
        try {
            net.kyori.adventure.text.Component kyoriComponent = MINIMESSAGE.deserialize(input);
            String json = GSON.serialize(kyoriComponent);
            JsonElement jsonElement = JsonParser.parseString(json);
            
            return ComponentSerialization.CODEC.parse(
                RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE), 
                jsonElement
            ).getOrThrow();
        } catch (Exception e) {
            return Component.literal(input);
        }
    }
}
