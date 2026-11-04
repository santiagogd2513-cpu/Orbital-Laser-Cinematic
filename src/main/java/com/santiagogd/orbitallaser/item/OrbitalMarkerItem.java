package com.santiagogd.orbitallaser.item;

import com.santiagogd.orbitallaser.cinematic.ClientCinematicHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Item used to mark the impact point of the Orbital Laser.
 * Right-click (or use) on a block or in the air to set the target and start the 3-minute cinematic.
 */
public class OrbitalMarkerItem extends Item {

    public OrbitalMarkerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null) return InteractionResult.PASS;

        // Target is the block that was clicked
        Vec3 target = context.getClickLocation();

        if (level.isClientSide) {
            startCinematic(player, target);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Raycast to find where the player is looking
        HitResult hit = player.pick(80.0, 0.0f, false); // up to 80 blocks

        Vec3 target;
        if (hit.getType() == HitResult.Type.BLOCK) {
            target = ((BlockHitResult) hit).getLocation();
        } else {
            // Looking at the sky / far away → use a point far in the look direction
            target = player.getEyePosition().add(player.getLookAngle().scale(60.0));
        }

        if (level.isClientSide) {
            startCinematic(player, target);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void startCinematic(Player player, Vec3 target) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientCinematicHandler.startFromItem(player, target));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Click para marcar el punto de impacto"));
        tooltip.add(Component.literal("§7y activar la cinemática del láser orbital."));
        tooltip.add(Component.literal("§8Duración: 3 minutos"));
    }
}