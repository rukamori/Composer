package moe.rukamori.composerapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ComposerDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
