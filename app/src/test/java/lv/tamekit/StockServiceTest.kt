package lv.tamekit

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import lv.tamekit.data.AppDatabase
import lv.tamekit.data.Material
import lv.tamekit.data.MovementType
import lv.tamekit.data.StockMovement
import lv.tamekit.domain.MoveResult
import lv.tamekit.domain.StockCalculator
import lv.tamekit.domain.StockService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StockServiceTest {
    private lateinit var db: AppDatabase
    private lateinit var service: StockService

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        service = StockService(db)
    }

    @After fun tearDown() { db.close() }

    private suspend fun newMaterial(opening: String) =
        service.addMaterial(Material(name = "Laka", unit = "l", purchasePriceCents = 1250), BigDecimal(opening))

    private suspend fun balance(id: Long) = StockCalculator.balance(db.movements().forMaterial(id))
    private suspend fun move(id: Long, t: MovementType, q: String) = service.move(id, 1250, t, BigDecimal(q), "")
    private fun assertNum(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test fun checklistScenario() {
        runBlocking {
            val id = newMaterial("20")
            assertNum("20", balance(id))
            assertEquals(MoveResult.OK, move(id, MovementType.IN, "10")); assertNum("30", balance(id))
            assertEquals(MoveResult.OK, move(id, MovementType.OUT, "5")); assertNum("25", balance(id))
            assertEquals(MoveResult.INSUFFICIENT, move(id, MovementType.OUT, "100")); assertNum("25", balance(id))
            assertEquals(MoveResult.OK, move(id, MovementType.ADJUSTMENT, "12")); assertNum("12", balance(id))
        }
    }

    @Test fun zeroOpeningBalanceCreatesNoMovement() {
        runBlocking {
            val id = newMaterial("0")
            assertEquals(0, db.movements().forMaterial(id).size)
        }
    }

    @Test fun invalidEntriesAreRejectedAndNothingIsSaved() {
        runBlocking {
            val id = newMaterial("20")
            assertEquals(MoveResult.INVALID, move(id, MovementType.IN, "0"))
            assertEquals(MoveResult.INVALID, move(id, MovementType.IN, "-1"))
            assertEquals(MoveResult.INVALID, move(id, MovementType.OUT, "-5"))
            assertEquals(MoveResult.INVALID, move(id, MovementType.ADJUSTMENT, "-1"))
            assertEquals(1, db.movements().forMaterial(id).size)
            assertNum("20", balance(id))
        }
    }

    @Test fun adjustmentToSameBalanceAddsNothing() {
        runBlocking {
            val id = newMaterial("20")
            assertEquals(MoveResult.OK, move(id, MovementType.ADJUSTMENT, "20"))
            assertEquals(1, db.movements().forMaterial(id).size)
        }
    }

    @Test fun foreignKeyBlocksMovementForMissingMaterial() {
        runBlocking {
            try {
                db.movements().insert(StockMovement(materialId = 999, movementType = MovementType.IN, quantity = BigDecimal.ONE))
                fail("expected a foreign key violation")
            } catch (e: SQLiteConstraintException) {
                // gaidītā uzvedība
            }
        }
    }
}
