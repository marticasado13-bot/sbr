package com.steelballrun;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/** Fragmento giratorio disparado por el Stand Thorn Spiral. */
public class SpiralShotEntity extends ThrownItemEntity {
    public SpiralShotEntity(EntityType<? extends SpiralShotEntity> type, World world) { super(type, world); }

    public SpiralShotEntity(World world, LivingEntity owner) {
        super(SteelBallRun.SPIRAL_SHOT_ENTITY, owner, world);
    }

    @Override
    protected Item getDefaultItem() { return Items.AMETHYST_SHARD; }

    @Override
    protected float getGravity() { return 0.0f; }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && age > 40) discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        super.onEntityHit(result);
        Entity target = result.getEntity();
        target.damage(getDamageSources().thrown(this, getOwner()), 5.0f);
    }

    @Override
    protected void onCollision(HitResult result) {
        super.onCollision(result);
        if (!getWorld().isClient) discard();
    }
}
