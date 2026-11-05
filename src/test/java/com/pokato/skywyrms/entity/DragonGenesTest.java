package com.pokato.skywyrms.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

class DragonGenesTest {
	@Test
	void tagRoundTripKeepsEveryField() {
		for (DragonKind body : DragonKind.values()) {
			for (DragonKind blood : DragonKind.values()) {
				DragonGenes genes = new DragonGenes(body, blood, 0.85F, 1.2F, 1.05F, 3, true);
				assertEquals(genes, DragonGenes.fromTag(genes.toTag(), DragonKind.EMBER), body + "/" + blood);
			}
		}
	}

	@Test
	void pureGenesArePurebred() {
		for (DragonKind kind : DragonKind.values()) {
			DragonGenes genes = DragonGenes.pure(kind);
			assertTrue(genes.purebred() && genes.has(kind), kind.toString());
		}
	}

	@Test
	void crossingStaysInTraitRangeAndInheritsElements() {
		RandomSource random = RandomSource.create(1234L);

		for (DragonKind a : DragonKind.values()) {
			for (DragonKind b : DragonKind.values()) {
				DragonGenes pa = DragonGenes.pure(a);
				DragonGenes pb = DragonGenes.pure(b);
				for (int i = 0; i < 20; i++) {
					DragonGenes child = DragonGenes.cross(pa, pb, random);
					assertTrue(child.has(a) && child.has(b), "child of " + a + "/" + b + " lacks a parent element");
					assertTrue(child.body() == a || child.body() == b, "body from a parent");
					assertTrue(child.blood() == a || child.blood() == b, "blood from a parent");
					assertEquals(1, child.generation());
					assertEquals(a != b, child.vigor(), "vigor only for two different purebreds");
					for (float trait : new float[]{child.health(), child.speed(), child.power()}) {
						assertTrue(trait >= DragonGenes.MIN_TRAIT && trait <= DragonGenes.MAX_TRAIT, "trait " + trait);
					}
				}
			}
		}
	}
}
