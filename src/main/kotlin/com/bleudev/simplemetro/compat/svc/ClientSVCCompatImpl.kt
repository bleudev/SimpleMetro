package com.bleudev.simplemetro.compat.svc

import com.bleudev.simplemetro.compat.ClientSVCCompat
import de.maxhenkel.voicechat.VoicechatClient
import de.maxhenkel.voicechat.voice.client.ClientManager
import net.minecraft.client.Minecraft

class ClientSVCCompatImpl : ClientSVCCompat {
    override fun isInGroup(): Boolean {
        return !VoicechatClient.CLIENT_CONFIG.hideIcons.get() && ClientManager.getPlayerStateManager().isInGroup(Minecraft.getInstance().player)
    }
}