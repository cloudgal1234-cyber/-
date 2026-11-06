package com.taskalarm.app.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PlaceRepository(context: Context) {
    private val app = context.applicationContext
    private val dao = AppDatabase.get(app).placeDao()

    /** Device-protected so the reference photos are available before the first unlock too. */
    private val dir: File = File(app.createDeviceProtectedStorageContext().filesDir, "places").apply { mkdirs() }

    val places: Flow<List<Place>> = dao.observeAll()

    suspend fun getAll(): List<Place> = dao.getAll()

    fun photoFile(place: Place): File = File(dir, place.photoFile)

    suspend fun save(key: String, references: List<ImageFeatures>, threshold: Float, photo: Bitmap) {
        val name = "$key-${System.currentTimeMillis()}.jpg"
        withContext(Dispatchers.IO) {
            dir.listFiles { f -> f.name.startsWith("$key-") }?.forEach { it.delete() }
            FileOutputStream(File(dir, name)).use { photo.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        }
        dao.upsert(Place(key, ImageFeatures.serializeAll(references), threshold, name, System.currentTimeMillis()))
    }

    suspend fun delete(key: String) {
        withContext(Dispatchers.IO) { dir.listFiles { f -> f.name.startsWith("$key-") }?.forEach { it.delete() } }
        dao.delete(key)
    }
}
