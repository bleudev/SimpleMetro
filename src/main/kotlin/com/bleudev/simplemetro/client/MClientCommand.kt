package com.bleudev.simplemetro.client

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.obj.MetroLine
import com.bleudev.simplemetro.obj.MetroStation
import com.bleudev.simplemetro.util.idModOnServer
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
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

@Environment(EnvType.CLIENT)
object MClientCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, context: CommandBuildContext) {
        dispatcher.register(command(ClientCommands.literal("m"), context))
        dispatcher.register(command(ClientCommands.literal("metro"), context))
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
                            .argument("name", StringArgumentType.greedyString())
                            .executes { ctx ->
                                addStation(
                                    ctx.getClientBlockPos("pos"),
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
                .then(ClientCommands
                    .argument("id", IntegerArgumentType.integer(1))
                    .then(ClientCommands
                        .literal("pos")
                        .then(ClientCommands
                            .argument("pos", BlockPosArgument.blockPos())
                            .executes { ctx ->
                                return@executes updateStation(IntegerArgumentType.getInteger(ctx, "id")) {
                                    it.pos = ctx.getClientBlockPos("pos")
                                    it
                                }.also {
                                    if (it == 1) {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.station.update.success"))
                                    } else {
                                        ctx.source.sendError(Component.translatable("commands.metro.station.update.error.dont_exist"))
                                    }
                                }
                            }
                        )
                    )
                    .then(ClientCommands
                        .literal("name")
                        .then(ClientCommands
                            .argument("name", StringArgumentType.greedyString())
                            .executes { ctx ->
                                return@executes updateStation(IntegerArgumentType.getInteger(ctx, "id")) {
                                    it.name = StringArgumentType.getString(ctx, "name")
                                    it
                                }.also {
                                    if (it == 1) {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.station.update.success"))
                                    } else {
                                        ctx.source.sendError(Component.translatable("commands.metro.station.update.error.dont_exist"))
                                    }
                                }
                            }
                        )
                    )
                    .then(ClientCommands
                        .literal("status")
                        .then(ClientCommands
                            .argument("status", StringArgumentType.word())
                            .suggests { _, b ->
                                for (status in MetroStation.Status.entries) {
                                    val s = status.serializedName
                                    if (s.startsWith(b.remainingLowerCase)) {
                                        b.suggest(s)
                                    }
                                }
                                b.buildFuture()
                            }
                            .executes { ctx ->
                                val status = MetroStation.Status.fromNameOrNull(StringArgumentType.getString(ctx, "status"))
                                if (status == null) {
                                    ctx.source.sendError(Component.translatable("commands.metro.station.update.error.status"))
                                    return@executes -1
                                } else {
                                    return@executes updateStation(IntegerArgumentType.getInteger(ctx, "id")) {
                                        it.status = status
                                        it
                                    }.also {
                                        if (it == 1) {
                                            ctx.source.sendFeedback(Component.translatable("commands.metro.station.update.success"))
                                        } else {
                                            ctx.source.sendError(Component.translatable("commands.metro.station.update.error.dont_exist"))
                                        }
                                    }
                                }
                            }
                        )
                    )
                    .then(ClientCommands
                        .literal("transfers")
                        .then(ClientCommands
                            .argument("transfers", StringArgumentType.greedyString())
                            .executes { ctx ->
                                val transfers = StringArgumentType.getString(ctx, "transfers").split(" ").map { s ->
                                    val t = s.toIntOrNull()
                                    if (t == null || SMetroInfo.getInstance().stations.all { it.id != t }) {
                                        ctx.source.sendError(Component.translatable("commands.metro.stations.error.unknown", t.toString()))
                                        return@executes -1
                                    }
                                    t
                                }
                                setTransfers(IntegerArgumentType.getInteger(ctx, "id"), transfers).also {
                                    if (it == 1) {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.station.update.success"))
                                    } else {
                                        ctx.source.sendError(Component.translatable("commands.metro.station.update.error.dont_exist"))
                                    }
                                }
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
                            .argument("name", StringArgumentType.greedyString())
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
                                        ctx.source.sendError(Component.translatable("commands.metro.stations.error.unknown", t.toString()))
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
            stations.add(MetroStation(id, pos, name, listOf(), MetroStation.Status.OPEN))
        } else {
            return -1
        }
        SMetroInfo.save(SMetroInfo(stations.toList(), info.lines))
        return 1
    }

    private fun updateStation(id: Int, updater: (MetroStation) -> MetroStation): Int {
        val info = SMetroInfo.getInstance()
        val stations = info.stations.toMutableList()
        val i = stations.indexOfFirst { it.id == id }
        if (i == -1) return -1
        else {
            stations[i] = updater(stations[i])
            SMetroInfo.save(SMetroInfo(stations.toList(), info.lines))
            return 1
        }
    }

    private fun setTransfers(id: Int, transfers: List<Int>): Int {
        val info = SMetroInfo.getInstance()
        val stations = info.stations.toMutableList()

        fun update(uid: Int, updater: (MetroStation) -> MetroStation) {
            val j = stations.indexOfFirst { it.id == uid }
            if (j == -1) return
            else {
                stations[j] = updater(stations[j])
            }
        }

        val i = stations.indexOfFirst { it.id == id }
        if (i == -1) return -1
        stations[i].transfers.forEach { id2 ->
            if (id2 !in transfers) {
                update(id2) {
                    val tr = it.transfers.toMutableList()
                    if (id in tr) tr.remove(id)
                    it.transfers = tr.toList()
                    it
                }
            }
        }
        transfers.forEach { id2 ->
            update(id2) {
                val tr = it.transfers.toMutableList()
                if (id !in tr) tr.add(id)
                it.transfers = tr.toList()
                it
            }
        }
        update(id) {
            it.transfers = transfers
            it
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

    private fun CommandContext<FabricClientCommandSource>.getClientBlockPos(name: String): BlockPos {
        return getArgument(name, Coordinates::class.java).getClientBlockPos(source)
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