package com.warpolitical.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.warpolitical.WarPoliticalMod;
import com.warpolitical.claim.ClaimEngine;
import com.warpolitical.model.Town;
import com.warpolitical.scenario.ScenarioParser;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

public class CommandRegistrar {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            dispatcher.register(CommandManager.literal("town")
                .then(CommandManager.literal("create")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            String name = StringArgumentType.getString(ctx, "name");

                            if (WarPoliticalMod.getInstance().getDataManager()
                                    .getTown(name).isPresent()) {
                                player.sendMessage(Text.literal("Город с таким именем уже существует!"));
                                return 0;
                            }

                            Town town = new Town(name, player.getUuid());
                            WarPoliticalMod.getInstance().getDataManager().addTown(town);
                            player.sendMessage(Text.literal("Город '" + name + "' создан!"));
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("info")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "name");
                            var optTown = WarPoliticalMod.getInstance().getDataManager()
                                    .getTown(name);
                            if (optTown.isEmpty()) {
                                ctx.getSource().sendMessage(Text.literal("Город не найден."));
                                return 0;
                            }
                            Town town = optTown.get();
                            ctx.getSource().sendMessage(Text.literal(
                                "=== " + town.getName() + " ===\n" +
                                "Жители: " + town.getResidents().size() + "\n" +
                                "Территория: " + town.getClaims().size() + " чанков\n" +
                                "Нация: " + (town.getNationName() != null ?
                                    town.getNationName() : "нет")
                            ));
                            return 1;
                        })
                    )
                )
                .then(CommandManager.literal("claim")
                    .then(CommandManager.argument("town", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            String townName = StringArgumentType.getString(ctx, "town");
                            ChunkPos chunkPos = player.getChunkPos();
                            String world = player.getWorld().getRegistryKey().getValue()
                                    .toString();

                            ClaimEngine.ClaimResult result = ClaimEngine.claim(
                                    townName, chunkPos.x, chunkPos.z, world,
                                    player.getUuid());

                            String message = switch (result) {
                                case SUCCESS -> "Чанк заклеймлен для " + townName + "!";
                                case TOWN_NOT_FOUND -> "Город не найден.";
                                case NOT_RESIDENT -> "Вы не житель этого города.";
                                case ALREADY_CLAIMED -> "Этот чанк уже заклеймлен.";
                                case LIMIT_REACHED -> "Лимит территории достигнут.";
                                default -> "Ошибка.";
                            };
                            player.sendMessage(Text.literal(message));
                            return result == ClaimEngine.ClaimResult.SUCCESS ? 1 : 0;
                        })
                    )
                )
            );

            dispatcher.register(CommandManager.literal("nation")
                .then(CommandManager.literal("create")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .then(CommandManager.argument("capital", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                String name = StringArgumentType.getString(ctx, "name");
                                String capital = StringArgumentType.getString(ctx, "capital");

                                var dm = WarPoliticalMod.getInstance().getDataManager();

                                if (dm.getNation(name).isPresent()) {
                                    player.sendMessage(Text.literal(
                                            "Нация с таким именем уже существует!"));
                                    return 0;
                                }

                                var optTown = dm.getTown(capital);
                                if (optTown.isEmpty()) {
                                    player.sendMessage(Text.literal("Город '" +
                                            capital + "' не найден."));
                                    return 0;
                                }

                                Town town = optTown.get();
                                if (!town.getMayor().equals(player.getUuid())) {
                                    player.sendMessage(Text.literal(
                                            "Вы должны быть мэром столицы!"));
                                    return 0;
                                }

                                var nation = new com.warpolitical.model.Nation(
                                        name, player.getUuid(), capital);
                                town.setNationName(name);
                                dm.addNation(nation);
                                dm.saveAll();

                                player.sendMessage(Text.literal(
                                        "Нация '" + name + "' создана! Столица: " +
                                        capital));
                                return 1;
                            })
                        )
                    )
                )
            );

            dispatcher.register(CommandManager.literal("scenario")
                .requires(source -> source.hasPermissionLevel(4))
                .then(CommandManager.literal("load")
                    .then(CommandManager.argument("file", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String file = StringArgumentType.getString(ctx, "file");
                            new ScenarioParser().loadScenario(file);
                            ctx.getSource().sendMessage(Text.literal(
                                    "Сценарий '" + file + "' загружен!"));
                            return 1;
                        })
                    )
                )
            );

        });

        WarPoliticalMod.LOGGER.info("Команды зарегистрированы.");
    }
}