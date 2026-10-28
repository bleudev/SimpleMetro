package com.bleudev.simplemetro.client

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.MetroLine
import com.bleudev.simplemetro.config.obj.MetroStation
import com.bleudev.simplemetro.util.idModOnServer
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.coordinates.LocalCoordinates
import net.minecraft.commands.arguments.coordinates.WorldCoordinates
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.server.permissions.Permissions
import net.minecraft.util.ARGB
import net.minecraft.world.phys.Vec3

@Environment(EnvType.CLIENT)
object MClientCommand {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>, context: CommandBuildContext) {
        dispatcher.register(command(ClientCommands.literal("m"), context))
        dispatcher.register(command(ClientCommands.literal("metro"), context))
    }

    private val dim: Identifier get() = (Minecraft.getInstance().level?.dimension() ?: ClientLevel.OVERWORLD).identifier()

    private fun command(
        current: LiteralArgumentBuilder<FabricClientCommandSource>,
        context: CommandBuildContext
    ): LiteralArgumentBuilder<FabricClientCommandSource> {
        return current
            .requires { !idModOnServer || it.permissions().hasPermission(Permissions.COMMANDS_ADMIN) }
            .then(ClientCommands
                .literal("station")
                .then(ClientCommands
                    .literal("add")
                    .then(ClientCommands
                        .argument("pos", BlockPosArgument.blockPos())
                        .then(ClientCommands
                            .argument("name", StringArgumentType.greedyString())
                            .executes { ctx ->
                                addStation(
                                    ctx.getClientBlockPos("pos"),
                                    StringArgumentType.getString(ctx, "name")
                                ).also { if (it == -1) {
                                    ctx.source.sendError(Component.translatable("commands.metro.station.add.error.position"))
                                } else {
                                    ctx.source.sendFeedback(Component.translatable("commands.metro.station.add.success", it))
                                }}
                            }
                        )
                    )
                )
                .then(ClientCommands
                    .literal("remove")
                    .then(stationIdArgument("id")
                        .executes { ctx ->
                            val id = IntegerArgumentType.getInteger(ctx, "id")
                            removeStation(id).also {
                                if (it == 1) {
                                    ctx.source.sendFeedback(Component.translatable("commands.metro.station.remove.success"))
                                } else {
                                    ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown", id))
                                }
                            }
                        }
                    )
                )
                .then(stationIdArgument("id")
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
                                        ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown"))
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
                                        ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown"))
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
                                    if (!status.unknown && s.startsWith(b.remainingLowerCase)) {
                                        b.suggest(s)
                                    }
                                }
                                b.buildFuture()
                            }
                            .executes { ctx ->
                                val status = MetroStation.Status.fromNameOrNull(StringArgumentType.getString(ctx, "status"))
                                if (status == null || status.unknown) {
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
                                            ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown"))
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
                                    if (t == null || SMetroInfo.getInstance(dim).stations.all { it.id != t }) {
                                        ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown", t.toString()))
                                        return@executes -1
                                    }
                                    t
                                }
                                setTransfers(IntegerArgumentType.getInteger(ctx, "id"), transfers).also {
                                    if (it == 1) {
                                        ctx.source.sendFeedback(Component.translatable("commands.metro.station.update.success"))
                                    } else {
                                        ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown"))
                                    }
                                }
                            }
                        )
                    )

                    .then(ClientCommands
                        .literal("place")
                        .then(lineIdArgument("line")
                            .then(ClientCommands
                                .literal("add")
                                .then(ClientCommands
                                    .literal("corner")
                                    .then(stationIdArgument("corner_id")
                                        .executes { ctx ->
                                            val id = IntegerArgumentType.getInteger(ctx, "id")
                                            val line = IntegerArgumentType.getInteger(ctx, "line")
                                            val cornerId = IntegerArgumentType.getInteger(ctx, "corner_id")
                                            placeStationAdd(
                                                id,
                                                line,
                                                listOf(cornerId)
                                            ).also {
                                                when (it) {
                                                    1 -> {
                                                        ctx.source.sendFeedback(Component.translatable("commands.metro.station.place.add.success", line))
                                                    }
                                                    2 -> {
                                                        ctx.source.sendFeedback(Component.translatable("commands.metro.station.place.add.success.line", line))
                                                    }
                                                    -1 -> {
                                                        ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown", id))
                                                    }
                                                    -2 -> {
                                                        ctx.source.sendError(Component.translatable("commands.metro.station.place.corner.error.corner_id", cornerId))
                                                    }
                                                    else -> if (it < 0) {
                                                        ctx.source.sendError(Component.translatable("commands.metro.generic.error.unknown", it))
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    .executes { ctx ->
                                        val id = IntegerArgumentType.getInteger(ctx, "id")
                                        val line = IntegerArgumentType.getInteger(ctx, "line")
                                        placeStationAdd(
                                            id,
                                            line,
                                            listOf()
                                        ).also {
                                            when (it) {
                                                1 -> {
                                                    ctx.source.sendFeedback(Component.translatable("commands.metro.station.place.add.success", line))
                                                }
                                                2 -> {
                                                    ctx.source.sendFeedback(Component.translatable("commands.metro.station.place.add.success.line", line))
                                                }
                                                -1 -> {
                                                    ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown", id))
                                                }
                                                else -> if (it < 0) {
                                                    ctx.source.sendError(Component.translatable("commands.metro.generic.error.unknown", it))
                                                }
                                            }
                                        }
                                    }
                                )
                                .then(ClientCommands
                                    .literal("between")
                                    .then(stationIdArgument("id1")
                                        .then(stationIdArgument("id2")
                                            .executes { ctx ->
                                                val id = IntegerArgumentType.getInteger(ctx, "id")
                                                val line = IntegerArgumentType.getInteger(ctx, "line")
                                                placeStationAdd(
                                                    id,
                                                    line,
                                                    listOf(
                                                        IntegerArgumentType.getInteger(ctx, "id1"),
                                                        IntegerArgumentType.getInteger(ctx, "id2")
                                                    )
                                                ).also {
                                                    when (it) {
                                                        1 -> {
                                                            ctx.source.sendFeedback(Component.translatable("commands.metro.station.place.add.success", line))
                                                        }
                                                        2 -> {
                                                            ctx.source.sendFeedback(Component.translatable("commands.metro.station.place.add.success.line", line))
                                                        }
                                                        -1 -> {
                                                            ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown", id))
                                                        }
                                                        -3 -> {
                                                            ctx.source.sendError(Component.translatable("commands.metro.station.place.add.between.error.not_neighbors"))
                                                        }
                                                        else -> if (it < 0) {
                                                            ctx.source.sendError(Component.translatable("commands.metro.generic.error.unknown", it))
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    )
                                )

                            )
                            .then(ClientCommands
                                .literal("remove")
                                .executes { ctx ->
                                    val id = IntegerArgumentType.getInteger(ctx, "id")
                                    val line = IntegerArgumentType.getInteger(ctx, "line")
                                    placeStationRemove(
                                        id,
                                        line
                                    ).also {
                                        when (it) {
                                            1 -> {
                                                ctx.source.sendFeedback(Component.translatable("commands.metro.station.place.remove.success", line))
                                            }
                                            -1 -> {
                                                ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown", id))
                                            }
                                            -2 -> {
                                                ctx.source.sendError(Component.translatable("commands.metro.generic.line.unknown", line))
                                            }
                                            -3 -> {
                                                ctx.source.sendError(Component.translatable("commands.metro.station.place.remove.error.not_placed", line))
                                            }
                                            else -> if (it < 0) {
                                                ctx.source.sendError(Component.translatable("commands.metro.generic.error.unknown", it))
                                            }
                                        }
                                    }
                                }
                            )
                        )
                    )
                )
            )
            .then(ClientCommands
                .literal("line")
                .then(lineIdArgument("id")
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
                                    if (t == null || SMetroInfo.getInstance(dim).stations.all { it.id != t }) {
                                        ctx.source.sendError(Component.translatable("commands.metro.generic.station.unknown", t.toString()))
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

    private fun stationIdArgument(name: String): RequiredArgumentBuilder<FabricClientCommandSource, Int> {
        return ClientCommands
            .argument(name, IntegerArgumentType.integer(1))
            .suggests { _, builder ->
                SMetroInfo.getInstance(dim).stations.forEach { station ->
                    if (station.id.toString().startsWith(builder.remainingLowerCase)) {
                        builder.suggest(station.id, Component.literal(station.name))
                    }
                }
                builder.buildFuture()
            }
    }
    private fun lineIdArgument(name: String): RequiredArgumentBuilder<FabricClientCommandSource, Int> {
        return ClientCommands
            .argument(name, IntegerArgumentType.integer(1))
            .suggests { _, builder ->
                SMetroInfo.getInstance(dim).lines.forEach { line ->
                    if (line.id.toString().startsWith(builder.remainingLowerCase)) {
                        builder.suggest(line.id, Component.literal(line.name).withColor(ARGB.color(1f, line.color)))
                    }
                }
                builder.buildFuture()
            }
    }

    private fun addStation(pos: BlockPos, name: String): Int {
        val info = SMetroInfo.getInstance(dim)
        val stations = ArrayList(info.stations)
        if (stations.all { !it.pos.atY(100).closerThan(pos.atY(100), 3.0) && it.pos.y > 3 }) {
            val id = (stations.maxOfOrNull { it.id } ?: 0) + 1
            stations.add(MetroStation(id, pos, name, listOf(), MetroStation.Status.OPEN))
            SMetroInfo.save(dim, SMetroInfo(stations.toList(), info.lines))
            return id
        } else {
            return -1
        }
    }

    private fun removeStation(id: Int): Int {
        val info = SMetroInfo.getInstance(dim)
        val stations = info.stations.toMutableList()
        if (stations.removeIf { it.id == id }) {
            val lines = info.lines.map { l ->
                l.stations = l.stations.filter { it != id }
                l
            }
            val st = stations.toList().map { s ->
                if (s.id != id) {
                    s.transfers = s.transfers.filter { it != id }
                }
                s
            }
            SMetroInfo.save(dim, SMetroInfo(st, lines))
            return 1
        } else {
            return -1
        }
    }

    private fun updateStation(id: Int, updater: (MetroStation) -> MetroStation): Int {
        val info = SMetroInfo.getInstance(dim)
        val stations = info.stations.toMutableList()
        val i = stations.indexOfFirst { it.id == id }
        if (i == -1) return -1
        else {
            stations[i] = updater(stations[i])
            SMetroInfo.save(dim, SMetroInfo(stations.toList(), info.lines))
            return 1
        }
    }

    private fun setTransfers(id: Int, transfers: List<Int>): Int {
        val info = SMetroInfo.getInstance(dim)
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
        SMetroInfo.save(dim, SMetroInfo(stations.toList(), info.lines))
        return 1
    }

    /**
     * 2 Done + New line created
     * 1 Done
     * -1 Station doesn't exist
     * -2 Around method for corners failed (id isn't corner)
     * -3 Between method ids isn't neighbors
     * */
    private fun placeStationAdd(id: Int, lineId: Int, betweenOrAround: List<Int>): Int {
        val info = SMetroInfo.getInstance(dim)
        val lines = info.lines.toMutableList()
        if (info.stations.none { it.id == id }) return -1
        val i = lines.indexOfFirst { it.id == lineId }
        var stations = mutableListOf<Int>()
        if (i != -1) stations = lines[i].stations.toMutableList()
        stations.remove(id) // Ensure this station isn't placed

        // Corner method
        if (betweenOrAround.isEmpty()) {
            if (stations.size < 2) stations.add(id)
            else {
                val pos1 = info.stations.first { it.id == stations.first() }.pos
                val pos2 = info.stations.first { it.id == stations.last() }.pos
                val pos0 = info.stations.first { it.id == id }.pos

                if (pos0.distSqr(pos1) <= pos0.distSqr(pos2)) {
                    stations.add(0, id)
                } else {
                    stations.add(id)
                }
            }
        }
        // Around method
        else {
            if (stations.size < 2) stations.add(id)
            else {
                // ONLY FOR CORNERS
                if (betweenOrAround.size == 1) {
                    if (stations.first() == betweenOrAround[0]) {
                        stations.add(0, id)
                    } else if (stations.last() == betweenOrAround[0]) {
                        stations.add(id)
                    } else return -2
                }
                else if (betweenOrAround.size == 2) {
                    val j = stations.indexOf(betweenOrAround.first())
                    val k = stations.indexOf(betweenOrAround.last())
                    val (j1, k1) = listOf(j, k).sorted()
                    if (k1-j1 == 1) {
                        stations.add(k1, id)
                    } else return -3
                }
            }
        }
        return addOrUpdateLine(lineId) {
            it.stations = stations.toList()
            it
        }
    }

    /**
     * 1 Done
     * -1 Station doesn't exist
     * -2 Line doesn't exist
     * -3 Stations isn't placed in this line
     * */
    private fun placeStationRemove(id: Int, lineId: Int): Int {
        val info = SMetroInfo.getInstance(dim)
        val lines = info.lines.toMutableList()
        if (info.stations.none { it.id == id }) return -1
        val i = lines.indexOfFirst { it.id == lineId }
        if (i == -1) return -2
        val stations = lines[i].stations.toMutableList()
        if (id !in stations) return -3
        stations.remove(id)
        lines[i].stations = stations.toList()
        SMetroInfo.save(dim, SMetroInfo(info.stations, lines.toList()))
        return 1
    }

    private fun addOrUpdateLine(lineId: Int, updater: (MetroLine) -> MetroLine): Int {
        val info = SMetroInfo.getInstance(dim)
        val lines = info.lines.toMutableList()
        val i = lines.indexOfFirst { it.id == lineId }
        if (i == -1) {
            val name = Component.translatable("smetro.line.name.default.id", lineId).string
            val nl = MetroLine(lineId, name, 0xffffff, listOf())
            lines.add(updater(nl))
            SMetroInfo.save(dim, SMetroInfo(info.stations, lines.toList()))
            return 2
        } else {
            lines[i] = updater(lines[i])
            SMetroInfo.save(dim, SMetroInfo(info.stations, lines.toList()))
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