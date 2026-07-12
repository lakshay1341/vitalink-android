package com.lakshay.vitalink.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [EncounterEntity::class, VitalEntity::class], version = 1, exportSchema = false)
abstract class VitaLinkDb : RoomDatabase() {
    abstract fun encounterDao(): EncounterDao
    abstract fun vitalDao(): VitalDao
}
