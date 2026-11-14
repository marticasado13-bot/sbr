package com.steelballrun;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class SteelBallEntity extends ThrownItemEntity {
    private float power = 0.5f;

    public SteelBallEntity(EntityType<? extends SteelBallEntity> type, World world) {
        super(type, world);
    }

    public SteelBallEntity(World world, LivingEntity owner) {
        super(SteelBallRun.STEEL_BALL_ENTITY, owner, world);
    }

    public void setPower(float power) { this.power = power; }

    @Override
    protected Item getDefaultItem() { return SteelBallRun.STEEL_BALL; }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        super.onEntityHit(result);
        Entity target = result.getEntity();
        float damage = 4.0f + power * 8.0f;
        target.damage(getDamageSources().thrown(this, getOwner()), damage);
        // Giro completo: el objetivo queda "paralizado" y mareado
        if (target instanceof LivingEntity living && power >= 0.9f) {
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0));
        }
    }

    @Override
    protected void onCollision(HitResult result) {
        super.onCollision(result);
        if (!getWorld().isClient) {
            this.dropStack(new ItemStack(SteelBallRun.STEEL_BALL));
            this.discard();
        }
    }
}
