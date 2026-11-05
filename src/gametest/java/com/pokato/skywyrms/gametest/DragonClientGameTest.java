package com.pokato.skywyrms.gametest;

import com.pokato.skywyrms.client.ModKeys;
import com.pokato.skywyrms.entity.DragonEggEntity;
import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonGenes;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.registry.ModEntities;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

/** Renders every dragon, a hybrid and an egg, rides a dragon, and uses an ability over a real network connection. */
@SuppressWarnings("UnstableApiUsage")
public class DragonClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			TestServerConnection connection = world.getConnection();
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("weather clear");
			connection.waitForChunksRender();

			for (DragonKind kind : DragonKind.values()) {
				showAndCapture(context, world.getServer(), connection, kind, kind, "dragon_" + kind.id());
			}

			showAndCapture(context, world.getServer(), connection, DragonKind.EMBER, DragonKind.VOID, "hybrid_ember_void");
			showEgg(context, world.getServer(), connection);
			rideAndCapture(context, world.getServer(), connection);
		}

		abilityOverNetwork(context);
	}

	private static Vec3 inFront(ServerPlayer player, double distance) {
		Vec3 look = player.getLookAngle();
		return player.position().add(look.x * distance, 0, look.z * distance);
	}

	private static void showAndCapture(ClientGameTestContext context, TestServerContext server, TestServerConnection connection,
			DragonKind body, DragonKind blood, String name) {
		int id = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			DragonEntity dragon = ModEntities.type(body).create(player.level(), EntitySpawnReason.COMMAND);
			dragon.setGenes(new DragonGenes(body, blood, 1.0F, 1.0F, 1.0F, 0, false), true);
			dragon.snapTo(inFront(player, 10).add(0, 1, 0), player.getYRot() + 180.0F, 0.0F);
			dragon.setNoAi(true);
			player.level().addFreshEntity(dragon);
			return dragon.getId();
		});
		waitForClientEntity(context, id);
		context.waitTicks(10);
		context.takeScreenshot(name);
		server.runOnServer(s -> connection.getServerPlayer().level().getEntity(id).discard());
	}

	private static void showEgg(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		int id = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			DragonEggEntity egg = DragonEggEntity.create(player.level(), DragonGenes.pure(DragonKind.FROST), player.getUUID(), 0);
			egg.snapTo(inFront(player, 3).add(0, 1, 0));
			player.level().addFreshEntity(egg);
			return egg.getId();
		});
		waitForClientEntity(context, id);
		context.waitTicks(10);
		context.takeScreenshot("egg_frost");
		server.runOnServer(s -> connection.getServerPlayer().level().getEntity(id).discard());
	}

	private static void rideAndCapture(ClientGameTestContext context, TestServerContext server, TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			DragonEntity dragon = saddledFor(player, DragonKind.FROST);
			player.level().addFreshEntity(dragon);
			player.startRiding(dragon);
		});
		context.waitFor(mc -> mc.player != null && mc.player.getVehicle() instanceof DragonEntity);
		context.waitTicks(20);
		context.takeScreenshot("riding_frost");
		server.runOnServer(s -> connection.getServerPlayer().stopRiding());
	}

	/** Holding the ability key on a client connected to a dedicated server must make the server cast the ability. */
	private static void abilityOverNetwork(ClientGameTestContext context) {
		try (TestDedicatedServerContext server = context.worldBuilder().createServer();
				var connection = server.connect()) {
			connection.waitForChunksRender();
			int id = server.computeOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				DragonEntity dragon = saddledFor(player, DragonKind.STORM);
				player.level().addFreshEntity(dragon);
				player.startRiding(dragon);
				return dragon.getId();
			});
			context.waitFor(mc -> mc.player != null && mc.player.getVehicle() instanceof DragonEntity);
			context.getInput().holdKey(ModKeys.ABILITY);
			context.waitTicks(5);
			context.getInput().releaseKey(ModKeys.ABILITY);
			connection.waitForServerboundPackets();
			server.waitFor(s -> ((DragonEntity) connection.getServerPlayer().level().getEntity(id)).energy() < 100.0F, 100);
			context.takeScreenshot("ability_storm_dedicated");
		}
	}

	private static DragonEntity saddledFor(ServerPlayer player, DragonKind kind) {
		DragonEntity dragon = ModEntities.type(kind).create(player.level(), EntitySpawnReason.COMMAND);
		dragon.snapTo(player.position(), player.getYRot(), 0.0F);
		dragon.tameTo(player.getUUID());
		dragon.setSaddled(true);
		return dragon;
	}

	private static void waitForClientEntity(ClientGameTestContext context, int id) {
		context.waitFor(mc -> mc.level != null && mc.level.getEntity(id) != null, 200);
	}
}
