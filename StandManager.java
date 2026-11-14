package com.steelballrun;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Guarda el Stand del jugador en etiquetas de comando (persisten en el NBT del jugador). */
public final class StandManager {
    public static final String TAG_PREFIX = "sbr_stand_";
    private static final Map<UUID, UUID> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> ABILITY_READY = new HashMap<>();

    private StandManager() {}

    public static StandType getType(PlayerEntity player) {
        for (String tag : player.getCommandTags()) {
            if (tag.startsWith(TAG_PREFIX)) {
                StandType t = StandType.byId(tag.substring(TAG_PREFIX.length()));
                if (t != null) return t;
            }
        }
        return null;
    }

    public static void setType(ServerPlayerEntity player, StandType type) {
        removeType(player);
        player.addCommandTag(TAG_PREFIX + type.id);
    }

    public static void removeType(ServerPlayerEntity player) {
        dismiss(player);
        for (StandType t : StandType.values()) player.removeCommandTag(TAG_PREFIX + t.id);
    }

    public static void copyTags(ServerPlayerEntity from, ServerPlayerEntity to) {
        StandType t = getType(from);
        if (t != null) to.addCommandTag(TAG_PREFIX + t.id);
    }

    /** Alterna invocar / guardar el Stand. */
    public static void toggle(ServerPlayerEntity player) {
        StandType type = getType(player);
        if (type == null) {
            player.sendMessage(Text.translatable("message.steelballrun.no_stand"), true);
            return;
        }
        if (dismiss(player)) {
            player.sendMessage(Text.translatable("message.steelballrun.dismissed"), true);
            return;
        }
        ServerWorld world = player.getServerWorld();
        StandEntity stand = new StandEntity(SteelBallRun.STAND_ENTITY, world);
        stand.setOwner(player);
        stand.setVariant(type.ordinal());
        stand.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0f);
        world.spawnEntity(stand);
        ACTIVE.put(player.getUuid(), stand.getUuid());
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.6f);
        player.sendMessage(Text.translatable("message.steelballrun.summoned", type.displayName()), true);
    }

    /** @return true si había un Stand activo y se guardó. */
    public static boolean dismiss(ServerPlayerEntity player) {
        UUID standId = ACTIVE.remove(player.getUuid());
        if (standId == null) return false;
        Entity e = player.getServerWorld().getEntity(standId);
        if (e == null) return false;
        e.discard();
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.6f);
        return true;
    }

    /** Usa la habilidad activa del Stand (requiere tenerlo invocado). */
    public static void useAbility(ServerPlayerEntity player) {
        StandType type = getType(player);
        if (type == null) {
            player.sendMessage(Text.translatable("message.steelballrun.no_stand"), true);
            return;
        }
        UUID standId = ACTIVE.get(player.getUuid());
        Entity e = standId == null ? null : player.getServerWorld().getEntity(standId);
        if (!(e instanceof StandEntity stand)) {
            player.sendMessage(Text.translatable("message.steelballrun.summon_first"), true);
            return;
        }
        long now = player.getServerWorld().getTime();
        Long ready = ABILITY_READY.get(player.getUuid());
        if (ready != null && now < ready) {
            player.sendMessage(Text.translatable("message.steelballrun.cooldown", (ready - now + 19) / 20), true);
            return;
        }
        int cooldown = stand.useAbility(player, type);
        ABILITY_READY.put(player.getUuid(), now + cooldown);
        player.sendMessage(Text.translatable("message.steelballrun.ability", type.abilityName()), true);
    }

    public static void clear() { ACTIVE.clear(); ABILITY_READY.clear(); }
}
