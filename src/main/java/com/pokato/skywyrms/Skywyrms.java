package com.pokato.skywyrms;

import com.pokato.skywyrms.platform.Platform;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Skywyrms {
   public static final String MOD_ID = "skywyrms";
   public static final Logger LOGGER = LoggerFactory.getLogger("Skywyrms");

   private Skywyrms() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("skywyrms", path);
   }

   public static void init() {
      LOGGER.info("Skywyrms init on {}", Platform.get().loaderName());
   }
}
