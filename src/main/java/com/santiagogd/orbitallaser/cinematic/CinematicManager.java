package com.santiagogd.orbitallaser.cinematic;

import com.santiagogd.orbitallaser.OrbitalLaserMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client-only, deterministic six-phase cinematic.
 * Camera paths are evaluated from the original state each tick (never accumulated),
 * so frame rate and phase transitions cannot introduce positional drift.
 */
@OnlyIn(Dist.CLIENT)
public final class CinematicManager {
    public static final int TOTAL_DURATION_TICKS = 20 * 60 * 3;
    private static boolean playing;
    private static boolean previousHideGui;
    private static int tick;
    private static Vec3 target = Vec3.ZERO;
    private static Vec3 startCamera = Vec3.ZERO;
    private static Vec3 cameraPosition = Vec3.ZERO;
    private static float startYaw, startPitch;
    private static float yaw, pitch, roll;
    private static float fov = 70.0F;
    private static ArmorStand cameraAnchor;
    private static Entity previousCameraEntity;

    private CinematicManager() {}

    public static void startCinematic(Vec3 impactTarget) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        target = impactTarget;
        startCamera = mc.gameRenderer.getMainCamera().getPosition();
        cameraPosition = startCamera;
        previousCameraEntity = mc.getCameraEntity();
        cameraAnchor = net.minecraft.world.entity.EntityType.ARMOR_STAND.create(mc.level);
        if (cameraAnchor == null) return;
        cameraAnchor.setInvisible(true);
        cameraAnchor.setNoGravity(true);
        cameraAnchor.setMarker(true);
        cameraAnchor.absMoveTo(startCamera.x, startCamera.y, startCamera.z, startYaw, startPitch);
        mc.level.addFreshEntity(cameraAnchor);
        mc.setCameraEntity(cameraAnchor);
        startYaw = mc.player.getYRot();
        startPitch = mc.player.getXRot();
        yaw = startYaw;
        pitch = startPitch;
        roll = 0.0F;
        fov = 70.0F;
        tick = 0;
        previousHideGui = mc.options.hideGui;
        playing = true;
        OrbitalLaserMod.LOGGER.info("Orbital cinematic started at {}", target);
    }

    public static void stopCinematic() {
        if (!playing) return;
        playing = false;
        tick = 0;
        roll = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        if (cameraAnchor != null) {
            if (mc.getCameraEntity() == cameraAnchor) {
                Entity restore = previousCameraEntity != null && previousCameraEntity.isAlive()
                        ? previousCameraEntity : mc.player;
                mc.setCameraEntity(restore);
            }
            cameraAnchor.discard();
            cameraAnchor = null;
        }
        previousCameraEntity = null;
        mc.options.hideGui = previousHideGui;
        OrbitalLaserMod.LOGGER.info("Orbital cinematic stopped.");
    }

    public static boolean isPlaying() { return playing; }
    public static int getCurrentTick() { return tick; }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !playing) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            stopCinematic();
            return;
        }
        if (++tick >= TOTAL_DURATION_TICKS) {
            stopCinematic();
            return;
        }
        mc.options.hideGui = true;
        updateShot(tick);
        syncCameraAnchor();
        spawnAtmosphere(mc);
    }

    private static void updateShot(int t) {
        Vec3 satellite = target.add(0.0, 300.0, 0.0);
        float phase;
        if (t < 500) {
            phase = ease(t / 500.0F);
            cameraPosition = startCamera.lerp(target.add(0, 24 + 24 * phase, 42 - 18 * phase), phase);
            aimAt(target.add(0, 70, 0));
            fov = lerp(70, 82, phase);
            roll = (float)Math.sin(phase * Math.PI) * 1.2F;
        } else if (t < 1400) {
            phase = ease((t - 500) / 900.0F);
            double angle = phase * Math.PI * 1.25;
            cameraPosition = target.add(Math.cos(angle) * 190, 70 + phase * 210, Math.sin(angle) * 190);
            aimAt(target.add(0, 110, 0));
            fov = lerp(82, 68, phase);
            roll = (float)Math.sin(phase * Math.PI * 2) * 2.0F;
        } else if (t < 2200) {
            phase = ease((t - 1400) / 800.0F);
            cameraPosition = target.add(34 - phase * 16, 285 + phase * 8, 34 - phase * 16);
            aimAt(satellite);
            fov = lerp(68, 48, phase);
            roll = 0;
        } else if (t < 2700) {
            phase = (t - 2200) / 500.0F;
            double orbit = t * 0.018;
            cameraPosition = satellite.add(Math.cos(orbit) * 28, 12 + Math.sin(t * 0.025) * 3, Math.sin(orbit) * 28);
            aimAt(satellite);
            fov = lerp(48, 58, ease(phase));
            roll = (float)Math.sin(phase * Math.PI) * 0.8F;
        } else if (t < 3300) {
            phase = ease((t - 2700) / 600.0F);
            Vec3 beamTrack = satellite.lerp(target, phase);
            cameraPosition = beamTrack.add(32, 18, 32);
            aimAt(beamTrack);
            fov = lerp(58, 76, phase);
            roll = 0;
        } else {
            phase = ease((t - 3300) / 300.0F);
            cameraPosition = target.add(32 - 10 * phase, 28 + 8 * phase, 32 - 10 * phase);
            aimAt(target);
            fov = lerp(76, 70, phase);
            roll = (float)Math.sin(phase * Math.PI) * 0.4F;
        }
    }

    private static void aimAt(Vec3 point) {
        Vec3 direction = point.subtract(cameraPosition).normalize();
        yaw = (float)(Mth.atan2(direction.z, direction.x) * (180.0 / Math.PI)) - 90.0F;
        double vertical = Mth.clamp(direction.y, -1.0, 1.0);
        pitch = (float)(-Math.asin(vertical) * (180.0 / Math.PI));
    }

    private static void syncCameraAnchor() {
        if (cameraAnchor == null || !cameraAnchor.isAlive()) return;
        double eyeHeight = cameraAnchor.getEyeHeight();
        cameraAnchor.absMoveTo(cameraPosition.x, cameraPosition.y - eyeHeight,
                cameraPosition.z, yaw, pitch);
    }

    private static void spawnAtmosphere(Minecraft mc) {
        if (mc.level == null || tick % 2 != 0) return;
        Vec3 satellite = target.add(0, 300, 0);
        float charge = Mth.clamp((tick - 1500) / 900.0F, 0, 1);
        if (tick >= 1400 && tick < 2700) {
            for (int i = 0; i < 3; i++) {
                double a = mc.level.random.nextDouble() * Math.PI * 2;
                double r = 2.5 + mc.level.random.nextDouble() * (3.0 + charge * 5.0);
                mc.level.addParticle(ParticleTypes.END_ROD,
                        satellite.x + Math.cos(a) * r, satellite.y + mc.level.random.nextDouble() * 2,
                        satellite.z + Math.sin(a) * r, 0, 0.025 + charge * 0.04, 0);
            }
            if (tick >= 2200 && tick < 2550 && tick % 8 == 0) {
                mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, satellite.x, satellite.y, satellite.z, 0, 0.08, 0);
            }
        }
        if (tick >= 2550 && tick < 3300 && tick % 5 == 0) {
            double spread = 1.5 + mc.level.random.nextDouble() * 3.5;
            mc.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    target.x + (mc.level.random.nextDouble() - 0.5) * spread,
                    target.y + mc.level.random.nextDouble() * 12,
                    target.z + (mc.level.random.nextDouble() - 0.5) * spread, 0, 0.12, 0);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!playing) return;
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(roll);
    }

    private static float ease(float value) {
        float t = Mth.clamp(value, 0, 1);
        return t * t * (3.0F - 2.0F * t);
    }
    private static float lerp(float a, float b, float t) {
        return Mth.lerp(Mth.clamp(t, 0, 1), a, b);
    }
}
