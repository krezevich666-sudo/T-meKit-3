package lv.tamekit

import lv.tamekit.domain.Parsing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ParsingTest {
    @Test fun decimalAcceptsDotAndComma() {
        assertEquals(BigDecimal("12.5"), Parsing.decimal("12,5"))
        assertEquals(BigDecimal("12.5"), Parsing.decimal(" 12.5 "))
    }

    @Test fun decimalRejectsInvalid() {
        listOf("", "abc", "-1", "1.2.3", "1e5", "12,").forEach { assertNull(it, Parsing.decimal(it)) }
    }

    @Test fun centsAreExact() {
        assertEquals(1250L, Parsing.cents("12,5"))
        assertEquals(1999L, Parsing.cents("19.99"))
        assertEquals(100L, Parsing.cents("1"))
    }

    @Test fun centsRejectMoreThanTwoDecimals() {
        assertNull(Parsing.cents("1.999"))
        assertEquals(1050L, Parsing.cents("10.500"))
    }
}
