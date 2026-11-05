package com.pokato.skywyrms.platform;

import com.pokato.skywyrms.net.AbilityPayload;
import java.nio.file.Path;
import net.minecraft.world.item.CreativeModeTab.Builder;

public interface Platform {
   String loaderName();

   void sendToServer(AbilityPayload payload);

   Builder creativeTabBuilder();

   Path configDir();

   static Platform get() {
      Platform p = Platform.Holder.instance;
      if (p == null) {
         throw new IllegalStateException("Skywyrms Platform not initialised yet");
      } else {
         return p;
      }
   }

   static void set(Platform platform) {
      Platform.Holder.instance = platform;
   }

   final class Holder {
      static volatile Platform instance;

      private Holder() {
      }
   }
}
