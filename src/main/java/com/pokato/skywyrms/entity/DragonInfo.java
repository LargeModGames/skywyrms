package com.pokato.skywyrms.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class DragonInfo {
   private DragonInfo() {
   }

   public static MutableComponent element(DragonKind kind) {
      return Component.translatable("element.skywyrms." + kind.element()).withColor(kind.color());
   }

   private static String percent(float factor) {
      return Integer.toString(Math.round(factor * 100.0F));
   }

   public static List<Component> geneLines(DragonGenes genes) {
      List<Component> lines = new ArrayList<>();
      MutableComponent genesLine = Component.translatable("info.skywyrms.genes", element(genes.body()), element(genes.blood()));
      genesLine.append(Component.literal("  "));
      genesLine.append(Component.translatable(genes.purebred() ? "info.skywyrms.purebred" : "info.skywyrms.hybrid").withStyle(ChatFormatting.GRAY));
      lines.add(genesLine);
      lines.add(
         Component.translatable("info.skywyrms.stats", percent(genes.healthFactor()), percent(genes.speedFactor()), percent(genes.powerFactor()))
            .withStyle(ChatFormatting.GRAY)
      );
      if (genes.vigor()) {
         lines.add(Component.translatable("info.skywyrms.vigor").withStyle(ChatFormatting.GREEN));
      }

      Component ability = Component.translatable("ability.skywyrms." + genes.body().ability().id()).withColor(genes.body().color());
      if (genes.purebred()) {
         lines.add(Component.translatable("info.skywyrms.ability", ability));
         lines.add(Component.translatable("info.skywyrms.pure_bonus").withStyle(ChatFormatting.GRAY));
      } else {
         lines.add(
            Component.translatable(
               "info.skywyrms.ability_infused",
               ability,
               Component.translatable("infusion.skywyrms." + genes.blood().element()).withColor(genes.blood().color())
            )
         );
      }

      MutableComponent traits = Component.translatable("trait.skywyrms." + genes.body().element());
      if (!genes.purebred()) {
         traits = traits.append(Component.literal(", ")).append(Component.translatable("trait.skywyrms." + genes.blood().element()));
      }

      lines.add(Component.translatable("info.skywyrms.traits", traits).withStyle(ChatFormatting.GRAY));
      lines.add(Component.translatable("info.skywyrms.generation", genes.generation()).withStyle(ChatFormatting.DARK_GRAY));
      return lines;
   }

   public static void send(ServerPlayer player, DragonEntity dragon) {
      DragonGenes genes = dragon.genes();
      player.sendSystemMessage(Component.literal("").append(dragon.getDisplayName().copy().withColor(dragon.getKind().color()).withStyle(ChatFormatting.BOLD)));
      if (dragon.isBaby()) {
         player.sendSystemMessage(Component.translatable("info.skywyrms.growth", Math.round(dragon.growth() * 100.0F)).withStyle(ChatFormatting.YELLOW));
      }

      for (Component line : geneLines(genes)) {
         player.sendSystemMessage(line);
      }
   }

   public static void sendEgg(ServerPlayer player, DragonEggEntity egg) {
      DragonGenes genes = egg.genes();
      player.sendSystemMessage(Component.literal("").append(egg.getDisplayName().copy().withColor(genes.body().color()).withStyle(ChatFormatting.BOLD)));
      player.sendSystemMessage(
         Component.translatable("info.skywyrms.egg_progress", Math.round(egg.progressFraction() * 100.0F)).withStyle(ChatFormatting.YELLOW)
      );
      if (egg.isWarm()) {
         player.sendSystemMessage(Component.translatable("info.skywyrms.egg_warm").withStyle(ChatFormatting.GOLD));
      } else {
         player.sendSystemMessage(Component.translatable("egg.skywyrms.hint." + genes.body().element()).withStyle(ChatFormatting.GRAY));
         if (!genes.purebred()) {
            player.sendSystemMessage(Component.translatable("egg.skywyrms.hint." + genes.blood().element()).withStyle(ChatFormatting.GRAY));
         }
      }

      for (Component line : geneLines(genes)) {
         player.sendSystemMessage(line);
      }
   }
}
