package com.terraforged.mod.mixin.client;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.terraforged.mod.TerraForged;
import com.terraforged.mod.client.screen.ScreenUtil;
import com.terraforged.mod.worldgen.datapack.DataPackExporter;
import java.io.File;
import java.nio.file.Path;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldGenSettingsComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CreateWorldScreen.class})
public abstract class MixinCreateWorldScreen {
   @Shadow
   protected DataPackConfig dataPacks;

   @Shadow
   public boolean hardCore;

   @Shadow
   public WorldGenSettingsComponent worldGenSettingsComponent;

   @Shadow
   protected abstract Path getTempDataPackDir();

   @Shadow
   protected abstract Pair<File, PackRepository> getDataPackSelectionSettings();

   @Shadow
   protected abstract void tryApplyNewDataPacks(PackRepository var1);

   @Inject(
      method = {"onCreate()V"},
      at = {@At("HEAD")}
   )
   private void onCreate(CallbackInfo ci) {
      if (ScreenUtil.isPresetEnabled((CreateWorldScreen)(Object)this)) {
         WorldGenSettings settings = this.worldGenSettingsComponent.makeSettings(this.hardCore);
         DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, this.worldGenSettingsComponent.registryHolder());
         DataResult<JsonElement> result = WorldGenSettings.CODEC.encodeStart(ops, settings);
         TerraForged.LOG.error(
            "PRE-PACK WorldGenSettings encode: success={} value={} error={}",
            result.result().isPresent(),
            result.result().orElse(null),
            result.error()
         );

         this.dataPacks = DataPackExporter.setup(this.getTempDataPackDir(), this.dataPacks);
         PackRepository packrepository = (PackRepository)this.getDataPackSelectionSettings().getSecond();
         packrepository.setSelected(this.dataPacks.getEnabled());
         this.tryApplyNewDataPacks(packrepository);
         TerraForged.LOG.info("Applied datapacks: {}", packrepository.getSelectedIds());
      }
   }
}
