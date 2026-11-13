package lv.tamekit

import lv.tamekit.data.MovementType
import lv.tamekit.data.StockMovement
import lv.tamekit.domain.StockCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class StockCalculatorTest {
    private fun mv(material: Long, t: MovementType, q: String) =
        StockMovement(materialId = material, movementType = t, quantity = BigDecimal(q))

    private fun assertNum(expected: String, actual: BigDecimal) =
        assertEquals(0, BigDecimal(expected).compareTo(actual))

    @Test fun balanceSumsSignedMovements() {
        val list = listOf(mv(1, MovementType.IN, "20"), mv(1, MovementType.IN, "10"), mv(1, MovementType.OUT, "-5"))
        assertNum("25", StockCalculator.balance(list))
    }

    @Test fun decimalsAreExact() {
        val list = listOf(mv(1, MovementType.IN, "12.5"), mv(1, MovementType.IN, "12.50"))
        assertNum("25", StockCalculator.balance(list))
        assertNum("0.3", StockCalculator.balance(listOf(mv(1, MovementType.IN, "0.1"), mv(1, MovementType.IN, "0.2"))))
    }

    @Test fun balancesAreSeparatedPerMaterial() {
        val map = StockCalculator.balances(listOf(mv(1, MovementType.IN, "5"), mv(2, MovementType.IN, "7"), mv(1, MovementType.OUT, "-2")))
        assertNum("3", map.getValue(1))
        assertNum("7", map.getValue(2))
    }

    @Test fun outIsNegativeAndInIsPositive() {
        assertNum("-5", StockCalculator.signed(MovementType.OUT, BigDecimal("5")))
        assertNum("5", StockCalculator.signed(MovementType.IN, BigDecimal("5")))
    }

    @Test fun adjustmentDeltaMovesBalanceToNewValue() {
        assertNum("-4", StockCalculator.adjustmentDelta(BigDecimal("6"), BigDecimal("10")))
        assertNum("2.5", StockCalculator.adjustmentDelta(BigDecimal("12.5"), BigDecimal("10")))
    }

    @Test fun zeroOpeningBalanceCreatesNoMovement() {
        assertEquals(null, StockCalculator.openingMovement(1, BigDecimal.ZERO, 100L))
    }

    @Test fun positiveOpeningBalanceCreatesInMovement() {
        val m = StockCalculator.openingMovement(7, BigDecimal("20"), 1250L)!!
        assertEquals(MovementType.IN, m.movementType)
        assertEquals(7L, m.materialId)
        assertNum("20", m.quantity)
        assertEquals(StockCalculator.OPENING, m.description)
    }

    @Test fun signMustMatchMovementType() {
        assertEquals(true, StockCalculator.isSignConsistent(MovementType.IN, BigDecimal("1")))
        assertEquals(false, StockCalculator.isSignConsistent(MovementType.IN, BigDecimal("-1")))
        assertEquals(true, StockCalculator.isSignConsistent(MovementType.OUT, BigDecimal("-1")))
        assertEquals(false, StockCalculator.isSignConsistent(MovementType.OUT, BigDecimal("1")))
        assertEquals(false, StockCalculator.isSignConsistent(MovementType.ADJUSTMENT, BigDecimal.ZERO))
    }

    @Test fun entryRulesPerMovementType() {
        assertEquals(true, StockCalculator.isValidEntry(MovementType.IN, BigDecimal("1")))
        assertEquals(false, StockCalculator.isValidEntry(MovementType.IN, BigDecimal.ZERO))
        assertEquals(false, StockCalculator.isValidEntry(MovementType.OUT, BigDecimal("-5")))
        assertEquals(true, StockCalculator.isValidEntry(MovementType.ADJUSTMENT, BigDecimal.ZERO))
        assertEquals(false, StockCalculator.isValidEntry(MovementType.ADJUSTMENT, BigDecimal("-1")))
    }
}
