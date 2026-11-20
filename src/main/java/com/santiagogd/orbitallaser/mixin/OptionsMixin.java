package com.santiagogd.orbitallaser.mixin;

import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 32), require = 0)
    private int orbitallaser$raiseRenderDistanceLimit(int original) {
        return original == 32 ? 128 : original;
    }
}
