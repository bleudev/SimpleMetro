package com.bleudev.simplemetro.announcement

object AnnouncementId {
    @Deprecated("Use nullability check instead")
    fun isValid(value: String?): Boolean = value != null
}
