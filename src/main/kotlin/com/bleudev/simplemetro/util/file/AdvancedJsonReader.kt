package com.bleudev.simplemetro.util.file

import com.bleudev.simplemetro.SMetro
import kotlinx.serialization.KSerializer
import java.nio.file.Path

open class AdvancedJsonReader<T : Any>(
    path: Path,
    private val loadErrMessage: String,
    private val saveErrMessage: String,
    private val serializer: KSerializer<T>,
    private val defaultSupplier: () -> T,
) : AdvancedFileReader<T>(path) {
    private var instance: T? = null

    override fun createDefault(): String {
        return SMetro.JSON.encodeToString(this.serializer, defaultSupplier())
    }

    override fun onCacheChange(cached: String) {
        this.instance = this.load()
    }

    override fun handleLoadError(e: Throwable) {
        SMetro.LOGGER.error(loadErrMessage, e)
    }

    override fun handleSaveError(e: Throwable) {
        SMetro.LOGGER.error(saveErrMessage, e)
    }

    override fun load(): T {
        return SMetro.JSON.decodeFromString(this.serializer, this.load0())
    }

    override fun save(value: T) {
        this.save0(SMetro.JSON.encodeToString(this.serializer, value))
    }
}