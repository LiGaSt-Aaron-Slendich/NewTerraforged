package com.terraforged.mod.mixin.common;

import com.terraforged.mod.compat.TectonicCompat;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collection;

/** Drop Tectonic generative packs whenever selection is applied. */
@Mixin(PackRepository.class)
public class MixinPackRepository {
    @ModifyVariable(
            method = "setSelected(Ljava/util/Collection;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Collection<String> newtf$filterTectonicPacks(Collection<String> selected) {
        return TectonicCompat.filterSelectedPackIds(selected);
    }
}
