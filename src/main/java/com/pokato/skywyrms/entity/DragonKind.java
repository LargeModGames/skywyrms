package com.pokato.skywyrms.entity;

public enum DragonKind {
   EMBER("ember_wyrm", "fire", 100.0, 10.0, 8.0, 0.6, 0.25, 1.0F, DragonKind.Ability.FIRE_BREATH, 16747050),
   FROST("frost_wyrm", "ice", 80.0, 8.0, 6.0, 0.66, 0.27, 1.0F, DragonKind.Ability.FROST_BREATH, 10480639),
   STORM("storm_wyrm", "storm", 70.0, 7.0, 4.0, 0.82, 0.27, 0.85F, DragonKind.Ability.LIGHTNING, 16771402),
   THORN("thorn_wyrm", "thorn", 110.0, 9.0, 10.0, 0.52, 0.23, 1.1F, DragonKind.Ability.VENOM_SPIT, 10215482),
   VOID("void_wyrm", "void", 90.0, 11.0, 6.0, 0.72, 0.27, 0.95F, DragonKind.Ability.VOID_BLINK, 13786111),
   TIDE("tide_wyrm", "tide", 80.0, 8.0, 6.0, 0.62, 0.25, 0.9F, DragonKind.Ability.TIDAL_JET, 4578790),
   SOLAR("solar_wyrm", "solar", 85.0, 9.0, 6.0, 0.9, 0.29, 0.8F, DragonKind.Ability.SUN_BEAM, 16767334);

   private final String id;
   private final String animKey;
   private final double maxHealth;
   private final double attackDamage;
   private final double armor;
   private final double flyingSpeed;
   private final double walkSpeed;
   private final float flapRate;
   private final DragonKind.Ability ability;
   private final int color;
   private static final DragonKind[] VALUES = values();

   DragonKind(
      String id,
      String animKey,
      double maxHealth,
      double attackDamage,
      double armor,
      double flyingSpeed,
      double walkSpeed,
      float flapRate,
      DragonKind.Ability ability,
      int color
   ) {
      this.id = id;
      this.animKey = animKey;
      this.maxHealth = maxHealth;
      this.attackDamage = attackDamage;
      this.armor = armor;
      this.flyingSpeed = flyingSpeed;
      this.walkSpeed = walkSpeed;
      this.flapRate = flapRate;
      this.ability = ability;
      this.color = color;
   }

   public String id() {
      return this.id;
   }

   public String animKey() {
      return this.animKey;
   }

   public double maxHealth() {
      return this.maxHealth;
   }

   public double attackDamage() {
      return this.attackDamage;
   }

   public double armor() {
      return this.armor;
   }

   public double flyingSpeed() {
      return this.flyingSpeed;
   }

   public double walkSpeed() {
      return this.walkSpeed;
   }

   public float flapRate() {
      return this.flapRate;
   }

   public DragonKind.Ability ability() {
      return this.ability;
   }

   public int color() {
      return this.color;
   }

   public GeoData geo() {
      return GeoData.get(this);
   }

   public float scale() {
      return this.geo().scale;
   }

   public boolean fireImmune() {
      return this == EMBER;
   }

   public String element() {
      return this.id.substring(0, this.id.indexOf(95));
   }

   public static DragonKind byName(String name) {
      if (name == null) {
         return null;
      }

      String n = name.substring(name.indexOf(58) + 1);

      for (DragonKind kind : values()) {
         if (kind.id.equals(n) || kind.element().equals(n)) {
            return kind;
         }
      }

      return null;
   }

   public static DragonKind byOrdinal(int ordinal) {
      return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : EMBER;
   }

   public enum Ability {
      FIRE_BREATH(true, "fire_breath"),
      FROST_BREATH(true, "frost_breath"),
      LIGHTNING(false, "lightning"),
      VENOM_SPIT(false, "venom_spit"),
      VOID_BLINK(false, "void_blink"),
      TIDAL_JET(true, "tidal_jet"),
      SUN_BEAM(true, "sun_beam");

      private final boolean held;
      private final String id;

      Ability(boolean held, String id) {
         this.held = held;
         this.id = id;
      }

      public boolean held() {
         return this.held;
      }

      public String id() {
         return this.id;
      }
   }
}
