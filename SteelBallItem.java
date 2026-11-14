package com.steelballrun;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/** Mantén clic derecho para cargar el giro y suelta para lanzar. */
public class SteelBallItem extends Item {
    public SteelBallItem(Settings settings) { super(settings); }

    @Override
    public UseAction getUseAction(ItemStack stack) { return UseAction.BOW; }

    @Override
    public int getMaxUseTime(ItemStack stack) { return 72000; }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        player.setCurrentHand(hand);
        return TypedActionResult.consume(player.getStackInHand(hand));
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (!(user instanceof PlayerEntity player)) return;
        int used = getMaxUseTime(stack) - remainingUseTicks;
        float power = Math.min(used / 30f, 1.0f); // 1.5 s para giro completo
        if (power < 0.1f) return;

        if (!world.isClient) {
            SteelBallEntity ball = new SteelBallEntity(world, player);
            ball.setItem(new ItemStack(SteelBallRun.STEEL_BALL));
            ball.setPower(power);
            ball.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, 1.5f + power * 2.0f, 0.5f);
            world.spawnEntity(ball);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.6f, 0.7f + power * 0.5f);
        if (!player.getAbilities().creativeMode) stack.decrement(1);
    }
}
