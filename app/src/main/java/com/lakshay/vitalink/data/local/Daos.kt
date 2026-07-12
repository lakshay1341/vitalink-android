package com.lakshay.vitalink.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface EncounterDao {
    @Query("SELECT * FROM encounters")
    suspend fun getAll(): List<EncounterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<EncounterEntity>)

    @Query("DELETE FROM encounters")
    suspend fun clear()

    /** Replace the cached ward in one transaction so a partial write never leaves stale rows. */
    @Transaction
    suspend fun replaceAll(items: List<EncounterEntity>) {
        clear()
        upsertAll(items)
    }
}

@Dao
interface VitalDao {
    @Query("SELECT * FROM vitals WHERE encounterId = :encounterId")
    suspend fun getForEncounter(encounterId: Long): List<VitalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<VitalEntity>)
}
