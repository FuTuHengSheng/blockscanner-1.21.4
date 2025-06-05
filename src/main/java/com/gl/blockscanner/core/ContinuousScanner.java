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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ContinuousScanner {
    private static final Map<ServerPlayerEntity, ScanTask> activeScans = new HashMap<>();

    // 修改 stopScan 方法
    public static boolean stopScan(ServerPlayerEntity player) {
        return activeScans.remove(player) != null;
    }

    public static class ScanTask {
        private final Set<Identifier> targetBlocks; // 存储目标方块集合
        private final ServerPlayerEntity player;
        private final int radius;
        private final Queue<ChunkPos> scanQueue = new ConcurrentLinkedQueue<>(); // 待扫描队列
        private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet(); // 线程安全集合
        private final String fileName;

        public ScanTask(ServerPlayerEntity player, int radius, Set<Identifier> targetBlocks) {
            this.player = player;
            this.radius = radius;
            this.targetBlocks = targetBlocks; // 初始化目标方块集合
            this.fileName = FileWriter.generateFileName(player);
            initializeQueue(); // 初始化队列
        }
        public class SpiralTraversal {
            public interface ChunkConsumer {
                void accept(int x, int z);
            }

            // 螺旋遍历算法：从中心向外层扩散
            public static void traverse(int centerX, int centerZ, int radius, ChunkConsumer consumer) {
                for (int r = 0; r <= radius; r++) {
                    for (int x = -r; x <= r; x++) {
                        for (int z = -r; z <= r; z++) {
                            if (Math.abs(x) == r || Math.abs(z) == r) { // 仅处理当前层边缘
                                consumer.accept(centerX + x, centerZ + z);
                            }
                        }
                    }
                }
            }
        }
        // 初始化扫描队列（螺旋式填充）
        private void initializeQueue() {
            ChunkPos center = player.getChunkPos();
            // 螺旋遍历算法：从中心向外扩展
            SpiralTraversal.traverse(center.x, center.z, radius, (x, z) -> {
                scanQueue.add(new ChunkPos(x, z));
            });
        }

        public void tick() {
            ServerWorld world = (ServerWorld) player.getWorld();
            // 每tick处理最多16个区块（可配置）
            int processed = 0;
            while (!scanQueue.isEmpty() && processed < 16) {
                ChunkPos cp = scanQueue.poll();
                if (!scannedChunks.contains(cp)) {
                    scanChunk(world, cp);
                    scannedChunks.add(cp);
                    processed++;
                }
            }
            // 玩家移动时动态补充新区块
            updateQueue();
        }

        // 动态更新队列（检测玩家移动后新增的区块）
        private void updateQueue() {
            ChunkPos currentCenter = player.getChunkPos();
            SpiralTraversal.traverse(currentCenter.x, currentCenter.z, radius, (x, z) -> {
                ChunkPos cp = new ChunkPos(x, z);
                if (!scannedChunks.contains(cp) && !scanQueue.contains(cp)) {
                    scanQueue.add(cp);
                }
            });
        }
        private void scanChunk(ServerWorld world, ChunkPos cp) {
            Chunk chunk = world.getChunk(cp.x, cp.z);
            ChunkPos chunkPos = chunk.getPos();

            // 遍历区块内所有方块
            for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
                for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
                    int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE, x, z);
                    for (int y = world.getBottomY(); y <= topY; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState state = world.getBlockState(pos);
                        Identifier blockId = Registries.BLOCK.getId(state.getBlock());
                        if (targetBlocks.contains(blockId)) { // 检查是否在目标集合中
                            String containerInfo = BlockScanner.getContainerContents(world, pos);
                            FileWriter.saveResult(
                                    fileName, pos, state.getBlock(),
                                    world.getRegistryKey().getValue().getPath(),
                                    containerInfo
                            );
                        }
                    }
                }
            }
        }
    }

    public static void startScan(ServerPlayerEntity player, int radius, Set<Identifier> targetBlocks) {
        activeScans.put(player, new ScanTask(player, radius, targetBlocks));
    }


    public static void tickAll() {
        activeScans.values().forEach(ScanTask::tick);
    }
}