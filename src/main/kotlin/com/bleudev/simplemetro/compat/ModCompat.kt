package com.bleudev.simplemetro.compat

internal object ModCompat {
    val SVC = CompatRef<SVCCompat>()

    class CompatRef<T>(var present: Boolean = false, private var ref: T? = null) {
        fun initialize(impl: T) {
            if (!this.present) {
                this.present = true
                this.ref = impl
            }
        }
        fun get(): T {
            return if (this.present) this.ref!! else throw NullPointerException()
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