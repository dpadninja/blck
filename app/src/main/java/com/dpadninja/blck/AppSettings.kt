package com.dpadninja.blck

import com.dpadninja.blck.helpers.PreferenceUtil
import kotlinx.coroutines.flow.MutableStateFlow

object AppSettings : PreferenceUtil() {
    private const val ENABLED = "enabled"
    private const val IDLE_TIMEOUT_SEC = "idle_timeout_sec"
    private const val ALLOWED_PACKAGES = "allowed_packages"
    private const val IDLE_ENABLED = "idle_enabled"

    val TIMEOUTS = listOf(10, 30, 60, 120, 300, 600, 900, 1800)

    val revision = MutableStateFlow(0)

    private fun bump() {
        revision.value = revision.value + 1
    }

    var enabled: Boolean
        get() = getBoolean(ENABLED, true)
        set(value) {
            putBoolean(ENABLED, value)
            bump()
        }

    var idleTimeoutSec: Int
        get() = getInt(IDLE_TIMEOUT_SEC, 300)
        set(value) {
            putInt(IDLE_TIMEOUT_SEC, value)
            bump()
        }

    var allowedPackages: Set<String>
        get() = getStringSet(ALLOWED_PACKAGES)
        set(value) {
            putStringSet(ALLOWED_PACKAGES, value)
            bump()
        }

    var idleTimeoutEnabled: Boolean
        get() = getBoolean(IDLE_ENABLED, true)
        set(value) {
            putBoolean(IDLE_ENABLED, value)
            bump()
        }

}
