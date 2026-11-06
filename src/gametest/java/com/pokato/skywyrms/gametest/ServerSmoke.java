package com.pokato.skywyrms.gametest;

import com.pokato.skywyrms.entity.DragonEntity;
import com.pokato.skywyrms.entity.DragonKind;
import com.pokato.skywyrms.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * With -Dskywyrms.smoke=true, force-loads the area around spawn on a real dedicated server, spawns every dragon,
 * lets them tick for five seconds, logs the verdict and stops the server. CI checks the log for the verdict line.
 * Without players nothing ticks on its own, so the area is forced and the test waits until it ticks entities.
 */
public class ServerSmoke implements ModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("SkywyrmsSmoke");
	private static final int RUN_TICKS = 100;
	private static final int MAX_WAIT_TICKS = 400;
	private static final int CHUNK_RADIUS = 5;
	private static final int SPACING = 8;
	private final List<DragonEntity> dragons = new ArrayList<>();
	private BlockPos spawn;
	private int waited;
	private int spawnTick = -1;

	@Override
	public void onInitialize() {
		if (Boolean.getBoolean("skywyrms.smoke")) {
			ServerTickEvents.END_SERVER_TICK.register(this::tick);
		}
	}

	private void tick(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (spawn == null) {
			spawn = level.getRespawnData().pos();
			for (int x = -CHUNK_RADIUS; x <= CHUNK_RADIUS; x++) {
				for (int z = -CHUNK_RADIUS; z <= CHUNK_RADIUS; z++) {
					level.setChunkForced((spawn.getX() >> 4) + x, (spawn.getZ() >> 4) + z, true);
				}
			}
		} else if (spawnTick < 0) {
			if (allTicking(level)) {
				spawnAll(level);
				spawnTick = server.getTickCount();
			} else if (++waited > MAX_WAIT_TICKS) {
				finish(server, "SKYWYRMS SMOKE FAILED: the area around spawn never started ticking entities");
			}
		} else if (server.getTickCount() - spawnTick >= RUN_TICKS) {
			List<String> problems = dragons.stream()
				.filter(d -> d.isRemoved() || d.isDeadOrDying() || d.tickCount < RUN_TICKS)
				.map(d -> d.getKind().id() + " (removed " + d.getRemovalReason() + ", health " + d.getHealth() + ", ticked " + d.tickCount + ")")
				.toList();
			if (dragons.size() == DragonKind.values().length && problems.isEmpty()) {
				finish(server, "SKYWYRMS SMOKE PASSED: " + dragons.size() + " dragons ticked for " + RUN_TICKS + " ticks");
			} else {
				finish(server, "SKYWYRMS SMOKE FAILED: spawned " + dragons.size() + ", problems " + problems);
			}
		}
	}

	private boolean allTicking(ServerLevel level) {
		for (int i = 0; i < DragonKind.values().length; i++) {
			if (!level.isPositionEntityTicking(spawn.offset(i * SPACING, 0, 0))) {
				return false;
			}
		}

		return true;
	}

	private void spawnAll(ServerLevel level) {
		DragonKind[] kinds = DragonKind.values();

		for (int i = 0; i < kinds.length; i++) {
			BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, spawn.offset(i * SPACING, 0, 0));
			DragonEntity dragon = ModEntities.type(kinds[i]).spawn(level, pos, EntitySpawnReason.COMMAND);
			if (dragon != null) {
				dragons.add(dragon);
			}
		}
	}

	private static void finish(MinecraftServer server, String verdict) {
		if (verdict.contains("PASSED")) {
			LOGGER.info(verdict);
		} else {
			LOGGER.error(verdict);
		}

		server.halt(false);
	}
}
