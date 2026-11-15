package com.educationwithhassan.financetools.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SavedCalculationEntity::class,
        FavoriteToolEntity::class,
        RecentToolEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun savedCalculationDao(): SavedCalculationDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getInstance(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_tools_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
