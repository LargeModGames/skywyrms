package com.pokato.skywyrms.entity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class GeoDataTest {
	/** Every bone DragonAnimator looks up by name. A missing one silently stops animating. */
	static final List<String> ANIMATED_BONES = animatedBones();

	private static List<String> animatedBones() {
		List<String> bones = new ArrayList<>(List.of(
			"root", "body", "head", "jaw", "saddle", "marker_ground", "marker_belly",
			"neck1", "neck2", "neck3", "neck4",
			"tail1", "tail2", "tail3", "tail4", "tail5", "tail6"
		));

		for (String leg : List.of("leg_fl", "leg_fr", "leg_bl", "leg_br")) {
			bones.addAll(List.of(leg, leg + "_shin", leg + "_foot"));
		}

		for (String wing : List.of("wing_l", "wing_r")) {
			for (String suffix : List.of("", "_fore", "_hand", "_mem", "_fore_mem", "_hand_mem", "_web")) {
				bones.add(wing + suffix);
			}
		}

		return bones;
	}

	@Test
	void everyKindLoads() {
		assertAll(kinds(kind -> assertTrue(kind.geo().parts.size() > 0, kind + " has no parts")));
	}

	@Test
	void animatedBonesExist() {
		assertAll(kinds(kind -> {
			List<String> missing = ANIMATED_BONES.stream().filter(b -> !kind.geo().byName.containsKey(b)).toList();
			assertEquals(List.of(), missing, kind + " lacks bones the animator moves");
		}));
	}

	@Test
	void partNamesAreUnique() {
		assertAll(kinds(kind -> {
			Set<String> seen = new HashSet<>();
			List<String> duplicates = kind.geo().parts.stream().map(p -> p.name).filter(n -> !seen.add(n)).toList();
			assertEquals(List.of(), duplicates, kind + " has duplicate part names");
		}));
	}

	@Test
	void mouthBoneExists() {
		assertAll(kinds(kind -> assertTrue(kind.geo().byName.containsKey(kind.geo().mouthBone), kind + " mouth_bone " + kind.geo().mouthBone)));
	}

	@Test
	void dimensionsArePositive() {
		assertAll(kinds(kind -> {
			GeoData geo = kind.geo();
			assertTrue(geo.scale > 0, kind + " scale");
			assertTrue(geo.bboxWidth > 0 && geo.bboxHeight > 0, kind + " bbox");
			assertTrue(geo.textureWidth > 0 && geo.textureHeight > 0, kind + " texture_size");

			for (GeoData.Part part : geo.parts) {
				for (GeoData.Cube cube : part.cubes) {
					assertTrue(cube.w >= 0 && cube.h >= 0 && cube.d >= 0, kind + " " + part.name + " has a negative cube size");
				}
			}
		}));
	}

	/**
	 * A box of size w,h,d at u,v uses 2(d+w) by d+h pixels. A flat sheet (one size 0, no inflate) is drawn by
	 * DragonModel.addSheet with only its two large faces, which use less.
	 */
	@Test
	void uvsFitTexture() {
		assertAll(kinds(kind -> {
			GeoData geo = kind.geo();
			List<String> outside = new ArrayList<>();

			for (GeoData.Part part : geo.parts) {
				for (GeoData.Cube c : part.cubes) {
					boolean sheet = c.inflate == 0 && (c.w == 0 || c.h == 0 || c.d == 0);
					float right;
					float bottom;
					if (!sheet) {
						right = c.u + 2 * (c.d + c.w);
						bottom = c.v + c.d + c.h;
					} else if (c.h == 0) {
						right = c.u + c.d + 2 * c.w;
						bottom = c.v + c.d;
					} else if (c.w == 0) {
						right = c.u + 2 * c.d;
						bottom = c.v + c.d + c.h;
					} else {
						right = c.u + 2 * c.w;
						bottom = c.v + c.h;
					}
					if (c.u < 0 || c.v < 0 || right > geo.textureWidth || bottom > geo.textureHeight) {
						outside.add(part.name + " uv " + c.u + "," + c.v + " size " + c.w + "x" + c.h + "x" + c.d);
					}
				}
			}

			assertEquals(List.of(), outside, kind + " cubes reach outside the " + geo.textureWidth + "x" + geo.textureHeight + " texture");
		}));
	}

	@Test
	void hitboxAndSeatMatchRegistration() {
		assertAll(kinds(kind -> {
			GeoData geo = kind.geo();
			assertEquals(geo.bboxWidth * geo.scale, geo.hitboxWidth(), 1e-6, kind + " hitbox width");
			assertEquals(geo.bboxHeight * geo.scale, geo.hitboxHeight(), 1e-6, kind + " hitbox height");
			assertTrue(geo.seatHeight() > 0 && geo.seatHeight() < geo.hitboxHeight() * 2, kind + " seat height " + geo.seatHeight());
		}));
	}

	static Executable[] kinds(KindCheck check) {
		DragonKind[] kinds = DragonKind.values();
		Executable[] out = new Executable[kinds.length];

		for (int i = 0; i < kinds.length; i++) {
			DragonKind kind = kinds[i];
			out[i] = () -> check.run(kind);
		}

		return out;
	}

	interface KindCheck {
		void run(DragonKind kind) throws Throwable;
	}
}
