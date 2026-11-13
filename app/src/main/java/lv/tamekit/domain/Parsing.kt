package lv.tamekit.domain

import java.math.BigDecimal

object Parsing {
    private val number = Regex("""^\d+(\.\d+)?$""")

    /** Nenegatīvs decimālskaitlis (komats vai punkts). Tukšs vai nederīgs teksts → null. */
    fun decimal(text: String): BigDecimal? {
        val t = text.trim().replace(',', '.')
        return if (number.matches(t)) BigDecimal(t) else null
    }

    /** Eiro teksts → centi. Nederīgs vai vairāk par 2 zīmēm aiz komata → null. */
    fun cents(text: String): Long? {
        val d = decimal(text) ?: return null
        if (d.stripTrailingZeros().scale() > 2) return null
        return try { d.movePointRight(2).longValueExact() } catch (e: ArithmeticException) { null }
    }
}
