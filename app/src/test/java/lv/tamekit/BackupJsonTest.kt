package lv.tamekit

import lv.tamekit.data.BackupJson
import lv.tamekit.data.Customer
import lv.tamekit.data.Material
import lv.tamekit.data.MovementType
import lv.tamekit.data.StockMovement
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BackupJsonTest {
    @Test fun movementRoundTrip() {
        val m = StockMovement(5, 3, MovementType.OUT, BigDecimal("-2.50"), null, null, "x", 123L)
        assertEquals(m, BackupJson.movementFrom(BackupJson.toJson(m)))
    }

    @Test fun materialRoundTrip() {
        val m = Material(1, "Lakas", "", "l", 1250L, BigDecimal("2.5"), "", true, 5L)
        assertEquals(m, BackupJson.materialFrom(BackupJson.toJson(m)))
    }

    @Test fun customerRoundTrip() {
        val c = Customer(2, "Jānis", "", "123", "", "", "", true, 9L)
        assertEquals(c, BackupJson.customerFrom(BackupJson.toJson(c)))
    }
}
