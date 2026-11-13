package lv.tamekit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lv.tamekit.data.AppDatabase
import lv.tamekit.data.Backup
import lv.tamekit.data.Company
import lv.tamekit.data.Customer
import lv.tamekit.data.Material
import lv.tamekit.data.MovementType
import lv.tamekit.data.StockMovement
import lv.tamekit.domain.DemoData
import lv.tamekit.domain.MoveResult
import lv.tamekit.domain.StockCalculator
import lv.tamekit.domain.StockService
import java.math.BigDecimal

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val stock = StockService(db)

    private fun <T> Flow<T>.state(initial: T) =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val company = db.company().observe().state<Company?>(null)
    val customers = db.customers().observeActive().state(emptyList<Customer>())
    val materials = db.materials().observeActive().state(emptyList<Material>())
    val movements = db.movements().observeAll().state(emptyList<StockMovement>())
    val balances = movements.map { StockCalculator.balances(it) }.state(emptyMap<Long, BigDecimal>())

    fun saveCompany(c: Company) = viewModelScope.launch { db.company().save(c) }
    fun addCustomer(c: Customer) = viewModelScope.launch { db.customers().insert(c) }
    fun archiveCustomer(c: Customer) = viewModelScope.launch { db.customers().update(c.copy(isArchived = true)) }
    fun removeMaterial(m: Material) = viewModelScope.launch {
        db.materials().update(m.copy(isActive = false, updatedAt = System.currentTimeMillis()))
    }

    fun addMaterial(m: Material, opening: BigDecimal) = viewModelScope.launch { stock.addMaterial(m, opening) }

    suspend fun move(m: Material, type: MovementType, entered: BigDecimal, note: String): MoveResult =
        stock.move(m.id, m.purchasePriceCents, type, entered, note)

    suspend fun loadDemo(): Boolean = DemoData.load(db)
    suspend fun clearAllData() = DemoData.clearAll(db)

    suspend fun exportJson(): String = Backup.export(db)
    suspend fun restoreJson(json: String) = Backup.restore(db, json)
}
