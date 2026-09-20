package com.lunarvr.system

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lunar_vr_prefs", Context.MODE_PRIVATE)

    var dwellIndex: Int
        get() = prefs.getInt("dwell_index", 2)
        set(value) = prefs.edit().putInt("dwell_index", value).apply()

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

    var economicMode: Boolean
        get() = prefs.getBoolean("economic_mode", false)
        set(value) = prefs.edit().putBoolean("economic_mode", value).apply()

    // Environment persistence
    var activeEnvironmentName: String
        get() = prefs.getString("active_environment", "LUNAR_EARTH_VIEW") ?: "LUNAR_EARTH_VIEW"
        set(value) = prefs.edit().putString("active_environment", value).apply()

    var isCustomModelActive: Boolean
        get() = prefs.getBoolean("is_custom_model_active", false)
        set(value) = prefs.edit().putBoolean("is_custom_model_active", value).apply()

    var customModelName: String?
        get() = prefs.getString("custom_model_name", null)
        set(value) = prefs.edit().putString("custom_model_name", value).apply()

    var customModelPath: String?
        get() = prefs.getString("custom_model_path", null)
        set(value) = prefs.edit().putString("custom_model_path", value).apply()

    var cameraPosX: Float
        get() = prefs.getFloat("camera_pos_x", 0f)
        set(value) = prefs.edit().putFloat("camera_pos_x", value).apply()

    var cameraPosY: Float
        get() = prefs.getFloat("camera_pos_y", 0f)
        set(value) = prefs.edit().putFloat("camera_pos_y", value).apply()

    var cameraPosZ: Float
        get() = prefs.getFloat("camera_pos_z", 0f)
        set(value) = prefs.edit().putFloat("camera_pos_z", value).apply()

    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
}
