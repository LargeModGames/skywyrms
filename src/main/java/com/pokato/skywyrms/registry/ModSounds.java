package com.pokato.skywyrms.registry;

import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.entity.DragonKind;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
   public static final String FLAP = "dragon.flap";
   public static final String STEP = "dragon.step";
   public static final String FIRE_BREATH = "ability.fire_breath";
   public static final String FROST_BREATH = "ability.frost_breath";
   public static final String TIDAL_JET = "ability.tidal_jet";
   public static final String LIGHTNING = "ability.lightning";
   public static final String VENOM_SPIT = "ability.venom_spit";
   public static final String VENOM_SPLAT = "ability.venom_splat";
   public static final String VOID_BLINK = "ability.void_blink";
   public static final String VOID_ARRIVE = "ability.void_arrive";
   public static final String EGG_CRACK = "egg.crack";
   public static final String EGG_HATCH = "egg.hatch";
   private static final Map<String, SoundEvent> EVENTS = new LinkedHashMap<>();

   private ModSounds() {
   }

   public static List<String> names() {
      List<String> names = new ArrayList<>();

      for (DragonKind kind : DragonKind.values()) {
         for (ModSounds.Voice voice : ModSounds.Voice.values()) {
            names.add(voiceName(kind, voice));
         }
      }

      Collections.addAll(
         names,
         "dragon.flap",
         "dragon.step",
         "ability.fire_breath",
         "ability.frost_breath",
         "ability.tidal_jet",
         "ability.lightning",
         "ability.venom_spit",
         "ability.venom_splat",
         "ability.void_blink",
         "ability.void_arrive",
         "egg.crack",
         "egg.hatch"
      );
      return names;
   }

   public static String voiceName(DragonKind kind, ModSounds.Voice voice) {
      return "dragon." + kind.element() + "." + voice.id;
   }

   public static synchronized SoundEvent get(String name) {
      SoundEvent event = EVENTS.get(name);
      if (event == null) {
         event = SoundEvent.createVariableRangeEvent(Skywyrms.id(name));
         EVENTS.put(name, event);
      }

      return event;
   }

   public static SoundEvent voice(DragonKind kind, ModSounds.Voice voice) {
      return get(voiceName(kind, voice));
   }

   public static void register(BiConsumer<Identifier, SoundEvent> registrar) {
      for (String name : names()) {
         registrar.accept(Skywyrms.id(name), get(name));
      }
   }

   public enum Voice {
      AMBIENT("ambient"),
      HURT("hurt"),
      DEATH("death"),
      ROAR("roar"),
      BABY("baby");

      public final String id;

      Voice(String id) {
         this.id = id;
      }
   }
}
