package com.bleudev.simplemetro.util.file

import java.nio.file.Files
import java.nio.file.Path

abstract class AdvancedFileReader<T : Any>(val path: Path) {
    private var cached: String? = null
    private var lastModTime: Long? = null

    private fun load1(): String {
        return if (Files.exists(this.path)) {
            Files.readString(this.path)
        } else {
            save0(this.createDefault())
        }
    }
    protected fun load0(): String {
        try {
            val modTime = if (Files.exists(this.path)) Files.getLastModifiedTime(this.path).toMillis() else null
            if (this.cached == null || this.lastModTime == null || this.lastModTime != modTime) {
                this.cached = load1()
                this.lastModTime = modTime
                this.onCacheChange(this.cached!!)
            }
            return this.cached!!
        } catch (e: Throwable) {
            handleLoadError(e)
            return this.cached ?: this.createDefault()
        }
    }

    protected fun save0(s: String): String {
        try {
            if (!Files.exists(this.path)) {
                Files.createDirectories(this.path.parent)
            }
            Files.writeString(this.path, s)
            this.cached = s
            this.lastModTime = Files.getLastModifiedTime(this.path).toMillis()
            this.onCacheChange(this.cached!!)
            return s
        } catch (e: Throwable) {
            handleSaveError(e)
            return this.cached ?: this.createDefault()
        }
    }

    protected abstract fun createDefault(): String
    protected abstract fun onCacheChange(cached: String)

    protected abstract fun handleLoadError(e: Throwable)
    protected abstract fun handleSaveError(e: Throwable)

    abstract fun load(): T
    abstract fun save(value: T)

    operator fun invoke(operation: T.() -> Unit): T {
        val i = load()
        i.operation()
        save(i)
        return i
    }
}