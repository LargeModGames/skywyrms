package com.pokato.skywyrms.client;

import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.FontDescription.Resource;

public final class HudBar {
   private static final FontDescription FONT = new Resource(Skywyrms.id("hud"));
   private static final int WHITE = 16777215;
   private static final int SEGMENTS = 24;
   private static final int RING_ADVANCE = 20;
   private static final char ICON = '\ue010';
   private static final char RING = '\ue060';
   private static final char SEGMENT = '\ue061';

   private HudBar() {
   }

   public static Component build(DragonEntity dragon) {
      DragonKind kind = dragon.getKind();
      DragonKind blood = dragon.blood();
      int energy = Math.max(0, Math.min(100, dragon.getEnergy()));
      int lit = Math.round(energy * 24 / 100.0F);
      MutableComponent out = Component.empty();
      out.append(glyphs("\ue060" + move(-20), 16777215));

      for (int k = 0; k < lit; k++) {
         int color = blood == kind ? kind.color() : mix(kind.color(), blood.color(), k / 23.0F);
         out.append(glyphs((char)(57441 + k) + move(-20), color));
      }

      out.append(glyphs(move(4) + (char)(57360 + kind.ordinal() * DragonKind.values().length + blood.ordinal()) + move(4), 16777215));
      out.append(Component.literal(" "));
      out.append(Component.translatable("ability.skywyrms." + kind.ability().id()).withColor(kind.color()));
      if (blood != kind) {
         out.append(Component.literal(" + ").withStyle(ChatFormatting.GRAY));
         out.append(Component.translatable("element.skywyrms." + blood.element()).withColor(blood.color()));
      }

      out.append(Component.literal(" " + energy + "%").withStyle(ChatFormatting.GRAY));
      return out;
   }

   private static int mix(int a, int b, float t) {
      int r = Math.round((a >> 16 & 0xFF) * (1.0F - t) + (b >> 16 & 0xFF) * t);
      int g = Math.round((a >> 8 & 0xFF) * (1.0F - t) + (b >> 8 & 0xFF) * t);
      int bl = Math.round((a & 0xFF) * (1.0F - t) + (b & 0xFF) * t);
      return r << 16 | g << 8 | bl;
   }

   private static MutableComponent glyphs(String s, int rgb) {
      return Component.literal(s).withColor(rgb).withStyle(st -> st.withFont(FONT).withShadowColor(0));
   }

   static String move(int px) {
      StringBuilder sb = new StringBuilder();
      int base = px < 0 ? 57344 : 57352;
      int left = Math.abs(px);
      int[] steps = px < 0 ? new int[]{128, 64, 32, 16, 8, 4, 2, 1} : new int[]{32, 16, 8, 4, 2, 1};

      for (int step : steps) {
         int index = Integer.numberOfTrailingZeros(step);

         while (left >= step) {
            sb.append((char)(base + index));
            left -= step;
         }
      }

      return sb.toString();
   }
}
