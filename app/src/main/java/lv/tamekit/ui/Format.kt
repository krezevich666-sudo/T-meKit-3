package lv.tamekit.ui

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatEur(cents: Long, locale: Locale): String {
    val nf = NumberFormat.getCurrencyInstance(locale)
    nf.currency = Currency.getInstance("EUR")
    return nf.format(BigDecimal.valueOf(cents, 2))
}

fun qty(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
