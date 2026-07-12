package com.lakshay.vitalink.di

import com.lakshay.vitalink.data.VitaLinkRepositoryImpl
import com.lakshay.vitalink.domain.VitaLinkRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepository(impl: VitaLinkRepositoryImpl): VitaLinkRepository
}
