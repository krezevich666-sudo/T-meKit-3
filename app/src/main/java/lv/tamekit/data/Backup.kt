package lv.tamekit.data

import androidx.room.withTransaction
import lv.tamekit.domain.StockCalculator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object BackupJson {
    fun toJson(c: Company) = JSONObject().put("id", c.id).put("name", c.name)
        .put("registrationNumber", c.registrationNumber).put("phone", c.phone).put("email", c.email)
        .put("address", c.address).put("logoPath", c.logoPath ?: JSONObject.NULL).put("createdAt", c.createdAt)

    fun companyFrom(o: JSONObject) = Company(
        o.getLong("id"), o.getString("name"), o.getString("registrationNumber"), o.getString("phone"),
        o.getString("email"), o.getString("address"),
        if (o.isNull("logoPath")) null else o.getString("logoPath"), o.getLong("createdAt")
    )

    fun toJson(c: Customer) = JSONObject().put("id", c.id).put("name", c.name).put("companyName", c.companyName)
        .put("phone", c.phone).put("email", c.email).put("address", c.address).put("notes", c.notes)
        .put("isArchived", c.isArchived).put("createdAt", c.createdAt)

    fun customerFrom(o: JSONObject) = Customer(
        o.getLong("id"), o.getString("name"), o.getString("companyName"), o.getString("phone"),
        o.getString("email"), o.getString("address"), o.getString("notes"),
        o.getBoolean("isArchived"), o.getLong("createdAt")
    )

    fun toJson(m: Material) = JSONObject().put("id", m.id).put("name", m.name).put("category", m.category)
        .put("unit", m.unit).put("purchasePriceCents", m.purchasePriceCents)
        .put("minimumStock", m.minimumStock.toPlainString()).put("supplier", m.supplier)
        .put("isActive", m.isActive).put("updatedAt", m.updatedAt)

    fun materialFrom(o: JSONObject) = Material(
        o.getLong("id"), o.getString("name"), o.getString("category"), o.getString("unit"),
        o.getLong("purchasePriceCents"), java.math.BigDecimal(o.getString("minimumStock")),
        o.getString("supplier"), o.getBoolean("isActive"), o.getLong("updatedAt")
    )

    fun toJson(s: StockMovement) = JSONObject().put("id", s.id).put("materialId", s.materialId)
        .put("movementType", s.movementType.name).put("quantity", s.quantity.toPlainString())
        .put("unitCostCents", s.unitCostCents ?: JSONObject.NULL)
        .put("referenceEstimateId", s.referenceEstimateId ?: JSONObject.NULL)
        .put("description", s.description).put("createdAt", s.createdAt)

    fun movementFrom(o: JSONObject) = StockMovement(
        o.getLong("id"), o.getLong("materialId"), MovementType.valueOf(o.getString("movementType")),
        java.math.BigDecimal(o.getString("quantity")),
        if (o.isNull("unitCostCents")) null else o.getLong("unitCostCents"),
        if (o.isNull("referenceEstimateId")) null else o.getLong("referenceEstimateId"),
        o.getString("description"), o.getLong("createdAt")
    )
}

data class BackupData(
    val company: List<Company>,
    val customers: List<Customer>,
    val materials: List<Material>,
    val movements: List<StockMovement>
)

/** Tīrs (bez datubāzes) kodētājs un pārbaudītājs, lai to varētu testēt ar JUnit. */
object BackupCodec {
    const val VERSION = 3 // sakrīt ar datubāzes versiju

    fun encode(d: BackupData, exportedAt: Long = System.currentTimeMillis()): String = JSONObject().apply {
        put("app", "TameKit")
        put("version", VERSION)
        put("exportedAt", exportedAt)
        put("company", JSONArray(d.company.map { BackupJson.toJson(it) }))
        put("customers", JSONArray(d.customers.map { BackupJson.toJson(it) }))
        put("materials", JSONArray(d.materials.map { BackupJson.toJson(it) }))
        put("movements", JSONArray(d.movements.map { BackupJson.toJson(it) }))
    }.toString(2)

    /** Met IllegalArgumentException, ja fails nav derīgs. */
    fun decode(json: String): BackupData {
        val o = try { JSONObject(json) } catch (e: JSONException) { throw IllegalArgumentException("Not valid JSON", e) }
        require(o.optString("app") == "TameKit") { "Not a TameKit backup" }
        require(o.optInt("version", -1) == VERSION) { "Unsupported backup version" }
        val data = try {
            BackupData(
                o.getJSONArray("company").objects().map { BackupJson.companyFrom(it) },
                o.getJSONArray("customers").objects().map { BackupJson.customerFrom(it) },
                o.getJSONArray("materials").objects().map { BackupJson.materialFrom(it) },
                o.getJSONArray("movements").objects().map { BackupJson.movementFrom(it) }
            )
        } catch (e: JSONException) {
            throw IllegalArgumentException("Missing or malformed data", e)
        }
        validate(data)
        return data
    }

    fun validate(d: BackupData) {
        require(d.company.size <= 1) { "Too many company rows" }
        d.company.forEach { require(it.id == 1L) { "Company id must be 1" } }
        require(d.customers.all { it.id > 0 } && d.materials.all { it.id > 0 } && d.movements.all { it.id > 0 }) { "Ids must be positive" }
        require(d.customers.map { it.id }.toSet().size == d.customers.size) { "Duplicate customer ids" }
        require(d.materials.map { it.id }.toSet().size == d.materials.size) { "Duplicate material ids" }
        require(d.movements.map { it.id }.toSet().size == d.movements.size) { "Duplicate movement ids" }
        d.customers.forEach { require(it.name.isNotBlank()) { "Customer without name" } }
        d.materials.forEach {
            require(it.name.isNotBlank() && it.unit.isNotBlank()) { "Material without name or unit" }
            require(it.purchasePriceCents >= 0) { "Negative price" }
            require(it.minimumStock.signum() >= 0) { "Negative minimum stock" }
        }
        val materialIds = d.materials.map { it.id }.toSet()
        d.movements.forEach {
            require(it.materialId in materialIds) { "Movement refers to a missing material" }
            require(StockCalculator.isSignConsistent(it.movementType, it.quantity)) { "Movement sign does not match its type" }
        }
        StockCalculator.balances(d.movements).values.forEach { require(it.signum() >= 0) { "Negative stock balance" } }
    }

    private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
}

object Backup {
    suspend fun export(db: AppDatabase): String = BackupCodec.encode(
        BackupData(db.company().all(), db.customers().all(), db.materials().all(), db.movements().all())
    )

    /** Fails vispirms pilnībā tiek pārbaudīts; ja tas nederīgs, esošie dati netiek skarti. */
    suspend fun restore(db: AppDatabase, json: String) {
        val d = BackupCodec.decode(json)
        db.withTransaction {
            db.movements().clear(); db.materials().clear(); db.customers().clear(); db.company().clear()
            d.company.forEach { db.company().save(it) }
            db.customers().insertAll(d.customers)
            db.materials().insertAll(d.materials)
            db.movements().insertAll(d.movements)
        }
    }
}
