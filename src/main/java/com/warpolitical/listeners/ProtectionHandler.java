package com.warpolitical.listeners;

import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.Town;
import com.warpolitical.world.WorldManager;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

import java.util.Optional;

public class ProtectionHandler {

    public static void register() {

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (serverPlayer.hasPermissionLevel(4)) return ActionResult.PASS;

            int chunkX = WorldManager.blockToChunk(pos.getX());
            int chunkZ = WorldManager.blockToChunk(pos.getZ());
            String worldName = world.getRegistryKey().getValue().toString();

            Optional<Town> optTown = WarPoliticalMod.getInstance()
                    .getWorldManager().getTownAt(chunkX, chunkZ, worldName);

            if (optTown.isPresent()) {
                Town town = optTown.get();
                if (!town.isResident(serverPlayer.getUuid())) {
                    serverPlayer.sendMessage(
                        Text.literal("Эта территория принадлежит городу '" +
                                town.getName() + "'!"), true);
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
            if (serverPlayer.hasPermissionLevel(4)) return ActionResult.PASS;

            var pos = hitResult.getBlockPos();
            int chunkX = WorldManager.blockToChunk(pos.getX());
            int chunkZ = WorldManager.blockToChunk(pos.getZ());
            String worldName = world.getRegistryKey().getValue().toString();

            Optional<Town> optTown = WarPoliticalMod.getInstance()
                    .getWorldManager().getTownAt(chunkX, chunkZ, worldName);

            if (optTown.isPresent()) {
                Town town = optTown.get();
                if (!town.isResident(serverPlayer.getUuid())) {
                    serverPlayer.sendMessage(
                        Text.literal("Эта территория принадлежит городу '" +
                                town.getName() + "'!"), true);
                    return ActionResult.FAIL;
                }
            }

            return ActionResult.PASS;
        });

        WarPoliticalMod.LOGGER.info("Обработчики защиты зарегистрированы.");
    }
}