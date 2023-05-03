package io.github.EmiliaThorsen.ornitheCubicChunks;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityProvider;
import net.minecraft.entity.Entity;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.TypeInstanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class cube {
	private final World world;
	private final int cubeXPos;
	private final int cubeYPos;
	private final int cubeZPos;
	private char[] blockData;
	private int blockCount;
	private byte[] skyLight;
	private byte[] blockLight;
	private final TypeInstanceMultiMap<Entity> entities  = new TypeInstanceMultiMap<>(Entity.class);
	private final Map<BlockPos, BlockEntity> blockEntities = Maps.newHashMap();
	private ConcurrentLinkedQueue<BlockPos> blockEntitiesToCreate = Queues.newConcurrentLinkedQueue();

	public cube(boolean hasSkyLight, World world, int cubeXPos, int cubeYPos, int cubeZPos) {
		this.world = world;
		this.cubeXPos = cubeXPos;
		this.cubeYPos = cubeYPos;
		this.cubeZPos = cubeZPos;
		this.blockData = new char[4096];
		this.blockLight = new byte[2048];
		if(hasSkyLight) {
			this.skyLight = new byte[2048];
		}
	}

	public void load(){
		this.world.loadBlockEntities(this.blockEntities.values());
		for (Entity entity : this.entities) {
			entity.beforeLoadedIntoWorld();
		}
		this.world.loadEntities(this.entities);
	}

	public BlockState getBlockState(int x, int y, int z) {
		if (blockCount == 0) {
			return Blocks.AIR.defaultState();
		}
		return Block.STATE_REGISTRY.get(this.blockData[y << 8 | z << 4 | x]);
	}

	public void setBlockState(int x, int y, int z, BlockState state) {
		Block previousBlock = this.getBlockState(x, y, z).getBlock();
		Block newBlock = state.getBlock();

		this.blockCount += ((newBlock != Blocks.AIR)?1:0) - ((previousBlock != Blocks.AIR)?1:0);

		this.blockData[y << 8 | z << 4 | x] = (char)Block.STATE_REGISTRY.getId(state);
	}

	public void addEntity(Entity entity) {
		entity.isLoaded = true;
		entity.chunkX = this.cubeXPos;
		entity.chunkY = this.cubeYPos;
		entity.chunkZ = this.cubeZPos;
		this.entities.add(entity);
	}

	public void removeEntity(Entity entity) {
		this.entities.remove(entity);
	}

	private BlockEntity createBlockEntity(BlockPos pos) {
		BlockState blockState = this.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
		Block block = blockState.getBlock();
		return !block.hasBlockEntity() ? null : ((BlockEntityProvider)block).createBlockEntity(this.world, block.getMetadataFromState(blockState));
	}

	public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.BlockEntityCreationType creationType) {
		BlockEntity blockEntity = this.blockEntities.get(pos);
		if (blockEntity == null) {
			if (creationType == WorldChunk.BlockEntityCreationType.IMMEDIATE) {
				blockEntity = this.createBlockEntity(pos);
				this.world.setBlockEntity(pos, blockEntity);
			} else if (creationType == WorldChunk.BlockEntityCreationType.QUEUED) {
				this.blockEntitiesToCreate.add(pos);
			}
		} else if (blockEntity.isRemoved()) {
			this.blockEntities.remove(pos);
			return null;
		}

		return blockEntity;
	}

	public void addBlockEntity(BlockEntity blockEntity) {
		this.setBlockEntity(blockEntity.getPos(), blockEntity);
		this.world.addBlockEntity(blockEntity);
	}

	public void setBlockEntity(BlockPos pos, BlockEntity blockEntity) {
		blockEntity.setWorld(this.world);
		blockEntity.setPos(pos);
		if (this.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15).getBlock() instanceof BlockEntityProvider) {
			if (this.blockEntities.containsKey(pos)) {
				this.blockEntities.get(pos).markRemoved();
			}

			blockEntity.cancelRemoval();
			this.blockEntities.put(pos, blockEntity);
		}
	}

	public void removeBlockEntity(BlockPos pos) {
		BlockEntity blockEntity = this.blockEntities.remove(pos);
		if (blockEntity != null) {
			blockEntity.markRemoved();
		}
	}


	public void unload() {
		for (BlockEntity blockEntity : this.blockEntities.values()) {
			this.world.unloadBlockEntity(blockEntity);
		}
		this.world.unloadEntities(this.entities);
	}

	public void tick(boolean skipRecheckGaps) {
		while(!this.blockEntitiesToCreate.isEmpty()) {
			BlockPos pos = this.blockEntitiesToCreate.poll();
			if (this.getBlockEntity(pos, WorldChunk.BlockEntityCreationType.CHECK) == null && this.getBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15).getBlock().hasBlockEntity()) {
				BlockEntity blockEntity = this.createBlockEntity(pos);
				this.world.setBlockEntity(pos, blockEntity);
				this.world.onRegionChanged(pos, pos);
			}
		}

	}

	public Map<BlockPos, BlockEntity> getBlockEntities() {
		return this.blockEntities;
	}

	public TypeInstanceMultiMap<Entity> getEntities() {
		return this.entities;
	}

	public boolean isEmpty() {return this.blockCount == 0;}

	public void setSkyLight(int x, int y, int z, int light) {
		int i = y << 7 | z << 3 | x >> 1;
		this.skyLight[i] = ((x & 1) == 0) ? (byte)(this.skyLight[i] & 240 | light & 15) : (byte)(this.skyLight[i] & 15 | (light & 15) << 4);
	}

	public int getSkyLight(int x, int y, int z) {
		int i = y << 7 | z << 3 | x >> 1;
		return ((x & 1) == 0) ? this.skyLight[i] & 15 : this.skyLight[i] >> 4 & 15;
	}

	public void setBlockLight(int x, int y, int z, int light) {
		int i = y << 7 | z << 3 | x >> 1;
		this.blockLight[i] = ((x & 1) == 0) ? (byte)(this.blockLight[i] & 240 | light & 15) : (byte)(this.blockLight[i] & 15 | (light & 15) << 4);
	}

	public int getBlockLight(int x, int y, int z) {
		int i = y << 7 | z << 3 | x >> 1;
		return ((x & 1) == 0) ? this.blockLight[i] & 15 : this.blockLight[i] >> 4 & 15;
	}

	public char[] getBlockData() {
		return this.blockData;
	}

	public void setBlockData(char[] blockData) {
		this.blockData = blockData;
	}

	public byte[] getBlockLightStorage() {
		return this.blockLight;
	}

	public byte[] getSkyLightStorage() {
		return this.skyLight;
	}

	public void setBlockLightStorage(byte[] blockLight) {
		this.blockLight = blockLight;
	}

	public void setSkyLightStorage(byte[] skyLight) {
		this.skyLight = skyLight;
	}

	public int getCubeYPos() {
		return this.cubeYPos;
	}
}
