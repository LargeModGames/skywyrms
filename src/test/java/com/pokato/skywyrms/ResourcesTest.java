package com.pokato.skywyrms;

import static com.pokato.skywyrms.TestResources.ASSETS;
import static com.pokato.skywyrms.TestResources.ROOT;
import static com.pokato.skywyrms.TestResources.asset;
import static com.pokato.skywyrms.TestResources.files;
import static com.pokato.skywyrms.TestResources.image;
import static com.pokato.skywyrms.TestResources.readObject;
import static com.pokato.skywyrms.TestResources.readStrictJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.entity.GeoData;
import com.pokato.skywyrms.registry.ModEntities;
import com.pokato.skywyrms.registry.ModParticles;
import com.pokato.skywyrms.registry.ModSounds;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ResourcesTest {
	@Test
	void everyJsonFileIsStrictJson() {
		List<Path> json = files(ROOT, ".json");
		assertTrue(!json.isEmpty(), "no JSON files found under " + ROOT);
		json.forEach(TestResources::readStrictJson);
	}

	@Test
	void fabricModJson() throws ClassNotFoundException {
		JsonObject mod = readObject(ROOT.resolve("fabric.mod.json"));
		assertEquals("skywyrms", mod.get("id").getAsString());
		assertEquals("${version}", mod.get("version").getAsString(), "version must stay templated from mod_version");
		assertEquals("MIT", mod.get("license").getAsString());
		assertTrue(Files.isRegularFile(ROOT.resolve(mod.get("icon").getAsString())), "icon file");

		for (var entry : mod.getAsJsonObject("entrypoints").entrySet()) {
			for (JsonElement name : entry.getValue().getAsJsonArray()) {
				Class.forName(name.getAsString(), false, getClass().getClassLoader());
			}
		}
	}

	@Test
	void licenseIsPokatosMit() throws IOException {
		String license = Files.readString(Path.of("LICENSE"));
		assertTrue(license.startsWith("MIT License"), "LICENSE must stay the MIT text");
		assertTrue(license.contains("Copyright (c) 2026 pokato"), "LICENSE must keep pokato's copyright");
	}

	@Test
	void dragonTexturesExistAndMatchModel() {
		for (DragonKind body : DragonKind.values()) {
			GeoData geo = body.geo();
			for (DragonKind blood : DragonKind.values()) {
				String base = body == blood ? body.id() : "hybrid/" + body.element() + "_" + blood.element();
				for (String suffix : List.of(".png", "_glow.png")) {
					BufferedImage texture = image(ASSETS.resolve("textures/entity/" + base + suffix));
					int factor = texture.getWidth() / geo.textureWidth;
					assertTrue(factor >= 1 && texture.getWidth() == geo.textureWidth * factor && texture.getHeight() == geo.textureHeight * factor,
						base + suffix + " is " + texture.getWidth() + "x" + texture.getHeight() + ", not a whole multiple of " + body.id() + " texture_size");
				}
			}
		}
	}

	@Test
	void eggTexturesExist() {
		for (DragonKind body : DragonKind.values()) {
			for (DragonKind blood : DragonKind.values()) {
				image(ASSETS.resolve("textures/entity/egg/" + body.element() + "_" + blood.element() + ".png"));
			}

			for (int stage = 0; stage < 3; stage++) {
				image(ASSETS.resolve("textures/entity/egg/glow_" + body.element() + "_" + stage + ".png"));
			}
		}
	}

	@Test
	void itemsHaveDefinitionsModelsAndTextures() {
		for (DragonKind kind : DragonKind.values()) {
			for (String item : List.of(kind.id() + "_egg", kind.id() + "_spawn_egg")) {
				JsonObject definition = readObject(asset("items", item, ".json"));
				String model = definition.getAsJsonObject("model").get("model").getAsString();
				JsonObject itemModel = readObject(asset("models", model, ".json"));

				if (itemModel.has("textures")) {
					for (var texture : itemModel.getAsJsonObject("textures").entrySet()) {
						String id = texture.getValue().getAsString();
						if (id.startsWith("skywyrms:")) {
							image(asset("textures", id, ".png"));
						}
					}
				}
			}
		}
	}

	@Test
	void particlesHaveDefinitionsAndTextures() {
		for (ModParticles.Style style : ModParticles.Style.values()) {
			JsonObject particle = readObject(ASSETS.resolve("particles/" + style.id + ".json"));
			for (JsonElement texture : particle.getAsJsonArray("textures")) {
				image(asset("textures/particle", texture.getAsString(), ".png"));
			}
		}
	}

	@Test
	void soundsAreDefinedAndFilesExist() {
		JsonObject sounds = readObject(ASSETS.resolve("sounds.json"));
		Set<String> lang = langKeys();

		for (String name : ModSounds.names()) {
			assertTrue(sounds.has(name), "sounds.json lacks registered sound " + name);
		}

		for (var event : sounds.entrySet()) {
			JsonObject definition = event.getValue().getAsJsonObject();
			if (definition.has("subtitle")) {
				assertTrue(lang.contains(definition.get("subtitle").getAsString()), "missing subtitle " + definition.get("subtitle"));
			}

			for (JsonElement sound : definition.getAsJsonArray("sounds")) {
				String file = sound.isJsonObject() ? sound.getAsJsonObject().get("name").getAsString() : sound.getAsString();
				boolean isEvent = sound.isJsonObject() && sound.getAsJsonObject().has("type")
					&& sound.getAsJsonObject().get("type").getAsString().equals("event");
				if (!isEvent && (file.startsWith("skywyrms:") || !file.contains(":"))) {
					Path ogg = asset("sounds", file, ".ogg");
					assertTrue(Files.isRegularFile(ogg), event.getKey() + " points to missing " + ogg);
				}
			}
		}
	}

	@Test
	void fontBitmapsExist() {
		JsonObject font = readObject(ASSETS.resolve("font/hud.json"));
		for (JsonElement provider : font.getAsJsonArray("providers")) {
			JsonObject p = provider.getAsJsonObject();
			if (p.has("file")) {
				image(asset("textures", p.get("file").getAsString(), ""));
			}
		}
	}

	@Test
	void lootTablesExist() {
		for (DragonKind kind : DragonKind.values()) {
			readObject(ROOT.resolve("data/skywyrms/loot_table/entities/" + kind.id() + ".json"));
		}
	}

	@Test
	void translationsCoverGeneratedKeys() {
		Set<String> lang = langKeys();
		List<String> needed = new ArrayList<>(List.of(
			"entity.skywyrms." + ModEntities.EGG_ID, "entity.skywyrms.hybrid", "itemGroup.skywyrms.main"
		));

		for (DragonKind kind : DragonKind.values()) {
			needed.addAll(List.of(
				"entity.skywyrms." + kind.id(),
				"item.skywyrms." + kind.id() + "_egg",
				"item.skywyrms." + kind.id() + "_spawn_egg",
				"ability.skywyrms." + kind.ability().id(),
				"element.skywyrms." + kind.element(),
				"element.skywyrms." + kind.element() + ".adjective",
				"egg.skywyrms.hint." + kind.element(),
				"infusion.skywyrms." + kind.element(),
				"trait.skywyrms." + kind.element()
			));
		}

		assertEquals(List.of(), needed.stream().filter(k -> !lang.contains(k)).toList(), "missing translations");
	}

	/** Full keys written as string literals in the code, like "message.skywyrms.hatched". */
	@Test
	void translationsCoverLiteralKeys() throws IOException {
		Set<String> lang = langKeys();
		Pattern literal = Pattern.compile("\"([a-zA-Z]+\\.skywyrms\\.[a-z0-9_.]*[a-z0-9_])\"");
		Set<String> missing = new TreeSet<>();

		for (Path source : files(Path.of("src/main/java"), ".java")) {
			Matcher m = literal.matcher(Files.readString(source));
			while (m.find()) {
				if (!lang.contains(m.group(1))) {
					missing.add(m.group(1) + " (" + source.getFileName() + ")");
				}
			}
		}

		assertEquals(Set.of(), missing, "keys used in code but missing from en_us.json");
	}

	private static Set<String> langKeys() {
		return readStrictJson(ASSETS.resolve("lang/en_us.json")).getAsJsonObject().keySet();
	}
}
