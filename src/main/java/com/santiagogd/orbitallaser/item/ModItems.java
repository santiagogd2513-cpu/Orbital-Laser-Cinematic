package com.santiagogd.orbitallaser.item;

import com.santiagogd.orbitallaser.OrbitalLaserMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, OrbitalLaserMod.MOD_ID);

    public static final RegistryObject<Item> ORBITAL_MARKER =
            ITEMS.register("orbital_marker", () ->
                    new OrbitalMarkerItem(new Item.Properties().stacksTo(1))
            );
}