package com.terraforged.mod.mixin.common;

import com.terraforged.mod.worldgen.structure.VillagePlacementValidator;
import net.minecraft.world.level.levelgen.feature.VillageFeature;
import net.minecraft.world.level.levelgen.structure.pieces.PieceGeneratorSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={VillageFeature.class})
public abstract class MixinVillageFeature {
    @Inject(method={"lambda$new$0"}, at={@At(value="RETURN")}, cancellable=true, require=0)
    private static void newterraforged$validateVillageSite(PieceGeneratorSupplier.Context context, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && !VillagePlacementValidator.isValid(context)) {
            cir.setReturnValue(false);
        }
    }
}
