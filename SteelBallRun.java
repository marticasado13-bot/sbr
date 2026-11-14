package com.steelballrun;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SteelBallRun implements ModInitializer {
    public static final String MOD_ID = "steelballrun";
    public static final Identifier TOGGLE_STAND = new Identifier(MOD_ID, "toggle_stand");
    public static final Identifier USE_ABILITY = new Identifier(MOD_ID, "use_ability");

    public static final Item STEEL_BALL = Registry.register(Registries.ITEM,
            new Identifier(MOD_ID, "steel_ball"),
            new SteelBallItem(new FabricItemSettings().maxCount(16)));

    public static final Item RACE_HORSESHOE = Registry.register(Registries.ITEM,
            new Identifier(MOD_ID, "race_horseshoe"),
            new RaceHorseshoeItem(new FabricItemSettings().maxCount(8)));

    public static final Item AWAKENING_ARROW = Registry.register(Registries.ITEM,
            new Identifier(MOD_ID, "awakening_arrow"),
            new AwakeningArrowItem(new FabricItemSettings().maxCount(4)));

    public static final EntityType<SteelBallEntity> STEEL_BALL_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "steel_ball"),
            FabricEntityTypeBuilder.<SteelBallEntity>create(SpawnGroup.MISC, SteelBallEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeBlocks(4).trackedUpdateRate(10).build());

    public static final EntityType<StandEntity> STAND_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "stand"),
            FabricEntityTypeBuilder.<StandEntity>createMob()
                    .spawnGroup(SpawnGroup.MISC)
                    .entityFactory(StandEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
                    .defaultAttributes(StandEntity::createStandAttributes)
                    .trackRangeBlocks(64).trackedUpdateRate(1).build());

    public static final EntityType<SpiralShotEntity> SPIRAL_SHOT_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(MOD_ID, "spiral_shot"),
            FabricEntityTypeBuilder.<SpiralShotEntity>create(SpawnGroup.MISC, SpiralShotEntity::new)
                    .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
                    .trackRangeBlocks(4).trackedUpdateRate(10).build());

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(e -> {
            e.add(STEEL_BALL);
            e.add(AWAKENING_ARROW);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(e -> e.add(RACE_HORSESHOE));

        // Tecla de invocación (cliente -> servidor)
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_STAND,
                (server, player, handler, buf, sender) -> server.execute(() -> StandManager.toggle(player)));

        ServerPlayNetworking.registerGlobalReceiver(USE_ABILITY,
                (server, player, handler, buf, sender) -> server.execute(() -> StandManager.useAbility(player)));

        // El Stand sobrevive a la muerte y se guarda al salir
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> StandManager.copyTags(oldPlayer, newPlayer));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> StandManager.dismiss(handler.getPlayer()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> StandManager.clear());

        // /stand set <tipo>  y  /stand remove  (solo operadores)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("stand")
                    .requires(src -> src.hasPermissionLevel(2));
            LiteralArgumentBuilder<ServerCommandSource> set = CommandManager.literal("set");
            for (StandType t : StandType.values()) {
                set.then(CommandManager.literal(t.id).executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
                    StandManager.setType(p, t);
                    ctx.getSource().sendFeedback(() -> Text.translatable("message.steelballrun.awakened", t.displayName()), false);
                    return 1;
                }));
            }
            root.then(set);
            root.then(CommandManager.literal("remove").executes(ctx -> {
                StandManager.removeType(ctx.getSource().getPlayerOrThrow());
                return 1;
            }));
            dispatcher.register(root);
        });
    }
}
