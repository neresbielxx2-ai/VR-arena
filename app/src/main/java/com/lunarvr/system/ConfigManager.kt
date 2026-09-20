package com.lunarvr.system

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("lunar_vr_prefs", Context.MODE_PRIVATE)

    var dwellIndex: Int
        get() = prefs.getInt("dwell_index", 2)
        set(value) = prefs.edit().putInt("dwell_index", value).apply()

    var dwellTimeSeconds: Float
        get() = prefs.getFloat("dwell_time_seconds", 2.0f)
        set(value) = prefs.edit().putFloat("dwell_time_seconds", value).apply()

    var barStyleOrdinal: Int
        get() = prefs.getInt("bar_style", 0)
        set(value) = prefs.edit().putInt("bar_style", value).apply()

    var barColorOrdinal: Int
        get() = prefs.getInt("bar_color", 0)
        set(value) = prefs.edit().putInt("bar_color", value).apply()

    var invertX: Boolean
        get() = prefs.getBoolean("invert_x", false)
        set(value) = prefs.edit().putBoolean("invert_x", value).apply()

    var invertY: Boolean
        get() = prefs.getBoolean("invert_y", false)
        set(value) = prefs.edit().putBoolean("invert_y", value).apply()

    var is6DofEnabled: Boolean
        get() = prefs.getBoolean("is_6dof_enabled", false)
        set(value) = prefs.edit().putBoolean("is_6dof_enabled", value).apply()

    var economicMode: Boolean
        get() = prefs.getBoolean("economic_mode", false)
        set(value) = prefs.edit().putBoolean("economic_mode", value).apply()

    // Environment persistence
    var activeEnvironmentName: String
        get() = prefs.getString("active_environment", "LUNAR_EARTH_VIEW") ?: "LUNAR_EARTH_VIEW"
        set(value) = prefs.edit().putString("active_environment", value).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
