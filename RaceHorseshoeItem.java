package com.steelballrun;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

/** Clic derecho sobre un caballo/burro/mula: velocidad y salto de carrera. */
public class RaceHorseshoeItem extends Item {
    public RaceHorseshoeItem(Settings settings) { super(settings); }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof AbstractHorseEntity) {
            if (!user.getWorld().isClient) {
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6000, 2));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 6000, 1));
                if (!user.getAbilities().creativeMode) stack.decrement(1);
            }
            return ActionResult.success(user.getWorld().isClient);
        }
        return ActionResult.PASS;
    }
}
