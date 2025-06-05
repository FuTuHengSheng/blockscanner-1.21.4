package com.gl.blockscanner.core;

import com.gl.blockscanner.util.FileWriter;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import java.util.Set;

public class BlockScanner {
    public static int startScan(ServerPlayerEntity player, int radius, Set<Identifier> targetBlockIds, String fileName) {
        ServerWorld world = (ServerWorld) player.getWorld();
        ChunkPos playerChunk = player.getChunkPos();
        int totalFound = 0;

        for (int x = playerChunk.x - radius; x <= playerChunk.x + radius; x++) {
            for (int z = playerChunk.z - radius; z <= playerChunk.z + radius; z++) {
                Chunk chunk = world.getChunk(x, z);
                totalFound += scanChunk(chunk, world, targetBlockIds, fileName);
            }
        }
        return totalFound;
    }

    private static int scanChunk(Chunk chunk, ServerWorld world, Set<Identifier> targetBlockIds, String fileName) {
        int foundInChunk = 0;
        ChunkPos chunkPos = chunk.getPos();
        for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
            for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                for (int y = world.getBottomY(); y <= topY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                    if (targetBlockIds.contains(blockId)) {
                        String containerContents = getContainerContents(world, pos);
                        FileWriter.saveResult(
                                fileName, pos, state.getBlock(),
                                world.getRegistryKey().getValue().getPath(),
                                containerContents
                        );
                        foundInChunk++;
                    }
                }
            }
        }
        return foundInChunk;
    }

    public static String getContainerContents(ServerWorld world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LootableContainerBlockEntity container) {
            StringBuilder contents = new StringBuilder();
            for (int i = 0; i < container.size(); i++) {
                ItemStack stack = container.getStack(i);
                if (!stack.isEmpty()) {
                    String displayName = stack.getName().getString();
                    contents.append("\"").append(displayName).append("\" x").append(stack.getCount()).append(", ");
                }
            }
            if (!contents.isEmpty()) {
                contents.setLength(contents.length() - 2);
                return contents.toString();
            }
        }
        return null;
    }
}