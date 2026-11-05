package com.pokato.skywyrms.item;

import com.pokato.skywyrms.compat.Nbt;
import com.pokato.skywyrms.config.ModConfig;
import com.pokato.skywyrms.entity.DragonEggEntity;
import com.pokato.skywyrms.entity.DragonGenes;
import com.pokato.skywyrms.entity.DragonInfo;
import com.pokato.skywyrms.entity.DragonKind;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public class DragonEggItem extends Item {
   public static final String TAG = "Skywyrms";
   private final DragonKind kind;

   public DragonEggItem(DragonKind kind, Properties properties) {
      super(properties);
      this.kind = kind;
   }

   public DragonKind kind() {
      return this.kind;
   }

   private static CompoundTag data(ItemStack stack) {
      CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
      if (custom == null) {
         return null;
      }

      CompoundTag root = custom.copyTag();
      return Nbt.getCompound(root, "Skywyrms");
   }

   public static void write(ItemStack stack, DragonGenes genes, UUID owner, int progress) {
      CompoundTag tag = new CompoundTag();
      tag.put("Genes", genes.toTag());
      if (owner != null) {
         tag.putString("Owner", owner.toString());
      }

      tag.putInt("Incubation", progress);
      CompoundTag root = new CompoundTag();
      root.put("Skywyrms", tag);
      stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
   }

   public DragonGenes genes(ItemStack stack) {
      CompoundTag tag = data(stack);
      if (tag == null) {
         return null;
      }

      CompoundTag genes = Nbt.getCompound(tag, "Genes");
      return genes == null ? null : DragonGenes.fromTag(genes, this.kind).withBody(this.kind);
   }

   private static UUID owner(ItemStack stack) {
      CompoundTag tag = data(stack);
      if (tag == null) {
         return null;
      }

      try {
         String text = Nbt.getString(tag, "Owner", "");
         return text.isEmpty() ? null : UUID.fromString(text);
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private static int progress(ItemStack stack) {
      CompoundTag tag = data(stack);
      return tag == null ? 0 : Math.max(0, Nbt.getInt(tag, "Incubation", 0));
   }

   @Override
   public Component getName(ItemStack stack) {
      DragonGenes genes = this.genes(stack);
      if (genes != null && !genes.purebred()) {
         Component dragon = Component.translatable(
            "entity.skywyrms.hybrid",
            Component.translatable("element.skywyrms." + genes.blood().element() + ".adjective"),
            Component.translatable("entity.skywyrms." + this.kind.id())
         );
         return Component.translatable("entity.skywyrms.dragon_egg.named", dragon);
      } else {
         return super.getName(stack);
      }
   }

   @Override
   public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
      this.tooltip(stack, tooltip);
   }

   private void tooltip(ItemStack stack, Consumer<Component> tooltip) {
      DragonGenes genes = this.genes(stack);
      if (genes == null) {
         tooltip.accept(Component.translatable("item.skywyrms.dragon_egg.unknown", DragonInfo.element(this.kind)).withStyle(ChatFormatting.GRAY));
      } else {
         int percent = (int)Math.min(100L, progress(stack) * 100L / ModConfig.get().eggHatchTicks());
         tooltip.accept(Component.translatable("info.skywyrms.egg_progress", percent).withStyle(ChatFormatting.YELLOW));

         for (Component line : DragonInfo.geneLines(genes)) {
            tooltip.accept(line);
         }
      }

      tooltip.accept(Component.translatable("egg.skywyrms.hint." + this.kind.element()).withStyle(ChatFormatting.DARK_AQUA));
      if (genes != null && !genes.purebred()) {
         tooltip.accept(Component.translatable("egg.skywyrms.hint." + genes.blood().element()).withStyle(ChatFormatting.DARK_AQUA));
      }

      tooltip.accept(Component.translatable("item.skywyrms.dragon_egg.place").withStyle(ChatFormatting.DARK_GRAY));
   }

   @Override
   public InteractionResult useOn(UseOnContext context) {
      if (context.getLevel() instanceof ServerLevel level) {
         ItemStack var11 = context.getItemInHand();
         Player player = context.getPlayer();
         BlockPos clicked = context.getClickedPos();
         BlockPos pos = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.relative(context.getClickedFace());
         DragonGenes genes = this.genes(var11);
         if (genes == null) {
            genes = DragonGenes.wild(this.kind, this.kind, level.getRandom());
         }

         UUID owner = owner(var11);
         if (owner == null && player != null) {
            owner = player.getUUID();
         }

         DragonEggEntity egg = DragonEggEntity.create(level, genes, owner, progress(var11));
         if (egg == null) {
            return InteractionResult.FAIL;
         }

         Vec3 at = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
         egg.snapTo(at.x, at.y, at.z, player != null ? player.getYRot() + 180.0F : 0.0F, 0.0F);
         if (!level.noCollision(egg, egg.getBoundingBox())) {
            return InteractionResult.FAIL;
         }

         level.addFreshEntity(egg);
         level.playSound(null, at.x, at.y, at.z, SoundEvents.TURTLE_LAY_EGG, SoundSource.NEUTRAL, 1.0F, 0.8F);
         if (player == null || !player.isCreative()) {
            var11.shrink(1);
         }

         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.SUCCESS;
      }
   }
}
