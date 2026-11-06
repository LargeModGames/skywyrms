package com.pokato.skywyrms.gametest;

import com.pokato.skywyrms.ability.DragonAbilities;
import com.pokato.skywyrms.config.ModConfig;
import com.pokato.skywyrms.entity.DragonEggEntity;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonGenes;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.entity.GeoData;
import com.pokato.skywyrms.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class DragonGameTests {
	private static final Vec3 CENTER = new Vec3(4.5, 2, 4.5);

	private static DragonEntity spawn(GameTestHelper helper, DragonKind kind) {
		DragonEntity dragon = helper.spawn(ModEntities.type(kind), CENTER);
		dragon.setNoAi(true);
		return dragon;
	}

	@GameTest(maxTicks = 100)
	public void everyDragonSpawnsAndTicks(GameTestHelper helper) {
		List<DragonEntity> dragons = new ArrayList<>();

		for (DragonKind kind : DragonKind.values()) {
			DragonEntity dragon = helper.spawn(ModEntities.type(kind), CENTER);
			GeoData geo = kind.geo();
			helper.assertValueEqual(dragon.getKind(), kind, "kind of " + kind.id());
			helper.assertValueEqual(dragon.getBbWidth(), geo.hitboxWidth(), "hitbox width of " + kind.id());
			helper.assertValueEqual(dragon.getBbHeight(), geo.hitboxHeight(), "hitbox height of " + kind.id());
			dragons.add(dragon);
		}

		helper.runAfterDelay(40, () -> {
			for (DragonEntity dragon : dragons) {
				helper.assertTrue(dragon.isAlive(), dragon.getKind().id() + " died while ticking");
			}

			helper.succeed();
		});
	}

	@GameTest
	public void everyHybridKeepsItsGenes(GameTestHelper helper) {
		for (DragonKind body : DragonKind.values()) {
			for (DragonKind blood : DragonKind.values()) {
				DragonEntity dragon = spawn(helper, body);
				dragon.setGenes(new DragonGenes(body, blood, 1.0F, 1.0F, 1.0F, 1, false), true);
				helper.assertValueEqual(dragon.blood(), blood, body.id() + " blood");
				helper.assertValueEqual(dragon.isPurebred(), body == blood, body.id() + "/" + blood.id() + " purebred");
				helper.assertTrue(dragon.hasElement(body) && dragon.hasElement(blood), body.id() + "/" + blood.id() + " elements");
				dragon.discard();
			}
		}

		helper.succeed();
	}

	@GameTest
	public void eggHatchesIntoTamedBaby(GameTestHelper helper) {
		UUID owner = UUID.randomUUID();

		for (DragonKind kind : DragonKind.values()) {
			DragonEggEntity egg = DragonEggEntity.create(helper.getLevel(), DragonGenes.pure(kind), owner, 0);
			helper.assertTrue(egg != null, "egg for " + kind.id());
			egg.snapTo(helper.absoluteVec(CENTER));
			helper.getLevel().addFreshEntity(egg);
			egg.hatch(helper.getLevel());
			helper.assertTrue(egg.isRemoved(), kind.id() + " egg not removed after hatching");
		}

		for (DragonKind kind : DragonKind.values()) {
			helper.assertEntityData(BlockPos.containing(CENTER), ModEntities.type(kind),
				dragon -> dragon.isBaby() && dragon.isTame() && owner.equals(DragonEntity.ownerUuid(dragon)));
		}

		helper.succeed();
	}

	@GameTest(maxTicks = 200)
	public void eggHatchesByIncubating(GameTestHelper helper) {
		DragonEggEntity egg = DragonEggEntity.create(helper.getLevel(), DragonGenes.pure(DragonKind.TIDE), null,
			ModConfig.get().eggHatchTicks() - 20);
		egg.snapTo(helper.absoluteVec(CENTER));
		helper.getLevel().addFreshEntity(egg);
		helper.succeedWhen(() -> {
			helper.assertEntityPresent(ModEntities.type(DragonKind.TIDE));
			helper.assertTrue(egg.isRemoved(), "egg still present");
		});
	}

	@GameTest(maxTicks = 100)
	public void breathAbilitiesRunWithoutErrors(GameTestHelper helper) {
		List<DragonEntity> breathers = new ArrayList<>();

		for (DragonKind kind : DragonKind.values()) {
			if (kind.ability().held()) {
				DragonEntity dragon = spawn(helper, kind);
				helper.assertTrue(dragon.aiBreath(60), kind.id() + " could not start breathing");
				breathers.add(dragon);
			}
		}

		helper.assertFalse(breathers.isEmpty(), "no breath dragons");
		helper.runAfterDelay(40, () -> {
			for (DragonEntity dragon : breathers) {
				helper.assertTrue(dragon.isAlive(), dragon.getKind().id() + " died while breathing");
				helper.assertTrue(dragon.isBreathing(), dragon.getKind().id() + " stopped breathing early");
				helper.assertTrue(dragon.energy() < 70.0F, dragon.getKind().id() + " breath never ticked, energy " + dragon.energy());
				dragon.stopBreath();
				helper.assertFalse(dragon.isBreathing(), dragon.getKind().id() + " still breathing after stop");
			}

			helper.succeed();
		});
	}

	@GameTest
	public void sunBeamHitsPointBlank(GameTestHelper helper) {
		DragonEntity dragon = spawn(helper, DragonKind.SOLAR);
		LivingEntity pig = helper.spawn(EntityTypes.PIG, helper.relativeVec(dragon.mouthPosition()).subtract(0, 0.45, 0));
		dragon.setTarget(pig);
		DragonAbilities.breathTick(dragon, helper.getLevel(), 3);
		helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "pig at the mouth was not hit");
		helper.succeed();
	}

	@GameTest
	public void sunBeamStopsAtWalls(GameTestHelper helper) {
		DragonEntity dragon = spawn(helper, DragonKind.SOLAR);
		dragon.setYRot(0.0F);
		dragon.setYBodyRot(0.0F);
		dragon.setYHeadRot(0.0F);
		Vec3 mouth = dragon.mouthPosition();
		BlockPos wall = BlockPos.containing(mouth.add(0, 0, 3));
		helper.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
		LivingEntity pig = helper.spawn(EntityTypes.PIG, helper.relativeVec(mouth.add(0, -0.45, 4.5)));
		dragon.setTarget(pig);
		DragonAbilities.breathTick(dragon, helper.getLevel(), 3);
		helper.getLevel().removeBlock(wall, false);
		helper.assertTrue(pig.getHealth() == pig.getMaxHealth(), "pig behind the wall was hit");
		helper.succeed();
	}

	/** Void blink jumps toward its target, so the target floats above the test area where the path is clear. */
	@GameTest(skyAccess = true)
	public void castAbilitiesSpendEnergyAndCoolDown(GameTestHelper helper) {
		int cast = 0;

		for (DragonKind kind : DragonKind.values()) {
			if (kind.ability().held()) {
				continue;
			}

			DragonEntity dragon = spawn(helper, kind);
			dragon.setTarget(helper.spawn(EntityTypes.PIG, CENTER.add(0, 16, 0)));
			dragon.setEnergy(100.0F);
			helper.assertTrue(dragon.castAbility(), kind.id() + " could not cast " + kind.ability().id());
			helper.assertTrue(dragon.energy() < 100.0F, kind.id() + " cast " + kind.ability().id() + " for free");
			helper.assertValueEqual(dragon.energy(), 100.0F - DragonAbilities.cost(kind.ability()), kind.id() + " energy after cast");
			helper.assertFalse(dragon.castAbility(), kind.id() + " cast again during cooldown");
			cast++;
		}

		helper.assertTrue(cast > 0, "no cast dragons");
		helper.succeed();
	}

	@GameTest
	public void mouthIsNearTheDragon(GameTestHelper helper) {
		for (DragonKind kind : DragonKind.values()) {
			DragonEntity dragon = spawn(helper, kind);
			double reach = (dragon.getBbWidth() + dragon.getBbHeight()) * 1.5;
			double distance = dragon.mouthPosition().distanceTo(dragon.position());
			helper.assertTrue(distance > 0.1 && distance < reach, kind.id() + " mouth is " + distance + " blocks away, limit " + reach);
		}

		helper.succeed();
	}

	@GameTest
	public void ownerCanRideSaddledDragon(GameTestHelper helper) {
		for (DragonKind kind : DragonKind.values()) {
			Player player = helper.makeMockPlayer(GameType.SURVIVAL);
			DragonEntity dragon = spawn(helper, kind);
			dragon.tameTo(player.getUUID());
			dragon.setSaddled(true);
			helper.assertTrue(player.startRiding(dragon), player.getName().getString() + " could not mount " + kind.id());
			helper.assertValueEqual(dragon.getControllingPassenger(), player, kind.id() + " controlling passenger");
			helper.assertTrue(dragon.getPassengerRidingPosition(player).y > dragon.getY(), kind.id() + " seat is below the dragon");
			dragon.ejectPassengers();
			dragon.discard();
		}

		helper.succeed();
	}
}
