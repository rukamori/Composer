package moe.rukamori.composerapp.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import moe.rukamori.composerapp.data.AndroidAudioRepository
import moe.rukamori.composerapp.data.AndroidExportRepository
import moe.rukamori.composerapp.data.AndroidSettingsRepository
import moe.rukamori.composerapp.data.RoomProjectRepository
import moe.rukamori.composerapp.data.db.ComposerDatabase
import moe.rukamori.composerapp.data.db.ProjectDao
import moe.rukamori.composerapp.domain.repository.AudioRepository
import moe.rukamori.composerapp.domain.repository.ExportRepository
import moe.rukamori.composerapp.domain.repository.ProjectRepository
import moe.rukamori.composerapp.domain.repository.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ComposerDatabase =
        Room.databaseBuilder(
            context,
            ComposerDatabase::class.java,
            "composer.db",
        ).build()

    @Provides
    fun provideProjectDao(database: ComposerDatabase): ProjectDao = database.projectDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindProjectRepository(repository: RoomProjectRepository): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindAudioRepository(repository: AndroidAudioRepository): AudioRepository

    @Binds
    @Singleton
    abstract fun bindExportRepository(repository: AndroidExportRepository): ExportRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: AndroidSettingsRepository): SettingsRepository
}
