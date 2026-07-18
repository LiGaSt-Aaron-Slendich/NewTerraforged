package com.terraforged.mod.platform;

public interface ClientAPI {
   ApiHolder<ClientAPI> HOLDER = new ApiHolder<>(new ClientAPI() {});

   default boolean hasPreset() {
      return false;
   }

   static ClientAPI get() {
      return HOLDER.get();
   }
}
