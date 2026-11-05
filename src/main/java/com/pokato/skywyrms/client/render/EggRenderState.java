package com.pokato.skywyrms.client.render;

import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class EggRenderState extends LivingEntityRenderState {
   public DragonKind body = DragonKind.EMBER;
   public DragonKind blood = DragonKind.EMBER;
   public int stage;
   public float wobble;
   public float wobblePhase;
}
