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
    version = 3,
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
                        priceType = "PACK",
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
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "白沙 (硬精品 / Baisha Fine Hard)",
                        price = 11.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 110.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "白沙 (和天下 / Baisha Hetianxia)",
                        price = 100.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 1000.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "黄果树 (佳品 / Huangguoshu Jiapin)",
                        price = 10.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 100.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "黄果树 (长香思 / Huangguoshu Changxiangsi)",
                        price = 13.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 130.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "双喜 (软经典 / Shuangxi Soft Classic)",
                        price = 10.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 100.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "双喜 (硬经典1906 / Shuangxi Classic 1906)",
                        price = 18.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 180.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "利群 (新版 / Liqun New Version)",
                        price = 16.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 160.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "玉溪 (软 / Yuxi Soft)",
                        price = 23.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 230.0,
                        packsPerCarton = 10,
                        isActive = false
                    )
                )
                cigaretteDao.insertCigarette(
                    Cigarette(
                        name = "芙蓉王 (硬黄 / Furongwang Hard Yellow)",
                        price = 25.0,
                        packSize = 20,
                        priceType = "PACK",
                        cartonPrice = 250.0,
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
