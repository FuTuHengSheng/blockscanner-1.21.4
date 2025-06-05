package com.gl.blockscanner.util;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileWriter {
    private static final ExecutorService writerExecutor = Executors.newSingleThreadExecutor();

    private static final Path OUTPUT_DIR = Paths.get("blockscanneroutput"); // 输出目录
    public static int scanCount = 0; // 扫描次数计数器
    // 生成唯一文件名（包含时间戳和玩家名）
    public static String generateFileName(ServerPlayerEntity player) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String playerName = player.getName().getString();
        return String.format("scan_%s_%s.txt", playerName, timestamp);
    }

    // 将结果追加到指定文件
    public static void saveResult(String fileName, BlockPos pos, Block block, String dimension, String containerContents) {
        writerExecutor.submit(() -> {
        try {
            if (!Files.exists(OUTPUT_DIR)) {
                Files.createDirectories(OUTPUT_DIR);
            }
            scanCount++;
            Path filePath = OUTPUT_DIR.resolve(fileName);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    filePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                String line = String.format(
                        "[编号: %d] [方块: %s] [坐标: (%d, %d, %d)] [维度: %s]",
                        scanCount,
                        block.getTranslationKey(),
                        pos.getX(), pos.getY(), pos.getZ(),
                        dimension
                );
                if (containerContents != null) {
                    line += " [容器内容: " + containerContents + "]";
                }
                line += "\n";
                writer.write(line);
            }
        } catch (IOException e) {
            System.err.println("文件写入失败: " + e.getMessage());
            e.printStackTrace();
        }});
    }

    public static void cleanEmptyFile(String fileName) {
        try {
            Path filePath = OUTPUT_DIR.resolve(fileName);
            if (Files.exists(filePath) && Files.size(filePath) == 0) {
                Files.delete(filePath);
                System.out.println("已删除空文件: " + filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("删除空文件失败: " + e.getMessage());
        }
    }

    // 新增实体写入方法
    public static void saveEntityResult(String fileName, BlockPos pos, EntityType<?> entityType, String dimension, String itemInfo) {
        try {
            if (!Files.exists(OUTPUT_DIR)) {
                Files.createDirectories(OUTPUT_DIR);
            }
            scanCount++;
            Path filePath = OUTPUT_DIR.resolve(fileName);
            try (BufferedWriter writer = Files.newBufferedWriter(
                    filePath,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                String line = String.format(
                        "[编号: %d] [实体: %s] [坐标: (%d, %d, %d)] [维度: %s]",
                        scanCount,
                        EntityType.getId(entityType).toString(),
                        pos.getX(), pos.getY(), pos.getZ(),
                        dimension
                );
                if (itemInfo != null) {
                    line += " [携带物品: " + itemInfo + "]";
                }
                line += "\n";
                writer.write(line);
            }
        } catch (IOException e) {
            System.err.println("实体写入失败: " + e.getMessage());
        }
    }
}