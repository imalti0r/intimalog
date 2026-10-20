package com.intima.log.di

import android.content.Context
import com.intima.log.data.db.IntimaDatabase
import com.intima.log.data.repo.PartnerRepository
import com.intima.log.data.repo.ProfileRepository
import com.intima.log.data.repo.RecordRepository
import com.intima.log.data.repo.RecordRepositoryImpl
import com.intima.log.data.repo.TemplateRepository
import com.intima.log.data.security.SecurityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): IntimaDatabase {
        val passphrase = SecurityManager.getOrCreateDbPassphrase(context)
        return IntimaDatabase.build(context, passphrase)
    }

    @Provides fun providePartnerDao(db: IntimaDatabase) = db.partnerDao()
    @Provides fun provideRecordDao(db: IntimaDatabase) = db.recordDao()
    @Provides fun provideTemplateDao(db: IntimaDatabase) = db.templateDao()
    @Provides fun provideCycleDao(db: IntimaDatabase) = db.cycleDao()

    @Provides
    @Singleton
    fun provideRecordRepository(impl: RecordRepositoryImpl): RecordRepository = impl

    @Provides
    @Singleton
    fun providePartnerRepository(@ApplicationContext context: Context, db: IntimaDatabase): PartnerRepository =
        PartnerRepository(db.partnerDao())

    @Provides
    @Singleton
    fun provideTemplateRepository(db: IntimaDatabase): TemplateRepository =
        TemplateRepository(db.templateDao())

    @Provides
    @Singleton
    fun provideProfileRepository(@ApplicationContext context: Context): ProfileRepository =
        ProfileRepository(context)
}
