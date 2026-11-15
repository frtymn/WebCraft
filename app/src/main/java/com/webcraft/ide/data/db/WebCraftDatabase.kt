package com.webcraft.ide.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(
    entities = [
        ProjectEntity::class,
        FileEntity::class,
        TabEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(WebCraftConverters::class)
abstract class WebCraftDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun fileDao(): FileDao
    abstract fun tabDao(): TabDao

    companion object {
        @Volatile private var INSTANCE: WebCraftDatabase? = null

        fun get(context: Context): WebCraftDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WebCraftDatabase::class.java,
                    "webcraft.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
