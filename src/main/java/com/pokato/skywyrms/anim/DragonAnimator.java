package com.pokato.skywyrms.anim;

public final class DragonAnimator {
   private static final String[] NECK = new String[]{"neck1", "neck2", "neck3", "neck4"};
   private static final String[] TAIL = new String[]{"tail1", "tail2", "tail3", "tail4", "tail5", "tail6"};
   private static final String[] SHINS = new String[]{"leg_fl_shin", "leg_fr_shin", "leg_bl_shin", "leg_br_shin"};
   private static final String[] CROWN_RAYS = numbered("deco_crown_ray_", 11);
   private static final String[] DORSAL_SPINES = numbered("deco_dorsal_", 25);
   private static final float PI = (float) Math.PI;
   private static final float D2R = (float) (Math.PI / 180.0);
   public static final float CROUCH_DIP = 4.0F;
   public static final float LAND_DIP = 4.5F;
   private static final float[] LOOK_W = new float[]{0.1F, 0.16F, 0.2F, 0.2F};
   private static final float[] LOOK_P = new float[]{0.06F, 0.1F, 0.18F, 0.24F};

   private DragonAnimator() {
   }

   private static DragonAnimator.Flavor flavor(String kind) {
      DragonAnimator.Flavor f = new DragonAnimator.Flavor();
      if (kind == null) {
         return f;
      }

      switch (kind) {
         case "fire":
            f.flapAmp = 0.62F;
            f.walkBob = 1.15F;
            f.roarLift = 1.2F;
            break;
         case "ice":
            f.flapAmp = 0.56F;
            f.tailWave = 1.35F;
            f.neckSway = 1.3F;
            f.walkBob = 0.8F;
            f.idleRate = 0.85F;
            break;
         case "storm":
            f.flapAmp = 0.66F;
            f.tipLag = 1.3F;
            f.neckSway = 1.2F;
            f.walkBob = 0.85F;
            f.foldSz = 0.32F;
            break;
         case "thorn":
            f.flapAmp = 0.58F;
            f.walkBob = 1.7F;
            f.walkSwing = 0.34F;
            f.tailWave = 0.7F;
            f.neckSway = 0.6F;
            f.idleRate = 0.75F;
            break;
         case "void":
            f.flapAmp = 0.6F;
            f.tipLag = 1.15F;
            f.tailWave = 1.2F;
            f.tailFlick = 1.0F;
            f.idleRate = 1.1F;
            break;
         case "tide":
            f.flapAmp = 0.52F;
            f.undulate = 1.0F;
            f.tailWave = 1.5F;
            f.neckSway = 1.2F;
            f.walkBob = 0.9F;
            break;
         case "solar":
            f.flapAmp = 0.64F;
            // stiffer wing: fore and hand follow the arm closely so the membranes stay joined at the elbow
            f.tipLag = 0.75F;
            f.neckSway = 1.1F;
            f.walkBob = 0.9F;
            f.hoverTilt = 0.14F;
            f.hoverBob = 0.45F;
            f.hoverAmp = 0.32F;
            f.hoverSway = 1.0F;
            f.wingWave = 1.0F;
            f.neckScale = 1.35F;
            f.tailScale = 1.4F;
            f.flicker = true;
      }

      return f;
   }

   private static float sin(float v) {
      return (float)Math.sin(v);
   }

   private static float cos(float v) {
      return (float)Math.cos(v);
   }

   private static float c01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : (v > hi ? hi : v);
   }

   private static float lerp(float a, float b, float t) {
      return a + (b - a) * t;
   }

   private static float smooth(float e0, float e1, float x) {
      float t = c01((x - e0) / (e1 - e0));
      return t * t * (3.0F - 2.0F * t);
   }

   private static float[] frame(float xx, float xy, float xz, float zx, float zy, float zz) {
      float l = (float)Math.sqrt(xx * xx + xy * xy + xz * xz);
      xx /= l;
      xy /= l;
      xz /= l;
      float d = zx * xx + zy * xy + zz * xz;
      zx -= xx * d;
      zy -= xy * d;
      zz -= xz * d;
      l = (float)Math.sqrt(zx * zx + zy * zy + zz * zz);
      zx /= l;
      zy /= l;
      zz /= l;
      float yx = zy * xz - zz * xy;
      float yy = zz * xx - zx * xz;
      float yz = zx * xy - zy * xx;
      return new float[]{xx, yx, zx, xy, yy, zy, xz, yz, zz};
   }

   private static float[] mulT(float[] a, float[] b) {
      float[] r = new float[9];

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            r[i * 3 + j] = a[i] * b[j] + a[3 + i] * b[3 + j] + a[6 + i] * b[6 + j];
         }
      }

      return r;
   }

   private static float[] euler(float[] r) {
      float ry = (float)(-Math.asin(clamp(r[6], -1.0F, 1.0F)));
      float rx = (float)Math.atan2(r[7], r[8]);
      float rz = (float)Math.atan2(r[3], r[0]);
      return new float[]{rx, ry, rz};
   }

   private static void addRot(Bone b, float rx, float ry, float rz) {
      b.rx += rx;
      b.ry += ry;
      b.rz += rz;
   }

   private static void towardRx(Bone b, float target, float w) {
      b.rx = b.rx + (target - b.rx) * w;
   }

   public static void animate(Skeleton s, AnimState a) {
      s.reset();
      DragonAnimator.Flavor F = flavor(a.kind);
      float t = a.time;
      float fly = c01(a.fly);
      float ground = 1.0F - fly;
      float sit = smooth(0.0F, 1.0F, c01(a.sit)) * ground;
      float stand = ground * (1.0F - sit);
      float walk = c01(a.walkSpeed) * stand;
      float glide = c01(a.glide) * fly;
      float speed = c01(a.speed);
      float hover = fly * (1.0F - smooth(0.12F, 0.55F, speed)) * (1.0F - c01(a.glide));
      float stoop = fly * smooth(0.0F, 1.0F, c01(a.stoop));
      float dive = Math.max(fly * smooth(0.3F, 0.9F, -a.vertVel) * c01((-a.pitch - 0.15F) * 2.0F), stoop);
      float climb = fly * c01(a.vertVel);
      float breath = c01(a.breath);
      float roar = smooth(0.0F, 1.0F, c01(a.roar));
      float hurt = c01(a.hurt);
      float bite = c01(a.bite);
      float crouch = smooth(0.0F, 1.0F, c01(a.crouch)) * ground;
      float leap = c01(a.leap);
      float flare = c01(a.flare);
      float landU = c01(a.land);
      float landF = landCurve(a) * ground;
      float landOn = a.land > 0.0F ? smooth(0.0F, 0.17F, landU) * (1.0F - smooth(0.85F, 1.0F, landU)) : 0.0F;
      float idleW = c01(a.idleW) * ground * (1.0F - c01(a.breath)) * (1.0F - smooth(0.0F, 1.0F, c01(a.roar)));
      float idleT = c01(a.idleT);
      Bone root = s.get("root");
      Bone body = s.get("body");
      Bone head = s.get("head");
      Bone jaw = s.get("jaw");
      Bone[] neck = new Bone[4];
      Bone[] tail = new Bone[6];

      for (int i = 0; i < 4; i++) {
         neck[i] = s.get(NECK[i]);
      }

      for (int i = 0; i < 6; i++) {
         tail[i] = s.get(TAIL[i]);
      }

      root.rx = root.rx + (-a.pitch - hover * F.hoverTilt);
      root.rz = root.rz + -a.roll;
      root.rx += 0.07F * crouch;
      float br = sin(t * 0.085F * F.idleRate);
      float bodyY = 0.3F * br * (1.0F - 0.6F * fly);
      float bodyX = 0.25F * sin(t * 0.031F) * stand;
      body.rx += 0.006F * br;
      body.rz = body.rz + 0.012F * sin(t * 0.031F) * stand;
      jaw.rx += 0.018F * (1.0F + br);

      for (int i = 0; i < 6; i++) {
         float ph = t * 0.07F * F.idleRate - i * 0.62F;
         float amp = (0.03F + 0.021F * i) * F.tailWave;
         tail[i].ry = tail[i].ry + amp * sin(ph) * (1.0F - 0.45F * fly) * (1.0F - 0.7F * sit);
         tail[i].rx = tail[i].rx + 0.01F * i * sin(ph * 0.7F + 1.3F) * (1.0F - 0.8F * sit);
         if (F.tailFlick > 0.0F && i >= 4) {
            float fl = sin(t * 0.045F + 2.0F);
            fl = fl > 0.82F ? (fl - 0.82F) / 0.18F : 0.0F;
            tail[i].ry = tail[i].ry + 0.22F * fl * sin(t * 0.9F) * F.tailFlick;
         }
      }

      for (int i = 0; i < 4; i++) {
         neck[i].ry = neck[i].ry + 0.018F * sin(t * 0.045F - i * 0.5F) * F.neckSway * (1.0F - 0.5F * sit);
         neck[i].rx = neck[i].rx + 0.012F * sin(t * 0.06F + i * 0.8F) * F.neckSway;
      }

      head.rx = head.rx + 0.012F * sin(t * 0.06F + 3.4F);
      float wp = a.walkPos * 0.6662F;
      if (walk > 0.001F) {
         walkLeg(s, "leg_fl", wp, walk, F, true);
         walkLeg(s, "leg_br", wp + 0.25F, walk, F, false);
         walkLeg(s, "leg_fr", wp + (float) Math.PI, walk, F, true);
         walkLeg(s, "leg_bl", wp + (float) Math.PI + 0.25F, walk, F, false);
         bodyY += walk * F.walkBob * 0.42F * cos(2.0F * wp);
         body.rz = body.rz + walk * F.walkBob * 0.028F * sin(wp);
         body.ry = body.ry + walk * 0.03F * sin(wp + 0.6F);
         body.rx = body.rx + walk * 0.012F * cos(2.0F * wp + 0.5F);

         for (int i = 0; i < 6; i++) {
            tail[i].ry = tail[i].ry + -walk * (0.035F + 0.012F * i) * sin(wp - 0.5F * i - 0.4F);
         }

         for (int i = 0; i < 4; i++) {
            neck[i].ry = neck[i].ry + walk * 0.022F * sin(wp + 0.35F * i);
         }

         neck[1].rx = neck[1].rx + walk * 0.03F * cos(2.0F * wp + 0.4F);
         neck[3].rx = neck[3].rx + -walk * 0.03F * cos(2.0F * wp + 0.4F);
         head.rx = head.rx + walk * 0.02F * cos(2.0F * wp + 1.0F);
      }

      float f = a.flapPhase;
      float flapW = 1.0F - c01(a.glide);
      boolean sway = F.hoverSway > 0.0F && hover > 0.001F;
      if (fly > 0.001F) {
         float[] neckFly = new float[]{-0.38397244F, -0.13962634F, (float) (Math.PI / 15), (float) (Math.PI / 15)};

         for (int i = 0; i < 4; i++) {
            neck[i].rx = neck[i].rx + (neckFly[i] - neck[i].rx0) * fly * 0.85F;
            neck[i].rx = neck[i].rx + fly * flapW * 0.03F * sin(f - 0.55F * (3 - i) - 0.3F);
            neck[i].rx = neck[i].rx + fly * F.undulate * 0.05F * sin(t * 0.16F + i * 0.7F);
            neck[i].rx += -climb * 0.05F + dive * 0.04F;
         }

         head.rx = head.rx + ((0.13962634F - head.rx0) * fly * 0.85F + fly * flapW * 0.03F * sin(f + 0.9F));
         tail[0].rx = tail[0].rx + (-0.05235988F - tail[0].rx0) * fly;

         for (int i = 1; i < 6; i++) {
            tail[i].rx = tail[i].rx + ((float) (Math.PI / 180.0) - tail[i].rx0) * fly;
         }

         for (int i = 0; i < 6; i++) {
            tail[i].rx = tail[i].rx + fly * flapW * (0.022F + 0.008F * i) * sin(f - 0.55F * i - 1.0F);
            tail[i].rx = tail[i].rx + fly * F.undulate * (0.03F + 0.012F * i) * sin(t * 0.16F - i * 0.7F);
            tail[i].rx += climb * 0.035F - dive * 0.02F;
         }

         bodyY += fly * flapW * 0.85F * sin(f) * (1.0F + 0.3F * hover);
         // hovering: the body rises a moment after each downstroke instead of hanging still
         bodyY += fly * flapW * hover * F.hoverBob * sin(f - 1.2F);

         if (sway) {
            for (int i = 0; i < 6; i++) {
               tail[i].rx = tail[i].rx + hover * F.hoverSway * (0.03F + 0.015F * i) * sin(f - 0.6F * i - 2.0F);
            }

            for (int i = 0; i < 4; i++) {
               neck[i].rx = neck[i].rx + hover * F.hoverSway * 0.02F * sin(f - 0.4F * i - 1.6F);
            }
         }
         body.rx = body.rx + fly * flapW * 0.025F * cos(f);
      }

      if (fly > 0.001F || flare > 0.001F || leap > 0.001F) {
         flightLegs(s, fly, hover, flare, leap);
         if (sway) {
            // dangling legs swing a little behind the wing beat
            for (String leg : SHINS) {
               Bone shin = s.get(leg);
               shin.rx = shin.rx + hover * F.hoverSway * 0.08F * sin(f - 2.2F);
            }
         }
      }

      float turn = clamp(a.yawVel, -1.0F, 1.0F) * (0.35F + 0.65F * fly) + clamp(a.roll, -1.0F, 1.0F) * 0.35F * fly;
      if (turn != 0.0F) {
         for (int i = 0; i < 4; i++) {
            neck[i].ry += turn * (0.06F + 0.035F * i);
            neck[i].rz += -turn * 0.03F * fly;
         }

         head.ry += turn * 0.1F;

         for (int i = 0; i < 6; i++) {
            tail[i].ry += -turn * (0.05F + 0.035F * i);
         }
      }

      if (sit > 0.001F) {
         sitPose(s, sit, neck, tail, head);
         float groundY = s.has("marker_ground") ? s.get("marker_ground").y0 : 38.0F;
         float bellyY = s.has("marker_belly") ? s.get("marker_belly").y0 : groundY - 17.0F;
         bodyY = bodyY * (1.0F - 0.6F * sit) + sit * (groundY - bellyY - 0.4F);
      }

      if (crouch > 0.001F) {
         neck[0].rx += 0.16F * crouch;
         neck[1].rx += 0.06F * crouch;
         neck[2].rx += -0.1F * crouch;
         neck[3].rx += -0.14F * crouch;
         head.rx += -0.18F * crouch;
         jaw.rx += 0.06F * crouch;
         tail[0].rx += 0.12F * crouch;

         for (int i = 1; i < 6; i++) {
            tail[i].rx += 0.025F * crouch;
         }
      }

      if (leap > 0.001F) {
         neck[0].rx += -0.1F * leap;
         neck[1].rx += -0.04F * leap;
         neck[3].rx += 0.06F * leap;
         head.rx += 0.06F * leap;

         for (int i = 0; i < 6; i++) {
            tail[i].rx += -(0.05F + 0.012F * i) * leap * (1.0F - 0.6F * fly);
         }
      }

      if (flare > 0.001F) {
         for (int i = 0; i < 4; i++) {
            neck[i].rx += -0.07F * flare;
         }

         head.rx += 0.16F * flare;
         tail[0].rx += -0.05F * flare;

         for (int i = 1; i < 6; i++) {
            tail[i].rx += 0.035F * flare;
         }
      }

      if (landF != 0.0F) {
         float jolt = landF;
         neck[0].rx += 0.12F * jolt;
         neck[1].rx += 0.06F * jolt;
         neck[3].rx += -0.04F * jolt;
         head.rx += -0.1F * jolt;
         jaw.rx = jaw.rx + 0.12F * Math.max(0.0F, jolt);

         for (int i = 0; i < 6; i++) {
            tail[i].rx += -0.03F * (i + 1) / 3.0F * jolt;
         }
      }

      float[] wingIdle = new float[]{0.0F, 0.0F, 0.0F};
      if (idleW > 0.001F && a.idle != 0) {
         idleAction(a.idle, idleT, idleW, t, root, body, head, jaw, neck, tail, wingIdle);
      }

      float dip = seatDip(a);
      float plantW = (1.0F - sit) * Math.max(ground * (1.0F - leap), landOn);
      if (plantW > 0.001F && (Math.abs(dip) > 0.001F || Math.abs(root.rx) > 0.001F)) {
         plantLegs(s, dip, root.rx, plantW);
      }

      float yaw = clamp(a.headYaw, -1.5F, 1.5F);
      float pit = clamp(a.headPitch, -0.9F, 0.9F);
      float[] wy = new float[]{0.1F, 0.16F, 0.2F, 0.2F, 0.34F};
      float[] wpit = new float[]{0.06F, 0.1F, 0.18F, 0.24F, 0.42F};

      for (int i = 0; i < 4; i++) {
         neck[i].ry = neck[i].ry + yaw * wy[i];
         neck[i].rx = neck[i].rx + pit * wpit[i];
      }

      head.ry = head.ry + yaw * wy[4];
      head.rx = head.rx + pit * wpit[4];
      head.rz += yaw * 0.08F;
      if (breath > 0.001F) {
         float[] tgt = new float[]{(float) (-Math.PI / 6), -0.10471976F, 0.2443461F, 0.27925268F};
         float w = breath * 0.85F;

         for (int i = 0; i < 4; i++) {
            float base = lerp(neck[i].rx0, i == 0 ? -0.38397244F : (i == 1 ? -0.13962634F : (float) (Math.PI / 15)), fly * 0.85F);
            neck[i].rx = neck[i].rx + (lerp(tgt[i], base, fly * 0.6F) - base) * w;
         }

         float hbase = lerp(head.rx0, 0.13962634F, fly * 0.85F);
         head.rx += (0.10471976F - hbase) * w;
         jaw.rx += 0.62F * breath;
         head.rz = head.rz + breath * 0.03F * sin(t * 2.3F);
         head.ry = head.ry + breath * 0.02F * sin(t * 1.7F + 1.0F);
         neck[3].rx = neck[3].rx + breath * 0.012F * sin(t * 2.9F);
         body.rx += breath * 0.03F * stand;
      }

      if (roar > 0.001F) {
         float w = roar * F.roarLift;
         neck[0].rx += -0.08F * w;
         neck[1].rx += -0.12F * w;
         neck[2].rx += -0.12F * w;
         neck[3].rx += -0.14F * w;
         head.rx += -0.3F * w;
         jaw.rx += 0.78F * roar;
         head.rz = head.rz + roar * 0.035F * sin(t * 2.9F);
         head.ry = head.ry + roar * 0.025F * sin(t * 2.1F);
         body.rx += -0.035F * roar * stand;

         for (int i = 0; i < 6; i++) {
            tail[i].rx += roar * 0.035F * stand;
         }
      }

      if (bite > 0.001F) {
         float lunge = (float)Math.pow(Math.max(0.0F, sin((float) Math.PI * bite)), 0.8F);
         neck[0].rx += 0.36F * lunge;
         neck[1].rx += 0.24F * lunge;
         neck[2].rx += -0.08F * lunge;
         neck[3].rx += -0.2F * lunge;
         head.rx += -0.14F * lunge;
         float open = smooth(0.0F, 0.22F, bite) * (1.0F - smooth(0.5F, 0.66F, bite));
         jaw.rx += 0.75F * open;
         head.rx = head.rx + 0.22F * smooth(0.5F, 0.66F, bite) * (1.0F - smooth(0.8F, 1.0F, bite));
         body.rx += 0.03F * lunge * stand;
      }

      if (hurt > 0.001F) {
         neck[0].rx += -0.16F * hurt;
         neck[2].rx += -0.1F * hurt;
         head.rx += -0.12F * hurt;
         head.ry = head.ry + hurt * 0.26F * sin(t * 2.2F);
         head.rz = head.rz + hurt * 0.12F * sin(t * 2.9F + 1.0F);
         body.rz = body.rz + hurt * 0.045F * sin(t * 2.6F);
         jaw.rx += 0.28F * hurt;

         for (int i = 0; i < 6; i++) {
            tail[i].ry = tail[i].ry + hurt * 0.1F * sin(t * 1.7F + i * 0.9F);
         }
      }

      float lift = c01(a.wingLift);
      float lockW = lift * (1.0F - smooth(0.3F, 0.9F, fly));
      float lockS = 1.0F;
      if (wingIdle[1] > 0.0F) {
         lockS = (lockW + wingIdle[1] * wingIdle[2]) / (lockW + wingIdle[1]);
         lockW = Math.max(lockW, wingIdle[1]);
      }

      float unfold = Math.max(fly, Math.max(0.3F * roar * F.roarLift, 0.14F * hurt) * (1.0F - 0.5F * sit));
      unfold = Math.max(unfold, Math.max(0.8F * lift, wingIdle[0]));
      wings(s, a, F, unfold, fly, glide, hover, dive, stoop, climb, br, sit, lockW, lockS, flare);
      proportions(s, F);
      if (F.flicker) {
         flicker(s, t);
      }
      float lim = 1.4F;
      body.x = body.x + clamp(bodyX, -lim, lim);
      body.y = body.y + ((sit > 0.001F ? bodyY : clamp(bodyY, -lim, lim)) + dip);
      s.get("saddle").visible = a.saddled;
   }

   private static void walkLeg(Skeleton s, String name, float ph, float walk, DragonAnimator.Flavor F, boolean front) {
      Bone thigh = s.get(name);
      Bone shin = s.get(name + "_shin");
      Bone foot = s.get(name + "_foot");
      float sw = -cos(ph);
      float lift = Math.max(0.0F, -sin(ph));
      lift = lift * lift * (3.0F - 2.0F * lift);
      float A = F.walkSwing * walk;
      float dThigh = A * sw - (front ? 0.22F : 0.12F) * lift * walk;
      float dShin = (front ? -0.55F : 0.6F) * lift * walk;
      dShin += (front ? 0.1F : -0.12F) * A * sin(ph + 0.6F);
      thigh.rx += dThigh;
      shin.rx += dShin;
      foot.rx += -(dThigh + dShin) * 0.85F + 0.4F * lift * walk;
   }

   private static void flightLegs(Skeleton s, float fly, float hover, float flare, float leap) {
      float tk = smooth(0.25F, 1.0F, fly) * (1.0F - flare);
      float hang = hover * 0.55F;

      for (int k = 0; k < 2; k++) {
         String side = k == 0 ? "l" : "r";
         Bone t = s.get("leg_f" + side);
         Bone sh = s.get("leg_f" + side + "_shin");
         Bone ft = s.get("leg_f" + side + "_foot");
         float tT = lerp(1.1170107F, 0.27925268F, hang);
         float tS = lerp(0.2443461F, -0.06981317F, hang);
         float tF = lerp(0.5934119F, (float) (Math.PI / 15), hang);
         t.rx = t.rx + (tT - t.rx0) * tk;
         sh.rx = sh.rx + (tS - sh.rx0) * tk;
         ft.rx = ft.rx + (tF - ft.rx0) * tk;
         t.rx += -0.34F * flare;
         sh.rx += -0.1F * flare;
         ft.rx += 0.3F * flare;
         t = s.get("leg_b" + side);
         sh = s.get("leg_b" + side + "_shin");
         ft = s.get("leg_b" + side + "_foot");
         tT = lerp(0.9075712F, (float) (Math.PI / 18), hang);
         tS = lerp((float) (Math.PI / 6), (float) (Math.PI * 2.0 / 9.0), hang);
         tF = lerp(0.4886922F, (float) (-Math.PI / 18), hang);
         t.rx = t.rx + (tT - t.rx0) * tk;
         sh.rx = sh.rx + (tS - sh.rx0) * tk;
         ft.rx = ft.rx + (tF - ft.rx0) * tk;
         t.rx += -0.26F * flare;
         sh.rx += 0.04F * flare;
         ft.rx += 0.32F * flare;
         float push = leap * (1.0F - 0.7F * tk);
         t.rx += 0.42F * push;
         sh.rx += 0.22F * push;
         ft.rx += 0.6F * push;
      }
   }

   private static void plantLegs(Skeleton s, float dip, float rootRx, float w) {
      float c = cos(rootRx);
      float sn = sin(rootRx);

      for (int k = 0; k < 4; k++) {
         boolean front = k < 2;
         String name = (front ? "leg_f" : "leg_b") + (k % 2 == 0 ? "l" : "r");
         Bone t = s.get(name);
         Bone sh = s.get(name + "_shin");
         Bone ft = s.get(name + "_foot");
         float l1 = sh.y0;
         float l2 = ft.y0;
         if (!(l1 <= 0.0F) && !(l2 <= 0.0F)) {
            float a1 = t.rx;
            float a2 = t.rx + sh.rx;
            float ay = t.y0 + l1 * cos(a1) + l2 * cos(a2);
            float az = t.z0 + l1 * sin(a1) + l2 * sin(a2);
            float ty = ay * c + az * sn - dip;
            float tz = -ay * sn + az * c;
            float vy = ty - t.y0;
            float vz = tz - t.z0;
            float r = (float)Math.sqrt(vy * vy + vz * vz);
            float r0 = (float)Math.sqrt((ay - t.y0) * (ay - t.y0) + (az - t.z0) * (az - t.z0));
            float rMax = l1 + l2 - 0.6F;
            float start = Math.min(Math.max(r0, rMax - 2.0F), rMax - 0.05F);
            if (r > start) {
               float span = rMax - start;
               r = rMax - span * (float)Math.exp(-(r - start) / span);
            }

            r = Math.max(r, Math.abs(l1 - l2) + 0.5F);
            float sgn = front ? -1.0F : 1.0F;
            float knee = (float)Math.acos(clamp((l1 * l1 + l2 * l2 - r * r) / (2.0F * l1 * l2), -1.0F, 1.0F));
            float alpha = (float)Math.acos(clamp((l1 * l1 + r * r - l2 * l2) / (2.0F * l1 * r), -1.0F, 1.0F));
            float n1 = (float)Math.atan2(vz, vy) - sgn * alpha;
            float n2 = sgn * ((float) Math.PI - knee);
            float footAbs = t.rx + sh.rx + ft.rx;
            float n3 = footAbs - rootRx - n1 - n2;
            t.rx = t.rx + (n1 - t.rx) * w;
            sh.rx = sh.rx + (n2 - sh.rx) * w;
            ft.rx = ft.rx + (n3 - ft.rx) * w;
         }
      }
   }

   private static float landCurve(AnimState a) {
      if (a.land <= 0.0F) {
         return 0.0F;
      }

      float u = c01(a.land);
      float tau = u * 18.0F;
      float f = tau < 2.5F ? smooth(0.0F, 2.5F, tau) : (float)Math.exp(-(tau - 2.5F) / 4.0F) * cos((tau - 2.5F) * 0.42F);
      return f * (1.0F - smooth(0.7F, 1.0F, u)) * (0.45F + 0.55F * c01(a.landPow));
   }

   public static float seatDip(AnimState a) {
      return 4.0F * smooth(0.0F, 1.0F, c01(a.crouch)) * (1.0F - c01(a.fly)) + 4.5F * landCurve(a);
   }

   private static float bump(float u, float a, float b, float ramp) {
      return smooth(a, a + ramp, u) * (1.0F - smooth(b - ramp, b, u));
   }

   private static void idleAction(int act, float u, float w, float t, Bone root, Bone body, Bone head, Bone jaw, Bone[] neck, Bone[] tail, float[] out) {
      switch (act) {
         case 1:
         case 2:
            float sd = act == 1 ? -1.0F : 1.0F;
            float e1 = bump(u, 0.03F, 0.47F, 0.13F);
            float e2 = bump(u, 0.5F, 0.96F, 0.13F);
            float yaw = sd * (1.05F * e1 - 0.95F * e2) * w;
            float pit = -0.28F * (e1 + e2) * w;

            for (int i = 0; i < 4; i++) {
               neck[i].ry = neck[i].ry + yaw * LOOK_W[i] * 1.2F;
               neck[i].rx = neck[i].rx + pit * LOOK_P[i];
            }

            head.ry += yaw * 0.3F;
            head.rx += pit * 0.42F;
            head.rz += yaw * 0.12F;

            for (int i = 0; i < 6; i++) {
               tail[i].ry += -yaw * 0.035F * (i + 1) / 3.0F;
            }
            break;
         case 3: {
            float env = smooth(0.0F, 0.22F, u) * (1.0F - smooth(0.74F, 0.98F, u)) * w;
            float bow = bump(u, 0.18F, 0.78F, 0.18F) * w;
            out[0] = 0.96F * env;
            out[1] = env;
            out[2] = 0.84F + 0.1F * sin(t * 0.85F) * bow;
            root.rx += 0.15F * bow;
            neck[0].rx += 0.26F * bow - 0.06F * env;
            neck[1].rx += 0.05F * bow;
            neck[2].rx += -0.06F * bow;
            neck[3].rx += -0.1F * bow;
            head.rx += -0.12F * bow;
            jaw.rx += 0.16F * bow;

            for (int i = 0; i < 6; i++) {
               tail[i].rx += 0.04F * env;
               tail[i].ry = tail[i].ry + 0.05F * sin(t * 0.5F - i * 0.8F) * env;
            }
            break;
         }
         case 4:
            float lift = smooth(0.0F, 0.3F, u) * (1.0F - smooth(0.74F, 1.0F, u)) * w;
            float open = smooth(0.14F, 0.44F, u) * (1.0F - smooth(0.62F, 0.72F, u)) * w;
            neck[0].rx += -0.12F * lift;
            neck[1].rx += -0.08F * lift;
            neck[2].rx += -0.04F * lift;
            neck[3].rx += 0.03F * lift;
            head.rx += -0.3F * lift;
            jaw.rx += 0.88F * open;
            head.rz = head.rz + 0.05F * sin(t * 0.7F) * open;
            float shake = bump(u, 0.72F, 0.94F, 0.06F) * w;
            head.ry = head.ry + 0.09F * sin(t * 1.9F) * shake;
            head.rz = head.rz + 0.06F * sin(t * 1.9F + 1.2F) * shake;
            break;
         case 5:
            float env = smooth(0.0F, 0.12F, u) * (1.0F - smooth(0.7F, 1.0F, u)) * w;
            float k = 1.55F;
            body.rz = body.rz + 0.11F * env * sin(t * 1.55F - 1.0F);

            for (int i = 0; i < 4; i++) {
               float ph = t * 1.55F + 0.55F * (3 - i);
               neck[i].rz = neck[i].rz + 0.05F * env * sin(ph);
               neck[i].ry = neck[i].ry + 0.05F * env * sin(ph + 0.4F);
            }

            head.rz = head.rz + 0.16F * env * sin(t * 1.55F + 2.4F);
            head.ry = head.ry + 0.1F * env * sin(t * 1.55F + 2.8F);
            jaw.rx += 0.1F * env;

            for (int i = 0; i < 6; i++) {
               tail[i].ry = tail[i].ry + 0.07F * env * sin(t * 1.55F - 1.6F - 0.6F * i);
            }

            out[0] = 0.2F * env * (0.8F + 0.2F * sin(t * 1.55F * 2.0F));
            out[1] = env;
            out[2] = 0.3F + 0.55F * sin(t * 1.55F + 0.6F);
      }
   }

   private static void sitPose(Skeleton s, float sit, Bone[] neck, Bone[] tail, Bone head) {
      float groundY = s.has("marker_ground") ? s.get("marker_ground").y0 : 38.0F;
      float bellyY = s.has("marker_belly") ? s.get("marker_belly").y0 : groundY - 17.0F;

      for (int k = 0; k < 4; k++) {
         boolean front = k < 2;
         String name = (front ? "leg_f" : "leg_b") + (k % 2 == 0 ? "l" : "r");
         Bone t = s.get(name);
         Bone sh = s.get(name + "_shin");
         Bone ft = s.get(name + "_foot");
         float lt = sh.y0;
         float ls = ft.y0;
         float restT = t.rx0;
         float restS = t.rx0 + sh.rx0;
         float hf = groundY - t.y0 - lt * cos(restT) - ls * cos(restS);
         float h = bellyY - t.y0 + 0.4F;
         float aT = front ? (float) (Math.PI * 2.0 / 5.0) : -1.3613569F;
         float c = clamp((h - hf - lt * cos(aT)) / Math.max(ls, 0.01F), -0.35F, 1.0F);
         float aS = (float)Math.acos(c) * (front ? -1 : 1);
         t.rx = t.rx + (aT - t.rx) * sit;
         sh.rx = sh.rx + (aS - aT - sh.rx) * sit;
         ft.rx = ft.rx + (-aS - ft.rx) * sit;
         float sgn = k % 2 == 0 ? 1.0F : -1.0F;
         t.rz += -sgn * 0.1F * sit;
      }

      float[] tgt = new float[]{-0.27925268F, -0.13962634F, (float) (Math.PI / 15), 0.27925268F};

      for (int i = 0; i < 4; i++) {
         neck[i].rx = neck[i].rx + (tgt[i] - neck[i].rx) * sit * 0.9F;
         neck[i].ry += sit * 0.1F;
      }

      head.rx = head.rx + (0.06981317F - head.rx) * sit * 0.9F;
      head.ry += sit * 0.06F;
      tail[0].rx = tail[0].rx + (-0.10471976F - tail[0].rx) * sit;
      tail[0].ry += sit * 0.3F;

      for (int i = 1; i < 6; i++) {
         tail[i].rx = tail[i].rx + ((i >= 4 ? 7.0F : 1.5F) * (float) (Math.PI / 180.0) - tail[i].rx) * sit;
         tail[i].ry += sit * (0.4F + 0.03F * i);
      }
   }

   private static float[] rotZYX(float rx, float ry, float rz) {
      float cx = cos(rx);
      float sx = sin(rx);
      float cy = cos(ry);
      float sy = sin(ry);
      float cz = cos(rz);
      float sz = sin(rz);
      return new float[]{
         cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx, sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx, -sy, cy * sx, cy * cx
      };
   }

   private static float[] mul(float[] a, float[] b) {
      float[] r = new float[9];

      for (int i = 0; i < 3; i++) {
         for (int j = 0; j < 3; j++) {
            r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
         }
      }

      return r;
   }

   private static float[] quat(float[] m) {
      float tr = m[0] + m[4] + m[8];
      float w;
      float x;
      float y;
      float z;
      if (tr > 0.0F) {
         float q = (float)Math.sqrt(tr + 1.0) * 2.0F;
         w = 0.25F * q;
         x = (m[7] - m[5]) / q;
         y = (m[2] - m[6]) / q;
         z = (m[3] - m[1]) / q;
      } else if (m[0] > m[4] && m[0] > m[8]) {
         float q = (float)Math.sqrt(1.0 + m[0] - m[4] - m[8]) * 2.0F;
         w = (m[7] - m[5]) / q;
         x = 0.25F * q;
         y = (m[1] + m[3]) / q;
         z = (m[2] + m[6]) / q;
      } else if (m[4] > m[8]) {
         float q = (float)Math.sqrt(1.0 + m[4] - m[0] - m[8]) * 2.0F;
         w = (m[2] - m[6]) / q;
         x = (m[1] + m[3]) / q;
         y = 0.25F * q;
         z = (m[5] + m[7]) / q;
      } else {
         float q = (float)Math.sqrt(1.0 + m[8] - m[0] - m[4]) * 2.0F;
         w = (m[3] - m[1]) / q;
         x = (m[2] + m[6]) / q;
         y = (m[5] + m[7]) / q;
         z = 0.25F * q;
      }

      return new float[]{w, x, y, z};
   }

   private static float[] slerp(float[] ma, float[] mb, float t) {
      if (t <= 0.0F) {
         return ma;
      }

      if (t >= 1.0F) {
         return mb;
      }

      float[] a = quat(ma);
      float[] b = quat(mb);
      float d = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
      if (d < 0.0F) {
         d = -d;
         b = new float[]{-b[0], -b[1], -b[2], -b[3]};
      }

      float wa;
      float wb;
      if (d > 0.9995F) {
         wa = 1.0F - t;
         wb = t;
      } else {
         float th = (float)Math.acos(d);
         float sn = sin(th);
         wa = sin((1.0F - t) * th) / sn;
         wb = sin(t * th) / sn;
      }

      float w = wa * a[0] + wb * b[0];
      float x = wa * a[1] + wb * b[1];
      float y = wa * a[2] + wb * b[2];
      float z = wa * a[3] + wb * b[3];
      float n = (float)Math.sqrt(w * w + x * x + y * y + z * z);
      w /= n;
      x /= n;
      y /= n;
      z /= n;
      return new float[]{
         1.0F - 2.0F * (y * y + z * z),
         2.0F * (x * y - z * w),
         2.0F * (x * z + y * w),
         2.0F * (x * y + z * w),
         1.0F - 2.0F * (x * x + z * z),
         2.0F * (y * z - x * w),
         2.0F * (x * z - y * w),
         2.0F * (y * z + x * w),
         1.0F - 2.0F * (x * x + y * y)
      };
   }

   private static void wings(
      Skeleton s,
      AnimState a,
      DragonAnimator.Flavor F,
      float unfold,
      float fly,
      float glide,
      float hover,
      float dive,
      float stoop,
      float climb,
      float br,
      float sit,
      float lockW,
      float lockS,
      float flare
   ) {
      float f = a.flapPhase;
      float lockC = (float)Math.sqrt(Math.max(0.0F, 1.0F - lockS * lockS));
      float sf = lerp(sin(f), lockS, lockW);
      float cf = lerp(cos(f), lockC, lockW);
      float flapW = 1.0F - c01(a.glide);
      float amp = F.flapAmp * flapW * (1.0F + F.hoverAmp * hover + 0.2F * climb) * (1.0F - 0.65F * dive) * (1.0F - 0.8F * stoop);
      float k = amp / Math.max(F.flapAmp, 0.01F);
      float upfold = (float)Math.pow(Math.max(0.0F, cf), 1.5) * k;
      float gl = c01(a.glide);
      float sfe = sf >= 0.0F ? sf : sf * lerp(0.25F, 1.0F, smooth(0.7F, 1.0F, fly)) * (1.0F - 0.45F * flare);
      float aRz = -(0.1F + amp * sfe + 0.1F * gl - 0.14F * dive + 0.1F * stoop);
      float aRy = -(0.26F + 0.18F * upfold + 0.2F * dive + 0.3F * stoop) + hover * 0.2F * -cf * flapW + 0.3F * flare;
      float aRx = 0.24F * -cf * k + 0.06F * climb - 0.06F * dive - 0.1F * flare;
      float fRz = -(F.tipLag * amp * 0.42F * -(cf * 0.9689F + sf * 0.2474F) - 0.1F * gl - 0.04F * dive);
      float fRy = 0.42F + 0.4F * upfold - 0.5F * dive - 0.2F * stoop - 0.12F * flare;
      float flutter = 0.028F * sin(a.time * 1.1F) * gl + 0.012F * sin(a.time * 0.37F) * gl;
      float hRz = -(F.tipLag * amp * 0.52F * -(cf * 0.7648F + sf * 0.6442F) - 0.08F * gl + flutter);
      float hRy = -(0.2F + 0.2F * upfold + 0.45F * dive - 0.15F * stoop);
      float hRx = 0.1F * -(cf * 0.8776F + sf * 0.4794F) * k;
      if (F.wingWave > 0.0F) {
         // the beat travels out through the wing: fore and hand bend the same way as the arm, a little later,
         // so the wing stays one smooth curve instead of zig-zagging at the elbow and wrist
         float sfLagF = sf * 0.8525F - cf * 0.5227F;
         float sfLagH = sf * 0.4536F - cf * 0.8912F;
         fRz = -(F.wingWave * amp * 0.34F * sfLagF - 0.1F * gl - 0.04F * dive);
         hRz = -(F.wingWave * amp * 0.26F * sfLagH - 0.08F * gl + flutter);
         // bending up and down happens around the joint line, so the membranes stay joined; sweeping back and
         // twisting open wedge-shaped gaps at elbow and wrist, so keep those small in flight
         fRy = 0.1F + 0.1F * upfold - 0.5F * dive - 0.2F * stoop - 0.12F * flare;
         hRy = -(0.04F + 0.05F * upfold + 0.45F * dive - 0.15F * stoop);
         hRx = 0.0F;
      }
      float szFly = 1.0F - 0.14F * upfold - 0.22F * dive - 0.25F * stoop;
      float[] eA;
      float[] eF;
      float[] eH;
      float szA;
      float szF;
      float szH;
      if (unfold >= 0.9999F) {
         eA = new float[]{aRx, aRy, aRz};
         eF = new float[]{0.0F, fRy, fRz};
         eH = new float[]{hRx, hRy, hRz};
         szH = szFly;
         szF = szFly;
         szA = szFly;
      } else {
         float[] k0a = frame(0.3F, -0.38F, 0.87F, 0.3F, 1.0F, 0.1F);
         float[] k0f = frame(0.1F, 0.3F, -0.95F, 0.25F, 1.0F, 0.0F);
         float[] k0h = frame(0.05F, 0.22F, 0.97F, 0.2F, 1.0F, 0.0F);
         float[] k1a = frame(0.52F, -0.72F, 0.46F, -0.15F, 0.35F, 1.0F);
         float[] k1f = frame(0.5F, -0.84F, -0.2F, 0.0F, 0.3F, 1.0F);
         float[] k1h = frame(0.78F, -0.56F, 0.28F, 0.0F, 0.25F, 1.0F);
         float[] k2a = rotZYX(aRx, aRy, aRz);
         float[] k2f = mul(k2a, rotZYX(0.0F, fRy, fRz));
         float[] k2h = mul(k2f, rotZYX(hRx, hRy, hRz));
         float p = unfold * 2.0F;
         float[] A;
         float[] Fo;
         float[] H;
         if (p <= 1.0F) {
            float w = smooth(0.0F, 1.0F, p);
            A = slerp(k0a, k1a, smooth(0.0F, 0.85F, p));
            Fo = slerp(k0f, k1f, w);
            H = slerp(k0h, k1h, smooth(0.15F, 1.0F, p));
            szA = lerp(F.foldSz, 0.6F, w);
            szF = lerp(F.foldSz, 0.55F, w);
            szH = lerp(F.foldSz, 0.5F, smooth(0.3F, 1.0F, p));
         } else {
            float w = smooth(0.0F, 1.0F, p - 1.0F);
            A = slerp(k1a, k2a, w);
            Fo = slerp(k1f, k2f, w);
            H = slerp(k1h, k2h, w);
            szA = lerp(0.6F, szFly, w);
            szF = lerp(0.55F, szFly, w);
            szH = lerp(0.5F, szFly, w);
         }

         eA = euler(A);
         eF = euler(mulT(A, Fo));
         eH = euler(mulT(Fo, H));
         float folded = 1.0F - c01(p);
         eA[2] += 0.02F * br * folded;
         eA[0] += -0.1F * sit * folded;
      }

      for (int side = 0; side < 2; side++) {
         float m = side == 0 ? 1.0F : -1.0F;
         String n = side == 0 ? "wing_l" : "wing_r";
         Bone arm = s.get(n);
         Bone fore = s.get(n + "_fore");
         Bone hand = s.get(n + "_hand");
         arm.rx = arm.rx0 + eA[0];
         arm.ry = arm.ry0 + m * eA[1];
         arm.rz = arm.rz0 + m * eA[2];
         fore.rx = fore.rx0 + eF[0];
         fore.ry = fore.ry0 + m * eF[1];
         fore.rz = fore.rz0 + m * eF[2];
         hand.rx = hand.rx0 + eH[0];
         hand.ry = hand.ry0 + m * eH[1];
         hand.rz = hand.rz0 + m * eH[2];
         s.get(n + "_mem").sz = szA;
         s.get(n + "_fore_mem").sz = szF;
         s.get(n + "_hand_mem").sz = szH;
         Bone web = s.get(n + "_web");
         float flex = Math.max(0.0F, eF[1]) * smooth(0.55F, 1.0F, unfold);
         float wsx = clamp((float)Math.tan(Math.min(flex, 1.2F)) / 0.4663F, 0.0F, 2.8F);
         web.sx = Math.max(wsx, 0.001F);
         web.sz = szA;
         web.visible = wsx > 0.02F;
      }
   }

   /** Solar wyrm: corona rays and back spines flicker in length like solar flames. */
   private static void flicker(Skeleton s, float t) {
      for (int i = 0; i < CROWN_RAYS.length; i++) {
         Bone ray = s.get(CROWN_RAYS[i]);
         ray.sy = ray.sy * (1.0F + 0.16F * sin(t * 0.45F + i * 1.9F) + 0.07F * sin(t * 1.35F + i * 0.7F));
      }

      for (int i = 0; i < DORSAL_SPINES.length; i++) {
         Bone spine = s.get(DORSAL_SPINES[i]);
         spine.sy = spine.sy * (1.0F + 0.12F * sin(t * 0.5F - i * 0.55F) + 0.05F * sin(t * 1.6F + i * 1.3F));
      }
   }

   private static String[] numbered(String prefix, int count) {
      String[] names = new String[count];
      for (int i = 0; i < count; i++) {
         names[i] = prefix + i;
      }

      return names;
   }

   /** Per-kind size of neck and tail, applied as bone scale so textures and the mouth position follow. */
   private static void proportions(Skeleton s, DragonAnimator.Flavor F) {
      if (F.neckScale != 1.0F) {
         scale(s.get("neck1"), F.neckScale);
         // the head is a child of the neck: undo the scale so only the neck grows
         scale(s.get("head"), 1.0F / F.neckScale);
      }

      if (F.tailScale != 1.0F) {
         scale(s.get("tail1"), F.tailScale);
      }
   }

   private static void scale(Bone bone, float k) {
      bone.sx *= k;
      bone.sy *= k;
      bone.sz *= k;
   }

   private static final class Flavor {
      float flapAmp = 0.6F;
      float tipLag = 1.0F;
      float walkSwing = 0.4F;
      float walkBob = 1.0F;
      float tailWave = 1.0F;
      float neckSway = 1.0F;
      float idleRate = 1.0F;
      float undulate = 0.0F;
      float foldSz = 0.35F;
      float tailFlick = 0.0F;
      float roarLift = 1.0F;
      /** How far the body leans back while hovering (radians). */
      float hoverTilt = 0.3F;
      /** Extra body rise and fall per wing beat while hovering. */
      float hoverBob = 0.0F;
      /** Extra wing beat amplitude while hovering. */
      float hoverAmp = 0.28F;
      /** Tail, neck and leg follow-through while hovering (0 = off). */
      float hoverSway = 0.0F;
      /** Above 0: fore and hand follow the arm as a travelling wave (smooth curve) instead of the default lag. */
      float wingWave = 0.0F;
      float neckScale = 1.0F;
      float tailScale = 1.0F;
      boolean flicker = false;
   }
}
