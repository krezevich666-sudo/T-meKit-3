package lv.tamekit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import lv.tamekit.data.AppDatabase
import lv.tamekit.data.Company
import lv.tamekit.data.Customer
import lv.tamekit.domain.DemoData
import lv.tamekit.domain.StockCalculator
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DemoDataTest {
    private lateinit var db: AppDatabase

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun tearDown() { db.close() }

    @Test fun loadsIntoEmptyDatabase() {
        runBlocking {
            assertEquals(true, DemoData.load(db))
            assertEquals(5, db.materials().all().size)
            assertEquals(1, db.customers().all().size)
            val varnish = db.materials().all().first { it.name == "Laka HS" }
            val balance = StockCalculator.balances(db.movements().all()).getValue(varnish.id)
            assertEquals(0, BigDecimal("4.5").compareTo(balance))
        }
    }

    @Test fun doesNotTouchExistingData() {
        runBlocking {
            db.customers().insert(Customer(name = "Īstais klients"))
            assertEquals(false, DemoData.load(db))
            assertEquals(1, db.customers().all().size)
            assertEquals(0, db.materials().all().size)
        }
    }

    @Test fun clearAllRemovesEverything() {
        runBlocking {
            DemoData.load(db)
            db.company().save(Company(name = "SIA"))
            DemoData.clearAll(db)
            assertEquals(0, db.customers().all().size)
            assertEquals(0, db.materials().all().size)
            assertEquals(0, db.movements().all().size)
            assertEquals(0, db.company().all().size)
        }
    }
}
