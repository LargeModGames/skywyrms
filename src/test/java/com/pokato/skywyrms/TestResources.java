package com.pokato.skywyrms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Strictness;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/** Reads files from src/main/resources, which is where Gradle runs tests from. */
final class TestResources {
	static final Path ROOT = Path.of("src/main/resources");
	static final Path ASSETS = ROOT.resolve("assets/skywyrms");

	private TestResources() {
	}

	static JsonElement readStrictJson(Path file) {
		try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			JsonReader reader = new JsonReader(in);
			reader.setStrictness(Strictness.STRICT);
			JsonElement element = TypeAdapters.JSON_ELEMENT.read(reader);
			assertEquals(JsonToken.END_DOCUMENT, reader.peek(), file + ": trailing content");
			return element;
		} catch (IOException | RuntimeException e) {
			return fail(file + ": " + e.getMessage(), e);
		}
	}

	static JsonObject readObject(Path file) {
		JsonElement element = readStrictJson(file);
		assertTrue(element.isJsonObject(), file + ": not a JSON object");
		return element.getAsJsonObject();
	}

	static List<Path> files(Path dir, String extension) {
		try (Stream<Path> walk = Files.walk(dir)) {
			return walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(extension)).sorted().toList();
		} catch (IOException e) {
			return fail(dir + ": " + e.getMessage(), e);
		}
	}

	/** Resolves "skywyrms:path" or "path" to a file under assets/skywyrms/{folder}/path{suffix}. */
	static Path asset(String folder, String id, String suffix) {
		String path = id.startsWith("skywyrms:") ? id.substring("skywyrms:".length()) : id;
		return ASSETS.resolve(folder).resolve(path + suffix);
	}

	static BufferedImage image(Path file) {
		assertTrue(Files.isRegularFile(file), "missing " + file);
		try {
			BufferedImage image = ImageIO.read(file.toFile());
			assertTrue(image != null, file + ": not a readable PNG");
			return image;
		} catch (IOException e) {
			return fail(file + ": " + e.getMessage(), e);
		}
	}
}
