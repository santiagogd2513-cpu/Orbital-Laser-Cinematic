package com.santiagogd.orbitallaser.cinematic;

import com.santiagogd.orbitallaser.OrbitalLaserMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * High-end 3-minute Orbital Laser Cinematic Manager.
 * Camera leaves the planet, shows satellite charge-up, fires massive beam.
 * Designed for high graphical load machines.
 */
@OnlyIn(Dist.CLIENT)
public class CinematicManager {

    public static final int TOTAL_DURATION_TICKS = 20 * 60 * 3; // 3 minutes exactly

    private static boolean isPlaying = false;
    private static int currentTick = 0;
    private static Vec3 targetPosition = Vec3.ZERO;
    private static Vec3 satellitePosition = Vec3.ZERO;

    // Camera state
    private static Vec3 cameraPos = Vec3.ZERO;
    private static float cameraYaw = 0;
    private static float cameraPitch = 0;
    private static float cameraFov = 70;

    public static void startCinematic(Vec3 impactTarget) {
        if (isPlaying) return;

        isPlaying = true;
        currentTick = 0;
        targetPosition = impactTarget;

        // Position satellite high above the target (in "space")
        satellitePosition = new Vec3(
                impactTarget.x,
                impactTarget.y + 380, // Very high - outside normal world height feel
                impactTarget.z
        );

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            cameraPos = mc.player.getEyePosition();
            cameraYaw = mc.player.getYRot();
            cameraPitch = mc.player.getXRot();
        }

        OrbitalLaserMod.LOGGER.info("Starting 3-minute Orbital Laser Cinematic. Target: {}", impactTarget);
    }

    public static void stopCinematic() {
        isPlaying = false;
        currentTick = 0;
        Minecraft.getInstance().options.hideGui = false;
    }

    public static boolean isPlaying() {
        return isPlaying;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !isPlaying) return;

        currentTick++;

        if (currentTick >= TOTAL_DURATION_TICKS) {
            stopCinematic();
            return;
        }

        updateCameraAndEffects(currentTick);
    }

    private static void updateCameraAndEffects(int tick) {
        float progress = tick / (float) TOTAL_DURATION_TICKS;

        // === PHASES OF THE 3-MINUTE CINEMATIC ===

        // Phase 1: 0:00 - 0:25  → Slow rise from player, looking at sky
        if (tick < 500) {
            float t = tick / 500f;
            cameraPos = cameraPos.add(0, easeInOut(t) * 40, 0);
            cameraPitch = lerp(cameraPitch, -55f, t * 0.05f);
            cameraFov = lerp(cameraFov, 85f, t * 0.03f);
        }

        // Phase 2: 0:25 - 1:10  → Camera leaves the planet, goes into space
        else if (tick < 1400) {
            float t = (tick - 500) / 900f;
            double height = easeInOut(t) * 320;
            cameraPos = new Vec3(
                    targetPosition.x + Math.sin(t * Math.PI * 0.6) * 60,
                    targetPosition.y + 60 + height,
                    targetPosition.z + Math.cos(t * Math.PI * 0.6) * 60
            );
            cameraPitch = lerp(cameraPitch, -25f, 0.02f);
            cameraYaw += 0.08f; // Slow orbit
            cameraFov = lerp(cameraFov, 65f, 0.02f);
        }

        // Phase 3: 1:10 - 1:50  → Approach satellite, show charge-up
        else if (tick < 2200) {
            float t = (tick - 1400) / 800f;
            cameraPos = lerpVec(cameraPos, satellitePosition.add(18, 8, 18), 0.025f);
            lookAt(satellitePosition);
            cameraFov = lerp(cameraFov, 50f, 0.015f);

            // Charge particles / intensity would ramp here
        }

        // Phase 4: 1:50 - 2:15  → Final charge + fire
        else if (tick < 2700) {
            float t = (tick - 2200) / 500f;
            cameraPos = satellitePosition.add(
                    Math.cos(tick * 0.04) * 12,
                    6 + Math.sin(tick * 0.03) * 3,
                    Math.sin(tick * 0.04) * 12
            );
            lookAt(satellitePosition);

            if (tick == 2550) {
                // FIRE event
                OrbitalLaserMod.LOGGER.info("ORBITAL LASER FIRED");
            }
        }

        // Phase 5: 2:15 - 2:45  → Follow the beam down to the planet
        else if (tick < 3300) {
            float t = (tick - 2700) / 600f;
            Vec3 beamPoint = lerpVec(satellitePosition, targetPosition, easeInOut(t));
            cameraPos = beamPoint.add(
                    Math.cos(tick * 0.02) * 25,
                    15,
                    Math.sin(tick * 0.02) * 25
            );
            lookAt(beamPoint);
            cameraFov = lerp(cameraFov, 75f, 0.02f);
        }

        // Phase 6: 2:45 - 3:00  → Impact + aftermath, slow return feel
        else {
            float t = (tick - 3300) / 300f;
            cameraPos = lerpVec(cameraPos, targetPosition.add(30, 25, 30), 0.03f);
            lookAt(targetPosition);
            cameraFov = lerp(cameraFov, 70f, 0.02f);
        }

        // Hide GUI during cinematic
        Minecraft.getInstance().options.hideGui = true;
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!isPlaying) return;

        event.setYaw(cameraYaw);
        event.setPitch(cameraPitch);
        // Roll can be added for more dramatic effect if wanted
    }

    // ===== Utility =====

    private static void lookAt(Vec3 target) {
        Vec3 dir = target.subtract(cameraPos).normalize();
        cameraYaw = (float) (Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90);
        cameraPitch = (float) -Math.toDegrees(Math.asin(dir.y));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * Math.min(1f, t);
    }

    private static Vec3 lerpVec(Vec3 a, Vec3 b, float t) {
        return a.add(b.subtract(a).scale(Math.min(1.0, t)));
    }

    private static float easeInOut(float t) {
        return t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
    }

    public static Vec3 getCameraPosition() {
        return cameraPos;
    }

    public static int getCurrentTick() {
        return currentTick;
    }
}