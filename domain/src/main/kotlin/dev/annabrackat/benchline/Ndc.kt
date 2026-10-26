package dev.annabrackat.benchline

data class NdcCode(val ndc11: String, val display: String, val source: String)

object Ndc {
    private const val SEGMENT_MESSAGE =
        "Those segment lengths are not a valid NDC. Use 4-4-2, 5-3-2, 5-4-1, or 5-4-2."

    fun checkDigit(data: String): Int {
        var sum = 0
        data.forEachIndexed { index, char ->
            val fromRight = data.length - 1 - index
            val weight = if (fromRight % 2 == 0) 3 else 1
            sum += char.digitToInt() * weight
        }
        return (10 - (sum % 10)) % 10
    }

    fun isValidGtin(full: String): Boolean {
        if (!full.matches(Regex("\\d{12,14}"))) return false
        val body = full.dropLast(1)
        return checkDigit(body) == full.last().digitToInt()
    }

    fun normalize(raw: String): DomainResult<NdcCode> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return DomainResult.Err("format", "Enter an NDC or a 12-digit barcode.")
        }
        if (trimmed.contains('-')) {
            val parts = trimmed.split('-').map { it.trim() }
            if (parts.size != 3 || parts.any { !it.matches(Regex("\\d+")) }) {
                return DomainResult.Err("format", SEGMENT_MESSAGE)
            }
            return fromParts(parts[0], parts[1], parts[2], "label")
        }
        if (!trimmed.matches(Regex("\\d+"))) {
            return DomainResult.Err("format", "NDCs are digits, with hyphens only where the label prints them.")
        }
        if (trimmed.length == 10) {
            return DomainResult.Err(
                "format",
                "A 10-digit NDC needs hyphens. The label is 4-4-2, 5-3-2, or 5-4-1.",
            )
        }
        if (trimmed.length == 11) {
            return fromParts(trimmed.substring(0, 5), trimmed.substring(5, 9), trimmed.substring(9), "digits")
        }
        if (trimmed.length == 12) {
            if (!isValidGtin(trimmed)) {
                return DomainResult.Err("format", "Barcode check digit does not match.")
            }
            val body = trimmed.dropLast(1)
            return fromParts(body.substring(0, 5), body.substring(5, 9), body.substring(9), "barcode")
        }
        return DomainResult.Err("format", "Use a hyphenated NDC, 11 digits, or a 12-digit barcode.")
    }

    private fun fromParts(a: String, b: String, c: String, source: String): DomainResult<NdcCode> {
        val eleven = when ("${a.length}-${b.length}-${c.length}") {
            "4-4-2" -> Triple(a.padStart(5, '0'), b, c)
            "5-3-2" -> Triple(a, b.padStart(4, '0'), c)
            "5-4-1" -> Triple(a, b, c.padStart(2, '0'))
            "5-4-2" -> Triple(a, b, c)
            else -> return DomainResult.Err("format", SEGMENT_MESSAGE)
        }
        val display = "${eleven.first}-${eleven.second}-${eleven.third}"
        return DomainResult.Ok(NdcCode(eleven.first + eleven.second + eleven.third, display, source))
    }
}
