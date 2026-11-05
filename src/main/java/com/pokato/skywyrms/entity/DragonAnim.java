package com.pokato.skywyrms.entity;

import com.pokato.skywyrms.anim.AnimState;
import com.pokato.skywyrms.anim.DragonAnimator;
import net.minecraft.util.Mth;

public final class DragonAnim {
   private static final float TWO_PI = (float) (Math.PI * 2);
   private static final float DEG = (float) (Math.PI / 180.0);
   private static final float HALF_PI = (float) (Math.PI / 2);
   public float fly;
   public float flyO;
   public float glide;
   public float glideO;
   public float pitch;
   public float pitchO;
   public float roll;
   public float rollO;
   public float yawVel;
   public float yawVelO;
   public float sideVel;
   public float vertVel;
   public float vertVelO;
   public float stoop;
   public float stoopO;
   public float speed;
   public float speedO;
   public float sit;
   public float sitO;
   public float breath;
   public float breathO;
   public float roar;
   public float roarO;
   public float bite;
   public float biteO;
   public float flapPhase;
   public float flapPhaseO;
   private float flapRate = 0.1F;
   private int airTicks;
   private boolean wasFlying;
   public float crouch;
   public float crouchO;
   public float wingLift;
   public float wingLiftO;
   public float leap;
   public float leapO;
   public float flare;
   public float flareO;
   private int windup;
   private int crouchHold;
   private int leapTicks = -1;
   private int landTicks = -1;
   private float landPow;
   private int wingHold;
   private float descent;
   private int idleAct;
   private int idleTicks;
   private int idleDuration;
   private long idleWindow = Long.MIN_VALUE;
   private long idlePlannedStart = -1L;
   private int idlePlannedAct;
   public float idleW;
   public float idleWO;
   public boolean leapNow;
   public boolean touchdownNow;
   public float touchdownPow;
   public int idleStarted;
   private final AnimState scratch = new AnimState();
   public int roarTicks;
   public int biteTicks;
   public int flashTicks;
   public static final int ROAR_DURATION = 32;
   public static final int BITE_DURATION = 10;
   private static final int IDLE_WINDOW = 160;

   public boolean tick(DragonEntity dragon) {
      this.flyO = this.fly;
      this.glideO = this.glide;
      this.pitchO = this.pitch;
      this.rollO = this.roll;
      this.yawVelO = this.yawVel;
      this.vertVelO = this.vertVel;
      this.stoopO = this.stoop;
      this.speedO = this.speed;
      this.sitO = this.sit;
      this.breathO = this.breath;
      this.roarO = this.roar;
      this.biteO = this.bite;
      this.flapPhaseO = this.flapPhase;
      this.crouchO = this.crouch;
      this.wingLiftO = this.wingLift;
      this.leapO = this.leap;
      this.flareO = this.flare;
      this.idleWO = this.idleW;
      this.leapNow = false;
      this.touchdownNow = false;
      this.idleStarted = 0;
      boolean flying = dragon.isFlying();
      boolean tookOff = flying && !this.wasFlying;
      boolean touchedDown = !flying && this.wasFlying;
      this.wasFlying = flying;
      this.airTicks = flying ? this.airTicks + 1 : 0;
      double dx = dragon.getX() - dragon.xo;
      double dy = dragon.getY() - dragon.yo;
      double dz = dragon.getZ() - dragon.zo;
      float horizontal = (float)Math.sqrt(dx * dx + dz * dz);
      float top = Math.max(0.1F, dragon.flightTopSpeed());
      if (tookOff) {
         this.windup = 0;
         this.crouchHold = 0;
         this.landTicks = -1;
         if (this.crouch > 0.3F) {
            this.leapNow = true;
            this.leapTicks = 0;
            this.flapPhase = this.flapPhaseO = 1.5207964F;
            this.flapRate = 0.45F;
         }
      }

      if (!flying && this.windup > 0) {
         this.windup--;
         this.crouch = 1.0F - this.windup / 6.0F;
         this.wingLift = approach(this.wingLift, 1.0F, 0.33333334F);
         if (this.windup == 0) {
            this.crouchHold = 5;
         }
      } else if (!flying && this.crouchHold > 0) {
         this.crouchHold--;
      } else {
         this.crouch = approach(this.crouch, 0.0F, flying ? 0.5F : 0.25F);
      }

      if (this.leapTicks >= 0) {
         this.leapTicks++;
         this.leap = this.leapTicks <= 2 ? this.leapTicks / 2.0F : Math.max(0.0F, 1.0F - (this.leapTicks - 2) / 10.0F);
         if (this.leapTicks > 12 || !flying) {
            this.leapTicks = -1;
         }
      } else {
         this.leap = approach(this.leap, 0.0F, 0.2F);
      }

      if (flying) {
         this.descent = Math.min((float)dy, this.descent * 0.6F + (float)dy * 0.4F);
      }

      float flareTarget = 0.0F;
      if (flying && this.airTicks > 10 && !dragon.isInWater()) {
         double height = dragon.heightAboveGround(6.0);
         float near = 1.0F - smoothstep(1.2F, 4.5F, (float)height);
         float coming = Mth.clamp((0.04F - (float)dy) / 0.12F, 0.0F, 1.0F);
         float slow = 1.0F - smoothstep(0.55F, 0.95F, horizontal / top);
         flareTarget = near * coming * slow;
      }

      if (touchedDown) {
         this.windup = 0;
         this.leapTicks = -1;
         this.landTicks = 0;
         this.landPow = Mth.clamp((-this.descent - 0.06F) / 0.45F, 0.0F, 1.0F);
         this.wingLift = 1.0F;
         this.wingHold = 3;
         if (!dragon.isInWater()) {
            this.touchdownNow = true;
            this.touchdownPow = this.landPow;
         }

         this.descent = 0.0F;
      }

      this.flare = flying ? approach(this.flare, flareTarget, 0.15F) : approach(this.flare, 0.0F, 0.2F);
      if (this.landTicks >= 0 && ++this.landTicks >= 18) {
         this.landTicks = -1;
      }

      if (flying && this.leapTicks >= 0) {
         if (this.fly > 0.9F) {
            this.wingLift = approach(this.wingLift, 0.0F, 0.2F);
         }
      } else if (flying || this.windup <= 0 && this.crouchHold <= 0) {
         if (this.wingHold > 0) {
            this.wingHold--;
         } else {
            this.wingLift = approach(this.wingLift, 0.0F, flying ? 0.2F : 0.125F);
         }
      }

      float flyTarget = flying ? 1.0F : 0.0F;
      if (flying && dy < 0.03 && this.airTicks > 14) {
         double height = dragon.heightAboveGround(3.0);
         if (height < 2.6) {
            flyTarget = 0.82F + 0.18F * (float)(height / 2.6);
         }
      }

      float flyStep = flying ? (this.leapTicks >= 0 ? 0.14285715F : 0.083333336F) : (this.landTicks >= 0 ? 0.2F : 0.1F);
      this.fly = approach(this.fly, flyTarget, flyStep);
      float speedTarget = Mth.clamp(horizontal / top, 0.0F, 1.0F);
      float vertTarget = Mth.clamp((float)dy / 0.5F, -1.0F, 1.0F);
      float yawTarget = Mth.clamp(Mth.wrapDegrees(dragon.yBodyRot - dragon.yBodyRotO) / 6.0F, -1.0F, 1.0F);
      float bodyYaw = dragon.yBodyRot * (float) (Math.PI / 180.0);
      float lateral = (float)(dx * -Mth.cos(bodyYaw) + dz * -Mth.sin(bodyYaw));
      float sideTarget = Mth.clamp(lateral / top, -1.0F, 1.0F);
      this.speed = this.speed + (speedTarget - this.speed) * 0.2F;
      this.vertVel = this.vertVel + (vertTarget - this.vertVel) * 0.2F;
      this.yawVel = this.yawVel + (yawTarget - this.yawVel) * 0.18F;
      this.sideVel = this.sideVel + (sideTarget - this.sideVel) * 0.2F;
      float stoopTarget = flying ? Mth.clamp(((float)(-dy) / top - 0.95F) / 0.75F, 0.0F, 1.0F) : 0.0F;
      this.stoop = this.stoop + (stoopTarget - this.stoop) * 0.12F;
      float pitchTarget = 0.0F;
      float rollTarget = 0.0F;
      if (flying) {
         float moving = Mth.clamp((horizontal + Math.abs((float)dy)) / 0.15F, 0.0F, 1.0F);
         pitchTarget = Mth.clamp((float)Math.atan2(dy, Math.max(horizontal, 0.3F)) * 0.85F, -0.65F - 0.5F * this.stoop, 0.5F) * moving;
         pitchTarget = Math.min(pitchTarget, 0.22F + 0.28F * smoothstep(8.0F, 30.0F, this.airTicks));
         float side = this.sideVel;
         if (side * this.yawVel < 0.0F) {
            side *= Math.max(0.0F, 1.0F - Math.abs(this.yawVel) * 4.0F);
         }

         rollTarget = Mth.clamp(this.yawVel * 0.95F + side * 1.25F, -0.62F, 0.62F);
         pitchTarget = Mth.lerp(this.flare, pitchTarget, 0.18F);
         rollTarget *= 1.0F - 0.7F * this.flare;
      }

      this.pitch = this.pitch + (pitchTarget - this.pitch) * 0.15F;
      this.roll = this.roll + (rollTarget - this.roll) * 0.12F;
      boolean beatBurst = (dragon.tickCount + dragon.getId() * 13) % 130 < 42;
      boolean glideNow = flying
         && this.airTicks > 20
         && this.speed > 0.55F
         && this.vertVel <= 0.08F
         && (!beatBurst || this.vertVel < -0.25F)
         && this.flare < 0.3F;
      this.glide = approach(this.glide, glideNow ? 1.0F : 0.0F, 0.1F);
      float rate;
      if (!flying) {
         rate = 0.1F;
      } else if (this.airTicks < 14) {
         rate = 0.45F;
      } else if (this.flare > 0.4F) {
         rate = 0.34F;
      } else if (this.vertVel > 0.3F) {
         rate = 0.4F;
      } else if (this.speed < 0.15F) {
         rate = 0.36F;
      } else {
         rate = 0.26F;
      }

      rate = Mth.lerp(this.glide, rate, 0.05F) * dragon.getKind().flapRate();
      this.flapRate = this.flapRate + (rate - this.flapRate) * 0.2F;
      this.flapPhase = this.flapPhase + this.flapRate;
      boolean beat = flying && this.glide < 0.6F && wrapsPast(this.flapPhaseO, this.flapPhase, (float) (Math.PI / 2));
      if (this.flapPhase > (float) (Math.PI * 2)) {
         this.flapPhase -= (float) (Math.PI * 2);
         this.flapPhaseO -= (float) (Math.PI * 2);
      }

      this.sit = approach(this.sit, dragon.isInSittingPose() && !flying ? 1.0F : 0.0F, 0.05F);
      this.tickIdle(dragon, flying);
      boolean breathing = dragon.isBreathing() || this.flashTicks > 0;
      this.breath = approach(this.breath, breathing ? 1.0F : 0.0F, breathing ? 0.2F : 0.125F);
      if (this.flashTicks > 0) {
         this.flashTicks--;
      }

      if (this.roarTicks > 0) {
         this.roarTicks--;
         this.roar = Mth.sin((float) Math.PI * (1.0F - this.roarTicks / 32.0F));
      } else {
         this.roar = 0.0F;
      }

      if (this.biteTicks > 0) {
         this.biteTicks--;
         this.bite = 1.0F - this.biteTicks / 10.0F;
      } else {
         this.bite = 0.0F;
         this.biteO = 0.0F;
      }

      return beat;
   }

   public void startWindup() {
      if (this.windup <= 0 && !(this.crouch > 0.5F) && !this.wasFlying) {
         this.windup = 6;
         this.idleW = 0.0F;
         this.idleAct = 0;
      }
   }

   public float seatDip(float partial) {
      AnimState a = this.scratch;
      a.fly = Mth.lerp(partial, this.flyO, this.fly);
      a.crouch = Mth.lerp(partial, this.crouchO, this.crouch);
      a.land = this.landValue(partial);
      a.landPow = this.landPow;
      return DragonAnimator.seatDip(a);
   }

   private float landValue(float partial) {
      return this.landTicks >= 0 ? Mth.clamp((this.landTicks + partial) / 18.0F, 0.001F, 1.0F) : 0.0F;
   }

   private void tickIdle(DragonEntity dragon, boolean flying) {
      boolean ridden = dragon.isVehicle();
      boolean sitting = dragon.isInSittingPose();
      boolean calm = !flying
         && this.fly < 0.01F
         && this.crouch < 0.01F
         && this.wingLift < 0.05F
         && this.landTicks < 0
         && this.windup == 0
         && dragon.walkAnimation.speed() < 0.08F
         && !dragon.isBreathing()
         && this.roarTicks == 0
         && this.biteTicks == 0
         && dragon.hurtTime == 0
         && dragon.isAlive();
      long time = dragon.level().getGameTime();
      long window = Math.floorDiv(time, 160);
      if (window != this.idleWindow) {
         this.idleWindow = window;
         long h = mix(dragon.getId() * -7046029254386353131L + window);
         this.idlePlannedStart = -1L;
         if ((h & 255L) < 115L) {
            this.idlePlannedStart = window * 160L + (int)((h >>> 8) % 100L);
            int r = (int)((h >>> 20) % 20L);
            this.idlePlannedAct = r < 7 ? ((h >>> 40 & 1L) == 0L ? 1 : 2) : (r < 11 ? 4 : (r < 16 ? 3 : 5));
         }
      }

      if (this.idleAct == 0 && this.idlePlannedStart >= 0L && time >= this.idlePlannedStart && time < this.idlePlannedStart + 6L) {
         int act = this.idlePlannedAct;
         boolean allowed = act == 4 || !ridden && (!sitting || act == 1 || act == 2);
         if (calm && allowed) {
            this.idleAct = act;
            this.idleTicks = 0;
            this.idleDuration = idleDuration(act);
            this.idleStarted = act;
            this.idlePlannedStart = -1L;
         }
      }

      if (this.idleAct != 0) {
         this.idleTicks++;
         boolean keep = calm && (!ridden || this.idleAct == 4);
         boolean done = this.idleTicks >= this.idleDuration;
         this.idleW = keep && !done ? approach(this.idleW, 1.0F, 0.25F) : approach(this.idleW, 0.0F, done ? 1.0F : 0.2F);
         if (this.idleW <= 0.0F && (done || !keep)) {
            this.idleAct = 0;
            this.idleWO = 0.0F;
         }
      }
   }

   public void playIdle(int act) {
      this.idleAct = act;
      this.idleTicks = 0;
      this.idleDuration = idleDuration(act);
      this.idleStarted = act;
      this.idleW = 0.0F;
   }

   private static int idleDuration(int act) {
      return switch (act) {
         case 3 -> 64;
         case 4 -> 46;
         case 5 -> 30;
         default -> 70;
      };
   }

   private static long mix(long z) {
      z = (z ^ z >>> 30) * -4658895280553007687L;
      z = (z ^ z >>> 27) * -7723592293110705685L;
      return z ^ z >>> 31;
   }

   private static float smoothstep(float e0, float e1, float x) {
      float t = Mth.clamp((x - e0) / (e1 - e0), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private static boolean wrapsPast(float from, float to, float mark) {
      return from < mark && to >= mark;
   }

   private static float approach(float value, float target, float step) {
      return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
   }

   public void fill(AnimState a, DragonEntity dragon, float partial) {
      a.kind = dragon.getKind().animKey();
      a.saddled = dragon.isSaddled();
      a.time = dragon.tickCount + partial + dragon.getId() * 37 % 211;
      a.walkPos = dragon.walkAnimation.position(partial) * 0.75F;
      a.walkSpeed = Math.min(1.0F, dragon.walkAnimation.speed(partial));
      a.fly = Mth.lerp(partial, this.flyO, this.fly);
      a.flapPhase = Mth.lerp(partial, this.flapPhaseO, this.flapPhase);
      a.glide = Mth.lerp(partial, this.glideO, this.glide);
      a.pitch = Mth.lerp(partial, this.pitchO, this.pitch);
      a.roll = Mth.lerp(partial, this.rollO, this.roll);
      a.yawVel = Mth.lerp(partial, this.yawVelO, this.yawVel);
      a.vertVel = Mth.lerp(partial, this.vertVelO, this.vertVel);
      a.stoop = Mth.lerp(partial, this.stoopO, this.stoop);
      a.speed = Mth.lerp(partial, this.speedO, this.speed);
      a.sit = Mth.lerp(partial, this.sitO, this.sit);
      a.breath = Mth.lerp(partial, this.breathO, this.breath);
      a.roar = Mth.lerp(partial, this.roarO, this.roar);
      a.bite = Mth.lerp(partial, this.biteO, this.bite);
      a.hurt = dragon.hurtTime > 0 && dragon.hurtDuration > 0 ? Mth.clamp((dragon.hurtTime - partial) / dragon.hurtDuration, 0.0F, 1.0F) : 0.0F;
      float bodyYaw = Mth.rotLerp(partial, dragon.yBodyRotO, dragon.yBodyRot);
      float headYaw = Mth.rotLerp(partial, dragon.yHeadRotO, dragon.yHeadRot);
      a.headYaw = Mth.wrapDegrees(headYaw - bodyYaw) * (float) (Math.PI / 180.0);
      a.headPitch = Mth.lerp(partial, dragon.xRotO, dragon.getXRot()) * (float) (Math.PI / 180.0);
      a.crouch = Mth.lerp(partial, this.crouchO, this.crouch);
      a.wingLift = Mth.lerp(partial, this.wingLiftO, this.wingLift);
      a.leap = Mth.lerp(partial, this.leapO, this.leap);
      a.flare = Mth.lerp(partial, this.flareO, this.flare);
      a.land = this.landValue(partial);
      a.landPow = this.landPow;
      a.idle = this.idleAct;
      a.idleT = this.idleAct == 0 ? 0.0F : Mth.clamp((this.idleTicks + partial) / Math.max(1, this.idleDuration), 0.0F, 1.0F);
      a.idleW = Mth.lerp(partial, this.idleWO, this.idleW);
   }
}
