package com.gl.blockscanner.core;

import com.gl.blockscanner.util.FileWriter;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

public class BlockScanTask {
    private final ServerPlayerEntity player;
    private final int radius;
    private final Set<Identifier> targetBlocks;
    private final Queue<ChunkPos> scanQueue = new ConcurrentLinkedQueue<>();
    private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
    private final String fileName;

    public BlockScanTask(ServerPlayerEntity player, int radius, Set<Identifier> targetBlocks) {
        this.player = player;
        this.radius = radius;
        this.targetBlocks = targetBlocks;
        this.fileName = FileWriter.generateFileName(player);
        initializeQueue();
    }

    // 初始化螺旋队列
    private void initializeQueue() {
        ChunkPos center = player.getChunkPos();
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.abs(x) == r || Math.abs(z) == r) {
                        scanQueue.add(new ChunkPos(center.x + x, center.z + z));
                    }
                }
            }
        }
    }

    // 每tick处理16个区块
    public boolean tick() {
        ServerWorld world = (ServerWorld) player.getWorld();
        int processed = 0;
        while (!scanQueue.isEmpty() && processed < 16) {
            ChunkPos cp = scanQueue.poll();
            if (!scannedChunks.contains(cp)) {
                scanChunk(world, cp);
                scannedChunks.add(cp);
                processed++;
            }
        }
        return !scanQueue.isEmpty();
    }

    // 扫描单个区块
    private void scanChunk(ServerWorld world, ChunkPos cp) {
        Chunk chunk = world.getChunk(cp.x, cp.z);
        ChunkPos chunkPos = chunk.getPos();
        for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
            for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
                int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                for (int y = world.getBottomY(); y <= topY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);
                    Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                    if (targetBlocks.contains(blockId)) {
                        String containerContents = BlockScanner.getContainerContents(world, pos);
                        FileWriter.saveResult(
                                fileName, pos, state.getBlock(),
                                world.getRegistryKey().getValue().getPath(),
                                containerContents
                        );
                    }
                }
            }
        }
    }

    public String getFileName() {
        return fileName;
    }
}