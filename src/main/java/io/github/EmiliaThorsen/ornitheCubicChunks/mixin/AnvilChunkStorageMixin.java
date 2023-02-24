package io.github.EmiliaThorsen.ornitheCubicChunks.mixin;

import io.github.EmiliaThorsen.ornitheCubicChunks.ornitheCubicChunksMod;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entities;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.resource.Identifier;
import net.minecraft.server.world.ScheduledTick;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleStorage;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;
import net.minecraft.world.chunk.storage.AnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;

@Mixin(AnvilChunkStorage.class)
public class AnvilChunkStorageMixin {
	private static final int[][] ruleSet = new int[][]{
			{10, 12, 5, 17, 5, 13, 3 , 14, 3 , 12, 4, 16, 4, 13, 8 },
			{11, 12, 4, 16, 4, 13, 2 , 15, 2 , 12, 5, 17, 5, 13, 9 },
			{8 , 13, 7, 17, 7, 12, 1 , 15, 1 , 13, 6, 16, 6, 12, 10},
			{9 , 13, 6, 16, 6, 12, 0 , 14, 0 , 13, 7, 17, 7, 12, 11},
			{11, 12, 1, 15, 1, 13, 6 , 16, 6 , 12, 0, 14, 0, 13, 8 },
			{10, 12, 0, 14, 0, 13, 7 , 17, 7 , 12, 1, 15, 1, 13, 9 },
			{9 , 13, 3, 14, 3, 12, 4 , 16, 4 , 13, 2, 15, 2, 12, 10},
			{8 , 13, 2, 15, 2, 12, 5 , 17, 5 , 13, 3, 14, 3, 12, 11},
			{2 , 15, 7, 17, 7, 14, 9 , 13, 9 , 15, 4, 16, 4, 14, 0 },
			{3 , 14, 6, 16, 6, 15, 8 , 13, 8 , 14, 5, 17, 5, 15, 1 },
			{5 , 17, 0, 14, 0, 16, 11, 12, 11, 17, 2, 15, 2, 16, 6 },
			{1 , 15, 4, 16, 4, 14, 10, 12, 10, 15, 7, 17, 7, 14, 3 }};
	private static final int[] directions = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -256, 256, 1, -1, 16, -16};

	/**
	 * @author EmiliaThorsen
	 * @reason kinda self-explanatory considering the mod
	 */
	@Overwrite
	private void writeChunkToNbt(WorldChunk worldChunk, World world, NbtCompound nbtCompound) {
		nbtCompound.putByte("V", (byte)1);
		nbtCompound.putInt("xPos", worldChunk.chunkX);
		nbtCompound.putInt("zPos", worldChunk.chunkZ);
		nbtCompound.putLong("LastUpdate", world.getTime());
		nbtCompound.putIntArray("HeightMap", worldChunk.getHeightMap());
		nbtCompound.putBoolean("TerrainPopulated", worldChunk.isTerrainPopulated());
		nbtCompound.putBoolean("LightPopulated", worldChunk.isLightPopulated());
		nbtCompound.putLong("InhabitedTime", worldChunk.getInhabitedTime());

		WorldChunkSection[] worldChunkSections = worldChunk.getSections();
		NbtList subChunks = new NbtList();
		boolean bl = !world.dimension.isDark();
		for (WorldChunkSection worldChunkSection : worldChunkSections) {
			if (worldChunkSection == null) continue;

			NbtCompound subChunkNbt = new NbtCompound();
			subChunkNbt.putByte("Y", (byte) (worldChunkSection.getOffsetY() >> 4));

			char[] blocks = worldChunkSection.getBlockData();
			int blockArrayPos = 0;

			byte[] output = new byte[4096];
			byte[] output2 = new byte[4096];
			byte[] length = new byte[4096];
			byte[] length2 = new byte[4096];
			int outArrayPos = 0;

			char prevBlock = blocks[0];
			int runLength = 0;

			ArrayDeque<Integer> ruleCallList = new ArrayDeque<>();
			ruleCallList.push(128);
			while(ruleCallList.size() != 0) {
				int currentRuleCall = ruleCallList.poll();
				int rule = currentRuleCall & 31;
				if(rule > 11){
					blockArrayPos += directions[rule];
					continue;
				}
				if (currentRuleCall >> 5 == 0) {
					char block = blocks[blockArrayPos];
					if(prevBlock == block) {
						runLength += 1;
					} else {
						output[outArrayPos] = (byte) ((prevBlock >> 8) & 255);
						output2[outArrayPos] = (byte) (prevBlock & 255);
						length[outArrayPos] = (byte) ((runLength >> 8) & 255);
						length2[outArrayPos] = (byte) (runLength & 255);
						outArrayPos += 1;
						runLength = 1;
						prevBlock = block;
					}
					continue;
				}
				for (int element = 0; element < 15; element++) {
					ruleCallList.push(ruleSet[rule][element] | ((currentRuleCall & 224) - 32));
				}
			}
			subChunkNbt.putByteArray("Blocks", output);
			subChunkNbt.putByteArray("Blocks2", output2);
			subChunkNbt.putByteArray("runLength", length);
			subChunkNbt.putByteArray("runLength2", length2);

			subChunkNbt.putByteArray("BlockLight", worldChunkSection.getBlockLightStorage().getData());
			if (bl) subChunkNbt.putByteArray("SkyLight", worldChunkSection.getSkyLightStorage().getData());

			subChunks.add(subChunkNbt);
		}

		nbtCompound.put("Sections", subChunks);
		nbtCompound.putByteArray("Biomes", worldChunk.getBiomes());
		worldChunk.setContainsEntities(false);
		NbtList nbtList2 = new NbtList();

		NbtCompound nbtCompound2;
		for(int i = 0; i < worldChunk.getEntities().length; ++i) {
			for (Entity entity : worldChunk.getEntities()[i]) {
				nbtCompound2 = new NbtCompound();
				if (entity.writeNbtNoRider(nbtCompound2)) {
					worldChunk.setContainsEntities(true);
					nbtList2.add(nbtCompound2);
				}
			}
		}

		nbtCompound.put("Entities", nbtList2);
		NbtList nbtList3 = new NbtList();

		for (BlockEntity blockEntity : worldChunk.getBlockEntities().values()) {
			nbtCompound2 = new NbtCompound();
			blockEntity.writeNbt(nbtCompound2);
			nbtList3.add(nbtCompound2);
		}

		nbtCompound.put("TileEntities", nbtList3);
		List<ScheduledTick> list = world.getScheduledTicks(worldChunk, false);
		if (list != null) {
			long o = world.getTime();
			NbtList nbtList4 = new NbtList();

			for (ScheduledTick scheduledTick : list) {
				NbtCompound nbtCompound3 = new NbtCompound();
				Identifier identifier = (Identifier) Block.REGISTRY.getKey(scheduledTick.getBlock());
				nbtCompound3.putString("i", identifier == null ? "" : identifier.toString());
				nbtCompound3.putInt("x", scheduledTick.pos.getX());
				nbtCompound3.putInt("y", scheduledTick.pos.getY());
				nbtCompound3.putInt("z", scheduledTick.pos.getZ());
				nbtCompound3.putInt("t", (int) (scheduledTick.time - o));
				nbtCompound3.putInt("p", scheduledTick.priority);
				nbtList4.add(nbtCompound3);
			}

			nbtCompound.put("TileTicks", nbtList4);
		}
	}

	/**
	 * @author EmiliaThorsen
	 * @reason yet again self-explanatory for the mod in question
	 */
	@Overwrite
	private WorldChunk createChunkFromNbt(World world, NbtCompound nbtCompound) {
		int i = nbtCompound.getInt("xPos");
		int j = nbtCompound.getInt("zPos");
		WorldChunk worldChunk = new WorldChunk(world, i, j);
		worldChunk.setHeightmap(nbtCompound.getIntArray("HeightMap"));
		worldChunk.setTerrainPopulated(nbtCompound.getBoolean("TerrainPopulated"));
		worldChunk.setLightPopulated(nbtCompound.getBoolean("LightPopulated"));
		worldChunk.setInhabitedTime(nbtCompound.getLong("InhabitedTime"));
		NbtList nbtList = nbtCompound.getList("Sections", 10);
		int k = 16;
		WorldChunkSection[] worldChunkSections = new WorldChunkSection[k];
		boolean bl = !world.dimension.isDark();
		int m;
		for(int l = 0; l < nbtList.size(); ++l) {
			NbtCompound nbtCompound2 = nbtList.getCompound(l);
			m = nbtCompound2.getByte("Y");
			WorldChunkSection worldChunkSection = new WorldChunkSection(m << 4, bl);

			byte[] blocks = nbtCompound2.getByteArray("Blocks");
			byte[] blocks2 = nbtCompound2.getByteArray("Blocks2");
			byte[] runLength = nbtCompound2.getByteArray("runLength");
			byte[] runLength2 = nbtCompound2.getByteArray("runLength2");
			int blockArrayPos = 0;
			char[] output = new char[4096];
			int outArrayPos = 0;

			ArrayDeque<Integer> ruleCallList = new ArrayDeque<>();
			ruleCallList.push(128);

			int currentRunLength = 0;
			char currentBlock = 0;

			while(ruleCallList.size() != 0) {
				int currentRuleCall = ruleCallList.poll();
				int rule = currentRuleCall & 31;
				if(rule > 11){
					blockArrayPos += directions[rule];
					continue;
				}
				if((currentRuleCall >> 5) == 0) {
					if(currentRunLength == 0) {
						currentBlock = (char) ((blocks[outArrayPos] & 255 << 8) | blocks2[outArrayPos]);
						currentRunLength = (runLength[outArrayPos] & 0x7F << 8) | runLength2[outArrayPos];
						outArrayPos += 1;
					}
					currentRunLength -= 1;
					output[blockArrayPos] = currentBlock;
					continue;
				}
				for (int element = 0; element < 15; element++) {
					ruleCallList.push(ruleSet[rule][element] | ((currentRuleCall & 224) - 32));
				}
			}

			worldChunkSection.setBlockData(output);

			worldChunkSection.setBlockLightStorage(new ChunkNibbleStorage(nbtCompound2.getByteArray("BlockLight")));
			if (bl) {
				worldChunkSection.setSkyLightStorage(new ChunkNibbleStorage(nbtCompound2.getByteArray("SkyLight")));
			}

			worldChunkSection.validateBlockCounters();
			worldChunkSections[m] = worldChunkSection;
		}

		worldChunk.setSections(worldChunkSections);

		if (nbtCompound.isType("Biomes", 7)) {worldChunk.setBiomes(nbtCompound.getByteArray("Biomes"));}

		NbtList nbtList2 = nbtCompound.getList("Entities", 10);
		if (nbtList2 != null) {
			for(int s = 0; s < nbtList2.size(); ++s) {
				NbtCompound nbtCompound3 = nbtList2.getCompound(s);
				Entity entity = Entities.create(nbtCompound3, world);
				worldChunk.setContainsEntities(true);
				if (entity != null) {
					worldChunk.addEntity(entity);
					Entity entity2 = entity;

					for(NbtCompound nbtCompound4 = nbtCompound3; nbtCompound4.isType("Riding", 10); nbtCompound4 = nbtCompound4.getCompound("Riding")) {
						Entity entity3 = Entities.create(nbtCompound4.getCompound("Riding"), world);
						if (entity3 != null) {
							worldChunk.addEntity(entity3);
							entity2.startRiding(entity3);
						}

						entity2 = entity3;
					}
				}
			}
		}

		NbtList nbtList3 = nbtCompound.getList("TileEntities", 10);
		if (nbtList3 != null) {
			for(m = 0; m < nbtList3.size(); ++m) {
				NbtCompound nbtCompound5 = nbtList3.getCompound(m);
				BlockEntity blockEntity = BlockEntity.fromNbt(nbtCompound5);
				if (blockEntity != null) {
					worldChunk.addBlockEntity(blockEntity);
				}
			}
		}

		if (nbtCompound.isType("TileTicks", 9)) {
			NbtList nbtList4 = nbtCompound.getList("TileTicks", 10);
			if (nbtList4 != null) {
				for(int t = 0; t < nbtList4.size(); ++t) {
					NbtCompound nbtCompound6 = nbtList4.getCompound(t);
					Block block;
					if (nbtCompound6.isType("i", 8)) {
						block = Block.byId(nbtCompound6.getString("i"));
					} else {
						block = Block.byRawId(nbtCompound6.getInt("i"));
					}

					world.loadScheduledTick(new BlockPos(nbtCompound6.getInt("x"), nbtCompound6.getInt("y"), nbtCompound6.getInt("z")), block, nbtCompound6.getInt("t"), nbtCompound6.getInt("p"));
				}
			}
		}

		return worldChunk;
	}
}
