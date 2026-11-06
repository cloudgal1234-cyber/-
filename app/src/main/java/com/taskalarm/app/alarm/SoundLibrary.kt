package com.taskalarm.app.alarm

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
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
            ?: fileName(context, id)
            ?: runCatching { RingtoneManager.getRingtone(context, Uri.parse(id))?.getTitle(context) }.getOrNull()
            ?: "צליל מהמכשיר"

    /** Display name of a file picked with the document picker, e.g. "alarm.mp3". */
    private fun fileName(context: Context, id: String): String? = runCatching {
        val uri = Uri.parse(id)
        if (uri.authority == "media" || uri.scheme != "content") return@runCatching null
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0)?.substringBeforeLast('.') else null
        }
    }.getOrNull()

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
