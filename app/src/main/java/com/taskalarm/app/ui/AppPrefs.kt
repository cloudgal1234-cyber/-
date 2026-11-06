package com.taskalarm.app.ui

import android.content.Context

object AppPrefs {
    private const val FILE = "app_prefs"
    private const val KEY_ONBOARDED = "onboarding_done"

    fun isOnboarded(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false)

    private const val KEY_LAST_SOUND = "last_sound"

    /** The sound chosen most recently, used as the default for new alarms. */
    fun lastSound(context: Context): String? =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LAST_SOUND, null)

    fun setLastSound(context: Context, sound: String) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_LAST_SOUND, sound).apply()

    fun setOnboarded(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDED, true).apply()
}
