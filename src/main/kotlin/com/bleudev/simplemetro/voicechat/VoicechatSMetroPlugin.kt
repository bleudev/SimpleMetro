package com.bleudev.simplemetro.voicechat

import com.bleudev.simplemetro.SMetro
import de.maxhenkel.voicechat.api.VoicechatApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.VoicechatServerApi
import de.maxhenkel.voicechat.api.VolumeCategory
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO


class VoicechatSMetroPlugin : VoicechatPlugin {
    override fun getPluginId(): String = SMetro.ID
    override fun initialize(api: VoicechatApi) {
        vcApi = api
    }
    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(VoicechatServerStartedEvent::class.java, ::onServerStarted)
    }

    private fun onServerStarted(event: VoicechatServerStartedEvent) {
        vcServerApi = event.voicechat
        announcements = vcServerApi!!.volumeCategoryBuilder()
            .setId(ANNOUNCEMENTS_CATEGORY)
            .setName("Announcements")
            .setNameTranslationKey("smetro.voicechat.category.announcements")
            .setDescription("Arrival / Departure announcements")
            .setDescriptionTranslationKey("smetro.voicechat.category.announcements.description")
            .setIcon(getIcon("category_announcements.png"))
            .build()

        vcServerApi!!.registerVolumeCategory(announcements)
    }

    private fun getIcon(path: String?): Array<IntArray>? {
        try {
            val resources = SMetro::class.java.classLoader.getResources(path)
            while (resources.hasMoreElements()) {
                val bufferedImage: BufferedImage = ImageIO.read(resources.nextElement().openStream())
                if (bufferedImage.width != 16 || bufferedImage.height != 16) {
                    continue
                }
                val image = Array(16) { IntArray(16) }
                for (x in 0..<bufferedImage.width) {
                    for (y in 0..<bufferedImage.height) {
                        image[x][y] = bufferedImage.getRGB(x, y)
                    }
                }
                return image
            }
        } catch (e: Exception) {
            SMetro.LOGGER.error("Failed to load icon '{}'", path, e)
        }
        return null
    }

    companion object {
        private const val ANNOUNCEMENTS_CATEGORY = "announcements"

        var vcApi: VoicechatApi? = null
        var vcServerApi: VoicechatServerApi? = null
        var announcements: VolumeCategory? = null
    }
}