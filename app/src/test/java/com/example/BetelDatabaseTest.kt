package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.BetelGoal
import com.example.data.model.BetelLog
import com.example.data.model.BetelProduct
import com.example.data.model.Cigarette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BetelDatabaseTest {
    @Test
    fun `turning off betel demo removes only demo logs`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = database.betelDao()
            val productId = dao.insertProduct(BetelProduct(name = "My betel", packPrice = 12.0, piecesPerPack = 6)).toInt()
            dao.insertLog(BetelLog(productId = productId, productName = "My betel", quantity = 1, logType = "SELF", cost = 2.0))
            dao.insertLogs(listOf(
                BetelLog(productId = productId, productName = "My betel", quantity = 1, logType = "SELF", cost = 2.0, isDemo = true),
                BetelLog(productId = productId, productName = "My betel", quantity = 1, logType = "RECEIVED_IN", cost = 0.0, isDemo = true)
            ))

            assertEquals(3, dao.logs().first().size)
            dao.deleteDemoLogs()
            assertEquals(1, dao.logs().first().size)
            assertEquals(false, dao.logs().first().single().isDemo)
            assertEquals(1, dao.products().first().size)
            dao.deleteDemoLogs()
            assertEquals(1, dao.logs().first().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun `betel records and goal persist without affecting cigarettes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            database.cigaretteDao().insertCigarette(Cigarette(name = "C", price = 20.0))
            val dao = database.betelDao()
            val id = dao.insertProduct(BetelProduct(name = "B", packPrice = 12.0, piecesPerPack = 6)).toInt()
            dao.setActive(id)
            dao.insertLog(BetelLog(productId = id, productName = "B", quantity = 2, logType = "SELF", cost = 4.0))
            dao.saveGoal(BetelGoal(dailyLimit = 3, monthlyBudget = 50.0))

            assertEquals(1, dao.products().first().size)
            assertEquals(true, dao.products().first().single().isActive)
            assertEquals(4.0, dao.logs().first().single().cost, 0.001)
            assertEquals(3, dao.goal().first()?.dailyLimit)
            assertEquals(1, database.cigaretteDao().getAllCigarettes().first().size)
        } finally {
            database.close()
        }
    }
}
