package com.lakshay.vitalink.di

import android.content.Context
import androidx.room.Room
import com.lakshay.vitalink.data.local.EncounterDao
import com.lakshay.vitalink.data.local.VitalDao
import com.lakshay.vitalink.data.local.VitaLinkDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun db(@ApplicationContext ctx: Context): VitaLinkDb =
        Room.databaseBuilder(ctx, VitaLinkDb::class.java, "vitalink.db").build()

    @Provides
    fun encounterDao(db: VitaLinkDb): EncounterDao = db.encounterDao()

    @Provides
    fun vitalDao(db: VitaLinkDb): VitalDao = db.vitalDao()
}
