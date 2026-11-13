package lv.tamekit

import lv.tamekit.data.BackupCodec
import lv.tamekit.data.BackupData
import lv.tamekit.data.Company
import lv.tamekit.data.Customer
import lv.tamekit.data.Material
import lv.tamekit.data.MovementType
import lv.tamekit.data.StockMovement
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BackupCodecTest {
    private fun sample() = BackupData(
        company = listOf(Company(1, "SIA Tests", "4000", "123", "a@b.lv", "Rīga", null, 1L)),
        customers = listOf(
            Customer(1, "Jānis", "", "123", "", "", "", false, 2L),
            Customer(2, "Anna", "", "", "", "", "", true, 3L)
        ),
        materials = listOf(Material(1, "Lakas", "", "l", 1250L, BigDecimal("2.5"), "", true, 5L)),
        movements = listOf(
            StockMovement(1, 1, MovementType.IN, BigDecimal("20"), 1250L, null, "OPENING", 10L),
            StockMovement(2, 1, MovementType.OUT, BigDecimal("-5"), null, null, "", 11L)
        )
    )

    @Test fun fullRoundTripKeepsEverything() {
        val d = sample()
        assertEquals(d, BackupCodec.decode(BackupCodec.encode(d, 99L)))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidJson() { BackupCodec.decode("{not json") }

    @Test(expected = IllegalArgumentException::class) fun rejectsForeignFile() {
        BackupCodec.decode("""{"app":"Other","version":3}""")
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsWrongVersion() {
        BackupCodec.decode(BackupCodec.encode(sample()).replace("\"version\": 3", "\"version\": 1"))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsMissingSection() {
        val o = JSONObject(BackupCodec.encode(sample()))
        o.remove("movements")
        BackupCodec.decode(o.toString())
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsMovementForMissingMaterial() {
        val d = sample()
        BackupCodec.validate(d.copy(movements = d.movements + StockMovement(3, 99, MovementType.IN, BigDecimal.ONE)))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsNegativePrice() {
        val d = sample()
        BackupCodec.validate(d.copy(materials = listOf(d.materials[0].copy(purchasePriceCents = -1))))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsSignThatDoesNotMatchType() {
        val d = sample()
        BackupCodec.validate(d.copy(movements = d.movements + StockMovement(3, 1, MovementType.OUT, BigDecimal("5"))))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsNegativeBalance() {
        val d = sample()
        BackupCodec.validate(d.copy(movements = d.movements + StockMovement(3, 1, MovementType.OUT, BigDecimal("-50"))))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsDuplicateIds() {
        val d = sample()
        BackupCodec.validate(d.copy(customers = d.customers + d.customers[0]))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsCompanyWithIdOtherThanOne() {
        val d = sample()
        BackupCodec.validate(d.copy(company = listOf(d.company[0].copy(id = 9))))
    }

    @Test(expected = IllegalArgumentException::class) fun rejectsNonPositiveIds() {
        val d = sample()
        BackupCodec.validate(d.copy(customers = listOf(d.customers[0].copy(id = 0))))
    }
}
