package com.bleudev.simplemetro.util

import com.bleudev.simplemetro.SMetro
import kotlin.math.min
import kotlin.reflect.jvm.jvmName

class ConfigTranslationKeyBuilder private constructor(private val current: Array<String?>) {
    fun category(category: String = "general"): ConfigTranslationKeyBuilder =
        update(arrayOfNulls<String>(5).also { it[1] = "category.$category"; it[2] = ""; it[3] = ""; it[4] = "" })
    fun root(): ConfigTranslationKeyBuilder =
        update(arrayOfNulls<String>(5).also { it[2] = "root"; it[3] = ""; it[4] = "" })
    fun group(group: String): ConfigTranslationKeyBuilder =
        update(arrayOfNulls<String>(4).also { it[2] = "group.$group"; it[3] = "" })
    fun option(option: String): ConfigTranslationKeyBuilder =
        update(arrayOfNulls<String>(4).also { it[3] = "option.$option" })
    fun description(): ConfigTranslationKeyBuilder =
        update(arrayOfNulls<String>(5).also { it[4] = "description" })
    fun additional(path: String): ConfigTranslationKeyBuilder =
        update(arrayOfNulls<String>(DEFAULT_SIZE).also { it[DEFAULT_SIZE-1] = path })
    fun clear(): ConfigTranslationKeyBuilder =
        update(Array<String?>(DEFAULT_SIZE){""}.also { it[0] = null })
    inline fun <reified T : Enum<T>> enum(value: T): ConfigTranslationKeyBuilder {
        var className = T::class.jvmName.substringAfterLast(".").substringAfterLast("$")
        val valueName = value.name
        return clear().additional("enum.$className.$valueName")
    }

    private fun update(patch: Array<String?>): ConfigTranslationKeyBuilder {
        val new = current.copyOf()
        for (i in 0..<min(new.size, patch.size)) {
            val p = patch[i]
            if (p != null) {
                new[i] = p.ifEmpty { null }
            }
        }
        return ConfigTranslationKeyBuilder(new)
    }
    fun update(patch: ConfigTranslationKeyBuilder): ConfigTranslationKeyBuilder = update(patch.current)
    fun build(): String = current.reduceOrNull { acc, string -> if (string.isNullOrEmpty()) acc else if (acc.isNullOrEmpty()) "" else "$acc.$string" } ?: ""

    override fun toString(): String = build()

    companion object {
        private const val DEFAULT_SIZE = 999
        fun of(id: String): ConfigTranslationKeyBuilder {
            val current = arrayOfNulls<String>(DEFAULT_SIZE)
            current[0] = "yacl3.config.$id"
            return ConfigTranslationKeyBuilder(current)
        }
        fun default(): ConfigTranslationKeyBuilder =
            of(SMetro.ID)
    }
}