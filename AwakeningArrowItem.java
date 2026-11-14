package com.steelballrun;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Clic derecho: te atraviesa y despierta un Stand aleatorio (cuesta 2 corazones). */
public class AwakeningArrowItem extends Item {
    public AwakeningArrowItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);

        ServerPlayerEntity sp = (ServerPlayerEntity) player;
        if (StandManager.getType(sp) != null) {
            sp.sendMessage(Text.translatable("message.steelballrun.already_awakened"), true);
            return TypedActionResult.fail(stack);
        }
        if (sp.getHealth() <= 6.0f) {
            sp.sendMessage(Text.translatable("message.steelballrun.too_weak"), true);
            return TypedActionResult.fail(stack);
        }

        sp.damage(sp.getDamageSources().magic(), 4.0f);
        StandType type = StandType.values()[sp.getRandom().nextInt(StandType.values().length)];
        StandManager.setType(sp, type);
        if (!sp.getAbilities().creativeMode) stack.decrement(1);
        sp.getItemCooldownManager().set(this, 20);

        world.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 0.6f);
        sp.sendMessage(Text.translatable("message.steelballrun.awakened", type.displayName()), false);
        return TypedActionResult.success(stack);
    }
}
