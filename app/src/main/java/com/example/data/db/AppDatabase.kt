package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CigaretteDao
import com.example.data.dao.SmokingGoalDao
import com.example.data.dao.SmokingLogDao
import com.example.data.model.Cigarette
import com.example.data.model.SmokingGoal
import com.example.data.model.SmokingLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Cigarette::class, SmokingLog::class, SmokingGoal::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cigaretteDao(): CigaretteDao
    abstract fun smokingLogDao(): SmokingLogDao
    abstract fun smokingGoalDao(): SmokingGoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smoking_tracker_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.cigaretteDao(), database.smokingGoalDao())
                    }
                }
            }

            suspend fun populateDatabase(
                cigaretteDao: CigaretteDao,
                smokingGoalDao: SmokingGoalDao
            ) {
                // Populate default cigarette brands
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "中华 (软中华 / Soft Chunghwa)",
                        price = 65.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 650.0,
                        packsPerCarton = 10,
                        isActive = true
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "炫赫门 (南京细支 / Xuanhemen)",
                        price = 18.0,
                        packSize = 20,
                        priceType = "CARTON",
                        cartonPrice = 180.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "万宝路 (薄荷双爆 / Marlboro Double Burst)",
                        price = 30.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 300.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )

                // Populate a default starting goal (e.g. limit to 10 cigarettes per day)
                smokingGoalDao.insertGoal(
                    SmokingGoal(
                        dailyLimit = 10,
                        monthlyBudget = 300.0,
                        isActive = true
                    )
                )
            }
        }
    }
}
