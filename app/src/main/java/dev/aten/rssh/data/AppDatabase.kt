package dev.aten.rssh.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Host::class, Command::class, TileSlot::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hosts(): HostDao
    abstract fun commands(): CommandDao
    abstract fun tileSlots(): TileSlotDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "rssh.db")
                .build()
                .also { instance = it }
        }
    }
}
