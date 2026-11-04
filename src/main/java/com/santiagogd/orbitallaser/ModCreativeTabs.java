package com.santiagogd.orbitallaser;

import com.santiagogd.orbitallaser.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OrbitalLaserMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ORBITAL_LASER_TAB =
            CREATIVE_MODE_TABS.register("orbital_laser_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.orbitallaser"))
                            .icon(() -> new ItemStack(ModItems.ORBITAL_MARKER.get()))
                            .displayItems((parameters, output) -> {
                                output.accept(ModItems.ORBITAL_MARKER.get());
                            })
                            .build()
            );
}