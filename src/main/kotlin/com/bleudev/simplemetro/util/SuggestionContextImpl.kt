package com.bleudev.simplemetro.util

import com.bleudev.simplemetro.config.SMetroInfo
import java.nio.file.Files

internal class SuggestionContextImpl(override val announcementIds: List<String> = listOf()) : SuggestionContext {
    companion object {
        fun create(): SuggestionContext {
            val annFiles = arrayListOf<String>()
            for (path in Files.walk(SMetroInfo.announcementsDirectory)) {
                val f = path.toFile()
                if (f.isFile && f.extension == "wav") {
                    annFiles.add(f.nameWithoutExtension)
                }
            }

            return SuggestionContextImpl(annFiles.toList())
        }
    }
}