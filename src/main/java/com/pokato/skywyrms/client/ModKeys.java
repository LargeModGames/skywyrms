package com.pokato.skywyrms.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.pokato.skywyrms.Skywyrms;
import java.util.function.Consumer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;

public final class ModKeys {
   public static final Category CATEGORY = Category.register(Skywyrms.id("main"));
   public static final KeyMapping ABILITY = key("key.skywyrms.ability", "key.keyboard.r");
   public static final KeyMapping DESCEND = key("key.skywyrms.descend", "key.keyboard.x");

   private ModKeys() {
   }

   private static KeyMapping key(String name, String defaultKey) {
      Key key = InputConstants.getKey(defaultKey);
      return new KeyMapping(name, key.getType(), key.getValue(), CATEGORY);
   }

   public static void register(Consumer<KeyMapping> registrar) {
      registrar.accept(ABILITY);
      registrar.accept(DESCEND);
   }
}
