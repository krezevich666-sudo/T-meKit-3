package lv.tamekit.domain

import lv.tamekit.data.MovementType
import lv.tamekit.data.StockMovement
import java.math.BigDecimal

object StockCalculator {
    const val OPENING = "OPENING"

    fun balance(movements: List<StockMovement>): BigDecimal =
        movements.fold(BigDecimal.ZERO) { acc, m -> acc + m.quantity }

    fun balances(movements: List<StockMovement>): Map<Long, BigDecimal> =
        movements.groupBy { it.materialId }.mapValues { balance(it.value) }

    /** Lietotāja ievadītais daudzums → daudzums ar zīmi (OUT ir negatīvs). */
    fun signed(type: MovementType, entered: BigDecimal): BigDecimal =
        if (type == MovementType.OUT) entered.negate() else entered

    fun adjustmentDelta(newBalance: BigDecimal, current: BigDecimal): BigDecimal = newBalance - current

    /** Sākuma atlikums: ja daudzums ir 0 (vai mazāks), kustība netiek veidota. */
    fun openingMovement(materialId: Long, quantity: BigDecimal, unitCostCents: Long?): StockMovement? =
        if (quantity.signum() > 0)
            StockMovement(materialId = materialId, movementType = MovementType.IN, quantity = quantity,
                unitCostCents = unitCostCents, description = OPENING)
        else null

    /** Lietotāja ievade: IN un OUT daudzums > 0, ADJUSTMENT (jaunais atlikums) >= 0. */
    fun isValidEntry(type: MovementType, entered: BigDecimal): Boolean = when (type) {
        MovementType.IN, MovementType.OUT -> entered.signum() > 0
        MovementType.ADJUSTMENT -> entered.signum() >= 0
    }

    /** IN > 0, OUT < 0, ADJUSTMENT ≠ 0. */
    fun isSignConsistent(type: MovementType, quantity: BigDecimal): Boolean = when (type) {
        MovementType.IN -> quantity.signum() > 0
        MovementType.OUT -> quantity.signum() < 0
        MovementType.ADJUSTMENT -> quantity.signum() != 0
    }
}
