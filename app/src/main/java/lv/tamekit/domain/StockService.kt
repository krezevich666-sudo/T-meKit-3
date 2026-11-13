package lv.tamekit.domain

import androidx.room.withTransaction
import lv.tamekit.data.AppDatabase
import lv.tamekit.data.Material
import lv.tamekit.data.MovementType
import lv.tamekit.data.StockMovement
import java.math.BigDecimal

enum class MoveResult { OK, INSUFFICIENT, INVALID }

/** Noliktavas biznesa loģika: pārbaudes un saglabāšana vienā transakcijā. Neatkarīga no ekrāniem. */
class StockService(private val db: AppDatabase) {

    suspend fun addMaterial(m: Material, opening: BigDecimal): Long = db.withTransaction {
        val id = db.materials().insert(m)
        StockCalculator.openingMovement(id, opening, m.purchasePriceCents)?.let { db.movements().insert(it) }
        id
    }

    suspend fun move(materialId: Long, unitCostCents: Long, type: MovementType, entered: BigDecimal, note: String): MoveResult {
        if (!StockCalculator.isValidEntry(type, entered)) return MoveResult.INVALID
        return db.withTransaction {
            val current = StockCalculator.balance(db.movements().forMaterial(materialId))
            if (type == MovementType.OUT && entered > current) {
                MoveResult.INSUFFICIENT
            } else {
                val delta = if (type == MovementType.ADJUSTMENT) StockCalculator.adjustmentDelta(entered, current)
                else StockCalculator.signed(type, entered)
                if (delta.signum() != 0) {
                    db.movements().insert(
                        StockMovement(
                            materialId = materialId, movementType = type, quantity = delta,
                            unitCostCents = if (type == MovementType.IN) unitCostCents else null,
                            description = note
                        )
                    )
                }
                MoveResult.OK
            }
        }
    }
}
