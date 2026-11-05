package com.pokato.skywyrms.client;

import com.pokato.skywyrms.entity.DragonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public final class DiveWindSound extends AbstractTickableSoundInstance {
   private final LocalPlayer player;
   private final DragonEntity dragon;
   private int quietTicks;
   private static DiveWindSound current;

   public DiveWindSound(LocalPlayer player, DragonEntity dragon) {
      super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
      this.player = player;
      this.dragon = dragon;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.0F;
      this.x = player.getX();
      this.y = player.getY();
      this.z = player.getZ();
   }

   @Override
   public void tick() {
      if (!this.player.isRemoved() && !this.dragon.isRemoved() && this.player.getVehicle() == this.dragon) {
         this.x = this.player.getX();
         this.y = this.player.getY();
         this.z = this.player.getZ();
         float stoop = this.dragon.anim.stoop;
         float target = Mth.clamp(stoop * 1.1F, 0.0F, 1.0F);
         this.volume = this.volume + (target - this.volume) * 0.15F;
         this.pitch = 0.85F + 0.35F * stoop;
         this.quietTicks = stoop < 0.03F ? this.quietTicks + 1 : 0;
         if (this.quietTicks > 30) {
            this.stop();
         }
      } else {
         this.stop();
      }
   }

   static void update(Minecraft minecraft, LocalPlayer player, DragonEntity dragon) {
      if (dragon != null && !(dragon.anim.stoop < 0.15F)) {
         if (current == null || current.isStopped()) {
            current = new DiveWindSound(player, dragon);
            minecraft.getSoundManager().play(current);
         }
      }
   }
}
