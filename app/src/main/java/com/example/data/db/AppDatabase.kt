package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BetelDao
import com.example.data.model.BetelGoal
import com.example.data.model.BetelLog
import com.example.data.model.BetelProduct
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
    entities = [Cigarette::class, SmokingLog::class, SmokingGoal::class, BetelProduct::class, BetelLog::class, BetelGoal::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun betelDao(): BetelDao
    abstract fun cigaretteDao(): CigaretteDao
    abstract fun smokingLogDao(): SmokingLogDao
    abstract fun smokingGoalDao(): SmokingGoalDao

    companion object {
        // Preserve existing smoking data when adding the independent betel tracker.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `betel_products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `packPrice` REAL NOT NULL, `piecesPerPack` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `betel_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `logType` TEXT NOT NULL, `cost` REAL NOT NULL, `timestamp` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `betel_goals` (`id` INTEGER NOT NULL, `dailyLimit` INTEGER NOT NULL, `monthlyBudget` REAL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `betel_logs` ADD COLUMN `isDemo` INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smoking_tracker_db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
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
