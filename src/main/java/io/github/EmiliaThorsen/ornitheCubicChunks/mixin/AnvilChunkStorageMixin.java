package io.github.EmiliaThorsen.ornitheCubicChunks.mixin;

import io.github.EmiliaThorsen.ornitheCubicChunks.ornitheCubicChunksMod;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
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
	private static final int[] lightOffSets = {0, 5, 21, 277};
	private static final int[] pow16 = {0, 16, 256, 4096};

	ByteArrayList lightSaver(ByteArrayList saveArray, byte[] blockLights) {
		int blockLight = (blockLights[0] & 15);
		int prevBlockLight = blockLight;
		int curLight = blockLight;
		int prev = blockLight;
		int rl = 0;

		for (int pos = 1; pos < 4096; pos++) {
			int checkPos = ((pos & 256)!=0)?pos^240:pos;
			checkPos = ((checkPos&16)!=0)?checkPos^15:checkPos;
			boolean even = ((checkPos & 1) == 0);
			blockLight = even ? (blockLights[checkPos >> 1] & 15) : (blockLights[checkPos >> 1] >> 4 & 15);
			int thing = blockLight - prevBlockLight;
			if (thing != prev | !((thing == -3) | (thing == -1) | (thing == 0) | (thing == 1) | (thing == 3))) {
				switch (prev) {
					case 0:
						int outNibblePosOffset = ((rl > 4)?1:0) + ((rl > 20)?1:0) + ((rl > 276)?1:0);
						saveArray.add((byte) (((rl > 4)?4:(rl & 15)) + outNibblePosOffset));
						rl -= lightOffSets[outNibblePosOffset];
						if(outNibblePosOffset > 0) {
							saveArray.add((byte) (rl & 15));
							if(outNibblePosOffset > 1) {
								saveArray.add((byte) ((rl >> 4) & 15));
								if(outNibblePosOffset > 2) {
									saveArray.add((byte) ((rl >> 8) & 15));

								}
							}
						}
						break;
					case -1:
						saveArray.add((byte) 8);
						saveArray.add((byte) rl);
						break;
					case 1:
						saveArray.add((byte) 9);
						saveArray.add((byte) rl);
						break;
					case -3:
						saveArray.add((byte) 10);
						saveArray.add((byte) rl);
						break;
					case 3:
						saveArray.add((byte) 11);
						saveArray.add((byte) rl);
						break;
					default:
						switch (curLight) {
							case 15:
								saveArray.add((byte) 12);
								break;
							case 0:
								saveArray.add((byte) 13);
								break;
							default:
								saveArray.add((byte) 14);
								saveArray.add((byte) curLight);
								break;
						}
				}
				prev = thing;
				curLight = blockLight;
				rl = 0;

			} else {
				rl += 1;
			}
			prevBlockLight = blockLight;

		}
		switch (prev) {
			case 0:
				int outNibblePosOffset = ((rl > 4)?1:0) + ((rl > 20)?1:0) + ((rl > 276)?1:0);
				saveArray.add((byte) (((rl > 4)?4:(rl & 15)) + outNibblePosOffset));
				rl -= lightOffSets[outNibblePosOffset];
				if(outNibblePosOffset > 0) {
					saveArray.add((byte) (rl & 15));
					if(outNibblePosOffset > 1) {
						saveArray.add((byte) ((rl >> 4) & 15));
						if(outNibblePosOffset > 2) {
							saveArray.add((byte) ((rl >> 8) & 15));

						}
					}
				}
				break;
			case -1:
				saveArray.add((byte) 8);
				saveArray.add((byte) rl);
				break;
			case 1:
				saveArray.add((byte) 9);
				saveArray.add((byte) rl);
				break;
			case -3:
				saveArray.add((byte) 10);
				saveArray.add((byte) rl);
				break;
			case 3:
				saveArray.add((byte) 11);
				saveArray.add((byte) rl);
				break;
			default:
				switch (curLight) {
					case 15:
						saveArray.add((byte) 12);
						break;
					case 0:
						saveArray.add((byte) 13);
						break;
					default:
						saveArray.add((byte) 14);
						saveArray.add((byte) curLight);
						break;
				}
		}
		return saveArray;
	}

	ornitheCubicChunksMod.loadReturns lightLoader(ByteArrayFIFOQueue inNibbles) {
		int rl = 0;
		byte[] lightLevels = new byte[2048];
		int currentBlockLight = 0;
		int currentChange = 0;
		for (int pos = 0; pos < 4096; pos++) {
			int checkPos = ((pos & 256)!=0)?pos^240:pos;
			checkPos = ((checkPos&16)!=0)?checkPos^15:checkPos;
			if (rl != 0) {
				rl -= 1;
			} else {
				byte nibble = inNibbles.dequeueByte();
				switch (nibble) {
					case 8:
						rl = inNibbles.dequeueByte();
						currentChange = -1;
						break;
					case 9:
						rl = inNibbles.dequeueByte();
						currentChange = 1;
						break;
					case 10:
						rl = inNibbles.dequeueByte();
						currentChange = -3;
						break;
					case 11:
						rl = inNibbles.dequeueByte();
						currentChange = 3;
						break;
					case 12:
						currentBlockLight = 15;
						currentChange = 0;
						break;
					case 13:
						currentBlockLight = 0;
						currentChange = 0;
						break;
					case 14:
						currentBlockLight = inNibbles.dequeueByte();
						currentChange = 0;
						break;
					default:
						if (nibble > 4) {
							rl = inNibbles.dequeueByte() + lightOffSets[nibble - 4];
							if(nibble > 5) {
								rl += inNibbles.dequeueByte() << 4;
								if(nibble > 6) {
									rl += inNibbles.dequeueByte() << 8;
								}
							}
						} else {
							rl = nibble;
						}
						currentChange = 0;
						break;
				}
			}
			currentBlockLight += currentChange;
			boolean even = ((checkPos & 1) == 0);
			lightLevels[checkPos >> 1] = (byte) (even?(lightLevels[checkPos >> 1] & 240 | currentBlockLight & 15):(lightLevels[checkPos >> 1] & 15 | (currentBlockLight & 15) << 4));
		}
		ornitheCubicChunksMod.loadReturns returns = new ornitheCubicChunksMod.loadReturns();
		returns.outputs = lightLevels;
		returns.inputs = inNibbles;
		return returns;
	}

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

			int palletePos;
			int runLenghSize;

			ByteArrayList outputs = new ByteArrayList();


			for (int pos = 0; pos < 4096; pos++) {
				if (prevBlock != blocks[pos]) {
					if (!(pallete.containsKey(prevBlock))) {
						outputs.add((byte) 0);
						outputs.add((byte) (((prevBlock) >> 12) & 15));
						outputs.add((byte) (((prevBlock) >> 8) & 15));
						outputs.add((byte) (((prevBlock) >> 4) & 15));
						outputs.add((byte) ((prevBlock) & 15));
						pallete.put(prevBlock, palleteSize);
						palletePos = palleteSize;
						palleteSize += 1;
						palleteNibbleSize += (palleteSize > pow16[palleteNibbleSize])?1:0;
					} else {
						palletePos = pallete.get(prevBlock);
					}
					runLenghSize = ((runLength > 12)?1:0) + ((runLength > 28)?1:0) + ((runLength > 284)?1:0);
					outputs.add((byte) (((runLength > 12)?12:(runLength & 15)) + runLenghSize));
					runLength -= offSets[runLenghSize];
					if(runLenghSize > 0) {
						outputs.add((byte) (runLength & 15));
						if(runLenghSize > 1) {
							outputs.add((byte) ((runLength >> 4) & 15));
							if(runLenghSize > 2) {
								outputs.add((byte) ((runLength >> 8) & 15));
							}
						}
					}
					outputs.add((byte) (palletePos & 15));
					if(palleteNibbleSize > 1) {
						outputs.add((byte) ((palletePos >> 4) & 15));
						if(palleteNibbleSize > 2) {
							outputs.add((byte) ((palletePos >> 8) & 15));
						}
					}
					runLength = 1;
					prevBlock = blocks[pos];
					continue;
				}
				runLength += 1;
			}
			if (!(pallete.containsKey(prevBlock))) {
				outputs.add((byte) 0);
				outputs.add((byte) (((prevBlock) >> 12) & 15));
				outputs.add((byte) (((prevBlock) >> 8) & 15));
				outputs.add((byte) (((prevBlock) >> 4) & 15));
				outputs.add((byte) ((prevBlock) & 15));
				pallete.put(prevBlock, palleteSize);
				palletePos = palleteSize;
				palleteSize += 1;
				palleteNibbleSize += (palleteSize > pow16[palleteNibbleSize])?1:0;
			} else {
				palletePos = pallete.get(prevBlock);
			}
			runLenghSize = ((runLength > 12)?1:0) + ((runLength > 28)?1:0) + ((runLength > 284)?1:0);
			outputs.add((byte) (((runLength > 12)?12:(runLength & 15)) + runLenghSize));
			runLength -= offSets[runLenghSize];
			if(runLenghSize > 0) {
				outputs.add((byte) (runLength & 15));
				if(runLenghSize > 1) {
					outputs.add((byte) ((runLength >> 4) & 15));
					if(runLenghSize > 2) {
						outputs.add((byte) ((runLength >> 8) & 15));
					}
				}
			}
			outputs.add((byte) (palletePos & 15));
			if(palleteNibbleSize > 1) {
				outputs.add((byte) ((palletePos >> 4) & 15));
				if(palleteNibbleSize > 2) {
					outputs.add((byte) ((palletePos >> 8) & 15));
				}
			}

			//start of light level saving
			ByteArrayList lightsOut = new ByteArrayList();
			outputs = lightSaver(outputs, worldChunkSection.getBlockLightStorage().getData());

			if (bl) {
				outputs = lightSaver(outputs, worldChunkSection.getSkyLightStorage().getData());
			}

			if(outputs.size() % 2 == 1) outputs.add((byte) 0);
			byte[] output = new byte[(outputs.size() >> 1)];
			int nibble = 0;
			for(int outputPos = 0; outputPos < (outputs.size() >> 1); outputPos++){
				output[outputPos] = (byte) ((outputs.getByte(nibble) << 4) + (outputs.getByte(nibble + 1)));
				nibble += 2;
			}
			outputs.clear();

			subChunkNbt.putByteArray("nibbles", output);

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
				Identifier identifier = Block.REGISTRY.getKey(scheduledTick.getBlock());
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
			ByteArrayFIFOQueue inNibbles = new ByteArrayFIFOQueue();
			for (byte nibble : input) {
				inNibbles.enqueue((byte) ((nibble >> 4) & 15));
				inNibbles.enqueue((byte) (nibble & 15));
			}

			int palleteSize = 0;
			int palleteNibbleSize = 1;
			ArrayList<Character> pallete = new ArrayList<>();

			char[] output = new char[4096];

			int currentRunLength = 0;
			char currentBlock = 0;

			for(int blockArrayPos = 0; blockArrayPos < 4096; blockArrayPos++) {
				if (currentRunLength == 0) {
					int nibble = inNibbles.dequeueByte();
					if (nibble == 0) {
						pallete.add((char) ((inNibbles.dequeueByte() << 12) + (inNibbles.dequeueByte() << 8) + (inNibbles.dequeueByte() << 4) + inNibbles.dequeueByte()));
						palleteSize += 1;
						palleteNibbleSize += (palleteSize > pow16[palleteNibbleSize])?1:0;
						nibble = inNibbles.dequeueByte();
					}

					if(nibble > 12) {
						currentRunLength = inNibbles.dequeueByte() + offSets[nibble - 12];
						if(nibble > 13) {
							currentRunLength += inNibbles.dequeueByte() << 4;
							if(nibble > 14) {
								currentRunLength += inNibbles.dequeueByte() << 8;
							}
						}
					} else currentRunLength = nibble;

					int palletePos = inNibbles.dequeueByte();
					if(palleteNibbleSize > 1) {
						palletePos += inNibbles.dequeueByte() << 4;
						if(palleteNibbleSize > 2) {
							palletePos += inNibbles.dequeueByte() << 8;
						}
					}
					currentBlock = pallete.get(palletePos);
				}
				currentRunLength -= 1;
				output[blockArrayPos] = currentBlock;
			}
			worldChunkSection.setBlockData(output);

			//start of light level loading

			ornitheCubicChunksMod.loadReturns returns = lightLoader(inNibbles);
			worldChunkSection.setBlockLightStorage(new ChunkNibbleStorage(returns.outputs));

			if (bl) {
				ornitheCubicChunksMod.loadReturns returns1 = lightLoader(returns.inputs);

				worldChunkSection.setSkyLightStorage(new ChunkNibbleStorage(returns1.outputs));
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
