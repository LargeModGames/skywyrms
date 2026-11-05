package com.pokato.skywyrms.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pokato.skywyrms.Skywyrms;
import com.pokato.skywyrms.platform.Platform;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
   public boolean naturalSpawning = true;
   public int spawnIntervalTicks = 400;
   public float spawnChance = 0.12F;
   public int spawnSpacing = 96;
   public boolean breathAffectsBlocks = true;
   public float eggHatchMinutes = 15.0F;
   public float babyGrowMinutes = 40.0F;
   public float wildHybridChance = 0.08F;
   private static ModConfig instance;

   public int eggHatchTicks() {
      return Math.max(20, Math.round(this.eggHatchMinutes * 1200.0F));
   }

   public int babyGrowTicks() {
      return Math.max(20, Math.round(this.babyGrowMinutes * 1200.0F));
   }

   private ModConfig() {
   }

   public static synchronized ModConfig get() {
      if (instance == null) {
         instance = load();
      }

      return instance;
   }

   private static ModConfig load() {
      ModConfig config = new ModConfig();

      Path file;
      try {
         file = Platform.get().configDir().resolve("skywyrms.json");
      } catch (RuntimeException e) {
         return config;
      }

      try {
         if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
               JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
               if (json.has("naturalSpawning")) {
                  config.naturalSpawning = json.get("naturalSpawning").getAsBoolean();
               }

               if (json.has("spawnIntervalTicks")) {
                  config.spawnIntervalTicks = Math.max(20, json.get("spawnIntervalTicks").getAsInt());
               }

               if (json.has("spawnChance")) {
                  config.spawnChance = Math.max(0.0F, Math.min(1.0F, json.get("spawnChance").getAsFloat()));
               }

               if (json.has("spawnSpacing")) {
                  config.spawnSpacing = Math.max(16, json.get("spawnSpacing").getAsInt());
               }

               if (json.has("breathAffectsBlocks")) {
                  config.breathAffectsBlocks = json.get("breathAffectsBlocks").getAsBoolean();
               }

               if (json.has("eggHatchMinutes")) {
                  config.eggHatchMinutes = Math.max(0.1F, Math.min(1000.0F, json.get("eggHatchMinutes").getAsFloat()));
               }

               if (json.has("babyGrowMinutes")) {
                  config.babyGrowMinutes = Math.max(0.1F, Math.min(1000.0F, json.get("babyGrowMinutes").getAsFloat()));
               }

               if (json.has("wildHybridChance")) {
                  config.wildHybridChance = Math.max(0.0F, Math.min(1.0F, json.get("wildHybridChance").getAsFloat()));
               }
            }
         }

         JsonObject out = new JsonObject();
         out.addProperty("naturalSpawning", config.naturalSpawning);
         out.addProperty("spawnIntervalTicks", config.spawnIntervalTicks);
         out.addProperty("spawnChance", config.spawnChance);
         out.addProperty("spawnSpacing", config.spawnSpacing);
         out.addProperty("breathAffectsBlocks", config.breathAffectsBlocks);
         out.addProperty("eggHatchMinutes", config.eggHatchMinutes);
         out.addProperty("babyGrowMinutes", config.babyGrowMinutes);
         out.addProperty("wildHybridChance", config.wildHybridChance);
         Files.createDirectories(file.getParent());

         try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(out, writer);
         }
      } catch (Exception e) {
         Skywyrms.LOGGER.warn("Could not read or write {}: {}", file, e.toString());
      }

      return config;
   }
}
