package com.axerioo.gamesphere.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// --- AppDatabase Class ---
// This abstract class represents the Room database for the application.

@Database(
    entities = [GameEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // Abstract method to get the DAO for the 'games' table.
    abstract fun gameDao(): GameDao

    // This object provides a way to get a singleton instance of the AppDatabase.
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Public method to get the singleton instance of the database.
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gamesphere_database"
                )
                    .build() // Creates the database instance.

                INSTANCE = instance
                instance
            }
        }
    }
}