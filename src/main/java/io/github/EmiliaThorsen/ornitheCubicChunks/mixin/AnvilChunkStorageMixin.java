package io.github.EmiliaThorsen.ornitheCubicChunks.mixin;

import io.github.EmiliaThorsen.ornitheCubicChunks.ornitheCubicChunksMod;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
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
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

@Mixin(AnvilChunkStorage.class)
public abstract class AnvilChunkStorageMixin {
	@Shadow
	public abstract boolean run();

	private static final int[] offSets = {0, 13, 29, 285};
	private static final int[] lightOffSets = {0, 6, 22, 278};
	private static final int[] pow16 = {0, 16, 256, 4096};

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

			//saving y value of subchunk
			NbtCompound subChunkNbt = new NbtCompound();
			subChunkNbt.putByte("Y", (byte) (worldChunkSection.getOffsetY() >> 4));

			//start of block compression and saving
			char[] blocks = worldChunkSection.getBlockData();

			char prevBlock = blocks[0];
			int runLength = 0;

			Int2IntOpenHashMap pallete = new Int2IntOpenHashMap();
			int palleteSize = 0;
			int palleteNibbleSize = 1;

			byte[] outNibbleArray = new byte[8192];
			int outNibblePos = 0;

			int palletePos;
			int outNibblePosOffset;

			for (int pos = 0; pos < 4096; pos++) {
				if (prevBlock != blocks[pos]) {
					if (!(pallete.containsKey(prevBlock))) {
						outNibbleArray[outNibblePos] = 0;
						outNibbleArray[outNibblePos + 1] = (byte) (((prevBlock) >> 12) & 15);
						outNibbleArray[outNibblePos + 2] = (byte) (((prevBlock) >> 8) & 15);
						outNibbleArray[outNibblePos + 3] = (byte) (((prevBlock) >> 4) & 15);
						outNibbleArray[outNibblePos + 4] = (byte) ((prevBlock) & 15);
						outNibblePos += 5;
						pallete.put(prevBlock, palleteSize);
						palletePos = palleteSize;
						palleteSize += 1;
						palleteNibbleSize += (palleteSize > pow16[palleteNibbleSize])?1:0;
					} else {
						palletePos = pallete.get(prevBlock);
					}
					outNibblePosOffset = ((runLength > 12)?1:0) + ((runLength > 28)?1:0) + ((runLength > 285)?1:0);
					outNibbleArray[outNibblePos] = (byte) (((runLength > 12)?12:(runLength & 15)) + outNibblePosOffset);
					runLength -= offSets[outNibblePosOffset];
					outNibbleArray[outNibblePos + 1] = (byte) ((runLength) & 15);
					outNibbleArray[outNibblePos + 2] = (byte) (((runLength) >> 4) & 15);
					outNibbleArray[outNibblePos + 3] = (byte) (((runLength) >> 8) & 15);
					outNibblePos += outNibblePosOffset + 1;
					outNibbleArray[outNibblePos] = (byte) (palletePos & 15);
					outNibbleArray[outNibblePos + 1] = (byte) (((palletePos) >> 4) & 15);
					outNibbleArray[outNibblePos + 2] = (byte) (((palletePos) >> 8) & 15);
					outNibblePos += palleteNibbleSize;

					runLength = 1;
					prevBlock = blocks[pos];
					continue;
				}
				runLength += 1;
			}
			if (!(pallete.containsKey(prevBlock))) {
				outNibbleArray[outNibblePos] = 0;
				outNibbleArray[outNibblePos + 1] = (byte) (((prevBlock) >> 12) & 15);
				outNibbleArray[outNibblePos + 2] = (byte) (((prevBlock) >> 8) & 15);
				outNibbleArray[outNibblePos + 3] = (byte) (((prevBlock) >> 4) & 15);
				outNibbleArray[outNibblePos + 4] = (byte) ((prevBlock) & 15);
				outNibblePos += 5;
				pallete.put(prevBlock, palleteSize);
				palletePos = palleteSize;
				palleteSize += 1;
				palleteNibbleSize += (palleteSize > pow16[palleteNibbleSize])?1:0;
			} else {
				palletePos = pallete.get(prevBlock);
			}
			outNibblePosOffset = ((runLength > 12)?1:0) + ((runLength > 28)?1:0) + ((runLength > 285)?1:0);
			outNibbleArray[outNibblePos] = (byte) (((runLength > 12)?12:(runLength & 15)) + outNibblePosOffset);
			runLength -= offSets[outNibblePosOffset];
			outNibbleArray[outNibblePos + 1] = (byte) ((runLength) & 15);
			outNibbleArray[outNibblePos + 2] = (byte) (((runLength) >> 4) & 15);
			outNibbleArray[outNibblePos + 3] = (byte) (((runLength) >> 8) & 15);
			outNibblePos += outNibblePosOffset + 1;
			outNibbleArray[outNibblePos] = (byte) (palletePos & 15);
			outNibbleArray[outNibblePos + 1] = (byte) (((palletePos) >> 4) & 15);
			outNibbleArray[outNibblePos + 2] = (byte) (((palletePos) >> 8) & 15);
			outNibblePos += palleteNibbleSize;

			byte[] output = new byte[1 + (outNibblePos >> 1)];
			int nibble = 0;
			for(int outputPos = 0; outputPos < (1 + (outNibblePos >> 1)); outputPos++){
				output[outputPos] = (byte) ((outNibbleArray[nibble] << 4) + (outNibbleArray[nibble + 1]));
				nibble += 2;
			}

			subChunkNbt.putByteArray("nibbles", output);

			//start of light level saving
			byte[] blockLights = worldChunkSection.getBlockLightStorage().getData();

			byte[] blockLightsOut = new byte[8000];


			int prevBlockLight = blockLights[0] & 15;

			int lightNibbleArrayPos = 0;
			switch (prevBlockLight) {
				case 15:
					blockLightsOut[lightNibbleArrayPos] = 12;
					lightNibbleArrayPos += 1;
					break;
				case 0:
					blockLightsOut[lightNibbleArrayPos] = 13;
					lightNibbleArrayPos += 1;
					break;
				default:
					blockLightsOut[lightNibbleArrayPos] = 14;
					blockLightsOut[lightNibbleArrayPos + 1] = (byte) (prevBlockLight & 15);
					lightNibbleArrayPos += 2;
					break;
			}

			for (int pos = 1; pos < 4096; pos++) {
				int checkPos = ((pos & 256)!=0)?pos^240:pos;
				checkPos = ((checkPos&16)!=0)?checkPos^15:checkPos;
				boolean even = ((checkPos & 1) == 0);
				int blockLight = even ? (blockLights[checkPos >> 1] & 15) : (blockLights[checkPos >> 1] >> 4 & 15);

					switch (blockLight - prevBlockLight) {
						case 0:
							blockLightsOut[lightNibbleArrayPos] = 0;
							lightNibbleArrayPos += 1;
							break;
						case -1:
							blockLightsOut[lightNibbleArrayPos] = 8;
							lightNibbleArrayPos += 1;
							break;
						case 1:
							blockLightsOut[lightNibbleArrayPos] = 9;
							lightNibbleArrayPos += 1;
							break;
						case -3:
							blockLightsOut[lightNibbleArrayPos] = 10;
							lightNibbleArrayPos += 1;
							break;
						case 3:
							blockLightsOut[lightNibbleArrayPos] = 11;
							lightNibbleArrayPos += 1;
							break;
						default:
							switch (blockLight) {
								case 15:
									blockLightsOut[lightNibbleArrayPos] = 12;
									lightNibbleArrayPos += 1;
									break;
								case 0:
									blockLightsOut[lightNibbleArrayPos] = 13;
									lightNibbleArrayPos += 1;
									break;
								default:
									blockLightsOut[lightNibbleArrayPos] = 14;
									blockLightsOut[lightNibbleArrayPos + 1] = (byte) blockLight;
									lightNibbleArrayPos += 2;
									break;
							}

				}
				prevBlockLight = blockLight;
			}


			subChunkNbt.putByteArray("BlockLight", blockLightsOut);

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

			//loading subchunk y level
			m = nbtCompound2.getByte("Y");
			WorldChunkSection worldChunkSection = new WorldChunkSection(m << 4, bl);

			//start of block uncompressing and loading
			byte[] input = nbtCompound2.getByteArray("nibbles");
			byte[] nibbles = new byte[input.length << 2];
			int nibblePos = 0;
			for (byte nibble : input) {
				nibbles[nibblePos] = (byte) ((nibble >> 4) & 15);
				nibbles[nibblePos + 1] = (byte) (nibble & 15);
				nibblePos += 2;
			}

			int currentNibble = 0;
			int palleteSize = 0;
			int palleteNibbleSize = 1;
			ArrayList<Character> pallete = new ArrayList<>();

			char[] output = new char[4096];

			int currentRunLength = 0;
			char currentBlock = 0;

			for(int blockArrayPos = 0; blockArrayPos < 4096; blockArrayPos++) {
				if (currentRunLength == 0) {
					if (nibbles[currentNibble] == 0) {
						pallete.add((char) ((nibbles[currentNibble + 1] << 12) + (nibbles[currentNibble + 2] << 8) + (nibbles[currentNibble + 3] << 4) + nibbles[currentNibble + 4]));
						currentNibble += 5;
						palleteSize += 1;
						palleteNibbleSize += (palleteSize > pow16[palleteNibbleSize])?1:0;
					}
					int num = (nibbles[currentNibble] > 12?1:0) + (nibbles[currentNibble] > 13?1:0) + (nibbles[currentNibble] > 14?1:0);
					currentRunLength = num == 0 ?nibbles[currentNibble]: nibbles[currentNibble + 1] + (num > 1?nibbles[currentNibble + 2] << 4:0) + (num > 2?nibbles[currentNibble + 3] << 8:0) + offSets[num];
					currentNibble += num + 1;
					currentBlock = pallete.get(nibbles[currentNibble] + (palleteNibbleSize > 1?(nibbles[currentNibble + 1] << 4):0) + (palleteNibbleSize > 2?(nibbles[currentNibble + 2] << 8):0));
					currentNibble += palleteNibbleSize;
				}
				currentRunLength -= 1;
				output[blockArrayPos] = currentBlock;
			}
			worldChunkSection.setBlockData(output);


			//start of light level loading
			byte[] blockLights = nbtCompound2.getByteArray("BlockLight");


			byte[] blockLightsOut = new byte[2048];
			int currentBlockLight = 0;
			int inArrayPos = 0;
			int currentChange;
			for (int pos = 0; pos < 4096; pos++) {
				int checkPos = ((pos & 256)!=0)?pos^240:pos;
				checkPos = ((checkPos&16)!=0)?checkPos^15:checkPos;

					switch (blockLights[inArrayPos]) {
						case 8:
							inArrayPos += 1;
							currentChange = -1;
							break;
						case 9:
							inArrayPos += 1;
							currentChange = 1;
							break;
						case 10:
							inArrayPos += 1;
							currentChange = -3;
							break;
						case 11:
							inArrayPos += 1;
							currentChange = 3;
							break;
						case 12:
							inArrayPos += 1;
							currentBlockLight = 15;
							currentChange = 0;
							break;
						case 13:
							inArrayPos += 1;
							currentBlockLight = 0;
							currentChange = 0;
							break;
						case 14:
							currentBlockLight = blockLights[inArrayPos + 1];
							inArrayPos += 2;
							currentChange = 0;
							break;
						default:
							inArrayPos += 1;
							currentChange = 0;
							break;

					}

				currentBlockLight += currentChange;
				boolean even = ((checkPos & 1) == 0);
				blockLightsOut[checkPos >> 1] = (byte) (even?(blockLightsOut[checkPos >> 1] & 240 | currentBlockLight & 15):(blockLightsOut[checkPos >> 1] & 15 | (currentBlockLight & 15) << 4));
			}

			worldChunkSection.setBlockLightStorage(new ChunkNibbleStorage(blockLightsOut));

			if (bl) {worldChunkSection.setSkyLightStorage(new ChunkNibbleStorage(nbtCompound2.getByteArray("SkyLight")));}

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
