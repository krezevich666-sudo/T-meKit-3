package lv.tamekit

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import lv.tamekit.data.AppDatabase
import lv.tamekit.data.Backup
import lv.tamekit.data.Company
import lv.tamekit.data.Customer
import lv.tamekit.data.Material
import lv.tamekit.data.MovementType
import lv.tamekit.data.StockMovement
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

/** Pilns Room eksporta un atjaunošanas tests ar divām atmiņas datubāzēm. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupDatabaseTest {
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    private fun newDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()

    @Before fun setUp() { source = newDb(); target = newDb() }
    @After fun tearDown() { source.close(); target.close() }

    private suspend fun seed(db: AppDatabase) {
        db.company().save(Company(name = "SIA Tests", phone = "123"))
        db.customers().insert(Customer(name = "Jānis", phone = "111"))
        db.customers().insert(Customer(name = "Anna", isArchived = true))
        val mid = db.materials().insert(
            Material(name = "Laka", unit = "l", purchasePriceCents = 1250, minimumStock = BigDecimal("2.5"))
        )
        db.materials().insert(Material(name = "Vecais", unit = "gab.", purchasePriceCents = 99, isActive = false))
        db.movements().insert(
            StockMovement(materialId = mid, movementType = MovementType.IN, quantity = BigDecimal("20"),
                unitCostCents = 1250, description = "OPENING")
        )
        db.movements().insert(StockMovement(materialId = mid, movementType = MovementType.OUT, quantity = BigDecimal("-5.5")))
    }

    @Test fun exportThenRestoreIntoAnotherDatabaseGivesIdenticalData() {
        runBlocking {
            seed(source)
            target.customers().insert(Customer(name = "Jāpazūd"))
            Backup.restore(target, Backup.export(source))
            assertEquals(source.company().all(), target.company().all())
            assertEquals(source.customers().all().sortedBy { it.id }, target.customers().all().sortedBy { it.id })
            assertEquals(source.materials().all().sortedBy { it.id }, target.materials().all().sortedBy { it.id })
            assertEquals(source.movements().all().sortedBy { it.id }, target.movements().all().sortedBy { it.id })
        }
    }

    @Test fun unreadableBackupLeavesExistingDataUntouched() {
        runBlocking {
            seed(target)
            val customers = target.customers().all()
            try {
                Backup.restore(target, "{oops")
                fail("expected a failure")
            } catch (e: IllegalArgumentException) {
                // gaidītā uzvedība
            }
            assertEquals(customers, target.customers().all())
            assertEquals(2, target.movements().all().size)
        }
    }

    @Test fun backupWithBrokenReferenceIsRejectedBeforeAnyChange() {
        runBlocking {
            seed(source)
            val broken = Backup.export(source).replace("\"materialId\": 1", "\"materialId\": 77")
            seed(target)
            val movements = target.movements().all()
            try {
                Backup.restore(target, broken)
                fail("expected a failure")
            } catch (e: IllegalArgumentException) {
                // gaidītā uzvedība
            }
            assertEquals(movements, target.movements().all())
        }
    }
}
