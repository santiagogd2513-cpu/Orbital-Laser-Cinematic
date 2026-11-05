package com.santiagogd.orbitallaser.cinematic;

import com.santiagogd.orbitallaser.OrbitalLaserMod;
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
 * High-intensity 3-minute orbital cinematic.
 * Camera is fully detached via an invisible ArmorStand and flies hundreds of blocks
 * above the world so the player actually sees the planet fall away.
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

    // Satellite is placed extremely high so it feels like real orbit
    private static final double SATELLITE_HEIGHT = 780.0;

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

        yaw = startYaw;
        pitch = startPitch;
        roll = 0.0F;
        fov = 70.0F;
        tick = 0;
        previousHideGui = mc.options.hideGui;
        playing = true;

        OrbitalLaserMod.LOGGER.info("=== ORBITAL CINEMATIC STARTED === Target: {}  Satellite height: {}", target, SATELLITE_HEIGHT);
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
        spawnEffects(mc);
    }

    private static void updateShot(int t) {
        Vec3 satellite = target.add(0.0, SATELLITE_HEIGHT, 0.0);

        // =========================================================
        // PHASE 1  (0:00 - 0:30)  Slow rise + look up
        // =========================================================
        if (t < 600) {
            float p = ease(t / 600.0F);
            cameraPosition = startCamera.lerp(
                    target.add(0, 80 + 120 * p, 0),
                    p
            );
            aimAt(target.add(0, 200 + 300 * p, 0));
            fov = lerp(70, 90, p);
            roll = (float) Math.sin(p * Math.PI) * 2.5F;
        }

        // =========================================================
        // PHASE 2  (0:30 - 1:20)  LEAVE THE PLANET - aggressive climb
        // =========================================================
        else if (t < 1600) {
            float p = ease((t - 600) / 1000.0F);
            double height = 80 + p * (SATELLITE_HEIGHT - 40);
            double radius = 180 - p * 90;
            double angle = p * Math.PI * 1.6;

            cameraPosition = target.add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
            );
            aimAt(target.add(0, height * 0.4, 0));
            fov = lerp(90, 62, p);
            roll = (float) Math.sin(p * Math.PI * 2.2) * 4.0F;
        }

        // =========================================================
        // PHASE 3  (1:20 - 1:55)  Approach satellite
        // =========================================================
        else if (t < 2300) {
            float p = ease((t - 1600) / 700.0F);
            cameraPosition = lerpVec(
                    cameraPosition,
                    satellite.add(45, 18, 45),
                    0.035f + p * 0.02f
            );
            aimAt(satellite);
            fov = lerp(62, 42, p);
            roll = lerp(roll, 0, 0.08f);
        }

        // =========================================================
        // PHASE 4  (1:55 - 2:20)  Charge + FIRE
        // =========================================================
        else if (t < 2800) {
            float p = (t - 2300) / 500.0F;
            double orbit = t * 0.025;
            cameraPosition = satellite.add(
                    Math.cos(orbit) * 32,
                    14 + Math.sin(t * 0.03) * 4,
                    Math.sin(orbit) * 32
            );
            aimAt(satellite);
            fov = lerp(42, 55, ease(p));
            roll = (float) Math.sin(p * Math.PI) * 1.5F;

            if (t == 2650) {
                OrbitalLaserMod.LOGGER.info(">>> ORBITAL LASER FIRED <<<");
            }
        }

        // =========================================================
        // PHASE 5  (2:20 - 2:50)  Follow the beam down
        // =========================================================
        else if (t < 3400) {
            float p = ease((t - 2800) / 600.0F);
            Vec3 beamPoint = satellite.lerp(target, p);
            cameraPosition = beamPoint.add(
                    Math.cos(t * 0.015) * 40,
                    25 + (1 - p) * 30,
                    Math.sin(t * 0.015) * 40
            );
            aimAt(beamPoint);
            fov = lerp(55, 78, p);
            roll = 0;
        }

        // =========================================================
        // PHASE 6  (2:50 - 3:00)  Impact + hold
        // =========================================================
        else {
            float p = ease((t - 3400) / 200.0F);
            cameraPosition = target.add(40 - 15 * p, 35 + 10 * p, 40 - 15 * p);
            aimAt(target.add(0, 5, 0));
            fov = lerp(78, 70, p);
            roll = (float) Math.sin(p * Math.PI) * 1.0F;
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
        if (tick >= 1600 && tick < 2800 && tick % 2 == 0) {
            float charge = Mth.clamp((tick - 1600) / 1000.0F, 0, 1);
            for (int i = 0; i < 4; i++) {
                double a = mc.level.random.nextDouble() * Math.PI * 2;
                double r = 3.0 + mc.level.random.nextDouble() * (4.0 + charge * 8.0);
                mc.level.addParticle(ParticleTypes.END_ROD,
                        satellite.x + Math.cos(a) * r,
                        satellite.y + (mc.level.random.nextDouble() - 0.3) * 3,
                        satellite.z + Math.sin(a) * r,
                        0, 0.04 + charge * 0.06, 0);
            }
        }

        // FIRE moment - heavy particles
        if (tick >= 2650 && tick < 2750) {
            for (int i = 0; i < 12; i++) {
                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        satellite.x + (mc.level.random.nextDouble() - 0.5) * 6,
                        satellite.y + (mc.level.random.nextDouble() - 0.5) * 4,
                        satellite.z + (mc.level.random.nextDouble() - 0.5) * 6,
                        0, 0.15, 0);
            }
        }

        // Beam particles (descending)
        if (tick >= 2700 && tick < 3400 && tick % 3 == 0) {
            float p = (tick - 2700) / 700.0F;
            Vec3 beamPoint = satellite.lerp(target, p);
            for (int i = 0; i < 6; i++) {
                mc.level.addParticle(ParticleTypes.END_ROD,
                        beamPoint.x + (mc.level.random.nextDouble() - 0.5) * 2.5,
                        beamPoint.y + (mc.level.random.nextDouble() - 0.5) * 4,
                        beamPoint.z + (mc.level.random.nextDouble() - 0.5) * 2.5,
                        0, -0.4, 0);
            }
        }

        // Impact
        if (tick >= 3350 && tick < 3500 && tick % 2 == 0) {
            for (int i = 0; i < 8; i++) {
                mc.level.addParticle(ParticleTypes.EXPLOSION,
                        target.x + (mc.level.random.nextDouble() - 0.5) * 12,
                        target.y + mc.level.random.nextDouble() * 8,
                        target.z + (mc.level.random.nextDouble() - 0.5) * 12,
                        0, 0.1, 0);
                mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        target.x + (mc.level.random.nextDouble() - 0.5) * 10,
                        target.y + 1,
                        target.z + (mc.level.random.nextDouble() - 0.5) * 10,
                        0, 0.2, 0);
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

    // ---------- helpers ----------
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
