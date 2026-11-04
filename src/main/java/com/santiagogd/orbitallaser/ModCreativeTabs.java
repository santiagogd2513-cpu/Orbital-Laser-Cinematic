package com.santiagogd.orbitallaser;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OrbitalLaserMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ORBITAL_LASER_TAB =
            CREATIVE_MODE_TABS.register("orbital_laser_tab", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.orbitallaser"))
                            .icon(() -> new ItemStack(Items.NETHER_STAR)) // Temporary icon (we'll change it later)
                            .displayItems((parameters, output) -> {
                                // Items of the mod will be added here later
                                // Example: output.accept(ModItems.SOME_ITEM.get());
                            })
                            .build()
            );
}