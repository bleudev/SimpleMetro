package com.bleudev.simplemetro.compat

internal object ModCompat {
    val SVC = CompatRef<SVCCompat>()
    val MODMENU = CompatRef<ModMenuCompat>()

    class CompatRef<T>(present: Boolean = false, private var ref: T? = null) {
        var present: Boolean = present
            private set

        fun initialize(impl: T) {
            if (!this.present) {
                this.present = true
                this.ref = impl
            }
        }
        fun getOrNull(): T? {
            return if (this.present) this.ref!! else null
        }
        fun get(): T {
            return getOrNull() ?: throw NullPointerException()
        }
        fun reset() {
            this.present = false
            this.ref = null
        }

        fun <R> ifPresent(action: T.() -> R) {
            val rf = this.ref
            if (this.present && rf != null ) {
                rf.action()
            }
        }

        operator fun <R> invoke(action: T.() -> R) = ifPresent(action)
    }
}