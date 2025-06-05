package com.gl.blockscanner.core;

import com.gl.blockscanner.util.FileWriter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import java.util.ArrayList;
import java.util.List;

import java.util.Set;
import java.util.stream.Collectors;

public class EntityScanner {
    public static int startScan(ServerPlayerEntity player, int radius, Set<Identifier> targetEntityIds, String fileName) {
        ServerWorld world = (ServerWorld) player.getWorld();
        ChunkPos playerChunk = player.getChunkPos();
        int totalFound = 0;

        Set<EntityType<?>> targetTypes = targetEntityIds.stream()
                .map(id -> Registries.ENTITY_TYPE.get(id))
                .filter(type -> type != null)
                .collect(Collectors.toSet());

        for (int x = playerChunk.x - radius; x <= playerChunk.x + radius; x++) {
            for (int z = playerChunk.z - radius; z <= playerChunk.z + radius; z++) {
                ChunkPos currentChunk = new ChunkPos(x, z);
                totalFound += scanChunk(world, currentChunk, targetTypes, fileName);
            }
        }
        return totalFound;
    }

    private static int scanChunk(ServerWorld world, ChunkPos targetChunk, Set<EntityType<?>> targetTypes, String fileName) {
        int foundInChunk = 0;
        for (Entity entity : world.iterateEntities()) {
            BlockPos pos = entity.getBlockPos();
            ChunkPos entityChunk = new ChunkPos(pos);
            if (entityChunk.x == targetChunk.x && entityChunk.z == targetChunk.z) {
                if (targetTypes.contains(entity.getType())) {
                    String itemInfo = getEntityItems(entity);
                    FileWriter.saveEntityResult(
                            fileName, pos, entity.getType(),
                            world.getRegistryKey().getValue().getPath(),
                            itemInfo
                    );
                    foundInChunk++;
                }
            }
        }
        return foundInChunk;
    }

    // 获取实体携带的物品信息
    private static String getEntityItems(Entity entity) {
        List<String> items = new ArrayList<>();

        // 1. 处理展示框（ItemFrame）
        if (entity instanceof ItemFrameEntity itemFrame) {
            ItemStack frameItem = itemFrame.getHeldItemStack();
            if (!frameItem.isEmpty()) {
                items.add(formatItemStack(frameItem));
            }
        }

        // 2. 处理盔甲架（ArmorStand）
        if (entity instanceof ArmorStandEntity armorStand) {
            for (ItemStack stack : armorStand.getHandItems()) {
                if (!stack.isEmpty()) {
                    items.add(formatItemStack(stack));
                }
            }
        }

        // 3. 处理生物（如僵尸、骷髅等）
        if (entity instanceof LivingEntity livingEntity) {
            // 主手和副手物品
            addItemIfNotEmpty(items, livingEntity.getMainHandStack());
            addItemIfNotEmpty(items, livingEntity.getOffHandStack());

            // 盔甲（可选）
            for (ItemStack stack : livingEntity.getArmorItems()) {
                addItemIfNotEmpty(items, stack);
            }
        }

        // 4. 其他实体（如掉落物）
        if (entity instanceof ItemEntity itemEntity) {
            addItemIfNotEmpty(items, itemEntity.getStack());
        }

        return items.isEmpty() ? null : String.join(", ", items);
    }

    private static void addItemIfNotEmpty(List<String> list, ItemStack stack) {
        if (!stack.isEmpty()) {
            list.add(formatItemStack(stack));
        }
    }

    private static String formatItemStack(ItemStack stack) {
        String displayName = stack.getName().getString();
        int count = stack.getCount();
        return String.format("\"%s\" x%d", displayName, count);
    }
}