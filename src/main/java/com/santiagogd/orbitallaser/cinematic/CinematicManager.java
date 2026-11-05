package com.santiagogd.orbitallaser.cinematic;

import com.santiagogd.orbitallaser.OrbitalLaserMod;
import com.santiagogd.orbitallaser.entity.SatelliteEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 3-minute orbital cinematic with a real visible satellite.
 * Camera path prioritises seeing the planet and then the satellite clearly.
 */
@OnlyIn(Dist.CLIENT)
public final class CinematicManager {

    public static final int TOTAL_DURATION_TICKS = 20 * 60 * 3; // 3 minutes

    private static boolean playing;
    private static boolean previousHideGui;
    private static int tick;

    private static Vec3 target = Vec3.ZERO;
    private static Vec3 startCamera = Vec3.ZERO;
    private static Vec3 cameraPosition = Vec3.ZERO;

    private static float yaw, pitch, roll;
    private static float fov = 70.0F;

    private static ArmorStand cameraAnchor;
    private static Entity previousCameraEntity;
    private static SatelliteEntity satelliteEntity;

    // Height of the satellite (still high, but we keep looking at the planet more)
    private static final double SATELLITE_HEIGHT = 520.0;

    private CinematicManager() {}

    public static void startCinematic(Vec3 impactTarget) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (playing) stopCinematic();

        target = impactTarget;
        startCamera = mc.gameRenderer.getMainCamera().getPosition();
        cameraPosition = startCamera;
        previousCameraEntity = mc.getCameraEntity();

        float startYaw = mc.player.getYRot();
        float startPitch = mc.player.getXRot();

        // Camera anchor
        cameraAnchor = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(mc.level);
        if (cameraAnchor == null) {
            OrbitalLaserMod.LOGGER.error("Failed to create camera anchor");
            return;
        }
        cameraAnchor.setInvisible(true);
        cameraAnchor.setNoGravity(true);
        cameraAnchor.setSilent(true);
        cameraAnchor.setCustomNameVisible(false);
        cameraAnchor.absMoveTo(startCamera.x, startCamera.y, startCamera.z, startYaw, startPitch);
        mc.level.addFreshEntity(cameraAnchor);
        mc.setCameraEntity(cameraAnchor);

        // Spawn the real satellite high above the target
        satelliteEntity = OrbitalLaserMod.SATELLITE.get().create(mc.level);
        if (satelliteEntity != null) {
            satelliteEntity.setPos(target.x, target.y + SATELLITE_HEIGHT, target.z);
            satelliteEntity.setCharge(0f);
            satelliteEntity.setFiring(false);
            mc.level.addFreshEntity(satelliteEntity);
            OrbitalLaserMod.LOGGER.info("Satellite spawned at Y={}", target.y + SATELLITE_HEIGHT);
        }

        yaw = startYaw;
        pitch = startPitch;
        roll = 0.0F;
        fov = 70.0F;
        tick = 0;
        previousHideGui = mc.options.hideGui;
        playing = true;

        OrbitalLaserMod.LOGGER.info("=== ORBITAL CINEMATIC STARTED ===");
    }

    public static void stopCinematic() {
        if (!playing) return;
        playing = false;
        tick = 0;
        roll = 0.0F;

        Minecraft mc = Minecraft.getInstance();
        if (cameraAnchor != null) {
            if (mc.getCameraEntity() == cameraAnchor) {
                Entity restore = (previousCameraEntity != null && previousCameraEntity.isAlive())
                        ? previousCameraEntity : mc.player;
                mc.setCameraEntity(restore);
            }
            cameraAnchor.discard();
            cameraAnchor = null;
        }
        if (satelliteEntity != null) {
            satelliteEntity.discard();
            satelliteEntity = null;
        }
        previousCameraEntity = null;
        mc.options.hideGui = previousHideGui;
        OrbitalLaserMod.LOGGER.info("Orbital cinematic finished.");
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static int getCurrentTick() {
        return tick;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !playing) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stopCinematic();
            return;
        }

        tick++;
        if (tick >= TOTAL_DURATION_TICKS) {
            stopCinematic();
            return;
        }

        mc.options.hideGui = true;
        updateShot(tick);
        syncCameraAnchor();
        updateSatellite();
        spawnEffects(mc);
    }

    private static void updateSatellite() {
        if (satelliteEntity == null || !satelliteEntity.isAlive()) return;

        // Keep it locked in position
        satelliteEntity.setPos(target.x, target.y + SATELLITE_HEIGHT, target.z);

        float charge = 0f;
        boolean firing = false;

        if (tick >= 1800 && tick < 2700) {
            charge = Mth.clamp((tick - 1800) / 900.0f, 0f, 1f);
        }
        if (tick >= 2650 && tick < 3200) {
            firing = true;
            charge = 1f;
        }

        satelliteEntity.setCharge(charge);
        satelliteEntity.setFiring(firing);
    }

    private static void updateShot(int t) {
        Vec3 satellite = target.add(0.0, SATELLITE_HEIGHT, 0.0);

        // =========================================================
        // PHASE 1 (0:00-0:35) Rise while looking at the ground
        // =========================================================
        if (t < 700) {
            float p = ease(t / 700.0F);
            cameraPosition = startCamera.lerp(target.add(0, 60 + 140 * p, 30), p);
            // Look mostly downward so the player sees the world
            aimAt(target.add(0, 10, 0));
            fov = lerp(70, 85, p);
            roll = (float) Math.sin(p * Math.PI) * 2.0F;
        }

        // =========================================================
        // PHASE 2 (0:35-1:25) High orbit looking DOWN at the planet
        // This is the key "see the world" phase
        // =========================================================
        else if (t < 1700) {
            float p = ease((t - 700) / 1000.0F);
            double height = 200 + p * 280;          // 200 → 480
            double radius = 220 - p * 80;           // keep some distance
            double angle = p * Math.PI * 1.8;

            cameraPosition = target.add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
            );
            // Always look at the ground so the planet stays in frame
            aimAt(target.add(0, 5, 0));
            fov = lerp(85, 58, p);
            roll = (float) Math.sin(p * Math.PI * 2) * 3.5F;
        }

        // =========================================================
        // PHASE 3 (1:25-1:55) Climb toward satellite while still
        // seeing the planet, then turn to look at the satellite
        // =========================================================
        else if (t < 2300) {
            float p = ease((t - 1700) / 600.0F);
            cameraPosition = lerpVec(
                    cameraPosition,
                    satellite.add(55, 25, 55),
                    0.04f
            );
            // Blend look target from ground → satellite
            Vec3 look = target.add(0, 5, 0).lerp(satellite, p);
            aimAt(look);
            fov = lerp(58, 40, p);
            roll = lerp(roll, 0, 0.1f);
        }

        // =========================================================
        // PHASE 4 (1:55-2:25) Close orbit around the satellite
        // Player should clearly see the satellite model
        // =========================================================
        else if (t < 2800) {
            float p = (t - 2300) / 500.0F;
            double orbit = t * 0.03;
            cameraPosition = satellite.add(
                    Math.cos(orbit) * 28,
                    12 + Math.sin(t * 0.04) * 5,
                    Math.sin(orbit) * 28
            );
            aimAt(satellite);
            fov = lerp(40, 48, ease(p));
            roll = (float) Math.sin(p * Math.PI) * 1.2F;

            if (t == 2650) {
                OrbitalLaserMod.LOGGER.info(">>> ORBITAL LASER FIRED <<<");
            }
        }

        // =========================================================
        // PHASE 5 (2:25-2:50) Follow the beam down to the planet
        // =========================================================
        else if (t < 3400) {
            float p = ease((t - 2800) / 600.0F);
            Vec3 beamPoint = satellite.lerp(target, p);
            cameraPosition = beamPoint.add(
                    Math.cos(t * 0.02) * 35,
                    20 + (1 - p) * 40,
                    Math.sin(t * 0.02) * 35
            );
            aimAt(beamPoint);
            fov = lerp(48, 75, p);
            roll = 0;
        }

        // =========================================================
        // PHASE 6 (2:50-3:00) Impact hold
        // =========================================================
        else {
            float p = ease((t - 3400) / 200.0F);
            cameraPosition = target.add(35 - 12 * p, 28 + 8 * p, 35 - 12 * p);
            aimAt(target.add(0, 4, 0));
            fov = lerp(75, 70, p);
            roll = (float) Math.sin(p * Math.PI) * 0.8F;
        }
    }

    private static void aimAt(Vec3 point) {
        Vec3 dir = point.subtract(cameraPosition).normalize();
        yaw = (float) (Mth.atan2(dir.z, dir.x) * (180.0 / Math.PI)) - 90.0F;
        pitch = (float) (-Math.asin(Mth.clamp(dir.y, -1.0, 1.0)) * (180.0 / Math.PI));
    }

    private static void syncCameraAnchor() {
        if (cameraAnchor == null || !cameraAnchor.isAlive()) return;
        double eye = cameraAnchor.getEyeHeight();
        cameraAnchor.absMoveTo(
                cameraPosition.x,
                cameraPosition.y - eye,
                cameraPosition.z,
                yaw, pitch
        );
    }

    private static void spawnEffects(Minecraft mc) {
        if (mc.level == null) return;

        Vec3 satellite = target.add(0, SATELLITE_HEIGHT, 0);

        // Charge particles around satellite
        if (tick >= 1800 && tick < 2800 && tick % 2 == 0) {
            float charge = Mth.clamp((tick - 1800) / 900.0F, 0, 1);
            int count = 3 + (int) (charge * 6);
            for (int i = 0; i < count; i++) {
                double a = mc.level.random.nextDouble() * Math.PI * 2;
                double r = 4.0 + mc.level.random.nextDouble() * (5.0 + charge * 10.0);
                mc.level.addParticle(ParticleTypes.END_ROD,
                        satellite.x + Math.cos(a) * r,
                        satellite.y + (mc.level.random.nextDouble() - 0.4) * 4,
                        satellite.z + Math.sin(a) * r,
                        0, 0.05 + charge * 0.08, 0);
            }
        }

        // FIRE burst
        if (tick >= 2650 && tick < 2750) {
            for (int i = 0; i < 18; i++) {
                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        satellite.x + (mc.level.random.nextDouble() - 0.5) * 8,
                        satellite.y + (mc.level.random.nextDouble() - 0.5) * 5,
                        satellite.z + (mc.level.random.nextDouble() - 0.5) * 8,
                        0, 0.2, 0);
                mc.level.addParticle(ParticleTypes.FLASH,
                        satellite.x, satellite.y, satellite.z, 0, 0, 0);
            }
        }

        // Thick beam
        if (tick >= 2680 && tick < 3400) {
            float p = Mth.clamp((tick - 2680) / 720.0F, 0, 1);
            Vec3 beamPoint = satellite.lerp(target, p);

            // Dense core
            for (int i = 0; i < 10; i++) {
                mc.level.addParticle(ParticleTypes.END_ROD,
                        beamPoint.x + (mc.level.random.nextDouble() - 0.5) * 1.8,
                        beamPoint.y + (mc.level.random.nextDouble() - 0.5) * 3,
                        beamPoint.z + (mc.level.random.nextDouble() - 0.5) * 1.8,
                        0, -0.6, 0);
            }
            // Outer glow
            if (tick % 2 == 0) {
                for (int i = 0; i < 4; i++) {
                    mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            beamPoint.x + (mc.level.random.nextDouble() - 0.5) * 3.5,
                            beamPoint.y,
                            beamPoint.z + (mc.level.random.nextDouble() - 0.5) * 3.5,
                            0, -0.3, 0);
                }
            }
        }

        // Impact
        if (tick >= 3350 && tick < 3550 && tick % 2 == 0) {
            for (int i = 0; i < 10; i++) {
                mc.level.addParticle(ParticleTypes.EXPLOSION,
                        target.x + (mc.level.random.nextDouble() - 0.5) * 14,
                        target.y + mc.level.random.nextDouble() * 10,
                        target.z + (mc.level.random.nextDouble() - 0.5) * 14,
                        0, 0.15, 0);
                mc.level.addParticle(ParticleTypes.LAVA,
                        target.x + (mc.level.random.nextDouble() - 0.5) * 12,
                        target.y + 1,
                        target.z + (mc.level.random.nextDouble() - 0.5) * 12,
                        0, 0.3, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!playing) return;
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(roll);
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!playing) return;
        event.setFOV(fov);
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Mth.clamp(t, 0, 1);
    }

    private static Vec3 lerpVec(Vec3 a, Vec3 b, float t) {
        return a.add(b.subtract(a).scale(Mth.clamp(t, 0, 1)));
    }
}
