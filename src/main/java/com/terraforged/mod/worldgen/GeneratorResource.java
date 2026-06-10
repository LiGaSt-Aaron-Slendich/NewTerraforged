package com.terraforged.mod.worldgen;

import com.terraforged.mod.worldgen.util.ChunkUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;

public class GeneratorResource {
    public final Holder<Biome>[] biomeBuffer2D = new Holder[16];
    public final FriendlyByteBuf fullSection = ChunkUtil.getFullSection();
}
