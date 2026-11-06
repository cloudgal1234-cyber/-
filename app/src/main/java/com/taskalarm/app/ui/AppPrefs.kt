package com.taskalarm.app.ui

import android.content.Context

object AppPrefs {
    private const val FILE = "app_prefs"
    private const val KEY_ONBOARDED = "onboarding_done"

    fun isOnboarded(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_ONBOARDED, true).apply()
}
