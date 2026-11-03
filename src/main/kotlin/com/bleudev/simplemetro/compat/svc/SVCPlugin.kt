package com.bleudev.simplemetro.compat.svc

import com.bleudev.simplemetro.SMetro
import com.bleudev.simplemetro.announcement.AnnouncementTransportState
import de.maxhenkel.voicechat.api.VoicechatApi
import de.maxhenkel.voicechat.api.VoicechatPlugin
import de.maxhenkel.voicechat.api.VolumeCategory
import de.maxhenkel.voicechat.api.events.EventRegistration
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent
import javax.imageio.ImageIO

class SVCPlugin : VoicechatPlugin {
    override fun getPluginId(): String = SMetro.ID

    override fun initialize(api: VoicechatApi) = Unit

    override fun registerEvents(registration: EventRegistration) {
        registration.registerEvent(VoicechatServerStartedEvent::class.java, ::onServerStarted)
    }

    private fun onServerStarted(event: VoicechatServerStartedEvent) {
        try {
            val serverApi = event.voicechat
            val category = serverApi.volumeCategoryBuilder()
                .setId(ANNOUNCEMENTS_CATEGORY)
                .setName("Announcements")
                .setNameTranslationKey("smetro.voicechat.category.announcements")
                .setDescription("Metro Announcements")
                .setDescriptionTranslationKey("smetro.voicechat.category.announcements.description")
                .setIcon(getIcon("assets/simplemetro/textures/gui/sprites/category_announcements.png"))
                .build()

            serverApi.registerVolumeCategory(category)
            announcements = category
            SVCServerAnnouncements.start(serverApi)
        } catch (exception: Exception) {
            AnnouncementTransportState.reset()
            SMetro.LOGGER.error("Simple Voice Chat station announcements could not be initialized", exception)
        }
    }

    private fun getIcon(path: String): Array<IntArray>? {
        try {
            val resources = SMetro::class.java.classLoader.getResources(path)
            while (resources.hasMoreElements()) {
                val image = resources.nextElement().openStream().use { input -> ImageIO.read(input) } ?: continue
                if (image.width != 16 || image.height != 16) continue
                return Array(16) { x -> IntArray(16) { y -> image.getRGB(x, y) } }
            }
        } catch (exception: Exception) {
            SMetro.LOGGER.error("Failed to load icon '{}'", path, exception)
        }
        return null
    }

    companion object {
        private const val ANNOUNCEMENTS_CATEGORY = "announcements"
        var announcements: VolumeCategory? = null
            private set
    }
}