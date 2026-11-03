package com.bleudev.simplemetro.announcement

object AnnouncementId {
    private val VALID_ID = Regex("[a-zа-яA-ZА-Я0-9][a-zа-яA-ZА-Я0-9._-]]{0,63}")

    fun isValid(value: String?): Boolean = value != null && VALID_ID.matches(value)
}
