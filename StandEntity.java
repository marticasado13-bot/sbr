package com.steelballrun;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.HitResult;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Stand espectral: sigue al dueño, ataca a sus enemigos y le da una aura según su tipo. */
public class StandEntity extends PathAwareEntity {
    private static final TrackedData<Integer> VARIANT =
            DataTracker.registerData(StandEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private UUID ownerId;
    private int attackCooldown = 0;
    private int barrageTicks = 0;

    public StandEntity(EntityType<? extends StandEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createStandAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() { return MathHelper.clamp(this.dataTracker.get(VARIANT), 0, StandType.values().length - 1); }
    public void setVariant(int v) { this.dataTracker.set(VARIANT, v); }
    public void setOwner(ServerPlayerEntity owner) { this.ownerId = owner.getUuid(); }

    private ServerPlayerEntity getOwnerPlayer() {
        if (ownerId == null || getServer() == null) return null;
        return getServer().getPlayerManager().getPlayer(ownerId);
    }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient) return;

        ServerPlayerEntity owner = getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.isSpectator()
                || owner.getWorld() != getWorld() || StandManager.getType(owner) == null) {
            discard();
            return;
        }

        StandType type = StandType.values()[getVariant()];
        setVelocity(Vec3d.ZERO);
        if (attackCooldown > 0) attackCooldown--;
        if (barrageTicks > 0) barrageTicks--;

        LivingEntity target = findTarget(owner);
        Vec3d goal;
        float yaw;
        if (target != null) {
            Vec3d dir = target.getPos().subtract(owner.getPos());
            dir = new Vec3d(dir.x, 0, dir.z);
            if (dir.lengthSquared() < 1.0E-4) dir = owner.getRotationVector().multiply(1, 0, 1);
            dir = dir.normalize();
            goal = target.getPos().subtract(dir.multiply(1.3));
            yaw = (float) (MathHelper.atan2(dir.z, dir.x) * 57.29578) - 90.0f;
        } else {
            double r = Math.toRadians(owner.getYaw());
            goal = owner.getPos().add(1.1 * Math.sin(r) - 0.9 * Math.cos(r), 0.1,
                    -1.1 * Math.cos(r) - 0.9 * Math.sin(r));
            yaw = owner.getYaw();
        }

        setPosition(getPos().lerp(goal, 0.45));
        setYaw(yaw);
        this.bodyYaw = yaw;
        this.headYaw = yaw;
        setPitch(0f);

        if (target != null && attackCooldown <= 0 && distanceTo(target) <= type.reach) {
            attack(owner, target, type);
            attackCooldown = barrageTicks > 0 ? 2 : type.cooldown;
        }
        if (age % 20 == 0) applyAura(owner, type);
    }

    private LivingEntity findTarget(ServerPlayerEntity owner) {
        LivingEntity t = owner.getAttacking();
        if (valid(t, owner) && owner.age - owner.getLastAttackTime() < 100) return t;
        t = owner.getAttacker();
        if (valid(t, owner) && owner.age - owner.getLastAttackedTime() < 100) return t;
        List<HostileEntity> list = getWorld().getEntitiesByClass(HostileEntity.class,
                owner.getBoundingBox().expand(6.0), e -> valid(e, owner));
        return list.stream()
                .min(Comparator.comparingDouble((HostileEntity e) -> owner.squaredDistanceTo(e)))
                .orElse(null);
    }

    private boolean valid(LivingEntity e, PlayerEntity owner) {
        return e != null && e != this && e != owner && !(e instanceof StandEntity)
                && e.isAlive() && owner.distanceTo(e) < 16.0f;
    }

    private void attack(ServerPlayerEntity owner, LivingEntity target, StandType type) {
        swingHand(Hand.MAIN_HAND);
        if (target.damage(getDamageSources().playerAttack(owner), type.damage)) {
            target.takeKnockback(type.knockback, getX() - target.getX(), getZ() - target.getZ());
            switch (type) {
                case TIDAL_ECHO -> owner.heal(1.0f);
                case ASHEN_WARDEN -> target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 80, 0));
                case THORN_SPIRAL -> target.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 60, 0));
                case WILD_HUNT -> owner.heal(1.0f);
                default -> { }
            }
        }
        playSound(SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 1.0f, 0.9f);
    }

    private void applyAura(ServerPlayerEntity owner, StandType type) {
        switch (type) {
            case CRIMSON_VANGUARD ->
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, false, false));
            case TIDAL_ECHO -> {
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, false));
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0, true, false, false));
            }
            case ASHEN_WARDEN ->
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, false));
            case GATE_WARDEN ->
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 0, true, false, false));
            case THORN_SPIRAL ->
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 60, 0, true, false, false));
            case WILD_HUNT -> {
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1, true, false, false));
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 60, 1, true, false, false));
            }
        }
    }

    /** Habilidad activa (tecla C). Devuelve el cooldown en ticks. */
    public int useAbility(ServerPlayerEntity owner, StandType type) {
        ServerWorld w = (ServerWorld) getWorld();
        return switch (type) {
            case CRIMSON_VANGUARD -> {          // Lluvia de golpes durante 3 s
                barrageTicks = 60;
                playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 0.6f);
                yield 600;
            }
            case TIDAL_ECHO -> {                // Pulso de marea: cura grande
                owner.heal(6.0f);
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
                playSound(SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.0f, 1.2f);
                yield 600;
            }
            case ASHEN_WARDEN -> {              // Escudo de ceniza: absorción
                owner.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 200, 1));
                playSound(SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 0.8f);
                yield 600;
            }
            case GATE_WARDEN -> {               // Paso por la grieta: teletransporte
                riftStep(owner);
                yield 160;
            }
            case THORN_SPIRAL -> {              // Salva de 5 fragmentos giratorios
                for (int i = -2; i <= 2; i++) {
                    SpiralShotEntity s = new SpiralShotEntity(w, owner);
                    s.setVelocity(owner, owner.getPitch(), owner.getYaw() + i * 8.0f, 0.0f, 2.2f, 0.5f);
                    w.spawnEntity(s);
                }
                playSound(SoundEvents.ENTITY_ARROW_SHOOT, 1.0f, 1.4f);
                yield 300;
            }
            case WILD_HUNT -> {                 // Embestida salvaje
                feralRush(owner);
                yield 200;
            }
        };
    }

    private void riftStep(ServerPlayerEntity owner) {
        LivingEntity t = findTarget(owner);
        double x, y, z;
        if (t != null) {                        // aparece a la espalda del objetivo
            Vec3d d = t.getRotationVector().multiply(1, 0, 1);
            if (d.lengthSquared() < 1.0E-4) d = new Vec3d(0, 0, 1);
            d = d.normalize();
            x = t.getX() - d.x * 1.6;
            y = t.getY();
            z = t.getZ() - d.z * 1.6;
        } else {                                // o 10 bloques hacia donde miras
            HitResult hit = owner.raycast(10.0, 1.0f, false);
            Vec3d p = hit.getPos().subtract(owner.getRotationVector().multiply(0.6));
            x = p.x; y = p.y; z = p.z;
        }
        getWorld().playSound(null, owner.getX(), owner.getY(), owner.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);
        owner.requestTeleport(x, y, z);
        getWorld().playSound(null, x, y, z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }

    private void feralRush(ServerPlayerEntity owner) {
        Vec3d look = owner.getRotationVector();
        owner.setVelocity(look.x * 1.8, 0.35, look.z * 1.8);
        owner.velocityModified = true;
        owner.fallDistance = 0.0f;
        for (HostileEntity e : getWorld().getEntitiesByClass(HostileEntity.class,
                owner.getBoundingBox().expand(3.0, 1.0, 3.0), x -> valid(x, owner))) {
            e.damage(getDamageSources().playerAttack(owner), 6.0f);
            e.takeKnockback(0.8, owner.getX() - e.getX(), owner.getZ() - e.getZ());
        }
        playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 0.8f, 1.3f);
    }

    // El Stand es intangible e invulnerable
    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canHit() { return false; }
    @Override public boolean shouldSave() { return false; }
    @Override public boolean canImmediatelyDespawn(double distance) { return false; }
}
