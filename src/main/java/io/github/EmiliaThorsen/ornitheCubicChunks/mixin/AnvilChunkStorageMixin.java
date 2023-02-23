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
	private static final LinkedList[] ruleSet = new LinkedList[]{
			new LinkedList<>(Arrays.asList(8 , 13, 4, 16, 4, 12, 3 , 14, 3 , 13, 5, 17, 5, 12, 10)),
			new LinkedList<>(Arrays.asList(9 , 13, 5, 17, 5, 12, 2 , 15, 2 , 13, 4, 16, 4, 12, 11)),
			new LinkedList<>(Arrays.asList(10, 12, 6, 16, 6, 13, 1 , 15, 1 , 12, 7, 17, 7, 13, 8 )),
			new LinkedList<>(Arrays.asList(11, 12, 7, 17, 7, 13, 0 , 14, 0 , 12, 6, 16, 6, 13, 9 )),
			new LinkedList<>(Arrays.asList(8 , 13, 0, 14, 0, 12, 6 , 16, 6 , 13, 1, 15, 1, 12, 11)),
			new LinkedList<>(Arrays.asList(9 , 13, 1, 15, 1, 12, 7 , 17, 7 , 13, 0, 14, 0, 12, 10)),
			new LinkedList<>(Arrays.asList(10, 12, 2, 15, 2, 13, 4 , 16, 4 , 12, 3, 14, 3, 13, 9 )),
			new LinkedList<>(Arrays.asList(11, 12, 3, 14, 3, 13, 5 , 17, 5 , 12, 2, 15, 2, 13, 8 )),
			new LinkedList<>(Arrays.asList(0 , 14, 4, 16, 4, 15, 9 , 13, 9 , 14, 7, 17, 7, 15, 2 )),
			new LinkedList<>(Arrays.asList(1 , 15, 5, 17, 5, 14, 8 , 13, 8 , 15, 6, 16, 6, 14, 3 )),
			new LinkedList<>(Arrays.asList(6 , 16, 2, 15, 2, 17, 11, 12, 11, 16, 0, 14, 0, 17, 5 )),
			new LinkedList<>(Arrays.asList(3 , 14, 7, 17, 7, 15, 10, 12, 10, 14, 4, 16, 4, 15, 1 ))
	};

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
		NbtList nbtList = new NbtList();
		boolean bl = !world.dimension.isDark();

		for (WorldChunkSection worldChunkSection : worldChunkSections) { //loop over all subchunks
			if (worldChunkSection == null) continue;

			NbtCompound subChunkNbt = new NbtCompound();
			subChunkNbt.putByte("Y", (byte) (worldChunkSection.getOffsetY() >> 4 & 255));
			ChunkNibbleStorage chunkNibbleStorage = new ChunkNibbleStorage();
			ChunkNibbleStorage chunkNibbleStorage2 = null;

			int blockArrayPos = 0;
			LinkedList<Integer> ruleCallList = new LinkedList<>();
			ruleCallList.add(0);
			LinkedList<Integer> iterationCallList = new LinkedList<>();
			iterationCallList.add(4);
			byte[] output = new byte[16 * 16 * 16];
			int outArrayPos = 0;

			while(ruleCallList.size() != 0) {
				int currentRuleCall = ruleCallList.getFirst();
				ruleCallList.removeFirst();
				int iteration = iterationCallList.getFirst();
				iterationCallList.removeFirst();
				switch (currentRuleCall){
					case 12:
						blockArrayPos -= 256;
						continue;
					case 13:
						blockArrayPos += 256;
						continue;
					case 14:
						blockArrayPos += 1;
						continue;
					case 15:
						blockArrayPos -= 1;
						continue;
					case 16:
						blockArrayPos += 16;
						continue;
					case 17:
						blockArrayPos -= 16;
						continue;
				}
				if(iteration == 0) {
					char block = worldChunkSection.getBlockData()[blockArrayPos];
					if (block >> 12 != 0) {
						if (chunkNibbleStorage2 == null) {chunkNibbleStorage2 = new ChunkNibbleStorage();}
						chunkNibbleStorage2.set(outArrayPos, block >> 12);
					}
					output[outArrayPos] = (byte) (block >> 4 & 255);
					chunkNibbleStorage.set(outArrayPos, block & 15);
					outArrayPos += 1;
					continue;
				}
				LinkedList<Integer> merged = new LinkedList<>();
				merged.addAll(this.ruleSet[currentRuleCall]);
				merged.addAll(ruleCallList);
				ruleCallList = merged;
				for (int element = 0; element < 15; element++) iterationCallList.addFirst(iteration-1);
			}
			subChunkNbt.putByteArray("Blocks", output);
			subChunkNbt.putByteArray("Data", chunkNibbleStorage.getData());
			if (chunkNibbleStorage2 != null) {
				subChunkNbt.putByteArray("Add", chunkNibbleStorage2.getData());
			}

			subChunkNbt.putByteArray("BlockLight", worldChunkSection.getBlockLightStorage().getData());
			if (bl) {
				subChunkNbt.putByteArray("SkyLight", worldChunkSection.getSkyLightStorage().getData());
			} else {
				subChunkNbt.putByteArray("SkyLight", new byte[worldChunkSection.getBlockLightStorage().getData().length]);
			}

			nbtList.add(subChunkNbt);
		}

		nbtCompound.put("Sections", nbtList);
		nbtCompound.putByteArray("Biomes", worldChunk.getBiomes());
		worldChunk.setContainsEntities(false);
		NbtList nbtList2 = new NbtList();

		Iterator iterator;
		NbtCompound nbtCompound2;
		for(int i = 0; i < worldChunk.getEntities().length; ++i) {
			iterator = worldChunk.getEntities()[i].iterator();

			while(iterator.hasNext()) {
				Entity entity = (Entity)iterator.next();
				nbtCompound2 = new NbtCompound();
				if (entity.writeNbtNoRider(nbtCompound2)) {
					worldChunk.setContainsEntities(true);
					nbtList2.add(nbtCompound2);
				}
			}
		}

		nbtCompound.put("Entities", nbtList2);
		NbtList nbtList3 = new NbtList();
		iterator = worldChunk.getBlockEntities().values().iterator();

		while(iterator.hasNext()) {
			BlockEntity blockEntity = (BlockEntity)iterator.next();
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
			ChunkNibbleStorage chunkNibbleStorage = new ChunkNibbleStorage(nbtCompound2.getByteArray("Data"));
			ChunkNibbleStorage chunkNibbleStorage2 = nbtCompound2.isType("Add", 7) ? new ChunkNibbleStorage(nbtCompound2.getByteArray("Add")) : null;

			int blockArrayPos = 0;
			LinkedList<Integer> ruleCallList = new LinkedList<>();
			ruleCallList.add(0);
			LinkedList<Integer> iterationCallList = new LinkedList<>();
			iterationCallList.add(4);
			char[] output = new char[blocks.length];
			int outArrayPos = 0;
			while(ruleCallList.size() != 0) {
				int currentRuleCall = ruleCallList.getFirst();
				ruleCallList.removeFirst();
				int iteration = iterationCallList.getFirst();
				iterationCallList.removeFirst();
				switch (currentRuleCall){
					case 12:
						blockArrayPos -= 256;
						continue;
					case 13:
						blockArrayPos += 256;
						continue;
					case 14:
						blockArrayPos += 1;
						continue;
					case 15:
						blockArrayPos -= 1;
						continue;
					case 16:
						blockArrayPos += 16;
						continue;
					case 17:
						blockArrayPos -= 16;
						continue;
				}
				if(iteration == 0) {
					int r = chunkNibbleStorage2 != null ? chunkNibbleStorage2.get(outArrayPos) : 0;
					output[blockArrayPos] = (char)(r << 12 | (blocks[outArrayPos] & 255) << 4 | chunkNibbleStorage.get(outArrayPos));
					outArrayPos += 1;
					continue;
				}
				LinkedList<Integer> merged = new LinkedList<>();
				merged.addAll(this.ruleSet[currentRuleCall]);
				merged.addAll(ruleCallList);
				ruleCallList = merged;
				for (int element = 0; element < 15; element++) iterationCallList.addFirst(iteration-1);
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
		if (nbtCompound.isType("Biomes", 7)) {
			worldChunk.setBiomes(nbtCompound.getByteArray("Biomes"));
		}

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
