package lv.tamekit.domain

import androidx.room.withTransaction
import lv.tamekit.data.AppDatabase
import lv.tamekit.data.Customer
import lv.tamekit.data.Material
import lv.tamekit.data.MovementType
import java.math.BigDecimal

/** Demonstrācijas dati auto krāsu noliktavas piemēram. Netiek ielādēti, ja datubāzē jau ir dati. */
object DemoData {
    private data class Item(val name: String, val priceCents: Long, val opening: String, val min: String)

    private val items = listOf(
        Item("Bāzes krāsa Silver", 3250, "4", "2"),
        Item("Cietinātājs Fast", 1890, "2", "3"), // zem minimālā atlikuma, lai redzētu brīdinājumu
        Item("Laka HS", 2650, "6", "2"),
        Item("Grunts Grey", 2200, "3", "2"),
        Item("Šķīdinātājs", 690, "8", "4")
    )

    suspend fun isEmpty(db: AppDatabase): Boolean =
        db.customers().all().isEmpty() && db.materials().all().isEmpty() && db.movements().all().isEmpty()

    suspend fun load(db: AppDatabase): Boolean {
        if (!isEmpty(db)) return false
        val service = StockService(db)
        db.customers().insert(Customer(name = "Demo klients SIA", phone = "20000000"))
        items.forEach {
            service.addMaterial(
                Material(name = it.name, unit = "l", purchasePriceCents = it.priceCents, minimumStock = BigDecimal(it.min)),
                BigDecimal(it.opening)
            )
        }
        val varnish = db.materials().all().first { it.name == "Laka HS" }
        service.move(varnish.id, varnish.purchasePriceCents, MovementType.OUT, BigDecimal("1.5"), "Demo darbs")
        return true
    }

    /** Dzēš VISUS datus (kustības pirmās, jo tās atsaucas uz materiāliem). */
    suspend fun clearAll(db: AppDatabase) = db.withTransaction {
        db.movements().clear(); db.materials().clear(); db.customers().clear(); db.company().clear()
    }
}
