package com.santiagogd.orbitallaser.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Orbital Satellite entity.
 * Used during the cinematic as the weapon platform that charges and fires the beam.
 */
public class SatelliteEntity extends Entity {

    private static final EntityDataAccessor<Float> CHARGE =
            SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.FLOAT);

    private static final EntityDataAccessor<Boolean> FIRING =
            SynchedEntityData.defineId(SatelliteEntity.class, EntityDataSerializers.BOOLEAN);

    public SatelliteEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(CHARGE, 0.0f);
        this.entityData.define(FIRING, false);
    }

    public float getCharge() {
        return this.entityData.get(CHARGE);
    }

    public void setCharge(float charge) {
        this.entityData.set(CHARGE, Math.max(0f, Math.min(1f, charge)));
    }

    public boolean isFiring() {
        return this.entityData.get(FIRING);
    }

    public void setFiring(boolean firing) {
        this.entityData.set(FIRING, firing);
    }

    @Override
    public void tick() {
        super.tick();

        // Slow rotation for visual presence
        this.setYRot(this.getYRot() + 0.4f);

        if (!this.level().isClientSide) {
            // Server-side charge logic can be driven by the cinematic later
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setCharge(tag.getFloat("Charge"));
        setFiring(tag.getBoolean("Firing"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Charge", getCharge());
        tag.putBoolean("Firing", isFiring());
    }
}