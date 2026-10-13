package com.bleudev.simplemetro.util

import com.bleudev.simplemetro.custom.network.protocol.common.custom.ServerboundMetroInfoEdit
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.loader.api.FabricLoader

internal val isClientSide get() = FabricLoader.getInstance().environmentType == EnvType.CLIENT
internal val idModOnServer get() = ClientPlayNetworking.canSend(ServerboundMetroInfoEdit.type)