package com.warpolitical.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.warpolitical.WarPoliticalMod;
import com.warpolitical.model.Nation;
import com.warpolitical.model.Town;
import com.warpolitical.scenario.ScenarioParser.ScenarioResult;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandRegistrar {

    public static void register(WarPoliticalMod mod) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("wpe")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(ctx -> showHelp(ctx))

                    .then(literal("load")
                        .then(argument("scenario", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                if (mod.getScenarioParser() != null) {
                                    mod.getScenarioParser().getAvailableScenarios()
                                            .forEach(builder::suggest);
                                }
                                return builder.buildFuture();
                            })
                            .executes(ctx -> loadScenario(ctx, mod))))

                    .then(literal("unload")
                        .executes(ctx -> unloadScenario(ctx, mod)))

                    .then(literal("list")
                        .executes(ctx -> listScenarios(ctx, mod)))

                    .then(literal("status")
                        .executes(ctx -> showStatus(ctx, mod)))

                    .then(literal("tp")
                        .then(argument("target", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                mod.getDataManager().getTowns().values()
                                        .forEach(t -> builder.suggest(t.getId()));
                                mod.getDataManager().getNations().keySet()
                                        .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> teleport(ctx, mod))))

                    .then(literal("info")
                        .then(literal("nation")
                            .then(argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    mod.getDataManager().getNations().keySet()
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> infoNation(ctx, mod))))
                        .then(literal("town")
                            .then(argument("id", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    mod.getDataManager().getTowns().values()
                                            .forEach(t -> builder.suggest(t.getId()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> infoTown(ctx, mod)))))

                    .then(literal("chunks")
                        .executes(ctx -> chunkInfo(ctx, mod)))

                    .then(literal("resident")
                        .then(literal("add")
                            .then(argument("town", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    mod.getDataManager().getTowns().values()
                                            .forEach(t -> builder.suggest(t.getId()));
                                    return builder.buildFuture();
                                })
                                .then(argument("player", StringArgumentType.word())
                                    .executes(ctx -> addResident(ctx, mod)))))
                        .then(literal("remove")
                            .then(argument("town", StringArgumentType.word())
                                .then(argument("player", StringArgumentType.word())
                                    .executes(ctx -> removeResident(ctx, mod)))))
                        .then(literal("list")
                            .then(argument("town", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    mod.getDataManager().getTowns().values()
                                            .forEach(t -> builder.suggest(t.getId()));
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> listResidents(ctx, mod)))))

                    .then(literal("nation")
                        .then(literal("war")
                            .then(argument("nation1", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    mod.getDataManager().getNations().keySet()
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(argument("nation2", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        mod.getDataManager().getNations().keySet()
                                                .forEach(builder::suggest);
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> nationWar(ctx, mod)))))
                        .then(literal("peace")
                            .then(argument("nation1", StringArgumentType.word())
                                .then(argument("nation2", StringArgumentType.word())
                                    .executes(ctx -> nationPeace(ctx, mod)))))
                        .then(literal("ally")
                            .then(argument("nation1", StringArgumentType.word())
                                .then(argument("nation2", StringArgumentType.word())
                                    .executes(ctx -> nationAlly(ctx, mod))))))

                    .then(literal("town")
                        .then(literal("setpvp")
                            .then(argument("townid", StringArgumentType.word())
                                .then(argument("value", StringArgumentType.word())
                                    .executes(ctx -> townSetPvp(ctx, mod)))))
                        .then(literal("setexplosions")
                            .then(argument("townid", StringArgumentType.word())
                                .then(argument("value", StringArgumentType.word())
                                    .executes(ctx -> townSetExplosions(ctx, mod))))))

                    .then(literal("bluemap")
                        .then(literal("render")
                            .executes(ctx -> {
                                mod.getBlueMapIntegration().renderAll();
                                ctx.getSource().sendMessage(
                                        Text.literal("✅ BlueMap обновлён")
                                                .formatted(Formatting.GREEN));
                                return 1;
                            }))
                        .then(literal("clear")
                            .executes(ctx -> {
                                mod.getBlueMapIntegration().shutdown();
                                ctx.getSource().sendMessage(
                                        Text.literal("✅ Маркеры очищены")
                                                .formatted(Formatting.GREEN));
                                return 1;
                            })))

                    .then(literal("reload")
                        .executes(ctx -> {
                            mod.getDataManager().loadAll();
                            ctx.getSource().sendMessage(
                                    Text.literal("✅ Данные перезагружены")
                                            .formatted(Formatting.GREEN));
                            if (mod.getBlueMapIntegration().isInitialized()) {
                                mod.getBlueMapIntegration().renderAll();
                            }
                            return 1;
                        }))
            );
        });
    }

    // ═══════════════════════════════════════
    //  ОБРАБОТЧИКИ
    // ═══════════════════════════════════════

    private static int showHelp(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource s = ctx.getSource();
        s.sendMessage(Text.literal("═══ WarPoliticalEngine ═══").formatted(Formatting.GOLD));
        s.sendMessage(Text.literal("/wpe load <сценарий>").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe unload").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe list").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe status").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe tp <город|нация>").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe info nation|town <id>").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe chunks").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe resident add|remove|list <город> [игрок]").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe nation war|peace|ally <н1> <н2>").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe town setpvp|setexplosions <город> <true|false>").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe bluemap render|clear").formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("/wpe reload").formatted(Formatting.YELLOW));
        return 1;
    }

    private static int loadScenario(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String name = StringArgumentType.getString(ctx, "scenario");
        ServerCommandSource s = ctx.getSource();

        if (mod.getDataManager().getActiveWorldName() != null) {
            s.sendMessage(Text.literal("⚠ Сценарий уже загружен! /wpe unload")
                    .formatted(Formatting.YELLOW));
            return 0;
        }

        s.sendMessage(Text.literal("⏳ Загрузка '" + name + "'...").formatted(Formatting.GREEN));

        ScenarioResult result = mod.getScenarioParser().loadAndApply(name);

        if (result.success) {
            s.sendMessage(Text.literal("✅ Загружено! Наций: " + result.nationCount
                    + " | Городов: " + result.townCount
                    + " | Чанков: " + result.chunkCount).formatted(Formatting.GREEN));

            mod.getServer().getPlayerManager().broadcast(
                    Text.literal("🌍 Новый вайп: " + result.scenarioName + "!")
                            .formatted(Formatting.GOLD), false);
        } else {
            s.sendMessage(Text.literal("❌ " + result.message).formatted(Formatting.RED));
        }

        return result.success ? 1 : 0;
    }

    private static int unloadScenario(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        if (mod.getDataManager().getActiveWorldName() == null) {
            ctx.getSource().sendMessage(
                    Text.literal("Нет активного сценария").formatted(Formatting.RED));
            return 0;
        }

        mod.getBlueMapIntegration().shutdown();
        mod.getDataManager().clearAll();
        mod.getDataManager().saveAll();

        ctx.getSource().sendMessage(
                Text.literal("✅ Сценарий выгружен").formatted(Formatting.GREEN));
        return 1;
    }

    private static int listScenarios(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        List<String> list = mod.getScenarioParser().getAvailableScenarios();
        ctx.getSource().sendMessage(
                Text.literal("═══ Сценарии ═══").formatted(Formatting.GOLD));

        if (list.isEmpty()) {
            ctx.getSource().sendMessage(
                    Text.literal("  Нет сценариев").formatted(Formatting.GRAY));
        } else {
            for (String sc : list) {
                ctx.getSource().sendMessage(
                        Text.literal("  • " + sc).formatted(Formatting.WHITE));
            }
        }
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        ServerCommandSource s = ctx.getSource();
        String world = mod.getDataManager().getActiveWorldName();

        s.sendMessage(Text.literal("═══ WPE Status ═══").formatted(Formatting.GOLD));
        s.sendMessage(Text.literal("Мир: " + (world != null ? world : "не загружен"))
                .formatted(world != null ? Formatting.GREEN : Formatting.RED));
        s.sendMessage(Text.literal("BlueMap: "
                + (mod.getBlueMapIntegration().isInitialized() ? "✅" : "❌"))
                .formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Наций: " + mod.getDataManager().getNations().size())
                .formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Городов: " + mod.getDataManager().getTowns().size())
                .formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Чанков: " + mod.getDataManager().getClaimedChunks().size())
                .formatted(Formatting.YELLOW));

        if (s.getPlayer() != null) {
            ServerPlayerEntity p = s.getPlayer();
            int cx = p.getChunkPos().x;
            int cz = p.getChunkPos().z;
            String wn = mod.getWorldManager().getWorldId(p.getServerWorld());

            Town town = mod.getDataManager().getTownAtChunk(wn, cx, cz);
            Nation nation = mod.getDataManager().getNationAtChunk(wn, cx, cz);

            s.sendMessage(Text.literal("── Текущий чанк [" + cx + ", " + cz + "] ──")
                    .formatted(Formatting.GRAY));
            s.sendMessage(Text.literal("Город: " + (town != null ? town.getName() : "—"))
                    .formatted(Formatting.WHITE));
            s.sendMessage(Text.literal("Нация: " + (nation != null ? nation.getName() : "—"))
                    .formatted(Formatting.WHITE));
        }

        return 1;
    }

    private static int teleport(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendMessage(Text.literal("Только для игроков").formatted(Formatting.RED));
            return 0;
        }

        String target = StringArgumentType.getString(ctx, "target");
        Town town = findTown(mod, target);

        if (town == null) {
            Nation nation = mod.getDataManager().getNation(target);
            if (nation != null && nation.getCapitalTownId() != null) {
                town = mod.getDataManager().getTown(nation.getCapitalTownId());
            }
        }

        if (town == null) {
            ctx.getSource().sendMessage(
                    Text.literal("❌ Не найдено: " + target).formatted(Formatting.RED));
            return 0;
        }

        ServerWorld world = findWorld(mod, town.getWorldName());
        double x = town.getCenterX() * 16 + 8;
        double z = town.getCenterZ() * 16 + 8;
        int y = world.getTopY(Heightmap.Type.WORLD_SURFACE, (int) x, (int) z) + 1;

        player.teleport(world, x, y, z, 0, 0);
        ctx.getSource().sendMessage(
                Text.literal("✅ Телепортация в " + town.getName()).formatted(Formatting.GREEN));
        return 1;
    }

    private static int chunkInfo(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        int cx = player.getChunkPos().x;
        int cz = player.getChunkPos().z;
        String wn = mod.getWorldManager().getWorldId(player.getServerWorld());

        Town town = mod.getDataManager().getTownAtChunk(wn, cx, cz);
        Nation nation = mod.getDataManager().getNationAtChunk(wn, cx, cz);
        boolean claimed = mod.getDataManager().isChunkClaimed(wn, cx, cz);

        ctx.getSource().sendMessage(
                Text.literal("── Чанк [" + cx + ", " + cz + "] ──").formatted(Formatting.YELLOW));
        ctx.getSource().sendMessage(
                Text.literal("Город: " + (town != null ? town.getName() : "Дикие земли"))
                        .formatted(Formatting.WHITE));
        ctx.getSource().sendMessage(
                Text.literal("Нация: " + (nation != null ? nation.getName() : "—"))
                        .formatted(Formatting.WHITE));
        ctx.getSource().sendMessage(
                Text.literal("Запривачен: " + (claimed ? "✅" : "❌"))
                        .formatted(Formatting.WHITE));
        return 1;
    }

    private static int infoNation(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String id = StringArgumentType.getString(ctx, "id");
        Nation nation = mod.getDataManager().getNation(id);
        if (nation == null) {
            ctx.getSource().sendMessage(
                    Text.literal("❌ Нация не найдена").formatted(Formatting.RED));
            return 0;
        }

        Map<String, Town> towns = mod.getDataManager().getTowns();
        ServerCommandSource s = ctx.getSource();

        s.sendMessage(Text.literal("═══ " + nation.getName() + " ═══").formatted(Formatting.GOLD));
        s.sendMessage(Text.literal("Лидер: " + nation.getLeaderName()).formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Строй: " + nation.getGovernmentType()).formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Городов: " + nation.getTownIds().size()).formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Чанков: " + nation.getTotalChunks(towns) + "/" + Nation.MAX_CHUNKS)
                .formatted(Formatting.YELLOW));

        for (String tid : nation.getTownIds()) {
            Town t = towns.get(tid);
            s.sendMessage(Text.literal("  • " + (t != null ? t.getName() : tid)
                    + " (" + (t != null ? t.getChunkCount() : 0) + " чанков)")
                    .formatted(Formatting.GRAY));
        }

        if (!nation.getAllies().isEmpty()) {
            s.sendMessage(Text.literal("Союзники: " + String.join(", ", nation.getAllies()))
                    .formatted(Formatting.GREEN));
        }
        if (!nation.getEnemies().isEmpty()) {
            s.sendMessage(Text.literal("Враги: " + String.join(", ", nation.getEnemies()))
                    .formatted(Formatting.RED));
        }
        if (nation.isAtWar()) {
            s.sendMessage(Text.literal("⚔ В СОСТОЯНИИ ВОЙНЫ").formatted(Formatting.DARK_RED));
        }

        return 1;
    }

    private static int infoTown(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String id = StringArgumentType.getString(ctx, "id");
        Town town = findTown(mod, id);
        if (town == null) {
            ctx.getSource().sendMessage(
                    Text.literal("❌ Город не найден").formatted(Formatting.RED));
            return 0;
        }

        Nation nation = mod.getDataManager().getNation(town.getNationId());
        ServerCommandSource s = ctx.getSource();

        s.sendMessage(Text.literal("═══ " + town.getName() + " ═══").formatted(Formatting.GOLD));
        s.sendMessage(Text.literal("Нация: " + (nation != null ? nation.getName() : "—"))
                .formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Мэр: " + town.getMayorName()).formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Центр: [" + town.getCenterX() + ", " + town.getCenterZ() + "]")
                .formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Чанков: " + town.getChunkCount() + "/" + Town.MAX_CHUNKS)
                .formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("PVP: " + (town.isPvpEnabled() ? "✅" : "❌"))
                .formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Взрывы: " + (town.isExplosionsEnabled() ? "✅" : "❌"))
                .formatted(Formatting.YELLOW));
        s.sendMessage(Text.literal("Резидентов: " + town.getResidents().size())
                .formatted(Formatting.YELLOW));

        return 1;
    }

    private static int addResident(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String townId = StringArgumentType.getString(ctx, "town");
        String playerName = StringArgumentType.getString(ctx, "player");
        Town town = findTown(mod, townId);
        if (town == null) {
            ctx.getSource().sendMessage(Text.literal("❌ Город не найден").formatted(Formatting.RED));
            return 0;
        }
        town.getResidents().add(playerName);
        mod.getDataManager().saveAll();
        ctx.getSource().sendMessage(
                Text.literal("✅ " + playerName + " добавлен в " + town.getName())
                        .formatted(Formatting.GREEN));
        return 1;
    }

    private static int removeResident(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String townId = StringArgumentType.getString(ctx, "town");
        String playerName = StringArgumentType.getString(ctx, "player");
        Town town = findTown(mod, townId);
        if (town == null) {
            ctx.getSource().sendMessage(Text.literal("❌ Город не найден").formatted(Formatting.RED));
            return 0;
        }
        town.getResidents().remove(playerName);
        mod.getDataManager().saveAll();
        ctx.getSource().sendMessage(
                Text.literal("✅ " + playerName + " удалён из " + town.getName())
                        .formatted(Formatting.GREEN));
        return 1;
    }

    private static int listResidents(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String townId = StringArgumentType.getString(ctx, "town");
        Town town = findTown(mod, townId);
        if (town == null) {
            ctx.getSource().sendMessage(Text.literal("❌ Город не найден").formatted(Formatting.RED));
            return 0;
        }
        ctx.getSource().sendMessage(
                Text.literal("Резиденты " + town.getName() + ":").formatted(Formatting.YELLOW));
        if (town.getResidents().isEmpty()) {
            ctx.getSource().sendMessage(Text.literal("  Пусто").formatted(Formatting.GRAY));
        } else {
            for (String r : town.getResidents()) {
                ctx.getSource().sendMessage(Text.literal("  • " + r).formatted(Formatting.WHITE));
            }
        }
        return 1;
    }

    private static int nationWar(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String id1 = StringArgumentType.getString(ctx, "nation1");
        String id2 = StringArgumentType.getString(ctx, "nation2");
        Nation n1 = mod.getDataManager().getNation(id1);
        Nation n2 = mod.getDataManager().getNation(id2);
        if (n1 == null || n2 == null) {
            ctx.getSource().sendMessage(Text.literal("❌ Нация не найдена").formatted(Formatting.RED));
            return 0;
        }
        n1.getEnemies().add(id2);
        n2.getEnemies().add(id1);
        n1.getAllies().remove(id2);
        n2.getAllies().remove(id1);
        n1.setAtWar(true);
        n2.setAtWar(true);
        mod.getDataManager().saveAll();
        if (mod.getBlueMapIntegration().isInitialized()) mod.getBlueMapIntegration().renderAll();

        mod.getServer().getPlayerManager().broadcast(
                Text.literal("⚔ " + n1.getName() + " объявила войну " + n2.getName() + "!")
                        .formatted(Formatting.DARK_RED), false);
        return 1;
    }

    private static int nationPeace(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String id1 = StringArgumentType.getString(ctx, "nation1");
        String id2 = StringArgumentType.getString(ctx, "nation2");
        Nation n1 = mod.getDataManager().getNation(id1);
        Nation n2 = mod.getDataManager().getNation(id2);
        if (n1 == null || n2 == null) {
            ctx.getSource().sendMessage(Text.literal("❌ Нация не найдена").formatted(Formatting.RED));
            return 0;
        }
        n1.getEnemies().remove(id2);
        n2.getEnemies().remove(id1);
        if (n1.getEnemies().isEmpty()) n1.setAtWar(false);
        if (n2.getEnemies().isEmpty()) n2.setAtWar(false);
        mod.getDataManager().saveAll();
        if (mod.getBlueMapIntegration().isInitialized()) mod.getBlueMapIntegration().renderAll();

        mod.getServer().getPlayerManager().broadcast(
                Text.literal("🕊 " + n1.getName() + " и " + n2.getName() + " заключили мир!")
                        .formatted(Formatting.GREEN), false);
        return 1;
    }

    private static int nationAlly(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String id1 = StringArgumentType.getString(ctx, "nation1");
        String id2 = StringArgumentType.getString(ctx, "nation2");
        Nation n1 = mod.getDataManager().getNation(id1);
        Nation n2 = mod.getDataManager().getNation(id2);
        if (n1 == null || n2 == null) {
            ctx.getSource().sendMessage(Text.literal("❌ Нация не найдена").formatted(Formatting.RED));
            return 0;
        }
        n1.getAllies().add(id2);
        n2.getAllies().add(id1);
        n1.getEnemies().remove(id2);
        n2.getEnemies().remove(id1);
        mod.getDataManager().saveAll();
        if (mod.getBlueMapIntegration().isInitialized()) mod.getBlueMapIntegration().renderAll();

        mod.getServer().getPlayerManager().broadcast(
                Text.literal("🤝 " + n1.getName() + " и " + n2.getName() + " стали союзниками!")
                        .formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int townSetPvp(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String townId = StringArgumentType.getString(ctx, "townid");
        String value = StringArgumentType.getString(ctx, "value");
        Town town = findTown(mod, townId);
        if (town == null) {
            ctx.getSource().sendMessage(Text.literal("❌ Город не найден").formatted(Formatting.RED));
            return 0;
        }
        town.setPvpEnabled(Boolean.parseBoolean(value));
        mod.getDataManager().saveAll();
        ctx.getSource().sendMessage(
                Text.literal("✅ PVP в " + town.getName() + " → " + value)
                        .formatted(Formatting.GREEN));
        return 1;
    }

    private static int townSetExplosions(CommandContext<ServerCommandSource> ctx, WarPoliticalMod mod) {
        String townId = StringArgumentType.getString(ctx, "townid");
        String value = StringArgumentType.getString(ctx, "value");
        Town town = findTown(mod, townId);
        if (town == null) {
            ctx.getSource().sendMessage(Text.literal("❌ Город не найден").formatted(Formatting.RED));
            return 0;
        }
        town.setExplosionsEnabled(Boolean.parseBoolean(value));
        mod.getDataManager().saveAll();
        ctx.getSource().sendMessage(
                Text.literal("✅ Взрывы в " + town.getName() + " → " + value)
                        .formatted(Formatting.GREEN));
        return 1;
    }

    // ═══════════════════════════════════════
    //  УТИЛИТЫ
    // ═══════════════════════════════════════

    private static Town findTown(WarPoliticalMod mod, String query) {
        Town town = mod.getDataManager().getTown(query);
        if (town != null) return town;
        for (Town t : mod.getDataManager().getTowns().values()) {
            if (t.getName().equalsIgnoreCase(query)
                    || t.getId().equalsIgnoreCase(query)) {
                return t;
            }
        }
        return null;
    }

    private static ServerWorld findWorld(WarPoliticalMod mod, String worldName) {
        ServerWorld world = mod.getWorldManager().getWorldByName(worldName);
        return world != null ? world : mod.getServer().getOverworld();
    }
}
