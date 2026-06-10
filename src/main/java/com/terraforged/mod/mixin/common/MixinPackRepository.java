package com.terraforged.mod.mixin.common;

import com.terraforged.mod.hooks.DatapackHook;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={PackRepository.class})
public class MixinPackRepository {
    @ModifyVariable(at=@At(value="HEAD"), index=2, target={@Desc(value="<init>", args={Pack.PackConstructor.class, RepositorySource[].class})})
    private static RepositorySource[] modifySources(RepositorySource[] sources) {
        return DatapackHook.injectRepositorySource(sources);
    }
}
