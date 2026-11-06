package com.pokato.skywyrms.client.render;

import com.pokato.skywyrms.anim.AnimState;
import com.pokato.skywyrms.entity.DragonKind;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.Vec3;

public class DragonRenderState extends LivingEntityRenderState {
   public DragonKind kind = DragonKind.EMBER;
   public DragonKind blood = DragonKind.EMBER;
   public final AnimState anim = new AnimState();
   /** True while the local player rides this dragon in first person: neck and head are hidden so they can aim. */
   public boolean riderView;
   /** Sun beam in progress; start and end are relative to the dragon's render position. */
   public boolean beam;
   public Vec3 beamStart = Vec3.ZERO;
   public Vec3 beamEnd = Vec3.ZERO;
   public float beamTime;
}
