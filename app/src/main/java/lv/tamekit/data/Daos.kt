package lv.tamekit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM company WHERE id = 1") fun observe(): Flow<Company?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(company: Company)
    @Query("SELECT * FROM company") suspend fun all(): List<Company>
    @Query("DELETE FROM company") suspend fun clear()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customer WHERE isArchived = 0 ORDER BY name COLLATE NOCASE") fun observeActive(): Flow<List<Customer>>
    @Insert suspend fun insert(customer: Customer): Long
    @Update suspend fun update(customer: Customer)
    @Query("SELECT * FROM customer") suspend fun all(): List<Customer>
    @Query("DELETE FROM customer") suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Customer>)
}

@Dao
interface MaterialDao {
    // Materiālus "dzēš" mīkstā veidā (isActive = false), lai vecie dati nepazūd.
    @Query("SELECT * FROM material WHERE isActive = 1 ORDER BY name COLLATE NOCASE") fun observeActive(): Flow<List<Material>>
    @Insert suspend fun insert(material: Material): Long
    @Update suspend fun update(material: Material)
    @Query("SELECT * FROM material") suspend fun all(): List<Material>
    @Query("DELETE FROM material") suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<Material>)
}

@Dao
interface MovementDao {
    @Query("SELECT * FROM stock_movement ORDER BY createdAt DESC, id DESC") fun observeAll(): Flow<List<StockMovement>>
    @Insert suspend fun insert(movement: StockMovement): Long
    @Query("SELECT * FROM stock_movement WHERE materialId = :materialId") suspend fun forMaterial(materialId: Long): List<StockMovement>
    @Query("SELECT * FROM stock_movement") suspend fun all(): List<StockMovement>
    @Query("DELETE FROM stock_movement") suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(items: List<StockMovement>)
}
