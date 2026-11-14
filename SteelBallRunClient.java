package com.steelballrun;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SteelBallRunClient implements ClientModInitializer {
    public static KeyBinding SUMMON_KEY;
    public static KeyBinding ABILITY_KEY;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(SteelBallRun.STEEL_BALL_ENTITY, FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(SteelBallRun.STAND_ENTITY, StandEntityRenderer::new);
        EntityRendererRegistry.register(SteelBallRun.SPIRAL_SHOT_ENTITY, FlyingItemEntityRenderer::new);

        SUMMON_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.steelballrun.summon_stand", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.steelballrun"));

        ABILITY_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.steelballrun.stand_ability", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, "key.categories.steelballrun"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ABILITY_KEY.wasPressed()) {
                if (client.player != null) ClientPlayNetworking.send(SteelBallRun.USE_ABILITY, PacketByteBufs.create());
            }
            while (SUMMON_KEY.wasPressed()) {
                if (client.player != null) ClientPlayNetworking.send(SteelBallRun.TOGGLE_STAND, PacketByteBufs.create());
            }
        });
    }
}
