package com.taskalarm.app.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.taskalarm.app.data.SoundIds

object SoundLibrary {
    data class Sound(val id: String, val title: String)

    val builtIns = listOf(
        Sound(SoundIds.SIREN, "סירנה (חזק במיוחד)"),
        Sound(SoundIds.BEEP, "צפצוף דיגיטלי"),
        Sound(SoundIds.CHIME, "מנגינה עולה"),
        Sound(SoundIds.SYSTEM_DEFAULT, "צליל ההתראה של המערכת"),
    )

    fun isCustom(id: String) = builtIns.none { it.id == id }

    fun title(context: Context, id: String): String =
        builtIns.firstOrNull { it.id == id }?.title
            ?: runCatching { RingtoneManager.getRingtone(context, Uri.parse(id))?.getTitle(context) }.getOrNull()
            ?: "צליל מהמכשיר"

    fun resolve(context: Context, id: String): Uri {
        val dir = context.createDeviceProtectedStorageContext().filesDir
        return when (id) {
            SoundIds.SIREN -> Uri.fromFile(ToneSynth.ensure(dir, "siren"))
            SoundIds.BEEP -> Uri.fromFile(ToneSynth.ensure(dir, "beep"))
            SoundIds.CHIME -> Uri.fromFile(ToneSynth.ensure(dir, "chime"))
            SoundIds.SYSTEM_DEFAULT ->
                RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
                    ?: Settings.System.DEFAULT_ALARM_ALERT_URI
            else -> Uri.parse(id)
        }
    }
}
