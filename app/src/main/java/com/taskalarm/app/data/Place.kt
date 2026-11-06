package com.taskalarm.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** A registered place in the user's home: reference features + a thumbnail photo. */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey val objectKey: String,
    /** [ImageFeatures.serializeAll] of the reference shots. */
    val features: String,
    /** Minimum similarity required to accept a live frame. */
    val threshold: Float,
    /** Thumbnail file name inside the places folder. */
    val photoFile: String,
    val createdAt: Long,
) {
    val references: List<ImageFeatures> get() = ImageFeatures.parseAll(features)
}

@Dao
interface PlaceDao {
    @Query("SELECT * FROM places")
    fun observeAll(): Flow<List<Place>>

    @Query("SELECT * FROM places")
    suspend fun getAll(): List<Place>

    @Upsert
    suspend fun upsert(place: Place)

    @Query("DELETE FROM places WHERE objectKey = :key")
    suspend fun delete(key: String)
}
