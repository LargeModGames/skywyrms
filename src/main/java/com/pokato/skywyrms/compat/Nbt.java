package com.pokato.skywyrms.compat;

import net.minecraft.nbt.CompoundTag;

public final class Nbt {
   private Nbt() {
   }

   public static String getString(CompoundTag tag, String key, String fallback) {
      return tag.getStringOr(key, fallback);
   }

   public static float getFloat(CompoundTag tag, String key, float fallback) {
      return tag.getFloatOr(key, fallback);
   }

   public static int getInt(CompoundTag tag, String key, int fallback) {
      return tag.getIntOr(key, fallback);
   }

   public static boolean getBoolean(CompoundTag tag, String key, boolean fallback) {
      return tag.getBooleanOr(key, fallback);
   }

   public static CompoundTag getCompound(CompoundTag tag, String key) {
      return tag.getCompound(key).orElse(null);
   }
}
