package com.bleudev.simplemetro.announcement

import com.bleudev.simplemetro.config.SMetroInfo
import com.bleudev.simplemetro.config.obj.Announcements

object AnnouncementResolver {
    fun resolve(
        info: SMetroInfo,
        stationId: Int,
        lineId: Int?,
        type: Announcements.AnnouncementType,
    ): List<String> {
        val station = info.stations.firstOrNull { it.id == stationId } ?: return emptyList()
        val line = lineId?.let { id -> info.lines.firstOrNull { it.id == id } }
            ?: info.lines.singleOrNull { stationId in it.stations }
        val stationAnnouncement = station.announcements.get(type)
            ?: line?.announcements?.get(type)
            ?: return emptyList()
        val statusAnnouncement = station.announcements.statuses[station.status]
            ?: line?.announcements?.statuses?.get(station.status)
        return listOfNotNull(stationAnnouncement, statusAnnouncement).distinct()
    }
}
