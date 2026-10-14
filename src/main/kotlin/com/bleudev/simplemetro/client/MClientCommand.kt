package com.bleudev.simplemetro.client

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.obj.MetroLine
import com.bleudev.simplemetro.obj.MetroStation
import com.bleudev.simplemetro.util.idModOnServer
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.LocalCoordinates
import net.minecraft.commands.arguments.coordinates.WorldCoordinates
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.Permissions
import net.minecraft.world.phys.Vec3

object MClientCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, context: CommandBuildContext) {
        val m = dispatcher.register(command(ClientCommands.literal("m"), context))
        dispatcher.register(ClientCommands.literal("metro").redirect(m))
    }

    private fun command(
        current: LiteralArgumentBuilder<FabricClientCommandSource>,
        context: CommandBuildContext
    ): LiteralArgumentBuilder<FabricClientCommandSource> {
        return current
            .requires { !idModOnServer || it.permissions().hasPermission(Permissions.COMMANDS_ADMIN) }
            .then(ClientCommands
                .literal("station")
                .then(ClientCommands
                    .literal("create")
                    .then(ClientCommands
                        .argument("pos", BlockPosArgument.blockPos())
                        .then(ClientCommands
                            .argument("name", StringArgumentType.string())
                            .executes { ctx ->
                                addStation(
                                    ctx.getArgument("pos", Coordinates::class.java).getClientBlockPos(ctx.source),
                                    StringArgumentType.getString(ctx, "name")
                                ).also { if (it == 1) {
                                    ctx.source.sendFeedback(Component.translatable("commands.metro.station.create.success"))
                                } else {
                                    ctx.source.sendError(Component.translatable("commands.metro.station.create.error.position"))
                                } }
                            }
                        )
                    )
                )
            )
            .then(ClientCommands
                .literal("line")
                .then(ClientCommands
                    .argument("id", IntegerArgumentType.integer(1))
                    .then(ClientCommands
                        .literal("name")
                        .then(ClientCommands
                            .argument("name", StringArgumentType.string())
                            .executes { ctx ->
                                addOrUpdateLine(IntegerArgumentType.getInteger(ctx, "id")) {
                                    it.name = StringArgumentType.getString(ctx, "name")
                                    it
                                }.also {
                                    if (it == 1) {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.line.success.updated"))
                                    } else {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.line.success.new"))
                                    }
                                }
                            }
                        )
                    )
                    .then(ClientCommands
                        .literal("color")
                        .then(ClientCommands
                            .argument("color", StringArgumentType.word())
                            .executes { ctx ->
                                var col: Int
                                try {
                                    @Suppress("LocalVariableName")
                                    val _col = StringArgumentType.getString(ctx, "color")
                                    if (_col.length != 6) throw IllegalArgumentException()
                                    col = _col.hexToInt()
                                } catch (_: IllegalArgumentException) {
                                    ctx.source.sendError(Component.translatable("commands.metro.line.error.color"))
                                    return@executes -1
                                }

                                addOrUpdateLine(IntegerArgumentType.getInteger(ctx, "id")) {
                                    it.color = col
                                    it
                                }.also {
                                    if (it == 1) {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.line.success.updated"))
                                    } else {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.line.success.new"))
                                    }
                                }
                            }
                        )
                    )
                    .then(ClientCommands
                        .literal("stations")
                        .then(ClientCommands
                            .argument("stations", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val st = StringArgumentType.getString(ctx, "stations").split(" ").map { s ->
                                    val t = s.toIntOrNull()
                                    if (t == null || SMetroInfo.getInstance().stations.all { it.id != t }) {
                                        ctx.source.sendError(Component.translatable("commands.metro.line.stations.error.unknown", t.toString()))
                                        return@executes -1
                                    }
                                    t
                                }
                                addOrUpdateLine(IntegerArgumentType.getInteger(ctx, "id")) {
                                    it.stations = st
                                    it
                                }.also {
                                    if (it == 1) {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.line.success.updated"))
                                    } else {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.line.success.new"))
                                    }
                                }
                            }
                        )
                    )
                )
            )
    }

    private fun addStation(pos: BlockPos, name: String): Int {
        val info = SMetroInfo.getInstance()
        val stations = ArrayList(info.stations)
        if (stations.all { !it.pos.atY(100).closerThan(pos.atY(100), 3.0) && it.pos.y > 3 }) {
            val id = (stations.maxOfOrNull { it.id } ?: 0) + 1
            stations.add(MetroStation(id, pos, name, listOf()))
        } else {
            return -1
        }
        SMetroInfo.save(SMetroInfo(stations.toList(), info.lines))
        return 1
    }

    private fun addOrUpdateLine(lineId: Int, updater: (MetroLine) -> MetroLine): Int {
        val info = SMetroInfo.getInstance()
        val lines = info.lines.toMutableList()
        val i = lines.indexOfFirst { it.id == lineId }
        if (i == -1) {
            val name = Component.translatable("smetro.line.name.default").string
            val nl = MetroLine((lines.maxOfOrNull { it.id } ?: 0) + 1, name, 0xffffff, listOf())
            lines.add(updater(nl))
            SMetroInfo.save(SMetroInfo(info.stations, lines.toList()))
            return 2
        } else {
            lines[i] = updater(lines[i])
            SMetroInfo.save(SMetroInfo(info.stations, lines.toList()))
            return 1
        }
    }

    private fun Coordinates.getClientBlockPos(sender: FabricClientCommandSource): BlockPos {
        val base = sender.position
        val almostAns = when (this) {
            is WorldCoordinates -> Vec3(x.get(base.x), y.get(base.y), z.get(base.z))
            is LocalCoordinates -> apply(base, sender.rotation)
            else -> error("Coordinates is neither WorldCoordinates nor LocalCoordinates.")
        }
        return BlockPos.containing(almostAns)
    }
}