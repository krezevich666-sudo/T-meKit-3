package lv.tamekit.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.math.BigDecimal

// Naudu glabā centos (Long), daudzumus kā BigDecimal.

@Entity(tableName = "company")
data class Company(
    @PrimaryKey val id: Long = 1,
    val name: String = "",
    val registrationNumber: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val logoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val companyName: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "material")
data class Material(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "",
    val unit: String,
    val purchasePriceCents: Long,
    val minimumStock: BigDecimal = BigDecimal.ZERO,
    val supplier: String = "",
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class MovementType { IN, OUT, ADJUSTMENT }

// Atlikums = visu kustību summa. quantity ir ar zīmi: IN > 0, OUT < 0, ADJUSTMENT = starpība.
@Entity(
    tableName = "stock_movement",
    indices = [Index("materialId")],
    foreignKeys = [ForeignKey(entity = Material::class, parentColumns = ["id"], childColumns = ["materialId"], onDelete = ForeignKey.RESTRICT)]
)
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val materialId: Long,
    val movementType: MovementType,
    val quantity: BigDecimal,
    val unitCostCents: Long? = null,
    val referenceEstimateId: Long? = null,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

class Converters {
    @TypeConverter fun fromBd(v: BigDecimal?): String? = v?.toPlainString()
    @TypeConverter fun toBd(v: String?): BigDecimal? = v?.let { BigDecimal(it) }
}
