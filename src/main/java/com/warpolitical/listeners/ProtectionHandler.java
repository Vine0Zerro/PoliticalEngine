package com.warpolitical.listeners;

import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.Nation;
import com.warpolitical.model.Town;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ProtectionHandler {

    private static final Map<UUID, String> lastTownCache = new HashMap<>();
    private static final Map<UUID, Long> messageCooldown = new HashMap<>();

    public static void register(WarPoliticalMod mod) {

        // Защита от ломания блоков
        PlayerBlockBreakEvents.BEFORE.register(
                (World world, PlayerEntity player, BlockPos pos,
                 BlockState state, BlockEntity blockEntity) -> {

            if (world.isClient()) return true;
            if (player.hasPermissionLevel(2)) return true;
            if (!isScenarioWorld(mod, world)) return true;

            if (!canModify(mod, player, pos)) {
                sendDeny(player, mod, pos);
                return false;
            }
            return true;
        });

        // Защита от установки/использования блоков
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (player.hasPermissionLevel(2)) return ActionResult.PASS;
            if (!isScenarioWorld(mod, world)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            if (!canModify(mod, player, pos)) {
                sendDeny(player, mod, pos);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // Защита от атаки блоков (начало ломания)
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (player.hasPermissionLevel(2)) return ActionResult.PASS;
            if (!isScenarioWorld(mod, world)) return ActionResult.PASS;

            if (!canModify(mod, player, pos)) {
                sendDeny(player, mod, pos);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        // Уведомления при перемещении (каждый тик сервера)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!isScenarioWorld(mod, player.getWorld())) continue;

                handleTerritoryNotification(mod, player);
            }
        });
    }

    private static void handleTerritoryNotification(WarPoliticalMod mod,
                                                      ServerPlayerEntity player) {
        int cx = player.getChunkPos().x;
        int cz = player.getChunkPos().z;
        String worldName = mod.getWorldManager().getWorldId(player.getServerWorld());

        Town town = mod.getDataManager().getTownAtChunk(worldName, cx, cz);
        String currentTownId = town != null ? town.getId() : null;
        String lastTownId = lastTownCache.get(player.getUuid());

        if (!Objects.equals(currentTownId, lastTownId)) {
            lastTownCache.put(player.getUuid(), currentTownId);

            if (town != null) {
                Nation nation = mod.getDataManager().getNation(town.getNationId());
                String nationName = nation != null ? nation.getName() : "";

                // ActionBar
                player.sendMessage(
                        Text.literal("⚐ " + town.getName() + " | " + nationName)
                                .formatted(Formatting.GOLD),
                        true);

                // Чат
                player.sendMessage(
                        Text.literal("Вы вошли в ")
                                .formatted(Formatting.GRAY)
                                .append(Text.literal(town.getName())
                                        .formatted(Formatting.YELLOW))
                                .append(Text.literal(" (" + nationName + ")")
                                        .formatted(Formatting.GREEN)),
                        false);
            } else {
                player.sendMessage(
                        Text.literal("🌿 Дикие земли")
                                .formatted(Formatting.DARK_GREEN),
                        true);
            }
        }
    }

    private static boolean isScenarioWorld(WarPoliticalMod mod, World world) {
        String active = mod.getDataManager().getActiveWorldName();
        if (active == null) return false;
        String worldName = world.getRegistryKey().getValue().getPath();
        return worldName.contains(active) || active.contains(worldName)
                || active.equals("overworld");
    }

    private static boolean canModify(WarPoliticalMod mod,
                                      PlayerEntity player, BlockPos pos) {
        String worldName = player.getWorld().getRegistryKey().getValue().getPath();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        return mod.getClaimEngine().canBuild(
                player.getName().getString(), worldName, chunkX, chunkZ);
    }

    private static void sendDeny(PlayerEntity player, WarPoliticalMod mod,
                                  BlockPos pos) {
        // Кулдаун чтобы не спамить
        long now = System.currentTimeMillis();
        Long last = messageCooldown.get(player.getUuid());
        if (last != null && now - last < 2000) return;
        messageCooldown.put(player.getUuid(), now);

        String worldName = player.getWorld().getRegistryKey().getValue().getPath();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        Town town = mod.getDataManager().getTownAtChunk(worldName, chunkX, chunkZ);
        String name = town != null ? town.getName() : "этой территории";

        player.sendMessage(
                Text.literal("❌ Нельзя строить на территории " + name)
                        .formatted(Formatting.RED),
                true);
    }
}
