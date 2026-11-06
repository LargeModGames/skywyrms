package com.pokato.skywyrms.client.render;

import com.pokato.skywyrms.anim.AnimState;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class DragonRenderState extends LivingEntityRenderState {
   public DragonKind kind = DragonKind.EMBER;
   public DragonKind blood = DragonKind.EMBER;
   public final AnimState anim = new AnimState();
   /** True while the local player rides this dragon in first person: neck and head are hidden so they can aim. */
   public boolean riderView;
}
