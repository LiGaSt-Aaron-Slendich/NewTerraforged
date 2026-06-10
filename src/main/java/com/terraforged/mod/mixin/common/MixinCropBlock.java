package com.terraforged.mod.mixin.common;

import com.terraforged.mod.worldgen.cave.CaveStatSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={CropBlock.class})
public abstract class MixinCropBlock {
    @Inject(method={"getGrowthSpeed"}, at={@At(value="RETURN")}, cancellable=true)
    private static void newterraforged$scaleGrowthSpeed(Block block, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!(level instanceof Level)) {
            return;
        }
        Level world = (Level)level;
        float factor = CaveStatSampler.cropGrowthFactor(world, pos);
        if (Math.abs(factor - 1.0f) > 0.001f) {
            cir.setReturnValue(Float.valueOf(((Float)cir.getReturnValue()).floatValue() * factor));
        }
    }
}
