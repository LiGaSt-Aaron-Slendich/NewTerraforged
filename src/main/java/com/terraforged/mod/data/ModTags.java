/*
 * MIT License
 *
 * Copyright (c) 2021 TerraForged
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.terraforged.mod.data;

import com.terraforged.mod.registry.lazy.LazyTag;
import net.minecraft.world.level.biome.Biome;

public interface ModTags {
    LazyTag<Biome> OVERWORLD = LazyTag.biome("overworld");

    // Trees
    LazyTag<Biome> COPSES = LazyTag.biome("trees/copses");
    LazyTag<Biome> HARDY = LazyTag.biome("trees/hardy");
    LazyTag<Biome> HARDY_SLOPES = LazyTag.biome("trees/hardy_slopes");
    LazyTag<Biome> PATCHY = LazyTag.biome("trees/patchy");
    LazyTag<Biome> RAINFOREST = LazyTag.biome("trees/rainforest");
    LazyTag<Biome> SPARSE = LazyTag.biome("trees/sparse");
    LazyTag<Biome> SPARSE_RAINFOREST = LazyTag.biome("trees/sparse_rainforest");
    LazyTag<Biome> TEMPERATE = LazyTag.biome("trees/temperate");
}
